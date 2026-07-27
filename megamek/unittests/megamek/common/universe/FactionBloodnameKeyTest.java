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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/**
 * Verifies that Bloodname data in a faction file loads, and loads completely.
 *
 * <p>Faction files are read with a plain Jackson mapper, which rejects unknown properties, and
 * {@code Factions2} catches the resulting exception and skips the file. An unrecognised key therefore
 * raises nothing at all - the Clan simply disappears from the game. That is why the first test here
 * matters more than it looks: it is the guard against a data addition silently deleting every Clan.</p>
 */
class FactionBloodnameKeyTest {

    /** Exercises every field the converter emits, including both transfer kinds and two Houses. */
    private static final String FACTION_YAML = """
          key: TESTCLAN
          name: Test Clan
          tags:
            - CLAN
          bloodnames:
            - name: Kerensky
              houses:
                - founder: Andery
                  founderFullName: Andery Kerensky
                  founderRank: Khan
                  founderAffiliation: Clan Wolf
                  phenotype: GENERAL
                  exclusive: true
                  limited: true
                  abjured: true
                  shared: true
                  dormant: 3062
                  reaved: 3075
                  reactivated: 3080
                  created: 2807
                  postReaving: [CW, CWIE]
                  absorbed: { clan: CSA, date: 3059 }
                  acquired: { clan: CCC, date: 2868 }
                  summary: A Clan Wolf founding legacy.
                  notableHolders:
                    - { name: Natasha Kerensky, rank: Khan, affiliation: Clan Wolf }
                    - { name: Nicholas Kerensky }
                  sources:
                    - { title: 'Historical: Operation Klondike', abbrev: 'His:OK' }
                    - { title: 'Field Manual: Warden Clans' }
                  history: ~
                - founder: Nicholas
          """;

    private static Faction2 load(String yaml) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
              Faction2.class);
    }

    @Test
    void aFactionCarryingBloodnamesStillLoads() throws Exception {
        assertNotNull(load(FACTION_YAML), "a faction file with a bloodnames block must still parse");
    }

    @Test
    void aFactionWithoutBloodnamesHasAnEmptyList() throws Exception {
        Faction2 faction = load("key: TESTIS\nname: Test House\n");
        assertNotNull(faction.getBloodnames(), "the list should be empty, never null");
        assertTrue(faction.getBloodnames().isEmpty());
    }

    @Test
    void aBloodnameCanCarrySeveralHouses() throws Exception {
        // Sixteen Bloodnames descend from more than one founder, which is why houses is a list.
        List<Bloodname2> bloodnames = load(FACTION_YAML).getBloodnames();
        assertEquals(1, bloodnames.size());
        assertEquals("Kerensky", bloodnames.get(0).getName());
        assertEquals(2, bloodnames.get(0).getHouses().size(), "Kerensky descends from two founders");
        assertEquals("Nicholas", bloodnames.get(0).getHouses().get(1).getFounder());
    }

    @Test
    void theMechanicalFieldsSurvive() throws Exception {
        // These are what decide whether a warrior may hold the name; losing any of them silently
        // changes which Bloodnames the game hands out.
        BloodnameHouse house = load(FACTION_YAML).getBloodnames().get(0).getHouses().get(0);

        assertEquals("GENERAL", house.getPhenotype());
        assertTrue(house.isExclusive());
        assertTrue(house.isLimited());
        assertTrue(house.isAbjured());
        assertTrue(house.isShared());
        assertEquals(3062, house.getDormant());
        assertEquals(3075, house.getReaved());
        assertEquals(3080, house.getReactivated());
        assertEquals(2807, house.getCreated());
        assertEquals(List.of("CW", "CWIE"), house.getPostReaving());

        assertNotNull(house.getAbsorbed());
        assertEquals("CSA", house.getAbsorbed().getClan());
        assertEquals(3059, house.getAbsorbed().getDate());
        assertNotNull(house.getAcquired());
        assertEquals("CCC", house.getAcquired().getClan());
        assertEquals(2868, house.getAcquired().getDate());
    }

    @Test
    void theDescriptiveFieldsSurvive() throws Exception {
        BloodnameHouse house = load(FACTION_YAML).getBloodnames().get(0).getHouses().get(0);

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
              "a publication with no sourcebook entry should carry a title and no abbrev");
    }

    @Test
    void aHouseWithOnlyAFounderLeavesEverythingElseUnset() throws Exception {
        // Most Houses fill only a handful of fields; the rest must default rather than fail to load.
        BloodnameHouse house = load(FACTION_YAML).getBloodnames().get(0).getHouses().get(1);

        assertEquals("Nicholas", house.getFounder());
        assertNull(house.getPhenotype());
        assertFalse(house.isExclusive());
        assertNull(house.getDormant());
        assertNull(house.getAbsorbed());
        assertTrue(house.getPostReaving().isEmpty());
        assertTrue(house.getNotableHolders().isEmpty());
        assertTrue(house.getSources().isEmpty());
    }
}
