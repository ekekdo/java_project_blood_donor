<<<<<<< HEAD
# Emergency Blood & Donor Coordination System

University web application project demonstrating core Java EE concepts with Jakarta Servlets 6.0, JSP, JSTL, plain JDBC and MySQL.

---

## Quick Start

### Prerequisites
| Tool | Version | Verify |
|------|---------|--------|
| JDK | 17 (or 21+ with `maven.compiler.release=17`) | `java -version` |
| Apache Maven | 3.9+ | `mvn -version` |
| MySQL Server | 8.0 | `mysql -V` |

### Database Setup
```bash
# 1. Run schema (creates database + tables + indexes + constraints)
mysql -u root -p < db/schema.sql

# 2. Run seed data (admin, 2 hospitals, 8 donors, 1 patient)
mysql -u root -p < db/sample_data.sql
```

### Configure DB Connection
```bash
# Windows
copy src\main\resources\db.properties.example src\main\resources\db.properties

# Linux/macOS
cp src/main/resources/db.properties.example src/main/resources/db.properties
```
Edit `src/main/resources/db.properties` and set your real MySQL password:
```properties
db.url=jdbc:mysql://localhost:3306/blood_coordination?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
db.username=root
db.password=YOUR_ACTUAL_PASSWORD
db.driver=com.mysql.cj.jdbc.Driver
```

> **Important:** `db.properties` is in `.gitignore`. Never commit a file with a real password.

### Build & Run
```bash
# Build the WAR (required before cargo:run)
mvn clean package

# Start embedded Tomcat 10.1 and deploy
mvn cargo:run
```
The application starts at **http://localhost:8080/blood-connect/**

---

## Test Accounts (from db/sample_data.sql)

| Role | Email | Password | Notes |
|------|-------|----------|-------|
| Admin | admin@blood.com | Admin@123 | Full access |
| Hospital (verified) | reception@citygeneral.in | Hospital@123 | Can raise requests |
| Hospital (unverified) | contact@sahyadri.org.in | Hospital@123 | **Cannot sign in** — rule 2 demo |
| Donor | aarav@example.in | Donor@123 | O- blood, Mumbai |
| Patient | sanjay@example.in | Patient@123 | Mumbai |

---

## Manual Test Checklist

### 1. Home page
- [ ] Open `http://localhost:8080/blood-connect/` → 200 OK, shows project description
- [ ] Click "Register" → lands on `/register.jsp`
- [ ] Click "Sign in" → lands on `/login.jsp`

### 2. Register as Donor
- [ ] Select "Donor" → donor fields appear (age, gender, blood group, city, address)
- [ ] Fill valid data → submit → redirected to `/donor/dashboard`, name shown
- [ ] Try duplicate email → error "An account already exists for email: ..."
- [ ] Try weak password (<8 chars, no letter+number) → field-level error
- [ ] Try invalid blood group / phone → field-level error

### 3. Register as Hospital
- [ ] Select "Hospital" → hospital fields appear (name, reg no, address, city, contact, authorised person)
- [ ] Submit → redirected to `/login.jsp?registered=1` with message "verified by admin"
- [ ] Try to sign in with unverified hospital → error "awaiting verification"

### 4. Register as Patient
- [ ] Select "Patient" → city + emergency contact fields appear
- [ ] Submit → redirected to `/patient/dashboard`, name shown

### 5. Register as Admin
- [ ] **Not possible** — register.jsp has no ADMIN radio; AuthService rejects it

### 6. Login
- [ ] Valid credentials → session created, redirected by role via `getDashboardPath()`
- [ ] Wrong password → generic "Invalid email or password" (no email enumeration)
- [ ] Non-existent email → same generic message

### 7. Session & Logout
- [ ] Login → visit another protected page → works without re-login
- [ ] Click "Sign out" → session destroyed, cookie cleared, back to home
- [ ] After logout, visit `/donor/dashboard` → 302 to `/login.jsp`

### 8. AuthFilter Protection
- [ ] Not logged in → `/donor/dashboard` → 302 to login
- [ ] Logged in as donor → `/admin/dashboard` → 403 Forbidden (wrong role)
- [ ] Logged in as hospital → `/donor/dashboard` → 403 Forbidden

### 9. Error Pages
- [ ] `/nonexistent` → 404 → custom `error.jsp`
- [ ] Any uncaught exception → 500 → custom `error.jsp` (no stack trace to user)

---

## Project Structure

