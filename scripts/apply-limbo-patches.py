#!/usr/bin/env python3
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: apply-limbo-patches.py LIMBO_SOURCE_DIR")

root = Path(sys.argv[1]).resolve()
patch_root = Path(__file__).resolve().parents[1] / "patches"
activity = root / "limbo-android-lib/src/main/java/com/max2idea/android/limbo/main/LimboActivity.java"
manifest = root / "limbo-android-lib/src/main/AndroidManifest.xml"
file_utils = root / "limbo-android-lib/src/main/java/com/max2idea/android/limbo/files/FileUtils.java"
arm_activity = root / "limbo-android-arm/src/main/java/com/limbo/emu/main/arm/LimboEmuActivity.java"
importer_source = patch_root / "WildfireRomZipImporterActivity.java"
importer_target = root / "limbo-android-lib/src/main/java/com/max2idea/android/limbo/files/WildfireRomZipImporterActivity.java"
arm_source = patch_root / "WildfireArmEmuActivity.java"
arm_target = root / "limbo-android-x86/src/main/java/com/limbo/emu/main/arm/LimboEmuActivity.java"
home_source = patch_root / "WildfireHomeActivity.java"
home_target = root / "limbo-android-x86/src/main/java/com/limbo/emu/main/WildfireHomeActivity.java"
machine_service = root / "limbo-android-lib/src/main/java/com/max2idea/android/limbo/machine/MachineService.java"
sdl_activity = root / "limbo-android-lib/src/main/java/com/max2idea/android/limbo/main/LimboSDLActivity.java"

def replace_once(path, old, new):
    value = path.read_text()
    count = value.count(old)
    if count != 1:
        raise SystemExit(f"Expected one patch anchor in {path}, found {count}: {old[:100]}")
    path.write_text(value.replace(old, new, 1))

replace_once(
    activity,
    'public class LimboActivity extends AppCompatActivity\n        implements MachineController.OnMachineStatusChangeListener,\n        MachineController.OnEventListener, Observer {\n',
    'public class LimboActivity extends AppCompatActivity\n        implements MachineController.OnMachineStatusChangeListener,\n        MachineController.OnEventListener, Observer {\n\n    private static final int WILDFIRE_IMPORT_ROM_ZIP = 9876;\n'
)

replace_once(
    activity,
    '        menu.add(0, IMPORT_BIOS_FILE, 0, R.string.ImportBIOSFile).setIcon(R.drawable.importvms);\n',
    '        menu.add(0, IMPORT_BIOS_FILE, 0, R.string.ImportBIOSFile).setIcon(R.drawable.importvms);\n        menu.add(0, WILDFIRE_IMPORT_ROM_ZIP, 0, "Import ROM ZIP").setIcon(R.drawable.importvms);\n'
)

replace_once(
    activity,
    '        } else if (item.getItemId() == IMPORT_BIOS_FILE) {\n            BIOSImporter.promptImportBIOSFile(this);\n',
    '        } else if (item.getItemId() == WILDFIRE_IMPORT_ROM_ZIP) {\n            startActivity(new android.content.Intent(this, com.max2idea.android.limbo.files.WildfireRomZipImporterActivity.class));\n        } else if (item.getItemId() == IMPORT_BIOS_FILE) {\n            BIOSImporter.promptImportBIOSFile(this);\n'
)

replace_once(
    manifest,
    '        <activity\n            android:name="com.max2idea.android.limbo.main.LimboFileManager"',
    '        <activity\n            android:name="com.max2idea.android.limbo.files.WildfireRomZipImporterActivity"\n            android:label="Import ROM ZIP" />\n        <activity\n            android:name="com.max2idea.android.limbo.main.LimboFileManager"'
)

replace_once(
    file_utils,
    '                || ext.equals("vhd") || ext.equals("fs"))',
    '                || ext.equals("vhd") || ext.equals("vhdx") || ext.equals("raw") || ext.equals("fs"))'
)

replace_once(
    arm_activity,
    '        try {\n            System.loadLibrary("qemu-system-arm");\n        } catch (Error ex) {\n            System.loadLibrary("qemu-system-aarch64");\n        }\n',
    '        System.loadLibrary("qemu-system-aarch64");\n'
)

if not importer_source.is_file():
    raise SystemExit(f"Missing importer source: {importer_source}")
importer_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(importer_source, importer_target)

for source, target in ((arm_source, arm_target), (home_source, home_target)):
    if not source.is_file():
        raise SystemExit(f"Missing Wildfire activity source: {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)

replace_once(
    machine_service,
    'NotificationChannel chan = new NotificationChannel(Config.notificationChannelID, Config.notificationChannelName, NotificationManager.IMPORTANCE_NONE);',
    'NotificationChannel chan = new NotificationChannel("wildfire_lab_vm_v2", "Wildfire Lab VM", NotificationManager.IMPORTANCE_LOW);'
)

replace_once(
    machine_service,
    'builder = new NotificationCompat.Builder(service, Config.notificationChannelID);',
    'builder = new NotificationCompat.Builder(service, "wildfire_lab_vm_v2");'
)

replace_once(
    machine_service,
    'mWifiLock.setReferenceCounted(false);',
    'mWifiLock.setReferenceCounted(false);\n        if (!mWifiLock.isHeld()) {\n            mWifiLock.acquire();\n        }'
)

replace_once(
    machine_service,
    'mWakeLock.setReferenceCounted(false);',
    'mWakeLock.setReferenceCounted(false);\n        if (!mWakeLock.isHeld()) {\n            mWakeLock.acquire();\n        }'
)

replace_once(
    machine_service,
    '    public void onDestroy() {\n        Log.d(TAG, "Service destroyed");\n        super.onDestroy();\n    }',
    '    public void onDestroy() {\n        Log.d(TAG, "Service destroyed");\n        releaseLocks();\n        if (service == this) {\n            service = null;\n        }\n        super.onDestroy();\n    }'
)

replace_once(
    machine_service,
    '    public void onTaskRemoved(Intent intent) {\n        LimboSettingsManager.setExitCode(this, Config.EXIT_SUCCESS);\n    }',
    '    public void onTaskRemoved(Intent intent) {\n        LimboSettingsManager.setExitCode(this, Config.EXIT_SUCCESS);\n        if (MachineController.getInstance().isRunning()) {\n            updateServiceNotification("VM continues running in the background");\n        }\n    }'
)

replace_once(
    sdl_activity,
    '        if (MachineController.getInstance().isRunning())\n            notifyAction(MachineAction.UPDATE_NOTIFICATION,\n                    getString(R.string.VMSuspended));',
    '        if (MachineController.getInstance().isRunning())\n            notifyAction(MachineAction.UPDATE_NOTIFICATION,\n                    "VM continues running in the background");'
)

print("Applied Wildfire background service, synchronized display and ROM ZIP importer patches.")
