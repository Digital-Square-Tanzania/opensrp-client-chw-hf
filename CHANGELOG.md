
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Hide MARP/PrEP Enrollment before age 15. From All Clients, open the client profile for MARP clients so eligible clients can use the overflow action to reopen their saved MARP profile with a localized enrollment message ([#224](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/224)).
- Use MARP wording in English and Kiswahili across Kituoni enrollment, client navigation, services, and related forms ([#222](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/222)).
- Use MHR and WHR in Kituoni screening, enrollment, Client Status, and MARP profiles, with bilingual enrollment and Client Status options and the existing group codes ([#223](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/223)).

### Fixed

- Preselect and validate sole MHR and WHR enrollment choices, and restore and display saved secondary groups in Client Status, visit history and the MARP profile ([#223](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/223)).
- Filter eligible MARP enrollment groups in both languages using the screening form field ([#222](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/222)).
- Integrate Kituoni startup and post-login fixes with Rhino pinned to 1.7.13, CHW Core 2.2.0-MOTHER-MENTOR-ALPHA-53-UCSW-92-ENTRY-INTEGRATION-MOH-SNAPSHOT, and Android NDK 28.0.12433566 ([#222](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/222)).
