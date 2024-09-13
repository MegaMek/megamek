/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import java.io.File;

import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestBattleArmor;
import megamek.logging.MMLogger;

/**
 * This util will go through all available units and filter them according to
 * the filter() method and print out any units that match the filter. Edit
 * {@link Entity} and {@link MekSummary} to apply the desired filter.
 */
public final class FilteredUnitListTool {
    private static final MMLogger logger = MMLogger.create(FilteredUnitListTool.class);
    private static final String UNIT_VERIFIER_OPTIONS = "data/mekfiles/UnitVerifierOptions.xml";

    /**
     * Edit the return value filter to any sort of check on entity or summary that,
     * when true, should make the unit be listed. E.g. when looking for all SV with
     * a fusion engine, use:
     *
     * <code>
     * passesFilter = entity.isSupportVehicle() && entity.getEngine().isFusion();
     *</code>
     *
     * All Primitive Meks with a standard gyro:
     *
     * <code>
     * passesFilter = entity instanceof Mek && entity.isPrimitive() &&
     * entity.getGyroType() == Mek.GYRO_STANDARD;
     * </code>
     *
     * SV with amphibious chassis:
     *
     * <code>
     * passesFilter = entity.isSupportVehicle() &&
     * entity.hasWorkingMisc(MiscType.F_AMPHIBIOUS);
     * </code>
     *
     * Note that the MekSummary contains Alpha Strike values and can be filtered
     * using those.
     *
     * @param entity The full Entity of the current unit to be tested
     * @return True if this unit is to be listed
     */
    private static boolean filter(final Entity entity) {
        if (!entity.isBattleArmor()) {
            return false;
        }

        EntityVerifier entityVerifier = EntityVerifier.getInstance(new File(UNIT_VERIFIER_OPTIONS));
        TestBattleArmor testBattleArmor = new TestBattleArmor((BattleArmor) entity, entityVerifier.baOption, null);
        return !testBattleArmor.correctEntity(new StringBuffer(), entity.getTechLevel());
    }

    // No changes necessary after here:
    public static void main(String[] args) {
        MekSummaryCache cache = MekSummaryCache.getInstance(true);
        logger.info("Filtering...");

        int countFound = 0;

        for (MekSummary unitSummary : cache.getAllMeks()) {
            Entity entity = loadEntity(unitSummary.getSourceFile(), unitSummary.getEntryName());
            if ((entity != null) && filter(entity)) {
                logger.info(entity.getShortName());
                countFound++;
            }
        }

        logger.info("-------------------------");
        String message = String.format("Found %s units.", countFound);
        logger.info(message);
    }

    public static @Nullable Entity loadEntity(File f, String entityName) {
        try {
            return new MekFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            return null;
        }
    }

    private FilteredUnitListTool() {
    }
}
