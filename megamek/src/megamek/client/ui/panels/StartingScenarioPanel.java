/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.panels;

import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;

public class StartingScenarioPanel extends JPanel {

    private static final String text = "<HTML><FONT FACE=Anta SIZE=+3>Starting Scenario";
    private static final String sign = "<HTML><FONT SIZE=+5>\uf50e";

    /**
     * Returns a panel that shows the centered notice saying "Under Construction" with a warning sign above.
     */
    public StartingScenarioPanel() {
        JPanel textPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.CENTER));
        textPanel.add(new JLabel(text));

        JPanel symbolPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel symbolLabel = new JLabel(sign);
        symbolLabel.setFont(FontHandler.symbolFont());
        symbolPanel.add(symbolLabel);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(Box.createVerticalGlue());
        add(symbolPanel);
        add(textPanel);
        add(Box.createVerticalGlue());
    }
}
