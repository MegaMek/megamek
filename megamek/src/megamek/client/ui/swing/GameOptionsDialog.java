/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * GameOptionsDialog.java
 *
 * Created on April 26, 2002, 2:14 PM
 */

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;

/**
 * Responsible for displaying the current game options and allowing the user to
 * change them.
 *
 * @author Ben
 */
public class GameOptionsDialog extends JDialog implements ActionListener, DialogOptionListener {

    /**
     *
     */
    private static final long serialVersionUID = -6072295678938594119L;
    private ClientGUI client;
    private GameOptions options;

    private boolean editable = true;
    private boolean cancelled = false;

    /**
     * A map that maps an option to a collection of DialogOptionComponents that
     * can effect the value of this option.
     */
    private Map<String, List<DialogOptionComponent>> optionComps = new HashMap<>();

    /**
     * Keeps track of the DialogOptionComponents that have been added to the
     * search panel. This is used to remove those components from optionComps
     * when they get removed.
     */
    private ArrayList<DialogOptionComponent> searchComps = new ArrayList<>();

    private int maxOptionWidth;

    private JTabbedPane panOptions = new JTabbedPane();

    /**
     * Panel that holds all of the options found via search
     */
    private JPanel panSearchOptions;
    /**
     * Text field that contains text to search on
     */
    private JTextField txtSearch;
    private JPanel panPassword = new JPanel();
    private JLabel labPass = new JLabel(Messages.getString("GameOptionsDialog.Password")); //$NON-NLS-1$
    private JTextField texPass = new JTextField(15);

    private JPanel panButtons = new JPanel();
    private JButton butSave = new JButton(Messages.getString("GameOptionsDialog.Save")); //$NON-NLS-1$
    private JButton butLoad = new JButton(Messages.getString("GameOptionsDialog.Load")); //$NON-NLS-1$
    private JButton butDefaults = new JButton(Messages.getString("GameOptionsDialog.Defaults")); //$NON-NLS-1$
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    /**
     * When the OK button is pressed, the options can be saved to a file; this
     * behavior happens by default but there are some situations where the
     * options should not be saved, such as when loading a scenario.
     */
    private boolean performSave = true;

    /**
     * Initialize this dialog.
     *
     * @param frame
     *            - the <code>Frame</code> parent of this dialog.
     * @param options
     *            - the <code>GameOptions</code> to be displayed.
     */
    private void init(JFrame frame, GameOptions options) {
        this.options = options;

        setupButtons();
        setupPassword();
        JPanel mainPanel = new JPanel(new GridBagLayout());

        // layout
        mainPanel.add(panOptions, GBC.eol().fill(GridBagConstraints.BOTH).insets(5, 5, 5, 5));
        mainPanel.add(panPassword, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 5, 5));
        mainPanel.add(panButtons, GBC.eol().anchor(GridBagConstraints.CENTER));

