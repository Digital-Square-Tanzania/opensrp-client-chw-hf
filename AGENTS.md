# Repository Guidelines

## Project Structure & Module Organization
- Module: `opensrp-chw-hf` (Android app).
- Source: `opensrp-chw-hf/src/main/java` (package `org.smartregister.chw.hf`).
- Resources: `opensrp-chw-hf/src/main/res` and `AndroidManifest.xml`.
- Assets: JSON forms, rules, and reports in `opensrp-chw-hf/src/main/assets`.
- Tests: `opensrp-chw-hf/src/test/java` (JUnit/Robolectric). No `androidTest` by default.
- Flavors: `nacp` with `debug`/`release` build types.

## Build, Test, and Development Commands
- Build app (default debug): `./gradlew :opensrp-chw-hf:assembleDebug`
- Build flavor: `./gradlew :opensrp-chw-hf:assembleNacpDebug`
- Install to device: `./gradlew :opensrp-chw-hf:installNacpDebug`
- Unit tests: `./gradlew :opensrp-chw-hf:testNacpDebugUnitTest`
- Coverage report (JaCoCo): `./gradlew :opensrp-chw-hf:jacocoTestReport` (see `opensrp-chw-hf/build/reports/jacoco/...`).
- Root lint is disabled; use IDE inspections or run module lint explicitly if needed.

## Coding Style & Naming Conventions
- Language: Java 11 (AndroidX). Indent 4 spaces; K&R braces.
- Classes: PascalCase (`AncMemberProfileActivity`). Methods/fields: camelCase.
- Resources: layouts `activity_*`/`fragment_*`; strings and ids lower_snake_case.
- Packages under `org.smartregister.chw.hf.*`. Keep feature folders coherent.
- Format via IDE; keep imports organized and unused code removed.

## Testing Guidelines
- Frameworks: JUnit4, Robolectric, Mockito.
- Place unit tests in `src/test/java`, name with `*Test.java`.
- Prefer small, deterministic tests; mock Android dependencies.
- Aim to keep coverage from regressing; verify reports with JaCoCo task above.

## Commit & Pull Request Guidelines
- Commits: concise, imperative subject (≤72 chars) + context in body.
  - Example: `Fix ANC profile crash on null visit date`
- Reference issues (e.g., `Fixes #123`). Group related changes.
- PRs: clear description, screenshots for UI changes, steps to test, and linked issues.
- CI/CD: project uses coverage publishing; ensure tests pass locally before opening PR.

## Security & Configuration Tips
- Do not commit secrets. Configure tokens in `local.properties`, `gradle.properties`, or env vars (e.g., `MAPBOX_DOWNLOADS_TOKEN`, `GITHUB_ACTOR`/`GITHUB_TOKEN`).
- `github.properties` is supported locally; avoid pushing real credentials.
- Verify `google-services.json` and API keys are handled per environment.

