# Asteroid Radar

[![Android CI](https://github.com/Tarek-Bohdima/AsteroidRadar/actions/workflows/build_pull_request.yml/badge.svg)](https://github.com/Tarek-Bohdima/AsteroidRadar/actions/workflows/build_pull_request.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.6.21-blueviolet?logo=kotlin)](#)
[![minSdk](https://img.shields.io/badge/minSdk-26-brightgreen)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](#license)

Asteroid Radar tracks Near Earth Objects (NEOs) using NASA's
[Asteroids NeoWs](https://api.nasa.gov/) feed and shows the
[Astronomy Picture of the Day](https://api.nasa.gov/) on launch.
Asteroids close-approaching Earth in the next seven days are pulled in the
background once a day, cached locally, and filtered by today / week / all stored.
Available on Google Play (internal track).

## Features

- **NeoWs feed** — close-approaching asteroids for the next seven days, with
  potentially-hazardous flagging.
- **APOD** — NASA's Astronomy Picture of the Day on the main screen.
- **Offline-first** — Room is the source of truth; the UI reads from the DB,
  the network path only writes.
- **Daily background refresh** via WorkManager (constraints: unmetered network
  + charging + battery-not-low + device-idle).
- **Filters** — sealed `AsteroidsFilter` (`TODAY` / `WEEK` / `STORED`) maps to
  one DAO query each.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Fragments + Data Binding + Navigation Component (safe-args) |
| Async | Kotlin Coroutines |
| Networking | Retrofit + Moshi + OkHttp + a custom converter that picks scalar vs JSON per `@ScalarResponse` / `@JsonResponse` annotation |
| Persistence | Room (compiled with KSP) |
| Background | WorkManager (`PeriodicWorkRequest`, KEEP policy) |
| Image loading | Picasso |
| Logging | Timber |

Architecture is a standard offline-first repo pattern (`domain` / `network` /
`database` / `repository` / `ui` / `work`). See [`CLAUDE.md`](CLAUDE.md) for
the load-bearing details and module-by-module orientation.

## Build and run

Requires JDK 17 and the Android SDK. Common commands:

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # signed release APK (needs keystore env vars)
./gradlew bundleRelease          # signed AAB for Play Store
./gradlew test                   # all unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)
```

Single-test run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.tarek.asteroidradar.ExampleUnitTest"
```

## Configuration

`app/build.gradle` reads secrets from environment variables first, falling back
to `local.properties`. For local development add to `local.properties`:

```properties
NASA_API_KEY=your_nasa_api_key            # https://api.nasa.gov/ → "Generate API Key"
# release-signing only — needed for assembleRelease / bundleRelease
KEYSTORE_PATH=/absolute/path/to/keystore.jks
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

The keystore must be **PKCS12** — `signingConfigs.release.storeType` is hard-coded
to `"PKCS12"` in `app/build.gradle`.

## Release flow

Releases are tag-driven. Push a tag matching `v*` and
[`.github/workflows/release.yml`](.github/workflows/release.yml) takes over:

1. Validates that `vMAJOR.MINOR.PATCH` matches `versionMajor/Minor/Patch` in
   `app/build.gradle` — mismatch fails the workflow before building.
2. Runs unit tests, then builds and signs both the APK (attached to the GitHub
   Release for sideloading) and the AAB (uploaded as a workflow artifact for
   manual upload to Play Console).
3. Tags whose name contains `INTERNAL`, `alpha`, `beta`, `rc`, or `RC` are
   auto-flagged as pre-release.

Required GitHub Secrets: `NASA_API_KEY`, `KEYSTORE_BASE64` (the PKCS12 keystore
base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Tag/version conventions: SemVer with an optional classifier suffix that maps to
a Play Store track:

| Classifier | Play track |
|---|---|
| `-INTERNAL` | Internal testing |
| `-ALPHA` | Closed alpha |
| `-BETA` | Open beta |
| `-RC` | Production rollout candidate |
| (none) / `-RELEASE` | Production |

## Roadmap

The phased modernization plan lives in
[`docs/IMPROVEMENT_PLAN.md`](docs/IMPROVEMENT_PLAN.md) (Gradle Kotlin DSL +
version catalog → convention plugin → code-quality tooling → toolchain bump →
Hilt → R8 → tests + Kover → edge-to-edge → eventual Compose migration).

## License

Released under the MIT License — every source file ships with the full block in
its header. See any `.kt` or `.gradle` file (e.g. [`build.gradle`](build.gradle))
for the canonical text.
