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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.client.ratgenerator.AbstractUnitRecord;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ui.Messages;
import megamek.common.UnitType;

/**
 *
 * Panel that allows choice of year, faction, rating, unit type
 *
 * @author Neoancient
 */
class ForceGenerationOptionsPanel extends JPanel implements ActionListener, FocusListener {

    private static final long serialVersionUID = -3462304612643343012L;

    private JTextField m_tRGUnits = new JTextField(3);
    private JTextField m_tYear = new JTextField(4);
    private JComboBox<FactionRecord> m_chFaction = new JComboBox<FactionRecord>();
    private JComboBox<FactionRecord> m_chSubfaction = new JComboBox<FactionRecord>();
    private JCheckBox m_chkShowMinor = new JCheckBox(Messages
            .getString("RandomArmyDialog.ShowMinorFactions"));
    private JComboBox<String> m_chUnitType = new JComboBox<String>();
    private JComboBox<String> m_chRating = new JComboBox<String>();

    private JPanel unitTypePanelContainer;
    private Map<String,JPanel> unitTypeCardMap;
    private boolean onlyAirGround = false;

    private int ratGenYear;

    public ForceGenerationOptionsPanel() {
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

        m_tYear.setText(String.valueOf(ratGenYear));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(m_tYear, c);
        m_tYear.setText(String.valueOf(ratGenYear));
        m_tYear.addFocusListener(this);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(new JLabel(Messages.getString("RandomArmyDialog.Unit")), c);

        m_tRGUnits.setText("4");

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(m_tRGUnits, c);

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
        add(m_chFaction, c);
        m_chFaction.setRenderer(factionCbRenderer);
        m_chFaction.addActionListener(this);

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
        add(m_chSubfaction, c);
        m_chSubfaction.setRenderer(factionCbRenderer);
        m_chSubfaction.addActionListener(this);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        add(m_chkShowMinor, c);
        m_chkShowMinor.addActionListener(this);

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
        add(m_chUnitType, c);
        m_chUnitType.addActionListener(this);

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
        add(m_chRating, c);
    }

    public void setUnitTypePanelContainer(JPanel panel, Map<String, JPanel> cardMap,
            boolean onlyAirGround) {
        unitTypePanelContainer = panel;
        unitTypeCardMap = cardMap;
        this.onlyAirGround = onlyAirGround;
    }

    public JPanel getUnitTypePanel(String unitType) {
        return unitTypeCardMap.get(unitType);
    }

    public int getNumUnits() {
        return Integer.parseInt(m_tRGUnits.getText());
    }

    public int getYear() {
        return Integer.parseInt(m_tYear.getText());
    }

    public FactionRecord getFaction() {
        if (m_chSubfaction.getSelectedItem() == null) {
            return (FactionRecord)m_chFaction.getSelectedItem();
        } else {
            return (FactionRecord)m_chSubfaction.getSelectedItem();
        }
    }

    public String getUnitType() {
        return (String)m_chUnitType.getSelectedItem();
    }

    public String getRating() {
        return (String)m_chRating.getSelectedItem();
    }

    public void setUnitTypes(List<Integer> list) {
        m_chUnitType.removeActionListener(this);
        m_chUnitType.removeAllItems();
        list.forEach(ut -> m_chUnitType.addItem(UnitType.getTypeName(ut)));
        m_chUnitType.addActionListener(this);
        m_chUnitType.setSelectedIndex(0);
    }

    public void setYear(int year) {
        m_tYear.setText(String.valueOf(year));
        updateFactionChoice();
    }

