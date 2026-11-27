package com.bloodconnect.util;

import com.bloodconnect.enums.BloodGroup;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodCompatibilityTest {

    @Test
    void oNegativeRecipientAcceptsOnlyONegative() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.O_NEGATIVE));
    }

    @Test
    void oPositiveRecipientAcceptsBothOGroups() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.O_POSITIVE));
    }

    @Test
    void aNegativeRecipientAcceptsONegativeAndANegative() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.A_NEGATIVE));
    }

    @Test
    void aPositiveRecipientAcceptsFourGroups() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE,
                        BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.A_POSITIVE));
    }

    @Test
    void bNegativeRecipientAcceptsONegativeAndBNegative() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.B_NEGATIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.B_NEGATIVE));
    }

    @Test
    void bPositiveRecipientAcceptsFourGroups() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE,
                        BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.B_POSITIVE));
    }

    @Test
    void abNegativeRecipientAcceptsFourGroups() {
        assertEquals(EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE,
                        BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.AB_NEGATIVE));
    }

    @Test
    void abPositiveRecipientAcceptsAllEightGroups() {
        assertEquals(EnumSet.allOf(BloodGroup.class),
                BloodCompatibility.compatibleDonorGroups(BloodGroup.AB_POSITIVE));
    }

    @Test
    void universalDonorCanDonateToUniversalRecipient() {
        assertTrue(BloodCompatibility.canDonateTo(BloodGroup.O_NEGATIVE, BloodGroup.AB_POSITIVE));
    }

    @Test
    void universalRecipientCannotDonateToUniversalDonor() {
        assertFalse(BloodCompatibility.canDonateTo(BloodGroup.AB_POSITIVE, BloodGroup.O_NEGATIVE));
    }

    @Test
    void nullArgumentsAreDenied() {
        assertFalse(BloodCompatibility.canDonateTo(null, BloodGroup.O_POSITIVE));
        assertFalse(BloodCompatibility.canDonateTo(BloodGroup.O_POSITIVE, null));
        assertTrue(BloodCompatibility.compatibleDonorGroups(null).isEmpty());
    }

    @Test
    void exactMatchRequiresSameGroup() {
        assertTrue(BloodCompatibility.isExactMatch(BloodGroup.A_POSITIVE, BloodGroup.A_POSITIVE));
        assertFalse(BloodCompatibility.isExactMatch(BloodGroup.A_POSITIVE, BloodGroup.A_NEGATIVE));
    }

    @Test
    void fullTableContainsEightRecipientGroupsAndIsUnmodifiable() {
        Map<BloodGroup, Set<BloodGroup>> table = BloodCompatibility.fullCompatibilityTable();
        assertEquals(8, table.size());
        assertThrows(UnsupportedOperationException.class,
                () -> table.put(BloodGroup.O_POSITIVE, EnumSet.noneOf(BloodGroup.class)));
    }

    @Test
    void describeDonorGroupsLabelsUniversalDonor() {
        String description = BloodCompatibility.describeDonorGroups(BloodGroup.AB_POSITIVE);
        assertTrue(description.contains("O-"));
        assertTrue(description.contains("compatible with every group"));
    }
}
