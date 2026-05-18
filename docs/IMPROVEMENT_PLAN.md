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
| 12 | Persistent APOD + Coil disk cache | Done (#119) — Room-backed APOD with parallel refresh. Rides `v3.0.3-INTERNAL` together with #122 + #124. |
| — | DI cleanup — kill `AsteroidApi` service-locator | Done (#122) — Retrofit service constructor-injected via Hilt; closes the last manual provider. Rides `v3.0.3-INTERNAL`. |
| — | NDK debug symbols in release AAB | Done (#124) — closes issue #112 (Play Console "missing debug symbols" warning). Rides `v3.0.3-INTERNAL`. |
| — | APOD video-day handling | Done (#125) — Coil `VideoFrameDecoder` for `video/*` APOD entries + video-card fallback. Bumps to **`v3.0.4-INTERNAL`**. |
| 13a | Toolchain bump aligned to NIA (AGP 9 + Kotlin 2.3 + Hilt 2.59, drop kapt) | Done (#127) — folded 13c's kapt removal in via NIA's `ksp(kotlin-metadata)` pattern. Bumps to **`v4.0.0-INTERNAL`**. |
| 13b | AndroidX group bump | Done (#129) — lifecycle 2.10, nav 2.9.8, room 2.8.4, work 2.11.2, +compileSdk/targetSdk 36. Bumps to **`v4.0.1-INTERNAL`**. Closes issue #78. |
| 13c | Drop kapt for Hilt | Collapsed into 13a — the toolchain bump forced it (Hilt's metadata library version cap surfaced under Kotlin 2.3, and NIA's pattern was the documented fix). |
| 14a | `:benchmark` module + `StartupBenchmark` | Done (#132) — first second-module in the project. AndroidX Macrobenchmark library on AGP-9-compatible `androidx.baselineprofile 1.5.0-alpha06`. |
| 14b | `BaselineProfileGenerator` + checked-in `baseline-prof.txt` | Done (#133) — 20.7k-entry profile generated on Pixel 7 / API 33 emulator; bundles into AAB at `BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof`. Bumps to **`v4.0.2-INTERNAL`** (tag bundled with 14c). |
| 14c | CI workflow + GMD provisioning | Done (#134) — manual `workflow_dispatch` only; provisions Pixel 7 / API 34 GMD; uploads Perfetto traces as artifacts. Closes issue #131. |
| — | **Module split** lands with feature #2, not as a phase | — |

Tick the table when phases land. Each phase below lists scope, rationale, and
the rough size; sub-bullets are the concrete deltas.

## Current shipping state

Snapshot for whoever opens this repo next (likely future-you). Reflects the
state at 2026-05-18, after Phase 14 closed end-to-end (`v4.0.2-INTERNAL`
tagged 2026-05-08, bundling 14a/b/c into a single cut).

- **Live on Play Internal**: `v3.0.4-INTERNAL` — bundles Phase 12 + DI
  cleanup + NDK symbols + APOD video-day handling. User confirmed it
  behaves correctly on a Pixel 7 Pro 2026-05-08.
- **Version-of-record on `master`**: `v4.0.2-INTERNAL` — Phase 14
  (baseline profiles + macrobenchmark). Four consecutive smoke-skips this
  cycle (`v3.0.3`, `v4.0.0`, `v4.0.1`, `v4.0.2`) — all explicit one-offs,
  not new policy. The pre-tag protocol in the operating principles still
  says install on a real device before any future `v*` tag; if the
  pattern continues past `v5.0`, fold it into the principles section.
- **v3.x → v4.0 release timeline** (chronological):
  - `v3.0.0-INTERNAL` — Phase 9c Compose rewrite. **Broken on real devices**
    via release-only converter-factory regression. Never roll back to.
  - `v3.0.1-INTERNAL` — hotfix (#114). Non-local-return loop bug in the
    custom Retrofit factory + wired up `moshi-kotlin-codegen`. Verified.
  - `v3.0.2-INTERNAL` — Phase 11 (#117). Replaced Moshi with
    kotlinx.serialization end-to-end. Reflection-free runtime. Verified.
  - `v3.0.3-INTERNAL` — Phase 12 (#119) + DI cleanup (#122) + NDK
    symbols (#124). Tagged 2026-05-08 *retroactively* (workflow run
    `25571009314`); smoke skipped because v3.0.4 already covered the
    underlying changes on-device.
  - `v3.0.4-INTERNAL` — APOD video-day handling (#125). Live on Play
    Internal, on user's phone 2026-05-08.
  - `v4.0.0-INTERNAL` — Phase 13a (#127). AGP 9 + Kotlin 2.3 + Hilt 2.59
    + KSP-driven Hilt + drop kapt. Tagged without device smoke (one-off
    skip; workflow run `25575673678` succeeded in 6m39s).
  - `v4.0.1-INTERNAL` — Phase 13b (#129). AndroidX group bump (lifecycle
    2.10, nav 2.9.8, room 2.8.4, work 2.11.2, +compileSdk/targetSdk 36).
    Tagged without device smoke (workflow run `25576639557`). Closes
    issue #78 cleanly via `Closes #78`.
  - `v4.0.2-INTERNAL` — Phase 14 (#132 + #133 + #134). Bundled into one
    tag covering all three sub-PRs per user direction at 14b close.
    Tagged 2026-05-08 (workflow run `25583579445`); smoke skipped (the
    fourth in the streak — benchmark module + macrobenchmark workflow
    are dev-tooling-only, never ship to users).
- **Next pickup**: queue empty. Pickup is from the "Quality bets" parking
  lot (capped at 3) or fresh feature work.

### Issue close-keyword lesson (2026-05-08)

PR #127 used `Refs #78` to keep the umbrella issue open until the final
sub-PR landed, but GitHub auto-closed `#78` on merge anyway via
`closingIssuesReferences` resolving to `[78]` (cause unclear; possibly
the issue body's own `Closes #72, #73, #74` references getting parsed
when #127 cross-linked it). Issue had to be reopened manually.

PR #129 then used explicit `Closes #78` and the close worked cleanly as
intended. Lesson:
- For umbrella issues that should stay open until later sub-PRs land,
  use prose mentions ("Part of #N") rather than `Refs #N`. Avoid putting
  any `Fixes`/`Closes`/`Resolves` keyword anywhere — including within
  the issue's quoted body.
- For the final sub-PR that should close the umbrella, use explicit
  `Closes #N`.

## Retrospective — v3.x cycle (Phases 9–12, plus the v3.0.0 incident)

A snapshot of what the v3.x cycle taught us, captured here so Phase 13
inherits the lessons rather than re-learning them.

### What went well — keep doing

- **One scope per PR.** Phase 9 splitting into 9a/9b/9c, Phase 7 into
  a/b/c, Phase 12 standing alone — each PR was small enough to review in
  one sitting. Sub-PR sequencing held up under stress (the Phase 5 Hilt
  three-parter shipped cleanly).
- **Issues-first discipline.** Every non-trivial change opened an issue
  before the PR. The acceptance criteria + test-name format paid off: PRs
  arrived self-documenting and reviewers had concrete things to check.
- **Closing the category, not the symptom.** v3.0.0's release-only crash
  was hot-fixed in #114 by adding `moshi-kotlin-codegen` (band-aid). Phase
  11 then *deleted Moshi entirely*, removing the whole class of "reflection
  vs R8" failure. Durability beat speed. Apply the same instinct to Phase
  13 — fix the kapt root cause, don't keep the workaround alive.
- **Convention plugin paid off late.** Phase 2's `build-logic/` felt
  premature for a single-module app, but Phase 9 (Compose) and Phase 5
  (Hilt) plugged in as one-line plugin applications. Architecture
  investments compound.
- **DAGP (Phase 10) caught 8 unused deps in one shot.** Cheap gate, high
  signal — keep it green; don't baseline failures unless the cost of fixing
  in-place is genuinely prohibitive.
- **Memory + IMPROVEMENT_PLAN.md as a handoff system.** Multi-session
  continuity worked. The "Current shipping state" section is the
  load-bearing handoff — keep it terse, keep it current.

### What didn't go well — change

- **Debug-only smoke shipped a broken release.** `v3.0.0-INTERNAL` passed
  every check except the one that mattered: `assembleRelease + install`
  on a real device. Cost: a hotfix cycle. Fix: pre-tag protocol now
  requires a release-build install, not `installDebug`.
- **Doc and code drifted.** Phase 9c's documented deletion list said
  `kotlin-kapt` would be dropped. It wasn't — the JavaPoet leak forced it
  back for Hilt's compiler. The doc claimed "Done"; reality required a
  workaround. Lesson: a "Done" tick is necessary but not sufficient — every
  third phase, audit the doc against `git grep` reality.
- **Coordinated bumps were wasted as Dependabot PRs.** Three failed
  attempts (#72/#73/#74) before anyone realized they had to land
  together. Dependabot is great for isolated bumps and useless for
  mutually-blocking ones. Phase 13 reframes this as a single planned phase.
- **"Quality bets" parking lot grew stale.** Baseline profiles sat there
  through two release cycles. Lesson: cap the parking lot at 3 entries;
  promote/cull at every phase close.
- **Issue bodies decay.** #78 was written ~6 months ago against
  `v2.0.0-INTERNAL` and a Data-Binding-still-uses-kapt premise. Both are
  now obsolete. Lesson: rewrite, don't patch, when the rationale moves.

### Going forward — operating principles

These are the rules of engagement for Phase 13 and beyond.

1. **Pre-tag protocol**: `./gradlew assembleRelease` + `adb install` on a
   real device before pushing any `v*` tag. Debug builds do not count.
   *Skip-the-smoke decisions are explicitly one-offs and are recorded in
   the release timeline (see `v3.0.3-INTERNAL` and `v4.0.0-INTERNAL`).*
2. **Doc-drift audit at every phase close**: spot-check the deletion list
   in the just-closed phase against `git grep` to confirm reality matches
   the doc. Note discrepancies in the next phase's section.
3. **Coordinated bumps get a phase, not a PR.** Mutually-blocking
   toolchain moves (AGP + Kotlin + AndroidX) ride one issue with one
   sub-PR sequence. Dependabot is held off the cluster until the phase
   closes.
4. **Close the category, not the symptom.** When a fix is a workaround,
   write the follow-up issue at the same time as the band-aid PR.
5. **Quality bets parking lot is capped at 3.** New entries displace old
   ones; promotion to a phase or removal happens at every phase close.
6. **Consult Google's reference sample before negotiating scope** — for
   toolchain bumps and unfamiliar Gradle/AGP/Kotlin/Hilt errors, fetch
   the relevant files from
   [Now in Android](https://github.com/android/nowinandroid)
   (`gradle/libs.versions.toml` + the matching
   `build-logic/convention/src/main/kotlin/<...>ConventionPlugin.kt`)
   *before* proposing a narrowed scope. Phase 13a learned this the hard
   way: a working-but-suboptimal Kotlin-2.2-with-kapt landing was
   replaced by NIA's Kotlin-2.3-with-`ksp(kotlin-metadata)` pattern in a
   single redirect, folding 13c into 13a.

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
- **Drop `kotlin-kapt` plugin application — deferred (and re-deferred).**
  Originally deferred from Phase 4 because Data Binding's `@BindingAdapter`
  compiler ran through kapt. Phase 9c then removed Data Binding entirely —
  but kapt was *re-applied* for Hilt's compiler to dodge a JavaPoet 1.10
  `ClassName.canonicalName()` `NoSuchMethodError` raised by
  `hiltAggregateDeps*`. The current home for the kapt removal is **Phase 13**,
  which bumps Hilt + AGP + Kotlin together and validates that the JavaPoet
  leak is fixed before deleting the workaround.

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

  **Reality-check (post-merge).** The deletion list above is what Phase 9c
  *intended* to delete. `kotlin-kapt` was re-applied during the same PR for
  Hilt's compiler to work around a JavaPoet 1.10 `ClassName.canonicalName()`
  `NoSuchMethodError` from `hiltAggregateDeps*`. The convention plugin sets
  `enableAggregatingTask = false` to fall back to per-module annotation
  processing. Phase 13 closes this out properly.

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

## Phase 13 — Coordinated AGP + Kotlin + AndroidX bump (drop kapt)

Goal: catch the toolchain back up to a supported track in one coherent jump,
and finally delete the kapt workaround that's been carried since Phase 9c.
Tracked in issue #78 (rewritten 2026-05-08; the original body was written
against a `v2.0.0-INTERNAL` premise and a Data-Binding rationale that Phase
9c made obsolete). Bumps version-of-record to `v4.0.0-INTERNAL` (Kotlin major
under the SemVer table in `CLAUDE.md`).

This phase exists because three Dependabot PRs (#72 / #73 / #74) closed
unmerged: each bump blocked on one of the others. Treating it as a phase
rather than three parallel PRs is one of the operating principles from the
v3.x retrospective above.

### Sub-PR breakdown (post-13a-merge reality)

- **13a — Toolchain bump aligned to NIA (`feat/phase-13a-toolchain`,
  PR #127).** **Done.** Originally scoped as just AGP + Kotlin + KSP +
  coroutines. The session that landed it expanded scope to also (a) bump
  Gradle wrapper 8.13 → 9.5.0 (embedded Kotlin 2.3.20 was needed to read
  Kotlin 2.3 plugin metadata), (b) bump AGP all the way to 9.0.0 (NIA's
  pin; AGP 9 has built-in Kotlin support so the `kotlin.android` plugin
  was removed), (c) bump Hilt 2.52 → 2.59.2 (Palantir javapoet fork
  resolves the JavaPoet 1.10 leak), (d) wire Hilt's compiler through KSP
  with an explicit `ksp(libs.kotlin.metadata)` pin per
  [Now in Android's HiltConventionPlugin](https://github.com/android/nowinandroid/blob/main/build-logic/convention/src/main/kotlin/HiltConventionPlugin.kt)
  pattern, (e) bump DAGP 2.6.1 → 2.18.0 with a buildscript-classpath
  `kotlin-metadata-jvm` constraint (DAGP's `ExplodeJarTask` reads the
  same metadata), (f) drop deprecated `android.enableJetifier=true` and
  the now-default `android.nonTransitiveRClass=false`. Net effect: 13c's
  scope folded in here. Bumped to `v4.0.0-INTERNAL`.
- **13b — AndroidX group (`feat/phase-13b-androidx`, PR #129).** **Done.**
  Pins moved: `lifecycle 2.8.7` → `2.10.0`, `navigation 2.8.5` → `2.9.8`,
  `room 2.8.0` → `2.8.4`, `work 2.10.0` → `2.11.2`, `activity 1.10.1` →
  `1.13.0`, `appcompat 1.7.0` → `1.7.1`, `constraintlayout 2.2.0` →
  `2.2.1`, `hilt-work / hilt-navigation-compose / hilt-compiler 1.2.0` →
  `1.3.0`. **Forced toolchain bumps**: `compileSdk` + `targetSdk` 35 →
  36 (lifecycle 2.10 / nav 2.9 floor; AGP flagged at configure time;
  `minSdk` stays at 26 — no breaking-for-users bump). One source change:
  `MainScreen.kt` import migration for the relocated `hiltViewModel()`
  in androidx.hilt 1.3.0 (`androidx.hilt.navigation.compose.hiltViewModel`
  → `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`). Compose
  BOM intentionally **not** bumped (still `2025.01.01`) — Compose surface
  needs its own validation phase separate from the AndroidX-core group.
  Bumped to `v4.0.1-INTERNAL`.
- **13c — Drop kapt.** **Collapsed into 13a.** Originally scoped as a
  follow-up that would only ship if a release build proved Hilt's
  `enableAggregatingTask = true` no longer raised the JavaPoet error.
  Reality: under Kotlin 2.3, Hilt's bundled `kotlin-metadata-jvm`
  rejected 2.3-emitted bytecode (`Provided Metadata instance has version
  2.3.0, while maximum supported version is 2.2.0`). NIA's documented
  fix — drop kapt, use `ksp(hilt-compiler) + ksp(kotlin-metadata)` —
  was the only way through. So 13a did 13c's work too, and the kapt
  workaround disappeared in the same PR that introduced its motivation.

### Things we hit during 13b (added 2026-05-08)

- **AndroidX 2.10 / 2.9 require `compileSdk 36`.** AGP raises a
  configure-time error if `compileSdk` is below the floor of any
  declared dependency. `targetSdk` was bumped to 36 alongside
  `compileSdk` for consistency; `minSdk` stays at 26 (no
  breaking-for-users bump per the SemVer table in `CLAUDE.md`).
- **`hiltViewModel()` relocated in androidx.hilt 1.3.0.** The function
  moved from `androidx.hilt.navigation.compose` to
  `androidx.hilt.lifecycle.viewmodel.compose`. Old import still
  compiles via `@Deprecated`, but lint emits a warning until updated.
  Single-line fix in `MainScreen.kt`.
- **NIA's pins are sometimes more conservative than the latest stable.**
  NIA stays on `activity 1.9.3` and `navigation 2.8.5`; we ended up
  ahead at `1.13.0` and `2.9.8` respectively. The NIA-consultation
  rule (operating principle 6) is for *patterns and architecture*, not
  for *version pins* — for pins, latest stable on Maven Central is
  fine when there's a clear motivation to bump.

### Things we hit during 13a (kept here so 13b doesn't relearn them)

- **Embedded Kotlin in Gradle 8.x can't read Kotlin 2.3 plugin metadata.**
  The build-logic module's `kotlin-dsl` uses Gradle's *embedded* Kotlin
  compiler (locked to 2.0.21 in Gradle 8.13). Bumping Kotlin to 2.3
  required Gradle 9.5.0 (embedded Kotlin 2.3.20). The wrapper bump is
  not optional — `kotlin-dsl` precompiled scripts can't be opted out of
  embedded-Kotlin compilation in a one-line override.
- **AGP 9.0 has built-in Kotlin support.** The `org.jetbrains.kotlin.android`
  plugin must be removed (applying it errors out with a "no longer
  required" message). Removed at root `apply false` *and* in the
  `asteroidradar.android.application` convention plugin's `plugins {}` block.
- **kapt is gone — but the kotlin-metadata pin is doing the same work.**
  The Hilt processor needs to read Kotlin metadata from the project's
  bytecode. Hilt's bundled `kotlin-metadata-jvm` is a minor or two behind
  Kotlin's release cycle. The `ksp(libs.kotlin.metadata)` declaration
  in the Hilt convention plugin overrides it on the KSP processor
  classpath; without it, switching kapt → KSP would just relocate the
  failure rather than fix it. Same trick is needed for DAGP via the root
  `buildscript { dependencies { classpath(libs.kotlin.metadata) } }`.
- **dagger#3965 (Hilt + KSP classloader split)** was a red herring on
  Hilt 2.52 + Gradle 9.x. Bumping Hilt to 2.59.2 made the error vanish
  entirely. Investigation cost ~30 minutes; the lesson is to always
  match Hilt's pin to NIA's before debugging plugin-classloader symptoms.
- **DAGP officially supports up to AGP 8.7** (per its own warning).
  DAGP 2.18.0 actually works on AGP 9.0, but it produces a "Proceed at
  your own risk" message. Watch for off-by-one severity on transitive
  dep flagging; if a DAGP miscall surfaces, the fallback is to disable
  the `buildHealth` gate temporarily until DAGP catches up.

### Operating-principle reinforcement (2026-05-08)

Phase 13a confirmed two operating principles from the v3.x retrospective:

1. **"Close the category, not the symptom"** — narrow 13a (Kotlin 2.2 +
   kapt) was a working solution for the metadata-cap symptom. The user
   redirected to NIA, which closed the *category* (kapt-driven Hilt) in
   one PR. Result: 13c collapses, the JavaPoet workaround comment
   disappears, and future Kotlin bumps don't re-relearn the same lesson.
2. **"Consult Google's reference samples before negotiating scope."**
   New durable rule, codified as `feedback_consult_google_samples` in
   memory: when bumping toolchain or hitting Gradle/AGP/Kotlin classloader
   or metadata errors, fetch NIA's `libs.versions.toml` + relevant
   convention plugin *before* proposing scope tradeoffs. NIA's pattern
   often is the documented fix.

## Phase 14 — Baseline profiles + macrobenchmark

Goal: measurable cold-start performance, not "feels fast in dev." Graduated
from the "Quality bets" parking lot — sat there through two release cycles
and is now timely (Phase 5 added the Hilt DI graph at startup, Phase 12
added the Room read for the cached APOD; both affect cold-start budget).

- New `:benchmark` module (the project's first second-module) with the
  AndroidX Macrobenchmark library. Compiles to a separate AAB so the
  benchmark instrumentation never ships to users.
- `StartupBenchmark` measuring cold-start time-to-first-frame across N
  iterations. Captures TraceData for offline review.
- `BaselineProfileGenerator` driving the four golden-path flows
  (NeoWs feed loads, APOD renders, navigate-to-detail, force-stop +
  relaunch from cache). Output `baseline-prof.txt` checked in under
  `app/src/main/`; AGP 8.x bundles it into the AAB automatically.
- CI: a separate workflow runs the macrobenchmark on a managed Gradle
  device (AGP's built-in AVD provisioning) and uploads the trace as an
  artifact. *Not* gating; perf signal is informational until we have a
  baseline to ratchet against.
- **Out of scope**: Macrobenchmark for non-startup flows, Firebase Test
  Lab integration, perf regression budgets in CI. Those land if the
  baseline profile shows real cold-start headroom worth defending.

### Why now

- Phase 5 graph + Phase 12 Room read together push the cold-start budget
  far enough that "feels fast" doesn't cut it anymore.
- Compose adoption (Phase 9) is exactly the surface baseline profiles were
  designed for — the AOT-compiled Compose Compose runtime hot path is a
  significant cold-start win.
- Cheap to write, expensive to retrofit later. If we're going to do it,
  doing it before adding a second feature (where the macrobenchmark module
  pattern locks in) is the right time.

## Quality bets to consider (capped at 3)

Capped per the v3.x retrospective. Promote to a phase or cull at every
phase close.

- **Gradle build scans** if a Develocity instance becomes available.
- **Jacoco merge** — alternative coverage backend if Kover blocks on a
  future Kotlin bump.
- **Phase 11b** — migrate `parseAsteroidsJsonResult` (the
  `org.json.JSONObject`-based NeoWs parser) onto a
  kotlinx.serialization `JsonTransformingSerializer`. Doable, small
  surface, low priority.

## How to use this document

When starting a phase, change its row in the status table to **In progress**,
link the issue/PR, and update sub-bullets with checkboxes if it helps. When
merged, mark **Done** with the PR number. The point is to keep the next-best
action obvious to whoever opens the repo a month from now (often: future-you).
