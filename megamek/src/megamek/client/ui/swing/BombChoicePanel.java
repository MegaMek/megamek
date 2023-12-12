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

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import megamek.common.BombType;
import megamek.common.IBomber;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2012-04-07
 */
public class BombChoicePanel extends JPanel implements Serializable, ItemListener {
    private final IBomber bomber;
    private final boolean at2Nukes;
    private final boolean allowAdvancedAmmo;

    private static final long serialVersionUID = 483782753790544050L;

    @SuppressWarnings("rawtypes")
    private JPanel interiorPanel;
    private JPanel exteriorPanel;
    private HashMap<String, JComboBox[]> b_choices = new HashMap<String, JComboBox[]>();
    private HashMap<String, JLabel[]> b_labels = new HashMap<String, JLabel[]>();
    private HashMap<String, Integer> maxPoints = new HashMap<String, Integer>();
    private HashMap<String, Integer> maxSize = new HashMap<String, Integer>();
    private int maxRows = (int) Math.ceil(BombType.B_NUM / 2.0);

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
        b_choices.put(INTNAME, new JComboBox[BombType.B_NUM]);
        b_choices.put(EXTNAME, new JComboBox[BombType.B_NUM]);
        b_labels.put(INTNAME, new JLabel[BombType.B_NUM]);
        b_labels.put(EXTNAME, new JLabel[BombType.B_NUM]);
        maxSize.put(INTNAME, 0);
        maxSize.put(EXTNAME, 0);
    }

    private int compileBombPoints(int[] choices) {
        int currentPoints = 0;
        for (int i = 0; i < choices.length; i++) {
            currentPoints += choices[i] * BombType.getBombCost(i);
        }
        return currentPoints;
    }

    @SuppressWarnings("unchecked")
    private void initPanel() {
        maxPoints.put(INTNAME, bomber.getMaxIntBombPoints());
        maxPoints.put(EXTNAME, bomber.getMaxExtBombPoints());

        maxSize.put(INTNAME, bomber.getMaxIntBombSize());
        maxSize.put(EXTNAME, bomber.getMaxExtBombSize());

        int[] intBombChoices = bomber.getIntBombChoices();
        int[] extBombChoices = bomber.getExtBombChoices();

        JPanel outer = new JPanel();
        outer.setLayout(new GridLayout(0, 2));
        TitledBorder titledBorder = new TitledBorder(new LineBorder(Color.blue), "Bombs");
        Font font2 = new Font("Verdana", Font.BOLD + Font.ITALIC, 12);
        titledBorder.setTitleFont(font2);
        EmptyBorder emptyBorder = new EmptyBorder(10, 10, 10, 10);
        CompoundBorder compoundBorder = new CompoundBorder(titledBorder, emptyBorder);
        outer.setBorder(compoundBorder);

        interiorPanel = initSubPanel(compileBombPoints(intBombChoices), intBombChoices, INTNAME);
        exteriorPanel = initSubPanel(compileBombPoints(extBombChoices), extBombChoices, EXTNAME);

        outer.add(interiorPanel);
        outer.add(exteriorPanel);
        add(outer);
    }

    private JPanel initSubPanel(int availBombPoints, int[] bombChoices, String title){

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
        for (int type = 0; type < BombType.B_NUM; type++) {
            b_labels.get(title)[type] = new JLabel();
            b_choices.get(title)[type] = new JComboBox<String>();

            int maxNumBombs = Math.round(availBombPoints / BombType.getBombCost(type)) + bombChoices[type];

            if (BombType.getBombCost(type) > maxSize.get(title))  {
                maxNumBombs = 0;
            }

            // somehow too many bombs were added
            if ((bombChoices[type] * BombType.getBombCost(type))  > maxSize.get(title)) {
                bombChoices[type] = maxSize.get(title) / BombType.getBombCost(type);
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
            b_labels.get(title)[type].setText(BombType.getBombName(type));
            b_choices.get(title)[type].addItemListener(this);

            if ((type == BombType.B_ALAMO) && !at2Nukes) {
                b_choices.get(title)[type].setEnabled(false);
            }
            if ((type > BombType.B_TAG) && !allowAdvancedAmmo) {
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
    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent ie) {

        for (String title: new String[]{INTNAME, EXTNAME}){
            int[] current = new int[BombType.B_NUM];
            int curPoints = 0;
            for (int type = 0; type < BombType.B_NUM; type++) {
                current[type] = b_choices.get(title)[type].getSelectedIndex();
                curPoints += current[type] * BombType.getBombCost(type);
            }

            int availBombPoints = maxPoints.get(title) - curPoints;

            for (int type = 0; type < BombType.B_NUM; type++) {
                b_choices.get(title)[type].removeItemListener(this);
                b_choices.get(title)[type].removeAllItems();
                int maxNumBombs = Math.round(availBombPoints / BombType.getBombCost(type)) + current[type];

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
        int[] choices = new int[BombType.B_NUM];
        // Internal bombs
        for (int type = 0; type < BombType.B_NUM; type++) {
            choices[type] = b_choices.get(INTNAME)[type].getSelectedIndex();
        }
        bomber.setIntBombChoices(choices);
        // External bombs
        for (int type = 0; type < BombType.B_NUM; type++) {
            choices[type] = b_choices.get(EXTNAME)[type].getSelectedIndex();
        }
        bomber.setExtBombChoices(choices);
    }
    public int[] getChoice() {
        int[] choices = new int[BombType.B_NUM];
        Arrays.fill(choices, 0);

        for (int type = 0; type < BombType.B_NUM; type++) {
            choices[type] += b_choices.get(INTNAME)[type].getSelectedIndex() + b_choices.get(EXTNAME)[type].getSelectedIndex();
        }
        return choices;
    }

    @Override
    public void setEnabled(boolean enabled) {
        for (String title : new String[]{INTNAME, EXTNAME}) {
            for (int type = 0; type < BombType.B_NUM; type++) {
                if ((type == BombType.B_ALAMO)
                        && !at2Nukes) {
                    b_choices.get(title)[type].setEnabled(false);
                } else if ((type > BombType.B_TAG)
                        && !allowAdvancedAmmo) {
                    b_choices.get(title)[type].setEnabled(false);
                } else if ((type == BombType.B_ASEW)
                        || (type == BombType.B_ALAMO)
                        || (type == BombType.B_TAG)) {
                    b_choices.get(title)[type].setEnabled(false);
                } else {
                    b_choices.get(title)[type].setEnabled(enabled);
                }
            }
        }
    }

}
