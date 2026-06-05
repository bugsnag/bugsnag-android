package com.bugsnag.android;

import com.bugsnag.android.internal.TaskType;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

class LibraryLoader {

    static final class LoadLibraryReport {
        final boolean loaded;
        final boolean retried;
        final boolean retrySucceeded;
        final String firstErrorClass;
        final String firstErrorMessage;
        final String secondErrorClass;
        final String secondErrorMessage;
        final long queueWaitNs;
        final long nativeLoadNs;
        final long callerBlockedNs;

        LoadLibraryReport(
            boolean loaded,
            boolean retried,
            boolean retrySucceeded,
            String firstErrorClass,
            String firstErrorMessage,
            String secondErrorClass,
            String secondErrorMessage,
            long queueWaitNs,
            long nativeLoadNs,
            long callerBlockedNs
        ) {
            this.loaded = loaded;
            this.retried = retried;
            this.retrySucceeded = retrySucceeded;
            this.firstErrorClass = firstErrorClass;
            this.firstErrorMessage = firstErrorMessage;
            this.secondErrorClass = secondErrorClass;
            this.secondErrorMessage = secondErrorMessage;
            this.queueWaitNs = queueWaitNs;
            this.nativeLoadNs = nativeLoadNs;
            this.callerBlockedNs = callerBlockedNs;
        }

        String getFinalErrorClass() {
            return secondErrorClass != null ? secondErrorClass : firstErrorClass;
        }

        String getFinalErrorMessage() {
            return secondErrorMessage != null ? secondErrorMessage : firstErrorMessage;
        }
    }

    static final class ResolveLibraryPathReport {
        final String mappedLibraryName;
        final String resolvedPath;
        final String pathSource;
        final boolean definitive;
        final boolean fileExists;
        final Long fileSizeBytes;

        ResolveLibraryPathReport(
            String mappedLibraryName,
            String resolvedPath,
            String pathSource,
            boolean definitive,
            boolean fileExists,
            Long fileSizeBytes
        ) {
            this.mappedLibraryName = mappedLibraryName;
            this.resolvedPath = resolvedPath;
            this.pathSource = pathSource;
            this.definitive = definitive;
            this.fileExists = fileExists;
            this.fileSizeBytes = fileSizeBytes;
        }
    }

    private final AtomicBoolean attemptedLoad = new AtomicBoolean();
    private boolean loaded = false;

    /**
     * Attempts to load a native library, returning false if the load was unsuccessful.
     * <p>
     * If a load was attempted and failed, an error report will be sent using the supplied client
     * and OnErrorCallback.
     *
     * @param name     the library name
     * @param client   the bugsnag client
     * @param callback an OnErrorCallback
     * @return true if the library was loaded, false if not
     */
    boolean loadLibrary(final String name, final Client client, final OnErrorCallback callback) {
        return loadLibraryWithDiagnostics(name, client, callback).loaded;
    }

    LoadLibraryReport loadLibraryWithDiagnostics(
        final String name,
        final Client client,
        final OnErrorCallback callback
    ) {
        final long callerStartNs = System.nanoTime();
        final long submitNs = System.nanoTime();
        final LoadLibraryReport[] reportHolder = new LoadLibraryReport[1];
        try {
            client.bgTaskService.submitTask(TaskType.IO, new Runnable() {
                @Override
                public void run() {
                    long runStartNs = System.nanoTime();
                    LoadLibraryReport loadReport = loadLibInternalWithDiagnostics(
                        name,
                        client,
                        callback
                    );
                    long runEndNs = System.nanoTime();

                    reportHolder[0] = new LoadLibraryReport(
                        loadReport.loaded,
                        loadReport.retried,
                        loadReport.retrySucceeded,
                        loadReport.firstErrorClass,
                        loadReport.firstErrorMessage,
                        loadReport.secondErrorClass,
                        loadReport.secondErrorMessage,
                        Math.max(0L, runStartNs - submitNs),
                        loadReport.nativeLoadNs,
                        Math.max(0L, runEndNs - callerStartNs)
                    );
                }
            }).get();
            return reportHolder[0] != null
                ? reportHolder[0]
                : new LoadLibraryReport(loaded, false, false, null, null, null, null, 0L, 0L, 0L);
        } catch (Throwable exc) {
            return new LoadLibraryReport(
                false,
                false,
                false,
                exc.getClass().getName(),
                exc.getMessage(),
                null,
                null,
                0L,
                0L,
                Math.max(0L, System.nanoTime() - callerStartNs)
            );
        }
    }

    void loadLibInternal(String name, Client client, OnErrorCallback callback) {
        loadLibInternalWithDiagnostics(name, client, callback);
    }

    LoadLibraryReport loadLibInternalWithDiagnostics(
        String name,
        Client client,
        OnErrorCallback callback
    ) {
        long loadStartNs = System.nanoTime();
        boolean retried = false;
        boolean retrySucceeded = false;
        String firstErrorClass = null;
        String firstErrorMessage = null;
        String secondErrorClass = null;
        String secondErrorMessage = null;

        if (!attemptedLoad.getAndSet(true)) {
            try {
                System.loadLibrary(name);
                loaded = true;
            } catch (UnsatisfiedLinkError firstError) {
                retried = true;
                firstErrorClass = firstError.getClass().getName();
                firstErrorMessage = firstError.getMessage();
                // retry once in case the failure wasn't permanent
                try {
                    System.loadLibrary(name);
                    loaded = true;
                    retrySucceeded = true;
                } catch (UnsatisfiedLinkError secondError) {
                    secondErrorClass = secondError.getClass().getName();
                    secondErrorMessage = secondError.getMessage();
                    client.notify(secondError, callback);
                }
            }
        }

        long loadEndNs = System.nanoTime();
        return new LoadLibraryReport(
            loaded,
            retried,
            retrySucceeded,
            firstErrorClass,
            firstErrorMessage,
            secondErrorClass,
            secondErrorMessage,
            0L,
            Math.max(0L, loadEndNs - loadStartNs),
            0L
        );
    }

    public String resolveLibraryPath(String name, Client client) {
        return resolveLibraryPathDetails(name, client).resolvedPath;
    }

    ResolveLibraryPathReport resolveLibraryPathDetails(String name, Client client) {
        String mappedName = System.mapLibraryName(name);
        String resolvedPath = mappedName;
        String pathSource = "mapped_name_fallback";
        boolean definitive = false;

        try {
            String nativeLibraryDir = client.appContext.getApplicationInfo().nativeLibraryDir;
            if (nativeLibraryDir != null) {
                resolvedPath = new File(nativeLibraryDir, mappedName).getAbsolutePath();
                pathSource = "application_info.nativeLibraryDir";
                definitive = true;
            }
        } catch (Throwable ignored) {
            // Best-effort diagnostics only; load behavior remains unchanged.
        }

        File file = new File(resolvedPath);
        boolean fileExists = file.exists();
        Long fileSizeBytes = fileExists ? file.length() : null;

        return new ResolveLibraryPathReport(
            mappedName,
            resolvedPath,
            pathSource,
            definitive,
            fileExists,
            fileSizeBytes
        );
    }

    boolean isLoaded() {
        return loaded;
    }
}
