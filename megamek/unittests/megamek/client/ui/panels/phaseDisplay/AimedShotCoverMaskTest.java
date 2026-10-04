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
package megamek.client.ui.panels.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the partial-cover part of the aimed-shot location mask.
 * <p>
 * Partial cover is a Mek rule: only a standing Mek receives it (TW p.102, p.171), and what it hides are the Mek's
 * legs, arms and torsos (TW p.102; TO:AR p.85 for expanded cover). The mask used the Mek location indexes for every
 * target type, so a tank or a battle armor squad handed cover by the to-hit code indexed past the end of its own,
 * shorter, location list.
 */
@DisplayName("AimedShotHandler partial-cover mask")
class AimedShotCoverMaskTest {

    private Game game;
    private FiringDisplay firingDisplay;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        firingDisplay = mock(FiringDisplay.class);
    }

    /** An attacker with aimed-shot capability from TCP + VDNI (IO p.81), so no targeting computer has to be mounted. */
    private BipedMek createAimingAttacker() {
        BipedMek mek = createMek(1, "Test Attacker", new Coords(0, 0), 3);
        mek.getCrew().getOptions().getOption(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR).setValue(true);
        mek.getCrew().getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
        return mek;
    }

    private BipedMek createMek(int id, String chassis, Coords position, int facing) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(id);
        mek.setChassis(chassis);
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();
        mek.setPosition(position);
        mek.setFacing(facing);
        return mek;
    }

    private BattleArmor createSquadTarget() {
        BattleArmor squad = new BattleArmor();
        squad.setGame(game);
        squad.setId(2);
        squad.setChassis("Test BA");
        squad.setModel("Standard");
        squad.setSquadSize(6);
        squad.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        squad.setCrew(new Crew(CrewType.INFANTRY_CREW));
        squad.setOwner(game.getPlayer(0));
        squad.autoSetInternal();
        squad.setPosition(new Coords(0, 2));
        squad.setFacing(0);
        return squad;
    }

    private Tank createTankTarget() {
        Tank tank = new Tank();
        tank.setGame(game);
        tank.setId(2);
        tank.setChassis("Test Tank");
        tank.setModel("Standard");
        tank.setWeight(50);
        tank.setCrew(new Crew(CrewType.CREW));
        tank.setOwner(game.getPlayer(0));
        tank.autoSetInternal();
        tank.setPosition(new Coords(0, 2));
        tank.setFacing(0);
        return tank;
    }

    /** The tall tank the short-building cover code can actually report cover on (height 1, eight locations). */
    private SuperHeavyTank createSuperHeavyTankTarget() {
        SuperHeavyTank tank = new SuperHeavyTank();
        tank.setGame(game);
        tank.setId(2);
        tank.setChassis("Test Superheavy Tank");
        tank.setModel("Standard");
        tank.setWeight(150);
        tank.setCrew(new Crew(CrewType.CREW));
        tank.setOwner(game.getPlayer(0));
        tank.autoSetInternal();
        tank.setPosition(new Coords(0, 2));
        tank.setFacing(0);
        return tank;
    }

    /** A targeting-computer attacker aiming at the target, with the given partial cover reported on it. */
    private AimedShotHandler handlerAimingThroughCover(Entity target, int partialCover) {
        when(firingDisplay.currentEntity()).thenReturn(createAimingAttacker());
        when(firingDisplay.getTarget()).thenReturn(target);
        AimedShotHandler handler = new AimedShotHandler(firingDisplay);
        handler.setAimingMode();
        assertTrue(handler.getAimingMode().isTargetingComputer(), "the attacker should be aiming with a TC");
        handler.setPartialCover(partialCover);
        return handler;
    }

    @Test
    @DisplayName("Cover reported on a battle armor squad does not run past its locations")
    void coverOnSquadStaysInsideItsLocations() {
        BattleArmor squad = createSquadTarget();
        AimedShotHandler handler = handlerAimingThroughCover(squad, LosEffects.COVER_HORIZONTAL);
        int locationCount = squad.getLocationNames().length;

        boolean[] mask = assertDoesNotThrow(() -> handler.createEnabledMask(locationCount));

        for (int trooper = BattleArmor.LOC_TROOPER_1; trooper < locationCount; trooper++) {
            assertTrue(mask[trooper], "cover hides Mek locations, never trooper " + trooper);
        }
    }

    @Test
    @DisplayName("Cover reported on a tank hides none of its locations")
    void coverOnTankHidesNothing() {
        Tank tank = createTankTarget();
        AimedShotHandler handler = handlerAimingThroughCover(tank, LosEffects.COVER_HORIZONTAL);

        boolean[] mask = assertDoesNotThrow(() -> handler.createEnabledMask(tank.getLocationNames().length));

        assertTrue(mask[Tank.LOC_FRONT], "a tank never has partial cover (TW p.102); the front stays selectable");
        assertTrue(mask[Tank.LOC_TURRET], "a tank never has partial cover (TW p.102); the turret stays selectable");
    }

    @Test
    @DisplayName("Cover reported on a superheavy tank leaves its turret selectable")
    void coverOnSuperHeavyTankKeepsTheTurret() {
        SuperHeavyTank tank = createSuperHeavyTankTarget();
        AimedShotHandler handler = handlerAimingThroughCover(tank, LosEffects.COVER_HORIZONTAL);

        boolean[] mask = handler.createEnabledMask(tank.getLocationNames().length);

        // index 7 is a Mek's left leg but a superheavy tank's turret; cover must not hide it
        assertTrue(mask[SuperHeavyTank.LOC_TURRET], "a superheavy tank never has partial cover (TW p.102)");
        assertTrue(mask[SuperHeavyTank.LOC_FRONT], "the front stays selectable");
    }

    @Test
    @DisplayName("Horizontal cover on a Mek still hides both legs")
    void horizontalCoverOnMekHidesLegs() {
        BipedMek target = createMek(2, "Test Target", new Coords(0, 2), 0);
        AimedShotHandler handler = handlerAimingThroughCover(target, LosEffects.COVER_HORIZONTAL);

        boolean[] mask = handler.createEnabledMask(target.getLocationNames().length);

        assertFalse(mask[Mek.LOC_LEFT_LEG], "horizontal cover hides the left leg (TW p.102)");
        assertFalse(mask[Mek.LOC_RIGHT_LEG], "horizontal cover hides the right leg (TW p.102)");
        assertTrue(mask[Mek.LOC_CENTER_TORSO], "the center torso stays selectable");
    }
}
