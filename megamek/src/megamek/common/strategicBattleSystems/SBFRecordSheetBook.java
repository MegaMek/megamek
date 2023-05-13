/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.strategicBattleSystems;

import megamek.common.annotations.Nullable;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents a collection of Strategic BattleForce sheets. It implements {@link Printable}
 * and can be printed using Java2D printing, see {@link java.awt.print.PrinterJob}.
 */
public class SBFRecordSheetBook implements Printable {

    private final List<SBFRecordSheet> recordSheets;
    private final Font headerFont;
    private final Font valueFont;

    /**
     * Constructs a new collection of Strategic BattleForce sheets for the given formations. Formations
     * must not be null. If formations is empty, a single empty record sheet (usable for manual fill-in
     * is printed. The given fonts are used for the sheets' texts.
     *
     * @param formations The formations to print sheets for.
     * @param headerFont The font to use for headers and fixed texts. The size of the font doesn't matter
     * @param valueFont The font to use for formation values. The size of the font doesn't matter
     */
    public SBFRecordSheetBook(Collection<SBFFormation> formations, @Nullable Font headerFont, @Nullable Font valueFont) {
        recordSheets = formations.stream().map(SBFRecordSheet::new).collect(Collectors.toList());
        if (recordSheets.isEmpty()) {
            // When no formations are given, add one empty sheet for manual fill-in
            recordSheets.add(new SBFRecordSheet(null));
        }
        this.headerFont = headerFont;
        this.valueFont = valueFont;
    }

    /**
     * Constructs a new collection of Strategic BattleForce sheets for the given formations. Formations
     * must not be null. If formations is empty, a single empty record sheet (usable for manual fill-in
     * is printed.
     *
     * @param formations The formations to print sheets for.
     */
    public SBFRecordSheetBook(Collection<SBFFormation> formations) {
        this(formations, null, null);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex < recordSheets.size()) {
            SBFRecordSheet recordSheet = recordSheets.get(pageIndex);
            if (headerFont != null) {
                recordSheet.setFont(headerFont);
            }
            if (valueFont != null) {
                recordSheet.setValueFont(valueFont);
            }
            recordSheet.print(graphics, pageFormat, 0);
            return Printable.PAGE_EXISTS;
        } else {
            return Printable.NO_SUCH_PAGE;
        }
    }
}