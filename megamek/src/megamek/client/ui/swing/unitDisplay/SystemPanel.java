package megamek.client.ui.swing.unitDisplay;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ChoiceDialog;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.*;
import megamek.common.*;
import megamek.common.equipment.MiscMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class shows the critical hits and systems for a mech
 */
class SystemPanel extends PicMap implements ItemListener, ActionListener, ListSelectionListener, IPreferenceChangeListener {

    private static int LOC_ALL_EQUIP = 0;
    private static int LOC_ALL_WEAPS = 1;
    private static int LOC_SPACER = 2;
    private static int LOC_OFFSET = 3;

    private final UnitDisplay unitDisplay;

    private static final long serialVersionUID = 6660316427898323590L;

    private JPanel panelMain;
    private JScrollPane tSlotScroll;
    private JLabel locLabel;
    private JLabel slotLabel;
    private JLabel modeLabel;
    private JLabel unitLabel;
    private JList<String> slotList;
    private JList<String> locList;
    private JList<String> unitList;

    private JComboBox<String> m_chMode;
    private JButton m_bDumpAmmo;

    private Entity en;
    private Vector<Entity> entities = new Vector<>();

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    SystemPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        locLabel = new JLabel(Messages.getString("MechDisplay.Location"), SwingConstants.CENTER);
        locLabel.setOpaque(false);
        locLabel.setForeground(Color.WHITE);
        slotLabel = new JLabel(Messages.getString("MechDisplay.Slot"), SwingConstants.CENTER);
        slotLabel.setOpaque(false);
        slotLabel.setForeground(Color.WHITE);

        unitLabel = new JLabel(Messages.getString("MechDisplay.Unit"), SwingConstants.CENTER);
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

        m_bDumpAmmo = new JButton(Messages.getString("MechDisplay.m_bDumpAmmo"));
        m_bDumpAmmo.setEnabled(false);
        m_bDumpAmmo.setActionCommand("dump");

        modeLabel = new JLabel(Messages.getString("MechDisplay.modeLabel"), SwingConstants.RIGHT);
        modeLabel.setOpaque(false);
        modeLabel.setForeground(Color.WHITE);

        // layout main panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panelMain = new JPanel(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(locLabel, c);
        panelMain.add(locLabel);

        c.weightx = 0.0;
        c.gridy = 0;
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(15, 1, 1, 9);
        gridbag.setConstraints(slotLabel, c);
        panelMain.add(slotLabel);

        c.weightx = 0.5;
        // c.weighty = 1.0;
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = 1;
        gridbag.setConstraints(locList, c);
        panelMain.add(locList);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 2;
        c.gridx = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(unitLabel, c);
        panelMain.add(unitLabel);

