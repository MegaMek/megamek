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

import megamek.common.enums.Light;
import megamek.common.enums.Weather;
import megamek.common.enums.Wind;

/**
 * This class will hold all the information on planetary conditions and a variety of helper functions
 * for those conditions
 */
public class PlanetaryConditions implements Serializable {

    private static final long serialVersionUID = 6838624193286089781L;

    public static final int BLACK_ICE_TEMP      = -30;

    // wind direction
    private static final String MSG_NAME_WINDDIRECTION_NORTH = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.North");
    private static final String MSG_NAME_WINDDIRECTION_NORTHEAST = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Northeast");
    private static final String MSG_NAME_WINDDIRECTION_SOUTHEAST = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Southeast");
    private static final String MSG_NAME_WINDDIRECTION_SOUTH = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.South");
    private static final String MSG_NAME_WINDDIRECTION_SOUTHWEST = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Southwest");
    private static final String MSG_NAME_WINDDIRECTION_NORTHWEST = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.Northwest");
    private static final String MSG_NAME_WINDDIRECTION_RANDOM = Messages.getString("PlanetaryConditions.DisplayableName.WindDirection.RandomWindDirection");
    // no east and west, because the map uses 6 side hex tiles.  east and west are skipped.
    private static String[] dirNames = { MSG_NAME_WINDDIRECTION_SOUTH, MSG_NAME_WINDDIRECTION_SOUTHWEST,
            MSG_NAME_WINDDIRECTION_NORTHWEST, MSG_NAME_WINDDIRECTION_NORTH, MSG_NAME_WINDDIRECTION_NORTHEAST,
            MSG_NAME_WINDDIRECTION_SOUTHEAST, MSG_NAME_WINDDIRECTION_RANDOM };
    public static final int DIR_SIZE = dirNames.length;
    public static final int DIR_RANDOM = 6;
    private static final String MSG_INDICATOR_WINDDIRECTION_NORTH = Messages.getString("PlanetaryConditions.Indicator.WindDirection.North");
    private static final String MSG_INDICATOR_WINDDIRECTION_NORTHEAST = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Northeast");
    private static final String MSG_INDICATOR_WINDDIRECTION_SOUTHEAST = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Southeast");
    private static final String MSG_INDICATOR_WINDDIRECTION_SOUTH = Messages.getString("PlanetaryConditions.Indicator.WindDirection.South");
    private static final String MSG_INDICATOR_WINDDIRECTION_SOUTHWEST = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Southwest");
    private static final String MSG_INDICATOR_WINDDIRECTION_NORTHWEST = Messages.getString("PlanetaryConditions.Indicator.WindDirection.Northwest");
    private static final String MSG_INDICATOR_WINDDIRECTION_RANDOM = Messages.getString("PlanetaryConditions.Indicator.WindDirection.RandomWindDirection");
    // no east and west, because the map uses 6 side hex tiles.  east and west are skipped.
    private static String[] windDirectionIndicators = { MSG_INDICATOR_WINDDIRECTION_SOUTH, MSG_INDICATOR_WINDDIRECTION_SOUTHWEST,
            MSG_INDICATOR_WINDDIRECTION_NORTHWEST, MSG_INDICATOR_WINDDIRECTION_NORTH, MSG_INDICATOR_WINDDIRECTION_NORTHEAST,
            MSG_INDICATOR_WINDDIRECTION_SOUTHEAST,  MSG_INDICATOR_WINDDIRECTION_RANDOM };

