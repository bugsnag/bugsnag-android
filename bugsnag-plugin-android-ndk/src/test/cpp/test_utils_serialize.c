#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>

#include <greatest/greatest.h>
#include <parson/parson.h>

#include <featureflags.h>
#include <utils/serializer.h>
#include <utils/serializer/buffered_writer.h>


#define SERIALIZE_TEST_FILE "/data/data/com.bugsnag.android.ndk.test/cache/"

void bsg_update_next_run_info(bsg_environment *env);

TEST test_last_run_info_serialization(void) {
  bsg_environment *env = calloc(1, sizeof(bsg_environment));
  strcpy(env->last_run_info_path, SERIALIZE_TEST_FILE);
  
  // update LastRunInfo with defaults
  env->next_event.app.is_launching = false;
  env->consecutive_launch_crashes = 1;
  bsg_update_next_run_info(env);
  ASSERT_STR_EQ("consecutiveLaunchCrashes=1\ncrashed=true\ncrashedDuringLaunch=false\0", env->next_last_run_info);

  // update LastRunInfo with consecutive crashes
  env->next_event.app.is_launching = true;
  env->consecutive_launch_crashes = 7;
  bsg_update_next_run_info(env);
  ASSERT_STR_EQ("consecutiveLaunchCrashes=8\ncrashed=true\ncrashedDuringLaunch=true\0", env->next_last_run_info);

  free(env);
  PASS();
}

SUITE(suite_json_serialization) {
  RUN_TEST(test_last_run_info_serialization);
}
