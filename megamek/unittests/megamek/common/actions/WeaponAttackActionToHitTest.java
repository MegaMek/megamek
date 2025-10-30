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

package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import megamek.common.CalledShot;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Aero;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @see WeaponAttackAction#toHit(Game, int, Targetable, int, boolean)
 */
public class WeaponAttackActionToHitTest {
    // These tests were originally made to help test megamek#1072

    Player mockPlayer;
    Player mockEnemy;
    GameOptions mockOptions;
    PlanetaryConditions mockPlanetaryConditions;

    Board mockBoard;
    Game mockGame;
    LosEffects mockLos;

    AmmoMounted mockAmmo;
    WeaponType mockWeaponType;
    WeaponMounted mockWeapon;
    Mounted mockWeaponEquipment;
    CrewType mockCrewType;
    PilotOptions mockPilotOptions;

    Crew mockCrew;

    @BeforeEach
    void initialize() {
        // Mock Players
        mockPlayer = mock(Player.class);
        when(mockPlayer.getTeam()).thenReturn(0);

        mockEnemy = mock(Player.class);
        when(mockEnemy.getTeam()).thenReturn(1);

        when(mockPlayer.isEnemyOf(mockEnemy)).thenReturn(true);
        when(mockEnemy.isEnemyOf(mockPlayer)).thenReturn(true);

        // Mock the board
        mockBoard = mock(Board.class);
        when(mockBoard.isSpace()).thenReturn(false);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        when(mockBoard.getHex(any(Coords.class))).thenReturn(new Hex());

        // Mock Options
        mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Planetary Conditions
        mockPlanetaryConditions = new PlanetaryConditions();

        // Mock Game
        mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);
        when(mockGame.getActions()).thenReturn(Collections.enumeration(Collections.emptyList()));
        when(mockGame.getPhase()).thenReturn(GamePhase.FIRING);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        when(mockGame.getBoard(any(Targetable.class))).thenReturn(mockBoard);
        when(mockGame.hasBoard(0)).thenReturn(true);
        when(mockGame.hasBoardLocation(any(Coords.class), anyInt())).thenReturn(true);
        when(mockGame.getHex(any(Coords.class), anyInt())).thenCallRealMethod();
        when(mockGame.onConnectedBoards(any(Targetable.class), any(Targetable.class))).thenReturn(true);
        when(mockGame.onTheSameBoard(any(Targetable.class), any(Targetable.class))).thenReturn(true);

