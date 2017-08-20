/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.Serializable;

/**
 * This class will hold all the information on planetary conditions and a variety of helper functions
 * for those conditions
 */
public class PlanetaryConditions implements Serializable {

    private static final long serialVersionUID = 6838624193286089781L;

    //light
    public static final int L_DAY          = 0;
    public static final int L_DUSK         = 1;
    public static final int L_FULL_MOON    = 2;
    public static final int L_MOONLESS     = 3;
    public static final int L_PITCH_BLACK  = 4;
    private static String[] lightNames = { "Daylight", "Dusk", "Full Moon Night", "Moonless Night",
                                           "Pitch Black"};
    public static final int L_SIZE = lightNames.length;

    //Weather
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
    public static final int WE_BLIZZARD         = 11;
    public static final int WE_ICE_STORM        = 12;
    public static final int WE_LIGHT_HAIL       = 13;
    public static final int WE_HEAVY_HAIL       = 14;
    private static String[] weatherNames = { "Clear", "Light Rain", "Moderate Rain", "Heavy Rain", "Gusting Rain", "Torrential Downpour",
                                             "Light Snowfall", "Moderate Snowfall", "Snow Flurries", "Heavy Snowfall", "Sleet", "Blizzard",
                                             "Ice Storm"};//, "Light Hail", "Heavy Hail"};
    public static final int WE_SIZE = weatherNames.length;

    //Wind
    public static final int WI_NONE        = 0;
    public static final int WI_LIGHT_GALE  = 1;
    public static final int WI_MOD_GALE    = 2;
    public static final int WI_STRONG_GALE = 3;
    public static final int WI_STORM       = 4;
    public static final int WI_TORNADO_F13 = 5;
    public static final int WI_TORNADO_F4  = 6;

    private static String[] windNames = { "Calm", "Light Gale", "Moderate Gale", "Strong Gale", "Storm", "Tornado F1-F3", "Tornado F4"};
    public static final int WI_SIZE = windNames.length;

    //wind direction
    private static String[] dirNames = { "North", "Northeast", "Southeast", "South","Southwest", "Northwest" };
    public static final int DIR_SIZE = dirNames.length;

    //atmospheric pressure
    public static final int ATMO_VACUUM   = 0;
    public static final int ATMO_TRACE    = 1;
    public static final int ATMO_THIN     = 2;
    public static final int ATMO_STANDARD = 3;
    public static final int ATMO_HIGH     = 4;
    public static final int ATMO_VHIGH    = 5;

    public static String[] atmoNames = {"Vacuum", "Trace", "Thin", "Standard", "High", "Very High" };
    public static final int ATMO_SIZE = atmoNames.length;

    //fog
    public static final int FOG_NONE  = 0;
    public static final int FOG_LIGHT = 1;
    public static final int FOG_HEAVY = 2;

    private static String[] fogNames = {"None", "Light Fog", "Heavy Fog" };
    public static final int FOG_SIZE = fogNames.length;

    //misc
    private boolean blowingSand = false;
    private boolean sandStorm = false;
    private boolean runOnce = false;

