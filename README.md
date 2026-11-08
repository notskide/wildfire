# TSkideOS

TSkideOS is a lightweight, touch-first phone interface being built to revive the HTC Wildfire A3333.

## Phone edition 0.2.0

The phone edition is a Java-only Android home replacement designed around the Wildfire's 240 × 320 display and Android 2.2 (API 8).

- Touch-first home screen with swipeable pages
- App drawer populated from installed launcher apps, using their real names and icons
- Launches installed phone apps
- Pull-down control centre with Wi-Fi, Bluetooth, silent mode and brightness
- Appearance, connections, sound and About settings
- Persistent accent colour
- Battery and clock information
- No native libraries or background services

Build with Gradle:

```sh
gradle :app:assembleDebug
```

The APK targets Android 2.2 / API 8 and can be selected as the default Home app after installation.

## Hardware and ROM boundary

The phone edition runs on top of the HTC Wildfire's existing Android 2.2.1 system. This deliberately preserves the phone's ARMv6 kernel, touchscreen, camera, radio and other hardware drivers. It is a replacement home environment, not yet a standalone bootable ROM. A true ROM requires a device-specific ARMv6 kernel, boot/recovery integration and hardware support.

## Emulator experiment

The `tskideos/` directory contains an earlier freestanding x86 Multiboot experiment for QEMU/Wildfire Lab. That ISO is only for emulator/kernel experiments and cannot boot directly on the ARMv6 HTC Wildfire.
