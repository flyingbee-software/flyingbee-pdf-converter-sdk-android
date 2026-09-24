# Local SDK Drop-in Directory

This folder is intentionally untracked for binary payloads.  Drop the
following SDK release here before building the app:

```
FPPDFFramework-10.3.6.aar
```

The file is referenced by `app/build.gradle.kts`:

```kotlin
implementation(files("libs/flyingbee/FPPDFFramework-10.3.6.aar"))
```

Why this lives outside the repository:
- The AAR is the closed-source Flyingbee SDK release artifact, not source.
- Keeping it out of git avoids bloating history with multi-megabyte binaries.
- Consumer projects fetch the SDK from the official Flyingbee distribution
  channel and place the AAR here manually.
