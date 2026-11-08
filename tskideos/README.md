# TSkideOS

TSkideOS is a small x86 operating-system experiment designed to boot in Wildfire Lab and other QEMU-based emulators.

## Current build

- GRUB Multiboot1 boot
- 800x600x32 VESA framebuffer request with VGA text fallback
- Custom pixel desktop and Tiny Skide mascot
- Polled PS/2 keyboard input
- Built-in interactive command shell
- No external runtime services or network requirement

## Keyboard

- Enter or T: open the terminal
- 1: open the terminal
- 2: open the About screen
- Escape: return to the desktop
- Enter in the terminal: run a command
- Backspace: edit the command

## Shell commands

- help
- about
- whoami
- ver
- neofetch
- mem
- echo TEXT
- color
- clear
- desktop
- reboot
- shutdown

## Build

On Ubuntu or Debian, install gcc-multilib, grub-pc-bin, grub-common, xorriso and mtools, then run make -C tskideos iso.

The ISO is written to tskideos/build/tskideos.iso.

## Emulator settings

Use an x86 PC machine, one CPU, 512 MB to 1 GB guest RAM, a standard VGA adapter, and boot from the CD-ROM. The kernel is 32-bit so it also works with the x86_64 QEMU machine used by Wildfire Lab.
