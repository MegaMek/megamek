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
package megamek.common.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies loading Bloodname data from {@code data/universe/bloodnames} - one folder per Clan, one
 * file per Bloodname.
 *
 * <p>A file that will not parse is logged and skipped rather than aborting the load, so these tests
 * assert the fields arrive rather than merely that nothing threw: a silently empty House would
 * otherwise look like success.</p>
 */
class Bloodnames2Test {

    private static final String TEST_DIRECTORY = "testresources/data/universe/bloodnames";

    private static Bloodnames2 bloodnames;

    @BeforeAll
    static void loadTestData() {
        bloodnames = new Bloodnames2(TEST_DIRECTORY);
    }

    @Test
    void bloodnamesLoadFromTheirOwnFiles() {
        assertFalse(bloodnames.isEmpty(), "the test data should have loaded");
        assertNotNull(bloodnames.getBloodname("Kerensky"));
        assertNotNull(bloodnames.getBloodname("Ward"));
    }

    @Test
    void lookupIgnoresCase() {
        // Nothing normalises the name a person carries, so a lookup has to tolerate any casing.
        assertNotNull(bloodnames.getBloodname("KERENSKY"));
        assertNotNull(bloodnames.getBloodname("kerensky"));
    }

    @Test
    void anUnknownOrBlankNameFindsNothing() {
        assertNull(bloodnames.getBloodname("NotARealBloodname"));
        assertNull(bloodnames.getBloodname(null));
        assertNull(bloodnames.getBloodname(""));
        assertTrue(bloodnames.getHouses(null).isEmpty());
        assertTrue(bloodnames.getHouses("NotARealBloodname").isEmpty());
    }

    @Test
    void aBloodnameCanCarrySeveralHouses() {
        // Sixteen Bloodnames descend from more than one founder.
        List<BloodnameHouse> houses = bloodnames.getHouses("Kerensky");
        assertEquals(2, houses.size(), "Kerensky descends from two founders");
        assertEquals("Andery", houses.get(0).getFounder());
        assertEquals("Nicholas", houses.get(1).getFounder());
    }

    @Test
    void bloodnamesAreIndexedByFoundingClan() {
        List<Bloodname2> founded = bloodnames.getBloodnamesFoundedBy("CW");
        assertEquals(2, founded.size());
        assertTrue(bloodnames.getBloodnamesFoundedBy("CJF").isEmpty(),
              "a Clan with no files should return empty, not fail");
        assertTrue(bloodnames.getBloodnamesFoundedBy(null).isEmpty());
    }

    @Test
    void theMechanicalFieldsSurvive() {
        // These decide whether a warrior may hold the name; losing any silently changes the outcome.
        BloodnameHouse house = bloodnames.getHouses("Kerensky").get(0);

        assertEquals("GENERAL", house.getPhenotype());
        assertTrue(house.isExclusive());
        assertTrue(house.isLimited());
        assertEquals(3060, house.getAbjured());
        assertEquals(3062, house.getDormant());
        assertEquals(3075, house.getReaved());
        assertEquals(3080, house.getReactivated());
        assertEquals(2807, house.getCreated());
        assertEquals(List.of("CW", "CWIE"), house.getPostReaving());

        assertNotNull(house.getAbsorbed());
        assertEquals("CSA", house.getAbsorbed().getClan());
        assertEquals(3059, house.getAbsorbed().getDate());
        assertEquals(1, house.getAcquired().size());
        assertEquals("CCC", house.getAcquired().get(0).getClan());
        assertEquals(2868, house.getAcquired().get(0).getDate());
        // A single shared record can name several Clans; each must arrive separately, or a Clan that
        // shares the legacy is never offered the name.
        assertEquals(2, house.getShared().size(), "CHH and CCO share this legacy");
        assertEquals("CHH", house.getShared().get(0).getClan());
        assertEquals("CCO", house.getShared().get(1).getClan());
        assertEquals(2872, house.getShared().get(1).getDate());
    }

    @Test
    void theDescriptiveFieldsSurvive() {
        BloodnameHouse house = bloodnames.getHouses("Kerensky").get(0);

        assertEquals("Andery Kerensky", house.getFounderFullName());
        assertEquals("Khan", house.getFounderRank());
        assertEquals("Clan Wolf", house.getFounderAffiliation());
        assertEquals("A Clan Wolf founding legacy.", house.getSummary());
        assertNull(house.getHistory(), "history is null until the histories are written");

        assertEquals(2, house.getNotableHolders().size());
        assertEquals("Natasha Kerensky", house.getNotableHolders().get(0).getName());
        assertEquals("Khan", house.getNotableHolders().get(0).getRank());
        assertNull(house.getNotableHolders().get(1).getRank(), "an unrecorded rank should be null");

        assertEquals(2, house.getSources().size());
        assertEquals("His:OK", house.getSources().get(0).getAbbrev());
        assertNull(house.getSources().get(1).getAbbrev(),
              "a publication with no sourcebook entry keeps its title and has no abbrev");
    }

    @Test
    void aHouseWithOnlyAFounderLeavesEverythingElseUnset() {
        // Most Houses fill only a handful of fields; the rest must default rather than fail to load.
        BloodnameHouse house = bloodnames.getHouses("Ward").get(0);

        assertEquals("Jal", house.getFounder());
        assertNull(house.getPhenotype());
        assertFalse(house.isExclusive());
        assertNull(house.getDormant());
        assertNull(house.getAbjured());
        assertNull(house.getAbsorbed());
        assertTrue(house.getAcquired().isEmpty());
        assertTrue(house.getShared().isEmpty());
        assertTrue(house.getPostReaving().isEmpty());
        assertTrue(house.getNotableHolders().isEmpty());
        assertTrue(house.getSources().isEmpty());
    }

    @Test
    void aMissingDirectoryLoadsNothingRatherThanFailing() {
        Bloodnames2 absent = new Bloodnames2("testresources/data/universe/no-such-directory");
        assertTrue(absent.isEmpty());
        assertTrue(absent.getHouses("Kerensky").isEmpty());
    }
}
