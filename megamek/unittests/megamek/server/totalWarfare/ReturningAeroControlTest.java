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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AeroSpaceFighter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9297: a fighter that lost control from heat and flew off the map in the same round came
 * back out of control, because the heat result only takes effect at the start of the next round, after the fly-off
 * had cleared the out-of-control state.
 */
class ReturningAeroControlTest {

    private TWGameManager gameManager;
    private Game game;
    private Player player;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        player = new Player(0, "Test");
        game.addPlayer(0, player);
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
    }

    private AeroSpaceFighter createFlyingOffFighter() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setOwner(player);
        game.addEntity(fighter);
        fighter.setDeployed(true);
        fighter.setFlyingOff(OffBoardDirection.WEST);
        return fighter;
    }

    @Test
    void fighterThatLostControlFromHeatReturnsInControl() {
        AeroSpaceFighter fighter = createFlyingOffFighter();
        // The heat phase failed the control roll this round; the end phase then flies the fighter off the map
        fighter.setOutControlHeat(true);

        gameManager.processFlyingOff(fighter);
        fighter.newRound(2);

        assertFalse(fighter.isOutControl(), "A fighter that flew off should come back under control");
    }

    @Test
    void heatLossOfControlStillAppliesToFighterThatStaysOnTheMap() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setOwner(player);
        game.addEntity(fighter);
        fighter.setDeployed(true);
        fighter.setOutControlHeat(true);

        fighter.newRound(2);

        assertTrue(fighter.isOutControl(), "A fighter that stays on the map must still lose control from heat");
    }
}
