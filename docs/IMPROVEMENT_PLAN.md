# AsteroidRadar improvement plan

Asteroid Radar is a Play-Store-published Android app (internal track). This
document is the living roadmap for modernizing it from "works for me" to "easy
to extend, easy to test, painless to release." Phases are independently
shippable; pick them off in order — each one stacks on the last.

## Status at a glance

| Phase | Theme | State |
|---|---|---|
| 0 | Repo hygiene (Dependabot, CI hardening, README, this doc) | **In progress** |
| 1 | Gradle Kotlin DSL + version catalog | Pending |
| 2 | Convention plugin (`build-logic/`) | Pending |
| 3 | Code-quality plumbing (Spotless / Detekt / Lint) | Pending |
| 4 | Toolchain modernization (Kotlin 2.x, AndroidX bumps, Picasso → Coil) | Done |
| 5 | Hilt | Pending |
| 6 | Production hardening (R8, fail-fast on missing API key) | Pending |
| 7 | Tests + Kover | Pending |
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

The chunky one. Touches every dependency declaration; one PR, one device-tested
release. Tag this as `v2.0.0-INTERNAL` — breaking for any fork.

- **Kotlin 1.6.21 → 2.0.x** (or current stable). 1.6 is from 2022 and blocks
  modern AndroidX bumps.
- **Coroutines 1.6.4 → 1.8.x.**
- **AndroidX bumps** — lifecycle 2.8+, fragment 1.8+, navigation 2.8+,
  work-runtime 2.10+, room 2.7+ (already 2.6). Activity 1.10+.
- **Picasso → Coil 2.x.** Picasso last released in 2018; Coil is the modern
  Kotlin-native equivalent and works fine in the view system. Update
  `BindingAdapters.kt` to use Coil's `load()` extension.
- **Drop `kotlin-kapt` plugin application** — KSP is the only annotation
  processor in this project (Room). The kapt plugin is currently applied but
  unused.
- Verify the app still works end-to-end on a device: NeoWs feed loads, APOD
  loads, filter switching works, background WorkManager triggers a refresh.

## Phase 5 — Hilt

Goal: introduce DI before feature work starts so feature modules slot in with
`@HiltViewModel` / `@AndroidEntryPoint` already wired.

- New convention plugin `asteroidradar.android.hilt` adds the hilt-gradle +
  ksp plugins, `enableAggregatingTask = true`, `hilt-android` impl, and
  `hilt-compiler` ksp.
- `AsteroidRadarApplication` gets `@HiltAndroidApp`.
- `MainActivity` + `MainFragment` + `DetailFragment` get `@AndroidEntryPoint`.
- `MainViewModel` becomes `@HiltViewModel` with constructor-injected
  `AsteroidRepository`. Its inner `Factory` class goes away.
- `RefreshDataWorker` becomes `@HiltWorker` with constructor-injected
  repository; the manual `getDatabase(context)` + repository construction
  goes away.
- The `getDatabase()` global singleton in `database/AsteroidDatabase.kt` gets
  replaced by a `@Provides @Singleton` in a Hilt `Module`.
- Hilt 2.51+ requires Kotlin 2.0+, which is why this phase comes after the
  toolchain bump.

## Phase 6 — Production hardening

Goal: a release build should fail fast on missing config, and ship a real
minified APK.

- `minifyEnabled true` + `shrinkResources true` for the release build type
  (currently `minifyEnabled false`).
- Slim starter `proguard-rules.pro` — `-keepattributes
  SourceFile,LineNumberTable` + `-renamesourcefileattribute SourceFile` so
  production crashes symbolicate via the AGP-emitted `mapping.txt`. AAR-shipped
  consumer rules from Hilt, Room, Retrofit, Moshi cover the rest. Adopters add
  reflective-keeps on demand. Use the [`r8-analyzer`](https://github.com/android/skills)
  Claude Code skill for keep-rule debugging.
- **Fail-fast on missing API key in release.** Currently a blank
  `NASA_API_KEY` in release results in a runtime 403 rather than a build
  failure. Add a check in the release build type's `buildConfigField` wiring.
- Lint baseline regenerated post-toolchain-bump (Phase 4 will likely surface
  new warnings).

## Phase 7 — Tests + Kover

Goal: replace the empty `Example*Test` shells with real coverage that doubles
as documentation for "how to test in this repo."

- **Kover plugin** in the convention plugin so every module is instrumented
  by default. Aggregate task: `./gradlew koverHtmlReport`. CI uploads the
  HTML + XML reports as artifacts (XML for any future Codecov/SonarCloud).
- **No coverage gate at first.** After 2–3 PRs of real tests, set a soft
  floor via `koverVerify` (start at 60%, ratchet up).
- **Real tests to write:**
  - `parseAsteroidsJsonResult` parser tests — feed it sample NeoWs payloads
    incl. edge cases (missing fields, empty date buckets).
  - `AsteroidDao` integration test using in-memory Room — `getAsteroids`,
    `getTodayAsteroids`, `getWeeklyAsteroids`, `deletePreviousAsteroid`.
  - `AsteroidRepository.getAsteroidSelection` filter mapping with a fake DAO.
  - `MainViewModel` state transitions with Turbine + MockK + Truth.
- Add bundle in `libs.versions.toml`: `test-shared` re-exports junit,
  truth, mockk, turbine, coroutines-test, androidx-test-ext, espresso-core.

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
