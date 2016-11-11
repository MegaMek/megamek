package megamek.client.ui.swing.unitDisplay;

import java.awt.Color;
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

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.HeatEffects;
import megamek.client.ui.swing.Slider;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.BattleArmor;
import megamek.common.ComputeECM;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.ILocationExposureStatus;
import megamek.common.INarcPod;
import megamek.common.IPlayer;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Sensor;
import megamek.common.Tank;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.TSEMPWeapon;

/**
 * This class shows information about a unit that doesn't belong elsewhere.
 */
class ExtraPanel extends PicMap implements ActionListener, ItemListener {

    /**
     * 
     */
    private final UnitDisplay unitDisplay;

    /**
     *
     */
    private static final long serialVersionUID = -4907296187995261075L;

    private JLabel lblLastTarget;
    private JLabel curSensorsL;
    private JLabel narcLabel;
    private JLabel unusedL;
    private JLabel carrysL;
    private JLabel heatL;
    private JLabel sinksL;
    private JTextArea unusedR;
    private JTextArea carrysR;
    private JTextArea heatR;
    private JTextArea lastTargetR;
    private JTextArea sinksR;
    private JButton sinks2B;
    private JButton dumpBombs;
    private JList<String> narcList;
    private int myMechId;

    private JComboBox<String> chSensors;

    private Slider prompt;

    private int sinks;
    private boolean dontChange;

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    JButton activateHidden = new JButton(
            Messages.getString("MechDisplay.ActivateHidden.Label"));

    JComboBox<String> activateHiddenPhase = new JComboBox<>();

    ExtraPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        prompt = null;

        narcLabel = new JLabel(
                Messages.getString("MechDisplay.AffectedBy"), SwingConstants.CENTER); //$NON-NLS-1$
        narcLabel.setOpaque(false);
        narcLabel.setForeground(Color.WHITE);

        narcList = new JList<String>(new DefaultListModel<String>());

        // transport stuff
        // unusedL = new JLabel( "Unused Space:", JLabel.CENTER );

        unusedL = new JLabel(
                Messages.getString("MechDisplay.UnusedSpace"), SwingConstants.CENTER); //$NON-NLS-1$
        unusedL.setOpaque(false);
        unusedL.setForeground(Color.WHITE);
        unusedR = new JTextArea("", 2, 25); //$NON-NLS-1$
        unusedR.setEditable(false);
        unusedR.setOpaque(false);
        unusedR.setForeground(Color.WHITE);

        carrysL = new JLabel(
                Messages.getString("MechDisplay.Carryng"), SwingConstants.CENTER); //$NON-NLS-1$
        carrysL.setOpaque(false);
        carrysL.setForeground(Color.WHITE);
        carrysR = new JTextArea("", 4, 25); //$NON-NLS-1$
        carrysR.setEditable(false);
        carrysR.setOpaque(false);
        carrysR.setForeground(Color.WHITE);

        sinksL = new JLabel(
                Messages.getString("MechDisplay.activeSinksLabel"),
                SwingConstants.CENTER);
        sinksL.setOpaque(false);
        sinksL.setForeground(Color.WHITE);
        sinksR = new JTextArea("", 1, 25);
        sinksR.setEditable(false);
        sinksR.setOpaque(false);
        sinksR.setForeground(Color.WHITE);

        sinks2B = new JButton(
                Messages.getString("MechDisplay.configureActiveSinksLabel"));
        sinks2B.setActionCommand("changeSinks");
        sinks2B.addActionListener(this);

        dumpBombs = new JButton(
                Messages.getString("MechDisplay.DumpBombsLabel"));
        dumpBombs.setActionCommand("dumpBombs");
        dumpBombs.addActionListener(this);

        heatL = new JLabel(
                Messages.getString("MechDisplay.HeatEffects"), SwingConstants.CENTER); //$NON-NLS-1$
        heatL.setOpaque(false);
        heatL.setForeground(Color.WHITE);
        heatR = new JTextArea("", 4, 25); //$NON-NLS-1$
        heatR.setEditable(false);
        heatR.setOpaque(false);
        heatR.setForeground(Color.WHITE);
        
        lblLastTarget = new JLabel(
                Messages.getString("MechDisplay.LastTarget"),
                SwingConstants.CENTER);
        lblLastTarget.setForeground(Color.WHITE);
        lblLastTarget.setOpaque(false);
        lastTargetR = new JTextArea("", 4, 25); //$NON-NLS-1$
        lastTargetR.setEditable(false);
        lastTargetR.setOpaque(false);
        lastTargetR.setForeground(Color.WHITE);

        curSensorsL = new JLabel(
                (Messages.getString("MechDisplay.CurrentSensors"))
                        .concat(" "),
                SwingConstants.CENTER);
        curSensorsL.setForeground(Color.WHITE);
        curSensorsL.setOpaque(false);

