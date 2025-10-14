/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.units.IBomber;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2012-04-07
 */
public class BombChoicePanel extends JPanel implements ItemListener {
    private final IBomber bomber;
    private final boolean at2Nukes;
    private final boolean allowAdvancedAmmo;

    private boolean empty = false;

    @Serial
    private static final long serialVersionUID = 483782753790544050L;

    private final String INTERNAL_NAME = "Internal";
    private final String EXTERNAL_NAME = "External";

    private final Map<String, EnumMap<BombTypeEnum, JComboBox<String>>> b_choices = Map.of(
          INTERNAL_NAME, new EnumMap<>(BombTypeEnum.class),
          EXTERNAL_NAME, new EnumMap<>(BombTypeEnum.class)
    );
    private final Map<String, EnumMap<BombTypeEnum, JLabel>> b_labels = Map.of(
          INTERNAL_NAME, new EnumMap<>(BombTypeEnum.class),
          EXTERNAL_NAME, new EnumMap<>(BombTypeEnum.class)
    );
    private final HashMap<String, Integer> maxPoints = new HashMap<>();
    private final HashMap<String, Integer> maxSize = new HashMap<>();
    private final int maxRows = (int) Math.ceil(BombTypeEnum.values().length / 2.0);

    //Variable for MekHQ functionality
    private final BombLoadout typeMax;

    public BombChoicePanel(IBomber bomber, boolean at2Nukes, boolean allowAdvancedAmmo) {
        this(bomber, at2Nukes, allowAdvancedAmmo, null);
    }

    //Constructor to call from MekHQ to pass in typeMax
    public BombChoicePanel(IBomber bomber, boolean at2Nukes, boolean allowAdvancedAmmo, BombLoadout typeMax) {
        this.bomber = bomber;
        this.at2Nukes = at2Nukes;
        this.allowAdvancedAmmo = allowAdvancedAmmo;
        this.typeMax = typeMax;

        initArrays();
        initPanel();
    }

    private void initArrays() {
        // Initialize control arrays
        maxSize.put(INTERNAL_NAME, 0);
        maxSize.put(EXTERNAL_NAME, 0);
    }

