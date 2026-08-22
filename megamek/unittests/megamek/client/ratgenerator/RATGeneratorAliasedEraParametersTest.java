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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import megamek.client.ratgenerator.FactionRecord.TechCategory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * An era file's {@code <factions>} section carries the per-era tuning for each faction: how much Clan and Star League
 * tech it fields, its margins, and who it takes salvage from. Those blocks are keyed by faction code, and a code that
 * has since been retired has to resolve to the faction that absorbed it.
 *
 * <p>It used to be looked up directly in the faction map, which by design holds no alias keys, so every block written
 * under a retired code was dropped with an error and the faction silently fell back on its parent's tech mix. Clan
 * Goliath Scorpion is the live case: it absorbed the Escorpion Imperio and the Scorpion Empire, and its blocks from
 * 3082 on are still keyed {@code CEI} and then {@code SE}, so it had no tuning of its own for seven era buckets.</p>
 *
 * <p>The test data mirrors both shapes the shipped files take. Era 3050 holds only the retired code, as 3082 through
 * 3160 do. Era 3060 holds the surviving and the retired code together, as 3078 and 3131 do.</p>
 */
class RATGeneratorAliasedEraParametersTest {

    /** Holds a block under the retired code only, with no block for the surviving faction. */
    private static final int ERA_WITH_ONLY_THE_RETIRED_CODE = 3050;
    /** Holds a block under each code, the surviving one first. */
    private static final int ERA_WITH_BOTH_CODES = 3060;

    /** The surviving faction, standing in for {@code CGS}. */
    private static final String SURVIVING_FACTION = "SUC";
    /** The retired code it absorbed, standing in for {@code CEI} and {@code SE}. */
    private static final String RETIRED_FACTION_CODE = "RET";

    /** Equipment rating to read the percentage lists at; the test faction declares the usual five levels. */
    private static final int TOP_RATING = 4;

    private static RATGenerator ratGenerator;

    @BeforeAll
    static void loadForceGeneratorFromTestData() throws Exception {
        ratGenerator = ForceGeneratorTestFixture.loadFromTestData(ERA_WITH_ONLY_THE_RETIRED_CODE);
        ratGenerator.loadYear(ERA_WITH_BOTH_CODES);
    }

    @AfterAll
    static void clearSharedSingletons() throws Exception {
        ForceGeneratorTestFixture.reset();
    }

    @Test
    void aBlockWrittenUnderTheRetiredCodeTunesTheSurvivingFaction() {
        FactionRecord survivingFaction = survivingFaction();
        int era = ERA_WITH_ONLY_THE_RETIRED_CODE;

        assertEquals(50, survivingFaction.getPctTech(TechCategory.OMNI, era, TOP_RATING),
              "The Omni percentage from the retired code's block should have loaded into the surviving faction");
        assertEquals(51, survivingFaction.getPctTech(TechCategory.CLAN, era, TOP_RATING),
              "The Clan percentage from the retired code's block should have loaded into the surviving faction");
        assertEquals(49, survivingFaction.getPctTech(TechCategory.IS_ADVANCED, era, TOP_RATING),
              "The Star League percentage from the retired code's block should have loaded into the surviving"
                    + " faction");
    }

    @Test
    void theMarginsAndSalvageFromTheRetiredCodeLoadToo() {
        FactionRecord survivingFaction = survivingFaction();
        int era = ERA_WITH_ONLY_THE_RETIRED_CODE;

        assertEquals(7, survivingFaction.getTechMargin(era),
              "A tech margin that never loads reads back as zero, which is the bug this pins down");
        assertEquals(13, survivingFaction.getPctSalvage(era),
              "The salvage percentage from the retired code's block should have loaded");

        Map<String, Integer> salvage = survivingFaction.getSalvage(era);
        assertEquals(2, salvage.get("LA"), "The salvage table from the retired code's block should have loaded");
    }

    @Test
    void theRetiredCodeIsNotItselfListedAsAFaction() {
        boolean retiredCodeIsListed = ratGenerator.getFactionList()
              .stream()
              .anyMatch(factionRecord -> RETIRED_FACTION_CODE.equals(factionRecord.getKey()));

        assertFalse(retiredCodeIsListed,
              "Resolving the retired code must not add it to the faction list, or the same faction would be offered"
                    + " to the player twice");
        assertSame(survivingFaction(), ratGenerator.getFaction(RETIRED_FACTION_CODE),
              "The retired code should resolve to the surviving faction");
    }

    @Test
    void aRetiredBlockAlongsideTheSurvivingOneReplacesOnlyTheFieldsItSets() {
        FactionRecord survivingFaction = survivingFaction();
        int era = ERA_WITH_BOTH_CODES;

        assertEquals(9, survivingFaction.getTechMargin(era),
              "Both blocks set the tech margin, and the retired one is read second, so its value should stand");
        assertEquals(64, survivingFaction.getPctTech(TechCategory.OMNI, era, TOP_RATING),
              "The retired block sets no Omni percentage, so the surviving block's value must survive the merge");
    }

    private static FactionRecord survivingFaction() {
        FactionRecord survivingFaction = ratGenerator.getFaction(SURVIVING_FACTION);
        assertNotNull(survivingFaction, "Test data should provide faction " + SURVIVING_FACTION);
        return survivingFaction;
    }
}
