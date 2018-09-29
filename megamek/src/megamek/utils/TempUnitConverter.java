/*
 * Copyright (C) 2018 - The MegaMek Team
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
package megamek.utils;

import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitType;
import megamek.common.loaders.BLKFile;

public class TempUnitConverter {
    
    static void processUnit(MechSummary ms) {
        try {
            Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            System.out.println("Processing " + entity.getShortNameRaw());
            BLKFile.encode(ms.getSourceFile().getPath(), entity);
        } catch (Exception ex) {
            System.err.println("Error parsing " + ms.getName());
        }
    }
    
    public static void main(String[] args) {
        MechSummaryCache msc = MechSummaryCache.getInstance(true);
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
        
        for (MechSummary ms : msc.getAllMechs()) {
            int unitType = UnitType.determineUnitTypeCode(ms.getUnitType());
            if ((unitType == UnitType.PROTOMEK)
                    || (unitType == UnitType.AERO)
                    || (unitType == UnitType.CONV_FIGHTER)
                    || (unitType == UnitType.SMALL_CRAFT)
                    || (unitType == UnitType.DROPSHIP)
                    || (unitType == UnitType.JUMPSHIP)
                    || (unitType == UnitType.WARSHIP)
                    || (unitType == UnitType.SPACE_STATION)) {
                processUnit(ms);
            }
        }
    }

}