        chSensors = new JComboBox<String>();
        chSensors.addItemListener(this);

        activateHidden.setToolTipText(Messages
                .getString("MechDisplay.ActivateHidden.ToolTip"));
        activateHiddenPhase.setToolTipText(Messages
                .getString("MechDisplay.ActivateHiddenPhase.ToolTip"));
        activateHidden.addActionListener(this);
        activateHiddenPhase.addItem(IGame.Phase
                .getDisplayableName(IGame.Phase.PHASE_MOVEMENT));
        activateHiddenPhase.addItem(IGame.Phase
                .getDisplayableName(IGame.Phase.PHASE_FIRING));
        activateHiddenPhase.addItem(IGame.Phase
                .getDisplayableName(IGame.Phase.PHASE_PHYSICAL));
        activateHiddenPhase.addItem(Messages
                .getString("MechDisplay.ActivateHidden.StopActivating"));

        // layout choice panel
        GridBagLayout gridbag;
        GridBagConstraints c;

        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 1.0;

        gridbag.setConstraints(curSensorsL, c);
        add(curSensorsL);

        gridbag.setConstraints(chSensors, c);
        add(chSensors);

        gridbag.setConstraints(narcLabel, c);
        add(narcLabel);

        c.insets = new Insets(1, 9, 1, 9);
        JScrollPane scrollPane = new JScrollPane(narcList);
        scrollPane
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gridbag.setConstraints(scrollPane, c);
        add(scrollPane);

        gridbag.setConstraints(unusedL, c);
        add(unusedL);

        gridbag.setConstraints(unusedR, c);
        add(unusedR);

        gridbag.setConstraints(carrysL, c);
        add(carrysL);

        gridbag.setConstraints(carrysR, c);
        add(carrysR);

        gridbag.setConstraints(dumpBombs, c);
        add(dumpBombs);

        gridbag.setConstraints(sinksL, c);
        add(sinksL);

        gridbag.setConstraints(sinksR, c);
        add(sinksR);

        gridbag.setConstraints(sinks2B, c);
        add(sinks2B);

        gridbag.setConstraints(heatL, c);
        add(heatL);

        c.insets = new Insets(1, 9, 18, 9);
        gridbag.setConstraints(heatR, c);
        add(heatR);
        
        c.insets = new Insets(0, 0, 0, 0);
        gridbag.setConstraints(lblLastTarget, c);
        add(lblLastTarget);
        
        c.insets = new Insets(1, 9, 18, 9);
        gridbag.setConstraints(lastTargetR, c);
        add(lastTargetR);

        c.insets = new Insets(1, 9, 1, 9);
        gridbag.setConstraints(activateHidden, c);
        gridbag.setConstraints(activateHiddenPhase, c);
        add(activateHidden);
        add(activateHiddenPhase);

