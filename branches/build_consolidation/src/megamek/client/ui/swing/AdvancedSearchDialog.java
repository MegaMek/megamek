/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
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

/**
 * Advanced search functions for the mech selector dialog
 *
 * @author  Jay Lawson
 */
package megamek.client.ui.swing;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import megamek.client.ui.Messages;
import megamek.common.EquipmentType;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.MiscType;
import megamek.common.WeaponType;

public class AdvancedSearchDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -2459992981678758743L;
    public MechSearchFilter mechFilter = null;
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JLabel lblWalk = new JLabel(Messages.getString("MechSelectorDialog.Search.Walk"));
    private JComboBox cWalk = new JComboBox();
    private JTextField tWalk = new JTextField(2);
    private JLabel lblJump = new JLabel(Messages.getString("MechSelectorDialog.Search.Jump"));
    private JComboBox cJump = new JComboBox();
    private JTextField tJump = new JTextField(2);
    private JLabel lblArmor = new JLabel(Messages.getString("MechSelectorDialog.Search.Armor"));
    private JComboBox cArmor = new JComboBox();
    private JLabel lblWeapons = new JLabel(Messages.getString("MechSelectorDialog.Search.Weapons"));
    private JLabel lblWeapons1 = new JLabel(Messages.getString("MechSelectorDialog.Search.WeaponsAtLeast"));
    private JTextField tWeapons1 = new JTextField(2);
    private JComboBox cWeapons1 = new JComboBox();
    private JComboBox cOrAnd = new JComboBox();
    private JLabel lblWeapons2 = new JLabel(Messages.getString("MechSelectorDialog.Search.WeaponsAtLeast"));
    private JTextField tWeapons2 = new JTextField(2);
    private JComboBox cWeapons2 = new JComboBox();
    private JLabel lblEquipment = new JLabel(Messages.getString("MechSelectorDialog.Search.Equipment"));
    private JCheckBox chkEquipment = new JCheckBox();
    private JComboBox cEquipment = new JComboBox();
    private JLabel lblYear = new JLabel(Messages.getString("MechSelectorDialog.Search.Year"));
    private JTextField tStartYear = new JTextField(4);
    private JTextField tEndYear = new JTextField(4);

    /** Creates a new instance of AdvancedSearchDialog */
    public AdvancedSearchDialog(Frame frame) {
        super(frame, Messages.getString("AdvancedSearchDialog.title"), true); //$NON-NLS-1$

        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        cWalk.addItem(Messages.getString("MechSelectorDialog.Search.AtLeast"));
        cWalk.addItem(Messages.getString("MechSelectorDialog.Search.EqualTo"));
        cWalk.addItem(Messages.getString("MechSelectorDialog.Search.NoMoreThan"));

        cJump.addItem(Messages.getString("MechSelectorDialog.Search.AtLeast"));
        cJump.addItem(Messages.getString("MechSelectorDialog.Search.EqualTo"));
        cJump.addItem(Messages.getString("MechSelectorDialog.Search.NoMoreThan"));

        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor25"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor50"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor75"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor90"));

        cOrAnd.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        cOrAnd.addItem(Messages.getString("MechSelectorDialog.Search.and"));

        populateWeaponsAndEquipmentChoices();

        //cWalk.setSelectedIndex(0);
        tWalk.setText("");
        //cJump.setSelectedIndex(0);
        tJump.setText("");
        //cArmor.setSelectedIndex(0);
        tWeapons1.setText("");
        //cWeapons1.setSelectedIndex(0);
        //cOrAnd.setSelectedIndex(0);
        tWeapons2.setText("");
        //cWeapons2.setSelectedIndex(0);
        chkEquipment.setSelected(false);
        //cEquipment.setSelectedIndex(0);

        // layout
        setLayout(new GridLayout(10, 1));

        Panel row1 = new Panel();
        row1.setLayout(new FlowLayout(FlowLayout.LEFT));
        row1.add(lblWalk);
        row1.add(cWalk);
        row1.add(tWalk);
        this.add(row1);

        Panel row2 = new Panel();
        row2.setLayout(new FlowLayout(FlowLayout.LEFT));
        row2.add(lblJump);
        row2.add(cJump);
        row2.add(tJump);
        this.add(row2);

        Panel row3 = new Panel();
        row3.setLayout(new FlowLayout(FlowLayout.LEFT));
        row3.add(lblArmor);
        row3.add(cArmor);
        this.add(row3);

        Panel row4 = new Panel();
        row4.setLayout(new FlowLayout(FlowLayout.LEFT));
        row4.add(lblWeapons);
        this.add(row4);

        Panel row5 = new Panel();
        row5.setLayout(new FlowLayout(FlowLayout.LEFT));
        row5.add(lblWeapons1);
        row5.add(tWeapons1);
        row5.add(cWeapons1);
        this.add(row5);

        Panel row6 = new Panel();
        row6.setLayout(new FlowLayout(FlowLayout.LEFT));
        row6.add(cOrAnd);
        this.add(row6);

        Panel row7 = new Panel();
        row7.setLayout(new FlowLayout(FlowLayout.LEFT));
        row7.add(lblWeapons2);
        row7.add(tWeapons2);
        row7.add(cWeapons2);
        this.add(row7);

        Panel row8 = new Panel();
        row8.setLayout(new FlowLayout(FlowLayout.LEFT));
        row8.add(lblEquipment);
        row8.add(chkEquipment);
        row8.add(cEquipment);
        this.add(row8);

        Panel row9 = new Panel();
        row9.setLayout(new FlowLayout(FlowLayout.LEFT));
        row9.add(lblYear);
        row9.add(tStartYear);
        row9.add(new Label("-"));
        row9.add(tEndYear);
        this.add(row9);

        Panel row10 = new Panel();
        row10.setLayout(new FlowLayout(FlowLayout.CENTER));
        row10.add(butOkay);
        row10.add(butCancel);
        this.add(row10);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation((frame.getLocation().x + (frame.getSize().width / 2)) - (getSize().width / 2), (frame.getLocation().y + (frame.getSize().height / 2)) - (getSize().height / 2));
    }

    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource().equals(butOkay)) {
            mechFilter = getMechSearchFilter();
            setVisible(false);
        }
        if (ev.getSource().equals(butCancel)) {
            mechFilter = null;
            setVisible(false);
        }
    }

    public MechSearchFilter showDialog() {
        setVisible(true);
        return getMechSearchFilter();
    }

    public void clearValues() {
        cWalk.setSelectedIndex(0);
        tWalk.setText("");
        cJump.setSelectedIndex(0);
        tJump.setText("");
        cArmor.setSelectedIndex(0);
        tWeapons1.setText("");
        cWeapons1.setSelectedIndex(0);
        cOrAnd.setSelectedIndex(0);
        tWeapons2.setText("");
        cWeapons2.setSelectedIndex(0);
        chkEquipment.setSelected(false);
        cEquipment.setSelectedIndex(0);
    }

    private void populateWeaponsAndEquipmentChoices() {
        List<String> weapons = new ArrayList<String>();
        List<String> equipment = new ArrayList<String>();
        /*
         * I am not going to filter by type and unit type because that may
         * change after the advanced filter is set up
         */
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if ((et instanceof WeaponType)) {
                /*
                 * && ((et.getTechLevel() == nType) || ((nType ==
                 * TechConstants.T_TW_ALL) && ((et .getTechLevel() ==
                 * TechConstants.T_INTRO_BOXSET) || (et.getTechLevel() ==
                 * TechConstants.T_IS_TW_NON_BOX) || (et .getTechLevel() ==
                 * TechConstants.T_CLAN_TW))) || (((nType ==
                 * TechConstants.T_IS_TW_ALL) || (nType ==
                 * TechConstants.T_IS_TW_NON_BOX)) && ((et .getTechLevel() ==
                 * TechConstants.T_INTRO_BOXSET) || (et .getTechLevel() ==
                 * TechConstants.T_IS_TW_NON_BOX))))) { if (!(nUnitType ==
                 * UnitType.SIZE) &&
                 * ((UnitType.getTypeName(nUnitType).equals("Mek") || UnitType
                 * .getTypeName(nUnitType).equals("Tank")) && (et
                 * .hasFlag(WeaponType.F_INFANTRY)))) { continue; }
                 */
                weapons.add(et.getName());
                if (et.hasFlag(WeaponType.F_C3M) || et.hasFlag(WeaponType.F_C3MBS)) {
                    /*
                     * && ((nType == TechConstants.T_TW_ALL) || (nType ==
                     * TechConstants.T_IS_TW_NON_BOX) || (nType ==
                     * TechConstants.T_IS_TW_ALL))) {
                     */
                    equipment.add(et.getName());
                }
            }
            if ((et instanceof MiscType)) {
                /*
                 * && ((et.getTechLevel() == nType) || ((nType ==
                 * TechConstants.T_TW_ALL) && ((et .getTechLevel() ==
                 * TechConstants.T_INTRO_BOXSET) || (et.getTechLevel() ==
                 * TechConstants.T_IS_TW_NON_BOX) || (et .getTechLevel() ==
                 * TechConstants.T_CLAN_TW))) || (((nType ==
                 * TechConstants.T_IS_TW_ALL) || (nType ==
                 * TechConstants.T_IS_TW_NON_BOX)) && ((et .getTechLevel() ==
                 * TechConstants.T_INTRO_BOXSET) || (et .getTechLevel() ==
                 * TechConstants.T_IS_TW_NON_BOX))))) {
                 */
                equipment.add(et.getName());
            }
        }
        Collections.sort(weapons);
        Collections.sort(equipment);
        for (String weaponName : weapons) {
            cWeapons1.addItem(weaponName);
            cWeapons2.addItem(weaponName);
        }
        for (String equipName : equipment) {
            cEquipment.addItem(equipName);
        }
        // cWeapons1.invalidate();
        // cWeapons2.invalidate();
        // cEquipment.invalidate();
        // pack();
    }

    public boolean isAdvancedSearchOff() {

        // return true;
        return tWalk.getText().equals("") && tJump.getText().equals("") && tWeapons1.getText().equals("") && tWeapons2.getText().equals("") && tStartYear.getText().equals("") && tEndYear.getText().equals("") && !chkEquipment.isSelected() && (cArmor.getSelectedIndex() == 0);

    }

    protected megamek.common.MechSearchFilter getMechSearchFilter() {
        MechSearchFilter ret = new MechSearchFilter();

        ret.sWalk = tWalk.getText();
        ret.iWalk = cWalk.getSelectedIndex();

        ret.sJump = tJump.getText();
        ret.iJump = cJump.getSelectedIndex();

        ret.iArmor = cArmor.getSelectedIndex();

        ret.sWep1Count = tWeapons1.getText();
        ret.sWep2Count = tWeapons2.getText();
        ret.oWep1 = cWeapons1.getSelectedItem();
        ret.oWep2 = cWeapons2.getSelectedItem();

        ret.sStartYear = tStartYear.getText();
        ret.sEndYear = tEndYear.getText();

        ret.iWepAndOr = cOrAnd.getSelectedIndex();
        ret.bCheckEquipment = chkEquipment.isSelected();
        ret.oEquipment = cEquipment.getSelectedItem();
        return ret;
    }

    public boolean isMatch(MechSummary mech) {
        if (isAdvancedSearchOff()) {
            return true;
        } else {
            return MechSearchFilter.isMatch(mech, getMechSearchFilter());
        }
    }

}
