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
package megamek.client.ui.swing;

import megamek.client.ui.swing.util.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * This panel shows the centered notice saying "Under Construction" with a warning sign above.
 */
public class UnderConstructionPanel extends JPanel {

    private static final String text = "<HTML><FONT FACE=Anta SIZE=+3>Under Construction";
    private static final String sign = "<HTML><FONT SIZE=+5>" + UIUtil.WARNING_SIGN;

    /**
     * Returns a panel that shows the centered notice saying "Under Construction" with a warning sign above.
     */
    public UnderConstructionPanel() {
        JPanel textPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.CENTER));
        textPanel.add(new JLabel(text));

        JPanel warningSignPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel warningSignLabel = new JLabel(sign);
        warningSignLabel.setForeground(GUIPreferences.getInstance().getCautionColor());
        warningSignPanel.add(warningSignLabel);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(Box.createVerticalGlue());
        add(warningSignPanel);
        add(textPanel);
        add(Box.createVerticalGlue());
    }
}