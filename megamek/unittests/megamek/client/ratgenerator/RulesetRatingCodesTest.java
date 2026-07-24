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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifies {@link Ruleset#parseRatingCodes(String)} - the parser behind the random equipment-rating
 * roll that runs when the Equipment Rating picker is left on its Random default.
 */
class RulesetRatingCodesTest {

    @Test
    void parseRatingCodes_plainCodes() {
        assertEquals(List.of("A", "B", "C", "D", "F"), Ruleset.parseRatingCodes("A,B,C,D,F"));
    }

    @Test
    void parseRatingCodes_stripsDisplayNames() {
        assertEquals(List.of("Keshik", "FL", "SL", "PG"),
              Ruleset.parseRatingCodes("Keshik,FL:Front Line,SL:Second Line,PG:Provisional Garrison"));
    }

    @Test
    void parseRatingCodes_trimsWhitespaceAndSkipsBlanks() {
        assertEquals(List.of("A", "B"), Ruleset.parseRatingCodes(" A , B ,"));
        assertTrue(Ruleset.parseRatingCodes("").isEmpty());
        assertTrue(Ruleset.parseRatingCodes(" , ").isEmpty());
    }
}
