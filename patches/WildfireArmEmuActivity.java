package com.limbo.emu.main.arm;

import android.os.Bundle;

import com.max2idea.android.limbo.log.Logger;
import com.max2idea.android.limbo.main.Config;
import com.max2idea.android.limbo.main.LimboActivity;
import com.max2idea.android.limbo.main.LimboApplication;

public class LimboEmuActivity extends LimboActivity {
    @Override
    public void onCreate(Bundle bundle) {
        LimboApplication.arch = Config.Arch.arm64;
        Config.clientClass = this.getClass();
        Config.enableKVM = false;
        Config.enableSDLSound = false;
        Config.enableSoftwareUpdates = false;
        Config.enableEmulatedFloppy = false;
        Config.enableEmulatedSDCard = true;
        if (LimboApplication.isHost64Bit() && Config.enableMTTCG) {
            Config.enableMTTCG = true;
        } else {
            Config.enableMTTCG = false;
        }
        Config.machineFolder = Config.machineFolder + "other/arm_machines/";
        super.onCreate(bundle);
        Logger.setupLogFile("/limbo/limbo-arm-log.txt");
    }

    @Override
    protected void loadQEMULib() {
        System.loadLibrary("qemu-system-aarch64");
    }
}
