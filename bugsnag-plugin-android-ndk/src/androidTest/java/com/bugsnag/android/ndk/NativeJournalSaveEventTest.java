package com.bugsnag.android.ndk;

import static com.bugsnag.android.ndk.VerifyUtilsKt.verifyNativeRun;

import com.bugsnag.android.BugsnagJournal;
import com.bugsnag.android.NoopLogger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NativeJournalSaveEventTest {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("bugsnag-ndk-test");
    }

    public native int run(String path);

    @Test
    public void testPassesNativeSuite() throws Exception {
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        File folderPath = folder.newFolder();
        File journalPath = new File(folderPath, "bsg-ct-event-test");
        BugsnagJournal journal = new BugsnagJournal(NoopLogger.INSTANCE, journalPath);
        journal.snapshot();
        verifyNativeRun(run(folderPath.toString()));

        Map<? super String, ?> document = BugsnagJournal.loadPreviousDocument(journalPath);
        folder.delete();

        // This is what is expected to be written by test_journal_save_event.c: test_write_event()
        Map<? super String, Object> expected = getExpectedContents();
        Assert.assertEquals(
                BugsnagTestUtils.normalized(expected),
                BugsnagTestUtils.normalized(document)
        );
    }

    private Map<? super String, Object> getExpectedContents() {
        Map<? super String, Object> root = new HashMap<>();

        // version info
        Map<? super String, Object> versionInfo = new HashMap<>();
        root.put("version-info", versionInfo);
        versionInfo.put("type", "Bugsnag Android");
        versionInfo.put("version", 1);

        // exceptions
        List<Object> exceptions = new LinkedList<>();
        root.put("exceptions", exceptions);
        Map<? super String, Object> exception = new HashMap<>();
        exceptions.add(exception);
        exception.put("message", "test message");
        exception.put("type", "c");
        exception.put("errorClass", "SIGSEGV");

        // stacktrace
        List<Object> stackTrace = new LinkedList<>();
        exception.put("stacktrace", stackTrace);
        Map<? super String, Object> traceEntry = new HashMap<>();
        stackTrace.add(traceEntry);
        traceEntry.put("frameAddress", "0x0");
        traceEntry.put("loadAddress", "0x1000");
        traceEntry.put("symbolAddress", "0x2000");
        traceEntry.put("file", "file_0.c");
        traceEntry.put("method", "method_0");
        traceEntry.put("lineNumber", new BigDecimal("1E+2"));
        traceEntry.put("isPC", true);

        traceEntry = new HashMap<>();
        stackTrace.add(traceEntry);
        traceEntry.put("frameAddress", "0x1");
        traceEntry.put("loadAddress", "0x1001");
        traceEntry.put("symbolAddress", "0x2001");
        traceEntry.put("file", "file_1.c");
        traceEntry.put("method", "method_1");
        traceEntry.put("lineNumber", new BigDecimal("101"));

        // severity reason
        root.put("severity", "error");
        root.put("unhandled", true);

        Map<String, Object> severityReason = new HashMap<>();
        root.put("severityReason", severityReason);
        severityReason.put("unhandledOverridden", false);
        severityReason.put("type", "signal");
        severityReason.put("attributes", Collections.singletonMap(
                "signalType", "SIGSEGV"
        ));

        // device time
        HashMap<String, Object> deviceMap = new HashMap<>();
        deviceMap.put("time", "1974-10-03T02:40:00Z");
        root.put("device", deviceMap);

        // session
        HashMap<String, Object> sessionMap = new HashMap<>();
        HashMap<String, Object> eventsMap = new HashMap<>();
        root.put("session", sessionMap);
        sessionMap.put("events", eventsMap);
        eventsMap.put("unhandled", 1);
        return root;
    }
}
