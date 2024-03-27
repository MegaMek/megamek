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
import megamek.common.planetaryconditions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlanetaryConditionsDeserializer extends StdDeserializer<PlanetaryConditions> {

    static final String TEMPERATURE = "temperature";
    static final String PRESSURE = "pressure";
    static final String GRAVITY = "gravity";
    static final String EMI_TEXT = "emi";
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

    static final Map<String, Atmosphere> PRESSURE_VALUES = Map.of("vacuum", Atmosphere.VACUUM,
            "trace", Atmosphere.TRACE, "thin", Atmosphere.THIN,
            "standard", Atmosphere.STANDARD, "high", Atmosphere.HIGH,
            "very high", Atmosphere.VERY_HIGH);

    static final Map<String, Fog> FOG_VALUES = Map.of("none", Fog.FOG_NONE,
            "light", Fog.FOG_LIGHT, "heavy", Fog.FOG_HEAVY);

    static final Map<String, Light> LIGHT_VALUES = Map.of("none", Light.DAY,
            "dusk", Light.DUSK, "full moon", Light.FULL_MOON,
            "moonless", Light.MOONLESS, "pitchblack", Light.PITCH_BLACK);

    static final Map<String, Wind> WIND_VALUES = Map.of("none", Wind.CALM,
            "light gale", Wind.LIGHT_GALE, "moderate gale", Wind.MOD_GALE,
            "strong gale", Wind.STRONG_GALE, "storm", Wind.STORM,
            "tornado", Wind.TORNADO_F1_TO_F3, "tornado f4", Wind.TORNADO_F4);

    static final Map<String, WindDirection> DIR_VALUES = Map.of("random", WindDirection.RANDOM,
            "N", WindDirection.NORTH, "NE", WindDirection.NORTHEAST, "SE", WindDirection.SOUTHEAST,
            "S", WindDirection.SOUTH, "SW", WindDirection.SOUTHWEST, "NW", WindDirection.NORTHWEST);

    static final Map<String, Weather> WEATHER_VALUES = new HashMap<>();

    static {
        WEATHER_VALUES.put("none", Weather.CLEAR);
        WEATHER_VALUES.put("light rain", Weather.LIGHT_RAIN);
        WEATHER_VALUES.put("moderate rain", Weather.MOD_RAIN);
        WEATHER_VALUES.put("heavy rain", Weather.HEAVY_RAIN);
        WEATHER_VALUES.put("gusting rain", Weather.GUSTING_RAIN);
        WEATHER_VALUES.put("downpour", Weather.DOWNPOUR);
        WEATHER_VALUES.put("light snow", Weather.LIGHT_SNOW);
        WEATHER_VALUES.put("moderate snow", Weather.MOD_SNOW);
        WEATHER_VALUES.put("snow flurries", Weather.SNOW_FLURRIES);
        WEATHER_VALUES.put("heavy snow", Weather.HEAVY_SNOW);
        WEATHER_VALUES.put("sleet", Weather.SLEET);
        WEATHER_VALUES.put("ice storm", Weather.ICE_STORM);
        WEATHER_VALUES.put("light hail", Weather.LIGHT_HAIL);
        WEATHER_VALUES.put("heavy hail", Weather.HEAVY_HAIL);
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
        if (node.has(EMI_TEXT)) {
            result.setEMI(node.get(EMI_TEXT).booleanValue() ? EMI.EMI : EMI.EMI_NONE);
        }
        if (node.has(BLOWING_SAND)) {
            result.setBlowingSand(node.get(BLOWING_SAND).booleanValue() ?
                    BlowingSand.BLOWING_SAND : BlowingSand.BLOWING_SAND_NONE);
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
                result.setWind(WIND_VALUES.get(windNode.get(WIND_STRENGTH).textValue()));
            }
            if (windNode.has(WIND_MIN)) {
                result.setWindMin(WIND_VALUES.get(windNode.get(WIND_MIN).textValue()));
            }
            if (windNode.has(WIND_MAX)) {
                result.setWindMax(WIND_VALUES.get(windNode.get(WIND_MAX).textValue()));
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