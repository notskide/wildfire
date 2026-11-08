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
    private static final int SETTINGS = 2;
    private static final int QUICK = 3;
    private static final int APPEARANCE = 4;
    private static final int WIRELESS = 5;
    private static final int SOUND = 6;
    private static final int ABOUT = 7;

    private static final int BG = Color.rgb(15, 19, 29);
    private static final int PANEL = Color.rgb(27, 33, 47);
    private static final int PANEL_LIGHT = Color.rgb(38, 46, 63);
    private static final int TEXT = Color.rgb(241, 243, 248);
    private static final int MUTED = Color.rgb(158, 169, 189);
    private static final int[] ACCENTS = {
        Color.rgb(239, 123, 97),
        Color.rgb(102, 190, 174),
        Color.rgb(145, 153, 226),
        Color.rgb(225, 174, 91)
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
        drawNavigation(canvas);
        canvas.restore();
        postInvalidateDelayed(1000);
    }

    private void drawWallpaper(Canvas canvas) {
        canvas.drawColor(BG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(24, 32, 48));
        canvas.drawCircle(208, 70, 93, paint);
        paint.setColor(Color.rgb(21, 39, 50));
        canvas.drawCircle(12, 240, 105, paint);
        paint.setColor(Color.argb(22, 255, 255, 255));
        canvas.drawCircle(192, 237, 72, paint);
    }

    private void drawStatus(Canvas canvas) {
        fill(canvas, 0, 0, 240, 25, Color.rgb(10, 13, 21));
        text(canvas, "TSKIDEOS", 11, 16, 9, TEXT, true);
        text(canvas, "PHONE EDITION", 155, 16, 7, MUTED, true);
    }

    private void drawHome(Canvas canvas) {
        drawStatus(canvas);
        if (homePage == 0) {
            text(canvas, new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date()).toUpperCase(Locale.US), 16, 49, 8, MUTED, true);
            String time = new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date());
            text(canvas, time, 14, 91, 39, TEXT, false);
            text(canvas, "A fresh start for your phone.", 17, 109, 9, MUTED, false);
            round(canvas, 12, 126, 216, 68, PANEL, 10);
            text(canvas, "YOUR PHONE", 24, 146, 8, accent(), true);
            text(canvas, "Small, simple, and yours.", 24, 165, 12, TEXT, true);
            text(canvas, activity.getBatteryLevel() + "% battery", 24, 182, 8, MUTED, false);
            text(canvas, "FAVOURITES", 16, 215, 8, MUTED, true);
            round(canvas, 10, 225, 220, 66, PANEL, 12);
            drawShortcut(canvas, 39, "Phone", 0);
            drawShortcut(canvas, 93, "Messages", 1);
            drawShortcut(canvas, 147, "Browser", 2);
            drawShortcut(canvas, 201, "Apps", 3);
            text(canvas, "SWIPE UP FOR ALL APPS", 66, 304, 7, MUTED, true);
        } else {
            text(canvas, "YOUR SPACE", 16, 53, 9, MUTED, true);
            text(canvas, "Pick up where you left off.", 16, 77, 16, TEXT, true);
            round(canvas, 12, 98, 216, 184, PANEL, 10);
            text(canvas, "INSTALLED APPS", 24, 119, 8, accent(), true);
            for (int i = 0; i < 6 && i < apps.size(); i++) {
                int col = i % 3;
                int row = i / 3;
                drawApp(canvas, apps.get(i), 40 + col * 80, 157 + row * 78, 31, true);
            }
            text(canvas, "SWIPE DOWN TO RETURN", 70, 304, 7, MUTED, true);
        }
    }

    private void drawShortcut(Canvas canvas, int x, String label, int kind) {
        drawSymbol(canvas, x, 252, kind, accent());
        centered(canvas, label, x, 278, 7, TEXT, true);
    }

    private void drawDrawer(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "ALL APPS", 15, 48, 15, TEXT, true);
        text(canvas, apps.size() + " installed", 16, 62, 8, MUTED, false);
        int visible = 12;
        for (int i = 0; i < visible; i++) {
            int index = appOffset + i;
            if (index >= apps.size()) break;
            int col = i % 3;
            int row = i / 3;
            drawApp(canvas, apps.get(index), 40 + col * 80, 91 + row * 52, 29, false);
        }
        if (apps.size() == 0) centered(canvas, "No launchable apps found", 120, 170, 10, MUTED, false);
        text(canvas, "TAP AN APP  ·  SWIPE TO SCROLL", 49, 294, 7, MUTED, true);
    }

    private void drawApp(Canvas canvas, TskideActivity.AppEntry app, int centerX, int centerY, int size, boolean large) {
        if (app.icon != null) {
            canvas.save();
            app.icon.setBounds(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2);
            try {
                app.icon.draw(canvas);
            } catch (Exception ignored) {
            }
            canvas.restore();
        } else {
            round(canvas, centerX - size / 2, centerY - size / 2, size, size, PANEL_LIGHT, 8);
            centered(canvas, app.label.substring(0, 1).toUpperCase(Locale.US), centerX, centerY + 4, 14, accent(), true);
        }
        centered(canvas, shorten(app.label, large ? 10 : 12), centerX, centerY + size / 2 + 12, 7, TEXT, false);
    }

    private void drawQuick(Canvas canvas) {
        drawStatus(canvas);
        round(canvas, 8, 32, 224, 260, PANEL, 13);
        text(canvas, "CONTROL CENTRE", 20, 54, 11, TEXT, true);
        text(canvas, new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date()), 20, 68, 8, MUTED, false);
        drawToggle(canvas, 18, 82, 98, 52, "Wi-Fi", activity.isWifiEnabled());
        drawToggle(canvas, 124, 82, 98, 52, "Bluetooth", activity.isBluetoothEnabled());
        drawToggle(canvas, 18, 144, 98, 52, "Silent", activity.isSilent());
        drawToggle(canvas, 124, 144, 98, 52, "Settings", false);
        text(canvas, "BRIGHTNESS", 20, 221, 8, MUTED, true);
        drawSlider(canvas, brightness, 20, 239, 200);
        text(canvas, "DRAG THE DOT TO ADJUST", 20, 263, 7, MUTED, false);
        text(canvas, "TAP OUTSIDE OR SWIPE UP TO CLOSE", 25, 280, 7, MUTED, false);
    }

    private void drawSettings(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "SETTINGS", 15, 52, 17, TEXT, true);
        text(canvas, "Make this phone yours.", 16, 69, 8, MUTED, false);
        drawSettingRow(canvas, 14, 88, "Appearance", "Accent and brightness", "A");
        drawSettingRow(canvas, 14, 139, "Wireless", "Wi-Fi and Bluetooth", "W");
        drawSettingRow(canvas, 14, 190, "Sound", "Ringer and silent mode", "S");
        drawSettingRow(canvas, 14, 241, "About TSkideOS", "Phone edition 0.1.0", "i");
    }

    private void drawSettingRow(Canvas canvas, int x, int y, String title, String subtitle, String icon) {
        round(canvas, x, y, 212, 43, PANEL, 8);
        round(canvas, x + 9, y + 9, 25, 25, PANEL_LIGHT, 7);
        centered(canvas, icon, x + 21, y + 26, 10, accent(), true);
        text(canvas, title, x + 44, y + 18, 10, TEXT, true);
        text(canvas, subtitle, x + 44, y + 32, 7, MUTED, false);
        fill(canvas, x + 194, y + 18, 5, 7, MUTED);
    }

    private void drawAppearance(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "APPEARANCE", 15, 52, 16, TEXT, true);
        text(canvas, "Choose a little colour.", 16, 69, 8, MUTED, false);
        round(canvas, 13, 88, 214, 82, PANEL, 9);
        text(canvas, "ACCENT COLOUR", 24, 108, 8, MUTED, true);
        for (int i = 0; i < ACCENTS.length; i++) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ACCENTS[i]);
            canvas.drawCircle(42 + i * 51, 137, 13, paint);
            if (i == accentIndex) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(TEXT);
                canvas.drawCircle(42 + i * 51, 137, 17, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        round(canvas, 13, 181, 214, 83, PANEL, 9);
        text(canvas, "SCREEN BRIGHTNESS", 24, 203, 8, MUTED, true);
        drawSlider(canvas, brightness, 24, 224, 190);
        text(canvas, brightness + "%", 24, 249, 8, TEXT, true);
    }

    private void drawWireless(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "WIRELESS", 15, 52, 17, TEXT, true);
        drawToggle(canvas, 14, 78, 212, 58, "Wi-Fi", activity.isWifiEnabled());
        drawToggle(canvas, 14, 148, 212, 58, "Bluetooth", activity.isBluetoothEnabled());
        round(canvas, 14, 219, 212, 42, PANEL, 8);
        text(canvas, "MORE CONNECTION SETTINGS", 25, 245, 8, TEXT, true);
    }

    private void drawSound(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "SOUND", 15, 52, 17, TEXT, true);
        drawToggle(canvas, 14, 82, 212, 64, "Silent mode", activity.isSilent());
        round(canvas, 14, 164, 212, 52, PANEL, 8);
        text(canvas, "RINGER AND VOLUME", 26, 194, 9, TEXT, true);
        text(canvas, "Use Android sound settings", 26, 207, 7, MUTED, false);
    }

    private void drawAbout(Canvas canvas) {
        drawStatus(canvas);
        text(canvas, "ABOUT", 15, 52, 17, TEXT, true);
        round(canvas, 13, 78, 214, 184, PANEL, 11);
        centered(canvas, "TSKIDEOS", 120, 120, 19, TEXT, true);
        centered(canvas, "PHONE EDITION 0.1.0", 120, 141, 8, accent(), true);
        centered(canvas, "A small, touch-first system", 120, 174, 9, MUTED, false);
        centered(canvas, "made for the HTC Wildfire.", 120, 188, 9, MUTED, false);
        centered(canvas, "Built for a 240 × 320 screen.", 120, 214, 8, TEXT, false);
        centered(canvas, "ARMv6 · Android 2.2+", 120, 235, 8, MUTED, false);
    }

    private void drawToggle(Canvas canvas, int x, int y, int w, int h, String label, boolean enabled) {
        round(canvas, x, y, w, h, enabled ? accent() : PANEL_LIGHT, 9);
        text(canvas, label, x + 11, y + 20, 9, enabled ? BG : TEXT, true);
        text(canvas, enabled ? "ON" : "OFF", x + 11, y + 36, 7, enabled ? BG : MUTED, true);
        paint.setColor(enabled ? BG : MUTED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x + w - 17, y + h / 2, 5, paint);
    }

    private void drawSlider(Canvas canvas, int value, int x, int y, int width) {
        round(canvas, x, y, width, 4, PANEL_LIGHT, 2);
        round(canvas, x, y, width * value / 100, 4, accent(), 2);
        paint.setColor(TEXT);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x + width * value / 100.0f, y + 2, 7, paint);
    }

    private void drawWifi(Canvas canvas, int x, int y, boolean enabled) {
        paint.setColor(enabled ? TEXT : MUTED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.3f);
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

    private void drawBattery(Canvas canvas, int x, int y, int level, boolean charging) {
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
        round(canvas, x - 17, y - 17, 34, 34, kind == 3 ? accent() : PANEL_LIGHT, 11);
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        if (kind == 0) {
            Path p = new Path();
            p.moveTo(x - 6, y - 10);
            p.cubicTo(x - 12, y - 8, x + 5, y + 13, x + 10, y + 5);
            p.lineTo(x + 4, y);
            p.lineTo(x, y + 4);
            p.lineTo(x - 5, y - 4);
            p.lineTo(x - 2, y - 8);
            canvas.drawPath(p, paint);
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
            canvas.drawArc(new RectF(x - 9, y - 5, x + 9, y + 5), 0, 180, false, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(x - 10, y - 10, x - 2, y - 2), 2, 2, paint);
            canvas.drawRoundRect(new RectF(x + 2, y - 10, x + 10, y - 2), 2, 2, paint);
            canvas.drawRoundRect(new RectF(x - 10, y + 2, x - 2, y + 10), 2, 2, paint);
            canvas.drawRoundRect(new RectF(x + 2, y + 2, x + 10, y + 10), 2, 2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawNavigation(Canvas canvas) {
        fill(canvas, 0, 299, 240, 21, Color.argb(110, 5, 7, 12));
        round(canvas, 99, 307, 42, 3, Color.rgb(133, 143, 160), 2);
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
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = x - downX;
            float dy = y - downY;
            if (Math.abs(dx) > 35 || Math.abs(dy) > 35) {
                handleSwipe(dx, dy);
            } else if (!moved) {
                handleTap(x, y);
            }
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
        if (downY <= 28 && dy > 35 && screen != QUICK) {
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
            int rows = Math.max(1, apps.size() / 3 + (apps.size() % 3 == 0 ? 0 : 1));
            int maxOffset = Math.max(0, rows - 4) * 3;
            appOffset = Math.max(0, Math.min(maxOffset, appOffset + (dy < 0 ? 3 : -3)));
        }
    }

    private void handleTap(float x, float y) {
        if (y >= 299) {
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
        if (screen != QUICK && y <= 25) {
            returnScreen = screen;
            screen = QUICK;
            return;
        }
        if (screen == QUICK) {
            if (y >= 82 && y <= 134) {
                if (x < 120) activity.toggleWifi();
                else activity.toggleBluetooth();
            } else if (y >= 144 && y <= 196) {
                if (x < 120) activity.toggleSilent();
                else {
                    screen = SETTINGS;
                    return;
                }
            } else if (y >= 222 && y <= 258) {
                brightness = Math.max(10, Math.min(100, (int)((x - 20) * 100 / 200)));
                activity.setBrightness(brightness);
            } else if (y < 75) {
                screen = returnScreen;
            }
            return;
        }
        if (screen == HOME) {
            if (homePage == 0 && y >= 225 && y < 294) {
                if (x < 66) activity.launchByHint("phone");
                else if (x < 120) activity.launchByHint("messag");
                else if (x < 174) activity.launchByHint("browser");
                else {
                    screen = DRAWER;
                    appOffset = 0;
                }
            } else if (homePage == 0 && y >= 126 && y <= 194) {
                screen = SETTINGS;
            } else if (homePage == 1 && y >= 120 && y <= 282) {
                int col = Math.max(0, Math.min(2, (int)(x / 80)));
                int row = y < 200 ? 0 : 1;
                int index = row * 3 + col;
                if (index < apps.size()) activity.launchPackage(apps.get(index).packageName);
            } else if (y < 100) {
                returnScreen = HOME;
                screen = QUICK;
            }
            return;
        }
        if (screen == DRAWER) {
            if (y < 64) {
                returnScreen = DRAWER;
                screen = QUICK;
                return;
            }
            int col = Math.max(0, Math.min(2, (int)(x / 80)));
            int row = Math.max(0, Math.min(3, (int)((y - 65) / 52)));
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
            if (y >= 115 && y <= 158) {
                int selected = Math.round((x - 42) / 51.0f);
                accentIndex = Math.max(0, Math.min(ACCENTS.length - 1, selected));
            } else if (y >= 212 && y <= 242) {
                brightness = Math.max(10, Math.min(100, (int)((x - 24) * 100 / 190)));
                activity.setBrightness(brightness);
            }
            return;
        }
        if (screen == WIRELESS) {
            if (y >= 78 && y < 136) activity.toggleWifi();
            else if (y >= 148 && y < 206) activity.toggleBluetooth();
            else if (y >= 219 && y < 264) activity.openWifiSettings();
            return;
        }
        if (screen == SOUND) {
            if (y >= 82 && y < 146) activity.toggleSilent();
            else if (y >= 164 && y < 220) activity.openSystemSettings();
            return;
        }
        if (screen == ABOUT) {
            if (y >= 78 && y <= 262) screen = SETTINGS;
        }
    }

    public boolean navigateBack() {
        if (screen == QUICK) {
            screen = returnScreen;
            invalidate();
            return true;
        }
        if (screen == DRAWER || screen == SETTINGS) {
            screen = HOME;
            homePage = 0;
            invalidate();
            return true;
        }
        if (screen == APPEARANCE || screen == WIRELESS || screen == SOUND || screen == ABOUT) {
            screen = SETTINGS;
            invalidate();
            return true;
        }
        if (screen == HOME && homePage != 0) {
            homePage = 0;
            invalidate();
            return true;
        }
        return false;
    }
}
