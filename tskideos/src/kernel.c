#include <stdint.h>
#include <stddef.h>

#define MULTIBOOT_MAGIC 0x2BADB002u
#define SHELL_ROWS 20
#define SHELL_COLS 54
#define INPUT_COLS 54

typedef struct __attribute__((packed)) {
    uint32_t flags;
    uint32_t mem_lower;
    uint32_t mem_upper;
    uint32_t boot_device;
    uint32_t cmdline;
    uint32_t mods_count;
    uint32_t mods_addr;
    uint8_t symbols[16];
    uint32_t mmap_length;
    uint32_t mmap_addr;
    uint32_t drives_length;
    uint32_t drives_addr;
    uint32_t config_table;
    uint32_t boot_loader_name;
    uint32_t apm_table;
    uint32_t vbe_control_info;
    uint32_t vbe_mode_info;
    uint16_t vbe_mode;
    uint16_t vbe_interface_seg;
    uint16_t vbe_interface_off;
    uint16_t vbe_interface_len;
    uint64_t framebuffer_addr;
    uint32_t framebuffer_pitch;
    uint32_t framebuffer_width;
    uint32_t framebuffer_height;
    uint8_t framebuffer_bpp;
    uint8_t framebuffer_type;
    uint8_t red_field_position;
    uint8_t red_mask_size;
    uint8_t green_field_position;
    uint8_t green_mask_size;
    uint8_t blue_field_position;
    uint8_t blue_mask_size;
} multiboot_info_t;

static volatile uint8_t *framebuffer;
static uint32_t screen_width;
static uint32_t screen_height;
static uint32_t screen_pitch;
static uint8_t framebuffer_bytes;
static uint8_t red_position = 16;
static uint8_t green_position = 8;
static uint8_t blue_position = 0;
static int graphics_enabled;
static volatile uint16_t *vga_memory = (volatile uint16_t *)0xB8000;
static uint8_t vga_attribute = 0x1F;
static char shell_lines[SHELL_ROWS][SHELL_COLS];
static uint32_t shell_line_count;
static char shell_input[INPUT_COLS];
static uint32_t shell_input_length;
static uint8_t shell_open;
static uint8_t about_open;
static uint8_t shift_down;
static uint8_t caps_lock;
static uint8_t accent_index;
static uint32_t memory_lower;
static uint32_t memory_upper;
static volatile uint32_t timer_ticks;
static volatile uint8_t redraw_pending = 1;
typedef struct __attribute__((packed)) {
    uint16_t offset_low;
    uint16_t selector;
    uint8_t zero;
    uint8_t flags;
    uint16_t offset_high;
} idt_entry_t;
typedef struct __attribute__((packed)) {
    uint16_t limit;
    uint32_t base;
} idt_pointer_t;
static idt_entry_t idt[256];
extern void timer_interrupt_stub(void);
extern void keyboard_interrupt_stub(void);
extern void default_interrupt_stub(void);
static const uint32_t accents[3] = {0xFF7A59, 0x68D8C5, 0xA78BFA};

static const uint8_t font[128][5] = {
    ['!']={0x00,0x00,0x5F,0x00,0x00}, ['"']={0x00,0x07,0x00,0x07,0x00},
    ['#']={0x14,0x7F,0x14,0x7F,0x14}, ['$']={0x24,0x2A,0x7F,0x2A,0x12},
    ['%']={0x23,0x13,0x08,0x64,0x62}, ['&']={0x36,0x49,0x55,0x22,0x50},
    ['\'']={0x00,0x05,0x03,0x00,0x00}, ['(']={0x00,0x1C,0x22,0x41,0x00},
    [')']={0x00,0x41,0x22,0x1C,0x00}, ['*']={0x14,0x08,0x3E,0x08,0x14},
    ['+']={0x08,0x08,0x3E,0x08,0x08}, [',']={0x00,0x50,0x30,0x00,0x00},
    ['-']={0x08,0x08,0x08,0x08,0x08}, ['.']={0x00,0x60,0x60,0x00,0x00},
    ['/']={0x20,0x10,0x08,0x04,0x02},
    ['0']={0x3E,0x51,0x49,0x45,0x3E}, ['1']={0x00,0x42,0x7F,0x40,0x00},
    ['2']={0x62,0x51,0x49,0x49,0x46}, ['3']={0x22,0x41,0x49,0x49,0x36},
    ['4']={0x18,0x14,0x12,0x7F,0x10}, ['5']={0x2F,0x49,0x49,0x49,0x31},
    ['6']={0x3E,0x49,0x49,0x49,0x32}, ['7']={0x01,0x71,0x09,0x05,0x03},
    ['8']={0x36,0x49,0x49,0x49,0x36}, ['9']={0x26,0x49,0x49,0x49,0x3E},
    [':']={0x00,0x36,0x36,0x00,0x00}, [';']={0x00,0x56,0x36,0x00,0x00},
    ['<']={0x08,0x14,0x22,0x41,0x00}, ['=']={0x14,0x14,0x14,0x14,0x14},
    ['>']={0x00,0x41,0x22,0x14,0x08}, ['?']={0x02,0x01,0x51,0x09,0x06},
    ['@']={0x32,0x49,0x79,0x41,0x3E},
    ['A']={0x7E,0x11,0x11,0x11,0x7E}, ['B']={0x7F,0x49,0x49,0x49,0x36},
    ['C']={0x3E,0x41,0x41,0x41,0x22}, ['D']={0x7F,0x41,0x41,0x22,0x1C},
    ['E']={0x7F,0x49,0x49,0x49,0x41}, ['F']={0x7F,0x09,0x09,0x09,0x01},
    ['G']={0x3E,0x41,0x49,0x49,0x7A}, ['H']={0x7F,0x08,0x08,0x08,0x7F},
    ['I']={0x00,0x41,0x7F,0x41,0x00}, ['J']={0x20,0x40,0x41,0x3F,0x01},
    ['K']={0x7F,0x08,0x14,0x22,0x41}, ['L']={0x7F,0x40,0x40,0x40,0x40},
    ['M']={0x7F,0x02,0x0C,0x02,0x7F}, ['N']={0x7F,0x04,0x08,0x10,0x7F},
    ['O']={0x3E,0x41,0x41,0x41,0x3E}, ['P']={0x7F,0x09,0x09,0x09,0x06},
    ['Q']={0x3E,0x41,0x51,0x21,0x5E}, ['R']={0x7F,0x09,0x19,0x29,0x46},
    ['S']={0x46,0x49,0x49,0x49,0x31}, ['T']={0x01,0x01,0x7F,0x01,0x01},
    ['U']={0x3F,0x40,0x40,0x40,0x3F}, ['V']={0x1F,0x20,0x40,0x20,0x1F},
    ['W']={0x3F,0x40,0x38,0x40,0x3F}, ['X']={0x63,0x14,0x08,0x14,0x63},
    ['Y']={0x07,0x08,0x70,0x08,0x07}, ['Z']={0x61,0x51,0x49,0x45,0x43},
    ['[']={0x00,0x7F,0x41,0x41,0x00}, ['\\']={0x02,0x04,0x08,0x10,0x20},
    [']']={0x00,0x41,0x41,0x7F,0x00}, ['^']={0x04,0x02,0x01,0x02,0x04},
    ['_']={0x40,0x40,0x40,0x40,0x40}, ['|']={0x00,0x00,0x7F,0x00,0x00},
    [96]={0x08,0x04,0x08,0x10,0x08}
};

