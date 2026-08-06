/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.BooksIcon;
import megamek.client.ui.dialogs.SourceChooserDialog;
import megamek.common.SimpleTechLevel;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;

class MiscSearchTab extends JPanel {

    private static final int LIST_COLUMN_WIDTH = 220;

    final JButton btnBaseClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
    final JTextField tStartWalk = new JTextField(4);
    final JTextField tEndWalk = new JTextField(4);
    final JTextField tStartJump = new JTextField(4);
    final JTextField tEndJump = new JTextField(4);
    final JTextField tStartTankTurrets = new JTextField(4);
    final JTextField tEndTankTurrets = new JTextField(4);
    final JTextField tStartLowerArms = new JTextField(4);
    final JTextField tEndLowerArms = new JTextField(4);
    final JTextField tStartHands = new JTextField(4);
    final JTextField tEndHands = new JTextField(4);
    final JComboBox<String> cArmor = new JComboBox<>();
    final JComboBox<String> cOfficial = new JComboBox<>();
    final JComboBox<String> cCanon = new JComboBox<>();
    final JComboBox<String> cPatchwork = new JComboBox<>();
    final JComboBox<String> cInvalid = new JComboBox<>();
    final JComboBox<String> cFailedToLoadEquipment = new JComboBox<>();
    final JComboBox<String> cClanEngine = new JComboBox<>();
    final JTextField tSource = new JTextField(24);
    final JTextField tMULId = new JTextField(4);
    final JTextField tStartYear = new JTextField(4);
    final JTextField tEndYear = new JTextField(4);
    final JTextField tStartTons = new JTextField(4);
    final JTextField tEndTons = new JTextField(4);
    final JTextField tStartBV = new JTextField(4);
    final JTextField tEndBV = new JTextField(4);
    TriStateItemList listCockpitType;
    TriStateItemList listArmorType;
    TriStateItemList listInternalsType;
    TriStateItemList listEngineType;
    TriStateItemList listGyroType;
    TriStateItemList listTechLevel;
    TriStateItemList listTechBase;
    TriStateItemList listMoveMode;

