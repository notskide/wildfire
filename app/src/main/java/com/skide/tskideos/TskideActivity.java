package com.skide.tskideos;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TskideActivity extends Activity {
    private TskideHomeView homeView;
    private int batteryLevel = 100;
    private boolean charging;

    public static class AppEntry {
        public final String label;
        public final String packageName;
        public final Drawable icon;

        public AppEntry(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (scale > 0) batteryLevel = batteryLevel * 100 / scale;
            charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
            if (homeView != null) homeView.invalidate();
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        homeView = new TskideHomeView(this);
        setContentView(homeView);
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        batteryReceiver.onReceive(this, battery);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeView != null) homeView.reloadApps();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (homeView != null && homeView.navigateBack()) return;
        super.onBackPressed();
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public boolean isCharging() {
        return charging;
    }

    public List<AppEntry> getLauncherApps() {
        List<AppEntry> result = new ArrayList<AppEntry>();
        PackageManager manager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> matches = manager.queryIntentActivities(intent, 0);
        for (ResolveInfo info : matches) {
            if (info.activityInfo == null) continue;
            String packageName = info.activityInfo.packageName;
            if (getPackageName().equals(packageName)) continue;
            String label;
            Drawable icon;
            try {
                label = info.loadLabel(manager).toString();
                icon = info.loadIcon(manager);
            } catch (Exception error) {
                continue;
            }
            boolean duplicate = false;
            for (AppEntry entry : result) {
                if (entry.packageName.equals(packageName)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) result.add(new AppEntry(label, packageName, icon));
        }
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(result, new Comparator<AppEntry>() {
            public int compare(AppEntry first, AppEntry second) {
                return collator.compare(first.label, second.label);
            }
        });
        return result;
    }

    public void launchPackage(String packageName) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch == null) {
                showMessage("This app cannot be opened.");
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception error) {
            showMessage("Couldn't open that app.");
        }
    }

    public void launchByHint(String hint) {
        String query = hint.toLowerCase(Locale.US);
        List<AppEntry> apps = getLauncherApps();
        for (AppEntry entry : apps) {
            String label = entry.label.toLowerCase(Locale.US);
            String packageName = entry.packageName.toLowerCase(Locale.US);
            if (label.equals(query) || packageName.contains(query)) {
                launchPackage(entry.packageName);
                return;
            }
        }
        for (AppEntry entry : apps) {
            if (entry.label.toLowerCase(Locale.US).contains(query)) {
                launchPackage(entry.packageName);
                return;
            }
        }
        showMessage(hint + " app not found.");
    }

    public boolean isWifiEnabled() {
        try {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return manager != null && manager.isWifiEnabled();
        } catch (Exception error) {
            return false;
        }
    }

    public void toggleWifi() {
        try {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (manager == null) {
                showMessage("Wi-Fi control unavailable.");
                return;
            }
            boolean enabled = !manager.isWifiEnabled();
            manager.setWifiEnabled(enabled);
            showMessage(enabled ? "Turning Wi-Fi on..." : "Turning Wi-Fi off...");
            if (homeView != null) homeView.invalidate();
        } catch (Exception error) {
            showMessage("Wi-Fi could not be changed.");
        }
    }

    public boolean isBluetoothEnabled() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        } catch (Exception error) {
            return false;
        }
    }

    public void toggleBluetooth() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                showMessage("Bluetooth is not available.");
                return;
            }
            if (adapter.isEnabled()) {
                adapter.disable();
                showMessage("Turning Bluetooth off...");
            } else {
                adapter.enable();
                showMessage("Turning Bluetooth on...");
            }
            if (homeView != null) homeView.invalidate();
        } catch (Exception error) {
            showMessage("Bluetooth could not be changed.");
        }
    }

    public boolean isSilent() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return manager != null && manager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }

    public void toggleSilent() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager == null) return;
        manager.setRingerMode(isSilent() ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
        showMessage(isSilent() ? "Silent mode on." : "Sound enabled.");
        if (homeView != null) homeView.invalidate();
    }

    public int getBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 160) * 100 / 255;
        } catch (Exception error) {
            return 63;
        }
    }

    public void setBrightness(int value) {
        int safeValue = Math.max(10, Math.min(100, value));
        float brightness = safeValue / 100.0f;
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = brightness;
        getWindow().setAttributes(params);
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, safeValue * 255 / 100);
        } catch (Exception error) {
            showMessage("Brightness changed for this session.");
        }
        if (homeView != null) homeView.invalidate();
    }

    public void openSystemSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception error) {
            showMessage("System settings are unavailable.");
        }
    }

    public void openWifiSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        } catch (Exception error) {
            openSystemSettings();
        }
    }

    public void openBluetoothSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (Exception error) {
            openSystemSettings();
        }
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
