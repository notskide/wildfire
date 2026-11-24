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
    '        <activity\n            android:name="com.max2idea.android.limbo.files.WildfireRomZipImporterActivity"\n            android:exported="false"\n            android:label="Import ROM ZIP" />\n        <activity\n            android:name="com.max2idea.android.limbo.main.LimboFileManager"'
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

print("Applied Wildfire multi-architecture and ROM ZIP importer patches.")