    private void initPanel() {
        maxPoints.put(INTERNAL_NAME, bomber.getMaxIntBombPoints());
        maxPoints.put(EXTERNAL_NAME, bomber.getMaxExtBombPoints());

        maxSize.put(INTERNAL_NAME, bomber.getMaxIntBombSize());
        maxSize.put(EXTERNAL_NAME, bomber.getMaxExtBombSize());

        BombLoadout intBombChoices = bomber.getIntBombChoices();
        BombLoadout extBombChoices = bomber.getExtBombChoices();

        int columns = (maxPoints.get(INTERNAL_NAME) > 0 ? 1 : 0) + (maxPoints.get(EXTERNAL_NAME) > 0 ? 1 : 0);
        // Should not occur!
        if (columns == 0) {
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

        JPanel interiorPanel = initSubPanel(maxPoints.get(INTERNAL_NAME) - intBombChoices.getTotalBombCost(),
              intBombChoices,
              INTERNAL_NAME);
        JPanel exteriorPanel = initSubPanel(maxPoints.get(EXTERNAL_NAME) - extBombChoices.getTotalBombCost(),
              extBombChoices,
              EXTERNAL_NAME);

        if (maxPoints.get(INTERNAL_NAME) != 0) {
            outer.add(interiorPanel);
        }
        if (maxPoints.get(EXTERNAL_NAME) != 0) {
            outer.add(exteriorPanel);
        }
        add(outer);
    }

    private JPanel initSubPanel(int availBombPoints, BombLoadout bombChoices, String title) {

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
            if (type == BombTypeEnum.NONE) {continue;}

            JLabel label = new JLabel();
            JComboBox<String> comboBox = new JComboBox<>();
            b_labels.get(title).put(type, label);
            b_choices.get(title).put(type, comboBox);

            int currentCount = bombChoices.getCount(type);
            int maxNumBombs = availBombPoints / type.getCost() + currentCount;

            if (type.getCost() > maxSize.get(title)) {
                maxNumBombs = 0;
            }

            // somehow too many bombs were added
            if ((currentCount * type.getCost()) > maxSize.get(title)) {
                currentCount = maxSize.get(title) / type.getCost();
                bombChoices.put(type, currentCount);
            }

            if (typeMax != null) {
                int typeMaxCount = typeMax.getCount(type);
                if ((maxNumBombs > 0) && (maxNumBombs > typeMaxCount)) {
                    maxNumBombs = typeMaxCount;
                }
            }

            if (currentCount > maxNumBombs) {
                maxNumBombs = currentCount;
            }

            if (maxNumBombs < 0) {
                maxNumBombs = 0;
            }

            if (maxNumBombs > maxSize.get(title)) {
                maxNumBombs = maxSize.get(title);
            }

            for (int x = 0; x <= maxNumBombs; x++) {
                comboBox.addItem(Integer.toString(x));
            }

            comboBox.setSelectedIndex(currentCount);
            label.setText(type.getDisplayName());
            comboBox.addItemListener(this);

            if ((type == BombTypeEnum.ALAMO) && !at2Nukes) {
                comboBox.setEnabled(false);
            }
            if (type.isAdvancedAmmo() && !allowAdvancedAmmo) {
                comboBox.setEnabled(false);
            }
            if (row >= maxRows) {
                row = 0;
                column += 2;
            }

            c.gridx = column;
            c.gridy = row;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(label, c);
            inner.add(label);

            c.gridx = column + 1;
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(comboBox, c);
            inner.add(comboBox);
            row++;
        }
        return inner;
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {

        for (String title : new String[] { INTERNAL_NAME, EXTERNAL_NAME }) {
            BombLoadout current = new BombLoadout();
            for (BombTypeEnum type : BombTypeEnum.values()) {
                if (type == BombTypeEnum.NONE) {continue;}
                int selectedCount = b_choices.get(title).get(type).getSelectedIndex();
                if (selectedCount > 0) {
                    current.put(type, selectedCount);
                }
            }
            int availBombPoints = maxPoints.get(title) - current.getTotalBombCost();

            for (BombTypeEnum type : BombTypeEnum.values()) {
                if (type == BombTypeEnum.NONE) {continue;}

                JComboBox<String> comboBox = b_choices.get(title).get(type);
                comboBox.removeItemListener(this);
                comboBox.removeAllItems();

                int currentCount = current.getCount(type);
                int maxNumBombs = (availBombPoints / type.getCost()) + currentCount;

                if (typeMax != null) {
                    int typeMaxCount = typeMax.getCount(type);
                    if ((maxNumBombs > 0) && (maxNumBombs > typeMaxCount)) {
                        maxNumBombs = typeMaxCount;
                    }
                }

                if (currentCount > maxNumBombs) {
                    maxNumBombs = currentCount;
                }

                if (maxNumBombs < 0) {
                    maxNumBombs = 0;
                }

                if (maxNumBombs > maxSize.get(title)) {
                    maxNumBombs = maxSize.get(title);
                }


                for (int x = 0; x <= maxNumBombs; x++) {
                    comboBox.addItem(Integer.toString(x));
                }
                comboBox.setSelectedIndex(currentCount);
                comboBox.addItemListener(this);
            }
        }
    }

    public void applyChoice() {
        // Return cleanly if bomber never had any capacity but e.g. Internal Bomb Bay tried to add bomb capacity.
        if (empty) {
            return;
        }

        // Internal bombs
        BombLoadout intChoices = new BombLoadout();
        for (BombTypeEnum type : BombTypeEnum.values()) {
            if (type == BombTypeEnum.NONE) {continue;}
            int count = b_choices.get(INTERNAL_NAME).get(type).getSelectedIndex();
            if (count > 0) {
                intChoices.put(type, count);
            }
        }
        bomber.setIntBombChoices(intChoices);

        // External bombs
        BombLoadout extChoices = new BombLoadout();
        for (BombTypeEnum type : BombTypeEnum.values()) {
            if (type == BombTypeEnum.NONE) {continue;}
            int count = b_choices.get(EXTERNAL_NAME).get(type).getSelectedIndex();
            if (count > 0) {
                extChoices.put(type, count);
            }
        }
        bomber.setExtBombChoices(extChoices);
    }

    public BombLoadout getChoice() {
        BombLoadout choices = new BombLoadout();
        if (empty) {
            return choices;
        }

        for (BombTypeEnum type : BombTypeEnum.values()) {
            if (type == BombTypeEnum.NONE) {continue;}
            int intCount = b_choices.get(INTERNAL_NAME).get(type).getSelectedIndex();
            int extCount = b_choices.get(EXTERNAL_NAME).get(type).getSelectedIndex();
            int totalCount = intCount + extCount;
            if (totalCount > 0) {
                choices.put(type, totalCount);
            }
        }
        return choices;
    }

    @Override
    public void setEnabled(boolean enabled) {
        for (String title : new String[] { INTERNAL_NAME, EXTERNAL_NAME }) {
            for (BombTypeEnum type : BombTypeEnum.values()) {
                if (type == BombTypeEnum.NONE) {continue;}
                JComboBox<String> comboBox = b_choices.get(title).get(type);
                if ((type == BombTypeEnum.ALAMO) && !at2Nukes) {
                    comboBox.setEnabled(false);
                } else if ((type.isAdvancedAmmo()) && !allowAdvancedAmmo) {
                    comboBox.setEnabled(false);
                } else {
                    comboBox.setEnabled(enabled);
                }
            }
        }
    }

}