        c.weightx = 0.5;
        // c.weighty = 1.0;
        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(unitList, c);
        panelMain.add(unitList);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 9);
        tSlotScroll = new JScrollPane(slotList);
        tSlotScroll.setMinimumSize(new Dimension(200, 100));
        gridbag.setConstraints(tSlotScroll, c);
        panelMain.add(tSlotScroll);

        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(modeLabel, c);
        c.insets = new Insets(1, 1, 1, 1);
        panelMain.add(modeLabel);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = 2;
        c.gridx = 2;
        c.insets = new Insets(1, 1, 1, 9);
        gridbag.setConstraints(m_chMode, c);
        panelMain.add(m_chMode);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 3;
        c.gridx = 1;
        c.insets = new Insets(4, 4, 15, 9);
        gridbag.setConstraints(m_bDumpAmmo, c);
        panelMain.add(m_bDumpAmmo);

        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);
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
        int dx = Math.round(((w - r.width) / 2));
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = minTopMargin;
        setContentMargins(dx, dy, dx, dy);
    }

    private CriticalSlot getSelectedCritical() {
        if ((locList.getSelectedIndex() == LOC_ALL_EQUIP)
                || (locList.getSelectedIndex() == LOC_ALL_WEAPS)
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

    private Mounted getSelectedEquipment() {
        if ((locList.getSelectedIndex() == LOC_ALL_EQUIP)) {
            if (slotList.getSelectedIndex() != -1) {
                return en.getMisc().get(slotList.getSelectedIndex());
            } else {
                return null;
            }
        }
        if (locList.getSelectedIndex() == LOC_ALL_WEAPS) {
            if (slotList.getSelectedIndex() != -1) {
                return en.getWeaponList().get(slotList.getSelectedIndex());
            } else {
                return null;
            }
        }

        final CriticalSlot cs = getSelectedCritical();
        if ((cs == null) || (unitDisplay.getClientGUI() == null)) {
            return null;
        }
        if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
            return null;
        }
        if ((cs.getMount().getType() instanceof MiscType)
                && cs.getMount().getType().hasFlag(MiscType.F_BOMB_BAY)) {
            Mounted m = cs.getMount();
            while (m.getLinked() != null) {
                m = m.getLinked();
            }
            return m;
        }
        if (cs.getMount2() != null) {
            ChoiceDialog choiceDialog = new ChoiceDialog(unitDisplay.getClientGUI().frame,
                    Messages.getString("MechDisplay.SelectMulti.title"),
                    Messages.getString("MechDisplay.SelectMulti.question"),
                    new String[] { cs.getMount().getName(),
                            cs.getMount2().getName() }, true);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer() == true) {
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
     * updates fields for the specified mech
     */
    public void displayMech(Entity newEntity) {
        en = newEntity;
        entities.clear();
        entities.add(newEntity);
        removeListeners();
        ((DefaultListModel<String>) unitList.getModel())
                .removeAllElements();
        ((DefaultListModel<String>) unitList.getModel())
                .addElement(Messages.getString("MechDisplay.Ego"));
        for (Entity loadee : newEntity.getLoadedUnits()) {
            ((DefaultListModel<String>) unitList.getModel())
                    .addElement(loadee.getModel());
            entities.add(loadee);
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
                Messages.getString("MechDisplay.AllEquipment"), LOC_ALL_EQUIP);
        locModel.insertElementAt(
                Messages.getString("MechDisplay.AllWeapons"), LOC_ALL_WEAPS);
        locModel.insertElementAt("-----", LOC_SPACER);
        for (int loc = 0; loc < en.locations(); loc++) {
            int idx = loc + LOC_OFFSET;
            if (en.getNumberOfCriticals(loc) > 0) {
                locModel.insertElementAt(en.getLocationName(loc), idx);
            }
        }
        locList.setSelectedIndex(0);
        displaySlots();
    }

    private void displaySlots() {
        int loc = locList.getSelectedIndex();
        DefaultListModel<String> slotModel =
                ((DefaultListModel<String>) slotList.getModel());
        slotModel.removeAllElements();

        // Display all Equipment
        if (loc == LOC_ALL_EQUIP) {
            for (Mounted m : en.getMisc()) {
                slotModel.addElement(getMountedDisplay(m, loc));
            }
            return;
        }

        // Display all Weapons
        if (loc == LOC_ALL_WEAPS) {
            for (Mounted m : en.getWeaponList()) {
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
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            StringBuffer sb = new StringBuffer(32);
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
                        // Protomechs have different system names.
                        if (en instanceof Protomech) {
                            sb.append(Protomech.systemNames[cs.getIndex()]);
                        } else {
                            sb.append(((Mech) en).getSystemName(cs
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

    private String getMountedDisplay(Mounted m, int loc) {
        return getMountedDisplay(m, loc, null);
    }

    private String getMountedDisplay(Mounted m, int loc, CriticalSlot cs) {
        String hotLoaded = Messages.getString("MechDisplay.isHotLoaded");
        StringBuffer sb = new StringBuffer();

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
            ClientGUI clientgui = unitDisplay.getClientGUI();
            if (clientgui == null) {
                return;
            }
            if (ev.getSource().equals(m_chMode)
                    && (ev.getStateChange() == ItemEvent.SELECTED)) {
                Mounted m = getSelectedEquipment();
                CriticalSlot cs = getSelectedCritical();
                if ((m != null) && m.hasModes()) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType()).isShield()
                                && !clientgui.getClient().getGame().getPhase().isFiring()) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.ShieldModePhase"));
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType()).isVibroblade()
                                && !clientgui.getClient().getGame().getPhase().isPhysical()) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.VibrobladeModePhase"));
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                                && m.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)
                                && !clientgui.getClient().getGame().getPhase().isMovement()) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.RetractableBladeModePhase"));
                            return;
                        }

                        // Can only charge a capacitor if the weapon has not been fired.
                        if ((m.getType() instanceof MiscType)
                                && (m.getLinked() != null)
                                && m.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                                && m.getLinked().isUsedThisRound()
                                && (nMode == 1)) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.CapacitorCharging"));
                            return;
                        }
                        m.setMode(nMode);
                        // send the event to the server
                        clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), nMode);

                        // notify the player
                        if (m.canInstantSwitch(nMode)) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.switched",
                                    m.getName(), m.curMode().getDisplayableName()));
                            int weap = this.unitDisplay.wPan.getSelectedWeaponNum();
                            this.unitDisplay.wPan.displayMech(en);
                            this.unitDisplay.wPan.selectWeapon(weap);
                        } else {
                            if (clientgui.getClient().getGame().getPhase().isDeployment()) {
                                clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtStart",
                                        m.getName(), m.pendingMode().getDisplayableName()));
                            } else {
                                clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtEnd",
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
                        if ((cs.getIndex() == Mech.SYSTEM_COCKPIT)
                                && en.hasEiCockpit() && (en instanceof Mech)) {
                            Mech mech = (Mech) en;
                            mech.setCockpitStatus(nMode);
                            clientgui.getClient().sendSystemModeChange(
                                    en.getId(), Mech.SYSTEM_COCKPIT, nMode);
                            if (mech.getCockpitStatus() == mech.getCockpitStatusNextRound()) {
                                clientgui.systemMessage(Messages.getString("MechDisplay.switched",
                                        "Cockpit", m_chMode.getSelectedItem()));
                            } else {
                                clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtEnd",
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
            ClientGUI clientgui = unitDisplay.getClientGUI();
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
                        String title = Messages.getString("MechDisplay.CancelDumping.title");
                        String body = Messages.getString("MechDisplay.CancelDumping.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    } else {
                        String title = Messages.getString("MechDisplay.CancelJettison.title");
                        String body = Messages.getString("MechDisplay.CancelJettison.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    }
                } else {
                    bDumping = true;
                    if (m.getType() instanceof AmmoType) {
                        String title = Messages.getString("MechDisplay.Dump.title");
                        String body = Messages.getString("MechDisplay.Dump.message", m.getName());
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    } else {
                        String title = Messages.getString("MechDisplay.Jettison.title");
                        String body = Messages.getString("MechDisplay.Jettison.message", m.getName());
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

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec
                        .getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit()
                .getImage(
                        new MegaMekFile(Configuration.widgetsDir(), udSpec
                                .getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
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
                    && (unitDisplay.getClientGUI() != null)) {

                Client client = unitDisplay.getClientGUI().getClient();
                m_bDumpAmmo.setEnabled(false);
                m_chMode.setEnabled(false);
                Mounted m = getSelectedEquipment();
                boolean carryingBAsOnBack = false;
                if ((en instanceof Mech)
                        && ((en.getExteriorUnitAt(Mech.LOC_CT, true) != null)
                                || (en.getExteriorUnitAt(Mech.LOC_LT, true) != null) || (en
                                .getExteriorUnitAt(Mech.LOC_RT, true) != null))) {
                    carryingBAsOnBack = true;
                }

                boolean invalidEnvironment = false;
                if ((en instanceof Mech)
                        && (en.getLocationStatus(Mech.LOC_CT) > ILocationExposureStatus.NORMAL)) {
                    invalidEnvironment = true;
                }

                if ((en instanceof Tank) && !(en instanceof GunEmplacement)
                        && (en.getLocationStatus(Tank.LOC_REAR) > ILocationExposureStatus.NORMAL)) {
                    invalidEnvironment = true;
                }

                ((DefaultComboBoxModel<String>) m_chMode.getModel())
                        .removeAllElements();
                boolean bOwner = client.getLocalPlayer().equals(en.getOwner());
                if ((m != null)
                        && bOwner
                        && (m.getType() instanceof AmmoType)
                        && !client.getGame().getPhase().isDeployment()
                        && !client.getGame().getPhase().isMovement()
                        && (m.getUsableShotsLeft() > 0)
                        && !m.isDumping()
                        && en.isActive()
                        && (client.getGame().getOptions().intOption(OptionsConstants.BASE_DUMPING_FROM_ROUND)
                                <= client.getGame().getRoundCount())
                        && !carryingBAsOnBack && !invalidEnvironment) {
                    m_bDumpAmmo.setEnabled(true);
                } else if ((m != null) && bOwner
                        && (m.getType() instanceof WeaponType)
                        && !m.isMissing() && m.isDWPMounted()) {
                    m_bDumpAmmo.setEnabled(true);
                    // Allow dumping of body-mounted missile launchers on BA
                } else if ((m != null) && bOwner
                        && (en instanceof BattleArmor)
                        && (m.getType() instanceof WeaponType)
                        && !m.isMissing() && m.isBodyMounted()
                        && m.getType().hasFlag(WeaponType.F_MISSILE)
                        && (m.getLinked() != null)
                        && (m.getLinked().getUsableShotsLeft() > 0)) {
                    m_bDumpAmmo.setEnabled(true);
                }
                int round = client.getGame().getRoundCount();
                boolean inSquadron = ((en instanceof Aero) && ((Aero) en)
                        .isInASquadron());
                if ((m != null) && bOwner && m.hasModes()) {
                    if (!m.isInoperable() && !m.isDumping()
                            && (en.isActive() || en.isActive(round) || inSquadron)
                            && m.isModeSwitchable()) {
                        m_chMode.setEnabled(true);
                    }
                    if (!m.isInoperable()
                            && (m.getType() instanceof MiscType)
                            && m.getType().hasFlag(MiscType.F_STEALTH)
                            && m.isModeSwitchable()) {
                        m_chMode.setEnabled(true);
                    }// if the maxtech eccm option is not set then the ECM
                     // should not show anything.
                    if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_ECM)
                            && !(client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_ECCM)
                                    || client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET))) {
                        return;
                    }
                    for (Enumeration<EquipmentMode> e = m.getType()
                            .getModes(); e.hasMoreElements();) {
                        EquipmentMode em = e.nextElement();
                        //Hack to prevent showing an option that is disabled by the server, but would
                        // be overwritten by every entity update if made also in the client
                        if (em.equals("HotLoad") && en instanceof Mech
                                && !client.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_HOTLOAD_IN_GAME)) {
                            continue;
                        }
                        m_chMode.addItem(em.getDisplayableName());
                    }
                    if (m_chMode.getModel().getSize() <= 1) {
                        m_chMode.removeAllItems();
                        m_chMode.setEnabled(false);
                    } else {
                        if (m.pendingMode().equals("None")) {
                            m_chMode.setSelectedItem(m.curMode()
                                    .getDisplayableName());
                        } else {
                            m_chMode.setSelectedItem(m.pendingMode()
                                    .getDisplayableName());
                        }
                    }
                } else {
                    CriticalSlot cs = getSelectedCritical();
                    if ((cs != null)
                            && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        if ((cs.getIndex() == Mech.SYSTEM_COCKPIT)
                                && en.hasEiCockpit()
                                && (en instanceof Mech)) {
                            m_chMode.setEnabled(true);
                            m_chMode.addItem("EI Off");
                            m_chMode.addItem("EI On");
                            m_chMode.addItem("Aimed shot");
                            m_chMode.setSelectedItem(((Mech) en).getCockpitStatusNextRound());
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

    private void adaptToGUIScale() {
        UIUtil.adjustContainer(panelMain, UIUtil.FONT_SCALE1);
        tSlotScroll.setMinimumSize(new Dimension(200, UIUtil.scaleForGUI(100)));
        tSlotScroll.setPreferredSize(new Dimension(200, UIUtil.scaleForGUI(100)));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }
}