static inline void outb(uint16_t port, uint8_t value) {
    __asm__ volatile("outb %0, %1" : : "a"(value), "Nd"(port));
}

static inline uint8_t inb(uint16_t port) {
    uint8_t value;
    __asm__ volatile("inb %1, %0" : "=a"(value) : "Nd"(port));
    return value;
}

static inline void outw(uint16_t port, uint16_t value) {
    __asm__ volatile("outw %0, %1" : : "a"(value), "Nd"(port));
}

static void serial_init(void) {
    outb(0x3F9, 0x00);
    outb(0x3FB, 0x80);
    outb(0x3F8, 0x01);
    outb(0x3F9, 0x00);
    outb(0x3FB, 0x03);
    outb(0x3FA, 0xC7);
    outb(0x3FC, 0x0B);
}

static void serial_write(char value) {
    for (uint32_t i = 0; i < 100000; i++) {
        if (inb(0x3FD) & 0x20) {
            outb(0x3F8, (uint8_t)value);
            return;
        }
    }
}

static void serial_print(const char *value) {
    while (*value) serial_write(*value++);
}

static void serial_hex(uint32_t value) {
    static const char digits[] = "0123456789ABCDEF";
    serial_print("0x");
    for (int32_t shift = 28; shift >= 0; shift -= 4) serial_write(digits[(value >> shift) & 15]);
}

static void serial_dec(uint32_t value) {
    char digits[11];
    uint32_t count = 0;
    do {
        digits[count++] = (char)('0' + value % 10);
        value /= 10;
    } while (value && count < sizeof(digits));
    while (count) serial_write(digits[--count]);
}

static uint32_t rgb(uint8_t r, uint8_t g, uint8_t b) {
    return ((uint32_t)r << red_position) | ((uint32_t)g << green_position) | ((uint32_t)b << blue_position);
}

static void put_pixel(int32_t x, int32_t y, uint32_t color) {
    if (!graphics_enabled || x < 0 || y < 0 || (uint32_t)x >= screen_width || (uint32_t)y >= screen_height) return;
    volatile uint8_t *pixel = framebuffer + (uint32_t)y * screen_pitch + (uint32_t)x * framebuffer_bytes;
    pixel[0] = (uint8_t)color;
    pixel[1] = (uint8_t)(color >> 8);
    pixel[2] = (uint8_t)(color >> 16);
    if (framebuffer_bytes == 4) pixel[3] = (uint8_t)(color >> 24);
}

static void fill_rect(int32_t x, int32_t y, int32_t width, int32_t height, uint32_t color) {
    if (!graphics_enabled || width <= 0 || height <= 0) return;
    int32_t x0 = x < 0 ? 0 : x;
    int32_t y0 = y < 0 ? 0 : y;
    int32_t x1 = x + width > (int32_t)screen_width ? (int32_t)screen_width : x + width;
    int32_t y1 = y + height > (int32_t)screen_height ? (int32_t)screen_height : y + height;
    for (int32_t py = y0; py < y1; py++) {
        volatile uint8_t *row = framebuffer + (uint32_t)py * screen_pitch;
        for (int32_t px = x0; px < x1; px++) {
            volatile uint8_t *pixel = row + (uint32_t)px * framebuffer_bytes;
            pixel[0] = (uint8_t)color;
            pixel[1] = (uint8_t)(color >> 8);
            pixel[2] = (uint8_t)(color >> 16);
            if (framebuffer_bytes == 4) pixel[3] = (uint8_t)(color >> 24);
        }
    }
}

