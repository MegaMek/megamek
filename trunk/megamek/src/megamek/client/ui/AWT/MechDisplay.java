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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.List;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.event.MechDisplayEvent;
import megamek.client.event.MechDisplayListener;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.AeroMapSet;
import megamek.client.ui.AWT.widget.ArmlessMechMapSet;
import megamek.client.ui.AWT.widget.BackGroundDrawer;
import megamek.client.ui.AWT.widget.BattleArmorMapSet;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.client.ui.AWT.widget.CapitalFighterMapSet;
import megamek.client.ui.AWT.widget.DisplayMapSet;
import megamek.client.ui.AWT.widget.GeneralInfoMapSet;
import megamek.client.ui.AWT.widget.GunEmplacementMapSet;
import megamek.client.ui.AWT.widget.InfantryMapSet;
import megamek.client.ui.AWT.widget.JumpshipMapSet;
import megamek.client.ui.AWT.widget.LargeSupportTankMapSet;
import megamek.client.ui.AWT.widget.MechMapSet;
import megamek.client.ui.AWT.widget.MechPanelTabStrip;
import megamek.client.ui.AWT.widget.PMUtil;
import megamek.client.ui.AWT.widget.PicMap;
import megamek.client.ui.AWT.widget.ProtomechMapSet;
import megamek.client.ui.AWT.widget.QuadMapSet;
import megamek.client.ui.AWT.widget.SpheroidMapSet;
import megamek.client.ui.AWT.widget.SquadronMapSet;
import megamek.client.ui.AWT.widget.TankMapSet;
import megamek.client.ui.AWT.widget.TransparentLabel;
import megamek.client.ui.AWT.widget.VTOLMapSet;
import megamek.client.ui.AWT.widget.WarshipMapSet;
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
import megamek.common.weapons.ACWeapon;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.RACWeapon;
import megamek.common.weapons.UACWeapon;

/**
 * Displays the info for a mech. This is also a sort of interface for special
 * movement and firing actions.
 */
public class MechDisplay extends BufferedPanel {
    // buttons & gizmos for top level

    /**
     *
     */
    private static final long serialVersionUID = -4645698758006581017L;

    MechPanelTabStrip tabStrip;

    public BufferedPanel displayP;
    public MovementPanel mPan;
    public ArmorPanel aPan;
    public WeaponPanel wPan;
    public SystemPanel sPan;
    public ExtraPanel ePan;
    private ClientGUI clientgui;

    private Entity currentlyDisplaying = null;
    private Vector<MechDisplayListener> eventListeners = new Vector<MechDisplayListener>();

    /**
     * Creates and lays out a new mech display.
     */
    public MechDisplay(ClientGUI clientgui) {
        super(new GridBagLayout());

        this.clientgui = clientgui;

        tabStrip = new MechPanelTabStrip(this);

        displayP = new BufferedPanel(new CardLayout());
        mPan = new MovementPanel();
        displayP.add("movement", mPan); //$NON-NLS-1$
        aPan = new ArmorPanel();
        displayP.add("armor", aPan); //$NON-NLS-1$
        wPan = new WeaponPanel(clientgui, this);
        displayP.add("weapons", wPan); //$NON-NLS-1$
        sPan = new SystemPanel(clientgui);
        displayP.add("systems", sPan); //$NON-NLS-1$
        ePan = new ExtraPanel(clientgui);
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

        clientgui.mechW.addKeyListener(clientgui.menuBar);
    }

