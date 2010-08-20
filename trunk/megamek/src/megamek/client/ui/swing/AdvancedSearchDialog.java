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
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.LinkedHashSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;

public class AdvancedSearchDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -2459992981678758743L;
    private MechSelectorDialog parent;

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
    public AdvancedSearchDialog(MechSelectorDialog msd, ClientGUI clientgui) {
        super(clientgui.frame,
                Messages.getString("AdvancedSearchDialog.title"), true); //$NON-NLS-1$
        parent = msd;

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

        //layout
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
        setLocation(clientgui.frame.getLocation().x
                + clientgui.frame.getSize().width / 2 - getSize().width / 2,
                clientgui.frame.getLocation().y
                        + clientgui.frame.getSize().height / 2
                        - getSize().height / 2);
    }

    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource() == butOkay) {
            parent.filterUnits();
            parent.enableResetButton(true);
            setVisible(false);
        }
        if (ev.getSource() == butCancel) {
            setVisible(false);
        }
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
        parent.enableResetButton(false);
    }

    private void populateWeaponsAndEquipmentChoices() {
        LinkedHashSet<String> weapons = new LinkedHashSet<String>();
        LinkedHashSet<String> equipment = new LinkedHashSet<String>();
        /*
         * I am not going to filter by type and unit type because that may change after the advanced filter is set up
         */
        int nType = parent.getType();
        int nUnitType = parent.getUnitType();
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e
                .hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if ((et instanceof WeaponType)) {
                   /* && ((et.getTechLevel() == nType)
                            || ((nType == TechConstants.T_TW_ALL) && ((et
                                    .getTechLevel() == TechConstants.T_INTRO_BOXSET)
                                    || (et.getTechLevel() == TechConstants.T_IS_TW_NON_BOX) || (et
                                    .getTechLevel() == TechConstants.T_CLAN_TW))) || (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_TW_NON_BOX)) && ((et
                            .getTechLevel() == TechConstants.T_INTRO_BOXSET) || (et
                            .getTechLevel() == TechConstants.T_IS_TW_NON_BOX))))) {
                if (!(nUnitType == UnitType.SIZE)
                        && ((UnitType.getTypeName(nUnitType).equals("Mek") || UnitType
                                .getTypeName(nUnitType).equals("Tank")) && (et
                                .hasFlag(WeaponType.F_INFANTRY)))) {
                    continue;
                }*/
                weapons.add(et.getName());
                if (et.hasFlag(WeaponType.F_C3M)) {
                      /*  && ((nType == TechConstants.T_TW_ALL)
                                || (nType == TechConstants.T_IS_TW_NON_BOX) || (nType == TechConstants.T_IS_TW_ALL))) {*/
                    equipment.add(et.getName());
                }
            }
            if ((et instanceof MiscType)) {
                  /*  && ((et.getTechLevel() == nType)
                            || ((nType == TechConstants.T_TW_ALL) && ((et
                                    .getTechLevel() == TechConstants.T_INTRO_BOXSET)
                                    || (et.getTechLevel() == TechConstants.T_IS_TW_NON_BOX) || (et
                                    .getTechLevel() == TechConstants.T_CLAN_TW))) || (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_TW_NON_BOX)) && ((et
                            .getTechLevel() == TechConstants.T_INTRO_BOXSET) || (et
                            .getTechLevel() == TechConstants.T_IS_TW_NON_BOX))))) {*/
                equipment.add(et.getName());
            }
        }
        for (String weaponName : weapons) {
            cWeapons1.addItem(weaponName);
            cWeapons2.addItem(weaponName);
        }
        for (String equipName : equipment) {
            cEquipment.addItem(equipName);
        }
        //cWeapons1.invalidate();
        //cWeapons2.invalidate();
        //cEquipment.invalidate();
        //pack();
    }

    public boolean isAdvancedSearchOff() {

       // return true;
       return tWalk.getText().equals("") && tJump.getText().equals("")
            && tWeapons1.getText().equals("") && tWeapons2.getText().equals("")
                && tStartYear.getText().equals("") && tEndYear.getText().equals("")
                    && !chkEquipment.isSelected() && cArmor.getSelectedIndex() == 0;

    }

    public boolean isMatch(MechSummary mech) {

        if(isAdvancedSearchOff()) {
            return true;
        }

        try {
            Entity entity = new MechFileParser(mech.getSourceFile(), mech.getEntryName()).getEntity();

            int walk = -1;
            try {
                walk = Integer.parseInt(tWalk.getText());
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (walk > -1) {
                if (cWalk.getSelectedIndex() == 0) { // at least
                    if (entity.getWalkMP() < walk) {
                        return false;
                    }
                } else if (cWalk.getSelectedIndex() == 1) { // equal to
                    if (walk != entity.getWalkMP()) {
                        return false;
                    }
                } else if (cWalk.getSelectedIndex() == 2) { // not more than
                    if (entity.getWalkMP() > walk) {
                        return false;
                    }
                }
            }

            int jump = -1;
            try {
                jump = Integer.parseInt(tJump.getText());
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (jump > -1) {
                if (cJump.getSelectedIndex() == 0) { // at least
                    if (entity.getJumpMP() < jump) {
                        return false;
                    }
                } else if (cJump.getSelectedIndex() == 1) { // equal to
                    if (jump != entity.getJumpMP()) {
                        return false;
                    }
                } else if (cJump.getSelectedIndex() == 2) { // not more than
                    if (entity.getJumpMP() > jump) {
                        return false;
                    }
                }
            }

            int sel = cArmor.getSelectedIndex();
            if (sel > 0) {
                int armor = entity.getTotalArmor();
                int maxArmor = entity.getTotalInternal() * 2 + 3;
                if (sel == 1) {
                    if (armor < (maxArmor * .25)) {
                        return false;
                    }
                } else if (sel == 2) {
                    if (armor < (maxArmor * .5)) {
                        return false;
                    }
                } else if (sel == 3) {
                    if (armor < (maxArmor * .75)) {
                        return false;
                    }
                } else if (sel == 4) {
                    if (armor < (maxArmor * .9)) {
                        return false;
                    }
                }
            }

            boolean weaponLine1Active = false;
            boolean weaponLine2Active = false;
            boolean foundWeapon1 = false;
            boolean foundWeapon2 = false;

            int count = 0;
            int weapon1 = -1;
            try {
                weapon1 = Integer.parseInt(tWeapons1.getText());
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (weapon1 > -1) {
                weaponLine1Active = true;
                for (int i = 0; i < entity.getWeaponList().size(); i++) {
                    WeaponType wt = (WeaponType) (entity.getWeaponList().get(i))
                            .getType();
                    if (wt.getName().equals(cWeapons1.getSelectedItem())) {
                        count++;
                    }
                }
                if (count >= weapon1) {
                    foundWeapon1 = true;
                }
            }

            count = 0;
            int weapon2 = -1;
            try {
                weapon2 = Integer.parseInt(tWeapons2.getText());
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (weapon2 > -1) {
                weaponLine2Active = true;
                for (int i = 0; i < entity.getWeaponList().size(); i++) {
                    WeaponType wt = (WeaponType) (entity.getWeaponList().get(i))
                            .getType();
                    if (wt.getName().equals(cWeapons2.getSelectedItem())) {
                        count++;
                    }
                }
                if (count >= weapon2) {
                    foundWeapon2 = true;
                }
            }

            int startYear = Integer.MIN_VALUE;
            int endYear = Integer.MAX_VALUE;
            try {
                startYear = Integer.parseInt(tStartYear.getText());
            } catch (NumberFormatException ne) {
                //ignore
            }
            try {
                endYear = Integer.parseInt(tEndYear.getText());
            } catch (NumberFormatException ne) {
                //ignore
            }
            if ((entity.getYear() < startYear) || (entity.getYear() > endYear)) {
                return false;
            }

            if (weaponLine1Active && !weaponLine2Active && !foundWeapon1) {
                return false;
            }
            if (weaponLine2Active && !weaponLine1Active && !foundWeapon2) {
                return false;
            }
            if (weaponLine1Active && weaponLine2Active) {
                if (cOrAnd.getSelectedIndex() == 0 /* 0 is "or" choice */) {
                    if (!foundWeapon1 && !foundWeapon2) {
                        return false;
                    }
                } else { // "and" choice in effect
                    if (!foundWeapon1 || !foundWeapon2) {
                        return false;
                    }
                }
            }

            count = 0;
            if (chkEquipment.isSelected()) {
                for (Mounted m : entity.getEquipment()) {
                    EquipmentType mt = m.getType();
                    if (mt.getName().equals(cEquipment.getSelectedItem())) {
                        count++;
                    }
                }
                if (count < 1) {
                    return false;
                }
            }
        } catch (EntityLoadingException ex) {
            //shouldn't happen
            return false;
        }

        return true;
    }

}
