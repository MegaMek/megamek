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

import java.util.ArrayList;
import java.util.List;

import megamek.common.Hex;
import megamek.common.MPCalculationSetting;
import megamek.common.TechConstants;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MekJumpMPTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void frankenMekJumpJetsClampLocationTonnageToCenterTorsoClass() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);

        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_LEG, 100);

        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_LEG);

        assertEquals(2, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    @Test
    void frankenMekJumpJetsDropFractionalMovementWhenJetIsDestroyed() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);

        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        mek.setFrankenMekStructureTonnage(Mek.LOC_LEFT_TORSO, 25);

        assertEquals(0.5, jumpJet.getTonnage(mek, Mek.LOC_RIGHT_TORSO, 1.0));
        assertEquals(1.0, jumpJet.getTonnage(mek, Mek.LOC_CENTER_TORSO, 1.0));

        Mounted<?> damagedJumpJet = mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        for (int jet = 0; jet < 2; jet++) {
            mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        }
        for (int jet = 0; jet < 3; jet++) {
            mek.addEquipment(jumpJet, Mek.LOC_LEFT_TORSO);
        }

        assertEquals(3, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));

        damagedJumpJet.setDestroyed(true);

        assertEquals(2, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    @Test
    void frankenMekJumpHeatCountsJetsFired() throws Exception {
        // CO p. 213: a 60-ton FrankenMek with six half-ton jets has Jumping MP 3 but generates 6 heat
        Mek mek = createFrankenMek(60, 240);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        for (int location : new int[] { Mek.LOC_RIGHT_TORSO, Mek.LOC_LEFT_TORSO }) {
            mek.setFrankenMekStructureTonnage(location, 25);
            for (int jet = 0; jet < 3; jet++) {
                mek.addEquipment(jumpJet, location);
            }
        }

        assertEquals(3, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));
        assertEquals(6, mek.getJumpHeat(3));
        assertEquals(3, mek.getJumpHeat(1));

        // A full-weight jet covers a whole MP on its own
        mek.addEquipment(jumpJet, Mek.LOC_CENTER_TORSO);
        assertEquals(5, mek.getJumpHeat(3));
    }

    @Test
    void frankenMekJumpHeatCountsTheJetsALowGravityJumpFires() throws Exception {
        // At 0.5 G six half-ton jets carry a 60-ton FrankenMek six hexes (TO:AR p. 53), one jet each (CO p. 213)
        Mek mek = createFrankenMek(60, 240);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        for (int jet = 0; jet < 6; jet++) {
            mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_RIGHT_TORSO);
        }
        Game game = new Game();
        game.getPlanetaryConditions().setGravity(0.5f);
        mek.setGame(game);

        assertEquals(6, mek.getJumpHeatForDistance(6));
        // BV passes the Jumping MP, which gravity doesn't change, also after a jump
        mek.recordJumpTakeoff();
        assertEquals(6, mek.getJumpHeat(3));

        // At 1.6 G the 3 MP carry it 1.875 hexes, rounded to 2: a two-hex jump fires all six jets, five reaching 1
        game.getPlanetaryConditions().setGravity(1.6f);
        assertEquals(6, mek.getJumpHeatForDistance(2));
    }

    @Test
    void frankenMekJumpHeatUsesOnlyJetsAboveWaterAtTakeoff() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        EquipmentType jet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        for (int i = 0; i < 6; i++) {
            mek.addEquipment(jet, Mek.LOC_RIGHT_TORSO);
        }
        mek.addEquipment(jet, Mek.LOC_RIGHT_LEG);

        Game game = new Game();
        Board board = Board.createEmptyBoard(4, 2);
        board.setHex(0, 0, new Hex(0, "water:1", null));
        board.setHex(0, 1, new Hex(0, "water:2", null));
        game.setBoard(board);
        mek.setGame(game);
        mek.setPosition(new Coords(0, 0));
        mek.setElevation(-1);
        assertEquals(3, mek.getJumpMP(MPCalculationSetting.DEDUCT_SUBMERGED_JJ));
        assertEquals(5, mek.getJumpHeat(3));

        // Taking off from depth-one water fires only the torso jets; BV keeps counting the installed ones
        mek.recordJumpTakeoff();
        mek.setPosition(new Coords(3, 0));
        mek.setElevation(0);
        assertEquals(6, mek.getJumpHeatForDistance(3));
        assertEquals(5, mek.getJumpHeat(3));

        mek.recordJumpTakeoff();
        mek.setPosition(new Coords(0, 1));
        mek.setElevation(-2);
        assertEquals(0, mek.getJumpMP(MPCalculationSetting.DEDUCT_SUBMERGED_JJ));
        assertEquals(5, mek.getJumpHeatForDistance(3));
    }

    @Test
    void frankenMekJumpHeatCountsTheJetsThatFiredAtTakeoff() throws Exception {
        // Six half-MP jets fire to jump 3 hexes (CO p. 213); three destroyed on landing don't lower the heat
        Mek mek = createFrankenMek(60, 240);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        List<Mounted<?>> jets = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            jets.add(mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_RIGHT_TORSO));
        }
        mek.recordJumpTakeoff();
        jets.subList(0, 3).forEach(jet -> jet.setDestroyed(true));

        assertEquals(6, mek.getJumpHeatForDistance(3));
    }

    @Test
    void wingDamageAfterTakeoffChangesOnlyFutureJumps() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        for (int i = 0; i < 6; i++) {
            mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_RIGHT_TORSO);
        }
        Mounted<?> wing = mek.addEquipment(EquipmentType.get("ISPartialWing"), Mek.LOC_LEFT_TORSO);

        mek.recordJumpTakeoff();
        assertEquals(4, mek.getJumpHeatForDistance(3));
        mek.getCriticalSlots(Mek.LOC_LEFT_TORSO).stream().filter(slot -> (slot != null) && (slot.getMount() == wing))
              .findFirst().orElseThrow().setDestroyed(true);

        assertEquals(4, mek.getJumpHeatForDistance(3));
        assertEquals(6, mek.getJumpHeat(3));
        mek.recordJumpTakeoff();
        assertEquals(6, mek.getJumpHeatForDistance(3));
    }

    @Test
    void frankenMekBattleValueJumpHeatIgnoresStandingWater() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        mek.setFrankenMekStructureTonnage(Mek.LOC_LEFT_TORSO, 25);
        for (int i = 0; i < 6; i++) {
            mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_LEFT_TORSO);
        }
        Game game = new Game();
        Board board = Board.createEmptyBoard(2, 1);
        board.setHex(1, 0, new Hex(0, "water:2", null));
        game.setBoard(board);
        mek.setGame(game);
        mek.setPosition(new Coords(0, 0));
        assertEquals(6, mek.getJumpHeat(mek.getJumpMP(MPCalculationSetting.BV_CALCULATION)));

        mek.setPosition(new Coords(1, 0));
        mek.setElevation(-2);
        assertEquals(6, mek.getJumpHeat(mek.getJumpMP(MPCalculationSetting.BV_CALCULATION)));
    }

    @Test
    void depthOneWaterCountsThePartialWingOnce() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(50);
        mek.addEquipment(EquipmentType.get("ISPartialWing"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_RIGHT_LEG);
        Game game = new Game();
        Board board = Board.createEmptyBoard(1, 1);
        board.setHex(0, 0, new Hex(0, "water:1", null));
        game.setBoard(board);
        mek.setGame(game);
        mek.setPosition(new Coords(0, 0));
        mek.setElevation(-1);
        assertEquals(0, mek.getJumpMP(MPCalculationSetting.DEDUCT_SUBMERGED_JJ));

        mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.JUMP_JET), Mek.LOC_RIGHT_TORSO);
        assertEquals(3, mek.getJumpMP(MPCalculationSetting.DEDUCT_SUBMERGED_JJ));
        assertEquals(3, mek.torsoJumpJets());
    }

    @Test
    void linkedFrankenMekLocationSourceClearsWhenStructureChanges() {
        Mek mek = createFrankenMek(60, 240);

        mek.linkFrankenMekLocationToSource(Mek.LOC_RIGHT_TORSO, "Archer XYZ");
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 65);

        assertEquals("", mek.getFrankenMekLocationSourceDisplayName(Mek.LOC_RIGHT_TORSO));
    }

    private Mek createFrankenMek(int weight, int engineRating) {
        Mek mek = new BipedMek();
        mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        mek.setWeight(weight);
        mek.setEngine(new Engine(engineRating, Engine.NORMAL_ENGINE, 0));
        mek.setFrankenMek(true);
        mek.setFrankenMekStructureTonnage(Mek.LOC_CENTER_TORSO, weight);
        return mek;
    }
}