        getContentPane().add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        GUIPreferences guip = GUIPreferences.getInstance();
        int width = guip.getGameOptionsSizeWidth();
        int height = guip.getGameOptionsSizeHeight();
        setSize(width, height);
        setResizable(true);
        setLocationRelativeTo(frame);
        Dimension size = new Dimension((getSize().width * 40) / 100, (getSize().height * 59) / 100);
        panOptions.setPreferredSize(size);
        panOptions.setMinimumSize(size);
        panOptions.setMaximumSize(size);

    }

    /**
     * Creates new <code>GameOptionsDialog</code> for a <code>Client</code>
     *
     * @param client
     *            - the <code>Client</code> parent of this dialog.
     */
    public GameOptionsDialog(ClientGUI client) {
        super(client.frame, Messages.getString("GameOptionsDialog.title"), true); //$NON-NLS-1$
        this.client = client;
        init(client.frame, client.getClient().getGame().getOptions());
    }

    public GameOptionsDialog(JFrame frame, GameOptions options, boolean shouldSave) {
        super(frame, Messages.getString("GameOptionsDialog.title"), true);
        // $NON-NLS-1$
        performSave = shouldSave;
        init(frame, options);
        butOkay.setEnabled(false);
    }

    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }

    private void send() {
        Vector<IBasicOption> changed = new Vector<IBasicOption>();

        for (List<DialogOptionComponent> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking
            // the first is fine
            if (comps.size() > 0) {
                DialogOptionComponent comp = comps.get(0);
                if (comp.hasChanged()) {
                    changed.addElement(comp.changedOption());
                    comp.setOptionChanged(false);
                }
            }
        }

        if ((client != null) && (changed.size() > 0)) {
            client.getClient().sendGameOptions(texPass.getText(), changed);
        }
    }

    private void doSave() {
        GameOptions.saveOptions(getOptions());
    }

    public Vector<IBasicOption> getOptions() {
        Vector<IBasicOption> output = new Vector<IBasicOption>();

        for (List<DialogOptionComponent> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking
            // the first is fine
            if (comps.size() > 0) {
                IBasicOption option = comps.get(0).changedOption();
                output.addElement(option);
            }
        }
        return output;
    }

    private void resetToDefaults() {
        for (List<DialogOptionComponent> comps : optionComps.values()) {
            for (DialogOptionComponent comp : comps) {
                if (!comp.isDefaultValue()) {
                    comp.resetToDefault();
                }
            }
        }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new HashMap<>();

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            JPanel groupPanel = addGroup(group);

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();
                addOption(groupPanel, option);
            }
        }

        addSearchPanel();

        // Make the width accomadate the longest game option label
        // without needing to scroll horizontally.
        setSize(Math.max(getSize().width, maxOptionWidth + 30), Math.max(getSize().height, 400));

        validate();
    }

    private void refreshSearchPanel() {
        panSearchOptions.removeAll();
        // We need to first remove all of the DialogOptionComponents
        // that were on the search panel
        for (DialogOptionComponent comp : searchComps) {
            List<DialogOptionComponent> compList = optionComps.get(comp.option.getName());
            if (compList != null) { // Shouldn't be null...
                compList.remove(comp);
            }
        }

        // Add new DialogOptionComponents for all matching Options
        final String searchText = txtSearch.getText().toLowerCase();
        ArrayList<DialogOptionComponent> allNewComps = new ArrayList<>();
        for (List<DialogOptionComponent> comps : optionComps.values()) {
            ArrayList<DialogOptionComponent> newComps = new ArrayList<>();
            for (DialogOptionComponent comp : comps) {
                String optName = comp.option.getDisplayableName().toLowerCase();
                String optDesc = comp.option.getDescription().toLowerCase();
                if ((optName.contains(searchText) || optDesc.contains(searchText)) && !searchText.equals("")) {
                    DialogOptionComponent newComp = new DialogOptionComponent(this, comp.option);
                    newComp.setEditable(comp.getEditable());
                    searchComps.add(newComp);
                    newComps.add(newComp);
                }
            }
            comps.addAll(newComps);
            allNewComps.addAll(newComps);
        }
        Collections.sort(allNewComps);
        for (DialogOptionComponent comp : allNewComps) {
            panSearchOptions.add(comp);
        }
        // panSearchOptions.validate();
        panOptions.repaint();
    }

    private JPanel addGroup(IOptionGroup group) {
        JPanel groupPanel = new JPanel();
        JScrollPane scrOptions = new JScrollPane(groupPanel);
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
        scrOptions.setAutoscrolls(true);
        scrOptions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrOptions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Panel for holding the label and text field for searching
        JPanel panText = new JPanel();
        JLabel lblSearch = new JLabel(Messages.getString("GameOptionsDialog.Search") + ":");
        txtSearch = new JTextField("");
        lblSearch.setToolTipText(Messages.getString("GameOptionsDialog.SearchToolTip"));
        txtSearch.setToolTipText(Messages.getString("GameOptionsDialog.SearchToolTip"));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                refreshSearchPanel();
            }

            public void insertUpdate(DocumentEvent e) {
                refreshSearchPanel();
            }

            public void removeUpdate(DocumentEvent e) {
                refreshSearchPanel();
            }
        });

        panText.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(10, 5, 15, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        panText.add(lblSearch, gbc);
        gbc.gridx++;
        gbc.weightx = 0.7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panText.add(txtSearch, gbc);

        panSearchOptions = new JPanel();
        panSearchOptions.setLayout(new BoxLayout(panSearchOptions, BoxLayout.Y_AXIS));

        panSearch.setLayout(new GridBagLayout());
        gbc.gridx = gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.7;
        panSearch.add(panText, gbc);
        gbc.gridy++;
        gbc.weighty = 0.7;

        gbc.fill = GridBagConstraints.BOTH;
        panSearch.add(panSearchOptions, gbc);

        panOptions.addTab(Messages.getString("GameOptionsDialog.Search"), scrOptions);
    }

    private void addOption(JPanel groupPanel, IOption option) {
        if (option == null) {
            return;
        }
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, true, true);

        groupPanel.add(optionComp);
        maxOptionWidth = Math.max(maxOptionWidth, optionComp.getPreferredSize().width);

        if (OptionsConstants.INIT_INF_DEPLOY_EVEN.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || !(options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_EVEN.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || !(options.getOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_MULTI.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_LATER)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_EVEN.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_LATER)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_LATER.equals(option.getName())) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN)).booleanValue() // $NON-NLS-1$
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVGRNDMOV_TACOPS_FALLING_EXPANDED)) { // $NON-NLS-1$
            if (!(options.getOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_LOS1)) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)) { // $NON-NLS-1$
            if (!options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES)) { // $NON-NLS-1$
            if ((options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS1)).booleanValue() // $NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC)) {
            if ((options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR)) {
            if ((options.getOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE)) {
            if ((options.getOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.RPG_BEGIN_SHUTDOWN)) {
            if ((options.getOption(OptionsConstants.RPG_MANUAL_SHUTDOWN)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
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
        } else if (option.getName().equals(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT)) {
            // Disable if individual init is on
            if (!options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT)) {
            // Disable if individual init is on
            if (!options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else {
            optionComp.setEditable(editable);
        }
        List<DialogOptionComponent> comps = optionComps.get(option.getName());
        if (comps == null) {
            comps = new ArrayList<DialogOptionComponent>();
            optionComps.put(option.getName(), comps);
        }
        comps.add(optionComp);
    }

    // Gets called when one of the option checkboxes is clicked.
    // Arguments are the GameOption object and the true/false
    // state of the checkbox.
    public void optionClicked(DialogOptionComponent clickedComp, IOption option, boolean state) {

        // Ensure that any other DialogOptionComponents with the same IOption
        // have the same value
        List<DialogOptionComponent> comps = optionComps.get(option.getName());
        for (DialogOptionComponent comp : comps) {
            if (!comp.equals(clickedComp)) {
                comp.setValue(clickedComp.getValue());
            }
        }

        if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOS_MOVE_EVEN.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_MULTI); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_LATER); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOS_MOVE_MULTI.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_LATER); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOS_MOVE_LATER.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_MULTI); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_MULTI); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_LATER); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT);
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT);
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if ("vacuum".equals(option.getName())) { //$NON-NLS-1$
            comps = optionComps.get("fire"); //$NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.ADVGRNDMOV_TACOPS_FALLING_EXPANDED); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_TACOPS_LOS1); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVCOMBAT_TACOPS_RANGE.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVCOMBAT_TACOPS_LOS1.equals(option.getName())) { // $NON-NLS-1$
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC)) {
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.resetToDefault();
            }
        }
        if (option.getName().equals(OptionsConstants.RPG_MANUAL_SHUTDOWN)) {
            comps = optionComps.get(OptionsConstants.RPG_BEGIN_SHUTDOWN); //$NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_ALTERNATE_MASC)) {
            comps = optionComps.get(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED); // $NON-NLS-1$
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            if (client != null) {
                for (Entity ent : client.getClient().getGame().getEntitiesVector()) {
                    if (ent instanceof Mech) {
                        ((Mech) ent).setBAGrabBars();
                    }
                    if (ent instanceof Tank) {
                        ((Tank) ent).setBAGrabBars();
                    }
                }
            }
        }
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butDefaults.addActionListener(this);
        butSave.addActionListener(this);
        butLoad.addActionListener(this);

        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butDefaults);
        panButtons.add(butSave);
        panButtons.add(butLoad);
    }

    private void setupPassword() {
        panPassword.setLayout(new BorderLayout());

        panPassword.add(labPass, BorderLayout.WEST);
        panPassword.add(texPass, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {
            cancelled = false;
            if (client != null) {
                send();
            }
            if (performSave) {
                doSave();
            }
        } else if (e.getSource().equals(butDefaults)) {
            resetToDefaults();
            return;
        } else if (e.getSource().equals(butSave)) {
            File gameOptsFile = selectGameOptionsFile(true);
            if (gameOptsFile != null) {
                GameOptions.saveOptions(getOptions(), gameOptsFile.getAbsolutePath());
            }
            return;
        } else if (e.getSource().equals(butLoad)) {
            File gameOptsFile = selectGameOptionsFile(false);
            if (gameOptsFile != null) {
                options.loadOptions(gameOptsFile, false);
                ArrayList<IOption> changed = new ArrayList<>();
                for (List<DialogOptionComponent> comps : optionComps.values()) {
                    // Each option in the list should have the same value, so
                    // picking the first is fine
                    if (comps.size() > 0) {
                        DialogOptionComponent comp = comps.get(0);
                        if (comp.hasChanged()) {
                            changed.add(comp.getOption());
                        }
                    }
                }
                refreshOptions();
                // We need to ensure that the IOption for the component doesn't
                // match, otherwise send() won't send updates to the server
                for (IOption opt : changed) {
                    String name = opt.getName();
                    List<DialogOptionComponent> comps = optionComps.get(name);
                    if (comps.size() > 0) {
                        comps.get(0).setOptionChanged(true);
                    }
                }
            }
            return;
        } else if (e.getSource().equals(butCancel)) {
            cancelled = true;
        }
        setVisible(false);
    }

    private File selectGameOptionsFile(boolean saveDialog) {
        JFileChooser fc = new JFileChooser("mmconf"); //$NON-NLS-1$
        fc.setLocation(getLocation().x + 150, getLocation().y + 100);
        // fc.setDialogTitle(Messages.getString(
        // "GameOptionsDialog.FileChooser.title")); //$NON-NLS-1$
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                } else if (dir.getName().endsWith(".xml")) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    try {
                        DocumentBuilder builder = dbf.newDocumentBuilder();
                        Document doc = builder.parse(dir);
                        NodeList listOfComponents = doc.getElementsByTagName("options");
                        if (listOfComponents.getLength() > 0) {
                            return true;
                        } else {
                            return false;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "GameOptions"; //$NON-NLS-1$
            }
        });
        int returnVal;
        if (saveDialog) {
            returnVal = fc.showSaveDialog(this);
        } else {
            returnVal = fc.showOpenDialog(this);
        }
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
     * @param editable
     *            - <code>true</code> if the contents of the dialog are
     *            editable, <code>false</code> if they are view-only.
     */
    public void setEditable(boolean editable) {

        // Set enabled state of all of the option components in the dialog.
        for (List<DialogOptionComponent> comps : optionComps.values()) {
            for (DialogOptionComponent comp : comps) {
                comp.setEditable(editable);
            }
        }

        // If the panel is editable, the player can commit or reset.
        texPass.setEnabled(editable);
        butOkay.setEnabled(editable);
        butDefaults.setEnabled(editable);

        // Update our data element.
        this.editable = editable;
    }

    /**
     * Determine whether the dialog is editable or view-only.
     *
     * @return <code>true</code> if the contents of the dialog are editable,
     *         <code>false</code> if they are view-only.
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Determine whether the dialog was cancelled.
     *
     * @return <code>true</code> if the dialog was cancelled, <code>false</code>
     *         if it was not
     */
    public boolean wasCancelled() {
        return cancelled;
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_DEACTIVATED) {
            GUIPreferences guip = GUIPreferences.getInstance();
            guip.setGameOptionsSizeHeight(getSize().height);
            guip.setGameOptionsSizeWidth(getSize().width);
        }
    }

}
