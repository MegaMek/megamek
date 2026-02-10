/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.advancedsearch.ASAdvancedSearchPanel;
import megamek.client.ui.dialogs.advancedsearch.AdvancedSearchDialog;
import megamek.client.ui.dialogs.advancedsearch.MekSearchFilter;
import megamek.common.loaders.MekSummary;
import megamek.common.util.SimpleRandomLanceCreator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

class SimpleRandomLancePanel extends JPanel implements RandomArmyTab {

    private enum BalanceBy {
        BV("RandomArmyDialog.Simple.BV_NAME"),
        PV("RandomArmyDialog.Simple.PV_NAME"),
        CBILL("RandomArmyDialog.Simple.CBILL_NAME"),
        TONS("RandomArmyDialog.Simple.TONS_NAME"),
        KG("RandomArmyDialog.Simple.KG_NAME");

        final String resourceKey;

        BalanceBy(String resourceKey) {
            this.resourceKey = resourceKey;
        }

        String displayName() {
            return Messages.getString(resourceKey);
        }
    }

    private final JFrame parentFrame;

    private final JButton toleranceButton = new JButton();
    private final JButton unitCountButton = new JButton();
    private final JButton targetValueButton = new JButton();
    private final JButton balanceByButton = new JButton();
    private final JButton filterButton;

    private final JLabel resultLabel = new JLabel();

    private List<MekSummary> roster; // the filtered list of units to build the force from

    private int tolerancePercent = 0;
    private int unitCount = 4;
    private int targetValue = 4000;
    private BalanceBy balanceBy = BalanceBy.BV;

    private AdvancedSearchDialog advancedSearchDialog;
    private MekSearchFilter twSearchFilter;
    private ASAdvancedSearchPanel asSearchFilter;

    public SimpleRandomLancePanel(AbstractRandomArmyDialog parentDialog) {
        this.parentFrame = parentDialog.parentFrame;

        Box contentPanel = Box.createVerticalBox();
        filterButton = new JButton("");

        filterButton.addActionListener(e -> showFilter());
        toleranceButton.addActionListener(this::showTolerancePopup);
        unitCountButton.addActionListener(this::showUnitCountPopup);
        targetValueButton.addActionListener(this::showTargetPopup);
        balanceByButton.addActionListener(this::showBalanceByPopup);

        contentPanel.add(filterButton);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(unitCountButton);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(balanceByButton);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(targetValueButton);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(toleranceButton);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(resultLabel);

        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(new EmptyBorder(25, 25, 0, 0));
        add(contentPanel);
        update();
    }

    void update() {
        targetValueButton.setText(Messages.getString("RandomArmyDialog.Simple.Target", targetValue));
        toleranceButton.setText(Messages.getString("RandomArmyDialog.Simple.Tolerance", tolerancePercent));
        unitCountButton.setText(Messages.getString("RandomArmyDialog.Simple.Units", unitCount));
        balanceByButton.setText(Messages.getString("RandomArmyDialog.Simple.BalanceBy", balanceBy.displayName()));

        roster = SimpleRandomLanceCreator.advancedFilterResult(asSearchFilter, twSearchFilter, null);
        filterButton.setText(Messages.getString("RandomArmyDialog.Simple.Filter", roster.size()));
    }

    private int tolerancePV() {
        return tolerancePercent * targetValue / 100;
    }

    private void showFilter() {
        if (advancedSearchDialog == null) {
            advancedSearchDialog = new AdvancedSearchDialog(parentFrame, 9999);
        }
        advancedSearchDialog.showDialog();
        twSearchFilter = advancedSearchDialog.getTWAdvancedSearch().getMekSearchFilter();
        asSearchFilter = advancedSearchDialog.getASAdvancedSearch();
        update();
    }

    private void showTolerancePopup(ActionEvent e) {
        var popup = new JPopupMenu();
        addActions(popup, ToleranceAction::new, 0, 5, 10, 25, 50);
        popup.add(new ManualValueAction(value -> tolerancePercent = value));
        showPopup(e, popup);
    }

    private void showUnitCountPopup(ActionEvent e) {
        var popup = new JPopupMenu();
        addActions(popup, UnitCountAction::new, 1, 2, 3, 4, 5, 6);
        popup.add(new ManualValueAction(value -> unitCount = value));
        showPopup(e, popup);
    }

