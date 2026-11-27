package com.bloodconnect.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumHelpersTest {

    @Test
    void bloodGroupParsesShortLabelCaseInsensitively() {
        assertEquals(BloodGroup.O_POSITIVE, BloodGroup.fromString(" o+ "));
    }

    @Test
    void bloodGroupParsesLongLabel() {
        assertEquals(BloodGroup.AB_NEGATIVE, BloodGroup.fromString("AB negative"));
    }

    @Test
    void bloodGroupDatabaseParserIsLenient() {
        assertEquals(BloodGroup.A_POSITIVE, BloodGroup.fromDb("a+"));
    }

    @Test
    void unknownBloodGroupIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BloodGroup.fromString("Z+"));
    }

    @Test
    void rhesusFactorIsDerivedFromLabel() {
        assertTrue(BloodGroup.B_POSITIVE.isRhPositive());
        assertFalse(BloodGroup.B_NEGATIVE.isRhPositive());
    }

    @Test
    void onlyActiveAccountsCanLogIn() {
        assertTrue(AccountStatus.ACTIVE.canLogin());
        assertFalse(AccountStatus.PENDING.canLogin());
        assertFalse(AccountStatus.SUSPENDED.canLogin());
        assertFalse(AccountStatus.DEACTIVATED.canLogin());
    }

    @Test
    void accountStatusParsesFromDatabaseText() {
        assertEquals(AccountStatus.SUSPENDED, AccountStatus.fromString("suspended"));
    }

    @Test
    void roleDatabaseParserReturnsDisplayName() {
        Role role = Role.fromDb("donor");
        assertEquals(Role.DONOR, role);
        assertEquals("Donor", role.getDisplayName());
    }

    @Test
    void requestStatusActiveFlagMatchesLifecycle() {
        assertTrue(RequestStatus.OPEN.isActive());
        assertTrue(RequestStatus.PARTIALLY_FULFILLED.isActive());
        assertFalse(RequestStatus.FULFILLED.isActive());
        assertFalse(RequestStatus.CANCELLED.isActive());
    }
}
