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

package megamek.common.ui;

import com.formdev.flatlaf.extras.components.FlatLabel;

import javax.swing.border.EmptyBorder;

/**
 * This JLabel is a specialized small-text label for use as a help/info text to be shown directly in a GUI, e.g. under
 * an input field. It is useful only for short texts but much easier to notice than a tooltip.
 */
@SuppressWarnings("unused") // utility class
public class SmallFontHelpTextLabel extends FlatLabel {

    /**
     * Constructs a help text label with the given text. The label uses a small font and is disabled, meaning its text
     * is greyed out. By default, the label uses an EmptyBorder with 4 thickness on the bottom and 0 elsewhere (the
     * border can be replaced).
     *
     * @param text The text to show
     */
    public SmallFontHelpTextLabel(String text) {
        setText(text);
        initialize();
    }

    /**
     * Constructs an empty help text label. The label uses a small font and is disabled, meaning its text is greyed out.
     * By default, the label uses an EmptyBorder with 4 thickness on the bottom and 0 elsewhere (the border can be
     * replaced).
     */
    public SmallFontHelpTextLabel() {
        initialize();
    }

    /**
     * Constructs an empty help text label with the given alignment, e.g. SwingConstants.CENTER. The label uses a small
     * font and is disabled, meaning its text is greyed out. By default, the label uses an EmptyBorder with 4 thickness
     * on the bottom and 0 elsewhere (the border can be replaced). The default * horizontal alignment is (as with
     * JLabel) SwingConstants.LEADING.
     */
    public SmallFontHelpTextLabel(int horizontalAlignment) {
        initialize();
        setHorizontalAlignment(horizontalAlignment);
    }

    /**
     * Constructs an empty help text label with the given text and the given alignment, e.g. SwingConstants.CENTER. The
     * label uses a small font and is disabled, meaning its text is greyed out. By default, the label uses an
     * EmptyBorder with 4 thickness on the bottom and 0 elsewhere (the border can be replaced). The default horizontal
     * alignment is (as with JLabel) SwingConstants.LEADING.
     */
    public SmallFontHelpTextLabel(String text, int horizontalAlignment) {
        this(text);
        setHorizontalAlignment(horizontalAlignment);
    }

    private void initialize() {
        setLabelType(LabelType.mini);
        setEnabled(false);
        setBorder(new EmptyBorder(0, 0, 4, 0));
    }
}
