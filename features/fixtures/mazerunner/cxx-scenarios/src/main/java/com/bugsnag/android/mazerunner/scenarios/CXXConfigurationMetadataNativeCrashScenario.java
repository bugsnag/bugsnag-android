package com.bugsnag.android.mazerunner.scenarios;

import static java.lang.Math.PI;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CXXConfigurationMetadataNativeCrashScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native int activate();

    /**
     */
    public CXXConfigurationMetadataNativeCrashScenario(@NonNull Configuration config,
                                                       @NonNull Context context,
                                                       @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        String metadata = getEventMetadata();
        if (metadata == null || !metadata.equals("no-metadata")) {
            config.addMetadata("fruit", "apple", "gala");
            config.addMetadata("fruit", "counters", 47);
            config.addMetadata("fruit", "ripe", true);

            config.addMetadata("complex", "message",
                    "That might've been one of the shortest assignments in the history of "
                            + "Starfleet. The Enterprise computer system is controlled by three "
                            + "primary main processor cores, cross-linked with a redundant "
                            + "melacortz ramistat, fourteen kiloquad interface modules.");

            Map<String, Object> map = new HashMap<>();
            map.put("location", "you are here");
            map.put("inventory", Arrays.asList(
                    "lots of string",
                    PI,
                    true
            ));

            config.addMetadata("complex", "maps", map);
            config.addMetadata("complex", "list", Arrays.asList(
                    "summer",
                    "winter",
                    "spring",
                    "autumn"
            ));

            Set<String> set = new LinkedHashSet<>();
            set.add("value1");
            set.add("2value");
            config.addMetadata("complex", "set", set);
        }
    }

    @Override
    public void startScenario() {
        super.startScenario();
        int value = activate();
        System.out.println("The result: " + value);
    }
}
