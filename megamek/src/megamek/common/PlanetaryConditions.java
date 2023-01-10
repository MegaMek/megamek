/*
* MegaMek -
* Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;

/**
 * This class will hold all the information on planetary conditions and a variety of helper functions
 * for those conditions
 */
public class PlanetaryConditions implements Serializable {

    private static final long serialVersionUID = 6838624193286089781L;

    // light
    public static final int L_DAY          = 0;
    public static final int L_DUSK         = 1;
    public static final int L_FULL_MOON    = 2;
    public static final int L_MOONLESS     = 3;
    public static final int L_PITCH_BLACK  = 4;
    private String msg_name_light_daylight = Messages.getString("PlanetaryConditions.DisplayableName.Light.Daylight");
    private String msg_name_light_dusk = Messages.getString("PlanetaryConditions.DisplayableName.Light.Dusk");
    private String msg_name_light_fullmoonnight = Messages.getString("PlanetaryConditions.DisplayableName.Light.Full Moon Night");
    private String msg_name_light_moonlessnight = Messages.getString("PlanetaryConditions.DisplayableName.Light.Moonless Night");
    private String msg_name_light_pitchblack = Messages.getString("PlanetaryConditions.DisplayableName.Light.Pitch Black");
    private String[] lightNames = { msg_name_light_daylight, msg_name_light_dusk, msg_name_light_fullmoonnight,
            msg_name_light_moonlessnight, msg_name_light_pitchblack };
    public  int l_size = lightNames.length;
    private String msg_indicator_light_day = Messages.getString("PlanetaryConditions.Indicator.Light.Day");
    private String msg_indicator_light_dusk = Messages.getString("PlanetaryConditions.Indicator.Light.Dusk");
    private String msg_indicator_light_full_moon = Messages.getString("PlanetaryConditions.Indicator.Light.FullMoon");
    private String msg_indicator_light_moonless = Messages.getString("PlanetaryConditions.Indicator.Light.Moonless");
    private String msg_indicator_light_pitch_black = Messages.getString("PlanetaryConditions.Indicator.Light.PitchBlack");
    private String[] lightIndicators = { msg_indicator_light_day, msg_indicator_light_dusk, msg_indicator_light_full_moon,
            msg_indicator_light_moonless, msg_indicator_light_pitch_black };

    // Weather
    public static final int WE_NONE             = 0;
    public static final int WE_LIGHT_RAIN       = 1;
    public static final int WE_MOD_RAIN         = 2;
    public static final int WE_HEAVY_RAIN       = 3;
    public static final int WE_GUSTING_RAIN     = 4;
    public static final int WE_DOWNPOUR         = 5;
    public static final int WE_LIGHT_SNOW       = 6;
    public static final int WE_MOD_SNOW         = 7;
    public static final int WE_SNOW_FLURRIES    = 8;
    public static final int WE_HEAVY_SNOW       = 9;
    public static final int WE_SLEET            = 10;
    public static final int WE_ICE_STORM        = 11;
    public static final int WE_LIGHT_HAIL       = 12;// NYI
    public static final int WE_HEAVY_HAIL       = 13;// NYI
    public static final int WE_LIGHTNING_STORM  = 14;// NYI
    //  public static final int WE_BLIZZARD         = 11; does not exist anymore
    private String msg_name_weather_clear = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Clear");
    private String msg_name_weather_lightrain = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Light Rain");
    private String msg_name_weather_modrain = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Moderate Rain");
    private String msg_name_weather_heavygrain = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Heavy Rain");
    private String msg_name_weather_gustingrain = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Gusting Rain");
    private String msg_name_weather_torrdownpour = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Torrential Downpour");
    private String msg_name_weather_lightsnowfall = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Light Snowfall");
    private String msg_name_weather_modsnowfall = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Moderate Snowfall");
    private String msg_name_weather_snowfluffies = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Snow Flurries");
    private String msg_name_weather_heavysnowfall = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Heavy Snowfall");
    private String msg_name_weather_sleet = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Sleet");
    private String msg_name_weather_icestorm = Messages.getString("PlanetaryConditions.DisplayableName.Weather.Ice Storm");
    private String[] weatherNames = { msg_name_weather_clear, msg_name_weather_lightrain, msg_name_weather_modrain,
            msg_name_weather_heavygrain, msg_name_weather_gustingrain, msg_name_weather_torrdownpour,
            msg_name_weather_lightsnowfall, msg_name_weather_modsnowfall, msg_name_weather_snowfluffies,
            msg_name_weather_heavysnowfall, msg_name_weather_sleet, msg_name_weather_icestorm };//, "Light Hail", "Heavy Hail", "Lightning Storm" };
    public int we_size = weatherNames.length;
    private String msg_indicator_weather_none = Messages.getString("PlanetaryConditions.Indicator.Weather.None");
    private String msg_indicator_weather_light_rain = Messages.getString("PlanetaryConditions.Indicator.Weather.LightRain");
    private String msg_indicator_weather_mod_rain = Messages.getString("PlanetaryConditions.Indicator.Weather.ModRain");
    private String msg_indicator_weather_heavy_rain = Messages.getString("PlanetaryConditions.Indicator.Weather.HeavyRain");
    private String msg_indicator_weather_gusting_rain = Messages.getString("PlanetaryConditions.Indicator.Weather.GustingRain");
    private String msg_indicator_weather_downpour = Messages.getString("PlanetaryConditions.Indicator.Weather.Downpour");
    private String msg_indicator_weather_light_snow = Messages.getString("PlanetaryConditions.Indicator.Weather.LightSnow");
    private String msg_indicator_weather_mod_snow = Messages.getString("PlanetaryConditions.Indicator.Weather.ModSnow");
    private String msg_indicator_weather_snow_flurries = Messages.getString("PlanetaryConditions.Indicator.Weather.SnowFlurries");
    private String msg_indicator_weather_heavy_snow = Messages.getString("PlanetaryConditions.Indicator.Weather.HeavySnow");
    private String msg_indicator_weather_sleet = Messages.getString("PlanetaryConditions.Indicator.Weather.Sleet");
    private String msg_indicator_weather_ice_storm = Messages.getString("PlanetaryConditions.Indicator.Weather.IceStorm");
    private String[] weatherIndicators = { msg_indicator_weather_none, msg_indicator_weather_light_rain, msg_indicator_weather_mod_rain,
            msg_indicator_weather_heavy_rain, msg_indicator_weather_gusting_rain, msg_indicator_weather_downpour,
            msg_indicator_weather_light_snow, msg_indicator_weather_mod_snow,  msg_indicator_weather_snow_flurries,
            msg_indicator_weather_heavy_snow, msg_indicator_weather_sleet, msg_indicator_weather_ice_storm};

