/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek;

import java.io.PrintWriter;
import java.io.Serial;
import java.io.Serializable;

import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;

/**
 * This is used for versioning, and to track the current Version the suite is running at.
 */
public final class Version implements Comparable<Version>, Serializable {
    // region Variable Declarations
    @Serial
    private static final long serialVersionUID = 3121116859864232639L;

    private static final MMLogger logger = MMLogger.create(Version.class);

    private int release;
    private int major;
    private int minor;
    private boolean snapshot;
    // endregion Variable Declarations

    // region Constructors

    /**
     * This Constructor is not to be used outside of unit testing
     */
    public Version() {
        setRelease(0);
        setMajor(0);
        setMinor(0);
        setSnapshot(false);
    }

    public Version(final @Nullable String text) {
        this();
        fillFromText(text);
    }

    public Version(final String release, final String major, final String minor, final String snapshot) {
        this();
        setRelease(MathUtility.parseInt(release, 0));
        setMajor(MathUtility.parseInt(major, 50));
        setMinor(MathUtility.parseInt(minor, 5));
        setSnapshot(MathUtility.parseBoolean(snapshot, false));
    }
    // endregion Constructors

    // region Getters
    public int getRelease() {
        return release;
    }

    public void setRelease(final int release) {
        this.release = release;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(final int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(final int minor) {
        this.minor = minor;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public void setSnapshot(final boolean snapshot) {
        this.snapshot = snapshot;
    }
    // endregion Getters

    /**
     * Use this method to determine if this version is higher than the version passed
     *
     * @param other The version we want to see if it is lower than this version
     *
     * @return true if this is higher than checkVersion
     *
     * @deprecated use {@link #isHigherThan(Version)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public boolean isHigherThan(final String other) {
        return isHigherThan(new Version(other));
    }

    /**
     * Use this method to determine if the version passed is less than this Version object.
     *
     * @param other The version we want to see if is less than this version
     *
     * @return true if checkVersion is less than this Version object
     */
    public boolean isHigherThan(final Version other) {
        return compareTo(other) > 0;
    }

    /**
     * Use this method to determine if this version is lower than the version passed
     *
     * @param other The version we want to see if it is higher than this version.
     *
     * @return true if this is lower than checkVersion
     *
     * @deprecated use {@link #isLowerThan(Version) instead}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public boolean isLowerThan(final String other) {
        return isLowerThan(new Version(other));
    }

    /**
     * Use this method to determine if this version is lower than the version passed
     *
     * @param other The version we want to see if it is higher than this version.
     *
     * @return true if this is lower than checkVersion
     */
    public boolean isLowerThan(final Version other) {
        return compareTo(other) < 0;
    }

    /**
     * @param lower the lower Version bound (exclusive)
     * @param upper the upper Version bound (exclusive)
     *
     * @return true if the version is between lower and upper versions, both exclusive
     */
    public boolean isBetween(final String lower, final String upper) {
        return isBetween(new Version(lower), new Version(upper));
    }

    /**
     * @param lower the lower Version bound (exclusive)
     * @param upper the upper Version bound (exclusive)
     *
     * @return true is the version is between lower and upper versions, both exclusive
     */
    public boolean isBetween(final Version lower, final Version upper) {
        return isHigherThan(lower) && isLowerThan(upper);
    }

    /**
     * @param other The version we want to see if it is the same as this version.
     *
     * @return true if this is same version as the other
     */
    public boolean is(final String other) {
        return is(new Version(other));
    }

    /**
     * @param other The version we want to see if it is the same as this version.
     *
     * @return true if this is same version as the other
     */
    public boolean is(final Version other) {
        return equals(other);
    }

    @Override
    public int compareTo(final Version other) {
        // Check Release version
        if (getRelease() > other.getRelease()) {
            return 1;
        } else if (getRelease() < other.getRelease()) {
            return -1;
        }

        // Release version is equal, try with Major
        if (getMajor() > other.getMajor()) {
            return 1;
        } else if (getMajor() < other.getMajor()) {
            return -1;
        }

        // Major version is also equal, try Minor
        if (getMinor() > other.getMinor()) {
            return 1;
        } else if (getMinor() < other.getMinor()) {
            return -1;
        }

        // Return 0 if the snapshots equal, otherwise this is lower is this is the
        // snapshot version
        if (isSnapshot() == other.isSnapshot()) {
            return 0;
        } else {
            return (isSnapshot() ? -1 : 1);
        }
    }

    // Added to complete the Java specification for the contract between compareTo
    // and equals
    public boolean equals(Object obj) {
        if (obj instanceof Version other) {
            return (getRelease() == other.getRelease() &&
                          getMajor() == other.getMajor() &&
                          getMinor() == other.getMinor() &&
                          isSnapshot() == other.isSnapshot());
        }

        return false;
    }

    // Added to complete the contract between equals() and hashCode()
    public int hashCode() {
        return toString().hashCode();
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, final int indent) {
        MMXMLUtility.writeSimpleXMLTag(pw, indent, "version", toString());
    }

    public void fillFromText(final @Nullable String text) {
        if (StringUtility.isNullOrBlank(text)) {
            final String nullOrBlank = ((text == null) ? "a null string" : "a blank string");
            final String message = String.format(MMLoggingConstants.VERSION_ERROR_CANNOT_PARSE_VERSION_FROM_STRING,
                  nullOrBlank);
            logger.fatalDialog(message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        final String[] snapshotSplit = text.split("-");
        final String[] versionSplit = snapshotSplit[0].split("\\.");

        if ((snapshotSplit.length > 2) || (versionSplit.length < 3)) {
            final String message = String.format(MMLoggingConstants.VERSION_ILLEGAL_VERSION_FORMAT, text);
            logger.fatalDialog(message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        try {
            setRelease(MathUtility.parseInt(versionSplit[0], 0));
        } catch (Exception e) {
            final String message = String.format(MMLoggingConstants.VERSION_FAILED_TO_PARSE_RELEASE, text);
            logger.fatalDialog(e, message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        try {
            setMajor(MathUtility.parseInt(versionSplit[1], 50));
        } catch (Exception e) {
            final String message = String.format(MMLoggingConstants.VERSION_FAILED_TO_PARSE_MAJOR, text);
            logger.fatalDialog(e, message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        try {
            setMinor(MathUtility.parseInt(versionSplit[2], 5));
        } catch (Exception e) {
            final String message = String.format(MMLoggingConstants.VERSION_FAILED_TO_PARSE_MINOR, text);
            logger.fatalDialog(e, message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        setSnapshot(snapshotSplit.length == 2);
    }
    // endregion File I/O

    @Override
    public String toString() {
        return String.format("%d.%02d.%02d%s", getRelease(), getMajor(), getMinor(), (isSnapshot() ? "-SNAPSHOT" : ""));
    }
}
