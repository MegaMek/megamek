/*
 * Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.loaders;

import java.io.*;
import java.util.*;

import megamek.common.*;
import org.apache.logging.log4j.LogManager;

/**
 * @author Ben
 * @since April 7, 2002, 8:47 PM
 */
public class MtfFile implements IMechLoader {

    private final String name;
    private final String model;
    private int mulId = -1;

    private String chassisConfig;
    private String techBase;
    private String techYear;
    private String rulesLevel;
    private String source = "Source:";

    private String tonnage;
    private String engine;
    private String internalType;
    private String gyroType;
    private String cockpitType;
    private String lamType;
    private String motiveType;
    private String ejectionType;

    private String heatSinks;
    private String jumpMP;
    private String baseChassieHeatSinks = "base chassis heat sinks:-1";

    private String armorType;
    private final String[] armorValues = new String[12];

    private final String[][] critData;
    private final List<String> noCritEquipment = new ArrayList<>();

    private String capabilities = "";
    private String deployment = "";
    private String overview = "";
    private String history = "";
    private String manufacturer = "";
    private String primaryFactory = "";
    private final Map<EntityFluff.System, String> systemManufacturers = new EnumMap<>(EntityFluff.System.class);
    private final Map<EntityFluff.System, String> systemModels = new EnumMap<>(EntityFluff.System.class);
    private String notes = "";
    private String imagePath = "";

    private int bv = 0;

    private final Map<EquipmentType, Mounted> hSharedEquip = new HashMap<>();
    private final List<Mounted> vSplitWeapons = new ArrayList<>();

    public static final int[] locationOrder =
            {Mech.LOC_LARM, Mech.LOC_RARM, Mech.LOC_LT, Mech.LOC_RT, Mech.LOC_CT, Mech.LOC_HEAD, Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_CLEG};
    public static final int[] rearLocationOrder =
            {Mech.LOC_LT, Mech.LOC_RT, Mech.LOC_CT};

    public static final String COCKPIT = "cockpit:";
    public static final String GYRO = "gyro:";
    public static final String MOTIVE = "motive:";
    public static final String EJECTION = "ejection:";
    public static final String MASS = "mass:";
    public static final String ENGINE = "engine:";
    public static final String STRUCTURE = "structure:";
    public static final String MYOMER = "myomer:";
    public static final String LAM = "lam:";
    public static final String CONFIG = "config:";
    public static final String TECH_BASE = "techbase:";
    public static final String ERA = "era:";
    public static final String SOURCE = "source:";
    public static final String RULES_LEVEL = "rules level:";
    public static final String HEAT_SINKS = "heat sinks:";
    public static final String BASE_CHASSIS_HEAT_SINKS = "base chassis heat sinks:";
    public static final String HS_SINGLE = "Single";
    public static final String HS_DOUBLE = "Double";
    public static final String HS_LASER = "Laser";
    public static final String HS_COMPACT = "Compact";
    public static final String TECH_BASE_IS = "IS";
    public static final String TECH_BASE_CLAN = "Clan";
    public static final String WALK_MP = "walk mp:";
    public static final String JUMP_MP = "jump mp:";
    public static final String ARMOR = "armor:";
    public static final String OVERVIEW = "overview:";
    public static final String CAPABILITIES = "capabilities:";
    public static final String DEPLOYMENT = "deployment:";
    public static final String HISTORY = "history:";
    public static final String MANUFACTURER = "manufacturer:";
    public static final String PRIMARY_FACTORY = "primaryfactory:";
    public static final String SYSTEM_MANUFACTURER = "systemmanufacturer:";
    public static final String SYSTEM_MODEL = "systemmode:";
    public static final String NOTES = "notes:";
    public static final String IMAGE_FILE = "imagefile:";
    public static final String BV = "bv:";
    public static final String WEAPONS = "weapons:";
    public static final String EMPTY = "-Empty-";
    public static final String ARMORED = "(ARMORED)";
    public static final String OMNIPOD = "(OMNIPOD)";
    public static final String NO_CRIT = "nocrit:";
    public static final String SIZE = ":SIZE:";
    public static final String MUL_ID = "mul id:";