    public void addBag(Component comp, GridBagConstraints c) {
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
        if (s == "movement") { //$NON-NLS-1$
            tabStrip.setTab(0);
        } else if (s == "armor") { //$NON-NLS-1$
            tabStrip.setTab(1);
        } else if (s == "weapons") { //$NON-NLS-1$
            tabStrip.setTab(3);
        } else if (s == "systems") { //$NON-NLS-1$
            tabStrip.setTab(2);
        } else if (s == "extras") { //$NON-NLS-1$
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
        eventListeners.addElement(listener);
    }

    /**
     * Removes the specified board listener.
     *
     * @param listener
     *            the listener.
     */
    public void removeMechDisplayListener(MechDisplayListener listener) {
        eventListeners.removeElement(listener);
    }

    /**
     * Notifies attached listeners of the event.
     *
     * @param event
     *            the mech display event.
     */
    public void processMechDisplayEvent(MechDisplayEvent event) {
        for (int i = 0; i < eventListeners.size(); i++) {
            MechDisplayListener lis = eventListeners.elementAt(i);
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
    class MovementPanel extends PicMap {

        /**
         *
         */
        private static final long serialVersionUID = 4441873286085299525L;

        private GeneralInfoMapSet gi;

        private int minTopMargin = 8;
        private int minLeftMargin = 8;

        public MovementPanel() {
            gi = new GeneralInfoMapSet(this);
            addElement(gi.getContentGroup());
            Vector<BackGroundDrawer> v = gi.getBackgroundDrawers();
            Enumeration<BackGroundDrawer> iter = v.elements();
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
            int dx = Math.round((w - r.width) / 2);
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
    class ArmorPanel extends PicMap {
        /**
         *
         */
        private static final long serialVersionUID = -3574160027530756521L;
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
        private int minTopMargin = 0;
        private int minLeftMargin = 0;
        private int minBottomMargin = 0;
        private int minRightMargin = 0;

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
            jump = new JumpshipMapSet(this);
            sphere = new SpheroidMapSet(this);
            warship = new WarshipMapSet(this);
            squad = new SquadronMapSet(this);
        }

        @Override
        public void onResize() {
            Rectangle r = getContentBounds();
            if (r == null) {
                return;
            }
            int w = Math.round((getSize().width - r.width) / 2);
            int h = Math.round((getSize().height - r.height) / 2);
            int dx = (w < minLeftMargin) ? minLeftMargin : w;
            int dy = (h < minTopMargin) ? minTopMargin : h;
            setContentMargins(dx, dy, minRightMargin, minBottomMargin);
        }

        /**
         * updates fields for the specified entity
         */
        public void displayMech(Entity en) {
            // Look out for a race condition.
            if (null == en) {
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
            if (null == ams) {
                System.err.println("The armor panel is null."); //$NON-NLS-1$
                return;
            }
            ams.setEntity(en);
            addElement(ams.getContentGroup());
            Vector<BackGroundDrawer> v = ams.getBackgroundDrawers();
            Enumeration<BackGroundDrawer> iter = v.elements();
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
    class WeaponPanel extends BufferedPanel implements ItemListener {
        /**
         *
         */
        private static final long serialVersionUID = -3872316514581547799L;

        private static final String IMAGE_DIR = "data/images/widgets";

        public List weaponList;
        public Choice m_chAmmo;
        public Choice m_chBayWeapon;

        public TransparentLabel wAmmo, wNameL, wHeatL, wArcHeatL, wDamL, wMinL,
                wShortL, wMedL, wLongL, wExtL, wAVL, wBayWeapon;
        public TransparentLabel wNameR, wHeatR, wArcHeatR, wDamR, wMinR,
                wShortR, wMedR, wLongR, wExtR, wShortAVR, wMedAVR, wLongAVR,
                wExtAVR;
        public TransparentLabel currentHeatBuildupL, currentHeatBuildupR;

        public TransparentLabel wTargetL, wRangeL, wToHitL;
        public TransparentLabel wTargetR, wRangeR, wToHitR;

        public TextArea toHitText;

        // I need to keep a pointer to the weapon list of the
        // currently selected mech.
        private Vector<Mounted> vAmmo;
        private Entity entity;
        private ClientGUI client;
        private MechDisplay owner;

        private final Font FONT_VALUE = new Font(
                "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayMediumFontSize")); //$NON-NLS-1$

        public WeaponPanel(ClientGUI client, MechDisplay owner) {
            super(new GridBagLayout());

            this.client = client;
            this.owner = owner;

            FontMetrics fm = getFontMetrics(FONT_VALUE);

            Color clr = Color.white;

            // weapon list
            weaponList = new java.awt.List(4, false);
            weaponList.addItemListener(this);
            weaponList.addKeyListener(client.menuBar);

            // layout main panel
            GridBagConstraints c = new GridBagConstraints();

            // adding Weapon List
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(15, 9, 1, 9);
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(weaponList, c);
            add(weaponList);

            // adding Ammo choice + label

            wAmmo = new TransparentLabel(
                    Messages.getString("MechDisplay.Ammo"), fm, clr, TransparentLabel.LEFT); //$NON-NLS-1$
            m_chAmmo = new Choice();
            m_chAmmo.addItemListener(this);
            m_chAmmo.addKeyListener(client.menuBar);

            wBayWeapon = new TransparentLabel(
                    Messages.getString("MechDisplay.Weapon"), fm, clr, TransparentLabel.LEFT); //$NON-NLS-1$
            m_chBayWeapon = new Choice();
            m_chBayWeapon.addItemListener(this);
            m_chBayWeapon.addKeyListener(client.menuBar);

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

            c.gridwidth = 1;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.gridy = 2;
            ((GridBagLayout) getLayout()).setConstraints(wAmmo, c);
            add(wAmmo);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 1;
            c.gridy = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            ((GridBagLayout) getLayout()).setConstraints(m_chAmmo, c);
            add(m_chAmmo);

            // Adding Heat Buildup

            currentHeatBuildupL = new TransparentLabel(
                    Messages.getString("MechDisplay.HeatBuildup"), fm, clr, TransparentLabel.RIGHT); //$NON-NLS-1$
            currentHeatBuildupR = new TransparentLabel(
                    "--", fm, clr, TransparentLabel.LEFT); //$NON-NLS-1$

            c.insets = new Insets(2, 9, 2, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 3;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            ((GridBagLayout) getLayout())
                    .setConstraints(currentHeatBuildupL, c);
            add(currentHeatBuildupL);

            c.insets = new Insets(2, 1, 2, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 2;
            c.anchor = GridBagConstraints.WEST;
            // c.fill = GridBagConstraints.HORIZONTAL;
            ((GridBagLayout) getLayout())
                    .setConstraints(currentHeatBuildupR, c);
            add(currentHeatBuildupR);

            // Adding weapon display labels
            wNameL = new TransparentLabel(
                    Messages.getString("MechDisplay.Name"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wHeatL = new TransparentLabel(
                    Messages.getString("MechDisplay.Heat"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wArcHeatL = new TransparentLabel(
                    Messages.getString("MechDisplay.ArcHeat"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wDamL = new TransparentLabel(
                    Messages.getString("MechDisplay.Damage"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wNameR = new TransparentLabel("", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wHeatR = new TransparentLabel(
                    "--", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wArcHeatR = new TransparentLabel(
                    "--", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wDamR = new TransparentLabel("--", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$

            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 4;
            ((GridBagLayout) getLayout()).setConstraints(wNameL, c);
            add(wNameL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridwidth = 1;
            c.gridx = 2;
            ((GridBagLayout) getLayout()).setConstraints(wHeatL, c);
            add(wHeatL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridwidth = 1;
            c.gridx = 3;
            ((GridBagLayout) getLayout()).setConstraints(wDamL, c);
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
            ((GridBagLayout) getLayout()).setConstraints(wNameR, c);
            add(wNameR);

            c.gridwidth = 1;
            c.gridx = 2;
            ((GridBagLayout) getLayout()).setConstraints(wHeatR, c);
            add(wHeatR);

            c.gridwidth = 1;
            c.gridx = 3;
            ((GridBagLayout) getLayout()).setConstraints(wDamR, c);
            add(wDamR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 4;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wArcHeatR, c);
            add(wArcHeatR);

            // Adding range labels
            wMinL = new TransparentLabel(
                    Messages.getString("MechDisplay.Min"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wShortL = new TransparentLabel(
                    Messages.getString("MechDisplay.Short"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wMedL = new TransparentLabel(
                    Messages.getString("MechDisplay.Med"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wLongL = new TransparentLabel(
                    Messages.getString("MechDisplay.Long"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wExtL = new TransparentLabel(
                    Messages.getString("MechDisplay.Ext"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wAVL = new TransparentLabel(
                    Messages.getString("MechDisplay.AV"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wMinR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wShortR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wMedR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wLongR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wExtR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wShortAVR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wMedAVR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wLongAVR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wExtAVR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$

            c.weightx = 1.0;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 6;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wMinL, c);
            add(wMinL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridx = 1;
            c.gridy = 6;
            ((GridBagLayout) getLayout()).setConstraints(wShortL, c);
            add(wShortL);

            c.gridx = 2;
            c.gridy = 6;
            ((GridBagLayout) getLayout()).setConstraints(wMedL, c);
            add(wMedL);

            // c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 3;
            c.gridy = 6;
            // c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wLongL, c);
            add(wLongL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 4;
            c.gridy = 6;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wExtL, c);
            add(wExtL);
            // ----------------

            c.insets = new Insets(1, 9, 2, 1);
            c.gridx = 0;
            c.gridy = 7;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wMinR, c);
            add(wMinR);

            c.insets = new Insets(1, 1, 2, 1);
            c.gridx = 1;
            c.gridy = 7;
            ((GridBagLayout) getLayout()).setConstraints(wShortR, c);
            add(wShortR);

            c.gridx = 2;
            c.gridy = 7;
            ((GridBagLayout) getLayout()).setConstraints(wMedR, c);
            add(wMedR);

            // c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 3;
            c.gridy = 7;
            // c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wLongR, c);
            add(wLongR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 4;
            c.gridy = 7;
            ((GridBagLayout) getLayout()).setConstraints(wExtR, c);
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
            wTargetL = new TransparentLabel(
                    Messages.getString("MechDisplay.Target"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wRangeL = new TransparentLabel(
                    Messages.getString("MechDisplay.Range"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wToHitL = new TransparentLabel(
                    Messages.getString("MechDisplay.ToHit"), fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$

            wTargetR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wRangeR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$
            wToHitR = new TransparentLabel(
                    "---", fm, clr, TransparentLabel.CENTER); //$NON-NLS-1$

            c.weightx = 0.0;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 9;
            c.gridwidth = 2;
            ((GridBagLayout) getLayout()).setConstraints(wTargetL, c);
            add(wTargetL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 9;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wTargetR, c);
            add(wTargetR);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 10;
            c.gridwidth = 2;
            ((GridBagLayout) getLayout()).setConstraints(wRangeL, c);
            add(wRangeL);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 10;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wRangeR, c);
            add(wRangeR);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 11;
            c.gridwidth = 2;
            ((GridBagLayout) getLayout()).setConstraints(wToHitL, c);
            add(wToHitL);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 11;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wToHitR, c);
            add(wToHitR);

            // to-hit text
            toHitText = new TextArea(
                    "", 2, 20, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
            toHitText.setEditable(false);
            toHitText.addKeyListener(client.menuBar);

            c.insets = new Insets(1, 9, 15, 9);
            c.gridx = 0;
            c.gridy = 12;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(toHitText, c);
            add(toHitText);

            setBackGround();

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
            IGame game = client.getClient().game;

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
            if ((en instanceof Mech)
                    && (en.isStealthActive() || en.isNullSigActive() || en
                            .isVoidSigActive())) {
                currentHeatBuildup += 10; // active stealth/nullsig/void sig
                                          // heat
            }

            if ((en instanceof Mech) && en.isChameleonShieldActive()) {
                currentHeatBuildup += 6;
            }

            for (Mounted m : entity.getEquipment()) {
                int capHeat = 0;
                if (m.hasChargedCapacitor()) {
                    capHeat += 5;
                }
                if (capHeat > 0) {
                    currentHeatBuildup += capHeat;
                }
            }

            // update weapon list
            weaponList.removeAll();
            m_chAmmo.removeAll();
            m_chAmmo.setEnabled(false);
            m_chBayWeapon.removeAll();
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
                    wn.append("/"); //$NON-NLS-1$
                    wn.append(en.getLocationAbbr(mounted.getSecondLocation()));
                }
                wn.append("]"); //$NON-NLS-1$
                // determine shots left & total shots left
                if ((wtype.getAmmoType() != AmmoType.T_NA)
                        && !wtype.hasFlag(WeaponType.F_ONESHOT)) {
                    int shotsLeft = 0;
                    if ((mounted.getLinked() != null)
                            && !mounted.getLinked().isDumping()) {
                        shotsLeft = mounted.getLinked().getShotsLeft();
                    }

                    EquipmentType typeUsed = null;
                    if (null != mounted.getLinked()) {
                        typeUsed = mounted.getLinked().getType();
                    }

                    int totalShotsLeft = entity
                            .getTotalMunitionsOfType(typeUsed);

                    wn.append(" ("); //$NON-NLS-1$
                    wn.append(shotsLeft);
                    wn.append("/"); //$NON-NLS-1$
                    wn.append(totalShotsLeft);
                    wn.append(")"); //$NON-NLS-1$
                }

                // MG rapidfire
                if (mounted.isRapidfire()) {
                    wn.append(Messages.getString("MechDisplay.rapidFire")); //$NON-NLS-1$
                }

                // Hotloaded Missiles/Launchers
                if (mounted.isHotLoaded()) {
                    wn.append(Messages.getString("MechDisplay.isHotLoaded")); //$NON-NLS-1$
                }

                // Fire Mode - lots of things have variable modes
                if (wtype.hasModes()) {
                    wn.append(" ");
                    wn.append(mounted.curMode().getDisplayableName());
                }
                weaponList.add(wn.toString());
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
                heatCapacityStr = heatCap + " [" + heatCapWater + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
                    .setText(heatText + " (" + heatCapacityStr + ")"); //$NON-NLS-1$ //$NON-NLS-2$

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
        }

        /**
         * Selects the weapon at the specified index in the list
         */
        public void selectWeapon(int wn) {
            if (wn == -1) {
                weaponList.select(-1);
                return;
            }
            int index = entity.getWeaponList().indexOf(entity.getEquipment(wn));
            weaponList.select(index);
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
                m_chAmmo.removeAll();
                m_chAmmo.setEnabled(false);
                m_chBayWeapon.removeAll();
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
            if ((ILocationExposureStatus.WET == entity.getLocationStatus(mounted
                    .getLocation()))
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
                wMedR.setText((shortR + 1) + " - " + mediumR); //$NON-NLS-1$
            } else {
                wMedR.setText("" + mediumR); //$NON-NLS-1$
            }
            if (longR - mediumR > 1) {
                wLongR.setText((mediumR + 1) + " - " + longR); //$NON-NLS-1$
            } else {
                wLongR.setText("" + longR); //$NON-NLS-1$
            }
            if (extremeR - longR > 1) {
                wExtR.setText((longR + 1) + " - " + extremeR); //$NON-NLS-1$
            } else {
                wExtR.setText("" + extremeR); //$NON-NLS-1$
            }

            // Update the range display to account for the weapon's loaded ammo.
            if (null != mounted.getLinked()) {
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
            m_chBayWeapon.removeAll();
            if (!(wtype instanceof BayWeapon) || !entity.usesWeaponBays()) {
                m_chBayWeapon.setEnabled(false);
            } else {
                m_chBayWeapon.setEnabled(true);
                for (int wId : mounted.getBayWeapons()) {
                    Mounted curWeapon = entity.getEquipment(wId);
                    if (null == curWeapon) {
                        continue;
                    }

                    m_chBayWeapon.add(formatBayWeapon(curWeapon));
                }
                if (chosen == -1) {
                    m_chBayWeapon.select(0);
                } else {
                    m_chBayWeapon.select(chosen);
                }
            }

            // update ammo selector
            m_chAmmo.removeAll();
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
                    m_chAmmo.add(formatAmmo(mounted.getLinked()));
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
                vAmmo = new Vector<Mounted>();
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

                        vAmmo.addElement(mountedAmmo);
                        m_chAmmo.add(formatAmmo(mountedAmmo));
                        if (mounted.getLinked() == mountedAmmo) {
                            nCur = i;
                        }
                        i++;
                    }
                }
                if (nCur != -1) {
                    m_chAmmo.select(nCur);
                }
            }

            // send event to other parts of the UI which care
            owner.processMechDisplayEvent(new MechDisplayEvent(this, entity,
                    mounted));
        }

        private String formatAmmo(Mounted m) {
            StringBuffer sb = new StringBuffer(64);
            int ammoIndex = m.getDesc().indexOf(
                    Messages.getString("MechDisplay.0")); //$NON-NLS-1$
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE) {
                sb.append("[").append(entity.getLocationAbbr(loc)).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (ammoIndex == -1) {
                sb.append(m.getDesc());
            } else {
                sb.append(m.getDesc().substring(0, ammoIndex));
                sb.append(m.getDesc().substring(ammoIndex + 4));
            }
            return sb.toString();
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
            // Override the display for the various ATM ammos
            if (AmmoType.T_ATM == atype.getAmmoType()) {
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

                // check for santa annas
                if (atype.hasFlag(AmmoType.F_NUCLEAR)) {
                    avShort = 100;
                    avMed = 100;
                    avLong = 100;
                    avExt = 100;
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

                if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                    WeaponType bayWType = ((WeaponType) m.getType());
                    // check for ammo
                    if (bayWType.getAmmoType() != AmmoType.T_NA) {
                        if ((m.getLinked() == null)
                                || (m.getLinked().getShotsLeft() < 1)) {
                            continue;
                        }
                    }
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

        }

        //
        // ItemListener
        //
        public void itemStateChanged(ItemEvent ev) {
            if (ev.getSource().equals(weaponList)) {
                m_chBayWeapon.removeAll();
                displaySelected();
            } else if (ev.getSource().equals(m_chAmmo)) {
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
                Mounted mAmmo = vAmmo.elementAt(m_chAmmo.getSelectedIndex());
                entity.loadWeapon(mWeap, mAmmo);

                // Refresh for hot load change
                if ((((oldAmmo == null) || !oldAmmo.isHotLoaded()) && mAmmo
                        .isHotLoaded())
                        || ((oldAmmo != null) && oldAmmo.isHotLoaded() && !mAmmo
                                .isHotLoaded())) {
                    displayMech(entity);
                    weaponList.select(n);
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
                if (client.curPanel instanceof FiringDisplay) {
                    ((FiringDisplay) client.curPanel).updateTarget();
                } else if (client.curPanel instanceof TargetingPhaseDisplay) {
                    ((TargetingPhaseDisplay) client.curPanel).updateTarget();
                }
                // Alert the server of the update.
                client.getClient().sendAmmoChange(entity.getId(),
                        entity.getEquipmentNum(mWeap),
                        entity.getEquipmentNum(mAmmo));
                displaySelected();
            } else if (ev.getItemSelectable() == m_chBayWeapon) {
                int n = weaponList.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                // I need to change the ammo selected
                displaySelected();
            }
        }
    }

    /**
     * This class shows the critical hits and systems for a mech
     */
    class SystemPanel extends BufferedPanel implements ItemListener,
            ActionListener {
        /**
         *
         */
        private static final long serialVersionUID = -1294456418186738773L;

        private static final String IMAGE_DIR = "data/images/widgets";

        private TransparentLabel locLabel, slotLabel, modeLabel, unitLabel;
        public java.awt.List slotList;
        public java.awt.List locList;
        public java.awt.List unitList;

        public Choice m_chMode;
        public Button m_bDumpAmmo;

        private final Font FONT_VALUE = new Font(
                "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$

        Entity en;
        Vector<Entity> entities = new Vector<Entity>();

        public SystemPanel(ClientGUI clientgui) {
            super();

            FontMetrics fm = getFontMetrics(FONT_VALUE);

            locLabel = new TransparentLabel(
                    Messages.getString("MechDisplay.Location"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$
            slotLabel = new TransparentLabel(
                    Messages.getString("MechDisplay.Slot"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$
            unitLabel = new TransparentLabel(
                    Messages.getString("MechDisplay.Unit"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$

            locList = new List(8, false);
            locList.addItemListener(this);
            locList.addKeyListener(clientgui.menuBar);

            slotList = new List(12, false);
            slotList.addItemListener(this);
            slotList.addKeyListener(clientgui.menuBar);
            // slotList.setEnabled(false);

            unitList = new List(8, false);
            unitList.addItemListener(this);
            unitList.addKeyListener(clientgui.menuBar);

            m_chMode = new Choice();
            m_chMode.add("   "); //$NON-NLS-1$
            m_chMode.setEnabled(false);
            m_chMode.addItemListener(this);
            m_chMode.addKeyListener(clientgui.menuBar);

            m_bDumpAmmo = new Button(Messages
                    .getString("MechDisplay.m_bDumpAmmo")); //$NON-NLS-1$
            m_bDumpAmmo.setEnabled(false);
            m_bDumpAmmo.setActionCommand("dump"); //$NON-NLS-1$
            m_bDumpAmmo.addActionListener(this);
            m_bDumpAmmo.addKeyListener(clientgui.menuBar);

            modeLabel = new TransparentLabel(
                    Messages.getString("MechDisplay.modeLabel"), fm, Color.white, TransparentLabel.RIGHT); //$NON-NLS-1$
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
            c.insets = new Insets(15, 9, 1, 1);
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
        }

        public int getSelectedLocation() {
            return locList.getSelectedIndex();
        }

        public CriticalSlot getSelectedCritical() {
            int loc = locList.getSelectedIndex();
            int slot = slotList.getSelectedIndex();
            if ((loc == -1) || (slot == -1)) {
                return null;
            }
            return en.getCritical(loc, slot);
        }

        public Mounted getSelectedEquipment() {
            final CriticalSlot cs = getSelectedCritical();
            if (null == cs) {
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
            unitList.removeAll();
            unitList.add(Messages.getString("MechDisplay.Ego"));
            for (Entity loadee : en.getLoadedUnits()) {
                unitList.add(loadee.getModel());
                entities.add(loadee);
            }
            unitList.select(0);
            displayLocations();
            displaySlots();
        }

        public void displayLocations() {
            locList.removeAll();
            for (int i = 0; i < en.locations(); i++) {
                if (en.getNumberOfCriticals(i) > 0) {
                    locList.add(en.getLocationName(i), i);
                }
            }
            locList.select(0);
        }

        private void displaySlots() {
            int loc = locList.getSelectedIndex();
            slotList.removeAll();
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
                                    .append(" (").append(m.curMode().getDisplayableName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
                            if ((m.getType() instanceof MiscType)
                                    && ((MiscType) m.getType()).isShield()) {
                                sb.append(" " + m.getDamageAbsorption(en, loc)
                                        + "/"
                                        + m.getCurrentDamageCapacity(en, loc)
                                        + ")");
                            }
                        }
                        break;
                    }
                }
                slotList.add(sb.toString());
            }
        }

        //
        // ItemListener
        //
        public void itemStateChanged(ItemEvent ev) {
            if (ev.getItemSelectable() == unitList) {
                if (null != getSelectedEntity()) {
                    en = getSelectedEntity();
                    m_chMode.removeAll();
                    m_chMode.setEnabled(false);
                    displayLocations();
                    displaySlots();
                }
            } else if (ev.getItemSelectable() == locList) {
                m_chMode.removeAll();
                m_chMode.setEnabled(false);
                displaySlots();
            } else if (ev.getItemSelectable() == slotList) {
                m_bDumpAmmo.setEnabled(false);
                m_chMode.setEnabled(false);
                modeLabel.setEnabled(false);
                Mounted m = getSelectedEquipment();
                boolean carryingBAsOnBack = false;
                if ((en instanceof Mech)
                        && ((en.getExteriorUnitAt(Mech.LOC_CT, true) != null)
                                || (en.getExteriorUnitAt(Mech.LOC_LT, true) != null) || (en
                                .getExteriorUnitAt(Mech.LOC_RT, true) != null))) {
                    carryingBAsOnBack = true;
                }

                boolean bOwner = (clientgui.getClient().getLocalPlayer() == en
                        .getOwner());
                if ((m != null)
                        && bOwner
                        && (m.getType() instanceof AmmoType)
                        && !(m.getType().hasInstantModeSwitch())
                        && (IGame.Phase.PHASE_DEPLOYMENT != clientgui
                                .getClient().game.getPhase())
                        && (m.getShotsLeft() > 0)
                        && !m.isDumping()
                        && en.isActive()
                        && (clientgui.getClient().game.getOptions().intOption(
                                "dumping_from_round") <= clientgui.getClient().game
                                .getRoundCount()) && !carryingBAsOnBack) {
                    m_bDumpAmmo.setEnabled(true);
                    if (clientgui.getClient().game.getOptions().booleanOption(
                            "tacops_hotload")
                            && (en instanceof Tank)
                            && m.getType().hasFlag(AmmoType.F_HOTLOAD)) {
                        m_bDumpAmmo.setEnabled(false);
                        modeLabel.setEnabled(true);
                        m_chMode.setEnabled(true);
                        m_chMode.removeAll();
                        for (Enumeration<EquipmentMode> e = m.getType()
                                .getModes(); e.hasMoreElements();) {
                            EquipmentMode em = e.nextElement();
                            m_chMode.add(em.getDisplayableName());
                        }
                        m_chMode.select(m.curMode().getDisplayableName());
                    }
                } else if ((m != null) && bOwner && m.getType().hasModes()) {
                    if (!m.isDestroyed() && en.isActive()) {
                        m_chMode.setEnabled(true);
                    }
                    if (!m.isDestroyed() && !en.isActive()
                            && en.isCapitalFighter()
                            && en.isPartOfFighterSquadron()) {
                        m_chMode.setEnabled(true);
                    }
                    if (!m.isDestroyed()
                            && m.getType().hasFlag(MiscType.F_STEALTH)) {
                        m_chMode.setEnabled(true);
                    }

                    // If not using tacops Energy Weapon rule then remove all
                    // the dial down statements
                    if (m.getType().hasFlag(WeaponType.F_ENERGY)
                            && (((WeaponType) m.getType()).getAmmoType() == AmmoType.T_NA)
                            && !clientgui.getClient().game.getOptions()
                                    .booleanOption("tacops_energy_weapons")) {
                        m_chMode.removeAll();
                        return;
                    }

                    // If not using tacops Gauss Weapon rule then remove all the
                    // power up/down modes
                    if ((m.getType() instanceof GaussWeapon)
                            && !clientgui.getClient().game.getOptions()
                                    .booleanOption("tacops_gauss_weapons")) {
                        m_chMode.removeAll();
                        return;
                    }
                    // disable rapid fire mode switching for Aeros
                    if (((en instanceof Aero) && (m.getType() instanceof ACWeapon))
                            || (m.getType() instanceof RACWeapon)
                            || (m.getType() instanceof UACWeapon)) {
                        m_chMode.removeAll();
                        return;
                    }
                    modeLabel.setEnabled(true);
                    m_chMode.removeAll();
                    for (Enumeration<EquipmentMode> e = m.getType().getModes(); e
                            .hasMoreElements();) {
                        EquipmentMode em = e.nextElement();
                        m_chMode.add(em.getDisplayableName());
                    }
                    m_chMode.select(m.curMode().getDisplayableName());
                } else {
                    CriticalSlot cs = getSelectedCritical();
                    if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM)) {
                        if ((cs.getIndex() == Mech.SYSTEM_COCKPIT)
                                && en.hasEiCockpit() && (en instanceof Mech)) {
                            m_chMode.removeAll();
                            m_chMode.setEnabled(true);
                            m_chMode.add("EI Off");
                            m_chMode.add("EI On");
                            m_chMode.add("Aimed shot");
                            m_chMode.select(((Mech) en)
                                    .getCockpitStatusNextRound());
                        }
                    }
                }
            } else if (ev.getItemSelectable() == m_chMode) {
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
        }

        // ActionListener
        public void actionPerformed(ActionEvent ae) {
            if (ae.getActionCommand().equals("dump")) { //$NON-NLS-1$
                Mounted m = getSelectedEquipment();
                boolean bOwner = (clientgui.getClient().getLocalPlayer() == en
                        .getOwner());
                if ((m == null) || !bOwner || !(m.getType() instanceof AmmoType)
                        || (m.getShotsLeft() <= 0)) {
                    return;
                }

                boolean bDumping;
                boolean bConfirmed = false;

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

    }

    /**
     * This class shows information about a unit that doesn't belong elsewhere.
     */
    class ExtraPanel extends BufferedPanel implements ItemListener,
            ActionListener {

        /**
         *
         */
        private static final long serialVersionUID = -8727340674330120715L;

        private static final String IMAGE_DIR = "data/images/widgets";

        private TransparentLabel curSensorsL, narcLabel, unusedL, carrysL,
                heatL, sinksL, targSysL;
        public Choice chSensors;
        public TextArea unusedR, carrysR, heatR, sinksR;
        public Button sinks2B;
        public java.awt.List narcList;
        private int myMechId;

        private Slider prompt;

        private int sinks;
        private boolean dontChange;

        private final Font FONT_VALUE = new Font(
                "SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayLargeFontSize")); //$NON-NLS-1$

        public ExtraPanel(ClientGUI clientgui) {
            super();

            prompt = null;

            FontMetrics fm = getFontMetrics(FONT_VALUE);

            curSensorsL = new TransparentLabel((Messages
                    .getString("MechDisplay.CurrentSensors")).concat(" "), fm,
                    Color.white, TransparentLabel.CENTER);

            chSensors = new Choice();
            chSensors.addItemListener(this);

            narcLabel = new TransparentLabel(
                    Messages.getString("MechDisplay.AffectedBy"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$

            narcList = new List(3, false);
            narcList.addKeyListener(clientgui.menuBar);

            // transport stuff
            // unusedL = new Label( "Unused Space:", Label.CENTER );

            unusedL = new TransparentLabel(
                    Messages.getString("MechDisplay.UnusedSpace"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$
            unusedR = new TextArea("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
            unusedR.setEditable(false);
            unusedR.addKeyListener(clientgui.menuBar);

            carrysL = new TransparentLabel(
                    Messages.getString("MechDisplay.Carryng"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$
            carrysR = new TextArea("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
            carrysR.setEditable(false);
            carrysR.addKeyListener(clientgui.menuBar);

            sinksL = new TransparentLabel(Messages
                    .getString("MechDisplay.activeSinksLabel"), fm,
                    Color.white, TransparentLabel.CENTER);
            sinksR = new TextArea("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
            sinksR.setEditable(false);
            sinksR.addKeyListener(clientgui.menuBar);

            sinks2B = new Button(Messages
                    .getString("MechDisplay.configureActiveSinksLabel"));
            sinks2B.setActionCommand("changeSinks");
            sinks2B.addActionListener(this);

            heatL = new TransparentLabel(
                    Messages.getString("MechDisplay.HeatEffects"), fm, Color.white, TransparentLabel.CENTER); //$NON-NLS-1$
            heatR = new TextArea("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$
            heatR.setEditable(false);
            heatR.addKeyListener(clientgui.menuBar);

            targSysL = new TransparentLabel((Messages
                    .getString("MechDisplay.TargSysLabel")).concat(" "), fm,
                    Color.white, TransparentLabel.CENTER);

            // layout choice panel
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();

            gridbag = new GridBagLayout();
            c = new GridBagConstraints();
            setLayout(gridbag);

            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(15, 9, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 1.0;

            c.weighty = 0.0;
            gridbag.setConstraints(curSensorsL, c);
            add(curSensorsL);

            c.weighty = 0.0;
            gridbag.setConstraints(chSensors, c);
            add(chSensors);

            c.weighty = 0.0;
            gridbag.setConstraints(narcLabel, c);
            add(narcLabel);

            c.insets = new Insets(1, 9, 1, 9);
            c.weighty = 1.0;
            gridbag.setConstraints(narcList, c);
            add(narcList);

            c.weighty = 0.0;
            gridbag.setConstraints(unusedL, c);
            add(unusedL);

            c.weighty = 1.0;
            gridbag.setConstraints(unusedR, c);
            add(unusedR);

            c.weighty = 0.0;
            gridbag.setConstraints(carrysL, c);
            add(carrysL);

            c.insets = new Insets(1, 9, 1, 9);
            c.weighty = 1.0;
            gridbag.setConstraints(carrysR, c);
            add(carrysR);

            c.weighty = 0.0;
            gridbag.setConstraints(sinksL, c);
            add(sinksL);

            c.insets = new Insets(1, 9, 1, 9);
            c.weighty = 1.0;
            gridbag.setConstraints(sinksR, c);
            add(sinksR);

            c.weighty = 0.0;
            gridbag.setConstraints(sinks2B, c);
            add(sinks2B);

            c.weighty = 0.0;
            gridbag.setConstraints(heatL, c);
            add(heatL);

            c.insets = new Insets(1, 9, 18, 9);
            c.weighty = 1.0;
            gridbag.setConstraints(heatR, c);
            add(heatR);

            gridbag.setConstraints(targSysL, c);
            add(targSysL);

            setBackGround();

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
            narcList.removeAll();
            sinks = 0;
            myMechId = en.getId();
            if (clientgui.getClient().getLocalPlayer().getId() != en
                    .getOwnerId()) {
                sinks2B.setEnabled(false);
                chSensors.setEnabled(false);
                dontChange = true;
            } else {
                sinks2B.setEnabled(true);
                chSensors.setEnabled(true);
                dontChange = false;
            }
            // Walk through the list of teams. There
            // can't be more teams than players.
            StringBuffer buff = null;
            Enumeration<Player> loop = clientgui.getClient().game.getPlayers();
            while (loop.hasMoreElements()) {
                Player player = loop.nextElement();
                int team = player.getTeam();
                if (en.isNarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuffer(Messages
                            .getString("MechDisplay.NARCedBy")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(Player.teamNames[team]).append("]"); //$NON-NLS-1$
                    narcList.add(buff.toString());
                }
                if (en.isINarcedBy(team) && !player.isObserver()) {
                    buff = new StringBuffer(Messages
                            .getString("MechDisplay.INarcHoming")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(Player.teamNames[team]).append("] ")//$NON-NLS-1$
                            .append(Messages.getString("MechDisplay.attached"))//$NON-NLS-1$
                            .append("."); //$NON-NLS-1$
                    narcList.add(buff.toString());
                }
            }
            if (en.isINarcedWith(INarcPod.ECM)) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.iNarcECMPodAttached")); //$NON-NLS-1$
                narcList.add(buff.toString());
            }
            if (en.isINarcedWith(INarcPod.HAYWIRE)) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.iNarcHaywirePodAttached")); //$NON-NLS-1$
                narcList.add(buff.toString());
            }
            if (en.isINarcedWith(INarcPod.NEMESIS)) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.iNarcNemesisPodAttached")); //$NON-NLS-1$
                narcList.add(buff.toString());
            }

            // Show inferno track.
            if (en.infernos.isStillBurning()) {
                buff = new StringBuffer(Messages
                        .getString("MechDisplay.InfernoBurnRemaining")); //$NON-NLS-1$
                buff.append(en.infernos.getTurnsLeftToBurn());
                narcList.add(buff.toString());
            }
            if ((en instanceof Tank) && ((Tank) en).isOnFire()) {
                narcList.add(Messages.getString("MechDisplay.OnFire"));
            }

            // Show electromagnetic interference.
            if (en.isSufferingEMI()) {
                narcList.add(Messages.getString("MechDisplay.IsEMId")); //$NON-NLS-1$
            }

            // Show ECM affect.
            Coords pos = en.getPosition();
            if (Compute.isAffectedByECM(en, pos, pos)) {
                narcList.add(Messages.getString("MechDisplay.InEnemyECMField")); //$NON-NLS-1$
            } else if (Compute.isAffectedByAngelECM(en, pos, pos)) {
                narcList.add(Messages
                        .getString("MechDisplay.InEnemyAngelECMField")); //$NON-NLS-1$
            }

            // Active Stealth Armor? If yes, we're under ECM
            if (en.isStealthActive()) {
                narcList.add(Messages.getString("MechDisplay.UnderStealth")); //$NON-NLS-1$
            }

            // burdened due to unjettisoned body-mounted missiles on BA?
            if ((en instanceof BattleArmor) && ((BattleArmor) en).isBurdened()) {
                narcList.add(Messages.getString("MechDisplay.Burdened")); //$NON-NLS-1$
            }

            // suffering from taser feedback?
            if (en.getTaserFeedBackRounds() > 0) {
                narcList
                        .add(en.getTaserFeedBackRounds()
                                + " " + Messages.getString("MechDisplay.TaserFeedBack"));//$NON-NLS-1$
            }

            // taser interference?
            if (en.getTaserInterference() > 0) {
                narcList
                        .add("+" + en.getTaserInterference() + " " + Messages.getString("MechDisplay.TaserInterference"));//$NON-NLS-1$
            }

            // Show Turret Locked.
            if ((en instanceof Tank) && !((Tank) en).hasNoTurret()
                    && !en.canChangeSecondaryFacing()) {
                narcList.add(Messages.getString("MechDisplay.Turretlocked")); //$NON-NLS-1$
            }

            // Show jammed weapons.
            for (Mounted weapon : en.getWeaponList()) {
                if (weapon.isJammed()) {
                    buff = new StringBuffer(weapon.getName());
                    buff.append(Messages.getString("MechDisplay.isJammed")); //$NON-NLS-1$
                    narcList.add(buff.toString());
                }
            }

            // Show breached locations.
            for (int loc = 0; loc < en.locations(); loc++) {
                if (ILocationExposureStatus.BREACHED == en
                        .getLocationStatus(loc)) {
                    buff = new StringBuffer(en.getLocationName(loc));
                    buff.append(Messages.getString("MechDisplay.Breached")); //$NON-NLS-1$
                    narcList.add(buff.toString());
                }
            }

            // transport values
            String unused = en.getUnusedString();
            if (unused.equals("")) {
                unused = Messages.getString("MechDisplay.None"); //$NON-NLS-1$
            }
            unusedR.setText(unused);
            Enumeration<Entity> iter = en.getLoadedUnits().elements();
            carrysR.setText(null);
            // boolean hasText = false;
            while (iter.hasMoreElements()) {
                Entity nextUnit = iter.nextElement();
                // if still being recovered, then let player know how long
                if (nextUnit.getRecoveryTurn() > 0) {
                    carrysR.append(nextUnit.getShortName() + " (Recovery Turn "
                            + Integer.toString(6 - nextUnit.getRecoveryTurn())
                            + ")");
                } else {
                    carrysR.append(nextUnit.getShortName());
                }
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

            refreshSensorChoices(en);

            if (null != en.getActiveSensor()) {
                curSensorsL.setText((Messages
                        .getString("MechDisplay.CurrentSensors")).concat(" ")
                        .concat(
                                Sensor.getSensorName(en.getActiveSensor()
                                        .getType())));
            } else {
                curSensorsL.setText((Messages
                        .getString("MechDisplay.CurrentSensors"))
                        .concat(" None"));
            }

            targSysL.setText((Messages.getString("MechDisplay.TargSysLabel"))
                    .concat(" ").concat(
                            MiscType.getTargetSysName(en.getTargSysType())));
        } // End public void displayMech( Entity )

        private void refreshSensorChoices(Entity en) {
            chSensors.removeAll();
            for (int i = 0; i < en.getSensors().size(); i++) {
                Sensor sensor = en.getSensors().elementAt(i);
                String condition = "";
                if (sensor.isBAP() && !en.hasBAP(false)) {
                    condition = " (Disabled)";
                }
                chSensors.add(Sensor.getSensorName(sensor.getType())
                        + condition);
                if (sensor.getType() == en.getNextSensor().getType()) {
                    chSensors.select(i);
                }
            }
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
            if (ae.getActionCommand().equals("changeSinks") && !dontChange) { //$NON-NLS-1$
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