static void fill_circle(int32_t cx, int32_t cy, int32_t radius, uint32_t color) {
    for (int32_t y = -radius; y <= radius; y++)
        for (int32_t x = -radius; x <= radius; x++)
            if (x * x + y * y <= radius * radius) put_pixel(cx + x, cy + y, color);
}

static void draw_char(int32_t x, int32_t y, char value, uint32_t color, int32_t scale) {
    unsigned char ch = (unsigned char)value;
    if (ch >= 'a' && ch <= 'z') ch = (unsigned char)(ch - 'a' + 'A');
    if (ch > 127) ch = '?';
    for (int32_t column = 0; column < 5; column++)
        for (int32_t row = 0; row < 7; row++)
            if (font[ch][column] & (1u << row)) fill_rect(x + column * scale, y + row * scale, scale, scale, color);
}

static void draw_text(int32_t x, int32_t y, const char *value, uint32_t color, int32_t scale) {
    while (*value) {
        draw_char(x, y, *value++, color, scale);
        x += 6 * scale;
    }
}

static void vga_clear(void) {
    for (uint32_t i = 0; i < 80 * 25; i++) vga_memory[i] = (uint16_t)vga_attribute << 8 | ' ';
}

static void vga_at(uint8_t x, uint8_t y, const char *value, uint8_t attribute) {
    uint32_t position = (uint32_t)y * 80 + x;
    while (*value && x < 80 && y < 25) {
        vga_memory[position++] = (uint16_t)attribute << 8 | (uint8_t)*value++;
        x++;
    }
}

static void init_graphics(uint32_t magic, multiboot_info_t *info) {
    graphics_enabled = 0;
    serial_print("Multiboot magic: ");
    serial_hex(magic);
    serial_print("\r\n");
    if (magic != MULTIBOOT_MAGIC || !info) {
        serial_print("No valid Multiboot information block\r\n");
        return;
    }
    serial_print("Multiboot flags: ");
    serial_hex(info->flags);
    serial_print("\r\n");
    if (!(info->flags & (1u << 12))) {
        serial_print("GRUB did not provide framebuffer information\r\n");
        return;
    }
    serial_print("Framebuffer: ");
    serial_hex((uint32_t)(info->framebuffer_addr >> 32));
    serial_hex((uint32_t)info->framebuffer_addr);
    serial_print(" ");
    serial_dec(info->framebuffer_width);
    serial_print("x");
    serial_dec(info->framebuffer_height);
    serial_print(" pitch=");
    serial_dec(info->framebuffer_pitch);
    serial_print(" bpp=");
    serial_dec(info->framebuffer_bpp);
    serial_print(" type=");
    serial_dec(info->framebuffer_type);
    serial_print("\r\n");
    if (info->framebuffer_type != 1 || (info->framebuffer_bpp != 24 && info->framebuffer_bpp != 32)) {
        serial_print("Unsupported framebuffer pixel format\r\n");
        return;
    }
    if (info->framebuffer_width < 640 || info->framebuffer_height < 480) {
        serial_print("Framebuffer is smaller than 640x480\r\n");
        return;
    }
    framebuffer = (volatile uint8_t *)(uintptr_t)info->framebuffer_addr;
    framebuffer_bytes = (uint8_t)(info->framebuffer_bpp / 8);
    screen_width = info->framebuffer_width;
    screen_height = info->framebuffer_height;
    screen_pitch = info->framebuffer_pitch;
    red_position = info->red_field_position;
    green_position = info->green_field_position;
    blue_position = info->blue_field_position;
    graphics_enabled = 1;
}

static void copy_text(char *destination, const char *source, uint32_t limit) {
    uint32_t i = 0;
    while (source[i] && i + 1 < limit) {
        destination[i] = source[i];
        i++;
    }
    destination[i] = 0;
}

static int text_equal(const char *a, const char *b) {
    uint32_t i = 0;
    while (a[i] && b[i]) {
        char ca = a[i];
        char cb = b[i];
        if (ca >= 'A' && ca <= 'Z') ca = (char)(ca - 'A' + 'a');
        if (cb >= 'A' && cb <= 'Z') cb = (char)(cb - 'A' + 'a');
        if (ca != cb) return 0;
        i++;
    }
    return a[i] == 0 && b[i] == 0;
}

static void append_line(const char *value) {
    if (shell_line_count >= SHELL_ROWS) {
        for (uint32_t i = 1; i < SHELL_ROWS; i++) copy_text(shell_lines[i - 1], shell_lines[i], SHELL_COLS);
        shell_line_count = SHELL_ROWS - 1;
    }
    copy_text(shell_lines[shell_line_count], value, SHELL_COLS);
    shell_line_count++;
}

