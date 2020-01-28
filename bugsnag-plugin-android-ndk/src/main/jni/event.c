#include "event.h"
#include "utils/string.h"
#include <string.h>

int bsg_find_next_free_metadata_index(bugsnag_event *event) {
  if (event->metadata.value_count < BUGSNAG_METADATA_MAX) {
    return event->metadata.value_count;
  } else {
    for (int i = 0; i < event->metadata.value_count; i++) {
      if (event->metadata.values[i].type == BSG_NONE_VALUE) {
        return i;
      }
    }
  }
  return -1;
}

int bugsnag_event_add_metadata_value(bugsnag_event *event, char *section,
                                     char *name) {
  int index = bsg_find_next_free_metadata_index(event);
  if (index < 0) {
    return index;
  }
  bsg_strncpy_safe(event->metadata.values[index].section, section,
                   sizeof(event->metadata.values[index].section));
  bsg_strncpy_safe(event->metadata.values[index].name, name,
                   sizeof(event->metadata.values[index].name));
  if (event->metadata.value_count < BUGSNAG_METADATA_MAX) {
    event->metadata.value_count = index + 1;
  }
  return index;
}
void bugsnag_event_add_metadata_double(bugsnag_event *event, char *section,
                                       char *name, double value) {
  int index = bugsnag_event_add_metadata_value(event, section, name);
  if (index >= 0) {
    event->metadata.values[index].type = BSG_NUMBER_VALUE;
    event->metadata.values[index].double_value = value;
  }
}

void bugsnag_event_add_metadata_string(bugsnag_event *event, char *section,
                                       char *name, char *value) {
  int index = bugsnag_event_add_metadata_value(event, section, name);
  if (index >= 0) {
    event->metadata.values[index].type = BSG_CHAR_VALUE;
    bsg_strncpy_safe(event->metadata.values[index].char_value, value,
                     sizeof(event->metadata.values[index].char_value));
  }
}

void bugsnag_event_add_metadata_bool(bugsnag_event *event, char *section,
                                     char *name, bool value) {
  int index = bugsnag_event_add_metadata_value(event, section, name);
  if (index >= 0) {
    event->metadata.values[index].type = BSG_BOOL_VALUE;
    event->metadata.values[index].bool_value = value;
  }
}

void bugsnag_event_remove_metadata(bugsnag_event *event, char *section,
                                   char *name) {
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0 &&
        strcmp(event->metadata.values[i].name, name) == 0) {
      memcpy(&event->metadata.values[i],
             &event->metadata.values[event->metadata.value_count - 1],
             sizeof(bsg_metadata_value));
      event->metadata.values[event->metadata.value_count - 1].type =
          BSG_NONE_VALUE;
      event->metadata.value_count--;
      break;
    }
  }
}

void bugsnag_event_remove_metadata_tab(bugsnag_event *event, char *section) {
  for (int i = 0; i < event->metadata.value_count; ++i) {
    if (strcmp(event->metadata.values[i].section, section) == 0) {
      event->metadata.values[i].type = BSG_NONE_VALUE;
    }
  }
}

void bugsnag_event_start_session(bugsnag_event *event, char *session_id,
                                 char *started_at, int handled_count, int unhandled_count) {
  bsg_strncpy_safe(event->session_id, session_id, sizeof(event->session_id));
  bsg_strncpy_safe(event->session_start, started_at,
                   sizeof(event->session_start));
  event->handled_events = handled_count;
  event->unhandled_events = unhandled_count;
}

char *bugsnag_event_get_context(void *event_ptr) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  return event->context;
}

void bugsnag_event_set_context(void *event_ptr, char *value) {
  bugsnag_event *event = (bugsnag_event *) event_ptr;
  bsg_strncpy_safe(event->context, value, sizeof(event->context));
}

const char *bsg_orientation_from_degrees(int orientation) {
  if (orientation < 0) {
    return "unknown";
  } else if (orientation >= 315 || orientation <= 45) {
    return "portrait";
  } else if (orientation <= 135) {
    return "landscape";
  } else if (orientation <= 225) {
    return "portrait";
  } else {
    return "landscape";
  }
}

void bugsnag_event_set_orientation(bugsnag_event *event, int value) {
  bsg_strncpy_safe(event->device.orientation,
                   (char *)bsg_orientation_from_degrees(value),
                   sizeof(event->device.orientation));
}

void bugsnag_event_set_build_uuid(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->app.build_uuid, value,
                   sizeof(event->app.build_uuid));
}

void bugsnag_event_set_release_stage(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->app.release_stage, value,
                   sizeof(event->app.release_stage));
}

void bugsnag_event_set_user_email(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->user.email, value, sizeof(event->user.email));
}

void bugsnag_event_set_user_name(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->user.name, value, sizeof(event->user.name));
}

void bugsnag_event_set_user_id(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->user.id, value, sizeof(event->user.id));
}

void bugsnag_event_set_app_version(bugsnag_event *event, char *value) {
  bsg_strncpy_safe(event->app.version, value, sizeof(event->app.version));
}

void bugsnag_event_add_breadcrumb(bugsnag_event *event,
                                  bugsnag_breadcrumb *crumb) {
  int crumb_index;
  if (event->crumb_count < BUGSNAG_CRUMBS_MAX) {
    crumb_index = event->crumb_count;
    event->crumb_count++;
  } else {
    crumb_index = event->crumb_first_index;
    event->crumb_first_index =
        (event->crumb_first_index + 1) % BUGSNAG_CRUMBS_MAX;
  }
  memcpy(&event->breadcrumbs[crumb_index], crumb, sizeof(bugsnag_breadcrumb));
}

void bugsnag_event_clear_breadcrumbs(bugsnag_event *event) {
  event->crumb_count = 0;
  event->crumb_first_index = 0;
}

bool bugsnag_event_has_session(bugsnag_event *event) {
    return strlen(event->session_id) > 0;
}
