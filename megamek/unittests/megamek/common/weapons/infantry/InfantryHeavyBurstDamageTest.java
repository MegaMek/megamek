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

package megamek.common.weapons.infantry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.Hex;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentFlag;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers which platoon the Heavy Burst damage bonus is read off.
 *
 * <p>TM p. 152 gives the Heavy Burst Weapon feature to a platoon whose own primary weapon is over the damage cap,
 * and that feature adds 1D6 damage against conventional infantry. The condition used to test the cap on the
 * TARGET, so a platoon's bonus depended on what the platoon it was shooting at happened to be carrying.</p>
 *
 * <p>Both cases below are invisible with the weapons MegaMek currently ships, because every weapon above the cap
 * also carries {@code F_INF_BURST} and satisfies the first clause before the cap is ever consulted. They are
 * written against the condition itself so the swap cannot come back.</p>
 */
class InfantryHeavyBurstDamageTest {

    private static final int HEAVY_BURST_REPORT_ID = 3422;

    private ConvInfantry mockAttacker;
    private ConvInfantry mockTarget;
    private InfantryWeaponHandler handler;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        mockAttacker = mock(ConvInfantry.class);
        mockTarget = mock(ConvInfantry.class);
        WeaponType mockWeaponType = mock(WeaponType.class);

        Game mockGame = mock(Game.class);
        TWGameManager mockGameManager = mock(TWGameManager.class);
        ToHitData mockToHit = mock(ToHitData.class);
        WeaponAttackAction mockAction = mock(WeaponAttackAction.class);
        WeaponMounted mockWeapon = mock(WeaponMounted.class);

        GameOptions mockOptions = mock(GameOptions.class);
        doReturn(false).when(mockOptions).booleanOption(any(String.class));
        doReturn(mockOptions).when(mockGame).getOptions();

        Board mockBoard = mock(Board.class);
        Hex mockHex = mock(Hex.class);
        doReturn(mockBoard).when(mockGame).getBoard();
        doReturn(mockHex).when(mockGame).getHexOf(any());
        doReturn(false).when(mockHex).containsTerrain(anyInt());

        doReturn(1).when(mockAttacker).getId();
        doReturn(mockAttacker).when(mockGame).getEntity(1);
        // The handler constructor resolves the firing platoon through the weapon carrier, not the action.
        doReturn(mockAttacker).when(mockAttacker).getAttackingEntity();
        // Must be stubbed explicitly: an unstubbed mock answers false, which would let a wrong condition
        // short-circuit before it reaches the cap check and pass this test for the wrong reason.
        doReturn(true).when(mockAttacker).isConventionalInfantry();
        doReturn(new Coords(0, 0)).when(mockAttacker).getPosition();
        doReturn(-1).when(mockAttacker).getSwarmTargetId();
        doReturn(10).when(mockAttacker).getShootingStrength();
        // One point of damage per trooper keeps the base damage well clear of zero, so the only thing that can
        // put a Heavy Burst line in the report is the bonus itself.
        doReturn(1.0).when(mockAttacker).getDamagePerTrooper();
        doReturn(null).when(mockAttacker).getMount();

        doReturn(2).when(mockTarget).getId();
        doReturn(new Coords(1, 0)).when(mockTarget).getPosition();
        doReturn(Targetable.TYPE_ENTITY).when(mockTarget).getTargetType();
        doReturn(mockTarget).when(mockGame).getTarget(Targetable.TYPE_ENTITY, 2);
        doReturn(true).when(mockTarget).isConventionalInfantry();
        doReturn(false).when(mockTarget).isMechanized();

        // No F_INF_BURST anywhere: the flag would satisfy the condition before the cap is consulted, which is
        // exactly what hides this bug with the weapons that ship today.
        doReturn(false).when(mockWeaponType).hasFlag(any(EquipmentFlag.class));
        doReturn(mockWeaponType).when(mockWeapon).getType();

        doReturn(1).when(mockAction).getEntityId();
        doReturn(mockAttacker).when(mockAction).getEntity(mockGame);
        doReturn(0).when(mockAction).getWeaponId();
        doReturn(mockWeapon).when(mockAttacker).getWeapon(0);
        doReturn(mockWeapon).when(mockAttacker).getEquipment(0);
        doReturn(Targetable.TYPE_ENTITY).when(mockAction).getTargetType();
        doReturn(2).when(mockAction).getTargetId();

        handler = new InfantryWeaponHandler(mockToHit, mockAction, mockGame, mockGameManager);
        setField(handler, "attackingEntity", mockAttacker);
        setField(handler, "target", mockTarget);
        setField(handler, "weapon", mockWeapon);
        setField(handler, "weaponType", mockWeaponType);
        setField(handler, "nRange", 1);
    }

    @Test
    @DisplayName("The attacking platoon's over-cap primary weapon earns the bonus")
    void attackerOverTheCapEarnsTheBonus() {
        doReturn(true).when(mockAttacker).primaryWeaponDamageCapped();
        doReturn(false).when(mockTarget).primaryWeaponDamageCapped();

        assertTrue(reportsHeavyBurst(runAttack()),
              "The attacker's primary weapon is over the cap, so TM p. 152 grants it Heavy Burst and its 1D6");
    }

    @Test
    @DisplayName("The target's over-cap primary weapon earns the attacker nothing")
    void targetOverTheCapEarnsNothing() {
        doReturn(false).when(mockAttacker).primaryWeaponDamageCapped();
        doReturn(true).when(mockTarget).primaryWeaponDamageCapped();

        assertFalse(reportsHeavyBurst(runAttack()),
              "Heavy Burst belongs to the platoon that owns the weapon; what the target carries is irrelevant");
    }

    private Vector<Report> runAttack() {
        Vector<Report> reports = new Vector<>();
        handler.calcHits(reports);
        return reports;
    }

    private boolean reportsHeavyBurst(Vector<Report> reports) {
        for (Report report : reports) {
            if (report.messageId == HEAVY_BURST_REPORT_ID) {
                return true;
            }
        }
        return false;
    }

    private static void setField(Object owner, String name, Object value) throws Exception {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(owner, value);
                return;
            } catch (NoSuchFieldException notHere) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
