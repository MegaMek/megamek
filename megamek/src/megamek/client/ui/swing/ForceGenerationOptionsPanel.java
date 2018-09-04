/*
 * MegaMek -
 * Copyright (C) 2016 The MegaMek team
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
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.FormationType;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.client.ratgenerator.UnitTable.Parameters;
import megamek.client.ui.Messages;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.UnitType;

/**
 *
 * Panel that allows choice of year, faction, rating, unit type
 *
 * @author Neoancient
 */
class ForceGenerationOptionsPanel extends JPanel implements ActionListener, FocusListener {

    private static final long serialVersionUID = -3462304612643343012L;
    
    public enum Use {
        RAT_GENERATOR, FORMATION_BUILDER //, FORCE_GENERATOR
    };
        
    private JTextField txtNumUnits = new JTextField(3);
    private JTextField txtYear = new JTextField(4);
    private JComboBox<FactionRecord> cbFaction = new JComboBox<FactionRecord>();
    private JComboBox<FactionRecord> cbSubfaction = new JComboBox<FactionRecord>();
    private JCheckBox chkShowMinor = new JCheckBox(Messages
            .getString("RandomArmyDialog.ShowMinorFactions"));
    private JComboBox<String> cbUnitType = new JComboBox<String>();
    private JComboBox<String> cbRating = new JComboBox<String>();

    private UnitTypeOptions panUnitTypeOptions;
   
    private int ratGenYear;
    
    private static final int[] UNIT_TYPES = {
        UnitType.MEK, UnitType.TANK, UnitType.BATTLE_ARMOR, UnitType.INFANTRY, UnitType.PROTOMEK,
        UnitType.VTOL, UnitType.NAVAL, UnitType.CONV_FIGHTER, UnitType.AERO, UnitType.SMALL_CRAFT,
        UnitType.DROPSHIP, UnitType.JUMPSHIP, UnitType.WARSHIP
    };
    private static final int EARLIEST_YEAR = 2398;
    private static final int LATEST_YEAR = 3150;

    public ForceGenerationOptionsPanel() {
        this(null, null);
    }
    
    public ForceGenerationOptionsPanel(Use use, ClientGUI gui) {
        setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.Year")), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(txtYear, c);
        txtYear.setText(String.valueOf(ratGenYear));
        txtYear.addFocusListener(this);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.Unit")), c);

        txtNumUnits.setText("4");

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(txtNumUnits, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.Faction")), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(cbFaction, c);
        cbFaction.setRenderer(factionCbRenderer);
        cbFaction.addActionListener(this);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.Command")), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(cbSubfaction, c);
        cbSubfaction.setRenderer(factionCbRenderer);
        cbSubfaction.addActionListener(this);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(chkShowMinor, c);
        chkShowMinor.addActionListener(this);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.UnitType")), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(cbUnitType, c);
        for (int i = 0; i < UNIT_TYPES.length; i++) {
            if (UNIT_TYPES[i] < UnitType.JUMPSHIP || !use.equals(Use.FORMATION_BUILDER)) {
                cbUnitType.addItem(UnitType.getTypeName(UNIT_TYPES[i]));
            }
        }
        cbUnitType.addActionListener(this);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.Rating")), c);

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(cbRating, c);
        
        if (use != null) {
            switch(use) {
            case RAT_GENERATOR:
                panUnitTypeOptions = new RATGenUnitTypeOptions();
                break;
            case FORMATION_BUILDER:
                panUnitTypeOptions = new FormationUnitTypeOptions();
                break;
            }
        }
        
        if (panUnitTypeOptions != null) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 0.0;
            c.weighty = 0.5;
            add(panUnitTypeOptions, c);            
        }
        cbUnitType.setSelectedItem(0);
        if (!RATGenerator.getInstance().isInitialized()) {
            RATGenerator.getInstance().registerListener(this);
        }
        if (RATGenerator.getInstance().isInitialized()) {
            updateFactionChoice();
        }

        if (panUnitTypeOptions != null) {
            panUnitTypeOptions.optionsChanged();
        }
    }
    
    public int getNumUnits() {
        return Integer.parseInt(txtNumUnits.getText());
    }

    public int getYear() {
        return Integer.parseInt(txtYear.getText());
    }

    public FactionRecord getFaction() {
        if (cbSubfaction.getSelectedItem() == null) {
            return (FactionRecord)cbFaction.getSelectedItem();
        } else {
            return (FactionRecord)cbSubfaction.getSelectedItem();
        }
    }

    public String getUnitTypeName() {
        return (String)cbUnitType.getSelectedItem();
    }
    
