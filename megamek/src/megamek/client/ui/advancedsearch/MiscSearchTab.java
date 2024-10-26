/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.advancedsearch;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.equipment.ArmorType;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

class MiscSearchTab extends JPanel {

    final JButton btnBaseClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
    final JTextField tStartWalk = new JTextField(4);
    final JTextField tEndWalk = new JTextField(4);
    final JTextField tStartJump = new JTextField(4);
    final JTextField tEndJump = new JTextField(4);
    final JTextField tStartTankTurrets = new JTextField(4);
    final JTextField tEndTankTurrets= new JTextField(4);
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
    final JTextField tSource = new JTextField(4);
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

        JPanel baseAttributesPanel = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        baseAttributesPanel.setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth  = 1;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridy = 0;

        JPanel p0Panel = new JPanel();
        p0Panel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Official")));
        p0Panel.add(cOfficial);
        p0Panel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Canon")));
        p0Panel.add(cCanon);
        baseAttributesPanel.add(p0Panel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        JPanel sPanel = new JPanel(new BorderLayout());
        var sourceLabel = new JLabel(Messages.getString("MekSelectorDialog.Search.Source"));
        sourceLabel.setToolTipText(Messages.getString("MekSelectorDialog.Search.Source.TT"));
        tSource.setToolTipText(Messages.getString("MekSelectorDialog.Search.Source.TT"));
        sPanel.add(sourceLabel, BorderLayout.WEST);
        sPanel.add(tSource, BorderLayout.CENTER);
        baseAttributesPanel.add(sPanel, c);
        JPanel mPanel = new JPanel(new BorderLayout());
        mPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.MULId")), BorderLayout.WEST);
        mPanel.add(tMULId, BorderLayout.CENTER);
        baseAttributesPanel.add(mPanel, c);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridy++;
        JPanel yearPanel = new JPanel();
        yearPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Year")));
        yearPanel.add(tStartYear);
        yearPanel.add(new JLabel("-"));
        yearPanel.add(tEndYear);
        baseAttributesPanel.add(yearPanel, c);
        JPanel p1bPanel = new JPanel();
        p1bPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.FailedToLoadEquipment")));
        p1bPanel.add(cFailedToLoadEquipment);
        baseAttributesPanel.add(p1bPanel, c);
        JPanel p1cPanel = new JPanel();
        p1cPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Invalid")));
        p1cPanel.add(cInvalid);
        baseAttributesPanel.add(p1cPanel, c);

        c.gridy++;
        JPanel bvPanel = new JPanel();
        bvPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.BV")));
        bvPanel.add(tStartBV);
        bvPanel.add(new JLabel("-"));
        bvPanel.add(tEndBV);
        baseAttributesPanel.add(bvPanel, c);
        JPanel tonsPanel = new JPanel();
        tonsPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Tons")));
        tonsPanel.add(tStartTons);
        tonsPanel.add(new JLabel("-"));
        tonsPanel.add(tEndTons);
        baseAttributesPanel.add(tonsPanel, c);

        c.gridy++;
        JPanel walkPanel = new JPanel();
        walkPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Walk")));
        walkPanel.add(tStartWalk);
        walkPanel.add(new JLabel("-"));
        walkPanel.add(tEndWalk);
        baseAttributesPanel.add(walkPanel, c);
        JPanel jumpPanel = new JPanel();
        jumpPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Jump")));
        jumpPanel.add(tStartJump);
        jumpPanel.add(new JLabel("-"));
        jumpPanel.add(tEndJump);
        baseAttributesPanel.add(jumpPanel, c);

        c.gridy++;
        JPanel lowerArmsPanel = new JPanel();
        lowerArmsPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.LowerArms")));
        lowerArmsPanel.add(tStartLowerArms);
        lowerArmsPanel.add(new JLabel("-"));
        lowerArmsPanel.add(tEndLowerArms);
        baseAttributesPanel.add(lowerArmsPanel, c);
        JPanel handsPanel = new JPanel();
        handsPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Hands")));
        handsPanel.add(tStartHands);
        handsPanel.add(new JLabel("-"));
        handsPanel.add(tEndHands);
        baseAttributesPanel.add(handsPanel, c);

        c.gridy++;
        JPanel p2Panel = new JPanel();
        p2Panel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.TankTurrets")));
        p2Panel.add(tStartTankTurrets);
        p2Panel.add(new JLabel("-"));
        p2Panel.add(tEndTankTurrets);
        baseAttributesPanel.add(p2Panel, c);
        JPanel armorPanel = new JPanel();
        armorPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Armor")));
        armorPanel.add(cArmor);
        baseAttributesPanel.add(armorPanel, c);

        return baseAttributesPanel;
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
        List<String> moveModes = Arrays.stream(EntityMovementMode.values()).map(EntityMovementMode::toString).distinct().toList();
        listMoveMode = new TriStateItemList(moveModes, 13);

        JPanel baseComboBoxesPanel = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        baseComboBoxesPanel.setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridx = 0;
        c.gridy = 0;

        JPanel cockpitPanel = new JPanel(new BorderLayout());
        cockpitPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.CockpitType")), BorderLayout.NORTH);
        cockpitPanel.add(new JScrollPane(listCockpitType.getComponent()), BorderLayout.CENTER);
        baseComboBoxesPanel.add(cockpitPanel, c);
        c.gridx = 1;
        JPanel enginePanel = new JPanel(new BorderLayout());
        enginePanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Engine")), BorderLayout.NORTH);
        enginePanel.add(new JScrollPane(listEngineType.getComponent()), BorderLayout.CENTER);
        JPanel clanEnginePanel = new JPanel();
        clanEnginePanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.ClanEngine")));
        clanEnginePanel.add(cClanEngine);
        enginePanel.add(clanEnginePanel, BorderLayout.SOUTH);
        baseComboBoxesPanel.add(enginePanel, c);
        c.gridx = 2;
        JPanel gyroPanel = new JPanel(new BorderLayout());
        gyroPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Gyro")), BorderLayout.NORTH);
        gyroPanel.add(new JScrollPane(listGyroType.getComponent()), BorderLayout.CENTER);
        baseComboBoxesPanel.add(gyroPanel, c);

        c.gridx = 0;
        c.gridy++;
        JPanel armorTypePanel = new JPanel(new BorderLayout());
        armorTypePanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.ArmorType")), BorderLayout.NORTH);
        armorTypePanel.add(new JScrollPane(listArmorType.getComponent()), BorderLayout.CENTER);
        JPanel patchworkPanel = new JPanel();
        patchworkPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.Patchwork")));
        patchworkPanel.add(cPatchwork);
        armorTypePanel.add(patchworkPanel, BorderLayout.SOUTH);
        baseComboBoxesPanel.add(armorTypePanel, c);
        c.gridx = 1;
        JPanel internalsPanel = new JPanel(new BorderLayout());
        internalsPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.InternalsType")), BorderLayout.NORTH);
        internalsPanel.add(new JScrollPane(listInternalsType.getComponent()), BorderLayout.CENTER);
        baseComboBoxesPanel.add(internalsPanel, c);

        c.gridx = 2;
        c.gridheight = 2;
        JPanel moveModePanel = new JPanel(new BorderLayout());
        moveModePanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.MoveMode")), BorderLayout.NORTH);
        moveModePanel.add(new JScrollPane(listMoveMode.getComponent()), BorderLayout.CENTER);
        baseComboBoxesPanel.add(moveModePanel, c);

        c.gridx = 0;
        c.gridy++;
        c.gridheight = 1;
        JPanel techLevelPanel = new JPanel(new BorderLayout());
        techLevelPanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.TechLevel")), BorderLayout.NORTH);
        techLevelPanel.add(new JScrollPane(listTechLevel.getComponent()), BorderLayout.CENTER);
        baseComboBoxesPanel.add(techLevelPanel, c);
        c.gridx = 1;
        JPanel techBasePanel = new JPanel(new BorderLayout());
        techBasePanel.add(new JLabel(Messages.getString("MekSelectorDialog.Search.TechBase")), BorderLayout.NORTH);
        techBasePanel.add(new JScrollPane(listTechBase.getComponent()), BorderLayout.CENTER);
        baseComboBoxesPanel.add(techBasePanel, c);

        return baseComboBoxesPanel;
    }

    MiscSearchTab() {
        btnBaseClear.addActionListener(e -> clear());

        GridBagConstraints c = new GridBagConstraints();
        setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth  = 1;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridy = 0;
        add(createBaseAttributes(), c);

        c.gridy++;
        add(createBaseComboBoxes(), c);

        c.weighty = 1;
        c.gridy++;
        JPanel clearPanel = new JPanel();
        clearPanel.add(btnBaseClear, c);
        add(clearPanel, c);
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
}
