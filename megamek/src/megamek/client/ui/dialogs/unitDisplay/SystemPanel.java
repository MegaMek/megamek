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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.ChoiceDialog;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.Configuration;
import megamek.common.CriticalSlot;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * This class shows the critical hits and systems for a mek
 */
class SystemPanel extends PicMap
      implements ItemListener, ActionListener, ListSelectionListener {

    private static final int LOC_ALL_EQUIP = 0;
    private static final int LOC_ALL_WEAPONS = 1;
    private static final int LOC_SPACER = 2;
    private static final int LOC_OFFSET = 3;

    private final UnitDisplayPanel unitDisplayPanel;

    @Serial
    private static final long serialVersionUID = 6660316427898323590L;

    private final JList<String> slotList;
    private final JList<String> locList;
    private final JList<String> unitList;

    private final JComboBox<String> m_chMode;
    private final JButton m_bDumpAmmo;

    private Entity en;
    private final Vector<Entity> entities = new Vector<>();

    SystemPanel(UnitDisplayPanel unitDisplayPanel) {
        this.unitDisplayPanel = unitDisplayPanel;
        JLabel locLabel = new JLabel(Messages.getString("MekDisplay.Location"), SwingConstants.CENTER);
        locLabel.setOpaque(false);
        locLabel.setForeground(Color.WHITE);
        JLabel slotLabel = new JLabel(Messages.getString("MekDisplay.Slot"), SwingConstants.CENTER);
        slotLabel.setOpaque(false);
        slotLabel.setForeground(Color.WHITE);

        JLabel unitLabel = new JLabel(Messages.getString("MekDisplay.Unit"), SwingConstants.CENTER);
        unitLabel.setOpaque(false);
        unitLabel.setForeground(Color.WHITE);

        locList = new JList<>(new DefaultListModel<>());
        locList.setOpaque(false);
        locList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        slotList = new JList<>(new DefaultListModel<>());
        slotList.setOpaque(false);
        slotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        unitList = new JList<>(new DefaultListModel<>());
        unitList.setOpaque(false);

        m_chMode = new JComboBox<>();
        m_chMode.addItem("   ");
        m_chMode.setEnabled(false);

        m_bDumpAmmo = new JButton(Messages.getString("MekDisplay.m_bDumpAmmo"));
        m_bDumpAmmo.setEnabled(false);
        m_bDumpAmmo.setActionCommand("dump");

        JLabel modeLabel = new JLabel(Messages.getString("MekDisplay.modeLabel"), SwingConstants.RIGHT);
        modeLabel.setOpaque(false);
        modeLabel.setForeground(Color.WHITE);

        // layout main panel
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        JPanel panelMain = new JPanel(gridBagLayout);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridBagLayout.setConstraints(locLabel, c);
        panelMain.add(locLabel);

        c.weightx = 0.0;
        c.gridy = 0;
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(15, 1, 1, 9);
        gridBagLayout.setConstraints(slotLabel, c);
        panelMain.add(slotLabel);

        c.weightx = 0.5;
        // c.weighty = 1.0;
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = 1;
        gridBagLayout.setConstraints(locList, c);
        panelMain.add(locList);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 2;
        c.gridx = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridBagLayout.setConstraints(unitLabel, c);
        panelMain.add(unitLabel);

        c.weightx = 0.5;
        // c.weighty = 1.0;
        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = GridBagConstraints.REMAINDER;
        gridBagLayout.setConstraints(unitList, c);
        panelMain.add(unitList);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 9);
        JScrollPane tSlotScroll = new JScrollPane(slotList);
        tSlotScroll.setMinimumSize(new Dimension(200, 100));
        gridBagLayout.setConstraints(tSlotScroll, c);
        panelMain.add(tSlotScroll);

        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridBagLayout.setConstraints(modeLabel, c);
        c.insets = new Insets(1, 1, 1, 1);
        panelMain.add(modeLabel);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = 2;
        c.gridx = 2;
        c.insets = new Insets(1, 1, 1, 9);
        gridBagLayout.setConstraints(m_chMode, c);
        panelMain.add(m_chMode);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 3;
        c.gridx = 1;
        c.insets = new Insets(4, 4, 15, 9);
        gridBagLayout.setConstraints(m_bDumpAmmo, c);
        panelMain.add(m_bDumpAmmo);

        setLayout(new BorderLayout());
        add(panelMain);
        panelMain.setOpaque(false);

        setBackGround();
        onResize();

        addListeners();
    }

    @Override
    public void onResize() {
        int w = getSize().width;
        Rectangle r = getContentBounds();
        if (r == null) {
            return;
        }
        int dx = Math.round(((w - r.width) / 2.0f));
        int minLeftMargin = 8;
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = 8;
        setContentMargins(dx, dy, dx, dy);
    }

    private CriticalSlot getSelectedCritical() {
        if ((locList.getSelectedIndex() == LOC_ALL_EQUIP)
              || (locList.getSelectedIndex() == LOC_ALL_WEAPONS)
              || (locList.getSelectedIndex() == LOC_SPACER)) {
            return null;
        }
        int loc = locList.getSelectedIndex();
        int slot = slotList.getSelectedIndex();
        loc -= LOC_OFFSET;
        if ((loc == -1) || (slot == -1)) {
            return null;
        }
        return en.getCritical(loc, slot);
    }

    private Mounted<?> getSelectedEquipment() {
        if ((locList.getSelectedIndex() == LOC_ALL_EQUIP)) {
            if (slotList.getSelectedIndex() != -1) {
                return en.getMisc().get(slotList.getSelectedIndex());
            } else {
                return null;
            }
        }
        if (locList.getSelectedIndex() == LOC_ALL_WEAPONS) {
            if (slotList.getSelectedIndex() != -1) {
                return en.getWeaponList().get(slotList.getSelectedIndex());
            } else {
                return null;
            }
        }

        final CriticalSlot cs = getSelectedCritical();
        if ((cs == null) || (unitDisplayPanel.getClientGUI() == null)) {
            return null;
        }
        if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
            return null;
        }
        if ((cs.getMount().getType() instanceof MiscType)
              && cs.getMount().getType().hasFlag(MiscType.F_BOMB_BAY)) {
            Mounted<?> m = cs.getMount();
            while (m.getLinked() != null) {
                m = m.getLinked();
            }
            return m;
        }
        if (cs.getMount2() != null) {
            ChoiceDialog choiceDialog = new ChoiceDialog(unitDisplayPanel.getClientGUI().getFrame(),
                  Messages.getString("MekDisplay.SelectMulti.title"),
                  Messages.getString("MekDisplay.SelectMulti.question"),
                  new String[] { cs.getMount().getName(),
                                 cs.getMount2().getName() },
                  true);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer()) {
                // load up the choices
                int[] toDump = choiceDialog.getChoices();
                if (toDump[0] == 0) {
                    return cs.getMount();
                } else {
                    return cs.getMount2();
                }
            }
        }
        return cs.getMount();
    }

    private Entity getSelectedEntity() {
        int unit = unitList.getSelectedIndex();
        if ((unit == -1) || (unit > entities.size())) {
            return null;
        }
        return entities.elementAt(unit);
    }

    /**
     * updates fields for the specified mek
     */
    public void displayMek(Entity newEntity) {
        en = newEntity;
        entities.clear();
        entities.add(newEntity);
        removeListeners();
        ((DefaultListModel<String>) unitList.getModel())
              .removeAllElements();
        ((DefaultListModel<String>) unitList.getModel())
              .addElement(Messages.getString("MekDisplay.Ego"));
        for (Entity loadedUnit : newEntity.getLoadedUnits()) {
            ((DefaultListModel<String>) unitList.getModel())
                  .addElement(loadedUnit.getModel());
            entities.add(loadedUnit);
        }
        unitList.setSelectedIndex(0);
        displayLocations();
        addListeners();
    }

    public void selectLocation(int loc) {
        locList.setSelectedIndex(loc + LOC_OFFSET);
    }

    private void displayLocations() {
        DefaultListModel<String> locModel = ((DefaultListModel<String>) locList
              .getModel());
        locModel.removeAllElements();
        locModel.insertElementAt(
              Messages.getString("MekDisplay.AllEquipment"), LOC_ALL_EQUIP);
        locModel.insertElementAt(
              Messages.getString("MekDisplay.AllWeapons"), LOC_ALL_WEAPONS);
        locModel.insertElementAt("-----", LOC_SPACER);
        for (int loc = 0; loc < en.locations(); loc++) {
            int idx = loc + LOC_OFFSET;
            if (en.getNumberOfCriticalSlots(loc) > 0) {
                locModel.insertElementAt(en.getLocationName(loc), idx);
            }
        }
        locList.setSelectedIndex(0);
        displaySlots();
    }

    private void displaySlots() {
        int loc = locList.getSelectedIndex();
        DefaultListModel<String> slotModel = ((DefaultListModel<String>) slotList.getModel());
        slotModel.removeAllElements();

        // Display all Equipment
        if (loc == LOC_ALL_EQUIP) {
            for (Mounted<?> m : en.getMisc()) {
                slotModel.addElement(getMountedDisplay(m, loc));
            }
            return;
        }

        // Display all Weapons
        if (loc == LOC_ALL_WEAPONS) {
            for (Mounted<?> m : en.getWeaponList()) {
                slotModel.addElement(getMountedDisplay(m, loc));
            }
            return;
        }

        // Display nothing for a spacer
        if (loc == LOC_SPACER) {
            return;
        }

        // Standard location handling
        loc -= LOC_OFFSET;
        for (int i = 0; i < en.getNumberOfCriticalSlots(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            StringBuilder sb = new StringBuilder(32);
            if (cs == null) {
                sb.append("---");
            } else {
                switch (cs.getType()) {
                    case CriticalSlot.TYPE_SYSTEM:
                        if (cs.isDestroyed() || cs.isMissing()) {
                            sb.append("*");
                        }
                        if (cs.isBreached()) {
                            sb.append("x");
                        }
                        // ProtoMeks have different system names.
                        if (en instanceof ProtoMek) {
                            sb.append(ProtoMek.SYSTEM_NAMES[cs.getIndex()]);
                        } else {
                            sb.append(((Mek) en).getSystemName(cs
                                  .getIndex()));
                        }
                        break;
                    case CriticalSlot.TYPE_EQUIPMENT:
                        sb.append(getMountedDisplay(cs.getMount(), loc, cs));
                        break;
                    default:
                }
                if (cs.isArmored()) {
                    sb.append(" (armored)");
                }
            }
            slotModel.addElement(sb.toString());
        }
        onResize();
    }

    private String getMountedDisplay(Mounted<?> m, int loc) {
        return getMountedDisplay(m, loc, null);
    }

    private String getMountedDisplay(Mounted<?> m, int loc, CriticalSlot cs) {
        String hotLoaded = Messages.getString("MekDisplay.isHotLoaded");
        StringBuilder sb = new StringBuilder();

        sb.append(m.getDesc());

        if ((cs != null) && cs.getMount2() != null) {
            sb.append(" ");
            sb.append(cs.getMount2().getDesc());
        }

        if (m.isHotLoaded()) {
            sb.append(hotLoaded);
        }

        if (m.hasModes()) {
            if (!m.curMode().getDisplayableName().isEmpty()) {
                sb.append(" (");
                sb.append(m.curMode().getDisplayableName());
                sb.append(')');
            }

            if (!m.pendingMode().equals("None")) {
                sb.append(" (next turn, ");
                sb.append(m.pendingMode().getDisplayableName());
                sb.append(')');
            }

            if ((m instanceof MiscMounted) && ((MiscMounted) m).getType().isShield()) {
                sb.append(" ").append(((MiscMounted) m).getDamageAbsorption(en, m.getLocation())).append('/')
                      .append(((MiscMounted) m).getCurrentDamageCapacity(en, m.getLocation())).append(')');
            }
        }
        return sb.toString();
    }

    //
    // ItemListener
    //
    @Override
    public void itemStateChanged(ItemEvent ev) {
        removeListeners();
        try {
            ClientGUI clientgui = unitDisplayPanel.getClientGUI();
            if (clientgui == null) {
                return;
            }
            if (ev.getSource().equals(m_chMode)
                  && (ev.getStateChange() == ItemEvent.SELECTED)) {
                Mounted<?> m = getSelectedEquipment();
                CriticalSlot cs = getSelectedCritical();
                if ((m != null) && m.hasModes()) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {
                        if ((m.getType() instanceof MiscType miscType) &&
                              miscType.isBoobyTrap()) {
                            // Verify is it is in the correct phase to arm it
                            // This should be controlled by the equipment itself
                            // TODO: Refactor so the equipment knows the phase they can be armed/disarmed

                            if ((clientgui.getClient().getGame().getPhase().isFiring() ||
                                  clientgui.getClient().getGame().getPhase().isPhysical())) {
                                if (nMode == 1) {
                                    if (!clientgui.doYesNoDialog(Messages.getString("MekDisplay.BoobyTrapWarningTitle"),
                                          Messages.getString("MekDisplay.BoobyTrapWarning"))) {
                                        return;
                                    }
                                }
                            } else {
                                clientgui.doAlertDialog(Messages.getString("MekDisplay.BoobyTrapMode"),
                                      Messages.getString("MekDisplay.BoobyTrapMode"));
                                return;
                            }
                        }

                        if ((m.getType() instanceof MiscType)
                              && ((MiscType) m.getType()).isShield()
                              && !clientgui.getClient().getGame().getPhase().isFiring()) {
                            clientgui.systemMessage(Messages.getString("MekDisplay.ShieldModePhase"));
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                              && ((MiscType) m.getType()).isVibroblade()
                              && !clientgui.getClient().getGame().getPhase().isPhysical()) {
                            clientgui.systemMessage(Messages.getString("MekDisplay.VibrobladeModePhase"));
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                              && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)
                              && !clientgui.getClient().getGame().getPhase().isMovement()) {
                            clientgui.systemMessage(Messages.getString("MekDisplay.RetractableBladeModePhase"));
                            return;
                        }

                        // Can only charge a capacitor if the weapon has not been fired.
                        if ((m.getType() instanceof MiscType)
                              && (m.getLinked() != null)
                              && m.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                              && m.getLinked().isUsedThisRound()
                              && (nMode == 1)) {
                            clientgui.systemMessage(Messages.getString("MekDisplay.CapacitorCharging"));
                            return;
                        }

                        m.setMode(nMode);
                        // send the event to the server
                        clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), nMode);

                        // notify the player
                        if (m.canInstantSwitch(nMode)) {
                            clientgui.systemMessage(Messages.getString("MekDisplay.switched",
                                  m.getName(), m.curMode().getDisplayableName()));
                            int weapon = this.unitDisplayPanel.wPan.getSelectedWeaponNum();
                            this.unitDisplayPanel.wPan.displayMek(en);
                            this.unitDisplayPanel.wPan.selectWeapon(weapon);
                        } else {
                            if (clientgui.getClient().getGame().getPhase().isDeployment()) {
                                clientgui.systemMessage(Messages.getString("MekDisplay.willSwitchAtStart",
                                      m.getName(), m.pendingMode().getDisplayableName()));
                            } else {
                                clientgui.systemMessage(Messages.getString("MekDisplay.willSwitchAtEnd",
                                      m.getName(), m.pendingMode().getDisplayableName()));
                            }
                        }
                        int loc = slotList.getSelectedIndex();
                        displaySlots();
                        slotList.setSelectedIndex(loc);
                    }
                } else if ((cs != null)
                      && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {
                        if ((cs.getIndex() == Mek.SYSTEM_COCKPIT)
                              && en.hasEiCockpit() && (en instanceof Mek mek)) {
                            mek.setCockpitStatus(nMode);
                            clientgui.getClient().sendSystemModeChange(
                                  en.getId(), Mek.SYSTEM_COCKPIT, nMode);
                            if (mek.getCockpitStatus() == mek.getCockpitStatusNextRound()) {
                                clientgui.systemMessage(Messages.getString("MekDisplay.switched",
                                      "Cockpit", m_chMode.getSelectedItem()));
                            } else {
                                clientgui.systemMessage(Messages.getString("MekDisplay.willSwitchAtEnd",
                                      "Cockpit", m_chMode.getSelectedItem()));
                            }
                        }
                    }
                }
            }
            onResize();
        } finally {
            addListeners();
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        removeListeners();
        try {
            ClientGUI clientgui = unitDisplayPanel.getClientGUI();
            if (clientgui == null) {
                return;
            }
            if ("dump".equals(ae.getActionCommand())) {
                Mounted<?> m = getSelectedEquipment();
                boolean bOwner = clientgui.getClient().getLocalPlayer().equals(en.getOwner());
                if ((m == null) || !bOwner) {
                    return;
                }

                // Check for BA dumping SRM launchers
                if ((en instanceof BattleArmor) && (!m.isMissing())
                      && m.isBodyMounted()
                      && m.getType().hasFlag(WeaponType.F_MISSILE)
                      && (m.getLinked() != null)
                      && (m.getLinked().getUsableShotsLeft() > 0)) {
                    boolean isDumping = !m.isPendingDump();
                    m.setPendingDump(isDumping);
                    clientgui.getClient().sendModeChange(en.getId(),
                          en.getEquipmentNum(m), isDumping ? -1 : 0);
                    int selIdx = slotList.getSelectedIndex();
                    displaySlots();
                    slotList.setSelectedIndex(selIdx);
                }

                if (((!(m.getType() instanceof AmmoType) || (m.getUsableShotsLeft() <= 0))
                      && !m.isDWPMounted()) || (m.isDWPMounted() && m.isMissing())) {
                    return;
                }

                boolean bDumping;
                boolean bConfirmed;

                if (m.isPendingDump()) {
                    bDumping = false;
                    if (m.getType() instanceof AmmoType) {
                        String title = Messages.getString("MekDisplay.CancelDumping.title");
                        String body = Messages.getString("MekDisplay.CancelDumping.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    } else {
                        String title = Messages.getString("MekDisplay.CancelJettison.title");
                        String body = Messages.getString("MekDisplay.CancelJettison.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    }
                } else {
                    bDumping = true;
                    if (m.getType() instanceof AmmoType) {
                        String title = Messages.getString("MekDisplay.Dump.title");
                        String body = Messages.getString("MekDisplay.Dump.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    } else {
                        String title = Messages.getString("MekDisplay.Jettison.title");
                        String body = Messages.getString("MekDisplay.Jettison.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    }
                }

                if (bConfirmed) {
                    m.setPendingDump(bDumping);
                    clientgui.getClient().sendModeChange(en.getId(),
                          en.getEquipmentNum(m), bDumping ? -1 : 0);
                    int selIdx = slotList.getSelectedIndex();
                    displaySlots();
                    slotList.setSelectedIndex(selIdx);
                }
            }
            onResize();
        } finally {
            addListeners();
        }
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler
              .getUnitDisplaySkin();

        Image tile = getToolkit()
              .getImage(
                    new MegaMekFile(Configuration.widgetsDir(), udSpec
                          .getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine())
                    .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner())
                    .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec
                    .getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit()
              .getImage(
                    new MegaMekFile(Configuration.widgetsDir(), udSpec
                          .getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec
                    .getBottomRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        removeListeners();
        try {
            if (event.getSource().equals(unitList)) {
                if (null != getSelectedEntity()) {
                    en = getSelectedEntity();
                    ((DefaultComboBoxModel<String>) m_chMode.getModel())
                          .removeAllElements();
                    m_chMode.setEnabled(false);
                    displayLocations();
                }
            } else if (event.getSource().equals(locList)) {
                ((DefaultComboBoxModel<String>) m_chMode.getModel())
                      .removeAllElements();
                m_chMode.setEnabled(false);
                displaySlots();
            } else if (event.getSource().equals(slotList)
                  && (unitDisplayPanel.getClientGUI() != null)) {

                Client client = unitDisplayPanel.getClientGUI().getClient();
                m_bDumpAmmo.setEnabled(false);
                m_chMode.setEnabled(false);
                Mounted<?> mounted = getSelectedEquipment();
                boolean carryingBAsOnBack = (en instanceof Mek)
                      && ((en.getExteriorUnitAt(Mek.LOC_CENTER_TORSO, true) != null)
                      || (en.getExteriorUnitAt(Mek.LOC_LEFT_TORSO, true) != null) || (en
                      .getExteriorUnitAt(Mek.LOC_RIGHT_TORSO, true) != null));

                boolean invalidEnvironment = (en instanceof Mek)
                      && (en.getLocationStatus(Mek.LOC_CENTER_TORSO) > ILocationExposureStatus.NORMAL);

                if ((en instanceof Tank) && !(en instanceof GunEmplacement)
                      && (en.getLocationStatus(Tank.LOC_REAR) > ILocationExposureStatus.NORMAL)) {
                    invalidEnvironment = true;
                }

                ((DefaultComboBoxModel<String>) m_chMode.getModel())
                      .removeAllElements();
                boolean bOwner = client.getLocalPlayer().equals(en.getOwner());
                if ((mounted != null)
                      && bOwner
                      && (mounted.getType() instanceof AmmoType)
                      && !client.getGame().getPhase().isDeployment()
                      && !client.getGame().getPhase().isMovement()
                      && (mounted.getUsableShotsLeft() > 0)
                      && !mounted.isDumping()
                      && en.isActive()
                      && (client.getGame().getOptions().intOption(OptionsConstants.BASE_DUMPING_FROM_ROUND) <= client
                      .getGame().getRoundCount())
                      && !carryingBAsOnBack && !invalidEnvironment) {
                    m_bDumpAmmo.setEnabled(true);
                } else if ((mounted != null) && bOwner
                      && (mounted.getType() instanceof WeaponType)
                      && !mounted.isMissing() && mounted.isDWPMounted()) {
                    m_bDumpAmmo.setEnabled(true);
                    // Allow dumping of body-mounted missile launchers on BA
                } else if ((mounted != null) && bOwner
                      && (en instanceof BattleArmor)
                      && (mounted.getType() instanceof WeaponType)
                      && !mounted.isMissing() && mounted.isBodyMounted()
                      && mounted.getType().hasFlag(WeaponType.F_MISSILE)
                      && (mounted.getLinked() != null)
                      && (mounted.getLinked().getUsableShotsLeft() > 0)) {
                    m_bDumpAmmo.setEnabled(true);
                }
                int round = client.getGame().getRoundCount();
                boolean inSquadron = en.isPartOfFighterSquadron();
                if ((mounted != null) && bOwner && mounted.hasModes()) {
                    if (!mounted.isInoperable() && !mounted.isDumping()
                          && (en.isActive() || en.isActive(round) || inSquadron)
                          && mounted.isModeSwitchable()) {
                        m_chMode.setEnabled(true);
                    }
                    if (!mounted.isInoperable()
                          && (mounted.getType() instanceof MiscType)
                          && mounted.getType().hasFlag(MiscType.F_STEALTH)
                          && mounted.isModeSwitchable()) {
                        m_chMode.setEnabled(true);
                    }
                    // Nova CEWS has built-in "ECM"/"Off" modes and should always be switchable
                    if (!mounted.isInoperable()
                          && (mounted.getType() instanceof MiscType)
                          && mounted.getType().hasFlag(MiscType.F_NOVA)
                          && mounted.isModeSwitchable()) {
                        m_chMode.setEnabled(true);
                    } // if the max tech eccm option is not set then the ECM
                    // should not show anything.
                    // Exception: Nova CEWS has built-in "ECM"/"Off" modes and should always be switchable
                    if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_ECM)
                          && !mounted.getType().hasFlag(MiscType.F_NOVA)
                          && !(client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_ECCM)
                          || client.getGame().getOptions()
                          .booleanOption(OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET))) {
                        return;
                    }
                    for (Enumeration<EquipmentMode> modeEnumeration = mounted.getType()
                          .getModes(); modeEnumeration.hasMoreElements(); ) {
                        EquipmentMode equipmentMode = modeEnumeration.nextElement();
                        // Hack to prevent showing an option that is disabled by the server, but would
                        // be overwritten by every entity update if made also in the client
                        if (equipmentMode.equals("HotLoad") && en instanceof Mek
                              && !client.getGame().getOptions()
                              .booleanOption(OptionsConstants.ADVANCED_COMBAT_HOT_LOAD_IN_GAME)) {
                            continue;
                        }
                        m_chMode.addItem(equipmentMode.getDisplayableName());
                    }
                    if (m_chMode.getModel().getSize() <= 1) {
                        m_chMode.removeAllItems();
                        m_chMode.setEnabled(false);
                    } else {
                        if (mounted.pendingMode().equals("None")) {
                            m_chMode.setSelectedItem(mounted.curMode()
                                  .getDisplayableName());
                        } else {
                            m_chMode.setSelectedItem(mounted.pendingMode()
                                  .getDisplayableName());
                        }
                    }
                } else {
                    CriticalSlot cs = getSelectedCritical();
                    if ((cs != null)
                          && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        if ((cs.getIndex() == Mek.SYSTEM_COCKPIT)
                              && en.hasEiCockpit()
                              && (en instanceof Mek)) {
                            m_chMode.setEnabled(true);
                            m_chMode.addItem("EI Off");
                            m_chMode.addItem("EI On");
                            m_chMode.addItem("Aimed shot");
                            m_chMode.setSelectedItem(((Mek) en).getCockpitStatusNextRound());
                        }
                    }
                }
            }
            onResize();
        } finally {
            addListeners();
        }
    }

    private void addListeners() {
        locList.addListSelectionListener(this);
        slotList.addListSelectionListener(this);
        unitList.addListSelectionListener(this);

        m_chMode.addItemListener(this);
        m_bDumpAmmo.addActionListener(this);
    }

    private void removeListeners() {
        locList.removeListSelectionListener(this);
        slotList.removeListSelectionListener(this);
        unitList.removeListSelectionListener(this);

        m_chMode.removeItemListener(this);
        m_bDumpAmmo.removeActionListener(this);
    }
}