    // atmospheric pressure
    public static final int ATMO_VACUUM   = 0;
    public static final int ATMO_TRACE    = 1;
    public static final int ATMO_THIN     = 2;
    public static final int ATMO_STANDARD = 3;
    public static final int ATMO_HIGH     = 4;
    public static final int ATMO_VHIGH    = 5;
    private static final String MSG_NAME_ATMOSPHERE_VACUUM = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Vacuum");
    private static final String MSG_NAME_ATMOSPHERE_TRACE = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Trace");
    private static final String MSG_NAME_ATMOSPHERE_THIN = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Thin");
    private static final String MSG_NAME_ATMOSPHERE_STANDARD = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Standard");
    private static final String MSG_NAME_ATMOSPHERE_HIGH = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.High");
    private static final String MSG_NAME_ATMOSPHERE_VHIGH = Messages.getString("PlanetaryConditions.DisplayableName.Atmosphere.Very High");
    public static String[] atmoNames = { MSG_NAME_ATMOSPHERE_VACUUM, MSG_NAME_ATMOSPHERE_TRACE, MSG_NAME_ATMOSPHERE_THIN,
            MSG_NAME_ATMOSPHERE_STANDARD, MSG_NAME_ATMOSPHERE_HIGH, MSG_NAME_ATMOSPHERE_VHIGH };
    public static final int ATMO_SIZE = atmoNames.length;
    private static final String MSG_INDICATOR_ATMOSPHERE_VACUUM = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Vacuum");
    private static final String MSG_INDICATOR_ATMOSPHERE_TRACE = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Trace");
    private static final String MSG_INDICATOR_ATMOSPHERE_THIN = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Thin");
    private static final String MSG_INDICATOR_ATMOSPHERE_STANDARD = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.Standard");
    private static final String MSG_INDICATOR_ATMOSPHERE_HIGH = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.High");
    private static final String MSG_INDICATOR_ATMOSPHERE_VHIGH = Messages.getString("PlanetaryConditions.Indicator.Atmosphere.VHigh");
    private static String[] atmosphereIndicators = { MSG_INDICATOR_ATMOSPHERE_VACUUM, MSG_INDICATOR_ATMOSPHERE_TRACE, MSG_INDICATOR_ATMOSPHERE_THIN,
            MSG_INDICATOR_ATMOSPHERE_STANDARD ,MSG_INDICATOR_ATMOSPHERE_HIGH ,MSG_INDICATOR_ATMOSPHERE_VHIGH };

    // fog
    public static final int FOG_NONE  = 0;
    public static final int FOG_LIGHT = 1;
    public static final int FOG_HEAVY = 2;
    private static final String MSG_NAME_FOG_NONE = Messages.getString("PlanetaryConditions.DisplayableName.Fog.None");
    private static final String MSG_NAME_FOG_LIGHT = Messages.getString("PlanetaryConditions.DisplayableName.Fog.Light Fog");
    private static final String MSG_NAME_FOG_HEAVY = Messages.getString("PlanetaryConditions.DisplayableName.Fog.Heavy Fog");
    private static String[] fogNames = { MSG_NAME_FOG_NONE, MSG_NAME_FOG_LIGHT, MSG_NAME_FOG_HEAVY };
    public static final int FOG_SIZE = fogNames.length;
    private static final String MSG_INDICATOR_FOG_NONE = Messages.getString("PlanetaryConditions.Indicator.Fog.None");
    private static final String MSG_INDICATOR_FOG_LIGHT = Messages.getString("PlanetaryConditions.Indicator.Fog.Light");
    private static final String MSG_INDICATOR_FOG_HEAVY = Messages.getString("PlanetaryConditions.Indicator.Fog.Heavy");
    private static String[] fogIndicators = { MSG_INDICATOR_FOG_NONE, MSG_INDICATOR_FOG_LIGHT, MSG_INDICATOR_FOG_HEAVY };

    // misc
    private boolean blowingSand = false;
    private static final String MSG_NAME_BLOWINGSAND_TRUE = Messages.getString("PlanetaryConditions.DisplayableName.SandBlowing.true");
    private static final String MSG_NAME_BLOWINGSAND_FALSE = Messages.getString("PlanetaryConditions.DisplayableName.SandBlowing.false");
    private static final String MSG_INDICATOR_BLOWINGSAND_TRUE = Messages.getString("PlanetaryConditions.Indicator.SandBlowing.true");
    private static final String MSG_INDICATOR_BLOWINGSAND_FALSE = Messages.getString("PlanetaryConditions.Indicator.SandBlowing.false");

