package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
    private Collection<String> projectPackages = new ArrayList<>();

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

        if (exc instanceof JsonStream.Streamable) {
            this.streamable = (JsonStream.Streamable) exc;
            this.name = "";
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
            List<Map<String, Object>> frames = customStackframes;
            Stacktrace stacktrace;
            // if customStackFrames is set on BugsnagException we are reading a cached file
            // which may contain additional fields, such as columnNumber/loadAddress etc.
            // in this case we should construct the StackTrace with the arbitrary map supplied.
            if (frames != null) {
                stacktrace = new Stacktrace(frames);
            } else {
                stacktrace = new Stacktrace(getStackTrace(), this.projectPackages);
            }

            stream.beginObject();
            stream.name("errorClass").value(getName());
            stream.name("message").value(getLocalizedMessage());
            stream.name("type").value(type);
            stream.name("stacktrace").value(stacktrace);
            stream.endObject();
        }
    }

    void setProjectPackages(Collection<String> projectPackages) {
        this.projectPackages = projectPackages;
    }
}
