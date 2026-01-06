---
name: 🐛 Bug Report
about: Create a report to help us improve Verses
title: "[BUG] "
labels: bug
assignees: ''

---

### ⚠️ Pre-Flight Checklist
- [ ] I have searched existing issues to ensure this is not a duplicate.
- [ ] I am using the latest version of Verses.
- [ ] I have tried cleaning the project (`./gradlew clean`).

### 💻 Environment Information
- **Verses Version:** [e.g. 1.0.0]
- **Android OS Version:** [e.g. Android 13]
- **Device / Emulator:** [e.g. Pixel 6 Pro]
- **Kotlin Version:** [e.g. 1.9.0]

### 🧨 Stack Trace / Crash Log
*Please paste the full stack trace inside the code block below. Do not use screenshots for logs.*
```text
java.lang.Exception: ...
```

### 🔍 Minimal Reproducible Code
*Please provide the specific `compose` block or setup code that causes the issue.*
```kotlin
// Your Verse DSL code here
recyclerView.composeColumn {
    item(...) { ... }
}
```

### 📝 Description & Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

### 预期结果 (Expected Behavior)
A clear and concise description of what you expected to happen.

### 📸 Screenshots / Video
If applicable, add screenshots to help explain your problem.