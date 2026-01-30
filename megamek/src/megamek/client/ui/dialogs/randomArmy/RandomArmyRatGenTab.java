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

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.UnitTable;
import megamek.client.ui.Messages;
import megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility;
import megamek.client.ui.util.LambdaAction;
import megamek.client.ui.util.UIUtil;
import megamek.common.loaders.MekSummary;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RandomArmyRatGenTab extends JPanel implements RandomArmyTab {

    private final AbstractRandomArmyDialog parentDialog;
    private final ForceGenerationOptionsPanel forceOptionsPanel;
    private final JTable ratTable;
    private final RatTableModel ratModel;
    protected UnitTable generatedRAT;

    private final Action viewAction = new LambdaAction(Messages.getString("RandomArmyDialog.View"),
          e -> viewReadout());
    private final Action bvAction = new LambdaAction(Messages.getString("RandomArmyDialog.ViewBV"),
          e -> viewBv());
    private final Action costAction = new LambdaAction(Messages.getString("RandomArmyDialog.ViewCost"),
          e -> viewCost());
    private final Action addFromRatAction = new LambdaAction(Messages.getString("RandomArmyDialog.AddToForce"),
          e -> addToChosenDirectly());

    public RandomArmyRatGenTab(AbstractRandomArmyDialog parentDialog, GameOptions gameOptions) {
        this.parentDialog = parentDialog;

        forceOptionsPanel = new ForceGenerationOptionsPanel(ForceGenerationOptionsPanel.Use.RAT_GENERATOR);
        setGameOptions(gameOptions);

        JButton generateRatButton = new JButton(Messages.getString("RandomArmyDialog.Generate"));
        generateRatButton.addActionListener(e -> generateRAT());

        ratModel = new RatTableModel(this);
        ratTable = new JTable(ratModel);
        ratTable.setName("RAT");
        ratTable.addMouseListener(new RandomArmyTableMouseAdapter());
        ratTable.setRowSorter(new TableRowSorter<>(ratModel));
        ratTable.setIntercellSpacing(new Dimension(5, 0));
        ratTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        for (int i = 0; i < ratModel.getColumnCount(); i++) {
            ratTable.getColumnModel().getColumn(i).setPreferredWidth(ratModel.getPreferredWidth(i));
        }
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        ratTable.getColumnModel().getColumn(RatTableModel.COL_BV).setCellRenderer(rightRenderer);

        var fixingPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        fixingPanel.add(forceOptionsPanel);

        var optionsScrollPane = new AbstractRandomArmyDialog.BorderlessScrollPane(fixingPanel) {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(optionsScrollPane, gbc);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        add(generateRatButton, gbc);
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JScrollPane(ratTable), gbc);
    }

    @Override
    public List<MekSummary> generateMekSummaries() {
        List<MekSummary> result = new ArrayList<>();
        int units = forceOptionsPanel.getNumUnits();
        if (units > 0 && generatedRAT != null && generatedRAT.getNumEntries() > 0) {
            result.addAll(generatedRAT.generateUnits(units));
        }
        // generateUnits removes salvage entries that have no units meeting criteria
        ratModel.refreshData();
        return result;
    }

    private void addToChosenDirectly() {
        if (generatedRAT != null) {
            for (int sel : ratTable.getSelectedRows()) {
                sel = ratTable.convertRowIndexToModel(sel);
                MekSummary ms = generatedRAT.getMekSummary(sel);
                if (ms != null) {
                    parentDialog.addToChosenUnits(ms);
                }
            }
        }
    }

    @SuppressWarnings(value = "unchecked")
    private void generateRAT() {
        FactionRecord fRec = forceOptionsPanel.getFaction();
        if (fRec != null) {
            generatedRAT = UnitTable.findTable(fRec,
                  forceOptionsPanel.getUnitType(),
                  forceOptionsPanel.getYear(),
                  forceOptionsPanel.getRating(),
                  (List<Integer>) forceOptionsPanel.getListOption("weightClasses"),
                  forceOptionsPanel.getIntegerOption("networkMask"),
                  (List<EntityMovementMode>) forceOptionsPanel.getListOption("motiveTypes"),
                  (List<MissionRole>) forceOptionsPanel.getListOption("roles"),
                  forceOptionsPanel.getIntegerOption("roleStrictness"));
            ratModel.refreshData();
        }
    }

    public class RandomArmyTableMouseAdapter extends MouseInputAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
    }

    private void showPopup(MouseEvent e) {
        if (e.getSource() == ratTable && ratTable.getSelectedRowCount() > 0) {
            var popup = new JPopupMenu();
            popup.add(viewAction);
            popup.add(bvAction);
            popup.add(costAction);
            popup.addSeparator();
            popup.add(addFromRatAction);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void viewReadout() {
        LobbyUtility.mekReadoutAction(loadSelectedEntities(), true, true, parentDialog.parentFrame);
    }

    private void viewBv() {
        LobbyUtility.mekBVAction(loadSelectedEntities(), true, true, parentDialog.parentFrame);
    }

    private void viewCost() {
        LobbyUtility.mekCostAction(loadSelectedEntities(), true, true, parentDialog.parentFrame);
    }

    @Override
    public void setGameOptions(GameOptions gameOptions) {
        int gameYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        forceOptionsPanel.setYear(gameYear);
    }

    private List<Entity> loadSelectedEntities() {
        return LobbyUtility.getSelectedEntities(ratTable).stream()
              .map(ratModel::getUnitAt)
              .map(MekSummary::loadEntity)
              .filter(Objects::nonNull)
              .toList();
    }
}
