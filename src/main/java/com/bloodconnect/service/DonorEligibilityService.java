package com.bloodconnect.service;

import com.bloodconnect.model.DonorUser;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates donor eligibility without using the database.
 *
 * <p>The servlet uses this class for the public checker page, and unit tests use it
 * with fixed dates so the 90-day rule is deterministic.</p>
 */
public final class DonorEligibilityService {

    /**
     * Evaluates a donor object against a supplied reference date.
     *
     * @param donor the donor to evaluate
     * @param today the date that counts as today
     * @return the eligibility decision and a human-readable explanation
     */
    public EligibilityResult evaluate(DonorUser donor, LocalDate today) {
        return evaluate(donor.isAvailable(), donor.getLastDonationDate(), today);
    }

    /**
     * Evaluates the rule from raw values.
     *
     * @param available        whether the donor is currently available
     * @param lastDonationDate the previous donation date, or null if never donated
     * @param today            the date that counts as today
     * @return the eligibility decision and a human-readable explanation
     */
    public EligibilityResult evaluate(boolean available, LocalDate lastDonationDate, LocalDate today) {
        if (!available) {
            return new EligibilityResult(false, "This donor is currently marked unavailable.");
        }
        if (lastDonationDate == null) {
            return new EligibilityResult(true, "No previous donation is recorded, so this donor is eligible.");
        }

        LocalDate eligibleOn = lastDonationDate.plusDays(DonorUser.DONATION_COOLDOWN_DAYS);
        if (!eligibleOn.isAfter(today)) {
            return new EligibilityResult(true,
                    "The 90-day rest period has ended. This donor is eligible from " + eligibleOn + ".");
        }

        long daysRemaining = ChronoUnit.DAYS.between(today, eligibleOn);
        return new EligibilityResult(false,
                "Wait " + daysRemaining + " more day(s). Eligibility starts on " + eligibleOn + ".");
    }

    /**
     * The outcome of one eligibility check.
     */
    public static final class EligibilityResult {

        private final boolean eligible;
        private final String message;

        /**
         * Creates a result.
         *
         * @param eligible whether the donor may donate
         * @param message  the explanation shown to the user
         */
        public EligibilityResult(boolean eligible, String message) {
            this.eligible = eligible;
            this.message = message;
        }

        /**
         * Returns whether the donor may donate.
         *
         * @return true when the donor is eligible
         */
        public boolean isEligible() {
            return eligible;
        }

        /**
         * Returns the explanation shown to the user.
         *
         * @return the result message
         */
        public String getMessage() {
            return message;
        }
    }
}
