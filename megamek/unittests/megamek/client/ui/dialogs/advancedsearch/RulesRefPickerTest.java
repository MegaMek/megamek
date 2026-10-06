/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 */
package megamek.client.ui.dialogs.advancedsearch;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import megamek.common.SourceBookCode;
import org.junit.jupiter.api.Test;

class RulesRefPickerTest {

    @Test
    void selectedBooksWrapInsideTheBorderedField() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RulesRefPicker picker = new RulesRefPicker();
            List<SourceBookCode> books = List.of(SourceBookCode.values());
            picker.setChoices(books);
            picker.setSelectedAbbreviations(Arrays.stream(SourceBookCode.values())
                  .map(SourceBookCode::getAbbrev)
                  .toList());

            picker.setSize(320, 1);
            Dimension narrowSize = picker.getPreferredSize();
            picker.setSize(1_200, 1);
            Dimension wideSize = picker.getPreferredSize();

            assertNotNull(picker.getBorder());
            assertTrue(narrowSize.width <= 320);
            assertTrue(narrowSize.height > wideSize.height);
        });
    }
}
