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

package megamek.client.ui;

import megamek.MMConstants;
import megamek.MegaMek;

import javax.swing.AbstractAction;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import static megamek.client.ui.Messages.getString;

/**
 * This Action copies OS, Java and project data to the clipboard.
 */
public class CopySystemDataAction extends AbstractAction {

    private final String currentProject;

    /**
     * Creates this action with the given current project name (the current project, when e.g. clicking the menu item in
     * the MML menu bar, should be MMLConstants.PROJECT_NAME). The origin project is determined automatically.
     *
     * @param currentProject The current project name (need not be any particular string)
     *
     * @see MegaMek#getOriginProject()
     */
    public CopySystemDataAction(String currentProject) {
        super(getString("CommonMenuBar.helpCopySystemData"));
        this.currentProject = currentProject;
        putValue(AbstractAction.SHORT_DESCRIPTION, getString("CommonMenuBar.helpCopySystemData.tip"));
    }

    /**
     * Creates this action, assuming that the current project is MM. The origin project is determined automatically.
     *
     * @see MegaMek#getOriginProject()
     */
    public CopySystemDataAction() {
        this(MMConstants.PROJECT_NAME);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(
              MegaMek.getUnderlyingInformation(MegaMek.getOriginProject(), currentProject)), null);
    }
}
