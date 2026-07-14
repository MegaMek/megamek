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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Faction2AresConventionsTest {
    @Test
    void defaultsToNonSignatory() {
        Faction2 faction = new Faction2();
        assertFalse(faction.isAresConventionsSignatory(2400));
        assertFalse(faction.isAresConventionsSignatory(2500));
        assertFalse(faction.isAresConventionsSignatory(3025));
    }

    @Test
    void followsDateKeyedSignatoryStatus() {
        Faction2 faction = new Faction2();
        faction.getAresConventionsSignatory().put(2412, true);
        faction.getAresConventionsSignatory().put(2786, false);

        assertFalse(faction.isAresConventionsSignatory(2411), "before the conventions were signed");
        assertTrue(faction.isAresConventionsSignatory(2412), "the year they were signed");
        assertTrue(faction.isAresConventionsSignatory(2700), "still observed");
        assertFalse(faction.isAresConventionsSignatory(2786), "renounced at the First Succession War");
        assertFalse(faction.isAresConventionsSignatory(3025), "long after renunciation");
    }
}
