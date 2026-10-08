-- ===========================================================================
--  Emergency Blood & Donor Coordination System - SAMPLE DATA
--
--  How to run (after db/schema.sql):
--    mysql -u root -p < db/sample_data.sql
--
--  This script RE-USES the 12 accounts above, so run schema.sql first.
--  It is safe to run more than once: every table is emptied first, so you can
--  reset the demo to exactly this state before a viva rehearsal.
--
--  ---------------------------------------------------------------------------
--  SIGN-IN CREDENTIALS (all passwords are BCrypt-hashed below; the plain text
--  is written here ONLY so you can log in during testing):
--
--    Role     | Email                  | Password       | Can sign in?
--    ---------+------------------------+----------------+-----------------------------
--    Admin    | admin@blood.com        | Admin@123      | yes
--    Hospital | reception@citygeneral.in| Hospital@123  | yes  (verified = 1)
--    Hospital | contact@sahyadri.org.in | Hospital@123 | NO - verified = 0, so
--              |                        |                | login is refused on purpose
--              |                        |                | (business rule 2)
--    Donor    | aarav@example.in       | Donor@123      | yes
--    Donor    | diya@example.in        | Donor@123      | yes
--    Donor    | rohan@example.in       | Donor@123      | yes
--    Donor    | ananya.iyer@example.in | Donor@123      | yes
--    Donor    | vikram@example.in      | Donor@123      | yes, but INELIGIBLE to donate
--              |                        |                | (donated 30 days ago, rule 1)
--    Donor    | priya@example.in       | Donor@123      | yes
--    Donor    | arjun@example.in       | Donor@123      | yes, ELIGIBLE again (110 days)
--    Donor    | meera@example.in       | Donor@123      | yes
--    Patient  | sanjay@example.in      | Patient@123    | yes
--  ---------------------------------------------------------------------------
-- ===========================================================================

USE blood_coordination;

-- Start from a known state so re-running never doubles up the rows.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE activity_logs;
TRUNCATE TABLE notifications;
TRUNCATE TABLE donations;
TRUNCATE TABLE donor_responses;
TRUNCATE TABLE blood_requests;
TRUNCATE TABLE hospitals;
TRUNCATE TABLE donors;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================================================
--  USERS  (ids are explicit so the child rows below can reference them and you
--  can read the relationships straight off the page)
-- ===========================================================================

-- The one administrator. It is created ONLY here - register.jsp does not offer
-- the ADMIN option, and AuthService.register() throws if anyone POSTs
-- role=ADMIN. That is the security guard that keeps the admin area sealed.
INSERT INTO users (id, name, email, phone, password_hash, role, status) VALUES
(1, 'Ananya Sharma',  'admin@blood.com',               '9810010001', '$2a$10$QhcphYX5zxO4yQCmOAIxJOV0T4O3zftXxAMOedLkWi4rA4gPqhtWK', 'ADMIN',    'ACTIVE');

-- Hospital 1: VERIFIED, so it can sign in and (in a later phase) raise requests.
INSERT INTO users (id, name, email, phone, password_hash, role, status) VALUES
(2, 'Dr. Meenakshi Rao', 'reception@citygeneral.in', '9840010002', '$2a$10$x1WrLqWNZ4.mw/VHw.vvdeVKSoYu8O8d9FjAYgF4y/WeEljziZNFe', 'HOSPITAL', 'ACTIVE'),
-- Hospital 2: registered but NOT verified. This row exists precisely so you can
-- demonstrate rule 2 live: try to sign in as it and AuthService refuses.
(3, 'Rajesh Kulkarni',    'contact@sahyadri.org.in', '9822010003', '$2a$10$x1WrLqWNZ4.mw/VHw.vvdeVKSoYu8O8d9FjAYgF4y/WeEljziZNFe', 'HOSPITAL', 'ACTIVE');

