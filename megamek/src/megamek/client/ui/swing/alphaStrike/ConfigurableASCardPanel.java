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
package megamek.client.ui.swing.alphaStrike;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfigurableASCardPanel extends JPanel {

    private final JComboBox<String> cbFont = new JComboBox<>();
    ASCardPanel cardPanel = new ASCardPanel();

    public ConfigurableASCardPanel() {
        setLayout(new FlowLayout());
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            cbFont.addItem(family);
        }
        cbFont.addActionListener(ev -> updateFont());
        add(cbFont);
        add(cardPanel);
    }

    public void setASElement(@Nullable AlphaStrikeElement element) {
        cardPanel.setASElement(element);
    }

    private void updateFont() {
        Font font = Font.decode((String) cbFont.getSelectedItem());
        cardPanel.setCardFont(font);
    }
}