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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins how a generation context reads a player's choices, especially the two that decide what a
 * later force builder can do with it: an absent faction must become the Inner Sphere rather than
 * nothing, and a sub-command must stay recognisable as one.
 */
class GenerationContextTest {

    @Test
    void noFactionChosenMeansTheInnerSphereAtLarge() {
        GenerationContext context = GenerationContext.defaultFor(3067);

        assertEquals(FactionRecord.IS_GENERAL_KEY, context.faction());
        assertEquals(3067, context.year());
        assertNull(context.rating());
        assertEquals(GenerationContext.Source.UNSPECIFIED, context.source(),
              "a default is not a choice, and must not be reported as one");
    }

    @Test
    void aNullFactionFromATabAlsoFallsBackToTheInnerSphere() {
        GenerationContext context = GenerationContext.of(null, 3025, "B",
              GenerationContext.Source.RAT_GENERATOR);

        assertEquals(FactionRecord.IS_GENERAL_KEY, context.faction());
        assertEquals("B", context.rating(), "a rating without a faction is still worth keeping");
    }

    @Test
    void aDottedKeyIsRecognisedAsASubCommandOfItsParent() {
        GenerationContext hussars = new GenerationContext("FS.CH", 3067, "A",
              GenerationContext.Source.RAT_GENERATOR);

        assertTrue(hussars.hasSubCommand());
        assertEquals("FS", hussars.parentFaction(),
              "a command falls back to its parent faction when it has no rules of its own");
    }

    @Test
    void aPlainKeyIsTheFactionAtLargeAndIsItsOwnParent() {
        GenerationContext suns = new GenerationContext("FS", 3067, null,
              GenerationContext.Source.FORMATION_BUILDER);

        assertFalse(suns.hasSubCommand());
        assertEquals("FS", suns.parentFaction());
    }

    @Test
    void aBlankRatingIsRecordedAsNoRating() {
        FactionRecord factionRecord = new FactionRecord("LA", "Lyran Alliance");

        assertNull(GenerationContext.of(factionRecord, 3050, "", GenerationContext.Source.RAT_GENERATOR)
              .rating(), "an empty combo selection is not a rating");
        assertNull(GenerationContext.of(factionRecord, 3050, null, GenerationContext.Source.RAT_GENERATOR)
              .rating());
        assertEquals("LA", GenerationContext.of(factionRecord, 3050, "C",
              GenerationContext.Source.RAT_GENERATOR).faction());
    }

    @Test
    void theSummaryNamesTheYearAndOnlyMentionsARatingWhenThereIsOne() {
        GenerationContext withRating = new GenerationContext("IS", 3067, "A",
              GenerationContext.Source.RAT_GENERATOR);
        GenerationContext withoutRating = new GenerationContext("IS", 3067, null,
              GenerationContext.Source.RAT_GENERATOR);

        assertTrue(withRating.describe().contains("3067"));
        assertTrue(withRating.describe().contains("rating A"));
        assertFalse(withoutRating.describe().contains("rating"));
    }
}
