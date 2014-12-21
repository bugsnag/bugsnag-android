package com.bugsnag.android;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import android.content.Context;

/**
 * Store and flush Error reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class ErrorStore {
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";

    private Configuration config;
    private String path;

    ErrorStore(Configuration config, Context appContext) {
        this.config = config;

        try {
            path = appContext.getCacheDir().getAbsolutePath() + UNSENT_ERROR_PATH;

            File outFile = new File(path);
            outFile.mkdirs();
            if(!outFile.exists()) {
                Logger.warn("Could not prepare error storage directory");
                path = null;
            }
        } catch(Exception e) {
            Logger.warn("Could not prepare error storage directory", e);
            path = null;
        }
    }

    // Flush any on-disk errors to Bugsnag
    void flush() {
        if(path == null) return;

        Async.run(new Runnable() {
            @Override
            public void run() {
                // Look up all saved error files
                File exceptionDir = new File(path);
                if(!exceptionDir.exists() || !exceptionDir.isDirectory()) return;

                File[] errorFiles = exceptionDir.listFiles();
                if(errorFiles.length > 0) {
                    Logger.info(String.format("Sending %d saved error(s) to Bugsnag", errorFiles.length));

                    for(File errorFile : errorFiles) {
                        try {
                            Notification notif = new Notification(config);
                            notif.addError(errorFile);
                            notif.deliver();

                            Logger.info("Deleting sent error file " + errorFile.getName());
                            errorFile.delete();
                        } catch (HttpClient.NetworkException e) {
                            Logger.warn("Could not send previously saved error(s) to Bugsnag, will try again later", e);
                        } catch (Exception e) {
                            Logger.warn("Problem sending unsent error from disk", e);
                            errorFile.delete();
                        }
                    }
                }
            }
        });
    }

    // Write an error to disk, for later sending
    void write(Error error) {
        if(path == null) return;

        String filename = String.format("%s%d.json", path, System.currentTimeMillis());
        try {
            Writer out = new FileWriter(filename);
            new JsonStream(out).value(error).close();

            Logger.info(String.format("Saved unsent error to disk (%s) ", filename));
        } catch (Exception e) {
            Logger.warn(String.format("Couldn't save unsent error to disk (%s) ", filename), e);
        }
    }
}