static void append_wrapped(const char *value) {
    char line[SHELL_COLS];
    uint32_t length = 0;
    while (*value) {
        if (*value == '\n' || length >= SHELL_COLS - 1) {
            line[length] = 0;
            append_line(line);
            length = 0;
            if (*value == '\n') {
                value++;
                continue;
            }
        }
        line[length++] = *value++;
    }
    line[length] = 0;
    append_line(line);
}

static void append_number_line(const char *prefix, uint32_t value, const char *suffix) {
    char line[SHELL_COLS];
    char digits[12];
    uint32_t n = 0;
    do {
        digits[n++] = (char)('0' + value % 10);
        value /= 10;
    } while (value && n < sizeof(digits));
    uint32_t p = 0;
    while (*prefix && p < SHELL_COLS - 2) line[p++] = *prefix++;
    while (n && p < SHELL_COLS - 2) line[p++] = digits[--n];
    while (*suffix && p < SHELL_COLS - 1) line[p++] = *suffix++;
    line[p] = 0;
    append_line(line);
}

static void shell_welcome(void) {
    shell_line_count = 0;
    append_line("HEY SKIDE! TINY SKIDE HERE.");
    append_line("WELCOME TO YOUR TSKIDEOS SHELL.");
    append_line("TYPE HELP IF YOU NEED ANYTHING.");
    append_line("");
}

static void shell_help(void) {
    append_line("AVAILABLE COMMANDS:");
    append_line("HELP ABOUT WHOAMI VER NEOFETCH");
    append_line("MEM ECHO COLOR CLEAR DESKTOP");
    append_line("HELLO JOKE MOOD PING REBOOT");
    append_line("SHUTDOWN");
}

static void shell_about(void) {
    append_line("TSKIDEOS 0.2.0");
    append_line("A SMALL OS WITH A LITTLE BUDDY.");
    append_line("BUILT FOR X86 AND QEMU.");
    append_line("TINY SKIDE SAYS HELLO!");
}

static void reboot_machine(void) {
    uint8_t status = inb(0x64);
    while (status & 0x02) status = inb(0x64);
    outb(0x64, 0xFE);
    for (;;) __asm__ volatile("hlt");
}

static void shutdown_machine(void) {
    outw(0x604, 0x2000);
    outw(0xB004, 0x2000);
    __asm__ volatile("cli");
    for (;;) __asm__ volatile("hlt");
}

static void process_command(void) {
    char command[20];
    uint32_t i = 0;
    while (shell_input[i] == ' ') i++;
    uint32_t start = i;
    while (shell_input[i] && shell_input[i] != ' ' && i - start < sizeof(command) - 1) i++;
    uint32_t length = i - start;
    for (uint32_t j = 0; j < length; j++) command[j] = shell_input[start + j];
    command[length] = 0;
    while (shell_input[i] == ' ') i++;
    const char *argument = &shell_input[i];
    serial_print("shell command: ");
    serial_print(command);
    serial_print("\r\n");

    if (!command[0]) {
        append_line("");
    } else if (text_equal(command, "help")) {
        shell_help();
    } else if (text_equal(command, "about")) {
        shell_about();
    } else if (text_equal(command, "whoami")) {
        append_line("YOU ARE SKIDE.");
    } else if (text_equal(command, "ver") || text_equal(command, "version")) {
        append_line("TSKIDEOS VERSION 0.2.0");
    } else if (text_equal(command, "neofetch")) {
        append_line("TINY SKIDE / TSKIDEOS");
        append_line("---------------------");
        append_line("KERNEL: TSKIDE 0.2.0");
        append_line("ARCH: I386 ON QEMU");
        append_line("SHELL: SKSH");
        append_line("DISPLAY: FRAMEBUFFER");
    } else if (text_equal(command, "hello") || text_equal(command, "hi") || text_equal(command, "hey")) {
        append_line("HEYYY SKIDE! GOOD TO SEE YOU.");
        append_line("I'M JUST CHILLING IN YOUR OS.");
    } else if (text_equal(command, "joke")) {
        append_line("WHY DID THE BYTE VISIT THE DOCTOR?");
        append_line("IT WAS FEELING A LITTLE BIT OFF.");
    } else if (text_equal(command, "mood")) {
        append_line("MOOD: 100 PERCENT CHILL.");
        append_line("STATUS: HAPPY TO BE HERE.");
    } else if (text_equal(command, "ping")) {
        append_line("PONG! I'M RIGHT HERE.");
    } else if (text_equal(command, "buddy") || text_equal(command, "skide")) {
        append_line("TINY SKIDE, AT YOUR SERVICE.");
    } else if (text_equal(command, "mem")) {
        append_number_line("LOW MEMORY: ", memory_lower, " KB");
        append_number_line("HIGH MEMORY: ", memory_upper, " KB");
    } else if (text_equal(command, "echo")) {
        append_wrapped(argument);
    } else if (text_equal(command, "color")) {
        accent_index = (uint8_t)((accent_index + 1) % 3);
        append_line("ACCENT COLOR CHANGED.");
    } else if (text_equal(command, "clear") || text_equal(command, "cls")) {
        shell_line_count = 0;
    } else if (text_equal(command, "desktop") || text_equal(command, "exit")) {
        shell_open = 0;
    } else if (text_equal(command, "reboot")) {
        append_line("REBOOTING...");
        reboot_machine();
    } else if (text_equal(command, "shutdown")) {
        append_line("SHUTTING DOWN...");
        shutdown_machine();
    } else {
        append_line("COMMAND NOT FOUND. TRY HELP.");
    }
    shell_input_length = 0;
    shell_input[0] = 0;
}

