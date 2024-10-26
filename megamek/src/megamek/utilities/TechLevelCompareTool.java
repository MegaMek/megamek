/*
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.utilities;

import java.util.Set;
import java.util.TreeSet;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;

/**
 * Compares computed static tech level to what is in the unit file and reports
 * all units that have equipment that exceeds the declared tech level, followed
 * by a list of all the equipment that caused failures.
 *
 * Note that some failures may be due to system or construction options rather
 * than EquipmentType.
 *
 * @author Neoancient
 *
 */

public class TechLevelCompareTool {
    private static final MMLogger logger = MMLogger.create(TechLevelCompareTool.class);

    static Set<EquipmentType> weaponSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    static Set<EquipmentType> ammoSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    static Set<EquipmentType> miscSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));

    private static final String EQUIPMENT_TYPE_FORMATTED_STRING = "\t%s (%s)";
    private static int badMeks = 0;

    public static void main(String[] args) {
        MekSummaryCache msc = MekSummaryCache.getInstance();

        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                logger.error(ignored, "Ignored Exception");
            }
        }

        logger.info("Any output you see from here are errors with the units.");

        for (MekSummary ms : msc.getAllMeks()) {
            Entity en = null;

            try {
                en = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (EntityLoadingException ignored) {
                String message = String.format("Could not load entity %s", ms.getName());
                logger.error(message);
                continue;
            }

            handleBadEntity(en);

        }

        printDetails();
    }

    private static void handleBadEntity(Entity entity) {

        SimpleTechLevel fixed = SimpleTechLevel.convertCompoundToSimple(entity.getTechLevel());
        SimpleTechLevel calc = entity.getStaticTechLevel();

        if (fixed.compareTo(calc) < 0) {
            String message = String.format("%s: %s/%s", entity.getShortName(), fixed, calc);
            logger.info(message);

            for (Mounted<?> m : entity.getEquipment()) {
                EquipmentType mountedEquipmentType = m.getType();

                if (fixed.compareTo(mountedEquipmentType.getStaticTechLevel()) < 0) {
                    if (mountedEquipmentType instanceof WeaponType weaponType) {
                        weaponSet.add(weaponType);
                    } else if (mountedEquipmentType instanceof AmmoType ammoType) {
                        ammoSet.add(ammoType);
                    } else {
                        miscSet.add(mountedEquipmentType);
                    }
                }
            }

            badMeks++;
        }
    }

    private static void printDetails() {
        String message = "";

        logger.info("Weapons:");
        for (EquipmentType et : weaponSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("Ammo:");
        for (EquipmentType et : ammoSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("MiscType:");
        for (EquipmentType et : miscSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        message = String.format("Failed: %d/%d", badMeks, MekSummaryCache.getInstance().getAllMeks().length);
        logger.info(message);

    }
}
