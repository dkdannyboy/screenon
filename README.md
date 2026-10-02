# ScreenOn

**필요한 동안, 화면은 그대로.**

ON인 동안 다른 앱을 사용해도 화면이 꺼지지 않는 작은 Android 네이티브 앱입니다. 광고, 떠다니는 버튼, 가입, 네트워크 통신이 없습니다. 홈 화면 위젯으로 상태를 확인하고 바로 ON/OFF 할 수 있습니다.

| 앱 | 홈 화면 위젯 |
|---|---|
| ![ScreenOn ON](docs/screenshots/light-on.png) | ![홈 위젯](docs/screenshots/widget-on.png) |

## 갤럭시 탭에서 ON인데도 화면이 꺼진다면

**1.1.0 호환 모드**를 사용하세요. 갤럭시에서는 기본으로 선택됩니다.

1. 기존 앱을 지우지 말고 새 APK를 덮어 설치합니다.
2. 앱에서 ON → **호환 모드 설정** → **시스템 설정 변경 허용**을 켭니다.
3. 앱으로 돌아와 ON을 누릅니다. 다른 앱 위의 버튼은 추가되지 않습니다.

ON 동안 자동 화면 꺼짐 시간을 연장하고 OFF·전원 버튼 잠금 때 원래 값으로 복원합니다. 강제 종료·삭제는 복원 코드를 실행할 수 없으므로 **먼저 OFF를 누르세요**. 강제 종료 후에는 앱을 다시 열어 복원하세요. 재부팅·업데이트 후에도 복원을 시도합니다. 기기 정책이나 권한 철회로 복원이 막히면 설정에서 직접 화면 꺼짐 시간을 바꾸거나 권한을 다시 허용하세요.

갤럭시 탭 S11 울트라 실기기의 실패 보고에 대응한 호환 보완이며, 해당 태블릿에서 해결됐다는 검증은 아직 없습니다.

## 설치

[최신 릴리스에서 ScreenOn APK 다운로드](https://github.com/dkdannyboy/screenon/releases/latest). Android 8.0 이상에서 다운로드한 APK를 열고 설치하세요. 기기에서 요청할 경우 다운로드에 사용한 브라우저/파일 앱의 APK 설치를 허용해야 합니다. 저장소가 비공개이므로 GitHub 접근 권한이 있는 계정이 필요합니다.

## 사용

1. 앱에서 **ON**을 누르세요. 다른 앱으로 이동해도 화면이 유지됩니다.
2. **홈 화면에 위젯 추가**를 누르면 런처의 추가 확인 화면이 열립니다. 지원하지 않는 런처에서는 홈 화면을 길게 누른 뒤 위젯 → ScreenOn을 선택하세요.
3. 위젯의 **ON/OFF**는 상태를 바꾸고, **ScreenOn** 이름은 앱을 엽니다.
4. 앱·위젯·알림의 **끄기**로 종료하세요. 전원 버튼으로 잠가도 자동 OFF 됩니다.

Android가 요구하는 조용한 실행 알림은 표시됩니다. 팝업이나 다른 앱 위의 버튼은 표시하지 않습니다. 알림 권한을 거부해도 화면 유지 기능은 사용할 수 있습니다. 재부팅·강제 종료 후에는 자동으로 켜지지 않습니다.

## 개발

- Kotlin / Jetpack Compose / Material 3
- minSdk 26, targetSdk 36, JDK 17
- Gradle wrapper 및 배포판 SHA-256 고정
- 인터넷·오버레이·접근성 권한 없음. 호환 모드에서만 시스템 설정 변경 권한 사용

```sh
# Android SDK 설치 후 local.properties에 sdk.dir 지정, 또는 ANDROID_HOME 설정
./gradlew assembleDebug lintDebug

# Android 에뮬레이터 또는 테스트용 기기 연결
./gradlew connectedDebugAndroidTest

# 처음 한 번만 로컬 서명키 생성
python3 scripts/create-signing-key.py
./gradlew assembleRelease
```

서명된 APK: `app/build/outputs/apk/release/app-release.apk`.

`.signing/`은 Git에서 제외됩니다. 이 폴더를 안전하게 별도 보관해야 동일한 서명으로 후속 업데이트를 배포할 수 있습니다. 키가 없는 환경의 릴리스 빌드는 unsigned이며, CI는 디버그·릴리스·기기 테스트 APK 빌드와 lint 검증을 수행합니다. 설치 파일은 GitHub 릴리스에서 제공합니다.

## 동작 범위와 검증

화면 유지에는 공개 API인 `SCREEN_BRIGHT_WAKE_LOCK`을 사용합니다. 이 API는 deprecated이지만, 권장 Activity 플래그는 다른 앱으로 이동하면 유지되지 않기 때문에 요구사항에 맞춰 선택했습니다. 제조사 절전 정책과 회사 관리 기기 정책에 따라 동작이 제한될 수 있습니다.

- [구현과 Android 제약](docs/ARCHITECTURE.md)
- [디자인 결정](docs/DESIGN.md)
- [실행 검증 기록](docs/VERIFICATION.md)

Google Play 등록은 수행하지 않았습니다. 직접 설치 가능한 서명 APK를 제공합니다.
