# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

Asteroid Radar is a Play-Store-published Android app (internal track) tracking NASA Near Earth Objects. Single-module, view-system UI (Fragments + Data Binding + Navigation), offline-first via Room, daily background refresh via WorkManager. Default package is `com.tarek.asteroidradar`.

**Roadmap and ongoing improvements** live in [`docs/IMPROVEMENT_PLAN.md`](docs/IMPROVEMENT_PLAN.md) — phased modernization (Gradle Kotlin DSL → convention plugin → quality tooling → toolchain bump → Hilt → R8 → tests + Kover). Check it before starting non-trivial work; PRs should reference the relevant phase.

## Build, test, run

Single-module Android Gradle project. JDK 17, Kotlin 1.6.21, AGP 8.3.0, `compileSdk`/`targetSdk` 35, `minSdk` 26.

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # signed release APK (needs keystore env vars, see below)
./gradlew bundleRelease          # signed AAB for Play Store
./gradlew test                   # all unit tests (JVM)
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)

# Run a single unit test class or method
./gradlew :app:testDebugUnitTest --tests "com.tarek.asteroidradar.ExampleUnitTest"
./gradlew :app:testDebugUnitTest --tests "com.tarek.asteroidradar.ExampleUnitTest.someMethod"
```

There is no separate lint step in CI; use `./gradlew lint` locally if needed.

## Required configuration

`app/build.gradle` reads these from env vars first, then falls back to `local.properties`:

- `NASA_API_KEY` — required for both debug and release; injected as `BuildConfig.NASA_API_KEY`.
- `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — required only when running `assembleRelease` / `bundleRelease`. The keystore must be PKCS12 (`storeType "PKCS12"` is hard-coded in `signingConfigs.release`).

`local.properties` is gitignored; do not commit secrets there to git.

## Versioning and tags

Version is computed in `app/build.gradle` from four fields at the top:

```
versionMajor / versionMinor / versionPatch / versionClassifier
versionCode = minSdk*1_000_000 + major*10_000 + minor*100 + patch
versionName = "<major>.<minor>.<patch>-<classifier>"
```

Bump these — not `versionCode`/`versionName` directly — when cutting a release.

SemVer with this app-shipped lens:

| Bump | When |
|---|---|
| **MAJOR** (`vX.0.0`) | Breaking for users — `minSdk` bump, complete UI rewrite, removed feature, or a toolchain migration that forces every fork to follow (Kotlin major, AGP major, etc.) |
| **MINOR** (`v1.X.0`) | New user-visible feature (a new filter, a new screen, new data source) |
| **PATCH** (`v1.0.X`) | Bug fixes, dep bumps, internal refactor — most Phase 0–3 PRs land as PATCH |

The classifier suffix maps cleanly to Play tracks:

| Classifier | Play track |
|---|---|
| `-INTERNAL` | Internal testing |
| `-ALPHA` | Closed alpha |
| `-BETA` | Open beta |
| `-RC` | Production rollout candidate |
| (none) / `-RELEASE` | Production |

**Conventions:**

- Bump `versionMajor/Minor/Patch` in `app/build.gradle.kts` as part of the PR that earns the bump, so the version-of-record on `master` always reflects what's there. Doc/CI/dep-bump-only PRs skip the bump.
- Cut a tag only when actually shipping a build — phases can stack into a single tag. e.g. Phases 4 + 5 + 6 ship as one `v2.0.0-INTERNAL` once the toolchain + DI + R8 rewrite is on a device, not three separate cuts. The version field tracks the work; the tag tracks shipments.
- When we tag, the release workflow runs: signed APK + AAB build, GitHub Release with auto-generated notes, AAB attached as a workflow artifact (Play Store upload stays manual).
- Tag names containing `INTERNAL`, `alpha`, `beta`, `rc`, or `RC` auto-flag as pre-release.

`.github/workflows/release.yml` runs on tags matching `v*`:

1. **Validates** that `vMAJOR.MINOR.PATCH` (classifier ignored) matches `versionMajor/Minor/Patch` in `app/build.gradle.kts`. Mismatch fails the workflow before building. Always update `build.gradle.kts` and commit before tagging.
2. Runs unit tests, decodes `KEYSTORE_BASE64` to a temp `.jks`, builds signed APK + AAB.
3. Attaches the APK to the GitHub Release; uploads the AAB as a workflow artifact (Play Store uploads are manual).

Required GitHub secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `NASA_API_KEY`.

PRs targeting `master` run `.github/workflows/build_pull_request.yml` (`assembleDebug` + `test`). Both workflows pin third-party Actions to commit SHAs (with the version tag in a trailing comment); Dependabot's `github-actions` ecosystem auto-bumps them weekly.

## Branching, commits, issues

