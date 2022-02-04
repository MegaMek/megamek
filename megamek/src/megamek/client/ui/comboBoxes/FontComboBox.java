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
package megamek.client.ui.comboBoxes;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.displayWrappers.FontDisplay;

import javax.swing.*;
import java.awt.*;

/**
 * This is a custom ComboBox that allows you to select a font from your available fonts, each
 * displayed in that font.
 */
public class FontComboBox extends MMComboBox<FontDisplay> {
    //region Constructors
    public FontComboBox(final String name) {
        super(name, FontDisplay.getSortedFontDisplays());
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FontDisplay) {
                    setFont(((FontDisplay) value).getFont());
                    list.setToolTipText(value.toString());
                }
                return this;
            }
        });
        addActionListener(evt -> {
            final FontDisplay fontDisplay = getSelectedItem();
            if (fontDisplay != null) {
                setFont(fontDisplay.getFont());
            }
        });
    }
    //endregion Constructors
}
