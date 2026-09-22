/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.BlowingSand;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;

/** Complete visual previews of actual planetary-condition combinations, using the scenario mapper. */
enum AtmospherePreset {
    CLEAR("Clear day"), DAWN("Dawn"), DUSK("Dusk"),
    FULL_MOON("Full moon"), MOONLESS("Moonless"), PITCH_BLACK("Pitch black"),
    LIGHT_FOG("Light fog"), HEAVY_FOG("Heavy fog"), RAIN_STORM("Rainstorm"),
    SNOW_STORM("Snowstorm"), SAND_STORM("Sandstorm"), LUNAR("Lunar day");

    final String label;

    AtmospherePreset(String label) {
        this.label = label;
    }

    /** Fresh, locally owned conditions: presets never read or modify the mutable game state. */
    PlanetaryConditions conditions() {
        var conditions = new PlanetaryConditions();
        conditions.setWindDirection(WindDirection.SOUTH);
        switch (this) {
            case CLEAR -> { }
            case DAWN, DUSK -> conditions.setLight(Light.DUSK_DAWN);
            case FULL_MOON -> conditions.setLight(Light.FULL_MOON);
            case MOONLESS -> conditions.setLight(Light.MOONLESS);
            case PITCH_BLACK -> conditions.setLight(Light.PITCH_BLACK);
            case LIGHT_FOG -> conditions.setFog(Fog.FOG_LIGHT);
            case HEAVY_FOG -> conditions.setFog(Fog.FOG_HEAVY);
            case RAIN_STORM -> conditions.setWeather(Weather.LIGHTNING_STORM);
            case SNOW_STORM -> {
                conditions.setWeather(Weather.HEAVY_SNOW);
                conditions.setWind(Wind.STORM);
            }
            case SAND_STORM -> {
                conditions.setBlowingSand(BlowingSand.BLOWING_SAND);
                conditions.setWind(Wind.STORM);
                conditions.setTemperature(40);
            }
            case LUNAR -> {
                conditions.setAtmosphere(Atmosphere.VACUUM);
                conditions.setGravity(0.16f);
            }
        }
        conditions.setTemperature(PlanetaryConditions.setTempFromWeather(conditions.getWeather(), conditions.getTemperature()));
        conditions.setWind(PlanetaryConditions.setWindFromWeather(conditions.getWeather(), conditions.getWind()));
        conditions.setWind(PlanetaryConditions.setWindFromBlowingSand(conditions.getBlowingSand(), conditions.getWind()));
        return conditions;
    }

    BoardAtmosphere.Settings settings(double timeSample) {
        return BoardAtmosphere.fromScenario(conditions(), false, timeSample(timeSample));
    }

    double timeSample(double timeSample) {
        // Reuse the window's sample, restricting dawn/dusk to the requested half of that scenario window.
        return switch (this) {
            case DAWN -> timeSample * 0.5;
            case DUSK -> Math.min(Math.nextDown(1.0), 0.5 + timeSample * 0.5);
            default -> timeSample;
        };
    }

    String description() {
        var conditions = conditions();
        return "Preview " + conditions.getLight() + ", " + conditions.getWeather() + ", " + conditions.getFog()
              + ", " + conditions.getWind() + ", " + conditions.getAtmosphere() + ", " + conditions.getTemperature()
              + " C, " + conditions.getBlowingSand() + ", " + conditions.getGravity()
              + " g. Resets atmosphere effects to their defaults; game conditions stay unchanged.";
    }
}