static uint32_t accent(void) {
    uint32_t value = accents[accent_index % 3];
    return rgb((uint8_t)(value >> 16), (uint8_t)(value >> 8), (uint8_t)value);
}

static void draw_mascot(int32_t center_x, int32_t top_y, int32_t scale) {
    uint32_t hair = rgb(35, 26, 25);
    uint32_t skin = rgb(174, 111, 78);
    uint32_t shirt = rgb(190, 196, 201);
    uint32_t shirt_shadow = rgb(133, 145, 154);
    uint32_t pants = rgb(42, 48, 64);
    uint32_t shoes = rgb(24, 27, 36);
    uint32_t eye = rgb(28, 24, 26);
    int32_t left = center_x - 17 * scale;
    fill_rect(center_x - 25 * scale, top_y + 87 * scale, 50 * scale, 5 * scale, rgb(25, 31, 54));
    fill_rect(left + 7 * scale, top_y + 65 * scale, 11 * scale, 21 * scale, pants);
    fill_rect(left + 21 * scale, top_y + 65 * scale, 11 * scale, 21 * scale, pants);
    fill_rect(left + 5 * scale, top_y + 82 * scale, 14 * scale, 5 * scale, shoes);
    fill_rect(left + 20 * scale, top_y + 82 * scale, 14 * scale, 5 * scale, shoes);
    fill_rect(left + 4 * scale, top_y + 39 * scale, 29 * scale, 29 * scale, shirt);
    fill_rect(left + 4 * scale, top_y + 62 * scale, 29 * scale, 5 * scale, shirt_shadow);
    fill_rect(left, top_y + 43 * scale, 7 * scale, 23 * scale, shirt);
    fill_rect(left + 30 * scale, top_y + 43 * scale, 7 * scale, 23 * scale, shirt);
    fill_rect(left + 1 * scale, top_y + 61 * scale, 6 * scale, 7 * scale, skin);
    fill_rect(left + 30 * scale, top_y + 61 * scale, 6 * scale, 7 * scale, skin);
    fill_rect(center_x - 5 * scale, top_y + 34 * scale, 10 * scale, 8 * scale, skin);
    fill_rect(center_x - 14 * scale, top_y + 8 * scale, 28 * scale, 27 * scale, skin);
    fill_rect(center_x - 17 * scale, top_y + 14 * scale, 4 * scale, 13 * scale, skin);
    fill_rect(center_x + 13 * scale, top_y + 14 * scale, 4 * scale, 13 * scale, skin);
    fill_rect(center_x - 16 * scale, top_y + 4 * scale, 32 * scale, 13 * scale, hair);
    fill_rect(center_x - 17 * scale, top_y + 10 * scale, 6 * scale, 12 * scale, hair);
    fill_rect(center_x - 10 * scale, top_y + 15 * scale, 6 * scale, 3 * scale, skin);
    fill_rect(center_x + 3 * scale, top_y + 15 * scale, 6 * scale, 3 * scale, skin);
    if ((timer_ticks % 100) < 2) {
        fill_rect(center_x - 9 * scale, top_y + 23 * scale, 4 * scale, 2 * scale, eye);
        fill_rect(center_x + 4 * scale, top_y + 23 * scale, 4 * scale, 2 * scale, eye);
    } else {
        fill_rect(center_x - 8 * scale, top_y + 22 * scale, 3 * scale, 3 * scale, eye);
        fill_rect(center_x + 5 * scale, top_y + 22 * scale, 3 * scale, 3 * scale, eye);
    }
    fill_rect(center_x - 3 * scale, top_y + 29 * scale, 7 * scale, 2 * scale, rgb(93, 48, 43));
    fill_rect(center_x - 4 * scale, top_y + 40 * scale, 8 * scale, 3 * scale, rgb(226, 230, 232));
}

static void draw_background(void) {
    uint32_t top = rgb(19, 24, 47);
    uint32_t bottom = rgb(34, 28, 67);
    for (uint32_t y = 0; y < screen_height; y++) {
        uint32_t t = y * 100 / screen_height;
        uint8_t r = (uint8_t)(((top >> 16) * (100 - t) + (bottom >> 16) * t) / 100);
        uint8_t g = (uint8_t)((((top >> 8) & 255) * (100 - t) + ((bottom >> 8) & 255) * t) / 100);
        uint8_t b = (uint8_t)(((top & 255) * (100 - t) + (bottom & 255) * t) / 100);
        fill_rect(0, (int32_t)y, (int32_t)screen_width, 1, rgb(r, g, b));
    }
    for (uint32_t i = 0; i < 72; i++) {
        int32_t x = (int32_t)((i * 97 + 31) % screen_width);
        int32_t y = (int32_t)((i * 53 + 19) % (screen_height > 100 ? screen_height - 100 : screen_height));
        fill_rect(x, y, (i % 5 == 0) ? 3 : 2, (i % 5 == 0) ? 3 : 2, rgb(82, 91, 139));
    }
    fill_circle((int32_t)(screen_width * 82 / 100), (int32_t)(screen_height * 32 / 100), (int32_t)(screen_height / 6), rgb(38, 43, 83));
    fill_circle((int32_t)(screen_width * 82 / 100), (int32_t)(screen_height * 32 / 100), (int32_t)(screen_height / 8), rgb(44, 51, 94));
}

