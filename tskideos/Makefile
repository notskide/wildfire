CC := gcc
AS := as
LD := ld
CFLAGS := -m32 -std=gnu11 -O2 -Wall -Wextra -Werror -ffreestanding -fno-stack-protector -fno-pic -fno-pie -fno-builtin -fno-omit-frame-pointer -mno-sse -mno-mmx -mno-80387
ASFLAGS := --32
LDFLAGS := -m elf_i386 -T linker.ld -nostdlib
BUILD := build

.PHONY: all iso clean check

all: $(BUILD)/kernel.elf

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/boot.o: src/boot.S | $(BUILD)
	$(AS) $(ASFLAGS) $< -o $@

$(BUILD)/kernel.o: src/kernel.c | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD)/kernel.elf: $(BUILD)/boot.o $(BUILD)/kernel.o linker.ld
	$(LD) $(LDFLAGS) -o $@ $(BUILD)/boot.o $(BUILD)/kernel.o
	grub-file --is-x86-multiboot $@

iso: $(BUILD)/kernel.elf
	rm -rf $(BUILD)/iso-root
	mkdir -p $(BUILD)/iso-root/boot/grub
	cp $(BUILD)/kernel.elf $(BUILD)/iso-root/boot/kernel.elf
	cp grub/grub.cfg $(BUILD)/iso-root/boot/grub/grub.cfg
	grub-mkrescue -o $(BUILD)/tskideos.iso $(BUILD)/iso-root

check: iso
	file $(BUILD)/kernel.elf $(BUILD)/tskideos.iso

clean:
	rm -rf $(BUILD)
