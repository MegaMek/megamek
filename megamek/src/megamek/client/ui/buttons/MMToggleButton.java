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

import java.awt.Color;
import javax.swing.JToggleButton;

import megamek.MMConstants;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.UIUtil;

/**
 * A JToggleButton that shows a check mark and cross mark to make its selection status clearer. The marks take
 * their colors from {@link UIUtil}, so they stay readable in both UI themes - a deep green on the light theme
 * instead of a pale one that washes out on a light background.
 *
 * @author Simon (Juliez)
 */
public class MMToggleButton extends JToggleButton {

    private static final String CHECK_MARK = ">\u2713 "; // Checkmark
    private static final String CROSS_MARK = ">\u2717 "; // X
    private static final String INTRO = "<HTML><NOBR><FONT COLOR=";
    private static final String CLOSE = "</FONT>";
    /** The color part is a fixed-length #RRGGBB value, so the marked prefix always has the same length. */
    private static final int MARK_LENGTH = "#RRGGBB".length() + CHECK_MARK.length()
          + INTRO.length() + CLOSE.length();

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
            text = INTRO + colorHex(UIUtil.uiGreen()) + CHECK_MARK + CLOSE + text;
        } else {
            text = INTRO + colorHex(UIUtil.uiLightRed()) + CROSS_MARK + CLOSE + text;
        }
        super.setText(text);
    }

    /** The color as the fixed-length {@code #RRGGBB} form the marked prefix is built and stripped with. */
    private static String colorHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        setText(getText());
    }

}
