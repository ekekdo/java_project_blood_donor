-- ===========================================================================
--  Emergency Blood & Donor Coordination System - SCHEMA
--
--  How to run:
--    mysql -u root -p < db/schema.sql
--    mysql -u root -p < db/sample_data.sql     (after this file)
--
--  Safe to re-run: every table is dropped (children first) and recreated, so
--  this script always produces the exact same, empty database.
--
--  Column names and types below match EXACTLY what UserDaoImpl, DonorDaoImpl
--  and HospitalDaoImpl and db/sample_data.sql read and write:
--    users(id, name, email, phone, password_hash, role, status, created_at)
--    donors(id, user_id, age, gender, blood_group, city, address, available,
--           last_donation_date)
--    hospitals(id, user_id, hospital_name, registration_no, address, city,
--              contact_number, authorized_person, verified)
--
--  Enum columns are VARCHAR (never MySQL ENUM) so the Java enums remain the
--  single source of truth - see the comment in DonorDaoImpl.
-- ===========================================================================

CREATE DATABASE IF NOT EXISTS blood_coordination
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE blood_coordination;

-- ---------------------------------------------------------------------------
--  DROP in dependency order (children first) so re-running never fails on a
--  dangling foreign key. FOREIGN_KEY_CHECKS = 0 makes the order irrelevant
--  even if a table is missing on the very first run.
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS activity_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS donations;
DROP TABLE IF EXISTS donor_responses;
DROP TABLE IF EXISTS blood_requests;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS hospitals;
DROP TABLE IF EXISTS donors;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================================================
--  1. USERS - one login row per account, whatever the role.
--  email has a UNIQUE index: UserDaoImpl relies on error 1062 / SQLState 23000
--  to translate a race-condition duplicate into DuplicateEmailException.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS users (
    id             INT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(15)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,          -- BCrypt hash, 60 chars + salt
    role           VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT chk_users_role   CHECK (role IN ('DONOR', 'HOSPITAL', 'PATIENT', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED', 'DEACTIVATED'))
) ENGINE = InnoDB;

-- Indexes required by the rubric (role and status are filtered on dashboards).
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- ===========================================================================
--  2. DONORS - 1-to-1 profile of a DONOR user.
--  user_id is UNIQUE so a person can never register two donor profiles.
--  CASCADE: deleting the login removes the orphaned profile too.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS donors (
    id                 INT         NOT NULL AUTO_INCREMENT,
    user_id            INT         NOT NULL,
    age                TINYINT     NULL,
    gender             VARCHAR(10) NULL,
    blood_group        VARCHAR(5)  NOT NULL,
    city               VARCHAR(100) NOT NULL,
    address            VARCHAR(255) NULL,          -- optional on the register form
    available          TINYINT(1)  NOT NULL DEFAULT 1,
    last_donation_date DATE        NULL,           -- NULL = never donated (always eligible)
    PRIMARY KEY (id),
    UNIQUE KEY uq_donors_user_id (user_id),
    CONSTRAINT fk_donors_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_donors_blood_group CHECK (
        blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    CONSTRAINT chk_donors_gender CHECK (gender IN ('Male', 'Female', 'Other')),
    CONSTRAINT chk_donors_age    CHECK (age IS NULL OR age BETWEEN 18 AND 65),
    CONSTRAINT chk_donors_available CHECK (available IN (0, 1))
) ENGINE = InnoDB;

-- Indexes required by the rubric; both are in the eligibility query
-- (WHERE blood_group = ? AND city = ? AND available = TRUE ...).
CREATE INDEX idx_donors_blood_group ON donors (blood_group);
CREATE INDEX idx_donors_city        ON donors (city);

-- ===========================================================================
--  3. HOSPITALS - 1-to-1 profile of a HOSPITAL user.
--  verified = 0 at registration (business rule 2); only an admin sets it to 1.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS hospitals (
    id                INT          NOT NULL AUTO_INCREMENT,
    user_id           INT          NOT NULL,
    hospital_name     VARCHAR(150) NOT NULL,
    registration_no   VARCHAR(50)  NOT NULL,
    address           VARCHAR(255) NOT NULL,
    city              VARCHAR(100) NOT NULL,
    contact_number    VARCHAR(20)  NOT NULL,
    authorized_person VARCHAR(100) NOT NULL,
    verified          TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hospitals_user_id (user_id),
    UNIQUE KEY uq_hospitals_registration_no (registration_no),
    CONSTRAINT fk_hospitals_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_hospitals_verified CHECK (verified IN (0, 1))
) ENGINE = InnoDB;

-- Index required by the rubric; HospitalDaoImpl.findByCity uses it.
CREATE INDEX idx_hospitals_city ON hospitals (city);

-- ===========================================================================
--  4. PATIENTS - 1-to-1 profile of a PATIENT user (city + emergency contact).
--  Designed for phase 1 registration data; no PatientDao exists yet, so the
--  table is empty until that phase lands.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS patients (
    id                INT          NOT NULL AUTO_INCREMENT,
    user_id           INT          NOT NULL,
    city              VARCHAR(100) NULL,
    emergency_contact VARCHAR(15)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_patients_user_id (user_id),
    CONSTRAINT fk_patients_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_patients_city ON patients (city);

-- ===========================================================================
--  5. BLOOD_REQUESTS - raised by a verified hospital or by a patient.
--  created_by is NOT NULL -> CASCADE (the request dies with its author).
--  hospital_id is NULLABLE -> SET NULL (patient-raised requests have no
--  hospital; losing the hospital row must not delete the request).
--  donor_id is the assigned/fulfilling donor - see the class comment in
--  DonorDao: it references donors.id, not users.id.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS blood_requests (
    id               INT          NOT NULL AUTO_INCREMENT,
    created_by       INT          NOT NULL,
    hospital_id      INT          NULL,
    donor_id         INT          NULL,
    patient_name     VARCHAR(100) NOT NULL,
    blood_group      VARCHAR(5)   NOT NULL,
    units            INT          NOT NULL DEFAULT 1,
    city             VARCHAR(100) NOT NULL,
    hospital_location VARCHAR(255) NULL,
    required_date    DATE         NOT NULL,
    urgency          VARCHAR(20)  NOT NULL DEFAULT 'ROUTINE',
    is_emergency     TINYINT(1)   NOT NULL DEFAULT 0,
    reason           VARCHAR(500) NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_blood_requests_creator
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_blood_requests_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_blood_requests_donor
        FOREIGN KEY (donor_id) REFERENCES donors (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_blood_requests_blood_group CHECK (
        blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    CONSTRAINT chk_blood_requests_units  CHECK (units > 0),
    CONSTRAINT chk_blood_requests_urgency CHECK (urgency IN ('ROUTINE', 'URGENT', 'CRITICAL')),
    CONSTRAINT chk_blood_requests_emergency CHECK (is_emergency IN (0, 1)),
    CONSTRAINT chk_blood_requests_status CHECK (
        status IN ('OPEN', 'PARTIALLY_FULFILLED', 'FULFILLED', 'CANCELLED', 'EXPIRED'))
) ENGINE = InnoDB;

-- Indexes required by the rubric; also serve the donor feed
-- (match on blood_group + city, ordered by urgency/status).
CREATE INDEX idx_blood_requests_blood_group ON blood_requests (blood_group);
CREATE INDEX idx_blood_requests_city        ON blood_requests (city);
CREATE INDEX idx_blood_requests_status      ON blood_requests (status);

-- ===========================================================================
--  6. DONOR_RESPONSES - what a donor decided about a request
--  (ACCEPTED / MAYBE / DECLINED / COMPLETED - see ResponseStatus).
--  One row per donor per request, hence the UNIQUE pair.
--  CASCADE: no response can outlive the request or the donor.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS donor_responses (
    id          INT         NOT NULL AUTO_INCREMENT,
    request_id  INT         NOT NULL,
    donor_id    INT         NOT NULL,
    status      VARCHAR(20) NOT NULL,
    message     VARCHAR(500) NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_donor_responses_request_donor (request_id, donor_id),
    CONSTRAINT fk_donor_responses_request
        FOREIGN KEY (request_id) REFERENCES blood_requests (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_donor_responses_donor
        FOREIGN KEY (donor_id) REFERENCES donors (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_donor_responses_status CHECK (
        status IN ('ACCEPTED', 'MAYBE', 'DECLINED', 'COMPLETED'))
) ENGINE = InnoDB;

CREATE INDEX idx_donor_responses_status ON donor_responses (status);

-- ===========================================================================
--  7. DONATIONS - one confirmed units collection, written together with the
--  request status and donors.last_donation_date in the phase-3 transaction.
--  donor_id is NOT NULL -> CASCADE (DonorDao javadoc: donations.donor_id
--  references the stable donors.id surrogate key).
--  request_id SET NULL: the donation history survives if a request row is
--  ever cleaned up.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS donations (
    id           INT      NOT NULL AUTO_INCREMENT,
    request_id   INT      NULL,
    donor_id     INT      NOT NULL,
    units        INT      NOT NULL DEFAULT 1,
    donation_date DATE    NOT NULL DEFAULT (CURRENT_DATE),
    notes        VARCHAR(255) NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_donations_request
        FOREIGN KEY (request_id) REFERENCES blood_requests (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_donations_donor
        FOREIGN KEY (donor_id) REFERENCES donors (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_donations_units CHECK (units > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_donations_donation_date ON donations (donation_date);

-- ===========================================================================
--  8. NOTIFICATIONS - in-app messages produced by the phase-2/3 worker thread.
--  is_read is NULLABLE TINYINT(1): AbstractBaseDao.mapNullableBoolean exists
--  precisely for this column ("available / verified / is_read").
-- ===========================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id         INT          NOT NULL AUTO_INCREMENT,
    user_id    INT          NOT NULL,
    request_id INT          NULL,
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    is_read    TINYINT(1)   NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notifications_request
        FOREIGN KEY (request_id) REFERENCES blood_requests (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_notifications_is_read CHECK (is_read IS NULL OR is_read IN (0, 1))
) ENGINE = InnoDB;

CREATE INDEX idx_notifications_user_id ON notifications (user_id);

-- ===========================================================================
--  9. ACTIVITY_LOGS - the audit trail the admin dashboard reads
--  ("every action is written to activity_logs" - DashboardServlet).
--  user_id SET NULL on purpose: an audit entry must survive the deletion of
--  the account that performed it, otherwise the log proves nothing.
-- ===========================================================================
CREATE TABLE IF NOT EXISTS activity_logs (
    id          INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50)  NULL,
    entity_id   INT          NULL,
    details     VARCHAR(1000) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_activity_logs_action ON activity_logs (action);
