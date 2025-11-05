/*
 * Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import megamek.codeUtilities.StringUtility;
import megamek.common.CriticalSlot;
import megamek.common.QuirkEntry;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.LiftHoist;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.QuadVee;
import megamek.common.units.System;
import megamek.common.units.TripodMek;
import megamek.common.units.UnitRole;
import megamek.logging.MMLogger;

/**
 * This class represents mtf files which are used to store Meks. The class contains the file reader while the mtf file
 * generation is currently located in {@link Mek#getMtf()}.
 *
 * @author Ben
 * @author Simon (Juliez)
 */
public class MtfFile implements IMekLoader {
    private static final MMLogger logger = MMLogger.create(MtfFile.class);

    private String chassis;
    private String model;
    private String clanChassisName = "";
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
    private String heatSinkKit;

    private String heatSinks;
    private String jumpMP;
    private String baseChassisHeatSinks = "base chassis heat sinks:-1";

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
    private final Map<System, String> systemManufacturers = new EnumMap<>(System.class);
    private final Map<System, String> systemModels = new EnumMap<>(System.class);
    private String notes = "";

    private String fluffImageEncoded = "";
    private String iconEncoded = "";

    private int bv = 0;
    private String role;

    private final Map<EquipmentType, Mounted<?>> hSharedEquip = new HashMap<>();
    private final List<Mounted<?>> vSplitWeapons = new ArrayList<>();

    private final List<String> quirkLines = new ArrayList<>();

    public static final int[] locationOrder = { Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_TORSO,
                                                Mek.LOC_RIGHT_TORSO, Mek.LOC_CENTER_TORSO,
                                                Mek.LOC_HEAD, Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG, Mek.LOC_CENTER_LEG };
    public static final int[] rearLocationOrder = { Mek.LOC_LEFT_TORSO, Mek.LOC_RIGHT_TORSO, Mek.LOC_CENTER_TORSO };

    public static final String COMMENT = "#";
    public static final String MTF_VERSION = "version:";
    public static final String GENERATOR = "generator:";
    public static final String CHASSIS = "chassis:";
    public static final String CLAN_CHASSIS_NAME = "clanname:";
    public static final String MODEL = "model:";
    public static final String COCKPIT = "cockpit:";
    public static final String GYRO = "gyro:";
    public static final String MOTIVE = "motive:";
    public static final String EJECTION = "ejection:";
    public static final String HEAT_SINK_KIT = "heat sink kit:";
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
    public static final String BV = "bv:";
    public static final String WEAPONS = "weapons:";
    public static final String EMPTY = "-Empty-";
    public static final String ARMORED = "(ARMORED)";
    public static final String OMNI_POD = "(OMNIPOD)";
    public static final String NO_CRIT = "nocrit:";
    public static final String SIZE = ":SIZE:";
    public static final String MUL_ID = "mul id:";
    public static final String QUIRK = "quirk:";
    public static final String WEAPON_QUIRK = "weaponquirk:";
    public static final String ROLE = "role:";
    public static final String FLUFF_IMAGE = "fluffimage:";
    public static final String ICON = "icon:";

    private static final Pattern LEGACY_SIZE_PATTERN = Pattern.compile("\\((\\d+(?:\\.\\d+)?)\\s*(?:ton|tons|m|kg)\\)",
          Pattern.CASE_INSENSITIVE);

