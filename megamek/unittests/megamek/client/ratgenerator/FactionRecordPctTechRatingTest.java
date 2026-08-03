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
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.client.ratgenerator.FactionRecord.TechCategory;
import org.junit.jupiter.api.Test;

/**
 * Covers how a faction's Clan/Star League/Omni percentages are indexed by equipment rating.
 *
 * <p>Those lists hold one value per rating level the faction itself declares
 * ({@code docs/Customization/RAT and Force Generator Stuff/rat-generator.txt}). A subcommand that declares a single
 * rating level therefore records a single value, while the rating it is asked about is a position in its parent's
 * larger system. Reading the one-entry list at that position missed every time, and the miss was indistinguishable
 * from "this faction declares nothing", so the subcommand's own percentages were dropped in favour of its parent's.</p>
 */
class FactionRecordPctTechRatingTest {

    private static final int ERA = 3078;
    /** The Inner Sphere rating system is F, D, C, B, A, so A sits at index 4. */
    private static final int RATING_INDEX_A = 4;
    private static final int RATING_INDEX_F = 0;
    /** What a caller passes when the faction applies no rating adjustments at all. */
    private static final int NO_RATING_ADJUSTMENT = -1;

    /**
     * A subcommand locked to one rating within its parent's system, as CGB.FRR is locked to A within the Free
     * Rasalhague Republic's F-A system.
     */
    private static FactionRecord subcommandLockedToRatingA() {
        FactionRecord subcommand = new FactionRecord("CGB.FRR", "Kungsarme");
        subcommand.setRatings("A");
        subcommand.setParentFactions("FRR");
        subcommand.setPctTech(TechCategory.IS_ADVANCED, ERA, "76");
        return subcommand;
    }

    private static FactionRecord factionWithFullRatingSystem() {
        FactionRecord faction = new FactionRecord("FRR", "Free Rasalhague Republic");
        faction.setRatings("F,D,C,B,A");
        faction.setPctTech(TechCategory.IS_ADVANCED, ERA, "13,19,33,46,74");
        return faction;
    }

    @Test
    void singleRatingSubcommandReadsItsOwnDeclaredPercentage() {
        FactionRecord subcommand = subcommandLockedToRatingA();

        assertEquals(76, subcommand.getPctTech(TechCategory.IS_ADVANCED, ERA, RATING_INDEX_A),
              "The subcommand declares one value for its one rating level, so asking at its rating must find it"
                    + " rather than fall through to the parent faction");
    }

    @Test
    void factionWithAFullRatingSystemStillReadsByRating() {
        FactionRecord faction = factionWithFullRatingSystem();

        assertEquals(74, faction.getPctTech(TechCategory.IS_ADVANCED, ERA, RATING_INDEX_A));
        assertEquals(13, faction.getPctTech(TechCategory.IS_ADVANCED, ERA, RATING_INDEX_F));
    }

    @Test
    void aCategoryTheFactionNeverDeclaresIsStillAbsent() {
        FactionRecord subcommand = subcommandLockedToRatingA();

        assertNull(subcommand.getPctTech(TechCategory.CLAN, ERA, RATING_INDEX_A),
              "Only the declared category should resolve; an undeclared one must stay absent so the caller can fall"
                    + " back");
    }

    @Test
    void anEraTheFactionNeverDeclaresIsStillAbsent() {
        FactionRecord subcommand = subcommandLockedToRatingA();

        assertNull(subcommand.getPctTech(TechCategory.IS_ADVANCED, 3145, RATING_INDEX_A));
    }

    @Test
    void aRatingBeyondAFullRatingSystemIsAbsentRatherThanAnError() {
        FactionRecord faction = factionWithFullRatingSystem();

        assertNull(faction.getPctTech(TechCategory.IS_ADVANCED, ERA, 9),
              "A rating the faction has no column for must read as absent");
    }

    @Test
    void aNegativeRatingIsAbsentRatherThanAnErrorForAFullRatingSystem() {
        FactionRecord faction = factionWithFullRatingSystem();

        assertNull(faction.getPctTech(TechCategory.IS_ADVANCED, ERA, NO_RATING_ADJUSTMENT),
              "A faction that applies no rating adjustments is asked with -1, which must not reach into the list");
    }

    @Test
    void singleRatingSubcommandStillReadsItsValueWhenNoRatingAdjustmentApplies() {
        FactionRecord subcommand = subcommandLockedToRatingA();

        assertEquals(76, subcommand.getPctTech(TechCategory.IS_ADVANCED, ERA, NO_RATING_ADJUSTMENT),
              "A subcommand declares one value that holds whatever rating is asked about, so -1 must read it rather"
                    + " than report nothing and send the lookup to the parent faction");
    }
}
