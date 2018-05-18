all: build

.PHONY: build test clean bump badge release

build:
	./gradlew sdk:build

clean:
	./gradlew clean

test:
	./gradlew :sdk:connectedCheck

bump: badge
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number bump`)
endif
	@echo Bumping the version number to $(VERSION)
	@sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$(VERSION)/" gradle.properties
	@sed -i '' "s/NOTIFIER_VERSION = .*;/NOTIFIER_VERSION = \"$(VERSION)\";/"\
	 sdk/src/main/java/com/bugsnag/android/Notifier.java

badge: build
	@echo "Counting ..."
	@./gradlew countReleaseDexMethods > counter.txt
	@awk 'BEGIN{ \
		"cat counter.txt | grep \"com.bugsnag.android\$\"" | getline output;\
		split(output, counts);\
		"du -k sdk/build/outputs/aar/bugsnag-android-release.aar | cut -f1" | getline size;\
		printf "![Method count and size](https://img.shields.io/badge/Methods%%20and%%20size-";\
		printf counts[1] "%%20classes%%20|%%20" counts[2] "%%20methods%%20|%%20" counts[3] "%%20fields%%20|%%20";\
		printf size "%%20KB-e91e63.svg)";\
		};' > tmp_url.txt
	@awk '/!.*Method count and size.*/ { getline < "tmp_url.txt" }1' README.md > README.md.tmp
	@mv README.md.tmp README.md
	@rm counter.txt tmp_url.txt


# Makes a release
release: clean bump
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number release`)
endif
	make VERSION=$(VERSION) bump && git commit -am "v$(VERSION)" && git tag v$(VERSION) \
	&& git push origin && git push --tags && ./gradlew clean assemble uploadArchives bintrayUpload
