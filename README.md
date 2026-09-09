# MotoNetSpeed

Small Android app for toggling Motorola's `internet_speed_switch` through Shizuku.

## What it does

The app exposes two operations through a restricted AIDL UserService:

- read `global internet_speed_switch`
- write `global internet_speed_switch` as `0` or `1`

The privileged service invokes only:

`/system/bin/cmd motsettings get global internet_speed_switch`

and

`/system/bin/cmd motsettings put global internet_speed_switch 0|1`

No arbitrary command input is exposed by the UI.

## Requirements

- Android 7.0+ (minSdk 24)
- Shizuku running and authorized for this app
- A Motorola build that provides the `motsettings` command/key

The app itself does not require root. The privilege used by the UserService is the privilege provided by Shizuku.

## Build

GitHub Actions builds the debug APK with:

- JDK 17
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- compileSdk 36
- targetSdk 35
- Shizuku API/provider 13.1.5

The workflow uploads `app-debug.apk` as an artifact.

## Important

The Motorola command is device/firmware specific. A successful APK build does not guarantee that every Motorola model exposes `internet_speed_switch`.
