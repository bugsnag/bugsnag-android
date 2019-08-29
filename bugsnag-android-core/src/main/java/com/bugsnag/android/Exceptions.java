package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Unwrap and serialize exception information and any "cause" exceptions.
 */
class Exceptions implements JsonStream.Streamable { // TODO delete redundant class
    private final BugsnagException exception;
    private String exceptionType;
    private String[] projectPackages;

    Exceptions(Configuration config, BugsnagException exception) {
        this.exception = exception;
        exceptionType = exception.getType();
        projectPackages = config.getProjectPackages();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        exception.toStream(writer);
    }

    BugsnagException getException() {
        return exception;
    }

    String getExceptionType() {
        return exceptionType;
    }

    void setExceptionType(@NonNull String type) {
        exceptionType = type;
    }

    String[] getProjectPackages() {
        return projectPackages;
    }

    void setProjectPackages(String[] projectPackages) {
        this.projectPackages = projectPackages;
    }
}
