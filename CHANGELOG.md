# Changelog

## Unreleased

## 0.1.3

### Changed

- Reworked Compose Up with project-scoped networks and volumes, `.env` interpolation, dependency ordering, safe
  reconciliation, and rollback on partial failure.
- Compose projects now use labels and configuration fingerprints to preserve unchanged running services and recreate
  changed services safely.
- Added Apple Container DNS-domain detection with actionable guidance when service-name discovery has not been
  configured.

### Fixed

- Table container isn’t present in the list

## 0.1.2

### Changed

- Replaced Containers, Images, Networks, and Volumes IDE action toolbars with native controls that update immediately
  with selection and runtime state.
- Grouped tool-window panel and toolbar sources by tab, and extracted shared toolbar execution, loading, and JSON
  preview components.
- Removed obsolete action registrations and implementations are now replaced by native toolbars.
- Optimize parseExposedPorts

## 0.1.1

### Changed

- WorkingDir now will be free text input instead of a picker.

## 0.1.0

### Breaking Changes

- Move ToolWindow into `Service Contributor`

## 0.0.3

### Fix

- Runline marker on Dockerfile are not executed properly.

## 0.0.2

### Added

- Add support for parsing `EXPOSE [port/protocol]` patterns.
- Add support for parsing `EXPOSE map [port/protocol]` patterns.
- Support build from `dockerfile`

### Fixed

- Duplicate Run line marker on Dockerfile

## 0.0.1

### Added

- Initial release.
