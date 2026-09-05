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

package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.Container;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.panels.VictoryOptionLayout;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.options.BasicOption;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;

/**
 * The lobby victory conditions dialog. It holds the victory game options that formerly lived on the Victory
 * Conditions tab of the game options dialog (turn limit, BV thresholds, kill count etc.); the tab is removed and
 * this dialog, opened by the lobby's Victory Conditions button in the Player Setup panel, is the only place to edit
 * them. On OK, the caller sends the changed options (see
 * {@link megamek.client.Client#sendGameOptions(String, Vector)}).
 */
public class VictoryConditionsDialog extends AbstractButtonDialog implements DialogOptionListener {

    /**
     * The name of the victory game options group (see {@link megamek.common.options.GameOptions}). Note that option
     * groups added with the single-argument {@code addGroup} have this as their name; their key is empty.
     */
    public static final String VICTORY_OPTIONS_GROUP_NAME = "victory";

    /**
     * The objective options, which get a tab of their own. Everything else in the victory group is the
     * long-standing set and stays on the first tab.
     */
    private static final Set<String> OBJECTIVE_OPTIONS = Set.of(
          OptionsConstants.VICTORY_USE_OBJECTIVES,
          OptionsConstants.VICTORY_VP_WIN_THRESHOLD,
          OptionsConstants.VICTORY_VP_SUDDEN_DEATH);

    /** Unscaled width the objectives note wraps at, a little under the tab's preferred width. */
    private static final int NOTE_WIDTH = 500;

    private final ClientGUI clientGui;
    private final JPanel victoryOptionsPanel = new JPanel();
    private final List<DialogOptionComponentYPanel> victoryOptionComps = new ArrayList<>();
    private final JTextField fieldPassword = new JTextField(15);
    private final JPanel objectiveOptionsPanel = new JPanel();

    public VictoryConditionsDialog(ClientGUI clientGui) {
        super(clientGui.getFrame(), "VictoryConditionsDialog", "VictoryConditionsDialog.title");
        this.clientGui = clientGui;
        refreshLobbyState();
        initialize();
    }

    /** Refreshes the victory option values from the game options; call before showing the dialog. */
    public void refreshLobbyState() {
        victoryOptionsPanel.removeAll();
        objectiveOptionsPanel.removeAll();
        victoryOptionComps.clear();
        for (Enumeration<IOptionGroup> groups = clientGui.getClient().getGame().getOptions().getGroups();
              groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            if (!VICTORY_OPTIONS_GROUP_NAME.equals(group.getName())) {
                continue;
            }
            for (Enumeration<IOption> optionsEnumeration = group.getOptions();
                  optionsEnumeration.hasMoreElements(); ) {
                IOption option = optionsEnumeration.nextElement();
                DialogOptionComponentYPanel optionComponent = new DialogOptionComponentYPanel(this, option, true);
                victoryOptionComps.add(optionComponent);
                if (OBJECTIVE_OPTIONS.contains(option.getName())) {
                    objectiveOptionsPanel.add(optionComponent);
                } else {
                    victoryOptionsPanel.add(optionComponent);
                }
            }
        }
        // bounded number fields, and greying out for anything that needs a master switch - the same rules
        // the game options pane applies, so the two places agree about what a legal setting is
        VictoryOptionLayout.apply(victoryOptionComps);
        // the same row shape as the option rows above it, or the box layout lines the bare label's left edge
        // up with the rows' centre line and the note lands on the right-hand side
        JPanel notePanel = new FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        // a label only wraps HTML that is given a width; this one is the tab's width less its margins
        String wrappedNote = "<html><div width=" + UIUtil.scaleForGUI(NOTE_WIDTH) + ">"
              + Messages.getString("VictoryConditionsDialog.objectivesNote") + "</div></html>";
        notePanel.add(new JLabel(wrappedNote));
        objectiveOptionsPanel.add(Box.createVerticalStrut(UIUtil.scaleForGUI(8)));
        objectiveOptionsPanel.add(notePanel);
        victoryOptionsPanel.revalidate();
        objectiveOptionsPanel.revalidate();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        victoryOptionsPanel.setLayout(new BoxLayout(victoryOptionsPanel, BoxLayout.PAGE_AXIS));
        objectiveOptionsPanel.setLayout(new BoxLayout(objectiveOptionsPanel, BoxLayout.PAGE_AXIS));

        // two tabs: the long-standing victory conditions, and the objectives that used to be buried at the
        // bottom of the same flat list with nothing to mark them out as a different kind of decision
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(Messages.getString("VictoryConditionsDialog.tab.conditions"),
              new JScrollPane(victoryOptionsPanel));
        tabs.addTab(Messages.getString("VictoryConditionsDialog.tab.objectives"),
              new JScrollPane(objectiveOptionsPanel));
        // without a preferred size, pack() squeezes the option rows into a double-scrollbar box; the user
        // can still resize the dialog, and the size is remembered while MegaMek runs (saved on normal exit)
        tabs.setPreferredSize(UIUtil.scaleForGUI(560, 420));

        JPanel passwordPanel = new FixedYPanel();
        passwordPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.password")));
        passwordPanel.add(fieldPassword);

        result.add(tabs);
        result.add(Box.createVerticalStrut(5));
        result.add(passwordPanel);
        return result;
    }

