# Emergency Blood & Donor Coordination System

BloodConnect is a university Java web application that connects patients and hospitals with registered blood donors. It uses Java 17, Jakarta Servlets 6.0, JSP/JSTL, plain JDBC, MySQL 8, BCrypt password hashing, and JUnit 5.

## Features

- Role-based accounts: Admin, Donor, Hospital, Patient
- Secure registration with BCrypt password hashing and field-level validation
- Hospital accounts start unverified and cannot sign in until an admin verifies them
- Polymorphic dashboards selected by `User.getDashboardPath()`
- Session management with session-fixation protection and role-based `AuthFilter` guards
- Blood compatibility lookup using `EnumMap<BloodGroup, Set<BloodGroup>>`
- Donor eligibility rule with a 90-day rest period
- Public donor eligibility checker at `/eligibility`
- Atomic registration: the `users` row and role-profile row commit together or roll back together
- PreparedStatement-only JDBC with SQL stored as complete string literals/text blocks
- Custom 403/404/500 error page and no stack traces shown to users

## Prerequisites

| Tool | Version | Verify |
|------|---------|--------|
| JDK | 17 (or newer with `maven.compiler.release=17`) | `java -version` |
| Apache Maven | 3.9+ | `mvn -version` |
| MySQL Server | 8.0 | `mysql -V` |
| Servlet container | Tomcat 10.1+ | deployed WAR |

## Database Setup

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/sample_data.sql
```

Copy the example config and fill in the local MySQL credentials:

```bash
copy src\main\resources\db.properties.example src\main\resources\db.properties
```

`src/main/resources/db.properties` is ignored by Git. Never commit real credentials.

## Build and Test

```bash
mvn -q test
mvn -q package
```

The JUnit 5 suite contains 49 no-database tests; all 49 pass with `mvn -q test`.

The WAR is produced as `target/blood-connect.war`. Deploy that file to Tomcat 10.1+ and open:

```text
http://localhost:8080/blood-connect/
```

## Test Accounts

Seed accounts come from `db/sample_data.sql`.

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Admin | admin@blood.com | Admin@123 | Seed-only account |
| Hospital, verified | reception@citygeneral.in | Hospital@123 | Can sign in |
| Hospital, unverified | contact@sahyadri.org.in | Hospital@123 | Cannot sign in |
| Donor | aarav@example.in | Donor@123 | O-, Mumbai |
| Patient | sanjay@example.in | Patient@123 | Pune |

## Manual Testing

See `docs/TESTING.md` for 20 manual test cases. Fill in the blank `Result` column while testing.

## Known Bugs and Fixes

See `docs/BUG_LOG.md` for the four real defects found during review and their fixes.

## Project Structure

```text
src/main/java/com/bloodconnect/
├── model/       User hierarchy, DonorUser, HospitalUser, PatientUser, AdminUser, UserFactory
├── enums/       Role, BloodGroup, AccountStatus, RequestStatus, ResponseStatus, UrgencyLevel, ProtectedArea
├── dao/         CrudDao, UserDao, DonorDao, HospitalDao, RegistrationDao
├── dao/impl/    UserDaoImpl, DonorDaoImpl, HospitalDaoImpl, JdbcRegistrationDao
├── service/     AuthService, DonorEligibilityService
├── servlet/     RegisterServlet, LoginServlet, LogoutServlet, DashboardServlet, EligibilityServlet, AppContextListener
├── filter/      AuthFilter
├── util/        DBUtil, SessionUtil, BloodCompatibility, AbstractBaseDao
├── exception/   BloodAppException and domain-specific subclasses
src/main/webapp/
├── index.jsp, login.jsp, register.jsp, error.jsp
├── admin/, donor/, hospital/, patient/    Dashboard JSPs
├── assets/                               CSS and favicon
└── WEB-INF/
    ├── web.xml
    └── views/
        ├── common/                       Shared header and footer
        ├── dashboard-common.jspf
        └── eligibility.jsp               Innovation page
src/test/java/com/bloodconnect/            JUnit 5 tests
db/schema.sql, db/sample_data.sql           Schema and seed data
docs/TESTING.md, docs/BUG_LOG.md             Review documentation
```

## Team and Contributions

| Team member | Roll number | Contribution |
|-------------|-------------|--------------|
| TBD | TBD | TBD |
| TBD | TBD | TBD |
| TBD | TBD | TBD |
| TBD | TBD | TBD |
