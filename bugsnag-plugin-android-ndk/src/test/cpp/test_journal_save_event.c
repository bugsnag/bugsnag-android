#include <greatest/greatest.h>
#include <fcntl.h>
#include <unistd.h>
#include "crashtime_journal.h"
#include "test_helpers.h"
#include "bugsnag_ndk.h"

void bsg_ctjournal_test_reset();
static char crashtime_journal_filename[PATH_MAX];

static bool init_test() {
    sprintf(crashtime_journal_filename, "%s/bsg-ct-event-test.journal.crashtime", test_temporary_folder_path);
    bsg_ctjournal_test_reset();
    return bsg_crashtime_journal_init(crashtime_journal_filename);
}

TEST test_write_event(void) {
    ASSERT(init_test());
    bugsnag_event event;
    event.severity = BSG_SEVERITY_ERR;
    event.unhandled = true;
    event.unhandled_events = 1;
    event.device.time = 150000000;

    // error
    bsg_error *error = &event.error;
    strcpy(error->errorMessage, "test message");
    strcpy(error->errorClass, "SIGSEGV");
    strcpy(error->type, "c");
    error->frame_count = 2;

    for (int i = 0; i < error->frame_count; i++) {
        bugsnag_stackframe *frame = &error->stacktrace[i];
        frame->frame_address = 0x0000 + i;
        frame->load_address = 0x1000 + i;
        frame->symbol_address = 0x2000 + i;
        frame->line_number = 100 + i;
        sprintf(frame->filename, "file_%d.c", i);
        sprintf(frame->method, "method_%d", i);
    }

    // threads
    event.thread_count = 2;
    strcpy(event.threads[0].name, "ConnectivityThr");
    event.threads[0].id = 29695;
    strcpy(event.threads[0].state, "running");
    strcpy(event.threads[1].name, "Binder:29227_3");
    event.threads[1].id = 29698;
    strcpy(event.threads[1].state, "sleeping");
    bsg_crashtime_journal_store_event(&event);
    PASS();
}

SUITE(suite_journal_save_event) {
    RUN_TEST(test_write_event);
}
