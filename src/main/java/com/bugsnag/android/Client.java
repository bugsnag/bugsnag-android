package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

import com.bugsnag.Error;
import com.bugsnag.MetaData;
import com.bugsnag.Metrics;
import com.bugsnag.Notification;
import com.bugsnag.http.NetworkException;
import com.bugsnag.http.BadResponseException;
import com.bugsnag.android.utils.Async;

public class Client extends com.bugsnag.Client {
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";
    private static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "2.2.0";

    private Logger logger;
    private Context applicationContext;
    private String cachePath;

    public Client(Context androidContext, String apiKey, boolean enableMetrics) {
        super(apiKey);

        // Create a logger
        logger = new Logger();
        setLogger(logger);

        // Get the application context, many things need this
        applicationContext = androidContext.getApplicationContext();

        this.diagnostics = new Diagnostics(config, applicationContext, this);

        cachePath = prepareCachePath();

        // Set notifier info
        setNotifierName(NOTIFIER_NAME);
        setNotifierVersion(NOTIFIER_VERSION);

        // Send metrics data (DAU/MAU etc) if enabled
        if(enableMetrics) {
            //TODO:SM We should prevent this sending on rotate
            makeMetricsRequest();
        }

        // Flush any queued exceptions
        flushErrors();

        logger.info("Bugsnag is loaded and ready to handle exceptions");
    }

    public void notify(Throwable e, String severity, MetaData overrides) {
        try {
            if(!config.shouldNotify()) return;
            if(config.shouldIgnore(e.getClass().getName())) return;

            // Create the error object to send
            final Error error = createError(e, severity, overrides);

            // Run beforeNotify callbacks
            if(!beforeNotify(error)) return;

            // Send the error
            Async.safeAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        Notification notif = createNotification(error);
                        notif.deliver();
                    } catch (NetworkException ex) {
                        // Write error to disk for later sending
                        logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");
                        logger.info(ex.toString());
                        writeErrorToDisk(error);
                    }
                }
            });
        } catch(Exception ex) {
            logger.warn("Error notifying Bugsnag", ex);
        }
    }

    private void flushErrors() {
        if(cachePath == null) return;

        Async.safeAsync(new Runnable() {
            @Override
            public void run() {
                // Look up all saved error files
                File exceptionDir = new File(cachePath);
                if(exceptionDir.exists() && exceptionDir.isDirectory()) {
                    Notification notif = null;

                    for(File errorFile : exceptionDir.listFiles()) {
                        try {
                            if(notif == null) notif = createNotification();
                            notif.setError(errorFile);
                            notif.deliver();

                            logger.debug("Deleting sent error file " + errorFile.getName());
                            errorFile.delete();
                        } catch (NetworkException e) {
                            logger.warn("Could not send error(s) to Bugsnag, will try again later", e);
                        } catch (Exception e) {
                            logger.warn("Problem sending unsent error from disk", e);
                            errorFile.delete();
                        }
                    }
                }
            }
        });
    }

    public void setContext(Activity context) {
        String contextString = ActivityStack.getContextName(context);
        setContext(contextString);
    }

    public void setLogger(Logger logger) {
        super.setLogger(logger);
        Async.logger = logger;
    }

    private void makeMetricsRequest() {
        Async.safeAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Metrics metrics = createMetrics();
                    metrics.deliver();
                } catch (NetworkException ex) {
                    logger.info("Could not send metrics to Bugsnag");
                } catch (BadResponseException ex) {
                    // The notification was delivered, but Bugsnag sent a non-200 response
                    logger.warn(ex.getMessage());
                }
            }
        });
    }

    private String prepareCachePath() {
        String path = null;

        try {
            path = applicationContext.getCacheDir().getAbsolutePath() + UNSENT_ERROR_PATH;

            File outFile = new File(path);
            outFile.mkdirs();
            if(!outFile.exists()) {
                logger.warn("Could not prepare cache directory");
                path = null;
            }
        } catch(Exception e) {
            logger.warn("Could not prepare cache directory", e);
            path = null;
        }

        return path;
    }

    private void writeErrorToDisk(Error error) {
        if(cachePath == null || error == null) return;

        try {
            error.writeToFile(String.format("%s%d.json", cachePath, System.currentTimeMillis()));
        } catch (IOException e) {
            logger.warn("Unable to save bugsnag error", e);
        }
    }
}
