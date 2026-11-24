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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Vector;

import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Every handler of a laser that can take an insulator makes the insulator check, so the check itself must leave a laser
 * without a working insulator alone (TO:AUE p.134).
 */
class LaserInsulatorCheckTest {
    private TWGameManager gameManager;

    /** Dice that roll the given d6 results in turn. */
    private static final class SequenceDice extends MMRandom {
        private final int[] results;
        private int next;

        private SequenceDice(int... results) {
            this.results = results;
        }

        @Override
        public int randomInt(int maxValue) {
            return results[next++ % results.length] - 1;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        // every d6 is a 4, so the insulator check rolls 8
        Compute.setRNG(new SequenceDice(4));
        gameManager = mock(TWGameManager.class);
        when(gameManager.applyCriticalHit(any(), anyInt(), any(), anyBoolean(), anyInt(), anyBoolean()))
              .thenReturn(new Vector<>());
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    private static Mounted<?> laserWith(String laser, String... enhancements) throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        Mounted<?> weapon = mek.addEquipment(EquipmentType.get(laser), Mek.LOC_RIGHT_TORSO);
        for (String enhancement : enhancements) {
            mek.addEquipment(EquipmentType.get(enhancement), Mek.LOC_RIGHT_TORSO);
        }
        MekFileParser.postLoadInit(mek);
        return weapon;
    }

    /** The canon Pandion (3140 Upgrade) insulates medium re-engineered lasers, which have a handler of their own */
    @Test
    void aReengineeredLaserHandlerMakesTheInsulatorCheck() throws Exception {
        Mounted<?> laser = laserWith("Medium Re-engineered Laser", "ISLaserInsulator");
        Mek attacker = (Mek) laser.getEntity();
        Mek target = new BipedMek();
        Game game = new Game();
        game.setBoard(Board.createEmptyBoard(1, 3));
        Player owner = new Player(0, "Player");
        game.addPlayer(0, owner);
        for (Mek mek : List.of(attacker, target)) {
            mek.setOwner(owner);
            game.addEntity(mek);
        }
        attacker.setPosition(new Coords(0, 0));
        target.setPosition(new Coords(0, 2));
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(laser));
        // The attack roll is a 2, the insulator check an 8
        Compute.setRNG(new SequenceDice(1, 1, 4, 4));

        AttackHandler handler = ((Weapon) laser.getType()).getCorrectHandler(new ToHitData(), attack, game,
              gameManager);

        assertInstanceOf(ReengineeredLaserWeaponHandler.class, handler);
        ((WeaponHandler) handler).doChecks(new Vector<>());
        verifyCriticalHitOn(laser);
    }

    /** The failure is a critical hit on a slot of the laser, which the manager resolves like any other */
    @Test
    void anInsulatedPulseLaserFailsOnARollOfTwo() throws Exception {
        Mounted<?> laser = laserWith("ISMediumPulseLaser", "ISLaserInsulator");
        EnergyWeaponHandler.checkInsulator(gameManager, 2, laser, 0, new Vector<Report>());
        verifyCriticalHitOn(laser);
    }

    @Test
    void aLaserWithoutAWorkingInsulatorIsLeftAlone() throws Exception {
        Mounted<?> bare = laserWith("ISMediumPulseLaser");
        EnergyWeaponHandler.checkInsulator(gameManager, 2, bare, 0, new Vector<Report>());

        Mounted<?> broken = laserWith("ISMediumPulseLaser", "ISLaserInsulator");
        broken.getEnhancement(MiscType.F_LASER_INSULATOR).setDestroyed(true);
        EnergyWeaponHandler.checkInsulator(gameManager, 2, broken, 0, new Vector<Report>());

        verifyNoCriticalHit();
        assertFalse(bare.isHit() || broken.isHit());
    }

    @Test
    void onlyARollOfTwoChecksTheInsulator() throws Exception {
        Mounted<?> laser = laserWith("ISMediumPulseLaser", "ISLaserInsulator");
        EnergyWeaponHandler.checkInsulator(gameManager, 3, laser, 0, new Vector<Report>());
        verifyNoCriticalHit();
        assertFalse(laser.isHit());
    }

    private void verifyCriticalHitOn(Mounted<?> laser) {
        verify(gameManager).applyCriticalHit(eq(laser.getEntity()), eq(laser.getLocation()),
              argThat(slot -> slot.getMount() == laser), eq(true), eq(0), eq(false));
    }

    private void verifyNoCriticalHit() {
        verify(gameManager, never()).applyCriticalHit(any(), anyInt(), any(), anyBoolean(), anyInt(), anyBoolean());
    }
}
