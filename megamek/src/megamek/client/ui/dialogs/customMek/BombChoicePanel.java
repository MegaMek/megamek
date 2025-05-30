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
package megamek.client.ui.dialogs.customMek;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import megamek.common.BombLoadout;
import megamek.common.BombType;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.IBomber;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2012-04-07
 */
public class BombChoicePanel extends JPanel implements ItemListener {
    private final IBomber bomber;
    private final boolean at2Nukes;
    private final boolean allowAdvancedAmmo;

    private boolean empty = false;

    private static final long serialVersionUID = 483782753790544050L;

    private JPanel interiorPanel;
    private JPanel exteriorPanel;
    private HashMap<String, JComboBox<String>[]> b_choices = new HashMap<String, JComboBox<String>[]>();
    private HashMap<String, JLabel[]> b_labels = new HashMap<String, JLabel[]>();
    private HashMap<String, Integer> maxPoints = new HashMap<String, Integer>();
    private HashMap<String, Integer> maxSize = new HashMap<String, Integer>();
    private int maxRows = (int) Math.ceil(BombTypeEnum.values().length / 2.0);

    //Variable for MekHQ functionality
    private int[] typeMax = null;

    private final String INTNAME = "Internal";
    private final String EXTNAME = "External";

    //private BombChoicePanel m_bombs;
    //private JPanel panBombs = new JPanel();

    public BombChoicePanel(IBomber bomber, boolean at2Nukes, boolean allowAdvancedAmmo) {
        this.bomber = bomber;
        this.at2Nukes = at2Nukes;
        this.allowAdvancedAmmo = allowAdvancedAmmo;

        initArrays();
        initPanel();
    }

    //Constructor to call from MekHQ to pass in typeMax
    public BombChoicePanel(IBomber bomber, boolean at2Nukes, boolean allowAdvancedAmmo, int[] typeMax) {
        this.bomber = bomber;
        this.at2Nukes = at2Nukes;
        this.allowAdvancedAmmo = allowAdvancedAmmo;
        this.typeMax = typeMax;

        initArrays();
        initPanel();
    }

    private void initArrays(){
        // Initialize control arrays
        b_choices.put(INTNAME, new JComboBox[BombTypeEnum.NUM]);
        b_choices.put(EXTNAME, new JComboBox[BombTypeEnum.NUM]);
        b_labels.put(INTNAME, new JLabel[BombTypeEnum.NUM]);
        b_labels.put(EXTNAME, new JLabel[BombTypeEnum.NUM]);
        maxSize.put(INTNAME, 0);
        maxSize.put(EXTNAME, 0);
    }

    private int compileBombPoints(BombLoadout choices) {
        int currentPoints = 0;
        choices.entrySet().forEach(entry -> {
            BombTypeEnum bombType = entry.getKey();
            currentPoints += entry.getValue() * bombType.getCost();
        });
        return currentPoints;
    }

    private void initPanel() {
        maxPoints.put(INTNAME, bomber.getMaxIntBombPoints());
        maxPoints.put(EXTNAME, bomber.getMaxExtBombPoints());

        maxSize.put(INTNAME, bomber.getMaxIntBombSize());
        maxSize.put(EXTNAME, bomber.getMaxExtBombSize());

        BombLoadout intBombChoices = bomber.getIntBombChoices();
        BombLoadout extBombChoices = bomber.getExtBombChoices();

        int columns = (maxPoints.get(INTNAME) > 0 ? 1 : 0) + (maxPoints.get(EXTNAME) > 0 ? 1 : 0);
        // Should not occur!
        if (columns == 0){
            empty = true;
            return;
        }

        JPanel outer = new JPanel();
        outer.setLayout(new GridLayout(0, columns));
        TitledBorder titledBorder = new TitledBorder(new LineBorder(Color.blue), "Bombs");
        Font font2 = new Font("Verdana", Font.BOLD + Font.ITALIC, 12);
        titledBorder.setTitleFont(font2);
        EmptyBorder emptyBorder = new EmptyBorder(10, 10, 10, 10);
        CompoundBorder compoundBorder = new CompoundBorder(titledBorder, emptyBorder);
        outer.setBorder(compoundBorder);

        interiorPanel = initSubPanel(maxPoints.get(INTNAME) - compileBombPoints(intBombChoices), intBombChoices, INTNAME);
        exteriorPanel = initSubPanel(maxPoints.get(EXTNAME) - compileBombPoints(extBombChoices), extBombChoices, EXTNAME);

        if (maxPoints.get(INTNAME) != 0) {
            outer.add(interiorPanel);
        }
        if (maxPoints.get(EXTNAME) != 0) {
            outer.add(exteriorPanel);
        }
        add(outer);
    }

