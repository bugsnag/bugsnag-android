all: build

.PHONY: build test clean bump release

build:
	./gradlew build

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

remote-integration-tests:
ifeq ($(BROWSER_STACK_USERNAME),)
	@$(error BROWSER_STACK_USERNAME is not defined)
endif
ifeq ($(BROWSER_STACK_ACCESS_KEY),)
	@$(error BROWSER_STACK_ACCESS_KEY is not defined)
endif
	@docker-compose up --build android-builder
	@docker-compose build android-maze-runner
ifneq ($(TEST_FEATURE),)
	@APP_LOCATION=/app/build/fixture.apk docker-compose run android-maze-runner features/$(TEST_FEATURE)
else
	@APP_LOCATION=/app/build/fixture.apk docker-compose run android-maze-runner
endif

test-fixture:
	# Build the notifier
	@./gradlew -PVERSION_NAME=9.9.9 clean assembleRelease publishToMavenLocal

	# Build the test fixture
	@./gradlew -p=features/fixtures/mazerunner/ assembleRelease
	@cp features/fixtures/mazerunner/app/build/outputs/apk/release/fixture.apk build/fixture.apk

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
	@sed -i '' "s/NOTIFIER_VERSION = .*;/NOTIFIER_VERSION = \"$(VERSION)\";/"\
	 bugsnag-android-core/src/main/java/com/bugsnag/android/Notifier.java
	@sed -i '' "s/## TBD/## $(VERSION) ($(shell date '+%Y-%m-%d'))/" CHANGELOG.md

# Makes a release
release:
ifneq ($(shell git diff origin/master..master),)
	@$(error You have unpushed commits on the master branch)
endif
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number release`)
endif
	@git add -p CHANGELOG.md README.md gradle.properties bugsnag-android-core/src/main/java/com/bugsnag/android/Notifier.java
	@git commit -m "Release v$(VERSION)"
	@git tag v$(VERSION)
	@git push origin master v$(VERSION)
	@./gradlew assembleRelease publish bintrayUpload && ./gradlew assembleRelease publish bintrayUpload -PreleaseNdkArtefact=true
