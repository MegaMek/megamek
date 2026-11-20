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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import org.junit.jupiter.api.Test;

class CoolantPodHeatTest {
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

    /** IO:AE p. 83: a unit with a radical heat sink system may not mount coolant pods. */
    @Test
    void aUnitMayNotMountACoolantPodAndARadicalHeatSink() throws Exception {
        EquipmentType.initializeTypes();
        Aero fighter = new AeroSpaceFighter();
        fighter.setWeight(50);
        fighter.addEquipment(EquipmentType.get("Radical Heat Sink System"), Aero.LOC_NOSE);
        fighter.addEquipment(EquipmentType.get(EquipmentTypeLookup.COOLANT_POD), Aero.LOC_NOSE);
        StringBuffer report = new StringBuffer();

        new TestAero(fighter, EntityVerifier.getInstance(null).aeroOption, null).hasIllegalEquipmentCombinations(report);

        assertTrue(report.toString().contains("Unit has coolant pod and radical heat sink system"), report::toString);
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
