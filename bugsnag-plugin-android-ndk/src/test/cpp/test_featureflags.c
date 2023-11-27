#include <greatest/greatest.h>
#include <featureflags.h>

TEST test_set_feature_flag(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  bsg_set_feature_flag(event, "sample_group", "a");
  bsg_set_feature_flag(event, "demo_mode", NULL);
  bsg_set_feature_flag(event, "zzz", NULL);
  bsg_set_feature_flag(event, "demo_mode", "yes");

  ASSERT_EQ(3, event->feature_flag_count);

  ASSERT_STR_EQ("sample_group", event->feature_flags[0].name);
  ASSERT_STR_EQ("a", event->feature_flags[0].variant);

  ASSERT_STR_EQ("demo_mode", event->feature_flags[1].name);
  ASSERT_STR_EQ("yes", event->feature_flags[1].variant);

  ASSERT_STR_EQ("zzz", event->feature_flags[2].name);
  ASSERT_EQ(NULL, event->feature_flags[2].variant);

  bsg_free_feature_flags(event);
  free(event);

  PASS();
}

TEST test_clear_feature_flag(void) {
  bugsnag_event *event = calloc(1, sizeof(bugsnag_event));

  bsg_set_feature_flag(event, "sample_group", "a");
  bsg_set_feature_flag(event, "demo_mode", NULL);
  bsg_set_feature_flag(event, "remaining", "flag");

  ASSERT_EQ(3, event->feature_flag_count);

  bsg_clear_feature_flag(event, "no_such_flag");
  ASSERT_EQ(3, event->feature_flag_count);

  bsg_clear_feature_flag(event, "demo_mode");

  ASSERT_EQ(2, event->feature_flag_count);
  ASSERT_STR_EQ("sample_group", event->feature_flags[0].name);
  ASSERT_STR_EQ("a", event->feature_flags[0].variant);
  ASSERT_STR_EQ("remaining", event->feature_flags[1].name);
  ASSERT_STR_EQ("flag", event->feature_flags[1].variant);

  bsg_free_feature_flags(event);
  free(event);

  PASS();
}

SUITE (suite_feature_flags) {
  RUN_TEST(test_set_feature_flag);
  RUN_TEST(test_clear_feature_flag);
}