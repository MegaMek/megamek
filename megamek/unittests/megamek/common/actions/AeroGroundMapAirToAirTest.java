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
package megamek.common.actions;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins what the rules engine actually allows for air-to-air fire between two aircraft over a ground mapsheet.
 *
 * <p>Written while chasing why bot fighters over a ground map never fired. The answer turned out to be that
 * the engine is perfectly happy - at matched altitude the shot is a 3+, and line of sight is clear even over
 * terrain - so the fault lay on the bot side. These assertions keep that baseline honest, and pin the
 * dead-zone boundaries (TW p.241) at the scale a ground mapsheet imposes.</p>
 */
class AeroGroundMapAirToAirTest {

    private static final int BOARD_WIDTH = 60;
    private static final int BOARD_HEIGHT = 60;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static Board flatGroundBoard() {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(BoardType.GROUND);
        return board;
    }

    private static AeroSpaceFighter fighter(Game game, int id, Player owner, Coords position, int altitude,
          int facing) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        WeaponMounted laser = (WeaponMounted) WeaponMounted.createMounted(fighter,
              EquipmentType.get("ISMediumLaser"));
        try {
            fighter.addEquipment(laser, Aero.LOC_NOSE, false);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        fighter.setCrew(new Crew(CrewType.SINGLE, "Test Pilot", 1, 4, 5, Gender.FEMALE, false, null));
        fighter.setId(id);
        fighter.setOwner(owner);
        fighter.setPosition(position);
        fighter.setAltitude(altitude);
        fighter.setFacing(facing);
        fighter.setSecondaryFacing(facing);
        fighter.setDeployed(true);
        game.addEntity(fighter, false);
        return fighter;
    }

    private record Shot(ToHitData toHit, boolean deadZone, boolean lineOfSight, int effectiveDistance) {}

    private static Shot shoot(int separation, int targetAltitude) {
        Game game = new Game();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(flatGroundBoard());

        Player us = new Player(1, "Us");
        us.setTeam(1);
        Player them = new Player(2, "Them");
        them.setTeam(2);
        game.addPlayer(1, us);
        game.addPlayer(2, them);

        Entity attacker = fighter(game, 1, us, new Coords(5, 5), 5, 3);
        Entity target = fighter(game, 2, them, new Coords(5, 5 + separation), targetAltitude, 0);

        WeaponMounted weapon = attacker.getWeaponList().getFirst();
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(weapon));
        return new Shot(attack.toHit(game), Compute.inDeadZone(game, attacker, target),
              LosEffects.calculateLOS(game, attacker, target).canSee(),
              Compute.effectiveDistance(game, attacker, target));
    }

    /**
     * The baseline the bot investigation rested on: two fighters at the same altitude over a ground map can
     * shoot each other, at any separation, and it is an easy shot.
     */
    @Test
    void matchedAltitudeOverAGroundMapIsALegalAndEasyShot() {
        for (int separation : new int[] { 1, 3, 10, 20, 40 }) {
            Shot shot = shoot(separation, 5);
            assertNotEquals(TargetRoll.IMPOSSIBLE, shot.toHit().getValue(),
                  "matched altitude at " + separation + " hexes should be legal: " + shot.toHit().getDesc());
            assertTrue(shot.toHit().getValue() <= 6,
                  "and should be a good shot, was " + shot.toHit().getValueAsString());
        }
    }

    /** Line of sight is not what stops air-to-air fire over a ground map. */
    @Test
    void lineOfSightIsClearBetweenAircraftOverAGroundMap() {
        assertTrue(shoot(10, 5).lineOfSight());
        assertTrue(shoot(20, 3).lineOfSight());
    }

    /**
     * TW p.241 at ground-mapsheet scale: one altitude of separation blocks fire until the horizontal range
     * clears one low-altitude hex, which the engine reaches at seventeen ground hexes rather than sixteen.
     */
    @Test
    void oneAltitudeApartClearsAtSeventeenHexes() {
        assertTrue(shoot(16, 4).deadZone(), "16 hexes one altitude apart is still the dead zone");
        assertEquals(TargetRoll.IMPOSSIBLE, shoot(16, 4).toHit().getValue());

        assertTrue(!shoot(17, 4).deadZone(), "17 hexes clears it");
        assertNotEquals(TargetRoll.IMPOSSIBLE, shoot(17, 4).toHit().getValue());
    }

    /** And two altitudes of separation needs twice that again. */
    @Test
    void twoAltitudesApartIsStillBlockedAtTwentyHexes() {
        assertTrue(shoot(20, 3).deadZone());
        assertEquals(TargetRoll.IMPOSSIBLE, shoot(20, 3).toHit().getValue());
    }
}
