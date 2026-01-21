/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek;

import java.io.PrintWriter;
import java.io.Serial;
import java.io.Serializable;
import java.util.ResourceBundle;

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

    private static final MMLogger LOGGER = MMLogger.create(Version.class);
    private static final ResourceBundle VERSION_BUNDLE = ResourceBundle.getBundle("Version");
    private static ResourceBundle EXTRA_VERSION_INFORMATION_BUNDLE = null;

    private int major;
    private int minor;
    private int patch;
    private String extra;
    // endregion Variable Declarations

    // region Constructors

    /**
     * This Constructor is not to be used outside of unit testing
     */
    public Version() {
        setMajor(MathUtility.parseInt(VERSION_BUNDLE.getString("major")));
        setMinor(MathUtility.parseInt(VERSION_BUNDLE.getString("minor")));
        setPatch(MathUtility.parseInt(VERSION_BUNDLE.getString("patch")));

        try {
            EXTRA_VERSION_INFORMATION_BUNDLE = ResourceBundle.getBundle("extraVersion");
        } catch (Exception ignored) {
        }
    }

    /**
     * Sets the version with Extra data.
     *
     * @param text The Version string to parse.
     */
    public Version(final @Nullable String text) {
        if (StringUtility.isNullOrBlank(text)) {
            final String nullOrBlank = ((text == null) ? "a null string" : "a blank string");
            final String message = String.format(MMLoggingConstants.VERSION_ERROR_CANNOT_PARSE_VERSION_FROM_STRING,
                  nullOrBlank);
            LOGGER.fatalDialog(message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        final String[] extraSplit = text.split("-", 2);
        final String[] versionSplit = extraSplit[0].split("\\.");

        if ((extraSplit.length > 2) || (versionSplit.length < 3)) {
            final String message = String.format(MMLoggingConstants.VERSION_ILLEGAL_VERSION_FORMAT, text);
            LOGGER.fatalDialog(message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        setMajor(MathUtility.parseInt(versionSplit[0]));
        setMinor(MathUtility.parseInt(versionSplit[1]));
        setPatch(MathUtility.parseInt(versionSplit[2]));
        setExtra(extraSplit.length == 2 ? extraSplit[1] : null);
    }

    /**
     * Sets the version.
     *
     * @param major Major Version
     * @param minor Minor Version
     * @param patch Patch Version
     */
    public Version(final int major, final int minor, final int patch) {
        this();
        setMajor(major);
        setMinor(minor);
        setPatch(patch);
    }

    /**
     * Sets the version.
     *
     * @param major Major Version
     * @param minor Minor Version
     * @param patch Patch Version
     */
    public Version(final String major, final String minor, final String patch) {
        this();
        setMajor(MathUtility.parseInt(major, 0));
        setMinor(MathUtility.parseInt(minor, 50));
        setPatch(MathUtility.parseInt(patch, 5));
    }

    /**
     * Sets the version with Extra data.
     *
     * @param major Major Version
     * @param minor Minor Version
     * @param patch Patch Version
     * @param extra Extra would be PR or nightly with git hash.
     */
    public Version(final String major, final String minor, final String patch, @Nullable final String extra) {
        this(major, minor, patch);
        setExtra(extra);
    }
    // endregion Constructors

    // region Getters
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

    public int getPatch() {
        return patch;
    }

    public void setPatch(final int patch) {
        this.patch = patch;
    }

    public String getExtra() {
        if (extra == null && EXTRA_VERSION_INFORMATION_BUNDLE != null) {
            String branch = EXTRA_VERSION_INFORMATION_BUNDLE.getString("branch");
            String gitHash = EXTRA_VERSION_INFORMATION_BUNDLE.getString("gitHash");

            extra = branch + "-" + gitHash;
        }

        if (extra == null) {
            return "";
        }

        return extra;
    }

    public void setExtra(@Nullable final String extra) {
        this.extra = extra;
    }
    // endregion Getters

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
        if (getMajor() > other.getMajor()) {
            return 1;
        } else if (getMajor() < other.getMajor()) {
            return -1;
        }

        // Release version is equal, try with Major
        if (getMinor() > other.getMinor()) {
            return 1;
        } else if (getMinor() < other.getMinor()) {
            return -1;
        }

        // Major version is also equal, try Minor
        if (getPatch() > other.getPatch()) {
            return 1;
        } else if (getPatch() < other.getPatch()) {
            return -1;
        }

        // Extra version is not compared
        return 0;
    }

    // Added to complete the Java specification for the contract between compareTo
    // and equals
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version other) {
            return (getMajor() == other.getMajor() &&
                  getMinor() == other.getMinor() &&
                  getPatch() == other.getPatch() &&
                  getExtra().equals(other.getExtra()));
        }

        return false;
    }

    // Added to complete the contract between equals() and hashCode()
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, final int indent) {
        MMXMLUtility.writeSimpleXMLTag(pw, indent, "version", toString());
    }

    @Override
    public String toString() {
        return String.format("%d.%02d.%02d%s",
              getMajor(),
              getMinor(),
              getPatch(),
              (!getExtra().isEmpty() ? "-" + getExtra() : ""));
    }
}
