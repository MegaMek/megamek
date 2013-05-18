/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.util;

import megamek.common.Entity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Deric Page dericpage@users.sourceforge.net
 * @since: 8/3/12 3:22 PM
 * @version: %I% %G%
 */
public class NonCombatUnitList {

    private static boolean initialized = false;
    private static List<String> unitList;

    private static void initUnitList() {
        if (initialized) {
            return;
        }
        unitList = new ArrayList<String>();

        String filePath = System.getProperty("user.dir");
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator;
        }
        filePath += "mmconf" + File.separator + "non_combat_units_list.conf";
        File file = new File(filePath);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            System.out.println(filePath + " could not be read!");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line.trim().equals("")) || line.startsWith(";")) {
                    continue;
                }
                int commentStart = line.indexOf(";");
                if (commentStart > -1) {
                    line = line.substring(0, commentStart);
                }
                String[] lineParts = line.split(",");
                String unitName = lineParts[0];
                if ((lineParts.length > 1) && !StringUtil.isNullOrEmpty(lineParts[1])) {
                    unitName += " " + lineParts[1].trim();
                }
                unitList.add(unitName);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        initialized = true;
    }

    public static void expire() {
        initialized = false;
    }

    public static boolean isNonCombatUnit(String chassis, String model) {
        initUnitList();
        String unitId = chassis;
        if (!StringUtil.isNullOrEmpty(model)) {
            unitId += " " + model;
        }
        return unitList.contains(unitId);
    }

    public static boolean isNonCombatUnit(Entity unit) {
        return isNonCombatUnit(unit.getChassis(), unit.getModel());
    }
}
