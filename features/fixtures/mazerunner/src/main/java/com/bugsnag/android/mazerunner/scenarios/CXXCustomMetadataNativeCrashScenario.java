package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Bugsnag;

import android.support.annotation.NonNull;

import java.lang.reflect.Array;

public class CXXCustomMetadataNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native int activate();

    public CXXCustomMetadataNativeCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addToTab("Riker Ipsum", "examples", "I'll be sure to note that in my log. You enjoyed that. They were just sucked into space. How long can two people talk about nothing? I've had twelve years to think about it. And if I had it to do over again, I would have grabbed the phaser and pointed it at you instead of them.");
        Bugsnag.addToTab("fruit", "apple", "gala");
        Bugsnag.addToTab("fruit", "counters", 47);
        Bugsnag.addToTab("fruit", "ripe", true);
        int value = activate();
        System.out.println("The result: " + value);
    }
}