static void draw_topbar(void) {
    fill_rect(0, 0, (int32_t)screen_width, 38, rgb(13, 16, 32));
    fill_rect(16, 10, 18, 18, accent());
    fill_rect(21, 5, 8, 7, rgb(255, 226, 190));
    draw_text(44, 15, "TSKIDEOS", rgb(241, 244, 255), 2);
    draw_text((int32_t)screen_width - 180, 15, "USER: SKIDE", rgb(176, 187, 214), 1);
    fill_rect((int32_t)screen_width - 17, 16, 5, 5, rgb(105, 214, 169));
}

static void draw_dock(void) {
    int32_t width = 310;
    int32_t x = ((int32_t)screen_width - width) / 2;
    int32_t y = (int32_t)screen_height - 65;
    fill_rect(x + 6, y + 6, width, 48, rgb(12, 15, 31));
    fill_rect(x, y, width, 48, rgb(31, 36, 64));
    fill_rect(x, y, width, 3, rgb(81, 90, 132));
    fill_rect(x + 17, y + 10, 28, 28, accent());
    draw_text(x + 23, y + 19, ">_", rgb(19, 22, 39), 1);
    fill_circle(x + 91, y + 24, 15, rgb(84, 104, 150));
    draw_text(x + 88, y + 18, "i", rgb(255, 255, 255), 2);
    fill_rect(x + 128, y + 10, 28, 28, rgb(80, 90, 120));
    draw_text(x + 135, y + 19, "?", rgb(255, 255, 255), 1);
    draw_text(x + 174, y + 19, "1 TERM", rgb(218, 224, 244), 1);
    draw_text(x + 237, y + 19, "2 ABOUT", rgb(218, 224, 244), 1);
}

static void draw_desktop(void) {
    if (!graphics_enabled) {
        vga_attribute = 0x1F;
        vga_clear();
        vga_at(2, 1, "TSKIDEOS 0.2.0", 0x1F);
        vga_at(2, 3, "TINY SKIDE IS HERE", 0x0E);
        vga_at(2, 6, "  /\\_/\\", 0x0B);
        vga_at(2, 7, " ( o.o )", 0x0B);
        vga_at(2, 8, "  > ^ <", 0x0B);
        vga_at(2, 11, "WELCOME TO YOUR LITTLE DESKTOP.", 0x0F);
        vga_at(2, 13, "PRESS ENTER OR T TO OPEN THE TERMINAL.", 0x0F);
        vga_at(2, 14, "PRESS 2 FOR ABOUT.", 0x0F);
        vga_at(2, 23, "TINY SKIDE - ALWAYS AROUND.", 0x0A);
        return;
    }
    draw_background();
    draw_topbar();
    int32_t panel_width = 580;
    int32_t panel_height = 322;
    int32_t px = ((int32_t)screen_width - panel_width) / 2;
    int32_t py = ((int32_t)screen_height - panel_height) / 2 - 8;
    fill_rect(px + 8, py + 10, panel_width, panel_height, rgb(11, 13, 28));
    fill_rect(px, py, panel_width, panel_height, rgb(39, 45, 76));
    fill_rect(px, py, panel_width, 34, rgb(48, 55, 91));
    fill_circle(px + 18, py + 17, 4, rgb(248, 103, 111));
    fill_circle(px + 33, py + 17, 4, rgb(250, 197, 92));
    fill_circle(px + 48, py + 17, 4, rgb(101, 213, 158));
    draw_text(px + 67, py + 13, "WELCOME", rgb(211, 218, 242), 1);
    draw_mascot(px + 145, py + 62, 3);
    draw_text(px + 276, py + 72, "HEY, I'M TINY SKIDE!", rgb(255, 255, 255), 2);
    draw_text(px + 276, py + 111, "YOUR LITTLE OS BUDDY.", rgb(181, 194, 222), 1);
    fill_rect(px + 276, py + 143, 240, 2, accent());
    draw_text(px + 276, py + 168, "A SMALL OS.", rgb(222, 228, 246), 2);
    draw_text(px + 276, py + 191, "A LITTLE PERSONALITY.", rgb(222, 228, 246), 1);
    fill_rect(px + 276, py + 225, 230, 34, accent());
    draw_text(px + 292, py + 238, "PRESS ENTER TO START", rgb(20, 24, 42), 1);
    draw_text(px + 276, py + 277, "OR PRESS 2 TO MEET ME", rgb(164, 177, 208), 1);
    draw_dock();
    draw_text(18, (int32_t)screen_height - 26, "TSKIDEOS 0.2.0 | BUILT SMALL. MADE YOURS.", rgb(157, 168, 201), 1);
}

