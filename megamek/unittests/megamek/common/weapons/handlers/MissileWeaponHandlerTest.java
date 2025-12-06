/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.ArrayList;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MissileWeaponHandler null ammo handling.
 * <p>
 * Tests verify that missile weapon handlers gracefully handle null ammo scenarios
 * without throwing NullPointerException. This addresses GitHub issue #2847 where
 * Aerospace Squadrons could trigger NPE during space battles.
 * <p>
 * @see <a href="https://github.com/MegaMek/megamek/issues/2847">GitHub Issue #2847</a>
 */
public class MissileWeaponHandlerTest {

    private static final int ATTACKER_ID = 1;
    private static final int TARGET_ID = 2;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Test that isNemesisConfusable() returns false when ammo is null,
     * instead of throwing NullPointerException.
     * <p>
     * This reproduces the scenario from GitHub issue #2847 where Aerospace
     * Squadrons could have weapons without properly linked ammunition.
     */
    @Test
    void testIsNemesisConfusableWithNullAmmoReturnsFalse() throws EntityLoadingException {
        MissileWeaponHandler handler = createMissileWeaponHandler();

        // Set ammo to null using reflection to simulate the bug scenario
        setAmmoFieldToNull(handler);

        // Should return false without throwing NPE
        boolean result = assertDoesNotThrow(
              () -> invokeIsNemesisConfusable(handler),
              "isNemesisConfusable() should not throw NPE when ammo is null"
        );

        assertFalse(result, "isNemesisConfusable() should return false when ammo is null");
    }

    /**
     * Creates a MissileWeaponHandler with mocked dependencies.
     */
    private MissileWeaponHandler createMissileWeaponHandler() throws EntityLoadingException {
        // Create mocks
        ToHitData mockToHit = mock(ToHitData.class);
        WeaponAttackAction mockAction = mock(WeaponAttackAction.class);
        Game mockGame = createMockGame();
        TWGameManager mockGameManager = mock(TWGameManager.class);

        // Configure action
        doReturn(ATTACKER_ID).when(mockAction).getEntityId();
        doReturn(0).when(mockAction).getWeaponId();
        doReturn(Targetable.TYPE_ENTITY).when(mockAction).getTargetType();
        doReturn(TARGET_ID).when(mockAction).getTargetId();

        // Create and configure attacker entity
        Entity attacker = createMockEntity(ATTACKER_ID);
        Entity target = createMockEntity(TARGET_ID);

        // Configure game to return entities
        doReturn(attacker).when(mockGame).getEntity(ATTACKER_ID);
        doReturn(target).when(mockGame).getTarget(Targetable.TYPE_ENTITY, TARGET_ID);
        doReturn(mockGame).when(attacker).getGame();

        // Create weapon mock
        WeaponMounted mockWeapon = mock(WeaponMounted.class);
        WeaponType mockWeaponType = mock(WeaponType.class);
        doReturn(mockWeaponType).when(mockWeapon).getType();
        doReturn("ISLRM5").when(mockWeaponType).getInternalName();
        doReturn(AmmoType.AmmoTypeEnum.LRM).when(mockWeaponType).getAmmoType();
        doReturn(null).when(mockWeapon).getLinked();
        doReturn(null).when(mockWeapon).getLinkedBy();
        doReturn(attacker).when(mockWeapon).getEntity();

        // Configure attacker to return weapon
        doReturn(mockWeapon).when(attacker).getEquipment(0);

        return new MissileWeaponHandler(mockToHit, mockAction, mockGame, mockGameManager);
    }

    /**
     * Creates a mock Game with required dependencies.
     */
    private Game createMockGame() {
        Game game = mock(Game.class);
        Board mockBoard = mock(Board.class);
        GameOptions mockOptions = mock(GameOptions.class);

        doReturn(mockBoard).when(game).getBoard();
        doReturn(mockOptions).when(game).getOptions();
        doReturn(false).when(mockOptions).booleanOption(any(String.class));
        doReturn(new ArrayList<>()).when(game).getEntitiesVector();

        return game;
    }

    /**
     * Creates a mock Entity with the given ID.
     */
    private Entity createMockEntity(int id) {
        Entity entity = mock(Entity.class);
        Player owner = mock(Player.class);

        doReturn(1).when(owner).getTeam();
        doReturn(owner).when(entity).getOwner();
        doReturn(id).when(entity).getId();
        doReturn(new Coords(0, 0)).when(entity).getPosition();
        doReturn(new ArrayList<>()).when(entity).getEquipment();
        doReturn(entity).when(entity).getAttackingEntity();

        return entity;
    }

    /**
     * Sets the ammo field to null using reflection.
     * This simulates the bug scenario where ammo is not properly initialized.
     */
    private void setAmmoFieldToNull(MissileWeaponHandler handler) {
        try {
            Field ammoField = AmmoWeaponHandler.class.getDeclaredField("ammo");
            ammoField.setAccessible(true);
            ammoField.set(handler, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ammo field to null", e);
        }
    }

    /**
     * Invokes the protected isNemesisConfusable() method using reflection.
     */
    private boolean invokeIsNemesisConfusable(MissileWeaponHandler handler) {
        try {
            java.lang.reflect.Method method = MissileWeaponHandler.class.getDeclaredMethod("isNemesisConfusable");
            method.setAccessible(true);
            return (boolean) method.invoke(handler);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap and rethrow the actual exception
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke isNemesisConfusable()", e);
        }
    }
}
