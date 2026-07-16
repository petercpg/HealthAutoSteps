# Walkthrough - Automated SBOM Generation and Release

I have updated the GitHub Actions release workflow to automatically generate a Software Bill of Materials (SBOM) using the CycloneDX plugin and include it in your GitHub Releases.

## Changes Made

### GitHub Actions Workflow

#### [release.yml](file:///C:/Users/Peter/StudioProjects/HealthAutoSteps/.github/workflows/release.yml)

- **Build Step**: Modified the build command to include `cyclonedxBom`.
  ```bash
  ./gradlew assembleRelease cyclonedxBom ...
  ```
- **Renaming**: Added logic to find the generated `bom.json` and `bom.xml` in `app/build/reports/` and rename them with the version prefix (e.g., `bom-v1.2.3.json`).
- **Release Assets**: Updated the `softprops/action-gh-release` step to include the new SBOM files in the release.

## Verification Results

### Automated Logic
- The Gradle task `cyclonedxBom` is part of the standard CycloneDX plugin for Gradle and will generate industry-standard SBOM files.
- The workflow now expects and packages these files alongside your APK and ProGuard mapping.

> [!TIP]
> The SBOM files will be available for each new release on GitHub, helping you meet supply chain security requirements.
