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

import static megamek.client.ui.Messages.getString;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import megamek.Version;
import megamek.client.ui.dialogs.releaseNotes.ReleaseNotes;
import megamek.client.ui.dialogs.releaseNotes.ReleaseNotesDialog;

/**
 * This Action opens the release notes of the running suite version in a dialog.
 */
public class ReleaseNotesAction extends AbstractAction {

    private final String repository;

    /**
     * Creates this action for the given GitHub repository of the MegaMek organization, e.g. "megameklab" in the MML
     * menu bar.
     *
     * @param repository The repository name, without the organization
     */
    public ReleaseNotesAction(String repository) {
        super(getString("CommonMenuBar.helpReleaseNotes"));
        this.repository = repository;
        putValue(AbstractAction.SHORT_DESCRIPTION, getString("CommonMenuBar.helpReleaseNotes.tip"));
    }

    /** Creates this action for MegaMek. */
    public ReleaseNotesAction() {
        this("megamek");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ReleaseNotesDialog.showReleaseNotes(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
              repository);
    }

    /**
     * Returns the address of the release notes for the given version. Each suite release is tagged
     * v&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;.&lt;revision&gt;, so a version with a revision has its own release page.
     * A version without one (a development build of a line that has not been released yet) has no release, so it
     * gets the list of all releases instead.
     *
     * @param repository The repository name, without the organization
     * @param version    The version to show the notes for
     *
     * @return The release page address
     */
    public static String releaseNotesUrl(String repository, Version version) {
        return ReleaseNotes.page(repository, version).toString();
    }
}
