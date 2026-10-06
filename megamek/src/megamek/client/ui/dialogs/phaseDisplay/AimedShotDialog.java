/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.Serial;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.IndexedRadioButton;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

public class AimedShotDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AimedShotDialog.class);

    private static final String DIALOG_NAME = "AimedShotDialog";

    @Serial
    private static final long serialVersionUID = 6527374019085650613L;

    /**
     * The checkboxes for available choices.
     */
    private final IndexedRadioButton[] checkboxes;
    private final boolean[] boxEnabled;

    public AimedShotDialog(JFrame parent, String title, String message,
          String[] choices, boolean[] enabled, int selectedIndex,
          ClientGUI clientGUI, Targetable target,
          ItemListener il, ActionListener al) {
        super(parent, title, false);
        super.setResizable(false);

        boxEnabled = enabled;

        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);

        GridBagConstraints c = new GridBagConstraints();

        JLabel labMessage = new JLabel(message, SwingConstants.LEFT);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridBagLayout.setConstraints(labMessage, c);
        getContentPane().add(labMessage);

        String div = "<DIV WIDTH=" + UIUtil.scaleForGUI(500) + ">" + UnitToolTip.getTargetTipDetail(target,
              clientGUI.getClient()) + "</DIV>";
        JLabel labTarget = new JLabel(UnitToolTip.wrapWithHTML(div), SwingConstants.LEFT);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout.setConstraints(labTarget, c);
        getContentPane().add(labTarget);

        ButtonGroup radioGroup = new ButtonGroup();
        checkboxes = new IndexedRadioButton[choices.length];

        for (int i = 0; i < choices.length; i++) {
            boolean even = (i & 1) == 0;
            checkboxes[i] = new IndexedRadioButton(choices[i], i == selectedIndex,
                  radioGroup, i);
            checkboxes[i].addItemListener(il);
            checkboxes[i].setEnabled(enabled[i]);
            c.gridwidth = even ? 1 : GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridBagLayout.setConstraints(checkboxes[i], c);
            add(checkboxes[i]);
        }

        JButton butNoAim = new JButton(Messages.getString("AimedShotDialog.dontAim"));
        butNoAim.addActionListener(al);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        c.anchor = GridBagConstraints.CENTER;
        gridBagLayout.setConstraints(butNoAim, c);
        add(butNoAim);

        butNoAim.requestFocus();

        pack();
        // First-time default: center on the parent frame. If the player has moved the dialog
        // before, setPreferences() below restores that remembered position over this centering.
        setLocation((parent.getLocation().x + (parent.getSize().width / 2))
              - (getSize().width / 2), (parent.getLocation().y
              + (parent.getSize().height / 2)) - (getSize().height / 2));

        setPreferences();

        // The dialog is rebuilt for every aimed shot and its height depends on how many locations
        // the current target has. Re-pack after restoring preferences so the size always fits this
        // target, while keeping the remembered location (pack() resizes but never moves the window).
        pack();

        // Growing the dialog at a remembered edge location (or a resolution change between sessions)
        // could leave it partially off-screen. Clamp its bounds so it always stays fully visible.
        UIUtil.updateWindowBounds(this);
    }

    /**
     * Restores the position the dialog was last left at, and keeps it up to date as the player moves the dialog. A
     * remembered position wins over the first-time centering in the constructor, so the dialog no longer snaps back
     * over the map on every aimed shot.
     */
    private void setPreferences() {
        try {
            setName(DIALOG_NAME);
            PreferencesNode preferences = MegaMek.getMMPreferences().forClass(AimedShotDialog.class);
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            // a dialog that cannot remember where it was is still perfectly usable
            LOGGER.error(exception, "Could not set the preferences of the aimed shot dialog");
        }
    }

    public void setEnableAll(boolean enableAll) {
        for (int i = 0; i < checkboxes.length; i++) {
            if (enableAll) {
                checkboxes[i].setEnabled(boxEnabled[i]);
            } else {
                checkboxes[i].setEnabled(false);
            }
        }
    }

}
