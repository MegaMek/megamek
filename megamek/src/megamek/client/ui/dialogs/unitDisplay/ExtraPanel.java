/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitDisplay;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.SliderDialog;
import megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.Configuration;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Sensor;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * This class shows information about a unit that doesn't belong elsewhere.
 */
class ExtraPanel extends PicMap implements ActionListener, ItemListener {
    private final UnitDisplayPanel unitDisplayPanel;

    private final JPanel panelMain;
    private final JLabel curSensorsL;
    private final JTextArea unusedR;
    private final JTextArea carriesR;
    private final JTextArea heatR;
    private final JTextArea lastTargetR;
    private final JTextArea sinksR;
    private final JButton sinks2B;
    private final JButton dumpBombs;
    private final JButton unitReadout;
    private final JList<String> narcList;
    private int myMekId;

    private final JComboBox<String> chSensors;

    private SliderDialog prompt;

    private int sinks;
    private boolean dontChange;

    JButton activateHidden = new JButton(Messages.getString("MekDisplay.ActivateHidden.Label"));

    MMComboBox<GamePhase> comboActivateHiddenPhase = new MMComboBox<>("comboActivateHiddenPhase");

    ExtraPanel(UnitDisplayPanel unitDisplayPanel) {
        this.unitDisplayPanel = unitDisplayPanel;
        prompt = null;

        JLabel narcLabel = new JLabel(Messages.getString("MekDisplay.AffectedBy"), SwingConstants.CENTER);
        narcLabel.setOpaque(false);
        narcLabel.setForeground(Color.WHITE);

        narcList = new JList<>(new DefaultListModel<>());

        JLabel unusedL = new JLabel(Messages.getString("MekDisplay.UnusedSpace"), SwingConstants.CENTER);
        unusedL.setOpaque(false);
        unusedL.setForeground(Color.WHITE);
        unusedR = new JTextArea("", 2, 25);
        unusedR.setEditable(false);
        unusedR.setOpaque(false);
        unusedR.setForeground(Color.WHITE);

        JLabel carriesL = new JLabel(Messages.getString("MekDisplay.Carryng"), SwingConstants.CENTER);
        carriesL.setOpaque(false);
        carriesL.setForeground(Color.WHITE);
        carriesR = new JTextArea("", 4, 25);
        carriesR.setEditable(false);
        carriesR.setOpaque(false);
        carriesR.setForeground(Color.WHITE);

        JLabel sinksL = new JLabel(
              Messages.getString("MekDisplay.activeSinksLabel"),
              SwingConstants.CENTER);
        sinksL.setOpaque(false);
        sinksL.setForeground(Color.WHITE);
        sinksR = new JTextArea("", 1, 25);
        sinksR.setEditable(false);
        sinksR.setOpaque(false);
        sinksR.setForeground(Color.WHITE);

        sinks2B = new JButton(
              Messages.getString("MekDisplay.configureActiveSinksLabel"));
        sinks2B.setActionCommand("changeSinks");
        sinks2B.addActionListener(this);

        dumpBombs = new JButton(Messages.getString("MekDisplay.DumpBombsLabel"));
        dumpBombs.setActionCommand("dumpBombs");
        dumpBombs.addActionListener(this);

        JLabel heatL = new JLabel(Messages.getString("MekDisplay.HeatEffects"), SwingConstants.CENTER);
        heatL.setOpaque(false);
        heatL.setForeground(Color.WHITE);
        heatR = new JTextArea("", 4, 25);
        heatR.setEditable(false);
        heatR.setOpaque(false);
        heatR.setForeground(Color.WHITE);

        JLabel lblLastTarget = new JLabel(Messages.getString("MekDisplay.LastTarget"),
              SwingConstants.CENTER);
        lblLastTarget.setForeground(Color.WHITE);
        lblLastTarget.setOpaque(false);
        lastTargetR = new JTextArea("", 4, 25);
        lastTargetR.setLineWrap(true);
        lastTargetR.setWrapStyleWord(true);
        lastTargetR.setEditable(false);
        lastTargetR.setOpaque(false);
        lastTargetR.setForeground(Color.WHITE);

        curSensorsL = new JLabel(Messages.getString("MekDisplay.CurrentSensors").concat(" "),
              SwingConstants.CENTER);
        curSensorsL.setForeground(Color.WHITE);
        curSensorsL.setOpaque(false);

        chSensors = new JComboBox<>();
        chSensors.addItemListener(this);

        activateHidden.setToolTipText(Messages.getString("MekDisplay.ActivateHidden.ToolTip"));
        comboActivateHiddenPhase.setToolTipText(Messages.getString("MekDisplay.ActivateHiddenPhase.ToolTip"));
        activateHidden.addActionListener(this);
        comboActivateHiddenPhase.addItem(GamePhase.UNKNOWN);
        comboActivateHiddenPhase.addItem(GamePhase.MOVEMENT);
        comboActivateHiddenPhase.addItem(GamePhase.FIRING);
        comboActivateHiddenPhase.addItem(GamePhase.PHYSICAL);
        comboActivateHiddenPhase.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                      (((value instanceof GamePhase) && ((GamePhase) value).isUnknown())
                            ? Messages.getString("MekDisplay.ActivateHidden.StopActivating")
                            : value),
                      index, isSelected, cellHasFocus);
            }
        });

        unitReadout = new JButton(Messages.getString("MekDisplay.UnitReadout"));
        unitReadout.setActionCommand("UnitReadout");
        unitReadout.addActionListener(this);

        // layout choice panel
        GridBagLayout gridBagLayout;
        GridBagConstraints c;

        gridBagLayout = new GridBagLayout();
        c = new GridBagConstraints();
        panelMain = new JPanel(gridBagLayout);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 9, 1, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weighty = 0;
        c.gridy = 0;
        c.gridx = 0;
        panelMain.add(curSensorsL, c);
        c.gridy++;
        panelMain.add(chSensors, c);

        c.gridy++;
        panelMain.add(narcLabel, c);
        c.gridy++;
        c.insets = new Insets(1, 9, 1, 9);
        JScrollPane scrollPane = new JScrollPane(narcList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panelMain.add(scrollPane, c);

        c.gridy++;
        panelMain.add(unusedL, c);
        c.gridy++;
        panelMain.add(unusedR, c);

        c.gridy++;
        panelMain.add(carriesL, c);
        c.gridy++;
        panelMain.add(carriesR, c);

        c.gridy++;
        panelMain.add(dumpBombs, c);

        c.gridy++;
        panelMain.add(sinksL, c);
        c.gridy++;
        panelMain.add(sinksR, c);
        c.gridy++;
        panelMain.add(sinks2B, c);

        c.gridy++;
        panelMain.add(heatL, c);
        c.gridy++;
        c.insets = new Insets(1, 9, 5, 9);
        panelMain.add(heatR, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        panelMain.add(lblLastTarget, c);
        c.gridy++;
        c.insets = new Insets(1, 9, 5, 9);
        panelMain.add(lastTargetR, c);

        c.gridy++;
        c.insets = new Insets(1, 9, 6, 9);
        panelMain.add(activateHidden, c);
        c.gridy++;
        panelMain.add(comboActivateHiddenPhase, c);

        c.gridy++;
        panelMain.add(unitReadout, c);

        c.weightx = 1;
        c.weighty = 1;
        panelMain.add(new Label(" "), c);

        setLayout(new BorderLayout());
        add(panelMain, BorderLayout.NORTH);
        panelMain.setOpaque(false);

        setBackGround();
        onResize();
    }

    @Override
    public void onResize() {
        int width = getSize().width;
        Rectangle contentBounds = getContentBounds();
        if (contentBounds == null) {
            return;
        }
        int dx = Math.round(((width - contentBounds.width) / 2.0f));
        int minLeftMargin = 8;
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = 8;
        setContentMargins(dx, dy, dx, dy);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

    }

    /**
     * updates fields for the specified mek
     */
    public void displayMek(Entity en) {
        // Clear the "Affected By" list.
        ((DefaultListModel<String>) narcList.getModel()).removeAllElements();
        sinks = 0;
        myMekId = en.getId();
        ClientGUI clientgui = unitDisplayPanel.getClientGUI();
        if ((clientgui != null) && (clientgui.getClient().getLocalPlayer().getId() != en.getOwnerId())) {
            sinks2B.setEnabled(false);
            dumpBombs.setEnabled(false);
            chSensors.setEnabled(false);
            dontChange = true;
        } else {
            sinks2B.setEnabled(true);
            dumpBombs.setEnabled(false);
            chSensors.setEnabled(true);
            dontChange = false;
        }

        // Walk through the list of teams. There
        // can't be more teams than players.
        StringBuilder buff;
        if (clientgui != null) {
            Game game = clientgui.getClient().getGame();
            GameOptions gameOptions = game.getOptions();

            for (Player player : game.getPlayersList()) {
                int team = player.getTeam();
                if (en.isNarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuilder(Messages.getString("MekDisplay.NARCedBy"));
                    buff.append(player.getName())
                          .append(" [").append(Player.TEAM_NAMES[team]).append(']');
                    ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
                }

                if (en.isINarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuilder(Messages.getString("MekDisplay.INarcHoming"));
                    buff.append(player.getName()).append(" [")
                          .append(Player.TEAM_NAMES[team]).append("] ")
                          .append(Messages.getString("MekDisplay.attached"))
                          .append('.');
                    ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
                }
            }

            if (en.isINarcedWith(INarcPod.ECM)) {
                buff = new StringBuilder(Messages.getString("MekDisplay.iNarcECMPodAttached"));
                ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
            }

            if (en.isINarcedWith(INarcPod.HAYWIRE)) {
                buff = new StringBuilder(Messages.getString("MekDisplay.iNarcHaywirePodAttached"));
                ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
            }

            if (en.isINarcedWith(INarcPod.NEMESIS)) {
                buff = new StringBuilder(Messages.getString("MekDisplay.iNarcNemesisPodAttached"));
                ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
            }

            // Show inferno track.
            if (en.infernos.isStillBurning()) {
                buff = new StringBuilder(Messages.getString("MekDisplay.InfernoBurnRemaining"));
                buff.append(en.infernos.getTurnsLeftToBurn());
                ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
            }

            if ((en instanceof Tank) && ((Tank) en).isOnFire()) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.OnFire"));
            }

            // Show electromagnetic interference.
            if (en.isSufferingEMI()) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.IsEMId"));
            }

            // Show ECM affect.
            Coords pos = en.getPosition();
            if (ComputeECM.isAffectedByAngelECM(en, pos, pos)) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.InEnemyAngelECMField"));
            } else if (ComputeECM.isAffectedByECM(en, pos, pos)) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.InEnemyECMField"));
            }

            // Active Stealth Armor? If yes, we're under ECM
            if (en.isStealthActive()
                  && ((en instanceof Mek) || (en instanceof Tank))) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.UnderStealth"));
            }

            // burdened due to unjettisoned body-mounted missiles on BA?
            if ((en instanceof BattleArmor) && ((BattleArmor) en).isBurdened()) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.Burdened"));
            }

            // suffering from taser feedback?
            if (en.getTaserFeedBackRounds() > 0) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(en.getTaserFeedBackRounds()
                            + " " + Messages.getString("MekDisplay.TaserFeedBack"));
            }

            // taser interference?
            if (en.getTaserInterference() > 0) {
                ((DefaultListModel<String>) narcList.getModel()).addElement("+"
                      + en.getTaserInterference() + " "
                      + Messages.getString("MekDisplay.TaserInterference"));
            }

            // suffering from TSEMP Interference?
            if (en.getTsempEffect() == MMConstants.TSEMP_EFFECT_INTERFERENCE) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.TSEMPInterference"));
            }

            // suffering from EMP Mine Interference?
            if (en.getEMPInterferenceRounds() > 0) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.EMPInterference",
                            en.getEMPInterferenceRounds()));
            }

            // suffering from EMP Mine Shutdown?
            if (en.getEMPShutdownRounds() > 0) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.EMPShutdown",
                            en.getEMPShutdownRounds()));
            }

            if (en.hasDamagedRHS()) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.RHSDamaged"));
            }

            // Show Turret Locked.
            if ((en instanceof Tank) && !((Tank) en).hasNoTurret()
                  && !en.canChangeSecondaryFacing()) {
                ((DefaultListModel<String>) narcList.getModel())
                      .addElement(Messages.getString("MekDisplay.Turretlocked"));
            }

            // Show jammed weapons.
            for (Mounted<?> weapon : en.getWeaponList()) {
                if (weapon.isJammed()) {
                    buff = new StringBuilder(weapon.getName());
                    buff.append(Messages.getString("MekDisplay.isJammed"));
                    ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
                }
            }

            // Show breached locations.
            for (int loc = 0; loc < en.locations(); loc++) {
                if (en.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    buff = new StringBuilder(en.getLocationName(loc));
                    buff.append(Messages.getString("MekDisplay.Breached"));
                    ((DefaultListModel<String>) narcList.getModel()).addElement(buff.toString());
                }
            }

            if (narcList.getModel().getSize() == 0) {
                ((DefaultListModel<String>) narcList.getModel()).addElement(" ");
            }

            // transport values
            String unused = en.getUnusedString();
            if (unused.isBlank()) {
                unused = Messages.getString("MekDisplay.None");
            }
            unusedR.setText(unused);
            carriesR.setText(null);
            // boolean hasText = false;
            for (Entity other : en.getLoadedUnits()) {
                carriesR.append(other.getShortName());
                carriesR.append("\n");
            }

            // Show club(s).
            for (Mounted<?> club : en.getClubs()) {
                carriesR.append(club.getName());
                carriesR.append("\n");
            }

            // show cargo.
            for (ICarryable cargo : en.getDistinctCarriedObjects()) {
                carriesR.append(cargo.specificName());
                carriesR.append("\n");
            }

            // We may not be saving captured pilots correctly on game save; some valid pilots don't have
            // entities.
            for (int pickedUpID : en.getPickedUpMekWarriors()) {
                Entity carried = game.getEntity(pickedUpID);
                carriesR.append((carried == null) ? "(ID " + pickedUpID + ")" : carried.getShortName());
                carriesR.append("\n");
            }

            // Show searchlight
            if (en.hasSearchlight()) {
                if (en.isUsingSearchlight()) {
                    carriesR.append(Messages.getString("MekDisplay.SearchlightOn"));
                } else {
                    carriesR.append(Messages.getString("MekDisplay.SearchlightOff"));
                }
            }

            // Show Heat Effects, but only for Meks.
            heatR.setText("");
            sinksR.setText("");

            if (en instanceof Mek m) {
                sinks2B.setEnabled(!dontChange);
                sinks = m.getActiveSinksNextRound();
                if (m.hasDoubleHeatSinks()) {
                    sinksR.append(Messages.getString("MekDisplay.activeSinksTextDouble",
                          sinks, sinks * 2));
                } else {
                    sinksR.append(Messages.getString("MekDisplay.activeSinksTextSingle", sinks));
                }

                if (m.hasRiscHeatSinkOverrideKit()) {
                    sinksR.append(Messages.getString("MekDisplay.RiscKit"));
                }

                boolean hasTSM = false;
                boolean mtHeat = false;
                if (m.hasTSM(false)) {
                    hasTSM = true;
                }

                if (gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT)) {
                    mtHeat = true;
                }
                heatR.setForeground(GUIPreferences.getInstance().getColorForHeat(en.heat));
                heatR.append(HeatEffects.getHeatEffects(en.heat, mtHeat, hasTSM));
            } else {
                // Non-Meks cannot configure their heat sinks
                sinks2B.setEnabled(false);
            }

            dumpBombs.setEnabled(false);

            refreshSensorChoices(en);

            if (null != en.getActiveSensor()) {
                String sensorDesc = "";
                if (gameOptions.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS)
                      || (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS))
                      && en.isSpaceborne()) {
                    sensorDesc = UnitToolTip.getSensorDesc(en);
                }
                String tmpStr = Messages.getString("MekDisplay.CurrentSensors") + " " + sensorDesc;
                tmpStr = String.format("<html><div WIDTH=%d>%s</div></html>", 250, tmpStr);
                curSensorsL.setText(tmpStr);
            } else {
                curSensorsL.setText((Messages.getString("MekDisplay.CurrentSensors")).concat(" "));
            }
        }

        if (en.getLastTarget() != Entity.NONE) {
            lastTargetR.setText(en.getLastTargetDisplayName());
        } else {
            lastTargetR.setText(Messages.getString("MekDisplay.None"));
        }

        activateHidden.setEnabled(!dontChange && en.isHidden());
        comboActivateHiddenPhase.setEnabled(!dontChange && en.isHidden());

        onResize();
    }

    private void refreshSensorChoices(Entity en) {
        chSensors.removeItemListener(this);
        chSensors.removeAllItems();
        for (int i = 0; i < en.getSensors().size(); i++) {
            Sensor sensor = en.getSensors().elementAt(i);
            String condition = "";
            if (sensor.isBAP() && !en.hasBAP(false)) {
                condition = " (Disabled)";
            }
            chSensors.addItem(sensor.getDisplayName() + condition);
            if ((en.getNextSensor() != null) && (sensor.type() == en.getNextSensor().type())) {
                chSensors.setSelectedIndex(i);
            }
        }
        chSensors.addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent ev) {
        ClientGUI clientgui = unitDisplayPanel.getClientGUI();
        if (clientgui == null) {
            return;
        }
        // Only act when a new item is selected
        if (ev.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        if ((ev.getItemSelectable() == chSensors)) {
            int sensorIdx = chSensors.getSelectedIndex();
            Entity entity = clientgui.getClient().getGame().getEntity(myMekId);

            if (entity != null) {
                Sensor sensor = entity.getSensors().elementAt(sensorIdx);
                entity.setNextSensor(sensor);
                refreshSensorChoices(entity);
                String sensorMsg = Messages.getString("MekDisplay.willSwitchAtEnd",
                      "Active Sensors",
                      sensor.getDisplayName());
                clientgui.systemMessage(sensorMsg);
                clientgui.getClient().sendSensorChange(myMekId, sensorIdx);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        ClientGUI clientgui = unitDisplayPanel.getClientGUI();
        if (clientgui == null) {
            return;
        }
        if ("changeSinks".equals(ae.getActionCommand()) && !dontChange) {
            Entity mekEntity = clientgui.getClient().getGame().getEntity(myMekId);

            if (mekEntity instanceof Mek mek) {
                prompt = new SliderDialog(clientgui.getFrame(),
                      Messages.getString("MekDisplay.changeSinks"),
                      Messages.getString("MekDisplay.changeSinks"), sinks,
                      0, mek.getNumberOfSinks());

                if (!prompt.showDialog()) {
                    return;
                }

                clientgui.getMenuBar().actionPerformed(ae);
                int numActiveSinks = prompt.getValue();

                mek.setActiveSinksNextRound(numActiveSinks);
                clientgui.getClient().sendSinksChange(myMekId, numActiveSinks);
                displayMek(mek);
            }
        } else if (activateHidden.equals(ae.getSource()) && !dontChange) {
            final GamePhase phase = comboActivateHiddenPhase.getSelectedItem();
            clientgui.getClient().sendActivateHidden(myMekId, (phase == null) ? GamePhase.UNKNOWN : phase);
        } else if (unitReadout.equals(ae.getSource())) {
            Entity entity = clientgui.getClient().getGame().getEntity(myMekId);
            LobbyUtility.mekReadout(entity, 0, false, clientgui.getFrame());
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension labelPrefSize = panelMain.getPreferredSize();
        Insets insets = getInsets();
        int height = labelPrefSize.height + insets.top + insets.bottom + 20;
        Dimension superPref = super.getPreferredSize();
        return new Dimension(superPref.width, Math.max(height, superPref.height));
    }
}
