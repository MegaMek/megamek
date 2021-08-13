/*
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.dialog.MMConfirmDialog;
import megamek.common.*;
import megamek.common.options.*;
import megamek.common.weapons.bayweapons.CapitalMissileBayWeapon;
import megamek.utils.MegaMekXmlUtil;
import static megamek.client.ui.swing.util.UIUtil.*;
import static megamek.client.ui.Messages.*;

/**
 * Responsible for displaying the current game options and allowing the user to
 * change them.
 *
 * @author Ben
 */
public class GameOptionsDialog extends JDialog implements ActionListener, DialogOptionListener {

    private static final long serialVersionUID = -6072295678938594119L;
    private ClientGUI clientGui;
    private JFrame frame;
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
    private JPanel panSearchOptions = new JPanel();
    /**
     * Text field that contains text to search on
     */
    private JTextField txtSearch = new JTextField("");
    private WrappingButtonPanel panPassword = new WrappingButtonPanel();
    private JLabel labPass = new JLabel(Messages.getString("GameOptionsDialog.Password")); 
    private JTextField texPass = new JTextField(15);
    private WrappingButtonPanel panButtons = new WrappingButtonPanel();
    private JButton butSave = new JButton(Messages.getString("GameOptionsDialog.Save")); 
    private JButton butLoad = new JButton(Messages.getString("GameOptionsDialog.Load")); 
    private JButton butDefaults = new JButton(Messages.getString("GameOptionsDialog.Defaults")); 
    private JButton butOkay = new JButton(Messages.getString("Okay")); 
    private JButton butCancel = new JButton(Messages.getString("Cancel")); 
    private MMToggleButton butUnofficial = new MMToggleButton("Unofficial Opts");
    private MMToggleButton butLegacy = new MMToggleButton("Legacy Opts");

    /**
     * When the OK button is pressed, the options can be saved to a file; this
     * behavior happens by default but there are some situations where the
     * options should not be saved, such as when loading a scenario.
     */
    private boolean performSave = true;
    
    private final static String UNOFFICIAL = "Unofficial";
    private final static String LEGACY = "Legacy";

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

        var mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(panOptions);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(panPassword);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(panButtons);
        add(mainPanel);