    /**
     * Creates new MtfFile
     */
    public MtfFile(InputStream is) throws EntityLoadingException {
        try (InputStreamReader isr = new InputStreamReader(is);
             BufferedReader r = new BufferedReader(isr)) {
            String version = r.readLine();
            if (version == null) {
                throw new EntityLoadingException("MTF File empty!");
            }
            // Version 1.0: Initial version.
            // Version 1.1: Added level 3 cockpit and gyro options.
            // version 1.2: added full head ejection
            // Version 1.3: Added MUL ID
            if (!version.trim().equalsIgnoreCase("Version:1.0")
                    && !version.trim().equalsIgnoreCase("Version:1.1")
                    && !version.trim().equalsIgnoreCase("Version:1.2")
                    && !version.trim().equalsIgnoreCase("Version:1.3")) {
                throw new EntityLoadingException("Wrong MTF file version.");
            }

            name = r.readLine();
            model = r.readLine();

            critData = new String[9][12];

            readCrits(r);
        } catch (IOException ex) {
            LogManager.getLogger().error("", ex);
            throw new EntityLoadingException("I/O Error reading file");
        } catch (StringIndexOutOfBoundsException ex) {
            LogManager.getLogger().error("", ex);
            throw new EntityLoadingException("StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
            LogManager.getLogger().error("", ex);
            throw new EntityLoadingException("NumberFormatException reading file (format error)");
        }
    }

    private void readCrits(BufferedReader r) throws IOException {
        int slot = 0;
        int loc = 0;
        String crit;
        int weaponsCount;
        int armorLocation;
        while (r.ready()) {
            crit = r.readLine().trim();
            if (crit.isEmpty()) {
                continue;
            }

            if (isValidLocation(crit)) {
                loc = getLocation(crit);
                slot = 0;
                continue;
            }

            if (isProcessedComponent(crit)) {
                continue;
            }

            weaponsCount = weaponsList(crit);

            if (weaponsCount > 0) {
                for (int count = 0; count < weaponsCount; count++) {
                    r.readLine();
                }
                continue;
            }

            armorLocation = getArmorLocation(crit);

            if (armorLocation >= 0) {
                armorValues[armorLocation] = crit;
                continue;
            }
            if (critData.length <= loc) {
                continue;
            }
            if (critData[loc].length <= slot) {
                continue;
            }
            critData[loc][slot++] = crit.trim();
        }
    }

    @Override
    public Entity getEntity() throws Exception {
        try {
            Mech mech;

            int iGyroType;
            try {
                iGyroType = Mech.getGyroTypeForString(gyroType.substring(5));
                if (iGyroType == Mech.GYRO_UNKNOWN) {
                    iGyroType = Mech.GYRO_STANDARD;
                }
            } catch (Exception ignored) {
                iGyroType = Mech.GYRO_STANDARD;
            }

            int iCockpitType;
            try {
                iCockpitType = Mech.getCockpitTypeForString(cockpitType.substring(8));
                if (iCockpitType == Mech.COCKPIT_UNKNOWN) {
                    iCockpitType = Mech.COCKPIT_STANDARD;
                }
            } catch (Exception ignored) {
                iCockpitType = Mech.COCKPIT_STANDARD;
            }
            boolean fullHead;
            try {
                fullHead = ejectionType.substring(9).equals(Mech.FULL_HEAD_EJECT_STRING);
            } catch (Exception ignored) {
                fullHead = false;
            }
            if (chassisConfig.contains("QuadVee")) {
                int iMotiveType;
                try {
                    iMotiveType = QuadVee.getMotiveTypeForString(motiveType.substring(7));
                    if (iMotiveType == QuadVee.MOTIVE_UNKNOWN) {
                        iMotiveType = QuadVee.MOTIVE_TRACK;
                    }
                } catch (Exception ignored) {
                    iMotiveType = QuadVee.MOTIVE_TRACK;
                }
                mech = new QuadVee(iGyroType, iMotiveType);
            } else if (chassisConfig.contains("Quad")) {
                mech = new QuadMech(iGyroType, iCockpitType);
            } else if (chassisConfig.contains("LAM")) {
                int iLAMType;
                try {
                    iLAMType = LandAirMech.getLAMTypeForString(lamType.substring(4));
                    if (iLAMType == LandAirMech.LAM_UNKNOWN) {
                        iLAMType = LandAirMech.LAM_STANDARD;
                    }
                } catch (Exception ignored) {
                    iLAMType = LandAirMech.LAM_STANDARD;
                }
                mech = new LandAirMech(iGyroType, iCockpitType, iLAMType);
            } else if (chassisConfig.contains("Tripod")) {
                mech = new TripodMech(iGyroType, iCockpitType);
            } else {
                mech = new BipedMech(iGyroType, iCockpitType);
            }
            mech.setFullHeadEject(fullHead);
            mech.setChassis(name.trim());
            mech.setModel(model.trim());
            mech.setMulId(mulId);
            mech.setYear(Integer.parseInt(techYear.substring(4).trim()));
            mech.setSource(source.substring("Source:".length()).trim());

            if (chassisConfig.contains("Omni")) {
                mech.setOmni(true);
            }
            setTechLevel(mech);
            mech.setWeight(Integer.parseInt(tonnage.substring(5)));

            int engineFlags = 0;
            if ((mech.isClan() && !mech.isMixedTech()) || (mech.isMixedTech() && mech.isClan() && !mech.itemOppositeTech(engine)) || (mech.isMixedTech() && !mech.isClan() && mech.itemOppositeTech(engine))) {
                engineFlags = Engine.CLAN_ENGINE;
            }
            if (mech.isSuperHeavy()) {
                engineFlags |= Engine.SUPERHEAVY_ENGINE;
            }

            int engineRating = Integer.parseInt(engine.substring(engine.indexOf(":") + 1, engine.indexOf(" ")));
            mech.setEngine(new Engine(engineRating, Engine.getEngineTypeByString(engine), engineFlags));

            mech.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));

            boolean dblSinks = heatSinks.contains(HS_DOUBLE);
            boolean laserSinks = heatSinks.contains(HS_LASER);
            boolean compactSinks = heatSinks.contains(HS_COMPACT);
            int expectedSinks = Integer.parseInt(heatSinks.substring(11, 13).trim());
            int baseHeatSinks = Integer.parseInt(baseChassieHeatSinks.substring("base chassis heat sinks:".length()).trim());
            // For mixed tech units with double heat sinks we want to install the correct type. Legacy files
            // don't specify, so we'll use TECH_BASE_ALL to indicate unknown and check for heat sinks on the
            // critical table.
            int heatSinkBase;
            if (heatSinks.contains(TECH_BASE_CLAN)) {
                heatSinkBase = ITechnology.TECH_BASE_CLAN;
            } else if (heatSinks.contains(TECH_BASE_IS)) {
                heatSinkBase = ITechnology.TECH_BASE_IS;
            } else {
                heatSinkBase = ITechnology.TECH_BASE_ALL;
            }

            String thisStructureType = internalType.substring(internalType.indexOf(':') + 1);
            if (!thisStructureType.isBlank()) {
                mech.setStructureType(thisStructureType);
            } else {
                mech.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
            }
            mech.autoSetInternal();

            String thisArmorType = armorType.substring(armorType.indexOf(':') + 1);
            if (thisArmorType.indexOf('(') != -1) {
                boolean clan = thisArmorType.toLowerCase().contains("clan");
                if (clan) {
                    switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                        case 2:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_TW);
                            break;
                        case 3:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED);
                            break;
                        case 4:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                            break;
                        case 5:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                            break;
                        default:
                            throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                    }
                } else {
                    switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                        case 1:
                            mech.setArmorTechLevel(TechConstants.T_INTRO_BOXSET);
                            break;
                        case 2:
                            mech.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);
                            break;
                        case 3:
                            mech.setArmorTechLevel(TechConstants.T_IS_ADVANCED);
                            break;
                        case 4:
                            mech.setArmorTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                            break;
                        case 5:
                            mech.setArmorTechLevel(TechConstants.T_IS_UNOFFICIAL);
                            break;
                        default:
                            throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                    }
                }
                thisArmorType = thisArmorType.substring(0, thisArmorType.indexOf('(')).trim();
                mech.setArmorType(thisArmorType);
            } else if (!thisArmorType.equals(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                mech.setArmorTechLevel(mech.getTechLevel());
                mech.setArmorType(thisArmorType);
            }

            if (thisArmorType.isBlank()) {
                mech.setArmorType(EquipmentType.T_ARMOR_STANDARD);
            }
            mech.recalculateTechAdvancement();

            for (int x = 0; x < locationOrder.length; x++) {
                if ((locationOrder[x] == Mech.LOC_CLEG) && !(mech instanceof TripodMech)) {
                    continue;
                }
                mech.initializeArmor(Integer.parseInt(armorValues[x].substring(armorValues[x].lastIndexOf(':') + 1)), locationOrder[x]);
                if (thisArmorType.equals(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                    boolean clan = armorValues[x].contains("Clan");
                    String armorName = armorValues[x].substring(armorValues[x].indexOf(':') + 1, armorValues[x].indexOf('('));
                    if (!armorName.contains("Clan") && !armorName.contains("IS")) {
                        if (clan) {
                            armorName = "Clan " + armorName;
                        } else {
                            armorName = "IS " + armorName;
                        }
                    }
                    mech.setArmorType(EquipmentType.getArmorType(EquipmentType.get(armorName)), locationOrder[x]);

                    String armorValue = armorValues[x].toLowerCase();
                    if (armorValue.contains("clan")) {
                        switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                            case 2:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_TW, locationOrder[x]);
                                break;
                            case 3:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED, locationOrder[x]);
                                break;
                            case 4:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_EXPERIMENTAL, locationOrder[x]);
                                break;
                            case 5:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_UNOFFICIAL, locationOrder[x]);
                                break;
                            default:
                                throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                        }
                    } else if (armorValue.contains("inner sphere")) {
                        switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                            case 1:
                                mech.setArmorTechLevel(TechConstants.T_INTRO_BOXSET, locationOrder[x]);
                                break;
                            case 2:
                                mech.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX, locationOrder[x]);
                                break;
                            case 3:
                                mech.setArmorTechLevel(TechConstants.T_IS_ADVANCED, locationOrder[x]);
                                break;
                            case 4:
                                mech.setArmorTechLevel(TechConstants.T_IS_EXPERIMENTAL, locationOrder[x]);
                                break;
                            case 5:
                                mech.setArmorTechLevel(TechConstants.T_IS_UNOFFICIAL, locationOrder[x]);
                                break;
                            default:
                                throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                        }
                    }
                }
            }

            for (int x = 0; x < rearLocationOrder.length; x++) {
                mech.initializeRearArmor(Integer.parseInt(armorValues[x + locationOrder.length].substring(10)), rearLocationOrder[x]);
            }

            // oog, crits.
            compactCriticals(mech);
            // we do these in reverse order to get the outermost
            // locations first, which is necessary for split crits to work
            for (int i = mech.locations() - 1; i >= 0; i--) {
                parseCrits(mech, i);
            }

            for (String equipment : noCritEquipment) {
                parseNoCritEquipment(mech, equipment);
            }

            if (mech instanceof LandAirMech) {
                // Set capital fighter stats for LAMs
                ((LandAirMech) mech).autoSetCapArmor();
                ((LandAirMech) mech).autoSetFatalThresh();
                int fuelTankCount = (int) mech.getEquipment().stream().filter(e -> e.is(EquipmentTypeLookup.LAM_FUEL_TANK)).count();
                ((LandAirMech) mech).setFuel(80 * (1 + fuelTankCount));
            }

            // add any heat sinks not allocated
            if (laserSinks) {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_LASER_HEAT_SINK);
            } else if (dblSinks) {
                // If the heat sink entry didn't specify Clan or IS double, check for sinks that take
                // critical slots. If none are found, default to the overall tech base of the unit.
                if (heatSinkBase == ITechnology.TECH_BASE_ALL) {
                    for (Mounted mounted : mech.getMisc()) {
                        if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                            heatSinkBase = mounted.getType().getTechBase();
                        }
                    }
                }
                boolean clan;
                switch (heatSinkBase) {
                    case ITechnology.TECH_BASE_IS:
                        clan = false;
                        break;
                    case ITechnology.TECH_BASE_CLAN:
                        clan = true;
                        break;
                    default:
                        clan = mech.isClan();
                }
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_DOUBLE_HEAT_SINK, clan);
            } else if (compactSinks) {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_COMPACT_HEAT_SINK);
            } else {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_HEAT_SINK);
            }

            if (mech.isOmni() && mech.hasEngine()) {
                if (baseHeatSinks >= 10) {
                    mech.getEngine().setBaseChassisHeatSinks(baseHeatSinks);
                } else {
                    mech.getEngine().setBaseChassisHeatSinks(expectedSinks);
                }
            }

            mech.getFluff().setCapabilities(capabilities);
            mech.getFluff().setOverview(overview);
            mech.getFluff().setDeployment(deployment);
            mech.getFluff().setHistory(history);
            mech.getFluff().setManufacturer(manufacturer);
            mech.getFluff().setPrimaryFactory(primaryFactory);
            mech.getFluff().setNotes(notes);
            systemManufacturers.forEach((k, v) -> mech.getFluff().setSystemManufacturer(k, v));
            systemModels.forEach((k, v) -> mech.getFluff().setSystemModel(k, v));
            mech.getFluff().setMMLImagePath(imagePath);

            mech.setArmorTonnage(mech.getArmorWeight());

            if (bv != 0) {
                mech.setUseManualBV(true);
                mech.setManualBV(bv);
            }
            return mech;
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            throw new Exception(ex);
        }
    }

    private void setTechLevel(Mech mech) throws EntityLoadingException {
        String techBase = this.techBase.substring(9).trim();
        if (techBase.equalsIgnoreCase("Inner Sphere")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 1:
                    mech.setTechLevel(TechConstants.T_INTRO_BOXSET);
                    break;
                case 2:
                    mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                    break;
                case 3:
                    mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                    break;
                case 4:
                    mech.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                    break;
                case 5:
                    mech.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
        } else if (techBase.equalsIgnoreCase("Clan")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2:
                    mech.setTechLevel(TechConstants.T_CLAN_TW);
                    break;
                case 3:
                    mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                    break;
                case 4:
                    mech.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                    break;
                case 5:
                    mech.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
        } else if (techBase.equalsIgnoreCase("Mixed (IS Chassis)")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2:
                    mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                    break;
                case 3:
                    mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                    break;
                case 4:
                    mech.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                    break;
                case 5:
                    mech.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
            mech.setMixedTech(true);
        } else if (techBase.equalsIgnoreCase("Mixed (Clan Chassis)")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2:
                    mech.setTechLevel(TechConstants.T_CLAN_TW);
                    break;
                case 3:
                    mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                    break;
                case 4:
                    mech.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                    break;
                case 5:
                    mech.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
            mech.setMixedTech(true);
        } else if (techBase.equalsIgnoreCase("Mixed")) {
            throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech base: " + techBase);
        }
    }

    private void parseCrits(Mech mech, int loc) throws EntityLoadingException {
        // check for removed arm actuators
        if (!(mech instanceof QuadMech)) {
            if ((loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM)) {
                String toCheck = critData[loc][3].toUpperCase().trim();
                if (toCheck.endsWith(ARMORED)) {
                    toCheck = toCheck.substring(0, toCheck.length() - ARMORED.length()).trim();
                }
                if (!toCheck.equalsIgnoreCase("Hand Actuator")) {
                    mech.setCritical(loc, 3, null);
                }
                toCheck = critData[loc][2].toUpperCase().trim();
                if (toCheck.endsWith(ARMORED)) {
                    toCheck = toCheck.substring(0, toCheck.length() - ARMORED.length()).trim();
                }
                if (!toCheck.equalsIgnoreCase("Lower Arm Actuator")) {
                    mech.setCritical(loc, 2, null);
                }
            }
        }

        // go thru file, add weapons
        for (int i = 0; i < mech.getNumberOfCriticals(loc); i++) {

            // parse out and add the critical
            String critName = critData[loc][i];

            critName = critName.trim();
            String critNameUpper = critName.toUpperCase();
            boolean rearMounted = false;
            boolean isArmored = false;
            boolean isTurreted = false;
            boolean isOmniPod = false;
            double size = 0.0;

            // Check for Armored Actuators
            if (critNameUpper.endsWith(ARMORED)) {
                critName = critName.substring(0, critName.length() - ARMORED.length()).trim();
                isArmored = true;
                critNameUpper = critName.toUpperCase();
            }

            if (critName.equalsIgnoreCase("Fusion Engine") || critName.equalsIgnoreCase("Engine")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Life Support")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Sensors")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Cockpit")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Gyro")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, true, isArmored));
                continue;
            } else if ((critName.contains("Actuator")) || critName.equalsIgnoreCase("Shoulder") || critName.equalsIgnoreCase("Hip")) {
                mech.getCritical(loc, i).setArmored(isArmored);
                continue;
            } else if (critName.equalsIgnoreCase("Landing Gear")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Avionics")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, true, isArmored));
                continue;
            }
            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }

            int sizeIndex = critNameUpper.indexOf(SIZE);
            if (sizeIndex > 0) {
                size = Double.parseDouble(critName.substring(sizeIndex + SIZE.length()));
                critNameUpper = critNameUpper.substring(0, sizeIndex);
            }
            if (critNameUpper.endsWith(OMNIPOD)) {
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - OMNIPOD.length()).trim();
                isOmniPod = true;
            }
            if (critNameUpper.endsWith("(T)")) {
                isTurreted = true;
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 3).trim();
            }
            if (critNameUpper.endsWith("(R)")) {
                rearMounted = true;
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 3).trim();
            }
            if (critNameUpper.endsWith("(SPLIT)")) {
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 7).trim();
            }
            // keep track of facing for vehicular grenade launchers
            int facing = -1;
            if (critNameUpper.endsWith("(FL)")) {
                facing = 5;
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 4).trim();
            }
            if (critNameUpper.endsWith("(FR)")) {
                facing = 1;
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 4).trim();
            }
            if (critNameUpper.endsWith("(RL)")) {
                facing = 4;
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 4).trim();
            }
            if (critNameUpper.endsWith("(RR)")) {
                facing = 2;
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - 4).trim();
            }
            critName = critName.substring(0, critNameUpper.length());
            EquipmentType etype2 = null;
            if (critName.contains("|")) {
                String critName2 = critName.substring(critName.indexOf("|") + 1);
                etype2 = EquipmentType.get(critName2);
                if (etype2 == null) {
                    etype2 = EquipmentType.get(mech.isClan() ? "Clan " + critName2 : "IS " + critName2);
                }
                critName = critName.substring(0, critName.indexOf("|"));
            }

            try {
                EquipmentType etype = EquipmentType.get(critName);
                if (etype == null) {
                    etype = EquipmentType.get(mech.isClan() ? "Clan " + critName : "IS " + critName);
                }
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these? Key on Type
                        Mounted m = hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mech.addCritical(loc, new CriticalSlot(m));
                            continue;
                        }
                        m = mech.addEquipment(etype, loc, rearMounted,
                                              BattleArmor.MOUNT_LOC_NONE, isArmored,
                                              isTurreted);
                        m.setOmniPodMounted(isOmniPod);
                        hSharedEquip.put(etype, m);
                    } else if (((etype instanceof WeaponType) && ((WeaponType) etype).isSplitable()) || ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_SPLITABLE))) {
                        // do we already have this one in this or an outer location?
                        Mounted m = null;
                        boolean bFound = false;
                        for (Mounted vSplitWeapon : vSplitWeapons) {
                            m = vSplitWeapon;
                            int nLoc = m.getLocation();
                            if ((((nLoc == loc) || (loc == Mech.getInnerLocation(nLoc)))
                                    || ((nLoc == Mech.LOC_CT) && (loc == Mech.LOC_HEAD)))
                                    && (m.getType() == etype)) {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound) {
                            m.setFoundCrits(m.getFoundCrits() + (mech.isSuperHeavy() ? 2 : 1));
                            if (m.getFoundCrits() >= m.getCriticals()) {
                                vSplitWeapons.remove(m);
                            }
                            // if we're in a new location, set the weapon as
                            // split
                            if (loc != m.getLocation()) {
                                m.setSplit(true);
                            }
                            // give the most restrictive location for arcs
                            int help = m.getLocation();
                            m.setLocation(Mech.mostRestrictiveLoc(loc, help));
                            if (loc != help) {
                                m.setSecondLocation(Mech.leastRestrictiveLoc(loc, help));
                            }
                        } else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setFoundCrits(1);
                            m.setArmored(isArmored);
                            m.setMechTurretMounted(isTurreted);
                            vSplitWeapons.add(m);
                        }
                        m.setArmored(isArmored);
                        m.setMechTurretMounted(isTurreted);
                        m.setOmniPodMounted(isOmniPod);
                        mech.addEquipment(m, loc, rearMounted);
                    } else {
                        Mounted mount;
                        if (etype2 == null) {
                            mount = mech.addEquipment(etype, loc, rearMounted,
                                                      BattleArmor.MOUNT_LOC_NONE, isArmored,
                                                      isTurreted, false, false, isOmniPod);
                        } else {
                            if (etype instanceof AmmoType) {
                                if (!(etype2 instanceof AmmoType) || (((AmmoType) etype).getAmmoType() != ((AmmoType) etype2).getAmmoType())) {
                                    throw new EntityLoadingException("Can't combine ammo for different weapons in one slot");
                                }
                            } else {
                                if (!(etype.equals(etype2)) || ((etype instanceof MiscType) && (!etype.hasFlag(MiscType.F_HEAT_SINK) && !etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)))) {
                                    throw new EntityLoadingException("must combine ammo or heatsinks in one slot");
                                }
                            }
                            mount = mech.addEquipment(etype, etype2, loc, isOmniPod, isArmored);
                        }
                        if (etype.isVariableSize()) {
                            if (size == 0.0) {
                                size = BLKFile.getLegacyVariableSize(critName);
                            }
                            mount.setSize(size);
                            // The size may require additional critical slots
                            // Account for loading Superheavy oversized Variable Size components
                            int critCount = mount.getCriticals();
                            if (mech.isSuperHeavy()){
                                critCount = (int)Math.ceil(critCount / 2.0);
                            }
                            for (int c = 1; c < critCount; c++) {
                                CriticalSlot cs = new CriticalSlot(mount);
                                mech.addCritical(loc, cs, i + c);
                            }
                        }

                        // vehicular grenade launchers need to have their facing
                        // set
                        if ((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_VGL)) {
                            if (facing == -1) {
                                // if facing has not been set earlier, we are
                                // front or rear mounted
                                if (rearMounted) {
                                    mount.setFacing(3);
                                } else {
                                    mount.setFacing(0);
                                }
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                    }
                } else {
                    if (!critName.equals(MtfFile.EMPTY)) {
                        // Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mech.addFailedEquipment(critName);
                        // Make the failed equipment an empty slot
                        critData[loc][i] = MtfFile.EMPTY;
                        // Compact criticals again
                        compactCriticals(mech, loc);
                        // Re-parse the same slot, since the compacting
                        // could have moved new equipment to this slot
                        i--;
                    }

                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }

    private void parseNoCritEquipment(Mech mech, String name) throws EntityLoadingException {
        int loc = Mech.LOC_NONE;
        int splitIndex = name.indexOf(":");
        if (splitIndex > 0) {
            loc = mech.getLocationFromAbbr(name.substring(splitIndex + 1));
            name = name.substring(0, splitIndex);
        }
        EquipmentType eq = EquipmentType.get(name);
        if (eq != null) {
            try {
                mech.addEquipment(eq, loc);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        } else {
            mech.addFailedEquipment(name);
        }
    }

    /**
     * This function moves all "empty" slots to the end of a location's critical
     * list. MegaMek adds equipment to the first empty slot available in a
     * location. This means that any "holes" (empty slots not at the end of a
     * location), will cause the file crits and MegaMek's crits to become out of
     * sync.
     */
    private void compactCriticals(Mech mech) {
        for (int loc = 0; loc < mech.locations(); loc++) {
            compactCriticals(mech, loc);
        }
    }

    private void compactCriticals(Mech mech, int loc) {
        if (loc == Mech.LOC_HEAD) {
            // This location has an empty slot inbetween systems crits
            // which will mess up parsing if compacted.
            return;
        }
        int firstEmpty = -1;
        for (int slot = 0; slot < mech.getNumberOfCriticals(loc); slot++) {
            if (critData[loc][slot] == null) {
                critData[loc][slot] = MtfFile.EMPTY;
            }

            if (critData[loc][slot].equals(MtfFile.EMPTY) && (firstEmpty == -1)) {
                firstEmpty = slot;
            }
            if ((firstEmpty != -1) && !critData[loc][slot].equals(MtfFile.EMPTY)) {
                // move this to the first empty slot
                critData[loc][firstEmpty] = critData[loc][slot];
                // mark the old slot empty
                critData[loc][slot] = MtfFile.EMPTY;
                // restart just after the moved slot's new location
                slot = firstEmpty;
                firstEmpty = -1;
            }
        }
    }

    private int getLocation(String location) {
        if (location.equalsIgnoreCase("Left Arm:") || location.equalsIgnoreCase("Front Left Leg:")) {
            return Mech.LOC_LARM;
        }

        if (location.equalsIgnoreCase("Right Arm:") || location.equalsIgnoreCase("Front Right Leg:")) {
            return Mech.LOC_RARM;
        }

        if (location.equalsIgnoreCase("Left Leg:") || location.equalsIgnoreCase("Rear Left Leg:")) {
            return Mech.LOC_LLEG;
        }

        if (location.equalsIgnoreCase("Right Leg:") || location.equalsIgnoreCase("Rear Right Leg:")) {
            return Mech.LOC_RLEG;
        }

        if (location.equalsIgnoreCase("Center Leg:")) {
            return Mech.LOC_CLEG;
        }

        if (location.equalsIgnoreCase("Left Torso:")) {
            return Mech.LOC_LT;
        }

        if (location.equalsIgnoreCase("Right Torso:")) {
            return Mech.LOC_RT;
        }

        if (location.equalsIgnoreCase("Center Torso:")) {
            return Mech.LOC_CT;
        }

        // else
        return Mech.LOC_HEAD;
    }

    private int getArmorLocation(String location) {

        int loc = -1;
        boolean rear = false;
        String locationName = location.toLowerCase();
        if (locationName.startsWith("la armor:") || locationName.startsWith("fll armor:")) {
            loc = Mech.LOC_LARM;
        } else if (locationName.startsWith("ra armor:") || locationName.startsWith("frl armor:")) {
            loc = Mech.LOC_RARM;
        } else if (locationName.startsWith("lt armor:")) {
            loc = Mech.LOC_LT;
        } else if (locationName.startsWith("rt armor:")) {
            loc = Mech.LOC_RT;
        } else if (locationName.startsWith("ct armor:")) {
            loc = Mech.LOC_CT;
        } else if (locationName.startsWith("hd armor:")) {
            loc = Mech.LOC_HEAD;
        } else if (locationName.startsWith("ll armor:") || locationName.startsWith("rll armor:")) {
            loc = Mech.LOC_LLEG;
        } else if (locationName.startsWith("rl armor:") || locationName.startsWith("rrl armor:")) {
            loc = Mech.LOC_RLEG;
        } else if (locationName.startsWith("rtl armor:")) {
            loc = Mech.LOC_LT;
            rear = true;
        } else if (locationName.startsWith("rtr armor:")) {
            loc = Mech.LOC_RT;
            rear = true;
        } else if (locationName.startsWith("rtc armor:")) {
            loc = Mech.LOC_CT;
            rear = true;
        } else if (locationName.startsWith("cl armor:")) {
            loc = Mech.LOC_CLEG;
        }

        if (!rear) {
            for (int pos = 0; pos < locationOrder.length; pos++) {
                if (locationOrder[pos] == loc) {
                    loc = pos;
                    break;
                }
            }
        } else {
            for (int pos = 0; pos < rearLocationOrder.length; pos++) {
                if (rearLocationOrder[pos] == loc) {
                    loc = pos + locationOrder.length;
                    break;
                }
            }
        }
        return loc;
    }

    private boolean isValidLocation(String location) {
        return location.equalsIgnoreCase("Left Arm:")
                || location.equalsIgnoreCase("Right Arm:")
                || location.equalsIgnoreCase("Left Leg:")
                || location.equalsIgnoreCase("Right Leg:")
                || location.equalsIgnoreCase("Center Leg:")
                || location.equalsIgnoreCase("Front Left Leg:")
                || location.equalsIgnoreCase("Front Right Leg:")
                || location.equalsIgnoreCase("Rear Left Leg:")
                || location.equalsIgnoreCase("Rear Right Leg:")
                || location.equalsIgnoreCase("Left Torso:")
                || location.equalsIgnoreCase("Right Torso:")
                || location.equalsIgnoreCase("Center Torso:")
                || location.equalsIgnoreCase("Head:");
    }

    private boolean isProcessedComponent(String line) {
        String lineLower = line.toLowerCase();
        if (lineLower.startsWith(COCKPIT)) {
            cockpitType = line;
            return true;
        }

        if (lineLower.startsWith(GYRO)) {
            gyroType = line;
            return true;
        }

        if (lineLower.startsWith(MOTIVE)) {
            motiveType = line;
            return true;
        }

        if (lineLower.startsWith(EJECTION)) {
            ejectionType = line;
            return true;
        }

        if (lineLower.startsWith(MASS)) {
            tonnage = line;
            return true;
        }

        if (lineLower.startsWith(ENGINE)) {
            engine = line;
            return true;
        }

        if (lineLower.startsWith(STRUCTURE)) {
            internalType = line;
            return true;
        }

        if (lineLower.startsWith(MYOMER)) {
            return true;
        }

        if (lineLower.startsWith(LAM)) {
            lamType = line;
            return true;
        }

        if (lineLower.startsWith(CONFIG)) {
            chassisConfig = line;
            return true;
        }

        if (lineLower.startsWith(TECH_BASE)) {
            techBase = line;
            return true;
        }

        if (lineLower.startsWith(ERA)) {
            techYear = line;
            return true;
        }

        if (lineLower.startsWith(SOURCE)) {
            source = line;
            return true;
        }

        if (lineLower.startsWith(RULES_LEVEL)) {
            rulesLevel = line;
            return true;
        }

        if (lineLower.startsWith(HEAT_SINKS)) {
            heatSinks = line;
            return true;
        }

        if (lineLower.startsWith(BASE_CHASSIS_HEAT_SINKS)) {
            baseChassieHeatSinks = line;
            return true;
        }

        if (lineLower.startsWith(WALK_MP)) {
            return true;
        }
        if (lineLower.startsWith(JUMP_MP)) {
            jumpMP = line;
            return true;
        }

        if (lineLower.startsWith(ARMOR)) {
            armorType = line;
            return true;
        }

        if (lineLower.startsWith(NO_CRIT)) {
            noCritEquipment.add(line.substring(NO_CRIT.length()));
            return true;
        }

        if (lineLower.startsWith(OVERVIEW)) {
            overview = line.substring(OVERVIEW.length());
            return true;
        }

        if (lineLower.startsWith(CAPABILITIES)) {
            capabilities = line.substring(CAPABILITIES.length());
            return true;
        }

        if (lineLower.startsWith(DEPLOYMENT)) {
            deployment = line.substring(DEPLOYMENT.length());
            return true;
        }

        if (lineLower.startsWith(HISTORY)) {
            history = line.substring(HISTORY.length());
            return true;
        }

        if (lineLower.startsWith(MANUFACTURER)) {
            manufacturer = line.substring(MANUFACTURER.length());
            return true;
        }

        if (lineLower.startsWith(PRIMARY_FACTORY)) {
            primaryFactory = line.substring(PRIMARY_FACTORY.length());
            return true;
        }

        if (lineLower.startsWith(SYSTEM_MANUFACTURER)) {
            String[] fields = line.split(":");
            if (fields.length > 2) {
                EntityFluff.System system = EntityFluff.System.parse(fields[1]);
                if (null != system) {
                    systemManufacturers.put(system, fields[2].trim());
                }
            }
            return true;
        }

        if (lineLower.startsWith(SYSTEM_MODEL)) {
            String[] fields = line.split(":");
            if (fields.length > 2) {
                EntityFluff.System system = EntityFluff.System.parse(fields[1]);
                if (null != system) {
                    systemModels.put(system, fields[2].trim());
                }
            }
            return true;
        }

        if (lineLower.startsWith(NOTES)) {
            notes = line.substring(NOTES.length());
            return true;
        }

        if (lineLower.startsWith(IMAGE_FILE)) {
            imagePath = line.substring(IMAGE_FILE.length());
            return true;
        }

        if (lineLower.startsWith(BV)) {
            bv = Integer.parseInt(line.substring(BV.length()));
            return true;
        }

        if (lineLower.startsWith(MUL_ID)) {
            mulId = Integer.parseInt(line.substring(MUL_ID.length()));
            return true;
        }

        return false;
    }

    private int weaponsList(String line) {
        if (line.toLowerCase().startsWith(WEAPONS)) {
            return Integer.parseInt(line.substring(WEAPONS.length()));
        }
        return -1;
    }
}
