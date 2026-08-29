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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.rolls.PilotingRollData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The piloting roll for moving into water reports two separate things when a unit runs in: that running forced the
 * check at all, and how deep the water is. They used to share one description, so the report printed the same phrase
 * twice and read as a doubled penalty.
 */
class WaterEntryPilotingRollTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static BipedMek runningMek() {
        BipedMek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        // Without an explicit mode the Mek counts as unable to run in water, and the run entry never fires.
        mek.setMovementMode(EntityMovementMode.BIPED);
        mek.setGame(new Game());
        return mek;
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = new CoreRulesManager();
    }

    @Test
    void runningIntoWaterReportsTheRunAndTheDepthSeparately() {
        // Only the Core Rules let a Mek run in water at all, so this is the only ruleset where both entries appear
        // and the duplicated wording was visible.
        Game.rulesManager = new CoreRulesManager();

        String report = runningMek().checkWaterMove(1, EntityMovementType.MOVE_RUN).getDesc().toLowerCase();

        assertTrue(report.contains("running into depth 1 water"),
              "the entry that forced the check should say the run is what forced it: " + report);
        assertTrue(report.contains("entering depth 1 water"),
              "the entry carrying the depth modifier keeps its wording, which Princess matches on: " + report);
    }

    @Test
    void totalWarfareNeverAddsTheRunEntryBecauseMeksCannotRunInWater() {
        Game.rulesManager = new TWRulesManager();

        String report = runningMek().checkWaterMove(1, EntityMovementType.MOVE_RUN).getDesc().toLowerCase();

        assertFalse(report.contains("running into"),
              "Total Warfare does not let a Mek run in water, so nothing forced the check that way: " + report);
        assertTrue(report.contains("entering depth 1 water"), "the depth is still reported: " + report);
    }

    @Test
    void walkingIntoWaterReportsOnlyTheDepth() {
        Game.rulesManager = new TWRulesManager();

        String report = runningMek().checkWaterMove(1, EntityMovementType.MOVE_WALK).getDesc().toLowerCase();

        assertFalse(report.contains("running into"),
              "a walking unit was not forced into the check by running: " + report);
        assertTrue(report.contains("entering depth 1 water"), "the depth is still reported: " + report);
    }
}
