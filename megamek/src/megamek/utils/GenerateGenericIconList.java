/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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
package megamek.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.client.ui.swing.tileset.MechTileset.MechEntry;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SpaceStation;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.Warship;
import org.apache.logging.log4j.LogManager;

/**
 * This program will generate a list of all of the units that use the default
 * (generic) icons.
 * 
 * @author arlith
 * @since January 2016
 */
public class GenerateGenericIconList implements MechSummaryCache.Listener {

    private static MechSummaryCache mechSummaryCache = null;
    
    public static void main(String[] args) {
        boolean ignoreUnofficial = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-unofficial")) {
                ignoreUnofficial = false;
            } else {
                LogManager.getLogger().error("Invalid argument of " + args[i]);
                return;
            }
        }
        
        GenerateGenericIconList gen = new GenerateGenericIconList();
        mechSummaryCache = MechSummaryCache.getInstance(ignoreUnofficial);
        mechSummaryCache.addListener(gen);
    }

    @Override
    public void doneLoading() {
        MechSummary[] ms = mechSummaryCache.getAllMechs();
        int genericCount = 0;
        Map<String, List<String>> typeNameMap = new HashMap<>();
        
        System.out.println("\n");
        System.out.println("Units using Generic Icons:");
        
        for (int i = 0; i < ms.length; i++) {
            Entity entity = loadEntity(ms[i].getSourceFile(), ms[i].getEntryName());
            MechEntry entry = MMStaticDirectoryManager.getMechTileset().entryFor(entity, -1);
            MechEntry defaultEntry = MMStaticDirectoryManager.getMechTileset().genericFor(entity, -1);
            if (entry.equals(defaultEntry)) {
                String name = entity.getChassis() + " " + entity.getModel();
                String type = "Unknown:";
                if (entity instanceof Mech) {
                    type = "Mechs:";
                } else if (entity instanceof VTOL) {
                    type = "VTOLs:";
                } else if (entity instanceof Tank) {
                    type = "Tanks:";
                } else if (entity instanceof BattleArmor) {
                    type = "BattleArmor:";
                } else if (entity instanceof Infantry) {
                    type = "Infantry:";
                } else if (entity instanceof SpaceStation) {
                    type = "SpaceStations:";
                } else if (entity instanceof Warship) {
                    type = "Warships:";
                } else if (entity instanceof Jumpship) {
                    type = "Jumpships:";
                } else if (entity instanceof SmallCraft) {
                    type = "Dropships:";
                } else if (entity instanceof Aero) {
                    type = "Aeros:";
                } else if (entity instanceof Protomech) {
                    type = "Protomechs:";
                }
                List<String> names = typeNameMap.computeIfAbsent(type, k -> new ArrayList<>());
                names.add(name);
                genericCount++;        
            }
        }
        
        for (String type : typeNameMap.keySet()) {
            System.out.println(type);
            List<String> names = typeNameMap.get(type);
            for (String name : names) {
                System.out.println("\t" + name);
            }
            System.out.println("\n");
        }
        
        System.out.println("\n");
        System.out.println("Total units with generic icons: " + genericCount);
        for (String type : typeNameMap.keySet()) {
            System.out.println("\t" + type + " "
                    + typeNameMap.get(type).size());
        }
    }
    
    public Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            LogManager.getLogger().error("", e);
        }
        return entity;
    }
    
}
