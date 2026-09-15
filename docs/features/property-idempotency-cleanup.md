# Property idempotency cleanup

## Purpose

Property creation idempotency records are retained for 24 hours. A scheduled
cleanup prevents expired response snapshots from remaining active indefinitely
while preserving the common soft-delete and audit policy.

## Behavior

- The job runs every 15 minutes by default.
- One execution locks and selects at most 100 active rows whose `expires_at` is
  not later than the execution time.
- Selected rows are soft deleted with a BATCH actor and a deletion reason.
- Valid records and already deleted records are not selected.
- The generated `active_idempotency_key` applies uniqueness only to active rows,
  so a client may reuse a key after its retention period expires.
- An in-process guard prevents overlapping executions in one application
  instance. Database row locks protect the selected rows across instances.

## Deployment

Run `database/schema/014_add_property_idempotency_cleanup.sql` before deploying
the application. The migration replaces the original active-key uniqueness
rule and adds the cleanup lookup index.

The migration changes indexes and can lock a large table. Back up the database
and measure the operation on staging before production execution. If the
migration cannot be completed, keep the new application version undeployed and
restore the former index with a reviewed forward-fix statement.

## Configuration

`PROPERTY_IDEMPOTENCY_CLEANUP_FIXED_DELAY_MS` controls the fixed delay in
milliseconds. The default is `900000` (15 minutes).

## Verification

- `PropertyIdempotencyCleanupServiceTest`
- `PropertyIdempotencyCleanupJobTest`
- Existing `PropertyIdempotencyServiceTest`
