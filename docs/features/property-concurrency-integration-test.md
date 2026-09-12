# Property concurrency MySQL integration test

## Purpose

The unit tests cover conflict handling, but mocks cannot prove the database
UNIQUE and JPA optimistic-lock behavior under real concurrent transactions.
This test runs two independent transactions against an isolated MySQL database.

## Verified scenarios

- Two active idempotency rows with the same member, endpoint, and key are
  inserted concurrently. Exactly one transaction succeeds.
- Two transactions load the same Property version and update it concurrently.
  Exactly one transaction succeeds, the other receives an optimistic-lock
  failure, and the stored version advances once.

## Isolation and safety

The test does not use the normal `DB_NAME`. It only runs when
`PROPERTY_CONCURRENCY_TEST_DB_NAME` exists and ends with `_test`.
It drops and recreates that dedicated database, applies only the required
Property and idempotency DDL, runs the tests, and drops the database afterward.

Never set `PROPERTY_CONCURRENCY_TEST_DB_NAME` to a development, staging, or
production database. The database user needs temporary test database
CREATE/DROP permission.

## Local execution

Load the normal `.env` values into the current process, set a dedicated name,
and run:

```powershell
$env:PROPERTY_CONCURRENCY_TEST_DB_NAME = "zipdav2_concurrency_test"
.\gradlew.bat test --tests "com.zipdaproperty.domain.property.concurrency.PropertyConcurrencyMySqlIntegrationTest" --rerun-tasks
```

When the environment variable is absent, the test is skipped so ordinary unit
test runs do not create or drop a database.
