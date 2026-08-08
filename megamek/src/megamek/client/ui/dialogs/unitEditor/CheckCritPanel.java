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
package megamek.client.ui.dialogs.unitEditor;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import megamek.codeUtilities.MathUtility;

/**
 * A row of check boxes standing for the critical hits a system or a piece of equipment has taken. Checking one
 * checks every box before it, so the hits always read as a count from the left.
 */
public class CheckCritPanel extends JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 8662728291188274362L;

    private final ArrayList<JCheckBox> checks = new ArrayList<>();

    /** Told whenever the hits change, so the armor diagram can stripe the location. */
    private final List<Runnable> hitsChangedListeners = new ArrayList<>();

    public CheckCritPanel(int crits, int current) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        for (int i = 0; i < crits; i++) {
            JCheckBox check = new JCheckBox("");
            check.setActionCommand(Integer.toString(i));
            check.addActionListener(this::checkBoxes);
            checks.add(check);
            add(check);
        }

        if (current > 0) {
            for (int i = 0; i < current && i < checks.size(); i++) {
                checks.get(i).setSelected(true);
            }
        }
    }

    public int getHits() {
        int hits = 0;
        for (JCheckBox check : checks) {
            if (check.isSelected()) {
                hits++;
            }
        }
        return hits;
    }

    public void setHits(int hits) {
        for (int i = 0; i < checks.size(); i++) {
            checks.get(i).setSelected(i < hits);
        }
        hitsChanged();
    }

    /**
     * Adds an action to run whenever the hits change, however they were changed. More than one thing follows a
     * crit: the armor diagram stripes the location, and an ammo bin's shots follow its crit.
     */
    public void addHitsChangedListener(Runnable listener) {
        hitsChangedListeners.add(listener);
    }

    private void hitsChanged() {
        hitsChangedListeners.forEach(Runnable::run);
    }

    private void checkBoxes(ActionEvent evt) {
        int hits = MathUtility.parseInt(evt.getActionCommand());
        boolean selected = checks.get(hits).isSelected();
        if (selected) {
            // check all those up to this one
            for (int i = 0; i < hits; i++) {
                checks.get(i).setSelected(true);
            }
        } else if (hits < checks.size()) {
            // deselect any above this one
            for (int i = hits; i < checks.size(); i++) {
                checks.get(i).setSelected(false);
            }
        }
        hitsChanged();

    }
}
