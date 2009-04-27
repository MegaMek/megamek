/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.event.MechDisplayEvent;
import megamek.client.event.MechDisplayListener;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.AeroMapSet;
import megamek.client.ui.swing.widget.ArmlessMechMapSet;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.BattleArmorMapSet;
import megamek.client.ui.swing.widget.CapitalFighterMapSet;
import megamek.client.ui.swing.widget.DisplayMapSet;
import megamek.client.ui.swing.widget.GeneralInfoMapSet;
import megamek.client.ui.swing.widget.GunEmplacementMapSet;
import megamek.client.ui.swing.widget.InfantryMapSet;
import megamek.client.ui.swing.widget.JumpshipMapSet;
import megamek.client.ui.swing.widget.LargeSupportTankMapSet;
import megamek.client.ui.swing.widget.MechMapSet;
import megamek.client.ui.swing.widget.MechPanelTabStrip;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.ProtomechMapSet;
import megamek.client.ui.swing.widget.QuadMapSet;
import megamek.client.ui.swing.widget.SpheroidMapSet;
import megamek.client.ui.swing.widget.SquadronMapSet;
import megamek.client.ui.swing.widget.TankMapSet;
import megamek.client.ui.swing.widget.VTOLMapSet;
import megamek.client.ui.swing.widget.WarshipMapSet;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.ArmlessMech;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentMode;
import megamek.common.EquipmentType;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ILocationExposureStatus;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LargeSupportTank;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.Sensor;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.Terrains;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.WeaponType;
import megamek.common.weapons.BayWeapon;

/**
 * Displays the info for a mech. This is also a sort of interface for special
 * movement and firing actions.
 */
public class MechDisplay extends JPanel {
    // buttons & gizmos for top level

    /**
     *
     */
    private static final long serialVersionUID = -2060993542227677984L;

    private MechPanelTabStrip tabStrip;

    private JPanel displayP;
    private MovementPanel mPan;
    private ArmorPanel aPan;
    public WeaponPanel wPan;
    private SystemPanel sPan;
    private ExtraPanel ePan;
    private ClientGUI clientgui;

    private Entity currentlyDisplaying;
    private ArrayList<MechDisplayListener> eventListeners = new ArrayList<MechDisplayListener>();

    /**
     * Creates and lays out a new mech display.
     */
    public MechDisplay(ClientGUI clientgui) {
        super(new GridBagLayout());

        this.clientgui = clientgui;

        tabStrip = new MechPanelTabStrip(this);

        displayP = new JPanel(new CardLayout());
        mPan = new MovementPanel();
        displayP.add("movement", mPan); //$NON-NLS-1$
        aPan = new ArmorPanel();
        displayP.add("armor", aPan); //$NON-NLS-1$
        wPan = new WeaponPanel();
        displayP.add("weapons", wPan); //$NON-NLS-1$
        sPan = new SystemPanel();
        displayP.add("systems", sPan); //$NON-NLS-1$
        ePan = new ExtraPanel();
        displayP.add("extras", ePan); //$NON-NLS-1$

        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 1, 0, 1);

        c.weightx = 1.0;
        c.weighty = 0.0;

        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(tabStrip, c);
        c.insets = new Insets(0, 1, 1, 1);
        c.weighty = 1.0;
        addBag(displayP, c);

