all: build

.PHONY: build test clean bump release

build:
	./gradlew build -PABI_FILTERS=arm64-v8a,armeabi-v7a,x86,x86_64

clean:
	./gradlew clean

test:
	./gradlew connectedCheck

remote-test:
ifeq ($(BROWSER_STACK_USERNAME),)
	@$(error BROWSER_STACK_USERNAME is not defined)
endif
ifeq ($(BROWSER_STACK_ACCESS_KEY),)
	@$(error BROWSER_STACK_ACCESS_KEY is not defined)
endif
	@APP_LOCATION=/app/bugsnag-android-core/build/outputs/apk/androidTest/debug/bugsnag-android-core-debug-androidTest.apk \
	 INSTRUMENTATION_DEVICES='["Google Nexus 5-4.4", "Google Pixel-7.1", "Google Pixel 3-9.0"]' \
	 docker-compose up --build android-instrumentation-tests

TEST_FIXTURE_NDK_VERSION ?= 16.1.4479499
test-fixtures:
	# Build the notifier
	@./gradlew -PVERSION_NAME=9.9.9 assembleRelease publishToMavenLocal -x check

	# Build the full test fixture
	@./gradlew -PTEST_FIXTURE_NDK_VERSION=$(TEST_FIXTURE_NDK_VERSION) -p=features/fixtures/mazerunner/ assembleRelease -x check
	@cp features/fixtures/mazerunner/app/build/outputs/apk/release/fixture.apk build/fixture.apk
	# copy the unstripped scenarios libs (for stack unwinding validation)
	# grabs the newest files as the likely just built ones
	LIB_CXX_BUGSNAG=$$(ls -dtr features/fixtures/mazerunner/cxx-scenarios-bugsnag/build/intermediates/cxx/RelWithDebInfo/* | head -n 1) && \
	LIB_CXX=$$(ls -dtr features/fixtures/mazerunner/cxx-scenarios/build/intermediates/cxx/RelWithDebInfo/* | head -n 1) && \
		cp $$LIB_CXX/obj/x86_64/libcxx-scenarios.so build/libcxx-scenarios-x86_64.so && \
		cp $$LIB_CXX/obj/x86/libcxx-scenarios.so build/libcxx-scenarios-x86.so && \
		cp $$LIB_CXX/obj/arm64-v8a/libcxx-scenarios.so build/libcxx-scenarios-arm64.so && \
		cp $$LIB_CXX/obj/armeabi-v7a/libcxx-scenarios.so build/libcxx-scenarios-arm32.so && \
		cp $$LIB_CXX_BUGSNAG/obj/x86_64/libcxx-scenarios-bugsnag.so build/libcxx-scenarios-bugsnag-x86_64.so && \
		cp $$LIB_CXX_BUGSNAG/obj/x86/libcxx-scenarios-bugsnag.so build/libcxx-scenarios-bugsnag-x86.so && \
		cp $$LIB_CXX_BUGSNAG/obj/arm64-v8a/libcxx-scenarios-bugsnag.so build/libcxx-scenarios-bugsnag-arm64.so && \
		cp $$LIB_CXX_BUGSNAG/obj/armeabi-v7a/libcxx-scenarios-bugsnag.so build/libcxx-scenarios-bugsnag-arm32.so

	# And the minimal (no NDK or ANR plugin) test fixture
	@./gradlew -PMINIMAL_FIXTURE=true -PTEST_FIXTURE_NAME=fixture-minimal.apk  -p=features/fixtures/mazerunner/ assembleRelease -x check
	@cp features/fixtures/mazerunner/app/build/outputs/apk/release/fixture-minimal.apk build/fixture-minimal.apk

	# And the debug test fixture (full fixture - but a debug build)
	@./gradlew -PTEST_FIXTURE_NDK_VERSION=$(TEST_FIXTURE_NDK_VERSION) -p=features/fixtures/mazerunner/ assembleDebug -x check
	@cp features/fixtures/mazerunner/app/build/outputs/apk/debug/fixture.apk build/fixture-debug.apk

bump:
ifneq ($(shell git diff --staged),)
	@git diff --staged
	@$(error You have uncommitted changes. Push or discard them to continue)
endif
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number bump`)
endif
	@echo Bumping the version number to $(VERSION)
	@sed -i '' "s/bugsnag-android:.*\"/bugsnag-android:$(VERSION)\"/" examples/sdk-app-example/app/build.gradle
	@sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$(VERSION)/" gradle.properties
	@sed -i '' "s/var version: String = .*/var version: String = \"$(VERSION)\",/"\
	 bugsnag-android-core/src/main/java/com/bugsnag/android/Notifier.kt
	@sed -i '' "s/## TBD/## $(VERSION) ($(shell date '+%Y-%m-%d'))/" CHANGELOG.md

.PHONY: check
check:
	@./gradlew lint detekt ktlintCheck checkstyle
	@./scripts/run-cpp-check.sh
	@./scripts/run-clang-format-ci-check.sh
