#include "report.h"
#include <string.h>
#include <time.h>
#include <stdlib.h>

int bugsnag_report_add_metadata_value(bugsnag_report *report, char *section, char *name) {
  if (report->metadata.value_count < BUGSNAG_METADATA_MAX - 1) {
    int index = report->metadata.value_count;
    strncpy(report->metadata.values[index].section, section, 32);
    strncpy(report->metadata.values[index].name, name, 32);
    report->metadata.value_count = index + 1;
    return index;
  }
  return -1;
}
void bugsnag_report_add_metadata_double(bugsnag_report *report, char *section, char *name, double value) {
  int index = bugsnag_report_add_metadata_value(report, section, name);
  if (index > 0) {
    report->metadata.values[index].type = BSG_NUMBER_VALUE;
    report->metadata.values[index].double_value = value;
  }
}

void bugsnag_report_add_metadata_string(bugsnag_report *report, char *section, char *name, char *value) {
  int index = bugsnag_report_add_metadata_value(report, section, name);
  if (index > 0) {
    report->metadata.values[index].type = BSG_CHAR_VALUE;
    strncpy(report->metadata.values[index].char_value, value, 64);
  }
}

void bugsnag_report_add_metadata_bool(bugsnag_report *report, char *section, char *name, bool value) {
  int index = bugsnag_report_add_metadata_value(report, section, name);
  if (index > 0) {
    report->metadata.values[index].type = BSG_BOOL_VALUE;
    report->metadata.values[index].bool_value = value;
  }
}

void bugsnag_report_remove_metadata(bugsnag_report *report, char *section, char *name) {
    for (int i = 0; i < report->metadata.value_count; ++i) {
        if (strcmp(report->metadata.values[i].section, section) == 0
            && strcmp(report->metadata.values[i].name, name) == 0) {
            memcpy(&report->metadata.values[i], &report->metadata.values[report->metadata.value_count - 1], sizeof(bsg_metadata_value));
            report->metadata.values[report->metadata.value_count - 1].type = BSG_NONE_VALUE;
            report->metadata.value_count--;
            break;
        }
    }
}

void bugsnag_report_add_breadcrumb(bugsnag_report *report, bugsnag_breadcrumb *crumb) {
  int crumb_index;
  if (report->crumb_count < BUGSNAG_CRUMBS_MAX) {
    crumb_index = report->crumb_count;
    report->crumb_count++;
  } else {
    report->crumb_first_index++;
    if (report->crumb_first_index >= BUGSNAG_CRUMBS_MAX) {
      report->crumb_first_index = 0;
    }
    crumb_index = report->crumb_first_index - 1;
    if (crumb_index < 0) {
      crumb_index = BUGSNAG_CRUMBS_MAX - 1;
    }
  }
  memcpy(&report->breadcrumbs[crumb_index], crumb, sizeof(bugsnag_breadcrumb));
}

void bugsnag_report_clear_breadcrumbs(bugsnag_report *report) {
  report->crumb_count = 0;
  report->crumb_first_index = 0;
}