        ((CardLayout) displayP.getLayout()).show(displayP, "movement"); //$NON-NLS-1$
        // tabStrip.setTab(0);
    }

    private void addBag(JComponent comp, GridBagConstraints c) {
        ((GridBagLayout) getLayout()).setConstraints(comp, c);
        add(comp);
    }

    /**
     * Displays the specified entity in the panel.
     */
    public void displayEntity(Entity en) {

        clientgui.mechW.setTitle(en.getShortName());

        currentlyDisplaying = en;

        mPan.displayMech(en);
        aPan.displayMech(en);
        wPan.displayMech(en);
        sPan.displayMech(en);
        ePan.displayMech(en);
    }

    /**
     * Returns the entity we'return currently displaying
     */

    public Entity getCurrentEntity() {
        return currentlyDisplaying;
    }

    /**
     * Changes to the specified panel.
     */
    public void showPanel(String s) {
        ((CardLayout) displayP.getLayout()).show(displayP, s);
        if ("movement".equals(s)) { //$NON-NLS-1$
            tabStrip.setTab(0);
        } else if ("armor".equals(s)) { //$NON-NLS-1$
            tabStrip.setTab(1);
        } else if ("weapons".equals(s)) { //$NON-NLS-1$
            tabStrip.setTab(3);
        } else if ("systems".equals(s)) { //$NON-NLS-1$
            tabStrip.setTab(2);
        } else if ("extras".equals(s)) { //$NON-NLS-1$
            tabStrip.setTab(4);
        }
    }

    /**
     * Adds the specified mech display listener to receive events from this
     * view.
     *
     * @param listener
     *            the listener.
     */
    public void addMechDisplayListener(MechDisplayListener listener) {
        eventListeners.add(listener);
    }

    /**
     * Notifies attached listeners of the event.
     *
     * @param event
     *            the mech display event.
     */
    private void processMechDisplayEvent(MechDisplayEvent event) {
        for (int i = 0; i < eventListeners.size(); i++) {
            MechDisplayListener lis = eventListeners.get(i);
            switch (event.getType()) {
            case MechDisplayEvent.WEAPON_SELECTED:
                lis.WeaponSelected(event);
                break;
            default:
                System.err.println("unknown event " + event.getType()
                        + " in processMechDisplayEvent");
                break;
            }
        }
    }

    /**
     * The movement panel contains all the buttons, readouts and gizmos relating
     * to moving around on the battlefield.
     */
    private class MovementPanel extends PicMap {

        /**
         *
         */
        private static final long serialVersionUID = 8284603003897415518L;

        private GeneralInfoMapSet gi;

        private int minTopMargin = 8;
        private int minLeftMargin = 8;

        MovementPanel() {
            gi = new GeneralInfoMapSet(this);
            addElement(gi.getContentGroup());
            Enumeration<BackGroundDrawer> iter = gi.getBackgroundDrawers()
                    .elements();
            while (iter.hasMoreElements()) {
                addBgDrawer(iter.nextElement());
            }
            onResize();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            update();
        }

        @Override
        public void onResize() {
            int w = getSize().width;
            Rectangle r = getContentBounds();
            int dx = Math.round(((w - r.width) / 2));
            if (dx < minLeftMargin) {
                dx = minLeftMargin;
            }
            int dy = minTopMargin;
            setContentMargins(dx, dy, dx, dy);
        }

        /**
         * updates fields for the specified mech
         */
        public void displayMech(Entity en) {
            gi.setEntity(en);
            onResize();
            update();
        }

    }

    /**
     * This panel contains the armor readout display.
     */
    private class ArmorPanel extends PicMap {
        /**
         *
         */
        private static final long serialVersionUID = -3612396252172441104L;
        private TankMapSet tank;
        private MechMapSet mech;
        private InfantryMapSet infantry;
        private BattleArmorMapSet battleArmor;
        private ProtomechMapSet proto;
        private VTOLMapSet vtol;
        private QuadMapSet quad;
        private GunEmplacementMapSet gunEmplacement;
        private ArmlessMechMapSet armless;
        private LargeSupportTankMapSet largeSupportTank;
        private AeroMapSet aero;
        private CapitalFighterMapSet capFighter;
        private SquadronMapSet squad;
        private JumpshipMapSet jump;
        private SpheroidMapSet sphere;
        private WarshipMapSet warship;
        private int minTopMargin;
        private int minLeftMargin;
        private int minBottomMargin;
        private int minRightMargin;

        private static final int minTankTopMargin = 8;
        private static final int minTankLeftMargin = 8;
        private static final int minVTOLTopMargin = 8;
        private static final int minVTOLLeftMargin = 8;
        private static final int minMechTopMargin = 18;
        private static final int minMechLeftMargin = 7;
        private static final int minMechBottomMargin = 0;
        private static final int minMechRightMargin = 0;
        private static final int minInfTopMargin = 8;
        private static final int minInfLeftMargin = 8;
        private static final int minAeroTopMargin = 8;
        private static final int minAeroLeftMargin = 8;

        @Override
        public void addNotify() {
            super.addNotify();
            tank = new TankMapSet(this);
            mech = new MechMapSet(this);
            infantry = new InfantryMapSet(this);
            battleArmor = new BattleArmorMapSet(this);
            proto = new ProtomechMapSet(this);
            vtol = new VTOLMapSet(this);
            quad = new QuadMapSet(this);
            gunEmplacement = new GunEmplacementMapSet(this);
            armless = new ArmlessMechMapSet(this);
            largeSupportTank = new LargeSupportTankMapSet(this);
            aero = new AeroMapSet(this);
            capFighter = new CapitalFighterMapSet(this);
            sphere = new SpheroidMapSet(this);
            jump = new JumpshipMapSet(this);
            warship = new WarshipMapSet(this);
            squad = new SquadronMapSet(this);
        }

        @Override
        public void onResize() {
            Rectangle r = getContentBounds();
            if (r == null) {
                return;
            }
            int w = Math.round(((getSize().width - r.width) / 2));
            int h = Math.round(((getSize().height - r.height) / 2));
            int dx = w < minLeftMargin ? minLeftMargin : w;
            int dy = h < minTopMargin ? minTopMargin : h;
            setContentMargins(dx, dy, minRightMargin, minBottomMargin);
        }

        /**
         * updates fields for the specified mech
         */
        public void displayMech(Entity en) {
            // Look out for a race condition.
            if (en == null) {
                return;
            }
            DisplayMapSet ams = mech;
            removeAll();
            if (en instanceof QuadMech) {
                ams = quad;
                minLeftMargin = minMechLeftMargin;
                minTopMargin = minMechTopMargin;
                minBottomMargin = minMechBottomMargin;
                minRightMargin = minMechRightMargin;
            } else if (en instanceof ArmlessMech) {
                ams = armless;
                minLeftMargin = minMechLeftMargin;
                minTopMargin = minMechTopMargin;
                minBottomMargin = minMechBottomMargin;
                minRightMargin = minMechRightMargin;
            } else if (en instanceof Mech) {
                ams = mech;
                minLeftMargin = minMechLeftMargin;
                minTopMargin = minMechTopMargin;
                minBottomMargin = minMechBottomMargin;
                minRightMargin = minMechRightMargin;
            } else if (en instanceof VTOL) {
                ams = vtol;
                minLeftMargin = minVTOLLeftMargin;
                minTopMargin = minVTOLTopMargin;
                minBottomMargin = minVTOLTopMargin;
                minRightMargin = minVTOLLeftMargin;
            } else if (en instanceof LargeSupportTank) {
                ams = largeSupportTank;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            } else if (en instanceof Tank) {
                ams = tank;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            } else if (en instanceof BattleArmor) {
                ams = battleArmor;
                minLeftMargin = minInfLeftMargin;
                minTopMargin = minInfTopMargin;
                minBottomMargin = minInfTopMargin;
                minRightMargin = minInfLeftMargin;
            } else if (en instanceof Infantry) {
                ams = infantry;
                minLeftMargin = minInfLeftMargin;
                minTopMargin = minInfTopMargin;
                minBottomMargin = minInfTopMargin;
                minRightMargin = minInfLeftMargin;
            } else if (en instanceof Protomech) {
                ams = proto;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            } else if (en instanceof GunEmplacement) {
                ams = gunEmplacement;
                minLeftMargin = minTankLeftMargin;
                minTopMargin = minTankTopMargin;
                minBottomMargin = minTankTopMargin;
                minRightMargin = minTankLeftMargin;
            } else if (en instanceof Warship) {
                ams = warship;
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            } else if (en instanceof Jumpship) {
                ams = jump;
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            } else if (en instanceof FighterSquadron) {
                ams = squad;
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            } else if (en instanceof Aero) {
                ams = aero;
                if (en instanceof SmallCraft) {
                    SmallCraft sc = (SmallCraft) en;
                    if (sc.isSpheroid()) {
                        ams = sphere;
                    }
                }
                if (en.isCapitalFighter()) {
                    ams = capFighter;
                }
                minLeftMargin = minAeroLeftMargin;
                minTopMargin = minAeroTopMargin;
                minBottomMargin = minAeroTopMargin;
                minRightMargin = minAeroLeftMargin;
            }
            if (ams == null) {
                System.err.println("The armor panel is null."); //$NON-NLS-1$
                return;
            }
            ams.setEntity(en);
            addElement(ams.getContentGroup());
            Enumeration<BackGroundDrawer> iter = ams.getBackgroundDrawers()
                    .elements();
            while (iter.hasMoreElements()) {
                addBgDrawer(iter.nextElement());
            }
            onResize();
            update();
        }
    }

    /**
     * This class contains the all the gizmos for firing the mech's weapons.
     */
    public class WeaponPanel extends PicMap implements ListSelectionListener, ActionListener {
        /**
         *
         */
        private static final long serialVersionUID = -5728839963281503332L;

        private static final String IMAGE_DIR = "data/images/widgets";

        public JList weaponList;
        private JComboBox m_chAmmo;
        public JComboBox m_chBayWeapon;

        private JLabel wAmmo;
        private JLabel wBayWeapon;
        private JLabel wNameL;
        private JLabel wHeatL;
        private JLabel wArcHeatL;
        private JLabel wDamL;
        private JLabel wMinL;
        private JLabel wShortL;
        private JLabel wMedL;
        private JLabel wLongL;
        private JLabel wExtL;
        private JLabel wAVL;
        private JLabel wNameR;
        private JLabel wHeatR;
        private JLabel wArcHeatR;
        private JLabel wDamR;
        private JLabel wMinR;
        private JLabel wShortR;
        private JLabel wMedR;
        private JLabel wLongR;
        private JLabel wExtR;
        private JLabel wShortAVR;
        private JLabel wMedAVR;
        private JLabel wLongAVR;
        private JLabel wExtAVR;
        private JLabel currentHeatBuildupL;
        private JLabel currentHeatBuildupR;

        private JLabel wTargetL;
        private JLabel wRangeL;
        private JLabel wToHitL;
        public JLabel wTargetR;
        public JLabel wRangeR;
        public JLabel wToHitR;

        public JTextArea toHitText;

        // I need to keep a pointer to the weapon list of the
        // currently selected mech.
        private ArrayList<Mounted> vAmmo;
        private Entity entity;

        private int minTopMargin = 8;
        private int minLeftMargin = 8;

        WeaponPanel() {

            GridBagLayout gridBagLayout = new GridBagLayout();
            setLayout(gridBagLayout);

            // weapon list
            weaponList = new JList(new DefaultListModel());
            weaponList.addListSelectionListener(this);

            // layout main panel
            GridBagConstraints c = new GridBagConstraints();

            // adding Weapon List
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(15, 9, 1, 9);
            c.weightx = 0.0;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(weaponList, c);
            add(weaponList);

            // adding Ammo choice + label

            wAmmo = new JLabel(
                    Messages.getString("MechDisplay.Ammo"), SwingConstants.LEFT); //$NON-NLS-1$
            wAmmo.setOpaque(false);
            wAmmo.setForeground(Color.WHITE);
            m_chAmmo = new JComboBox();
            m_chAmmo.addActionListener(this);

            wBayWeapon = new JLabel(
                    Messages.getString("MechDisplay.Weapon"), SwingConstants.LEFT); //$NON-NLS-1$
            wBayWeapon.setOpaque(false);
            wBayWeapon.setForeground(Color.WHITE);
            m_chBayWeapon = new JComboBox();
            m_chBayWeapon.addActionListener(this);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridwidth = 1;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.gridy = 1;
            ((GridBagLayout) getLayout()).setConstraints(wBayWeapon, c);
            add(wBayWeapon);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 1;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            ((GridBagLayout) getLayout()).setConstraints(m_chBayWeapon, c);
            add(m_chBayWeapon);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridwidth = 1;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.gridy = 2;
            gridBagLayout.setConstraints(wAmmo, c);
            add(wAmmo);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 1;
            c.gridy = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            gridBagLayout.setConstraints(m_chAmmo, c);
            add(m_chAmmo);

            // Adding Heat Buildup

            currentHeatBuildupL = new JLabel(Messages
                    .getString("MechDisplay.HeatBuildup"), SwingConstants.RIGHT); //$NON-NLS-1$
            currentHeatBuildupL.setOpaque(false);
            currentHeatBuildupL.setForeground(Color.WHITE);
            currentHeatBuildupR = new JLabel("--", SwingConstants.LEFT); //$NON-NLS-1$
            currentHeatBuildupR.setOpaque(false);
            currentHeatBuildupR.setForeground(Color.WHITE);

            c.insets = new Insets(2, 9, 2, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 3;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            gridBagLayout.setConstraints(currentHeatBuildupL, c);
            add(currentHeatBuildupL);

            c.insets = new Insets(2, 1, 2, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 2;
            c.anchor = GridBagConstraints.WEST;
            // c.fill = GridBagConstraints.HORIZONTAL;
            gridBagLayout.setConstraints(currentHeatBuildupR, c);
            add(currentHeatBuildupR);

            // Adding weapon display labels
            wNameL = new JLabel(
                    Messages.getString("MechDisplay.Name"), SwingConstants.CENTER); //$NON-NLS-1$
            wNameL.setOpaque(false);
            wNameL.setForeground(Color.WHITE);
            wHeatL = new JLabel(
                    Messages.getString("MechDisplay.Heat"), SwingConstants.CENTER); //$NON-NLS-1$
            wHeatL.setOpaque(false);
            wHeatL.setForeground(Color.WHITE);
            wDamL = new JLabel(
                    Messages.getString("MechDisplay.Damage"), SwingConstants.CENTER); //$NON-NLS-1$
            wDamL.setOpaque(false);
            wDamL.setForeground(Color.WHITE);
            wArcHeatL = new JLabel(
                    Messages.getString("MechDisplay.ArcHeat"), SwingConstants.CENTER); //$NON-NLS-1$
            wArcHeatL.setOpaque(false);
            wArcHeatL.setForeground(Color.WHITE);
            wNameR = new JLabel("", SwingConstants.CENTER); //$NON-NLS-1$
            wNameR.setOpaque(false);
            wNameR.setForeground(Color.WHITE);
            wHeatR = new JLabel("--", SwingConstants.CENTER); //$NON-NLS-1$
            wHeatR.setOpaque(false);
            wHeatR.setForeground(Color.WHITE);
            wDamR = new JLabel("--", SwingConstants.CENTER); //$NON-NLS-1$
            wDamR.setOpaque(false);
            wDamR.setForeground(Color.WHITE);
            wArcHeatR = new JLabel("--", SwingConstants.CENTER); //$NON-NLS-1$
            wArcHeatR.setOpaque(false);
            wArcHeatR.setForeground(Color.WHITE);

            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 4;
            gridBagLayout.setConstraints(wNameL, c);
            add(wNameL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridwidth = 1;
            c.gridx = 2;
            gridBagLayout.setConstraints(wHeatL, c);
            add(wHeatL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridwidth = 1;
            c.gridx = 3;
            gridBagLayout.setConstraints(wDamL, c);
            add(wDamL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 4;
            ((GridBagLayout) getLayout()).setConstraints(wArcHeatL, c);
            add(wArcHeatL);

            c.insets = new Insets(1, 9, 2, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 5;
            gridBagLayout.setConstraints(wNameR, c);
            add(wNameR);

            c.gridwidth = 1;
            c.gridx = 2;
            gridBagLayout.setConstraints(wHeatR, c);
            add(wHeatR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 3;
            c.gridwidth = 1;
            gridBagLayout.setConstraints(wDamR, c);
            add(wDamR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 4;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wArcHeatR, c);
            add(wArcHeatR);

            // Adding range labels
            wMinL = new JLabel(
                    Messages.getString("MechDisplay.Min"), SwingConstants.CENTER); //$NON-NLS-1$
            wMinL.setOpaque(false);
            wMinL.setForeground(Color.WHITE);
            wShortL = new JLabel(
                    Messages.getString("MechDisplay.Short"), SwingConstants.CENTER); //$NON-NLS-1$
            wShortL.setOpaque(false);
            wShortL.setForeground(Color.WHITE);
            wMedL = new JLabel(
                    Messages.getString("MechDisplay.Med"), SwingConstants.CENTER); //$NON-NLS-1$
            wMedL.setOpaque(false);
            wMedL.setForeground(Color.WHITE);
            wLongL = new JLabel(
                    Messages.getString("MechDisplay.Long"), SwingConstants.CENTER); //$NON-NLS-1$
            wLongL.setOpaque(false);
            wLongL.setForeground(Color.WHITE);
            wExtL = new JLabel(
                    Messages.getString("MechDisplay.Ext"), SwingConstants.CENTER); //$NON-NLS-1$
            wExtL.setOpaque(false);
            wExtL.setForeground(Color.WHITE);
            wMinR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wMinR.setOpaque(false);
            wMinR.setForeground(Color.WHITE);
            wShortR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wShortR.setOpaque(false);
            wShortR.setForeground(Color.WHITE);
            wMedR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wMedR.setOpaque(false);
            wMedR.setForeground(Color.WHITE);
            wLongR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wLongR.setOpaque(false);
            wLongR.setForeground(Color.WHITE);
            wExtR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wExtR.setOpaque(false);
            wExtR.setForeground(Color.WHITE);
            wAVL = new JLabel(
                    Messages.getString("MechDisplay.AV"), SwingConstants.CENTER); //$NON-NLS-1$
            wAVL.setOpaque(false);
            wAVL.setForeground(Color.WHITE);
            wShortAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wShortAVR.setOpaque(false);
            wShortAVR.setForeground(Color.WHITE);
            wMedAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wMedAVR.setOpaque(false);
            wMedAVR.setForeground(Color.WHITE);
            wLongAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wLongAVR.setOpaque(false);
            wLongAVR.setForeground(Color.WHITE);
            wExtAVR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wExtAVR.setOpaque(false);
            wExtAVR.setForeground(Color.WHITE);

            c.weightx = 1.0;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 6;
            c.gridwidth = 1;
            gridBagLayout.setConstraints(wMinL, c);
            add(wMinL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridx = 1;
            c.gridy = 6;
            gridBagLayout.setConstraints(wShortL, c);
            add(wShortL);

            c.gridx = 2;
            c.gridy = 6;
            gridBagLayout.setConstraints(wMedL, c);
            add(wMedL);

            // c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 3;
            c.gridy = 6;
            // c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(wLongL, c);
            add(wLongL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 4;
            c.gridy = 6;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(wExtL, c);
            add(wExtL);
            // ----------------

            c.insets = new Insets(1, 9, 2, 1);
            c.gridx = 0;
            c.gridy = 7;
            c.gridwidth = 1;
            gridBagLayout.setConstraints(wMinR, c);
            add(wMinR);

            c.insets = new Insets(1, 1, 2, 1);
            c.gridx = 1;
            c.gridy = 7;
            gridBagLayout.setConstraints(wShortR, c);
            add(wShortR);

            c.gridx = 2;
            c.gridy = 7;
            gridBagLayout.setConstraints(wMedR, c);
            add(wMedR);

            // c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 3;
            c.gridy = 7;
            // c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(wLongR, c);
            add(wLongR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 4;
            c.gridy = 7;
            gridBagLayout.setConstraints(wExtR, c);
            add(wExtR);

            // ----------------
            c.insets = new Insets(1, 9, 2, 1);
            c.gridx = 0;
            c.gridy = 8;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wAVL, c);
            add(wAVL);

            c.insets = new Insets(1, 1, 2, 1);
            c.gridx = 1;
            c.gridy = 8;
            ((GridBagLayout) getLayout()).setConstraints(wShortAVR, c);
            add(wShortAVR);

            c.gridx = 2;
            c.gridy = 8;
            ((GridBagLayout) getLayout()).setConstraints(wMedAVR, c);
            add(wMedAVR);

            // c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 3;
            c.gridy = 8;
            // c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wLongAVR, c);
            add(wLongAVR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 4;
            c.gridy = 8;
            ((GridBagLayout) getLayout()).setConstraints(wExtAVR, c);
            add(wExtAVR);

            // target panel
            wTargetL = new JLabel(
                    Messages.getString("MechDisplay.Target"), SwingConstants.CENTER); //$NON-NLS-1$
            wTargetL.setOpaque(false);
            wTargetL.setForeground(Color.WHITE);
            wRangeL = new JLabel(
                    Messages.getString("MechDisplay.Range"), SwingConstants.CENTER); //$NON-NLS-1$
            wRangeL.setOpaque(false);
            wRangeL.setForeground(Color.WHITE);
            wToHitL = new JLabel(
                    Messages.getString("MechDisplay.ToHit"), SwingConstants.CENTER); //$NON-NLS-1$
            wToHitL.setOpaque(false);
            wToHitL.setForeground(Color.WHITE);

            wTargetR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wTargetR.setOpaque(false);
            wTargetR.setForeground(Color.WHITE);
            wRangeR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wRangeR.setOpaque(false);
            wRangeR.setForeground(Color.WHITE);
            wToHitR = new JLabel("---", SwingConstants.CENTER); //$NON-NLS-1$
            wToHitR.setOpaque(false);
            wToHitR.setForeground(Color.WHITE);

            c.weightx = 0.0;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 9;
            c.gridwidth = 1;
            gridBagLayout.setConstraints(wTargetL, c);
            add(wTargetL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 9;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(wTargetR, c);
            add(wTargetR);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 10;
            c.gridwidth = 1;
            gridBagLayout.setConstraints(wRangeL, c);
            add(wRangeL);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 10;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(wRangeR, c);
            add(wRangeR);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 11;
            c.gridwidth = 1;
            gridBagLayout.setConstraints(wToHitL, c);
            add(wToHitL);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 11;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(wToHitR, c);
            add(wToHitR);

            // to-hit text
            toHitText = new JTextArea("", 2, 20); //$NON-NLS-1$
            toHitText.setEditable(false);
            toHitText.setLineWrap(true);
            toHitText.setFont(new Font("SansSerif", Font.PLAIN, 10)); //$NON-NLS-1$

            c.insets = new Insets(1, 9, 15, 9);
            c.gridx = 0;
            c.gridy = 12;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBagLayout.setConstraints(toHitText, c);
            add(toHitText);

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
            Image tile = getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            int b = BackGroundDrawer.TILING_BOTH;
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL
                    | BackGroundDrawer.VALIGN_TOP;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL
                    | BackGroundDrawer.VALIGN_BOTTOM;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                    | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                    | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/br_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

        }

        /**
         * updates fields for the specified mech
         * <p/>
         * fix the ammo when it's added
         */
        public void displayMech(Entity en) {

            // Grab a copy of the game.
            IGame game = clientgui.getClient().game;

            // update pointer to weapons
            entity = en;

            int currentHeatBuildup = en.heat // heat from last round
                    + en.getEngineCritHeat() // heat engine crits will add
                    + Math.min(15, en.heatFromExternal) // heat from external
                    // sources
                    + en.heatBuildup; // heat we're building up this round
            if (en instanceof Mech) {
                if (en.infernos.isStillBurning()) { // hit with inferno ammo
                    currentHeatBuildup += en.infernos.getHeat();
                }
                if (!((Mech) en).hasLaserHeatSinks()) {
                    // extreme temperatures.
                    if (game.getPlanetaryConditions().getTemperature() > 0) {
                        currentHeatBuildup += game.getPlanetaryConditions()
                                .getTemperatureDifference(50, -30);
                    } else {
                        currentHeatBuildup -= game.getPlanetaryConditions()
                                .getTemperatureDifference(50, -30);
                    }
                }
            }
            Coords position = entity.getPosition();
            if (!en.isOffBoard() && (position != null)) {
                IHex hex = game.getBoard().getHex(position);
                if (hex.containsTerrain(Terrains.FIRE) && (hex.getFireTurn() > 0)) {
                    currentHeatBuildup += 5; // standing in fire
                }
                if (hex.terrainLevel(Terrains.MAGMA) == 1) {
                    currentHeatBuildup += 5;
                } else if (hex.terrainLevel(Terrains.MAGMA) == 2) {
                    currentHeatBuildup += 10;
                }
            }
            if (((en instanceof Mech) && en.isStealthActive())
                    || en.isNullSigActive() || en.isVoidSigActive()) {
                currentHeatBuildup += 10; // active stealth/null sig/void sig
                // heat
            }

            if ((en instanceof Mech) && en.isChameleonShieldActive()) {
                currentHeatBuildup += 6;
            }

            // update weapon list
            ((DefaultListModel) weaponList.getModel()).removeAllElements();
            ((DefaultComboBoxModel) m_chAmmo.getModel()).removeAllElements();
            m_chAmmo.setEnabled(false);
            m_chBayWeapon.removeAllItems();
            m_chBayWeapon.setEnabled(false);

            // on large craft we may need to take account of firing arcs
            boolean[] usedFrontArc = new boolean[entity.locations()];
            boolean[] usedRearArc = new boolean[entity.locations()];
            for (int i = 0; i < entity.locations(); i++) {
                usedFrontArc[i] = false;
                usedRearArc[i] = false;
            }

            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted mounted = entity.getWeaponList().get(i);
                WeaponType wtype = (WeaponType) mounted.getType();
                StringBuffer wn = new StringBuffer(mounted.getDesc());
                wn.append(" ["); //$NON-NLS-1$
                wn.append(en.getLocationAbbr(mounted.getLocation()));
                if (mounted.isSplit()) {
                    wn.append('/');
                    wn.append(en.getLocationAbbr(mounted.getSecondLocation()));
                }
                wn.append(']');
                // determine shots left & total shots left
                if ((wtype.getAmmoType() != AmmoType.T_NA)
                        && !wtype.hasFlag(WeaponType.F_ONESHOT)) {
                    int shotsLeft = 0;
                    if ((mounted.getLinked() != null)
                            && !mounted.getLinked().isDumping()) {
                        shotsLeft = mounted.getLinked().getShotsLeft();
                    }

                    EquipmentType typeUsed = null;
                    if (mounted.getLinked() != null) {
                        typeUsed = mounted.getLinked().getType();
                    }

                    int totalShotsLeft = entity
                            .getTotalMunitionsOfType(typeUsed);

                    wn.append(" ("); //$NON-NLS-1$
                    wn.append(shotsLeft);
                    wn.append('/');
                    wn.append(totalShotsLeft);
                    wn.append(')');
                }

                // MG rapidfire
                if (mounted.isRapidfire()) {
                    wn.append(Messages.getString("MechDisplay.rapidFire")); //$NON-NLS-1$
                }

                // Hotloaded Missile Launchers
                if (mounted.isHotLoaded()) {
                    wn.append(Messages.getString("MechDisplay.isHotLoaded")); //$NON-NLS-1$
                }

                // Fire Mode - lots of things have variable modes
                if (wtype.hasModes()) {
                    wn.append(' ');
                    wn.append(mounted.curMode().getDisplayableName());
                }
                ((DefaultListModel) weaponList.getModel()).addElement(wn
                        .toString());
                if (mounted.isUsedThisRound()
                        && (game.getPhase() == mounted.usedInPhase())
                        && (game.getPhase() == IGame.Phase.PHASE_FIRING)) {
                    // add heat from weapons fire to heat tracker
                    if (entity.usesWeaponBays()) {
                        // if using bay heat option then don't add total arc
                        if (game.getOptions().booleanOption("heat_by_bay")) {
                            for (int wId : mounted.getBayWeapons()) {
                                currentHeatBuildup += entity.getEquipment(wId)
                                        .getCurrentHeat();
                            }
                        } else {
                            // check whether arc has fired
                            int loc = mounted.getLocation();
                            boolean rearMount = mounted.isRearMounted();
                            if (!rearMount) {
                                if (!usedFrontArc[loc]) {
                                    currentHeatBuildup += entity.getHeatInArc(
                                            loc, rearMount);
                                    usedFrontArc[loc] = true;
                                }
                            } else {
                                if (!usedRearArc[loc]) {
                                    currentHeatBuildup += entity.getHeatInArc(
                                            loc, rearMount);
                                    usedRearArc[loc] = true;
                                }
                            }
                        }
                    } else {
                        if (!mounted.isBombMounted()) {
                            currentHeatBuildup += mounted.getCurrentHeat();
                        }
                    }
                }
            }

            // This code block copied from the MovementPanel class,
            // bad coding practice (duplicate code).
            int heatCap = en.getHeatCapacity();
            int heatCapWater = en.getHeatCapacityWithWater();
            String heatCapacityStr = Integer.toString(heatCap);

            if (heatCap < heatCapWater) {
                heatCapacityStr = heatCap + " [" + heatCapWater + ']'; //$NON-NLS-1$
            }
            // end duplicate block

            String heatText = Integer.toString(currentHeatBuildup);
            if (currentHeatBuildup > en.getHeatCapacityWithWater()) {
                heatText += "*"; // overheat indication //$NON-NLS-1$
            }
            // check for negative values due to extreme temp
            if (currentHeatBuildup < 0) {
                currentHeatBuildup = 0;
            }
            currentHeatBuildupR
                    .setText(heatText + " (" + heatCapacityStr + ')'); //$NON-NLS-1$

            // change what is visible based on type
            if (entity.usesWeaponBays()) {
                wArcHeatL.setVisible(true);
                wArcHeatR.setVisible(true);
                m_chBayWeapon.setVisible(true);
                wBayWeapon.setVisible(true);
            } else {
                wArcHeatL.setVisible(false);
                wArcHeatR.setVisible(false);
                m_chBayWeapon.setVisible(false);
                wBayWeapon.setVisible(false);
            }

            if (entity instanceof Aero) {
                wAVL.setVisible(true);
                wShortAVR.setVisible(true);
                wMedAVR.setVisible(true);
                wLongAVR.setVisible(true);
                wExtAVR.setVisible(true);
                wMinL.setVisible(false);
                wMinR.setVisible(false);
            } else {
                wAVL.setVisible(false);
                wShortAVR.setVisible(false);
                wMedAVR.setVisible(false);
                wLongAVR.setVisible(false);
                wExtAVR.setVisible(false);
                wMinL.setVisible(true);
                wMinR.setVisible(true);
            }

            // If MaxTech range rules are in play, display the extreme range.
            if (game.getOptions().booleanOption("tacops_range") || (entity instanceof Aero)) { //$NON-NLS-1$
                wExtL.setVisible(true);
                wExtR.setVisible(true);
            } else {
                wExtL.setVisible(false);
                wExtR.setVisible(false);
            }
            onResize();
        }

        /**
         * Selects the weapon at the specified index in the list
         */
        public void selectWeapon(int wn) {
            if (wn == -1) {
                weaponList.setSelectedIndex(-1);
                return;
            }
            int index = entity.getWeaponList().indexOf(entity.getEquipment(wn));
            weaponList.setSelectedIndex(index);
            displaySelected();
        }

        /**
         * Selects the weapon at the specified index in the list
         */
        public int getSelectedWeaponNum() {
            int selected = weaponList.getSelectedIndex();
            if (selected == -1) {
                return -1;
            }
            return entity.getEquipmentNum(entity.getWeaponList().get(selected));
        }

        /**
         * displays the selected item from the list in the weapon display panel.
         */
        private void displaySelected() {
            // short circuit if not selected
            if (weaponList.getSelectedIndex() == -1) {
                ((DefaultComboBoxModel) m_chAmmo.getModel())
                        .removeAllElements();
                m_chAmmo.setEnabled(false);
                m_chBayWeapon.removeAllItems();
                m_chBayWeapon.setEnabled(false);
                wNameR.setText(""); //$NON-NLS-1$
                wHeatR.setText("--"); //$NON-NLS-1$
                wArcHeatR.setText("---"); //$NON-NLS-1$
                wDamR.setText("--"); //$NON-NLS-1$
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("---"); //$NON-NLS-1$
                wMedR.setText("---"); //$NON-NLS-1$
                wLongR.setText("---"); //$NON-NLS-1$
                wExtR.setText("---"); //$NON-NLS-1$
                return;
            }
            Mounted mounted = entity.getWeaponList().get(
                    weaponList.getSelectedIndex());
            WeaponType wtype = (WeaponType) mounted.getType();
            // update weapon display
            wNameR.setText(mounted.getDesc());
            if (mounted.hasChargedCapacitor()) {
                wHeatR.setText(Integer.toString((Compute.dialDownHeat(mounted,
                        wtype) + 5)));
            } else if (wtype.hasFlag(WeaponType.F_ENERGY)
                    && wtype.hasModes()
                    && clientgui.getClient().game.getOptions().booleanOption(
                            "tacops_energy_weapons")) {
                wHeatR.setText(Integer.toString((Compute.dialDownHeat(mounted,
                        wtype))));
            } else {
                wHeatR.setText(Integer.toString(mounted.getCurrentHeat()));
            }

            wArcHeatR.setText(Integer.toString(entity.getHeatInArc(mounted
                    .getLocation(), mounted.isRearMounted())));

            if (wtype.getDamage() == WeaponType.DAMAGE_MISSILE) {
                wDamR.setText(Messages.getString("MechDisplay.Missile")); //$NON-NLS-1$
            } else if (wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
                wDamR.setText(Messages.getString("MechDisplay.Variable")); //$NON-NLS-1$
            } else if (wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
                wDamR.setText(Messages.getString("MechDisplay.Special")); //$NON-NLS-1$
            } else if (wtype.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                StringBuffer damage = new StringBuffer();
                damage.append(Integer.toString(wtype.getRackSize()))
                        .append('/').append(
                                Integer.toString(wtype.getRackSize() / 2));
                wDamR.setText(damage.toString());
            } else if (wtype.hasFlag(WeaponType.F_ENERGY)
                    && wtype.hasModes()
                    && clientgui.getClient().game.getOptions().booleanOption(
                            "tacops_energy_weapons")) {
                if (mounted.hasChargedCapacitor()) {
                    wDamR.setText(Integer.toString(Compute.dialDownDamage(
                            mounted, wtype) + 5));
                } else {
                    wDamR.setText(Integer.toString(Compute.dialDownDamage(
                            mounted, wtype)));
                }
            } else {
                wDamR.setText(Integer.toString(wtype.getDamage()));
            }

            // update range
            int shortR = wtype.getShortRange();
            int mediumR = wtype.getMediumRange();
            int longR = wtype.getLongRange();
            int extremeR = wtype.getExtremeRange();
            if ((entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET)
                    || (longR == 0)) {
                shortR = wtype.getWShortRange();
                mediumR = wtype.getWMediumRange();
                longR = wtype.getWLongRange();
                extremeR = wtype.getWExtremeRange();
            }
            if (wtype.getMinimumRange() > 0) {
                wMinR.setText(Integer.toString(wtype.getMinimumRange()));
            } else {
                wMinR.setText("---"); //$NON-NLS-1$
            }
            if (shortR > 1) {
                wShortR.setText("1 - " + shortR); //$NON-NLS-1$
            } else {
                wShortR.setText("" + shortR); //$NON-NLS-1$
            }
            if (mediumR - shortR > 1) {
                wMedR.setText(shortR + 1 + " - " + mediumR); //$NON-NLS-1$
            } else {
                wMedR.setText("" + mediumR); //$NON-NLS-1$
            }
            if (longR - mediumR > 1) {
                wLongR.setText(mediumR + 1 + " - " + longR); //$NON-NLS-1$
            } else {
                wLongR.setText("" + longR); //$NON-NLS-1$
            }
            if (extremeR - longR > 1) {
                wExtR.setText(longR + 1 + " - " + extremeR); //$NON-NLS-1$
            } else {
                wExtR.setText("" + extremeR); //$NON-NLS-1$
            }

            // Update the range display to account for the weapon's loaded ammo.
            if (mounted.getLinked() != null) {
                updateRangeDisplayForAmmo(mounted.getLinked());
            }

            if (entity instanceof Aero) {
                // change damage report to a statement of standard or capital
                if (wtype.isCapital()) {
                    wDamR.setText(Messages.getString("MechDisplay.CapitalD")); //$NON-NLS-1$
                } else {
                    wDamR.setText(Messages.getString("MechDisplay.StandardD")); //$NON-NLS-1$
                }

                // if this is a weapons bay, then I need to compile it to get
                // accurate results
                if (wtype instanceof BayWeapon) {
                    compileWeaponBay(mounted, wtype.isCapital());
                } else {
                    // otherwise I need to replace range display with standard
                    // ranges and attack values
                    updateAttackValues(wtype, mounted.getLinked());
                }

            }

            // update weapon bay selector
            int chosen = m_chBayWeapon.getSelectedIndex();
            m_chBayWeapon.removeAllItems();
            if (!(wtype instanceof BayWeapon) || !entity.usesWeaponBays()) {
                m_chBayWeapon.setEnabled(false);
            } else {
                m_chBayWeapon.setEnabled(true);
                for (int wId : mounted.getBayWeapons()) {
                    Mounted curWeapon = entity.getEquipment(wId);
                    if (null == curWeapon) {
                        continue;
                    }

                    m_chBayWeapon.addItem(formatBayWeapon(curWeapon));
                }

                if (chosen == -1) {
                    m_chBayWeapon.setSelectedIndex(0);
                } else {
                    m_chBayWeapon.setSelectedIndex(chosen);
                }
            }

            // update ammo selector
            ((DefaultComboBoxModel) m_chAmmo.getModel()).removeAllElements();
            Mounted oldmount = mounted;
            if (wtype instanceof BayWeapon) {
                int n = m_chBayWeapon.getSelectedIndex();
                if (n == -1) {
                    n = 0;
                }
                mounted = entity.getEquipment(mounted.getBayWeapons()
                        .elementAt(n));
                wtype = (WeaponType) mounted.getType();
            }
            if (wtype.getAmmoType() == AmmoType.T_NA) {
                m_chAmmo.setEnabled(false);
            } else if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                if (mounted.getLinked().getShotsLeft() == 1) {
                    m_chAmmo.addItem(formatAmmo(mounted.getLinked()));
                    m_chAmmo.setEnabled(true);
                } else {
                    m_chAmmo.setEnabled(false);
                }
            } else {
                if (!(entity instanceof Infantry)
                        || (entity instanceof BattleArmor)) {
                    m_chAmmo.setEnabled(true);
                } else {
                    m_chAmmo.setEnabled(false);
                }
                vAmmo = new ArrayList<Mounted>();
                int nCur = -1;
                int i = 0;
                for (Mounted mountedAmmo : entity.getAmmo()) {
                    AmmoType atype = (AmmoType) mountedAmmo.getType();
                    // for all aero units other than fighters,
                    // ammo must be located in the same place to be usable
                    boolean same = true;
                    if ((entity instanceof SmallCraft)
                            || (entity instanceof Jumpship)) {
                        same = (mounted.getLocation() == mountedAmmo
                                .getLocation());
                    }

                    boolean rightBay = true;
                    if (entity.usesWeaponBays()
                            && !(entity instanceof FighterSquadron)) {
                        rightBay = oldmount.ammoInBay(entity
                                .getEquipmentNum(mountedAmmo));
                    }

                    if (mountedAmmo.isAmmoUsable() && same && rightBay
                            && (atype.getAmmoType() == wtype.getAmmoType())
                            && (atype.getRackSize() == wtype.getRackSize())) {

                        vAmmo.add(mountedAmmo);
                        m_chAmmo.addItem(formatAmmo(mountedAmmo));
                        if (mounted.getLinked().equals(mountedAmmo)) {
                            nCur = i;
                        }
                        i++;
                    }
                }
                if (nCur != -1) {
                    m_chAmmo.setSelectedIndex(nCur);
                }
            }

            // send event to other parts of the UI which care
            processMechDisplayEvent(new MechDisplayEvent(this, entity, mounted));
            onResize();
        }

        private Object formatAmmo(Mounted m) {
            StringBuffer sb = new StringBuffer(64);
            int ammoIndex = m.getDesc().indexOf(
                    Messages.getString("MechDisplay.0")); //$NON-NLS-1$
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE) {
                sb.append('[').append(entity.getLocationAbbr(loc)).append("] "); //$NON-NLS-1$
            }
            if (ammoIndex == -1) {
                sb.append(m.getDesc());
            } else {
                sb.append(m.getDesc().substring(0, ammoIndex));
                sb.append(m.getDesc().substring(ammoIndex + 4));
            }
            final String ammoString = sb.toString();
            return new Object() {
                @Override
                public String toString() {
                    return ammoString;
                }
            };
        }

        private String formatBayWeapon(Mounted m) {
            StringBuffer sb = new StringBuffer(64);
            sb.append(m.getDesc());
            return sb.toString();
        }

        /**
         * Update the range display for the selected ammo.
         *
         * @param atype
         *            - the <code>AmmoType</code> of the weapon's loaded ammo.
         */
        private void updateRangeDisplayForAmmo(Mounted mAmmo) {

            AmmoType atype = (AmmoType) mAmmo.getType();
            // Only override the display for the various ATM and MML ammos
            if (atype.getAmmoType() == AmmoType.T_ATM) {
                if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    wMinR.setText("4"); //$NON-NLS-1$
                    wShortR.setText("1 - 9"); //$NON-NLS-1$
                    wMedR.setText("10 - 18"); //$NON-NLS-1$
                    wLongR.setText("19 - 27"); //$NON-NLS-1$
                    wExtR.setText("28 - 36"); //$NON-NLS-1$
                } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                    wMinR.setText("---"); //$NON-NLS-1$
                    wShortR.setText("1 - 3"); //$NON-NLS-1$
                    wMedR.setText("4 - 6"); //$NON-NLS-1$
                    wLongR.setText("7 - 9"); //$NON-NLS-1$
                    wExtR.setText("10 - 12"); //$NON-NLS-1$
                } else {
                    wMinR.setText("4"); //$NON-NLS-1$
                    wShortR.setText("1 - 5"); //$NON-NLS-1$
                    wMedR.setText("6 - 10"); //$NON-NLS-1$
                    wLongR.setText("11 - 15"); //$NON-NLS-1$
                    wExtR.setText("16 - 20"); //$NON-NLS-1$
                }
            } // End weapon-is-ATM
            else if (atype.getAmmoType() == AmmoType.T_MML) {
                if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                    wMinR.setText("6"); //$NON-NLS-1$
                    wShortR.setText("1 - 7"); //$NON-NLS-1$
                    wMedR.setText("8 - 14"); //$NON-NLS-1$
                    wLongR.setText("15 - 21"); //$NON-NLS-1$
                    wExtR.setText("21 - 28"); //$NON-NLS-1$
                } else {
                    wMinR.setText("---"); //$NON-NLS-1$
                    wShortR.setText("1 - 3"); //$NON-NLS-1$
                    wMedR.setText("4 - 6"); //$NON-NLS-1$
                    wLongR.setText("7 - 9"); //$NON-NLS-1$
                    wExtR.setText("10 - 12"); //$NON-NLS-1$
                }
            }

            // Min range 0 for hotload
            if (mAmmo.isHotLoaded()) {
                wMinR.setText("---");
            }

            onResize();
        } // End private void updateRangeDisplayForAmmo( AmmoType )

        private void updateAttackValues(WeaponType wtype, Mounted wAmmo) {
            // update Attack Values and change range
            int avShort = wtype.getRoundShortAV();
            int avMed = wtype.getRoundMedAV();
            int avLong = wtype.getRoundLongAV();
            int avExt = wtype.getRoundExtAV();
            int maxr = wtype.getMaxRange();

            // change range and attack values based upon ammo
            if (null != wAmmo) {
                AmmoType atype = (AmmoType) wAmmo.getType();
                double[] changes = changeAttackValues(atype, avShort, avMed,
                        avLong, avExt, maxr);
                avShort = (int) changes[0];
                avMed = (int) changes[1];
                avLong = (int) changes[2];
                avExt = (int) changes[3];
                maxr = (int) changes[4];
            }

            // set default values in case if statement stops
            wShortAVR.setText("---"); //$NON-NLS-1$
            wMedAVR.setText("---"); //$NON-NLS-1$
            wLongAVR.setText("---"); //$NON-NLS-1$
            wExtAVR.setText("---"); //$NON-NLS-1$
            wShortR.setText("---"); //$NON-NLS-1$
            wMedR.setText("---"); //$NON-NLS-1$
            wLongR.setText("---"); //$NON-NLS-1$
            wExtR.setText("---"); //$NON-NLS-1$
            // every weapon gets at least short range
            wShortAVR.setText(Integer.toString(avShort));
            if (wtype.isCapital()) {
                wShortR.setText("1-12"); //$NON-NLS-1$
            } else {
                wShortR.setText("1-6"); //$NON-NLS-1$
            }
            if (maxr > WeaponType.RANGE_SHORT) {
                wMedAVR.setText(Integer.toString(avMed));
                if (wtype.isCapital()) {
                    wMedR.setText("13-24"); //$NON-NLS-1$
                } else {
                    wMedR.setText("7-12"); //$NON-NLS-1$
                }
            }
            if (maxr > WeaponType.RANGE_MED) {
                wLongAVR.setText(Integer.toString(avLong));
                if (wtype.isCapital()) {
                    wLongR.setText("25-40"); //$NON-NLS-1$
                } else {
                    wLongR.setText("13-20"); //$NON-NLS-1$
                }
            }
            if (maxr > WeaponType.RANGE_LONG) {
                wExtAVR.setText(Integer.toString(avExt));
                if (wtype.isCapital()) {
                    wExtR.setText("41-50"); //$NON-NLS-1$
                } else {
                    wExtR.setText("21-25"); //$NON-NLS-1$
                }
            }

            onResize();
        }

        private double[] changeAttackValues(AmmoType atype, double avShort,
                double avMed, double avLong, double avExt, int maxr) {

            if (AmmoType.T_ATM == atype.getAmmoType()) {
                if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    maxr = WeaponType.RANGE_EXT;
                    avShort = avShort / 2;
                    avMed = avMed / 2;
                    avLong = avMed;
                    avExt = avMed;
                } else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                    maxr = WeaponType.RANGE_SHORT;
                    avShort = avShort + (avShort / 2);
                    avMed = 0;
                    avLong = 0;
                    avExt = 0;
                }
            } // End weapon-is-ATM
            else if (atype.getAmmoType() == AmmoType.T_MML) {
                // first check for artemis
                int bonus = 0;
                if (atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
                    int rack = atype.getRackSize();
                    if (rack == 5) {
                        bonus += 1;
                    } else if (rack >= 7) {
                        bonus += 2;
                    }
                    avShort = avShort + bonus;
                    avMed = avMed + bonus;
                    avLong = avLong + bonus;
                }
                if (!atype.hasFlag(AmmoType.F_MML_LRM)) {
                    maxr = WeaponType.RANGE_SHORT;
                    avShort = avShort * 2;
                    avMed = 0;
                    avLong = 0;
                    avExt = 0;
                }
            } // end weapon is MML
            else if ((atype.getAmmoType() == AmmoType.T_LRM)
                    || (atype.getAmmoType() == AmmoType.T_SRM)) {

                if (atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
                    if (atype.getAmmoType() == AmmoType.T_LRM) {
                        int bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
                        avShort = avShort + bonus;
                        avMed = avMed + bonus;
                        avLong = avLong + bonus;
                    }
                    if (atype.getAmmoType() == AmmoType.T_SRM) {
                        avShort = avShort + 2;
                    }
                }
            } else if (atype.getAmmoType() == AmmoType.T_AC_LBX) {
                if (atype.getMunitionType() == AmmoType.M_CLUSTER) {
                    int newAV = (int) Math.floor(0.6 * atype.getRackSize());
                    avShort = newAV;
                    if (avMed > 0) {
                        avMed = newAV;
                    }
                    if (avLong > 0) {
                        avLong = newAV;
                    }
                    if (avExt > 0) {
                        avExt = newAV;
                    }
                }
            } else if (atype.getAmmoType() == AmmoType.T_AR10) {
                if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    avShort = 4;
                    avMed = 4;
                    avLong = 4;
                    avExt = 4;
                } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    avShort = 3;
                    avMed = 3;
                    avLong = 3;
                    avExt = 3;
                } else {
                    avShort = 2;
                    avMed = 2;
                    avLong = 2;
                    avExt = 2;
                }
            }

            double[] result = { avShort, avMed, avLong, avExt, maxr };
            return result;

        }

        private void compileWeaponBay(Mounted weapon, boolean isCapital) {

            Vector<Integer> bayWeapons = weapon.getBayWeapons();
            WeaponType wtype = (WeaponType) weapon.getType();

            // set default values in case if statement stops
            wShortAVR.setText("---"); //$NON-NLS-1$
            wMedAVR.setText("---"); //$NON-NLS-1$
            wLongAVR.setText("---"); //$NON-NLS-1$
            wExtAVR.setText("---"); //$NON-NLS-1$
            wShortR.setText("---"); //$NON-NLS-1$
            wMedR.setText("---"); //$NON-NLS-1$
            wLongR.setText("---"); //$NON-NLS-1$
            wExtR.setText("---"); //$NON-NLS-1$

            int heat = 0;
            double avShort = 0;
            double avMed = 0;
            double avLong = 0;
            double avExt = 0;
            int maxr = WeaponType.RANGE_SHORT;

            for (int wId : bayWeapons) {
                Mounted m = entity.getEquipment(wId);
                if (!m.isBreached()
                        && !m.isDestroyed()
                        && !m.isJammed()
                        && ((m.getLinked() == null) || (m.getLinked()
                                .getShotsLeft() > 0))) {
                    WeaponType bayWType = ((WeaponType) m.getType());
                    heat = heat + m.getCurrentHeat();
                    double mAVShort = bayWType.getShortAV();
                    double mAVMed = bayWType.getMedAV();
                    double mAVLong = bayWType.getLongAV();
                    double mAVExt = bayWType.getExtAV();
                    int mMaxR = bayWType.getMaxRange();

                    // deal with any ammo adjustments
                    if (null != m.getLinked()) {
                        double[] changes = changeAttackValues((AmmoType) m
                                .getLinked().getType(), mAVShort, mAVMed,
                                mAVLong, mAVExt, mMaxR);
                        mAVShort = changes[0];
                        mAVMed = changes[1];
                        mAVLong = changes[2];
                        mAVExt = changes[3];
                        mMaxR = (int) changes[4];
                    }

                    avShort = avShort + mAVShort;
                    avMed = avMed + mAVMed;
                    avLong = avLong + mAVLong;
                    avExt = avExt + mAVExt;
                    if (mMaxR > maxr) {
                        maxr = mMaxR;
                    }
                }
            }
            // check for bracketing
            double mult = 1.0;
            if (wtype.hasModes() && weapon.curMode().equals("Bracket 80%")) {
                mult = 0.8;
            }
            if (wtype.hasModes() && weapon.curMode().equals("Bracket 60%")) {
                mult = 0.6;
            }
            if (wtype.hasModes() && weapon.curMode().equals("Bracket 40%")) {
                mult = 0.4;
            }
            avShort = mult * avShort;
            avMed = mult * avMed;
            avLong = mult * avLong;
            avExt = mult * avExt;

            wHeatR.setText(Integer.toString(heat));
            wShortAVR.setText(Integer.toString((int) Math.ceil(avShort)));
            if (isCapital) {
                wShortR.setText("1-12"); //$NON-NLS-1$
            } else {
                wShortR.setText("1-6"); //$NON-NLS-1$
            }
            if (maxr > WeaponType.RANGE_SHORT) {
                wMedAVR.setText(Integer.toString((int) Math.ceil(avMed)));
                if (isCapital) {
                    wMedR.setText("13-24"); //$NON-NLS-1$
                } else {
                    wMedR.setText("7-12"); //$NON-NLS-1$
                }
            }
            if (maxr > WeaponType.RANGE_MED) {
                wLongAVR.setText(Integer.toString((int) Math.ceil(avLong)));
                if (isCapital) {
                    wLongR.setText("25-40"); //$NON-NLS-1$
                } else {
                    wLongR.setText("13-20"); //$NON-NLS-1$
                }
            }
            if (maxr > WeaponType.RANGE_LONG) {
                wExtAVR.setText(Integer.toString((int) Math.ceil(avExt)));
                if (isCapital) {
                    wExtR.setText("41-50"); //$NON-NLS-1$
                } else {
                    wExtR.setText("21-25"); //$NON-NLS-1$
                }
            }
            onResize();
        }

        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            if (event.getSource().equals(weaponList)) {
                m_chBayWeapon.removeAllItems();
                displaySelected();
            }
            onResize();
        }

        public void actionPerformed(ActionEvent ev) {
            if (ev.getSource().equals(m_chAmmo)
                    && (m_chAmmo.getSelectedIndex() != -1)) {
                // only change our own units
                if (!clientgui.getClient().getLocalPlayer().equals(
                        entity.getOwner())) {
                    return;
                }
                int n = weaponList.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                Mounted mWeap = entity.getWeaponList().get(n);
                Mounted oldWeap = mWeap;
                // if this is a weapon bay, then this is not what we want
                boolean isBay = false;
                if (mWeap.getType() instanceof BayWeapon) {
                    // this is not the weapon we are looking for
                    isBay = true;
                    n = m_chBayWeapon.getSelectedIndex();
                    if (n == -1) {
                        return;
                    }
                    mWeap = entity.getEquipment(mWeap.getBayWeapons()
                            .elementAt(n));
                }

                Mounted oldAmmo = mWeap.getLinked();
                Mounted mAmmo = vAmmo.get(m_chAmmo.getSelectedIndex());
                entity.loadWeapon(mWeap, mAmmo);

                // Refresh for hot load change
                if ((((oldAmmo == null) || !oldAmmo.isHotLoaded()) && mAmmo
                        .isHotLoaded())
                        || ((oldAmmo != null) && oldAmmo.isHotLoaded() && !mAmmo
                                .isHotLoaded())) {
                    displayMech(entity);
                    weaponList.setSelectedIndex(n);
                    displaySelected();
                }

                // Update the range display to account for the weapon's loaded
                // ammo.
                updateRangeDisplayForAmmo(mAmmo);
                if (entity instanceof Aero) {
                    WeaponType wtype = (WeaponType) mWeap.getType();
                    if (isBay) {
                        compileWeaponBay(oldWeap, wtype.isCapital());
                    } else {
                        // otherwise I need to replace range display with
                        // standard ranges and attack values
                        updateAttackValues(wtype, mAmmo);
                    }
                }

                // When in the Firing Phase, update the targeting information.
                // TODO: make this an accessor function instead of a member
                // access.
                if (clientgui.curPanel instanceof FiringDisplay) {
                    ((FiringDisplay) clientgui.curPanel).updateTarget();
                } else if (clientgui.curPanel instanceof TargetingPhaseDisplay) {
                    ((TargetingPhaseDisplay) clientgui.curPanel).updateTarget();
                }
                // Alert the server of the update.
                clientgui.getClient().sendAmmoChange(entity.getId(),
                        entity.getEquipmentNum(mWeap),
                        entity.getEquipmentNum(mAmmo));
                displaySelected();
            } else if (ev.getSource().equals(m_chBayWeapon)
                    && (m_chBayWeapon.getItemCount() > 0)) {
                int n = weaponList.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                displaySelected();
            }
            onResize();
        }
    }

    /**
     * This class shows the critical hits and systems for a mech
     */
    private class SystemPanel extends PicMap implements ItemListener,
            ActionListener, ListSelectionListener {
        /**
         *
         */
        private static final long serialVersionUID = 6660316427898323590L;

        private static final String IMAGE_DIR = "data/images/widgets";

        private JLabel locLabel;
        private JLabel slotLabel;
        private JLabel modeLabel;
        private JLabel unitLabel;
        private JList slotList;
        private JList locList;
        private JList unitList;

        private JComboBox m_chMode;
        private JButton m_bDumpAmmo;

        private Entity en;
        private Vector<Entity> entities = new Vector<Entity>();

        private int minTopMargin = 8;
        private int minLeftMargin = 8;

        SystemPanel() {
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

            locList = new JList(new DefaultListModel());
            locList.setOpaque(false);
            locList.addListSelectionListener(this);

            slotList = new JList(new DefaultListModel());
            slotList.setOpaque(false);
            slotList.addListSelectionListener(this);

            unitList = new JList(new DefaultListModel());
            unitList.setOpaque(false);
            unitList.addListSelectionListener(this);

            m_chMode = new JComboBox();
            m_chMode.addItem("   "); //$NON-NLS-1$
            m_chMode.setEnabled(false);
            m_chMode.addItemListener(this);

            m_bDumpAmmo = new JButton(Messages
                    .getString("MechDisplay.m_bDumpAmmo")); //$NON-NLS-1$
            m_bDumpAmmo.setEnabled(false);
            m_bDumpAmmo.setActionCommand("dump"); //$NON-NLS-1$
            m_bDumpAmmo.addActionListener(this);

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
            gridbag.setConstraints(slotList, c);
            add(slotList);

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
            int loc = locList.getSelectedIndex();
            int slot = slotList.getSelectedIndex();
            if ((loc == -1) || (slot == -1)) {
                return null;
            }
            return en.getCritical(loc, slot);
        }

        private Mounted getSelectedEquipment() {
            final CriticalSlot cs = getSelectedCritical();
            if (cs == null) {
                return null;
            }
            if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                return null;
            }
            return en.getEquipment(cs.getIndex());
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
        public void displayMech(Entity en) {
            this.en = en;
            entities.clear();
            entities.add(en);
            ((DefaultListModel) unitList.getModel()).removeAllElements();
            ((DefaultListModel) unitList.getModel()).addElement(Messages
                    .getString("MechDisplay.Ego"));
            for (Entity loadee : en.getLoadedUnits()) {
                ((DefaultListModel) unitList.getModel()).addElement(loadee
                        .getModel());
                entities.add(loadee);
            }
            unitList.setSelectedIndex(0);
            displayLocations();
            displaySlots();
        }

        private void displayLocations() {
            ((DefaultListModel) locList.getModel()).removeAllElements();
            for (int i = 0; i < en.locations(); i++) {
                if (en.getNumberOfCriticals(i) > 0) {
                    ((DefaultListModel) locList.getModel()).insertElementAt(en
                            .getLocationName(i), i);
                }
            }
            locList.setSelectedIndex(0);
            displaySlots();
        }

        private void displaySlots() {
            int loc = locList.getSelectedIndex();
            ((DefaultListModel) slotList.getModel()).removeAllElements();
            for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
                final CriticalSlot cs = en.getCritical(loc, i);
                StringBuffer sb = new StringBuffer(32);
                if (cs == null) {
                    sb.append("---"); //$NON-NLS-1$
                } else {
                    switch (cs.getType()) {
                    case CriticalSlot.TYPE_SYSTEM:
                        sb.append(cs.isDestroyed() ? "*" : "")//$NON-NLS-1$ //$NON-NLS-2$
                                .append(cs.isBreached() ? "x" : ""); //$NON-NLS-1$ //$NON-NLS-2$
                        // Protomechs have different systme names.
                        if (en instanceof Protomech) {
                            sb.append(Protomech.systemNames[cs.getIndex()]);
                        } else {
                            sb.append(((Mech) en).getSystemName(cs.getIndex()));
                        }
                        break;
                    case CriticalSlot.TYPE_EQUIPMENT:
                        Mounted m = en.getEquipment(cs.getIndex());
                        sb
                                .append(cs.isDestroyed() ? "*" : "").append(cs.isBreached() ? "x" : "").append(m.getDesc()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        if (m.isHotLoaded()) {
                            sb.append(Messages
                                    .getString("MechDisplay.isHotLoaded")); //$NON-NLS-1$
                        }
                        if (m.getType().hasModes()) {
                            sb
                                    .append(" (").append(m.curMode().getDisplayableName()).append(')'); //$NON-NLS-1$
                            if ((m.getType() instanceof MiscType)
                                    && ((MiscType) m.getType()).isShield()) {
                                sb.append(" " + m.getDamageAbsorption(en, loc)
                                        + '/'
                                        + m.getCurrentDamageCapacity(en, loc)
                                        + ')');
                            }
                        }
                        break;
                    default:
                    }
                }
                ((DefaultListModel) slotList.getModel()).addElement(sb
                        .toString());
            }
            onResize();
        }

        //
        // ItemListener
        //
        public void itemStateChanged(ItemEvent ev) {
            if (ev.getItemSelectable() == m_chMode) {
                Mounted m = getSelectedEquipment();
                CriticalSlot cs = getSelectedCritical();
                if ((m != null) && m.getType().hasModes()) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType()).isShield()
                                && (clientgui.getClient().game.getPhase() != IGame.Phase.PHASE_FIRING)) {
                            clientgui.systemMessage(Messages.getString(
                                    "MechDisplay.ShieldModePhase", null));//$NON-NLS-1$
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType()).isVibroblade()
                                && (clientgui.getClient().game.getPhase() != IGame.Phase.PHASE_PHYSICAL)) {
                            clientgui.systemMessage(Messages.getString(
                                    "MechDisplay.VibrobladeModePhase", null));//$NON-NLS-1$
                            return;
                        }

                        if ((m.getType() instanceof MiscType)
                                && ((MiscType) m.getType())
                                        .hasSubType(MiscType.S_RETRACTABLE_BLADE)
                                && (clientgui.getClient().game.getPhase() != IGame.Phase.PHASE_MOVEMENT)) {
                            clientgui
                                    .systemMessage(Messages
                                            .getString(
                                                    "MechDisplay.RetractableBladeModePhase", null));//$NON-NLS-1$
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
                                                    "MechDisplay.switched", new Object[] { m.getName(), m.curMode().getDisplayableName() }));//$NON-NLS-1$
                        } else {
                            if (IGame.Phase.PHASE_DEPLOYMENT == clientgui
                                    .getClient().game.getPhase()) {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.willSwitchAtStart", new Object[] { m.getName(), m.pendingMode().getDisplayableName() }));//$NON-NLS-1$
                            } else {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.willSwitchAtEnd", new Object[] { m.getName(), m.pendingMode().getDisplayableName() }));//$NON-NLS-1$
                            }
                        }
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
                            if (mech.getCockpitStatus() == mech
                                    .getCockpitStatusNextRound()) {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.switched", new Object[] { "Cockpit", m_chMode.getSelectedItem() }));//$NON-NLS-1$
                            } else {
                                clientgui
                                        .systemMessage(Messages
                                                .getString(
                                                        "MechDisplay.willSwitchAtEnd", new Object[] { "Cockpit", m_chMode.getSelectedItem() }));//$NON-NLS-1$
                            }
                        }
                    }
                }
            }
            onResize();
        }

        // ActionListener
        public void actionPerformed(ActionEvent ae) {
            if ("dump".equals(ae.getActionCommand())) { //$NON-NLS-1$
                Mounted m = getSelectedEquipment();
                boolean bOwner = clientgui.getClient().getLocalPlayer().equals(
                        en.getOwner());
                if ((m == null) || !bOwner || !(m.getType() instanceof AmmoType)
                        || (m.getShotsLeft() <= 0)) {
                    return;
                }

                boolean bDumping;
                boolean bConfirmed;

                if (m.isPendingDump()) {
                    bDumping = false;
                    String title = Messages
                            .getString("MechDisplay.CancelDumping.title"); //$NON-NLS-1$
                    String body = Messages
                            .getString(
                                    "MechDisplay.CancelDumping.message", new Object[] { m.getName() }); //$NON-NLS-1$
                    bConfirmed = clientgui.doYesNoDialog(title, body);
                } else {
                    bDumping = true;
                    String title = Messages.getString("MechDisplay.Dump.title"); //$NON-NLS-1$
                    String body = Messages
                            .getString(
                                    "MechDisplay.Dump.message", new Object[] { m.getName() }); //$NON-NLS-1$
                    bConfirmed = clientgui.doYesNoDialog(title, body);
                }

                if (bConfirmed) {
                    m.setPendingDump(bDumping);
                    clientgui.getClient().sendModeChange(en.getId(),
                            en.getEquipmentNum(m), bDumping ? -1 : 0);
                }
            }
            onResize();
        }

        private void setBackGround() {
            Image tile = getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            int b = BackGroundDrawer.TILING_BOTH;
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL
                    | BackGroundDrawer.VALIGN_TOP;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL
                    | BackGroundDrawer.VALIGN_BOTTOM;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                    | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                    | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/br_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

        }

        public void valueChanged(ListSelectionEvent event) {
            if (event.getSource().equals(unitList)) {
                if (null != getSelectedEntity()) {
                    en = getSelectedEntity();
                    ((DefaultComboBoxModel) m_chMode.getModel())
                            .removeAllElements();
                    m_chMode.setEnabled(false);
                    displayLocations();
                }
            } else if (event.getSource().equals(locList)) {
                ((DefaultComboBoxModel) m_chMode.getModel())
                        .removeAllElements();
                m_chMode.setEnabled(false);
                displaySlots();
            } else if (event.getSource().equals(slotList)) {
                m_bDumpAmmo.setEnabled(false);
                m_chMode.setEnabled(false);
                Mounted m = getSelectedEquipment();

                boolean bOwner = clientgui.getClient().getLocalPlayer().equals(
                        en.getOwner());
                if ((m != null)
                        && bOwner
                        && (m.getType() instanceof AmmoType)
                        && !m.getType().hasInstantModeSwitch()
                        && (clientgui.getClient().game.getPhase() != IGame.Phase.PHASE_DEPLOYMENT)
                        && (m.getShotsLeft() > 0) && !m.isDumping()
                        && en.isActive()) {
                    m_bDumpAmmo.setEnabled(true);
                } else if ((m != null) && bOwner && m.getType().hasModes()) {
                    if (!m.isDestroyed() && en.isActive()) {
                        m_chMode.setEnabled(true);
                    }
                    if (!m.isDestroyed()
                            && m.getType().hasFlag(MiscType.F_STEALTH)) {
                        m_chMode.setEnabled(true);
                    }// if the maxtech eccm option is not set then the ECM
                    // should not show anything.
                    if (m.getType().hasFlag(MiscType.F_ECM)
                            && !clientgui.getClient().game.getOptions()
                                    .booleanOption("tacops_eccm")) {
                        ((DefaultComboBoxModel) m_chMode.getModel())
                                .removeAllElements();
                        return;
                    }
                    ((DefaultComboBoxModel) m_chMode.getModel())
                            .removeAllElements();
                    for (Enumeration<EquipmentMode> e = m.getType().getModes(); e
                            .hasMoreElements();) {
                        EquipmentMode em = e.nextElement();
                        m_chMode.addItem(em.getDisplayableName());
                    }
                    m_chMode.setSelectedItem(m.curMode().getDisplayableName());
                } else {
                    CriticalSlot cs = getSelectedCritical();
                    if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        if ((cs.getIndex() == Mech.SYSTEM_COCKPIT)
                                && en.hasEiCockpit() && (en instanceof Mech)) {
                            ((DefaultComboBoxModel) m_chMode.getModel())
                                    .removeAllElements();
                            m_chMode.setEnabled(true);
                            m_chMode.addItem("EI Off");
                            m_chMode.addItem("EI On");
                            m_chMode.addItem("Aimed shot");
                            m_chMode.setSelectedItem(new Integer(((Mech) en)
                                    .getCockpitStatusNextRound()));
                        }
                    }
                }
            }
            onResize();
        }
    }

    /**
     * This class shows information about a unit that doesn't belong elsewhere.
     */
    private class ExtraPanel extends PicMap implements ActionListener,
            ItemListener {

        /**
         *
         */
        private static final long serialVersionUID = -4907296187995261075L;

        private static final String IMAGE_DIR = "data/images/widgets";

        private JLabel curSensorsL;
        private JLabel narcLabel;
        private JLabel unusedL;
        private JLabel carrysL;
        private JLabel heatL;
        private JLabel sinksL;
        private JLabel targSysL;
        private JTextArea unusedR;
        private JTextArea carrysR;
        private JTextArea heatR;
        private JTextArea sinksR;
        private JButton sinks2B;
        private JButton dumpBombs;
        private JList narcList;
        private int myMechId;

        private JComboBox chSensors;

        private Slider prompt;

        private int sinks;
        private boolean dontChange;

        private int minTopMargin = 8;
        private int minLeftMargin = 8;

        ExtraPanel() {
            prompt = null;

            narcLabel = new JLabel(
                    Messages.getString("MechDisplay.AffectedBy"), SwingConstants.CENTER); //$NON-NLS-1$
            narcLabel.setOpaque(false);
            narcLabel.setForeground(Color.WHITE);

            narcList = new JList(new DefaultListModel());

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

            sinksL = new JLabel(Messages
                    .getString("MechDisplay.activeSinksLabel"),
                    SwingConstants.CENTER);
            sinksL.setOpaque(false);
            sinksL.setForeground(Color.WHITE);
            sinksR = new JTextArea("", 1, 25);
            sinksR.setEditable(false);
            sinksR.setOpaque(false);
            sinksR.setForeground(Color.WHITE);

            sinks2B = new JButton(Messages
                    .getString("MechDisplay.configureActiveSinksLabel"));
            sinks2B.setActionCommand("changeSinks");
            sinks2B.addActionListener(this);

            dumpBombs = new JButton(Messages
                    .getString("MechDisplay.DumpBombsLabel"));
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

            targSysL = new JLabel((Messages
                    .getString("MechDisplay.TargSysLabel")).concat(" "),
                    SwingConstants.CENTER);
            targSysL.setForeground(Color.WHITE);
            targSysL.setOpaque(false);

            curSensorsL = new JLabel((Messages
                    .getString("MechDisplay.CurrentSensors")).concat(" "),
                    SwingConstants.CENTER);
            curSensorsL.setForeground(Color.WHITE);
            curSensorsL.setOpaque(false);

            chSensors = new JComboBox();
            chSensors.addItemListener(this);

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

            c.insets = new Insets(1, 9, 1, 9);
            gridbag.setConstraints(targSysL, c);
            add(targSysL);

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
            Image tile = getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            int b = BackGroundDrawer.TILING_BOTH;
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL
                    | BackGroundDrawer.VALIGN_TOP;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL
                    | BackGroundDrawer.VALIGN_BOTTOM;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                    | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                    | BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                    | BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/br_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

        }

        /**
         * updates fields for the specified mech
         */
        public void displayMech(Entity en) {
            // Clear the "Affected By" list.
            ((DefaultListModel) narcList.getModel()).removeAllElements();
            sinks = 0;
            myMechId = en.getId();
            if (clientgui.getClient().getLocalPlayer().getId() != en
                    .getOwnerId()) {
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
            Enumeration<Player> loop = clientgui.getClient().game.getPlayers();
            while (loop.hasMoreElements()) {
                Player player = loop.nextElement();
                int team = player.getTeam();
                if (en.isNarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuffer(Messages
                            .getString("MechDisplay.NARCedBy")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(Player.teamNames[team]).append(']');
                    ((DefaultListModel) narcList.getModel()).addElement(buff
                            .toString());
                }
                if (en.isINarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuffer(Messages
                            .getString("MechDisplay.INarcHoming")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(Player.teamNames[team]).append("] ")//$NON-NLS-1$
                            .append(Messages.getString("MechDisplay.attached"))//$NON-NLS-1$
                            .append('.');
                    ((DefaultListModel) narcList.getModel()).addElement(buff
                            .toString());
                }
            }
            if (en.isINarcedWith(INarcPod.ECM)) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.iNarcECMPodAttached")); //$NON-NLS-1$
                ((DefaultListModel) narcList.getModel()).addElement(buff
                        .toString());
            }
            if (en.isINarcedWith(INarcPod.HAYWIRE)) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.iNarcHaywirePodAttached")); //$NON-NLS-1$
                ((DefaultListModel) narcList.getModel()).addElement(buff
                        .toString());
            }
            if (en.isINarcedWith(INarcPod.NEMESIS)) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.iNarcNemesisPodAttached")); //$NON-NLS-1$
                ((DefaultListModel) narcList.getModel()).addElement(buff
                        .toString());
            }

            // Show inferno track.
            if (en.infernos.isStillBurning()) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.InfernoBurnRemaining")); //$NON-NLS-1$
                buff.append(en.infernos.getTurnsLeftToBurn());
                ((DefaultListModel) narcList.getModel()).addElement(buff
                        .toString());
            }
            if ((en instanceof Tank) && ((Tank) en).isOnFire()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.OnFire"));
            }

            // Show electromagnic interference.
            if (en.isSufferingEMI()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.IsEMId")); //$NON-NLS-1$
            }

            // Show ECM affect.
            Coords pos = en.getPosition();
            if (Compute.isAffectedByECM(en, pos, pos)) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.InEnemyECMField")); //$NON-NLS-1$
            } else if (Compute.isAffectedByAngelECM(en, pos, pos)) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.InEnemyAngelECMField")); //$NON-NLS-1$
            }

            // Active Stealth Armor? If yes, we're under ECM
            if (en.isStealthActive()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.UnderStealth")); //$NON-NLS-1$
            }

            // burdened due to unjettisoned body-mounted missiles on BA?
            if ((en instanceof BattleArmor) && ((BattleArmor) en).isBurdened()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.Burdened")); //$NON-NLS-1$
            }

            // suffering from taser feedback?
            if (en.getTaserFeedBackRounds() > 0) {
                ((DefaultListModel) narcList.getModel())
                        .addElement(en.getTaserFeedBackRounds()
                                + " " + Messages.getString("MechDisplay.TaserFeedBack"));//$NON-NLS-1$
            }

            // taser interference?
            if (en.getTaserInterference() > 0) {
                ((DefaultListModel) narcList.getModel())
                        .addElement("+" + en.getTaserInterference() + " " + Messages.getString("MechDisplay.TaserInterference"));//$NON-NLS-1$
            }

            // Show Turret Locked.
            if ((en instanceof Tank) && !((Tank) en).hasNoTurret()
                    && !en.canChangeSecondaryFacing()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages
                        .getString("MechDisplay.Turretlocked")); //$NON-NLS-1$
            }

            // Show jammed weapons.
            for (Mounted weapon : en.getWeaponList()) {
                if (weapon.isJammed()) {
                    buff = new StringBuffer(weapon.getName());
                    buff.append(Messages.getString("MechDisplay.isJammed")); //$NON-NLS-1$
                    ((DefaultListModel) narcList.getModel()).addElement(buff
                            .toString());
                }
            }

            // Show breached locations.
            for (int loc = 0; loc < en.locations(); loc++) {
                if (en.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    buff = new StringBuffer(en.getLocationName(loc));
                    buff.append(Messages.getString("MechDisplay.Breached")); //$NON-NLS-1$
                    ((DefaultListModel) narcList.getModel()).addElement(buff
                            .toString());
                }
            }

            if (narcList.getModel().getSize() == 0) {
                ((DefaultListModel) narcList.getModel()).addElement(" ");
            }

            // transport values
            String unused = en.getUnusedString();
            if ("".equals(unused)) {
                unused = Messages.getString("MechDisplay.None"); //$NON-NLS-1$
            }
            unusedR.setText(unused);
            Enumeration<Entity> iter = en.getLoadedUnits().elements();
            carrysR.setText(null);
            // boolean hasText = false;
            while (iter.hasMoreElements()) {
                carrysR.append(iter.nextElement().getShortName());
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
                    carrysR.append(Messages
                            .getString("MechDisplay.SearchlightOn")); //$NON-NLS-1$
                } else {
                    carrysR.append(Messages
                            .getString("MechDisplay.SearchlightOff")); //$NON-NLS-1$
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
                            "MechDisplay.activeSinksTextDouble",
                            new Object[] { new Integer(sinks),
                                    new Integer(sinks * 2) }));
                } else {
                    sinksR.append(Messages.getString(
                            "MechDisplay.activeSinksTextSingle",
                            new Object[] { new Integer(sinks) }));
                }

                boolean hasTSM = false;
                boolean mtHeat = false;
                if (((Mech) en).hasTSM()) {
                    hasTSM = true;
                }

                if (clientgui.getClient().game.getOptions().booleanOption(
                        "tacops_heat")) {
                    mtHeat = true;
                }
                heatR.append(HeatEffects
                        .getHeatEffects(en.heat, mtHeat, hasTSM));
            } else {
                // Non-Mechs cannot configure their heatsinks
                sinks2B.setEnabled(false);
            }
