/*
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.advancedsearch.ASAdvancedSearchPanel;
import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchDialog;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.client.ui.util.IntRangeTextField;
import megamek.client.ui.util.UIUtil;
import megamek.common.TechConstants;
import megamek.common.loaders.MekSummary;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.util.RandomArmyCreator;
import megamek.common.util.SimpleRandomLanceCreator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static megamek.client.ui.clientGUI.calculationReport.HTMLCalculationReport.HTML_START;

class RandomArmyBvTab extends JPanel implements RandomArmyTab {

    private static final String WARNING_SIGN_COLORED = "<FONT COLOR = YELLOW> " + UIUtil.WARNING_SIGN + "</FONT> ";

    private GameOptions gameOptions;

    private AdvancedSearchDialog advancedSearchDialog;
    private MekSearchFilter twSearchFilter;
    private ASAdvancedSearchPanel asSearchFilter;

    private final JButton clearAdvSearchButton = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearchClear"));

    private final JLabel mekRoster = new JLabel();
    private final JLabel veeRoster = new JLabel();
    private final JLabel baRoster = new JLabel();
    private final JLabel infRoster = new JLabel();

    private final JTextField bvMin = new JTextField(6);
    private final JTextField bvMax = new JTextField(6);
    private final JTextField yearMin = new JTextField(4);
    private final JTextField yearMax = new JTextField(4);

    private final IntRangeTextField mekCount = new IntRangeTextField(3);
    private final IntRangeTextField veeCount = new IntRangeTextField(3);
    private final IntRangeTextField baCount = new IntRangeTextField(3);
    private final IntRangeTextField infCount = new IntRangeTextField(3);

    private final JCheckBox padWithInf = new JCheckBox(Messages.getString("RandomArmyDialog.Pad"));
    private final JCheckBox onlyCanon = new JCheckBox(Messages.getString("RandomArmyDialog.Canon"));

    private final JFrame parentFrame;

    public RandomArmyBvTab(JFrame parentFrame, GameOptions gameOptions) {
        this.gameOptions = gameOptions;
        this.parentFrame = parentFrame;

        var advSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton advSearchButton = new JButton(Messages.getString("RandomArmyDialog.AdvancedSearch"));
        advSearchPanel.add(advSearchButton);
        advSearchPanel.add(clearAdvSearchButton);
        clearAdvSearchButton.setEnabled(false);

        var yearsPanel = new LinePanel();
        yearsPanel.add(new JLabel(Messages.getString("RandomArmyDialog.Year")));
        yearsPanel.add(Box.createHorizontalStrut(15));
        yearsPanel.add(yearMin);
        yearsPanel.add(new JLabel(" - "));
        yearsPanel.add(yearMax);

        var bvPanel = new LinePanel();
        bvPanel.add(bvMin);
        bvPanel.add(new JLabel(" - "));
        bvPanel.add(bvMax);

        var standardInsets = new Insets(3, 3, 3, 3);
        var headerInsets = new Insets(3, 3, 15, 3);
        var indentedInsets = new Insets(3, 25, 3, 3);

        var mainPanel = new UIUtil.FixedYPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        gbc.insets = headerInsets;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new HeaderPanel(Messages.getString("RandomArmyDialog.Filter")), gbc);
        gbc.fill = GridBagConstraints.NONE;

        gbc.insets = indentedInsets;
        mainPanel.add(yearsPanel, gbc);
        mainPanel.add(onlyCanon, gbc);
        mainPanel.add(advSearchPanel, gbc);
        mainPanel.add(Box.createVerticalStrut(25), gbc);

        gbc.insets = headerInsets;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new HeaderPanel(Messages.getString("RandomArmyDialog.Units")), gbc);
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridwidth = 1;
        gbc.insets = indentedInsets;
        mainPanel.add(new JLabel(Messages.getString("RandomArmyDialog.BV")), gbc);
        mainPanel.add(new JLabel(Messages.getString("RandomArmyDialog.Meks")), gbc);
        mainPanel.add(new JLabel(Messages.getString("RandomArmyDialog.Vees")), gbc);
        mainPanel.add(new JLabel(Messages.getString("RandomArmyDialog.BA")), gbc);
        mainPanel.add(new JLabel(Messages.getString("RandomArmyDialog.Infantry")), gbc);
        gbc.insets = standardInsets;
        gbc.weightx = 1;
        gbc.gridx++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        mainPanel.add(bvPanel, gbc);
        gbc.gridwidth = 1;
        mainPanel.add(mekCount, gbc);
        mainPanel.add(veeCount, gbc);
        mainPanel.add(baCount, gbc);
        mainPanel.add(infCount, gbc);

        gbc.gridx++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0;
        mainPanel.add(mekRoster, gbc);
        mainPanel.add(veeRoster, gbc);
        mainPanel.add(baRoster, gbc);
        mainPanel.add(infRoster, gbc);

        gbc.gridx = 0;
        gbc.insets = indentedInsets;
        gbc.weightx = 0;
        mainPanel.add(padWithInf, gbc);

        setBorder(new EmptyBorder(25, 25, 0, 0));
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(mainPanel);

        setDefaults();
        updateRoster();
        onlyCanon.addActionListener(e -> updateRoster());

        yearMin.addActionListener(e -> updateRoster());
        yearMin.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateRoster();
            }
        });

        yearMax.addActionListener(e -> updateRoster());
        yearMax.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateRoster();
            }
        });

        advSearchButton.addActionListener(e -> {
            createAdvancedSearchDialog();
            advancedSearchDialog.showDialog();
            twSearchFilter = advancedSearchDialog.getTWAdvancedSearch().getMekSearchFilter();
            asSearchFilter = advancedSearchDialog.getASAdvancedSearch();
            clearAdvSearchButton.setEnabled(twSearchFilter != null || asSearchFilter != null);
            updateRoster();
        });
        clearAdvSearchButton.addActionListener(e -> {
            createAdvancedSearchDialog();
            advancedSearchDialog.clearSearches();
            twSearchFilter = null;
            asSearchFilter = null;
            clearAdvSearchButton.setEnabled(false);
            updateRoster();
        });
    }

    private void updateRoster() {
        Predicate<MekSummary> filter = m ->
              (m.getYear() >= yearMin() && m.getYear() <= yearMax())
                    && (!onlyCanon.isSelected() || m.isCanon());

        List<MekSummary> roster = SimpleRandomLanceCreator.advancedFilterResult(
              asSearchFilter,
              twSearchFilter,
              filter);
        long mekRosterCount = roster.stream().filter(MekSummary::isMek).count();
        long veeRosterCount = roster.stream().filter(MekSummary::isVehicle).count();
        long baRosterCount = roster.stream().filter(MekSummary::isBattleArmor).count();
        long infRosterCount = roster.stream().filter(MekSummary::isConventionalInfantry).count();
        String rosterMessage = Messages.getString("RandomArmyDialog.OutOf");
        String formatString = "%s %d";
        mekRoster.setText(formatString.formatted(rosterMessage, mekRosterCount));
        veeRoster.setText(formatString.formatted(rosterMessage, veeRosterCount));
        baRoster.setText(formatString.formatted(rosterMessage, baRosterCount));
        infRoster.setText(formatString.formatted(rosterMessage, infRosterCount));
        if (mekRosterCount == 0 && mekCount.getIntVal() > 0) {
            mekRoster.setText(HTML_START + mekRoster.getText() + WARNING_SIGN_COLORED);
        }
        if (veeRosterCount == 0 && veeCount.getIntVal() > 0) {
            veeRoster.setText(HTML_START + veeRoster.getText() + WARNING_SIGN_COLORED);
        }
        if (baRosterCount == 0 && baCount.getIntVal() > 0) {
            baRoster.setText(HTML_START + baRoster.getText() + WARNING_SIGN_COLORED);
        }
        if (infRosterCount == 0 && infCount.getIntVal() > 0) {
            infRoster.setText(HTML_START + infRoster.getText() + WARNING_SIGN_COLORED);
        }
    }

    private void createAdvancedSearchDialog() {
        advancedSearchDialog = new AdvancedSearchDialog(parentFrame,
              gameOptions.intOption(OptionsConstants.ALLOWED_YEAR));
    }

    private static class LinePanel extends JPanel {
        public LinePanel() {
            setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        }
    }

    private static class HeaderPanel extends Box {
        public HeaderPanel(String title) {
            super(BoxLayout.X_AXIS);
            add(new JLabel(title));
            add(Box.createHorizontalStrut(15));
            add(new CenteredSeparator());
        }
    }

    @Override
    public List<MekSummary> generateMekSummaries() {
        try {
            RandomArmyCreator.Parameters parameters = new RandomArmyCreator.Parameters();
            parameters.advancedSearchFilter = twSearchFilter;
            parameters.asPanel = asSearchFilter;
            parameters.meks = Integer.parseInt(mekCount.getText());
            parameters.tanks = Integer.parseInt(veeCount.getText());
            parameters.ba = Integer.parseInt(baCount.getText());
            parameters.infantry = Integer.parseInt(infCount.getText());
            parameters.canon = onlyCanon.isSelected();
            parameters.maxBV = Integer.parseInt(bvMax.getText());
            parameters.minBV = Integer.parseInt(bvMin.getText());
            parameters.padWithInfantry = padWithInf.isSelected();
            parameters.tech = TechConstants.T_ALL;
            parameters.minYear = yearMin();
            parameters.maxYear = yearMax();
            return RandomArmyCreator.generateArmy(parameters);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Couldn't parse the given numbers.", "Error",
                  JOptionPane.ERROR_MESSAGE);
            return Collections.emptyList();
        }
    }

    @Override
    public void setGameOptions(GameOptions gameOptions) {
        this.gameOptions = gameOptions;
    }

    @Override
    public void setSkillGenerator(AbstractSkillGenerator skillGenerator) {
        // Skills are not generated in this army creator
    }

    private void setDefaults() {
        mekCount.setText("4");
        veeCount.setText("0");
        baCount.setText("0");
        infCount.setText("0");
        bvMin.setText("4000");
        bvMax.setText("8000");
        yearMin.setText("3000");
        yearMax.setText("3047");
    }

    private int yearMin() {
        try {
            return Integer.parseInt(yearMin.getText());
        } catch (NumberFormatException e) {
            return 1950;
        }
    }

    private int yearMax() {
        try {
            return Integer.parseInt(yearMax.getText());
        } catch (NumberFormatException e) {
            return 9999;
        }
    }

    public static class CenteredSeparator extends JSeparator {

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.drawLine(0, 0, getWidth(), 0);
            } finally {
                g2.dispose();
            }
        }
    }
}
