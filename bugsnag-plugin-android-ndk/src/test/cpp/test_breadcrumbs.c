#include <greatest/greatest.h>
#include <event.h>
#include <time.h>

bugsnag_breadcrumb *init_breadcrumb(const char *name, const char *message, bsg_breadcrumb_t type) {
  bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
  crumb->type = type;
  strcpy(crumb->name, name);
  strcpy(crumb->timestamp, "2018-08-29T21:41:39Z");
  strcpy(crumb->metadata[0].key, "message");
  strcpy(crumb->metadata[0].value, message);

  return crumb;
}

TEST test_add_breadcrumb(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  bugsnag_breadcrumb *crumb = init_breadcrumb("stroll", "this is a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb);
  ASSERT_EQ(1, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  ASSERT(strcmp("stroll", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[0].metadata[0].key) == 0);
  ASSERT(strcmp("this is a drill.", event->breadcrumbs[0].metadata[0].value) == 0);
  free(crumb);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("walking...", "this is not a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb2);
  ASSERT_EQ(2, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  ASSERT(strcmp("stroll", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[0].metadata[0].key) == 0);
  ASSERT(strcmp("this is a drill.", event->breadcrumbs[0].metadata[0].value) == 0);
  ASSERT(strcmp("walking...", event->breadcrumbs[1].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[1].metadata[0].key) == 0);
  ASSERT(strcmp("this is not a drill.", event->breadcrumbs[1].metadata[0].value) == 0);

  free(event);
  free(crumb2);
  PASS();
}

TEST test_add_breadcrumbs_over_max(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  // HACK: assumes the max number of crumbs is 30
  for (int i=0; i < 64; i++) {
    char *format = malloc(sizeof(char) * 64);
    memset(format, 0, sizeof(char) * 64);
    sprintf(format, "crumb: %d", i);
    bugsnag_breadcrumb *crumb = init_breadcrumb(format, "go go go", BSG_CRUMB_USER);
    bugsnag_event_add_breadcrumb(event, crumb);
    free(crumb);
    free(format);
  }
  ASSERT(strcmp("crumb: 60", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("crumb: 61", event->breadcrumbs[1].name) == 0);
  ASSERT(strcmp("crumb: 62", event->breadcrumbs[2].name) == 0);
  ASSERT(strcmp("crumb: 63", event->breadcrumbs[3].name) == 0);
  ASSERT(strcmp("crumb: 34", event->breadcrumbs[4].name) == 0);
  ASSERT(strcmp("crumb: 35", event->breadcrumbs[5].name) == 0);
  ASSERT(strcmp("crumb: 58", event->breadcrumbs[28].name) == 0);
  ASSERT(strcmp("crumb: 59", event->breadcrumbs[29].name) == 0);
  ASSERT_EQ(BUGSNAG_CRUMBS_MAX, event->crumb_count);
  ASSERT_EQ(4, event->crumb_first_index);
  free(event);
  PASS();
}

TEST test_clear_empty_breadcrumbs(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  bugsnag_event_clear_breadcrumbs(event);
  ASSERT_EQ(0, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  PASS();
}

TEST test_clear_breadcrumbs(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  bugsnag_breadcrumb *crumb1 = init_breadcrumb("running!", "this is a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb1);
  free(crumb1);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("walking...", "this is not a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb2);
  bugsnag_event_clear_breadcrumbs(event);
  ASSERT_EQ(0, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);

  free(event);
  free(crumb2);
  PASS();
}

SUITE(breadcrumbs) {
  RUN_TEST(test_add_breadcrumb);
  RUN_TEST(test_add_breadcrumbs_over_max);
  RUN_TEST(test_clear_empty_breadcrumbs);
  RUN_TEST(test_clear_breadcrumbs);
}
