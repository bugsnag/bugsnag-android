package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Error implements JsonStream.Streamable {

    private final ErrorImpl impl;
    private final Logger logger;

    Error(@NonNull ErrorImpl impl,
          @NonNull Logger logger) {
        this.impl = impl;
        this.logger = logger;
    }

    private void error(String property) {
        logger.e("Invalid null value supplied to error." + property + ", ignoring");
    }

    public void setErrorClass(@NonNull String errorClass) {
        if (errorClass != null) {
            impl.setErrorClass(errorClass);
        } else {
            error("errorClass");
        }
    }

    @NonNull
    public String getErrorClass() {
        return impl.getErrorClass();
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        impl.setErrorMessage(errorMessage);
    }

    @Nullable
    public String getErrorMessage() {
        return impl.getErrorMessage();
    }

    public void setType(@NonNull ErrorType type) {
        if (type != null) {
            impl.setType(type);
        } else {
            error("type");
        }
    }

    @NonNull
    public ErrorType getType() {
        return impl.getType();
    }

    @NonNull
    public List<Stackframe> getStacktrace() {
        return impl.getStacktrace();
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        impl.toStream(stream);
    }

    static List<Error> createError(@NonNull Throwable exc,
                                   @NonNull Collection<String> projectPackages,
                                   @NonNull Logger logger) {
        return ErrorImpl.Companion.createError(exc, projectPackages, logger);
    }
}
