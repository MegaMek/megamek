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

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.ToHitData;
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
 * Every shot at a building from the hex next to it hits (TW p. 171), so a platoon firing at an adjacent building hits
 * with every trooper instead of rolling on the Cluster Hits Table.
 */
class InfantryBuildingAttackTest {

    private static final int SHOOTING_STRENGTH = 28;

    private InfantryWeaponHandler handler;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        ConvInfantry attacker = mock(ConvInfantry.class);
        Targetable building = mock(Targetable.class);
        WeaponType weaponType = mock(WeaponType.class);
        WeaponMounted weapon = mock(WeaponMounted.class);
        Game game = mock(Game.class);
        WeaponAttackAction action = mock(WeaponAttackAction.class);

        GameOptions options = mock(GameOptions.class);
        doReturn(false).when(options).booleanOption(any(String.class));
        doReturn(options).when(game).getOptions();
        Hex hex = mock(Hex.class);
        doReturn(mock(Board.class)).when(game).getBoard();
        doReturn(hex).when(game).getHexOf(any());
        doReturn(false).when(hex).containsTerrain(anyInt());

        doReturn(1).when(attacker).getId();
        doReturn(attacker).when(game).getEntity(1);
        doReturn(attacker).when(attacker).getAttackingEntity();
        doReturn(true).when(attacker).isConventionalInfantry();
        doReturn(new Coords(0, 0)).when(attacker).getPosition();
        doReturn(-1).when(attacker).getSwarmTargetId();
        doReturn(SHOOTING_STRENGTH).when(attacker).getShootingStrength();
        doReturn(1.0).when(attacker).getDamagePerTrooper();

        doReturn(2).when(building).getId();
        doReturn(new Coords(1, 0)).when(building).getPosition();
        doReturn(Targetable.TYPE_BUILDING).when(building).getTargetType();

        doReturn(false).when(weaponType).hasFlag(any(EquipmentFlag.class));
        doReturn(weaponType).when(weapon).getType();
        doReturn(1).when(action).getEntityId();
        doReturn(attacker).when(action).getEntity(game);
        doReturn(0).when(action).getWeaponId();
        doReturn(weapon).when(attacker).getWeapon(0);
        doReturn(weapon).when(attacker).getEquipment(0);
        doReturn(Targetable.TYPE_ENTITY).when(action).getTargetType();
        doReturn(2).when(action).getTargetId();

        handler = new InfantryWeaponHandler(mock(ToHitData.class), action, game, mock(TWGameManager.class));
        setField(handler, "attackingEntity", attacker);
        setField(handler, "target", building);
        setField(handler, "weapon", weapon);
        setField(handler, "weaponType", weaponType);
        setField(handler, "nRange", 1);
    }

    @Test
    @DisplayName("A platoon next to a building hits it with every trooper (TW p. 171)")
    void everyTrooperHitsAnAdjacentBuilding() {
        int damage = handler.calcHits(new Vector<>());

        assertEquals(SHOOTING_STRENGTH, damage, "28 troopers doing 1 damage each all hit");
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