-- The 8 donors.
INSERT INTO users (id, name, email, phone, password_hash, role, status) VALUES
(4, 'Aarav Sharma',  'aarav@example.in',       '9825010004', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(5, 'Diya Patel',    'diya@example.in',        '9825010005', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(6, 'Rohan Mehta',   'rohan@example.in',       '9825010006', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(7, 'Ananya Iyer',   'ananya.iyer@example.in', '9843010007', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(8, 'Vikram Singh',  'vikram@example.in',      '9811010008', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(9, 'Priya Nair',    'priya@example.in',       '9848010009', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(10, 'Arjun Reddy',  'arjun@example.in',       '9844010010', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE'),
(11, 'Meera Joshi',  'meera@example.in',       '9822010011', '$2a$10$OkClOSe6pHVg.nx4lzNToefbeI4ilnPcoJiacjDrOxm1yyYPjeIka', 'DONOR', 'ACTIVE');

-- The one patient.
INSERT INTO users (id, name, email, phone, password_hash, role, status) VALUES
(12, 'Sanjay Kumar', 'sanjay@example.in', '9833010012', '$2a$10$pdpKDBTo5ikAiVtZ1dWxq.OUMC83.vR5SrqA6ZTG2zY/9l4XMVfv2', 'PATIENT', 'ACTIVE');

-- Keep AUTO_INCREMENT ahead of the ids we just inserted explicitly, otherwise the
-- next real registration would try to reuse id 1 and fail the PRIMARY KEY.
ALTER TABLE users AUTO_INCREMENT = 100;

-- ===========================================================================
--  HOSPITALS
-- ===========================================================================
INSERT INTO hospitals (user_id, hospital_name, registration_no, address, city,
                       contact_number, authorized_person, verified) VALUES
(2, 'City General Hospital',      'TN/MCH/2004/1187', '45 Beach Road, Triplicane',       'Chennai', '044-28276000', 'Dr. Meenakshi Rao', 1),
-- verified = 0 -> business rule 2 applies to this account.
(3, 'Sahyadri Blood Bank',        'MH/PUNE/2011/0456', 'Plot 12, Laxmi Road, Aundh',       'Pune',    '020-67119900', 'Rajesh Kulkarni',    0);

-- ===========================================================================
--  DONORS  - eight rows, all eight blood groups, spread across eight cities
--
--  last_donation_date is written with DATE_SUB(CURDATE(), INTERVAL n DAY)
--  rather than a hard-coded date, so the 90-day rule stays demonstrable no
--  matter when the script is run:
--
--    Vikram Singh : 30 days ago  -> INSIDE the 90-day window, so he is correctly
--                                  EXCLUDED by the eligible-donor query.
--    Arjun Reddy  : 110 days ago -> OUTSIDE the window, so he is correctly
--                                  INCLUDED again.
--    Everyone else: NULL, meaning "has never donated", so all are eligible.
-- ===========================================================================
INSERT INTO donors (user_id, age, gender, blood_group, city, address, available, last_donation_date) VALUES
(4,  27, 'Male',   'O-',  'Mumbai',    'A-403, Sai Krupa, Andheri East',      1, NULL),
(5,  24, 'Female', 'A+',  'Ahmedabad', 'B-12, Satellite, Ahmedabad',          1, NULL),
(6,  31, 'Male',   'B+',  'Pune',      'Flat 7, Kothrud, Pune',               1, NULL),
(7,  22, 'Female', 'AB+', 'Chennai',   '28/4 Besant Nagar, Chennai',          1, NULL),
(8,  35, 'Male',   'AB-', 'Delhi',     'C-9, Dwarka Sector 12, New Delhi',    1, DATE_SUB(CURDATE(), INTERVAL 30 DAY)),
(9,  29, 'Female', 'A-',  'Kochi',     'Villa 3, Kakkanad, Kochi',            1, NULL),
(10, 26, 'Male',   'O+',  'Hyderabad', 'H.No 5-1-10, Banjara Hills, Hyderabad', 1, DATE_SUB(CURDATE(), INTERVAL 110 DAY)),
(11, 33, 'Female', 'B-',  'Nagpur',    '18, Sadar Bazaar, Nagpur',            1, NULL);

-- ---------------------------------------------------------------------------
--  VERIFICATION QUERIES - run these to prove the seed data is correct and to
--  show the examiner that the numbers come from the database, not from the page.
-- ---------------------------------------------------------------------------

-- How many accounts of each role exist:
--   SELECT role, COUNT(*) AS total FROM users GROUP BY role;

-- The 8 groups are all present, one donor each:
--   SELECT blood_group, COUNT(*) AS donors FROM donors GROUP BY blood_group ORDER BY blood_group;

-- Only one hospital is verified (business rule 2):
--   SELECT hospital_name, city, verified FROM hospitals;

-- Business rule 1: donors of O- in Mumbai, ignoring the 90-day cooldown.
--   SELECT u.name, d.blood_group, d.city, d.available, d.last_donation_date
--   FROM donors d JOIN users u ON u.id = d.user_id
--   WHERE d.blood_group = 'O-' AND d.city = 'Mumbai' AND d.available = 1
--     AND (d.last_donation_date IS NULL
--          OR d.last_donation_date < DATE_SUB(CURDATE(), INTERVAL 90 DAY));

-- Sanity check: Vikram Singh must be EXCLUDED (30 days ago) and
-- Arjun Reddy INCLUDED (110 days ago) by the query above.