    // Wind
    public static final int WI_NONE        = 0;
    public static final int WI_LIGHT_GALE  = 1;
    public static final int WI_MOD_GALE    = 2;
    public static final int WI_STRONG_GALE = 3;
    public static final int WI_STORM       = 4;
    public static final int WI_TORNADO_F13 = 5;
    public static final int WI_TORNADO_F4  = 6;
    private String msg_name_windstrength_light_calm = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Calm");
    private String msg_name_windstrength_light_gale = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Light Gale");
    private String msg_name_windstrength_mod_gale = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Moderate Gale");
    private String msg_name_windstrength_strong_gale = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Strong Gale");
    private String msg_name_windstrength_storm = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Storm");
    private String msg_name_windstrength_tornado_f13 = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Tornado F1-F3");
    private String msg_name_windstrength_tornado_f4 = Messages.getString("PlanetaryConditions.DisplayableName.WindStrength.Tornado F4");
    private String[] windNames = { msg_name_windstrength_light_calm, msg_name_windstrength_light_gale,
            msg_name_windstrength_mod_gale, msg_name_windstrength_strong_gale,
            msg_name_windstrength_storm, msg_name_windstrength_tornado_f13,
            msg_name_windstrength_tornado_f4 };
    public int wi_size = windNames.length;
    private String msg_indicator_windstrength_light_calm = Messages.getString("PlanetaryConditions.Indicator.WindStrength.Calm");
    private String msg_indicator_windstrength_light_gale = Messages.getString("PlanetaryConditions.Indicator.WindStrength.LightGale");
    private String msg_indicator_windstrength_mod_gale = Messages.getString("PlanetaryConditions.Indicator.WindStrength.ModGale");
    private String msg_indicator_windstrength_strong_gale = Messages.getString("PlanetaryConditions.Indicator.WindStrength.StrongGale");
    private String msg_indicator_windstrength_storm = Messages.getString("PlanetaryConditions.Indicator.WindStrength.Storm");
    private String msg_indicator_windstrength_tornado_f13 = Messages.getString("PlanetaryConditions.Indicator.WindStrength.TornadoF13");
    private String msg_indicator_windstrength_tornado_f4 = Messages.getString("PlanetaryConditions.Indicator.WindStrength.TornadoF4");
    private String[] windStrengthIndicators = { msg_indicator_windstrength_light_calm, msg_indicator_windstrength_light_gale,
            msg_indicator_windstrength_mod_gale, msg_indicator_windstrength_strong_gale,
            msg_indicator_windstrength_storm, msg_indicator_windstrength_tornado_f13,
            msg_indicator_windstrength_tornado_f4 };

    // wind direction
    private String msg_name_winddirection_north = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.North");
    private String msg_name_winddirection_northeast = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Northeast");
    private String msg_name_winddirection_southeast = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Southeast");
    private String msg_name_winddirection_south = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.South");
    private String msg_name_winddirection_southwest = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Southwest");
    private String msg_name_winddirection_northwest = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Northwest");
    private String msg_name_winddirection_random = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.RandomWindDirection");
    // no east and west, because the map uses 6 side hex tiles.  east and west are skipped.
    private String[] dirNames = { msg_name_winddirection_north, msg_name_winddirection_northeast,
            msg_name_winddirection_southeast, msg_name_winddirection_south, msg_name_winddirection_southwest,
            msg_name_winddirection_northwest, msg_name_winddirection_random };
    public int dir_size = dirNames.length;
    public static final int DIR_RANDOM = 6;
    private String msg_indicator_winddirection_north = Messages.getString("PlanetaryConditions.Indicator.WindDirection.North");
    private String msg_indicator_winddirection_northeast = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Northeast");
    private String msg_indicator_winddirection_southeast = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Southeast");
    private String msg_indicator_winddirection_south = Messages.getString("PlanetaryConditions.Indicator.WindDirection.South");
    private String msg_indicator_winddirection_southwest = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Southwest");
    private String msg_indicator_winddirection_northwest = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Northwest");
    private String msg_indicator_winddirection_random = Messages.getString("PlanetaryConditions.Indicator.WindDirection.RandomWindDirection");
    // no east and west, because the map uses 6 side hex tiles.  east and west are skipped.
    private String[] windDirectionIndicators = { msg_indicator_winddirection_north, msg_indicator_winddirection_northeast,
            msg_indicator_winddirection_southeast,  msg_indicator_winddirection_south, msg_indicator_winddirection_southwest,
            msg_indicator_winddirection_northwest, msg_indicator_winddirection_random };

