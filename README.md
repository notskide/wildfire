# TSkideOS

TSkideOS is an independent operating system project. Its primary build is a bootable x86 ISO for PC emulators such as QEMU and Limbo.

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

The ISO is created at `tskideos/build/tskideos.iso`. GitHub Actions builds and tests the ISO on every relevant kernel change.

## Run in Limbo or QEMU

Create an x86 PC virtual machine, attach `tskideos.iso` as a CD-ROM, and boot from the CD-ROM. A small 128 MB RAM allocation is sufficient for the current kernel.

## Hardware boundary

The current ISO targets x86 and is intended for PC emulators. It does not boot directly on the HTC Wildfire A3333, which uses an ARMv6 processor. A Wildfire edition requires a separate ARMv6 kernel, boot image, and device-specific drivers. The Android launcher prototype in `app/` is separate from the TSkideOS operating system and is not the ISO.
