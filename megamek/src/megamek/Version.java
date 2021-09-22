/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;
import megamek.utils.MegaMekXmlUtil;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * This is used for versioning, and to track the current Version the suite is running at.
 */
public final class Version implements Comparable<Version>, Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 3121116859864232639L;

    private int release;
    private int major;
    private int minor;
    private boolean snapshot;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This Constructor is not to be used outside of unit testing
     */
    public Version() {
        setRelease(1);
        setMajor(0);
        setMinor(0);
        setSnapshot(false);
    }

    public Version(final @Nullable String text) {
        this();
        fillFromText(text);
    }
    //endregion Constructors

    //region Getters
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
    //endregion Getters

    /**
     * Use this method to determine if this version is higher than the version passed
     *
     * @param other The version we want to see if it is lower than this version
     * @return true if this is higher than checkVersion
     */
    public boolean isHigherThan(final String other) {
        return isHigherThan(new Version(other));
    }

    /**
     * Use this method to determine if the version passed is less than this Version object.
     *
     * @param other The version we want to see if is less than this version
     * @return true if checkVersion is less than this Version object
     */
    public boolean isHigherThan(final Version other) {
        return compareTo(other) > 0;
    }

    /**
     * Use this method to determine if this version is lower than the version passed
     *
     * @param other The version we want to see if it is higher than this version.
     * @return true if this is lower than checkVersion
     */
    public boolean isLowerThan(final String other) {
        return isLowerThan(new Version(other));
    }

    /**
     * Use this method to determine if this version is lower than the version passed
     *
     * @param other The version we want to see if it is higher than this version.
     * @return true if this is lower than checkVersion
     */
    public boolean isLowerThan(final Version other) {
            return compareTo(other) < 0;
    }

    /**
     * @param lower the lower Version bound (exclusive)
     * @param upper the upper Version bound (exclusive)
     * @return true if the version is between lower and upper versions, both exclusive
     */
    public boolean isBetween(final String lower, final String upper) {
       return isBetween(new Version(lower), new Version(upper));
    }

    /**
     * @param lower the lower Version bound (exclusive)
     * @param upper the upper Version bound (exclusive)
     * @return true is the version is between lower and upper versions, both exclusive
     */
    public boolean isBetween(final Version lower, final Version upper) {
        return isHigherThan(lower) && isLowerThan(upper);
    }

    /**
     * @param other The version we want to see if it is the same as this version.
     * @return true if this is same version as the other
     */
    public boolean is(final String other) {
        return is(new Version(other));
    }

    /**
     * @param other The version we want to see if it is the same as this version.
     * @return true if this is same version as the other
     */
    public boolean is(final Version other) {
        return compareTo(other) == 0;
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

        // Return 0 if the snapshots equal, otherwise this is lower is this is the snapshot version
        return (isSnapshot() == other.isSnapshot()) ? 0 : (isSnapshot() ? -1 : 1);
    }

    //region File I/O
    public void writeToXML(final PrintWriter pw, final int indent) {
        MegaMekXmlUtil.writeSimpleXMLTag(pw, indent, "version", toString());
    }

    public void fillFromText(final @Nullable String text) {
        if (StringUtil.isNullOrEmpty(text)) {
            final String message = String.format(
                    "Cannot parse the version from %s. This may lead to severe issues that cannot be otherwise explained.",
                    ((text == null) ? "a null string" : text));
            MegaMek.getLogger().fatal(message);
            JOptionPane.showMessageDialog(null, message, "Version Parsing Failure",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String[] snapshotSplit = text.split("-");
        final String[] versionSplit = snapshotSplit[0].split("\\.");

        if ((snapshotSplit.length > 2) || (versionSplit.length < 3)) {
            final String message = String.format(
                    "Version text %s is in an illegal version format. Versions should be in the format 'release.major.minor-SNAPSHOT', with the snapshot being an optional inclusion. This may lead to severe issues that cannot be otherwise explained.",
                    text);
            MegaMek.getLogger().fatal(message);
            JOptionPane.showMessageDialog(null, message, "Version Parsing Failure",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            setRelease(Integer.parseInt(versionSplit[0]));
        } catch (Exception e) {
            final String message = String.format(
                    "Failed to parse the release value from Version text %s. This may lead to severe issues that cannot be otherwise explained.",
                    text);
            MegaMek.getLogger().fatal(message, e);
            JOptionPane.showMessageDialog(null, message, "Version Parsing Failure",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            setMajor(Integer.parseInt(versionSplit[1]));
        } catch (Exception e) {
            final String message = String.format(
                    "Failed to parse the major value from Version text %s. This may lead to severe issues that cannot be otherwise explained.",
                    text);
            MegaMek.getLogger().fatal(message, e);
            JOptionPane.showMessageDialog(null, message, "Version Parsing Failure",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            setMinor(Integer.parseInt(versionSplit[2]));
        } catch (Exception e) {
            final String message = String.format(
                    "Failed to parse the minor value from Version text %s. This may lead to severe issues that cannot be otherwise explained.",
                    text);
            MegaMek.getLogger().fatal(message, e);
            JOptionPane.showMessageDialog(null, message, "Version Parsing Failure",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        setSnapshot(snapshotSplit.length == 2);
    }
    //endregion File I/O

    @Override
    public String toString() {
        return String.format("%d.%d.%d%s", getRelease(), getMajor(), getMinor(),
                (isSnapshot() ? "-SNAPSHOT" : ""));
    }
}