    private boolean sandStorm = false;
    private boolean runOnce = false;

    // set up the specific conditions
    private Light light = Light.DAY;
    private Weather weather = Weather.WEATHER_NONE;
    private Weather oldWeather = Weather.WEATHER_NONE;
    private Wind wind = Wind.CALM;
    private Wind windMin = Wind.CALM;
    private Wind windMax = Wind.TORNADO_F4;
    private int windDirection = DIR_RANDOM;
    private boolean shiftWindDirection = false;
    private boolean shiftWindStrength = false;
    private boolean isSleeting = false;
    private int atmosphere = ATMO_STANDARD;
    private int fog = FOG_NONE;
    private int temperature = 25;
    private int oldTemperature = 25;
    private static final String MSG_NAME_TEMPERATURE_COLD = Messages.getString("PlanetaryConditions.DisplayableName.Temperature.ExtremeCold");
    private static final String MSG_NAME_TEMPERATURE_HEAT = Messages.getString("PlanetaryConditions.DisplayableName.Temperature.ExtremeHeat");
    private static final String MSG_INDICATOR_TEMPERATURE_COLD = Messages.getString("PlanetaryConditions.Indicator.Temperature.ExtremeCold");
    private static final String MSG_INDICATOR_TEMPERATURE_HEAT= Messages.getString("PlanetaryConditions.Indicator.Temperature.ExtremeHeat");
    private static final String MSG_INDICATOR_TEMPERATURE_NORMAL = Messages.getString("PlanetaryConditions.Indicator.Temperature.Normal");
    private float gravity = (float) 1.0;
    private static final String MSG_INDICATOR_GRAVITY_LOW= Messages.getString("PlanetaryConditions.Indicator.Gravity.Low");
    private static final String MSG_INDICATOR_GRAVITY_NORMAL = Messages.getString("PlanetaryConditions.Indicator.Gravity.Normal");
    private static final String MSG_INDICATOR_GRAVITY_HIGH = Messages.getString("PlanetaryConditions.Indicator.Gravity.High");
    private boolean emi = false;
    private static final String MSG_NAME_EMI_TRUE = Messages.getString("PlanetaryConditions.DisplayableName.EMI.true");
    private static final String MSG_NAME_EMI_FALSE = Messages.getString("PlanetaryConditions.DisplayableName.EMI.false");
    private static final String MSG_INDICATOR_EMI_TRUE = Messages.getString("PlanetaryConditions.Indicator.EMI.true");
    private static final String MSG_INDICATOR_EMI_FALSE = Messages.getString("PlanetaryConditions.Indicator.EMI.false");
    private boolean terrainAffected = true;

    /**
     * Constructor
     */
    public PlanetaryConditions() {

    }

