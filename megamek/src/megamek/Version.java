/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
 *
 * <p>Versions are written as {@code major.minor.patch[.revision][-extra]}. The revision is an optional fourth
 * component reserved for special point releases that ship on top of an existing patch release, such as
 * {@code 0.49.19.1}. Ordinary releases have no revision at all, and both {@link #toString()} and the comparison
 * methods behave exactly as they did before the component existed.</p>
 *
 * <p>Note that {@link #toString()} pads the minor and patch components to two digits, so the release labelled
 * {@code v0.51.0.1} on GitHub renders here as {@code 0.51.00.1}.</p>
 */
public final class Version implements Comparable<Version>, Serializable {
    // region Variable Declarations
    @Serial
    private static final long serialVersionUID = 3121116859864232639L;

    private static final MMLogger LOGGER = MMLogger.create(Version.class);
    private static final ResourceBundle VERSION_BUNDLE = ResourceBundle.getBundle("Version");
    private static ResourceBundle EXTRA_VERSION_INFORMATION_BUNDLE = null;

    /**
     * The value {@link #getRevision()} reports when a version carries no fourth component, which is the case for
     * every ordinary release.
     *
     * <p>Zero is deliberately used as the "absent" marker rather than a negative sentinel: a {@code Version}
     * restored from an older save game or from an older client's network packet has no stored revision, and Java
     * deserialization leaves such a field at zero. Treating zero as absent therefore keeps those versions reading
     * exactly as they were written. It also means a hand-written {@code 0.51.0.0} is understood as plain
     * {@code 0.51.0}, which is the same thing.</p>
     */
    public static final int NO_REVISION = 0;

    /** Key of the optional revision entry in {@code Version.properties}. Absent for ordinary releases. */
    private static final String REVISION_BUNDLE_KEY = "revision";

    private int major;
    private int minor;
    private int patch;
    private int revision;
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
        setRevision(readOptionalRevisionFromBundle());

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

        // Three components is the ordinary case; a fourth is the optional revision of a point release. Anything
        // else is rejected rather than truncated, so a malformed version cannot silently become a valid one.
        final boolean hasTooFewComponents = versionSplit.length < 3;
        final boolean hasTooManyComponents = versionSplit.length > 4;
        if (hasTooFewComponents || hasTooManyComponents) {
            final String message = String.format(MMLoggingConstants.VERSION_ILLEGAL_VERSION_FORMAT, text);
            LOGGER.fatalDialog(message, MMLoggingConstants.VERSION_PARSE_FAILURE);
            return;
        }

        setMajor(MathUtility.parseInt(versionSplit[0]));
        setMinor(MathUtility.parseInt(versionSplit[1]));
        setPatch(MathUtility.parseInt(versionSplit[2]));
        setRevision((versionSplit.length == 4) ? MathUtility.parseInt(versionSplit[3], NO_REVISION) : NO_REVISION);
        setExtra(extraSplit.length == 2 ? extraSplit[1] : null);
    }

    /**
     * Sets the version. The resulting version has no revision component.
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
        // The no-argument constructor above seeds the revision from Version.properties, which describes the
        // running build rather than the version being constructed here. Clear it so that, on a point release,
        // an explicitly built version is not silently promoted to that release's revision.
        setRevision(NO_REVISION);
    }

    /**
     * Sets the version. The resulting version has no revision component.
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
        // See the note in the (int, int, int) constructor above.
        setRevision(NO_REVISION);
    }

    /**
     * Sets the version with Extra data. The resulting version has no revision component.
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

    /**
     * Sets the version with an optional revision and Extra data.
     *
     * <p>This is the form used when rebuilding a version that was written to a file, where the revision is simply
     * absent for anything produced by an ordinary release.</p>
     *
     * @param major    Major Version
     * @param minor    Minor Version
     * @param patch    Patch Version
     * @param revision Optional fourth component of a point release, or {@code null} when there is none
     * @param extra    Extra would be PR or nightly with git hash.
     */
    public Version(final String major, final String minor, final String patch, @Nullable final String revision,
          @Nullable final String extra) {
        this(major, minor, patch, extra);
        setRevision((revision == null) ? NO_REVISION : MathUtility.parseInt(revision, NO_REVISION));
    }
    // endregion Constructors

    /**
     * Reads the optional {@code revision} entry from {@code Version.properties}.
     *
     * <p>The key is absent for ordinary releases and is only added for a special point release such as
     * {@code 0.49.19.1}, so a missing key is the expected case and is not reported as a problem.</p>
     *
     * @return the configured revision, or {@link #NO_REVISION} when the key is absent, blank or not a number
     */
    private static int readOptionalRevisionFromBundle() {
        if (!VERSION_BUNDLE.containsKey(REVISION_BUNDLE_KEY)) {
            return NO_REVISION;
        }

        final String configuredRevision = VERSION_BUNDLE.getString(REVISION_BUNDLE_KEY).trim();
        if (configuredRevision.isEmpty()) {
            return NO_REVISION;
        }

        final int parsedRevision = MathUtility.parseInt(configuredRevision, NO_REVISION);
        if (parsedRevision <= NO_REVISION) {
            LOGGER.warn(
                  "[Version] Version.properties sets revision to '{}', which is not a positive number. "
                        + "The build will be versioned without a fourth component. Remove the entry for an "
                        + "ordinary release, or set it to a positive number for a point release.",
                  configuredRevision);
            return NO_REVISION;
        }

        LOGGER.debug("[Version] Optional revision component {} loaded from Version.properties", parsedRevision);
        return parsedRevision;
    }

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

    /**
     * @return the optional fourth version component, or {@link #NO_REVISION} when this version has none, which is
     *       the case for every ordinary release
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Sets the optional fourth version component.
     *
     * <p>A value at or below {@link #NO_REVISION} is stored as {@link #NO_REVISION}. Without that, a version
     * built from malformed text such as {@code 0.51.0.-1} would render as though it had no revision while still
     * comparing as lower than, and unequal to, the release it claims to be.</p>
     *
     * @param revision the optional fourth version component, or {@link #NO_REVISION} for no revision
     */
    public void setRevision(final int revision) {
        this.revision = Math.max(revision, NO_REVISION);
    }

    /**
     * @return {@code true} if this version carries a fourth component, as a point release such as
     *       {@code 0.49.19.1} does, otherwise {@code false}
     */
    public boolean hasRevision() {
        return revision > NO_REVISION;
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
        // Check the major version
        if (getMajor() > other.getMajor()) {
            return 1;
        } else if (getMajor() < other.getMajor()) {
            return -1;
        }

        // Major version is equal, try the minor version
        if (getMinor() > other.getMinor()) {
            return 1;
        } else if (getMinor() < other.getMinor()) {
            return -1;
        }

        // Minor version is also equal, try the patch version
        if (getPatch() > other.getPatch()) {
            return 1;
        } else if (getPatch() < other.getPatch()) {
            return -1;
        }

        // Patch version is also equal, try the optional revision. A version without one sorts below the point
        // releases built on top of it, so 0.51.0 is lower than 0.51.0.1.
        if (getRevision() > other.getRevision()) {
            return 1;
        } else if (getRevision() < other.getRevision()) {
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
                  getRevision() == other.getRevision() &&
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
        return String.format("%d.%02d.%02d%s%s",
              getMajor(),
              getMinor(),
              getPatch(),
              (hasRevision() ? "." + getRevision() : ""),
              (!getExtra().isEmpty() ? "-" + getExtra() : ""));
    }
}