        // Mock LosEffects
        mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getFlares()).thenReturn(new Vector<>());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);
        when(mockLos.losModifiers(any(Game.class), anyInt(), anyBoolean())).thenReturn(new ToHitData());

        // Mock Ammo
        mockAmmo = mock(AmmoMounted.class);
        when(mockAmmo.canFire()).thenReturn(true);
        when(mockAmmo.hasUsableShotsLeft()).thenReturn(true);
        when(mockAmmo.getUsableShotsLeft()).thenReturn(10);

        // Mock Weapon Type
        mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.getName()).thenReturn("Mock Weapon Type");
        when(mockWeaponType.getInternalName()).thenReturn("Mock Internal Weapon Type");
        when(mockWeaponType.getDamage()).thenReturn(5);
        mockWeaponType.setShortRange(3);
        mockWeaponType.setMediumRange(10);
        mockWeaponType.setLongRange(20);
        when(mockWeaponType.getMaxRange()).thenReturn(20);
        when(mockWeaponType.getMaxRange(any(), any())).thenReturn(20);
        when(mockWeaponType.getRanges(any(), any())).thenReturn(new int[] { 0, 3, 10, 20, 20 });
        when(mockWeaponType.getWRanges()).thenReturn(new int[] { 0, 3, 10, 20, 20 });

        // Mock Weapon
        mockWeapon = mock(WeaponMounted.class);
        mockWeaponEquipment = mockWeapon;
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockWeapon.getLinkedAmmo()).thenReturn(mockAmmo);
        when(mockWeapon.canFire()).thenReturn(true);
        when(mockWeapon.canFire(anyBoolean(), anyBoolean())).thenReturn(true);
        when(mockWeapon.getCalledShot()).thenReturn(new CalledShot());

        // Mock Crew Type
        mockCrewType = mock(CrewType.class);
        when(mockCrewType.getMaxPrimaryTargets()).thenReturn(0);

        // Mock Pilot Options
        mockPilotOptions = mock(PilotOptions.class);
        when(mockPilotOptions.stringOption(anyString())).thenReturn("mock");

        // Mock Crew
        mockCrew = mock(Crew.class);
        when(mockCrew.isActive()).thenReturn(true);
        when(mockCrew.getCrewType()).thenReturn(mockCrewType);
        when(mockCrew.getOptions()).thenReturn(mockPilotOptions);
    }


    @Test
    void initialTest() {
        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getOwner()).thenReturn(mockPlayer);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getWeapon(anyInt())).thenReturn(mockWeapon);
        when(mockAttackingEntity.getEquipment(anyInt())).thenReturn(mockWeaponEquipment);
        when(mockAttackingEntity.getCrew()).thenReturn(mockCrew);
        when(mockAttackingEntity.getSwarmTargetId()).thenReturn(Entity.NONE);

        Tank mockTarget = mock(Tank.class);
        when(mockTarget.getOwner()).thenReturn(mockEnemy);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.getSwarmTargetId()).thenReturn(Entity.NONE);

        when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);
        when(mockGame.getEntity(1)).thenReturn(mockTarget);


        when(mockTarget.getGame()).thenReturn(mockGame);
        when(mockAttackingEntity.getGame()).thenReturn(mockGame);

        try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class, invocationOnMock -> mockLos)) {
            mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                  .thenReturn(mockLos);


            ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(-5, toHit.getValue());
        }
    }

    @Test
    void immobileTargetTest() {
        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getOwner()).thenReturn(mockPlayer);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getWeapon(anyInt())).thenReturn(mockWeapon);
        when(mockAttackingEntity.getEquipment(anyInt())).thenReturn(mockWeaponEquipment);
        when(mockAttackingEntity.getCrew()).thenReturn(mockCrew);
        when(mockAttackingEntity.getSwarmTargetId()).thenReturn(Entity.NONE);

        Tank mockTarget = mock(Tank.class);
        when(mockTarget.getOwner()).thenReturn(mockEnemy);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.getSwarmTargetId()).thenReturn(Entity.NONE);
        when(mockTarget.isImmobile()).thenReturn(true);

        when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);
        when(mockGame.getEntity(1)).thenReturn(mockTarget);

        when(mockTarget.getGame()).thenReturn(mockGame);
        when(mockAttackingEntity.getGame()).thenReturn(mockGame);

        try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class, invocationOnMock -> mockLos)) {
            mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                  .thenReturn(mockLos);


            ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(-9, toHit.getValue());
        }
    }

    @Test
    void doubleBlindNightTest() {

        mockPlanetaryConditions.setLight(Light.PITCH_BLACK);
        when(mockOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)).thenReturn(true);

        Tank mockAttackingEntity = mock(Tank.class);
        when(mockAttackingEntity.getOwner()).thenReturn(mockPlayer);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getWeapon(anyInt())).thenReturn(mockWeapon);
        when(mockAttackingEntity.getEquipment(anyInt())).thenReturn(mockWeaponEquipment);
        when(mockAttackingEntity.getCrew()).thenReturn(mockCrew);
        when(mockAttackingEntity.getSwarmTargetId()).thenReturn(Entity.NONE);

        Tank mockTarget = mock(Tank.class);
        when(mockTarget.getOwner()).thenReturn(mockEnemy);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 10));
        when(mockTarget.isIlluminated()).thenReturn(true);
        when(mockTarget.getSwarmTargetId()).thenReturn(Entity.NONE);

        when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);
        when(mockGame.getEntity(1)).thenReturn(mockTarget);


        when(mockTarget.getGame()).thenReturn(mockGame);
        when(mockAttackingEntity.getGame()).thenReturn(mockGame);

        try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class, invocationOnMock -> mockLos)) {
            mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                  .thenReturn(mockLos);


            ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(-4, toHit.getValue());

            // The target was illuminated - what if they aren't?
            when(mockTarget.isIlluminated()).thenReturn(false);
            toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());

            // What if they came closer?
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
            toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(-1, toHit.getValue());
        }
    }

    @Test
    void aeroToGround() {
        Aero mockAttackingEntity = mock(Aero.class);
        when(mockAttackingEntity.getOwner()).thenReturn(mockPlayer);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getAltitude()).thenReturn(6);
        when(mockAttackingEntity.getWeapon(anyInt())).thenReturn(mockWeapon);
        when(mockAttackingEntity.getEquipment(anyInt())).thenReturn(mockWeaponEquipment);
        when(mockAttackingEntity.getCrew()).thenReturn(mockCrew);
        when(mockAttackingEntity.getSwarmTargetId()).thenReturn(Entity.NONE);
        when(mockAttackingEntity.isAirborne()).thenReturn(true);
        when(mockAttackingEntity.passedOver(any())).thenReturn(true);


        Tank mockTarget = mock(Tank.class);
        when(mockTarget.getOwner()).thenReturn(mockEnemy);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
        when(mockTarget.isIlluminated()).thenReturn(false);
        when(mockTarget.getSwarmTargetId()).thenReturn(Entity.NONE);

        when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);
        when(mockGame.getEntity(1)).thenReturn(mockTarget);


        when(mockTarget.getGame()).thenReturn(mockGame);
        when(mockAttackingEntity.getGame()).thenReturn(mockGame);
        try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class, invocationOnMock -> mockLos)) {
            mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                  .thenReturn(mockLos);


            ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(0, toHit.getValue());

            // In pitch black?
            mockPlanetaryConditions.setLight(Light.PITCH_BLACK);
            toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(4, toHit.getValue());

            // And now with double-blind:
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)).thenReturn(true);
            toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
            assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
        }
    }

    @Nested
    class aeroToGroundHexInDark {
        BombMounted mockBomb;
        Aero mockAttackingEntity;
        Targetable mockTarget;

        @BeforeEach
        void beforeEach() {
            mockPlanetaryConditions.setLight(Light.PITCH_BLACK);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)).thenReturn(true);

            mockBomb = mock(BombMounted.class);

            mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getOwner()).thenReturn(mockPlayer);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(5);
            when(mockAttackingEntity.getWeapon(anyInt())).thenReturn(mockWeapon);
            when(mockAttackingEntity.getEquipment(anyInt())).thenReturn(mockWeaponEquipment);
            when(mockAttackingEntity.getCrew()).thenReturn(mockCrew);
            when(mockAttackingEntity.getSwarmTargetId()).thenReturn(Entity.NONE);
            when(mockAttackingEntity.getBombs(any())).thenReturn(List.of(new BombMounted[] { mockBomb }));
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.passedOver(any())).thenReturn(true);


            mockTarget = mock(Targetable.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
            when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_CLEAR);


            when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);

            when(mockAttackingEntity.getGame()).thenReturn(mockGame);
        }

        @Test
        void cannotStrikeGroundWithoutStrafing() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);
                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
            }
        }

        @Test
        void canStrikeGroundWithStrafing() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);
                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, true);
                assertEquals(11, toHit.getValue());
            }
        }


        @Test
        void diveBombingCommonScenarios() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(false);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(11, toHit.getValue());
            }
        }

        @Test
        void cannotDiveBombAtHighAltitude() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(false);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                when(mockAttackingEntity.getAltitude()).thenReturn(6);
                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
            }
        }

        @Test
        void cannotDiveBombAtLowAltitude() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);
                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(false);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                when(mockAttackingEntity.getAltitude()).thenReturn(2);
                try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
                    mockedCompute.when(() -> Compute.isAirToGround(any(), any()))
                          .thenReturn(true);
                    ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                    assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
                }
            }
        }

        @Test
        void altitudeBombingAtLowAltitude() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockAttackingEntity.getAltitude()).thenReturn(2);
                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(false);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(true);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(11, toHit.getValue());
            }
        }
    }
}