    public void updateFactionChoice() {
        FactionRecord old = (FactionRecord)m_chFaction.getSelectedItem();
        m_chFaction.removeActionListener(this);
        m_chFaction.removeAllItems();
        ArrayList<FactionRecord> recs = new ArrayList<>();
        for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
            if ((!fRec.isMinor() || m_chkShowMinor.isSelected())
                    && !fRec.getKey().contains(".") && fRec.isActiveInYear(ratGenYear)) {
                recs.add(fRec);
            }
        }
        Collections.sort(recs, factionSorter);
        for (FactionRecord fRec : recs) {
            m_chFaction.addItem(fRec);
        }
        m_chFaction.setSelectedItem(old);
        if (m_chFaction.getSelectedItem() == null) {
            m_chFaction.setSelectedItem(RATGenerator.getInstance().getFaction("IS"));
        }
        updateSubfactionChoice();
        m_chFaction.addActionListener(this);
    }

    public void updateSubfactionChoice() {
        FactionRecord old = (FactionRecord)m_chSubfaction.getSelectedItem();
        m_chSubfaction.removeActionListener(this);
        m_chSubfaction.removeAllItems();
        FactionRecord selectedFaction = (FactionRecord)m_chFaction.getSelectedItem();
        if (selectedFaction != null) {
            ArrayList<FactionRecord> recs = new ArrayList<>();
            for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
                if (fRec.getKey().startsWith(selectedFaction.getKey() + ".")
                        && fRec.isActiveInYear(ratGenYear)) {
                    recs.add(fRec);
                }
            }
            Collections.sort(recs, factionSorter);
            m_chSubfaction.addItem(null); //No specific subcommand.
            for (FactionRecord fRec : recs) {
                m_chSubfaction.addItem(fRec);
            }
        }
        m_chSubfaction.setSelectedItem(old);
        updateRatingChoice();
        m_chSubfaction.addActionListener(this);
    }

    /**
     * When faction or subfaction is changed, refresh ratings combo box with appropriate
     * values for selected faction.
     *
     */

    public void updateRatingChoice() {
        int current = m_chRating.getSelectedIndex();
        m_chRating.removeAllItems();
        FactionRecord fRec = (FactionRecord)m_chSubfaction.getSelectedItem();
        if (fRec == null) {
            // Subfaction is "general"
            fRec = (FactionRecord)m_chFaction.getSelectedItem();
        }
        ArrayList<String> ratingLevels = fRec.getRatingLevels();
        if (ratingLevels.isEmpty()) {
            // Get rating levels from parent faction(s)
            ratingLevels = fRec.getRatingLevelSystem();
        }
        if (ratingLevels.size() > 1) {
            for (int i = ratingLevels.size() - 1; i >= 0; i--) {
                m_chRating.addItem(ratingLevels.get(i));
            }
        }
        if (current < 0 && m_chRating.getItemCount() > 0) {
            m_chRating.setSelectedIndex(0);
        } else {
            m_chRating.setSelectedIndex(Math.min(current, m_chRating.getItemCount() - 1));
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(m_chFaction)) {
            updateSubfactionChoice();
        } else if (ev.getSource().equals(m_chSubfaction)) {
            updateRatingChoice();
        } else if (ev.getSource().equals(m_chkShowMinor)) {
            updateFactionChoice();
        } else if (ev.getSource().equals(m_chUnitType)) {
            if (unitTypePanelContainer != null) {
                CardLayout layout = (CardLayout)unitTypePanelContainer.getLayout();
                //FIXME: this is a rough hack
                if (onlyAirGround) {
                    layout.show(unitTypePanelContainer,
                            AbstractUnitRecord.parseUnitType((String)m_chUnitType.getSelectedItem()) < UnitType.CONV_FIGHTER? "ground" : "air");
                } else {
                    layout.show(unitTypePanelContainer, (String)m_chUnitType.getSelectedItem());
                }
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        //ignored
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (e.getSource().equals(m_tYear)) {
            try {
                ratGenYear = Integer.parseInt(m_tYear.getText());
                if (ratGenYear < RATGenerator.getInstance().getEraSet().first()) {
                    ratGenYear = RATGenerator.getInstance().getEraSet().first();
                } else if (ratGenYear > RATGenerator.getInstance().getEraSet().last()) {
                    ratGenYear = RATGenerator.getInstance().getEraSet().last();
                }
            } catch (NumberFormatException ex) {
                //ignore and restore to previous value
            }
            setYear(ratGenYear);
            RATGenerator.getInstance().loadYear(ratGenYear);
        }
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
}

