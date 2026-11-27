package com.bloodconnect.service;

import com.bloodconnect.model.DonorUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonorEligibilityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);

    @Test
    void donorWhoNeverDonatedIsEligible() {
        DonorUser donor = donorWithLastDonation(null);
        assertTrue(donor.isEligibleToDonate(TODAY));
    }

    @Test
    void donorAfterEightyNineDaysIsNotEligible() {
        DonorUser donor = donorWithLastDonation(TODAY.minusDays(89));
        assertFalse(donor.isEligibleToDonate(TODAY));
    }

    @Test
    void donorOnExactlyNinetiethDayIsEligible() {
        DonorUser donor = donorWithLastDonation(TODAY.minusDays(90));
        assertTrue(donor.isEligibleToDonate(TODAY));
    }

    @Test
    void donorAfterNinetyOneDaysIsEligible() {
        DonorUser donor = donorWithLastDonation(TODAY.minusDays(91));
        assertTrue(donor.isEligibleToDonate(TODAY));
    }

    @Test
    void unavailableDonorIsNeverEligible() {
        DonorUser donor = donorWithLastDonation(null);
        donor.setAvailable(false);
        assertFalse(donor.isEligibleToDonate(TODAY));
    }

    @Test
    void serviceReportsNeverDonatedAsEligible() {
        DonorEligibilityService.EligibilityResult result =
                new DonorEligibilityService().evaluate(true, null, TODAY);
        assertTrue(result.isEligible());
        assertTrue(result.getMessage().contains("No previous donation"));
    }

    @Test
    void serviceReportsEightyNineDaysAsNotEligibleWithOneDayRemaining() {
        DonorEligibilityService.EligibilityResult result = new DonorEligibilityService()
                .evaluate(true, TODAY.minusDays(89), TODAY);
        assertFalse(result.isEligible());
        assertTrue(result.getMessage().contains("1 more day"));
    }

    @Test
    void serviceReportsExactlyNinetyDaysAsEligible() {
        DonorEligibilityService.EligibilityResult result = new DonorEligibilityService()
                .evaluate(true, TODAY.minusDays(90), TODAY);
        assertTrue(result.isEligible());
        assertTrue(result.getMessage().contains("90-day rest period has ended"));
    }

    @Test
    void serviceReportsUnavailableDonorAsNotEligible() {
        DonorEligibilityService.EligibilityResult result = new DonorEligibilityService()
                .evaluate(false, null, TODAY);
        assertFalse(result.isEligible());
        assertEquals("This donor is currently marked unavailable.", result.getMessage());
    }

    private DonorUser donorWithLastDonation(LocalDate date) {
        DonorUser donor = new DonorUser();
        donor.setAvailable(true);
        donor.setLastDonationDate(date);
        return donor;
    }
}
