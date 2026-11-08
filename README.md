# TSkideOS

TSkideOS is being rebuilt around the original goal: a lightweight, touch-first phone experience for the HTC Wildfire A3333.

## Phone edition

The Android phone edition is in the `app/` directory. It is a Java-only Android home replacement designed for the Wildfire's 240 × 320 display and Android 2.2 (API 8).

Current features:
- Touch-first home screen with swipe navigation
- App drawer populated from installed launcher apps
- Launches the phone's existing apps
- Quick control panel for Wi-Fi, Bluetooth, silent mode and brightness
- Appearance, wireless, sound and About screens
- Uses the real Android status bar so system notifications remain accessible
- No native libraries or background services

Build with Gradle:

```sh
gradle :app:assembleDebug
```

The APK targets Android 2.2 / API 8 and is intended to be selected as the default Home app after installation.

## Important installation boundary

The phone edition is the first usable TSkideOS experience, running on top of the phone's existing Android 2.2.1 system. This preserves the Wildfire's existing kernel, touchscreen drivers, camera, radio and other device support. It is not yet a standalone replacement ROM or boot image. A true ROM requires a device-specific ARMv6 kernel, boot/recovery integration and matching hardware support.

## Emulator experiment

The `tskideos/` directory contains an earlier freestanding x86 Multiboot experiment for QEMU/Wildfire Lab. That ISO is useful for kernel experiments but cannot boot directly on the ARMv6 HTC Wildfire.