    // atmospheric pressure
    public static final int ATMO_VACUUM   = 0;
    public static final int ATMO_TRACE    = 1;
    public static final int ATMO_THIN     = 2;
    public static final int ATMO_STANDARD = 3;
    public static final int ATMO_HIGH     = 4;
    public static final int ATMO_VHIGH    = 5;
    private String msg_name_atmosphere_vacuum = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Vacuum");
    private String msg_name_atmosphere_trace = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Trace");
    private String msg_name_atmosphere_thin = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Thin");
    private String msg_name_atmosphere_standard = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Standard");
    private String msg_name_atmosphere_high = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.High");
    private String msg_name_atmosphere_vhigh = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Very High");
    public String[] atmoNames = { msg_name_atmosphere_vacuum, msg_name_atmosphere_trace, msg_name_atmosphere_thin,
            msg_name_atmosphere_standard, msg_name_atmosphere_high, msg_name_atmosphere_vhigh };
    public int atmo_size = atmoNames.length;
    private String msg_indicator_atmosphere_vacuum = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Vacuum");
    private String msg_indicator_atmosphere_trace = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Trace");
    private String msg_indicator_atmosphere_thin = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Thin");
    private String msg_indicator_atmosphere_standard = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Standard");
    private String msg_indicator_atmosphere_high = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.High");
    private String msg_indicator_atmosphere_vhigh = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.VHigh");
    private String[] atmosphereIndicators = { msg_indicator_atmosphere_vacuum, msg_indicator_atmosphere_trace, msg_indicator_atmosphere_thin,
            msg_indicator_atmosphere_standard ,msg_indicator_atmosphere_high ,msg_indicator_atmosphere_vhigh };

    // fog
    public static final int FOG_NONE  = 0;
    public static final int FOG_LIGHT = 1;
    public static final int FOG_HEAVY = 2;
    private String msg_name_fog_none = Messages.getString("PlanetaryConditions.DisplayableName.Fog.None");
    private String msg_name_fog_light = Messages.getString("PlanetaryConditions.DisplayableName.Fog.Light Fog");
    private String msg_name_fog_heavy = Messages.getString("PlanetaryConditions.DisplayableName.Fog.Heavy Fog");
    private String[] fogNames = { msg_name_fog_none, msg_name_fog_light, msg_name_fog_heavy };
    public int fog_size = fogNames.length;
    private String msg_indicator_fog_none = Messages.getString("PlanetaryConditions.Indicator.Fog.None");
    private String msg_indicator_fog_light = Messages.getString("PlanetaryConditions.Indicator.Fog.Light");
    private String msg_indicator_fog_heavy = Messages.getString("PlanetaryConditions.Indicator.Fog.Heavy");
    private String[] fogIndicators = { msg_indicator_fog_none, msg_indicator_fog_light, msg_indicator_fog_heavy };

    // misc
    private boolean blowingSand = false;
    private String msg_name_blowingsand_true = Messages.getString("PlanetaryConditions.DisplayableName.SandBlowing.true");
    private String msg_name_blowingsand_false = Messages.getString("PlanetaryConditions.DisplayableName.SandBlowing.false");
    private String msg_indicator_blowingsand_true = Messages.getString("PlanetaryConditions.Indicator.SandBlowing.true");
    private String msg_indicator_blowingsand_false = Messages.getString("PlanetaryConditions.Indicator.SandBlowing.false");

    private boolean sandStorm = false;
    private boolean runOnce = false;

    // set up the specific conditions
    private int lightConditions = L_DAY;
    private int weatherConditions = WE_NONE;
    private int oldWeatherConditions = WE_NONE;
    private int windStrength = WI_NONE;
    private int windDirection = DIR_RANDOM;
    private boolean shiftWindDirection = false;
    private boolean shiftWindStrength = false;
    private boolean isSleeting = false;
    private int atmosphere = ATMO_STANDARD;
    private int fog = FOG_NONE;
    private int temperature = 25;
    private int oldTemperature = 25;
    private String msg_name_temperature_cold = Messages.getString("PlanetaryConditions.DisplayableName.Temperature.ExtremeCold");
    private String msg_name_temperature_heat = Messages.getString("PlanetaryConditions.DisplayableName.Temperature.ExtremeHeat");
    private String msg_indicator_temperature_cold = Messages.getString("PlanetaryConditions.Indicator.Temperature.ExtremeCold");
    private String msg_indicator_temperature_heat= Messages.getString("PlanetaryConditions.Indicator.Temperature.ExtremeHeat");
    private String msg_indicator_temperature_normal = Messages.getString("PlanetaryConditions.Indicator.Temperature.Normal");
    private float gravity = (float) 1.0;
    private String msg_indicator_gravity_low= Messages.getString("PlanetaryConditions.Indicator.Gravity.Low");
    private String msg_indicator_gravity_normal = Messages.getString("PlanetaryConditions.Indicator.Gravity.Normal");
    private String msg_indicator_gravity_high = Messages.getString("PlanetaryConditions.Indicator.Gravity.High");
    private boolean emi = false;
    private String msg_name_emi_true = Messages.getString("PlanetaryConditions.DisplayableName.EMI.true");
    private String msg_name_emi_false = Messages.getString("PlanetaryConditions.DisplayableName.EMI.false");
    private String msg_indicator_emi_true = Messages.getString("PlanetaryConditions.Indicator.EMI.true");
    private String msg_indicator_emi_false = Messages.getString("PlanetaryConditions.Indicator.EMI.false");
    private boolean terrainAffected = true;
    private int maxWindStrength = PlanetaryConditions.WI_TORNADO_F4;
    private int minWindStrength = PlanetaryConditions.WI_NONE;

    /**
     * Constructor
     */
    public PlanetaryConditions() {

    }

