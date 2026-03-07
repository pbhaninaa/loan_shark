# UUID Migration Guide

This project has been migrated from numeric (BIGINT/Long) IDs to UUIDs (GUIDs) throughout the system.

## What Changed

### Database (Flyway V13)
- All primary key columns: `BIGINT` → `CHAR(36)` (UUID string)
- All foreign key columns: `BIGINT` → `CHAR(36)`
- Existing data is preserved: old IDs are replaced with new UUIDs via mapping tables during migration.
- **Single-row tables** (`business_capital`, `loan_interest_settings`) use fixed UUIDs so the app can find them:
  - Business capital: `00000000-0000-0000-0000-000000000001`
  - Loan interest settings: `00000000-0000-0000-0000-000000000002`

### Backend (Java)
- **Entities**: `@Id` and all ID/FK fields use `java.util.UUID`. IDs are generated in `@PrePersist` (or assigned for fixed config rows).
- **DTOs**: All `Long id` (and related IDs) replaced with `UUID`; JSON serialization uses string representation.
- **Repositories**: `JpaRepository<Entity, Long>` → `JpaRepository<Entity, UUID>`.
- **Services / Controllers**: Method signatures and path variables use `UUID` instead of `Long`.

### Frontend (Vue)
- IDs are now strings (e.g. `"550e8400-e29b-41d4-a716-446655440000"`). Existing code that uses `id` in URLs or payloads continues to work as long as it does not assume numeric type (e.g. `Number(id)` may be removed where IDs are only displayed or sent as strings).

## Running the Migration

1. **Back up your database** before running.
2. Start the application (or run Flyway); migration **V13** will run and convert all tables.
3. Restart the app after migration so the new schema is used.

## Rollback

There is no automatic rollback. To revert, restore the database from backup and use a version of the code before the UUID migration.

## Fixed UUIDs (config tables)

The app uses these constants for single-row tables:

- `BusinessCapital`: `00000000-0000-0000-0000-000000000001`
- `LoanInterestSettings`: `00000000-0000-0000-0000-000000000002`

Do not change these in the database without updating the corresponding constants in the Java code.
