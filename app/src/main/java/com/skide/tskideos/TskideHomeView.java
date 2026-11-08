package com.skide.tskideos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TskideHomeView extends View {
    private static final int HOME = 0;
    private static final int DRAWER = 1;
    private static final int QUICK = 2;
    private static final int SETTINGS = 3;
    private static final int APPEARANCE = 4;
    private static final int WIRELESS = 5;
    private static final int SOUND = 6;
    private static final int ABOUT = 7;

    private static final int BG = Color.rgb(13, 17, 25);
    private static final int PANEL = Color.rgb(25, 32, 44);
    private static final int PANEL2 = Color.rgb(34, 43, 58);
    private static final int TEXT = Color.rgb(242, 245, 250);
    private static final int MUTED = Color.rgb(147, 160, 180);
    private static final int[] ACCENTS = {
        Color.rgb(115, 190, 180),
        Color.rgb(232, 137, 112),
        Color.rgb(145, 157, 224),
        Color.rgb(224, 183, 105),
        Color.rgb(164, 145, 198)
    };

    private final TskideActivity activity;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private List<TskideActivity.AppEntry> apps;
    private int screen = HOME;
    private int returnScreen = HOME;
    private int homePage;
    private int appOffset;
    private int accentIndex;
    private int brightness;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float downX;
    private float downY;
    private boolean moved;

    public TskideHomeView(Context context) {
        super(context);
        activity = (TskideActivity) context;
        setFocusable(true);
        setFocusableInTouchMode(true);
        android.content.SharedPreferences preferences = context.getSharedPreferences("tskideos", Context.MODE_PRIVATE);
        accentIndex = preferences.getInt("accent", 0);
        if (accentIndex < 0 || accentIndex >= ACCENTS.length) accentIndex = 0;
        brightness = activity.getBrightness();
        reloadApps();
    }

    public void reloadApps() {
        apps = activity.getLauncherApps();
        if (apps == null) apps = new java.util.ArrayList<TskideActivity.AppEntry>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        scaleX = getWidth() / 240.0f;
        scaleY = getHeight() / 320.0f;
        canvas.save();
        canvas.scale(scaleX, scaleY);
        drawWallpaper(canvas);
        if (screen == HOME) drawHome(canvas);
        else if (screen == DRAWER) drawDrawer(canvas);
        else if (screen == QUICK) drawQuick(canvas);
        else if (screen == SETTINGS) drawSettings(canvas);
        else if (screen == APPEARANCE) drawAppearance(canvas);
        else if (screen == WIRELESS) drawWireless(canvas);
        else if (screen == SOUND) drawSound(canvas);
        else if (screen == ABOUT) drawAbout(canvas);
        drawBottomBar(canvas);
        canvas.restore();
        postInvalidateDelayed(1000);
    }

    private void drawWallpaper(Canvas canvas) {
        canvas.drawColor(BG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(22, 34, 43));
        canvas.drawCircle(218, 70, 105, paint);
        paint.setColor(Color.rgb(19, 27, 42));
        canvas.drawCircle(4, 252, 120, paint);
        paint.setColor(Color.argb(18, 255, 255, 255));
        canvas.drawCircle(185, 225, 70, paint);
    }

    private void drawStatus(Canvas canvas) {
        fill(canvas, 0, 0, 240, 23, Color.rgb(8, 12, 18));
        text(canvas, new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date()), 10, 15, 9, TEXT, true);
        int right = 229;
        drawBattery(canvas, right - 15, 7, activity.getBatteryLevel());
        text(canvas, activity.isCharging() ? "+" : "", right - 27, 15, 8, accent(), true);
        if (activity.isWifiEnabled()) drawWifi(canvas, right - 44, 11, true);
        if (activity.isBluetoothEnabled()) drawBluetooth(canvas, right - 57, 11, true);
        fill(canvas, 0, 23, 240, 1, Color.argb(35, 255, 255, 255));
    }

    private void drawHome(Canvas canvas) {
        drawStatus(canvas);
        if (homePage == 0) {
            text(canvas, new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()).toUpperCase(Locale.US), 16, 49, 8, MUTED, true);
            text(canvas, new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date()), 13, 94, 42, TEXT, false);
            text(canvas, "YOUR DAY, AT A GLANCE", 17, 111, 7, MUTED, true);
            round(canvas, 12, 128, 216, 69, PANEL, 11);
            fill(canvas, 12, 128, 3, 69, accent());
            text(canvas, "TSKIDEOS", 25, 148, 8, accent(), true);
            text(canvas, "A little room to breathe.", 25, 168, 12, TEXT, true);
            text(canvas, activity.getBatteryLevel() + "% battery" + (activity.isCharging() ? " · charging" : ""), 25, 185, 8, MUTED, false);
            text(canvas, "FAVOURITES", 16, 218, 8, MUTED, true);
            round(canvas, 9, 226, 222, 65, PANEL, 12);
            drawShortcut(canvas, 36, "Phone", 0);
            drawShortcut(canvas, 92, "Messages", 1);
            drawShortcut(canvas, 148, "Browser", 2);
            drawShortcut(canvas, 204, "Apps", 3);
            centered(canvas, "SWIPE UP FOR ALL APPS", 120, 299, 7, MUTED, true);
        } else {
            text(canvas, "YOUR APPS", 16, 52, 9, MUTED, true);
            text(canvas, "Your favourites.", 16, 76, 19, TEXT, true);
            round(canvas, 12, 94, 216, 188, PANEL, 11);
            int count = Math.min(6, apps.size());
            for (int i = 0; i < count; i++) {
                int col = i % 3;
                int row = i / 3;
                drawApp(canvas, apps.get(i), 48 + col * 72, 145 + row * 78, 32);
            }
            if (count == 0) centered(canvas, "No apps found", 120, 180, 9, MUTED, false);
            centered(canvas, "SWIPE LEFT OR UP FOR MORE", 120, 299, 7, MUTED, true);
        }
        drawPageDots(canvas);
    }

    private void drawShortcut(Canvas canvas, int x, String label, int kind) {
        drawSymbol(canvas, x, 252, kind, accent());
        centered(canvas, label, x, 280, 7, TEXT, true);
    }

    private void drawPageDots(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 2; i++) {
            paint.setColor(i == homePage ? accent() : Color.rgb(80, 91, 108));
            canvas.drawCircle(114 + i * 12, 310, i == homePage ? 2.5f : 2, paint);
        }
    }

    private void drawDrawer(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "APPS", 15, 49, 19, TEXT, true);
        text(canvas, apps.size() + " available on this phone", 16, 65, 8, MUTED, false);
        int visible = 12;
        for (int i = 0; i < visible; i++) {
            int index = appOffset + i;
            if (index >= apps.size()) break;
            int col = i % 3;
            int row = i / 3;
            drawApp(canvas, apps.get(index), 40 + col * 80, 100 + row * 49, 27);
        }
        if (apps.size() == 0) centered(canvas, "No launchable apps found", 120, 170, 9, MUTED, false);
        centered(canvas, "TAP AN APP  ·  SWIPE TO SCROLL", 120, 291, 7, MUTED, true);
    }

    private void drawApp(Canvas canvas, TskideActivity.AppEntry app, int centerX, int centerY, int size) {
        if (app.icon != null) {
            canvas.save();
            app.icon.setBounds(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2);
            try {
                app.icon.draw(canvas);
            } catch (Exception ignored) {
            }
            canvas.restore();
        } else {
            round(canvas, centerX - size / 2, centerY - size / 2, size, size, PANEL2, 8);
            String initial = app.label.length() == 0 ? "?" : app.label.substring(0, 1).toUpperCase(Locale.US);
            centered(canvas, initial, centerX, centerY + 4, 13, accent(), true);
        }
        centered(canvas, shorten(app.label, 11), centerX, centerY + size / 2 + 12, 7, TEXT, false);
    }

    private void drawQuick(Canvas canvas) {
        drawStatus(canvas);
        round(canvas, 8, 31, 224, 260, PANEL, 13);
        text(canvas, "CONTROL CENTRE", 19, 53, 11, TEXT, true);
        text(canvas, new SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(new Date()), 19, 68, 8, MUTED, false);
        drawToggle(canvas, 17, 82, 99, 51, "Wi-Fi", activity.isWifiEnabled());
        drawToggle(canvas, 124, 82, 99, 51, "Bluetooth", activity.isBluetoothEnabled());
        drawToggle(canvas, 17, 143, 99, 51, "Silent", activity.isSilent());
        drawToggle(canvas, 124, 143, 99, 51, "Settings", false);
        text(canvas, "BRIGHTNESS", 19, 220, 8, MUTED, true);
        drawSlider(canvas, brightness, 20, 238, 200);
        text(canvas, "TAP OR DRAG TO ADJUST", 19, 261, 7, MUTED, false);
        centered(canvas, "SWIPE UP TO CLOSE", 120, 280, 7, MUTED, true);
    }

    private void drawSettings(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "SETTINGS", 15, 53, 20, TEXT, true);
        text(canvas, "Make this phone yours.", 16, 70, 8, MUTED, false);
        drawSettingRow(canvas, 13, 88, "Appearance", "Colour and brightness", "A");
        drawSettingRow(canvas, 13, 139, "Connections", "Wi-Fi and Bluetooth", "W");
        drawSettingRow(canvas, 13, 190, "Sound", "Ringer and silent mode", "S");
        drawSettingRow(canvas, 13, 241, "About TSkideOS", "Version and device", "i");
    }

    private void drawSettingRow(Canvas canvas, int x, int y, String title, String subtitle, String symbol) {
        round(canvas, x, y, 214, 43, PANEL, 9);
        round(canvas, x + 9, y + 9, 25, 25, PANEL2, 7);
        centered(canvas, symbol, x + 21, y + 26, 10, accent(), true);
        text(canvas, title, x + 44, y + 18, 10, TEXT, true);
        text(canvas, subtitle, x + 44, y + 32, 7, MUTED, false);
        text(canvas, "›", x + 195, y + 27, 15, MUTED, false);
    }

    private void drawAppearance(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "APPEARANCE", 15, 53, 17, TEXT, true);
        text(canvas, "A look that feels like yours.", 16, 70, 8, MUTED, false);
        round(canvas, 13, 87, 214, 103, PANEL, 10);
        text(canvas, "ACCENT COLOUR", 24, 108, 8, MUTED, true);
        for (int i = 0; i < ACCENTS.length; i++) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ACCENTS[i]);
            canvas.drawCircle(35 + i * 42, 143, 11, paint);
            if (i == accentIndex) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(TEXT);
                canvas.drawCircle(35 + i * 42, 143, 15, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        text(canvas, "Changes are saved automatically.", 24, 174, 7, MUTED, false);
        round(canvas, 13, 201, 214, 75, PANEL, 10);
        text(canvas, "SCREEN BRIGHTNESS", 24, 222, 8, MUTED, true);
        drawSlider(canvas, brightness, 24, 241, 190);
        text(canvas, brightness + "%", 24, 263, 8, TEXT, true);
    }

    private void drawWireless(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "CONNECTIONS", 15, 53, 17, TEXT, true);
        drawToggle(canvas, 14, 81, 212, 57, "Wi-Fi", activity.isWifiEnabled());
        drawToggle(canvas, 14, 151, 212, 57, "Bluetooth", activity.isBluetoothEnabled());
        round(canvas, 14, 222, 212, 43, PANEL, 9);
        text(canvas, "MORE CONNECTION SETTINGS", 25, 248, 8, TEXT, true);
    }

    private void drawSound(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "SOUND", 15, 53, 19, TEXT, true);
        drawToggle(canvas, 14, 83, 212, 62, "Silent mode", activity.isSilent());
        round(canvas, 14, 162, 212, 55, PANEL, 9);
        text(canvas, "RINGER AND VOLUME", 26, 190, 9, TEXT, true);
        text(canvas, "Open the phone's sound controls", 26, 205, 7, MUTED, false);
    }

    private void drawAbout(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "ABOUT", 15, 53, 19, TEXT, true);
        round(canvas, 13, 83, 214, 180, PANEL, 11);
        centered(canvas, "TSKIDEOS", 120, 125, 21, TEXT, true);
        centered(canvas, "PHONE EDITION 0.2.0", 120, 146, 8, accent(), true);
        centered(canvas, "A lighter home for your Wildfire.", 120, 179, 8, MUTED, false);
        centered(canvas, "Designed for a 240 × 320 screen.", 120, 202, 8, TEXT, false);
        centered(canvas, "Android 2.2+ · ARMv6 device", 120, 225, 7, MUTED, false);
        centered(canvas, "Built small. Made yours.", 120, 245, 8, accent(), true);
    }

    private void drawToggle(Canvas canvas, int x, int y, int w, int h, String label, boolean enabled) {
        round(canvas, x, y, w, h, enabled ? accent() : PANEL2, 9);
        text(canvas, label, x + 11, y + 20, 9, enabled ? BG : TEXT, true);
        text(canvas, enabled ? "ON" : "OFF", x + 11, y + 36, 7, enabled ? BG : MUTED, true);
        paint.setColor(enabled ? BG : MUTED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x + w - 17, y + h / 2, 5, paint);
    }

    private void drawSlider(Canvas canvas, int value, int x, int y, int width) {
        round(canvas, x, y, width, 4, PANEL2, 2);
        round(canvas, x, y, width * value / 100, 4, accent(), 2);
        paint.setColor(TEXT);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x + width * value / 100.0f, y + 2, 7, paint);
    }

    private void drawWifi(Canvas canvas, int x, int y, boolean enabled) {
        paint.setColor(enabled ? TEXT : MUTED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        RectF arc = new RectF(x - 5, y - 6, x + 5, y + 4);
        canvas.drawArc(arc, 220, 100, false, paint);
        arc.inset(2, 2);
        canvas.drawArc(arc, 220, 100, false, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y + 4, 1.5f, paint);
    }

    private void drawBluetooth(Canvas canvas, int x, int y, boolean enabled) {
        paint.setColor(enabled ? TEXT : MUTED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        Path path = new Path();
        path.moveTo(x, y - 7);
        path.lineTo(x + 4, y - 3);
        path.lineTo(x - 3, y + 3);
        path.lineTo(x + 4, y + 7);
        path.lineTo(x, y + 3);
        path.lineTo(x, y - 7);
        path.lineTo(x - 3, y - 3);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBattery(Canvas canvas, int x, int y, int level) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(TEXT);
        canvas.drawRect(x, y, x + 13, y + 8, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(level > 15 ? accent() : Color.rgb(224, 92, 87));
        canvas.drawRect(x + 1, y + 1, x + 1 + 11 * Math.max(0, Math.min(100, level)) / 100.0f, y + 7, paint);
        fill(canvas, x + 13, y + 2, 2, 4, TEXT);
    }

    private void drawSymbol(Canvas canvas, int x, int y, int kind, int color) {
        round(canvas, x - 17, y - 17, 34, 34, kind == 3 ? accent() : PANEL2, 11);
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        if (kind == 0) {
            Path path = new Path();
            path.moveTo(x - 6, y - 10);
            path.cubicTo(x - 12, y - 8, x + 5, y + 13, x + 10, y + 5);
            path.lineTo(x + 4, y);
            path.lineTo(x, y + 4);
            path.lineTo(x - 5, y - 4);
            path.lineTo(x - 2, y - 8);
            canvas.drawPath(path, paint);
        } else if (kind == 1) {
            RectF bubble = new RectF(x - 10, y - 8, x + 10, y + 6);
            canvas.drawRoundRect(bubble, 4, 4, paint);
            Path tail = new Path();
            tail.moveTo(x - 4, y + 6);
            tail.lineTo(x - 7, y + 11);
            tail.lineTo(x + 1, y + 6);
            canvas.drawPath(tail, paint);
        } else if (kind == 2) {
            canvas.drawCircle(x, y, 10, paint);
            canvas.drawArc(new RectF(x - 5, y - 10, x + 5, y + 10), 0, 360, false, paint);
            canvas.drawLine(x - 9, y, x + 9, y, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(x - 10, y - 10, x - 2, y - 2), 2, 2, paint);
            canvas.drawRoundRect(new RectF(x + 2, y - 10, x + 10, y - 2), 2, 2, paint);
            canvas.drawRoundRect(new RectF(x - 10, y + 2, x - 2, y + 10), 2, 2, paint);
            canvas.drawRoundRect(new RectF(x + 2, y + 2, x + 10, y + 10), 2, 2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBottomBar(Canvas canvas) {
        fill(canvas, 0, 300, 240, 20, Color.argb(175, 7, 10, 16));
        round(canvas, 99, 307, 42, 3, Color.rgb(139, 151, 169), 2);
    }

    private void fill(Canvas canvas, float x, float y, float w, float h, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(x, y, x + w, y + h, paint);
    }

    private void round(Canvas canvas, float x, float y, float w, float h, int color, float radius) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        rect.set(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private void text(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
        canvas.drawText(value, x, y, paint);
    }

    private void centered(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(value, x - paint.measureText(value) / 2.0f, y, paint);
    }

    private int accent() {
        return ACCENTS[accentIndex];
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max - 1) + "…";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() / scaleX;
        float y = event.getY() / scaleY;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = x;
            downY = y;
            moved = false;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (Math.abs(x - downX) > 12 || Math.abs(y - downY) > 12) moved = true;
            if (screen == QUICK && downY >= 220 && downY <= 260 && Math.abs(x - downX) > 2) {
                brightness = Math.max(10, Math.min(100, (int)((x - 20) * 100 / 200)));
                activity.setBrightness(brightness);
                invalidate();
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = x - downX;
            float dy = y - downY;
            if (Math.abs(dx) > 35 || Math.abs(dy) > 35) handleSwipe(dx, dy);
            else if (!moved) handleTap(x, y);
            performClick();
            invalidate();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void handleSwipe(float dx, float dy) {
        if (downY <= 25 && dy > 35 && screen != QUICK) {
            returnScreen = screen;
            screen = QUICK;
        } else if (screen == QUICK && dy < -35) {
            screen = returnScreen;
        } else if (screen == HOME && Math.abs(dx) > Math.abs(dy)) {
            homePage = dx < 0 ? 1 : 0;
        } else if (screen == HOME && dy < -35) {
            screen = DRAWER;
            appOffset = 0;
        } else if (screen == HOME && dy > 35) {
            homePage = 0;
        } else if (screen == DRAWER && Math.abs(dy) > Math.abs(dx)) {
            int rows = apps.size() / 3 + (apps.size() % 3 == 0 ? 0 : 1);
            int maxOffset = Math.max(0, rows - 4) * 3;
            appOffset = Math.max(0, Math.min(maxOffset, appOffset + (dy < 0 ? 3 : -3)));
        }
    }

    private void handleTap(float x, float y) {
        if (y >= 300) {
            if (x < 78) navigateBack();
            else if (x > 162) {
                screen = DRAWER;
                appOffset = 0;
            } else {
                screen = HOME;
                homePage = 0;
            }
            return;
        }
        if (screen != QUICK && y <= 23) {
            returnScreen = screen;
            screen = QUICK;
            return;
        }
        if (screen == QUICK) {
            if (y >= 82 && y <= 133) {
                if (x < 120) activity.toggleWifi();
                else activity.toggleBluetooth();
            } else if (y >= 143 && y <= 194) {
                if (x < 120) activity.toggleSilent();
                else screen = SETTINGS;
            } else if (y >= 222 && y <= 260) {
                brightness = Math.max(10, Math.min(100, (int)((x - 20) * 100 / 200)));
                activity.setBrightness(brightness);
            } else if (y < 75) screen = returnScreen;
            return;
        }
        if (screen == HOME) {
            if (homePage == 0 && y >= 226 && y < 294) {
                if (x < 64) activity.launchByHint("phone");
                else if (x < 120) activity.launchByHint("messag");
                else if (x < 176) activity.launchByHint("browser");
                else {
                    screen = DRAWER;
                    appOffset = 0;
                }
            } else if (homePage == 0 && y >= 128 && y <= 197) {
                screen = SETTINGS;
            } else if (homePage == 1 && y >= 94 && y <= 282) {
                int col = Math.max(0, Math.min(2, (int)(x / 80)));
                int row = y < 183 ? 0 : 1;
                int index = row * 3 + col;
                if (index < apps.size()) activity.launchPackage(apps.get(index).packageName);
            } else if (y < 100) {
                returnScreen = HOME;
                screen = QUICK;
            }
            return;
        }
        if (screen == DRAWER) {
            if (y < 68) {
                returnScreen = DRAWER;
                screen = QUICK;
                return;
            }
            int col = Math.max(0, Math.min(2, (int)(x / 80)));
            int row = Math.max(0, Math.min(3, (int)((y - 75) / 49)));
            int index = appOffset + row * 3 + col;
            if (index >= 0 && index < apps.size()) activity.launchPackage(apps.get(index).packageName);
            return;
        }
        if (screen == SETTINGS) {
            if (y >= 88 && y < 132) screen = APPEARANCE;
            else if (y >= 139 && y < 183) screen = WIRELESS;
            else if (y >= 190 && y < 234) screen = SOUND;
            else if (y >= 241 && y < 286) screen = ABOUT;
            return;
        }
        if (screen == APPEARANCE) {
            if (y >= 122 && y <= 163) {
                int selected = Math.round((x - 35) / 42.0f);
                accentIndex = Math.max(0, Math.min(ACCENTS.length - 1, selected));
                activity.getSharedPreferences("tskideos", Context.MODE_PRIVATE).edit().putInt("accent", accentIndex).commit();
            } else if (y >= 229 && y <= 253) {
                brightness = Math.max(10, Math.min(100, (int)((x - 24) * 100 / 190)));
                activity.setBrightness(brightness);
            }
            return;
        }
        if (screen == WIRELESS) {
            if (y >= 81 && y < 138) activity.toggleWifi();
            else if (y >= 151 && y < 208) activity.toggleBluetooth();
            else if (y >= 222 && y < 266) activity.openWifiSettings();
            return;
        }
        if (screen == SOUND) {
            if (y >= 83 && y < 145) activity.toggleSilent();
            else if (y >= 162 && y < 220) activity.openSystemSettings();
            return;
        }
        if (screen == ABOUT && y >= 83 && y <= 263) screen = SETTINGS;
    }

    public boolean navigateBack() {
        if (screen == QUICK) screen = returnScreen;
        else if (screen == DRAWER) screen = HOME;
        else if (screen == SETTINGS) {
            screen = HOME;
            homePage = 0;
        } else if (screen == APPEARANCE || screen == WIRELESS || screen == SOUND || screen == ABOUT) screen = SETTINGS;
        else if (screen == HOME && homePage != 0) homePage = 0;
        else return false;
        invalidate();
        return true;
    }
}