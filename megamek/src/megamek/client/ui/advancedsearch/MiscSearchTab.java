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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public class MiscSearchTab extends JPanel {

    JButton btnBaseClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
    JLabel lblWalk = new JLabel(Messages.getString("MekSelectorDialog.Search.Walk"));
    JTextField tStartWalk = new JTextField(4);
    JTextField tEndWalk = new JTextField(4);
    JLabel lblJump = new JLabel(Messages.getString("MekSelectorDialog.Search.Jump"));
    JTextField tStartJump = new JTextField(4);
    JTextField tEndJump = new JTextField(4);
    JLabel lblTankTurrets = new JLabel(Messages.getString("MekSelectorDialog.Search.TankTurrets"));
    JTextField tStartTankTurrets = new JTextField(4);
    JTextField tEndTankTurrets= new JTextField(4);
    JLabel lblLowerArms = new JLabel(Messages.getString("MekSelectorDialog.Search.LowerArms"));
    JTextField tStartLowerArms = new JTextField(4);
    JTextField tEndLowerArms = new JTextField(4);
    JLabel lblHands = new JLabel(Messages.getString("MekSelectorDialog.Search.Hands"));
    JTextField tStartHands = new JTextField(4);
    JTextField tEndHands = new JTextField(4);
    JLabel lblArmor = new JLabel(Messages.getString("MekSelectorDialog.Search.Armor"));
    JComboBox<String> cArmor = new JComboBox<>();
    JLabel lblOfficial = new JLabel(Messages.getString("MekSelectorDialog.Search.Official"));
    JComboBox<String> cOfficial = new JComboBox<>();
    JLabel lblCanon = new JLabel(Messages.getString("MekSelectorDialog.Search.Canon"));
    JComboBox<String> cCanon = new JComboBox<>();
    JLabel lblPatchwork = new JLabel(Messages.getString("MekSelectorDialog.Search.Patchwork"));
    JComboBox<String> cPatchwork = new JComboBox<>();
    JLabel lblInvalid = new JLabel(Messages.getString("MekSelectorDialog.Search.Invalid"));
    JComboBox<String> cInvalid = new JComboBox<>();
    JLabel lblFailedToLoadEquipment = new JLabel(Messages.getString("MekSelectorDialog.Search.FailedToLoadEquipment"));
    JComboBox<String> cFailedToLoadEquipment = new JComboBox<>();
    JLabel lblClanEngine = new JLabel(Messages.getString("MekSelectorDialog.Search.ClanEngine"));
    JComboBox<String> cClanEngine = new JComboBox<>();
    JLabel lblSource = new JLabel(Messages.getString("MekSelectorDialog.Search.Source"));
    JTextField tSource = new JTextField(4);
    JLabel lblMULId = new JLabel(Messages.getString("MekSelectorDialog.Search.MULId"));
    JTextField tMULId = new JTextField(4);
    JLabel lblYear = new JLabel(Messages.getString("MekSelectorDialog.Search.Year"));
    JTextField tStartYear = new JTextField(4);
    JTextField tEndYear = new JTextField(4);
    JLabel lblTons = new JLabel(Messages.getString("MekSelectorDialog.Search.Tons"));
    JTextField tStartTons = new JTextField(4);
    JTextField tEndTons = new JTextField(4);
    JLabel lblBV = new JLabel(Messages.getString("MekSelectorDialog.Search.BV"));
    JTextField tStartBV = new JTextField(4);
    JTextField tEndBV = new JTextField(4);
    JLabel lblCockpitType = new JLabel(Messages.getString("MekSelectorDialog.Search.CockpitType"));
    JList<TriStateItem> listCockpitType = new JList<>(new DefaultListModel<>());
    JScrollPane spCockpitType = new JScrollPane(listCockpitType);
    JLabel lblArmorType = new JLabel(Messages.getString("MekSelectorDialog.Search.ArmorType"));
    JList<TriStateItem> listArmorType = new JList<>(new DefaultListModel<>());
    JScrollPane spArmorType = new JScrollPane(listArmorType);
    JLabel lblInternalsType = new JLabel(Messages.getString("MekSelectorDialog.Search.InternalsType"));
    JList<TriStateItem> listInternalsType = new JList<>(new DefaultListModel<>());
    JScrollPane spInternalsType = new JScrollPane(listInternalsType);
    JLabel lblEngineType = new JLabel(Messages.getString("MekSelectorDialog.Search.Engine"));
    JList<TriStateItem> listEngineType = new JList<>(new DefaultListModel<>());
    JScrollPane spEngineType = new JScrollPane(listEngineType);
    JLabel lblGyroType = new JLabel(Messages.getString("MekSelectorDialog.Search.Gyro"));
    JList<TriStateItem> listGyroType = new JList<>(new DefaultListModel<>());
    JScrollPane spGyroType = new JScrollPane(listGyroType);
    JLabel lblTechLevel = new JLabel(Messages.getString("MekSelectorDialog.Search.TechLevel"));
    JList<TriStateItem> listTechLevel = new JList<>(new DefaultListModel<>());
    JScrollPane spTechLevel = new JScrollPane(listTechLevel);
    JLabel lblTechBase = new JLabel(Messages.getString("MekSelectorDialog.Search.TechBase"));
    JList<TriStateItem> listTechBase = new JList<>(new DefaultListModel<>());
    JScrollPane spTechBase = new JScrollPane(listTechBase);

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
        c.gridx = 0; c.gridy = 0;

        JPanel p0Panel = new JPanel();
        p0Panel.add(lblOfficial);
        p0Panel.add(cOfficial);
        p0Panel.add(lblCanon);
        p0Panel.add(cCanon);
        baseAttributesPanel.add(p0Panel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        JPanel sPanel = new JPanel(new BorderLayout());
        sPanel.add(lblSource, BorderLayout.WEST);
        sPanel.add(tSource, BorderLayout.CENTER);
        baseAttributesPanel.add(sPanel, c);
        c.gridx = 2;
        JPanel mPanel = new JPanel(new BorderLayout());
        mPanel.add(lblMULId, BorderLayout.WEST);
        mPanel.add(tMULId, BorderLayout.CENTER);
        baseAttributesPanel.add(mPanel, c);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridx = 0; c.gridy++;
        JPanel yearPanel = new JPanel();
        yearPanel.add(lblYear);
        yearPanel.add(tStartYear);
        yearPanel.add(new JLabel("-"));
        yearPanel.add(tEndYear);
        baseAttributesPanel.add(yearPanel, c);
        c.gridx = 1;
        JPanel p1bPanel = new JPanel();
        p1bPanel.add(lblFailedToLoadEquipment);
        p1bPanel.add(cFailedToLoadEquipment);
        baseAttributesPanel.add(p1bPanel, c);
        c.gridx = 2;
        JPanel p1cPanel = new JPanel();
        p1cPanel.add(lblInvalid);
        p1cPanel.add(cInvalid);
        baseAttributesPanel.add(p1cPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel bvPanel = new JPanel();
        bvPanel.add(lblBV);
        bvPanel.add(tStartBV);
        bvPanel.add(new JLabel("-"));
        bvPanel.add(tEndBV);
        baseAttributesPanel.add(bvPanel, c);
        c.gridx = 1;
        JPanel tonsPanel = new JPanel();
        tonsPanel.add(lblTons);
        tonsPanel.add(tStartTons);
        tonsPanel.add(new JLabel("-"));
        tonsPanel.add(tEndTons);
        baseAttributesPanel.add(tonsPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel walkPanel = new JPanel();
        walkPanel.add(lblWalk);
        walkPanel.add(tStartWalk);
        walkPanel.add(new JLabel("-"));
        walkPanel.add(tEndWalk);
        baseAttributesPanel.add(walkPanel, c);
        c.gridx = 1;
        JPanel jumpPanel = new JPanel();
        jumpPanel.add(lblJump);
        jumpPanel.add(tStartJump);
        jumpPanel.add(new JLabel("-"));
        jumpPanel.add(tEndJump);
        baseAttributesPanel.add(jumpPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel lowerArmsPanel = new JPanel();
        lowerArmsPanel.add(lblLowerArms);
        lowerArmsPanel.add(tStartLowerArms);
        lowerArmsPanel.add(new JLabel("-"));
        lowerArmsPanel.add(tEndLowerArms);
        baseAttributesPanel.add(lowerArmsPanel, c);
        c.gridx = 1;
        JPanel handsPanel = new JPanel();
        handsPanel.add(lblHands);
        handsPanel.add(tStartHands);
        handsPanel.add(new JLabel("-"));
        handsPanel.add(tEndHands);
        baseAttributesPanel.add(handsPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel p2Panel = new JPanel();
        p2Panel.add(lblTankTurrets);
        p2Panel.add(tStartTankTurrets);
        p2Panel.add(new JLabel("-"));
        p2Panel.add(tEndTankTurrets);
        baseAttributesPanel.add(p2Panel, c);
        c.gridx = 1;
        JPanel armorPanel = new JPanel();
        armorPanel.add(lblArmor);
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

        loadTriStateItem(ArmorType.getAllArmorCodeName(), listArmorType, 5);
        loadTriStateItem(Mek.getAllCockpitCodeName(), listCockpitType, 7);
        loadTriStateItem(EquipmentType.getAllStructureCodeName(), listInternalsType, 7);
        loadTriStateItem(Engine.getAllEngineCodeName(), listEngineType, 5);
        loadTriStateItem(Entity.getAllGyroCodeName(), listGyroType, 7);
        loadTriStateItem(SimpleTechLevel.getAllSimpleTechLevelCodeName(), listTechLevel, 5);
        loadTriStateItem(Entity.getTechBaseDescriptions(), listTechBase, 4);

        JPanel baseComboBoxesPanel = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        baseComboBoxesPanel.setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth  = 1;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridx = 0; c.gridy = 0;

        JPanel cockpitPanel = new JPanel(new BorderLayout());
        cockpitPanel.add(lblCockpitType, BorderLayout.NORTH);
        cockpitPanel.add(spCockpitType, BorderLayout.CENTER);
        baseComboBoxesPanel.add(cockpitPanel, c);
        c.gridx = 1;
        JPanel enginePanel = new JPanel(new BorderLayout());
        enginePanel.add(lblEngineType, BorderLayout.NORTH);
        enginePanel.add(spEngineType, BorderLayout.CENTER);
        JPanel clanEnginePanel = new JPanel();
        clanEnginePanel.add(lblClanEngine);
        clanEnginePanel.add(cClanEngine);
        enginePanel.add(clanEnginePanel, BorderLayout.SOUTH);
        baseComboBoxesPanel.add(enginePanel, c);
        c.gridx = 2;
        JPanel gyroPanel = new JPanel(new BorderLayout());
        gyroPanel.add(lblGyroType, BorderLayout.NORTH);
        gyroPanel.add(spGyroType, BorderLayout.CENTER);
        baseComboBoxesPanel.add(gyroPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel armorTypePanel = new JPanel(new BorderLayout());
        armorTypePanel.add(lblArmorType, BorderLayout.NORTH);
        armorTypePanel.add(spArmorType, BorderLayout.CENTER);
        JPanel patchworkPanel = new JPanel();
        patchworkPanel.add(lblPatchwork);
        patchworkPanel.add(cPatchwork);
        armorTypePanel.add(patchworkPanel, BorderLayout.SOUTH);
        baseComboBoxesPanel.add(armorTypePanel, c);
        c.gridx = 1;
        JPanel internalsPanel = new JPanel(new BorderLayout());
        internalsPanel.add(lblInternalsType, BorderLayout.NORTH);
        internalsPanel.add(spInternalsType, BorderLayout.CENTER);
        baseComboBoxesPanel.add(internalsPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel techLevelPanel = new JPanel(new BorderLayout());
        techLevelPanel.add(lblTechLevel, BorderLayout.NORTH);
        techLevelPanel.add(spTechLevel, BorderLayout.CENTER);
        baseComboBoxesPanel.add(techLevelPanel, c);
        c.gridx = 1;
        JPanel techBasePanel = new JPanel(new BorderLayout());
        techBasePanel.add(lblTechBase, BorderLayout.NORTH);
        techBasePanel.add(spTechBase, BorderLayout.CENTER);
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
        c.gridx = 0; c.gridy = 0;

        add(createBaseAttributes(), c);
        c.gridx = 0; c.gridy++;
        add(createBaseComboBoxes(), c);

        c.weighty = 1;
        JPanel clearPanel = new JPanel();
        c.gridx = 0; c.gridy++;
        clearPanel.add(btnBaseClear, c);
        add(clearPanel, c);
    }

    private void loadTriStateItem(List<String> s, JList<TriStateItem> l, int count) {
        DefaultListModel<TriStateItem> dlma = new DefaultListModel<>();

        for (String desc : s) {
            dlma.addElement(new TriStateItem("\u2610", desc));
        }

        l.setModel(dlma);
        l.setVisibleRowCount(count);
        jListSetup(l);
    }

    private void loadTriStateItem(Map<Integer, String> s, JList<TriStateItem> l, int count) {
        DefaultListModel<TriStateItem> dlma = new DefaultListModel<>();

        for (Map.Entry<Integer, String> desc : s.entrySet()) {
            dlma.addElement(new TriStateItem("\u2610", desc.getKey(), desc.getValue()));
        }

        l.setModel(dlma);
        l.setVisibleRowCount(count);
        jListSetup(l);
    }

    private void jListSetup(JList<TriStateItem> l) {
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.setSelectionModel(new NoSelectionModel());
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList<TriStateItem> list = (JList<TriStateItem>) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });
    }

    private void toggleText(JList<TriStateItem> list, int index) {
        ListModel<TriStateItem> m = list.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);

            if (index == i) {
                if (ms.state.contains("\u2610")) {
                    ms.state = "\u2611";
                } else if (ms.state.contains("\u2611")) {
                    ms.state = "\u2612";
                } else if (ms.state.contains("\u2612")) {
                    ms.state = "\u2610";
                }
            }
        }

        list.setModel(m);
        list.repaint();
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

        clearTriStateItem(listArmorType);
        clearTriStateItem(listCockpitType);
        clearTriStateItem(listEngineType);
        clearTriStateItem(listGyroType);
        clearTriStateItem(listInternalsType);
        clearTriStateItem(listTechLevel);
        clearTriStateItem(listTechBase);
    }

    void clearTriStateItem(JList<TriStateItem> l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            ms.state = "\u2610";
        }

        l.setModel(m);
        l.repaint();
    }
}
