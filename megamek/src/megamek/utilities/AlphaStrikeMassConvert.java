/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;

/**
 * This utility converts all units that can be converted and for which filter() returns true to AlphaStrike elements and
 * outputs the stats to the clipboard. This can be pasted to Excel (best use the Text Import Wizard to set the column
 * spacer to TAB and set the format of the Damage column to "Text").
 */
public class AlphaStrikeMassConvert {

    private static final String COLUMN_SEPARATOR = "\t";
    private static final String INTERNAL_DELIMITER = ",";

    private static final MMLogger logger = MMLogger.create(AlphaStrikeMassConvert.class);

    /**
     * Main entry point for unit -> AS conversion.
     *
     * @param args
     *
     * @throws EntityLoadingException
     */
    public static void main(String[] args) throws EntityLoadingException {
        logger.debug("Starting AlphaStrike conversion.");
        StringBuilder table = new StringBuilder(clipboardHeaderString());
        MekSummary[] units = MekSummaryCache.getInstance().getAllMeks();

        for (MekSummary unit : units) {
            Entity entity = new MekFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();

            if (!ASConverter.canConvert(entity) || !entity.hasMulId()) {
                continue;
            }

            String message = String.format("Processing %s...", entity.getShortName());
            logger.debug(message);
            AlphaStrikeElement ase = ASConverter.convert(entity);
            table.append(clipboardElementString(ase));
        }

        StringSelection stringSelection = new StringSelection(table.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        System.exit(0);
    }

    /**
     * Creates a tab separated header for the file.
     *
     * @return
     */
    private static String clipboardHeaderString() {
        List<String> headers = new ArrayList<>();
        headers.add("Chassis");
        headers.add("Model");
        headers.add("MUL ID");
        headers.add("Role");
        headers.add("Type");
        headers.add("SZ");
        headers.add("MV");
        headers.add("Arm");
        headers.add("Str");
        headers.add("Thr");
        headers.add("Dmg S/M/L");
        headers.add("OV");
        headers.add("PV");
        headers.add("Specials");
        headers.add("\n");
        return String.join("\t", headers);
    }

    /**
     * Returns a String representing the entities to export to the clipboard.
     *
     * @param element
     *
     * @return
     */
    private static StringBuilder clipboardElementString(AlphaStrikeElement element) {
        List<String> stats = new ArrayList<>();
        stats.add(element.getChassis());
        stats.add(element.getModel());
        stats.add(element.getMulId() + "");
        stats.add(element.getRole().toString());
        stats.add(element.getASUnitType().toString());
        stats.add(element.getSize() + "");
        stats.add(element.getMovementAsString());
        stats.add(element.getFullArmor() + "");
        stats.add(element.getFullStructure() + "");
        stats.add(element.usesThreshold() ? element.getThreshold() + "" : " ");
        stats.add(element.getStandardDamage() + "");
        stats.add(element.getOV() + "");
        stats.add(element.getPointValue() + "");
        stats.add(AlphaStrikeHelper.getSpecialsExportString(INTERNAL_DELIMITER, element));
        stats.add("\n");
        return new StringBuilder(String.join(COLUMN_SEPARATOR, stats));
    }

    /**
     * Private Constructor
     */
    private AlphaStrikeMassConvert() {
    }
}