    /** @return The text entered in the server password field; the server checks it only when it has a password */
    public String getPassword() {
        return fieldPassword.getText();
    }

    /**
     * @return The victory game options the user changed, for
     *       {@link megamek.client.Client#sendGameOptions(String, Vector)}; empty when nothing changed
     */
    public Vector<IBasicOption> getChangedVictoryOptions() {
        Vector<IBasicOption> changedOptions = new Vector<>();
        for (DialogOptionComponentYPanel optionComponent : victoryOptionComps) {
            if (optionComponent.hasChanged()) {
                changedOptions.addElement(optionComponent.changedOption());
                optionComponent.setOptionChanged(false);
            }
        }
        return changedOptions;
    }

    /**
     * Persists all game options - including the victory options as edited in this dialog - to the default game options
     * file, so that the victory conditions survive between games. This mirrors the save that the former Victory
     * Conditions tab performed as part of the game options dialog's OK handling; without it, a newly hosted game
     * reloads the options file and reverts every victory option to its default (the options sent to a running server
     * are not written to disk).
     *
     * <p>The full option set is written because {@link GameOptions#saveOptions(Vector)} overwrites the entire
     * file; saving only the victory options would drop every other game option. The victory options use the values
     * currently shown in this dialog (the sent changes are not yet reflected in the local game options, which are only
     * updated once the server echoes them back), while all other options keep their current game values.</p>
     */
    public void saveVictoryOptions() {
        Vector<IBasicOption> optionsToSave = new Vector<>();
        for (Enumeration<IOptionGroup> groups = clientGui.getClient().getGame().getOptions().getGroups();
              groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> optionsEnumeration = group.getOptions();
                  optionsEnumeration.hasMoreElements(); ) {
                IOption option = optionsEnumeration.nextElement();
                optionsToSave.addElement(optionForSave(option));
            }
        }
        GameOptions.saveOptions(optionsToSave);
    }

    /**
     * @param option a game option being written to the options file
     *
     * @return the edited value from this dialog's component when it holds one for the given option, otherwise the
     *       option's current value unchanged
     */
    private IBasicOption optionForSave(IOption option) {
        for (DialogOptionComponentYPanel optionComponent : victoryOptionComps) {
            if (optionComponent.getOption().getName().equals(option.getName())) {
                return optionComponent.changedOption();
            }
        }
        return new BasicOption(option.getName(), option.getValue());
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel optionComponent, IOption option, boolean state) {
        // no dependent-option handling needed for the victory options
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel optionComponent, IOption option, int position) {
        // no dependent-option handling needed for the victory options
    }
}
