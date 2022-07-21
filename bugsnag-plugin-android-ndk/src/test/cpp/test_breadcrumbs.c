#include <event.h>
#include <greatest/greatest.h>
#include <time.h>
#include <utils/serializer/json_writer.h>
#include <utils/serializer/migrate.h>

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

  free(event);
  free(crumb2);
  PASS();
}

TEST test_add_breadcrumbs_over_max(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));
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
  ASSERT_EQ(BUGSNAG_CRUMBS_MAX, event->crumb_count);
  ASSERT_EQ(14, event->crumb_first_index);

  ASSERT_STR_EQ("crumb: 50", event->breadcrumbs[0].name);
  ASSERT_STR_EQ("crumb: 51", event->breadcrumbs[1].name);
  ASSERT_STR_EQ("crumb: 52", event->breadcrumbs[2].name);
  ASSERT_STR_EQ("crumb: 53", event->breadcrumbs[3].name);

  ASSERT_STR_EQ("crumb: 63", event->breadcrumbs[13].name);
  ASSERT_STR_EQ("crumb: 14", event->breadcrumbs[14].name);
  ASSERT_STR_EQ("crumb: 15", event->breadcrumbs[15].name);
  ASSERT_STR_EQ("crumb: 16", event->breadcrumbs[16].name);
  free(event);
  PASS();
}

TEST test_bsg_calculate_total_crumbs(void) {
  ASSERT_EQ(0, bsg_calculate_total_crumbs(0));
  ASSERT_EQ(5, bsg_calculate_total_crumbs(5));
  ASSERT_EQ(22, bsg_calculate_total_crumbs(22));
  ASSERT_EQ(25, bsg_calculate_total_crumbs(25));
  ASSERT_EQ(50, bsg_calculate_total_crumbs(51));
  ASSERT_EQ(50, bsg_calculate_total_crumbs(55));
  PASS();
}

TEST test_bsg_calculate_start_index(void) {
  ASSERT_EQ(0, bsg_calculate_v1_start_index(0));
  ASSERT_EQ(0, bsg_calculate_v1_start_index(3));
  ASSERT_EQ(0, bsg_calculate_v1_start_index(17));
  ASSERT_EQ(0, bsg_calculate_v1_start_index(24));
  ASSERT_EQ(0, bsg_calculate_v1_start_index(25));
  ASSERT_EQ(1, bsg_calculate_v1_start_index(26));
  ASSERT_EQ(3, bsg_calculate_v1_start_index(28));
  ASSERT_EQ(5, bsg_calculate_v1_start_index(30));
  PASS();
}

TEST test_bsg_calculate_crumb_index(void) {
  ASSERT_EQ(0, bsg_calculate_v1_crumb_index(0, 0));

  // zero offset
  ASSERT_EQ(24, bsg_calculate_v1_crumb_index(24, 0));
  ASSERT_EQ(25, bsg_calculate_v1_crumb_index(25, 0));
  ASSERT_EQ(26, bsg_calculate_v1_crumb_index(26, 0));
  ASSERT_EQ(0, bsg_calculate_v1_crumb_index(30, 0));

  // offset
  ASSERT_EQ(15, bsg_calculate_v1_crumb_index(0, 15));
  ASSERT_EQ(15, bsg_calculate_v1_crumb_index(5, 10));
  ASSERT_EQ(24, bsg_calculate_v1_crumb_index(10, 14));
  ASSERT_EQ(25, bsg_calculate_v1_crumb_index(10, 15));
  ASSERT_EQ(26, bsg_calculate_v1_crumb_index(11, 15));
  ASSERT_EQ(29, bsg_calculate_v1_crumb_index(14, 15));
  ASSERT_EQ(0, bsg_calculate_v1_crumb_index(20, 10));
  ASSERT_EQ(1, bsg_calculate_v1_crumb_index(20, 11));
  ASSERT_EQ(4, bsg_calculate_v1_crumb_index(23, 11));
  PASS();
}

SUITE(suite_breadcrumbs) {
  RUN_TEST(test_add_breadcrumb);
  RUN_TEST(test_add_breadcrumbs_over_max);
  RUN_TEST(test_bsg_calculate_total_crumbs);
  RUN_TEST(test_bsg_calculate_start_index);
  RUN_TEST(test_bsg_calculate_crumb_index);
}
