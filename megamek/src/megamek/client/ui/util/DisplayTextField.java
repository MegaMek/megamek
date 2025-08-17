/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.util;

import javax.swing.JTextField;
import javax.swing.text.Document;

/**
 * This is a specialized JTextField that is meant to be used as a pure display, i.e. to show a text value that comes
 * from some other interaction, e.g. the weapons selected for Conventional Infantry in MML, while at the same time
 * looking like an input text field. It does not allow editing and cannot get focus. If the text overflows the length of
 * the field, the start of the text is shown (the end is cut off).
 */
public class DisplayTextField extends JTextField {

    public DisplayTextField() {
        initialize();
    }

    public DisplayTextField(String text) {
        super(text);
        initialize();
    }

    public DisplayTextField(int columns) {
        super(columns);
        initialize();
    }

    public DisplayTextField(String text, int columns) {
        super(text, columns);
        initialize();
    }

    public DisplayTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
        initialize();
    }

    private void initialize() {
        setEditable(false);
        setFocusable(false);
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        setCaretPosition(0);
    }
}