    /** Creates new PlanetaryConditions that is a duplicate of another */
    public PlanetaryConditions(PlanetaryConditions other) {
        light = other.light;
        weather = other.weather;
        wind = other.wind;
        windMin = other.windMin;
        windMax = other.windMax;
        windDirection = other.windDirection;
        shiftWindDirection = other.shiftWindDirection;
        shiftWindStrength = other.shiftWindStrength;
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

    public void alterConditions(PlanetaryConditions conditions) {
        light = conditions.light;
        weather = conditions.weather;
        wind = conditions.wind;
        windMin = conditions.windMin;
        windMax = conditions.windMax;
        windDirection = conditions.windDirection;
        shiftWindDirection = conditions.shiftWindDirection;
        shiftWindStrength = conditions.shiftWindStrength;
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

    public void setLight(Light light) {
        this.light = light;
    }

    /** @return The time of day lighting conditions (one of PlanetaryConditions.L_*). */
    public Light getLight() {
        return light;
    }

    public boolean isDay() {
        return Light.isDay(light);
    }

    public boolean isDusk() {
        return Light.isDusk(light);
    }

    public boolean isFullMoon() {
        return Light.isFullMoon(light);
    }

    public boolean isMoonless() {
        return Light.isMoonless(light);
    }

    public boolean isPitchBack() {
        return Light.isPitchBack(light);
    }

    /**
     * Returns true when visual range is increased by a illumination
     * in the light condition, i.e. in dusk/dawn, full moon,
     * moonless and pitch black night.
     */
    public boolean isIlluminationEffective() {
        return Light.isIlluminationEffective(light);
    }

    /**
     * Returns true when visual range is dark
     * in the light condition, i.e. in full moon,
     * moonless and pitch black night.
     */
    public boolean isDark() {
        return Light.isDark(light);
    }

    /**
     * Returns true when visual range is very dark
     * in the light condition, i.e. in moonless and pitch black night.
     */
    public boolean isVeryDark() {
        return Light.isVeryDark(light);
    }

    /**
     * Returns true when visual range is light
     * in the light condition, i.e. in day, dusk
     */
    public boolean isLighted() {
        return Light.isLighted(light);
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public Weather getWeather() {
        return weather;
    }

    public boolean isWeatherNone() {
        return Weather.isWeatherNone(weather);
    }

    public boolean isLightRain() {
        return Weather.isLightRain(weather);
    }

    public boolean isModerateRain() {
        return Weather.isModerateRain(weather);
    }

    public boolean isHeavyRain() {
        return Weather.isHeavyRain(weather);
    }

    public boolean isGustingRain() {
        return Weather.isGustingRain(weather);
    }

    public boolean isDownpour() {
        return Weather.isDownpour(weather);
    }

    public boolean isLightSnow() {
        return Weather.isLightSnow(weather);
    }

    public boolean isModerateSnow() {
        return Weather.isModerateSnow(weather);
    }

    public boolean isHeavySnow() {
        return Weather.isHeavySnow(weather);
    }

    public boolean isSnowFlurries() {
        return Weather.isSnowFlurries(weather);
    }

    public boolean isSleet() {
        return Weather.isSleet(weather);
    }

    public boolean isIceStorm() {
        return Weather.isIceStorm(weather);
    }

    public boolean isLightHail() {
        return Weather.isLightHail(weather);
    }

    public boolean isHeaveHail() {
        return Weather.isHeaveHail(weather);
    }

    public boolean isLightningStorm() {
        return Weather.isLightningStorm(weather);
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public Wind getWind() {
        return wind;
    }

    public void setWindMin(Wind windMin) {
        this.windMin = windMin;
    }

    public Wind getWindMin() {
        return windMin;
    }

    public void setWindMax(Wind windMax) {
        this.windMax = windMax;
    }

    public Wind getWindMax() {
        return windMax;
    }

    public boolean isCalm() {
        return Wind.isCalm(wind);
    }

    public boolean isLightGale() {
        return Wind.isLightGale(wind);
    }

    public boolean isModerateGale() {
        return Wind.isModerateGale(wind);
    }

    public boolean isStrongGale() {
        return Wind.isStrongGale(wind);
    }

    public boolean isStorm() {
        return Wind.isStorm(wind);
    }

    public boolean isTornadoF1ToF3() {
        return Wind.isTornadoF1ToF3(wind);
    }

    public boolean isTornadoF4( ) {
        return Wind.isTornadoF4(wind);
    }

    public boolean isGreaterThanLightGale() {
        return Wind.isGreaterThanLightGale(wind);
    }

    public boolean isGreaterThanModerateGale() {
        return Wind.isGreaterThanModerateGale(wind);
    }

    public boolean isGreaterThanStrongGale() {
        return Wind.isGreaterThanStrongGale(wind);
    }

    public boolean isGreaterThanStorm() {
        return Wind.isGreaterThanStorm(wind);
    }

    public boolean isLessThanModerateGale() {
        return Wind.isLessThanModerateGale(wind);
    }

    public boolean isLessThanTornadoF1ToF3() {
        return Wind.isLessThanTornadoF1ToF3(wind);
    }



    public static String getTemperatureDisplayableName(int temp) {
        if (isExtremeTemperature(temp) && (temp > 0)) {
            return String.format("%d (%s)", temp, MSG_NAME_TEMPERATURE_HEAT);
        } else if (isExtremeTemperature(temp) && (temp <= 0)) {
            return String.format("%d (%s)", temp, MSG_NAME_TEMPERATURE_COLD);
        } else {
            return String.valueOf(temp);
        }
    }

    public static String getWindDirDisplayableName(int type) {
        if ((type >= 0) && (type < DIR_SIZE)) {
            return dirNames[type];
        }
        throw new IllegalArgumentException("Unknown wind direction");
    }

    public static String getAtmosphereDisplayableName(int type) {
        if ((type >= 0) && (type < ATMO_SIZE)) {
            return atmoNames[type];
        }
        throw new IllegalArgumentException("Unknown atmospheric pressure condition");
    }

    public static String getFogDisplayableName(int type) {
        if ((type >= 0) && (type < FOG_SIZE)) {
            return fogNames[type];
        }
        throw new IllegalArgumentException("Unknown fog condition");
    }

    public String getWindDirDisplayableName() {
        return getWindDirDisplayableName(windDirection);
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
            if (isDusk()) {
                penalty = 1;
            } else if (isFullMoon()) {
                penalty = 2;
            } else if (isMoonless()) {
                penalty = 3;
            } else if (isPitchBack()) {
                penalty = 4;
            }
        } else {
            if (isMoonless()) {
                penalty = 1;
            } else if (isPitchBack()) {
                penalty = 2;
            }
        }

        return penalty;
    }


    /**
     * heat bonus to hit for being overheated in darkness
     */
    public int getLightHeatBonus(int heat) {
        double divisor = 10000.0;
        if (isDusk()) {
            divisor = 25.0;
        } else if (isFullMoon()) {
            divisor = 20.0;
        } else if (isMoonless()) {
            divisor = 15.0;
        } else if (isPitchBack()) {
            divisor = 10.0;
        }

        return (-1 * (int) Math.floor(heat / divisor));
    }

    /**
     * to-hit penalty for weather
     */
    public int getWeatherHitPenalty(Entity en) {
        if ((isLightRain() || isLightSnow())
                && en.isConventionalInfantry()) {
            return 1;
        } else if (isModerateRain() || isHeavyRain()
                || isModerateSnow() || isHeavySnow()
                || isSleet() || isGustingRain()
                || isSnowFlurries()) {
            return 1;
        } else if(isDownpour()) {
            return 2;
        } else  {
            return 0;
        }
    }

    /**
     * piloting penalty for weather
     */
    public int getWeatherPilotPenalty() {
        if (isHeavyRain() || isHeavySnow() || isGustingRain()) {
            return 1;
        } else if (isDownpour()) {
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

        switch (wind) {
            case MOD_GALE:
                if ((en instanceof VTOL) || (en.getMovementMode() == EntityMovementMode.WIGE)) {
                    penalty = 1;
                }
                break;
            case STRONG_GALE:
                if ((en instanceof VTOL) || (en.getMovementMode() == EntityMovementMode.WIGE)
                        || (en.getMovementMode() == EntityMovementMode.HOVER)) {
                    penalty = 2;
                } else if ((en instanceof Mech) || (en.isAirborne())) {
                    penalty = 1;
                }
                break;
            case STORM:
                if ((en instanceof VTOL) || (en instanceof Mech) || (en.getMovementMode() == EntityMovementMode.WIGE)
                        || (en.getMovementMode() == EntityMovementMode.HOVER)) {
                    penalty = 3;
                } else if (en.isAirborne()) {
                    penalty = 2;
                }
                break;
            case TORNADO_F1_TO_F3:
                penalty = 3;
                break;
            case TORNADO_F4:
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
                    wind = wind.lowerWind();
                    doSleetCheck();
                    doSandStormCheck();
                    break;
                case 6: // stronger
                    wind = wind.raiseWind();
                    doSleetCheck();
                    doSandStormCheck();
                    break;
            }
        }

        // atmospheric pressure may limit wind strength
        if ((atmosphere == ATMO_TRACE) && (isGreaterThanStorm())) {
            wind = Wind.STORM;
        }

        if ((atmosphere ==ATMO_THIN) && (isTornadoF4())) {
            wind = Wind.TORNADO_F1_TO_F3;
        }
    }

    /**
     * modifiers for fire ignition
     */
    public int getIgniteModifiers() {
        int mod = 0;

        if (isLightRain() || isModerateRain()) {
            mod += 1;
        }

        if (isHeavyRain() || isDownpour()
                || isLightSnow() || isModerateSnow()
                ||isGustingRain() || isSnowFlurries()) {
            mod += 2;
        }

        if (isHeavySnow() || isLightHail() || isHeaveHail()) {
            mod += 3;
        }

        if (isLightGale() || isModerateGale()) {
            mod += 2;
        }

        if (isStrongGale() || isStorm() || isIceStorm()) {
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
        switch (weather) {
            case LIGHT_HAIL:
            case LIGHT_RAIN:
            case LIGHT_SNOW:
                roll = roll + 1;
                break;
            case HEAVY_HAIL:
            case MOD_RAIN:
            case MOD_SNOW:
            case SNOW_FLURRIES:
                roll = roll + 2;
                break;
            case HEAVY_RAIN:
            case GUSTING_RAIN:
            case HEAVY_SNOW:
                roll = roll + 3;
                break;
            case DOWNPOUR:
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
        } else if (isGreaterThanStorm()) {
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
        switch (wind) {
            case LIGHT_GALE:
                if (!(en instanceof BattleArmor)
                        && ((en.getMovementMode() == EntityMovementMode.INF_LEG)
                                || (en.getMovementMode() == EntityMovementMode.INF_JUMP))) {
                    mod -= 1;
                }
                break;
            case MOD_GALE:
                if (en.isConventionalInfantry()) {
                    mod -= 1;
                }
                break;
            case STRONG_GALE:
            case STORM:
                if (en instanceof BattleArmor) {
                    mod -= 1;
                } else if (en instanceof Infantry) {
                    mod -= 2;
                }
                break;
            case TORNADO_F1_TO_F3:
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
        if (isTornadoF4() && !(en instanceof Mech)) {
            return "tornado";
        }
        if (isTornadoF1ToF3() && (en.isConventionalInfantry()
            || ((en.getMovementMode() == EntityMovementMode.HOVER)
            || (en.getMovementMode() == EntityMovementMode.WIGE)
            || (en.getMovementMode() == EntityMovementMode.VTOL)))) {
            return "tornado";
        }
        if (isStorm() && en.isConventionalInfantry()) {
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

        int lightRange = 0;

        // TO:AR v6 p189
        // Illuminated?  Flat 45 hex distance
        if (targetIlluminated && (isIlluminationEffective())) {
            lightRange = 45;
        } else if (Spotlight && (isIlluminationEffective())) {
            // Using a searchlight?  Flat 30 hex range
            if (isMechVee || isAero || isLargeCraft) {
                lightRange = 30;
            } else {
                // Except infantry/handheld, 10 hexes
                lightRange = 10;
            }
    } else if (isPitchBack()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                lightRange = 3;
            } else if (isAero) {
                lightRange = 5;
            } else if (isLargeCraft) {
                lightRange = 4;
            } else {
                lightRange = 1;
            }
        } else if (isMoonless()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                lightRange = 5;
            } else if (isAero) {
                lightRange = 10;
            } else if (isLargeCraft) {
                lightRange = 8;
            } else {
                lightRange = 2;
            }
        } else if (isFullMoon()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                lightRange = 10;
            } else if (isAero) {
                lightRange = 20;
            } else if (isLargeCraft) {
                lightRange = 15;
            } else {
                lightRange = 5;
            }
        } else if (isDusk()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                lightRange = 15;
            } else if (isAero) {
                lightRange = 30;
            } else if (isLargeCraft) {
                lightRange = 20;
            } else {
                lightRange = 8;
            }
        } else {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                lightRange = 60;
            } else if (isAero) {
                lightRange = 120;
            } else if (isLargeCraft) {
                lightRange = 70;
            } else {
                lightRange = 30;
            }
        }

        int otherRange = 0;

        if (fog == FOG_HEAVY) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                otherRange =  5;
            } else if (isAero) {
                otherRange =  10;
            } else if (isLargeCraft) {
                otherRange =  8;
            } else {
                otherRange = 2;
            }
        } else if (isHeaveHail()
                || isSleet()
                || isHeavySnow()
                || (blowingSand && isGreaterThanLightGale())
                || isGustingRain()
                || isIceStorm()
                || isDownpour()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                otherRange = 10;
            } else if (isAero) {
                otherRange = 20;
            } else if (isLargeCraft) {
                otherRange = 15;
            } else {
                otherRange = 5;
            }
        } else if ((isHeavyRain() || isSnowFlurries() || isModerateSnow())
                && isGreaterThanLightGale()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                otherRange = 15;
            } else if (isAero) {
                otherRange = 30;
            } else if (isLargeCraft) {
                otherRange = 20;
            } else {
                otherRange = 8;
            }
        } else if (isModerateSnow()
                || isModerateRain()) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                otherRange = 20;
            } else if (isAero) {
                otherRange = 50;
            } else if (isLargeCraft) {
                otherRange = 25;
            } else {
                otherRange = 10;
            }
        } else if (isLightSnow()
                || isLightRain()
                || isLightHail()
                || (fog == FOG_LIGHT)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                otherRange = 30;
            } else if (isAero) {
                otherRange = 60;
            } else if (isLargeCraft) {
                otherRange = 35;
            } else {
                otherRange = 15;
            }
        } else {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                otherRange = 60;
            } else if (isAero) {
                otherRange = 120;
            } else if (isLargeCraft) {
                otherRange = 70;
            } else {
                otherRange = 30;
            }
        }

        return Math.min(lightRange, otherRange);
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
        return (fog > FOG_NONE) || Light.isDark(light);
    }

    public boolean isSandBlowing() {
        return blowingSand;
    }

    public void setBlowingSand(boolean b) {
        blowingSand = b;
    }

    private void setTempFromWeather() {
        switch (weather) {
            case SLEET:
            case LIGHT_SNOW:
                temperature = -40;
                break;
            case MOD_SNOW:
            case SNOW_FLURRIES:
            case HEAVY_SNOW:
                temperature = -50;
                break;
            case ICE_STORM:
                temperature = -60;
                break;
            default:
        }
    }

    private void setWindFromWeather() {
        switch (weather) {
            case SLEET:
                setSleet(true);
                break;
            case ICE_STORM:
            case SNOW_FLURRIES:
                wind = Wind.MOD_GALE;
                shiftWindStrength = false;
                break;
            case GUSTING_RAIN:
                wind = Wind.STRONG_GALE;
                shiftWindStrength = false;
                break;
            default:
        }
    }

    public boolean isSleeting() {
        return isSleeting;
    }

    public void setSleet(boolean sleet) {
        isSleeting = sleet;
    }

    private void doSleetCheck() {
        if (isSleeting && isLessThanModerateGale()) {
            setSleet(false);
            weather = Weather.WEATHER_NONE;
            oldWeather = Weather.SLEET;
            oldTemperature = temperature;
            temperature = 25;
        }
        if (isSleeting() && (isGreaterThanModerateGale())) {
            shiftWindStrength = false;
            wind = Wind.MOD_GALE;
        }
        if ((Weather.isSleet(oldWeather))
                && isModerateGale()
                && !isSleeting()) {
            setSleet(true);
            temperature = oldTemperature;
            oldWeather = Weather.WEATHER_NONE;
            oldTemperature = 25;
            weather = Weather.SLEET;
        }
    }

    private void setSandStorm() {
        if (blowingSand && isLessThanModerateGale()) {
            wind = Wind.MOD_GALE;
            sandStorm = true;
        }
    }

    private void doSandStormCheck() {
        if (blowingSand && isLessThanModerateGale()) {
            sandStorm = blowingSand;
            blowingSand = false;
        }
        if (sandStorm && isGreaterThanLightGale()) {
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

    public String getFogIndicator(int type) {
        if ((type >= 0) && (type < FOG_SIZE)) {
            return fogIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Fog Indicator");
    }

    public String getFogIndicator() {
        return  getFogIndicator(fog);
    }

    public String getWindDirectionIndicator(int type) {
        if ((type >= 0) && (type < DIR_SIZE)) {
            return windDirectionIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Wind Direction Indicator");
    }

    public String getWindDirectionIndicator() {
        return getWindDirectionIndicator(windDirection);
    }

    public String getAtmosphereIndicator(int type) {
        if ((type >= 0) && (type < ATMO_SIZE)) {
            return atmosphereIndicators[type];
        }
        throw new IllegalArgumentException("Unknown Atmosphere Indicator");
    }

    public String getAtmosphereIndicator() {
        return getAtmosphereIndicator(atmosphere);
    }

    public String getGravityIndicator() {
        if  (gravity > 1.0) {
            return MSG_INDICATOR_GRAVITY_HIGH;
        }
        else if ((gravity < 1.0)) {
            return MSG_INDICATOR_GRAVITY_LOW;
        }

        return MSG_INDICATOR_GRAVITY_NORMAL;
    }

    public String getTemperatureIndicator() {
        if  (isExtremeTemperatureHeat()) {
            return MSG_INDICATOR_TEMPERATURE_HEAT;
        }
        else if (isExtremeTemperatureCold()) {
            return MSG_INDICATOR_TEMPERATURE_COLD;
        }

        return MSG_INDICATOR_TEMPERATURE_NORMAL;
    }

    public String getEMIIndicator() {
        return hasEMI() ? MSG_INDICATOR_EMI_TRUE : MSG_INDICATOR_EMI_FALSE;
    }

    public String getSandBlowingIndicator() {
        return hasEMI() ? MSG_INDICATOR_BLOWINGSAND_TRUE : MSG_INDICATOR_BLOWINGSAND_FALSE;
    }

    public String getEMIDisplayableValue() {
        return hasEMI() ? MSG_NAME_EMI_TRUE : MSG_NAME_EMI_FALSE;
    }

    public String getSandBlowingDisplayableValue() {
        return isSandBlowing() ? MSG_NAME_BLOWINGSAND_TRUE : MSG_NAME_BLOWINGSAND_FALSE;
    }

    public static String  getEMIDisplayableValue(boolean b) {
        return b ? MSG_NAME_EMI_TRUE : MSG_NAME_EMI_FALSE;
    }

    public static String  getSandBlowingDisplayableValue(boolean b) {
        return b ? MSG_NAME_BLOWINGSAND_TRUE : MSG_NAME_BLOWINGSAND_FALSE;
    }

}