- **Branches** — `feat/<slug>`, `fix/<slug>`, `chore/<slug>`, `docs/<slug>`, `ci/<slug>`, `build/<slug>`, `refactor/<slug>`, `test/<slug>`, `perf/<slug>`. Match the Conventional Commits prefixes.
- **Commits** — [Conventional Commits](https://www.conventionalcommits.org/) prefixes (`feat:`, `fix:`, `chore:`, `docs:`, `ci:`, `build:`, `test:`, `refactor:`, `perf:`).
- **Issues first** — every non-trivial change opens a GitHub issue first. The PR body uses `Fixes #N` (auto-closes the issue on merge) or `Refs #N` (cross-links without closing). Doc/typo/dep-bump PRs are exempt.
- **One scope per PR.** Phase 0 ships as three small PRs, not one mega-diff. Same pattern through the roadmap.
- **Merge** — squash-merge by default; delete the branch on merge so the active-branches list stays scannable.

## Recommended Claude Code skills

Google maintains official AI-optimized skills for Android at <https://github.com/android/skills>. Skills relevant to this project's roadmap:

| Skill | Use when |
|---|---|
| [`r8-analyzer`](https://github.com/android/skills) | Phase 6 (R8 enablement, ProGuard keep rules) |
| [`edge-to-edge`](https://github.com/android/skills) | Phase 8 (edge-to-edge adoption) |
| [`jetpack-compose`](https://github.com/android/skills) | Phase 9 (Compose migration), if/when it's pulled forward |
| [`agp-9-upgrade`](https://github.com/android/skills) | When AGP 9 lands as a dedicated migration |
| [`navigation`](https://github.com/android/skills) | If revisiting Navigation safe-args setup |

Skills are not vendored — install locally when starting the matching phase.

## Architecture

Standard offline-first Android architecture inside one package `com.tarek.asteroidradar`:

```
domain/      — plain Kotlin models exposed to UI (Asteroid, PictureOfDay)
network/     — Retrofit service, DTOs, Moshi setup, JSON parsing helpers
database/    — Room entities, DAO, AsteroidDatabase singleton via getDatabase()
repository/  — AsteroidRepository: single source of truth, mediates network ↔ DB
work/        — RefreshDataWorker (CoroutineWorker) for background refresh
ui/main/     — MainFragment + MainViewModel + AsteroidAdapter
ui/detail/   — DetailFragment
util/        — BindingAdapters, Constants
AsteroidRadarApplication.kt, MainActivity.kt
```

Key flows worth knowing before editing:

- **Data flow.** UI observes `LiveData` from `AsteroidRepository.getAsteroidSelection(filter)`, which reads from Room via `AsteroidDao`. Network refresh is a separate write-only path (`refreshAsteroids()` → DAO `insertAll`). The repo never returns network data directly; the DB is the source of truth. `AsteroidsFilter` is a sealed class (`TODAY` / `WEEK` / `STORED`) and the DAO has one query per case — when adding a filter, add a DAO query, a sealed subclass, and a branch in `getAsteroidSelection`.
- **Two API endpoints, two response shapes.** `neo/rest/v1/feed` returns a JSON string parsed manually (`parseAsteroidsJsonResult` in `network/NetworkUtils.kt`) because of its nested-by-date structure; `planetary/apod` is decoded by Moshi. Both go through one Retrofit instance using a custom `HandleScalarAndJsonConverterFactory` in `network/service.kt` that picks the converter from `@ScalarResponse` / `@JsonResponse` annotations on each service method. Adding a new endpoint means tagging it with one of those annotations.
- **Background refresh.** `AsteroidRadarApplication.onCreate()` enqueues a unique periodic `RefreshDataWorker` (1 day, `ExistingPeriodicWorkPolicy.KEEP`) gated on unmetered network + charging + battery-not-low + device-idle. The worker calls `deletePastAsteroids()` then `refreshAsteroids()`. `MainViewModel.init` also triggers a refresh on launch — keep both paths idempotent.
- **DB singleton.** `getDatabase()` in `database/AsteroidDatabase.kt` uses a `lateinit` global plus `synchronized` block. There is no DI framework; `MainViewModel` and `RefreshDataWorker` both call `getDatabase(context)` directly. `MainViewModel` is constructed via its inner `Factory` in `MainFragment`.
- **Data Binding + custom adapters.** `buildFeatures.dataBinding = true`. Layouts call into `util/BindingAdapters.kt` (e.g. image loading via Coil's `imageView.load(...)`, asteroid status icons). When changing UI bindings, check there first before adding new logic in fragments.
- **KSP, not kapt, for Room.** Room compiler runs through KSP (`com.google.devtools.ksp`); generated sources are added to `main` via `sourceSets { main { java { srcDir 'build/generated/ksp/src/main/kotlin' } } }`. `kotlin-kapt` is still applied but isn't currently used by any processor.
- **Navigation safe-args** is enabled (`androidx.navigation.safeargs.kotlin`) — pass arguments between `MainFragment` and `DetailFragment` through the generated `*Directions` / `*Args` classes rather than raw bundles.