    /** Creates new PlanetaryConditions that is a duplicate of another */
    public PlanetaryConditions(PlanetaryConditions other) {
        lightConditions = other.lightConditions;
        weatherConditions = other.weatherConditions;
        windStrength = other.windStrength;
        windDirection = other.windDirection;
        shiftWindDirection = other.shiftWindDirection;
        shiftWindStrength = other.shiftWindStrength;
        minWindStrength = other.minWindStrength;
        maxWindStrength = other.maxWindStrength;
        atmosphere = other.atmosphere;
        temperature = other.temperature;
        gravity = other.gravity;
        emi = other.emi;
        fog = other.fog;
        terrainAffected = other.terrainAffected;
        blowingSand = other.blowingSand;
        runOnce = other.runOnce;
    }

    /** clone! */
    @Override
    public Object clone() {
        return new PlanetaryConditions(this);
    }

    public String getLightDisplayableName(int type) {
        if ((type >= 0) && (type < l_size)) {
            return lightNames[type];
        }
        throw new IllegalArgumentException("Unknown light condition");
    }

    public String getWeatherDisplayableName(int type) {
        if ((type >= 0) && (type < we_size)) {
            return weatherNames[type];
        }
        throw new IllegalArgumentException("Unknown weather condition");
    }

    public String getTemperatureDisplayableName(int temp) {
        if (isExtremeTemperature(temp) && (temp > 0)) {
            return String.format("%d (%s)", temp, msg_name_temperature_heat);
        } else if (isExtremeTemperature(temp) && (temp <= 0)) {
            return String.format("%d (%s)", temp, msg_name_temperature_cold);
        } else {
            return String.valueOf(temp);
        }
    }

    public String getWindDirDisplayableName(int type) {
        if ((type >= 0) && (type < dir_size)) {
            return dirNames[type];
        }
        throw new IllegalArgumentException("Unknown wind direction");
    }

    public String getWindDisplayableName(int type) {
        if ((type >= 0) && (type < wi_size)) {
            return windNames[type];
        }
        throw new IllegalArgumentException("Unknown wind condition");
    }

    public String getAtmosphereDisplayableName(int type) {
        if ((type >= 0) && (type < atmo_size)) {
            return atmoNames[type];
        }
        throw new IllegalArgumentException("Unknown atmospheric pressure condition");
    }

    public String getFogDisplayableName(int type) {
        if ((type >= 0) && (type < fog_size)) {
            return fogNames[type];
        }
        throw new IllegalArgumentException("Unknown fog condition");
    }

    public String getWindDirDisplayableName() {
        return getWindDirDisplayableName(windDirection);
    }

    public String getLightDisplayableName() {
        return getLightDisplayableName(lightConditions);
    }

    public String getWeatherDisplayableName() {
        return getWeatherDisplayableName(weatherConditions);
    }

    public String getWindDisplayableName() {
        return getWindDisplayableName(windStrength);
    }

    public String getAtmosphereDisplayableName() {
        return getAtmosphereDisplayableName(atmosphere);
    }

    public String getFogDisplayableName() {
        return getFogDisplayableName(fog);
    }

    /**
     * to-hit penalty for light
     */
    public int getLightHitPenalty(boolean isWeapon) {
        int penalty = 0;
        if (isWeapon) {
            if (lightConditions == L_DUSK) {
                penalty = 1;
            } else if (lightConditions == L_FULL_MOON) {
                penalty = 2;
            } else if (lightConditions == L_MOONLESS) {
                penalty = 3;
            } else if (lightConditions == L_PITCH_BLACK) {
                penalty = 4;
            }
        } else {
            if (lightConditions == L_MOONLESS) {
                penalty = 1;
            } else if (lightConditions == L_PITCH_BLACK) {
                penalty = 2;
            }
        }

        return penalty;
    }
    
    /** 
     * Returns true when the light conditions give a hit penalty and
     * the hit penalty can be offset by a searchlight, i.e. in full moon,
     * moonless and pitch black night.
     */
    public boolean isSearchlightEffective() {
        return (lightConditions == L_FULL_MOON) || (lightConditions == L_MOONLESS)
                || (lightConditions == L_PITCH_BLACK);
    }

    /** Returns true when the given weather is prohibited for temperatures of 30 degC and more. */
    public static boolean requiresLowTemp(int weather) {
        return weather == WE_LIGHT_HAIL ||
                weather == WE_HEAVY_HAIL ||
                weather == WE_LIGHT_SNOW || 
                weather == WE_SLEET ||
                weather == WE_SNOW_FLURRIES ||
                weather == WE_HEAVY_SNOW ||
                weather == WE_ICE_STORM || 
                weather == WE_MOD_SNOW;
    }

    /**
     * heat bonus to hit for being overheated in darkness
     */
    public int getLightHeatBonus(int heat) {
        double divisor = 10000.0;
        if (lightConditions == L_DUSK) {
            divisor = 25.0;
        } else if (lightConditions == L_FULL_MOON) {
            divisor = 20.0;
        } else if (lightConditions == L_MOONLESS) {
            divisor = 15.0;
        } else if (lightConditions == L_PITCH_BLACK) {
            divisor = 10.0;
        }

        return (-1 * (int) Math.floor(heat / divisor));
    }

