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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ScreenLauncherHandler damage application.
 * <p>
 * Tests verify that Screen Launcher damage is applied correctly per official errata (Forum Topic 77239, Xotl ruling
 * April 26, 2023):
 * <ul>
 *   <li>Large Craft (500+ tons): Single 15-point hit</li>
 *   <li>Small/Individual craft: 5-point clusters (3 hits of 5 damage each)</li>
 *   <li>Fighter Squadron: 15 damage to each fighter independently</li>
 * </ul>
 *
 * @author Hammer - Built with Claude Code
 * @since 2025-12-05
 */
public class ScreenLauncherHandlerTest {

    private Entity mockAttacker;
    private Game mockGame;
    private TWGameManager mockGameManager;
    private ToHitData mockToHit;
    private WeaponAttackAction mockAction;
    private Coords targetCoords;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mockAttacker = mock(Entity.class);
        mockGame = mock(Game.class);
        mockGameManager = mock(TWGameManager.class);
        mockToHit = mock(ToHitData.class);
        mockAction = mock(WeaponAttackAction.class);
        targetCoords = new Coords(5, 5);

        // Configure basic mocks
        Board mockBoard = mock(Board.class);
        GameOptions mockOptions = mock(GameOptions.class);

        doReturn(mockBoard).when(mockGame).getBoard();
        doReturn(mockOptions).when(mockGame).getOptions();
        doReturn(false).when(mockOptions).booleanOption(any(String.class));

        // Configure attacker
        doReturn(1).when(mockAttacker).getId();
        doReturn(mockAttacker).when(mockGame).getEntity(1);
        doReturn(mockGame).when(mockAttacker).getGame();
        doReturn(mockAttacker).when(mockAttacker).getAttackingEntity();

        // Configure weapon
        WeaponMounted mockWeapon = mock(WeaponMounted.class);
        WeaponType mockWeaponType = mock(WeaponType.class);
        AmmoMounted mockAmmo = mock(AmmoMounted.class);

        doReturn(mockWeaponType).when(mockWeapon).getType();
        doReturn("Screen Launcher").when(mockWeaponType).getName();
        doReturn("ISScreenLauncher").when(mockWeaponType).getInternalName();
        doReturn(mockAmmo).when(mockWeapon).getLinked();
        doReturn(1).when(mockAmmo).getUsableShotsLeft();
        doReturn(mockWeapon).when(mockAttacker).getEquipment(0);

        // Configure action
        doReturn(1).when(mockAction).getEntityId();
        doReturn(0).when(mockAction).getWeaponId();
        doReturn(Targetable.TYPE_HEX_SCREEN).when(mockAction).getTargetType();
        doReturn(1000).when(mockAction).getTargetId();

        // Mock target (Targetable)
        Targetable mockTarget = mock(Targetable.class);
        doReturn("Screen Target").when(mockTarget).getDisplayName();
        doReturn(targetCoords).when(mockTarget).getPosition();
        doReturn(mockTarget).when(mockGame).getTarget(Targetable.TYPE_HEX_SCREEN, 1000);

        // Configure toHit
        doReturn(TargetRoll.AUTOMATIC_SUCCESS).when(mockToHit).getValue();