    private void showTargetPopup(ActionEvent e) {
        var popup = new JPopupMenu();
        int[] targetValues = switch (balanceBy) {
            case BV -> new int[] { 1000, 2000, 3000, 4000, 5000, 6000 };
            case PV -> new int[] { 20, 40, 60, 80, 100, 120 };
            case TONS -> new int[] { 80, 100, 125, 150, 200, 320, 400 };
            case KG -> new int[] { 200, 500, 1000, 2000, 5000, 8000 };
            case CBILL -> new int[] { 2_500_000, 5_000_000, 7_500_000, 10_000_000, 15_000_000, 20_000_000, 50_000_000 };
        };
        addActions(popup, TargetPvAction::new, targetValues);
        popup.add(new ManualValueAction(value -> targetValue = value));
        showPopup(e, popup);
    }

    private void showBalanceByPopup(ActionEvent e) {
        var popup = new JPopupMenu();
        Arrays.stream(BalanceBy.values()).map(BalanceByAction::new).forEach(popup::add);
        showPopup(e, popup);
    }

    private void addActions(JPopupMenu popup, Function<Integer, Action> creator, int... values) {
        for (int value : values) {
            popup.add(creator.apply(value));
        }
    }

    private void showPopup(ActionEvent e, JPopupMenu popup) {
        Dimension buttonSize = ((JButton) e.getSource()).getSize();
        int x = buttonSize.width;
        popup.show((JButton) e.getSource(), x, 0);
    }

    @Override
    public List<MekSummary> generateMekSummaries() {
        ToDoubleFunction<MekSummary> strengthMapper = switch (balanceBy) {
            case BV -> MekSummary::getBV;
            case PV -> MekSummary::getPointValue;
            case CBILL -> ms -> (double) ms.getCost();
            case TONS -> MekSummary::getTons;
            case KG -> ms -> ms.getTons() * 1000;
        };

        var creator = new SimpleRandomLanceCreator<>(strengthMapper);
        List<MekSummary> result = creator.buildForce(roster, unitCount, targetValue, tolerancePV());
        double achievedValue = result.stream().mapToDouble(strengthMapper).sum();
        resultLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
        resultLabel.setText(Messages.getString("RandomArmyDialog.Simple.Created",
              formatResult(achievedValue),
              balanceBy.displayName()));
        return result;
    }

    private String formatResult(double resultValue) {
        if (balanceBy == BalanceBy.TONS) {
            return String.format("%.2f", resultValue);
        } else {
            return "" + (int) resultValue;
        }
    }

    //region ACTIONS

    private class ToleranceAction extends AbstractAction {

        private final int actionTolerancePercent;

        ToleranceAction(int percent) {
            super(percent + "%");
            actionTolerancePercent = percent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tolerancePercent = actionTolerancePercent;
            update();
        }
    }

    private class UnitCountAction extends AbstractAction {

        private final int actionUnitCount;

        UnitCountAction(int count) {
            super(count + "");
            actionUnitCount = count;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            unitCount = actionUnitCount;
            update();
        }
    }

    private class TargetPvAction extends AbstractAction {

        private final int actionTargetPv;

        TargetPvAction(int count) {
            super(count + "");
            actionTargetPv = count;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            targetValue = actionTargetPv;
            update();
        }
    }

    private class BalanceByAction extends AbstractAction {

        private final BalanceBy actionBalanceBy;

        BalanceByAction(BalanceBy actionBalanceBy) {
            super(actionBalanceBy.displayName());
            this.actionBalanceBy = actionBalanceBy;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            balanceBy = actionBalanceBy;
            update();
        }
    }

    private class ManualValueAction extends AbstractAction {

        private final Consumer<Integer> writeToField;

        ManualValueAction(Consumer<Integer> writeToField) {
            super(Messages.getString("RandomArmyDialog.Simple.Other"));
            this.writeToField = writeToField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object result = JOptionPane.showInputDialog(Messages.getString("RandomArmyDialog.Simple.EnterValue"));
            if (result != null) {
                try {
                    int value = Integer.parseInt(result.toString());
                    writeToField.accept(value);
                } catch (NumberFormatException ignored) {
                    // bad input, not necessary to do anything
                }
                update();
            }
        }
    }
    //endregion
}
