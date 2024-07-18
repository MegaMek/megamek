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
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;

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

    static Set<EquipmentType> weaponSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    static Set<EquipmentType> ammoSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    static Set<EquipmentType> miscSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));

    public static void main(String[] args) {
        int bad = 0;
        MechSummaryCache msc = MechSummaryCache.getInstance();
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (MechSummary ms : msc.getAllMechs()) {
            Entity en = null;
            try {
                en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (EntityLoadingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (null != en) {
                SimpleTechLevel fixed = SimpleTechLevel.convertCompoundToSimple(en.getTechLevel());
                SimpleTechLevel calc = en.getStaticTechLevel();
                if (fixed.compareTo(calc) < 0) {
                    System.out.println(en.getShortName() + ": " + fixed + "/" + calc);
                    for (Mounted m : en.getEquipment()) {
                        if (fixed.compareTo(m.getType().getStaticTechLevel()) < 0) {
                            if (m.getType() instanceof WeaponType) {
                                weaponSet.add(m.getType());
                            } else if (m.getType() instanceof AmmoType) {
                                ammoSet.add(m.getType());
                            } else {
                                miscSet.add(m.getType());
                            }
                        }
                    }
                    bad++;
                }
            } else {
                System.err.println("Could not load entity " + ms.getName());
            }
        }
        System.out.println("Weapons:");
        for (EquipmentType et : weaponSet) {
            System.out.println("\t" + et.getName() + " (" + et.getStaticTechLevel().toString() + ")");
        }
        System.out.println("Ammo:");
        for (EquipmentType et : ammoSet) {
            System.out.println("\t" + et.getName() + " (" + et.getStaticTechLevel().toString() + ")");
        }
        System.out.println("MiscType:");
        for (EquipmentType et : miscSet) {
            System.out.println("\t" + et.getName() + " (" + et.getStaticTechLevel().toString() + ")");
        }
        System.out.println("Failed: " + bad + "/" + msc.getAllMechs().length);
    }

}
