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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
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
 * A charged PPC capacitor explodes with its PPC when either takes a critical hit, unless the PPC fired this turn and
 * released the charge, and the explosion destroys both (TO:AUE p.149, Core p.188).
 */
class PPCCapacitorExplosionTest {
    private final RulesManager originalRules = Game.rulesManager;
    private TWGameManager manager;
    private Mek mek;
    private Mounted<?> ppc;
    private Mounted<?> capacitor;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        Game game = manager.getGame();
        Game.rulesManager = new TWRulesManager();
        game.setBoard(Board.createEmptyBoard(1, 1));
        Player player = new Player(0, "Player");
        game.addPlayer(0, player);
        mek = new BipedMek();
        mek.setOwner(player);
        mek.setPosition(new Coords(0, 0));
        // A side torso of 21 internal structure keeps the explosion inside it
        mek.setWeight(100);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        mek.autoSetInternal();
        mek.setAutoEject(false);
        game.addEntity(mek);
        ppc = mek.addEquipment(EquipmentType.get("ISHeavyPPC"), Mek.LOC_RIGHT_TORSO);
        capacitor = mek.addEquipment(EquipmentType.get("ISPPCCapacitor"), Mek.LOC_RIGHT_TORSO);
        capacitor.setLinked(ppc);
        capacitor.setModeImmediately(Mounted.MODE_CAPACITOR_CHARGE);
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = originalRules;
    }

    @Test
    void aHitOnEitherExplodesForTheirCombinedMaximumDamage() {
        assertTrue(ppc.getType().isExplosive(ppc));
        assertTrue(capacitor.getType().isExplosive(capacitor));
        // The heavy PPC's 15 and the capacitor's 5 (TO:AUE p.149)
        assertEquals(20, ppc.getExplosionDamage());
        assertEquals(20, capacitor.getExplosionDamage());
    }

    @Test
    void underCoreTheyExplodeForTwiceTheirCombinedSlots() {
        Game.rulesManager = new CoreRulesManager();
        // The heavy PPC's 4 slots and the capacitor's 1 (Core p.188)
        assertEquals(10, ppc.getExplosionDamage());
        assertEquals(10, capacitor.getExplosionDamage());
    }

    @Test
    void firingThePpcThisTurnReleasesTheCharge() {
        ppc.setUsedThisRound(true);

        assertFalse(ppc.getType().isExplosive(ppc));
        assertFalse(capacitor.getType().isExplosive(capacitor));
        assertEquals(0, capacitor.getExplosionDamage());
    }

    @Test
    void theExplosionDestroysAllSlotsOfThePpcAndItsCapacitor() {
        manager.explodeEquipment(mek, Mek.LOC_RIGHT_TORSO, capacitor);

        assertTrue(ppc.isHit());
        assertTrue(capacitor.isHit());
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(Mek.LOC_RIGHT_TORSO); slot++) {
            CriticalSlot criticalSlot = mek.getCritical(Mek.LOC_RIGHT_TORSO, slot);
            if ((criticalSlot != null) && ((criticalSlot.getMount() == ppc) || (criticalSlot.getMount() == capacitor))) {
                assertTrue(criticalSlot.isHit(), "slot " + slot);
            }
        }
    }
}