    //set up the specific conditions
    private int lightConditions = WI_NONE;
    private int weatherConditions = WE_NONE;
    private int oldWeatherConditions = WE_NONE;
    private int windStrength = WI_NONE;
    private int windDirection = -1;
    private boolean shiftWindDirection = false;
    private boolean shiftWindStrength = false;
    private boolean isSleeting = false;
    private int atmosphere = ATMO_STANDARD;
    private int fog = FOG_NONE;
    private int temperature = 25;
    private int oldTemperature = 25;
    private float gravity = (float)1.0;
    private boolean emi = false;
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
    }

    /** clone! */
    @Override
    public Object clone() {
        return new PlanetaryConditions(this);
    }

    public static String getLightDisplayableName(int type) {
        if ((type >= 0) && (type < L_SIZE)) {
            return Messages.getString("PlanetaryConditions." + lightNames[type]);
        }
        throw new IllegalArgumentException("Unknown light condition");
    }

    public static String getWeatherDisplayableName(int type) {
        if ((type >= 0) && (type < WE_SIZE)) {
            return Messages.getString("PlanetaryConditions." + weatherNames[type]);
        }
        throw new IllegalArgumentException("Unknown weather condition");
    }

    public static String getWindDirDisplayableName(int type) {
        if ((type >= 0) && (type < WI_SIZE)) {
            return Messages.getString("PlanetaryConditions." + dirNames[type]);
        }
        throw new IllegalArgumentException("Unknown wind direction");
    }

    public static String getWindDisplayableName(int type) {
        if ((type >= 0) && (type < WI_SIZE)) {
            return Messages.getString("PlanetaryConditions." + windNames[type]);
        }
        throw new IllegalArgumentException("Unknown wind condition");
    }

    public static String getAtmosphereDisplayableName(int type) {
        if ((type >= 0) && (type < ATMO_SIZE)) {
            return Messages.getString("PlanetaryConditions." + atmoNames[type]);
        }
        throw new IllegalArgumentException("Unknown atmospheric pressure condition");
    }

    public static String getFogDisplayableName(int type) {
        if ((type >= 0) && (type < FOG_SIZE)) {
            return Messages.getString("PlanetaryConditions." + fogNames[type]);
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
        if(isWeapon) {
            if(lightConditions == L_DUSK) {
                penalty = 1;
            }
            else if(lightConditions == L_FULL_MOON) {
                penalty = 2;
            }
            else if(lightConditions == L_MOONLESS) {
                penalty = 3;
            }
            else if(lightConditions == L_PITCH_BLACK) {
                penalty = 4;
            }
        }
        else {
            if(lightConditions == L_MOONLESS) {
                penalty = 1;
            }
            else if(lightConditions == L_PITCH_BLACK) {
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
        if(lightConditions == L_DUSK) {
            divisor = 25.0;
        }
        else if(lightConditions == L_FULL_MOON) {
            divisor = 20.0;
        }
        else if(lightConditions == L_MOONLESS) {
            divisor = 15.0;
        }
        else if(lightConditions == L_PITCH_BLACK) {
            divisor = 10.0;
        }
        return (-1 * (int)Math.floor(heat / divisor));
    }

    /**
     * piloting penalty for running/flanking/etc for light
     */
    public int getLightPilotPenalty() {
        if(lightConditions == L_MOONLESS) {
            return 1;
        }
        else if(lightConditions == L_PITCH_BLACK) {
            return 2;
        }
        return 0;
    }

    /**
     * to-hit penalty for weather
     */
    public int getWeatherHitPenalty(Entity en) {
        if(((weatherConditions == WE_LIGHT_RAIN) || (weatherConditions == WE_LIGHT_SNOW))
                && (en instanceof Infantry) && !(en instanceof BattleArmor)) {
            return 1;
        }
        else if((weatherConditions == WE_MOD_RAIN) || (weatherConditions == WE_HEAVY_RAIN)
                || (weatherConditions == WE_MOD_SNOW) || (weatherConditions == WE_HEAVY_SNOW)
                || (weatherConditions == WE_SLEET) || (weatherConditions == WE_GUSTING_RAIN)
                || (weatherConditions == WE_SNOW_FLURRIES) || (weatherConditions == WE_BLIZZARD)) {
            return 1;
        }
        else if(weatherConditions == WE_DOWNPOUR) {
            return 2;
        }
        else  {
            return 0;
        }
    }

    /**
     * piloting penalty for weather
     */
    public int getWeatherPilotPenalty() {
        if((weatherConditions == WE_HEAVY_RAIN) || (weatherConditions == WE_HEAVY_SNOW)
                || (weatherConditions == WE_GUSTING_RAIN) || (weatherConditions == WE_BLIZZARD)) {
            return 1;
        }
        else if(weatherConditions == WE_DOWNPOUR) {
            return 2;
        }
        else  {
            return 0;
        }
    }


    /**
     * gravity penalties to PSRs
     * According to email from TPTB, you apply a penalty for every 0.5 gravities above or below 1 (rounding up)
     */
    public int getGravityPilotPenalty() {
        return (int)Math.floor(Math.abs(gravity - 1.0) / 0.5);
    }

    /**
     * piloting penalty for wind
     */
    public int getWindPilotPenalty(Entity en) {
        int penalty = 0;

        switch(windStrength) {
        case (WI_MOD_GALE):
            if((en instanceof VTOL) || (en.getMovementMode() == EntityMovementMode.WIGE)) {
                penalty = 1;
            }
            break;
        case (WI_STRONG_GALE):
            if((en instanceof VTOL) || (en.getMovementMode() == EntityMovementMode.WIGE)
                    || (en.getMovementMode() == EntityMovementMode.HOVER)) {
                penalty = 2;
            }
            else if((en instanceof Mech) || (en.isAirborne())) {
                penalty = 1;
            }
            break;
        case (WI_STORM):
            if((en instanceof VTOL) || (en instanceof Mech) || (en.getMovementMode() == EntityMovementMode.WIGE)
                    || (en.getMovementMode() == EntityMovementMode.HOVER)) {
                penalty = 3;
            }
            else if(en.isAirborne()) {
                penalty = 2;
            }
            break;
        case (WI_TORNADO_F13):
            penalty = 3;
            break;
        case (WI_TORNADO_F4):
            penalty = 5;
            break;
        default:
            penalty = 0;
        }
        return penalty;
    }

    public void determineWind() {
        if (windDirection == -1) {
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

        //atmospheric pressure may limit wind strength
        if((atmosphere == ATMO_TRACE) && (windStrength > WI_STORM)) {
            windStrength = WI_STORM;
        }

        if((atmosphere ==ATMO_THIN) && (windStrength > WI_TORNADO_F13)) {
            windStrength = WI_TORNADO_F13;
        }
    }

    /**
     * modifiers for fire ignition
     */
    public int getIgniteModifiers() {
        int mod = 0;

        if((weatherConditions == WE_LIGHT_RAIN) || (weatherConditions == WE_MOD_RAIN)) {
            mod += 1;
        }
        if((weatherConditions == WE_HEAVY_RAIN) || (weatherConditions == WE_DOWNPOUR)
                || (weatherConditions == WE_LIGHT_SNOW) || (weatherConditions == WE_MOD_SNOW)
                || (weatherConditions == WE_GUSTING_RAIN) || (weatherConditions == WE_SNOW_FLURRIES)) {
            mod += 2;
        }
        if((weatherConditions == WE_HEAVY_SNOW) || (weatherConditions == WE_BLIZZARD)
                || (weatherConditions == WE_LIGHT_HAIL) || (weatherConditions == WE_HEAVY_HAIL)) {
            mod += 3;
        }
        if((windStrength == WI_LIGHT_GALE) || (windStrength == WI_MOD_GALE)) {
            mod += 2;
        }
        if((windStrength == WI_STRONG_GALE) || (windStrength == WI_STORM) || (weatherConditions == WE_ICE_STORM)) {
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
        switch(weatherConditions) {
        case (WE_LIGHT_HAIL):
        case (WE_LIGHT_RAIN):
        case (WE_LIGHT_SNOW):
            roll = roll + 1;
            break;
        case (WE_HEAVY_HAIL):
        case (WE_MOD_RAIN):
        case (WE_MOD_SNOW):
        case WE_SNOW_FLURRIES:
            roll = roll + 2;
            break;
        case (WE_HEAVY_RAIN):
        case (WE_GUSTING_RAIN):
        case (WE_HEAVY_SNOW):
        case WE_BLIZZARD:
            roll = roll + 3;
            break;
        case (WE_DOWNPOUR):
            roll = roll + 4;
            break;
        default:
            roll = -1;
        }

        if(roll > 10) {
            return true;
        }

          return false;
    }

    /**
     * Returns how much higher or lower than a given range, divided by
     * ten, rounded up, the temperature is
     */

    public int getTemperatureDifference(int high, int low) {
        int i = 0;
        //if the low is more than the high, reverse
        if(low > high) {
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
        if(atmosphere < ATMO_THIN) {
            return "atmosphere this thin";
        }
        if(windStrength > WI_STORM) {
            return "a tornado";
        }

        return null;
    }

    /**
     * Planetary conditions on movement, except for gravity
     * @param entity - the entity in question
     * @return an <code>int</code> with the modifier to movement
     */
    public int getMovementMods(Entity en) {
        int mod = 0;

        // weather mods are calculated based on conditional effects ie extreme temperatures, wind

        // wind mods
        switch(windStrength) {
        case(WI_LIGHT_GALE):
            if(!(en instanceof BattleArmor)
                    && ((en.getMovementMode() == EntityMovementMode.INF_LEG)
                            || (en.getMovementMode() == EntityMovementMode.INF_JUMP))) {
                mod -= 1;
            }
            break;
        case (WI_MOD_GALE):
            if((en instanceof Infantry) && !(en instanceof BattleArmor)) {
                mod -= 1;
            }
            break;
        case (WI_STRONG_GALE):
        case (WI_STORM):
            if(en instanceof BattleArmor) {
                mod -= 1;
            }
            else if(en instanceof Infantry) {
                mod -= 2;
            }
            break;
        case (WI_TORNADO_F13):
            if(en.isAirborne()) {
                mod -= 1;
            }
            else {
                mod -= 2;
            }
            break;
        }

        // atmospheric pressure mods
        switch(atmosphere) {
        case(ATMO_THIN):
            if((en.getMovementMode() == EntityMovementMode.HOVER)
                    || (en.getMovementMode() == EntityMovementMode.WIGE)
                    || (en.getMovementMode() == EntityMovementMode.VTOL)) {
                mod -= 2;
            }
            break;
        case(ATMO_HIGH):
        case(ATMO_VHIGH):
            if((en.getMovementMode() == EntityMovementMode.HOVER)
                    || (en.getMovementMode() == EntityMovementMode.WIGE)
                    || (en.getMovementMode() == EntityMovementMode.VTOL)) {
                mod += 1;
            }
            break;
        }

        // temperature difference
        if((en instanceof Tank) || (en instanceof Infantry) || (en instanceof Protomech)) {
            mod -= Math.abs(getTemperatureDifference(50,-30));
        }

        return mod;
    }

    /**
     * is the given entity type doomed in these conditions?
     * @return a string given the reason for being doomed, null if not doomed
     */
    public String whyDoomed(Entity en, IGame game) {
        if ((atmosphere < ATMO_THIN) && en.doomedInVacuum()) {
            return "vacuum";
        }
        if ((windStrength == WI_TORNADO_F4) && !(en instanceof Mech)) {
            return "tornado";
        }
        if ((windStrength == WI_TORNADO_F13)
                && (((en instanceof Infantry) && !(en instanceof BattleArmor))
                        || ((en.getMovementMode() == EntityMovementMode.HOVER)
                    || (en.getMovementMode() == EntityMovementMode.WIGE)
                    || (en.getMovementMode() == EntityMovementMode.VTOL)))) {
            return "tornado";
        }
        if((windStrength == WI_STORM) && ((en instanceof Infantry) && !(en instanceof BattleArmor))) {
            return "storm";
        }
        if ((temperature > 50 || temperature < -30) && en.doomedInExtremeTemp()) {
            if (Compute.isInBuilding(game, en)) {
                return null;
            }
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

        //Needed for MekWars for Maximum Visual Range.
        if ( en == null ) {
            isMechVee = true;
            Spotlight = targetIlluminated;
        }else {
            Spotlight = en.isUsingSpotlight();
            isMechVee = (en instanceof Mech && !en.isAero()) || (en instanceof Tank);
            isLargeCraft = (en instanceof Dropship) || (en instanceof Jumpship);
            isAero = (en.isAero()) && !isLargeCraft;
        }
        //anything else is infantry

        if ((lightConditions == L_PITCH_BLACK) && !Spotlight
                && !targetIlluminated) {
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
            }
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
        } else if (((lightConditions == L_MOONLESS) && !Spotlight && !targetIlluminated)
                || ((lightConditions == L_FULL_MOON) && !Spotlight && !targetIlluminated)
                || (weatherConditions == WE_BLIZZARD)
                || ((weatherConditions == WE_HEAVY_SNOW) && (windStrength >= WI_STRONG_GALE))) {
            if(isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 5;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
            }
            if (isAero) {
                return 10;
            }
            if (isLargeCraft) {
                return 8;
            }
            return 2;
        } else if ((weatherConditions == WE_HEAVY_SNOW)
                || (weatherConditions == WE_HEAVY_RAIN)
                || (weatherConditions == WE_DOWNPOUR)
                || (weatherConditions == WE_SLEET)
                || (weatherConditions == WE_ICE_STORM)
                || (weatherConditions == WE_HEAVY_HAIL)
                || (fog == FOG_HEAVY)
                || (blowingSand && (windStrength >= WI_MOD_GALE))) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 10;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
            }
            if (isAero) {
                return 20;
            }
            if (isLargeCraft) {
                return 15;
            }
            return 5;
        } else if ((lightConditions == L_PITCH_BLACK) && !targetIlluminated) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 13;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
            }
            if (isAero) {
                return 15;
            }
            if (isLargeCraft) {
                return 14;
            }
            return 6;
        } else if (((lightConditions > L_DAY) && !Spotlight && !targetIlluminated)
                || (weatherConditions == WE_GUSTING_RAIN)
                || (weatherConditions == WE_SNOW_FLURRIES)
                || (weatherConditions == WE_MOD_SNOW) && (windStrength >= WI_MOD_GALE)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 15;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
            }
            if (isAero) {
                return 30;
            }
            if (isLargeCraft) {
                return 20;
            }
            return 8;
        } else if((lightConditions > L_DAY) && !targetIlluminated) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 25;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
            }
            if (isAero) {
                return 40;
            }
            if (isLargeCraft) {
                return 30;
            }
            return 13;
        } else if ((weatherConditions == WE_MOD_SNOW)
                || (weatherConditions == WE_MOD_RAIN)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 20;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
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
                || (weatherConditions == WE_LIGHT_HAIL) || (fog == FOG_LIGHT)) {
            if (isMechVee || (isAero && (en.getAltitude() < 2))) {
                return 30;
            }
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
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
            if (isAero && (en.getAltitude() > 9)) {
                return 0;
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

        //atmospheric pressure mods
        switch(atmosphere) {
        case(ATMO_TRACE):
            return 8;
        case(ATMO_THIN):
            return 5;
        case(ATMO_HIGH):
            return 2;
        case(ATMO_VHIGH):
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
        weatherConditions = type;
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

    //can weather alter the terrain (add snow, mud, etc.)
    public boolean isTerrainAffected() {
        return terrainAffected;
    }

    public boolean isRecklessConditions() {
        return (fog > FOG_NONE) || (lightConditions > L_DUSK);
    }

    public int getMaxWindStrength(){
        return maxWindStrength;
    }

    public void setMaxWindStrength(int strength){
        maxWindStrength = strength;
    }

    public int getMinWindStrength(){
        return minWindStrength;
    }

    public void setMinWindStrength(int strength){
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
        
        if (runOnce) {
            setTempFromWeather();
            setWindFromWeather();
            setSandStorm();
            runOnce = false;
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
            case WE_BLIZZARD:
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
                windStrength = WI_MOD_GALE;
                setSleet(true);
                break;
            case WE_ICE_STORM:
            case WE_SNOW_FLURRIES:
                windStrength = WI_MOD_GALE;
                shiftWindStrength = false;
                break;
            case WE_GUSTING_RAIN:
            case WE_BLIZZARD:
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
                && !isSleeting() ) {
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
        runOnce = true;
    }
}
