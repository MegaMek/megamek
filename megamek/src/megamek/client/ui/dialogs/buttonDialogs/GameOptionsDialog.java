/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.buttonDialogs;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.util.UIUtil.FixedYPanel;
import static megamek.client.ui.util.UIUtil.WrappingButtonPanel;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.dialogs.MMDialogs.MMConfirmDialog;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.common.TechConstants;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.weapons.bayWeapons.capital.CapitalMissileBayWeapon;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Responsible for displaying the current game options and allowing the user to change them. */
public class GameOptionsDialog extends AbstractButtonDialog implements ActionListener, DialogOptionListener {

    private ClientGUI clientGui;
    private JFrame frame;
    private GameOptions options;
    private boolean editable = true;

    /**
     * A map that maps an option to a collection of DialogOptionComponents that can effect the value of this option.
     */
    private Map<String, List<DialogOptionComponentYPanel>> optionComps = new HashMap<>();

    /**
     * Keeps track of the DialogOptionComponents that have been added to the search panel. This is used to remove those
     * components from optionComps when they get removed.
     */
    private final ArrayList<DialogOptionComponentYPanel> searchComps = new ArrayList<>();

    private int maxOptionWidth;

    private final JTabbedPane panOptions = new JTabbedPane();

    /** Panel that holds all the options found via search */
    private final JPanel panSearchOptions = new JPanel();

    /** Text field that contains text to search on */
    private final JTextField txtSearch = new JTextField("");

