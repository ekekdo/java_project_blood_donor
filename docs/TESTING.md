# Manual Testing Cases

Fill in the blank `Result` column while testing the deployed WAR.

| ID | Role | Steps | Expected Result | Result |
|----|------|-------|-----------------|--------|
| TC-01 | Guest | Open `/blood-connect/` | Home page loads with 200 and project description |  |
| TC-02 | Guest | Look at navbar and footer | Both contain an "Eligibility" / "Donor eligibility checker" link |  |
| TC-03 | Guest | Open `/eligibility` | Checker form loads; JSP is not directly addressable by file path |  |
| TC-04 | Guest | Submit `not-a-date` in date field | Warning says to use `YYYY-MM-DD`; no 500 |  |
| TC-05 | Guest | Leave date empty and keep available checked | Result says no previous donation and donor is eligible |  |
| TC-06 | Guest | Enter a date 89 days before today and submit | Result says not eligible yet and shows 1 day remaining |  |
| TC-07 | Guest | Enter a date exactly 90 days before today and submit | Result says the 90-day rest period has ended and donor is eligible |  |
| TC-08 | Guest | Open `/register.jsp` | Donor, Hospital and Patient options are shown; Admin is absent |  |
| TC-09 | Guest | Register a valid donor | Redirects to `/donor/dashboard`; donor name appears |  |
| TC-10 | Guest | Register a valid patient | Redirects to `/patient/dashboard`; patient name appears |  |
| TC-11 | Guest | Inspect the `patients` table after TC-10 | One row exists for the new patient with city and emergency contact |  |
| TC-12 | Guest | Submit registration with an existing email | Returns to form with field-level duplicate-email message; message is not lost |  |
| TC-13 | Guest | Submit a password shorter than 8 characters | Returns to form with password validation message |  |
| TC-14 | Guest | Force `role=ADMIN` in the request | Registration is rejected as admin self-registration |  |
| TC-15 | Hospital | Sign in with `contact@sahyadri.org.in` / `Hospital@123` | Login is blocked with "awaiting verification" message |  |
| TC-16 | Hospital | Sign in with `reception@citygeneral.in` / `Hospital@123` | Redirects to `/hospital/dashboard` |  |
| TC-17 | Any user | Sign in with a wrong password | Generic "Invalid email or password" message appears |  |
| TC-18 | Donor | Sign in, then open `/admin/dashboard` | 403 Forbidden via AuthFilter |  |
| TC-19 | Any user | Sign in, click Sign out, then open `/donor/dashboard` | Redirects to login page |  |
| TC-20 | Any user | Sign in and open the exact URL `/donor/dashboard` | Dashboard loads normally; no forwarding loop or HTTP 500 |  |
| TC-21 | Guest | Submit an invalid donor age | Returns to the form with age error and sticky values |  |
| TC-22 | Guest | Register a hospital, then try to sign in | Registration says admin verification required; sign-in is blocked |  |
| TC-23 | Guest | Open `/missing-page` | Custom `error.jsp` shows a safe 404 message |  |
| TC-24 | Guest | Inspect `target/blood-connect.war` | `WEB-INF/classes/db.properties` is present |  |
