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
| 4 | Toolchain modernization (Kotlin 2.x, AndroidX bumps, Picasso → Coil) | Done (#62, #64, #66) — manual device smoke pending |
| 5 | Hilt | Done (#80, #82, #84) |
| 6 | Production hardening (R8, fail-fast on missing API key) | **In progress** — 6a (#87) shipped fail-fast + slim proguard; 6b (#86) enables R8 alongside the AGP bump |
| 7 | Tests + Kover | **In progress** — 7a (#88) lands Kover infra + test-shared bundle; 7b/7c add real tests |
| 8 | Edge-to-edge | Pending |
| 9 | Compose migration | Deferred |
| — | **Module split** lands with feature #2, not as a phase | — |

Tick the table when phases land. Each phase below lists scope, rationale, and
the rough size; sub-bullets are the concrete deltas.

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
- **After 7c:** turn on `koverVerify` with a soft floor (60% per the plan),
  ratchet up. XML report (Codecov / SonarCloud) is a future thing — still
  a non-goal for the phase.

## Phase 8 — Edge-to-edge

Goal: opt into edge-to-edge layouts. Modern Android (16+) effectively requires
this. The official [`edge-to-edge`](https://github.com/android/skills) Claude
Code skill is a ready-made adoption guide — install when this phase starts.

- `MainActivity.onCreate` calls `WindowCompat.setDecorFitsSystemWindows(window,
  false)`.
- Status-bar / navigation-bar colors via theme attributes.
- Insets handled in fragment root layouts via `OnApplyWindowInsetsListener` or
  the AndroidX insets-compat APIs.

## Phase 9 — Compose migration (deferred)

Goal: rewrite the UI in Jetpack Compose. Tracked here so it's not forgotten;
**not committed to** as a near-term phase. View system + Data Binding works
fine for this app and the migration is a several-PR effort.

If/when it happens: introduce the `asteroidradar.android.compose` convention
plugin, migrate `DetailFragment` first (simpler), then `MainFragment`. Use the
[`jetpack-compose`](https://github.com/android/skills) Claude Code skill.

## Quality bets to consider (no phase yet)

- **`dependency-analysis-android-gradle-plugin`** — flag unused / misplaced
  dependencies. Highest signal once we have multiple modules.
- **Baseline profiles + a macrobenchmark module** for cold-start
  performance. Worthwhile after Phase 5 / Hilt landed (DI graph affects
  startup).
- **Gradle build scans** if a Develocity instance becomes available.
- **Jacoco merge** — alternative coverage backend if Kover blocks on a Kotlin
  bump.

## How to use this document

When starting a phase, change its row in the status table to **In progress**,
link the issue/PR, and update sub-bullets with checkboxes if it helps. When
merged, mark **Done** with the PR number. The point is to keep the next-best
action obvious to whoever opens the repo a month from now (often: future-you).
