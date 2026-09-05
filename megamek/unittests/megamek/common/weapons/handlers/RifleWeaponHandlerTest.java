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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.CalledShot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.rolls.Roll;
import megamek.common.units.BipedMek;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RifleWeaponHandlerTest {

    private BipedMek mockAttacker;
    private BipedMek mockEntityTarget;
    private Game mockGame;
    private TWGameManager mockGameManager;
    private ToHitData mockToHit;
    private WeaponAttackAction mockAction;
    private WeaponMounted mockWeapon;
    private WeaponType mockWeaponType;
    private Board mockBoard;
    private Hex mockHex;
    private IBuilding mockBuilding;
    private RifleWeaponHandler handler;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        mockAttacker = mock(BipedMek.class);
        mockEntityTarget = mock(BipedMek.class);
        mockGame = mock(Game.class);
        mockGameManager = mock(TWGameManager.class);
        mockToHit = mock(ToHitData.class);
        mockAction = mock(WeaponAttackAction.class);
        mockWeapon = mock(WeaponMounted.class);
        mockWeaponType = mock(WeaponType.class);
        mockBoard = mock(Board.class);
        mockHex = mock(Hex.class);
        mockBuilding = mock(IBuilding.class);

        GameOptions mockOptions = mock(GameOptions.class);
        doReturn(false).when(mockOptions).booleanOption(any(String.class));
        doReturn(mockOptions).when(mockGame).getOptions();
        doReturn(mockBoard).when(mockGame).getBoard();
        doReturn(mockHex).when(mockGame).getHexOf(any(BipedMek.class));
        doReturn(false).when(mockHex).containsTerrain(anyInt());

        Coords attackerCoords = new Coords(0, 0);
        Coords targetCoords = new Coords(1, 0);
        doReturn(1).when(mockAttacker).getId();
        doReturn(mockAttacker).when(mockGame).getEntity(1);
        doReturn(mockAttacker).when(mockAttacker).getAttackingEntity();
        doReturn(attackerCoords).when(mockAttacker).getPosition();
        doReturn(-1).when(mockAttacker).getSwarmTargetId();

        doReturn(2).when(mockEntityTarget).getId();
        doReturn(targetCoords).when(mockEntityTarget).getPosition();
        doReturn(Targetable.TYPE_ENTITY).when(mockEntityTarget).getTargetType();
        doReturn(mockEntityTarget).when(mockGame).getTarget(Targetable.TYPE_ENTITY, 2);

        doReturn("CT").when(mockEntityTarget).getLocationAbbr(any(HitData.class));
        doReturn(false).when(mockEntityTarget).removePartialCoverHits(anyInt(), anyInt(), anyInt());

        CalledShot mockCalledShot = mock(CalledShot.class);
        doReturn(CalledShot.CALLED_NONE).when(mockCalledShot).getCall();
        doReturn(mockCalledShot).when(mockWeapon).getCalledShot();
        doReturn(mockWeaponType).when(mockWeapon).getType();
        doReturn("RifleWeapon").when(mockWeaponType).getInternalName();
        doReturn(mockWeapon).when(mockAttacker).getEquipment(0);

        doReturn(1).when(mockAction).getEntityId();
        doReturn(0).when(mockAction).getWeaponId();
        doReturn(Targetable.TYPE_ENTITY).when(mockAction).getTargetType();
        doReturn(2).when(mockAction).getTargetId();
        doReturn(Mek.LOC_NONE).when(mockAction).getAimedLocation();
        doReturn(AimingMode.NONE).when(mockAction).getAimingMode();

        doReturn(new Vector<Report>()).when(mockGameManager).damageEntity(
              any(), any(), anyInt(), anyBoolean(), any(DamageType.class),
              anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());

        doReturn(0.5).when(mockBuilding).getDamageToScale();

        handler = new RifleWeaponHandler(mockToHit, mockAction, mockGame, mockGameManager);

        // RifleWeaponHandler sets its private `hit` field in calcDamagePerHit(); inject directly
        HitData hitData = new HitData(Mek.LOC_CENTER_TORSO, false);
        setField(handler, "hit", hitData);
        setField(handler, "nDamPerHit", 10);

        // RifleWeaponHandler.handleEntityDamage reads roll.getIntValue() for box-cars check
        Roll mockRoll = mock(Roll.class);
        doReturn(6).when(mockRoll).getIntValue();
        handler.roll = mockRoll;
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Class<?> cls = obj.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " not found in hierarchy of " + obj.getClass().getName());
    }

    /**
     * Tests for {@link RifleWeaponHandler#handleEntityDamage}
     */
    @Nested
    @DisplayName("handleEntityDamage")
    class HandleEntityDamage {

        @Test
        @DisplayName("damage is scaled when entity is inside a building")
        void damageIsScaledForEntityInsideBuilding() {
            doReturn(true).when(mockEntityTarget).isInBuilding();

            handler.handleEntityDamage(mockEntityTarget, new Vector<>(), mockBuilding, 1, 1, 0);

            ArgumentCaptor<Integer> damageCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(mockGameManager).damageEntity(
                  any(), any(), damageCaptor.capture(),
                  anyBoolean(), any(DamageType.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
            assertEquals(5, damageCaptor.getValue(), "Damage should be halved (floor(0.5 * 10)) when entity is inside the building");
        }

        @Test
        @DisplayName("damage is not scaled when entity is not inside the building")
        void damageIsNotScaledForEntityOutsideBuilding() {
            doReturn(false).when(mockEntityTarget).isInBuilding();

            handler.handleEntityDamage(mockEntityTarget, new Vector<>(), mockBuilding, 1, 1, 0);

            ArgumentCaptor<Integer> damageCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(mockGameManager).damageEntity(
                  any(), any(), damageCaptor.capture(),
                  anyBoolean(), any(DamageType.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
            assertEquals(10, damageCaptor.getValue(), "Damage should be unmodified when entity is not inside the building");
        }

        @Test
        @DisplayName("damage is not scaled when there is no building")
        void damageIsNotScaledWithNullBuilding() {
            handler.handleEntityDamage(mockEntityTarget, new Vector<>(), null, 1, 1, 0);

            ArgumentCaptor<Integer> damageCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(mockGameManager).damageEntity(
                  any(), any(), damageCaptor.capture(),
                  anyBoolean(), any(DamageType.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
            assertEquals(10, damageCaptor.getValue(), "Damage should be unmodified when there is no building");
        }
    }
}
