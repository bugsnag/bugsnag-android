all: build

.PHONY: build test clean

build:
	./gradlew build

clean:
	./gradlew clean

test:
	./gradlew :connectedCheck

release:
	./gradlew clean :uploadArchives

bump:
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number bump`)
endif
	@echo Bumping the version number to $(VERSION)
	@sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$(VERSION)/" gradle.properties
	@sed -i '' "s/NOTIFIER_VERSION = .*;/NOTIFIER_VERSION = \"$(VERSION)\";/"\
	 sdk/src/main/java/com/bugsnag/android/Notifier.java



