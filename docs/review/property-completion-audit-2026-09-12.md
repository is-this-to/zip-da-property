# Property backend completion audit (2026-09-12)

## 1. Audit scope and precedence

- Baseline: final v1.6 Property SRS, API specification, and database definition artifacts.
- Implementation: `origin/dev` at `a72529d` and the source, tests, configuration, and schema files present in this repository.
- User-approved deviation: Property events use direct Kafka publication. The Outbox table and Outbox pattern are intentionally excluded even though the final v1.6 baseline describes them.
- User-approved contract override: APIs owned by 임호탁 use the `/api/property/properties/**` service path. This path is not an incomplete item in 임호탁's scope.
- This document does not treat an unmerged remote feature branch as completed code.

The audit checks the whole Property backend, not only one developer's assigned features. The September 11 completion target has passed; the remaining work below must therefore be treated as completion blockers rather than later phases.

## 2. Executive conclusion

The core Property write flows are in good condition: creation, partial update, transaction/publication status transitions, soft delete/restore, edit detail, my-list cursor pagination, address/public-location integration, Member permission synchronization, verification lifecycle, evidence cleanup, expiry, renewal notification, audit recording, and direct Kafka publication are present and well covered by unit tests.

The whole final backend is not 90–99% complete. Against the final 49-API catalog and complete database scope, the evidence-based estimate is **about 55–60%**. This is a range rather than a fabricated exact value:

- 8 API contracts match the catalog directly or use the user-approved 임호탁 service path.
- 13 catalog functions have an implementation or architectural equivalent, but their URL/method/shape still requires the relevant owner's contract decision.
- 27 catalog APIs are absent from `dev`.
- 1 report API exists only on an old, unmerged teammate branch.
- 19 schema tables are created locally versus 33 expected baseline tables after intentionally excluding Outbox; several missing tables belong to teammate domains.
- The full suite passes on the latest `dev`: 332 tests, 0 failures, 0 errors, and 3 intentionally skipped integration tests.
- Against the fixed scope assigned to 임호탁, implementation is complete. The broader whole-backend estimate below includes other owners' API and schema areas and must not be used to reduce that assigned-scope completion figure.

The earlier 90–99% numbers measured a narrower sequence of the current developer's feature tasks. They must not be used as whole-backend completion percentages.

## 3. Highest-priority findings

### High — Other owners' public API contracts still require coordination

The user confirmed `/api/property/properties/**` as the correct path for APIs owned by 임호탁. Those endpoints must not be treated as incomplete due to the final workbook's shorter prefix.

The remaining Region, Location, File, Map, and Option path differences belong to the relevant API owners. Before bulk renaming their paths, those owners must decide which source is authoritative and align gateway routes, clients, documentation, and tests together.

Do not implement or rename another owner's endpoints as part of 임호탁's remaining work.

### Critical — Clean database creation is incomplete and ordering is ambiguous

- JPA entities exist for `region` and `region_boundary`, but no current `database/schema` file creates either table.
- There are two files numbered `007`: image and verification.
- `database/README.md` lists `007_create_property_verification_tables.sql` but omits `007_create_property_image_table.sql`.
- A deployment that follows the README can therefore miss the image table, and a clean environment cannot create all entity dependencies from this repository alone.

Required correction: add or restore the Region DDL source, renumber the duplicate migration, update the documented execution order, and validate the complete schema on a new empty database.

### High — Final API catalog coverage is incomplete

The status of all 49 catalog entries is summarized below.

| Status | API numbers | Count | Notes |
|---|---:|---:|---|
| Direct or user-approved contract | 10–15, 18–19 | 8 | 임호탁 property service path is approved; My properties and favorite APIs match directly |
| Functional implementation with owner decision/equivalent | 1–4, 6–7, 34–35, 37–39, 48–49 | 13 | Other-owner prefixes need their decision; verification is generic; Member integration is Kafka rather than HTTP |
| Missing from `dev` | 5, 8–9, 16–17, 20–33, 36, 41–47 | 27 | Validation endpoint, public list/detail, complexes, search state/history/preferences, file delete, report/admin report APIs, exact-location internal API |
| Only on unmerged remote branch | 40 | 1 | `origin/feature/report_JSL` contains an old report-create implementation only |

The implementation also has extra APIs not represented in the 49-entry catalog, including publication-status change, restore, and administrator verification review. These may be valid additions, but they do not compensate for missing catalog contracts.

### Resolved in assigned scope — Duplicate/fraud detection

FR-PR-038 registration-time risk evaluation is implemented. It checks normalized address, address/price/publisher repetition, image SHA-256 reuse, and forbidden text patterns before persistence. The decision and rule codes are stored in `property_risk_assessment`, blocked registrations do not persist a Property, and unit plus repository integration tests cover the behavior.

### High — Retention and anonymization jobs are incomplete

- Expired idempotency records are now soft-deleted by a scheduled, bounded cleanup job. This item is resolved in 임호탁's scope.
- The requirement to delete or anonymize exact address/original coordinate data after the public-retention period was not found.
- Verification evidence retention and expiry processing are implemented; this finding concerns the other retained data.

### High — Header authentication depends entirely on gateway isolation

`HeaderAuthenticationFilter` accepts `X-User-Id` and `X-User-Role` directly and creates an authenticated principal, while `SecurityConfiguration` permits every request. This is safe only if the Property service is unreachable from untrusted networks and the gateway strips and reissues those headers.

