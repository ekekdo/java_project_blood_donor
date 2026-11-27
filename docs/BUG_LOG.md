# Bug Log

| ID | Bug | Root cause | Fix in current code | Status |
|----|-----|------------|---------------------|--------|
| BUG-01 | The first database call returned HTTP 500 | `pom.xml` had `packagingExcludes` removing `WEB-INF/classes/db.properties` from the WAR, so `DBUtil` could not find its config at runtime | Removed the exclusion and documented that the WAR must contain `db.properties` | Fixed |
| BUG-02 | Every dashboard could loop until HTTP 500 | Prefix servlet mappings such as `/admin/*` also matched the forwarded JSP `/admin/dashboard.jsp`, causing the servlet to call itself again | DashboardServlet now uses exact URLs: `/admin/dashboard`, `/donor/dashboard`, `/patient/dashboard`, `/hospital/dashboard`; AuthFilter still protects the areas | Fixed |
| BUG-03 | Validation and authentication messages were lost | Error handlers redirected, which starts a new request and discards request attributes | RegisterServlet, LoginServlet and DashboardServlet forward to the JSP with request attributes; redirects are used only after successful changes where a flash message is stored in the session | Fixed |
| BUG-04 | Patient registration did not save to `patients` | `AuthService.register()` inserted only the `users` row; no patient profile insert existed even though `db/schema.sql` already defines `patients` | Added `RegistrationDao` / `JdbcRegistrationDao`; registration now inserts the user row and exactly one donor, hospital or patient profile row using one JDBC connection with `setAutoCommit(false)`, `commit()`, and `rollback()` on failure. `db/schema.sql` was not changed | Fixed |
| BUG-05 | Donor eligibility used an off-by-one rule | Java used `isBefore(today)` and SQL used `< DATE_SUB(...)`, so a donor exactly 90 days after the previous donation was still blocked | Java now treats exactly 90 days as eligible and SQL uses `<= DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY)` | Fixed |

Runtime confirmation of the fixes is recorded in `docs/TESTING.md`; automated no-database checks are run with `mvn -q test`.
