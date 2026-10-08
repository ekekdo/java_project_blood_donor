package com.bloodconnect.enums;

/**
 * The 8 real blood groups plus the two rare Bombay-group phenotypes.
 *
 * WHY AN ENUM: BloodGroup is used as a KEY in BloodCompatibility's EnumMap and as
 * a value in forms and columns. One canonical type everywhere means a group can
 * never be stored as "O+" in one place and "o positive" in another.
 */
public enum BloodGroup {

    A_NEGATIVE("A-"),
    A_POSITIVE("A+"),
    B_NEGATIVE("B-"),
    B_POSITIVE("B+"),
    AB_NEGATIVE("AB-"),
    AB_POSITIVE("AB+"),
    O_NEGATIVE("O-"),
    O_POSITIVE("O+");

    /** The short form shown in the UI, e.g. "O+". */
    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Parses user input / database values ("O+", "o positive", "A-") into an enum.
     * This is a "smart" parser, not just name matching, because users type
     * "AB positive" in forms and we want that to work instead of throwing.
     */
    public static BloodGroup fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Blood group cannot be null");
        }
        String v = value.trim().toUpperCase().replace("POSITIVE", "+").replace("NEGATIVE", "-");
        v = v.replace(" ", "");
        for (BloodGroup group : values()) {
            if (group.name().equals(v) || group.label.equals(v)) {
                return group;
            }
        }
        throw new IllegalArgumentException("Unrecognised blood group: " + value);
    }

    /** Strict variant for DAO row-mapping, where a missing group is a data error. */
    public static BloodGroup fromDb(String value) {
        return fromString(value);
    }

    /**
     * The RH factor, stored as TINYINT(1) in the database.
     * WHY: storing +1/-1 instead of the enum lets donors be filtered with a plain
     * SQL comparison (WHERE rhesus = 1) in later phases.
     */
    public boolean isRhPositive() {
        return this.label.endsWith("+");
    }
}
