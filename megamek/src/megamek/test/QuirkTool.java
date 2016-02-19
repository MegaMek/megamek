/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
 *  Copyright © 2016 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Protomech;
import megamek.common.QuirksHandler;
import megamek.common.Tank;
import megamek.common.VTOL;

/**
 * This program is a tool to help gather information about quirks.  It goes
 * through each canon quirk entry and will print out 1) if it belongs to 
 * multiple types of units (which is an error), 2) if it belongs to no unit 
 * (which likely means there's an error).  Also, since some errors don't appear
 * until a unit is loaded, it will force any problematic quirks to throw up
 * warnings.
 * 
 * @author arlith
 * @date January 2016
 */
public class QuirkTool implements MechSummaryCache.Listener {

    private static MechSummaryCache mechSummaryCache = null;

    public static void main(String[] args) {     
        EquipmentType.initializeTypes();
        
        QuirkTool qc = new QuirkTool();
        mechSummaryCache = MechSummaryCache.getInstance(true);
        mechSummaryCache.addListener(qc);
    }

    @Override
    public void doneLoading() {
        MechSummary[] ms = mechSummaryCache.getAllMechs();

        try {
            QuirksHandler.initQuirksList();
        } catch (IOException e) {
            System.err.println("Error initializing quirks!");
            e.printStackTrace();
            return;
        }
        
        Set<String> quirkIds = QuirksHandler.getCanonQuirkIds();
        Map<String, List<Entity>> idEntitiesMap = new HashMap<>();
        
        for (String quirkId : quirkIds) {
            for (MechSummary summary : ms) {              
                String allId = QuirksHandler.getUnitId(summary.getChassis(),
                        "all");
                String specificId = QuirksHandler.getUnitId(
                        summary.getChassis(), summary.getModel());
                List<Entity> entities;
                if (quirkId.equals(allId) || quirkId.equals(specificId)) {
                    entities = idEntitiesMap.get(quirkId);
                    if (entities == null) {
                        entities = new ArrayList<>();
                        idEntitiesMap.put(quirkId, entities);
                    }
                    entities.add(loadEntity(summary.getSourceFile(),
                            summary.getEntryName()));
                }
            }
        }
        
        for (String quirkId : idEntitiesMap.keySet()) {
            List<Entity> entities = idEntitiesMap.get(quirkId);
            Set<Integer> types = new HashSet<>();
            boolean containsNonMech = false;
            for (Entity ent : entities) {
                if (!(ent instanceof Mech)) {
                    System.out.println("Entry: " + quirkId
                            + " contains non 'mech!");
                    containsNonMech = true;
                }
                if (ent instanceof Mech) {
                    types.add(1);
                } else if (ent instanceof Tank) {
                    types.add(2);
                } else if (ent instanceof Aero) {
                    types.add(3);
                } else if (ent instanceof VTOL) {
                    types.add(4);
                } else if (ent instanceof Infantry) {
                    types.add(5);
                } else if (ent instanceof Protomech) {
                    types.add(6);
                } else {
                    types.add(7);
                }
            }
            if (containsNonMech) {
                System.out.println("non-mech entry: " + quirkId);
            }
            if (types.size() > 1) {
                System.out.println("Entry: " + quirkId
                        + " contains mixed types!");
            }
            if (types.size() == 0) {
                System.out.println("Entry: " + quirkId
                        + " doesn't have any matches!");
            }
        }
    }
    
    public Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            System.out.println("Exception: " + e.toString()); //$NON-NLS-1$
        }
        return entity;
    }

    
}
