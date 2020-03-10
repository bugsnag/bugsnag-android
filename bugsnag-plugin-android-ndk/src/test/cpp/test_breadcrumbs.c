#include <greatest/greatest.h>
#include <event.h>
#include <time.h>

bugsnag_breadcrumb *init_breadcrumb(const char *name, char *message, bugsnag_breadcrumb_type type) {
  bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
  crumb->type = type;
  strcpy(crumb->name, name);
  strcpy(crumb->timestamp, "2018-08-29T21:41:39Z");
  bsg_add_metadata_value_str(&crumb->metadata, "metaData", "message", message);
  return crumb;
}

TEST test_add_breadcrumb(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  bugsnag_breadcrumb *crumb = init_breadcrumb("stroll", "this is a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb);
  ASSERT_EQ(1, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  ASSERT(strcmp("stroll", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[0].metadata.values[0].name) == 0);
  ASSERT(strcmp("this is a drill.", event->breadcrumbs[0].metadata.values[0].char_value) == 0);
  free(crumb);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("walking...", "this is not a drill.", BSG_CRUMB_USER);
  bugsnag_event_add_breadcrumb(event, crumb2);
  ASSERT_EQ(2, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  ASSERT(strcmp("stroll", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[0].metadata.values[0].name) == 0);
  ASSERT(strcmp("this is a drill.", event->breadcrumbs[0].metadata.values[0].char_value) == 0);
  ASSERT(strcmp("walking...", event->breadcrumbs[1].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[1].metadata.values[0].name) == 0);
  ASSERT(strcmp("this is not a drill.", event->breadcrumbs[1].metadata.values[0].char_value) == 0);

  free(event);
  free(crumb2);
  PASS();
}

TEST test_add_breadcrumbs_over_max(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  int breadcrumb_count = 64;

  for (int i=0; i < breadcrumb_count; i++) {
    char *format = malloc(sizeof(char) * breadcrumb_count);
    memset(format, 0, sizeof(char) * breadcrumb_count);
    sprintf(format, "crumb: %d", i);
    bugsnag_breadcrumb *crumb = init_breadcrumb(format, "go go go", BSG_CRUMB_USER);
    bugsnag_event_add_breadcrumb(event, crumb);
    free(crumb);
    free(format);
  }

  // assertions assume that the crumb count is always 25
  ASSERT_EQ(BUGSNAG_CRUMBS_MAX, event->crumb_count);
  ASSERT_EQ(14, event->crumb_first_index);

  ASSERT(strcmp("crumb: 50", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("crumb: 51", event->breadcrumbs[1].name) == 0);
  ASSERT(strcmp("crumb: 52", event->breadcrumbs[2].name) == 0);
  ASSERT(strcmp("crumb: 53", event->breadcrumbs[3].name) == 0);

  ASSERT(strcmp("crumb: 63", event->breadcrumbs[13].name) == 0);
  ASSERT(strcmp("crumb: 39", event->breadcrumbs[14].name) == 0);
  ASSERT(strcmp("crumb: 40", event->breadcrumbs[15].name) == 0);
  ASSERT(strcmp("crumb: 41", event->breadcrumbs[16].name) == 0);
  free(event);
  PASS();
}

SUITE(breadcrumbs) {
  RUN_TEST(test_add_breadcrumb);
  RUN_TEST(test_add_breadcrumbs_over_max);
}
