## Goal

<!-- What is the intent of this change?
e.g. "When initializing the Bugsnag client, it is currently difficult to (...)
      this change simplifies the process by (...)"

     "Improves the performance of data filtering"

     "Adds additional test coverage to multi-threaded use of Configuration
      objects"
-->

<!-- For new features, include design documentation:

## Design

Why was this approach to the goal used?

-->

## Changeset

<!-- What structures or properties or functions were:

### Added

### Removed

### Changed

-->

## Tests

<!-- How was this change tested? What manual and automated tests were
     run/added? -->

## Discussion

### Alternative Approaches

<!-- What other approaches were considered or discussed? -->

### Outstanding Questions

<!-- Are there any parts of the design or the implementation which seem
     less than ideal and that could require additional discussion?
     List here: -->

### Linked issues

<!--

Fixes #
Related to #

-->

## Review

<!-- When submitting for review, consider the points for self-review and the
     criteria which will be used for secondary review -->

For the submitter, initial self-review:

- [ ] Commented on code changes inline explain the reasoning behind the approach
- [ ] Reviewed the test cases added for completeness and possible points for discussion
- [ ] A changelog entry was added for the goal of this pull request
- [ ] Check the scope of the changeset - is everything in the diff required for the pull request?
- This pull request is ready for:
  - [ ] Initial review of the intended approach, not yet feature complete
  - [ ] Structural review of the classes, functions, and properties modified
  - [ ] Final review

For the pull request reviewer(s), this changeset has been reviewed for:

- [ ] Consistency across platforms for structures or concepts added or modified
- [ ] Consistency between the changeset and the goal stated above
- [ ] Internal consistency with the rest of the library - is there any overlap between existing interfaces and any which have been added?
- [ ] Usage friction - is the proposed change in usage cumbersome or complicated?
- [ ] Performance and complexity - are there any cases of unexpected O(n^3) when iterating, recursing, flat mapping, etc?
- [ ] Concurrency concerns - if components are accessed asynchronously, what issues will arise
- [ ] Thoroughness of added tests and any missing edge cases
- [ ] Idiomatic use of the language