    public Integer getUnitType() {
        return ModelRecord.parseUnitType((String)cbUnitType.getSelectedItem());
    }

    public String getRating() {
        return (String)cbRating.getSelectedItem();
    }

    public void setYear(int year) {
        ratGenYear = year;
        txtYear.setText(String.valueOf(year));
        if (RATGenerator.getInstance().isInitialized()) {
            updateFactionChoice();
        }
    }
    
    public Integer getIntegerOption(String key) {
        return panUnitTypeOptions == null? null : panUnitTypeOptions.getIntegerVal(key);
    }

    public Boolean getBooleanOption(String key) {
        return panUnitTypeOptions == null? null : panUnitTypeOptions.getBooleanVal(key);
    }

    public String getStringOption(String key) {
        return panUnitTypeOptions == null? null : panUnitTypeOptions.getStringVal(key);
    }
    
    public List<?> getListOption(String key) {
        return panUnitTypeOptions == null? new ArrayList<>() : panUnitTypeOptions.getListVal(key);
    }

    public void updateFactionChoice() {
        FactionRecord old = (FactionRecord)cbFaction.getSelectedItem();
        cbFaction.removeActionListener(this);
        cbFaction.removeAllItems();
        ArrayList<FactionRecord> recs = new ArrayList<>();
        for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
            if ((!fRec.isMinor() || chkShowMinor.isSelected())
                    && !fRec.getKey().contains(".") && fRec.isActiveInYear(ratGenYear)) {
                recs.add(fRec);
            }
        }
        Collections.sort(recs, factionSorter);
        for (FactionRecord fRec : recs) {
            cbFaction.addItem(fRec);
        }
        cbFaction.setSelectedItem(old);
        if (cbFaction.getSelectedItem() == null) {
            cbFaction.setSelectedItem(RATGenerator.getInstance().getFaction("IS"));
        }
        updateSubfactionChoice();
        cbFaction.addActionListener(this);
    }

    public void updateSubfactionChoice() {
        FactionRecord old = (FactionRecord)cbSubfaction.getSelectedItem();
        cbSubfaction.removeActionListener(this);
        cbSubfaction.removeAllItems();
        FactionRecord selectedFaction = (FactionRecord)cbFaction.getSelectedItem();
        if (selectedFaction != null) {
            ArrayList<FactionRecord> recs = new ArrayList<>();
            for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
                if (fRec.getKey().startsWith(selectedFaction.getKey() + ".")
                        && fRec.isActiveInYear(ratGenYear)) {
                    recs.add(fRec);
                }
            }
            Collections.sort(recs, factionSorter);
            cbSubfaction.addItem(null); //No specific subcommand.
            for (FactionRecord fRec : recs) {
                cbSubfaction.addItem(fRec);
            }
        }
        cbSubfaction.setSelectedItem(old);
        updateRatingChoice();
        cbSubfaction.addActionListener(this);
    }

    /**
     * When faction or subfaction is changed, refresh ratings combo box with appropriate
     * values for selected faction.
     *
     */

    public void updateRatingChoice() {
        int current = cbRating.getSelectedIndex();
        cbRating.removeAllItems();
        FactionRecord fRec = (FactionRecord)cbSubfaction.getSelectedItem();
        if (fRec == null) {
            // Subfaction is "general"
            fRec = (FactionRecord)cbFaction.getSelectedItem();
        }
        ArrayList<String> ratingLevels = fRec.getRatingLevels();
        if (ratingLevels.isEmpty()) {
            // Get rating levels from parent faction(s)
            ratingLevels = fRec.getRatingLevelSystem();
        }
        if (ratingLevels.size() > 1) {
            for (int i = ratingLevels.size() - 1; i >= 0; i--) {
                cbRating.addItem(ratingLevels.get(i));
            }
        }
        if (current < 0 && cbRating.getItemCount() > 0) {
            cbRating.setSelectedIndex(0);
        } else {
            cbRating.setSelectedIndex(Math.min(current, cbRating.getItemCount() - 1));
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(cbFaction)) {
            updateSubfactionChoice();
        } else if (ev.getSource().equals(cbSubfaction)) {
            updateRatingChoice();
        } else if (ev.getSource().equals(chkShowMinor)) {
            updateFactionChoice();
        } else if (ev.getSource().equals(cbUnitType)) {
            if (panUnitTypeOptions != null) {
                panUnitTypeOptions.optionsChanged();
            }
        } else if (ev.getActionCommand().equals("ratGenInitialized")) {
            updateFactionChoice();
            RATGenerator.getInstance().removeListener(this);
            RATGenerator.getInstance().loadYear(ratGenYear);
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        //ignored
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (e.getSource().equals(txtYear)) {
            try {
                ratGenYear = Integer.parseInt(txtYear.getText());
                if (ratGenYear < EARLIEST_YEAR) {
                    ratGenYear = EARLIEST_YEAR;
                } else if (ratGenYear > LATEST_YEAR) {
                    ratGenYear = LATEST_YEAR;
                }
            } catch (NumberFormatException ex) {
                //ignore and restore to previous value
            }
            setYear(ratGenYear);
            RATGenerator.getInstance().loadYear(ratGenYear);
        }
    }
    
    public void updateGeneratedUnits(List<MechSummary> list) {
    	panUnitTypeOptions.updateGeneratedUnits(list);
    }

    private DefaultListCellRenderer factionCbRenderer = new DefaultListCellRenderer() {
        private static final long serialVersionUID = -333065979253244440L;

        @Override
        public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            if (value == null) {
                setText("General");
            } else {
                setText(((FactionRecord)value).getName(ratGenYear));
            }
            return this;
        }
    };
    
    private Comparator<FactionRecord> factionSorter = new Comparator<FactionRecord>() {
        @Override
        public int compare(FactionRecord o1, FactionRecord o2) {
            return o1.getName(ratGenYear).compareTo(o2.getName(ratGenYear));
        }
    };

    /**
     * Additional options that are conditional on the selected unit type can be added
     */
    abstract class UnitTypeOptions extends JPanel {
        private static final long serialVersionUID = -7141802206126462796L;

        abstract public void optionsChanged();
        
        public Integer getIntegerVal(String key) {
            return null;
        }
        
        public Boolean getBooleanVal(String key) {
            return null;
        }
        
        public String getStringVal(String key) {
            return null;
        }
        
        public List<? extends Object> getListVal(String key) {
            return new ArrayList<>();
        }
        
        public abstract void updateGeneratedUnits(List<MechSummary> list);
    }
    
    public class RATGenUnitTypeOptions extends UnitTypeOptions {
        private static final long serialVersionUID = 6536972747395725718L;

        private Map<String,RATGenUnitTypeCard> cardMap = new HashMap<>();
        
        public RATGenUnitTypeOptions() {
            setLayout(new CardLayout());
            for (int i = 0; i < cbUnitType.getItemCount(); i++) {
                int ut = ModelRecord.parseUnitType(cbUnitType.getItemAt(i));
                RATGenUnitTypeCard card = new RATGenUnitTypeCard(ut);
                cardMap.put(cbUnitType.getItemAt(i), card);
                add(card, cbUnitType.getItemAt(i));
            };
        }
        
        public void optionsChanged() {
            ((CardLayout)getLayout()).show(this, (String)cbUnitType.getSelectedItem());
        }
        
        private RATGenUnitTypeCard currentCard() {
            return cardMap.get(cbUnitType.getSelectedItem());
        }
        
        @Override
        public Integer getIntegerVal(String key) {
            switch (key) {
            case "networkMask":
                return currentCard().getNetworkMask();
            case "roleStrictness":
                return currentCard().getRoleStrictness();
            }
            return null;
        }
        
        @Override
        public List<? extends Object> getListVal(String key) {
            switch(key) {
            case "weightClasses":
                return currentCard().getSelectedWeights();
            case "motiveTypes":
                return currentCard().getMotiveTypes();
            case "roles":
                return currentCard().getSelectedRoles();
            }
            return new ArrayList<>();
        }

        public void updateGeneratedUnits(List<MechSummary> list) {
        	
        }
    }

    /**
     * Options that vary according to unit type
     *
     */
    
    class RATGenUnitTypeCard extends JPanel {
        private static final long serialVersionUID = -3961143911841133921L;

        private JComboBox<String> cbWeightClass = new JComboBox<String>();
        private ArrayList<JCheckBox> weightChecks = new ArrayList<JCheckBox>();
        private JComboBox<String> cbRoleStrictness = new JComboBox<String>();
        private ArrayList<JCheckBox> roleChecks = new ArrayList<JCheckBox>();
        private ButtonGroup networkButtons = new ButtonGroup();
        private ArrayList<JCheckBox> subtypeChecks = new ArrayList<JCheckBox>();
        
        public RATGenUnitTypeCard(int unitType) {
            setLayout(new BorderLayout());

            JPanel panWeightClass = new JPanel(new GridBagLayout());
            panWeightClass.setBorder(BorderFactory.createTitledBorder(Messages
                    .getString("RandomArmyDialog.WeightClass")));
            add(panWeightClass, BorderLayout.WEST);
            
            JPanel panRoles = new JPanel(new GridBagLayout());
            panRoles.setBorder(BorderFactory.createTitledBorder(Messages
                    .getString("RandomArmyDialog.MissionRole")));
            
            JPanel panStrictness = new JPanel();
            panStrictness.add(new JLabel(Messages.getString("RandomArmyDialog.Strictness")));
            cbRoleStrictness.setToolTipText(Messages.getString("RandomArmyDialog.Strictness.tooltip"));
            cbRoleStrictness.addItem(Messages.getString("RandomArmyDialog.Low"));
            cbRoleStrictness.addItem(Messages.getString("RandomArmyDialog.Medium"));
            cbRoleStrictness.addItem(Messages.getString("RandomArmyDialog.High"));
            cbRoleStrictness.setSelectedIndex(1);
            panStrictness.add(cbRoleStrictness);
            
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
            panRoles.add(panStrictness, c);
            
            add(panRoles, BorderLayout.CENTER);

            JPanel panNetwork = new JPanel(new GridBagLayout());
            panNetwork.setBorder(BorderFactory.createTitledBorder(Messages
                    .getString("RandomArmyDialog.Network")));
            add(panNetwork, BorderLayout.EAST);
            
            JPanel panMotive = new JPanel();
            add(panMotive, BorderLayout.NORTH);
            
            switch(unitType) {
            case UnitType.MEK:
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_ULTRA_LIGHT,
                        EntityWeightClass.WEIGHT_COLOSSAL, false);
                break;
            case UnitType.TANK:
            case UnitType.NAVAL:
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_LIGHT,
                        EntityWeightClass.WEIGHT_ASSAULT, false);
                break;
            case UnitType.PROTOMEK:             
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_LIGHT,
                        EntityWeightClass.WEIGHT_ASSAULT, true);
                break;
            case UnitType.BATTLE_ARMOR:
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_ULTRA_LIGHT,
                        EntityWeightClass.WEIGHT_ASSAULT, true);
                break;
            case UnitType.AERO:
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_LIGHT,
                        EntityWeightClass.WEIGHT_HEAVY, false);
                break;
            case UnitType.DROPSHIP:
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_SMALL_DROP,
                        EntityWeightClass.WEIGHT_LARGE_DROP, true);
                break;
            case UnitType.WARSHIP:
                addWeightClasses(panWeightClass, EntityWeightClass.WEIGHT_SMALL_WAR,
                        EntityWeightClass.WEIGHT_LARGE_WAR, true);
                break;
            default:
                panWeightClass.setVisible(false);
            }
            
            for (MissionRole role : MissionRole.values()) {
                if (role.fitsUnitType(unitType)) {
                    JCheckBox chk = new JCheckBox(Messages.getString("MissionRole."
                            + role.toString()));
                    chk.setToolTipText(Messages.getString("MissionRole."
                            + role.toString() + ".tooltip"));
                    chk.setName(role.toString());
                    roleChecks.add(chk);
                }
            }
            Collections.sort(roleChecks, (c1, c2) -> c1.getText().compareTo(c2.getText()));
            c = new GridBagConstraints();
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
            for (int i = 0; i < roleChecks.size(); i++) {
                c.gridx = i % 3;
                c.gridy = i / 3 + 1;
                if (c.gridx == 2) {
                    c.weightx = 1.0;
                } else {
                    c.weightx = 0.0;
                }
                if (i == roleChecks.size() - 1) {
                    c.weighty = 1.0;
                }
                panRoles.add(roleChecks.get(i), c);
            }
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;

            switch (unitType) {
            case UnitType.MEK:
            case UnitType.TANK:
            case UnitType.VTOL:
            case UnitType.NAVAL:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.NoNetwork"),
                        ModelRecord.NETWORK_NONE);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3S"),
                        ModelRecord.NETWORK_C3_SLAVE);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3M"),
                        ModelRecord.NETWORK_C3_MASTER);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3I"),
                        ModelRecord.NETWORK_C3I);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3SB"),
                        ModelRecord.NETWORK_BOOSTED_SLAVE);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3MB"),
                        ModelRecord.NETWORK_BOOSTED_MASTER);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3CC"),
                        ModelRecord.NETWORK_COMPANY_COMMAND);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3CCB"),
                        ModelRecord.NETWORK_COMPANY_COMMAND|ModelRecord.NETWORK_BOOSTED);
                c.weighty = 1.0;
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.Nova"),
                        ModelRecord.NETWORK_NOVA);
                break;
            case UnitType.BATTLE_ARMOR:
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.NoNetwork"),
                        ModelRecord.NETWORK_NONE);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3BA"),
                        ModelRecord.NETWORK_BA_C3);
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3BAB"),
                        ModelRecord.NETWORK_BOOSTED_SLAVE);
                c.weighty = 1.0;
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3I"),
                        ModelRecord.NETWORK_C3I);
                break;
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.WARSHIP:
            case UnitType.SPACE_STATION:
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.NoNetwork"),
                        ModelRecord.NETWORK_NONE);
                c.weighty = 1.0;
                addNetworkButton(panNetwork, c, networkButtons, Messages.getString("RandomArmyDialog.C3N"),
                        ModelRecord.NETWORK_NAVAL_C3);
                break;
            }
            
            switch(unitType) {
            case UnitType.TANK:
                panMotive.add(createSubtypeCheck("hover", true));
                panMotive.add(createSubtypeCheck("tracked", true));
                panMotive.add(createSubtypeCheck("wheeled", true));
                panMotive.add(createSubtypeCheck("wige", true));
                panMotive.add(createSubtypeCheck("vtol", false));
                break;
            case UnitType.INFANTRY:
                panMotive.add(createSubtypeCheck("leg", true));
                panMotive.add(createSubtypeCheck("jump", true));
                panMotive.add(createSubtypeCheck("motorized", true));
                panMotive.add(createSubtypeCheck(Messages.getString("RandomArmyDialog.Mech.hover"),
                        "hover", true));
                panMotive.add(createSubtypeCheck(Messages.getString("RandomArmyDialog.Mech.tracked"),
                        "tracked", true));
                panMotive.add(createSubtypeCheck(Messages.getString("RandomArmyDialog.Mech.wheeled"),
                        "wheeled", true));
                break;
            case UnitType.BATTLE_ARMOR:
                panMotive.add(createSubtypeCheck("leg", true));
                panMotive.add(createSubtypeCheck("jump", true));
                panMotive.add(createSubtypeCheck("umu", true));
                panMotive.add(createSubtypeCheck("vtol", true));
                break;
            case UnitType.NAVAL:
                panMotive.add(createSubtypeCheck("naval", true));
                panMotive.add(createSubtypeCheck("hydrofoil", true));
                panMotive.add(createSubtypeCheck("submarine", true));
                break;
            case UnitType.DROPSHIP:
                panMotive.add(createSubtypeCheck("aerodyne", true));
                panMotive.add(createSubtypeCheck("spheroid", true));
                break;
            }
        }
        
        private void addWeightClasses(JPanel panel, int start, int end, boolean all) {
            cbWeightClass.addItem(Messages.getString("RandomArmyDialog.Mixed"));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
            panel.add(cbWeightClass);
            
            c.gridx = 0;
            c.gridwidth = 2;
            for(int i = start; i <= end; i++) {
                String name = Messages.getString("RandomArmyDialog.weight_class_" + i);
                cbWeightClass.addItem(name);
                JCheckBox chk = new JCheckBox(name);
                chk.setName(String.valueOf(i));
                chk.setSelected(all);
                weightChecks.add(chk);
                c.gridy++;
                if (i == end) {
                    c.weightx = 1.0;
                    c.weighty = 1.0;
                }
                panel.add(chk, c);
            }
            cbWeightClass.addActionListener(e -> {
                for (JCheckBox chk : weightChecks) {
                    chk.setEnabled(cbWeightClass.getSelectedIndex() == 0);
                }
            });
            if (all) {
                cbWeightClass.setSelectedIndex(0);
            } else if (start > EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                cbWeightClass.setSelectedIndex(1);
            } else {
                cbWeightClass.setSelectedIndex(2);
            }
        }
        
        private void addNetworkButton(JPanel panel, GridBagConstraints constraints,
                ButtonGroup group, String text, int mask) {
            JRadioButton btn = new JRadioButton(text);
            btn.setActionCommand(String.valueOf(mask));
            btn.setSelected(mask == ModelRecord.NETWORK_NONE);
            panel.add(btn, constraints);
            group.add(btn);
            constraints.gridy++;
        }
        
        private JCheckBox createSubtypeCheck(String name, boolean select) {
            return createSubtypeCheck(Messages.getString("RandomArmyDialog.Motive." + name),
                    name, select);
        }
        
        private JCheckBox createSubtypeCheck(String text, String name, boolean select) {
            JCheckBox chk = new JCheckBox(text);
            chk.setName(name);
            chk.setSelected(select);
            subtypeChecks.add(chk);
            return chk;
        }
        
        public List<Integer> getSelectedWeights() {
            if (cbWeightClass.getSelectedIndex() > 0) {
                ArrayList<Integer> retVal = new ArrayList<Integer>();
                retVal.add(Integer.parseInt(weightChecks
                        .get(cbWeightClass.getSelectedIndex() - 1).getName()));
                return retVal;
            }
            return weightChecks.stream().filter(chk -> chk.isSelected())
                    .map(chk -> Integer.parseInt(chk.getName())).collect(Collectors.toList());
        }

        public List<MissionRole> getSelectedRoles() {
            return roleChecks.stream().filter(chk -> chk.isSelected())
                    .map(chk -> MissionRole.parseRole(chk.getName()))
                        .filter(role -> role != null).collect(Collectors.toList());
        }
        
        public int getRoleStrictness() {
            return cbRoleStrictness.getSelectedIndex() + 1;
        }
        
        public int getNetworkMask() {
            if (networkButtons.getSelection() != null) {
                return Integer.valueOf(networkButtons.getSelection().getActionCommand());
            }
            return ModelRecord.NETWORK_NONE;
        }
        
        public List<EntityMovementMode> getMotiveTypes() {
            return subtypeChecks.stream().filter(chk -> chk.isSelected())
                    .map(chk -> EntityMovementMode.getMode(chk.getName())).collect(Collectors.toList());
        }
    }
    
    class FormationUnitTypeOptions extends UnitTypeOptions {
        private static final long serialVersionUID = -6448946137013919069L;

        FormationTypesCard groundCard;
        FormationTypesCard airCard;
        
        public FormationUnitTypeOptions() {
            setLayout(new CardLayout());
            
            groundCard = new FormationTypesCard(true);
            airCard = new FormationTypesCard(false);
            add(groundCard, "Ground");
            add(airCard, "Air");
        }
        
        public void optionsChanged() {
            if (getUnitType() != null) {
                ((CardLayout)getLayout()).show(this, getUnitType() < UnitType.CONV_FIGHTER?
                        "Ground" : "Air");
            }
            currentCard().updateUnitType(getUnitType());
        }
        
        private FormationTypesCard currentCard() {
            if (getUnitType() != null && getUnitType() >= UnitType.CONV_FIGHTER) {
                return airCard;
            }
            return groundCard;
        }
        
        @Override
        public Integer getIntegerVal(String key) {
            switch(key) {
            case "numOtherUnits":
                return currentCard().numOtherUnits();
            case "otherUnitType":
                return currentCard().getOtherUnitType();
            case "network":
                return currentCard().getNetwork();
            }
            return null;
        }
        
        @Override
        public Boolean getBooleanVal(String key) {
            switch(key) {
            case "mechBA":
                return currentCard().mechBA();
            case "airLance":
                return currentCard().airLance();
            }
            return null;
        }
        
        @Override
        public String getStringVal(String key) {
            switch(key) {
            case "formationType":
                return currentCard().getFormation();
            }
            return null;
        }
        
        @Override
        public void updateGeneratedUnits(List<MechSummary> list) {
        	currentCard().setGeneratedUnits(list);
        }
    }

    class FormationTypesCard extends JPanel {

        private static final long serialVersionUID = 1439149790457737700L;

        private JRadioButton bSimpleFormation = new JRadioButton(Messages.getString("RandomArmyDialog.simpleFormation"));
        private JRadioButton bMechBA = new JRadioButton(Messages.getString("RandomArmyDialog.mechBA"));
        private JRadioButton bAirLance = new JRadioButton(Messages.getString("RandomArmyDialog.airLance"));
        private JRadioButton bOtherUnitType = new JRadioButton(Messages.getString("RandomArmyDialog.otherUnitType"));
        private JComboBox<String> cbOtherUnitType = new JComboBox<>();
        private JTextField tNumUnits = new JTextField("0");
        private ButtonGroup formationBtnGroup = new ButtonGroup();
        private JComboBox<String> cbNetwork = new JComboBox<>();
        private Map<String,Integer> networkOptions = new LinkedHashMap<>();
        private JTextArea txtNoFormation = new JTextArea();
        private List<MechSummary> generatedUnits = null;
        
        public FormationTypesCard(boolean groundUnit) {
            setLayout(new GridBagLayout());
            
            JPanel panFormations = new JPanel(new GridBagLayout());
            JPanel panOtherOptions = new JPanel(new GridBagLayout());
            
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.5;
            c.weighty = 1.0;
            add(panFormations, c);
            
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.weightx = 0.5;
            c.weighty = 0.0;
            add(panOtherOptions, c);
            
            // Sort main types alphabetically, and subtypes alphabetically within the main.
            List<FormationType> formations = FormationType.getAllFormations().stream()
                    .filter(ft -> ft.isGround() == groundUnit).collect(Collectors.toList());
            Map<String,Set<String>> formationGroups = formations.stream()
                    .collect(Collectors.groupingBy(FormationType::getCategory, TreeMap::new,
                            Collectors.mapping(FormationType::getName,
                                    Collectors.toCollection(TreeSet::new))));
                
            int rows = (formations.size() + 1) / 2;

            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            c.weighty = 1.0;
            
            Insets mainInsets = new Insets(0, 10, 0, 10);
            Insets subInsets = new Insets(0, 30, 0, 10);
            for (String group : formationGroups.keySet()) {
                c.insets = mainInsets;
                if (formationGroups.get(group).contains(group)) {
                    JRadioButton btn = new JRadioButton(FormationType.getFormationType(group).getNameWithFaction());
                    if (formationBtnGroup.getButtonCount() == 0) {
                        btn.setSelected(true);
                    }
                    btn.setActionCommand(group);
                    panFormations.add(btn, c);
                    formationBtnGroup.add(btn);
                    formationGroups.get(group).remove(group);
                } else {
                    JLabel lbl = new JLabel(FormationType.getFormationType(group).getNameWithFaction(),
                            SwingConstants.LEFT);
                    panFormations.add(lbl, c);
                }
                c.gridy++;
                c.insets = subInsets;
                for (String form : formationGroups.get(group)) {
                    JRadioButton btn = new JRadioButton(FormationType.getFormationType(form).getNameWithFaction());
                    if (formationBtnGroup.getButtonCount() == 0) {
                        btn.setSelected(true);
                    }
                    btn.setActionCommand(form);
                    panFormations.add(btn, c);
                    formationBtnGroup.add(btn);
                    c.gridy++;
                }
                if (c.gridy >= rows) {
                    c.gridx++;
                    c.gridy = 0;
                }
            }
            
            ButtonGroup btnGroup = new ButtonGroup();            
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            c.weighty = 0.0;
            panOtherOptions.add(bSimpleFormation, c);
            bSimpleFormation.addItemListener(ev -> tNumUnits.setEnabled(!bSimpleFormation.isSelected()));
            btnGroup.add(bSimpleFormation);
            
            c.gridx = 0;
            c.gridy++;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            c.weighty = 0.0;
            panOtherOptions.add(bMechBA, c);
            btnGroup.add(bMechBA);
            
            c.gridy++;
            c.gridwidth = 2;
            panOtherOptions.add(bAirLance, c);
            btnGroup.add(bAirLance);
            
            c.gridy++;
            c.gridwidth = 2;
            panOtherOptions.add(bOtherUnitType, c);
            btnGroup.add(bOtherUnitType);
            bOtherUnitType.addItemListener(ev -> cbOtherUnitType.setEnabled(bOtherUnitType.isSelected()));
            
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 2;
            panOtherOptions.add(cbOtherUnitType, c);
            cbOtherUnitType.setEnabled(false);
            for (int i = 0; i < UnitType.JUMPSHIP; i++) {
                if (i != UnitType.GUN_EMPLACEMENT) {
                    cbOtherUnitType.addItem(UnitType.getTypeName(i));
                }
            }
            
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = 1;
            panOtherOptions.add(new JLabel(Messages.getString("RandomArmyDialog.additionalUnits")), c);
            c.gridx = 1;
            c.gridwidth = 1;
            panOtherOptions.add(tNumUnits, c);

            bSimpleFormation.setSelected(true);
                        
            if (groundUnit) {
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = 1;
                panOtherOptions.add(new JLabel(Messages.getString("ForceGenerationOptions.Network")), c);

                networkOptions.put(Messages.getString("ForceGenerationOptions.Optional"),
                        ModelRecord.NETWORK_NONE);
                networkOptions.put(Messages.getString("ForceGenerationOptions.C3Lance"),
                        ModelRecord.NETWORK_C3_MASTER);
                networkOptions.put(Messages.getString("ForceGenerationOptions.C3Command"),
                        ModelRecord.NETWORK_C3_MASTER | ModelRecord.NETWORK_COMPANY_COMMAND);
                networkOptions.put(Messages.getString("ForceGenerationOptions.C3i"),
                        ModelRecord.NETWORK_C3I);
                networkOptions.put(Messages.getString("ForceGenerationOptions.C3BLance"),
                        ModelRecord.NETWORK_BOOSTED_MASTER);
                networkOptions.put(Messages.getString("ForceGenerationOptions.C3BCommand"),
                        ModelRecord.NETWORK_BOOSTED_MASTER | ModelRecord.NETWORK_COMPANY_COMMAND);
                networkOptions.put(Messages.getString("ForceGenerationOptions.Nova"),
                        ModelRecord.NETWORK_NOVA);
                networkOptions.keySet().forEach(cbNetwork::addItem);
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                panOtherOptions.add(cbNetwork, c);
            }
            
            c.gridx = 0;
            c.gridy++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            JButton btn = new JButton(Messages.getString("ForceGenerationOptions.formationAnalysis"));
            panOtherOptions.add(btn, c);
            btn.addActionListener(ev -> showAnalysis());
            
            c.gridy++;
            txtNoFormation.setText(Messages.getString("ForceGenerationOptions.noFormation"));
            txtNoFormation.setLineWrap(true);
            txtNoFormation.setWrapStyleWord(true);
            txtNoFormation.setVisible(false);
            panOtherOptions.add(txtNoFormation, c);
        }
        
        public String getFormation() {
            if (formationBtnGroup.getSelection() != null) {
                return formationBtnGroup.getSelection().getActionCommand();
            }
            return null;
        }
        
        public boolean simpleFormation() {
            return bSimpleFormation.isSelected();
        }
        
        public boolean mechBA() {
            return bMechBA.isSelected();
        }
        
        public boolean airLance() {
            return bAirLance.isSelected();
        }
        
        public int numOtherUnits() {
            try {
                return Integer.parseInt(tNumUnits.getText());
            } catch (NumberFormatException ex) {
                tNumUnits.setText("0");
                return 0;
            }
        }
        
        public int getOtherUnitType() {
            if (bOtherUnitType.isSelected() && cbOtherUnitType.getSelectedIndex() > 0) {
                return ModelRecord.parseUnitType((String)cbOtherUnitType.getSelectedItem());
            }
            return -1;
        }
        
        public void updateUnitType(int ut) {
            boolean selectionDisabled = false;
            for (Enumeration<AbstractButton> e = formationBtnGroup.getElements(); e.hasMoreElements();) {
                final AbstractButton btn = e.nextElement();
                FormationType ft = FormationType.getFormationType(btn.getActionCommand());
                if (ft != null) {
                    btn.setEnabled(ft.isAllowedUnitType(ut));
                    if (btn.isSelected() && !btn.isEnabled()) {
                        selectionDisabled = true;
                    }
                }
            }
            if (selectionDisabled) {
                for (Enumeration<AbstractButton> e = formationBtnGroup.getElements(); e.hasMoreElements();) {
                    final AbstractButton btn = e.nextElement();
                    if (btn.isEnabled()) {
                        btn.setSelected(true);
                        return;
                    }
                }
                //We shouldn't reach this point, but if we do the previous selection doesn't change.
            }
        }
        
        public int getNetwork() {
            if (cbNetwork.getSelectedItem() != null) {
                return networkOptions.get(cbNetwork.getSelectedItem());
            }
            return ModelRecord.NETWORK_NONE;
        }
        
        private void showAnalysis() {
            List<UnitTable.Parameters> params = new ArrayList<>();
            FormationType ft = FormationType.getFormationType(getFormation());
            Parameters p = new UnitTable.Parameters(getFaction(),
                    getUnitType(), ratGenYear, (String)cbRating.getSelectedItem(),
                    IntStream.rangeClosed(ft.getMinWeightClass(), ft.getMaxWeightClass())
                    .mapToObj(Integer::valueOf).collect(Collectors.toList()),
                    ModelRecord.NETWORK_NONE, EnumSet.noneOf(EntityMovementMode.class),
                    ft.getMissionRoles(),
                    2, getFaction());
            params.add(p);
            int numUnits = getNumUnits();
            if (getOtherUnitType() >= 0) {
                p = p.copy();
                p.setUnitType(getOtherUnitType());
                params.add(p);
                numUnits += numOtherUnits();
            }
            AnalyzeFormationDialog afd = new AnalyzeFormationDialog(null,
            		generatedUnits,
                    FormationType.getFormationType(getFormation()),
                    params, numUnits, getNetwork());
            afd.setVisible(true);
        }
        
        public void setGeneratedUnits(List<MechSummary> list) {
        	generatedUnits = list;
        	txtNoFormation.setVisible(list == null || list.isEmpty());
        }
    }    
}

