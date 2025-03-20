/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.common.universe;

import megamek.client.ratgenerator.FactionRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Faction2Test {

    @Test
    void isActiveInYear() {
        // A faction with no active year entries must count as always active
        var faction1 = new Faction2();
        assertTrue(faction1.isActiveInYear(1000));
        assertTrue(faction1.isActiveInYear(2000));
        assertTrue(faction1.isActiveInYear(3000));
        assertTrue(faction1.isActiveInYear(4000));

        faction1.getYearsActive().add(new FactionRecord.DateRange(null, 2500));
        assertTrue(faction1.isActiveInYear(1000));
        assertTrue(faction1.isActiveInYear(2000));
        assertFalse(faction1.isActiveInYear(3000));
        assertFalse(faction1.isActiveInYear(4000));

        var faction2 = new Faction2();
        faction2.getYearsActive().add(new FactionRecord.DateRange(100, 200));
        assertFalse(faction2.isActiveInYear(1000));
        assertFalse(faction2.isActiveInYear(2000));
        assertFalse(faction2.isActiveInYear(3000));
        assertFalse(faction2.isActiveInYear(4000));
    }
}
