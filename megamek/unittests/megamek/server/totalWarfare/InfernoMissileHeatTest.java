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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InfernoMissileHeatTest {
    private RulesManager originalRules;
    private TWGameManager manager;
    private Mek attacker;
    private Mek target;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void deploy() {
        originalRules = Game.rulesManager;
        manager = new TWGameManager();
        Game game = manager.getGame();
        Board board = Board.createEmptyBoard(1, 3);
        board.setHex(0, 1, new Hex(1));
        game.setBoard(board);
        Player owner = new Player(0, "Player");
        game.addPlayer(0, owner);
        attacker = new BipedMek();
        attacker.setOwner(owner);
        attacker.setPosition(new Coords(0, 0));
        game.addEntity(attacker);
        target = spy(new BipedMek());
        target.setOwner(owner);
        target.setPosition(new Coords(0, 2));
        target.setArmorType(EquipmentType.T_ARMOR_HEAT_DISSIPATING, Mek.LOC_RIGHT_TORSO);
        target.initializeRearArmor(1, Mek.LOC_RIGHT_TORSO);
        game.addEntity(target);
        assertEquals(LosEffects.COVER_HORIZONTAL, LosEffects.calculateLOS(game, attacker, target).getTargetCover());
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = originalRules;
    }

    /** Rolls a leg (behind the cover), the armored rear right torso, then the bare front right torso. */
    private void rollLegThenRearThenFront() {
        doReturn(new HitData(Mek.LOC_LEFT_LEG), new HitData(Mek.LOC_RIGHT_TORSO, true),
              new HitData(Mek.LOC_RIGHT_TORSO)).when(target).rollHitLocation(anyInt(), anyInt());
    }

    @Test
    void coreRollsEachMissileForCoverAndNegatesHeatWhereArmorIsLeft() {
        // Xotl's ruling (Core forum, 19 Oct 2026): roll locations whenever cover or stripped armor can matter
        Game.rulesManager = new CoreRulesManager();
        rollLegThenRearThenFront();

        manager.deliverInfernoMissiles(attacker, target, 3);

        // The leg missile strikes the cover, the rear missile meets heat-dissipating armor, the front one bare structure
        assertEquals(2, target.heatFromExternal);
        verify(target, times(3)).rollHitLocation(anyInt(), anyInt());
    }

    @Test
    void fullDetailInfantryKeepsItsInfernoEffectUnderCore() {
        Game.rulesManager = new CoreRulesManager();
        ConvInfantry platoon = new ConvInfantry();
        platoon.setOwner(attacker.getOwner());
        platoon.setPosition(target.getPosition());
        platoon.setSquadSize(7);
        platoon.setSquadCount(4);
        platoon.autoSetInternal();
        manager.getGame().addEntity(platoon);

        manager.deliverInfernoMissiles(attacker, platoon, 2);
        assertEquals(22, platoon.getInternal(ConvInfantry.LOC_INFANTRY));
        assertEquals(0, platoon.heatFromExternal);
    }

    @Test
    void totalWarfareChecksEachMissileForCoverAndArmorOnTheStruckFacing() {
        Game.rulesManager = new TWRulesManager();
        rollLegThenRearThenFront();

        manager.deliverInfernoMissiles(attacker, target, 3);

        assertEquals(1 + 2, target.heatFromExternal);
        verify(target, times(3)).rollHitLocation(anyInt(), anyInt());

        // The armor counts as it was when each missile struck (Core p.201), so stripping it later changes nothing
        target.initializeRearArmor(0, Mek.LOC_RIGHT_TORSO);
        assertEquals(3, target.heatFromExternal);
    }

    @Test
    void totalWarfareAreaEffectHitUsesTheLocationlessArmorRule() {
        Game.rulesManager = new TWRulesManager();
        for (int location = 0; location < target.locations(); location++) {
            target.setArmorType(EquipmentType.T_ARMOR_HEAT_DISSIPATING, location);
            target.initializeArmor(1, location);
        }

        manager.deliverInfernoMissiles(attacker, target, 5, true);
        assertEquals(5, target.heatFromExternal);
        verify(target, never()).rollHitLocation(anyInt(), anyInt());

        // Heat without a location needs the armor in every location
        target.setArmor(0, Mek.LOC_LEFT_ARM);
        manager.deliverInfernoMissiles(attacker, target, 5, true);
        assertEquals(5 + 10, target.heatFromExternal);
    }
}