static void draw_terminal(void) {
    if (!graphics_enabled) {
        vga_attribute = 0x07;
        vga_clear();
        vga_at(0, 0, " TSKIDEOS TERMINAL - SKSH ", 0x70);
        uint32_t visible = shell_line_count < 20 ? shell_line_count : 20;
        uint32_t start = shell_line_count - visible;
        for (uint32_t i = 0; i < visible && i < 20; i++) vga_at(1, (uint8_t)(2 + i), shell_lines[start + i], 0x07);
        vga_at(1, 23, "TSKIDE > ", 0x0B);
        vga_at(10, 23, shell_input, 0x0F);
        return;
    }
    draw_background();
    draw_topbar();
    int32_t x = 45;
    int32_t y = 48;
    int32_t width = (int32_t)screen_width - 90;
    int32_t height = (int32_t)screen_height - 116;
    fill_rect(x + 8, y + 10, width, height, rgb(8, 10, 21));
    fill_rect(x, y, width, height, rgb(20, 24, 40));
    fill_rect(x, y, width, 38, rgb(43, 49, 75));
    fill_circle(x + 18, y + 19, 4, rgb(248, 103, 111));
    fill_circle(x + 33, y + 19, 4, rgb(250, 197, 92));
    fill_circle(x + 48, y + 19, 4, rgb(101, 213, 158));
    draw_text(x + 67, y + 15, "SKSH TERMINAL", rgb(232, 237, 250), 1);
    draw_text(x + width - 92, y + 15, "ESC: HOME", rgb(168, 180, 211), 1);
    draw_text(x + 22, y + 57, "TINY SKIDE SHELL", accent(), 2);
    draw_text(x + 22, y + 83, "TYPE HELP FOR COMMANDS.", rgb(155, 170, 200), 1);
    uint32_t visible = 15;
    uint32_t start = shell_line_count > visible ? shell_line_count - visible : 0;
    for (uint32_t i = start; i < shell_line_count; i++) {
        int32_t row = (int32_t)(i - start);
        draw_text(x + 22, y + 112 + row * 20, shell_lines[i], rgb(218, 225, 243), 2);
    }
    fill_rect(x + 16, y + height - 54, width - 32, 1, rgb(59, 67, 97));
    draw_text(x + 22, y + height - 36, "TSKIDE >", accent(), 2);
    draw_text(x + 130, y + height - 36, shell_input, rgb(245, 247, 255), 2);
    fill_rect(x + 130 + (int32_t)shell_input_length * 12, y + height - 38, 7, 16, accent());
}

static void draw_about(void) {
    if (!graphics_enabled) {
        vga_attribute = 0x1F;
        vga_clear();
        vga_at(2, 2, "ABOUT TSKIDEOS", 0x0E);
        vga_at(2, 5, "TSKIDEOS IS A SMALL X86 OPERATING SYSTEM.", 0x0F);
        vga_at(2, 7, "TINY SKIDE LIVES ON YOUR DESKTOP.", 0x0F);
        vga_at(2, 9, "BUILT-IN GRAPHICS, KEYBOARD AND SHELL.", 0x0F);
        vga_at(2, 12, "PRESS ESC TO RETURN.", 0x0B);
        return;
    }
    draw_background();
    draw_topbar();
    int32_t x = ((int32_t)screen_width - 500) / 2;
    int32_t y = ((int32_t)screen_height - 320) / 2;
    fill_rect(x + 8, y + 10, 500, 320, rgb(10, 12, 27));
    fill_rect(x, y, 500, 320, rgb(39, 45, 76));
    fill_rect(x, y, 500, 38, rgb(48, 55, 91));
    draw_text(x + 20, y + 15, "ABOUT TSKIDEOS", rgb(238, 241, 255), 1);
    draw_mascot(x + 100, y + 74, 2);
    draw_text(x + 190, y + 76, "TSKIDEOS 0.2.0", accent(), 2);
    draw_text(x + 190, y + 111, "A SMALL X86 OPERATING SYSTEM.", rgb(228, 233, 248), 1);
    draw_text(x + 190, y + 138, "TINY SKIDE LIVES ON YOUR DESKTOP.", rgb(228, 233, 248), 1);
    draw_text(x + 190, y + 165, "BUILT-IN GRAPHICS, KEYBOARD AND SHELL.", rgb(190, 201, 225), 1);
    draw_text(x + 190, y + 192, "NO NETWORK. NO EXTRA SERVICES.", rgb(190, 201, 225), 1);
    fill_rect(x + 190, y + 232, 260, 2, accent());
    draw_text(x + 190, y + 260, "PRESS ESC TO RETURN HOME.", rgb(177, 189, 218), 1);
}

static char translate_scancode(uint8_t code) {
    static const char normal[128] = {
        [0x02]='1',[0x03]='2',[0x04]='3',[0x05]='4',[0x06]='5',[0x07]='6',[0x08]='7',[0x09]='8',[0x0A]='9',[0x0B]='0',
        [0x0C]='-',[0x0D]='=',[0x10]='q',[0x11]='w',[0x12]='e',[0x13]='r',[0x14]='t',[0x15]='y',[0x16]='u',[0x17]='i',[0x18]='o',[0x19]='p',
        [0x1A]='[',[0x1B]=']',[0x1E]='a',[0x1F]='s',[0x20]='d',[0x21]='f',[0x22]='g',[0x23]='h',[0x24]='j',[0x25]='k',[0x26]='l',
        [0x27]=';',[0x28]='\'',[0x29]=96,[0x2B]=92,[0x2C]='z',[0x2D]='x',[0x2E]='c',[0x2F]='v',[0x30]='b',[0x31]='n',[0x32]='m',
        [0x33]=',',[0x34]='.',[0x35]='/',[0x39]=' '
    };
    char ch = normal[code];
    if (!ch) return 0;
    if (ch >= 'a' && ch <= 'z') {
        if (shift_down ^ caps_lock) ch = (char)(ch - 'a' + 'A');
        return ch;
    }
    if (!shift_down) return ch;
    switch (ch) {
        case '1': return '!';
        case '2': return '@';
        case '3': return '#';
        case '4': return '$';
        case '5': return '%';
        case '6': return '^';
        case '7': return '&';
        case '8': return '*';
        case '9': return '(';
        case '0': return ')';
        case '-': return '_';
        case '=': return '+';
        case '[': return '{';
        case ']': return '}';
        case ';': return ':';
        case '\'': return '"';
        case 96: return '~';
        case 92: return '|';
        case ',': return '<';
        case '.': return '>';
        case '/': return '?';
        default: return ch;
    }
}