    private JPanel createBaseAttributes() {
        loadYesNo(cOfficial);
        loadYesNo(cCanon);
        loadYesNo(cInvalid);
        loadYesNo(cFailedToLoadEquipment);

        JPanel baseAttributesPanel = new JPanel(new GridBagLayout());
        baseAttributesPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));

        addLabeledControl(baseAttributesPanel, 0, 0, Messages.getString("MekSelectorDialog.Search.Official"),
              cOfficial);
        addLabeledControl(baseAttributesPanel, 0, 1, Messages.getString("MekSelectorDialog.Search.Canon"), cCanon);
        addLabeledControl(baseAttributesPanel, 0, 2, Messages.getString("MekSelectorDialog.Search.MULId"), tMULId);

        addSourceControl(baseAttributesPanel, 1);

        addLabeledControl(baseAttributesPanel, 2, 0, Messages.getString("MekSelectorDialog.Search.Year"),
              createRangeControl(tStartYear, tEndYear));
        addLabeledControl(baseAttributesPanel, 2, 1,
              Messages.getString("MekSelectorDialog.Search.FailedToLoadEquipment"), cFailedToLoadEquipment);
        addLabeledControl(baseAttributesPanel, 2, 2, Messages.getString("MekSelectorDialog.Search.Invalid"),
              cInvalid);

        addLabeledControl(baseAttributesPanel, 3, 0, Messages.getString("MekSelectorDialog.Search.BV"),
              createRangeControl(tStartBV, tEndBV));
        addLabeledControl(baseAttributesPanel, 3, 1, Messages.getString("MekSelectorDialog.Search.Tons"),
              createRangeControl(tStartTons, tEndTons));
        addLabeledControl(baseAttributesPanel, 3, 2, Messages.getString("MekSelectorDialog.Search.Armor"), cArmor);

        addLabeledControl(baseAttributesPanel, 4, 0, Messages.getString("MekSelectorDialog.Search.Walk"),
              createRangeControl(tStartWalk, tEndWalk));
        addLabeledControl(baseAttributesPanel, 4, 1, Messages.getString("MekSelectorDialog.Search.Jump"),
              createRangeControl(tStartJump, tEndJump));
        addLabeledControl(baseAttributesPanel, 4, 2, Messages.getString("MekSelectorDialog.Search.TankTurrets"),
              createRangeControl(tStartTankTurrets, tEndTankTurrets));

        addLabeledControl(baseAttributesPanel, 5, 0, Messages.getString("MekSelectorDialog.Search.LowerArms"),
              createRangeControl(tStartLowerArms, tEndLowerArms));
        addLabeledControl(baseAttributesPanel, 5, 1, Messages.getString("MekSelectorDialog.Search.Hands"),
              createRangeControl(tStartHands, tEndHands));

        return baseAttributesPanel;
    }

    private void addSourceControl(JPanel panel, int row) {
        JLabel sourceLabel = addAttributeLabel(panel, row, 0,
              Messages.getString("MekSelectorDialog.Search.Source"));
        sourceLabel.setToolTipText(Messages.getString("MekSelectorDialog.Search.Source.TT"));
        tSource.setToolTipText(Messages.getString("MekSelectorDialog.Search.Source.TT"));

        JButton chooseSourceButton = new JButton(new BooksIcon());
        chooseSourceButton.setToolTipText(Messages.getString("MekSelectorDialog.Search.Source.selectSource"));
        chooseSourceButton.addActionListener(e -> {
            String result = SourceChooserDialog.showMultiChoiceDialog(this, true, tSource.getText());
            if (result != null) {
                tSource.setText(result);
            }
        });

        JPanel sourceControl = new JPanel(new GridBagLayout());
        GridBagConstraints sourceGbc = new GridBagConstraints();
        sourceGbc.gridx = 0;
        sourceGbc.gridy = 0;
        sourceGbc.weightx = 1;
        sourceGbc.fill = GridBagConstraints.HORIZONTAL;
        sourceGbc.anchor = GridBagConstraints.WEST;
        sourceControl.add(tSource, sourceGbc);

        sourceGbc.gridx++;
        sourceGbc.weightx = 0;
        sourceGbc.fill = GridBagConstraints.NONE;
        sourceGbc.insets = new Insets(0, 5, 0, 0);
        sourceControl.add(chooseSourceButton, sourceGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 5;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = attributeControlInsets(row);
        panel.add(sourceControl, gbc);
    }

    private void addLabeledControl(JPanel panel, int row, int group, String labelText, JComponent control) {
        int labelColumn = group * 2;
        addAttributeLabel(panel, row, labelColumn, labelText);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = labelColumn + 1;
        gbc.gridy = row;
        gbc.weightx = group == 2 ? 1 : 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = attributeControlInsets(row);
        panel.add(control, gbc);
    }

    private JLabel addAttributeLabel(JPanel panel, int row, int column, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = attributeLabelInsets(row, column);
        panel.add(label, gbc);
        return label;
    }

    private Insets attributeLabelInsets(int row, int column) {
        return new Insets(row == 0 ? 0 : 6, column == 0 ? 0 : 28, 0, 0);
    }

    private Insets attributeControlInsets(int row) {
        return new Insets(row == 0 ? 0 : 6, 5, 0, 0);
    }

    private JPanel createRangeControl(JTextField startField, JTextField endField) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(startField, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel.add(new JLabel("-"), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(endField, gbc);
        return panel;
    }

    private void loadYesNo(JComboBox<String> cb) {
        cb.addItem(Messages.getString("MekSelectorDialog.Search.Any"));
        cb.addItem(Messages.getString("MekSelectorDialog.Search.Yes"));
        cb.addItem(Messages.getString("MekSelectorDialog.Search.No"));
    }

    private JPanel createBaseComboBoxes() {
        cArmor.addItem(Messages.getString("MekSelectorDialog.Search.Any"));
        cArmor.addItem(Messages.getString("MekSelectorDialog.Search.Armor25"));
        cArmor.addItem(Messages.getString("MekSelectorDialog.Search.Armor50"));
        cArmor.addItem(Messages.getString("MekSelectorDialog.Search.Armor75"));
        cArmor.addItem(Messages.getString("MekSelectorDialog.Search.Armor90"));

        loadYesNo(cClanEngine);
        loadYesNo(cPatchwork);

        listArmorType = new TriStateItemList(ArmorType.getAllArmorCodeName(), 5);
        listCockpitType = new TriStateItemList(Mek.getAllCockpitCodeName(), 7);
        listInternalsType = new TriStateItemList(EquipmentType.getAllStructureCodeName(), 7);
        listEngineType = new TriStateItemList(Engine.getAllEngineCodeName(), 5);
        listGyroType = new TriStateItemList(Entity.getAllGyroCodeName(), 7);
        listTechLevel = new TriStateItemList(SimpleTechLevel.getAllSimpleTechLevelCodeName(), 5);

        listTechBase = new TriStateItemList(Entity.getTechBaseDescriptions(), 4);
        List<String> moveModes = Arrays.stream(EntityMovementMode.values())
              .map(EntityMovementMode::toString)
              .distinct()
              .toList();
        listMoveMode = new TriStateItemList(moveModes, 13);

        JPanel baseComboBoxesPanel = new JPanel(new GridBagLayout());
        baseComboBoxesPanel.setBorder(BorderFactory.createEmptyBorder(18, 10, 0, 10));

        addListPanel(baseComboBoxesPanel, 0, 0,
              createListPanel(Messages.getString("MekSelectorDialog.Search.CockpitType"), listCockpitType, null));
        addListPanel(baseComboBoxesPanel, 0, 1,
              createListPanel(Messages.getString("MekSelectorDialog.Search.Engine"), listEngineType,
                  createComboFooter(Messages.getString("MekSelectorDialog.Search.ClanEngine"), cClanEngine)));
        addListPanel(baseComboBoxesPanel, 0, 2,
              createListPanel(Messages.getString("MekSelectorDialog.Search.Gyro"), listGyroType, null));

        addListPanel(baseComboBoxesPanel, 1, 0,
              createListPanel(Messages.getString("MekSelectorDialog.Search.ArmorType"), listArmorType,
                  createComboFooter(Messages.getString("MekSelectorDialog.Search.Patchwork"), cPatchwork)));
        addListPanel(baseComboBoxesPanel, 1, 1,
              createListPanel(Messages.getString("MekSelectorDialog.Search.InternalsType"), listInternalsType, null));
        addListPanel(baseComboBoxesPanel, 1, 2,
              createListPanel(Messages.getString("MekSelectorDialog.Search.MoveMode"), listMoveMode, null), 2);

        addListPanel(baseComboBoxesPanel, 2, 0,
              createListPanel(Messages.getString("MekSelectorDialog.Search.TechLevel"), listTechLevel, null));
        addListPanel(baseComboBoxesPanel, 2, 1,
              createListPanel(Messages.getString("MekSelectorDialog.Search.TechBase"), listTechBase, null));

        return baseComboBoxesPanel;
    }

    private void addListPanel(JPanel panel, int row, int column, JComponent component) {
        addListPanel(panel, row, column, component, 1);
    }

    private void addListPanel(JPanel panel, int row, int column, JComponent component, int rowSpan) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = row;
        gbc.gridheight = rowSpan;
        gbc.weightx = 1;
        gbc.weighty = rowSpan > 1 ? 1 : 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, column == 0 ? 0 : 12, 12, 0);
        panel.add(component, gbc);
    }

    private JPanel createListPanel(String labelText, TriStateItemList itemList, JComponent footer) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel(labelText), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(itemList.getComponent());
        Dimension preferredSize = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(LIST_COLUMN_WIDTH, preferredSize.height));
        panel.add(scrollPane, BorderLayout.CENTER);

        if (footer != null) {
            panel.add(footer, BorderLayout.SOUTH);
        }
        return panel;
    }

    private JPanel createComboFooter(String labelText, JComboBox<String> comboBox) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 0, 5);
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(4, 0, 0, 0);
        panel.add(comboBox, gbc);
        return panel;
    }

    MiscSearchTab() {
        btnBaseClear.addActionListener(e -> clear());

        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(createBaseAttributes(), gbc);

        gbc.gridy++;
        add(createBaseComboBoxes(), gbc);

        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        JPanel clearPanel = new JPanel();
        clearPanel.add(btnBaseClear);
        add(clearPanel, gbc);
    }

    void clear() {
        tStartWalk.setText("");
        tEndWalk.setText("");
        tStartJump.setText("");
        tEndJump.setText("");
        cArmor.setSelectedIndex(0);
        cOfficial.setSelectedIndex(0);
        cCanon.setSelectedIndex(0);
        cPatchwork.setSelectedIndex(0);
        cInvalid.setSelectedIndex(0);
        cFailedToLoadEquipment.setSelectedIndex(0);
        cClanEngine.setSelectedIndex(0);
        tStartTankTurrets.setText("");
        tEndTankTurrets.setText("");
        tStartLowerArms.setText("");
        tEndLowerArms.setText("");
        tStartHands.setText("");
        tEndHands.setText("");
        tStartYear.setText("");
        tEndYear.setText("");
        tStartTons.setText("");
        tEndTons.setText("");
        tStartBV.setText("");
        tEndBV.setText("");
        tSource.setText("");
        tMULId.setText("");

        listCockpitType.clear();
        listArmorType.clear();
        listInternalsType.clear();
        listEngineType.clear();
        listGyroType.clear();
        listTechLevel.clear();
        listTechBase.clear();
        listMoveMode.clear();
    }

    AdvSearchState.MiscState getState() {
        var state = new AdvSearchState.MiscState();
        state.startWalk = tStartWalk.getText();
        state.endWalk = tEndWalk.getText();
        state.startJump = tStartJump.getText();
        state.endJump = tEndJump.getText();
        state.armor = cArmor.getSelectedIndex();
        state.official = cOfficial.getSelectedIndex();
        state.canon = cCanon.getSelectedIndex();
        state.patchwork = cPatchwork.getSelectedIndex();
        state.invalid = cInvalid.getSelectedIndex();
        state.failedToLoadEquipment = cFailedToLoadEquipment.getSelectedIndex();
        state.clanEngine = cClanEngine.getSelectedIndex();
        state.startTankTurrets = tStartTankTurrets.getText();
        state.endTankTurrets = tEndTankTurrets.getText();
        state.startLowerArms = tStartLowerArms.getText();
        state.endLowerArms = tEndLowerArms.getText();
        state.startHands = tStartHands.getText();
        state.endHands = tEndHands.getText();
        state.startYear = tStartYear.getText();
        state.endYear = tEndYear.getText();
        state.startTons = tStartTons.getText();
        state.endTons = tEndTons.getText();
        state.startBV = tStartBV.getText();
        state.endBV = tEndBV.getText();
        state.source = tSource.getText();
        state.mulId = tMULId.getText();

        state.cockpitType = listCockpitType.getState();
        state.armorType = listArmorType.getState();
        state.internalsType = listInternalsType.getState();
        state.engineType = listEngineType.getState();
        state.gyroType = listGyroType.getState();
        state.techLevel = listTechLevel.getState();
        state.techBase = listTechBase.getState();
        state.moveMode = listMoveMode.getState();
        return state;
    }

    void applyState(AdvSearchState.MiscState state) {
        tStartWalk.setText(state.startWalk);
        tEndWalk.setText(state.endWalk);
        tStartJump.setText(state.startJump);
        tEndJump.setText(state.endJump);
        cArmor.setSelectedIndex(state.armor);
        cOfficial.setSelectedIndex(state.official);
        cCanon.setSelectedIndex(state.canon);
        cPatchwork.setSelectedIndex(state.patchwork);
        cInvalid.setSelectedIndex(state.invalid);
        cFailedToLoadEquipment.setSelectedIndex(state.failedToLoadEquipment);
        cClanEngine.setSelectedIndex(state.clanEngine);
        tStartTankTurrets.setText(state.startTankTurrets);
        tEndTankTurrets.setText(state.endTankTurrets);
        tStartLowerArms.setText(state.startLowerArms);
        tEndLowerArms.setText(state.endLowerArms);
        tStartHands.setText(state.startHands);
        tEndHands.setText(state.endHands);
        tStartYear.setText(state.startYear);
        tEndYear.setText(state.endYear);
        tStartTons.setText(state.startTons);
        tEndTons.setText(state.endTons);
        tStartBV.setText(state.startBV);
        tEndBV.setText(state.endBV);
        tSource.setText(state.source);
        tMULId.setText(state.mulId);

        listCockpitType.applyState(state.cockpitType);
        listArmorType.applyState(state.armorType);
        listInternalsType.applyState(state.internalsType);
        listEngineType.applyState(state.engineType);
        listGyroType.applyState(state.gyroType);
        listTechLevel.applyState(state.techLevel);
        listTechBase.applyState(state.techBase);
        listMoveMode.applyState(state.moveMode);
    }
}