    private JPanel initSubPanel(int availBombPoints, BombTypeEnum[] bombChoices, String title){

        // Set up sub-panel
        JPanel inner = new JPanel();
        TitledBorder titledBorder = new TitledBorder(new LineBorder(Color.blue), title);
        Font font3 = new Font("Verdana", Font.BOLD + Font.ITALIC, 10);
        titledBorder.setTitleFont(font3);
        EmptyBorder emptyBorder = new EmptyBorder(10, 10, 10, 10);
        CompoundBorder compoundBorder = new CompoundBorder(titledBorder, emptyBorder);
        inner.setBorder(compoundBorder);

        GridBagLayout g = new GridBagLayout();
        inner.setLayout(g);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        int column = 0;
        int row = 0;
        for (BombTypeEnum type : BombTypeEnum.values()) {
            b_labels.get(title)[type] = new JLabel();
            b_choices.get(title)[type] = new JComboBox<String>();

            int maxNumBombs = Math.round(availBombPoints / type.getCost()) + bombChoices[type];

            if (type.getCost() > maxSize.get(title))  {
                maxNumBombs = 0;
            }

            // somehow too many bombs were added
            if ((bombChoices[type] * type.getCost())  > maxSize.get(title)) {
                bombChoices[type] = maxSize.get(title) / type.getCost();
            }

            if (typeMax != null) {
                if ((maxNumBombs > 0) && (maxNumBombs > typeMax[type])) {
                    maxNumBombs = typeMax[type];
                }
            }

            if (bombChoices[type] > maxNumBombs) {
                maxNumBombs = bombChoices[type];
            }

            if (maxNumBombs < 0) {
                maxNumBombs = 0;
            }

            if (maxNumBombs > maxSize.get(title)) {
                maxNumBombs = maxSize.get(title);
            }

            for (int x = 0; x <= maxNumBombs; x++) {
                b_choices.get(title)[type].addItem(Integer.toString(x));
            }

            b_choices.get(title)[type].setSelectedIndex(bombChoices[type]);
            b_labels.get(title)[type].setText(type.getDisplayName());
            b_choices.get(title)[type].addItemListener(this);

            if ((type == BombTypeEnum.ALAMO) && !at2Nukes) {
                b_choices.get(title)[type].setEnabled(false);
            }
            if ((type > BombTypeEnum.TAG) && !allowAdvancedAmmo) {
                b_choices.get(title)[type].setEnabled(false);
            }

            if (row >= maxRows) {
                row = 0;
                column += 2;
            }

            c.gridx = column;
            c.gridy = row;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(b_labels.get(title)[type], c);
            inner.add(b_labels.get(title)[type]);

            c.gridx = column + 1;
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(b_choices.get(title)[type], c);
            inner.add(b_choices.get(title)[type]);
            row++;
        }
        return inner;
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {

        for (String title: new String[]{INTNAME, EXTNAME}){
            int[] current = new int[BombTypeEnum.NUM];
            int curPoints = 0;
            for (BombTypeEnum type : BombTypeEnum.values()) {
                if (type == BombTypeEnum.NONE) continue;
                current[type] = b_choices.get(title)[type].getSelectedIndex();
                curPoints += current[type] * type.getCost();
            }

            int availBombPoints = maxPoints.get(title) - curPoints;

            for (BombTypeEnum type : BombTypeEnum.values()) {
                if (type == BombTypeEnum.NONE) continue;
                b_choices.get(title)[type].removeItemListener(this);
                b_choices.get(title)[type].removeAllItems();
                int maxNumBombs = Math.round(availBombPoints / type.getCost()) + current[type];

                if (typeMax != null) {
                    if ((maxNumBombs > 0) && (maxNumBombs > typeMax[type])) {
                        maxNumBombs = typeMax[type];
                    }
                }

                if (current[type] > maxNumBombs) {
                    maxNumBombs = current[type];
                }

                if (maxNumBombs < 0) {
                    maxNumBombs = 0;
                }

                if (maxNumBombs > maxSize.get(title)) {
                    maxNumBombs = maxSize.get(title);
                }


                for (int x = 0; x <= maxNumBombs; x++) {
                    b_choices.get(title)[type].addItem(Integer.toString(x));
                }
                b_choices.get(title)[type].setSelectedIndex(current[type]);
                b_choices.get(title)[type].addItemListener(this);
            }
        }
    }

    public void applyChoice() {
        // Return cleanly if bomber never had any capacity but e.g. Internal Bomb Bay tried add bomb capacity.
        if (empty) {
            return;
        }

        BombLoadout choices = new BombLoadout();
        // Internal bombs
        for (int type = 0; type < BombTypeEnum.NUM; type++) {
            choices[type] = b_choices.get(INTNAME)[type].getSelectedIndex();
        }
        bomber.setIntBombChoices(choices);
        // External bombs
        for (int type = 0; type < BombTypeEnum.NUM; type++) {
            choices[type] = b_choices.get(EXTNAME)[type].getSelectedIndex();
        }
        bomber.setExtBombChoices(choices);
    }
    public BombLoadout getChoice() {
        BombLoadout choices = new BombLoadout();
        if (empty) {
            return choices;
        }

        for (int type = 0; type < BombTypeEnum.NUM; type++) {
            choices[type] += b_choices.get(INTNAME)[type].getSelectedIndex() + b_choices.get(EXTNAME)[type].getSelectedIndex();
        }
        return choices;
    }

    @Override
    public void setEnabled(boolean enabled) {
        for (String title : new String[]{INTNAME, EXTNAME}) {
            for (int type = 0; type < BombTypeEnum.NUM; type++) {
                if ((type == BombTypeEnum.ALAMO)
                        && !at2Nukes) {
                    b_choices.get(title)[type].setEnabled(false);
                } else if ((type > BombTypeEnum.TAG)
                        && !allowAdvancedAmmo) {
                    b_choices.get(title)[type].setEnabled(false);
                } else if ((type == BombTypeEnum.ASEW)
                        || (type == BombTypeEnum.ALAMO)
                        || (type == BombTypeEnum.TAG)) {
                    b_choices.get(title)[type].setEnabled(false);
                } else {
                    b_choices.get(title)[type].setEnabled(enabled);
                }
            }
        }
    }

}
