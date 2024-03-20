/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlanetaryConditionsDeserializer extends StdDeserializer<PlanetaryConditions> {

    static final String TEMPERATURE = "temperature";
    static final String PRESSURE = "pressure";
    static final String GRAVITY = "gravity";
    static final String EMI = "emi";
    static final String BLOWING_SAND = "blowingsand";
    static final String ALLOW_TERRAIN_CHANGES = "terrainchanges";
    static final String WEATHER = "weather";
    static final String LIGHT = "light";
    static final String WIND = "wind";
    static final String WIND_STRENGTH = "strength";
    static final String WIND_DIRECTION = "direction";
    static final String WIND_SHIFTING = "shifting";
    static final String FOG = "fog";
    static final String WIND_MIN = "minimum";
    static final String WIND_MAX = "maximum";

    static final Map<String, Integer> PRESSURE_VALUES = Map.of("vacuum", PlanetaryConditions.ATMO_VACUUM,
            "trace", PlanetaryConditions.ATMO_TRACE, "thin", PlanetaryConditions.ATMO_THIN,
            "standard", PlanetaryConditions.ATMO_STANDARD, "high", PlanetaryConditions.ATMO_HIGH,
            "very high", PlanetaryConditions.ATMO_VHIGH);

    static final Map<String, Integer> FOG_VALUES = Map.of("none", PlanetaryConditions.FOG_NONE,
            "light", PlanetaryConditions.FOG_LIGHT, "heavy", PlanetaryConditions.FOG_HEAVY);

    static final Map<String, Integer> LIGHT_VALUES = Map.of("none", PlanetaryConditions.L_DAY,
            "dusk", PlanetaryConditions.L_DUSK, "full moon", PlanetaryConditions.L_FULL_MOON,
            "moonless", PlanetaryConditions.L_MOONLESS, "pitchblack", PlanetaryConditions.L_PITCH_BLACK);

    static final Map<String, Integer> WIND_VALUES = Map.of("none", PlanetaryConditions.WI_NONE,
            "light gale", PlanetaryConditions.WI_LIGHT_GALE, "moderate gale", PlanetaryConditions.WI_MOD_GALE,
            "strong gale", PlanetaryConditions.WI_STRONG_GALE, "storm", PlanetaryConditions.WI_STORM,
            "tornado", PlanetaryConditions.WI_TORNADO_F13, "tornado f4", PlanetaryConditions.WI_TORNADO_F4);

    static final Map<String, Integer> DIR_VALUES = Map.of("random", PlanetaryConditions.DIR_RANDOM,
            "N", 0, "NE", 1, "SE", 2, "S", 3, "SW", 4, "NW", 5);

    static final Map<String, Integer> WEATHER_VALUES = new HashMap<>();

    static {
        WEATHER_VALUES.put("none", PlanetaryConditions.WE_NONE);
        WEATHER_VALUES.put("light rain", PlanetaryConditions.WE_LIGHT_RAIN);
        WEATHER_VALUES.put("moderate rain", PlanetaryConditions.WE_MOD_RAIN);
        WEATHER_VALUES.put("heavy rain", PlanetaryConditions.WE_HEAVY_RAIN);
        WEATHER_VALUES.put("gusting rain", PlanetaryConditions.WE_GUSTING_RAIN);
        WEATHER_VALUES.put("downpour", PlanetaryConditions.WE_DOWNPOUR);
        WEATHER_VALUES.put("light snow", PlanetaryConditions.WE_LIGHT_SNOW);
        WEATHER_VALUES.put("moderate snow", PlanetaryConditions.WE_MOD_SNOW);
        WEATHER_VALUES.put("snow flurries", PlanetaryConditions.WE_SNOW_FLURRIES);
        WEATHER_VALUES.put("heavy snow", PlanetaryConditions.WE_HEAVY_SNOW);
        WEATHER_VALUES.put("sleet", PlanetaryConditions.WE_SLEET);
        WEATHER_VALUES.put("ice storm", PlanetaryConditions.WE_ICE_STORM);
        WEATHER_VALUES.put("light hail", PlanetaryConditions.WE_LIGHT_HAIL);
        WEATHER_VALUES.put("heavy hail", PlanetaryConditions.WE_HEAVY_HAIL);
    }

    public PlanetaryConditionsDeserializer() {
        this(null);
    }

    public PlanetaryConditionsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PlanetaryConditions deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        PlanetaryConditions result = new PlanetaryConditions();
        if (node.has(TEMPERATURE)) {
            result.setTemperature(node.get(TEMPERATURE).intValue());
        }
        if (node.has(GRAVITY)) {
            result.setGravity(node.get(GRAVITY).floatValue());
        }
        if (node.has(PRESSURE)) {
            result.setAtmosphere(0);
        }
        if (node.has(EMI)) {
            result.setEMI(node.get(EMI).booleanValue());
        }
        if (node.has(BLOWING_SAND)) {
            result.setBlowingSand(node.get(BLOWING_SAND).booleanValue());
        }
        if (node.has(ALLOW_TERRAIN_CHANGES)) {
            result.setTerrainAffected(node.get(ALLOW_TERRAIN_CHANGES).booleanValue());
        }
        if (node.has(PRESSURE)) {
            result.setAtmosphere(PRESSURE_VALUES.get(node.get(PRESSURE).textValue()));
        }
        if (node.has(FOG)) {
            result.setFog(FOG_VALUES.get(node.get(FOG).textValue()));
        }
        if (node.has(LIGHT)) {
            result.setLight(LIGHT_VALUES.get(node.get(LIGHT).textValue()));
        }
        if (node.has(WIND)) {
            JsonNode windNode = node.get(WIND);
            if (windNode.has(WIND_STRENGTH)) {
                result.setWindStrength(WIND_VALUES.get(windNode.get(WIND_STRENGTH).textValue()));
            }
            if (windNode.has(WIND_MIN)) {
                result.setMinWindStrength(WIND_VALUES.get(windNode.get(WIND_MIN).textValue()));
            }
            if (windNode.has(WIND_MAX)) {
                result.setMaxWindStrength(WIND_VALUES.get(windNode.get(WIND_MAX).textValue()));
            }
            if (windNode.has(WIND_SHIFTING)) {
                result.setShiftingWindDirection(windNode.get(WIND_SHIFTING).booleanValue());
            }
            if (windNode.has(WIND_DIRECTION)) {
                result.setWindDirection(DIR_VALUES.get(windNode.get(WIND_DIRECTION).textValue()));
            }
        }
        if (node.has(WEATHER)) {
            result.setWeather(WEATHER_VALUES.get(node.get(WEATHER).textValue()));
        }
        return result;
    }
}