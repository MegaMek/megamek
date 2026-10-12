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

import static megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.ratingToSelect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The Equipment Rating picker and the force it generates must agree. Before, a faction's default rating was given to
 * the force even when the picker did not offer it: Clan Wolf infantry showed Garrison but generated as Front Line.
 */
class ForceGeneratorRatingTest {

    @Test
    void aDefaultThatIsOfferedIsUsed() {
        assertEquals("FL", ratingToSelect(List.of("Keshik", "FL", "SL", "PG", "Sol"), "FL"));
    }

    @Test
    void aDefaultThatIsNotOfferedGivesWayToTheFirstOffered() {
        assertEquals("PG", ratingToSelect(List.of("PG", "Sol"), "FL"),
              "Clan Wolf infantry has no Front Line; the force must take the Garrison the picker shows");
    }

    @Test
    void withNothingOfferedTheDefaultStands() {
        assertEquals("FL", ratingToSelect(List.of(), "FL"));
    }

    @Test
    void noDefaultAndNothingOfferedGivesNoRating() {
        assertNull(ratingToSelect(List.of(), null));
    }
}
