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

package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the movement cost of moving under water (MegaMek issue #8305).
 * <p>
 * The Terrain Table's two water rows describe how deep the unit is, not how deep the water is. A standing BattleMek
 * is two levels tall, so in Depth 1 water it wades with its head above the surface and pays the lower cost; in
 * Depth 2 it is completely under and pays the higher one. A ground vehicle is one level tall, so Depth 1 water
 * already covers it completely and it pays the fully submerged cost at every depth it can enter.
 */
class SubmergedMovementCostTest extends GameBoardTestCase {

    private static final String CV_ENVIRONMENTAL_SEALING = "Environmental Sealed Chassis";

    /** The base cost of a single forward step onto clear, level ground. */
    private static final int BASE_STEP_COST = 1;

    /** Terrain Table cost for wading, i.e. being only partly under the surface. */
    private static final int WADING_COST = 1;

    /** Terrain Table cost for being completely under the surface, under Total Warfare rules. */
    private static final int SUBMERGED_COST = 3;

    /** The same cost under the Core Rules ruleset, whose Playtest mobility changes lowered it. */
    private static final int SUBMERGED_COST_CORE_RULES = 2;

    /** Elevation of a unit standing on the bottom of a Depth 1 hex, so no elevation change is charged. */
    private static final int SEABED_OF_DEPTH_ONE = -1;

    /** Elevation of a unit standing on the bottom of a Depth 2 hex. */
    private static final int SEABED_OF_DEPTH_TWO = -2;

    static {
        initializeBoard("SHALLOW_WATER", """
              size 1 3
              hex 0101 0 "water:1" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "water:1" ""
              end""");
        initializeBoard("DEEP_WATER", """
              size 1 3
              hex 0101 0 "water:2" ""
              hex 0102 0 "water:2" ""
              hex 0103 0 "water:2" ""
              end""");
    }

    private RulesManager originalRulesManager;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void useTotalWarfareRules() {
        originalRulesManager = Game.rulesManager;
        Game.rulesManager = new TWRulesManager();
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = originalRulesManager;
    }

    private static Tank sealedTank() throws LocationFullException {
        Tank tank = new Tank();
        tank.setWeight(50.0);
        EquipmentType sealing = EquipmentType.get(CV_ENVIRONMENTAL_SEALING);
        assertNotNull(sealing, "The Combat Vehicle Environmental Sealing chassis mod must exist");
        tank.addEquipment(sealing, Tank.LOC_BODY);
        return tank;
    }

    @Test
    void sealedVehiclePaysTheSubmergedCostInDepthOneWater() throws LocationFullException {
        setBoard("SHALLOW_WATER");

        MovePath movePath = getMovePathFor(sealedTank(), SEABED_OF_DEPTH_ONE, EntityMovementMode.TRACKED,
              MoveStepType.FORWARDS);

        assertEquals(BASE_STEP_COST + SUBMERGED_COST, movePath.getMpUsed(),
              "A one-level-tall vehicle is completely under the surface in Depth 1 water");
    }

    @Test
    void sealedVehiclePaysTheSubmergedCostInDepthTwoWater() throws LocationFullException {
        setBoard("DEEP_WATER");

        MovePath movePath = getMovePathFor(sealedTank(), SEABED_OF_DEPTH_TWO, EntityMovementMode.TRACKED,
              MoveStepType.FORWARDS);

        assertEquals(BASE_STEP_COST + SUBMERGED_COST, movePath.getMpUsed(),
              "The cost does not change with depth once the vehicle is already fully submerged");
    }

    @Test
    void standingMekStillWadesThroughDepthOneWater() {
        setBoard("SHALLOW_WATER");

        MovePath movePath = getMovePathFor(new BipedMek(), SEABED_OF_DEPTH_ONE, EntityMovementMode.BIPED,
              MoveStepType.FORWARDS);

        assertEquals(BASE_STEP_COST + WADING_COST, movePath.getMpUsed(),
              "A standing Mek keeps its head above Depth 1 water, so its cost is unchanged");
    }

    @Test
    void standingMekPaysTheSubmergedCostInDepthTwoWater() {
        setBoard("DEEP_WATER");

        MovePath movePath = getMovePathFor(new BipedMek(), SEABED_OF_DEPTH_TWO, EntityMovementMode.BIPED,
              MoveStepType.FORWARDS);

        assertEquals(BASE_STEP_COST + SUBMERGED_COST, movePath.getMpUsed(),
              "A Mek is completely under the surface in Depth 2 water, so its cost is unchanged");
    }

    @Test
    void theSubmergedCostFollowsTheSelectedRuleset() throws LocationFullException {
        setBoard("SHALLOW_WATER");
        Game.rulesManager = new CoreRulesManager();

        MovePath movePath = getMovePathFor(sealedTank(), SEABED_OF_DEPTH_ONE, EntityMovementMode.TRACKED,
              MoveStepType.FORWARDS);

        assertEquals(BASE_STEP_COST + SUBMERGED_COST_CORE_RULES, movePath.getMpUsed(),
              "The submerged vehicle still pays the submerged cost, at the Core Rules value");
    }
}
