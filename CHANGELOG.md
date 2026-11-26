
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Suggest an editable Negative HIV result for the eligible first Kituoni MARP visit and require provider verification before recording it ([#244](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/244)). No version or dependency changes.
- Record pregnancy or breastfeeding status in Kituoni MARP Client Status for women in the maternal high-risk group. Prefill the first visit from screening and make the saved current or latest status available to other visit forms ([#239](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/239)).
- Start Kituoni MARP screening for female clients with required pregnancy and breastfeeding questions in English and Kiswahili. Route applicable Yes answers to the dedicated High Risk branch ([#225](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/225)).
- Add the bilingual pregnancy and breastfeeding High Risk checklist and its sole dominant MARP group. Save the applicable intake answers with enrollment and show the existing no-group outcome when None is selected ([#226](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/226)).

### Changed

- Resolve the existing OpenSRP Plan Evaluator `0.0.19-SNAPSHOT` from a repository restricted to that exact coordinate, with the upstream Apache-2.0 attribution and license (UCSW-48).
- Hide Family Planning in Kituoni MARP Biomedical Services while the client is pregnant, allow submission without it, and restore it in the same visit after saving a breastfeeding or non-pregnant status ([#246](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/246), UCSW-48).
- Classify and process otherwise complete pregnant Biomedical visits without a Family Planning answer. Use Core `2.2.0-KITUONI-UCSW-48-VISIT-9-SNAPSHOT`, which exports the KVP completion repair. The facility app version remains 1.4.0 (67) (UCSW-48).

- Apply English and Kiswahili register wording to Kituoni MARP Biomedical visit forms and list Negative before Positive for HBV and HCV ([#245](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/245), UCSW-49). No version or dependency changes.

- Use the bilingual register wording and result order for Kituoni MARP HTS. Record refusal to receive results as a distinct outcome without requiring CTC data ([#242](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/242)). No version or dependency changes.

- Simplify Kituoni MARP HTS by removing Client Type and the post-test counselling question. Require a bilingual CTC ID for either CTC enrolment option on Positive and Known positive results, and format pasted 14-digit IDs with leading zeros preserved ([#241](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/241)). No version or dependency changes.

- Use the bilingual Other MARP categories checklist in Kituoni Client Status. Filter register groups by dominant group, sex and age, preserve eligible screening defaults and require an exclusive None or at least one group ([#240](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/240)). No version or dependency changes.

- Use the bilingual Visit Type labels in Kituoni MARP Client Status. Save a first Transfer in or Return programme date as the MARP enrolment date, show it on the profile and retain it when re-saving the same visit type through Visit History. Use the register date for monthly enrolment reporting and backfill missing system dates from registration events in database version 32 ([#238](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/238)).
- Keep partial Kituoni MARP visits available from the profile. Return to the profile and hide the visit actions after all four service cards are submitted, then restore the follow-up action on the next device calendar day ([#237](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/237)).
- Use the bilingual MARPS register group names and eligibility rules for the dominant group at Kituoni enrollment. Save the selected group and the remaining eligible groups under their existing codes ([#235](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/235)).
- Update the Kituoni vAGYW checklist and make it required when shown. Offer the localized enrollment option for qualifying answers and clear stale criteria after backtracking ([#232](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/232)).
- Ask a single bilingual sexual intercourse question in Kituoni MARP screening. Route No directly to Other Groups and Yes to Discordant Couple ([#231](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/231)).
- Show None in English and Hakuna in Kiswahili for the exclusive, non-qualifying vAGYW screening answer in Kituoni ([#230](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/230)).
- Replace Kituoni MARP Other Groups with a bilingual checklist and add enrollment groups for long-distance truck drivers, fisherfolk, miners, and other key and vulnerable populations. Keep legacy enrollment codes and labels available ([#233](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/233)).
- Move bilingual Discordant Couple screening after Sexual Behaviour in Kituoni and offer the Discordant partner group when a qualifying partner parameter is selected ([#228](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/228)).
- Use bilingual Drug Use screening wording and show the matching dominant enrollment option for injecting or non-injecting drug use in Kituoni ([#227](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/227)).
- Hide MARP/PrEP Enrollment before age 15. From All Clients, open the client profile for MARP clients so eligible clients can use the overflow action to reopen their saved MARP profile with a localized enrollment message ([#224](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/224)).
- Use MARP wording in English and Kiswahili across Kituoni enrollment, client navigation, services, and related forms ([#222](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/222)).
- Use MHR and WHR in Kituoni screening, enrollment, Client Status, and MARP profiles, with bilingual enrollment and Client Status options and the existing group codes ([#223](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/223)).

### Fixed

- Preserve a first Kituoni MARP visit’s New type across repeated Biomedical Visit History edits ([#242](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/242), UCSW-41). No version or dependency changes.
- Show the localized Discordant Couple checklist error when either Next control is tapped and allow the user to continue after selecting parameters ([#228](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/228)).
- Clear a skipped injecting answer and keep the reselected dominant MARP group when saving after Drug Use backtracking ([#227](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/227)).
- Show the localized required-field prompt when Next is tapped with an empty pregnancy and breastfeeding High Risk checklist, and clear the prompt after a selection ([#226](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/226)).

- Preselect and validate sole MHR and WHR enrollment choices, and restore and display saved secondary groups in Client Status, visit history and the MARP profile ([#223](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/223)).
- Filter eligible MARP enrollment groups in both languages using the screening form field ([#222](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/222)).
- Integrate Kituoni startup and post-login fixes with Rhino pinned to 1.7.13, CHW Core 2.2.0-MOTHER-MENTOR-ALPHA-53-UCSW-92-ENTRY-INTEGRATION-MOH-SNAPSHOT, and Android NDK 28.0.12433566 ([#222](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/222)).

### Removed

- Remove the PrEP Assessment page and its two questions from male and female Kituoni MARP screening. Continue directly from Other Groups to MARP Enrollment ([#234](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/234)).
- Remove the separate Mobile Population question and retire its group for new Kituoni MARP screening and enrollment while preserving existing enrollments ([#230](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/230)).
- Remove the prisoner/inmate question and retire Prisoners and Rumandee as choices for new Kituoni MARP screening and enrollment while preserving existing enrollments ([#229](https://github.com/Digital-Square-Tanzania/opensrp-client-chw-hf/issues/229)).
