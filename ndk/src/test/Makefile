all: test

SRCDIR= ../main/jni
SRC=$(SRCDIR)/report.c \
	$(SRCDIR)/utils/serializer.c \
	$(SRCDIR)/utils/string.c \
	$(SRCDIR)/deps/parson/parson.c

TESTS=$(shell ls cpp/*.c)

bugsnag_test: $(TESTS) $(SRC)
	@$(CC) -Icpp/deps -I$(SRCDIR) -I$(SRCDIR)/deps $(TESTS) $(SRC) -o bugsnag_test

test: bugsnag_test
	@./bugsnag_test

clean:
	@rm -f bugsnag_test
