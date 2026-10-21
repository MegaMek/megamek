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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.interfaces.IEntityRemovalConditions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for clearing a unit's removal condition between games (MekHQ issue #9332). The condition never
 * goes down through its setter, so a Mek wrecked in one battle and sent into another by MekHQ kept its salvageable
 * condition, and fleeing the later battle left a wreck rather than counting as a retreat.
 */
class EntityRemovalConditionTest {

    @Test
    void aLesserConditionDoesNotReplaceAGreaterOne() {
        BipedMek mek = new BipedMek();
        mek.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        mek.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);

        assertEquals(IEntityRemovalConditions.REMOVE_SALVAGEABLE, mek.getRemovalCondition(),
              "The setter keeps the more final condition");
    }

    @Test
    void resetClearsTheCondition() {
        BipedMek mek = new BipedMek();
        mek.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);

        mek.resetRemovalCondition();

        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, mek.getRemovalCondition());
    }

    @Test
    void aUnitWreckedEarlierIsRecordedAsRetreatingOnceReset() {
        BipedMek mek = new BipedMek();
        mek.setRemovalCondition(IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        mek.resetRemovalCondition();
        mek.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);

        assertEquals(IEntityRemovalConditions.REMOVE_IN_RETREAT, mek.getRemovalCondition(),
              "Fleeing a later battle counts as a retreat, not a wreck");
    }
}
