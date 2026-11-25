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

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Heat-dissipating armor acts while the location struck has armor at the time a hit is applied (Core p.201, IO:AE p.81),
 * so it is judged as each hit strikes; the Heat Phase only adds the outside heat within the cap of 15 (TW p.159, Core
 * p.103).
 */
class HeatPhaseOutsideHeatTest {
    private RulesManager originalRules;
    private TWGameManager manager;
    private Mek mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void deploy() {
        originalRules = Game.rulesManager;
        manager = new TWGameManager();
        Game game = manager.getGame();
        game.setBoard(Board.createEmptyBoard(1, 1));
        Player owner = new Player(0, "Player");
        game.addPlayer(0, owner);
        mek = new BipedMek();
        mek.setOwner(owner);
        mek.setPosition(new Coords(0, 0));
        mek.setWeight(50);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        mek.autoSetInternal();
        mek.setArmorType(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(5, location);
        }
        game.addEntity(mek);
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = originalRules;
    }

    /** @return The heat the Heat Phase added to the Mek, which has no heat sinks and builds no heat of its own */
    private int outsideHeatInTheHeatPhase() {
        assertEquals(0, mek.getHeatCapacityWithWater());
        int heatBefore = mek.heat;
        new HeatResolver(manager).resolveHeat();
        return mek.heat - heatBefore;
    }

    @Test
    void totalWarfareHalvesEachHitBeforeTheCap() {
        Game.rulesManager = new TWRulesManager();
        for (int hit = 0; hit < 4; hit++) {
            mek.addOutsideHeat(10, new HitData(Mek.LOC_LEFT_TORSO));
        }

        // 40 heat halved to 20, then held to 15
        assertEquals(15, outsideHeatInTheHeatPhase());
    }

    @Test
    void totalWarfareArmorStrippedAfterTheHitKeepsItsHeatHalved() {
        Game.rulesManager = new TWRulesManager();
        mek.addOutsideHeat(6, new HitData(Mek.LOC_LEFT_TORSO));
        mek.setArmor(0, Mek.LOC_LEFT_TORSO);
        mek.addOutsideHeat(6, new HitData(Mek.LOC_LEFT_TORSO));

        // The first hit met the armor; the second strikes the stripped location in full
        assertEquals(3 + 6, outsideHeatInTheHeatPhase());
    }

    @Test
    void totalWarfareJudgesTheArmorFaceTheHitStruck() {
        Game.rulesManager = new TWRulesManager();
        mek.initializeRearArmor(5, Mek.LOC_LEFT_TORSO);
        mek.setArmor(0, Mek.LOC_LEFT_TORSO, true);
        mek.addOutsideHeat(6, new HitData(Mek.LOC_LEFT_TORSO, true));
        mek.addOutsideHeat(6, new HitData(Mek.LOC_LEFT_TORSO, false));

        // The stripped rear lets its hit through in full while the front keeps halving its own
        assertEquals(6 + 3, outsideHeatInTheHeatPhase());
    }

    @Test
    void coreNegatesTheHeatOfHitsOnLocationsThatKeepTheirArmor() {
        Game.rulesManager = new CoreRulesManager();
        mek.setArmor(0, Mek.LOC_RIGHT_TORSO);
        mek.addOutsideHeat(6, new HitData(Mek.LOC_LEFT_TORSO));
        mek.addOutsideHeat(6, new HitData(Mek.LOC_RIGHT_TORSO));

        assertEquals(6, outsideHeatInTheHeatPhase());
    }

    @Test
    void coreNegatesHeatWithoutALocationWhileAnyArmorIsLeft() {
        Game.rulesManager = new CoreRulesManager();
        for (int location = 1; location < mek.locations(); location++) {
            mek.setArmor(0, location);
        }
        mek.addOutsideHeat(6);
        assertEquals(0, outsideHeatInTheHeatPhase());

        mek.setArmor(0, 0);
        mek.addOutsideHeat(6);
        assertEquals(6, outsideHeatInTheHeatPhase());
    }
}
