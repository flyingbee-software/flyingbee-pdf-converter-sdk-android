# Shared Flyingbee SDK Drop-in Directory

This folder holds the single copy of the commercial SDK release artifact
used by **all three demo projects** (`Kotlin/`, `Java/`, `CPP/`):

```
FPPDFFramework-10.3.6.aar
```

Each demo references it as a direct file dependency from its
`app/build.gradle.kts`:

```kotlin
implementation(files("../../libs/flyingbee/FPPDFFramework-10.3.6.aar"))
```

Why one shared copy:
- The AAR is the closed-source Flyingbee SDK release artifact, not source.
- Shipping three identical ~64 MB copies in the repo bloats history; one
  copy keeps every demo building with a single drop-in.
- Consumer projects fetch the SDK from the official Flyingbee distribution
  channel and place the AAR here once.
