package com.bugsnag.android;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.content.Context;

/**
 * Store and flush Error reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class ErrorStore {
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";
    private static final int MAX_STORED_ERRORS = 100;

    final Configuration config;
    final String path;

    ErrorStore(Configuration config, Context appContext) {
        this.config = config;

        String path;
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
        this.path = path;
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
                if(errorFiles != null && errorFiles.length > 0) {
                    Logger.info(String.format(Locale.US, "Sending %d saved error(s) to Bugsnag", errorFiles.length));

                    for(File errorFile : errorFiles) {
                        try {
                            Report report = new Report(config.getApiKey(), errorFile);
                            HttpClient.post(config.getEndpoint(), report);

                            Logger.info("Deleting sent error file " + errorFile.getName());
                            if (!errorFile.delete())
                                errorFile.deleteOnExit();
                        } catch (HttpClient.NetworkException e) {
                            Logger.warn("Could not send previously saved error(s) to Bugsnag, will try again later", e);
                        } catch (Exception e) {
                            Logger.warn("Problem sending unsent error from disk", e);
                            if (!errorFile.delete())
                                errorFile.deleteOnExit();
                        }
                    }
                }
            }
        });
    }

    // Write an error to disk, for later sending
    void write(Error error) {
        if(path == null) return;

        // Limit number of saved errors to prevent disk space issues
        File exceptionDir = new File(path);
        if (exceptionDir.isDirectory()) {
            File[] files = exceptionDir.listFiles();
            if (files.length >= MAX_STORED_ERRORS) {
                // Sort files then delete the first one (oldest timestamp)
                Arrays.sort(files);
                Logger.warn(String.format("Discarding oldest error as stored error limit reached (%s)", files[0].getPath()));
                if (!files[0].delete()) {
                    files[0].deleteOnExit();
                }
            }
        }

        String filename = String.format(Locale.US, "%s%d.json", path, System.currentTimeMillis());
        Writer out = null;
        try {
            out = new FileWriter(filename);

            JsonStream stream = new JsonStream(out);
            stream.value(error);
            stream.close();

            Logger.info(String.format("Saved unsent error to disk (%s) ", filename));
        } catch (Exception e) {
            Logger.warn(String.format("Couldn't save unsent error to disk (%s) ", filename), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