Required operational evidence: gateway header-removal policy, service network isolation, and an integration/security test showing that callers cannot forge internal identity headers.

### Medium — Operational resilience and performance evidence is incomplete

- Kakao Local and Member clients configure connection/read timeouts.
- No retry/circuit-breaker policy was found for Kakao Local failures.
- No Micrometer/Actuator metrics, timers, counters, or documented alert thresholds were found.
- No load/performance test proving catalog latency targets was found.
- Optimistic-lock and active-idempotency uniqueness now have opt-in, isolated MySQL concurrency tests. Load/performance evidence remains absent.

### Medium — Complete versioned OpenAPI artifact is not checked in

The assigned Property write APIs now have generated `/api-docs` contract tests for paths, methods, success statuses, required headers, and representative errors. A complete, versioned OpenAPI file covering all 49 cross-team APIs is still not checked in and requires team-wide ownership.

## 4. Database scope comparison

Current schema scripts create these 19 tables:

`property`, `property_address`, `property_audit_event`, `property_favorite`, `property_file`, `property_idempotency`, `property_image`, `property_member_event_consumption`, `property_member_state`, `property_option`, `property_option_code`, `property_option_history`, `property_publisher_snapshot`, `property_revision`, `property_risk_assessment`, `property_status_history`, `property_type_option`, `property_verification`, `property_verification_evidence`.

Tables expected by the final baseline but absent from current schema scripts include:

- Region/transit: `region`, `region_boundary`, `region_import_batch`, `subway_line`, `subway_station`, `subway_station_line`, `property_subway_access`
- Complex: `apartment_complex`, `apartment_complex_external_ref`, `apartment_complex_facility`
- Search: `property_saved_search`, `property_search_history`, `property_search_preference`
- Report: `property_report`, `property_report_action`, `property_report_appeal`, `property_report_evidence`
- Intentionally excluded: `property_outbox`

`property_member_event_consumption` and `property_member_state` are newer architectural additions and are not baseline defects.

## 5. Verified strengths

- Optimistic version control and conflict responses are implemented across mutation flows.
- Idempotency protects property creation and distinguishes replay from conflicting payloads.
- Soft delete and restore record revision/audit information.
- Exact and public locations are separated; exact location and address hash are not exposed in public DTOs reviewed during this work.
- Member state is synchronized by Kafka with duplicate-event consumption protection.
- Verification supports owner/tenant flows, administrator decisions, expiry, evidence cleanup, and renewal notifications.
- Property audit and event publication use direct Kafka as explicitly requested, with no Outbox usage.
- The application uses database-managed DDL (`ddl-auto: none`) and explicit schema scripts.
- FR-PR-038 pre-registration risk assessment, expired-idempotency cleanup, write observability, and generated OpenAPI contract checks are implemented.
- Existing coverage is substantial; the latest full suite completed 332 tests with 0 failures, 0 errors, and 3 opt-in integration tests skipped by default.

## 6. Assigned-scope completion and remaining coordination

임호탁's fixed implementation scope is complete at `a72529d`. Do not lower that percentage because of the remaining cross-team or operational items below, and do not implement another owner's features without coordination.

1. Apply the repository DDL set to each development/deployment database in documented order and verify schema drift before integration testing.
2. Coordinate the remaining Region/search/report/file/complex/public-query contracts with their owners.
3. Confirm gateway identity-header stripping, service network isolation, Kafka broker/topic settings, and log alert configuration in the deployment environment.
4. Decide team-wide ownership for the complete versioned OpenAPI artifact and clean-database migration verification.

## 7. Accepted deviations and remaining decisions

- **Accepted:** direct Kafka publication without Outbox, per explicit user decision. Known trade-off: a database commit can succeed while publication fails, so event loss/recovery behavior must be accepted or separately mitigated.
- **Accepted:** 임호탁-owned Property APIs use `/api/property/properties/**`.
- **Decision required:** Member inbound integration through Kafka instead of API 48.
- **Implemented in assigned scope:** owner, tenant, and reverification submission endpoints plus administrator review use the approved Property service path and documented success statuses.
- **Coordination required:** ownership and integration plan for report, search, complex, Region/subway, and public query domains.

## 8. Verification record

- `git fetch origin`: successful; PR #67 is merged as `a72529d`.
- Local `dev` fast-forward: successful from `be346fb` to `a72529d`; local and remote `dev` are identical.
- Static source/config/schema inspection: completed.
- Final API catalog comparison: 49 APIs categorized.
- Current schema inventory and JPA table mapping comparison: completed.
- `./gradlew.bat test --rerun-tasks`: successful on `a72529d` in 1m 6s; 55 suites, 332 tests, 0 failures, 0 errors, 3 intentionally skipped integration tests.
- Isolated MySQL concurrency verification: 2 tests passed using the guarded `zipdav2_concurrency_test` database, which was removed afterward.
- Risk repository integration verification: failed because local `zipdav2` has not applied all repository DDL (`property_image` missing and `property_verification.renewal_notified_at` missing). This is classified as local schema drift, not a query-code failure; apply the current schema scripts before rerunning.
- Final Git checks: clean working tree before this documentation update, no whitespace errors, and `dev...origin/dev` was `0 0`.
