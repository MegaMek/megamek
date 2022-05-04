/* MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
import java.util.List;

import megamek.common.options.OptionsConstants;

/**
 * This class will hold all the information about a particular active sensor,
 * including its rolls
 */
public class Sensor implements Serializable {

    private static final long serialVersionUID = 6838624193286089782L;

    private int type;

    // types of sensors
    public static final int TYPE_MEK_RADAR = 0;
    public static final int TYPE_VEE_RADAR = 1;
    public static final int TYPE_BAP = 2;
    public static final int TYPE_CLAN_BAP = 3;
    public static final int TYPE_BLOODHOUND = 4;
    public static final int TYPE_WATCHDOG = 5;
    public static final int TYPE_LIGHT_AP = 6;
    public static final int TYPE_MEK_IR = 7;
    public static final int TYPE_VEE_IR = 8;
    public static final int TYPE_MEK_MAGSCAN = 9;
    public static final int TYPE_VEE_MAGSCAN = 10;
    public static final int TYPE_BA_HEAT = 11;
    public static final int TYPE_BA_IMPROVED = 12;
    public static final int TYPE_MEK_SEISMIC = 13;
    public static final int TYPE_VEE_SEISMIC = 14;
    public static final int TYPE_EW_EQUIPMENT = 15;
    public static final int TYPE_NOVA = 16;
    public static final int TYPE_BAPP = 17;
    public static final int TYPE_AERO_SENSOR = 18;
    public static final int TYPE_SPACECRAFT_RADAR = 19;
    public static final int TYPE_SPACECRAFT_ESM = 20;
    public static final int TYPE_SPACECRAFT_THERMAL = 21;
    public static final int TYPE_AERO_THERMAL = 22;

    public static final String WATCHDOG = "WatchdogECMSuite";
    public static final String NOVA = "NovaCEWS";
    public static final String BAP = "BeagleActiveProbe";
    public static final String BAPP = "BeagleActiveProbePrototype";
    public static final String CLAN_AP = "CLActiveProbe";
    public static final String BLOODHOUND = "BloodhoundActiveProbe";
    public static final String LIGHT_AP = "CLLightActiveProbe";
    public static final String ISIMPROVED = "ISImprovedSensors";
    public static final String CLIMPROVED = "CLImprovedSensors";
    public static final String CLBALIGHT_AP = "CLBALightActiveProbe";
    public static final String ISBALIGHT_AP = "ISBALightActiveProbe";
    public static final String EW_EQUIPMENT = "ISElectronicWarfareEquipment";

    private static String[] sensorNames = { "Mech Radar", "Vehicle Radar",
            "Beagle Active Probe", "Clan BAP", "Bloodhound AP", "Watchdog",
            "Light AP", "Mech IR", "Vehicle IR", "Mech Magscan",
            "Vehicle Magscan", "Heat Sensors", "Improved Sensors",
            "Mech Seismic", "Vehicle Seismic", "EW Equipment", "Nova CEWS", "Beagle Active Probe Prototype", 
            "Aero Sensor Suite (Active)", "Spacecraft Radar (Active)", "Spacecraft Electronic Support Measures (Passive)",
            "Spacecraft Thermal/Optical Sensors (Passive)", "Aero Thermal/Optical Sensors (Passive)"};
    public static final int SIZE = sensorNames.length;
    
    //Constants for space automatic visual detection ranges
    public static final int ASF_RADAR_AUTOSPOT_RANGE = 55;
    public static final int ASF_OPTICAL_FIRING_SOLUTION_RANGE = 14;
    public static final int LC_RADAR_AUTOSPOT_RANGE = 555;
    public static final int LC_RADAR_GROUND_RANGE = 30;
    //Yeah, same value, but we might want to know what it's for later...
    public static final int ASF_RADAR_MAX_RANGE = 555;
    public static final int LC_OPTICAL_FIRING_SOLUTION_RANGE = 139;

