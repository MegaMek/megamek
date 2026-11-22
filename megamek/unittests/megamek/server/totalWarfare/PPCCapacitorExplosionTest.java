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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.PPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A charged PPC capacitor explodes with its PPC when either takes a critical hit, unless the PPC fired this turn and
 * released the charge, and the explosion destroys both (TO:AUE p.149, Core p.188). A to-hit roll of 2 burns the
 * charged system out instead (TO:AUE p.149).
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
        mek.setCrew(spy(mek.getCrew()));
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

    /**
     * A PPC fired in this Attack Phase "is considered to have discharged before the critical hit, and the critical hit
     * simply destroys the affected slot with no explosive effects" (TO:AUE p.149)
     */
    @Test
    void aCriticalHitAfterThePpcFiresDoesNotExplodeTheCapacitor() {
        Game game = manager.getGame();
        game.setBoard(Board.createEmptyBoard(1, 3));
        Mek target = new BipedMek();
        target.setOwner(mek.getOwner());
        target.setPosition(new Coords(0, 2));
        game.addEntity(target);
        WeaponAttackAction attack = new WeaponAttackAction(mek.getId(), target.getId(), mek.getEquipmentNum(ppc));

        assertInstanceOf(PPCHandler.class,
              ((Weapon) ppc.getType()).getCorrectHandler(new ToHitData(), attack, game, manager));
        assertTrue(ppc.isUsedThisRound(), "Firing marks the PPC used this round");
        assertTrue(capacitor.curMode().equals(Mounted.MODE_CAPACITOR_CHARGE),
              "The capacitor's switch off takes effect next turn");
        int internalBefore = mek.getInternal(Mek.LOC_RIGHT_TORSO);

        manager.applyCriticalHit(mek, Mek.LOC_RIGHT_TORSO, slotsOf(mek, capacitor).get(0), true, 0, false);

        assertTrue(capacitor.isHit());
        assertFalse(ppc.isHit(), "Nothing exploded to destroy the PPC");
        assertEquals(internalBefore, mek.getInternal(Mek.LOC_RIGHT_TORSO));
    }

    /**
     * A to-hit roll of 2 "burns out the system before it can fire, damaging the capacitor slot as well as the first
     * critical slot of the corresponding weapon" (TO:AUE p.149)
     */
    @Test
    void aBurnoutDamagesTheCapacitorSlotAndThePpcsFirstSlot() {
        Game.rulesManager.getRulesWeapons().checkPPCCapacitor(2, mek, (WeaponMounted) ppc);

        assertTrue(ppc.isHit());
        assertTrue(capacitor.isHit());
        assertEquals(List.of(true, false, false, false), damagedSlots(mek, ppc));
        assertEquals(List.of(true), damagedSlots(mek, capacitor));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void burnoutChecksTheArmorOfEachSlotIndependently(boolean armorPpc) {
        CriticalSlot ppcSlot = slotsOf(mek, ppc).getFirst();
        CriticalSlot capacitorSlot = slotsOf(mek, capacitor).getFirst();
        ppcSlot.setArmored(armorPpc);
        capacitorSlot.setArmored(!armorPpc);

        Game.rulesManager.getRulesWeapons().checkPPCCapacitor(2, mek, (WeaponMounted) ppc);

        assertFalse(ppcSlot.isArmored());
        assertFalse(capacitorSlot.isArmored());
        assertEquals(!armorPpc, ppcSlot.isDestroyed());
        assertEquals(!armorPpc, ppc.isHit());
        assertEquals(armorPpc, capacitorSlot.isDestroyed());
        assertEquals(armorPpc, capacitor.isHit());
    }

    @Test
    void aBurnedOutPpcDoesNotDamageAnAdjacentTargetsBuilding() throws EntityLoadingException {
        Mek target = targetInBuilding();
        IBuilding building = manager.getGame().getBoard(target).getBuildingAt(target.getPosition());
        int originalCF = building.getCurrentCF(target.getPosition());

        firePpcOnTwo(target);

        assertTrue(capacitor.isHit());
        assertEquals(originalCF, building.getCurrentCF(target.getPosition()));
    }

    @Test
    void firstBurnoutStripsSlotArmorAndSecondBurnoutDestroysTheExposedSlots() throws EntityLoadingException {
        List<CriticalSlot> ppcSlots = slotsOf(mek, ppc);
        CriticalSlot capacitorSlot = slotsOf(mek, capacitor).getFirst();
        ppc.setArmored(true);
        capacitor.setArmored(true);
        ppcSlots.forEach(slot -> slot.setArmored(true));
        capacitorSlot.setArmored(true);
        Mek target = targetInBuilding();
        IBuilding building = manager.getGame().getBoard(target).getBuildingAt(target.getPosition());
        int originalCF = building.getCurrentCF(target.getPosition());

        firePpcOnTwo(target);
        mek.applyDamage();
        assertFalse(ppcSlots.getFirst().isArmored());
        assertFalse(capacitorSlot.isArmored());
        assertTrue(ppcSlots.stream().noneMatch(CriticalSlot::isDamaged));
        assertFalse(capacitorSlot.isDamaged());
        assertTrue(ppc.isOperable());
        assertTrue(capacitor.isOperable());
        assertEquals(originalCF, building.getCurrentCF(target.getPosition()));

        mek.newRound(2);
        assertEquals(0, ppc.hasChargedCapacitor());
        capacitor.setMode(Mounted.MODE_CAPACITOR_CHARGE);
        mek.newRound(3);
        assertEquals(1, ppc.hasChargedCapacitor());
        firePpcOnTwo(target);
        mek.applyDamage();
        assertTrue(ppc.isDestroyed());
        assertTrue(capacitor.isDestroyed());
        assertTrue(capacitorSlot.isDestroyed());
        assertEquals(List.of(true, false, false, false), ppcSlots.stream().map(CriticalSlot::isDestroyed).toList());
        assertTrue(ppcSlots.stream().skip(1).allMatch(CriticalSlot::isArmored));
        assertEquals(originalCF, building.getCurrentCF(target.getPosition()));
    }

    private Mek targetInBuilding() {
        Game game = manager.getGame();
        Board board = new Board();
        board.load("""
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "bldg_elev:3;building:1;bldg_cf:50" ""
              end
              """, null);
        game.setBoard(board);
        Mek target = new BipedMek();
        target.setOwner(mek.getOwner());
        target.setPosition(new Coords(0, 1));
        game.addEntity(target);
        return target;
    }

    private void firePpcOnTwo(Mek target) throws EntityLoadingException {
        Game game = manager.getGame();
        WeaponAttackAction attack = new WeaponAttackAction(mek.getId(), target.getId(), mek.getEquipmentNum(ppc));
        Roll two = mock(Roll.class);
        when(two.getIntValue()).thenReturn(2);
        when(two.getReport()).thenReturn("2");
        doReturn(two).when(mek.getCrew()).rollGunnerySkill(game, attack);

        PPCHandler handler = new PPCHandler(new ToHitData(2, "test"), attack, game, manager);
        handler.handle(GamePhase.FIRING, new Vector<>());
    }

    /** @return The critical slots in its location that hold the equipment */
    private static List<CriticalSlot> slotsOf(Mek unit, Mounted<?> mounted) {
        List<CriticalSlot> slots = new ArrayList<>();
        for (int slot = 0; slot < unit.getNumberOfCriticalSlots(mounted.getLocation()); slot++) {
            CriticalSlot criticalSlot = unit.getCritical(mounted.getLocation(), slot);
            if ((criticalSlot != null) && (criticalSlot.getMount() == mounted)) {
                slots.add(criticalSlot);
            }
        }
        return slots;
    }

    /** @return Whether each slot of the equipment, in slot order, is damaged */
    private static List<Boolean> damagedSlots(Mek unit, Mounted<?> mounted) {
        return slotsOf(unit, mounted).stream().map(CriticalSlot::isDamaged).toList();
    }
}
