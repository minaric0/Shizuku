# SM-A175N / One UI 9 / Android 17 (API 37)

Target: Samsung SM-A175N (Galaxy A17), Android 17 and One UI 9.
This is a compatibility-focused fork, not a root or system-UID escalation tool.

## Changes

- Compile and target SDK 37.
- Request Android 17 `ACCESS_LOCAL_NETWORK` at the moment wireless startup or pairing is selected.
- Declare normal `USE_LOOPBACK_INTERFACE` for local ADB.
- Detect wireless-debugging ports through mDNS rather than `Settings.Global.ADB_ENABLED`, which can report an artificial zero.
- Keep the original Shizuku binder protocol and require the original TLS pairing / user authorization.
- Use the actual local-interface mDNS host instead of hard-coded loopback (required when Android 17 adbd binds to WLAN).\n- Bound ADB connect/read operations to avoid indefinitely hung UI and boot startup.
- Do not attempt wireless startup from a boot receiver before local-network access is granted.
- Debug APK is built in GitHub Actions; no signing secrets required.

## How to install / validate

1. Run the `SM-A175N Android 17 debug APK` GitHub Actions workflow and download its artifact.
2. Back up Shizuku authorizations before replacing an installed instance. Different signing keys cannot update an existing installed application. Uninstall the old app first if necessary.
3. Enable Developer options > Wireless debugging; grant Nearby devices to the fork.
4. Pair the app using the OS wireless-debugging pairing code, and tap Start.
5. Check server UID 2000 (ADB shell), client grant/revoke, and return from background.
6. Restart and verify behavior. Wireless debugging may be reset by the OS, so startup after reboot is best-effort, **not guaranteed**.
7. For device-specific reports include `adb shell getprop ro.build.fingerprint`, `adb shell getprop ro.build.version.sdk`, and a **redacted** app log. Do not share pairing codes, keys or private logs.

## Boundaries

- Root remains required for UID 0; ADB mode runs with shell privilege only.
- One UI battery restrictions and Samsung debugging policies are not bypassed.
- A debug-signed APK does not replace an official signed release, and publishing a derivative requires adherence to upstream branding and license restrictions.
