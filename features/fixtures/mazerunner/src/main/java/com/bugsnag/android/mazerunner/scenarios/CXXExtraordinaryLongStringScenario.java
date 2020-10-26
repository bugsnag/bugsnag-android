package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class CXXExtraordinaryLongStringScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native int crash(int value);

    /**
     */
    public CXXExtraordinaryLongStringScenario(@NonNull Configuration config,
                                              @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
        config.setAppVersion("22.312.749.78.300.810.24.167.321.505.337.177.970.655.513.768.209"
                + ".616.429.5.654.552.117.275.422.698.110.941.6.611.737.439.489.121.879.119.207"
                + ".999.721.827.22.312.749.78.300.810.24.167.321.505.337.177.970.655.513.768.209"
                + ".616.429.5.654.552.117.275.422.698.110.941.6.611.737.439.489.121.879.119.207"
                + ".999.721.827");
        config.setContext("ObservableSessionInitializerStringParserStringSessionProxyGlobal"
                + "ServletUtilStringGlobalManagementObjectActivity");
    }

    @Override
    public void run() {
        super.run();
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        crash(39383);
    }
}
