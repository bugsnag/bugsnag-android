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
