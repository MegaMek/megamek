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
package megamek.client.ui.buttons;

import javax.swing.JToggleButton;

import megamek.MMConstants;
import megamek.client.ui.util.FlatLafStyleBuilder;

/**
 * A JToggleButton that shows a check mark and cross mark to make its selection status clearer.
 *
 * @author Simon (Juliez)
 */
public class MMToggleButton extends JToggleButton {

    private static final String CHECK = "#90FF90>\u2713 ";
    private static final String CROSS = "#FF9090>\u2717 ";
    private static final String INTRO = "<HTML><NOBR><FONT COLOR=";
    private static final String CLOSE = "</FONT>";
    private static final int MARK_LENGTH = CHECK.length() + INTRO.length() + CLOSE.length();

    public MMToggleButton(String text) {
        super();
        setText(text);
        // The standard UI font doesn't show unicode characters (on Win10)
        new FlatLafStyleBuilder().font(MMConstants.FONT_DIALOG).apply(this);
        addActionListener(event -> setText(getText()));
    }

    public MMToggleButton(String text, boolean selection) {
        super();
        setText(text);
        setSelected(selection);
        // The standard UI font doesn't show unicode characters (on Win10)
        new FlatLafStyleBuilder().font(MMConstants.FONT_DIALOG).apply(this);
        addActionListener(event -> setText(getText()));
    }

    @Override
    public void setText(String text) {
        if (text.length() > MARK_LENGTH && text.startsWith(INTRO)) {
            text = text.substring(MARK_LENGTH);
        }
        if (isSelected()) {
            text = INTRO + CHECK + CLOSE + text;
        } else {
            text = INTRO + CROSS + CLOSE + text;
        }
        super.setText(text);
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        setText(getText());
    }

}