/*
            if (en instanceof Aero
                    && ((Aero) en).hasBombs()
                    && IGame.Phase.PHASE_DEPLOYMENT != clientgui.getClient().game
                            .getPhase()) {
                // TODO: I should at some point check and make sure that this
                // unit has any bombs that it could dump
                dumpBombs.setEnabled(!dontChange);
            } else {
            */
                dumpBombs.setEnabled(false);
            //}

            refreshSensorChoices(en);

            if (null != en.getActiveSensor()) {
                curSensorsL.setText((Messages
                        .getString("MechDisplay.CurrentSensors")).concat(" ")
                        .concat(
                                Sensor.getSensorName(en.getActiveSensor()
                                        .getType())));
            } else {
                curSensorsL.setText((Messages
                        .getString("MechDisplay.CurrentSensors")).concat(" "));
            }

            targSysL.setText((Messages.getString("MechDisplay.TargSysLabel"))
                    .concat(" ").concat(
                            MiscType.getTargetSysName(en.getTargSysType())));
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
                chSensors.addItem(Sensor.getSensorName(sensor.getType())
                        + condition);
                if (sensor.getType() == en.getNextSensor().getType()) {
                    chSensors.setSelectedIndex(i);
                }
            }
            chSensors.addItemListener(this);
        }

        public void itemStateChanged(ItemEvent ev) {
            if (ev.getItemSelectable() == chSensors) {
                Entity en = clientgui.getClient().game.getEntity(myMechId);
                en.setNextSensor(en.getSensors().elementAt(
                        chSensors.getSelectedIndex()));
                refreshSensorChoices(en);
                clientgui
                        .systemMessage(Messages
                                .getString(
                                        "MechDisplay.willSwitchAtEnd", new Object[] { "Active Sensors", Sensor.getSensorName(en.getSensors().elementAt(chSensors.getSelectedIndex()).getType()) }));//$NON-NLS-1$
                clientgui.getClient().sendUpdateEntity(
                        clientgui.getClient().game.getEntity(myMechId));
            }
        }

        public void actionPerformed(ActionEvent ae) {
            if ("changeSinks".equals(ae.getActionCommand()) && !dontChange) { //$NON-NLS-1$
                prompt = new Slider(clientgui.frame, Messages
                        .getString("MechDisplay.changeSinks"), Messages
                        .getString("MechDisplay.changeSinks"), sinks, 0,
                        ((Mech) clientgui.getClient().game.getEntity(myMechId))
                                .getNumberOfSinks());
                if (!prompt.showDialog()) {
                    return;
                }
                clientgui.menuBar.actionPerformed(ae);
                int helper = prompt.getValue();

                ((Mech) clientgui.getClient().game.getEntity(myMechId))
                        .setActiveSinksNextRound(helper);
                clientgui.getClient().sendUpdateEntity(
                        clientgui.getClient().game.getEntity(myMechId));
                displayMech(clientgui.getClient().game.getEntity(myMechId));
            }
        }
    }
}
