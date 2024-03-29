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

package megamek.common.planetaryconditions;

import megamek.common.*;

import java.io.Serializable;


/**
 * This class will hold all the information on planetary conditions and a variety of helper functions
 * for those conditions
 */
public class PlanetaryConditions implements Serializable {

    private static final long serialVersionUID = 6838624193286089781L;

    // set up the specific conditions
    private BlowingSand blowingSand = BlowingSand.BLOWING_SAND_NONE;
    private BlowingSand sandStorm = BlowingSand.BLOWING_SAND_NONE;
    private EMI emi = EMI.EMI_NONE;
    private boolean runOnce = false;
    private Light light = Light.DAY;
    private Weather weather = Weather.CLEAR;
    private Weather oldWeather = Weather.CLEAR;
    private Wind wind = Wind.CALM;
    private Wind windMin = Wind.CALM;
    private Wind windMax = Wind.TORNADO_F4;
    private WindDirection windDirection = WindDirection.RANDOM;
    private boolean shiftWindDirection = false;
    private boolean shiftWindStrength = false;
    private boolean isSleeting = false;
    private Atmosphere atmosphere = Atmosphere.STANDARD;
    private Fog fog = Fog.FOG_NONE;
    private int temperature = 25;
    private int oldTemperature = 25;

    public static final int BLACK_ICE_TEMP = -30;
    private float gravity = (float) 1.0;
    private boolean terrainAffected = true;
    private static final String MSG_NAME_TEMPERATURE_COLD = Messages.getString("PlanetaryConditions.DisplayableName.Temperature.ExtremeCold");
    private static final String MSG_NAME_TEMPERATURE_HEAT = Messages.getString("PlanetaryConditions.DisplayableName.Temperature.ExtremeHeat");
    private static final String MSG_INDICATOR_TEMPERATURE_COLD = "\u2744";
    private static final String MSG_INDICATOR_TEMPERATURE_HEAT = "\uD83D\uDD25";
    private static final String MSG_INDICATOR_TEMPERATURE_NORMAL = "\uD83C\uDF21";
    private static final String MSG_INDICATOR_GRAVITY_LOW = "\u2B71";
    private static final String MSG_INDICATOR_GRAVITY_NORMAL = "\u23AF";
    private static final String MSG_INDICATOR_GRAVITY_HIGH = "\u2B73";

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

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public Weather getWeather() {
        return weather;
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

    public void setWindDirection(WindDirection windDirection) {
        this.windDirection = windDirection;
    }

    public WindDirection getWindDirection() {
        return windDirection;
    }

    public void setAtmosphere(Atmosphere atmosphere) {
        this.atmosphere = atmosphere;
    }

    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    public Fog getFog() {
        return fog;
    }

    public void setFog(Fog fog) {
        this.fog = fog;
    }

    public boolean isFog() {
        return !fog.isFogNone();
    }

    public BlowingSand getBlowingSand() {
        return blowingSand;
    }

    public void setBlowingSand(BlowingSand blowingSand) {
       this.blowingSand = blowingSand;
    }

    public boolean isBlowingSand() {
        return blowingSand.isBlowingSand();
    }

    public void setEMI(EMI emi) {
        this.emi = emi;
    }

    public EMI getEMI() {
        return emi;
    }

    public boolean isEMI() {
        return emi.isEMI();
    }

    public static String getTemperatureDisplayableName(int temp) {
        if (isExtremeTemperature(temp)
                && (temp > 0)) {
            return String.format("%d (%s)", temp, MSG_NAME_TEMPERATURE_HEAT);
        } else if (isExtremeTemperature(temp)
                && (temp <= 0)) {
            return String.format("%d (%s)", temp, MSG_NAME_TEMPERATURE_COLD);
        } else {
            return String.valueOf(temp);
        }
    }

    /**
     * to-hit penalty for light
     * TO:AR 6th ed. p. 56
     */
    public int getLightHitPenalty(boolean isWeapon) {
        int penalty = 0;
        if (isWeapon) {
            if (getLight().isDusk()) {
                penalty = 1;
            } else if (getLight().isFullMoonOrGlare()) {
                penalty = 2;
            } else if (getLight().isMoonlessOrSolarFlare()) {
                penalty = 3;
            } else if (getLight().isPitchBack()) {
                penalty = 4;
            }
        } else {
            if (getLight().isMoonlessOrSolarFlare()) {
                penalty = 1;
            } else if (getLight().isPitchBack()) {
                penalty = 2;
            }
        }

        return penalty;
    }

    /**
     * heat bonus to hit for being overheated in darkness
     * TO:AR 6th ed. p. 56
     */
    public int getLightHeatBonus(int heat) {
        double divisor = 10000.0;
        if (getLight().isDusk()) {
            divisor = 25.0;
        } else if (getLight().isFullMoonOrGlare()) {
            divisor = 20.0;
        } else if (getLight().isMoonlessOrSolarFlare()) {
            divisor = 15.0;
        } else if (getLight().isPitchBack()) {
            divisor = 10.0;
        }

        return (-1 * (int) Math.floor(heat / divisor));
    }

    /**
     * to-hit penalty for weather
     */
    public int getWeatherHitPenalty(Entity en) {
        if (getWeather().isLightRainOrLightSnow()
                && en.isConventionalInfantry()) {
            return 1;
        } else if (getWeather().isModerateRainOrHeavyRainOrGustingRainOrModerateSnowOrSnowFlurriesOrHeavySnowOrSleet()) {
            return 1;
        } else if(getWeather().isDownpour()) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * piloting penalty for weather
     */
    public int getWeatherPilotPenalty() {
        if (getWeather().isHeavyRainOrGustingRainOrHeavySnow()) {
            return 1;
        } else if (getWeather().isDownpour()) {
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
                if ((en instanceof VTOL)
                        || en.getMovementMode().isWiGE()) {
                    penalty = 1;
                }
                break;
            case STRONG_GALE:
                if ((en instanceof VTOL)
                        || en.getMovementMode().isHoverOrWiGE()) {
                    penalty = 2;
                } else if ((en instanceof Mech)
                        || (en.isAirborne())) {
                    penalty = 1;
                }
                break;
            case STORM:
                if ((en instanceof VTOL)
                        || (en instanceof Mech)
                        || en.getMovementMode().isHoverOrWiGE()) {
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
        if (getWindDirection().isRandomWindDirection()) {
            // Initial wind direction. If using level 2 rules, this
            // will be the wind direction for the whole battle.
            windDirection = WindDirection.getWindDirection(Compute.d6(1) - 1);
        } else if (shiftWindDirection) {
            // Wind direction changes on a roll of 1 or 6
            switch (Compute.d6()) {
                case 1: // rotate clockwise
                    windDirection = windDirection.rotateClockwise();
                    break;
                case 6: // rotate counter-clockwise
                    windDirection = windDirection.rotateCounterClockwise();
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
        if (getAtmosphere().isTrace()
                && getWind().isStrongerThan(Wind.STORM)) {
            wind = Wind.STORM;
        }

        if (getAtmosphere().isThin()
                && (getWind().isTornadoF4())) {
            wind = Wind.TORNADO_F1_TO_F3;
        }
    }

    /**
     * modifiers for fire ignition
     */
    public int getIgniteModifiers() {
        int mod = 0;

        if (getWeather().isLightRainOrModerateRain() ) {
            mod += 1;
        }

        if (getWeather().isHeavyRainOrGustingRainOrDownpourOrLightSnowOrModerateSnowOrSnowFlurries()) {
            mod += 2;
        }

        if (getWeather().isHeavySnowOrLightHailOrHeaveHail() ) {
            mod += 3;
        }

        if (getWind().isLightGaleOrModerateGale()) {
            mod += 2;
        }

        if (getWind().isStrongGaleOrStorm()
                || getWeather().isIceStorm()) {
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
        if (getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            return "atmosphere too thin";
        } else if (getWind().isStrongerThan(Wind.STORM)) {
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
                        && en.getMovementMode().isJumpOrLegInfantry()) {
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
            default:
        }

        // atmospheric pressure mods
        switch (atmosphere) {
            case THIN:
                if (en.getMovementMode().isHoverVTOLOrWiGE()) {
                    mod -= 2;
                }
                break;
            case HIGH:
            case VERY_HIGH:
                if (en.getMovementMode().isHoverVTOLOrWiGE()) {
                    mod += 1;
                }
                break;
            default:
        }

        // temperature difference
        boolean InfantryNotXCT = (en instanceof Infantry)
                && !((Infantry) en).isXCT();
        if ((en instanceof Tank)
                || InfantryNotXCT
                || (en instanceof Protomech)) {
            mod -= Math.abs(getTemperatureDifference(50,-30));
        }

        return mod;
    }

    /**
     * is the given entity type doomed in these conditions?
     * @return a string given the reason for being doomed, null if not doomed
     */
    public String whyDoomed(Entity en, Game game) {
        if (getAtmosphere().isLighterThan(Atmosphere.THIN)
                && en.doomedInVacuum()) {
            return "vacuum";
        }
        if (getWind().isTornadoF4()
                && !(en instanceof Mech)) {
            return "tornado";
        }
        boolean doomF1ToF3Types = en.isConventionalInfantry()
                || en.getMovementMode().isHoverVTOLOrWiGE();
        if (getWind().isTornadoF1ToF3()
                && doomF1ToF3Types) {
            return "tornado";
        }
        if (getWind().isStorm()
                && en.isConventionalInfantry()) {
            return "storm";
        }
        if (isExtremeTemperature()
                && en.doomedInExtremeTemp()
                && !Compute.isInBuilding(game, en)) {
            return "extreme temperature";
        }
        return null;
    }

    public boolean isBlowingSandActive() {
        return getBlowingSand().isBlowingSand()
                && getWind().isStrongerThan(Wind.LIGHT_GALE);
    }

    /**
     * visual range based on conditions
     * TO:AR 6th ed. p. 189
     *
     */
    public int getVisualRange(Entity en, boolean targetIlluminated) {

        boolean Spotlight = false;

        boolean isMechOrVee = false;
        boolean isLargeCraft = false;
        boolean isAero = false;

        // Needed for MekWars for Maximum Visual Range.
        if (en == null) {
            isMechOrVee = true;
            Spotlight = targetIlluminated;
        } else {
            Spotlight = en.isUsingSearchlight();
            isMechOrVee = (en instanceof Mech && !en.isAero())
                    || (en instanceof Tank);
            isLargeCraft = (en instanceof Dropship)
                    || (en instanceof Jumpship);
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
        boolean isLowAltitudeAero = (isAero
                && (en.getAltitude() < 2));
        if (targetIlluminated
                && (getLight().isDuskOrFullMoonOrMoonlessOrPitchBack())) {
            lightRange = 45;
        } else if (Spotlight
                && (getLight().isDuskOrFullMoonOrMoonlessOrPitchBack())) {
            // Using a searchlight?  Flat 30 hex range
            if (isMechOrVee || isAero || isLargeCraft) {
                lightRange = 30;
            } else {
                // Except infantry/handheld, 10 hexes
                lightRange = 10;
            }
        } else if (getLight().isPitchBack()) {
            if (isMechOrVee || isLowAltitudeAero) {
                lightRange = 3;
            } else if (isAero) {
                lightRange = 5;
            } else if (isLargeCraft) {
                lightRange = 4;
            } else {
                lightRange = 1;
            }
        } else if (getLight().isMoonless()) {
            if (isMechOrVee || isLowAltitudeAero) {
                lightRange = 5;
            } else if (isAero) {
                lightRange = 10;
            } else if (isLargeCraft) {
                lightRange = 8;
            } else {
                lightRange = 2;
            }
        } else if (getLight().isFullMoon()) {
            if (isMechOrVee || isLowAltitudeAero) {
                lightRange = 10;
            } else if (isAero) {
                lightRange = 20;
            } else if (isLargeCraft) {
                lightRange = 15;
            } else {
                lightRange = 5;
            }
        } else if (getLight().isDusk()) {
            if (isMechOrVee || isLowAltitudeAero) {
                lightRange = 15;
            } else if (isAero) {
                lightRange = 30;
            } else if (isLargeCraft) {
                lightRange = 20;
            } else {
                lightRange = 8;
            }
        } else {
            if (isMechOrVee || isLowAltitudeAero) {
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

        if (getFog().isFogHeavy()) {
            if (isMechOrVee || isLowAltitudeAero) {
                otherRange = 5;
            } else if (isAero) {
                otherRange = 10;
            } else if (isLargeCraft) {
                otherRange = 8;
            } else {
                otherRange = 2;
            }
        } else if (isBlowingSandActive()
                || getWeather().isGustingRainOrDownpourOrHeavySnowOrIceStormOrSleetOrHeavyHail()) {
            if (isMechOrVee || isLowAltitudeAero) {
                otherRange = 10;
            } else if (isAero) {
                otherRange = 20;
            } else if (isLargeCraft) {
                otherRange = 15;
            } else {
                otherRange = 5;
            }
        } else if (getWeather().isHeavyRainOrModerateSnowOrSnowFlurries()
                && getWind().isStrongerThan(Wind.LIGHT_GALE)) {
            if (isMechOrVee || isLowAltitudeAero) {
                otherRange = 15;
            } else if (isAero) {
                otherRange = 30;
            } else if (isLargeCraft) {
                otherRange = 20;
            } else {
                otherRange = 8;
            }
        } else if (getWeather().isModerateRainOrModerateSnow()) {
            if (isMechOrVee || isLowAltitudeAero) {
                otherRange = 20;
            } else if (isAero) {
                otherRange = 50;
            } else if (isLargeCraft) {
                otherRange = 25;
            } else {
                otherRange = 10;
            }
        } else if (getWeather().isLightRainOrLightSnowOrLightHail()
                || getFog().isFogLight()) {
            if (isMechOrVee || isLowAltitudeAero) {
                otherRange = 30;
            } else if (isAero) {
                otherRange = 60;
            } else if (isLargeCraft) {
                otherRange = 35;
            } else {
                otherRange = 15;
            }
        } else {
            if (isMechOrVee || isLowAltitudeAero) {
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
            case TRACE:
                return 8;
            case THIN:
                return 5;
            case HIGH:
                return 2;
            case VERY_HIGH:
                return 1;
            default:
                return 3;
        }
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

    public void setTemperature(int tem) {
        temperature = tem;
    }

    public int getTemperature() {
        return temperature;
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

    public void setTerrainAffected(boolean b) {
        terrainAffected = b;
    }

    // can weather alter the terrain (add snow, mud, etc.)
    public boolean isTerrainAffected() {
        return terrainAffected;
    }

    public boolean isRecklessConditions() {
        return !getFog().isFogNone()
                || getLight().isFullMoonOrGlareOrMoonlessOrSolarFlareOrPitchBack();
    }

    public static int setTempFromWeather(Weather weather, int temperature) {
        switch (weather) {
            case SLEET:
            case LIGHT_SNOW:
            case LIGHT_HAIL:
            case HEAVY_HAIL:
                return -40;
            case MOD_SNOW:
            case SNOW_FLURRIES:
            case HEAVY_SNOW:
                return -50;
            case ICE_STORM:
                return -60;
            default:
                return temperature;
        }
    }

    private void setTempFromWeather() {
        temperature = setTempFromWeather(weather, temperature);
    }

    public static Wind setWindFromWeather(Weather weather, Wind wind) {
        switch (weather) {
            case ICE_STORM:
            case SNOW_FLURRIES:
                return Wind.MOD_GALE;
            case GUSTING_RAIN:
                return Wind.STRONG_GALE;
            default:
                return wind;
        }
    }

    private void setWindFromWeather() {
        wind = setWindFromWeather(weather, wind);

        switch (weather) {
            case SLEET:
                setSleet(true);
                break;
            case SNOW_FLURRIES:
            case GUSTING_RAIN:
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
        if (isSleeting
                && getWind().isWeakerThan(Wind.MOD_GALE)) {
            setSleet(false);
            weather = Weather.CLEAR;
            oldWeather = Weather.SLEET;
            oldTemperature = temperature;
            temperature = 25;
        }
        if (isSleeting()
                && getWind().isStrongerThan(Wind.MOD_GALE)) {
            shiftWindStrength = false;
            wind = Wind.MOD_GALE;
        }
        if (oldWeather.isSleet()
                && getWind().isModerateGale()
                && !isSleeting()) {
            setSleet(true);
            temperature = oldTemperature;
            oldWeather = Weather.CLEAR;
            oldTemperature = 25;
            weather = Weather.SLEET;
        }
    }

    public static Wind setWindFromBlowingSand(BlowingSand blowingSand, Wind wind) {
        if (blowingSand.isBlowingSand()
                && wind.isWeakerThan(Wind.MOD_GALE)) {
            return Wind.MOD_GALE;
        }
        return wind;
    }

    private void setSandStorm() {
        wind = setWindFromBlowingSand(blowingSand, wind);
        sandStorm = BlowingSand.BLOWING_SAND;
    }

    private void doSandStormCheck() {
        if (isBlowingSand()
                && getWind().isWeakerThan(Wind.MOD_GALE)) {
            sandStorm = blowingSand;
            blowingSand = BlowingSand.BLOWING_SAND_NONE;
        }
        if (sandStorm.isBlowingSand()
                && getWind().isStrongerThan(Wind.LIGHT_GALE)) {
            sandStorm = blowingSand;
            blowingSand = BlowingSand.BLOWING_SAND;
        }
    }

    public boolean isExtremeTemperatureHeat() {
        return (isExtremeTemperature() && (temperature > 0));
    }

    public boolean isExtremeTemperatureCold() {
        return (isExtremeTemperature() && (temperature < 0));
    }

    public String getGravityIndicator() {
        if (gravity > 1.0) {
            return MSG_INDICATOR_GRAVITY_HIGH;
        } else if (gravity < 1.0) {
            return MSG_INDICATOR_GRAVITY_LOW;
        }

        return MSG_INDICATOR_GRAVITY_NORMAL;
    }

    public String getTemperatureIndicator() {
        if (isExtremeTemperatureHeat()) {
            return MSG_INDICATOR_TEMPERATURE_HEAT;
        } else if (isExtremeTemperatureCold()) {
            return MSG_INDICATOR_TEMPERATURE_COLD;
        }

        return MSG_INDICATOR_TEMPERATURE_NORMAL;
    }
}
