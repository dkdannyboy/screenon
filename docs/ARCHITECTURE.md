# Architecture and Android constraints

## Scope

Kotlin + Jetpack Compose, Android 8.0/API 26 and newer; compile/target API 36. The app has no backend, network permission, trackers, advertising SDK, accessibility service, overlay window, or hidden overlay. Compatibility mode explicitly requests system-settings-write access.

## Screen retention across apps

Android's recommended `FLAG_KEEP_SCREEN_ON` only works while the owning Activity is foreground. That cannot fulfill this app's cross-app requirement. ScreenOn deliberately uses the public but deprecated `PowerManager.SCREEN_BRIGHT_WAKE_LOCK` in a user-started `specialUse` foreground service. The lock keeps the display lit without drawing on other applications. Basic mode does not set brightness or change the system timeout. Compatibility mode adds the timeout guard described below. It does not use `ACQUIRE_CAUSES_WAKEUP` or bypass the lock screen.

This compatibility choice is intentional, not a claim of a modern non-deprecated display API. Test on the intended physical devices; OEM power policies, enterprise device policy, low-power restrictions and future Android changes can constrain behavior. Google Play distribution would additionally require the special-use foreground-service declaration/review. This repository and signed APK are suitable for direct installation; Play publication is not performed.

References checked 2026-09-30:
- https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
- https://developer.android.com/reference/android/os/PowerManager#SCREEN_BRIGHT_WAKE_LOCK
- https://developer.android.com/develop/background-work/services/fgs/service-types#special-use

## Lifecycle

- ON: user activity or widget → foreground service → silent low-importance notification → display lock → publish runtime state → redraw widget.
- Repeated ON: reuses the lock and original start time.
- OFF: release lock and remove foreground notification; normal device timeout applies.
- User power button / screen-off broadcast: stop session. Unlock does not restart it.
- Task swiped away: the foreground service remains until OFF or screen lock.
- Process killed, force-stop, restart: lock is released by Android, no sticky service or auto-ON. Application startup/next widget refresh clears old launcher state.
- A cached launcher widget can briefly show old status after an abrupt process kill because no app code can run at that instant. Explicit ON/OFF widget intents make tapping stale ON safely request OFF, rather than unintentionally restarting.

`AwakeController` exposes StateFlow of runtime state, not a persisted preference claiming the lock is alive. Only the service publishes ON after acquisition. One UI preference remembers whether optional notification permission has been asked. Denying it does not disable core functionality; Android's foreground-service task management indication remains system-controlled. The app never promises to hide OS-required indications.

## Signing

Release signing material is generated locally under ignored `.signing/`. Preserve it securely for compatible updates. No keys/passwords enter Git. CI has no signing secret and validates debug, unsigned release and device-test APK builds plus lint. It writes a job summary without uploading Actions artifacts; installable builds are distributed through Releases. The distributed release APK is signed locally and verified with apksigner.


## 1.1.0 compatibility guard (2026-10-02)

Reported hardware: Galaxy Tab S11 Ultra. The user confirms adaptive brightness was off and the screen dims then fully sleeps while ON. No device log/ADB connection is available, so wake-lock suppression vs service termination is not established. Do not label emulator success as an OEM fix confirmation.

- Samsung defaults to compatibility mode; other devices may opt in. Missing WRITE_SETTINGS permission prevents ON and directs the user to the OS grant screen. A widget with missing permission opens the app.
- Native Activity FLAG_KEEP_SCREEN_ON additionally protects our own foreground screen.
- TimeoutGuard synchronously journals the original SCREEN_OFF_TIMEOUT before writing Int.MAX_VALUE (about 24.85 days). Android has no public infinite setting; the display wake lock remains the primary indefinite mechanism. No brightness value is written.
- Failed write/readback fails closed, releases the wake lock and attempts rollback.
- OFF, screen-off, shutdown and service destruction restore the journaled timeout only if the current value is still ours. External/user changes are respected and stop the session, rather than being overwritten in a loop.
- Application startup and BOOT_COMPLETED/MY_PACKAGE_REPLACED restore a leftover journal; they never start ON. Revoked permission retains the journal for retry.
- Force-stop, a crash, uninstall or OEM process kill may leave the long timeout until the next app start/reboot recovery. Android cannot guarantee cleanup at arbitrary process death. The in-app permission explanation explicitly discloses this; turn OFF before force-stop/uninstall. Device administrator maximum-time-to-lock, OEM clamping and battery policy can still override the mechanism.
- Public API reference: https://developer.android.com/reference/android/provider/Settings.System#SCREEN_OFF_TIMEOUT and https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_WRITE_SETTINGS
