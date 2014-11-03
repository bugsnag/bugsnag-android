package com.bugsnag.android;

import java.lang.Thread.UncaughtExceptionHandler;

class ExceptionHandler implements UncaughtExceptionHandler {
    private UncaughtExceptionHandler originalHandler;
    private Client client;

    public static void install(Client client) {
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if(currentHandler instanceof ExceptionHandler) {
            currentHandler = ((ExceptionHandler)currentHandler).originalHandler;
        }

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(currentHandler, client));
    }

    public static void remove() {
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if(currentHandler instanceof ExceptionHandler) {
            Thread.setDefaultUncaughtExceptionHandler(((ExceptionHandler)currentHandler).originalHandler);
        }
    }

    public ExceptionHandler(UncaughtExceptionHandler originalHandler, Client client) {
        this.originalHandler = originalHandler;
        this.client = client;
    }

    public void uncaughtException(Thread t, Throwable e) {
        client.autoNotify(e);
        if(originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        } else {
            System.err.printf("Exception in thread \"%s\" ", t.getName());
            e.printStackTrace(System.err);
        }
    }
}
