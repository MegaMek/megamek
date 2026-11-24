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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A Combat Vehicle whose crew is killed by a Crew Killed critical hit stays on the board, but is immobile and counts
 * as destroyed for victory (TW p.194).
 */
class TankCrewKilledTest {

    private Game game;
    private Player owner;
    private Tank tank;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        owner = new Player(1, "Owner");
        game.addPlayer(1, owner);

        tank = new Tank();
        tank.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setOriginalWalkMP(4);
        tank.setWeight(25);
        for (int location = 0; location < tank.locations(); location++) {
            tank.initializeArmor(10, location);
            tank.initializeInternal(3, location);
        }
        tank.setId(1);
        tank.setOwner(owner);
        tank.setPosition(new Coords(3, 3));
        tank.setDeployed(true);
        game.addEntity(tank, false);
    }

    /**
     * Does what the server does at the start of the phase after the critical hit: the doomed crew dies, then damage
     * marked during the last phase is applied.
     */
    private void startNextPhase() {
        tank.newPhase(GamePhase.MOVEMENT);
        tank.applyDamage();
    }

    private void killCrew() {
        tank.getCrew().setDoomed(true);
        startNextPhase();
    }

    private void enableNoImmobileVehiclesOption() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_UNOFF_NO_IMMOBILE_VEHICLES)
              .setValue(true);
    }

    @Test
    @DisplayName("A vehicle with a killed crew is immobile, but stays on the board intact")
    void crewKilledVehicleIsImmobile() {
        killCrew();

        assertTrue(tank.isImmobile(), "TW p.194: without its crew the vehicle is considered immobile");
        assertFalse(tank.isDestroyed(), "TW p.194: the vehicle remains intact and can still be targeted");
        assertFalse(tank.isSelectableThisTurn(), "without its crew the vehicle can take no action");
    }

    @Test
    @DisplayName("A vehicle with a killed crew counts as destroyed for victory")
    void crewKilledVehicleCountsAsDestroyedForVictory() {
        assertTrue(owner.getBV() > 0, "the healthy vehicle should add its BV to its owner");

        killCrew();

        assertEquals(0, owner.getBV(), "BV victory conditions must not count a vehicle whose crew was killed");
        assertEquals(0, game.getLiveDeployedEntitiesOwnedBy(owner),
              "battlefield control must not count a vehicle whose crew was killed");
        List<Entity> carcasses = Collections.list(game.getCarcassEntities());
        assertTrue(carcasses.contains(tank), "kill count victory counts carcasses as kills");
    }

    @Test
    @DisplayName("The unofficial no-immobile-vehicles option still waives the penalty for motive damage")
    void noImmobileVehiclesOptionWaivesMotiveDamage() {
        enableNoImmobileVehiclesOption();

        tank.immobilize();
        startNextPhase();

        assertFalse(tank.isImmobile(), "with the option on, a drive or engine hit does not make the vehicle immobile");
    }

    @Test
    @DisplayName("The unofficial no-immobile-vehicles option does not cover a killed crew")
    void noImmobileVehiclesOptionDoesNotCoverKilledCrew() {
        enableNoImmobileVehiclesOption();

        killCrew();

        assertTrue(tank.isImmobile(), "the option is about drive and engine hits; a crewless vehicle is immobile");
    }
}
