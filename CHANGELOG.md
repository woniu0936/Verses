# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