        pack();
        GUIPreferences guip = GUIPreferences.getInstance();
        int width = guip.getGameOptionsSizeWidth();
        int height = guip.getGameOptionsSizeHeight();
        setSize(width, height);
        setResizable(true);
        setLocationRelativeTo(frame);
    }

    /**
     * Creates new <code>GameOptionsDialog</code> for a <code>Client</code>
     *
     * @param cg
     *            - the <code>ClientGUI</code> parent of this dialog.
     */
    public GameOptionsDialog(ClientGUI cg) {
        super(cg.frame, Messages.getString("GameOptionsDialog.title"), true); 
        clientGui = cg;
        frame = cg.getFrame();
        init(frame, cg.getClient().getGame().getOptions());
    }

    public GameOptionsDialog(JFrame jf, GameOptions options, boolean shouldSave) {
        super(jf, Messages.getString("GameOptionsDialog.title"), true);
        performSave = shouldSave;
        frame = jf;
        init(frame, options);
        butOkay.setEnabled(false);
    }

    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
        butUnofficial.setSelected(GUIPreferences.getInstance().getBoolean(GUIPreferences.OPTIONS_SHOW_UNOFFICIAL));
        butLegacy.setSelected(GUIPreferences.getInstance().getBoolean(GUIPreferences.OPTIONS_SHOW_LEGACY));
        toggleOptions(butUnofficial.isSelected(), "Unofficial");
        toggleOptions(butLegacy.isSelected(), "Legacy");
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

        if ((clientGui != null) && (changed.size() > 0)) {
            clientGui.getClient().sendGameOptions(texPass.getText(), changed);
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
        validate();
    }
    
    /** 
     * When show is true, options that contain the given String str are shown.
     * When show is false, these options are hidden and deselected. 
     * Used to show/hide unofficial and legcy options.
     */
    private void toggleOptions(boolean show, String str) {
        for (List<DialogOptionComponent> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking the first is fine
            if (comps.size() > 0) {
                DialogOptionComponent comp = comps.get(0);
                if (comp.getOption().getDisplayableName().contains(str)) {
                    comp.setVisible(show);
                    // Disable hidden options
                    if (!show && comp.getOption().getType() == IOption.BOOLEAN) {
                        comp.setSelected(false);
                    }
                }
            }
        }
    }
    
    private boolean shouldShow(DialogOptionComponent comp) {
        boolean hiddenUnofficial = !butUnofficial.isSelected() 
                && comp.getOption().getDisplayableName().contains(UNOFFICIAL);
        boolean hiddenLegacy = !butLegacy.isSelected() 
                && comp.getOption().getDisplayableName().contains(LEGACY);
        return !hiddenLegacy && !hiddenUnofficial;
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
        if (!searchText.equals("")) {
            ArrayList<DialogOptionComponent> allNewComps = new ArrayList<>();
            for (List<DialogOptionComponent> comps : optionComps.values()) {
                ArrayList<DialogOptionComponent> newComps = new ArrayList<>();
                for (DialogOptionComponent comp : comps) {
                    String optName = comp.option.getDisplayableName().toLowerCase();
                    String optDesc = comp.option.getDescription().toLowerCase();
                    if ((optName.contains(searchText) || optDesc.contains(searchText)) && shouldShow(comp)) {
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
        }
        // panSearchOptions.validate();
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

        panSearchOptions.setLayout(new BoxLayout(panSearchOptions, BoxLayout.PAGE_AXIS));
        panSearchBar.add(lblSearch);
        panSearchBar.add(txtSearch);
        panSearch.add(panSearchBar);
        panSearch.add(panSearchOptions);
        panOptions.addTab(Messages.getString("GameOptionsDialog.Search"), scrOptions);
    }

    private void addOption(JPanel groupPanel, IOption option) {
        if (option == null) {
            return;
        }
        DialogOptionComponent optionComp = new DialogOptionComponent(this, option, true, true);

        groupPanel.add(optionComp);
        maxOptionWidth = Math.max(maxOptionWidth, optionComp.getPreferredSize().width);

        if (OptionsConstants.INIT_INF_DEPLOY_EVEN.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || !(options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_EVEN.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || !(options.getOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_MULTI.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_LATER)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_EVEN.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_LATER)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOS_MOVE_LATER.equals(option.getName())) { 
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_EVEN)).booleanValue() 
                    || (options.getOption(OptionsConstants.INIT_PROTOS_MOVE_MULTI)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVGRNDMOV_TACOPS_FALLING_EXPANDED)) { 
            if (!(options.getOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_LOS1)) { 
            if ((options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES)).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)) { 
            if (!options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE).booleanValue() 
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES)) { 
            if ((options.getOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS1)).booleanValue() 
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
        } else if (option.getName().equals(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_VELOCITY)) {
            if (option.intValue() < CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY) {
                //Set to the minimum velocity if under
                option.setValue(CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY);
            } else if (option.intValue() > CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY) {
                //Set to the maximum velocity if over
                option.setValue(CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY);
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
        } else if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            // Disable if any lance movement is on
            if (!options.getOption(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT).booleanValue() 
                    && !options.getOption(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT).booleanValue()) {
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

        if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOS_MOVE_EVEN.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_MULTI); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_LATER); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOS_MOVE_MULTI.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_LATER); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOS_MOVE_LATER.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_MULTI); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_MULTI); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOS_MOVE_LATER); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER); 
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
        if (option.getName().equals(OptionsConstants.ADVGRNDMOV_VEHICLE_LANCE_MOVEMENT)
                || option.getName().equals(OptionsConstants.ADVGRNDMOV_MEK_LANCE_MOVEMENT)) {
            comps = optionComps.get(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE);
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if ("vacuum".equals(option.getName())) { 
            comps = optionComps.get("fire"); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.ADVGRNDMOV_TACOPS_FALLING_EXPANDED); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_TACOPS_LOS1); 
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
        if (OptionsConstants.ADVCOMBAT_TACOPS_LOS1.equals(option.getName())) { 
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_TACOPS_DEAD_ZONES); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC)) {
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_KIND_RAPID_AC); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD)) {
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.resetToDefault();
            }
        }
        if (option.getName().equals(OptionsConstants.RPG_MANUAL_SHUTDOWN)) {
            comps = optionComps.get(OptionsConstants.RPG_BEGIN_SHUTDOWN); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_ALTERNATE_MASC)) {
            comps = optionComps.get(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED); 
            for (DialogOptionComponent comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            if (clientGui != null) {
                for (Entity ent : clientGui.getClient().getGame().getEntitiesVector()) {
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
        butUnofficial.addActionListener(this);
        butLegacy.addActionListener(this);
        
        panButtons.add(butUnofficial);
        panButtons.add(butLegacy);
        panButtons.add(Box.createHorizontalStrut(30));
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butDefaults);
        panButtons.add(butSave);
        panButtons.add(butLoad);
    }

    private void setupPassword() {
        labPass.setLabelFor(texPass);
        panPassword.add(labPass);
        panPassword.add(texPass);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {
            cancelled = false;
            if (clientGui != null) {
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
            toggleOptions(butUnofficial.isSelected(), "Unofficial");
            refreshSearchPanel();
            GUIPreferences.getInstance().setValue(GUIPreferences.OPTIONS_SHOW_UNOFFICIAL, butUnofficial.isSelected());
            return;
        } else if (e.getSource().equals(butLegacy)) {
            if (!butLegacy.isSelected()) {
                boolean okay = MMConfirmDialog.confirm(frame, "Warning", getString("GameOptionsDialog.HideWarning"));
                if (!okay) {
                    butLegacy.removeActionListener(this);
                    butLegacy.setSelected(true);
                    butLegacy.addActionListener(this);
                    return;
                }
            }
            toggleOptions(butLegacy.isSelected(), "Legacy");
            refreshSearchPanel();
            GUIPreferences.getInstance().setValue(GUIPreferences.OPTIONS_SHOW_LEGACY, butLegacy.isSelected());
            return;
        }
        setVisible(false);
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
                        DocumentBuilder builder = MegaMekXmlUtil.newSafeDocumentBuilder();
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
        int returnVal;
        returnVal = saveDialog ? fc.showSaveDialog(this) : fc.showOpenDialog(this);
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
        butUnofficial.setEnabled(editable);
        butLegacy.setEnabled(editable);

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
