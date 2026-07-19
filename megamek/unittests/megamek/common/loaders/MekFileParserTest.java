/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MekFileParserTest {

    @BeforeEach
    void pinCanonUnitNames() {
        // The real list lives in docs/OfficialUnitList.txt, which is a build artifact and is absent on a clean
        // checkout. Pin a known list so the lookup can be tested at all. Must stay sorted: the lookup is a binary
        // search.
        MekFileParser.setCanonUnitNames(new Vector<>(List.of("Archer ARC-2R", "Atlas AS7-D")));
    }

    @AfterEach
    void restoreCanonUnitNames() {
        // Null restores the lazy default: the next lookup reads the real list again
        MekFileParser.setCanonUnitNames(null);
    }

    @Test
    void canonUnitNameIsRecognised() {
        assertTrue(MekFileParser.isCanonUnitName("Atlas AS7-D"));
        assertTrue(MekFileParser.isCanonUnitName("Archer ARC-2R"));
    }

    @Test
    void customUnitNameIsNotCanon() {
        // This is what MegaMekLab asks about a unit the player is editing
        assertFalse(MekFileParser.isCanonUnitName("Grimjack GRM-1A"));
    }

    @Test
    void renamingACanonUnitMakesItCustom() {
        // The whole point of exposing this: Entity.isCanon() is stamped at load, so it still reads true after an
        // editor renames the unit. Asking by name gives the current answer.
        assertTrue(MekFileParser.isCanonUnitName("Atlas AS7-D"));
        assertFalse(MekFileParser.isCanonUnitName("Atlas AS7-D2"));
    }

    @Test
    void unknownNameIsNotCanon() {
        assertFalse(MekFileParser.isCanonUnitName(""));
        assertFalse(MekFileParser.isCanonUnitName("Zzzz ZZZ-1"));
    }
}
