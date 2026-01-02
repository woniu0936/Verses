# Contributing to Verses

First off, thank you for considering contributing to Verses! It's people like you that make the open-source community such an amazing place to learn, inspire, and create.

## Code of Conduct

By participating in this project, you are expected to uphold our Code of Conduct.

## How Can I Contribute?

### Reporting Bugs
- Use the **Bug Report** template when opening an issue.
- Describe the expected behavior and the actual behavior.
- Provide a minimal reproducible example if possible.

### Suggesting Enhancements
- Use the **Feature Request** template.
- Explain *why* this enhancement would be useful to most users.

### Pull Requests
1. **Fork** the repository and create your branch from `main`.
2. **Setup**: Run `./gradlew build` to ensure your environment is correct.
3. **Style**: Follow the "Engineering Excellence Guidelines" in `GEMINI.md`.
    - Use 100% idiomatic Kotlin.
    - Maintain 100% KDoc coverage for public APIs.
4. **Test**: Add unit tests for any new logic in `verses/src/test`.
5. **Verify**: Ensure all tests pass and linting is clean.
6. **Commit**: Use [Conventional Commits](https://www.conventionalcommits.org/).

## Development Workflow

- **JDK**: 17
- **Kotlin**: 2.2.21
- **Build System**: Gradle Kotlin DSL

## 🌌 Engineering Philosophy
- **Declarative**: We favor "What to show" over "How to show it".
- **Minimalist**: Keep the library footprint small. No external dependencies beyond AndroidX.
- **Performance**: Every allocation counts. Use `inline` and `reified` where appropriate.
