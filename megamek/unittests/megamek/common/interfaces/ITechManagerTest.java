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

package megamek.common.interfaces;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import org.junit.jupiter.api.Test;

class ITechManagerTest {

    @Test
    void isLegalUsesAdditionalTechAvailabilityYears() {
        ITechnology lostech = new TechAdvancement(TechBase.IS)
              .setISAdvancement(2500, 2600, 2700, 3025, ITechnology.DATE_NONE);

        assertFalse(new TestTechManager(3050).isLegal(lostech));
        assertTrue(new TestTechManager(3050, 3000).isLegal(lostech));
    }

    private static class TestTechManager implements ITechManager {
        private final List<Integer> techAvailabilityYears;

        private TestTechManager(Integer... techAvailabilityYears) {
            this.techAvailabilityYears = List.of(techAvailabilityYears);
        }

        @Override
        public int getTechIntroYear() {
            return techAvailabilityYears.get(0);
        }

        @Override
        public int getGameYear() {
            return techAvailabilityYears.get(0);
        }

        @Override
        public List<Integer> getTechAvailabilityYears() {
            return techAvailabilityYears;
        }

        @Override
        public Faction getTechFaction() {
            return Faction.NONE;
        }

        @Override
        public boolean useClanTechBase() {
            return false;
        }

        @Override
        public boolean useMixedTech() {
            return false;
        }

        @Override
        public SimpleTechLevel getTechLevel() {
            return SimpleTechLevel.STANDARD;
        }

        @Override
        public boolean unofficialNoYear() {
            return false;
        }

        @Override
        public boolean useVariableTechLevel() {
            return false;
        }

        @Override
        public boolean showExtinct() {
            return false;
        }
    }
}