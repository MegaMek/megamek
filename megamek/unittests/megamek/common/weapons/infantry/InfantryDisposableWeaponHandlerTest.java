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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the Disposable Infantry Weapon damage formula (TO:AuE p.116, Corrected Sixth Printing): total damage
 * equals three times the disposable weapon's per-trooper damage, multiplied by the number of troopers who hit on the
 * Cluster Hits Table, rounded normally.
 */
class InfantryDisposableWeaponHandlerTest {

    private ConvInfantry mockAttacker;
    private BipedMek mockMekTarget;
    private Game mockGame;
    private TWGameManager mockGameManager;
    private ToHitData mockToHit;
    private WeaponAttackAction mockAction;
    private WeaponMounted mockWeapon;
    private InfantryWeapon mockWeaponType;
    private Board mockBoard;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mockAttacker = mock(ConvInfantry.class);
        mockMekTarget = mock(BipedMek.class);
        mockGame = mock(Game.class);
        mockGameManager = mock(TWGameManager.class);
        mockToHit = mock(ToHitData.class);
        mockAction = mock(WeaponAttackAction.class);
        mockWeapon = mock(WeaponMounted.class);
        mockWeaponType = mock(InfantryWeapon.class);
        mockBoard = mock(Board.class);

        GameOptions mockOptions = mock(GameOptions.class);
        doReturn(false).when(mockOptions).booleanOption(any(String.class));
        doReturn(mockOptions).when(mockGame).getOptions();
        doReturn(mockBoard).when(mockGame).getBoard();

        doReturn(1).when(mockAttacker).getId();
        doReturn(mockAttacker).when(mockGame).getEntity(1);
        doReturn(mockAttacker).when(mockAttacker).getAttackingEntity();
        doReturn(new Coords(0, 0)).when(mockAttacker).getPosition();
        // Not swarming: forces the Cluster Hits Table (Compute.missilesHit) branch
        doReturn(-1).when(mockAttacker).getSwarmTargetId();

        doReturn(2).when(mockMekTarget).getId();
        doReturn(new Coords(1, 0)).when(mockMekTarget).getPosition();
        doReturn(Targetable.TYPE_ENTITY).when(mockMekTarget).getTargetType();
        doReturn(mockMekTarget).when(mockGame).getTarget(Targetable.TYPE_ENTITY, 2);

        doReturn(mockWeaponType).when(mockWeapon).getType();
        doReturn("InfantryDisposableTestWeapon").when(mockWeaponType).getInternalName();
        doReturn(mockWeapon).when(mockAttacker).getEquipment(0);

        doReturn(1).when(mockAction).getEntityId();
        doReturn(mockAttacker).when(mockAction).getEntity(mockGame);
        doReturn(0).when(mockAction).getWeaponId();
        when(mockAttacker.getWeapon(0)).thenReturn(mockWeapon);
        doReturn(Targetable.TYPE_ENTITY).when(mockAction).getTargetType();
        doReturn(2).when(mockAction).getTargetId();

        doReturn("").when(mockToHit).getTableDesc();
    }

    private InfantryDisposableWeaponHandler newHandler() throws Exception {
        return new InfantryDisposableWeaponHandler(mockToHit, mockAction, mockGame, mockGameManager);
    }

    private static Object getField(Object obj, String name) throws Exception {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " not found in hierarchy of " + obj.getClass().getName());
    }

    @Test
    @DisplayName("damage is 3 x weapon damage x troopers who hit, rounded (rounds up)")
    void damageIsTripledPerTrooperRoundsUp() throws Exception {
        // Rocket Launcher (LAW): 0.53 damage-each; 28 troopers hit -> round(3 * 0.53 * 28) = round(44.52) = 45
        doReturn(0.53).when(mockWeaponType).getInfantryDamage();
        doReturn(28).when(mockAttacker).getShootingStrength();
        InfantryDisposableWeaponHandler handler = newHandler();

        int damage;
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.missilesHit(anyInt(), anyInt())).thenReturn(28);
            damage = handler.calcHits(new Vector<>());
        }

        assertEquals(45, damage, "Total damage should be round(3 * 0.53 * 28) = 45");
    }

    @Test
    @DisplayName("damage rounds down at the half-way mark per normal rounding")
    void damageRoundsNormally() throws Exception {
        // Dragonsbane Disposable: 0.16 damage-each; 10 troopers -> 3 * 0.16 * 10 = 4.8 -> round = 5
        doReturn(0.16).when(mockWeaponType).getInfantryDamage();
        doReturn(10).when(mockAttacker).getShootingStrength();
        InfantryDisposableWeaponHandler handler = newHandler();

        int damage;
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.missilesHit(anyInt(), anyInt())).thenReturn(10);
            damage = handler.calcHits(new Vector<>());
        }

        assertEquals(5, damage, "Total damage should be round(3 * 0.16 * 10) = 5");
    }

    @Test
    @DisplayName("only the troopers who hit contribute, not the full platoon")
    void damageScalesWithTroopersWhoHit() throws Exception {
        doReturn(0.53).when(mockWeaponType).getInfantryDamage();
        doReturn(28).when(mockAttacker).getShootingStrength();
        InfantryDisposableWeaponHandler handler = newHandler();

        int damage;
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            // Only 7 of 28 troopers hit -> round(3 * 0.53 * 7) = round(11.13) = 11
            compute.when(() -> Compute.missilesHit(anyInt(), anyInt())).thenReturn(7);
            damage = handler.calcHits(new Vector<>());
        }

        assertEquals(11, damage, "Total damage should use troopers who hit (7), not full strength");
    }

    @Test
    @DisplayName("firing a Disposable Weapon expends it for the rest of the scenario")
    void useAmmoMarksWeaponFired() throws Exception {
        doReturn(0.53).when(mockWeaponType).getInfantryDamage();
        doReturn(28).when(mockAttacker).getShootingStrength();
        InfantryDisposableWeaponHandler handler = newHandler();

        handler.useAmmo();

        verify(mockWeapon).setFired(true);
        verify(mockWeapon).setUsedThisRound(true);
    }
}
