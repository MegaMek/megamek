/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2016 Nicholas Walczak (walczak@cs.umn.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.test;

import megamek.common.*;
import megamek.common.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * This program is a tool to help gather information about quirks. It goes through each canon quirk
 * entry and will print out:
 * 1) if it belongs to multiple types of units (which is an error)
 * 2) if it belongs to no unit (which likely means there's an error)
 *
 * Also, since some errors don't appear until a unit is loaded, it will force any problematic quirks
 * to throw up warnings.
 * 
 * @author arlith
 * @since January 2016
 */
public class QuirkTool implements MechSummaryCache.Listener {
    private static MechSummaryCache mechSummaryCache = null;

    public static void main(String... args) {
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
        } catch (Exception e) {
            System.out.println("Error initializing quirks");
            return;
        }
        
        Set<String> quirkIds = QuirksHandler.getCanonQuirkIds();
        Map<String, List<Entity>> idEntitiesMap = new HashMap<>();
        
        for (String quirkId : quirkIds) {
            for (MechSummary summary : ms) {              
                String allId = QuirksHandler.getUnitId(summary.getChassis(), "all",
                        MechSummary.determineETypeName(summary));
                String specificId = QuirksHandler.getUnitId(summary.getChassis(),
                        summary.getModel(), MechSummary.determineETypeName(summary));
                List<Entity> entities;
                if (quirkId.equals(allId) || quirkId.equals(specificId)) {
                    entities = idEntitiesMap.computeIfAbsent(quirkId, k -> new ArrayList<>());
                    entities.add(loadEntity(summary.getSourceFile(), summary.getEntryName()));
                }
            }

            if (idEntitiesMap.get(quirkId) == null) {
                System.out.println("Entry: " + quirkId + "doesn't have any matches!");
            }
        }
        
        for (final String quirkId : idEntitiesMap.keySet()) {
            List<Entity> entities = idEntitiesMap.get(quirkId);
            Set<Integer> types = new HashSet<>();
            boolean containsNonMech = false;
            for (final Entity ent : entities) {
                if (!(ent instanceof Mech)) {
                    System.out.println("Entry " + quirkId + " contains non 'Mek");
                    containsNonMech = true;
                }

                if (ent instanceof Mech) {
                    types.add(1);
                } else if (ent instanceof VTOL) {
                    types.add(4);
                } else if (ent instanceof Tank) {
                    types.add(2);
                } else if (ent instanceof Aero) {
                    types.add(3);
                } else if (ent instanceof Infantry) {
                    types.add(5);
                } else if (ent instanceof Protomech) {
                    types.add(6);
                } else {
                    types.add(7);
                }
            }

            if (containsNonMech) {
                System.out.println("Non-Mek Entry " + quirkId);
            }

            if (types.size() > 1) {
                System.out.println("Entry " + quirkId + " contains mixed types");
            }

            if (types.isEmpty()) {
                System.out.println("Entry " + quirkId + " doesn't have any matches");
            }
        }
    }

    public @Nullable Entity loadEntity(final File file, final String entityName) {
        try {
            return new MechFileParser(file, entityName).getEntity();
        } catch (Exception e) {
            System.out.println("couldn't load entity");
            return null;
        }
    }
}
