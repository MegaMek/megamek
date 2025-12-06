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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
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
 * Unit tests for CLIATMHandler null ammo handling.
 * <p>
 * Tests verify that iATM weapon handlers gracefully handle null ammo scenarios
 * without throwing NullPointerException. This addresses GitHub issue #2847 where
 * Aerospace Squadrons could trigger NPE during space battles.
 * <p>
 * @see <a href="https://github.com/MegaMek/megamek/issues/2847">GitHub Issue #2847</a>
 */
public class CLIATMHandlerTest {

    private static final int ATTACKER_ID = 1;
    private static final int TARGET_ID = 2;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Test that handle() returns false when ammo is null,
     * instead of throwing NullPointerException.
     */
    @Test
    void testHandleWithNullAmmoReturnsFalse() throws EntityLoadingException {
        CLIATMHandler handler = createCLIATMHandler();

        // Set ammo to null using reflection
        setAmmoFieldToNull(handler);

        Vector<Report> vPhaseReport = new Vector<>();

        // Should return false without throwing NPE
        boolean result = assertDoesNotThrow(
              () -> handler.handle(GamePhase.FIRING, vPhaseReport),
              "handle() should not throw NPE when ammo is null"
        );

        assertFalse(result, "handle() should return false when ammo is null");
    }

    /**
     * Test that calcDamagePerHit() returns 0 when ammo is null,
     * instead of throwing NullPointerException.
     */
    @Test
    void testCalcDamagePerHitWithNullAmmoReturnsZero() throws EntityLoadingException {
        CLIATMHandler handler = createCLIATMHandler();

        // Set ammo to null using reflection
        setAmmoFieldToNull(handler);

        // Should return 0 without throwing NPE
        int result = assertDoesNotThrow(
              () -> invokeCalcDamagePerHit(handler),
              "calcDamagePerHit() should not throw NPE when ammo is null"
        );

        assertEquals(0, result, "calcDamagePerHit() should return 0 when ammo is null");
    }

    /**
     * Test that calcHits() returns 0 when ammo is null,
     * instead of throwing NullPointerException.
     */
    @Test
    void testCalcHitsWithNullAmmoReturnsZero() throws EntityLoadingException {
        CLIATMHandler handler = createCLIATMHandler();

        // Set ammo to null using reflection
        setAmmoFieldToNull(handler);

        Vector<Report> vPhaseReport = new Vector<>();

        // Should return 0 without throwing NPE
        int result = assertDoesNotThrow(
              () -> invokeCalcHits(handler, vPhaseReport),
              "calcHits() should not throw NPE when ammo is null"
        );

        assertEquals(0, result, "calcHits() should return 0 when ammo is null");
    }

    /**
     * Test that calcMissileHits() returns 0 when ammo is null,
     * instead of throwing NullPointerException.
     */
    @Test
    void testCalcMissileHitsWithNullAmmoReturnsZero() throws EntityLoadingException {
        CLIATMHandler handler = createCLIATMHandler();

        // Set ammo to null using reflection
        setAmmoFieldToNull(handler);

        Vector<Report> vPhaseReport = new Vector<>();

        // Should return 0 without throwing NPE
        int result = assertDoesNotThrow(
              () -> invokeCalcMissileHits(handler, vPhaseReport),
              "calcMissileHits() should not throw NPE when ammo is null"
        );

        assertEquals(0, result, "calcMissileHits() should return 0 when ammo is null");
    }

    /**
     * Creates a CLIATMHandler with mocked dependencies.
     */
    private CLIATMHandler createCLIATMHandler() throws EntityLoadingException {
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
        doReturn("CLiATM5").when(mockWeaponType).getInternalName();
        doReturn(AmmoType.AmmoTypeEnum.IATM).when(mockWeaponType).getAmmoType();
        doReturn(null).when(mockWeapon).getLinked();
        doReturn(null).when(mockWeapon).getLinkedBy();
        doReturn(attacker).when(mockWeapon).getEntity();

        // Configure attacker to return weapon
        doReturn(mockWeapon).when(attacker).getEquipment(0);

        return new CLIATMHandler(mockToHit, mockAction, mockGame, mockGameManager);
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
     */
    private void setAmmoFieldToNull(CLIATMHandler handler) {
        try {
            Field ammoField = AmmoWeaponHandler.class.getDeclaredField("ammo");
            ammoField.setAccessible(true);
            ammoField.set(handler, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ammo field to null", e);
        }
    }

    /**
     * Invokes the protected calcDamagePerHit() method using reflection.
     */
    private int invokeCalcDamagePerHit(CLIATMHandler handler) {
        try {
            Method method = CLIATMHandler.class.getDeclaredMethod("calcDamagePerHit");
            method.setAccessible(true);
            return (int) method.invoke(handler);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke calcDamagePerHit()", e);
        }
    }

    /**
     * Invokes the protected calcHits() method using reflection.
     */
    @SuppressWarnings("unchecked")
    private int invokeCalcHits(CLIATMHandler handler, Vector<Report> vPhaseReport) {
        try {
            Method method = CLIATMHandler.class.getDeclaredMethod("calcHits", Vector.class);
            method.setAccessible(true);
            return (int) method.invoke(handler, vPhaseReport);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke calcHits()", e);
        }
    }

    /**
     * Invokes the protected calcMissileHits() method using reflection.
     */
    @SuppressWarnings("unchecked")
    private int invokeCalcMissileHits(CLIATMHandler handler, Vector<Report> vPhaseReport) {
        try {
            Method method = CLIATMHandler.class.getDeclaredMethod("calcMissileHits", Vector.class);
            method.setAccessible(true);
            return (int) method.invoke(handler, vPhaseReport);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke calcMissileHits()", e);
        }
    }
}
