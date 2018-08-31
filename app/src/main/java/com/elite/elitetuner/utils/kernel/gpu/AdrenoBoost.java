package com.elite.elitetuner.utils.kernel.gpu;

import android.content.Context;

import com.elite.elitetuner.R;
import com.elite.elitetuner.fragments.ApplyOnBootFragment;
import com.elite.elitetuner.utils.Utils;
import com.elite.elitetuner.utils.root.Control;

public class AdrenoBoost {

    private static final String ADRENOBOOST = "/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost";

    public static String getAdrenoBoost(Context context) {
        int mode = Utils.strToInt(Utils.readFile(ADRENOBOOST));
        switch (mode){
            case 0:
                return context.getString(R.string.gpu_adreno_boost_off);
            case 1:
                return context.getString(R.string.gpu_adreno_boost_low);
            case 2:
                return context.getString(R.string.gpu_adreno_boost_medium);
            case 3:
                return context.getString(R.string.gpu_adreno_boost_high);
        }
        return null;
    }

    public static void setAdrenoBoost(int value, Context context) {
        run(Control.write(String.valueOf(value), ADRENOBOOST), ADRENOBOOST, context);
    }

    public static boolean hasAdrenoBoost() {
        return Utils.existFile(ADRENOBOOST);
    }

    private static void run(String command, String id, Context context) {
        Control.runSetting(command, ApplyOnBootFragment.GPU, id, context);
    }
}
