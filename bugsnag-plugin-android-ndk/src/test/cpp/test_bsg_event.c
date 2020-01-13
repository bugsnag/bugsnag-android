#include <greatest/greatest.h>
#include <event.h>
#include <utils/string.h>
#include "../../main/assets/include/bugsnag.h"

bugsnag_event *init_event() {
    bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
    bsg_strncpy_safe(event->context, "Foo", sizeof(event->context));
    return event;
}

TEST test_event_context(void) {
    bugsnag_event *event = init_event();
    ASSERT_STR_EQ("Foo", event->context);
    bugsnag_event_set_context(event, "SomeContext");
    ASSERT_STR_EQ("SomeContext", event->context);
    free(event);
    PASS();
}

SUITE(event_mutators) {
    RUN_TEST(test_event_context);
}
