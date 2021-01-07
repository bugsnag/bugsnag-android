#ifndef BUGSNAG_ANDROID_BUILD_H
#define BUGSNAG_ANDROID_BUILD_H
#ifdef CLANG_ANALYZE_ASYNCSAFE
#define __asyncsafe __attribute__((asyncsafe));
#else
#define __asyncsafe
#endif
#endif // BUGSNAG_ANDROID_BUILD_H
