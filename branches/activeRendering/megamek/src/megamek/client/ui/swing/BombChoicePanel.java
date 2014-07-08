/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.Aero;
import megamek.common.BombType;

/**
 * This class loads the default quirks list from the mmconf/defaultQuirks.xml file.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 2012-04-07
 */
public class BombChoicePanel extends JPanel implements Serializable, ItemListener {
    private final Aero aero;
    private final boolean at2Nukes;
    private final boolean allowAdvancedAmmo;

    private static final long serialVersionUID = 483782753790544050L;

    @SuppressWarnings("rawtypes")
    private JComboBox[] b_choices = new JComboBox[BombType.B_NUM];
    private JLabel[] b_labels = new JLabel[BombType.B_NUM];
    private int maxPoints = 0;
    private int maxRows = (int) Math.ceil(BombType.B_NUM / 2.0);

    private BombChoicePanel m_bombs;
    private JPanel panBombs = new JPanel();

    @SuppressWarnings("unchecked")
    public BombChoicePanel(Aero aero, boolean at2Nukes, boolean allowAdvancedAmmo) {
        this.aero = aero;
        this.at2Nukes = at2Nukes;
        this.allowAdvancedAmmo = allowAdvancedAmmo;

        maxPoints = aero.getMaxBombPoints();
        int[] bombChoices = aero.getBombChoices();

        // how many bomb points am I currently using?
        int curBombPoints = 0;
        for (int i = 0; i < bombChoices.length; i++) {
            curBombPoints += bombChoices[i] * BombType.getBombCost(i);
        }
        int availBombPoints = aero.getMaxBombPoints() - curBombPoints;

        GridBagLayout g = new GridBagLayout();
        setLayout(g);
        GridBagConstraints c = new GridBagConstraints();

        int column = 0;
        int row = 0;
        for (int type = 0; type < BombType.B_NUM; type++) {

            b_labels[type] = new JLabel();
            b_choices[type] = new JComboBox<String>();

            for (int x = 0; x <= Math.max(Math.round(availBombPoints
                                                     / BombType.getBombCost(type)), bombChoices[type]); x++) {
                b_choices[type].addItem(Integer.toString(x));
            }

            b_choices[type].setSelectedIndex(bombChoices[type]);
            b_labels[type].setText(BombType.getBombName(type));
            b_choices[type].addItemListener(this);

            if ((type == BombType.B_ALAMO)
                && !at2Nukes) {
                b_choices[type].setEnabled(false);
            }
            if ((type > BombType.B_TAG)
                && !allowAdvancedAmmo) {
                b_choices[type].setEnabled(false);
            }
            if ((type == BombType.B_ASEW) || (type == BombType.B_ALAMO)
                || (type == BombType.B_TAG)) {
                b_choices[type].setEnabled(false);
            }

            if (row >= maxRows) {
                row = 0;
                column += 2;
            }

            c.gridx = column;
            c.gridy = row;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(b_labels[type], c);
            add(b_labels[type]);

            c.gridx = column + 1;
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(b_choices[type], c);
            add(b_choices[type]);
            row++;
        }
    }

    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent ie) {

        int[] current = new int[BombType.B_NUM];
        int curPoints = 0;
        for (int type = 0; type < BombType.B_NUM; type++) {
            current[type] = b_choices[type].getSelectedIndex();
            curPoints += current[type] * BombType.getBombCost(type);
        }

        int availBombPoints = maxPoints - curPoints;

        for (int type = 0; type < BombType.B_NUM; type++) {
            b_choices[type].removeItemListener(this);
            b_choices[type].removeAllItems();
            for (int x = 0; x <= Math.max(Math.round(availBombPoints
                                                     / BombType.getBombCost(type)), current[type]); x++) {
                b_choices[type].addItem(Integer.toString(x));
            }
            b_choices[type].setSelectedIndex(current[type]);
            b_choices[type].addItemListener(this);
        }
    }

    public void applyChoice() {
        int[] choices = new int[BombType.B_NUM];
        for (int type = 0; type < BombType.B_NUM; type++) {
            choices[type] = b_choices[type].getSelectedIndex();
        }

        aero.setBombChoices(choices);

    }

    @Override
    public void setEnabled(boolean enabled) {
        for (int type = 0; type < BombType.B_NUM; type++) {
            if ((type == BombType.B_ALAMO)
                && !at2Nukes) {
                b_choices[type].setEnabled(false);
            } else if ((type > BombType.B_TAG)
                       && !allowAdvancedAmmo) {
                b_choices[type].setEnabled(false);
            } else if ((type == BombType.B_ASEW)
                       || (type == BombType.B_ALAMO)
                       || (type == BombType.B_TAG)) {
                b_choices[type].setEnabled(false);
            } else {
                b_choices[type].setEnabled(enabled);
            }
        }
    }

}
