# TSkideOS and Wildfire Lab

This repository contains two related experiments:

- **TSkideOS** is an independent operating-system project. Its current kernel is a 32-bit x86 OS built as a bootable GRUB ISO for PC emulators.
- **Wildfire Lab** is one Android-hosted virtual-machine app based on Limbo/QEMU. It includes both PC and ARM64 guest engines, with an in-app architecture chooser.

## TSkideOS 0.3.0

The current kernel boots through GRUB using Multiboot, initializes a framebuffer when one is available, draws the TSkideOS desktop, reads a PS/2 keyboard and provides the SKSH command shell.

- Bootable GRUB ISO for x86 PCs and emulators
- Framebuffer graphics with VGA text fallback
- Keyboard input and a built-in command shell
- System information, memory reporting, accent colours and desktop/about screens
- Reboot and emulator shutdown commands
- QEMU boot test that verifies the shell accepts a command

Build locally on a Linux x86 toolchain:

```sh
make -C tskideos clean iso
```

The ISO is created at `tskideos/build/tskideos.iso`. GitHub Actions builds and tests the ISO on relevant kernel changes.

## Wildfire Lab 0.4.0

GitHub Actions packages one installable Android APK named **Wildfire Lab**. On launch, choose the guest architecture:

- **PC systems** runs x86 and x86_64 guest operating systems.
- **Phone / ARM systems** runs AArch64 (ARM64) guest operating systems designed for generic virtual hardware.

Both engines are included in the same APK. The Android host app is built for 64-bit ARM devices; the guest CPU architecture is selected inside Wildfire Lab and is independent from the Android device's own CPU architecture.

### Supported image types

Wildfire Lab uses QEMU's disk and optical-drive support:

- ISO images for CD/DVD boot media
- Raw disk images such as IMG and RAW
- QCOW and QCOW2
- VDI and VMDK
- VPC/VHD disk images
- Floppy images such as IMA

The file picker is intentionally not restricted to a short extension list. Whether an image boots still depends on its format, guest architecture, virtual machine configuration and the operating system inside it.

### Importing phone ROM ZIPs

Use **Import ROM ZIP** in the VM app to extract a ROM archive into a folder you choose. The importer blocks unsafe archive paths and limits the number of entries and total extracted size.

A ROM ZIP is a container, not a bootable virtual disk. It may contain files such as `boot.img`, `system.img`, `vendor.img`, `super.img`, `recovery.img` or `payload.bin`. Attach only components that match the VM's virtual hardware and boot method. Android sparse images or payload packages may need conversion before QEMU can use them.

### Phone OS compatibility boundary

The Mobile edition adds an AArch64 (ARM64) virtual machine; it does not emulate every real phone model. Firmware images for a specific Samsung, Pixel, HTC or other handset can depend on that device's exact SoC, boot chain, graphics, radio and vendor drivers. Such images are not guaranteed to boot in a generic QEMU machine. This is a virtualisation feature, not a phone flasher and it does not install firmware onto the Android device running the app.

The existing TSkideOS ISO remains x86-only. A native ARM phone build of TSkideOS would require a separate ARM kernel, boot image and suitable device drivers.

## Build and download

The **Build Wildfire Lab native engine** workflow compiles the ARM64 Android host libraries with x86_64 and AArch64 guest engines. The APK workflow places both guest engines in one app and publishes one `wildfirelab-unified-apk` artifact containing `WildfireLab-0.4.0.apk`. Changes to the app workflow or patch scripts also trigger packaging against the latest successful native-engine build.

The **Build TSkideOS ISO** workflow separately builds and tests `tskideos/build/tskideos.iso`.

## Run TSkideOS in an emulator

Create an x86 PC virtual machine, attach `tskideos.iso` as a CD-ROM, and boot from the CD-ROM. A small 128 MB RAM allocation is sufficient for the current kernel.

## Hardware boundary

The current TSkideOS ISO targets x86 and is intended for PC emulators. It does not boot directly on ARM phone hardware such as the HTC Wildfire A3333. A Wildfire-specific native edition needs a separate ARMv6 kernel, boot image and device drivers.
