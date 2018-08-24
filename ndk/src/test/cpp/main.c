#include <greatest/greatest.h>

SUITE(string_utils);
SUITE(serialize_utils);
SUITE(breadcrumbs);

GREATEST_MAIN_DEFS();

int main(int argc, char *argv[]) {
    GREATEST_MAIN_BEGIN();

    RUN_SUITE(string_utils);
    RUN_SUITE(serialize_utils);
    RUN_SUITE(breadcrumbs);

    GREATEST_MAIN_END();

    return 0;
}
