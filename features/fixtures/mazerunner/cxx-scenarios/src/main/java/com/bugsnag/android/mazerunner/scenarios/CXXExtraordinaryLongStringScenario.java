package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXExtraordinaryLongStringScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native int crash(int value);

    /**
     */
    public CXXExtraordinaryLongStringScenario(@NonNull Configuration config,
                                              @NonNull Context context,
                                              @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAppVersion("22.312.749.78.300.810.24.167.321.505.337.177.970.655.513.768.209"
                + ".616.429.5.654.552.117.275.422.698.110.941.6.611.737.439.489.121.879.119.207"
                + ".999.721.827.22.312.749.78.300.810.24.167.321.505.337.177.970.655.513.768.209"
                + ".616.429.5.654.552.117.275.422.698.110.941.6.611.737.439.489.121.879.119.207"
                + ".999.721.827");
        config.setContext("ObservableSessionInitializerStringParserStringSessionProxyGlobal"
                + "ServletUtilStringGlobalManagementObjectActivity");
    }

    @Override
    public void startScenario() {
        super.startScenario();
        crash(39383);
    }
}
