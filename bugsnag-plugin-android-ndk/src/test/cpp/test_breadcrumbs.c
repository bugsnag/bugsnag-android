#include <event.h>
#include <greatest/greatest.h>
#include <time.h>

bugsnag_breadcrumb *init_breadcrumb(const char *name, const char *message, bugsnag_breadcrumb_type type) {
  bugsnag_breadcrumb *crumb = calloc(1, sizeof(bugsnag_breadcrumb));
  crumb->type = type;
  strcpy(crumb->name, name);
  strcpy(crumb->timestamp, "2018-08-29T21:41:39Z");
  crumb->metadata.values[0] = (bsg_metadata_value) {
    .name = {"message"},
    .section = {"metaData"},
    .type = BSG_METADATA_CHAR_VALUE,
  };
  strcpy(crumb->metadata.values[0].char_value, message);
  crumb->metadata.value_count = 1;
  return crumb;
}

TEST test_add_breadcrumb(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  event->max_crumb_count = 50;
  event->breadcrumbs =
      calloc(event->max_crumb_count, sizeof(bugsnag_breadcrumb));
  bugsnag_breadcrumb *crumb = init_breadcrumb("stroll", "this is a drill.", BSG_CRUMB_USER);
  bsg_event_add_breadcrumb(event, crumb);
  ASSERT_EQ(1, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  ASSERT(strcmp("stroll", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[0].metadata.values[0].name) == 0);
  ASSERT(strcmp("this is a drill.", event->breadcrumbs[0].metadata.values[0].char_value) == 0);
  free(crumb);
  bugsnag_breadcrumb *crumb2 = init_breadcrumb("walking...", "this is not a drill.", BSG_CRUMB_USER);
  bsg_event_add_breadcrumb(event, crumb2);
  ASSERT_EQ(2, event->crumb_count);
  ASSERT_EQ(0, event->crumb_first_index);
  ASSERT(strcmp("stroll", event->breadcrumbs[0].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[0].metadata.values[0].name) == 0);
  ASSERT(strcmp("this is a drill.", event->breadcrumbs[0].metadata.values[0].char_value) == 0);
  ASSERT(strcmp("walking...", event->breadcrumbs[1].name) == 0);
  ASSERT(strcmp("message", event->breadcrumbs[1].metadata.values[0].name) == 0);
  ASSERT(strcmp("this is not a drill.", event->breadcrumbs[1].metadata.values[0].char_value) == 0);

  free(event->breadcrumbs);
  free(event);
  free(crumb2);
  PASS();
}

TEST test_add_breadcrumbs_over_max(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
  event->max_crumb_count = 50;
  event->breadcrumbs =
      calloc(event->max_crumb_count, sizeof(bugsnag_breadcrumb));
  int breadcrumb_count = 64;

  for (int i=0; i < breadcrumb_count; i++) {
    char *format = calloc(1, sizeof(char) * breadcrumb_count);
    memset(format, 0, sizeof(char) * breadcrumb_count);
    sprintf(format, "crumb: %d", i);
    bugsnag_breadcrumb *crumb = init_breadcrumb(format, "go go go", BSG_CRUMB_USER);
    bsg_event_add_breadcrumb(event, crumb);
    free(crumb);
    free(format);
  }

  // assertions assume that the crumb count is always 50
  ASSERT_EQ(event->max_crumb_count, event->crumb_count);
  ASSERT_EQ(14, event->crumb_first_index);

  ASSERT_STR_EQ("crumb: 50", event->breadcrumbs[0].name);
  ASSERT_STR_EQ("crumb: 51", event->breadcrumbs[1].name);
  ASSERT_STR_EQ("crumb: 52", event->breadcrumbs[2].name);
  ASSERT_STR_EQ("crumb: 53", event->breadcrumbs[3].name);

  ASSERT_STR_EQ("crumb: 63", event->breadcrumbs[13].name);
  ASSERT_STR_EQ("crumb: 14", event->breadcrumbs[14].name);
  ASSERT_STR_EQ("crumb: 15", event->breadcrumbs[15].name);
  ASSERT_STR_EQ("crumb: 16", event->breadcrumbs[16].name);
  free(event->breadcrumbs);
  free(event);
  PASS();
}

SUITE(suite_breadcrumbs) {
  RUN_TEST(test_add_breadcrumb);
  RUN_TEST(test_add_breadcrumbs_over_max);
}
