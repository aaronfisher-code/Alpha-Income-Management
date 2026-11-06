# Account payment edit and deletion safety

A failed invoice-number change previously mutated the desktop's selected table
record before the API saved it. Retrying after a duplicate-number error could
then target the other existing invoice. Edits now use a separate draft, preserving
the selected record's original invoice number, dates, amount and description until
an authoritative reload. Save and Delete remain blocked while a change and its
refresh are running.

Deletion previously used form encoding on a URL path: `19133 1` became
`19133+1`, and other reserved characters were encoded twice. The desktop now
passes the raw invoice number as a URI variable. After DELETE, it retrieves the
payment's original month and verifies that the selected store/invoice key is
absent. It then refreshes the displayed rows and totals before showing success.
This also detects a zero-row deletion reported as successful by an older API.
A refresh failure is reported separately; it does not leave an indefinite busy
indicator or display a success message.

The matching Alpha API returns:

- **409 Conflict** when an add or invoice-number edit collides with another
  payment in the same store, including another month. The existing database
  uniqueness constraint continues to protect both records.
- **404 Not Found** when a delete removes no row, or an update's original key
  no longer exists. An existing unchanged update remains valid even when the
  JDBC driver reports zero changed rows.

Invoice references remain unique by `(storeID, invoiceNo)` across all months.
For genuinely repeated external references, retain distinct internal references
and describe the relationship in the payment description. These changes do not
add database columns or migrate data. They do not add general payment audit
history or optimistic concurrency control for simultaneous edits by different
users. Invoice references containing slashes still need a separate endpoint
routing design; no routing guarantee has been added for them here.

## Rollout

Deploy the matching Alpha API first, then release the desktop update to all
staff. Older desktops retain the unsafe edit retry behavior until updated;
the API's duplicate/missing-row checks alone cannot establish which existing
record an older client intended to edit. These source changes have not themselves
been deployed or published.

## Verification

The desktop controller regression tests exercise the actual JavaFX edit handler:
two duplicate failures keep the original identity and values, an in-flight save
cannot be resubmitted, refresh failures cannot report success, and successful
refresh updates the table and totals before success. Service tests cover exact
encoding of spaces and reserved characters, an older API's false delete success,
and deletion/verification errors.

The API tests use an isolated H2 database in MySQL mode and the actual controller,
service and repository. They cover duplicate references across months, repeated
failed renames followed by a valid distinct reference, exact/store-scoped deletes,
updated totals, repeated/malformed deletes, and missing/unchanged updates.

Run from each repository:

```sh
mvn test
```

On a Java 27 development machine, Alpha API's existing Byte Buddy test dependency
needs `mvn -Dnet.bytebuddy.experimental=true test`. Its configured deployment
build targets Java 22. This runtime flag is for local verification and is not a
production application setting.