        setBackGround();
        onResize();
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
                        .toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new File(Configuration.widgetsDir(), udSpec.getLeftLine())
                        .toString());
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

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        // Clear the "Affected By" list.
        ((DefaultListModel<String>) narcList.getModel()).removeAllElements();
        sinks = 0;
        myMechId = en.getId();
        ClientGUI clientgui = unitDisplay.getClientGUI();
        if ((clientgui != null) && (clientgui.getClient().getLocalPlayer().getId() != en
                .getOwnerId())) {
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
        StringBuffer buff;
        if (clientgui != null) {
            Enumeration<IPlayer> loop = clientgui.getClient().getGame().getPlayers();
            while (loop.hasMoreElements()) {
                IPlayer player = loop.nextElement();
                int team = player.getTeam();
                if (en.isNarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuffer(
                            Messages.getString("MechDisplay.NARCedBy")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(IPlayer.teamNames[team]).append(']');
                    ((DefaultListModel<String>) narcList.getModel())
                            .addElement(buff.toString());
                }
                if (en.isINarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuffer(
                            Messages.getString("MechDisplay.INarcHoming")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(IPlayer.teamNames[team]).append("] ")//$NON-NLS-1$
                            .append(Messages.getString("MechDisplay.attached"))//$NON-NLS-1$
                            .append('.');
                    ((DefaultListModel<String>) narcList.getModel())
                            .addElement(buff.toString());
                }
            }
            if (en.isINarcedWith(INarcPod.ECM)) {
                buff = new StringBuffer(
                        Messages.getString("MechDisplay.iNarcECMPodAttached")); //$NON-NLS-1$
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(buff.toString());
            }
            if (en.isINarcedWith(INarcPod.HAYWIRE)) {
                buff = new StringBuffer(
                        Messages.getString("MechDisplay.iNarcHaywirePodAttached")); //$NON-NLS-1$
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(buff.toString());
            }
            if (en.isINarcedWith(INarcPod.NEMESIS)) {
                buff = new StringBuffer(
                        Messages.getString("MechDisplay.iNarcNemesisPodAttached")); //$NON-NLS-1$
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(buff.toString());
            }

            // Show inferno track.
            if (en.infernos.isStillBurning()) {
                buff = new StringBuffer(
                        Messages.getString("MechDisplay.InfernoBurnRemaining")); //$NON-NLS-1$
                buff.append(en.infernos.getTurnsLeftToBurn());
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(buff.toString());
            }
            if ((en instanceof Tank) && ((Tank) en).isOnFire()) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".OnFire")); //$NON-NLS-1$
            }

            // Show electromagnic interference.
            if (en.isSufferingEMI()) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".IsEMId")); //$NON-NLS-1$
            }

            // Show ECM affect.
            Coords pos = en.getPosition();
            if (ComputeECM.isAffectedByAngelECM(en, pos, pos)) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".InEnemyAngelECMField")); //$NON-NLS-1$
            } else if (ComputeECM.isAffectedByECM(en, pos, pos)) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".InEnemyECMField")); //$NON-NLS-1$
            }

            // Active Stealth Armor? If yes, we're under ECM
            if (en.isStealthActive()
                    && ((en instanceof Mech) || (en instanceof Tank))) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".UnderStealth")); //$NON-NLS-1$
            }

            // burdened due to unjettisoned body-mounted missiles on BA?
            if ((en instanceof BattleArmor) && ((BattleArmor) en).isBurdened()) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".Burdened")); //$NON-NLS-1$
            }

            // suffering from taser feedback?
            if (en.getTaserFeedBackRounds() > 0) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(en.getTaserFeedBackRounds()
                                + " " + Messages.getString("MechDisplay.TaserFeedBack"));//$NON-NLS-1$
            }

            // taser interference?
            if (en.getTaserInterference() > 0) {
                ((DefaultListModel<String>) narcList.getModel()).addElement("+" //$NON-NLS-1$
                        + en.getTaserInterference()
                        + " "
                        + Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".TaserInterference"));//$NON-NLS-1$
            }

            // suffering from TSEMP Interference?
            if (en.getTsempEffect() == TSEMPWeapon.TSEMP_EFFECT_INTERFERENCE) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay.TSEMPInterference"));//$NON-NLS-1$
            }

            if (en.hasDamagedRHS()) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay.RHSDamaged"));//$NON-NLS-1$
            }

            // Show Turret Locked.
            if ((en instanceof Tank) && !((Tank) en).hasNoTurret()
                    && !en.canChangeSecondaryFacing()) {
                ((DefaultListModel<String>) narcList.getModel())
                        .addElement(Messages.getString("MechDisplay" //$NON-NLS-1$
                                + ".Turretlocked")); //$NON-NLS-1$
            }

            // Show jammed weapons.
            for (Mounted weapon : en.getWeaponList()) {
                if (weapon.isJammed()) {
                    buff = new StringBuffer(weapon.getName());
                    buff.append(Messages.getString("MechDisplay.isJammed")); //$NON-NLS-1$
                    ((DefaultListModel<String>) narcList.getModel())
                            .addElement(buff.toString());
                }
            }

            // Show breached locations.
            for (int loc = 0; loc < en.locations(); loc++) {
                if (en.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    buff = new StringBuffer(en.getLocationName(loc));
                    buff.append(Messages.getString("MechDisplay.Breached")); //$NON-NLS-1$
                    ((DefaultListModel<String>) narcList.getModel())
                            .addElement(buff.toString());
                }
            }

            if (narcList.getModel().getSize() == 0) {
                ((DefaultListModel<String>) narcList.getModel()).addElement(" "); //$NON-NLS-1$
            }
        }


        // transport values
        String unused = en.getUnusedString();
        if ("".equals(unused)) {
            unused = Messages.getString("MechDisplay.None"); //$NON-NLS-1$
        }
        unusedR.setText(unused);
        carrysR.setText(null);
        // boolean hasText = false;
        for (Entity other : en.getLoadedUnits()) {
            carrysR.append(other.getShortName());
            carrysR.append("\n"); //$NON-NLS-1$
        }

        // Show club(s).
        for (Mounted club : en.getClubs()) {
            carrysR.append(club.getName());
            carrysR.append("\n"); //$NON-NLS-1$
        }

        // Show searchlight
        if (en.hasSpotlight()) {
            if (en.isUsingSpotlight()) {
                carrysR.append(Messages.getString("MechDisplay.SearchlightOn")); //$NON-NLS-1$
            } else {
                carrysR.append(Messages.getString("MechDisplay.SearchlightOff")); //$NON-NLS-1$
            }
        }

        // Show Heat Effects, but only for Mechs.
        heatR.setText(""); //$NON-NLS-1$
        sinksR.setText("");

        if (en instanceof Mech) {
            Mech m = (Mech) en;

            sinks2B.setEnabled(!dontChange);
            sinks = m.getActiveSinksNextRound();
            if (m.hasDoubleHeatSinks()) {
                sinksR.append(Messages.getString(
                        "MechDisplay.activeSinksTextDouble", //$NON-NLS-1$
                        new Object[]{new Integer(sinks),
                                     new Integer(sinks * 2)}));
            } else {
                sinksR.append(Messages.getString(
                        "MechDisplay.activeSinksTextSingle", //$NON-NLS-1$
                        new Object[]{new Integer(sinks)}));
            }

            boolean hasTSM = false;
            boolean mtHeat = false;
            if (((Mech) en).hasTSM()) {
                hasTSM = true;
            }

            if ((clientgui != null)
                    && clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) { //$NON-NLS-1$
                mtHeat = true;
            }
            heatR.append(HeatEffects.getHeatEffects(en.heat, mtHeat, hasTSM));
        } else {
            // Non-Mechs cannot configure their heatsinks
            sinks2B.setEnabled(false);
        }
        /*
         * if (en instanceof Aero && ((Aero) en).hasBombs() &&
         * IGame.Phase.PHASE_DEPLOYMENT != clientgui.getClient().game
         * .getPhase()) { // TODO: I should at some point check and make
         * sure that this // unit has any bombs that it could dump
         * dumpBombs.setEnabled(!dontChange); } else {
         */
        dumpBombs.setEnabled(false);
        // }

        refreshSensorChoices(en);

        if (null != en.getActiveSensor()) {
            curSensorsL.setText((Messages
                    .getString("MechDisplay.CurrentSensors")).concat(" ") //$NON-NLS-1$
                    .concat(en.getSensorDesc()));
        } else {
            curSensorsL.setText((Messages
                    .getString("MechDisplay.CurrentSensors")).concat(" ")); //$NON-NLS-1$
        }
        
        if (en.getLastTarget() != Entity.NONE) {
            lastTargetR.setText(en.getLastTargetDisplayName());
        } else {
            lastTargetR.setText(Messages.getString("MechDisplay.None")); //$NON-NLS-1$
        }

        activateHidden.setEnabled(!dontChange && en.isHidden());
        activateHiddenPhase.setEnabled(!dontChange && en.isHidden());

        onResize();
    } // End public void displayMech( Entity )

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
            if (sensor.getType() == en.getNextSensor().getType()) {
                chSensors.setSelectedIndex(i);
            }
        }
        chSensors.addItemListener(this);
    }

    public void itemStateChanged(ItemEvent ev) {
        ClientGUI clientgui = unitDisplay.getClientGUI();
        if (clientgui == null) {
            return;
        }
        // Only act when a new item is selected
        if (ev.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        if ((ev.getItemSelectable() == chSensors)) {
            int sensorIdx = chSensors.getSelectedIndex();
            Entity en = clientgui.getClient().getGame().getEntity(myMechId);
            Sensor s = en.getSensors().elementAt(sensorIdx);
            en.setNextSensor(s);
            refreshSensorChoices(en);
            String sensorMsg = Messages.getString(
                    "MechDisplay.willSwitchAtEnd", new Object[] { //$NON-NLS-1$
                            "Active Sensors", s.getDisplayName() }); //$NON-NLS-1$
            clientgui.systemMessage(sensorMsg);
            clientgui.getClient().sendSensorChange(myMechId, sensorIdx);
        }
    }

    public void actionPerformed(ActionEvent ae) {
        ClientGUI clientgui = unitDisplay.getClientGUI();
        if (clientgui == null) {
            return;
        }
        if ("changeSinks".equals(ae.getActionCommand()) && !dontChange) { //$NON-NLS-1$
            prompt = new Slider(clientgui.frame,
                    Messages.getString("MechDisplay.changeSinks"), //$NON-NLS-1$
                    Messages.getString("MechDisplay.changeSinks"), sinks, //$NON-NLS-1$
                    0, ((Mech) clientgui.getClient().getGame()
                            .getEntity(myMechId)).getNumberOfSinks());
            if (!prompt.showDialog()) {
                return;
            }
            clientgui.getMenuBar().actionPerformed(ae);
            int numActiveSinks = prompt.getValue();

            ((Mech) clientgui.getClient().getGame().getEntity(myMechId))
                    .setActiveSinksNextRound(numActiveSinks);
            clientgui.getClient().sendSinksChange(myMechId, numActiveSinks);
            displayMech(clientgui.getClient().getGame().getEntity(myMechId));
        } else if (activateHidden.equals(ae.getSource()) && !dontChange) {
            IGame.Phase activationPhase = IGame.Phase
                    .getPhaseFromName((String) activateHiddenPhase
                            .getSelectedItem());
            clientgui.getClient().sendActivateHidden(myMechId, activationPhase);
        }
    }
}