/*
 * Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.commandLine;

import megamek.common.RangeType;
import megamek.common.TechConstants;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.logging.MMLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods for exporting the equipment database from the command line.
 */
class EquipmentExporter {

    private static final MMLogger LOGGER = MMLogger.create(EquipmentType.class);

    static void writeEquipmentDatabase(File f) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write("MegaMek Equipment Database");
            w.newLine();
            w.write("This file can be regenerated with java -jar MegaMek.jar -eqdb ");
            w.write(f.toString());
            w.newLine();
            w.write("Type,Tech Base,Rules,Name,Aliases");
            w.newLine();
            for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
                EquipmentType type = e.nextElement();
                if (type instanceof AmmoType) {
                    w.write("A,");
                } else if (type instanceof WeaponType) {
                    w.write("W,");
                } else {
                    w.write("M,");
                }
                for (int year : type.getTechLevels().keySet()) {
                    w.write(year + "-" + TechConstants.getTechName(type.getTechLevel(year)));
                }
                w.write(",");
                for (int year : type.getTechLevels().keySet()) {
                    w.write(year + "-" + TechConstants.getLevelName(type.getTechLevel(year)));
                }
                w.write(",");
                for (Enumeration<String> names = type.getNames(); names.hasMoreElements(); ) {
                    String name = names.nextElement();
                    w.write(name + ",");
                }
                w.newLine();
            }
            w.flush();
            w.close();
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    static void writeEquipmentExtendedDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Extended Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqedb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                  "Type,Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,Alias");
            bufferedWriter.newLine();
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
                  equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (equipmentType instanceof AmmoType) {
                    bufferedWriter.write("A");
                } else if (equipmentType instanceof WeaponType) {
                    bufferedWriter.write("W");
                } else {
                    bufferedWriter.write("M");
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(equipmentType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = equipmentType.getTechLevels()
                      .keySet()
                      .stream()
                      .map(equipmentType::getTechLevel)
                      .sorted() // ordered for ease of use
                      .distinct()
                      .toList();

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getTechName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getLevelName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getFullRatingName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getStaticTechLevel().toString());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getIntroductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getPrototypeDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getProductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getCommonDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getExtinctionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getReintroductionDateName());

                bufferedWriter.write("\",");
                if (equipmentType.getBaseTonnage() == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.getBaseTonnage()));
                }

                bufferedWriter.write(",");
                if (equipmentType.getBaseCriticalSlots() == EquipmentType.CRITICAL_SLOTS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString((int) equipmentType.getBaseCriticalSlots()));
                }

                bufferedWriter.write(",");
                if (equipmentType.getBaseCost() == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.getCost(null, false, -1, 1.0)));
                }

                bufferedWriter.write(",");
                if (equipmentType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(equipmentType.getRulesRefs());

                bufferedWriter.write("\",\"");
                for (Enumeration<String> names = equipmentType.getNames(); names.hasMoreElements(); ) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }
                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    static void writeEquipmentWeaponDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Weapon Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqwdb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                  "Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,MinimalRange,ShortRange,MediumRange,LongRange,ExtremeRange,ShortWaterRange,MediumWaterRange,LongWaterRange,ExtremeWaterRange,MinimalDamage,ShortDamage,MediumDamage,LongDamage,ExtremeDamage,Alias");
            bufferedWriter.newLine();

            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
                  equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (!(equipmentType instanceof WeaponType weaponType)) {
                    continue;
                }

                bufferedWriter.write("\"");
                bufferedWriter.write(weaponType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = weaponType.getTechLevels()
                      .keySet()
                      .stream()
                      .map(weaponType::getTechLevel)
                      .sorted() // ordered for ease of use
                      .distinct()
                      .toList();

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getTechName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getLevelName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getFullRatingName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getStaticTechLevel().toString());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getIntroductionDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getPrototypeDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getProductionDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getCommonDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getExtinctionDateName());
                bufferedWriter.write("\",\"");
                bufferedWriter.write(weaponType.getTechAdvancement().getReintroductionDateName());
                bufferedWriter.write("\",");
                if (weaponType.getBaseTonnage() == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(weaponType.getBaseTonnage()));
                }
                bufferedWriter.write(",");
                if (weaponType.getBaseCriticalSlots() == EquipmentType.CRITICAL_SLOTS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString((int) weaponType.getBaseCriticalSlots()));
                }
                bufferedWriter.write(",");
                if (weaponType.getBaseCost() == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(weaponType.getCost(null, false, -1, 1.0)));
                }
                bufferedWriter.write(",");
                if (weaponType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(weaponType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(weaponType.getRulesRefs());

                int minimalRange = weaponType.getMinimumRange();
                minimalRange = (minimalRange < 0) ? -1 : minimalRange;
                bufferedWriter.write("\",");
                bufferedWriter.write(Integer.toString(minimalRange));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getShortRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getMediumRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getLongRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getExtremeRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWShortRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWMediumRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWLongRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getWExtremeRange()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_MINIMUM)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_SHORT)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_MEDIUM)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_LONG)));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(weaponType.getDamage(RangeType.RANGE_EXTREME)));

                bufferedWriter.write(",\"");
                for (Enumeration<String> names = weaponType.getNames(); names.hasMoreElements(); ) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }

                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    static void writeEquipmentAmmoDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Armor Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqadb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                  "Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,CountAsFlak?,MunitionType,DamagePerShot,RackSize,Shots,AmmoRatio,IsCapital,KgPerShot,AeroUse?,Alias");
            bufferedWriter.newLine();
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
                  equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (!(equipmentType instanceof AmmoType ammoType)) {
                    continue;
                }

                bufferedWriter.write("\"");
                bufferedWriter.write(ammoType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = ammoType.getTechLevels()
                      .keySet()
                      .stream()
                      .map(ammoType::getTechLevel)
                      .sorted() // ordered for ease of use
                      .distinct()
                      .toList();

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getTechName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getLevelName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getFullRatingName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getStaticTechLevel().toString());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getIntroductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getPrototypeDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getProductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getCommonDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getExtinctionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(ammoType.getTechAdvancement().getReintroductionDateName());

                bufferedWriter.write("\",");
                if (ammoType.getBaseTonnage() == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(ammoType.getBaseTonnage()));
                }

                bufferedWriter.write(",");
                if (ammoType.getBaseCriticalSlots() == EquipmentType.CRITICAL_SLOTS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString((int) ammoType.getBaseCriticalSlots()));
                }

                bufferedWriter.write(",");
                if (ammoType.getBaseCost() == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(ammoType.getCost(null, false, -1, 1.0)));
                }

                bufferedWriter.write(",");
                if (ammoType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(ammoType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(ammoType.getRulesRefs());

                bufferedWriter.write("\",");
                bufferedWriter.write(Boolean.toString(ammoType.countsAsFlak()));

                bufferedWriter.write(",");
                bufferedWriter.write(ammoType.getMunitionType().toString());

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(ammoType.getDamagePerShot()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(ammoType.getRackSize()));

                bufferedWriter.write(",");
                bufferedWriter.write(Integer.toString(ammoType.getShots()));

                bufferedWriter.write(",");
                bufferedWriter.write(Double.toString(ammoType.getAmmoRatio()));

                bufferedWriter.write(",");
                bufferedWriter.write(Boolean.toString(ammoType.isCapital()));

                bufferedWriter.write(",");
                bufferedWriter.write(Double.toString(ammoType.getKgPerShot()));

                bufferedWriter.write(",");
                bufferedWriter.write(Boolean.toString(ammoType.canAeroUse()));

                bufferedWriter.write(",\"");
                for (Enumeration<String> names = ammoType.getNames(); names.hasMoreElements(); ) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }
                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    static void writeEquipmentMiscDatabase(File f) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f));
            bufferedWriter.write("MegaMek Equipment Extended Database");
            bufferedWriter.newLine();
            bufferedWriter.write("This file can be regenerated with java -jar MegaMek.jar -eqmdb ");
            bufferedWriter.write(f.toString());
            bufferedWriter.newLine();
            bufferedWriter.write(
                  "Name,Tech Base,Rules,Tech Rating,Static Tech Level,Introduction Date,Prototype Date,Production Date,Common Date,Extinction Date,Re-Introduction Date,Tonnage,CriticalSlots,Cost,BV,RulesRef,Alias");
            bufferedWriter.newLine();
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
                  equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if ((equipmentType instanceof AmmoType) || (equipmentType instanceof WeaponType)) {
                    continue;
                }

                bufferedWriter.write("\"");
                bufferedWriter.write(equipmentType.getName());

                // Gather the unique tech levels for this equipment ...
                List<Integer> levels = equipmentType.getTechLevels()
                      .keySet()
                      .stream()
                      .map(equipmentType::getTechLevel)
                      .sorted() // ordered for ease of use
                      .distinct()
                      .toList();

                // ... and use them to output the tech names ...
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getTechName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                // ... and associated rules levels.
                bufferedWriter.write("\",\"");
                bufferedWriter.write(levels.stream()
                      .map(TechConstants::getLevelName)
                      .distinct()
                      .collect(Collectors.joining("/")));

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getFullRatingName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getStaticTechLevel().toString());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getIntroductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getPrototypeDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getProductionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getCommonDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getExtinctionDateName());

                bufferedWriter.write("\",\"");
                bufferedWriter.write(equipmentType.getTechAdvancement().getReintroductionDateName());

                bufferedWriter.write("\",");
                if (equipmentType.getBaseTonnage() == EquipmentType.TONNAGE_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.getBaseTonnage()));
                }

                bufferedWriter.write(",");
                if (equipmentType.getBaseCriticalSlots() == EquipmentType.CRITICAL_SLOTS_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Integer.toString((int) equipmentType.getBaseCriticalSlots()));
                }

                bufferedWriter.write(",");
                if (equipmentType.getBaseCost() == EquipmentType.COST_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.getCost(null, false, -1, 1.0)));
                }

                bufferedWriter.write(",");
                if (equipmentType.bv == EquipmentType.BV_VARIABLE) {
                    bufferedWriter.write("Variable");
                } else {
                    bufferedWriter.write(Double.toString(equipmentType.bv));
                }

                bufferedWriter.write(",\"");
                bufferedWriter.write(equipmentType.getRulesRefs());

                bufferedWriter.write("\",\"");
                for (Enumeration<String> names = equipmentType.getNames(); names.hasMoreElements(); ) {
                    String name = names.nextElement();
                    bufferedWriter.write(name + ",");
                }

                bufferedWriter.write("\"");
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }
}
