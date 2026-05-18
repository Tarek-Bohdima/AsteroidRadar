# Structured logging — sealed events + Hilt fanout

A reusable pattern for Android logging that scales from "Logcat-only in
debug" to "multi-sink remote observability" without touching call sites
when you add or remove a sink.

This document is the load-bearing reference: copy it into another Kotlin
Android project, follow the recipe, get the same pattern. The worked
example lives in `app/src/main/java/com/tarek/asteroidradar/log/`.

## The problem with printf-style logging

`Timber.d("Repository: refresh failed %s", e.message)` rots:

- **Free-text messages** drift apart over time. Every call site invents
  its own prefix ("Repository:", "MainViewModel:") and format.
- **No typed payload.** Adding a new field (`durationMs`, `userId`) means
  editing every call site that should carry it, and every grep that
  needs to find them.
- **No taxonomy.** Filtering by domain ("show me everything network-related")
  relies on grepping prefixes, which has no compile-time guarantee.
- **Sink lock-in.** When you eventually want to ship structured events
  to Crashlytics / Sentry / Datadog, every call site has to learn the
  new SDK. The migration is `O(call sites)`.

The pattern below solves all four.

## The pattern in one diagram

```
                   ┌─────────────────┐
   call site ────▶ │   Logger (DI)   │ ─── interface, what consumers depend on
                   └────────┬────────┘
                            │
                   ┌────────▼────────┐
                   │ CompositeLogger │ ─── singular Hilt binding, fans out
                   └────────┬────────┘
                            │  Set<Logger>  (multibound via @IntoSet)
                ┌───────────┼──────────────┐
                ▼           ▼              ▼
         ┌──────────┐ ┌───────────────┐ ┌──────────────┐
         │ Timber   │ │  Crashlytics  │ │  …other      │
         │ Logger   │ │  Logger       │ │  sinks       │
         └──────────┘ └───────────────┘ └──────────────┘
```

Four moving parts:

1. **`LogEvent`** — sealed taxonomy. Each concrete event carries a tag,
   priority, message, optional attributes, and an optional `Throwable`.
2. **`Logger`** — the contract. One method: `log(event: LogEvent)`.
3. **`CompositeLogger`** — the dispatcher. Injects `Set<Logger>` and
   forwards every event to every sink.
4. **Hilt module** — wires sinks into the set (`@IntoSet`) and binds
   `CompositeLogger` as the singular `Logger` consumers see.

## Step 1 — Define `LogPriority`

Platform-independent severity. Kept out of `android.util.Log` so JVM unit
tests can construct events without an Android classpath.

```kotlin
enum class LogPriority { Verbose, Debug, Info, Warn, Error }
```

