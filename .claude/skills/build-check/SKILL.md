---
name: build-check
description: Run a debug build and report results in a structured way. Use to verify the project compiles after making changes.
user-invocable: true
allowed-tools: Bash(gradlew.bat *)
---

# Build Check Skill

Run a debug build of the Feedflow project and report the results.

## Steps

### 1. Run the build

```bash
gradlew.bat assembleDebug 2>&1
```

Use a timeout of 5 minutes (300000ms) since Android builds can be slow.

### 2. Report results

**If the build succeeds**, report:
```
BUILD SUCCESSFUL
```

**If the build fails**, analyze the output and report:

1. **Error summary**: one-line description of what went wrong
2. **File and line**: the exact file path and line number where the error occurred
3. **Error message**: the compiler/build error message
4. **Suggested fix**: what needs to change to fix it

Format:
```
BUILD FAILED

Error: [summary]
Location: [file:line]
Message: [compiler message]
Fix: [suggested action]
```

If there are multiple errors, list all of them. Focus on the FIRST error since later errors are often cascading failures.

### 3. Optionally fix

If the user asked you to fix build errors (not just check), go ahead and fix them after reporting. Then re-run the build to verify.

## Notes

- Use `gradlew.bat` (not `./gradlew`) — this is a Windows environment
- Build output can be very long; focus on the error lines (lines containing `error:`, `FAILED`, or `e:`)
- Kapt/KSP annotation processor errors (Hilt, Room) often appear before the actual Kotlin compilation errors — read them carefully
- If the build hangs, it may be a Gradle daemon issue — try `gradlew.bat --stop` then rebuild