    private final WrappingButtonPanel panPassword = new WrappingButtonPanel();
    private final JLabel labPass = new JLabel(Messages.getString("GameOptionsDialog.Password"));
    private final JTextField texPass = new JTextField(15);
    private final WrappingButtonPanel panButtons = new WrappingButtonPanel();
    private final JButton butSave = new JButton(Messages.getString("GameOptionsDialog.Save"));
    private final JButton butLoad = new JButton(Messages.getString("GameOptionsDialog.Load"));
    private final JButton butDefaults = new JButton(Messages.getString("GameOptionsDialog.Defaults"));
    private final JButton butOkay = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));
    private final MMToggleButton butUnofficial = new MMToggleButton("Unofficial Opts");

    /**
     * When the OK button is pressed, the options can be saved to a file; this behavior happens by default but there are
     * some situations where the options should not be saved, such as when loading a scenario.
     */
    private boolean performSave = true;

    private final static String UNOFFICIAL = "Unofficial";

    /**
     * Creates a new GameOptionsDialog with the given ClientGUI as parent. The ClientGUI supplies the game options. Used
     * in the lobby and game.
     */
    public GameOptionsDialog(ClientGUI cg) {
        super(cg.getFrame(), "GameOptionsDialog", "GameOptionsDialog.title");
        clientGui = cg;
        init(cg.getFrame(), cg.getClient().getGame().getOptions());
    }

    /**
     * Creates a new GameOptionsDialog with the given JFrame as parent. Uses the given game options. Used when starting
     * a scenario.
     */
    public GameOptionsDialog(JFrame frame, GameOptions options, boolean shouldSave) {
        super(frame, "GameOptionsDialog", "GameOptionsDialog.title");
        performSave = shouldSave;
        init(frame, options);
    }

    /** Initial dialog setup for both constructors. */
    private void init(JFrame frame, GameOptions options) {
        this.options = options;
        this.frame = frame;
        labPass.setLabelFor(texPass);
        panPassword.add(labPass);
        panPassword.add(texPass);
        refreshOptions();
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        var mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(panOptions);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(panPassword);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(panButtons);
        return mainPanel;
    }

    @Override
    protected JPanel createButtonPanel() {
        butOkay.addActionListener(this::okButtonActionPerformed);
        butCancel.addActionListener(this::cancelActionPerformed);
        butDefaults.addActionListener(this::resetToDefaults);
        butSave.addActionListener(this);
        butLoad.addActionListener(this);
        butUnofficial.addActionListener(this);

        panButtons.add(butUnofficial);
        panButtons.add(Box.createHorizontalStrut(30));
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butDefaults);
        panButtons.add(butSave);
        panButtons.add(butLoad);

        return panButtons;
    }

    /** Updates the dialog ui with the given options. */
    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }

    private void send() {
        Vector<IBasicOption> changed = new Vector<>();

        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking the first is fine
            if (!comps.isEmpty()) {
                DialogOptionComponentYPanel comp = comps.get(0);
                if (comp.hasChanged()) {
                    changed.addElement(comp.changedOption());
                    comp.setOptionChanged(false);
                }
            }
        }

        if ((clientGui != null) && !changed.isEmpty()) {
            clientGui.getClient().sendGameOptions(texPass.getText(), changed);
        }
    }

    private void doSave() {
        GameOptions.saveOptions(getOptions());
    }

    public Vector<IBasicOption> getOptions() {
        Vector<IBasicOption> output = new Vector<>();

        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking
            // the first is fine
            if (!comps.isEmpty()) {
                IBasicOption option = comps.get(0).changedOption();
                output.addElement(option);
            }
        }
        return output;
    }

    private void resetToDefaults(final ActionEvent ev) {
        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            for (DialogOptionComponentYPanel comp : comps) {
                if (!comp.isDefaultValue()) {
                    comp.resetToDefault();
                }
            }
        }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new HashMap<>();

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            JPanel groupPanel = addGroup(group);
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                addOption(groupPanel, option);
            }
        }
        butUnofficial.setSelected(!(Boolean) options.getOption(OptionsConstants.BASE_HIDE_UNOFFICIAL).getValue());
        toggleOptions();
        addSearchPanel();
        validate();
    }

    /**
     * When show is true, options that contain the given String str are shown. When show is false, these options are
     * hidden and deselected. Used to show/hide unofficial options.
     */
    private void toggleOptions() {
        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking the first is fine
            if (!comps.isEmpty()) {
                DialogOptionComponentYPanel comp = comps.get(0);
                if (isUnofficialOption(comp)) {
                    comp.setVisible(shouldShow(comp));
                    if (!shouldShow(comp)) {
                        // Disable hidden unofficial options
                        if (comp.getOption().getType() == IOption.BOOLEAN) {
                            comp.setSelected(false);
                        }
                    }
                }
                if (isHiddenOption(comp)) {
                    comp.setVisible(false);
                }

            }
        }
    }

    /** Returns true when the given Option should never show in the dialog. */
    private boolean isHiddenOption(DialogOptionComponentYPanel comp) {
        return comp.getOption().getName().equals(OptionsConstants.BASE_HIDE_UNOFFICIAL);
    }

    private boolean isUnofficialOption(DialogOptionComponentYPanel comp) {
        return comp.getOption().getDisplayableName().contains(UNOFFICIAL);
    }

    /** Returns true when the given Option should be visible in the dialog. */
    private boolean shouldShow(DialogOptionComponentYPanel comp) {
        boolean isHiddenUnofficial = !butUnofficial.isSelected() && isUnofficialOption(comp);
        return !(isHiddenUnofficial || isHiddenOption(comp));
    }

    private void refreshSearchPanel() {
        panSearchOptions.removeAll();
        searchComps.clear();

        // Add new DialogOptionComponents for all matching Options
        final String searchText = txtSearch.getText().toLowerCase();
        if (!searchText.isBlank()) {
            ArrayList<DialogOptionComponentYPanel> allNewComps = new ArrayList<>();
            for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
                for (DialogOptionComponentYPanel comp : comps) {
                    String optName = comp.getOption().getDisplayableName().toLowerCase();
                    String optDesc = comp.getOption().getDescription().toLowerCase();
                    if ((optName.contains(searchText) || optDesc.contains(searchText)) && shouldShow(comp)) {
                        allNewComps.add(comp);
                    }
                }
            }
            Collections.sort(allNewComps);
            for (DialogOptionComponentYPanel comp : allNewComps) {
                searchComps.add(comp);
                panSearchOptions.add(comp);
            }
        }
        panSearchOptions.revalidate();
        panOptions.repaint();
    }

    private JPanel addGroup(IOptionGroup group) {
        JPanel groupPanel = new JPanel();
        JScrollPane scrOptions = new JScrollPane(groupPanel);
        scrOptions.getVerticalScrollBar().setUnitIncrement(16);
        groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
        scrOptions.setAutoscrolls(true);
        scrOptions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrOptions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panOptions.addTab(group.getDisplayableName(), scrOptions);
        return groupPanel;
    }

    private void addSearchPanel() {
        JPanel panSearch = new JPanel();
        JScrollPane scrOptions = new JScrollPane(panSearch);
        scrOptions.getVerticalScrollBar().setUnitIncrement(16);
        panSearch.setLayout(new BoxLayout(panSearch, BoxLayout.PAGE_AXIS));
        scrOptions.setAutoscrolls(true);
        scrOptions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrOptions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Panel for holding the label and text field for searching
        var panSearchBar = new FixedYPanel();
        JLabel lblSearch = new JLabel(Messages.getString("GameOptionsDialog.Search") + ":");
        lblSearch.setLabelFor(txtSearch);
        lblSearch.setToolTipText(Messages.getString("GameOptionsDialog.SearchToolTip"));
        txtSearch.setToolTipText(Messages.getString("GameOptionsDialog.SearchToolTip"));
        txtSearch.setColumns(20);
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshSearchPanel();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshSearchPanel();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshSearchPanel();
            }
        });

        panSearchOptions.setLayout(new BoxLayout(panSearchOptions, BoxLayout.PAGE_AXIS));
        panSearchBar.add(lblSearch);
        panSearchBar.add(txtSearch);
        panSearch.add(panSearchBar);
        panSearch.add(panSearchOptions);
        panOptions.addTab(Messages.getString("GameOptionsDialog.Search"), scrOptions);
        refreshSearchPanel();
    }

    private void addOption(JPanel groupPanel, IOption option) {
        if (option == null) {
            return;
        }
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, true, true);

        groupPanel.add(optionComp);
        maxOptionWidth = Math.max(maxOptionWidth, optionComp.getPreferredSize().width);

        if (OptionsConstants.INIT_INF_DEPLOY_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  !(options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  !(options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_LATER.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FALLING_EXPANDED)) {
            if (!(options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN)).booleanValue()
                  || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)) {
            if ((options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)).booleanValue() || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)) {
            if (!options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE).booleanValue() || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)) {
            if ((options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)).booleanValue() || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC)) {
            if ((options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_VELOCITY)) {
            if (option.intValue() < CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY) {
                //Set to the minimum velocity if under
                option.setValue(CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY);
            } else if (option.intValue() > CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY) {
                //Set to the maximum velocity if over
                option.setValue(CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED)) {
            if ((options.getOption(OptionsConstants.ADVANCED_ALTERNATE_MASC)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("techlevel")) {
            for (String tlName : TechConstants.T_SIMPLE_NAMES) {
                optionComp.addValue(tlName);
            }
            optionComp.setSelected(option.stringValue());
            optionComp.setEditable(editable);
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT)) {
            // Disable if individual init is on
            if (!options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT)) {
            // Disable if individual init is on
            if (!options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            // Disable if any lance movement is on
            if (!options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT).booleanValue() &&
                  !options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else {
            optionComp.setEditable(editable);
        }
        List<DialogOptionComponentYPanel> comps = optionComps.computeIfAbsent(option.getName(), k -> new ArrayList<>());
        comps.add(optionComp);
    }

    // Gets called when one of the option checkboxes is clicked.
    // Arguments are the GameOption object and the true/false
    // state of the checkbox.
    @Override
    public void optionClicked(DialogOptionComponentYPanel clickedComp, IOption option, boolean state) {

        // Ensure that any other DialogOptionComponents with the same IOption
        // have the same value
        List<DialogOptionComponentYPanel> comps = optionComps.get(option.getName());
        for (DialogOptionComponentYPanel comp : comps) {
            if (!comp.equals(clickedComp)) {
                comp.setValue(clickedComp.getValue());
            }
        }

        if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOMEKS_MOVE_LATER.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT) ||
              option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT)) {
            comps = optionComps.get(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if ("vacuum".equals(option.getName())) {
            comps = optionComps.get("fire");
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FALLING_EXPANDED);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC)) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_ALTERNATE_MASC)) {
            comps = optionComps.get(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            if (clientGui != null) {
                for (Entity ent : clientGui.getClient().getGame().getEntitiesVector()) {
                    if (ent instanceof Mek) {
                        ((Mek) ent).setBAGrabBars();
                    }
                    if (ent instanceof Tank) {
                        ((Tank) ent).setBAGrabBars();
                    }
                }
            }
        }
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel clickedComp, IOption option, int i) {
        // tracks changes to a combobox option - nothing implemented yet
    }

    @Override
    protected void okAction() {
        if (clientGui != null) {
            send();
        }
        if (performSave) {
            doSave();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butSave) {
            File gameOptsFile = selectGameOptionsFile(true);
            if (gameOptsFile != null) {
                GameOptions.saveOptions(getOptions(), gameOptsFile.getAbsolutePath());
            }

        } else if (e.getSource() == butLoad) {
            File gameOptsFile = selectGameOptionsFile(false);
            if (gameOptsFile != null) {
                options.loadOptions(gameOptsFile, false);
                ArrayList<IOption> changed = new ArrayList<>();
                for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
                    // Each option in the list should have the same value, so picking the first is fine
                    if (!comps.isEmpty()) {
                        DialogOptionComponentYPanel comp = comps.get(0);
                        if (comp.hasChanged()) {
                            changed.add(comp.getOption());
                        }
                    }
                }
                refreshOptions();
                // We need to ensure that the IOption for the component doesn't
                // match, otherwise send() won't send updates to the server
                for (IOption opt : changed) {
                    List<DialogOptionComponentYPanel> comps = optionComps.get(opt.getName());
                    if (!comps.isEmpty()) {
                        comps.get(0).setOptionChanged(true);
                    }
                }
            }

        } else if (e.getSource().equals(butUnofficial)) {
            if (!butUnofficial.isSelected()) {
                boolean okay = MMConfirmDialog.confirm(frame, "Warning", getString("GameOptionsDialog.HideWarning"));
                if (!okay) {
                    butUnofficial.removeActionListener(this);
                    butUnofficial.setSelected(true);
                    butUnofficial.addActionListener(this);
                    return;
                }
            }
            optionComps.get(OptionsConstants.BASE_HIDE_UNOFFICIAL).get(0).setSelected(!butUnofficial.isSelected());
            toggleOptions();
            refreshSearchPanel();

        }
    }

    private File selectGameOptionsFile(boolean saveDialog) {
        JFileChooser fc = new JFileChooser("mmconf");
        fc.setLocation(getLocation().x + 150, getLocation().y + 100);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                } else if (dir.getName().endsWith(".xml")) {
                    try {
                        DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
                        Document doc = builder.parse(dir);
                        NodeList listOfComponents = doc.getElementsByTagName("options");
                        return listOfComponents.getLength() > 0;
                    } catch (Exception e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "GameOptions";
            }
        });
        int returnVal = saveDialog ? fc.showSaveDialog(this) : fc.showOpenDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return null;
        }
        File result = fc.getSelectedFile();
        if (!result.getName().endsWith(".xml")) {
            result = new File(result + ".xml");
        }
        return result;
    }

    /**
     * Update the dialog so that it is editable or view-only.
     *
     * @param editable - <code>true</code> if the contents of the dialog are editable, <code>false</code> if they are
     *                 view-only.
     */
    public void setEditable(boolean editable) {

        // Set enabled state of all the option components in the dialog.
        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            for (DialogOptionComponentYPanel comp : comps) {
                comp.setEditable(editable);
            }
        }

        // If the panel is editable, the player can commit or reset.
        texPass.setEnabled(editable);
        butOkay.setEnabled(editable);
        butDefaults.setEnabled(editable);
        butUnofficial.setEnabled(editable);

        // Update our data element.
        this.editable = editable;
    }

    /**
     * Determine whether the dialog is editable or view-only.
     *
     * @return <code>true</code> if the contents of the dialog are editable,
     *       <code>false</code> if they are view-only.
     */
    public boolean isEditable() {
        return editable;
    }
}
