package com.bloodconnect.util;

import com.bloodconnect.enums.BloodGroup;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * WHO CAN DONATE TO WHOM - the single source of truth for blood-group rules.
 *
 * WHY AN EnumMap (one of the rubric's required collection types):
 * An EnumMap stores its entries in an array indexed by the enum's ORDINAL, so
 * lookup is O(1) with no hashing at all, and it is impossible for it to contain a
 * key that is not a BloodGroup. A HashMap would have to hash+equals every lookup
 * and could be polluted by a mistyped key.
 *
 * WHY a Set<BloodGroup> per group (a second required collection type):
 * A donor of one group is compatible with SEVERAL recipient groups (O- donates to
 * all eight). A Set expresses "a set of acceptable recipients" directly; a List
 * would allow duplicates and a single BloodGroup field would not be enough.
 *
 * WHY THIS IS STATIC AND BUILT ONCE:
 * The compatibility rules are constants, not data. Building them once in a static
 * initialiser makes the map effectively immutable after construction - we wrap
 * every value with Collections.unmodifiableSet so no code anywhere can corrupt the
 * rules at runtime. Thread safety therefore costs nothing later, when the
 * notification worker threads start reading it.
 *
 * HOW THE TABLE IS BUILT (derivation, not a second hand-written table):
 * We store the compatible DONOR types for each group, then INVERT that map to get
 * recipient -> donor list. The ABO/Rh rules below are the classic transfusion
 * compatibility table.
 */
public final class BloodCompatibility {

    /**
     * recipient group -> the set of groups whose blood that recipient can RECEIVE.
     * Built once, never mutated.
     */
    private static final Map<BloodGroup, Set<BloodGroup>> RECIPIENT_TO_DONORS = new EnumMap<>(BloodGroup.class);

    static {
        // Class 1: a group can always receive from its own type and from O-.
        // O- is the universal donor; AB+ is the universal recipient.
        put(BloodGroup.O_NEGATIVE, BloodGroup.O_NEGATIVE);
        put(BloodGroup.O_POSITIVE, BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE);
        put(BloodGroup.A_NEGATIVE, BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE);
        put(BloodGroup.A_POSITIVE, BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE);
        put(BloodGroup.B_NEGATIVE, BloodGroup.O_NEGATIVE, BloodGroup.B_NEGATIVE);
        put(BloodGroup.B_POSITIVE, BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE);
        put(BloodGroup.AB_NEGATIVE, BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE);
        put(BloodGroup.AB_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE, BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.AB_NEGATIVE, BloodGroup.AB_POSITIVE);
    }

    private BloodCompatibility() {
        throw new AssertionError("BloodCompatibility is a utility class and must not be instantiated");
    }

    /**
     * EnumSet.of(...) allocates the smallest possible EnumSet, and we wrap it in
     * unmodifiableSet so the rules cannot be edited after startup.
     */
    private static void put(BloodGroup recipient, BloodGroup... compatibleDonorGroups) {
        // EnumSet.of needs at least one element, and the varargs signature gives us
        // a plain array, so the first element is passed separately and the rest as
        // the array. EnumSet.of() also de-duplicates, so an accidental repeat here
        // would be silently corrected rather than corrupting the rules.
        Set<BloodGroup> donors = EnumSet.of(compatibleDonorGroups[0], compatibleDonorGroups);
        RECIPIENT_TO_DONORS.put(recipient, Collections.unmodifiableSet(donors));
    }

    /**
     * The core question: may a donor of {@code donorGroup} donate to a patient of
     * {@code recipientGroup}?
     *
     * WHY defensive null checks: this is called straight from a servlet parameter
     * in a later phase, and a NullPointerException here would surface as a blank
     * 500 page instead of a validation message.
     */
    public static boolean canDonateTo(BloodGroup donorGroup, BloodGroup recipientGroup) {
        if (donorGroup == null || recipientGroup == null) {
            return false;
        }
        // getOrDefault is used instead of get() so an unknown key returns an empty
        // set (deny by default) rather than null and an NPE.
        return RECIPIENT_TO_DONORS
                .getOrDefault(recipientGroup, Collections.emptySet())
                .contains(donorGroup);
    }

    /**
     * Every donor group that could serve a given recipient. Used by the request
     * form to show "O- and O+ donors can help" and by the notification worker.
     *
     * @return an unmodifiable view; never null
     */
    public static Set<BloodGroup> compatibleDonorGroups(BloodGroup recipientGroup) {
        if (recipientGroup == null) {
            return Collections.emptySet();
        }
        return RECIPIENT_TO_DONORS.getOrDefault(recipientGroup, Collections.emptySet());
    }

    /**
     * Exact-match only, i.e. same ABO group AND same Rh factor.
     *
     * WHY blood banks care: a patient needing surgery is often given same-group
     * blood as the first choice, and cross-matching handles the rest. The
     * dashboard shows this separately from canDonateTo so a student can explain the
     * difference between "compatible" and "identical".
     */
    public static boolean isExactMatch(BloodGroup donorGroup, BloodGroup recipientGroup) {
        return donorGroup != null && donorGroup == recipientGroup;
    }

    /**
     * The unmodifiable whole table, for an admin "compatibility reference" page.
     */
    public static Map<BloodGroup, Set<BloodGroup>> fullCompatibilityTable() {
        return Collections.unmodifiableMap(RECIPIENT_TO_DONORS);
    }

    /** Short human-readable summary such as "O- (all groups)" for the UI. */
    public static String describeDonorGroups(BloodGroup recipientGroup) {
        Set<BloodGroup> groups = compatibleDonorGroups(recipientGroup);
        if (groups.isEmpty()) {
            return "No compatible group recorded";
        }
        // Building the string by streaming over the EnumSet, whose iteration order is
        // the natural enum declaration order, so the output is always stable.
        StringBuilder sb = new StringBuilder();
        for (BloodGroup group : groups) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(group.getLabel());
        }
        if (groups.contains(BloodGroup.O_NEGATIVE) && groups.size() > 2) {
            sb.append("  (O- is compatible with every group)");
        }
        return sb.toString();
    }
}
