/*
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
package megamek.client.ui.swing.tileset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ui.swing.tileset.MechTileset.MechEntry;
import megamek.common.Entity;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

/**
 * This program will generate a list of all the units that use the default
 * (generic) icons. It ignores non-canon units.
 * 
 * @author arlith
 */
public class GenerateGenericIconList implements MechSummaryCache.Listener {

    private static MechSummaryCache mechSummaryCache = null;
    
    public static void main(String[] args) {
        GenerateGenericIconList gen = new GenerateGenericIconList();
        System.out.println("Loading Cache...");
        mechSummaryCache = MechSummaryCache.getInstance(true);
        mechSummaryCache.addListener(gen);
    }

    @Override
    public void doneLoading() {
        Map<String, List<String>> chassisUsing = new HashMap<>();
        Map<String, List<String>> genericsUsing = new HashMap<>();
        for (MechSummary mechSummary : mechSummaryCache.getAllMechs()) {
            if (!mechSummary.isCanon()) {
                continue;
            }
            Entity entity = mechSummary.loadEntity();
            if (entity == null) {
                System.out.println("Couldn't load entity for: " + mechSummary);
                continue;
            }
            if (MMStaticDirectoryManager.getMechTileset().hasOnlyChassisMatch(entity) && !entity.getModel().isBlank()) {
                String name = entity.getChassis() + " " + entity.getModel();
                String type = getTypeName(entity);
                List<String> names = chassisUsing.computeIfAbsent(type, k -> new ArrayList<>());
                names.add(name);
                continue;
            }
            MechEntry entry = MMStaticDirectoryManager.getMechTileset().entryFor(entity, -1);
            MechEntry defaultEntry = MMStaticDirectoryManager.getMechTileset().genericFor(entity, -1);
            if (entry == null) {
                System.out.println("Found no entry for: " + entity);
            } else if (defaultEntry == null) {
                System.out.println("Found no default entry for: " + entity);
            } else if (entry.equals(defaultEntry)) {
                String name = entity.getChassis() + " " + entity.getModel();
                String type = getTypeName(entity);
                List<String> names = genericsUsing.computeIfAbsent(type, k -> new ArrayList<>());
                names.add(name);
            }
        }

        System.out.println();
        System.out.println("Units using Chassis Icons (not including units that have no model!):");
        for (String type : chassisUsing.keySet()) {
            System.out.println();
            System.out.println(type);
            List<String> names = chassisUsing.get(type);
            for (String name : names) {
                System.out.println("    " + name);
            }
        }

        System.out.println();
        System.out.println("Units using Generic Icons:");
        for (String type : genericsUsing.keySet()) {
            System.out.println();
            System.out.println(type);
            List<String> names = genericsUsing.get(type);
            for (String name : names) {
                System.out.println("    " + name);
            }
        }

        System.out.println();
        int genericCount = genericsUsing.values().stream().mapToInt(List::size).sum();
        System.out.println("Total units with generic icons: " + genericCount);
        for (String type : genericsUsing.keySet()) {
            System.out.println("    " + type + genericsUsing.get(type).size());
        }
    }

    private static String getTypeName(Entity entity) {
        return entity.getClass().getSimpleName() + ": ";
    }
}
