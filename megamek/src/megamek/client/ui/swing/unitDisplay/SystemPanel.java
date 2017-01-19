package megamek.client.ui.swing.unitDisplay;

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
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ChoiceDialog;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Configuration;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentMode;
import megamek.common.IGame;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;

/**
 * This class shows the critical hits and systems for a mech
 */
class SystemPanel extends PicMap implements ItemListener, ActionListener,
        ListSelectionListener {
    
    private static int LOC_ALL_EQUIP = 0;
    private static int LOC_ALL_WEAPS = 1;
    private static int LOC_SPACER = 2;
    private static int LOC_OFFSET = 3;
    
    /**
     * 
     */
    private final UnitDisplay unitDisplay;

    /**
     *
     */
    private static final long serialVersionUID = 6660316427898323590L;

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
    private Vector<Entity> entities = new Vector<Entity>();

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    SystemPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        locLabel = new JLabel(
                Messages.getString("MechDisplay.Location"), SwingConstants.CENTER); //$NON-NLS-1$
        locLabel.setOpaque(false);
        locLabel.setForeground(Color.WHITE);
        slotLabel = new JLabel(
                Messages.getString("MechDisplay.Slot"), SwingConstants.CENTER); //$NON-NLS-1$
        slotLabel.setOpaque(false);
        slotLabel.setForeground(Color.WHITE);

        unitLabel = new JLabel(
                Messages.getString("MechDisplay.Unit"), SwingConstants.CENTER); //$NON-NLS-1$
        unitLabel.setOpaque(false);
        unitLabel.setForeground(Color.WHITE);

        locList = new JList<String>(new DefaultListModel<String>());
        locList.setOpaque(false);
        locList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        slotList = new JList<String>(new DefaultListModel<String>());
        slotList.setOpaque(false);
        slotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        unitList = new JList<String>(new DefaultListModel<String>());
        unitList.setOpaque(false);

        m_chMode = new JComboBox<String>();
        m_chMode.addItem("   "); //$NON-NLS-1$
        m_chMode.setEnabled(false);

        m_bDumpAmmo = new JButton(
                Messages.getString("MechDisplay.m_bDumpAmmo")); //$NON-NLS-1$
        m_bDumpAmmo.setEnabled(false);
        m_bDumpAmmo.setActionCommand("dump"); //$NON-NLS-1$

        modeLabel = new JLabel(
                Messages.getString("MechDisplay.modeLabel"), SwingConstants.RIGHT); //$NON-NLS-1$
        modeLabel.setOpaque(false);
        modeLabel.setForeground(Color.WHITE);
        // modeLabel.setEnabled(false);

        // layout main panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(locLabel, c);
        add(locLabel);

        c.weightx = 0.0;
        c.gridy = 0;
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(15, 1, 1, 9);
        gridbag.setConstraints(slotLabel, c);
        add(slotLabel);

        c.weightx = 0.5;
        // c.weighty = 1.0;
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = 1;
        gridbag.setConstraints(locList, c);
        add(locList);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 2;
        c.gridx = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(unitLabel, c);
        add(unitLabel);

        c.weightx = 0.5;
        // c.weighty = 1.0;
        c.gridy = 3;
        c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(unitList, c);
        add(unitList);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 9);
        JScrollPane tSlotScroll = new JScrollPane(slotList);
        tSlotScroll.setMinimumSize(new Dimension(200, 100));
        gridbag.setConstraints(tSlotScroll, c);
        add(tSlotScroll);

        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(modeLabel, c);
        c.insets = new Insets(1, 1, 1, 1);
        add(modeLabel);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = 2;
        c.gridx = 2;
        c.insets = new Insets(1, 1, 1, 9);
        gridbag.setConstraints(m_chMode, c);
        add(m_chMode);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 3;
        c.gridx = 1;
        c.insets = new Insets(4, 4, 15, 9);
        gridbag.setConstraints(m_bDumpAmmo, c);
        add(m_bDumpAmmo);

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
                sb.append("---"); //$NON-NLS-1$
            } else {
                switch (cs.getType()) {
                    case CriticalSlot.TYPE_SYSTEM:
                        if (cs.isDestroyed() || cs.isMissing()) {
                            sb.append("*"); //$NON-NLS-1$
                        }
                        if (cs.isBreached()) {
                            sb.append("x"); //$NON-NLS-1$
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
                    sb.append(" (armored)");  //$NON-NLS-1$
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
        String hotLoaded = Messages.getString("MechDisplay.isHotLoaded"); //$NON-NLS-1$
        StringBuffer sb = new StringBuffer();
        
        sb.append(m.getDesc());

        if ((cs != null) && cs.getMount2() != null) {
            sb.append(" "); //$NON-NLS-1$
            sb.append(cs.getMount2().getDesc()); //$NON-NLS-1$
        }
        if (m.isHotLoaded()) {
            sb.append(hotLoaded);
        }
        if (m.getType().hasModes()) {
            if (m.curMode().getDisplayableName().length() > 0) {
                sb.append(" ("); //$NON-NLS-1$
                sb.append(m.curMode().getDisplayableName());
                sb.append(')'); //$NON-NLS-1$
            }
            if (!m.pendingMode().equals("None")) { //$NON-NLS-1$
                sb.append(" (next turn, "); //$NON-NLS-1$
                sb.append(m.pendingMode().getDisplayableName());
                sb.append(')'); //$NON-NLS-1$
            }
            if ((m.getType() instanceof MiscType)
                    && ((MiscType) m.getType()).isShield()) {
                sb.append(" " //$NON-NLS-1$
                        + m.getDamageAbsorption(en, m.getLocation())
                        + '/' //$NON-NLS-1$
                        + m.getCurrentDamageCapacity(en,
                                m.getLocation()) + ')'); //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    //
    // ItemListener
    //
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
                if ((m != null) && m.getType().hasModes()) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType()).isShield()
                                && (clientgui.getClient().getGame()
                                        .getPhase() != IGame.Phase.PHASE_FIRING)) {
                            clientgui.systemMessage(Messages.getString(
                                    "MechDisplay.ShieldModePhase", null));//$NON-NLS-1$
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType()).isVibroblade()
                                && (clientgui.getClient().getGame().getPhase() 
                                        != IGame.Phase.PHASE_PHYSICAL)) {
                            clientgui.systemMessage(Messages.getString(
                                    "MechDisplay.VibrobladeModePhase", null));//$NON-NLS-1$
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType())
                                        .hasSubType(MiscType.S_RETRACTABLE_BLADE)
                                && (clientgui.getClient().getGame()
                                        .getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
                            clientgui
                                    .systemMessage(Messages
                                            .getString(
                                                    "MechDisplay.RetractableBladeModePhase",
                                                    null));//$NON-NLS-1$
                            return;
                        }

                        // Can only charge a capacitor if the weapon has not
                        // been fired.
                        if ((m.getType() instanceof MiscType)
                                && (m.getLinked() != null)
                                && ((MiscType) m.getType())
                                        .hasFlag(MiscType.F_PPC_CAPACITOR)
                                && m.getLinked().isUsedThisRound()
                                && (nMode == 1)) {
                            clientgui.systemMessage(Messages.getString(
                                    "MechDisplay.CapacitorCharging", null));//$NON-NLS-1$
                            return;
                        }
                        m.setMode(nMode);
                        // send the event to the server
                        clientgui.getClient().sendModeChange(en.getId(),
                                en.getEquipmentNum(m), nMode);

                        // notify the player
                        if (m.canInstantSwitch(nMode)) {
                            clientgui
                                    .systemMessage(Messages
                                            .getString(
                                                    "MechDisplay.switched",
                                                    new Object[] {
                                                            m.getName(),
                                                            m.curMode()
                                                                    .getDisplayableName() }));//$NON-NLS-1$
                            int weap = this.unitDisplay.wPan.getSelectedWeaponNum();
                            this.unitDisplay.wPan.displayMech(en);
                            this.unitDisplay.wPan.selectWeapon(weap);
                            // displaySlots();
                        } else {
                            if (IGame.Phase.PHASE_DEPLOYMENT == clientgui
                                    .getClient().getGame().getPhase()) {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.willSwitchAtStart",
                                                        new Object[] {
                                                                m.getName(),
                                                                m.pendingMode()
                                                                        .getDisplayableName() }));
                                //$NON-NLS-1$
                            } else {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.willSwitchAtEnd",
                                                        new Object[] {
                                                                m.getName(),
                                                                m.pendingMode()
                                                                        .getDisplayableName() }));
                                //$NON-NLS-1$
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
                                && en.hasEiCockpit()
                                && (en instanceof Mech)) {
                            Mech mech = (Mech) en;
                            mech.setCockpitStatus(nMode);
                            clientgui.getClient().sendSystemModeChange(
                                    en.getId(), Mech.SYSTEM_COCKPIT, nMode);
                            if (mech.getCockpitStatus() == mech
                                    .getCockpitStatusNextRound()) {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.switched",
                                                        new Object[] {
                                                                "Cockpit",
                                                                m_chMode.getSelectedItem() }));
                                //$NON-NLS-1$
                            } else {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.willSwitchAtEnd",
                                                        new Object[] {
                                                                "Cockpit",
                                                                m_chMode.getSelectedItem() }));
                                //$NON-NLS-1$
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

    // ActionListener
    public void actionPerformed(ActionEvent ae) {
        removeListeners();
        try {
            ClientGUI clientgui = unitDisplay.getClientGUI();
            if (clientgui == null) {
                return;
            }
            if ("dump".equals(ae.getActionCommand())) { //$NON-NLS-1$
                Mounted m = getSelectedEquipment();
                boolean bOwner = clientgui.getClient().getLocalPlayer()
                        .equals(en.getOwner());
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

                if (((!(m.getType() instanceof AmmoType) || (m
                        .getUsableShotsLeft() <= 0)) && !m.isDWPMounted())
                        || (m.isDWPMounted() && (m.isMissing() == true))) {
                    return;
                }

                boolean bDumping;
                boolean bConfirmed;

                if (m.isPendingDump()) {
                    bDumping = false;
                    if (m.getType() instanceof AmmoType) {
                        String title = Messages
                                .getString("MechDisplay.CancelDumping.title"); //$NON-NLS-1$
                        String body = Messages
                                .getString(
                                        "MechDisplay.CancelDumping.message", new Object[] { m.getName() }); //$NON-NLS-1$
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    } else {
                        String title = Messages
                                .getString("MechDisplay.CancelJettison.title"); //$NON-NLS-1$
                        String body = Messages
                                .getString(
                                        "MechDisplay.CancelJettison.message", new Object[] { m.getName() }); //$NON-NLS-1$
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    }
                } else {
                    bDumping = true;
                    if (m.getType() instanceof AmmoType) {
                        String title = Messages
                                .getString("MechDisplay.Dump.title"); //$NON-NLS-1$
                        String body = Messages
                                .getString(
                                        "MechDisplay.Dump.message", new Object[] { m.getName() }); //$NON-NLS-1$
                        bConfirmed = clientgui.doYesNoDialog(title, body);
                    } else {
                        String title = Messages
                                .getString("MechDisplay.Jettison.title"); //$NON-NLS-1$
                        String body = Messages
                                .getString(
                                        "MechDisplay.Jettison.message", new Object[] { m.getName() }); //$NON-NLS-1$
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
                        new File(Configuration.widgetsDir(), udSpec
                                .getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getTopLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getBottomLine())
                        .toString()); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getLeftLine())
                        .toString()); //$NON-NLS-1$
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getRightLine())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getTopLeftCorner())
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec
                        .getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit()
                .getImage(
                        new File(Configuration.widgetsDir(), udSpec
                                .getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec
                        .getBottomRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

    }

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

                if ((en instanceof Tank)
                        && (en.getLocationStatus(Tank.LOC_REAR) > ILocationExposureStatus.NORMAL)) {
                    invalidEnvironment = true;
                }

                ((DefaultComboBoxModel<String>) m_chMode.getModel())
                        .removeAllElements();
                boolean bOwner = client.getLocalPlayer().equals(en.getOwner());
                if ((m != null)
                        && bOwner
                        && (m.getType() instanceof AmmoType)
                        && (client.getGame().getPhase() != IGame.Phase.PHASE_DEPLOYMENT)
                        && (client.getGame().getPhase() != IGame.Phase.PHASE_MOVEMENT)
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
                if ((m != null) && bOwner && m.getType().hasModes()) {
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
                    if (m.getType().hasFlag(MiscType.F_ECM)
                            && !(client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_ECCM)
                                    || client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_GHOST_TARGET))) {
                        return;
                    }
                    for (Enumeration<EquipmentMode> e = m.getType()
                            .getModes(); e.hasMoreElements();) {
                        EquipmentMode em = e.nextElement();
                        m_chMode.addItem(em.getDisplayableName());
                    }
                    m_chMode.setSelectedItem(m.curMode()
                            .getDisplayableName());
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
                            m_chMode.setSelectedItem(new Integer(
                                    ((Mech) en).getCockpitStatusNextRound()));
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