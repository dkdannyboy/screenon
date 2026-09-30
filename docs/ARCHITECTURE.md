# Architecture and Android constraints

## Scope

Kotlin + Jetpack Compose, Android 8.0/API 26 and newer; compile/target API 36. The app has no backend, network permission, trackers, advertising SDK, accessibility service, overlay window, or global settings writes.

## Screen retention across apps

Android's recommended `FLAG_KEEP_SCREEN_ON` only works while the owning Activity is foreground. That cannot fulfill this app's cross-app requirement. ScreenOn deliberately uses the public but deprecated `PowerManager.SCREEN_BRIGHT_WAKE_LOCK` in a user-started `specialUse` foreground service. The lock keeps the display lit without drawing on other applications. It does not set a custom brightness or change the system screen timeout. It does not use `ACQUIRE_CAUSES_WAKEUP` or bypass the lock screen.

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

Release signing material is generated locally under ignored `.signing/`. Preserve it securely for compatible updates. No keys/passwords enter Git. CI has no signing secret and produces a debug APK plus an unsigned release verification build. The distributed release APK is signed locally and verified with apksigner.
