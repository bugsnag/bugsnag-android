all: build

.PHONY: build test clean bump release

build:
	./gradlew build -PABI_FILTERS=arm64-v8a,armeabi,armeabi-v7a,x86,x86_64

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
	@./gradlew -PVERSION_NAME=9.9.9 clean assembleRelease publishToMavenLocal

	# Build the full test fixture
	@./gradlew -PTEST_FIXTURE_NDK_VERSION=$(TEST_FIXTURE_NDK_VERSION) -p=features/fixtures/mazerunner/ assembleRelease
	@cp features/fixtures/mazerunner/app/build/outputs/apk/release/fixture.apk build/fixture.apk

	# And the minimal (no NDK or ANR plugin) test fixture
	@./gradlew -PMINIMAL_FIXTURE=true -PTEST_FIXTURE_NAME=fixture-minimal.apk  -p=features/fixtures/mazerunner/ assembleRelease
	@cp features/fixtures/mazerunner/app/build/outputs/apk/release/fixture-minimal.apk build/fixture-minimal.apk

bump:
ifneq ($(shell git diff --staged),)
	@git diff --staged
	@$(error You have uncommitted changes. Push or discard them to continue)
endif
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number bump`)
endif
	@echo Bumping the version number to $(VERSION)
	@sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$(VERSION)/" gradle.properties
	@sed -i '' "s/var version: String = .*/var version: String = \"$(VERSION)\",/"\
	 bugsnag-android-core/src/main/java/com/bugsnag/android/Notifier.kt
	@sed -i '' "s/## TBD/## $(VERSION) ($(shell date '+%Y-%m-%d'))/" CHANGELOG.md