        // Configure gameManager to return empty reports
        doReturn(new Vector<Report>()).when(mockGameManager).damageEntity(any(), any(), anyInt());
    }

    /**
     * Test that calcAttackValue() returns 15 damage. Per Screen Launcher rules, damage is always 15 standard-scale
     * points.
     */
    @Test
    void testCalcAttackValueReturns15() throws EntityLoadingException {
        ScreenLauncherHandler handler = new ScreenLauncherHandler(
              mockToHit, mockAction, mockGame, mockGameManager);

        // Use reflection to access protected method, or test through handle()
        // For simplicity, we verify through the handle() method behavior
        assertEquals(15, handler.calcAttackValue(),
              "Screen Launcher should always deal 15 damage");
    }

    /**
     * Test that small craft (non-large craft) receives damage in 5-point clusters. Per official errata, small craft
     * receive 3 hits of 5 damage each.
     */
    @Test
    void testSmallCraftReceivesClusterDamage() throws EntityLoadingException {
        // Create mock small craft - explicitly mock both conditions for test isolation
        Entity smallCraft = mock(Entity.class);
        doReturn(false).when(smallCraft).isCapitalScale();
        doReturn(false).when(smallCraft).isLargeCraft();
        doReturn(new HitData(0)).when(smallCraft).rollHitLocation(anyInt(), anyInt());

        // Configure game to return small craft in target hex
        List<Entity> entitiesInHex = new ArrayList<>();
        entitiesInHex.add(smallCraft);
        doReturn(entitiesInHex).when(mockGame).getEntitiesVector(targetCoords);

        ScreenLauncherHandler handler = new ScreenLauncherHandler(
              mockToHit, mockAction, mockGame, mockGameManager);

        handler.handle(GamePhase.FIRING, new Vector<>());

        // Verify damageEntity called 3 times with 5 damage each (5 + 5 + 5 = 15)
        verify(mockGameManager, times(3)).damageEntity(eq(smallCraft), any(HitData.class), eq(5));
    }

    /**
     * Test that capital-scale craft receives damage as a single hit. Per official errata, capital-scale targets
     * receive one 15-point hit (which gets converted to 2 damage by the damage system).
     */
    @Test
    void testCapitalScaleCraftReceivesSingleHit() throws EntityLoadingException {
        // Create mock capital-scale craft - explicitly mock both conditions for test isolation
        Entity capitalCraft = mock(Entity.class);
        doReturn(true).when(capitalCraft).isCapitalScale();
        doReturn(false).when(capitalCraft).isLargeCraft();
        doReturn(new HitData(0)).when(capitalCraft).rollHitLocation(anyInt(), anyInt());

        // Configure game to return capital craft in target hex
        List<Entity> entitiesInHex = new ArrayList<>();
        entitiesInHex.add(capitalCraft);
        doReturn(entitiesInHex).when(mockGame).getEntitiesVector(targetCoords);

        ScreenLauncherHandler handler = new ScreenLauncherHandler(
              mockToHit, mockAction, mockGame, mockGameManager);

        handler.handle(GamePhase.FIRING, new Vector<>());

        // Verify damageEntity called once with 15 damage
        verify(mockGameManager, times(1)).damageEntity(eq(capitalCraft), any(HitData.class), eq(15));
    }

    /**
     * Test that fighter squadron has each fighter damaged independently. Per official errata, each fighter in a
     * squadron receives 15 damage.
     */
    @Test
    void testFighterSquadronDamagesEachFighter() throws EntityLoadingException {
        // Create mock fighter squadron with 3 fighters
        FighterSquadron squadron = mock(FighterSquadron.class);
        Entity fighter1 = mock(Entity.class);
        Entity fighter2 = mock(Entity.class);
        Entity fighter3 = mock(Entity.class);

        doReturn(new HitData(0)).when(fighter1).rollHitLocation(anyInt(), anyInt());
        doReturn(new HitData(0)).when(fighter2).rollHitLocation(anyInt(), anyInt());
        doReturn(new HitData(0)).when(fighter3).rollHitLocation(anyInt(), anyInt());

        List<Entity> subEntities = List.of(fighter1, fighter2, fighter3);
        doReturn(subEntities).when(squadron).getSubEntities();

        // Configure game to return squadron in target hex
        List<Entity> entitiesInHex = new ArrayList<>();
        entitiesInHex.add(squadron);
        doReturn(entitiesInHex).when(mockGame).getEntitiesVector(targetCoords);

        ScreenLauncherHandler handler = new ScreenLauncherHandler(
              mockToHit, mockAction, mockGame, mockGameManager);

        handler.handle(GamePhase.FIRING, new Vector<>());

        // Verify each fighter receives 15 damage (once per fighter)
        verify(mockGameManager, times(1)).damageEntity(eq(fighter1), any(HitData.class), eq(15));
        verify(mockGameManager, times(1)).damageEntity(eq(fighter2), any(HitData.class), eq(15));
        verify(mockGameManager, times(1)).damageEntity(eq(fighter3), any(HitData.class), eq(15));
    }
}