static void open_terminal(void) {
    shell_open = 1;
    about_open = 0;
    shell_welcome();
}

static void handle_key(uint8_t code) {
    if (code & 0x80) {
        uint8_t released = (uint8_t)(code & 0x7F);
        if (released == 0x2A || released == 0x36) shift_down = 0;
        return;
    }
    if (code == 0x2A || code == 0x36) {
        shift_down = 1;
        return;
    }
    if (code == 0x3A) {
        caps_lock = (uint8_t)!caps_lock;
        return;
    }
    if (code == 0x01) {
        shell_open = 0;
        about_open = 0;
        return;
    }
    if (shell_open) {
        if (code == 0x1C) {
            append_line("TSKIDE >");
            process_command();
        } else if (code == 0x0E) {
            if (shell_input_length) shell_input[--shell_input_length] = 0;
        } else if (shell_input_length < INPUT_COLS - 1) {
            char ch = translate_scancode(code);
            if (ch >= 32 && ch <= 126) {
                shell_input[shell_input_length++] = ch;
                shell_input[shell_input_length] = 0;
            }
        }
        return;
    }
    if (about_open) return;
    char ch = translate_scancode(code);
    if (code == 0x1C || ch == 't' || ch == 'T' || ch == '1') open_terminal();
    else if (ch == '2' || ch == 'a' || ch == 'A') about_open = 1;
}

static void set_idt_gate(uint8_t vector, void (*handler)(void)) {
    uint32_t address = (uint32_t)(uintptr_t)handler;
    idt[vector].offset_low = (uint16_t)(address & 0xFFFF);
    idt[vector].selector = 0x08;
    idt[vector].zero = 0;
    idt[vector].flags = 0x8E;
    idt[vector].offset_high = (uint16_t)(address >> 16);
}

void timer_irq_handler(void) {
    timer_ticks++;
    uint32_t phase = timer_ticks % 100;
    if (phase == 0 || phase == 2) redraw_pending = 1;
    outb(0x20, 0x20);
}

void keyboard_irq_handler(void) {
    handle_key(inb(0x60));
    redraw_pending = 1;
    outb(0x20, 0x20);
}

static void setup_interrupts(void) {
    for (uint32_t i = 0; i < 256; i++) set_idt_gate((uint8_t)i, default_interrupt_stub);
    set_idt_gate(0x20, timer_interrupt_stub);
    set_idt_gate(0x21, keyboard_interrupt_stub);
    idt_pointer_t pointer;
    pointer.limit = (uint16_t)(sizeof(idt) - 1);
    pointer.base = (uint32_t)(uintptr_t)&idt;
    __asm__ volatile("lidt %0" : : "m"(pointer));
    outb(0x20, 0x11);
    outb(0xA0, 0x11);
    outb(0x21, 0x20);
    outb(0xA1, 0x28);
    outb(0x21, 0x04);
    outb(0xA1, 0x02);
    outb(0x21, 0x01);
    outb(0xA1, 0x01);
    outb(0x21, 0xFC);
    outb(0xA1, 0xFF);
    uint16_t divisor = (uint16_t)(1193182 / 20);
    outb(0x43, 0x36);
    outb(0x40, (uint8_t)(divisor & 0xFF));
    outb(0x40, (uint8_t)(divisor >> 8));
    __asm__ volatile("sti");
}

void kernel_main(uint32_t magic, uint32_t info_address) {
    multiboot_info_t *info = (multiboot_info_t *)(uintptr_t)info_address;
    serial_init();
    serial_print("TSkideOS 0.2.0 booting\r\n");
    if (magic == MULTIBOOT_MAGIC && info && (info->flags & 1u)) {
        memory_lower = info->mem_lower;
        memory_upper = info->mem_upper;
    }
    init_graphics(magic, info);
    if (graphics_enabled) serial_print("framebuffer online\r\n");
    else serial_print("VGA text fallback online\r\n");
    serial_print("Tiny Skide desktop ready\r\n");
    timer_ticks = 2;
    draw_desktop();
    redraw_pending = 0;
    setup_interrupts();
    for (;;) {
        __asm__ volatile("cli");
        if (redraw_pending) {
            redraw_pending = 0;
            __asm__ volatile("sti");
            if (shell_open) draw_terminal();
            else if (about_open) draw_about();
            else draw_desktop();
        } else {
            __asm__ volatile("sti; hlt");
        }
    }
}