```
src/main/java/com/bloodconnect/
├── model/              # User, DonorUser, HospitalUser, PatientUser, AdminUser, UserFactory
├── enums/              # Role, BloodGroup, UrgencyLevel, RequestStatus, ResponseStatus, AccountStatus, ProtectedArea
├── dao/                # CrudDao<T,ID>, UserDao, DonorDao, HospitalDao
├── dao/impl/           # UserDaoImpl, DonorDaoImpl, HospitalDaoImpl (PreparedStatement CRUD)
├── service/            # AuthService (register, login, BCrypt)
├── servlet/            # RegisterServlet, LoginServlet, LogoutServlet, DashboardServlet, AppContextListener
├── filter/             # AuthFilter (protects /admin/*, /donor/*, /patient/*, /hospital/*)
├── util/               # DBUtil, SessionUtil, BloodCompatibility, AbstractBaseDao
├── exception/          # BloodAppException -> DatabaseException, AuthenticationException, ValidationException, DuplicateEmailException, ResourceNotFoundException
├── thread/             # (placeholder for phase 3 notification worker)
```

```
src/main/webapp/
├── index.jsp                 # honest landing page, no fake stats
├── login.jsp                 # email + password
├── register.jsp              # role radio → dynamic donor/hospital/patient fields
├── error.jsp                 # 403/404/500 handled by container
├── WEB-INF/
│   ├── web.xml               # welcome-file, error-page, session-config
│   └── views/
│       ├── common/
│       │   ├── header.jspf   # navbar, flash message, session-driven dashboard link
│       │   └── footer.jspf
│       └── dashboard-common.jspf   # shared dashboard skeleton + live counts
├── admin/dashboard.jsp
├── donor/dashboard.jsp
├── hospital/dashboard.jsp
├── patient/dashboard.jsp
└── assets/
    ├── favicon.svg
    └── css/app.css
```

```
db/
├── schema.sql        # CREATE DATABASE, 9 tables, FKs, indexes, CHECK constraints
└── sample_data.sql   # 1 admin, 2 hospitals, 8 donors (all 8 groups), 1 patient + verification queries
```

---

## Rubric Mapping

| Concept | Where It's Visible |
|---------|-------------------|
| **OOP – Abstract class & polymorphism** | `model/User` (abstract, `getDashboardPath()`), subclasses `DonorUser`, `HospitalUser`, `PatientUser`, `AdminUser`; `LoginServlet` calls `user.getDashboardPath()` once |
| **OOP – Encapsulation** | All model fields `private` with getters/setters; `passwordHash` never exposed |
| **Interfaces – Generic CRUD** | `CrudDao<T,ID>` with `create/findById/findAll/update/delete`; `UserDao`, `DonorDao`, `HospitalDao` extend it |
| **Enums** | `Role`, `BloodGroup`, `UrgencyLevel`, `RequestStatus`, `ResponseStatus`, `AccountStatus`, `ProtectedArea` |
| **Exception hierarchy** | `BloodAppException` (base) → `DatabaseException`, `AuthenticationException`, `ValidationException`, `DuplicateEmailException`, `ResourceNotFoundException`; DAOs wrap `SQLException` |
| **Collections – EnumMap/Set** | `util/BloodCompatibility` uses `EnumMap<BloodGroup, Set<BloodGroup>>` for O(1) compatibility lookup |
| **JDBC – PreparedStatement** | All DAO impls use `PreparedStatement`; no string-concatenated SQL anywhere |
| **JDBC – Schema/FKs/indexes** | `db/schema.sql`: 9 tables, FKs with `ON DELETE CASCADE/SET NULL`, indexes on `blood_group`, `city`, `role`, `status` |
| **JDBC – Transactions (design)** | `AuthService.register` documents that two inserts should be atomic; `DonorDao.recordDonationDate` is the hook for phase-3 single-transaction fulfilment |
| **Servlets – request/response** | `RegisterServlet`, `LoginServlet`, `LogoutServlet`, `DashboardServlet` handle GET/POST, redirect, forward |
| **Servlets – sessions** | `SessionUtil.establishSession` (invalidates old), stores `userId`, `role`, `name`, `dashboardPath`; `AuthFilter` reads them |
| **Servlets – Filter** | `AuthFilter` @WebFilter on `/admin/*, /donor/*, /patient/*, /hospital/*`; role-checked via `ProtectedArea` enum |

---

## Commands Summary

```bash
# One-time setup
mysql -u root -p < db/schema.sql
mysql -u root -p < db/sample_data.sql
copy src\main\resources\db.properties.example src\main\resources\db.properties
# edit db.properties with your password

# Every time you want to run
mvn clean package cargo:run
# then open http://localhost:8080/blood-connect/
```

---

## What is NOT Built Yet (Phase 2+)

- Raising blood requests (`BloodRequestServlet`)
- Donor matching & notification worker thread (`BlockingQueue` + `ExecutorService`)
- Request fulfilment in one JDBC transaction
- Admin verification screens, audit log viewer
- Password reset, email notifications

The schema, enums, DAO interfaces and `BloodCompatibility` are already designed for these.
=======
# java_project_blood_donor
An online platform that connects patients/hospitals requiring blood with registered blood donors.  The system maintains donor information, blood groups, availability, blood requests, donation history, and emergency requests. It helps users quickly identify suitable donors and allows hospitals or authorized users to coordinate blood requirements.
>>>>>>> 83e3a4d01e70ece9dd46f1ea7142c200ee2224ea
