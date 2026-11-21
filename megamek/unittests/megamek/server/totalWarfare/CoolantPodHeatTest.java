/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CoolantPodHeatTest {
    /** Dice that roll the same face on every d6. */
    private static final class FixedDice extends MMRandom {
        private final int face;

        private FixedDice(int face) {
            this.face = face;
        }

        @Override
        public int randomInt(int maxValue) {
            return face - 1;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @Test
    void fighterCoolantPodAddsOnePointPerActiveSink() throws Exception {
        TWGameManager manager = new TWGameManager();
        Aero fighter = deployFighter(manager);
        fighter.setActiveSinksNextRound(4);
        fighter.newRound(1);
        Mounted<?> pod = fighter.addEquipment(EquipmentType.get(EquipmentTypeLookup.COOLANT_POD), Aero.LOC_NOSE);
        pod.setModeImmediately("dump");
        fighter.heat = 15;

        new HeatResolver(manager).resolveAeroHeat(fighter, new Vector<>(), new Vector<>(), 0, 0);

        assertEquals(0, pod.getBaseShotsLeft());
        assertEquals(3, fighter.heat); // four doubles sink eight normally, plus four from the pod
    }

    @Test
    void anEfficientCoolantPodIsKeptWhenNoActiveSinkCouldUseIt() throws Exception {
        TWGameManager manager = new TWGameManager();
        Aero fighter = deployFighter(manager);
        fighter.setActiveSinksNextRound(0);
        fighter.newRound(1);
        Mounted<?> pod = fighter.addEquipment(EquipmentType.get(EquipmentTypeLookup.COOLANT_POD), Aero.LOC_NOSE);
        pod.setModeImmediately("efficient");
        fighter.heat = 1;

        new HeatResolver(manager).resolveAeroHeat(fighter, new Vector<>(), new Vector<>(), 0, 0);

        assertEquals(1, pod.getBaseShotsLeft());
    }

    /**
     * Core pp. 103, 208: a coolant pod used in the turn the radical heat sink system is engaged is triggered but has no
     * effect. IO:AE p. 83 lets no unit mount both.
     */
    @Test
    void aCoolantPodHasNoEffectWithAnEngagedRadicalHeatSink() throws Exception {
        TWGameManager manager = new TWGameManager();
        Aero fighter = deployFighter(manager);
        fighter.setActiveSinksNextRound(4);
        fighter.newRound(1);
        fighter.addEquipment(EquipmentType.get("Radical Heat Sink System"), Aero.LOC_NOSE).setModeImmediately("On");
        Mounted<?> pod = fighter.addEquipment(EquipmentType.get(EquipmentTypeLookup.COOLANT_POD), Aero.LOC_NOSE);
        pod.setModeImmediately("dump");
        fighter.heat = 15;

        // The radical system's own bonus is left out here to show the pod alone
        new HeatResolver(manager).resolveAeroHeat(fighter, new Vector<>(), new Vector<>(), 0, 0);

        assertEquals(0, pod.getBaseShotsLeft());
        assertEquals(7, fighter.heat);
    }

    /**
     * A coolant pod "increases the cooling capacity of all of the Mek's active heat sinks by 1 point each
     * (regardless of heat sink type)" (Core p. 208, TO:AUE p. 116): 1 per double heat sink, not 2
     */
    @Test
    void aMekCoolantPodAddsOnePointPerActiveHeatSinkOfAnyType() throws Exception {
        TWGameManager manager = new TWGameManager();
        Mek mek = deployMek(manager);
        Mounted<?> pod = mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.COOLANT_POD), Mek.LOC_RIGHT_TORSO);
        pod.setModeImmediately("dump");

        new HeatResolver(manager).resolveHeat();

        assertEquals(0, pod.getBaseShotsLeft());
        assertEquals(12 - 6 - 3, mek.heat); // three doubles sink six, plus three from the pod
        assertTrue(reported(manager, 5260));
    }

    /** Core p. 208: with the radical heat sink system engaged, the pod "is triggered but has no effect" */
    @Test
    void aMekCoolantPodHasNoEffectWithAnEngagedRadicalHeatSink() throws Exception {
        TWGameManager manager = new TWGameManager();
        Mek mek = deployMek(manager);
        mek.addEquipment(EquipmentType.get("Radical Heat Sink System"), Mek.LOC_RIGHT_TORSO).setModeImmediately("On");
        Mounted<?> pod = mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.COOLANT_POD), Mek.LOC_RIGHT_TORSO);
        pod.setModeImmediately("dump");
        // The activation roll is a 12, so the radical system adds its 3
        Compute.setRNG(new FixedDice(6));

        new HeatResolver(manager).resolveHeat();

        assertEquals(0, pod.getBaseShotsLeft(), "The pod is spent");
        assertEquals(12 - 6 - 3, mek.heat, "The sinks and the radical system cool; the pod adds nothing");
        assertTrue(reported(manager, 5261));
        assertTrue(reported(manager, 5540), "The successful activation reports its cooling");
    }

    /** Core p. 208: "On a failed activation, the Mek's cooling capacity is not increased this turn" */
    @Test
    void aFailedRadicalHeatSinkActivationCoolsNothingExtra() throws Exception {
        TWGameManager manager = new TWGameManager();
        Mek mek = deployMek(manager);
        Mounted<?> radicalHeatSink = mek.addEquipment(EquipmentType.get("Radical Heat Sink System"),
              Mek.LOC_RIGHT_TORSO);
        radicalHeatSink.setModeImmediately("On");
        // The activation roll is a 2, below the first use's target of 3
        Compute.setRNG(new FixedDice(1));

        new HeatResolver(manager).resolveHeat();

        assertEquals(12 - 6, mek.heat, "Only the heat sinks cool");
        assertTrue(radicalHeatSink.isDestroyed(), "The failed system takes a critical hit");
        assertFalse(reported(manager, 5540), "A failed activation reports no cooling");
    }

    private static boolean reported(TWGameManager manager, int messageId) {
        return manager.getMainPhaseReport().stream().anyMatch(report -> report.messageId == messageId);
    }

    /** @return A Mek at 12 heat with three active double heat sinks, which dissipate 6 */
    private static Mek deployMek(TWGameManager manager) throws Exception {
        EquipmentType.initializeTypes();
        Game game = manager.getGame();
        game.setBoard(Board.createEmptyBoard(1, 1));
        Player owner = new Player(0, "Player");
        game.addPlayer(0, owner);
        Mek mek = new BipedMek();
        mek.setOwner(owner);
        mek.setPosition(new Coords(0, 0));
        mek.setWeight(50);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        mek.autoSetInternal();
        game.addEntity(mek);
        for (int sink = 0; sink < 3; sink++) {
            mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.IS_DOUBLE_HS), Mek.LOC_LEFT_TORSO);
        }
        mek.heat = 12;
        return mek;
    }

    private static Aero deployFighter(TWGameManager manager) {
        EquipmentType.initializeTypes();
        Game game = manager.getGame();
        game.setBoard(Board.createEmptyBoard(1, 1));
        Player owner = new Player(0, "Player");
        game.addPlayer(0, owner);
        Aero fighter = new AeroSpaceFighter();
        fighter.setOwner(owner);
        fighter.setPosition(new Coords(0, 0));
        game.addEntity(fighter);
        fighter.setHeatSinks(10);
        fighter.setHeatType(Aero.HEAT_DOUBLE);
        return fighter;
    }
}