    /**
     * Constructor
     */
    public Sensor(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getDisplayName() {
        if ((type >= 0) && (type < SIZE)) {
            return sensorNames[type];
        }
        throw new IllegalArgumentException("Unknown sensor type");
    }

    public boolean isBAP() {
        return (type == TYPE_BAP) || (type == TYPE_BLOODHOUND)
                || (type == TYPE_CLAN_BAP) || (type == TYPE_WATCHDOG)
                || (type == TYPE_LIGHT_AP) || (type == TYPE_EW_EQUIPMENT)
                || (type == TYPE_NOVA) || (type == TYPE_BAPP);
    }

    public int getRangeByBracket() {

        switch (type) {
            case TYPE_BAP:
                return 12;
            case TYPE_BAPP:
                return 12;
            case TYPE_BLOODHOUND:
                return 16;
            case TYPE_CLAN_BAP:
                return 15;
            case TYPE_WATCHDOG:
            case TYPE_LIGHT_AP:
            case TYPE_VEE_MAGSCAN:
            case TYPE_VEE_IR:
            case TYPE_BA_HEAT:
                return 9;
            case TYPE_NOVA:
                // I've not found a reference for sensor range of NovaCEWS.
                // Assuming Watchdog range.
                return 9;
            case TYPE_MEK_MAGSCAN:
            case TYPE_MEK_IR:
                //Under the current errata (3.0,Dec 2017), the rules only give aero sensor ranges against overflown ground units
                //No differences in range are mentioned for any sensor but active probe, so I'm assuming magscan range for standard sensors
            case TYPE_AERO_SENSOR:
                return 10;
            case TYPE_MEK_RADAR:
                return 8;
            case TYPE_VEE_RADAR:
            case TYPE_BA_IMPROVED:
                return 6;
            case TYPE_EW_EQUIPMENT:
                return 3;
            case TYPE_MEK_SEISMIC:
                return 2;
            case TYPE_VEE_SEISMIC:
                return 1;
            //The ranges listed for the various sensors in SO are so far beyond gameplay distances that I'm condensing
            //them into just the types that have different detection mechanics. 
            case TYPE_SPACECRAFT_RADAR:
            case TYPE_SPACECRAFT_ESM:
                return 5555;
            case TYPE_SPACECRAFT_THERMAL:
                return 1388;
            case TYPE_AERO_THERMAL:
                return 139;
            default:
                return 0;
        }
    }

    public int adjustRange(int range, Game game, LosEffects los) {

        if (((type == TYPE_MEK_RADAR) || (type == TYPE_VEE_RADAR)
                || (type == TYPE_VEE_MAGSCAN) || (type == TYPE_MEK_MAGSCAN))
                && ((los.getHardBuildings() + los.getSoftBuildings()) > 0)) {
            return 0;
        }
        
        if (los.isBlockedByHill()
                && (type != TYPE_MEK_SEISMIC)
                && (type != TYPE_VEE_SEISMIC)
                && ((type != TYPE_MEK_MAGSCAN) || game.getOptions()
                        .booleanOption(OptionsConstants.ADVANCED_MAGSCAN_NOHILLS))
                && ((type != TYPE_VEE_MAGSCAN) || game.getOptions()
                        .booleanOption(OptionsConstants.ADVANCED_MAGSCAN_NOHILLS)) && !isBAP()) {
            return 0;
        }

        if ((type != TYPE_MEK_SEISMIC) && (type != TYPE_VEE_SEISMIC)) {
            if (game.getPlanetaryConditions().hasEMI()) {
                range -= 4;
            }
            // TODO: add lightning
        }

        if ((type == TYPE_MEK_RADAR) || (type == TYPE_VEE_RADAR)
                || (type == TYPE_VEE_IR) || (type == TYPE_MEK_IR)
                || (type == TYPE_BA_HEAT)) {
            range -= los.getHeavyWoods() + los.getSoftBuildings();
            range -= 2 * (los.getUltraWoods() + los.getHardBuildings());
        }

        if ((type == TYPE_MEK_IR) || (type == TYPE_VEE_IR)) {
            range -= game.getPlanetaryConditions().getTemperatureDifference(50,
                    -30);
        }
        
        //Most spacecraft sensors only work in space...
        if (!game.getBoard().inSpace() && 
                (type == TYPE_SPACECRAFT_ESM 
                || type == TYPE_SPACECRAFT_THERMAL 
                || type == TYPE_AERO_THERMAL)) {
            range = 0;            
        }
        
        //Aero/Small Craft Active Sensors have longer range in space
        if (game.getBoard().inSpace() && type == TYPE_AERO_SENSOR) {
            range = ASF_RADAR_MAX_RANGE;
        }
        
        //DropShip radar has reduced range when not in space
        if (!game.getBoard().inSpace() && type == TYPE_SPACECRAFT_RADAR) {
            range = LC_RADAR_GROUND_RANGE;
        }

        return range;

    }

    public int getModsForStealth(Entity te) {
        int mod = 0;

        // first if we have seismic/magscan/IR we don't have to mod anything
        if ((type == TYPE_MEK_SEISMIC) || (type == TYPE_VEE_SEISMIC)
                || (type == TYPE_VEE_IR) || (type == TYPE_MEK_IR)
                || (type == TYPE_BA_HEAT) || (type == TYPE_MEK_MAGSCAN)
                || (type == TYPE_VEE_MAGSCAN)) {
            return mod;
        }

        boolean hasSneak = te.isConventionalInfantry() && (((Infantry) te).hasSneakCamo()
                || ((Infantry) te).hasSneakIR() || ((Infantry) te).hasDEST());
        boolean hasSneakECM = te.isConventionalInfantry() && ((Infantry) te).hasSneakECM();

        // these are cumulative, so lets just plow through the table on pg. 224 (ick)
        switch (type) {
            case TYPE_BAP:
            case TYPE_BAPP:
            case TYPE_EW_EQUIPMENT:
                if (te.isVoidSigActive()) {
                    mod += 6;
                }

                if (te.isNullSigActive()) {
                    mod += 5;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 3;
                }

                if (hasSneakECM) {
                    mod += 3;
                }

                if (hasSneak) {
                    mod += 2;
                }
                break;
            case TYPE_WATCHDOG:
            case TYPE_NOVA:
                // WOR : same as above. No data available, assuming Watchdog performance
                if (te.isVoidSigActive()) {
                    mod += 6;
                }

                if (te.isNullSigActive()) {
                    mod += 5;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 3;
                }

                if (hasSneakECM) {
                    mod += 2;
                }

                if (hasSneak) {
                    mod += 1;
                }
                break;
            case TYPE_CLAN_BAP:
                if (te.isVoidSigActive()) {
                    mod += 5;
                }

                if (te.isNullSigActive()) {
                    mod += 5;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 3;
                }

                if (hasSneakECM) {
                    mod += 2;
                }

                if (hasSneak) {
                    mod += 1;
                }
                break;
            case TYPE_BLOODHOUND:
                if (te.isChameleonShieldActive()) {
                    mod += 1;
                }

                if (te.isVoidSigActive()) {
                    mod += 4;
                }

                if (te.isNullSigActive()) {
                    mod += 3;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 1;
                }

                if (te.hasWorkingMisc(MiscType.F_VISUAL_CAMO, -1)) {
                    mod += 1;
                }

                if (hasSneakECM) {
                    mod += 1;
                }

                if (hasSneak) {
                    mod += 1;
                }
                break;
            case TYPE_LIGHT_AP:
                if (te.isVoidSigActive()) {
                    mod += 6;
                }

                if (te.isNullSigActive()) {
                    mod += 6;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 4;
                }

                if (hasSneakECM) {
                    mod += 3;
                }

                if (hasSneak) {
                    mod += 2;
                }
                break;
            case TYPE_MEK_RADAR:
                if (te.isChameleonShieldActive()) {
                    mod += 2;
                }

                if (te.isVoidSigActive()) {
                    mod += 7;
                }

                if (te.isNullSigActive()) {
                    mod += 6;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 4;
                }

                if (te.hasWorkingMisc(MiscType.F_VISUAL_CAMO, -1)) {
                    mod += 2;
                }

                if (hasSneakECM) {
                    mod += 4;
                }

                if (hasSneak) {
                    mod += 2;
                }
                break;
            case TYPE_VEE_RADAR:
            case TYPE_BA_IMPROVED:
                if (te.isChameleonShieldActive()) {
                    mod += 3;
                }

                if (te.isVoidSigActive()) {
                    mod += 7;
                }

                if (te.isNullSigActive()) {
                    mod += 7;
                }

                if (te.isStealthActive() && !te.isNullSigActive()) {
                    mod += 5;
                }

                if (te.hasWorkingMisc(MiscType.F_VISUAL_CAMO, -1)) {
                    mod += 3;
                }

                if (hasSneakECM) {
                    mod += 3;
                }

                if (hasSneak) {
                    mod += 1;
                }
                break;
        }

        return mod;
    }

    /**
     * Computes the sensor check modifier for ECM.
     * 
     * @param en
     * @param allECMInfo  A collection of ECMInfo for all entities, this value
     *                    can be null and it will be computed when it's
     *                    needed, however passing in the pre-computed
     *                    collection is much faster
     * @return
     */
    public int getModForECM(Entity en, List<ECMInfo> allECMInfo) {
        // how many ECM fields are affecting the entity?
        Coords pos = en.getPosition();
        ECMInfo ecmInfo = ComputeECM.getECMEffects(en, pos, pos, true,
                allECMInfo);
        double ecm, ecmAngel;
        ecm = ecmAngel = 0;
        if (ecmInfo != null) {
            ecm = Math.max(0, ecmInfo.getECMStrength());
            ecmAngel = Math.max(0, ecmInfo.getAngelECMStrength());
        }

        switch (type) {
            case TYPE_BAP:
            case TYPE_BAPP:
            case TYPE_CLAN_BAP:
            case TYPE_WATCHDOG:
                // as above, no data, assuming watchdog quality
            case TYPE_NOVA:
            case TYPE_EW_EQUIPMENT:
                return (int) Math.floor((ecm * 4) + (ecmAngel * 5));
            case TYPE_BLOODHOUND:
                return (int) Math.floor((ecm * 2) + (ecmAngel * 3));
            case TYPE_LIGHT_AP:
            case TYPE_MEK_RADAR:
                return (int) Math.floor((ecm * 5) + (ecmAngel * 6));
            case TYPE_VEE_RADAR:
            case TYPE_BA_IMPROVED:
                return (int) Math.floor((ecm * 6) + (ecmAngel * 7));
            default:
                return 0;
        }
    }

    public int getModForMetalContent(Entity en, Entity te) {
        // How much metal is affecting the entity?
        int metal = Compute.getMetalInPath(en, en.getPosition(), te.getPosition());

        switch (type) {
            case TYPE_MEK_MAGSCAN:
            case TYPE_VEE_MAGSCAN:
                return metal;
            default:
                return 0;
        }
    }

    public int entityAdjustments(int range, Entity target, Game game) {
        // You need to have moved to be detected by seismic and be on the ground
        if (((type == TYPE_MEK_SEISMIC) || (type == TYPE_VEE_SEISMIC))
                && ((target.mpUsed == 0) || (target.getElevation() > 0))) {
            return 0;
        }

        // If you have infrared, then each increment of 5 heat will increase the range
        if ((type == TYPE_MEK_IR) || (type == TYPE_VEE_IR)) {
            // If the target isn't overheating then you can't detect it
            if (target.heat < 1) {
                return 0;
            }

            range += target.heat / 5;

            if ((null != game.getBoard().getHex(target.getPosition()))
                    && game.getBoard().getHex(target.getPosition()).containsTerrain(Terrains.FIRE)) {
                range += 1;
            }
        }

        if ((type == TYPE_MEK_MAGSCAN) || (type == TYPE_VEE_MAGSCAN)) {
            if (target.getWeight() > 1000) {
                range += 3;
            } else if (target.getWeight() > 100) {
                range += 2;
            } else if (target.getWeight() >= 80) {
                range += 1;
            } else if (target.getWeight() < 20) {
                range = 0;
            }

            if ((null != game.getBoard().getHex(target.getPosition()))
                    && game.getBoard().getHex(target.getPosition()).containsTerrain(Terrains.INDUSTRIAL)) {
                return 0;
            }
        }

        return range;
    }
}
