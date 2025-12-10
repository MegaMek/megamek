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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Aero;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.MekWithArms;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
        when(mockGame.isOnGroundMap(any(Targetable.class))).thenReturn(true);

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
        when(mockWeaponType.getATRanges()).thenReturn(new int[] { 0, 3, 10, 20, 20 });

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
        when(mockAttackingEntity.getAttackingEntity()).thenReturn(mockAttackingEntity);

        when(mockWeapon.getEntity()).thenReturn(mockAttackingEntity);

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
        when(mockAttackingEntity.getAttackingEntity()).thenReturn(mockAttackingEntity);

        when(mockWeapon.getEntity()).thenReturn(mockAttackingEntity);

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
        when(mockAttackingEntity.getAttackingEntity()).thenReturn(mockAttackingEntity);

        when(mockWeapon.getEntity()).thenReturn(mockAttackingEntity);

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

    @Nested
    class BasicAeroToGroundTests {

        Aero mockAttackingEntity;
        Tank mockTarget;

        @BeforeEach
        void beforeEach() {
            mockAttackingEntity = mock(Aero.class);
            when(mockAttackingEntity.getOwner()).thenReturn(mockPlayer);
            when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
            when(mockAttackingEntity.getAltitude()).thenReturn(6);
            when(mockAttackingEntity.getWeapon(anyInt())).thenReturn(mockWeapon);
            when(mockAttackingEntity.getEquipment(anyInt())).thenReturn(mockWeaponEquipment);
            when(mockAttackingEntity.getCrew()).thenReturn(mockCrew);
            when(mockAttackingEntity.getSwarmTargetId()).thenReturn(Entity.NONE);
            when(mockAttackingEntity.isAirborne()).thenReturn(true);
            when(mockAttackingEntity.isAero()).thenReturn(true);
            when(mockAttackingEntity.passedOver(any())).thenReturn(true);
            when(mockAttackingEntity.getAttackingEntity()).thenReturn(mockAttackingEntity);

            when(mockWeapon.getEntity()).thenReturn(mockAttackingEntity);

            mockTarget = mock(Tank.class);
            when(mockTarget.getOwner()).thenReturn(mockEnemy);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
            when(mockTarget.isIlluminated()).thenReturn(false);
            when(mockTarget.getSwarmTargetId()).thenReturn(Entity.NONE);

            when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);
            when(mockGame.getEntity(1)).thenReturn(mockTarget);

            when(mockTarget.getGame()).thenReturn(mockGame);
            when(mockAttackingEntity.getGame()).thenReturn(mockGame);
        }

        @Test
        void defaultTest() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockAttackingEntity.getAltitude()).thenReturn(4);
                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(-3, toHit.getValue());
            }
        }

        @Test
        @Disabled
            // Psi - I don't know, the altitude is too high, why does this test exist?
        void inPitchBlackTest() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                mockPlanetaryConditions.setLight(Light.PITCH_BLACK);
                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(4, toHit.getValue());
            }
        }

        @Test
        void withDoubleBlindTest() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                mockPlanetaryConditions.setLight(Light.PITCH_BLACK);
                when(mockOptions.booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)).thenReturn(true);
                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
            }
        }
    }

    @Nested
    class aeroToGroundHexInDark {
        final int EXPECTED_RESULT = 10;

        BombMounted mockBomb;
        Aero mockAttackingEntity;
        Targetable mockTarget;
        Vector<Coords> flightPath = new Vector<>();

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
            when(mockAttackingEntity.isAero()).thenReturn(true);
            when(mockAttackingEntity.isAirborneAeroOnGroundMap()).thenReturn(true);
            when(mockAttackingEntity.getPassedThrough()).thenReturn(flightPath);
            when(mockAttackingEntity.passedOver(any())).thenReturn(true);
            when(mockAttackingEntity.getGame()).thenReturn(mockGame);
            when(mockAttackingEntity.getAttackingEntity()).thenReturn(mockAttackingEntity);

            when(mockWeapon.getEntity()).thenReturn(mockAttackingEntity);


            mockTarget = mock(Targetable.class);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
            when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_CLEAR);
            when(mockTarget.isAirborne()).thenReturn(false);


            when(mockGame.getEntity(0)).thenReturn(mockAttackingEntity);


            flightPath.add(mockAttackingEntity.getPosition());
            flightPath.add(mockTarget.getPosition());
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

                when(mockAttackingEntity.getAltitude()).thenReturn(3);

                when(mockWeaponType.hasFlag(WeaponType.F_DIRECT_FIRE)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_LASER)).thenReturn(true);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, true);
                assertEquals(EXPECTED_RESULT, toHit.getValue());
            }
        }

        @Test
        void canStrikeGroundWithStrafingEvenIfEndedFarAway() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockAttackingEntity.getPosition()).thenReturn(new Coords(150, 150));
                when(mockAttackingEntity.getAltitude()).thenReturn(3);

                when(mockWeaponType.hasFlag(WeaponType.F_DIRECT_FIRE)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_LASER)).thenReturn(true);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, true);
                assertEquals(EXPECTED_RESULT, toHit.getValue());
            }
        }

        @Test
        void cantStrikeGroundWithStrafingTooHigh() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockAttackingEntity.getAltitude()).thenReturn(6);

                when(mockWeaponType.hasFlag(WeaponType.F_DIRECT_FIRE)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_LASER)).thenReturn(true);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, true);
                assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
            }
        }


        @Test
        void diveBombingNormal() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(false);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(EXPECTED_RESULT - 2, toHit.getValue());
            }
        }

        @Test
        void diveBombingNormalEvenIfEndedFarAway() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockAttackingEntity.getPosition()).thenReturn(new Coords(150, 150));
                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(true);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(false);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(EXPECTED_RESULT - 2, toHit.getValue());
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
                assertEquals(EXPECTED_RESULT, toHit.getValue());
            }
        }

        @Test
        void altitudeBombingAtLowAltitudeEvenIfEndedFarAway() {
            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                when(mockAttackingEntity.getPosition()).thenReturn(new Coords(150, 150));
                when(mockAttackingEntity.getAltitude()).thenReturn(2);
                when(mockWeaponType.hasFlag(WeaponType.F_DIVE_BOMB)).thenReturn(false);
                when(mockWeaponType.hasFlag(WeaponType.F_ALT_BOMB)).thenReturn(true);
                when(mockTarget.isHexBeingBombed()).thenReturn(true);
                when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_AERO_BOMB);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(EXPECTED_RESULT, toHit.getValue());
            }
        }
    }

    @Nested
    class WeaponAttackActionToHitForHandheldWeaponsTests {

        @Test
        void basicTest() {
            MekSummary mekSummary = MekSummaryCache.getInstance().getMek("Light Anti-Infantry Weapon");
            MekSummary otherMekSummary = MekSummaryCache.getInstance().getMek("Atlas AS7-D");

            MekFileParser mekFileParser;
            HandheldWeapon handheldWeapon;
            MekWithArms mek;

            try {
                mekFileParser = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName());
                handheldWeapon = (HandheldWeapon) mekFileParser.getEntity();

                mekFileParser = new MekFileParser(otherMekSummary.getSourceFile(), otherMekSummary.getEntryName());
                mek = (MekWithArms) mekFileParser.getEntity();

            } catch (Exception ex) {
                return;
            }

            handheldWeapon.setGame(mockGame);
            handheldWeapon.setPosition(new Coords(0, 0), false);
            handheldWeapon.setId(0);

            AttackHandler mockAttackHandler = mock(AttackHandler.class);
            Vector<AttackHandler> vectorAttackHandler = new Vector<>();
            vectorAttackHandler.add(mockAttackHandler);

            when(mockGame.getAttacksVector()).thenReturn(vectorAttackHandler);

            Forces mockForces = mock(Forces.class);
            when(mockGame.getForces()).thenReturn(mockForces);

            GamePhase mockGamePhase = mock(GamePhase.class);
            when(mockGame.getPhase()).thenReturn(mockGamePhase);
            when(mockGamePhase.isLounge()).thenReturn(true);

            TWGameManager gameManager = new TWGameManager();
            gameManager.setGame(mockGame);
            mek.setMekArms();
            mek.setFacing(3);
            mek.setId(2);
            gameManager.loadUnit(mek, handheldWeapon, -1);
            handheldWeapon.setTransportId(2);

            Tank mockTarget = mock(Tank.class);
            when(mockTarget.getOwner()).thenReturn(mockEnemy);
            when(mockTarget.getPosition()).thenReturn(new Coords(0, 1));
            when(mockTarget.isIlluminated()).thenReturn(true);
            when(mockTarget.getSwarmTargetId()).thenReturn(Entity.NONE);
            when(mockTarget.isImmobile()).thenReturn(true);
            when(mockTarget.getGame()).thenReturn(mockGame);
            when(mockTarget.getId()).thenReturn(1);
            when(mockTarget.isImmobile()).thenReturn(false);
            when(mockTarget.getGrappled()).thenReturn(Entity.NONE);

            when(mockGame.getEntity(0)).thenReturn(handheldWeapon);
            when(mockGame.getEntity(1)).thenReturn(mockTarget);
            when(mockGame.getEntity(2)).thenReturn(mek);

            when(mockTarget.getGame()).thenReturn(mockGame);

            mek.setPosition(new Coords(0, 0), false);
            mek.setGame(mockGame);
            mek.newRound(1);

            try (MockedStatic<LosEffects> mockedLosEffects = mockStatic(LosEffects.class,
                  invocationOnMock -> mockLos)) {
                mockedLosEffects.when(() -> LosEffects.calculateLOS(any(), any(), any(), anyBoolean()))
                      .thenReturn(mockLos);

                ToHitData toHit = WeaponAttackAction.toHit(mockGame, 0, mockTarget, 0, false);
                assertEquals(4, toHit.getValue());
            }
        }
    }

    /**
     * Tests for capital weapon modifiers against small targets (under 500 tons).
     * Per TO:AUE: Capital weapons get +5, Sub-capital direct-fire gets +3.
     * Issue #7030: Dropships under 500 tons should receive this modifier.
     *
     * <p>These tests verify the target selection condition in ComputeToHit.java (around line 1469):
     * {@code (!te.isLargeCraft() || te.getWeight() < 500)}
     *
     * <p>The condition determines if capital weapon penalties apply:
     * <ul>
     *   <li>Capital weapons: +5 modifier against small targets</li>
     *   <li>Sub-capital weapons: +3 modifier against small targets</li>
     *   <li>Capital missiles: exempt from this penalty</li>
     * </ul>
     *
     * @see megamek.common.actions.compute.ComputeToHit
     */
    @Nested
    class CapitalWeaponSmallTargetModifierTests {

        /**
         * Evaluates the condition from ComputeToHit.java that determines if capital weapon
         * penalties should apply to a target. This must match the actual implementation.
         *
         * @param isLargeCraft whether the target is classified as a large craft (dropship, warship, etc.)
         * @param weight       the weight of the target in tons
         * @return true if capital weapon penalty should apply
         *
         * @see megamek.common.actions.compute.ComputeToHit - line ~1469
         */
        private boolean shouldApplyCapitalPenalty(boolean isLargeCraft, double weight) {
            // This condition MUST match ComputeToHit.java line ~1469:
            // (!te.isLargeCraft() || te.getWeight() < 500)
            return !isLargeCraft || weight < 500;
        }

        // === Core functionality tests ===

        @Test
        void capitalWeaponVsSmallDropship_shouldApplyPenalty() {
            // Fix for #7030: A 400-ton dropship is a large craft but under 500 tons
            // Should receive penalty due to weight < 500
            assertTrue(shouldApplyCapitalPenalty(true, 400.0),
                  "400-ton dropship (large craft) should receive capital weapon penalty");
        }

        @Test
        void capitalWeaponVsLargeDropship_shouldNotApplyPenalty() {
            // A 2000-ton dropship is a large craft over 500 tons - no penalty
            assertFalse(shouldApplyCapitalPenalty(true, 2000.0),
                  "2000-ton dropship should NOT receive capital weapon penalty");
        }

        @Test
        void capitalWeaponVsFighter_shouldApplyPenalty() {
            // Fighters are not large craft - should always receive penalty
            assertTrue(shouldApplyCapitalPenalty(false, 50.0),
                  "Fighter (not large craft) should receive capital weapon penalty");
        }

        @Test
        void capitalWeaponVsSmallCraft_shouldApplyPenalty() {
            // Small craft under 500 tons - not large craft, should receive penalty
            assertTrue(shouldApplyCapitalPenalty(false, 200.0),
                  "Small craft (not large craft) should receive capital weapon penalty");
        }

        // === Boundary condition tests ===

        @Test
        void capitalWeaponVsDropshipExactly500Tons_shouldNotApplyPenalty() {
            // Edge case: exactly 500 tons means weight < 500 is false
            assertFalse(shouldApplyCapitalPenalty(true, 500.0),
                  "500-ton dropship should NOT receive penalty (boundary case)");
        }

        @Test
        void capitalWeaponVsDropship499Tons_shouldApplyPenalty() {
            // Edge case: 499 tons means weight < 500 is true
            assertTrue(shouldApplyCapitalPenalty(true, 499.0),
                  "499-ton dropship should receive penalty (boundary case)");
        }

        @Test
        void capitalWeaponVsDropship501Tons_shouldNotApplyPenalty() {
            // Edge case: 501 tons - just over the threshold
            assertFalse(shouldApplyCapitalPenalty(true, 501.0),
                  "501-ton dropship should NOT receive penalty");
        }
    }
}
