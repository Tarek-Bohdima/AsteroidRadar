# AsteroidRadar improvement plan

Asteroid Radar is a Play-Store-published Android app (internal track). This
document is the living roadmap for modernizing it from "works for me" to "easy
to extend, easy to test, painless to release." Phases are independently
shippable; pick them off in order — each one stacks on the last.

## Status at a glance

| Phase | Theme | State |
|---|---|---|
| 0 | Repo hygiene (Dependabot, CI hardening, README, this doc) | Done |
| 1 | Gradle Kotlin DSL + version catalog | Done (#52) |
| 2 | Convention plugin (`build-logic/`) | Done (#54) |
| 3 | Code-quality plumbing (Spotless / Detekt / Lint) | Done (#56, #58, #60) |
| 4 | Toolchain modernization (Kotlin 2.x, AndroidX bumps, Picasso → Coil) | Done (#62, #64, #66) — v2.0.0-INTERNAL tag held; superseded by the v3.0.x device smoke under Phase 9c + the v3.0.1 hotfix |
| 5 | Hilt | Done (#80, #82, #84) |
| 6 | Production hardening (R8, fail-fast on missing API key) | Done (#87, #100) — 6a fail-fast + slim proguard; 6b bundled AGP 8.3 → 8.7.3 + R8 + `shrinkResources` |
| 7 | Tests + Kover | Done (#89, #91, #93) — `koverVerify` 60% INSTRUCTION floor wired in the post-7c follow-up (issue #94) |
| 8 | Edge-to-edge | Done (#97) — included a NoActionBar + Toolbar migration that the issue's non-goal #4 had ruled out |
| 9 | Compose migration | Done (#103, #105, #107) — 9a convention plugin + BOM + bundle; 9b `DetailFragment` → `DetailScreen` via `ComposeView`; 9c `MainFragment` → `MainScreen` + Nav-Compose typed routes (drops Data Binding + safe-args) |
| — | v3.0.0-INTERNAL release-only crash | Hotfix #114 → **v3.0.1-INTERNAL** verified clean. Bad converter-factory loop + Moshi codegen wasn't wired up; details in issue #113. |
| 10 | Dependency-analysis (`buildHealth` gate) | Done (#110) |
| 11 | Moshi → kotlinx.serialization (drop runtime reflection) | Done (#117) — single serialization library across nav routes (Phase 9c) and HTTP (Phase 11). Released as **v3.0.2-INTERNAL**, verified clean. |
| 12 | Persistent APOD + Coil disk cache | In progress — issue #116, branch `feat/persistent-apod-cache`. Addresses the cold-start image latency users have flagged on every v3.x verification. |
| — | **Module split** lands with feature #2, not as a phase | — |

Tick the table when phases land. Each phase below lists scope, rationale, and
the rough size; sub-bullets are the concrete deltas.

## Current shipping state

Snapshot for whoever opens this repo next (likely future-you). Reflects the
state at the close of the v3.0.2 release cycle.

- **Live on Play Internal**: `v3.0.2-INTERNAL` — verified on Pixel 7 Pro
  (clean install through Play Store update), 2026-05-07. Both endpoints
  (NeoWs feed + APOD) load; rotation, navigation, filters all work.
- **v3.x release timeline**:
  - `v3.0.0-INTERNAL` — Phase 9c Compose rewrite. **Broken on real devices**
    via release-only converter-factory regression. Never roll this back to.
  - `v3.0.1-INTERNAL` — hotfix (#114). Fixed a non-local-return loop bug
    in the custom Retrofit factory + wired up `moshi-kotlin-codegen`.
    Verified clean.
  - `v3.0.2-INTERNAL` — Phase 11 (#117). Replaced Moshi with
    kotlinx.serialization end-to-end. Single serialization library across
    nav routes and network. Reflection-free runtime. Verified clean.
- **In progress**: Phase 12 — persistent APOD cache (issue #116, branch
  `feat/persistent-apod-cache`). Room entity + DAO + DB migration 1→2 +
  Repository Flow getter + ViewModel sources `imageOfTheDay` from the DB
  instead of the one-shot network call. Bumps version-of-record to
  `v3.0.3-INTERNAL`. Tag held for device smoke (force-stop-and-relaunch
  to confirm the cached image paints before the asteroid list).
- **Backlog**: issue #112 (NDK debug symbols, build-only fix to silence a
  Play Console warning — five lines under `buildTypes.release { ndk { … } }`).

## Watchpoints for future sessions

- **R8 release-only regressions are a known risk in this codebase.** v3.0.0
  shipped a release-only crash that debug builds couldn't reproduce. Phase
  11 closed the *category* (no runtime reflection in our code anymore), but
  any new dependency that uses Class.forName / KCallable / kotlin-reflect
  needs a release-build smoke before tagging. Run `./gradlew assembleRelease`
  + `adb install` on the AVD before pushing any tag, not just `installDebug`.
- **R8 fallback option**: the user has flagged that they would drop R8
  entirely (`isMinifyEnabled = false`) if a *third* R8-only bug surfaces.
  Phase 11 should make this less likely, but the option stays on the table.
- **Tag-driven release**: pushing any `v*` tag fires the release workflow,
  which builds + signs + publishes a GitHub Release with AAB / APK /
  mapping.txt. The user uploads the AAB to Play Console manually. Don't
  push tags autonomously even with explicit "go ahead" — re-verify
  pre-conditions (version-of-record matches tag, secrets configured,
  master has the merged target PR) first.
- **Issues-first.** Every non-trivial change opens a GitHub issue before
  the PR. Doc, typo, and dep-bump PRs are exempt. Hotfixes still get an
  issue. See [`CLAUDE.md`](../CLAUDE.md#branching-commits-issues).

## Versioning and tags

Tag/release policy lives in [`CLAUDE.md`](../CLAUDE.md#versioning-and-tags).
tl;dr: bump `versionMajor/Minor/Patch` in `app/build.gradle.kts` per phase
that earns it, so the version-of-record stays honest, but only cut a tag
(which triggers a release workflow run + Internal-track-eligible signed
build) when there's an actual reason to ship. Phases can stack into a single
tag. Phase 4's toolchain bump puts the project at `v2.0.0-INTERNAL` in the
build script whether or not we tag immediately.

---

## Phase 0 — Repo hygiene (in progress)

Goal: get the meta-paperwork into shape before touching the build. Skipped
ConsultMe-style community files (CONTRIBUTING / SECURITY / Code of Conduct /
issue + PR templates) — solo project, not worth the surface area.

Three small PRs:

- **CI hardening + Dependabot** — pin third-party Action SHAs in both
  workflows, add weekly grouped Dependabot for `gradle` + `github-actions`
  ecosystems, add `concurrency:` + `permissions: contents: read` to the PR
  workflow, explicit `permissions: contents: write` on release. AGP 9 majors
  pinned in Dependabot until Phase 4 / a dedicated migration. Extend the
  pre-release auto-flag to include `rc` / `RC`.
- **README rewrite** — replace the 10-line "use this Gradle wrapper" note
  with: tagline + status badges, features, tech-stack table, build/run, the
  `local.properties` template (`NASA_API_KEY` + four keystore vars + the
  PKCS12 store-type constraint), release flow, classifier-to-Play-track table.
- **`docs/IMPROVEMENT_PLAN.md` + `CLAUDE.md` update** — this document; plus
  CLAUDE.md gains the project-context note, refactored "Versioning and tags"
  section with the SemVer table and Play-track mapping, branching/commits
  conventions, and a "Recommended Claude Code skills" pointer.

## Phase 1 — Gradle Kotlin DSL + version catalog

Goal: kill stringly-typed Groovy build scripts; centralize versions.

Modexa "[7 Gradle Kotlin DSL Tricks](https://medium.com/@Modexa/7-gradle-kotlin-dsl-tricks-for-human-friendly-builds-68506270906f)" tricks 1, 3, 4, 6, 7 apply directly:

- **Trick 1: Version Catalog** — new `gradle/libs.versions.toml` with
  `[versions]`, `[libraries]`, `[bundles]`, `[plugins]`. Single source of
  truth for every dependency.
- **Trick 3: Type-safe project accessors** —
  `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` in
  `settings.gradle.kts`. `project(":app")` becomes `projects.app`. Cheap to
  enable now even with one module; pays off as soon as a second module lands.
- **Trick 4: Lazy task APIs** — convert any `tasks.create(...)` to
  `tasks.register(...)`. The existing `copyDependencyLibs` task is already
  lazy; this is mostly a check.
- **Trick 6: Bundles** — `[bundles]` for lifecycle libs (lifecycle-viewmodel,
  lifecycle-livedata, lifecycle-runtime + fragment-ktx) and the testing libs
  group (junit, androidx-test-ext, espresso-core).
- **Trick 7: Helper functions** — extract the env-var-or-`local.properties`
  fallback into a typed `env()` helper instead of inlining
  `System.getenv("X") ?: localProperties['X'].toString()` four times.

Module conversions:
- `build.gradle` → `build.gradle.kts` (root)
- `settings.gradle` → `settings.gradle.kts`
- `app/build.gradle` → `app/build.gradle.kts`
- Update Gradle wrapper to a Kotlin-DSL-friendly modern version (Gradle 8.10+).

## Phase 2 — Convention plugin (`build-logic/`)

Goal: move Android/Kotlin/SDK/signing config out of the module script and into
a precompiled script plugin so adding a feature module is near-one-liner.

- New `build-logic/` included build with one plugin:
  `asteroidradar.android.application` — composes AGP-app + kotlin-android +
  kotlin-parcelize + safe-args + KSP plugin application; sets
  compileSdk/minSdk/targetSdk; JVM 17 toolchain; release/debug build types;
  signing config; data binding + buildConfig.
- Shared helpers (`configureKotlinAndroid`, `configureBuildFeatures`) in a
  Kotlin sources module.
- `app/build.gradle.kts` shrinks to plugin application + `namespace` +
  module-specific dependencies.

The feature-module convention plugins (`*.android.library`, `*.android.compose`,
`*.android.hilt`) get added when their respective phases land — Hilt in Phase 5,
Compose in Phase 9 if it happens.

## Phase 3 — Code-quality plumbing

Goal: enforce formatting, static analysis, and lint in CI before tests run.

- **Spotless** — ktlint integration + license-header check that **preserves
  the existing MIT block** (don't rewrite to ConsultMe's `// Copyright $YEAR
  MyCompany` literal). Apply to `*.kt` and `*.gradle.kts`.
- **Detekt** — config in `config/detekt.yml`. Stricter rules can ratchet up
  later; start with the upstream defaults plus a few opt-ins.
- **Android Lint** — set `checkReleaseBuilds = true`, generate
  `app/lint-baseline.xml`. Regenerate per module via
  `./gradlew :<module>:updateLintBaseline` rather than hand-editing.
- **CI ordering** — `spotlessCheck` → `detekt` → `lintRelease` → `test` →
  `assembleDebug`. Fast checks first so signal arrives early.
- Upload lint + test reports as workflow artifacts on every run (with
  `if: always()` so failures still surface them).

## Phase 4 — Toolchain modernization

The chunky one. Shipped as three sub-PRs against the `feat/phase-4*` branch
naming: Kotlin/coroutines bump, AndroidX bump, Picasso → Coil swap. Puts the
project at `v2.0.0-INTERNAL` in `app/build.gradle.kts` (breaking for any
fork — Kotlin major + dependency replacements). Tag is held under the
revised release policy (CLAUDE.md → "ship-when-ready") until we're actually
ready to publish a build.

- **Kotlin 1.6.21 → 2.0.21** (#62). Plus coroutines 1.6.4 → 1.8.1 in the
  same PR — they share the BOM-aligned bump.
- **AndroidX bumps** (#64) — lifecycle 2.8+, fragment 1.8+, navigation 2.8+,
  work-runtime 2.10+, activity 1.10+. Room was already on 2.6 and didn't
  need this PR; the 2.7+ bump can ride along with Phase 5's Hilt work.
- **Picasso → Coil 2.7.0** (#66). Updated `BindingAdapters.kt` to use Coil's
  `imageView.load(uri) { … }` extension. Moved the `centerCrop` scale type
  onto `fragment_main.xml` since Coil composes scaling on the view, not the
  request. Dropped a Picasso-only `NotificationPermission` lint-baseline
  entry.
- **Drop `kotlin-kapt` plugin application — deferred.** AGP 8.3.0's Data
  Binding compiler still discovers `@BindingAdapter` methods via kapt. Will
  drop when we're on AGP 8.6+ (Data Binding moves to KSP); rolls into the
  AGP-9 / Phase-6 vicinity rather than belonging here.

**Open Phase 4 follow-up — manual device smoke.** Before tagging
`v2.0.0-INTERNAL`, install the debug APK on a device and exercise the four
flows: NeoWs feed loads (today/week/saved filters), APOD image renders
through Coil's crossfade, navigation to detail + back, background
`RefreshDataWorker` triggers (Charging + WiFi + idle). The instrumented
`MainActivityCoilSmokeTest` (#66) is a downpayment on this but doesn't
exercise the full happy path; it only asserts views render. Phase 7 will
fold real Espresso coverage in.

## Phase 5 — Hilt

Goal: introduce DI before feature work starts so feature modules slot in with
`@HiltViewModel` / `@AndroidEntryPoint` already wired. Hilt 2.51+ requires
Kotlin 2.0+, which is why this phase comes after the Phase 4 toolchain bump.

### Sub-PR breakdown

The phase splits cleanly into three sub-PRs because Hilt allows partial
adoption — `@HiltAndroidApp` alone doesn't break anything until something
asks Hilt for an injection. Branch naming: `feat/phase-5a-…` etc.

- **5a — Plugin scaffolding (`feat/phase-5a-hilt-scaffold`).** New
  convention plugin `asteroidradar.android.hilt` adds the hilt-gradle + ksp
  plugins, sets `enableAggregatingTask = true`, declares `hilt-android` impl
  and `hilt-compiler` ksp. Apply it to `:app`. Add `@HiltAndroidApp` to
  `AsteroidRadarApplication`. **No behavior change** — app still uses the
  manual `getDatabase()` + `MainViewModel.Factory` paths. Verifies the
  build pipeline + Hilt-component generation works in isolation.
- **5b — DB module + ViewModel migration (`feat/phase-5b-hilt-vm`).** New
  `@Module @InstallIn(SingletonComponent::class)` providing
  `AsteroidDatabase`, `AsteroidDao`, `AsteroidRepository` as
  `@Provides @Singleton`. `MainActivity` / `MainFragment` /
  `DetailFragment` get `@AndroidEntryPoint`. `MainViewModel` becomes
  `@HiltViewModel` with a constructor-injected `AsteroidRepository`; the
  inner `Factory` class is removed and `MainFragment` switches to
  `by viewModels()` (or `hiltViewModel()`). The `getDatabase()` global is
  *kept for now* — `RefreshDataWorker` still uses it.
- **5c — Worker migration (`feat/phase-5c-hilt-worker`).** Add
  `androidx.hilt:hilt-work` + the `androidx-hilt-compiler` ksp processor.
  `RefreshDataWorker` becomes `@HiltWorker` with a constructor-injected
  `AsteroidRepository`. Wire a `HiltWorkerFactory` into the application via
  `Configuration.Provider` (so WorkManager picks up Hilt-managed workers).
  Delete the `getDatabase()` global singleton from
  `database/AsteroidDatabase.kt` and the `lateinit INSTANCE` machinery —
  the Hilt module is now the only construction site.

### Things to watch during Phase 5

- **Data Binding still uses kapt** (Phase 4 deferred kapt removal). Hilt
  uses ksp. Both should coexist fine — they have separate annotation sets.
  If kapt and ksp clash on a generated source path, the
  `sourceSets.main.java.srcDir 'build/generated/ksp/src/main/kotlin'` shim
  in `app/build.gradle.kts` is the suspect.
- **Worker construction is the riskiest delete.** `RefreshDataWorker` is
  enqueued from `AsteroidRadarApplication.onCreate()`; if
  `Configuration.Provider` isn't wired correctly, WorkManager falls back
  to its default factory and Hilt-injected workers fail with
  "could not instantiate" at runtime. Test on a device — the unit-test
  classpath won't catch this.
- **`MainViewModel.Factory`'s removal**. `MainFragment` currently
  constructs `MainViewModel` via the inner `Factory` to pass `Application`
  in for `getDatabase(application)`. Once the DB is `@Provides`-d,
  `Application` doesn't need to flow through; just inject the repository
  directly into `MainViewModel`.

## Phase 6 — Production hardening

Goal: a release build should fail fast on missing config, and ship a real
minified APK.

### Sub-PR breakdown

The phase splits because R8 from AGP 8.3.0 can't parse the Kotlin 2.1
metadata that Retrofit 3.0+ ships with — R8 enablement therefore rides
with the eventual AGP bump.

- **6a (`feat/phase-6a-fail-fast-and-slim-proguard`).** Configure-time
  fail-fast on a blank `NASA_API_KEY` for any release task in the graph
  (debug + tests stay lenient). `app/proguard-rules.pro` slimmed to
  `-keepattributes SourceFile,LineNumberTable` +
  `-renamesourcefileattribute SourceFile`; the old Glide / Gson /
  broad-Serializable rules go away. R8 stays disabled — slim rules just
  pre-stage 6b. Ships now.
- **6b (`feat/phase-6b-enable-r8`).** Flip `isMinifyEnabled = true` +
  `isShrinkResources = true`. Lint baseline regen if R8-related warnings
  appear. Combine with the AGP bump (Room 2.8.x and ktlint/spotless 7.x
  also wait on AGP ≥ 8.5) so the toolchain advances once. Use the
  [`r8-analyzer`](https://github.com/android/skills) Claude Code skill for
  keep-rule debugging if a release smoke surfaces a missing rule.

## Phase 7 — Tests + Kover

Goal: replace the empty `Example*Test` shells with real coverage that doubles
as documentation for "how to test in this repo."

### Sub-PR breakdown

The phase splits into infrastructure-then-tests so the first PR doesn't have
to defend coverage numbers, and the test PRs don't have to defend
build-script changes.

- **7a (`feat/phase-7a-kover-and-test-bundle`).** Kover plugin (HTML report
  task `./gradlew :app:koverHtmlReport`); `test-shared` bundle in
  `libs.versions.toml` (junit + truth + mockk + turbine + coroutines-test);
  CI uploads the HTML report as a `kover-report` artifact alongside the
  existing `build-reports`. **No new tests, no `koverVerify` gate.** The
  existing `ParseAsteroidsJsonResultTest` is the smoke that confirms the
  Kover wire-up.
- **7b (`feat/phase-7b-jvm-tests`).** Real JVM tests on the new bundle:
  `AsteroidRepository.getAsteroidSelection` filter mapping with a fake DAO
  (the LoD fix in 5c made this trivially possible); `MainViewModel` state
  transitions with Turbine + MockK + Truth; parser-test edge cases.
- **7c (`feat/phase-7c-instrumented-tests`).** `AsteroidDao` integration
  test using in-memory Room — `getAsteroids`, `getTodayAsteroids`,
  `getWeeklyAsteroids`, `deletePreviousAsteroid`. Empty `Example*Test`
  shells get deleted in this PR (truly replaced now).
- **After 7c — done.** `koverVerify` enforces a 60% INSTRUCTION floor on a
  filtered scope (parser, repo, domain, ViewModel, DAO entities). Generated
  code (DataBinding, Hilt, Room `*_Impl`, safe-args) and pure-UI surfaces
  (Application/Activity/Fragments/Adapter/Worker/BindingAdapters) are
  excluded — they need Espresso or on-device smoke, not unit tests. Filter
  list lives in `app/build.gradle.kts`. Ratchet up the floor when future
  test PRs raise the actual coverage. XML report (Codecov / SonarCloud) is
  still a non-goal for the phase.

## Phase 8 — Edge-to-edge

Goal: opt into edge-to-edge layouts. Modern Android (16+) effectively requires
this. The official [`edge-to-edge`](https://github.com/android/skills) Claude
Code skill is a ready-made adoption guide — install when this phase starts.

- `MainActivity.onCreate` calls `WindowCompat.setDecorFitsSystemWindows(window,
  false)`.
- Status-bar / navigation-bar colors via theme attributes.
- Insets handled in fragment root layouts via `OnApplyWindowInsetsListener` or
  the AndroidX insets-compat APIs.

## Phase 9 — Compose migration

Goal: rewrite the UI in Jetpack Compose. View system + Data Binding works
fine, but Compose is the long-term path — Android Studio templates, Google
sample apps, and most modern Jetpack libraries (incl. the
[`jetpack-compose`](https://github.com/android/skills) Claude Code skill)
assume it. Phase 9 also pays down two incidental debts: it deletes every
`@BindingAdapter`, which lets us drop the last `kotlin-kapt` user, and it
replaces safe-args wiring with type-safe Compose Navigation routes.

**Sequencing.** Phase 9 lands **after Phase 6b** (R8 + AGP bump). Reasons:
(a) keep-rule debugging is cleaner against the existing view-system surface
than against a Compose-runtime-plus-view-system mix; (b) the AGP bump is
what makes `kotlin-kapt` removal viable, and Phase 9c is the natural site
for that cleanup since it deletes the last `@BindingAdapter`. Bumps the
project to `v3.0.0-INTERNAL` (Compose rewrite is a breaking change for any
fork); tag held until 9c lands and we device-smoke.

### Sub-PR breakdown

The phase splits into three sub-PRs because Compose allows partial adoption
— a `ComposeView` inside a fragment is a normal interop point, so 9b can
ship the smaller surface (Detail) without disrupting Main. Branch naming:
`feat/phase-9a-…` etc.

- **9a — Foundation (`feat/phase-9a-compose-foundation`).** New convention
  plugin `asteroidradar.android.compose` enables `buildFeatures.compose =
  true` and applies the `org.jetbrains.kotlin.plugin.compose` plugin (the
  Compose Compiler ships as a Kotlin plugin since Kotlin 2.0 — no separate
  compiler version pin). Catalog gains `androidx-compose-bom`,
  `androidx-compose-ui`, `compose-ui-tooling-preview` (debug),
  `compose-material3`, `androidx-activity-compose`,
  `androidx-lifecycle-runtime-compose`, `androidx-hilt-navigation-compose`,
  `androidx-navigation-compose`, `coil-compose`. New
  `[bundles] compose = […]`. Apply convention to `:app`. Add the
  [Mans0n Compose detekt rules](https://github.com/mrmans0n/compose-rules)
  to the existing detekt config. **No user-visible change**; a single
  throwaway `@Preview` Composable verifies the build pipeline.
- **9b — DetailFragment → DetailScreen (`feat/phase-9b-compose-detail`).**
  New `ui/detail/DetailScreen.kt` reproduces `fragment_detail.xml` in
  Compose; help-button surfaces a Material 3 `AlertDialog`; status drawables
  go through `coil-compose`'s `AsyncImage`. `DetailFragment` becomes a thin
  `ComposeView` shim — Nav-Compose migration deferred to 9c so this PR is
  scoped to one screen. Delete `fragment_detail.xml`; delete the four
  Detail-only `@BindingAdapter`s after grep-confirming
  `item_view_list_asteroids.xml` and `fragment_main.xml` don't use them.
  Add `compose-ui-test-junit4` smoke against `DetailScreen`.
- **9c — MainFragment → MainScreen + Nav-Compose
  (`feat/phase-9c-compose-main`).** Convert `MainViewModel`'s `LiveData` to
  `StateFlow`; UI collects via `collectAsStateWithLifecycle`. New
  `ui/main/MainScreen.kt`: Material 3 `Scaffold` + `TopAppBar` overflow
  (three filter items) + APOD header + `LazyColumn` with
  `items(asteroids, key = { it.id })` (replaces `AsteroidAdapter` +
  `DiffUtil`). Insets via `Modifier.windowInsetsPadding(...)` — reuses the
  Phase 8 work. `MainActivity` becomes a `ComponentActivity` with
  `setContent { … }` driving a Compose `NavHost` (Nav 2.8+ typed routes via
  kotlinx-serialization, replacing safe-args). **Deletes**: `MainFragment`,
  `DetailFragment` (the shim), `AsteroidAdapter`, `fragment_main.xml`,
  `item_view_list_asteroids.xml`, `main_overflow_menu.xml`,
  `main_nav_graph.xml`, `activity_main.xml`, `BindingAdapters.kt` (now
  empty). **Build-script cleanup**: drop `dataBinding = true`, the
  `kotlin-kapt` plugin, the `androidx.navigation.safeargs.kotlin` plugin,
  the `build/generated/ksp/...` srcDir shim. **Catalog cleanup**: drop
  `androidx-recyclerview`, `androidx-fragment-ktx`,
  `androidx-navigation-fragment-ktx`, `androidx-navigation-ui-ktx`, the
  safe-args Gradle plugin entry. Regenerate lint baseline once.

### Things to watch during Phase 9

- **Coil 2 vs Coil 3.** Phase 4 landed Coil 2.7.0. If a BOM bump pulls Coil
  3 in, imports move to `coil3.compose.AsyncImage` and the artifact is
  still `coil-compose`. Don't mix versions across `coil-kt` and
  `coil-compose` artifacts.
- **Hilt + Compose Navigation interop.** Use `hiltViewModel()` from
  `androidx.hilt:hilt-navigation-compose`, not `viewModel()` —
  `hiltViewModel()` scopes to the nav backstack entry, which is what we
  want for `MainViewModel` per-graph singletonship.
- **Edge-to-edge regressions.** Phase 8 used `WindowInsetsCompat`
  listeners; Compose uses `Modifier.windowInsetsPadding`. Easy to forget
  the bottom inset on a `LazyColumn` and lose access to the last row —
  device-smoke on a gesture-nav handset before tagging.
- **R8 keep rules.** Compose itself is well-rulebooked, but
  `kotlinx-serialization` typed Nav routes need a serializer keep rule.
  Phase 6b establishes the R8 baseline; 9c adds the first new keep surface
  since.
- **Test infra.** Existing JVM tests (repo, ViewModel, parser) don't
  change. UI smoke moves from the empty `MainActivityCoilSmokeTest`
  (#66 down-payment, never filled in) to `compose-ui-test-junit4` against
  `MainScreen` / `DetailScreen`.

## Phase 10 — Dependency-analysis (`buildHealth` gate)

Goal: wire Tony Robalik's [dependency-analysis-gradle-plugin](https://github.com/autonomousapps/dependency-analysis-gradle-plugin) (DAGP) so unused / misplaced dependencies fail CI before they survive into another release.

- DAGP applied via the `asteroidradar.android.application` convention plugin (root-only application doesn't reach subprojects whose Android plugin is applied via a precompiled script plugin); root keeps `:buildHealth` as the aggregator.
- Issue-severity overrides in root `build.gradle.kts` silence "use transitive deps directly" — the Compose BOM is the version-of-record for the whole `compose.*` tree. Whitelists `androidx.work:work-runtime-ktx` from "unused" because DAGP misses inline-reified `PeriodicWorkRequestBuilder<T>` usage.
- First DAGP run flagged 8 unused deps — fixed in-place rather than baselined: dropped `androidx-activity-ktx`, `androidx-core-ktx`, `androidx-lifecycle-viewmodel-ktx`, `androidx-room-ktx` (`viewModelScope` is in `lifecycle-viewmodel` since 2.5+; no `withTransaction` usage), `coil` non-compose, `retrofit-coroutines-adapter` (Retrofit 3 has native suspend), `androidx-espresso-core` and `androidx-test-core-ktx` (no Espresso / `ActivityScenario` survived 9c).
- Reclassified `kotlinx-coroutines-android` `implementation` → `runtimeOnly` (`Dispatchers.Main` resolves through `coroutines-core`; the Android impl loads via `ServiceLoader`); `compose-ui-test-manifest` `debugImplementation` → `debugRuntimeOnly`.
- New CI step `Run dependency-analysis (buildHealth)` after `lintRelease`, before `assembleDebug`. Report attached as a workflow artifact.

## Phase 11 — Moshi → kotlinx.serialization

Goal: single serialization library across the codebase, fully reflection-free at runtime. Phase 9c already added `kotlinx-serialization-json` for Nav-Compose typed routes; Phase 11 reaps that investment by routing the network JSON path through it as well.

Forced by the v3.0.0 release-only crash (issue #113 / #114): Moshi's reflection adapter (`KotlinJsonAdapterFactory`) is fragile under R8 because R8 strips most of `kotlin-reflect`. The hotfix wired `moshi-kotlin-codegen` as a band-aid; Phase 11 removed the *category* by deleting Moshi entirely.

- `network/DataTransferObjects.kt` — `ImageOfTheDay` flips from `@JsonClass(generateAdapter = true)` + `@Json(name = "media_type")` to `@Serializable` + `@SerialName("media_type")`.
- `network/service.kt` — drops `Moshi.Builder() + KotlinJsonAdapterFactory + MoshiConverterFactory`; uses `Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())` from the official `retrofit2-converter-kotlinx-serialization`. Builder order unchanged: Scalars first (raw `String` body for `getAsteroids`), kotlinx-serialization second (`@Serializable` DTOs).
- Catalog: drop `moshi`, `moshi-kotlin`, `moshi-kotlin-codegen`, `retrofit-converter-moshi` + the `moshi` version pin; add `retrofit-converter-kotlinx-serialization`.
- `domain/PictureOfDay.kt` cleaned up — dead `@Json` annotation removed (domain models don't carry serialization annotations; the conversion happens in `asDomainModel()`).
- Bumps to `v3.0.2-INTERNAL`. Verified post-merge end-to-end through Play Internal Testing on a Pixel 7 Pro, 2026-05-07.

**Out of scope** — migrating `parseAsteroidsJsonResult` (the `org.json.JSONObject`-based parser of the nested-by-date NeoWs feed) to kotlinx.serialization. Doable with a `JsonTransformingSerializer`; small surface, captured as a possible Phase 11b if appetite remains.

## Phase 12 — Persistent APOD + Coil disk cache

Goal: kill the cold-start APOD-image latency that users have flagged on every v3.x verification. Two serial network round-trips (APOD endpoint → URL, then URL → image bytes) make the APOD visibly later than the asteroid list. Tracked in issue #116. Bumps to `v3.0.3-INTERNAL` (patch — internal refactor with a UX win, no new screen/filter/data-source).

- **Room**: new `picture_of_day` table (one row, replace-on-conflict) via `DatabasePictureOfDay` + `PICTURE_OF_DAY_ROW_ID = 0`. DB version 1 → 2 with `MIGRATION_1_2` (additive — `asteroid_database` untouched).
- **DAO**: `PictureOfDayDao.getPictureOfDay(): Flow<DatabasePictureOfDay?>` (single-row `WHERE id = 0`) and a suspend `insertPictureOfDay(...)` with `OnConflictStrategy.REPLACE`.
- **DI**: `DatabaseModule` adds the `addMigrations(MIGRATION_1_2)` call, provides `PictureOfDayDao`, and threads it into `AsteroidRepository`'s constructor.
- **Repository**: `getPictureOfDay(): Flow<PictureOfDay?>` reads from the DAO; new `refreshPictureOfDay()` writes the network result via `ImageOfTheDay.asDatabaseModel()`. The old imperative `getImageOfTheDay()` is deleted — Room is now the single source of truth, mirroring the asteroid path.
- **MainViewModel**: `imageOfTheDay: StateFlow<PictureOfDay?>` is sourced from `repository.getPictureOfDay()` via `stateIn(WhileSubscribed(5_000))`. `init` kicks off `refreshAsteroids()` and `refreshPictureOfDay()` in two independent `viewModelScope.launch` blocks so the round-trips don't queue.
- **Coil disk cache**: Coil 2's default disk cache (~2% of `cacheDir`) covers same-URL re-requests across cold starts; verified manually via cold-start-twice smoke. No explicit `ImageLoader { diskCache(…) }` configured — the default suffices for one-image-per-day cache pressure.

### Tests

- JVM: `MainViewModelTest` — `init triggers parallel refresh of asteroids and APOD`, `imageOfTheDay mirrors the cached APOD flow`, `refreshPictureOfDay error path is swallowed`.
- JVM: `AsteroidRepositoryTest` — `getPictureOfDay maps the cached entity to the domain model`, `getPictureOfDay emits null when nothing is cached`.
- Instrumented: `PictureOfDayDaoTest` — `getPictureOfDay_emitsNullBeforeFirstInsert`, `getPictureOfDay_returnsRowAfterInsert`, `insertPictureOfDay_replacesExistingRowOnConflict`.
- Manual smoke: fresh install (one-time slow path), force-stop + relaunch (instant from Room + Coil disk cache), next-day relaunch (yesterday paints instantly, today swaps in via crossfade).

A `MigrationTestHelper` test would require wiring `room.schemaLocation` and committing a baseline schema JSON — out of scope for this PR. The migration is small and additive; the in-memory DAO test exercises the v2 schema.

## Quality bets to consider (no phase yet)

- **Baseline profiles + a macrobenchmark module** for cold-start
  performance. Worthwhile after Phase 5 / Hilt landed (DI graph affects
  startup); compounds nicely with Phase 12.
- **Gradle build scans** if a Develocity instance becomes available.
- **Jacoco merge** — alternative coverage backend if Kover blocks on a Kotlin
  bump.

## How to use this document

When starting a phase, change its row in the status table to **In progress**,
link the issue/PR, and update sub-bullets with checkboxes if it helps. When
merged, mark **Done** with the PR number. The point is to keep the next-best
action obvious to whoever opens the repo a month from now (often: future-you).
