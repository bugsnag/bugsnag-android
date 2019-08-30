package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Used to store information about an exception that was not provided with an exception object
 */
@ThreadSafe
public class BugsnagException extends Throwable implements JsonStream.Streamable {

    private static final long serialVersionUID = 5068182621179433346L;
    /**
     * The name of the exception (used instead of the exception class)
     */
    private String name;
    private String message;
    private final List<Map<String, Object>> customStackframes;

    private String type = Configuration.DEFAULT_EXCEPTION_TYPE;

    private JsonStream.Streamable streamable;
    private String[] projectPackages;

    /**
     * Constructor
     *
     * @param name    The name of the exception (used instead of the exception class)
     * @param message The exception message
     * @param frames  The exception stack trace
     */
    public BugsnagException(@NonNull String name,
                            @NonNull String message,
                            @NonNull StackTraceElement[] frames) {
        super(message);
        setStackTrace(frames);
        this.name = name;
        this.customStackframes = null;
    }

    BugsnagException(@NonNull Throwable exc) {
        super(exc.getMessage());

        if (exc instanceof BugsnagException) {
            this.message = ((BugsnagException) exc).getMessage();
            this.name = ((BugsnagException) exc).getName();
            this.type = ((BugsnagException) exc).getType();
        } else if (exc instanceof JsonStream.Streamable) {
            this.streamable = (JsonStream.Streamable) exc;
        } else {
            this.name = exc.getClass().getName();
        }
        setStackTrace(exc.getStackTrace());
        initCause(exc.getCause());
        this.customStackframes = null;
    }

    BugsnagException(@NonNull String name,
                     @NonNull String message,
                     @NonNull List<Map<String, Object>> customStackframes) {
        super(message);
        setStackTrace(new StackTraceElement[]{});
        this.name = name;
        this.customStackframes = customStackframes;
    }

    /**
     * @return The name of the exception (used instead of the exception class)
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the error displayed in the bugsnag dashboard
     *
     * @param name the new name
     */
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /**
     * @return The error message, which is the exception message by default
     */
    @NonNull
    public String getMessage() {
        return message != null ? message : super.getMessage();
    }

    /**
     * Sets the message of the error displayed in the bugsnag dashboard
     *
     * @param message the new message
     */
    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    @NonNull
    String getType() {
        return type;
    }

    void setType(@NonNull String type) {
        this.type = type;
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {

        // the class has been passed a custom exception such as JavaScriptException in React Native
        // if this value is not null. These classes currently handle their own serialization
        // so we delegate to them
        if (streamable != null) {
            streamable.toStream(stream);
        } else {
            // iterate through the exceptions and serialise an array, starting with the current
            // throwable and continuing until the cause is null
            stream.beginArray();

            Throwable currentEx = this;
            while (currentEx != null) {
                serializeException(stream, currentEx);
                currentEx = currentEx.getCause();
            }
            stream.endArray();
        }
    }

    private void serializeException(@NonNull JsonStream writer,
                                    Throwable currentEx) throws IOException {
        if (currentEx instanceof BugsnagException) {
            List<Map<String, Object>> frames = ((BugsnagException) currentEx).customStackframes;

            // if customStackFrames is set on BugsnagException we are reading a cached file
            // which may contain additional fields, such as columnNumber/loadAddress etc.
            // in this case we should construct the StackTrace with the arbitrary map supplied.
            if (frames != null) {
                String exceptionName = getExceptionName(currentEx);
                String localizedMessage = currentEx.getLocalizedMessage();
                Stacktrace stacktrace = new Stacktrace(frames);
                exceptionToStream(writer, exceptionName, localizedMessage, stacktrace);
            } else {
                writeThrowable(writer, currentEx);
            }
        } else if (currentEx instanceof JsonStream.Streamable) {
            ((JsonStream.Streamable) currentEx).toStream(writer);
        } else {
            writeThrowable(writer, currentEx);
        }
    }

    private void writeThrowable(@NonNull JsonStream writer,
                                Throwable currentEx) throws IOException {
        String exceptionName = getExceptionName(currentEx);
        String localizedMessage = currentEx.getLocalizedMessage();
        Stacktrace stacktrace = new Stacktrace(currentEx.getStackTrace(), this.projectPackages);
        exceptionToStream(writer, exceptionName, localizedMessage, stacktrace);
    }

    /**
     * Get the class name from the exception contained in this Error report.
     */
    private String getExceptionName(@NonNull Throwable throwable) {
        if (throwable instanceof BugsnagException) {
            return ((BugsnagException) throwable).getName();
        } else {
            return throwable.getClass().getName();
        }
    }

    private void exceptionToStream(@NonNull JsonStream writer,
                                   String name,
                                   String message,
                                   Stacktrace stacktrace) throws IOException {
        writer.beginObject();
        writer.name("errorClass").value(name);
        writer.name("message").value(message);
        writer.name("type").value(type);
        writer.name("stacktrace").value(stacktrace);
        writer.endObject();
    }

    void setProjectPackages(String[] projectPackages) {
        this.projectPackages = projectPackages;
    }
}
