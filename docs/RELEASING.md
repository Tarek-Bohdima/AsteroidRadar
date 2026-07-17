# Releasing Asteroid Radar

How a signed build gets cut and shipped. **Releases are tag-driven:** you push a
`v*` tag on `master`, GitHub Actions builds and *signs* the artifacts, and a
GitHub Release is created automatically. The one manual step is uploading the AAB
to the Play Console.

This is the operational runbook. The *policy* behind version numbers and tracks
(the SemVer table, classifier → track mapping) lives in
[`../CLAUDE.md`](../CLAUDE.md) under "Versioning and tags" — read that first if
you're deciding *which* number to bump. This file is the *how*.

---

## TL;DR

```bash
# 1. Bump the version fields in app/build.gradle.kts (see step 1) and commit on master.
# 2. Tag and push:
git tag v4.2.0-INTERNAL
git push origin v4.2.0-INTERNAL
# 3. Watch the run, then upload the AAB from the GitHub Release to Play Console.
```

That's the whole thing. Everything below is detail.

---

## What is and isn't automated

| Step | Who does it |
|---|---|
| Version validation (tag vs `build.gradle.kts`) | **CI** (fails fast on mismatch) |
| Unit tests | **CI** |
| Keystore decode + **APK/AAB signing** | **CI** — Gradle signs using the keystore secrets |
| `mapping.txt` (R8 deobfuscation) generation | **CI** |
| GitHub Release + changelog + artifact attach | **CI** |
| **Upload AAB to Play Console → Internal testing** | **You, manually** — drag-and-drop the AAB |

So: **signing is not manual.** You never touch the keystore during a release —
the workflow decodes `KEYSTORE_BASE64` and Gradle's `signingConfigs.release`
handles it. The only manual action is the Play upload, which is intentionally
kept out of CI.

---

## Tag naming — the rules that actually matter

The Release workflow (`.github/workflows/release.yml`) triggers on **`push` of a
tag matching `v*`** and nothing else. That gives three hard rules:

1. **The tag MUST start with `v`.** `v4.2.0-INTERNAL` triggers a release;
   `internal-4.2.0` or `4.2.0` do **not** — CI never fires and no build happens.
2. **The internal-testing designation is a *suffix*, not a prefix.** It's the
   `-INTERNAL` classifier. Full form: `vMAJOR.MINOR.PATCH-INTERNAL`.
3. **Versions do not restart.** The version-of-record on `master` is already past
   `v1.0.0` (currently `4.1.0`). The next internal release continues from there —
   e.g. `v4.2.0-INTERNAL` — it is never `v1.0.0`.

The classifier maps to a Play track (see CLAUDE.md for the full table):

| Tag suffix | Play track | Auto-flagged pre-release on GitHub? |
|---|---|---|
| `-INTERNAL` | Internal testing | Yes |
| `-ALPHA` | Closed alpha | Yes (`alpha`) |
| `-BETA` | Open beta | Yes (`beta`) |
| `-RC` / `-rc` | Production rollout candidate | Yes |
| (none) / `-RELEASE` | Production | No |

The GitHub Release is marked **pre-release** automatically when the tag contains
`INTERNAL`, `alpha`, `beta`, `rc`, or `RC`.

---

## Step-by-step: cutting an internal release

### 1. Bump the version in `app/build.gradle.kts`

Edit the four fields near the top of `app/build.gradle.kts` — **never** edit
`versionCode` / `versionName` directly; they're computed:

```kotlin
val versionMajor = 4
val versionMinor = 2          // ← bump the field that earns the bump
val versionPatch = 0
val versionClassifier = "INTERNAL"
```

`versionCode` and `versionName` are derived:

```
versionCode = versionCodeMinSdk*1_000_000 + major*10_000 + minor*100 + patch
versionName = "<major>.<minor>.<patch>-<classifier>"
```

Which field to bump is a SemVer decision — see the table in CLAUDE.md. Commit
this on `master` (via a normal PR) **before** tagging:

```bash
git commit -am "build: bump version to 4.2.0-INTERNAL"
```

> **The tag's numeric part must equal `major.minor.patch` in `build.gradle.kts`.**
> CI's first step re-derives `MAJOR.MINOR.PATCH` from the file and compares it to
> the tag with the `v` and classifier stripped (`v4.2.0-INTERNAL` → `4.2.0`). A
> mismatch fails the workflow before anything is built. The classifier itself is
> *not* validated — it's just a label.

### 2. Tag and push

Tags can stack multiple phases into one shipment — cut a tag only when you're
actually shipping a build, not once per merged PR.

```bash
git tag v4.2.0-INTERNAL
git push origin v4.2.0-INTERNAL
```

### 3. Watch the run

```bash
gh run watch      # or: gh run list --workflow=release.yml
```

The workflow runs, in order: validate version → `./gradlew test` → decode
keystore + `google-services.json` → `assembleRelease` → `bundleRelease` → locate
artifacts → generate changelog → create the GitHub Release.

### 4. Grab the artifacts

On success a GitHub Release named after the tag is published with three files:

| File | Use |
|---|---|
| `*.aab` | **Upload this to Play Console.** Required bundle format for Play. |
| `*.apk` | Sideload smoke build — `adb install` it directly, no Play needed. |
| `mapping.txt` | R8 symbol map for local `retrace`. Play gets its own copy from inside the AAB automatically. |

### 5. Upload to Play (manual)

Play Console → Asteroid Radar → **Testing → Internal testing → Create new
release** → drop in the `*.aab` → roll out. This step is deliberately not
automated.

---

## Required GitHub secrets

Set under **Settings → Secrets and variables → Actions**. The release fails
without them:

| Secret | What it is |
|---|---|
| `KEYSTORE_BASE64` | base64 of the PKCS12 keystore — `base64 -i keystore.jks \| tr -d '\n'` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | signing key alias |
| `KEY_PASSWORD` | signing key password |
| `NASA_API_KEY` | NASA API key baked into the release build |
| `GOOGLE_SERVICES_JSON_BASE64` | base64 of `app/google-services.json` (Firebase Crashlytics) |

For local `assembleRelease` / `bundleRelease`, the same signing inputs come from
env vars (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) or
`local.properties` — see CLAUDE.md → "Required configuration".

---

## Troubleshooting

- **"Version mismatch" failure at step 1 of the workflow.** The tag's
  `major.minor.patch` doesn't match `app/build.gradle.kts`. Fix the fields on
  `master`, commit, delete the bad tag, and re-tag:
  ```bash
  git tag -d v4.2.0-INTERNAL
  git push origin :refs/tags/v4.2.0-INTERNAL   # delete remote tag
  # fix build.gradle.kts, commit, then re-tag and push
  ```
- **Workflow never started.** The tag didn't match `v*` (e.g. `internal-4.2.0`).
  Delete it and tag with a leading `v`.
- **"App not signed" / signing errors in CI.** A signing secret is missing or the
  keystore isn't PKCS12 (`storeType "PKCS12"` is hard-coded). Re-check the six
  secrets above.
- **Play rejects the AAB (`versionCode` already used).** You re-tagged the same
  version. Bump `versionPatch` (or higher), commit, and cut a new tag —
  `versionCode` advances automatically from the formula.
- **"No debug symbols" warning in Play Console.** Expected and harmless — AndroidX
  ships stripped `.so` files. Not a release blocker.