    /**
     * piloting penalty for running/flanking/etc for light
     */
    public int getLightPilotPenalty() {
        if (lightConditions == L_MOONLESS) {
            return 1;
        } else if (lightConditions == L_PITCH_BLACK) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * to-hit penalty for weather
     */
    public int getWeatherHitPenalty(Entity en) {
        if (((weatherConditions == WE_LIGHT_RAIN) || (weatherConditions == WE_LIGHT_SNOW))
                && en.isConventionalInfantry()) {
            return 1;
        } else if ((weatherConditions == WE_MOD_RAIN) || (weatherConditions == WE_HEAVY_RAIN)
                || (weatherConditions == WE_MOD_SNOW) || (weatherConditions == WE_HEAVY_SNOW)
                || (weatherConditions == WE_SLEET) || (weatherConditions == WE_GUSTING_RAIN)
                || (weatherConditions == WE_SNOW_FLURRIES)) {
            return 1;
        } else if (weatherConditions == WE_DOWNPOUR) {
            return 2;
        } else  {
            return 0;
        }
    }

    /**
     * piloting penalty for weather
     */
    public int getWeatherPilotPenalty() {
        if ((weatherConditions == WE_HEAVY_RAIN) || (weatherConditions == WE_HEAVY_SNOW)
                || (weatherConditions == WE_GUSTING_RAIN)) {
            return 1;
        } else if (weatherConditions == WE_DOWNPOUR) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * gravity penalties to PSRs
     * According to email from TPTB, you apply a penalty for every 0.5 gravities above or below 1 (rounding up)
     */
    public int getGravityPilotPenalty() {
        return (int) Math.floor(Math.abs(gravity - 1.0) / 0.5);
    }

    /**
     * piloting penalty for wind
     */
    public int getWindPilotPenalty(Entity en) {
        int penalty = 0;

        switch (windStrength) {
            case WI_MOD_GALE:
                if ((en instanceof VTOL) || (en.getMovementMode() == EntityMovementMode.WIGE)) {
                    penalty = 1;
                }
                break;
            case WI_STRONG_GALE:
                if ((en instanceof VTOL) || (en.getMovementMode() == EntityMovementMode.WIGE)
                        || (en.getMovementMode() == EntityMovementMode.HOVER)) {
                    penalty = 2;
                } else if ((en instanceof Mech) || (en.isAirborne())) {
                    penalty = 1;
                }
                break;
            case WI_STORM:
                if ((en instanceof VTOL) || (en instanceof Mech) || (en.getMovementMode() == EntityMovementMode.WIGE)
                        || (en.getMovementMode() == EntityMovementMode.HOVER)) {
                    penalty = 3;
                } else if (en.isAirborne()) {
                    penalty = 2;
                }
                break;
            case WI_TORNADO_F13:
                penalty = 3;
                break;
            case WI_TORNADO_F4:
                penalty = 5;
                break;
            default:
                break;
        }
        return penalty;
    }

    public void determineWind() {
        if (windDirection == DIR_RANDOM) {
            // Initial wind direction. If using level 2 rules, this
            // will be the wind direction for the whole battle.
            windDirection = Compute.d6(1) - 1;
        } else if (shiftWindDirection) {
            // Wind direction changes on a roll of 1 or 6
            switch (Compute.d6()) {
                case 1: // rotate clockwise
                    windDirection = (windDirection + 1) % 6;
                    break;
                case 6: // rotate counter-clockwise
                    windDirection = (windDirection + 5) % 6;
            }
        }
        if (shiftWindStrength) {
            // Wind strength changes on a roll of 1 or 6
            switch (Compute.d6()) {
                case 1: // weaker
                    windStrength = Math.max(minWindStrength, --windStrength);
                    doSleetCheck();
                    doSandStormCheck();
                    break;
                case 6: // stronger
                    windStrength = Math.min(maxWindStrength, ++windStrength);
                    doSleetCheck();
                    doSandStormCheck();
                    break;
            }
        }

        // atmospheric pressure may limit wind strength
        if ((atmosphere == ATMO_TRACE) && (windStrength > WI_STORM)) {
            windStrength = WI_STORM;
        }

        if ((atmosphere ==ATMO_THIN) && (windStrength > WI_TORNADO_F13)) {
            windStrength = WI_TORNADO_F13;
        }
    }

    /**
     * modifiers for fire ignition
     */
    public int getIgniteModifiers() {
        int mod = 0;

        if ((weatherConditions == WE_LIGHT_RAIN) || (weatherConditions == WE_MOD_RAIN)) {
            mod += 1;
        }

        if ((weatherConditions == WE_HEAVY_RAIN) || (weatherConditions == WE_DOWNPOUR)
                || (weatherConditions == WE_LIGHT_SNOW) || (weatherConditions == WE_MOD_SNOW)
                || (weatherConditions == WE_GUSTING_RAIN) || (weatherConditions == WE_SNOW_FLURRIES)) {
            mod += 2;
        }

        if ((weatherConditions == WE_HEAVY_SNOW)
                || (weatherConditions == WE_LIGHT_HAIL) || (weatherConditions == WE_HEAVY_HAIL)) {
            mod += 3;
        }

        if ((windStrength == WI_LIGHT_GALE) || (windStrength == WI_MOD_GALE)) {
            mod += 2;
        }

        if ((windStrength == WI_STRONG_GALE) || (windStrength == WI_STORM) || (weatherConditions == WE_ICE_STORM)) {
            mod += 4;
        }

        if (getTemperature() > 30) {
            mod -= getTemperatureDifference(30,-30);
        } else if (getTemperature() < 30) {
            mod += getTemperatureDifference(30, -30);
        }

        return mod;
    }

    /**
     * Do a roll for these weather conditions putting out fire
     * return boolean
     */
    public boolean putOutFire() {
        int roll = Compute.d6(2);
        switch (weatherConditions) {
            case WE_LIGHT_HAIL:
            case WE_LIGHT_RAIN:
            case WE_LIGHT_SNOW:
                roll = roll + 1;
                break;
            case WE_HEAVY_HAIL:
            case WE_MOD_RAIN:
            case WE_MOD_SNOW:
            case WE_SNOW_FLURRIES:
                roll = roll + 2;
                break;
            case WE_HEAVY_RAIN:
            case WE_GUSTING_RAIN:
            case WE_HEAVY_SNOW:
                roll = roll + 3;
                break;
            case WE_DOWNPOUR:
                roll = roll + 4;
                break;
            default:
                roll = -1;
                break;
        }

        return roll > 10;
    }

    /**
     * Returns how much higher or lower than a given range, divided by
     * ten, rounded up, the temperature is
     */
    public int getTemperatureDifference(int high, int low) {
        int i = 0;
        //if the low is more than the high, reverse
        if (low > high) {
            int tempLow = low;
            low = high;
            high = tempLow;

        }

        if ((getTemperature() >= low)
                && (getTemperature() <= high)) {
            return i;
        } else if (getTemperature() < low) {
            do {
                i++;
            } while ((getTemperature() + (i * 10)) < low);
            return i;
        } else {
            do {
                i++;
            } while ((getTemperature() - (i * 10)) > high);
        }
        return i;
    }

    /**
     *
     * @return a <code>String</code> with the reason why you cannot start a fire here
     */
    public String cannotStartFire() {
        if (atmosphere < ATMO_THIN) {
            return "atmosphere too thin";
        } else if (windStrength > WI_STORM) {
            return "a tornado";
        } else {
            return null;
        }
    }

    /**
     * Planetary conditions on movement, except for gravity
     * @param en - the entity in question
     * @return an <code>int</code> with the modifier to movement
     */
    public int getMovementMods(Entity en) {
        int mod = 0;

        // weather mods are calculated based on conditional effects ie extreme temperatures, wind

        // wind mods
        switch (windStrength) {
            case WI_LIGHT_GALE:
                if (!(en instanceof BattleArmor)
                        && ((en.getMovementMode() == EntityMovementMode.INF_LEG)
                                || (en.getMovementMode() == EntityMovementMode.INF_JUMP))) {
                    mod -= 1;
                }
                break;
            case WI_MOD_GALE:
                if (en.isConventionalInfantry()) {
                    mod -= 1;
                }
                break;
            case WI_STRONG_GALE:
            case WI_STORM:
                if (en instanceof BattleArmor) {
                    mod -= 1;
                } else if (en instanceof Infantry) {
                    mod -= 2;
                }
                break;
            case WI_TORNADO_F13:
                if (en.isAirborne()) {
                    mod -= 1;
                } else {
                    mod -= 2;
                }
                break;
        }

        // atmospheric pressure mods
        switch (atmosphere) {
            case ATMO_THIN:
                if (en.getMovementMode().isHoverVTOLOrWiGE()) {
                    mod -= 2;
                }
                break;
            case ATMO_HIGH:
            case ATMO_VHIGH:
                if (en.getMovementMode().isHoverVTOLOrWiGE()) {
                    mod += 1;
                }
                break;
        }

        // temperature difference
        if ((en instanceof Tank)
                || ((en instanceof Infantry) && !((Infantry) en).isXCT()) || (en instanceof Protomech)) {
            mod -= Math.abs(getTemperatureDifference(50,-30));
        }

        return mod;
    }

    /**
     * is the given entity type doomed in these conditions?
     * @return a string given the reason for being doomed, null if not doomed
     */
    public String whyDoomed(Entity en, Game game) {
        if ((atmosphere < ATMO_THIN) && en.doomedInVacuum()) {
            return "vacuum";
        }
        if ((windStrength == WI_TORNADO_F4) && !(en instanceof Mech)) {
            return "tornado";
        }
        if ((windStrength == WI_TORNADO_F13) && (en.isConventionalInfantry()
            || ((en.getMovementMode() == EntityMovementMode.HOVER)
            || (en.getMovementMode() == EntityMovementMode.WIGE)
            || (en.getMovementMode() == EntityMovementMode.VTOL)))) {
            return "tornado";
        }
        if ((windStrength == WI_STORM) && en.isConventionalInfantry()) {
            return "storm";
        }
        if (isExtremeTemperature() && en.doomedInExtremeTemp() && !Compute.isInBuilding(game, en)) {
            return "extreme temperature";
        }
        return null;
    }

    /**
     * visual range based on conditions
     *
     */
    public int getVisualRange(Entity en, boolean targetIlluminated) {

        boolean Spotlight = false;

        boolean isMechVee = false;
        boolean isLargeCraft = false;
        boolean isAero = false;

        // Needed for MekWars for Maximum Visual Range.
        if (en == null) {
            isMechVee = true;
            Spotlight = targetIlluminated;
        } else {
            Spotlight = en.isUsingSearchlight();
            isMechVee = (en instanceof Mech && !en.isAero()) || (en instanceof Tank);
            isLargeCraft = (en instanceof Dropship) || (en instanceof Jumpship);
            isAero = (en.isAero()) && !isLargeCraft;
        }
        // anything else is infantry
        
        // Beyond altitude 9, Aeros can't see. No need to repeat this test.
        if (isAero && (en.getAltitude() > 9)) {
            return 0;
        }
        
        // New rulings per v3.02 errata. Spotlights are easier, yay!
        // Illuminated?  Flat 45 hex distance
        if (targetIlluminated && (lightConditions > L_DAY)) {
            return 45;
        } else if (Spotlight && (lightConditions > L_DAY)) {
            // Using a searchlight?  Flat 30 hex range
            if (isMechVee || isAero || isLargeCraft) {
                return 30;
            }
            // Except infantry/handheld, 10 hexes
            return 10;
        } else if (lightConditions == L_PITCH_BLACK) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 3;
            }
            if (isAero) {
                return 5;
            }
            if (isLargeCraft) {
                return 4;
            }
            return 1;
        } else if ((lightConditions == L_MOONLESS)
                || (fog == FOG_HEAVY)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 5;
            }
            if (isAero) {
                return 10;
            }
            if (isLargeCraft) {
                return 8;
            }
            return 2;
        } else if ((weatherConditions == WE_HEAVY_HAIL)
                || (weatherConditions == WE_SLEET)
                || (weatherConditions == WE_HEAVY_SNOW)
                || (blowingSand && (windStrength >= WI_MOD_GALE))
                || (lightConditions == L_FULL_MOON)
                || (weatherConditions == WE_GUSTING_RAIN)
                || (weatherConditions == WE_ICE_STORM)
                || (weatherConditions == WE_DOWNPOUR)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 10;
            }
            if (isAero) {
                return 20;
            }
            if (isLargeCraft) {
                return 15;
            }
            return 5;
        } else if ((lightConditions == L_DUSK)
                || (weatherConditions == WE_HEAVY_RAIN)
                || (weatherConditions == WE_SNOW_FLURRIES)
                || (weatherConditions == WE_MOD_SNOW) && (windStrength >= WI_MOD_GALE)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 15;
            }
            if (isAero) {
                return 30;
            }
            if (isLargeCraft) {
                return 20;
            }
            return 8;
        } else if ((weatherConditions == WE_MOD_SNOW)
                || (weatherConditions == WE_MOD_RAIN)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 20;
            }
            if (isAero) {
                return 50;
            }
            if (isLargeCraft) {
                return 25;
            }
            return 10;
        } else if ((lightConditions > L_DAY)
                || (weatherConditions == WE_LIGHT_SNOW)
                || (weatherConditions == WE_LIGHT_RAIN)
                || (weatherConditions == WE_LIGHT_HAIL) 
                || (fog == FOG_LIGHT)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 30;
            }
            if (isAero) {
                return 60;
            }
            if (isLargeCraft) {
                return 35;
            }
            return 15;
        } else {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 60;
            }
            if (isAero) {
                return 120;
            }
            if (isLargeCraft) {
                return 70;
            }
            return 30;
        }
    }

    public int getDropRate() {
        // atmospheric pressure mods
        switch (atmosphere) {
            case ATMO_TRACE:
                return 8;
            case ATMO_THIN:
                return 5;
            case ATMO_HIGH:
                return 2;
            case ATMO_VHIGH:
                return 1;
            default:
                return 3;
        }
    }

    public void setLight(int type) {
        lightConditions = type;
    }


    /** @return The time of day lighting conditions (one of PlanetaryConditions.L_*). */
    public int getLight() {
        return lightConditions;
    }

    public void setWeather(int type) {
        if ((type < 0) || (type >= we_size)) {
            LogManager.getLogger().error(String.format("Invalid weather type supplied: %d", type));
        } else {
            weatherConditions = type;
        }
    }

    public int getWeather() {
        return weatherConditions;
    }

    public void setWindStrength(int type) {
        windStrength = type;
    }

    public int getWindStrength() {
        return windStrength;
    }

    public void setWindDirection(int type) {
        windDirection = type;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public void setShiftingWindDirection(boolean b) {
        shiftWindDirection = b;
    }

    public boolean shiftingWindDirection() {
        return shiftWindDirection;
    }

    public void setShiftingWindStrength(boolean b) {
        shiftWindStrength = b;
    }

    public boolean shiftingWindStrength() {
        return shiftWindStrength;
    }

    public void setAtmosphere(int a) {
        atmosphere = a;
    }

    public int getAtmosphere() {
        return atmosphere;
    }

    public void setTemperature(int tem) {
        temperature = tem;
    }

    public int getTemperature() {
        return temperature;
    }

    public boolean isVacuum() {
        return (atmosphere == ATMO_VACUUM) || (atmosphere == ATMO_TRACE);
    }

    public static boolean isExtremeTemperature(int temperature) {
        return (temperature > 50) || (temperature < -30);
    }

    public boolean isExtremeTemperature() {
        return isExtremeTemperature(temperature);
    }

    public void setGravity(float f) {
        gravity = f;
    }

    public float getGravity() {
        return gravity;
    }

    public void setEMI(boolean b) {
        emi = b;
    }

    public boolean hasEMI() {
        return emi;
    }

    public int getFog() {
        return fog;
    }

    public void setFog(int fog) {
        this.fog = fog;
    }

    public void setTerrainAffected(boolean b) {
        terrainAffected = b;
    }

    // can weather alter the terrain (add snow, mud, etc.)
    public boolean isTerrainAffected() {
        return terrainAffected;
    }

    public boolean isRecklessConditions() {
        return (fog > FOG_NONE) || (lightConditions > L_DUSK);
    }

    public int getMaxWindStrength() {
        return maxWindStrength;
    }

    public void setMaxWindStrength(int strength) {
        maxWindStrength = strength;
    }

    public int getMinWindStrength() {
        return minWindStrength;
    }

    public void setMinWindStrength(int strength) {
        minWindStrength = strength;
    }

    public boolean isSandBlowing() {
        return blowingSand;
    }

    public void setBlowingSand(boolean b) {
        blowingSand = b;
    }

    public void alterConditions(PlanetaryConditions conditions) {
        lightConditions = conditions.lightConditions;
        weatherConditions = conditions.weatherConditions;
        windStrength = conditions.windStrength;
        windDirection = conditions.windDirection;
        shiftWindDirection = conditions.shiftWindDirection;
        shiftWindStrength = conditions.shiftWindStrength;
        minWindStrength = conditions.minWindStrength;
        maxWindStrength = conditions.maxWindStrength;
        atmosphere = conditions.atmosphere;
        temperature = conditions.temperature;
        gravity = conditions.gravity;
        emi = conditions.emi;
        fog = conditions.fog;
        terrainAffected = conditions.terrainAffected;
        blowingSand = conditions.blowingSand;
        runOnce = conditions.runOnce;

        if (!runOnce) {
            setTempFromWeather();
            setWindFromWeather();
            setSandStorm();
            runOnce = true;
        }

    }

    private void setTempFromWeather() {
        switch (weatherConditions) {
            case WE_SLEET:
            case WE_LIGHT_SNOW:
                temperature = -40;
                break;
            case WE_MOD_SNOW:
            case WE_SNOW_FLURRIES:
            case WE_HEAVY_SNOW:
                temperature = -50;
                break;
            case WE_ICE_STORM:
                temperature = -60;
                break;
        }
    }

    private void setWindFromWeather() {
        switch (weatherConditions) {
            case WE_SLEET:
                setSleet(true);
                break;
            case WE_ICE_STORM:
            case WE_SNOW_FLURRIES:
                windStrength = WI_MOD_GALE;
                shiftWindStrength = false;
                break;
            case WE_GUSTING_RAIN:
                windStrength = WI_STRONG_GALE;
                shiftWindStrength = false;
                break;
        }
    }

    public boolean isSleeting() {
        return isSleeting;
    }

    public void setSleet(boolean sleet) {
        isSleeting = sleet;
    }

    private void doSleetCheck() {
        if (isSleeting && windStrength < WI_MOD_GALE) {
            setSleet(false);
            weatherConditions = WE_NONE;
            oldWeatherConditions = WE_SLEET;
            oldTemperature = temperature;
            temperature = 25;
        }
        if (isSleeting() && windStrength > WI_MOD_GALE) {
            shiftWindStrength = false;
            windStrength = WI_MOD_GALE;
        }
        if ((oldWeatherConditions == WE_SLEET)
                && (windStrength == WI_MOD_GALE)
                && !isSleeting()) {
            setSleet(true);
            temperature = oldTemperature;
            oldWeatherConditions = WE_NONE;
            oldTemperature = 25;
            weatherConditions = WE_SLEET;
        }
    }

    private void setSandStorm() {
        if (blowingSand && windStrength < WI_MOD_GALE) {
            windStrength = WI_MOD_GALE;
            sandStorm = true;
        }
    }

    private void doSandStormCheck() {
        if (blowingSand && windStrength < WI_MOD_GALE) {
            sandStorm = blowingSand;
            blowingSand = false;
        }
        if (sandStorm && windStrength > WI_LIGHT_GALE) {
            sandStorm = blowingSand;
            blowingSand = true;
        }
    }

    public void setRunOnce(boolean run) {
        runOnce = run;
    }

    public boolean isExtremeTemperatureHeat() {
        return  (isExtremeTemperature() && (temperature > 0));
    }

    public boolean isExtremeTemperatureCold() {
        return (isExtremeTemperature() && (temperature < 0));
    }

    public String getLightIndicator(int type) {
        if ((type >= 0) && (type < l_size)) {
            return lightIndicators[type];
        }
        throw new IllegalArgumentException("Unknown light Indicator");
    }

    public String getLightIndicator() {
        return getLightIndicator(lightConditions);
    }

    public String getFogIndicator(int type) {
        if ((type >= 0) && (type < fog_size)) {
            return fogIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Fog Indicator");
    }

    public String getFogIndicator() {
        return  getFogIndicator(fog);
    }

    public String getWindStrengthIndicator(int type) {
        if ((type >= 0) && (type < wi_size)) {
            return windStrengthIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Wind Strength Indicator");
    }

    public String getWindStrengthIndicator() {
        return getWindStrengthIndicator(windStrength);
    }

    public String getWindDirectionIndicator(int type) {
        if ((type >= 0) && (type < dir_size)) {
            return windDirectionIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Wind Direction Indicator");
    }

    public String getWindDirectionIndicator() {
        return getWindDirectionIndicator(windDirection);
    }

    public String getWeatherIndicator(int type) {
        if ((type >= 0) && (type < we_size)) {
            return weatherIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Weather Indicator");
    }

    public String getWeatherIndicator() {
        return getWeatherIndicator(weatherConditions);
    }

    public String getAtmosphereIndicator(int type) {
        if ((type >= 0) && (type < atmo_size)) {
            return atmosphereIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Atmosphere Indicator");
    }

    public String getAtmosphereIndicator() {
        return getAtmosphereIndicator(atmosphere);
    }

    public String getGravityIndicator() {
        if  (gravity > 1.0) {
            return msg_indicator_gravity_high;
        }
        else if ((gravity < 1.0)) {
            return msg_indicator_gravity_low;
        }

        return msg_indicator_gravity_normal;
    }

    public String getTemperatureIndicator() {
        if  (isExtremeTemperatureHeat()) {
            return msg_indicator_temperature_heat;
        }
        else if (isExtremeTemperatureCold()) {
            return msg_indicator_temperature_cold;
        }

        return msg_indicator_temperature_normal;
    }

    public String getEMIIndicator() {
        return hasEMI() ? msg_indicator_emi_true : msg_indicator_emi_false;
    }

    public String getSandBlowingIndicator() {
        return hasEMI() ? msg_indicator_blowingsand_true : msg_indicator_blowingsand_false;
    }

    public String getEMIDisplayableValue() {
        return hasEMI() ? msg_name_emi_true : msg_name_emi_false;
    }

    public String getSandBlowingDisplayableValue() {
        return isSandBlowing() ? msg_name_blowingsand_true : msg_name_blowingsand_false;
    }

}
