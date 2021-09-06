# New Module Checklist

A checklist of sanity checks for creating new modules in the repository.

1. Apply the `BugsnagBuildPlugin` to the new module and set plugin extension properties as necessary
2. Add a `README.md` describing the module
3. Ensure the module has consumer ProGuard rules (if necessary)
4. Confirm that the package name is correct and that a POM/AAR is generated correctly
5. Update the [Project Structure](PROJECT_STRUCTURE.md) docs
6. Claim the module on Google Play SDK Console
