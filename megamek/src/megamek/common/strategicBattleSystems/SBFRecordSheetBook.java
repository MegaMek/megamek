/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.strategicBattleSystems;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;

/**
 * This class represents a collection of Strategic BattleForce sheets. It implements {@link Printable} and can be
 * printed using Java2D printing, see {@link java.awt.print.PrinterJob}.
 */
public class SBFRecordSheetBook implements Printable {

    private final List<SBFRecordSheet> recordSheets;
    private final Font headerFont;
    private final Font valueFont;

    /**
     * Constructs a new collection of Strategic BattleForce sheets for the given formations. Formations must not be
     * null. If formations is empty, a single empty record sheet (usable for manual fill-in is printed. The given fonts
     * are used for the sheets' texts.
     *
     * @param formations The formations to print sheets for.
     * @param headerFont The font to use for headers and fixed texts. The size of the font doesn't matter
     * @param valueFont  The font to use for formation values. The size of the font doesn't matter
     */
    public SBFRecordSheetBook(Collection<SBFFormation> formations, @Nullable Font headerFont,
          @Nullable Font valueFont) {
        recordSheets = formations.stream().map(SBFRecordSheet::new).collect(Collectors.toList());
        if (recordSheets.isEmpty()) {
            // When no formations are given, add one empty sheet for manual fill-in
            recordSheets.add(new SBFRecordSheet(null));
        }
        this.headerFont = headerFont;
        this.valueFont = valueFont;
    }

    /**
     * Constructs a new collection of Strategic BattleForce sheets for the given formations. Formations must not be
     * null. If formations is empty, a single empty record sheet (usable for manual fill-in is printed.
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
