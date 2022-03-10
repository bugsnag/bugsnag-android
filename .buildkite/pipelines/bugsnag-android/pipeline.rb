Buildkite::Builder.pipeline do
  plugin :docker_compose, "docker-compose#v3.7.0"
  plugin :artifacts, "artifacts#v1.2.0"

  # trigger do
  #   label "Trigger RN tests for al builds of our next branch"
  #   # IF
  #   condition 'build.branch == "next"'
  #   trigger "bugsnag-js"
  #   build \
  #     branch: "next",
  #     message: "Run RN tests with latest Android next branch",
  #     env: \
  #       "BUILD_RN_WITH_LATEST_NATIVES""true"
  #   async true
  # end

  command do
    label "echo Branch name"
    command "echo #{BRANCH_NAME}"
  end
  #
  # command do
  #   label "Build Android base image"
  #   key "android-common"
  #   timeout_in_minutes 30
  #   plugin :docker_compose,
  #          build: "android-common",
  #          'image-repository': "855461928731.dkr.ecr.us-west-1.amazonaws.com/android",
  #          'cache-from': "android-common:855461928731.dkr.ecr.us-west-1.amazonaws.com/android:latest"
  #   plugin :docker_compose,
  #          push: "android-common:855461928731.dkr.ecr.us-west-1.amazonaws.com/android:latest"
  # end
  #
  # command do
  #   label "Build Android base CI image"
  #   key "android-ci"
  #   depends_on "android-common"
  #   timeout_in_minutes 30
  #   plugin :docker_compose,
  #          build: "android-ci",
  #          'image-repository': "855461928731.dkr.ecr.us-west-1.amazonaws.com/android",
  #          'cache-from': "android-ci:855461928731.dkr.ecr.us-west-1.amazonaws.com/android:ci-${BRANCH_NAME}"
  #   plugin :docker_compose,
  #          push: "android-ci:855461928731.dkr.ecr.us-west-1.amazonaws.com/android:ci-${BRANCH_NAME}"
  # end
  #
  # command do
  #   label "Audit current licenses"
  #   depends_on "android-ci"
  #   timeout_in_minutes 30
  #   plugin :docker_compose,
  #          run: "android-license-audit",
  #          command: %w[./scripts/audit-dependency-licenses.sh]
  # end
  #
  # command do
  #   label "Build fixture APK r16"
  #   key "fixture-r16"
  #   depends_on "android-ci"
  #   timeout_in_minutes 30
  #   artifact_paths "build/release/fixture-r16.apk"
  #   plugin :docker_compose,
  #          run: "android-builder"
  #   env \
  #     MAVEN_VERSION: "3.6.1",
  #     TEST_FIXTURE_NDK_VERSION: "17.2.4988734",
  #     TEST_FIXTURE_NAME: "fixture-r16.apk",
  #     USE_LEGACY_OKHTTP: "true",
  #     BUILD_TASK: "assembleRelease"
  # end
  #
  # command do
  #   label "Build fixture APK r19"
  #   key "fixture-r19"
  #   depends_on "android-ci"
  #   timeout_in_minutes 30
  #   artifact_paths "build/release/fixture-r19.apk"
  #   plugin :docker_compose,
  #          run: "android-builder"
  #   env \
  #     MAVEN_VERSION: "3.6.1",
  #     TEST_FIXTURE_NDK_VERSION: "19.2.5345600",
  #     TEST_FIXTURE_NAME: "fixture-r19.apk",
  #     BUILD_TASK: "assembleRelease"
  # end
  #
  # command do
  #   label "Build fixture APK r21"
  #   key "fixture-r21"
  #   depends_on "android-ci"
  #   timeout_in_minutes 30
  #   artifact_paths "build/release/fixture-r21.apk"
  #   plugin :docker_compose,
  #          run: "android-builder"
  #   env \
  #     MAVEN_VERSION: "3.6.1",
  #     TEST_FIXTURE_NDK_VERSION: "21.3.6528147",
  #     TEST_FIXTURE_NAME: "fixture-r21.apk",
  #     BUILD_TASK: "assembleRelease"
  # end
  #
  # command do
  #   label "Android Lint"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "./gradlew lint"
  # end
  #
  # command do
  #   label "Android Checkstyle"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "./gradlew checkstyle"
  # end
  #
  # command do
  #   label "Android Detekt"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "./gradlew detekt"
  # end
  #
  # command do
  #   label "Android Ktlint"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "./gradlew ktlintCheck"
  # end
  #
  # command do
  #   label "Android CppCheck"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "bash ./scripts/run-cpp-check.sh"
  # end
  #
  # command do
  #   label "Android ClangFormat"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "bash ./scripts/run-clang-format-ci-check.sh"
  # end
  #
  # command do
  #   label "Android Lint mazerunner scenarios"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "cd features/fixtures/mazerunner && ./gradlew ktlintCheck detekt checkstyle"
  # end
  #
  # command do
  #   label "Android JVM tests"
  #   depends_on "android-ci"
  #   timeout_in_minutes 10
  #   plugin :docker_compose,
  #          run: "android-ci"
  #   command "./gradlew test"
  # end
  #
  # command do
  #   label "Android 4.4 NDK r16 smoke tests"
  #   key "android-4-4-smoke"
  #   depends_on "fixture-r16"
  #   timeout_in_minutes 60
  #   plugin :artifacts,
  #          download: "build/release/fixture-r16.apk",
  #          upload: "maze_output/failed/**/*"
  #   plugin :docker_compose,
  #          pull: "android-maze-runner",
  #          run: "android-maze-runner",
  #          command: %w[features/smoke_tests --app=/app/build/release/fixture-r16.apk --farm=bs --device=ANDROID_4_4 --fail-fast]
  #   concurrency 9
  #   concurrency_group "browserstack-app"
  #   # Not present in the step - Can be fixed with a PR to the Buildkite-Builder repo
  #   # concurrency_method "eager"
  # end
  #
  #
  # # Android 11+ devices have the GWP-ASAN tool enabled which randomly samples native memory to
  # # to detect misuse (such as use-after-free, buffer overflow). If a failure is detected then
  # # the device will raise a SIGABRT mentioning GWP-ASAN - this can be investigated further
  # # by inspecting the devices logs.
  # command do
  #   label "Android 12 NDK r21 smoke tests"
  #   key "android-12-smoke"
  #   depends_on "fixture-r21"
  #   timeout_in_minutes 60
  #   plugin :artifacts,
  #          download: "build/release/fixture-r21.apk",
  #          upload: "maze_output/failed/**/*"
  #   plugin :docker_compose,
  #          pull: "android-maze-runner",
  #          run: "android-maze-runner",
  #          args: %w[TESTS_DIR=features/smoke_tests],
  #          command: %w[features/smoke_tests --app=/app/build/release/fixture-r21.apk --farm=bs --device=ANDROID_12_0 --fail-fast]
  #   concurrency 9
  #   concurrency_group "browserstack-app"
  #   # Not present in the step - Can be fixed with a PR to the Buildkite-Builder repo
  #   # concurrency_method "eager"
  # end

  command do
    label "Conditionally trigger full set of tests"
    command 'sh -c .buildkite/pipeline_trigger.sh'
  end
end