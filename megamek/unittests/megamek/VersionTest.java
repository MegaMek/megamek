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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
