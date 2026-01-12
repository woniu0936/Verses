# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-01-12

### Performance (Major)
- **Kernel Overhaul**: Implemented instance-level ViewType caching in `VerseModel`, reducing `getItemViewType` lookup cost by ~95% (O(1) access).
- **Hot Path Optimization**: Removed redundant O(n) prototype searches in `VerseAdapter` during scrolling.
- **Autonomous Engine**: Refined `VersePreloader` and `VerseRecycledViewPool` to support burst-mode recovery and thread-safe idempotent scaling.

### Added
- **Lifecycle Callbacks**: Added `onCreate` callback to `VerseModel` and DSL for one-time initialization (e.g., setting listeners) to avoid allocation in `onBind`.
- **Preload API**: Exposed manual `VersePreloader.preload()` API for proactive view warming before heavy page transitions.

### Changed
- **API Update**: `VerseModel.getViewType()` is now `final` for performance stability. Subclasses should implement `resolveViewType()`.
- **Memory Safety**: Fixed potential Context leak in `VerseAdapterRegistry` using WeakReference.

## [1.0.0] - 2026-01-05

### Major
- **Stable Release**: First industrial-grade stable release of Verses.
- **Compose Alignment**: Renamed APIs to align with Jetpack Compose (`composeColumn`, `composeRow`, etc.) for reduced cognitive load.

### Features
- **Lifecycle**: Added `onAttach` / `onDetach` hooks for visibility tracking (e.g., video auto-play).
- **Pooling**: Introduced `contentType` in DSL for fine-grained control over View pooling beyond class types.
- **Safety**: Integrated binary-compatibility-validator to prevent accidental API breakage.

### Performance & Fixes
- **Memory**: Implemented stateless proxy listeners and localized factories to prevent Context leaks.
- **Diffing**: Fixed `DiffUtil` reliability by ignoring `onClick` lambdas in equality checks, ensuring proper layout updates.
- **Architecture**: Removed experimental memoization-based anchors in favor of a cleaner, re-evaluation based model.

## [1.0.0-alpha6] - 2026-01-02

### Added
- Enhanced GitHub Release workflow with rich release notes and artifact descriptions.
- Improved `release.sh` with remote tag existence checks and conditional commits.

### Fixed
- **Determinism**: Refactored ViewType generation to be based on key hash codes, ensuring stability across shared `RecyclerViewPool` instances.
- **Memory Safety**: Improved recursive cleanup of nested `RecyclerView` adapters to prevent context leaks.

### Changed
- Streamlined `release.sh` to focus on core tagging and pushing logic while maintaining version synchronization.

## [1.0.0-alpha5] - 2026-01-01

### Added
- Initial implementation of the declarative DSL.
- Support for ViewBinding and Custom View rendering.
- StaggeredGrid and GridLayout span support.
- Static spacing decoration and automatic global resource pooling.
