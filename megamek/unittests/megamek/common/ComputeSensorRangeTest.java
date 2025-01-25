/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common;

import megamek.common.options.GameOptions;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Weather;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Suite of tests for testing {@link Compute#getSensorRangeByBracket(Game, Entity, Targetable, LosEffects)}.
 * This largely corresponds to TO:AR pg. 190, "Sensor Range Tables"
 */
public class ComputeSensorRangeTest {

    // Start Field sources
    static List<Integer> activeProbeSensorTypes = Arrays.asList(Sensor.TYPE_BAP, Sensor.TYPE_BLOODHOUND, Sensor.TYPE_CLAN_AP, Sensor.TYPE_EW_EQUIPMENT, Sensor.TYPE_WATCHDOG, Sensor.TYPE_LIGHT_AP, Sensor.TYPE_NOVA);
    static List<Integer> radarSensorTypes = Arrays.asList(Sensor.TYPE_MEK_RADAR, Sensor.TYPE_VEE_RADAR);
    static List<Integer> seismicSensorTypes = Arrays.asList(Sensor.TYPE_MEK_SEISMIC, Sensor.TYPE_VEE_SEISMIC);
    static List<Integer> irSensorTypes = Arrays.asList(Sensor.TYPE_MEK_IR, Sensor.TYPE_VEE_IR);
    static List<Integer> magscanSensorTypes = Arrays.asList(Sensor.TYPE_MEK_MAGSCAN, Sensor.TYPE_VEE_MAGSCAN);
    //  End Field Sources

    // Start tests for Compute.getSensorRangeByBracket(Game, Entity, Targetable, LosEffects)

    @ParameterizedTest
    @FieldSource(value = "radarSensorTypes")
    void testGetRadarSensorRangeByBracket(int sensorType) {

        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock weather
        Weather mockWeather = mock(Weather.class);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = mock(PlanetaryConditions.class); //new PlanetaryConditions();
        when(mockPlanetaryConditions.getWeather()).thenReturn(mockWeather);


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

        Hex mockTargetHex = mock(Hex.class);

        // Mock Sensor
        Sensor activeSensor = new Sensor(sensorType);

        // Expected Sensor Range - Normal Conditions, should be full range
        int expectedValue = activeSensor.getRangeByBracket();

        Entity mockAttackingEntity = mock(Entity.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getActiveSensor()).thenReturn(activeSensor);

        // Let's put the enemy at our max range
        Entity mockTarget = mock(Entity.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, expectedValue));

        when(mockBoard.getHex(mockTarget.getPosition())).thenReturn(mockTargetHex);

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a hill in the way. This should block Radar
        when(mockLos.isBlockedByHill()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's get rid of the hill. Let's put a building in the way instead. This should block radar.
        when(mockLos.isBlockedByHill()).thenReturn(false);
        when(mockLos.getHardBuildings()).thenReturn(1);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a different kind of building in the way. This should still block radar.
        when(mockLos.getHardBuildings()).thenReturn(0);
        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the building and test planetary conditions. EMI should reduce the range by 4.
        when(mockLos.getSoftBuildings()).thenReturn(0);
        when(mockPlanetaryConditions.isEMI()).thenReturn(true);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's add a lightening storm (-1 range). This should stack with EMI.
        when(mockWeather.isLightningStorm()).thenReturn(true);
        assertEquals(Math.max(0, expectedValue - 5), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the EMI and test lightening on its own.
        when(mockPlanetaryConditions.isEMI()).thenReturn(false);
        assertEquals(Math.max(0, expectedValue - 1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the lightening and test forest hexes.
        when(mockWeather.isLightningStorm()).thenReturn(false);
        when(mockLos.getLightWoods()).thenReturn(3); // Should have no effect
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Heavy Woods/Jungle should remove 1 hex per bracket per hex
        when(mockLos.getHeavyWoods()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(2);
        assertEquals(Math.max(0, expectedValue - 2), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Ultraheavy Woods/Jungle should remove 2 hex per bracket per hex
        when(mockLos.getUltraWoods()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(2);
        assertEquals(Math.max(0, expectedValue - 6), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(0);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getLightWoods()).thenReturn(0);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(0);

        // Water time - LOS blocked by water only works for sensors if the sensor is magscan
        // or the attacking entity is a naval vessel.
        when(mockLos.isBlockedByWater()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // If the attacker is a naval vessel it should be able to see:
        when(mockAttackingEntity.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.isBlockedByWater()).thenReturn(false);
        when(mockAttackingEntity.getMovementMode()).thenReturn(null);

        // Some sensors are effected by the target or tile's heat
        mockTarget.heat = 15;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.heat = 0;

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(false);

        // Some sensors are effected by the target's weight
        when(mockTarget.getWeight()).thenReturn(19.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(20.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(79.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(80.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(100.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(101.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(1000.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(1000.1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(0.0);

        // Industrial terrain blocks some sensors
        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(false);

        // Moving effects some sensors detection
        mockTarget.mpUsed = 0;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 1;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getElevation()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 0;
        when(mockTarget.getElevation()).thenReturn(0);
    }

    @ParameterizedTest
    @FieldSource(value = "irSensorTypes")
    void testGetIRSensorRangeByBracket(int sensorType) {

        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock weather
        Weather mockWeather = mock(Weather.class);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = mock(PlanetaryConditions.class); //new PlanetaryConditions();
        when(mockPlanetaryConditions.getWeather()).thenReturn(mockWeather);


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

        Hex mockTargetHex = mock(Hex.class);

        // Mock Sensor
        Sensor activeSensor = new Sensor(sensorType);

        // Expected Sensor Range - Normal Conditions, should be full range
        int expectedValue = activeSensor.getRangeByBracket();

        Entity mockAttackingEntity = mock(Entity.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getActiveSensor()).thenReturn(activeSensor);

        // Let's put the enemy at our max range
        Entity mockTarget = mock(Entity.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, expectedValue));

        when(mockBoard.getHex(mockTarget.getPosition())).thenReturn(mockTargetHex);

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a hill in the way. This should block Radar
        when(mockLos.isBlockedByHill()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's get rid of the hill. Let's put a building in the way instead. This reduces the range of many sensors.
        when(mockLos.isBlockedByHill()).thenReturn(false);
        when(mockLos.getHardBuildings()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 2), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 3), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHardBuildings()).thenReturn(0);
        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getSoftBuildings()).thenReturn(2);
        assertEquals(Math.max(0, expectedValue - 2), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the building and test planetary conditions. EMI should reduce the range by 4.
        when(mockLos.getSoftBuildings()).thenReturn(0);
        when(mockPlanetaryConditions.isEMI()).thenReturn(true);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's add a lightening storm (-1 range). This should stack with EMI.
        when(mockWeather.isLightningStorm()).thenReturn(true);
        assertEquals(Math.max(0, expectedValue - 5), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the EMI and test lightening on its own.
        when(mockPlanetaryConditions.isEMI()).thenReturn(false);
        assertEquals(Math.max(0, expectedValue - 1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the lightening and test forest hexes.
        when(mockWeather.isLightningStorm()).thenReturn(false);
        when(mockLos.getLightWoods()).thenReturn(3); // Should have no effect
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Heavy Woods/Jungle should remove 1 hex per bracket per hex
        when(mockLos.getHeavyWoods()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(2);
        assertEquals(Math.max(0, expectedValue - 2), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Ultraheavy Woods/Jungle should remove 2 hex per bracket per hex
        when(mockLos.getUltraWoods()).thenReturn(1);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(2);
        assertEquals(Math.max(0, expectedValue - 6), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(0);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getLightWoods()).thenReturn(0);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(0);

        // Water time - LOS blocked by water only works for sensors if the sensor is magscan
        // or the attacking entity is a naval vessel.
        when(mockLos.isBlockedByWater()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // If the attacker is a naval vessel it should be able to see:
        when(mockAttackingEntity.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.isBlockedByWater()).thenReturn(false);
        when(mockAttackingEntity.getMovementMode()).thenReturn(null);

        // Some sensors are effected by the target or tile's heat
        mockTarget.heat = 15;
        assertEquals(expectedValue+3, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        assertEquals(expectedValue+4, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.heat = 0;

        assertEquals(expectedValue+1, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(false);

        // Some sensors are effected by the target's weight
        when(mockTarget.getWeight()).thenReturn(19.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(20.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(79.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(80.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(100.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(101.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(1000.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(1000.1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(0.0);

        // Industrial terrain blocks some sensors
        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(false);

        // Moving effects some sensors detection
        mockTarget.mpUsed = 0;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 1;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getElevation()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 0;
        when(mockTarget.getElevation()).thenReturn(0);
    }

    @ParameterizedTest
    @FieldSource(value = "magscanSensorTypes")
    void testGetMagscanSensorRangeByBracket(int sensorType) {

        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock weather
        Weather mockWeather = mock(Weather.class);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = mock(PlanetaryConditions.class); //new PlanetaryConditions();
        when(mockPlanetaryConditions.getWeather()).thenReturn(mockWeather);


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

        Hex mockTargetHex = mock(Hex.class);

        // Mock Sensor
        Sensor activeSensor = new Sensor(sensorType);

        // Expected Sensor Range - Normal Conditions, should be full range
        int expectedValue = activeSensor.getRangeByBracket();

        Entity mockAttackingEntity = mock(Entity.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getActiveSensor()).thenReturn(activeSensor);

        // Let's put the enemy at our max range
        Entity mockTarget = mock(Entity.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, expectedValue));
        when(mockTarget.getWeight()).thenReturn(50.0); //Magscan needs target to have weight


        when(mockBoard.getHex(mockTarget.getPosition())).thenReturn(mockTargetHex);

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a hill in the way.
        when(mockLos.isBlockedByHill()).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's get rid of the hill. Let's put a building in the way instead. .
        when(mockLos.isBlockedByHill()).thenReturn(false);
        when(mockLos.getHardBuildings()).thenReturn(1);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a different kind of building in the way. This should still block radar.
        when(mockLos.getHardBuildings()).thenReturn(0);
        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the building and test planetary conditions. EMI should reduce the range by 4.
        when(mockLos.getSoftBuildings()).thenReturn(0);
        when(mockPlanetaryConditions.isEMI()).thenReturn(true);
        assertEquals(Math.max(0, expectedValue - 4), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's add a lightening storm (-1 range). This should stack with EMI.
        when(mockWeather.isLightningStorm()).thenReturn(true);
        assertEquals(Math.max(0, expectedValue - 5), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the EMI and test lightening on its own.
        when(mockPlanetaryConditions.isEMI()).thenReturn(false);
        assertEquals(Math.max(0, expectedValue - 1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the lightening and test forest hexes.
        when(mockWeather.isLightningStorm()).thenReturn(false);
        when(mockLos.getLightWoods()).thenReturn(3); // Should have no effect
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Heavy Woods/Jungle
        when(mockLos.getHeavyWoods()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Ultraheavy Woods/Jungle
        when(mockLos.getUltraWoods()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getLightWoods()).thenReturn(0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(0);

        // Water time - LOS blocked by water only works for sensors if the sensor is magscan
        // or the attacking entity is a naval vessel.
        when(mockLos.isBlockedByWater()).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // If the attacker is a naval vessel it should be able to see:
        when(mockAttackingEntity.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.isBlockedByWater()).thenReturn(false);
        when(mockAttackingEntity.getMovementMode()).thenReturn(null);

        // Some sensors are effected by the target or tile's heat
        mockTarget.heat = 15;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.heat = 0;

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(false);

        // Some sensors are effected by the target's weight
        when(mockTarget.getWeight()).thenReturn(19.9);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(20.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(79.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(80.0);
        assertEquals(expectedValue+1, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(100.0);
        assertEquals(expectedValue+1, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(101.0);
        assertEquals(expectedValue+2, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(1000.0);
        assertEquals(expectedValue+2, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(1000.1);
        assertEquals(expectedValue+3, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(50.0);

        // Industrial terrain blocks some sensors
        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(false);

        // Moving effects some sensors detection
        mockTarget.mpUsed = 0;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 1;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getElevation()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 0;
        when(mockTarget.getElevation()).thenReturn(0);
    }

    @ParameterizedTest
    @FieldSource(value = "seismicSensorTypes")
    void testGetSeismicSensorRangeByBracket(int sensorType) {

        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock weather
        Weather mockWeather = mock(Weather.class);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = mock(PlanetaryConditions.class); //new PlanetaryConditions();
        when(mockPlanetaryConditions.getWeather()).thenReturn(mockWeather);


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

        Hex mockTargetHex = mock(Hex.class);

        // Mock Sensor
        Sensor activeSensor = new Sensor(sensorType);

        // Expected Sensor Range - Normal Conditions, should be full range
        int expectedValue = activeSensor.getRangeByBracket();

        Entity mockAttackingEntity = mock(Entity.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getActiveSensor()).thenReturn(activeSensor);

        // Let's put the enemy at our max range
        Entity mockTarget = mock(Entity.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, expectedValue));
        mockTarget.mpUsed = 1;

        when(mockBoard.getHex(mockTarget.getPosition())).thenReturn(mockTargetHex);

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a hill in the way. This should block Radar
        when(mockLos.isBlockedByHill()).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's get rid of the hill. Let's put a building in the way instead. This reduces the range of many sensors.
        when(mockLos.isBlockedByHill()).thenReturn(false);
        when(mockLos.getHardBuildings()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHardBuildings()).thenReturn(0);
        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getSoftBuildings()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the building and test planetary conditions. EMI should reduce the range by 4.
        when(mockLos.getSoftBuildings()).thenReturn(0);
        when(mockPlanetaryConditions.isEMI()).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's add a lightening storm (-1 range). This should stack with EMI.
        when(mockWeather.isLightningStorm()).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the EMI and test lightening on its own.
        when(mockPlanetaryConditions.isEMI()).thenReturn(false);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the lightening and test forest hexes.
        when(mockWeather.isLightningStorm()).thenReturn(false);
        when(mockLos.getLightWoods()).thenReturn(3); // Should have no effect
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Heavy Woods/Jungle should remove 1 hex per bracket per hex
        when(mockLos.getHeavyWoods()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Ultraheavy Woods/Jungle should remove 2 hex per bracket per hex
        when(mockLos.getUltraWoods()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getLightWoods()).thenReturn(0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(0);

        // Water time - LOS blocked by water only works for sensors if the sensor is magscan
        // or the attacking entity is a naval vessel.
        when(mockLos.isBlockedByWater()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // If the attacker is a naval vessel it should be able to see:
        when(mockAttackingEntity.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.isBlockedByWater()).thenReturn(false);
        when(mockAttackingEntity.getMovementMode()).thenReturn(null);

        // Some sensors are effected by the target or tile's heat
        mockTarget.heat = 15;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.heat = 0;

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(false);

        // Some sensors are effected by the target's weight
        when(mockTarget.getWeight()).thenReturn(19.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(20.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(79.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(80.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(100.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(101.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(1000.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(1000.1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(0.0);

        // Industrial terrain blocks some sensors
        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(false);

        // Moving effects some sensors detection
        mockTarget.mpUsed = 0;
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 1;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getElevation()).thenReturn(1);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 1;
        when(mockTarget.getElevation()).thenReturn(0);
    }

    @ParameterizedTest
    @FieldSource(value = "activeProbeSensorTypes")
    void testGetActiveProbeSensorRangeByBracket(int sensorType) {

        // Mock Player
        Player mockPlayer = mock(Player.class);

        // Mock the board
        Board mockBoard = mock(Board.class);
        when(mockBoard.inSpace()).thenReturn(false);

        // Mock Options
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        // Mock weather
        Weather mockWeather = mock(Weather.class);

        // Mock Planetary Conditions
        PlanetaryConditions mockPlanetaryConditions = mock(PlanetaryConditions.class); //new PlanetaryConditions();
        when(mockPlanetaryConditions.getWeather()).thenReturn(mockWeather);


        Game mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);


        LosEffects mockLos = mock(LosEffects.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getPlayer(anyInt())).thenReturn(mockPlayer);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPlanetaryConditions);

        Hex mockTargetHex = mock(Hex.class);

        // Mock Sensor
        Sensor activeSensor = new Sensor(sensorType);

        // Expected Sensor Range - Normal Conditions, should be full range
        int expectedValue = activeSensor.getRangeByBracket();

        Entity mockAttackingEntity = mock(Entity.class);
        when(mockAttackingEntity.getPosition()).thenReturn(new Coords(0, 0));
        when(mockAttackingEntity.getActiveSensor()).thenReturn(activeSensor);
        when(mockAttackingEntity.hasBAP(anyBoolean())).thenReturn(true);

        // Let's put the enemy at our max range
        Entity mockTarget = mock(Entity.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(0, expectedValue));


        when(mockBoard.getHex(mockTarget.getPosition())).thenReturn(mockTargetHex);

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a hill in the way.
        when(mockLos.isBlockedByHill()).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's get rid of the hill. Let's put a building in the way instead. .
        when(mockLos.isBlockedByHill()).thenReturn(false);
        when(mockLos.getHardBuildings()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's put a different kind of building in the way. This should still block radar.
        when(mockLos.getHardBuildings()).thenReturn(0);
        when(mockLos.getSoftBuildings()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the building and test planetary conditions. EMI should reduce the range by 4.
        when(mockLos.getSoftBuildings()).thenReturn(0);
        when(mockPlanetaryConditions.isEMI()).thenReturn(true);
        when(mockAttackingEntity.hasBAP(anyBoolean())).thenReturn(false); // BAPs are disabled under EMI
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's add a lightening storm (-1 range). This should stack with EMI.
        when(mockWeather.isLightningStorm()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the EMI and test lightening on its own.
        when(mockAttackingEntity.hasBAP(anyBoolean())).thenReturn(true); // BAPs are disabled under EMI
        when(mockPlanetaryConditions.isEMI()).thenReturn(false);

        assertEquals(Math.max(0, expectedValue-1), Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Let's remove the lightening and test forest hexes.
        when(mockWeather.isLightningStorm()).thenReturn(false);
        when(mockLos.getLightWoods()).thenReturn(3); // Should have no effect
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Heavy Woods/Jungle
        when(mockLos.getHeavyWoods()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // Ultraheavy Woods/Jungle
        when(mockLos.getUltraWoods()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(2);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getHeavyWoods()).thenReturn(0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getLightWoods()).thenReturn(0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.getUltraWoods()).thenReturn(0);

        // Water time - LOS blocked by water only works for sensors if the sensor is magscan
        // or the attacking entity is a naval vessel.
        when(mockLos.isBlockedByWater()).thenReturn(true);
        assertEquals(0, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        // If the attacker is a naval vessel it should be able to see:
        when(mockAttackingEntity.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockLos.isBlockedByWater()).thenReturn(false);
        when(mockAttackingEntity.getMovementMode()).thenReturn(null);

        // Some sensors are effected by the target or tile's heat
        mockTarget.heat = 15;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.heat = 0;

        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.FIRE)).thenReturn(false);

        // Some sensors are effected by the target's weight
        when(mockTarget.getWeight()).thenReturn(19.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(20.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(79.9);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(80.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(100.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(101.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(1000.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));
        when(mockTarget.getWeight()).thenReturn(1001.0);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getWeight()).thenReturn(50.0);

        // Industrial terrain blocks some sensors
        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(true);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTargetHex.containsTerrain(Terrains.INDUSTRIAL)).thenReturn(false);

        // Moving effects some sensors detection
        mockTarget.mpUsed = 0;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 1;
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        when(mockTarget.getElevation()).thenReturn(1);
        assertEquals(expectedValue, Compute.getSensorRangeByBracket(mockGame, mockAttackingEntity, mockTarget, mockLos));

        mockTarget.mpUsed = 0;
        when(mockTarget.getElevation()).thenReturn(0);
    }


    // End tests for Compute.getSensorRangeByBracket(Game, Entity, Targetable, LosEffects)
}
