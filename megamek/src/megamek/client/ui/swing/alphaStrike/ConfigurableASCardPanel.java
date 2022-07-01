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

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfigurableASCardPanel extends JPanel {

    private final JComboBox<String> fontChooser = new JComboBox<>();
    private final JComboBox<String> sizeChooser = new JComboBox<>();
    ASCardPanel cardPanel = new ASCardPanel();

    public ConfigurableASCardPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));


        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            fontChooser.addItem(family);
        }
        fontChooser.addActionListener(ev -> updateFont());
        sizeChooser.addItem("2");
        sizeChooser.addItem("1.5");
        sizeChooser.addItem("1");
        sizeChooser.addItem("0.75");
        sizeChooser.addItem("0.5");
        sizeChooser.addItem("0.33");
        sizeChooser.setSelectedItem("1");
        sizeChooser.addActionListener(ev -> updateSize());

        var chooserLine = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        chooserLine.add(new JLabel("Font: "));
        chooserLine.add(fontChooser);
        chooserLine.add(new JLabel("      Card Size: "));
        chooserLine.add(sizeChooser);

        var cardLine = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        cardLine.add(cardPanel);

        add(chooserLine);
        add(cardLine);
    }

    public void setASElement(@Nullable AlphaStrikeElement element) {
        cardPanel.setASElement(element);
    }

    private void updateFont() {
        Font font = Font.decode((String) fontChooser.getSelectedItem());
        cardPanel.setCardFont(font);
    }

    private void updateSize() {
        cardPanel.setScale(Float.parseFloat((String)sizeChooser.getSelectedItem()));
    }

}