    /**
     * Modern unit files store resizable equipment like Cargo:SIZE:4.0 Old equipment stores it like Cargo (4 tons) This
     * is a helper method to parse old-style resizable equipment
     *
     * @param eqName The name of the equipment (including the size designator)
     *
     * @return The parsed size of the equipment
     */
    public static double extractLegacySize(String eqName) {
        var m = LEGACY_SIZE_PATTERN.matcher(eqName);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        } else {
            return 0;
        }
    }

    public MtfFile(InputStream is) throws EntityLoadingException {
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
              BufferedReader r = new BufferedReader(isr)) {
            critData = new String[9][12];
            readLines(r);
        } catch (IOException ex) {
            logger.error("", ex);
            throw new EntityLoadingException("I/O Error reading file");
        } catch (StringIndexOutOfBoundsException ex) {
            logger.error("", ex);
            throw new EntityLoadingException("StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
            logger.error("", ex);
            throw new EntityLoadingException("NumberFormatException reading file (format error)");
        }
    }

    @Override
    public Entity getEntity() throws Exception {
        try {
            Mek mek;

            int iGyroType;
            try {
                iGyroType = Mek.getGyroTypeForString(gyroType.substring(5));
                if (iGyroType == Mek.GYRO_UNKNOWN) {
                    iGyroType = Mek.GYRO_STANDARD;
                }
            } catch (Exception ignored) {
                iGyroType = Mek.GYRO_STANDARD;
            }

            int iCockpitType;
            try {
                iCockpitType = Mek.getCockpitTypeForString(cockpitType.substring(8));
                if (iCockpitType == Mek.COCKPIT_UNKNOWN) {
                    iCockpitType = Mek.COCKPIT_STANDARD;
                }
            } catch (Exception ignored) {
                iCockpitType = Mek.COCKPIT_STANDARD;
            }
            boolean fullHead;
            try {
                fullHead = ejectionType.substring(9).equals(Mek.FULL_HEAD_EJECT_STRING);
            } catch (Exception ignored) {
                fullHead = false;
            }
            boolean riscHeatSinkKit;
            try {
                riscHeatSinkKit = heatSinkKit.substring(HEAT_SINK_KIT.length()).equals(Mek.RISC_HEAT_SINK_OVERRIDE_KIT);
            } catch (Exception ignored) {
                riscHeatSinkKit = false;
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
                mek = new QuadVee(iGyroType, iMotiveType);
            } else if (chassisConfig.contains("Quad")) {
                mek = new QuadMek(iGyroType, iCockpitType);
            } else if (chassisConfig.contains("LAM")) {
                int iLAMType;
                try {
                    iLAMType = LandAirMek.getLAMTypeForString(lamType.substring(4));
                    if (iLAMType == LandAirMek.LAM_UNKNOWN) {
                        iLAMType = LandAirMek.LAM_STANDARD;
                    }
                } catch (Exception ignored) {
                    iLAMType = LandAirMek.LAM_STANDARD;
                }
                mek = new LandAirMek(iGyroType, iCockpitType, iLAMType);
            } else if (chassisConfig.contains("Tripod")) {
                mek = new TripodMek(iGyroType, iCockpitType);
            } else {
                mek = new BipedMek(iGyroType, iCockpitType);
            }
            mek.setFullHeadEject(fullHead);
            mek.setRiscHeatSinkOverrideKit(riscHeatSinkKit);
            mek.setChassis(chassis.trim());
            mek.setClanChassisName(clanChassisName);
            mek.setModel(model.trim());
            mek.setMulId(mulId);
            mek.setYear(Integer.parseInt(techYear.substring(4).trim()));
            mek.setSource(source.substring("Source:".length()).trim());
            if (StringUtility.isNullOrBlank(role)) {
                mek.setUnitRole(UnitRole.UNDETERMINED);
            } else {
                mek.setUnitRole(UnitRole.parseRole(role));
            }

            if (chassisConfig.contains("Omni")) {
                mek.setOmni(true);
            }
            setTechLevel(mek);
            mek.setWeight(Integer.parseInt(tonnage.substring(5)));

            int engineFlags = 0;
            if ((mek.isClan() && !mek.isMixedTech())
                  || (mek.isMixedTech() && mek.isClan() && !mek.itemOppositeTech(engine))
                  || (mek.isMixedTech() && !mek.isClan() && mek.itemOppositeTech(engine))) {
                engineFlags = Engine.CLAN_ENGINE;
            }
            if (mek.isSuperHeavy()) {
                engineFlags |= Engine.SUPERHEAVY_ENGINE;
            }

            int engineRating = Integer.parseInt(engine.substring(engine.indexOf(":") + 1, engine.indexOf(" ")));
            mek.setEngine(new Engine(engineRating, Engine.getEngineTypeByString(engine), engineFlags));

            mek.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));

            boolean dblSinks = heatSinks.contains(HS_DOUBLE);
            boolean laserSinks = heatSinks.contains(HS_LASER);
            boolean compactSinks = heatSinks.contains(HS_COMPACT);
            int expectedSinks = Integer.parseInt(heatSinks.substring(11, 13).trim());
            int baseHeatSinks = Integer
                  .parseInt(baseChassisHeatSinks.substring("base chassis heat sinks:".length()).trim());

            String thisStructureType = internalType.substring(internalType.indexOf(':') + 1);
            if (!thisStructureType.isBlank()) {
                mek.setStructureType(thisStructureType);
            } else {
                mek.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
            }
            mek.autoSetInternal();

            String thisArmorType = armorType.substring(armorType.indexOf(':') + 1);
            if (thisArmorType.indexOf('(') != -1) {
                boolean clan = thisArmorType.toLowerCase().contains("clan");
                if (clan) {
                    switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                        case 2:
                            mek.setArmorTechLevel(TechConstants.T_CLAN_TW);
                            break;
                        case 3:
                            mek.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED);
                            break;
                        case 4:
                            mek.setArmorTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                            break;
                        case 5:
                            mek.setArmorTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                            break;
                        default:
                            throw new EntityLoadingException(
                                  "Unsupported tech level: " + rulesLevel.substring(12).trim());
                    }
                } else {
                    switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                        case 1:
                            mek.setArmorTechLevel(TechConstants.T_INTRO_BOX_SET);
                            break;
                        case 2:
                            mek.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);
                            break;
                        case 3:
                            mek.setArmorTechLevel(TechConstants.T_IS_ADVANCED);
                            break;
                        case 4:
                            mek.setArmorTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                            break;
                        case 5:
                            mek.setArmorTechLevel(TechConstants.T_IS_UNOFFICIAL);
                            break;
                        default:
                            throw new EntityLoadingException(
                                  "Unsupported tech level: " + rulesLevel.substring(12).trim());
                    }
                }
                thisArmorType = thisArmorType.substring(0, thisArmorType.indexOf('(')).trim();
                mek.setArmorType(thisArmorType);
            } else if (!thisArmorType.equals(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                mek.setArmorTechLevel(mek.getTechLevel());
                mek.setArmorType(thisArmorType);
            }

            if (thisArmorType.isBlank()) {
                mek.setArmorType(EquipmentType.T_ARMOR_STANDARD);
            }
            mek.recalculateTechAdvancement();

            for (int x = 0; x < locationOrder.length; x++) {
                if ((locationOrder[x] == Mek.LOC_CENTER_LEG) && !(mek instanceof TripodMek)) {
                    continue;
                }
                mek.initializeArmor(Integer.parseInt(armorValues[x].substring(armorValues[x].lastIndexOf(':') + 1)),
                      locationOrder[x]);
                if (thisArmorType.equals(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                    String armorName = isClan(x);
                    mek.setArmorType(EquipmentType.getArmorType(EquipmentType.get(armorName)), locationOrder[x]);

                    String armorValue = armorValues[x].toLowerCase();
                    if (armorValue.contains("clan")) {
                        switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                            case 2:
                                mek.setArmorTechLevel(TechConstants.T_CLAN_TW, locationOrder[x]);
                                break;
                            case 3:
                                mek.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED, locationOrder[x]);
                                break;
                            case 4:
                                mek.setArmorTechLevel(TechConstants.T_CLAN_EXPERIMENTAL, locationOrder[x]);
                                break;
                            case 5:
                                mek.setArmorTechLevel(TechConstants.T_CLAN_UNOFFICIAL, locationOrder[x]);
                                break;
                            default:
                                throw new EntityLoadingException(
                                      "Unsupported tech level: " + rulesLevel.substring(12).trim());
                        }
                    } else if (armorValue.contains("inner sphere")) {
                        switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                            case 1:
                                mek.setArmorTechLevel(TechConstants.T_INTRO_BOX_SET, locationOrder[x]);
                                break;
                            case 2:
                                mek.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX, locationOrder[x]);
                                break;
                            case 3:
                                mek.setArmorTechLevel(TechConstants.T_IS_ADVANCED, locationOrder[x]);
                                break;
                            case 4:
                                mek.setArmorTechLevel(TechConstants.T_IS_EXPERIMENTAL, locationOrder[x]);
                                break;
                            case 5:
                                mek.setArmorTechLevel(TechConstants.T_IS_UNOFFICIAL, locationOrder[x]);
                                break;
                            default:
                                throw new EntityLoadingException(
                                      "Unsupported tech level: " + rulesLevel.substring(12).trim());
                        }
                    }
                }
            }

            for (int x = 0; x < rearLocationOrder.length; x++) {
                mek.initializeRearArmor(Integer.parseInt(armorValues[x + locationOrder.length].substring(10)),
                      rearLocationOrder[x]);
            }

            // oog, crits.
            compactCriticalSlots(mek);
            // we do these in reverse order to get the outermost
            // locations first, which is necessary for split crits to work
            for (int i = mek.locations() - 1; i >= 0; i--) {
                parseCrits(mek, i);
            }

            for (String equipment : noCritEquipment) {
                parseNoCritEquipment(mek, equipment);
            }

            if (mek instanceof LandAirMek) {
                // Set capital fighter stats for LAMs
                ((LandAirMek) mek).autoSetCapArmor();
                ((LandAirMek) mek).autoSetFatalThresh();
                int fuelTankCount = (int) mek.getEquipment().stream()
                      .filter(e -> e.is(EquipmentTypeLookup.LAM_FUEL_TANK)).count();
                ((LandAirMek) mek).setFuel(80 * (1 + fuelTankCount));
            }

            // add any heat sinks not allocated
            if (laserSinks) {
                mek.addEngineSinks(expectedSinks - mek.heatSinks(), MiscType.F_LASER_HEAT_SINK);
            } else if (dblSinks) {
                // If the heat sink entry didn't specify Clan or IS double, check for sinks that
                // take critical slots. If none are found, default to the overall tech base of
                // the unit.
                boolean clan;

                TechBase heatSinkBase = TechBase.ALL;

                if (heatSinks.contains(TECH_BASE_CLAN)) {
                    heatSinkBase = TechBase.CLAN;
                } else if (heatSinks.contains(TECH_BASE_IS)) {
                    heatSinkBase = TechBase.IS;
                }

                clan = switch (heatSinkBase) {
                    case IS -> false;
                    case CLAN -> true;
                    default -> mek.isClan();
                };

                mek.addEngineSinks(expectedSinks - mek.heatSinks(), MiscType.F_DOUBLE_HEAT_SINK, clan);
            } else if (compactSinks) {
                mek.addEngineSinks(expectedSinks - mek.heatSinks(), MiscType.F_COMPACT_HEAT_SINK);
            } else {
                mek.addEngineSinks(expectedSinks - mek.heatSinks(), MiscType.F_HEAT_SINK);
            }

            if (mek.isOmni() && mek.hasEngine()) {
                if (baseHeatSinks >= 10) {
                    mek.getEngine().setBaseChassisHeatSinks(baseHeatSinks);
                } else {
                    mek.getEngine().setBaseChassisHeatSinks(expectedSinks);
                }
            }

            mek.getFluff().setCapabilities(capabilities);
            mek.getFluff().setOverview(overview);
            mek.getFluff().setDeployment(deployment);
            mek.getFluff().setHistory(history);
            mek.getFluff().setManufacturer(manufacturer);
            mek.getFluff().setPrimaryFactory(primaryFactory);
            mek.getFluff().setNotes(notes);
            mek.getFluff().setFluffImage(fluffImageEncoded);
            mek.setIcon(iconEncoded);
            systemManufacturers.forEach((k, v) -> mek.getFluff().setSystemManufacturer(k, v));
            systemModels.forEach((k, v) -> mek.getFluff().setSystemModel(k, v));

            mek.setArmorTonnage(mek.getArmorWeight());

            if (bv != 0) {
                mek.setUseManualBV(true);
                mek.setManualBV(bv);
            }

            List<QuirkEntry> quirks = getQuirkEntries();
            mek.loadQuirks(quirks);

            return mek;
        } catch (Exception ex) {
            logger.error("", ex);
            throw new Exception(ex);
        }
    }

    private List<QuirkEntry> getQuirkEntries() {
        List<QuirkEntry> quirks = new ArrayList<>();
        for (String quirkLine : quirkLines) {
            if (quirkLine.startsWith(QUIRK)) {
                QuirkEntry quirkEntry = new QuirkEntry(quirkLine.substring(QUIRK.length()));
                quirks.add(quirkEntry);
            } else if (quirkLine.startsWith(WEAPON_QUIRK)) {
                String[] fields = quirkLine.substring(WEAPON_QUIRK.length()).split(":");
                int slot = Integer.parseInt(fields[2]);
                QuirkEntry quirkEntry = new QuirkEntry(fields[0], fields[1], slot, fields[3]);
                quirks.add(quirkEntry);
            }
        }
        return quirks;
    }

    private String isClan(int x) {
        boolean clan = armorValues[x].contains("Clan");
        String armorName = armorValues[x].substring(armorValues[x].indexOf(':') + 1,
              armorValues[x].indexOf('('));
        if (!armorName.contains("Clan") && !armorName.contains("IS")) {
            if (clan) {
                armorName = "Clan " + armorName;
            } else {
                armorName = "IS " + armorName;
            }
        }
        return armorName;
    }

    private String readLineIgnoringComments(BufferedReader reader) throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                return "";
            } else if (!line.startsWith(COMMENT)) {
                return line;
            }
        }
    }

    private void readLines(BufferedReader r) throws IOException {
        int slot = 0;
        int loc = 0;
        int weaponsCount;
        int armorLocation;
        while (r.ready()) {
            String line = r.readLine().trim();

            if (line.isBlank() || line.startsWith(COMMENT) || line.startsWith(GENERATOR)) {
                continue;
            }

            if (line.toLowerCase().startsWith(MTF_VERSION)) {
                // Reading the version, chassis and model as the first three lines without
                // header is kept
                // for backward compatibility for user-generated units. However, the version is
                // no longer checked
                // for correct values as that makes no difference so long as the unit can be
                // loaded
                // Version 1.0: Initial version.
                // Version 1.1: Added level 3 cockpit and gyro options.
                // version 1.2: added full head ejection
                // Version 1.3: Added MUL ID

                String generatorOrChassis = readLineIgnoringComments(r);
                if (generatorOrChassis.toLowerCase().startsWith(GENERATOR)) {
                    // Compatibility with SSW 0.7.6.1 - Generator: comes between Version and chassis
                    chassis = readLineIgnoringComments(r);
                } else {
                    chassis = generatorOrChassis;
                }
                model = readLineIgnoringComments(r);
                continue;
            }

            if (isTitleLine(line)) {
                continue;
            }

            if (isValidLocation(line)) {
                loc = getLocation(line);
                slot = 0;
                continue;
            }

            if (isProcessedComponent(line)) {
                continue;
            }

            weaponsCount = weaponsList(line);

            if (weaponsCount > 0) {
                for (int count = 0; count < weaponsCount; count++) {
                    r.readLine();
                }
                continue;
            }

            armorLocation = getArmorLocation(line);

            if (armorLocation >= 0) {
                armorValues[armorLocation] = line;
                continue;
            }
            if (critData.length <= loc) {
                continue;
            }
            if (critData[loc].length <= slot) {
                continue;
            }
            critData[loc][slot++] = line.trim();
        }
    }

    private void setTechLevel(Mek mek) throws EntityLoadingException {
        String techBase = this.techBase.substring(9).trim();
        if (techBase.equalsIgnoreCase("Inner Sphere")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 1:
                    mek.setTechLevel(TechConstants.T_INTRO_BOX_SET);
                    break;
                case 2:
                    mek.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                    break;
                case 3:
                    mek.setTechLevel(TechConstants.T_IS_ADVANCED);
                    break;
                case 4:
                    mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                    break;
                case 5:
                    mek.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
        } else if (techBase.equalsIgnoreCase("Clan")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2:
                    mek.setTechLevel(TechConstants.T_CLAN_TW);
                    break;
                case 3:
                    mek.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                    break;
                case 4:
                    mek.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                    break;
                case 5:
                    mek.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
        } else if (techBase.equalsIgnoreCase("Mixed (IS Chassis)")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2:
                    mek.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                    break;
                case 3:
                    mek.setTechLevel(TechConstants.T_IS_ADVANCED);
                    break;
                case 4:
                    mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                    break;
                case 5:
                    mek.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
            mek.setMixedTech(true);
        } else if (techBase.equalsIgnoreCase("Mixed (Clan Chassis)")) {
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2:
                    mek.setTechLevel(TechConstants.T_CLAN_TW);
                    break;
                case 3:
                    mek.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                    break;
                case 4:
                    mek.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                    break;
                case 5:
                    mek.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                    break;
                default:
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            }
            mek.setMixedTech(true);
        } else if (techBase.equalsIgnoreCase("Mixed")) {
            throw new EntityLoadingException(
                  "Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech base: " + techBase);
        }
    }

    private void parseCrits(Mek mek, int loc) throws EntityLoadingException {
        // check for removed arm actuators
        if (!(mek instanceof QuadMek)) {
            if ((loc == Mek.LOC_LEFT_ARM) || (loc == Mek.LOC_RIGHT_ARM)) {
                String toCheck = critData[loc][3].toUpperCase().trim();
                if (toCheck.endsWith(ARMORED)) {
                    toCheck = toCheck.substring(0, toCheck.length() - ARMORED.length()).trim();
                }
                if (!toCheck.equalsIgnoreCase("Hand Actuator")) {
                    mek.setCritical(loc, 3, null);
                }
                toCheck = critData[loc][2].toUpperCase().trim();
                if (toCheck.endsWith(ARMORED)) {
                    toCheck = toCheck.substring(0, toCheck.length() - ARMORED.length()).trim();
                }
                if (!toCheck.equalsIgnoreCase("Lower Arm Actuator")) {
                    mek.setCritical(loc, 2, null);
                }
            }
        }

        // go through file, add weapons
        for (int i = 0; i < mek.getNumberOfCriticalSlots(loc); i++) {

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
                mek.setCritical(loc, i,
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Life Support")) {
                mek.setCritical(loc, i,
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Sensors")) {
                mek.setCritical(loc, i,
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Cockpit")) {
                mek.setCritical(loc, i,
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Gyro")) {
                mek.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, true, isArmored));
                continue;
            } else if ((critName.contains("Actuator")) || critName.equalsIgnoreCase("Shoulder")
                  || critName.equalsIgnoreCase("Hip")) {
                mek.getCritical(loc, i).setArmored(isArmored);
                continue;
            } else if (critName.equalsIgnoreCase("Landing Gear")) {
                mek.setCritical(loc, i,
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Avionics")) {
                mek.setCritical(loc, i,
                      new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, true, isArmored));
                continue;
            }
            // if the slot's full already, skip it.
            if (mek.getCritical(loc, i) != null) {
                continue;
            }

            int sizeIndex = critNameUpper.indexOf(SIZE);
            if (sizeIndex > 0) {
                size = Double.parseDouble(critName.substring(sizeIndex + SIZE.length()));
                critNameUpper = critNameUpper.substring(0, sizeIndex);
            }
            if (critNameUpper.endsWith(OMNI_POD)) {
                critNameUpper = critNameUpper.substring(0, critNameUpper.length() - OMNI_POD.length()).trim();
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
                    etype2 = EquipmentType.get(mek.isClan() ? "Clan " + critName2 : "IS " + critName2);
                }
                critName = critName.substring(0, critName.indexOf("|"));
            }

            try {
                EquipmentType etype = EquipmentType.get(critName);
                if (etype == null) {
                    etype = EquipmentType.get(mek.isClan() ? "Clan " + critName : "IS " + critName);
                }
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these? Key on Type
                        Mounted<?> m = hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mek.addCritical(loc, new CriticalSlot(m));
                            continue;
                        }
                        m = mek.addEquipment(etype, loc, rearMounted,
                              BattleArmor.MOUNT_LOC_NONE, isArmored,
                              isTurreted);
                        m.setOmniPodMounted(isOmniPod);
                        hSharedEquip.put(etype, m);
                        if (etype.is(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER)) {
                            if (size == 0) {
                                // legacy MTF loading: MJB gave their MP as the jump MP
                                size = mek.getOriginalJumpMP(true);
                                mek.setOriginalJumpMP(0);
                            }

                            m.setSize(size);
                        }
                    } else if (etype instanceof MiscType && etype.hasFlag(MiscType.F_TARGETING_COMPUTER)) {
                        // Targeting computers are special, they need to be loaded like spreadable
                        // equipment, but they aren't spreadable
                        Mounted<?> m = hSharedEquip.get(etype);
                        if (m == null) {
                            m = mek.addTargCompWithoutSlots((MiscType) etype, loc, isOmniPod, isArmored);
                            hSharedEquip.put(etype, m);
                        }
                        mek.addCritical(loc, new CriticalSlot(m));

                    } else if (((etype instanceof WeaponType) && ((WeaponType) etype).isSplittableOverCriticalSlots())
                          || ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_SPLITABLE))) {
                        // do we already have this one in this or an outer location?
                        Mounted<?> m = null;
                        boolean bFound = false;
                        for (Mounted<?> vSplitWeapon : vSplitWeapons) {
                            m = vSplitWeapon;
                            int nLoc = m.getLocation();
                            if ((((nLoc == loc) || (loc == Mek.getInnerLocation(nLoc)))
                                  || ((nLoc == Mek.LOC_CENTER_TORSO) && (loc == Mek.LOC_HEAD)))
                                  && (m.getType() == etype)) {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound) {
                            m.setFoundCrits(m.getFoundCrits() + (mek.isSuperHeavy() ? 2 : 1));
                            if (m.getFoundCrits() >= m.getNumCriticalSlots()) {
                                vSplitWeapons.remove(m);
                            }
                            // if we're in a new location, set the weapon as
                            // split
                            if (loc != m.getLocation()) {
                                m.setSplit(true);
                            }
                            // give the most restrictive location for arcs
                            int help = m.getLocation();
                            m.setLocation(Mek.mostRestrictiveLoc(loc, help));
                            if (loc != help) {
                                m.setSecondLocation(Mek.leastRestrictiveLoc(loc, help));
                            }
                        } else {
                            // make a new one
                            m = Mounted.createMounted(mek, etype);
                            m.setFoundCrits(1);
                            m.setArmored(isArmored);
                            m.setMekTurretMounted(isTurreted);
                            vSplitWeapons.add(m);
                        }
                        m.setArmored(isArmored);
                        m.setMekTurretMounted(isTurreted);
                        m.setOmniPodMounted(isOmniPod);
                        mek.addEquipment(m, loc, rearMounted);
                    } else {
                        Mounted<?> mount;
                        if (etype2 == null) {
                            mount = mek.addEquipment(etype, loc, rearMounted,
                                  BattleArmor.MOUNT_LOC_NONE, isArmored,
                                  isTurreted, false, false, isOmniPod);
                        } else {
                            if (etype instanceof AmmoType) {
                                if (!(etype2 instanceof AmmoType)
                                      || (((AmmoType) etype).getAmmoType() != ((AmmoType) etype2).getAmmoType())) {
                                    throw new EntityLoadingException(
                                          "Can't combine ammo for different weapons in one slot");
                                }
                            } else {
                                if (!(etype.equals(etype2))
                                      || ((etype instanceof MiscType) && (!etype.hasFlag(MiscType.F_HEAT_SINK)
                                      && !etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)))) {
                                    throw new EntityLoadingException("must combine ammo or heat sinks in one slot");
                                }
                            }
                            mount = mek.addEquipment(etype, etype2, loc, isOmniPod, isArmored);
                        }
                        if (etype.isVariableSize()) {
                            if (size == 0) {
                                size = extractLegacySize(critName);
                            }

                            mount.setSize(size);
                            // The size may require additional critical slots
                            // Account for loading Superheavy oversized Variable Size components
                            int critCount = mount.getNumCriticalSlots();
                            if (mek.isSuperHeavy()) {
                                critCount = (int) Math.ceil(critCount / 2.0);
                            }
                            for (int c = 1; c < critCount; c++) {
                                CriticalSlot cs = new CriticalSlot(mount);
                                mek.addCritical(loc, cs, i + c);
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
                        if (etype instanceof MiscType && mount.getType().hasFlag(MiscType.F_LIFT_HOIST)) {
                            // Cargo container too?
                            mek.addTransporter(new LiftHoist(mount, mek.getWeight() / 2), isOmniPod);
                        }
                    }
                } else {
                    if (!critName.equals(MtfFile.EMPTY)) {
                        // Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mek.addFailedEquipment(critName);
                        // Make the failed equipment an empty slot
                        critData[loc][i] = MtfFile.EMPTY;
                        // Compact criticalSlots again
                        compactCriticalSlots(mek, loc);
                        // Reparse the same slot, since the compacting
                        // could have moved new equipment to this slot
                        i--;
                    }

                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }

    private void parseNoCritEquipment(Mek mek, String name) throws EntityLoadingException {
        int loc = Mek.LOC_NONE;
        int splitIndex = name.indexOf(":");
        if (splitIndex > 0) {
            loc = mek.getLocationFromAbbr(name.substring(splitIndex + 1));
            name = name.substring(0, splitIndex);
        }
        EquipmentType eq = EquipmentType.get(name);
        if (eq != null) {
            try {
                mek.addEquipment(eq, loc);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        } else {
            mek.addFailedEquipment(name);
        }
    }

    /**
     * This function moves all "empty" slots to the end of a location's critical list. MegaMek adds equipment to the
     * first empty slot available in a location. This means that any "holes" (empty slots not at the end of a location),
     * will cause the file crits and MegaMek's crits to become out of sync.
     */
    private void compactCriticalSlots(Mek mek) {
        for (int loc = 0; loc < mek.locations(); loc++) {
            compactCriticalSlots(mek, loc);
        }
    }

    private void compactCriticalSlots(Mek mek, int loc) {
        if (loc == Mek.LOC_HEAD) {
            // This location has an empty slot in between systems crits
            // which will mess up parsing if compacted.
            return;
        }
        int firstEmpty = -1;
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(loc); slot++) {
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
            return Mek.LOC_LEFT_ARM;
        }

        if (location.equalsIgnoreCase("Right Arm:") || location.equalsIgnoreCase("Front Right Leg:")) {
            return Mek.LOC_RIGHT_ARM;
        }

        if (location.equalsIgnoreCase("Left Leg:") || location.equalsIgnoreCase("Rear Left Leg:")) {
            return Mek.LOC_LEFT_LEG;
        }

        if (location.equalsIgnoreCase("Right Leg:") || location.equalsIgnoreCase("Rear Right Leg:")) {
            return Mek.LOC_RIGHT_LEG;
        }

        if (location.equalsIgnoreCase("Center Leg:")) {
            return Mek.LOC_CENTER_LEG;
        }

        if (location.equalsIgnoreCase("Left Torso:")) {
            return Mek.LOC_LEFT_TORSO;
        }

        if (location.equalsIgnoreCase("Right Torso:")) {
            return Mek.LOC_RIGHT_TORSO;
        }

        if (location.equalsIgnoreCase("Center Torso:")) {
            return Mek.LOC_CENTER_TORSO;
        }

        // else
        return Mek.LOC_HEAD;
    }

    private int getArmorLocation(String location) {

        int loc = -1;
        boolean rear = false;
        String locationName = location.toLowerCase();
        if (locationName.startsWith("la armor:") || locationName.startsWith("fll armor:")) {
            loc = Mek.LOC_LEFT_ARM;
        } else if (locationName.startsWith("ra armor:") || locationName.startsWith("frl armor:")) {
            loc = Mek.LOC_RIGHT_ARM;
        } else if (locationName.startsWith("lt armor:")) {
            loc = Mek.LOC_LEFT_TORSO;
        } else if (locationName.startsWith("rt armor:")) {
            loc = Mek.LOC_RIGHT_TORSO;
        } else if (locationName.startsWith("ct armor:")) {
            loc = Mek.LOC_CENTER_TORSO;
        } else if (locationName.startsWith("hd armor:")) {
            loc = Mek.LOC_HEAD;
        } else if (locationName.startsWith("ll armor:") || locationName.startsWith("rll armor:")) {
            loc = Mek.LOC_LEFT_LEG;
        } else if (locationName.startsWith("rl armor:") || locationName.startsWith("rrl armor:")) {
            loc = Mek.LOC_RIGHT_LEG;
        } else if (locationName.startsWith("rtl armor:")) {
            loc = Mek.LOC_LEFT_TORSO;
            rear = true;
        } else if (locationName.startsWith("rtr armor:")) {
            loc = Mek.LOC_RIGHT_TORSO;
            rear = true;
        } else if (locationName.startsWith("rtc armor:")) {
            loc = Mek.LOC_CENTER_TORSO;
            rear = true;
        } else if (locationName.startsWith("cl armor:")) {
            loc = Mek.LOC_CENTER_LEG;
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

        if (lineLower.startsWith(HEAT_SINK_KIT)) {
            heatSinkKit = line;
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
            baseChassisHeatSinks = line;
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

        if (lineLower.startsWith(CLAN_CHASSIS_NAME)) {
            clanChassisName = line.substring(CLAN_CHASSIS_NAME.length());
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
                System system = System.parse(fields[1]);
                if (null != system) {
                    systemManufacturers.put(system, fields[2].trim());
                }
            }
            return true;
        }

        if (lineLower.startsWith(SYSTEM_MODEL)) {
            String[] fields = line.split(":");
            if (fields.length > 2) {
                System system = System.parse(fields[1]);
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

        if (lineLower.startsWith(BV)) {
            bv = Integer.parseInt(line.substring(BV.length()));
            return true;
        }

        if (lineLower.startsWith(MUL_ID)) {
            mulId = Integer.parseInt(line.substring(MUL_ID.length()));
            return true;
        }

        if (lineLower.startsWith(FLUFF_IMAGE)) {
            fluffImageEncoded = line.substring(FLUFF_IMAGE.length());
            return true;
        }

        if (lineLower.startsWith(ICON)) {
            iconEncoded = line.substring(ICON.length());
            return true;
        }

        if (lineLower.startsWith(QUIRK) || lineLower.startsWith(WEAPON_QUIRK)) {
            quirkLines.add(line);
            return true;
        }

        if (lineLower.startsWith(ROLE)) {
            role = line.substring(ROLE.length());
            return true;
        }

        return false;
    }

    private boolean isTitleLine(String line) {
        String lineLower = line.toLowerCase();
        if (lineLower.startsWith(CHASSIS)) {
            chassis = line.substring(CHASSIS.length());
            return true;
        } else if (lineLower.startsWith(MODEL)) {
            model = line.substring(MODEL.length());
            return true;
        } else {
            return false;
        }
    }

    private int weaponsList(String line) {
        if (line.toLowerCase().startsWith(WEAPONS)) {
            return Integer.parseInt(line.substring(WEAPONS.length()));
        }
        return -1;
    }
}
