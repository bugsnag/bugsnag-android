all: build

.PHONY: build test clean bump release

build:
	./gradlew build -PABI_FILTERS=arm64-v8a,armeabi,armeabi-v7a,x86,x86_64

clean:
	./gradlew clean

test:
	./gradlew connectedCheck

remote-test:
ifeq ($(MAZE_DEVICE_FARM_USERNAME),)
	@$(error MAZE_DEVICE_FARM_USERNAME is not defined)
endif
ifeq ($(MAZE_DEVICE_FARM_ACCESS_KEY),)
	@$(error MAZE_DEVICE_FARM_ACCESS_KEY is not defined)
endif
	@APP_LOCATION=/app/bugsnag-android-core/build/outputs/apk/androidTest/debug/bugsnag-android-core-debug-androidTest.apk \
	 INSTRUMENTATION_DEVICES='["Google Nexus 5-4.4", "Google Pixel-7.1", "Google Pixel 3-9.0"]' \
	 docker-compose up --build android-instrumentation-tests

TEST_FIXTURE_NDK_VERSION ?= 16.1.4479499
test-fixture:
	@./gradlew -PVERSION_NAME=9.9.9 clean assembleRelease publishToMavenLocal
	@./gradlew -PTEST_FIXTURE_NDK_VERSION=$(TEST_FIXTURE_NDK_VERSION) -p=features/fixtures/mazerunner/ assembleRelease
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
	@sed -i '' "s/var version: String = .*/var version: String = \"$(VERSION)\",/"\
	 bugsnag-android-core/src/main/java/com/bugsnag/android/Notifier.kt
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
	@./gradlew clean assembleRelease publish bintrayUpload -PABI_FILTERS=arm64-v8a,armeabi,armeabi-v7a,x86,x86_64 \
	 && ./gradlew clean assembleRelease publish bintrayUpload -PABI_FILTERS=arm64-v8a,armeabi,armeabi-v7a,x86,x86_64 -PreleaseNdkArtefact=true
