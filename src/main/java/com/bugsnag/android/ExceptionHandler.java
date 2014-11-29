package com.bugsnag.android;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashSet;
import java.util.Set;

class ExceptionHandler implements UncaughtExceptionHandler {
    private UncaughtExceptionHandler originalHandler;
    private Set<Client> clients = new HashSet<Client>();

    static void enable(Client client) {
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();

        // Find or create the Bugsnag ExceptionHandler
        ExceptionHandler bugsnagHandler;
        if(currentHandler instanceof ExceptionHandler) {
            bugsnagHandler = (ExceptionHandler)currentHandler;
        } else {
            bugsnagHandler = new ExceptionHandler(currentHandler);
            Thread.setDefaultUncaughtExceptionHandler(bugsnagHandler);
        }

        // Subscribe this client to uncaught exceptions
        bugsnagHandler.clients.add(client);
    }

    static void disable(Client client) {
        // Find tje Bugsnag ExceptionHandler
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if(currentHandler instanceof ExceptionHandler) {
            // Unsubscribe this client from uncaught exceptions
            ExceptionHandler bugsnagHandler = (ExceptionHandler)currentHandler;
            bugsnagHandler.clients.remove(client);

            // Remove the Bugsnag ExceptionHandler if no clients are subscribed
            if(bugsnagHandler.clients.size() == 0) {
                Thread.setDefaultUncaughtExceptionHandler(bugsnagHandler.originalHandler);
            }
        }
    }

    public ExceptionHandler(UncaughtExceptionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    public void uncaughtException(Thread t, Throwable e) {
        // Notify any subscribed clients of the uncaught exception
        for(Client client : clients) {
            client.notify(e, Severity.ERROR);
        }

        // Pass exception on to original exception handler
        if(originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        } else {
            System.err.printf("Exception in thread \"%s\" ", t.getName());
            e.printStackTrace(System.err);
        }
    }
}