Sinks map this to their own level type (Timber's `.v/.d/.i/.w/.e`,
Crashlytics's `setCustomKey`/`recordException`, etc.).

## Step 2 — Define the `LogEvent` taxonomy

Group concrete events by domain via nested `sealed class`es:

```kotlin
sealed class LogEvent {
    abstract val tag: String
    abstract val priority: LogPriority
    abstract val message: String
    open val attributes: Map<String, Any> = emptyMap()
    open val throwable: Throwable? = null

    sealed class Network : LogEvent() {
        override val tag: String = "Network"

        data class RequestFailed(
            val url: String,
            val cause: Throwable,
        ) : Network() {
            override val priority = LogPriority.Warn
            override val message = "request_failed"
            override val attributes: Map<String, Any> = mapOf("url" to url)
            override val throwable: Throwable? = cause
        }
    }

    sealed class App : LogEvent() {
        override val tag: String = "App"

        data object Launched : App() {
            override val priority = LogPriority.Info
            override val message = "application onCreate"
        }
    }
}
```

Use `data object` for parameter-less events; `data class` for events with
typed payloads. The compiler enforces that every concrete event provides
the abstract properties — you can't forget a tag the way you can with
`Timber.d("...")`.

## Step 3 — The `Logger` contract

```kotlin
fun interface Logger {
    fun log(event: LogEvent)
}
```

A `fun interface` lets sinks be written as lambdas in tests, but the
common case is a class implementation injected by Hilt.

## Step 4 — Write a sink

A sink is a `Logger` implementation that knows one rendering. Timber:

```kotlin
@Singleton
class TimberLogger @Inject constructor() : Logger {
    override fun log(event: LogEvent) {
        val tree = Timber.tag(event.tag)
        val msg = event.message + event.attributes.toSuffix()
        when (event.priority) {
            LogPriority.Verbose -> tree.v(event.throwable, msg)
            LogPriority.Debug -> tree.d(event.throwable, msg)
            LogPriority.Info -> tree.i(event.throwable, msg)
            LogPriority.Warn -> tree.w(event.throwable, msg)
            LogPriority.Error -> tree.e(event.throwable, msg)
        }
    }

    private fun Map<String, Any>.toSuffix(): String =
        if (isEmpty()) "" else entries.joinToString(
            prefix = " ", separator = " ",
        ) { "${it.key}=${it.value}" }
}
```

A future remote sink (Crashlytics, Sentry) is exactly the same shape — a
class implementing `Logger`, mapping the event into the SDK's API.

## Step 5 — The composite dispatcher

```kotlin
@Singleton
class CompositeLogger @Inject constructor(
    private val sinks: Set<@JvmSuppressWildcards Logger>,
) : Logger {
    override fun log(event: LogEvent) {
        sinks.forEach { it.log(event) }
    }
}
```

`@JvmSuppressWildcards` is required for Dagger multibinding sets in
Kotlin — without it Hilt generates `Set<? extends Logger>` and refuses
to inject your set.

## Step 6 — The Hilt module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {
    @Binds
    @IntoSet
    abstract fun bindsTimberLoggerIntoSet(impl: TimberLogger): Logger

    @Binds
    abstract fun bindsLogger(impl: CompositeLogger): Logger
}
```

Two bindings, two roles:

- `@IntoSet` contributions populate `Set<Logger>` — only TimberLogger here.
- The plain `@Binds Logger` (no `@IntoSet`) is the singular `Logger`
  binding consumers inject. Hilt sees them as separate binding keys —
  `Set<Logger>` and `Logger` — so `CompositeLogger`'s injected set won't
  accidentally include itself.

## Step 7 — Call sites

Anywhere that previously called `Timber.d(...)`:

```kotlin
class MainViewModel @Inject constructor(
    private val repository: Repository,
    private val logger: Logger,
) : ViewModel() {
    init {
        viewModelScope.launch {
            try {
                repository.refresh()
            } catch (e: Exception) {
                logger.log(LogEvent.Network.RequestFailed(url = "/feed", cause = e))
            }
        }
    }
}
```

The call site only knows about `Logger` and `LogEvent`. Nothing else.

## Adding a new sink — the payoff

To add Sentry (or Crashlytics, or Datadog if you like burning money):

1. Write `SentryLogger : Logger`.
2. Add one line to `LoggerModule`:
   `@Binds @IntoSet abstract fun bindsSentryLogger(impl: SentryLogger): Logger`.
3. Done.

Zero call-site changes. The fanout picks up the new sink automatically.

## What this pattern is **not**

- **Not RUM (Real User Monitoring).** RUM tracks session/screen
  lifecycles via SDK-specific instrumentation (Firebase Performance,
  Datadog RUM, Sentry Replays). It can complement structured logging but
  isn't replaced by it.
- **Not APM tracing.** Distributed tracing across mobile → backend
  requires OkHttp interceptors + server-side span correlation. Out of
  scope for the log path.
- **Not log aggregation.** That's the sink's responsibility. The pattern
  delivers events *to* a sink; getting them into a queryable dashboard
  is the sink's problem.

## Pitfalls to avoid

- **Don't make `CompositeLogger` itself bind into the set.** A naïve
  `@Binds @IntoSet abstract fun bindsComposite(impl: CompositeLogger): Logger`
  would create a recursive injection. Keep the `@IntoSet` bindings to
  sink-only types, and the singular `@Binds` binding to the composite.
- **Don't forget `@JvmSuppressWildcards` on the `Set<Logger>` injection.**
  Without it, Dagger's Java-flavored type signature collides with
  Kotlin's variance and the module fails to compile.
- **Don't forget to map nullability on `Throwable`.** If a concrete event
  always carries a cause, declare the constructor parameter non-null
  (`val cause: Throwable`) and assign it to the base override
  (`override val throwable: Throwable? = cause`). Don't force callers
  through nullability they shouldn't have to think about.
- **Don't catch exceptions inside `CompositeLogger.log`.** If a sink
  misbehaves, you want the error to surface — not get swallowed silently.
  Add per-sink resilience inside the sink if you need it, not at the
  fanout layer.

## Alternative shapes worth knowing

- **Timber-tree fanout** (sinks-as-`Timber.Tree` subclasses, call sites
  stay `Timber.tag(...)..i(...)`). Lower ceremony, no event taxonomy,
  no compile-time payload completeness. Good for tiny apps with one
  remote sink. Bad if you want the typed-event benefits.
- **Single Logger interface, Set injection at call site.** Skip
  `CompositeLogger`; consumers inject `Set<Logger>` directly and iterate.
  Saves a class but pushes the loop into every call site. Not worth it.
- **NowInAndroid-style build-type swap.** `Stub*Helper` for debug,
  `Firebase*Helper` for release, swapped via Hilt module variants. Works
  for analytics where you typically want *one* impl per build type;
  doesn't generalise to multi-sink fanout.

## See also

- `app/src/main/java/com/tarek/asteroidradar/log/` — the worked example.
- `docs/IMPROVEMENT_PLAN.md` Phase 15 — the implementation history,
  including the 15a / 15b split.
