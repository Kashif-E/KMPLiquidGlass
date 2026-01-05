# Contributing to KMP Liquid Glass

Thank you for your interest in contributing to KMP Liquid Glass! This document provides guidelines and instructions for contributing.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Making Changes](#making-changes)
- [Submitting Pull Requests](#submitting-pull-requests)
- [Coding Standards](#coding-standards)
- [Testing](#testing)

## Code of Conduct

Please be respectful and constructive in all interactions. We welcome contributors of all experience levels.

## Getting Started

### Prerequisites

- **JDK 21** or higher
- **Android Studio** (latest stable) or **IntelliJ IDEA** with Kotlin Multiplatform plugin
- **Xcode** (for iOS development, macOS only)
- **Node.js** (for web/Wasm builds)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/AndroidLiquidGlass.git
   cd AndroidLiquidGlass
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/Kashif-E/AndroidLiquidGlass.git
   ```

## Development Setup

### Building the Project

```bash
# Build all modules
./gradlew build

# Build specific platform
./gradlew :backdrop:assembleRelease          # Android library
./gradlew :catalog:androidApp:assembleDebug  # Android app
./gradlew :catalog:desktopApp:jar            # Desktop app
```

### Running the Catalog App

```bash
# Android
./gradlew :catalog:androidApp:installDebug

# Desktop
./gradlew :catalog:desktopApp:run

# Web
./gradlew :catalog:webApp:wasmJsBrowserRun
# Or use the convenience script:
./run-web.sh
```

### iOS Development

1. Open `catalog/iosApp/iosApp.xcodeproj` in Xcode
2. Build and run on simulator or device

## Project Structure

```
KMPLiquidGlass/
├── backdrop/                    # Core library (published to Maven Central)
│   └── src/
│       ├── commonMain/          # Shared Kotlin code
│       ├── androidMain/         # Android-specific implementations
│       ├── skiaMain/            # Shared Skia code (iOS, Desktop, Web)
│       ├── iosMain/             # iOS-specific code
│       ├── desktopMain/         # Desktop JVM code
│       └── wasmJsMain/          # Web/Wasm code
├── catalog/                     # Demo application
│   ├── sharedUI/                # Shared UI components
│   ├── androidApp/              # Android demo app
│   ├── desktopApp/              # Desktop demo app
│   ├── webApp/                  # Web demo app
│   └── iosApp/                  # iOS demo app (Xcode)
└── gradle/
    └── libs.versions.toml       # Dependency version catalog
```

## Making Changes

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring

### Workflow

1. Sync with upstream:
   ```bash
   git fetch upstream
   git checkout master
   git merge upstream/master
   ```

2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Make your changes and commit:
   ```bash
   git add .
   git commit -m "feat: add new backdrop effect"
   ```

4. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

## Submitting Pull Requests

### PR Checklist

- [ ] Code compiles without errors on all platforms
- [ ] New public APIs are documented with KDoc
- [ ] Changes follow existing code style
- [ ] Commit messages follow conventional commits format
- [ ] PR description explains the changes and motivation

### Commit Message Format

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting, no code change
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance tasks

Examples:
```
feat(backdrop): add radial blur effect
fix(android): resolve crash on API 21
docs: update installation instructions
```

## Coding Standards

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `val` over `var` when possible
- Prefer immutable collections
- Document public APIs with KDoc

### Compose Guidelines

```kotlin
// ✅ Good: Modifier as first optional parameter
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
)

// ✅ Good: State with remember
var isPressed by remember { mutableStateOf(false) }

// ❌ Bad: State without remember
var isPressed by mutableStateOf(false)
```

### Platform-Specific Code

- Place shared code in `commonMain`
- Use `skiaMain` for code shared between iOS, Desktop, and Web
- Use `expect`/`actual` for platform abstractions
- Android uses native Android Graphics APIs
- Skia platforms use `org.jetbrains.skia` APIs

## Testing

### Running Tests

```bash
# All tests
./gradlew test

# Android tests
./gradlew :backdrop:testDebugUnitTest

# Desktop tests
./gradlew :backdrop:desktopTest
```

### Visual Testing

For UI changes, please include screenshots or recordings showing:
- Before and after (if modifying existing behavior)
- Different platforms (at minimum Android and one Skia platform)
- Edge cases (different screen sizes, dark/light mode)

## Questions?

- Open an issue for bugs or feature requests
- Start a discussion for questions or ideas

Thank you for contributing! 🎉
