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
package megamek.client.ui.dialogs.randomArmy;

import static megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.preferredEchelonItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers the echelon a host can ask the Formation combo to open on. Codes are the ruleset's own: the echelon number
 * (constants.txt: LANCE 3, COMPANY 4, BATTALION 5, REGIMENT 6) plus an optional modifier - "+" reinforced, "-"
 * understrength, "^" augmented.
 */
class ForceGeneratorOptionsViewEchelonTest {

    @Test
    void noPreference_leavesTheRulesetDefaultInCharge() {
        assertNull(preferredEchelonItem(List.of("3", "4", "5"), null));
    }

    @Test
    void preferredEchelonPresent_isPicked() {
        assertEquals("4", preferredEchelonItem(List.of("3", "4", "5"), 4));
    }

    @Test
    void preferredEchelonAbsent_fallsThroughToTheRulesetDefault() {
        assertNull(preferredEchelonItem(List.of("3", "5", "6"), 4),
              "a faction that fields no company must fall back rather than pick a different size");
    }

    @Test
    void plainCodeWins_overAModifiedOneOfTheSameSize() {
        assertEquals("4", preferredEchelonItem(List.of("4+", "4", "4-"), 4),
              "an ordinary company opens ahead of a reinforced or understrength one");
    }

    @Test
    void modifiedCodeIsAcceptedWhenNoPlainOneExists() {
        assertEquals("4+", preferredEchelonItem(List.of("3", "4+", "5"), 4));
        assertEquals("4^", preferredEchelonItem(List.of("4^", "5"), 4));
    }

    @Test
    void modifiedCodeTakesTheFirstOffered() {
        assertEquals("4-", preferredEchelonItem(List.of("4-", "4+"), 4));
    }

    @Test
    void multiDigitEchelonsAreNotConfusedWithSingleDigitOnes() {
        assertNull(preferredEchelonItem(List.of("14", "41"), 4),
              "a code of 14 or 41 is not a company");
        assertEquals("14", preferredEchelonItem(List.of("14", "41"), 14));
    }

    @Test
    void emptyOrNullInputs_returnNull() {
        assertNull(preferredEchelonItem(List.of(), 4));
        assertNull(preferredEchelonItem(null, 4));
    }

    @Test
    void nullEntriesAreSkippedRatherThanThrowing() {
        List<String> codes = new ArrayList<>();
        codes.add(null);
        codes.add("4");
        assertEquals("4", preferredEchelonItem(codes, 4));
    }

    @Test
    void codeWithNoDigitsIsNeverMatched() {
        assertNull(preferredEchelonItem(List.of("^", "+"), 4),
              "an unparseable code must not match, and must not read as echelon 0 either");
        assertNull(preferredEchelonItem(List.of("^"), 0));
    }
}
