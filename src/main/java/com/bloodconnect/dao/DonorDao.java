package com.bloodconnect.dao;

import com.bloodconnect.enums.BloodGroup;
import com.bloodconnect.exception.DatabaseException;
import com.bloodconnect.model.DonorUser;

import java.util.List;

/**
 * Persistence contract for the {@code donors} table.
 *
 * WHY the ID type is Long and not the users.id: donors has its own surrogate
 * primary key `id`, because blood_requests.donor_id and donations.donor_id must
 * reference something stable that is not the login record.
 */
public interface DonorDao extends CrudDao<DonorUser, Long> {

    /**
     * Resolves the donor profile for a login, so LoginServlet can attach donor
     * details to the session.
     */
    DonorUser findByUserId(long userId) throws DatabaseException;

    /**
     * THE key query of the whole project: "which donors can donate to group X in
     * city Y?".
     *
     * WHY it takes explicit criteria instead of a Map or a Criteria object: three
     * String parameters are self-documenting, and PreparedStatement placeholders
     * keep the SQL injection-safe. The eligibility rule (available = TRUE AND
     * last_donation_date older than 90 days) is enforced HERE IN SQL, because
     * filtering in Java would mean loading every donor into memory.
     */
    List<DonorUser> findEligibleDonors(BloodGroup bloodGroup, String city) throws DatabaseException;

    /** All donors of a group, ignoring the 90-day rule (used by admin screens). */
    List<DonorUser> findByBloodGroup(BloodGroup bloodGroup) throws DatabaseException;

    /** Toggle used by the donor dashboard later. */
    boolean setAvailability(long donorId, boolean available) throws DatabaseException;

    /**
     * Called from inside the fulfil-request transaction. Deliberately narrow so the
     * transaction can do exactly one thing, and so a partial failure is obvious.
     */
    boolean recordDonationDate(long donorId, java.time.LocalDate donationDate) throws DatabaseException;

    long countAll() throws DatabaseException;
}
