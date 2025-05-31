/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.ai.dataset.UnitEnrichment;
import megamek.ai.dataset.UnitEnrichmentSerializer;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.templates.TROView;
import megamek.logging.MMLogger;

/**
 * This class provides a utility to read in all the /data/mekfiles and print that data out into a TSV format, but
 * only with the information that CASPAR training needs to enrich its current training data.
 *
 * @author Luana Coppio
 */
public final class MekDataForCasparTrainingTool {
    private static final MMLogger LOGGER = MMLogger.create(MekDataForCasparTrainingTool.class);

    // Excel import works better with the .txt extension instead of .csv
    private static final String FILE_NAME = "meks.tsv";
    public static final String LINE_BREAK = "\n";

    public static void main(String... args) {
        UnitEnrichmentSerializer serializer = new UnitEnrichmentSerializer();
        Game game = new Game();
        try (PrintWriter pw = new PrintWriter(FILE_NAME); BufferedWriter bw = new BufferedWriter(pw)) {
            MekSummaryCache cache = MekSummaryCache.getInstance(true);
            MekSummary[] units = cache.getAllMeks();
            bw.write(serializer.getHeaderLine() + LINE_BREAK);
            for (MekSummary unit : units) {
                var entity = unit.loadEntity();
                entity.setGame(game);
                bw.write(serializer.serialize(UnitEnrichment.fromEntity(entity)) + LINE_BREAK);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e, "Could not open file for output!");
        } catch (IOException e) {
            LOGGER.error(e, "IO Exception");
        }
    }

    private MekDataForCasparTrainingTool() {
    }
}
