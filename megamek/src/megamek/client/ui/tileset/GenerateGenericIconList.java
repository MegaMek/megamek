/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.tileset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;

/**
 * This program will generate a list of all the units that use the default (generic) icons. It ignores non-canon units.
 *
 * @author arlith
 */
public class GenerateGenericIconList implements MekSummaryCache.Listener {

    private static MekSummaryCache mekSummaryCache = null;

    public static void main(String[] args) {
        GenerateGenericIconList gen = new GenerateGenericIconList();
        System.out.println("Loading Cache...");
        mekSummaryCache = MekSummaryCache.getInstance(true);
        mekSummaryCache.addListener(gen);
    }

    @Override
    public void doneLoading() {
        Map<String, List<String>> chassisUsing = new HashMap<>();
        Map<String, List<String>> genericsUsing = new HashMap<>();
        for (MekSummary mekSummary : mekSummaryCache.getAllMeks()) {
            if (!mekSummary.isCanon()) {
                continue;
            }
            Entity entity = mekSummary.loadEntity();
            if (entity == null) {
                System.out.println("Couldn't load entity for: " + mekSummary);
                continue;
            }
            if (MMStaticDirectoryManager.getMekTileset().hasOnlyChassisMatch(entity) && !entity.getModel().isBlank()) {
                String name = entity.getChassis() + " " + entity.getModel();
                String type = getTypeName(entity);
                List<String> names = chassisUsing.computeIfAbsent(type, k -> new ArrayList<>());
                names.add(name);
                continue;
            }
            MekTileset.MekEntry entry = MMStaticDirectoryManager.getMekTileset().entryFor(entity, -1);
            MekTileset.MekEntry defaultEntry = MMStaticDirectoryManager.getMekTileset().genericFor(entity, -1);
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
        outputChassisNames(chassisUsing);
        
        System.out.println("Units using Generic Icons:");
        outputChassisNames(genericsUsing);
        int genericCount = genericsUsing.values().stream().mapToInt(List::size).sum();
        System.out.println("Total units with generic icons: " + genericCount);
        for (String type : genericsUsing.keySet()) {
            System.out.println("    " + type + genericsUsing.get(type).size());
        }
    }

    private void outputChassisNames(Map<String, List<String>> chassisUsing) {
        for (String type : chassisUsing.keySet()) {
            System.out.println();
            System.out.println(type);
            List<String> names = chassisUsing.get(type);
            for (String name : names) {
                System.out.println("    " + name);
            }
        }

        System.out.println();
    }

    private static String getTypeName(Entity entity) {
        return entity.getClass().getSimpleName() + ": ";
    }
}
