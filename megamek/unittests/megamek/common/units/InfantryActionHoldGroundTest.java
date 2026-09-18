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

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Infantry committed to an infantry vs. infantry action (TO:AR p. 169) sit out the Movement, Weapon Attack and
 * Physical Attack phases, so the only way out of the fight is the withdrawal the book provides.
 */
@DisplayName("Infantry action: committed units hold their ground")
class InfantryActionHoldGroundTest {

    private static final int BUILDING_ID = 7;

    private Game game;
    private ConvInfantry platoon;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        Player player = new Player(0, "Player");
        game.addPlayer(0, player);
        game.setBoard(new Board(16, 17));

        platoon = new ConvInfantry();
        platoon.setOwner(player);
        platoon.setGame(game);
        platoon.setSquadSize(28);
        platoon.setSquadCount(1);
        platoon.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        platoon.setId(1);
        game.addEntity(platoon);
        platoon.setPosition(new Coords(5, 5));
        platoon.setDeployed(true);
    }

    private void commitToAnAction() {
        platoon.setInfantryCombatTargetId(BUILDING_ID);
        platoon.setInfantryCombatAttacker(true);
    }

    private void setRule(boolean isOn) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_INFANTRY_ACTION_COMMITTED_UNITS_HOLD)
              .setValue(isOn);
    }

    @Test
    @DisplayName("The rule is on unless the players turn it off")
    void ruleDefaultsToOn() {
        assertTrue(game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_INFANTRY_ACTION_COMMITTED_UNITS_HOLD));
    }

    @Test
    @DisplayName("A platoon in no action moves as usual; once committed it gets no movement turn")
    void committedPlatoonGetsNoMovementTurn() {
        assertTrue(platoon.isEligibleFor(GamePhase.MOVEMENT), "the same platoon, uncommitted, does get a turn");

        commitToAnAction();

        assertFalse(platoon.isEligibleFor(GamePhase.MOVEMENT));
    }

    @Test
    @DisplayName("A committed platoon sits out firing and physical attacks too")
    void committedPlatoonSitsOutTheAttackPhases() {
        commitToAnAction();

        assertTrue(platoon.isHeldInPlaceByInfantryAction(GamePhase.FIRING));
        assertTrue(platoon.isHeldInPlaceByInfantryAction(GamePhase.PHYSICAL));
        assertTrue(platoon.isHeldInPlaceByInfantryAction(GamePhase.TARGETING));
        assertTrue(platoon.isHeldInPlaceByInfantryAction(GamePhase.OFFBOARD));
        assertFalse(platoon.isEligibleFor(GamePhase.FIRING));
        assertFalse(platoon.isEligibleFor(GamePhase.PHYSICAL));
    }

    @Test
    @DisplayName("It is not held out of the declarations phase, which is where it withdraws")
    void committedPlatoonStillDeclares() {
        commitToAnAction();

        assertFalse(platoon.isHeldInPlaceByInfantryAction(GamePhase.PREEND_DECLARATIONS));
        assertFalse(platoon.isHeldInPlaceByInfantryAction(GamePhase.END));
    }

    @Test
    @DisplayName("With the rule off, a committed platoon moves and fires as before")
    void ruleOffLeavesCommittedPlatoonFree() {
        setRule(false);
        commitToAnAction();

        assertFalse(platoon.isHeldInPlaceByInfantryAction(GamePhase.MOVEMENT));
        assertTrue(platoon.isEligibleFor(GamePhase.MOVEMENT));
    }

    @Test
    @DisplayName("When the action ends the platoon is free again")
    void freedWhenTheActionEnds() {
        commitToAnAction();
        platoon.clearInfantryCombatState();

        assertTrue(platoon.isEligibleFor(GamePhase.MOVEMENT));
    }
}
