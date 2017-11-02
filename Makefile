all: build

.PHONY: build test clean

build:
	./gradlew build

clean:
	./gradlew clean

test:
	./gradlew :connectedCheck

bump:
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number bump`)
endif
	@echo Bumping the version number to $(VERSION)
	@sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$(VERSION)/" gradle.properties
	@sed -i '' "s/NOTIFIER_VERSION = .*;/NOTIFIER_VERSION = \"$(VERSION)\";/"\
	 sdk/src/main/java/com/bugsnag/android/Notifier.java


# Makes a release
release:
ifeq ($(VERSION),)g
	@$(error VERSION is not defined. Run with `make VERSION=number release`)
endif
	make VERSION=$(VERSION) bump && git commit -am "v$(VERSION)" && git tag v$(VERSION) \
	&& git push origin && git push --tags && ./gradlew clean uploadArchives
