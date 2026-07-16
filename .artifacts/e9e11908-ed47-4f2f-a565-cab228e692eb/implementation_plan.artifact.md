# Implementation Plan - Automate SBOM Generation and Publication

This plan outlines the steps to automate the generation of a Software Bill of Materials (SBOM) using the CycloneDX Gradle plugin and include it as an asset in GitHub Releases.

## Proposed Changes

### GitHub Actions Workflow

#### [MODIFY] [release.yml](file:///C:/Users/Peter/StudioProjects/HealthAutoSteps/.github/workflows/release.yml)

- Add a step to generate the SBOM using the `./gradlew :app:cyclonedxBom` command.
- Update the "Rename Artifacts" step to handle the SBOM files (rename them to include the version number for clarity).
- Update the "Create Release" step to include the SBOM files in the release assets.

## Verification Plan

### Manual Verification
- Trigger the "Android Release (Production)" workflow manually (via `workflow_dispatch`).
- Verify that the resulting GitHub Release contains:
    - `HealthAutoSteps-v<version>.apk`
    - `mapping-v<version>.txt`
    - `bom-v<version>.json`
    - `bom-v<version>.xml`
