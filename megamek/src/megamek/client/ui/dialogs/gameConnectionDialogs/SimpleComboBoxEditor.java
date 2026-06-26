/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.gameConnectionDialogs;

import java.awt.Component;
import java.io.Serial;
import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;

/**
 * We need a way to access the action map for a JComboBox editor, so that we can have it fire an action when enter is
 * pressed. This simple class allows this.
 */
public class SimpleComboBoxEditor extends JTextField implements ComboBoxEditor {

    @Serial
    private static final long serialVersionUID = 4496820410417436582L;

    @Override
    public Component getEditorComponent() {
        return this;
    }

    @Override
    public void setItem(Object anObject) {
        if (anObject != null) {
            setText(anObject.toString());
        } else {
            setText(null);
        }
    }

    @Override
    public Object getItem() {
        return getText();
    }

}
