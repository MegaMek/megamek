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
package megamek.utilities;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import java.io.File;

/**
 * This util will go through all available units and filter them according to the filter() method and
 * print out any units that match the filter.
 */
public class FilteredUnitListTool {

    private static boolean filter(final Entity entity, final MechSummary summary) {
        boolean passesFilter;
        // Between the lines, set passesFilter to any sort of check on entity or MechSummary that, when true,
        // should make the unit be listed.
        // E.g. when looking for all SV with a fusion engine, use:
        // passesFilter = entity.isSupportVehicle() && entity.getEngine().isFusion();
        // All Primitive Meks with a standard gyro:
        // passesFilter = entity instanceof Mech && entity.isPrimitive() && entity.getGyroType() == Mech.GYRO_STANDARD;
        // SV with amphibious chassis:
        // passesFilter = entity.isSupportVehicle() && entity.hasWorkingMisc(MiscType.F_AMPHIBIOUS);
        // Note that the MechSummary contains Alpha Strike values and can be filtered using those.
        // --------------------
        passesFilter = entity instanceof BattleArmor && entity.getMovementMode().isJumpInfantry() && entity.getJumpMP() == 0;

        // --------------------
        return passesFilter;
    }

    // No changes necessary after here:
    public static void main(String[] args) {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);
        System.out.println("Filtering...");
        int countFound = 0;
        for (MechSummary unitSummary : cache.getAllMechs()) {
            Entity entity = loadEntity(unitSummary.getSourceFile(), unitSummary.getEntryName());
            if ((entity != null) && filter(entity, unitSummary)) {
                System.out.println(entity.getShortName());
                countFound++;
            }
        }
        System.out.println("-------------------------");
        System.out.println("Found " + countFound + " units.");
    }

    public static @Nullable Entity loadEntity(File f, String entityName) {
        try {
            return new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            return null;
        }
    }

    private FilteredUnitListTool() { }
}
