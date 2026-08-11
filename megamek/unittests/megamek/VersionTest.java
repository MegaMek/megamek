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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionTest {
    private Version version;

    @BeforeEach
    void setUp() {
        version = new Version("1.50.5-test");
    }

    @AfterEach
    void tearDown() {
        version = null;
    }

    @Test
    void getMajor() {
        assertEquals(1, version.getMajor());
    }

    @Test
    void setMajor() {
        version.setMajor(2);
        assertEquals(2, version.getMajor());
    }

    @Test
    void getMinor() {
        assertEquals(50, version.getMinor());
    }

    @Test
    void setMinor() {
        version.setMinor(5);
        assertEquals(5, version.getMinor());
    }

    @Test
    void getPatch() {
        assertEquals(5, version.getPatch());
    }

    @Test
    void setPatch() {
        version.setPatch(6);
        assertEquals(6, version.getPatch());
    }

    @Test
    void getExtra() {
        assertEquals("test", version.getExtra());
    }

    @Test
    void setExtra() {
        version.setExtra("testing");
        assertEquals("testing", version.getExtra());
    }

    @Test
    void isHigherThan() {
        Version otherVersion = new Version("2.50.05");
        assertFalse(version.isHigherThan(otherVersion));
    }

    @Test
    void isLowerThan() {
        Version otherVersion = new Version("2.50.05");
        assertTrue(version.isLowerThan(otherVersion));
    }

    @Test
    void isBetween() {
        Version upperVersion = new Version("2.50.05");
        Version lowerVersion = new Version("0.50.05");
        assertTrue(version.isBetween(lowerVersion, upperVersion));
    }

    @Test
    void is() {
        Version otherVersion = new Version("1.50.05-test");
        assertTrue(version.is(otherVersion));
    }

    @Test
    void testToString() {
        assertEquals("1.50.05-test", version.toString());
    }

    @Test
    void ordinaryVersionHasNoRevision() {
        Version ordinaryVersion = versionWithoutExtra("0.51.0");
        assertFalse(ordinaryVersion.hasRevision());
        assertEquals(Version.NO_REVISION, ordinaryVersion.getRevision());
    }

    @Test
    void ordinaryVersionRendersWithoutRevision() {
        assertEquals("0.51.00", versionWithoutExtra("0.51.0").toString());
    }

    @Test
    void pointReleaseParsesRevision() {
        Version pointRelease = versionWithoutExtra("0.51.0.1");
        assertTrue(pointRelease.hasRevision());
        assertEquals(0, pointRelease.getMajor());
        assertEquals(51, pointRelease.getMinor());
        assertEquals(0, pointRelease.getPatch());
        assertEquals(1, pointRelease.getRevision());
    }

    @Test
    void pointReleaseRendersRevision() {
        assertEquals("0.51.00.1", versionWithoutExtra("0.51.0.1").toString());
    }

    @Test
    void pointReleaseKeepsRevisionAlongsideExtra() {
        Version nightlyPointRelease = new Version("0.51.0.1-nightly");
        assertEquals(1, nightlyPointRelease.getRevision());
        assertEquals("nightly", nightlyPointRelease.getExtra());
        assertEquals("0.51.00.1-nightly", nightlyPointRelease.toString());
    }

    @Test
    void pointReleaseIsHigherThanTheReleaseItBuildsOn() {
        assertTrue(new Version("0.51.0.1").isHigherThan(new Version("0.51.0")));
        assertTrue(new Version("0.51.0").isLowerThan(new Version("0.51.0.1")));
    }

    @Test
    void laterRevisionIsHigherThanEarlierRevision() {
        assertTrue(new Version("0.51.0.2").isHigherThan(new Version("0.51.0.1")));
    }

    @Test
    void revisionDoesNotOutrankTheNextPatchRelease() {
        assertTrue(new Version("0.51.1").isHigherThan(new Version("0.51.0.9")));
    }

    @Test
    void pointReleaseIsNotTheSameVersionAsTheReleaseItBuildsOn() {
        assertFalse(versionWithoutExtra("0.51.0.1").is(versionWithoutExtra("0.51.0")));
    }

    @Test
    void zeroRevisionIsTreatedAsNoRevision() {
        // A save game or network packet written before the revision component existed restores the field as zero,
        // so zero has to mean "no revision" for those to keep reading as the versions they were written by.
        Version explicitZeroRevision = versionWithoutExtra("0.51.0.0");
        assertFalse(explicitZeroRevision.hasRevision());
        assertEquals("0.51.00", explicitZeroRevision.toString());
        assertTrue(explicitZeroRevision.is(versionWithoutExtra("0.51.0")));
    }

    @Test
    void revisionIsReadBackFromTheSavedComponents() {
        Version savedPointRelease = new Version("0", "51", "0", "1", "");
        assertEquals(1, savedPointRelease.getRevision());
        assertEquals("0.51.00.1", savedPointRelease.toString());
    }

    @Test
    void absentSavedRevisionYieldsNoRevision() {
        Version savedOrdinaryRelease = new Version("0", "51", "0", null, "");
        assertFalse(savedOrdinaryRelease.hasRevision());
        assertEquals("0.51.00", savedOrdinaryRelease.toString());
    }

    @Test
    void pointReleaseIsNotEqualToTheReleaseItBuildsOn() {
        assertNotEquals(versionWithoutExtra("0.51.0.1"), versionWithoutExtra("0.51.0"));
    }

    @Test
    void negativeRevisionFromMalformedTextIsNormalisedToNoRevision() {
        // Without normalising, such a version would render as "0.51.00" while still sorting below, and being
        // unequal to, the 0.51.0 it renders as.
        Version malformedRevision = versionWithoutExtra("0.51.0.-1");
        assertFalse(malformedRevision.hasRevision());
        assertEquals(Version.NO_REVISION, malformedRevision.getRevision());
        assertEquals("0.51.00", malformedRevision.toString());
        assertEquals(versionWithoutExtra("0.51.0"), malformedRevision);
    }

    @Test
    void negativeRevisionSetDirectlyIsNormalisedToNoRevision() {
        Version ordinaryVersion = versionWithoutExtra("0.51.0");
        ordinaryVersion.setRevision(-1);
        assertFalse(ordinaryVersion.hasRevision());
        assertEquals(Version.NO_REVISION, ordinaryVersion.getRevision());
    }

    /**
     * Builds a version from its text form with the extra component explicitly cleared.
     *
     * <p>{@link Version#getExtra()} falls back to the running build's branch and git hash whenever the component
     * is {@code null}, and continuous integration builds do populate that. Clearing it keeps the assertions above
     * about the version number alone, whatever the build they run in.</p>
     *
     * @param text the version text to parse
     *
     * @return the parsed version, with no extra component
     */
    private static Version versionWithoutExtra(final String text) {
        Version parsedVersion = new Version(text);
        parsedVersion.setExtra("");
        return parsedVersion;
    }
}
