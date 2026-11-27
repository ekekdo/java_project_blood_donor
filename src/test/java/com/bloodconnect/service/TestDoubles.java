package com.bloodconnect.service;

import com.bloodconnect.dao.HospitalDao;
import com.bloodconnect.dao.RegistrationDao;
import com.bloodconnect.dao.UserDao;
import com.bloodconnect.enums.AccountStatus;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.exception.DuplicateEmailException;
import com.bloodconnect.model.DonorUser;
import com.bloodconnect.model.HospitalUser;
import com.bloodconnect.model.PatientUser;
import com.bloodconnect.model.User;

import java.util.List;
import java.util.Optional;

final class TestDoubles {

    private TestDoubles() {
    }

    static final class FakeUserDao implements UserDao {

        private boolean emailExists;
        private User created;

        void setEmailExists(boolean emailExists) {
            this.emailExists = emailExists;
        }

        User getCreated() {
            return created;
        }

        @Override
        public Long create(User entity) {
            created = entity;
            return 100L;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public List<User> findAll() {
            return List.of();
        }

        @Override
        public boolean update(User entity) {
            return false;
        }

        @Override
        public boolean delete(Long id) {
            return false;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public boolean emailExists(String email) {
            return emailExists;
        }

        @Override
        public long countByStatus(AccountStatus status) {
            return 0L;
        }

        @Override
        public long countAll() {
            return 0L;
        }
    }

    static final class FakeHospitalDao implements HospitalDao {

        private boolean registrationNumberExists;

        void setRegistrationNumberExists(boolean registrationNumberExists) {
            this.registrationNumberExists = registrationNumberExists;
        }

        @Override
        public Long create(HospitalUser entity) {
            return 1L;
        }

        @Override
        public Optional<HospitalUser> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public List<HospitalUser> findAll() {
            return List.of();
        }

        @Override
        public boolean update(HospitalUser entity) {
            return false;
        }

        @Override
        public boolean delete(Long id) {
            return false;
        }

        @Override
        public HospitalUser findByUserId(long userId) {
            return null;
        }

        @Override
        public boolean registrationNoExists(String registrationNo) {
            return registrationNumberExists;
        }

        @Override
        public List<HospitalUser> findByCity(String city) {
            return List.of();
        }

        @Override
        public boolean setVerified(long hospitalId, boolean verified) {
            return false;
        }

        @Override
        public long countAll() {
            return 0L;
        }
    }

    static final class FakeRegistrationDao implements RegistrationDao {

        private User user;
        private DonorUser donor;
        private HospitalUser hospital;
        private PatientUser patient;

        User getUser() {
            return user;
        }

        DonorUser getDonor() {
            return donor;
        }

        HospitalUser getHospital() {
            return hospital;
        }

        PatientUser getPatient() {
            return patient;
        }

        @Override
        public Long register(User user, DonorUser donor, HospitalUser hospital, PatientUser patient)
                throws DatabaseException, DuplicateEmailException {
            this.user = user;
            this.donor = donor;
            this.hospital = hospital;
            this.patient = patient;
            user.setId(100L);
            return 100L;
        }
    }
}
