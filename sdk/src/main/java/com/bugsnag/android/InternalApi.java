package com.bugsnag.android;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method, type, or field is part of Bugsnag's internal API. Code annotated with
 * this marker are subject to change without warning, regardless of their visibility.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({TYPE, FIELD, METHOD, CONSTRUCTOR})
@interface InternalApi {
}
