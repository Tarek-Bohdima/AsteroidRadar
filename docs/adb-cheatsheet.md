# adb cheatsheet

The commands used during AsteroidRadar on-device smokes, in roughly the
order they come up in a session, plus a handful of useful extras that
didn't fit a specific PR. Reach for this when verifying a release build
on a real device or driving the AVD for a phase-close smoke.

Replace `com.tarek.asteroidradar` with the package you're targeting if
copying into another project.

---

## Device discovery

```bash
adb devices
```

Lists attached devices/emulators with their state (`device`, `offline`,
`unauthorized`). First command in any session — confirms the host can
see the target.

```bash
adb shell getprop ro.build.version.release   # → "13" = Android 13
adb shell getprop ro.product.model           # → "sdk_gphone64_x86_64"
```

Reads system properties. Use when you need to confirm API level or
device model (helpful when a session might be running against a
different device than expected).

---

## App lifecycle

```bash
adb shell am force-stop com.tarek.asteroidradar
```

Force-stops the app, same as System Settings → Force Stop. Run before a
cold-launch test so you're not warm-launching into an existing process.

```bash
adb shell monkey -p com.tarek.asteroidradar \
  -c android.intent.category.LAUNCHER 1
```

Launches the app via its launcher `Intent` without needing the exact
`Activity` class name. `-p` scopes the monkey to one package,
`-c android.intent.category.LAUNCHER` targets the launcher activity,
trailing `1` is the event count. Cleaner than
`am start -n pkg/.Activity` because you don't have to remember the
activity FQN.

---

## Logcat (the workhorse)

```bash
adb logcat -c
```

Clears the device-side log buffer. Run before launching/triggering
anything you want to capture cleanly — otherwise you wade through
buffered noise.

```bash
adb logcat -d -s "App:*" "Network:*" "Work:*"
```

- `-d` dumps the current buffer and exits (vs. streaming forever).
- `-s` filters by tag-priority pairs. `Tag:*` = any priority for that
  tag. Other suffixes: `Tag:V` / `Tag:D` / `Tag:I` / `Tag:W` / `Tag:E`
  to set a minimum priority.
- **zsh gotcha**: quote each `Tag:*` arg or zsh glob-expands the `*` and
  you get `no matches found`.

```bash
adb logcat -d 2>&1 | grep -iE "okhttp|retrofit|com\.tarek\.asteroidradar"
```

Falls back to full-buffer dump + grep when you don't yet know the right
tag. `-iE` = case-insensitive extended regex. Use this when something's
broken and you're hunting.

```bash
adb logcat -v threadtime
```

Streams in the threadtime format (timestamp + pid + tid + priority + tag
+ message). Useful for correlating multi-thread interleaving.

```bash
adb logcat *:S MyTag:V
```

Silences everything (`*:S`) and re-enables one tag at Verbose. Inverse
of `-s` filtering — handy when you want only your tag in a noisy boot
sequence.

---

## Network control (emulator)

```bash
adb shell svc wifi disable
adb shell svc wifi enable
adb shell svc data disable
adb shell svc data enable
```

Toggles WiFi and mobile-data radios. Useful for exercising
offline-failure code paths without poking the emulator UI.
**Remember to re-enable both at end of session** — they persist across
emulator restarts.

```bash
adb shell settings put global airplane_mode_on 1 \
  && adb shell am broadcast -a android.intent.action.AIRPLANE_MODE
```

Broader than `svc wifi/data disable`: also kills Bluetooth and is
indistinguishable from a real airplane-mode toggle for code that
inspects `ConnectivityManager`.

```bash
adb shell ping -c 2 -W 2 8.8.8.8
```

Sanity check from the device side — confirms the emulator itself has
connectivity, not just the host laptop. `-c 2` = 2 packets,
`-W 2` = 2-second per-packet timeout. Emulator ping output can be noisy
due to clock drift; trust the packet-loss-percentage line, not the
per-packet "wrong data byte" warnings.

---

## WorkManager / JobScheduler

```bash
adb shell dumpsys jobscheduler | grep -A 1 "com.tarek.asteroidradar"
```

Dumps the JobScheduler view of pending jobs. WorkManager registers each
unique periodic work as a JobScheduler job — this is how you find the
**JOB #** (e.g. `#u0a174/1` → job ID `1` for that UID) to force-run.
Without this lookup the `run -f` command can't address the right job.

```bash
adb shell cmd jobscheduler run -f com.tarek.asteroidradar 1
```

Force-runs JobScheduler job ID `1` for that package, **bypassing all
the WorkManager constraints** (charging / unmetered network /
battery-not-low / device-idle). The `-f` flag is what waives the
constraints — without it the scheduler refuses if any aren't met. The
last argument is the JobScheduler job ID from the `dumpsys` step above
(not the WorkManager work ID — those are different).

**Gotcha**: WorkManager throttles periodic jobs to one execution per
interval. After you force-run once, a second `run -f` returns
`Delaying execution... before schedule`. To re-trigger you'd need to
wait the periodic interval, cancel + re-enqueue, or use WorkManager's
`TestDriver` from an instrumented test.

---

## Install / deploy

```bash
./gradlew :app:installDebug
```

Not `adb` but wraps `adb install -r <apk>` after a Gradle build. Use
this rather than `adb install` directly so the build stays in sync with
your latest edits.

---

## Reset state

```bash
adb shell pm clear com.tarek.asteroidradar
```

Wipes app data + cache. Next launch is first-launch fresh — useful for
verifying onboarding or migration flows.

```bash
adb shell input keyevent KEYCODE_HOME
```

Programmatic home-button press. Useful for "cold launch from home"
tests where you want the OS to redraw the home screen before relaunch.

---

## Cleanup

```bash
adb emu kill
```

Shuts down the connected emulator cleanly (same as closing the AVD
window). Only works on emulators — for physical devices use unplug or
`adb disconnect <serial>`.

---

## Worked example — Phase 15a smoke (2026-05-18)

The full sequence used to verify the typed-event logging in PR #152.
Reproducible recipe for any future phase that adds a new domain of
events:

```bash
# 1. Confirm emulator is up.
adb devices

# 2. Build + install fresh debug APK.
./gradlew :app:installDebug

# 3. Cold-launch with clean logcat → expect App.Launched.
adb logcat -c
adb shell am force-stop com.tarek.asteroidradar
adb shell monkey -p com.tarek.asteroidradar \
  -c android.intent.category.LAUNCHER 1
sleep 6
adb logcat -d -s "App:*" "Network:*" "Work:*"

# 4. Force failure path: kill network and relaunch.
adb shell svc wifi disable
adb shell svc data disable
adb logcat -c
adb shell am force-stop com.tarek.asteroidradar
adb shell monkey -p com.tarek.asteroidradar \
  -c android.intent.category.LAUNCHER 1
sleep 8
adb logcat -d -s "Network:*"

# 5. Re-enable network for the worker run.
adb shell svc wifi enable
adb shell svc data enable

# 6. Trigger the periodic worker bypassing constraints.
adb shell dumpsys jobscheduler | grep -A 1 com.tarek.asteroidradar
# Note the JOB # — e.g. #u0a174/1 → job ID 1.
adb logcat -c
adb shell cmd jobscheduler run -f com.tarek.asteroidradar 1
sleep 15
adb logcat -d -s "Work:*"

# 7. Done.
adb emu kill
```
