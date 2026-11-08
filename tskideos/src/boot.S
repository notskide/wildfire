.section .multiboot,"a"
.align 4
.long 0x1BADB002
.long 0x00000007
.long -(0x1BADB002 + 0x00000007)
.long 0
.long 800
.long 600
.long 32

.section .bss
.align 16
stack_bottom:
.skip 16384
stack_top:

.section .text
.global _start
.type _start, @function
_start:
    cli
    cld
    mov $stack_top, %esp
    xor %ebp, %ebp
    push %ebx
    push %eax
    call kernel_main
halt:
    cli
    hlt
    jmp halt
.size _start, . - _start

.global timer_interrupt_stub
.type timer_interrupt_stub, @function
timer_interrupt_stub:
    pusha
    call timer_irq_handler
    popa
    iret

.global keyboard_interrupt_stub
.type keyboard_interrupt_stub, @function
keyboard_interrupt_stub:
    pusha
    call keyboard_irq_handler
    popa
    iret

.global default_interrupt_stub
.type default_interrupt_stub, @function
default_interrupt_stub:
    iret
