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

import megamek.client.event.MechDisplayEvent;
import megamek.client.event.MechDisplayListener;
import megamek.client.ui.AWT.Messages;
import megamek.client.ui.swing.widget.ArmlessMechMapSet;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.BattleArmorMapSet;
import megamek.client.ui.swing.widget.BufferedPanel;
import megamek.client.ui.swing.widget.DisplayMapSet;
import megamek.client.ui.swing.widget.GeneralInfoMapSet;
import megamek.client.ui.swing.widget.GunEmplacementMapSet;
import megamek.client.ui.swing.widget.InfantryMapSet;
import megamek.client.ui.swing.widget.MechMapSet;
import megamek.client.ui.swing.widget.MechPanelTabStrip;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.ProtomechMapSet;
import megamek.client.ui.swing.widget.QuadMapSet;
import megamek.client.ui.swing.widget.TankMapSet;
import megamek.client.ui.swing.widget.VTOLMapSet;
import megamek.common.*;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.CardLayout;
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

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MechDisplay extends JPanel {
    // buttons & gizmos for top level

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
        //tabStrip.setTab(0);
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
     * Adds the specified mech display listener to receive
     * events from this view.
     *
     * @param listener the listener.
     */
    public void addMechDisplayListener(MechDisplayListener listener) {
        eventListeners.add(listener);
    }

    /**
     * Notifies attached listeners of the event.
     *
     * @param event the mech display event.
     */
    private void processMechDisplayEvent(MechDisplayEvent event) {
        for (int i = 0; i < eventListeners.size(); i++) {
            MechDisplayListener lis = eventListeners.get(i);
            switch (event.getType()) {
                case MechDisplayEvent.WEAPON_SELECTED:
                    lis.WeaponSelected(event);
                    break;
                default:
                    System.err.println("unknown event " + event.getType() + " in processMechDisplayEvent");
                    break;
            }
        }
    }

    /**
     * The movement panel contains all the buttons, readouts
     * and gizmos relating to moving around on the
     * battlefield.
     */
    private class MovementPanel extends PicMap {

        private GeneralInfoMapSet gi;

        private int minTopMargin = 8;
        private int minLeftMargin = 8;

        MovementPanel() {
            gi = new GeneralInfoMapSet(this);
            addElement(gi.getContentGroup());
            Vector v = gi.getBackgroundDrawers();
            Enumeration iter = v.elements();
            while (iter.hasMoreElements()) {
                addBgDrawer((BackGroundDrawer) iter.nextElement());
            }
            onResize();
        }

        public void addNotify() {
            super.addNotify();
            update();
        }

        public void onResize() {
            int w = getSize().width;
            Rectangle r = getContentBounds();
            int dx = Math.round(((w - r.width) / 2));
            if (dx < minLeftMargin) dx = minLeftMargin;
            int dy = minTopMargin;
            if (r != null) setContentMargins(dx, dy, dx, dy);
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
        private TankMapSet tank;
        private MechMapSet mech;
        private InfantryMapSet infantry;
        private BattleArmorMapSet battleArmor;
        private ProtomechMapSet proto;
        private VTOLMapSet vtol;
        private QuadMapSet quad;
        private GunEmplacementMapSet gunEmplacement;
        private ArmlessMechMapSet armless;
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
        }

        public void onResize() {
            Rectangle r = getContentBounds();
            if (r == null) return;
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
            }
            if (ams == null) {
                System.err.println("The armor panel is null."); //$NON-NLS-1$
                return;
            }
            ams.setEntity(en);
            addElement(ams.getContentGroup());
            Vector v = ams.getBackgroundDrawers();
            Enumeration iter = v.elements();
            while (iter.hasMoreElements()) {
                addBgDrawer((BackGroundDrawer) iter.nextElement());
            }
            onResize();
            update();
        }
    }

    /**
     * This class contains the all the gizmos for firing the
     * mech's weapons.
     */
    public class WeaponPanel extends BufferedPanel
            implements ItemListener, ListSelectionListener {
        private static final String IMAGE_DIR = "data/images/widgets";

        public JList weaponList;
        private JComboBox m_chAmmo;

        private JLabel wAmmo;
        private JLabel wNameL;
        private JLabel wHeatL;
        private JLabel wDamL;
        private JLabel wMinL;
        private JLabel wShortL;
        private JLabel wMedL;
        private JLabel wLongL;
        private JLabel wExtL;
        private JLabel wNameR;
        private JLabel wHeatR;
        private JLabel wDamR;
        private JLabel wMinR;
        private JLabel wShortR;
        private JLabel wMedR;
        private JLabel wLongR;
        private JLabel wExtR;
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

        WeaponPanel() {
            super(new GridBagLayout());

            // weapon list
            weaponList = new JList(new DefaultListModel());
            weaponList.addListSelectionListener(this);

            // layout main panel
            GridBagConstraints c = new GridBagConstraints();

            //adding Weapon List
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(15, 9, 1, 9);
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(weaponList, c);
            add(weaponList);

            //adding Ammo choice + label

            wAmmo = new JLabel(Messages.getString("MechDisplay.Ammo"), JLabel.LEFT); //$NON-NLS-1$
            wAmmo.setOpaque(true);
            m_chAmmo = new JComboBox();
            m_chAmmo.addItemListener(this);

            c.insets = new Insets(1, 9, 1, 1);

            c.gridwidth = 1;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.gridy = 1;
            ((GridBagLayout) getLayout()).setConstraints(wAmmo, c);
            add(wAmmo);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 1;
            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            ((GridBagLayout) getLayout()).setConstraints(m_chAmmo, c);
            add(m_chAmmo);

            //Adding Heat Buildup

            currentHeatBuildupL = new JLabel(Messages.getString("MechDisplay.HeatBuildup"), JLabel.RIGHT); //$NON-NLS-1$
            currentHeatBuildupL.setOpaque(false);
            currentHeatBuildupR = new JLabel("--", JLabel.LEFT); //$NON-NLS-1$
            currentHeatBuildupR.setOpaque(false);

            c.insets = new Insets(2, 9, 2, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 2;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            ((GridBagLayout) getLayout()).setConstraints(currentHeatBuildupL, c);
            add(currentHeatBuildupL);

            c.insets = new Insets(2, 1, 2, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 2;
            c.anchor = GridBagConstraints.WEST;
            //c.fill = GridBagConstraints.HORIZONTAL;
            ((GridBagLayout) getLayout()).setConstraints(currentHeatBuildupR, c);
            add(currentHeatBuildupR);


            //Adding weapon display labels
            wNameL = new JLabel(Messages.getString("MechDisplay.Name"), JLabel.CENTER); //$NON-NLS-1$
            wNameL.setOpaque(true);
            wHeatL = new JLabel(Messages.getString("MechDisplay.Heat"), JLabel.CENTER); //$NON-NLS-1$
            wHeatL.setOpaque(true);
            wDamL = new JLabel(Messages.getString("MechDisplay.Damage"), JLabel.CENTER); //$NON-NLS-1$
            wDamL.setOpaque(true);
            wNameR = new JLabel("", JLabel.CENTER); //$NON-NLS-1$
            wNameR.setOpaque(false);
            wHeatR = new JLabel("--", JLabel.CENTER); //$NON-NLS-1$
            wHeatR.setOpaque(false);
            wDamR = new JLabel("--", JLabel.CENTER); //$NON-NLS-1$
            wDamR.setOpaque(false);

            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 3;
            ((GridBagLayout) getLayout()).setConstraints(wNameL, c);
            add(wNameL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridwidth = 1;
            c.gridx = 2;
            ((GridBagLayout) getLayout()).setConstraints(wHeatL, c);
            add(wHeatL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridx = 3;
            ((GridBagLayout) getLayout()).setConstraints(wDamL, c);
            add(wDamL);

            c.insets = new Insets(1, 9, 2, 1);
            c.gridwidth = 2;
            c.gridx = 0;
            c.gridy = 4;
            ((GridBagLayout) getLayout()).setConstraints(wNameR, c);
            add(wNameR);

            c.gridwidth = 1;
            c.gridx = 2;
            ((GridBagLayout) getLayout()).setConstraints(wHeatR, c);
            add(wHeatR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 3;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wDamR, c);
            add(wDamR);


            // Adding range labels
            wMinL = new JLabel(Messages.getString("MechDisplay.Min"), JLabel.CENTER); //$NON-NLS-1$
            wMinL.setOpaque(true);
            wShortL = new JLabel(Messages.getString("MechDisplay.Short"), JLabel.CENTER); //$NON-NLS-1$
            wShortL.setOpaque(true);
            wMedL = new JLabel(Messages.getString("MechDisplay.Med"), JLabel.CENTER); //$NON-NLS-1$
            wMedL.setOpaque(true);
            wLongL = new JLabel(Messages.getString("MechDisplay.Long"), JLabel.CENTER); //$NON-NLS-1$
            wLongL.setOpaque(true);
            wExtL = new JLabel(Messages.getString("MechDisplay.Ext"), JLabel.CENTER); //$NON-NLS-1$
            wExtL.setOpaque(true);
            wMinR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wMinR.setOpaque(true);
            wShortR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wShortR.setOpaque(true);
            wMedR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wMedR.setOpaque(true);
            wLongR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wLongR.setOpaque(true);
            wExtR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wExtR.setOpaque(true);

            c.weightx = 1.0;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 5;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wMinL, c);
            add(wMinL);

            c.insets = new Insets(2, 1, 1, 1);
            c.gridx = 1;
            c.gridy = 5;
            ((GridBagLayout) getLayout()).setConstraints(wShortL, c);
            add(wShortL);

            c.gridx = 2;
            c.gridy = 5;
            ((GridBagLayout) getLayout()).setConstraints(wMedL, c);
            add(wMedL);
        
//         c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 3;
            c.gridy = 5;
//  c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wLongL, c);
            add(wLongL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 4;
            c.gridy = 5;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wExtL, c);
            add(wExtL);
            //----------------

            c.insets = new Insets(1, 9, 2, 1);
            c.gridx = 0;
            c.gridy = 6;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wMinR, c);
            add(wMinR);

            c.insets = new Insets(1, 1, 2, 1);
            c.gridx = 1;
            c.gridy = 6;
            ((GridBagLayout) getLayout()).setConstraints(wShortR, c);
            add(wShortR);

            c.gridx = 2;
            c.gridy = 6;
            ((GridBagLayout) getLayout()).setConstraints(wMedR, c);
            add(wMedR);

//         c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 3;
            c.gridy = 6;
//  c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wLongR, c);
            add(wLongR);

            c.insets = new Insets(1, 1, 2, 9);
            c.gridx = 4;
            c.gridy = 6;
            ((GridBagLayout) getLayout()).setConstraints(wExtR, c);
            add(wExtR);


            // target panel
            wTargetL = new JLabel(Messages.getString("MechDisplay.Target"), JLabel.CENTER); //$NON-NLS-1$
            wTargetL.setOpaque(true);
            wRangeL = new JLabel(Messages.getString("MechDisplay.Range"), JLabel.CENTER); //$NON-NLS-1$
            wRangeL.setOpaque(true);
            wToHitL = new JLabel(Messages.getString("MechDisplay.ToHit"), JLabel.CENTER); //$NON-NLS-1$
            wToHitL.setOpaque(true);

            wTargetR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wTargetR.setOpaque(true);
            wRangeR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wRangeR.setOpaque(true);
            wToHitR = new JLabel("---", JLabel.CENTER); //$NON-NLS-1$
            wToHitR.setOpaque(true);

            c.weightx = 0.0;
            c.insets = new Insets(2, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 7;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wTargetL, c);
            add(wTargetL);

            c.insets = new Insets(2, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 7;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wTargetR, c);
            add(wTargetR);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 8;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wRangeL, c);
            add(wRangeL);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 8;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wRangeR, c);
            add(wRangeR);

            c.insets = new Insets(1, 9, 1, 1);
            c.gridx = 0;
            c.gridy = 9;
            c.gridwidth = 1;
            ((GridBagLayout) getLayout()).setConstraints(wToHitL, c);
            add(wToHitL);

            c.insets = new Insets(1, 1, 1, 9);
            c.gridx = 1;
            c.gridy = 9;
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout) getLayout()).setConstraints(wToHitR, c);
            add(wToHitR);

            // to-hit text
            toHitText = new JTextArea("", 2, 20); //$NON-NLS-1$
            toHitText.setEditable(false);

            c.insets = new Insets(1, 9, 15, 9);
            c.gridx = 0;
            c.gridy = 10;
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

            b = BackGroundDrawer.TILING_HORIZONTAL |
                    BackGroundDrawer.VALIGN_TOP;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                    BackGroundDrawer.VALIGN_BOTTOM;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_TOP |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_BOTTOM |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_TOP |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_BOTTOM |
                    BackGroundDrawer.HALIGN_RIGHT;
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
                    + en.heatBuildup; // heat we're building up this round
            if (en instanceof Mech) {
                if (en.infernos.isStillBurning()) { // hit with inferno ammo
                    currentHeatBuildup += en.infernos.getHeat();
                }
                if (!((Mech) en).hasLaserHeatSinks()) {
                    // extreme temperatures.
                    if (game.getOptions().intOption("temperature") > 0) {
                        currentHeatBuildup += game.getTemperatureDifference();
                    } else {
                        currentHeatBuildup -= game.getTemperatureDifference();
                    }
                }
            }
            Coords position = entity.getPosition();
            if (!en.isOffBoard() && position != null) {
                IHex hex = game.getBoard().getHex(position);
                if (hex.terrainLevel(Terrains.FIRE) == 2) {
                    currentHeatBuildup += 5; // standing in fire
                }
                if (hex.terrainLevel(Terrains.MAGMA) == 1) {
                    currentHeatBuildup += 5;
                } else if (hex.terrainLevel(Terrains.MAGMA) == 2) {
                    currentHeatBuildup += 10;
                }
            }
            if (en instanceof Mech && en.isStealthActive()) {
                currentHeatBuildup += 10; // active stealth heat
            }

            // update weapon list
            ((DefaultListModel) weaponList.getModel()).removeAllElements();
            m_chAmmo.removeAll();
            m_chAmmo.setEnabled(false);

            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted mounted = entity.getWeaponList().get(i);
                WeaponType wtype = (WeaponType) mounted.getType();
                StringBuffer wn = new StringBuffer(mounted.getDesc());
                wn.append(" ["); //$NON-NLS-1$
                wn.append(en.getLocationAbbr(mounted.getLocation()));
                if (mounted.isSplit()) {
                    wn.append('/'); //$NON-NLS-1$
                    wn.append(en.getLocationAbbr(mounted.getSecondLocation()));
                }
                wn.append(']'); //$NON-NLS-1$
                // determine shots left & total shots left
                if (wtype.getAmmoType() != AmmoType.T_NA
                        && !wtype.hasFlag(WeaponType.F_ONESHOT)) {
                    int shotsLeft = 0;
                    if (mounted.getLinked() != null
                            && !mounted.getLinked().isDumping()) {
                        shotsLeft = mounted.getLinked().getShotsLeft();
                    }

                    EquipmentType typeUsed = null;
                    if (mounted.getLinked() != null) {
                        typeUsed = mounted.getLinked().getType();
                    }

                    int totalShotsLeft = entity.getTotalMunitionsOfType(typeUsed);

                    wn.append(" ("); //$NON-NLS-1$
                    wn.append(shotsLeft);
                    wn.append('/'); //$NON-NLS-1$
                    wn.append(totalShotsLeft);
                    wn.append(')'); //$NON-NLS-1$
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
                    wn.append(mounted.curMode().getDisplayableName()); //$NON-NLS-1$
                }
                ((DefaultListModel) weaponList.getModel()).addElement(wn.toString());
                if (mounted.isUsedThisRound() && game.getPhase() == mounted.usedInPhase()
                        && game.getPhase() == IGame.PHASE_FIRING) {
                    // add heat from weapons fire to heat tracker
                    currentHeatBuildup += wtype.getHeat() * mounted.howManyShots();
                }
            }

            // This code block copied from the MovementPanel class,
            //  bad coding practice (duplicate code).
            int heatCap = en.getHeatCapacity();
            int heatCapWater = en.getHeatCapacityWithWater();
            String heatCapacityStr = Integer.toString(heatCap);

            if (heatCap < heatCapWater) {
                heatCapacityStr = heatCap + " [" + heatCapWater + ']'; //$NON-NLS-1$ //$NON-NLS-2$
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
            currentHeatBuildupR.setText(heatText + " (" + heatCapacityStr + ')'); //$NON-NLS-1$ //$NON-NLS-2$

            // If MaxTech range rules are in play, display the extreme range.
            if (game.getOptions().booleanOption("maxtech_range")) { //$NON-NLS-1$
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
         * displays the selected item from the list in the weapon
         * display panel.
         */
        private void displaySelected() {
            // short circuit if not selected
            if (weaponList.getSelectedIndex() == -1) {
                m_chAmmo.removeAll();
                m_chAmmo.setEnabled(false);
                wNameR.setText(""); //$NON-NLS-1$
                wHeatR.setText("--"); //$NON-NLS-1$
                wDamR.setText("--"); //$NON-NLS-1$
                wMinR.setText("---"); //$NON-NLS-1$
                wShortR.setText("---"); //$NON-NLS-1$
                wMedR.setText("---"); //$NON-NLS-1$
                wLongR.setText("---"); //$NON-NLS-1$
                wExtR.setText("---"); //$NON-NLS-1$
                return;
            }
            Mounted mounted = entity.getWeaponList().get(weaponList.getSelectedIndex());
            WeaponType wtype = (WeaponType) mounted.getType();
            // update weapon display
            wNameR.setText(mounted.getDesc());
            wHeatR.setText(wtype.getHeat() + ""); //$NON-NLS-1$
            if (wtype.getDamage() == WeaponType.DAMAGE_MISSILE) {
                wDamR.setText(Messages.getString("MechDisplay.Missile")); //$NON-NLS-1$
            } else if (wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
                wDamR.setText(Messages.getString("MechDisplay.Variable")); //$NON-NLS-1$
            } else if (wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
                wDamR.setText(Messages.getString("MechDisplay.Special")); //$NON-NLS-1$
            } else if (wtype.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                StringBuffer damage = new StringBuffer();
                damage.append(Integer.toString(wtype.getRackSize()))
                        .append('/')
                        .append(Integer.toString(wtype.getRackSize() / 2));
                wDamR.setText(damage.toString());
            } else {
                wDamR.setText(Integer.toString(wtype.getDamage()));
            }

            // update range
            int shortR = wtype.getShortRange();
            int mediumR = wtype.getMediumRange();
            int longR = wtype.getLongRange();
            int extremeR = wtype.getExtremeRange();
            if (entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET
                    || longR == 0) {
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

            // update ammo selector
            boolean bOwner = clientgui.getClient().getLocalPlayer().equals(entity.getOwner());
            m_chAmmo.removeAll();
            if (wtype.getAmmoType() == AmmoType.T_NA || !bOwner) {
                m_chAmmo.setEnabled(false);
            } else if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                if (mounted.getLinked().getShotsLeft() == 1) {
                    m_chAmmo.addItem(formatAmmo(mounted.getLinked()));
                    m_chAmmo.setEnabled(true);
                } else {
                    m_chAmmo.setEnabled(false);
                }
            } else {
                if (!(entity instanceof Infantry)) {
                    m_chAmmo.setEnabled(true);
                } else {
                    m_chAmmo.setEnabled(false);
                }
                vAmmo = new ArrayList<Mounted>();
                int nCur = -1;
                int i = 0;
                for (Mounted mountedAmmo : entity.getAmmo()) {
                    AmmoType atype = (AmmoType) mountedAmmo.getType();
                    if (mountedAmmo.isAmmoUsable() &&
                            atype.getAmmoType() == wtype.getAmmoType() &&
                            atype.getRackSize() == wtype.getRackSize()) {

                        vAmmo.add(mountedAmmo);
                        m_chAmmo.addItem(formatAmmo(mountedAmmo));
                        if (mounted.getLinked().equals(mountedAmmo)) {
                            nCur = i;
                        }
                        i++;
                    }
                }
                if (nCur == -1) {
                    // outcommenting this fixes bug 1041003 (if a linked ammobin
                    // gets breached, it will continue to be used, and other ammo
                    // can't be selected for the weapon that the breached ammo was
                    // linked to), and did not cause anything to misbehave in my
                    // testing, apart from the Choice being enabled even if there
                    // is no ammo to choose from, but that is only a minor issue
                    // m_chAmmo.setEnabled(false);
                } else {
                    m_chAmmo.setSelectedIndex(nCur);
                }
            }

            //send event to other parts of the UI which care
            processMechDisplayEvent(new MechDisplayEvent(this, entity, mounted));
        }

        private String formatAmmo(Mounted m) {
            StringBuffer sb = new StringBuffer(64);
            int ammoIndex = m.getDesc().indexOf(Messages.getString("MechDisplay.0")); //$NON-NLS-1$
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE) {
                sb.append('[').append(entity.getLocationAbbr(loc)).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (ammoIndex == -1) {
                sb.append(m.getDesc());
            } else {
                sb.append(m.getDesc().substring(0, ammoIndex));
                sb.append(m.getDesc().substring(ammoIndex + 4));
            }
            return sb.toString();
        }

        /**
         * Update the range display for the selected ammo.
         *
         * @param atype - the <code>AmmoType</code> of the weapon's loaded ammo.
         */
        private void updateRangeDisplayForAmmo(Mounted mAmmo) {

            AmmoType atype = (AmmoType) mAmmo.getType();
            // Only override the display for the various ATM ammos
            if (atype.getAmmoType() == AmmoType.T_ATM) {
                if (atype.getAmmoType() == AmmoType.T_ATM
                        && atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    wMinR.setText("4"); //$NON-NLS-1$
                    wShortR.setText("1 - 9"); //$NON-NLS-1$
                    wMedR.setText("10 - 18"); //$NON-NLS-1$
                    wLongR.setText("19 - 27"); //$NON-NLS-1$
                    wExtR.setText("28 - 36"); //$NON-NLS-1$
                } else if (atype.getAmmoType() == AmmoType.T_ATM
                        && atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
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
            
            //Min range 0 for hotload
            if(mAmmo.isHotLoaded())
                wMinR.setText("---");

        } // End private void updateRangeDisplayForAmmo( AmmoType )

        //
        // ItemListener
        //
        public void itemStateChanged(ItemEvent ev) {
            if (ev.getItemSelectable().equals(m_chAmmo)) {
                int n = weaponList.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                Mounted mWeap = entity.getWeaponList().get(n);
                Mounted oldAmmo = mWeap.getLinked();
                Mounted mAmmo = vAmmo.get(m_chAmmo.getSelectedIndex());
                entity.loadWeapon(mWeap, mAmmo);
                
                // Refresh for hot load change
                if(((oldAmmo == null || !oldAmmo.isHotLoaded()) &&
                        mAmmo.isHotLoaded()) ||
                        (oldAmmo != null && 
                         oldAmmo.isHotLoaded() &&
                         !mAmmo.isHotLoaded())) {
                    displayMech(entity);
                    weaponList.setSelectedIndex(n);
                    displaySelected();
                }

                // Update the range display to account for the weapon's loaded ammo.
                
                updateRangeDisplayForAmmo(mAmmo);

                // When in the Firing Phase, update the targeting information.
                // TODO: make this an accessor function instead of a member access.
                if (clientgui.curPanel instanceof FiringDisplay) {
                    ((FiringDisplay) clientgui.curPanel).updateTarget();
                } else if (clientgui.curPanel instanceof TargetingPhaseDisplay) {
                    ((TargetingPhaseDisplay) clientgui.curPanel).updateTarget();
                }

                // Alert the server of the update.
                clientgui.getClient().sendAmmoChange(entity.getId(),
                        entity.getEquipmentNum(mWeap),
                        entity.getEquipmentNum(mAmmo));
            }
        }

        public void valueChanged(ListSelectionEvent event) {
            if (event.getSource().equals(weaponList)) {
                displaySelected();
            }
        }
    }

    /**
     * This class shows the critical hits and systems for a mech
     */
    private class SystemPanel extends BufferedPanel
            implements ItemListener, ActionListener, ListSelectionListener {
        private static final String IMAGE_DIR = "data/images/widgets";

        private JLabel locLabel;
        private JLabel slotLabel;
        private JLabel modeLabel;
        private JList slotList;
        private JList locList;

        private JComboBox m_chMode;
        private JButton m_bDumpAmmo;

        private Entity en;

        SystemPanel() {
            locLabel = new JLabel(Messages.getString("MechDisplay.Location"), JLabel.CENTER); //$NON-NLS-1$
            locLabel.setOpaque(true);
            slotLabel = new JLabel(Messages.getString("MechDisplay.Slot"), JLabel.CENTER); //$NON-NLS-1$
            slotLabel.setOpaque(true);

            locList = new JList(new DefaultListModel());
            locList.addListSelectionListener(this);

            slotList = new JList(new DefaultListModel());
            slotList.addListSelectionListener(this);

            m_chMode = new JComboBox();
            m_chMode.addItem("   "); //$NON-NLS-1$
            m_chMode.setEnabled(false);
            m_chMode.addItemListener(this);

            m_bDumpAmmo = new JButton(Messages.getString("MechDisplay.m_bDumpAmmo")); //$NON-NLS-1$
            m_bDumpAmmo.setEnabled(false);
            m_bDumpAmmo.setActionCommand("dump"); //$NON-NLS-1$
            m_bDumpAmmo.addActionListener(this);

            modeLabel = new JLabel(Messages.getString("MechDisplay.modeLabel"), JLabel.RIGHT); //$NON-NLS-1$
            modeLabel.setOpaque(true);
            //modeLabel.setEnabled(false);


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
            //c.weighty = 1.0;
            c.gridy = 1;
            c.gridx = 0;
            c.gridwidth = 1;
            c.insets = new Insets(1, 9, 15, 1);
            c.gridheight = GridBagConstraints.REMAINDER;
            gridbag.setConstraints(locList, c);
            add(locList);

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

        private CriticalSlot getSelectedCritical() {
            int loc = locList.getSelectedIndex();
            int slot = slotList.getSelectedIndex();
            if (loc == -1 || slot == -1) {
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

        /**
         * updates fields for the specified mech
         */
        public void displayMech(Entity en) {
            this.en = en;

            ((DefaultListModel) locList.getModel()).removeAllElements();
            for (int i = 0; i < en.locations(); i++) {
                if (en.getNumberOfCriticals(i) > 0) {
                    ((DefaultListModel) locList.getModel()).insertElementAt(en.getLocationName(i), i);
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
                            sb.append(cs.isDestroyed() ? "*" : "").append(cs.isBreached() ? "x" : "").append(m.getDesc()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                            if ( m.isHotLoaded() )
                                sb.append(Messages.getString("MechDisplay.isHotLoaded")); //$NON-NLS-1$
                            if (m.getType().hasModes()) {
                                sb.append(" (").append(m.curMode().getDisplayableName()).append(')'); //$NON-NLS-1$ //$NON-NLS-2$
                                if (m.getType() instanceof MiscType && ((MiscType) m.getType()).isShield()) {
                                    sb.append(" " + m.getDamageAbsorption(en, loc) + '/' + m.getCurrentDamageCapacity(en, loc) + ')');
                                }
                            }
                            break;
                        default:
                    }
                }
                ((DefaultListModel) slotList.getModel()).addElement(sb.toString());
            }
        }

        //
        // ItemListener
        //
        public void itemStateChanged(ItemEvent ev) {
            if (ev.getItemSelectable().equals(m_chMode)) {
                Mounted m = getSelectedEquipment();
                CriticalSlot cs = getSelectedCritical();
                if (m != null && m.getType().hasModes()) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {

                        if (m.getType() instanceof MiscType
                                && ((MiscType) m.getType()).isShield()
                                && clientgui.getClient().game.getPhase() != IGame.PHASE_FIRING) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.ShieldModePhase", null));//$NON-NLS-1$
                            return;
                        }

                        if (m.getType() instanceof MiscType
                                && ((MiscType) m.getType()).isVibroblade()
                                && clientgui.getClient().game.getPhase() != IGame.PHASE_PHYSICAL) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.VibrobladeModePhase", null));//$NON-NLS-1$
                            return;
                        }

                        m.setMode(nMode);
                        // send the event to the server
                        clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), nMode);

                        // notify the player
                        if (m.getType().hasInstantModeSwitch()) {
                            clientgui.systemMessage(Messages.getString("MechDisplay.switched", new Object[]{m.getName(), m.curMode().getDisplayableName()}));//$NON-NLS-1$
                        } else {
                            if (clientgui.getClient().game.getPhase() == IGame.PHASE_DEPLOYMENT) {
                                clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtStart", new Object[]{m.getName(), m.pendingMode().getDisplayableName()}));//$NON-NLS-1$
                            } else {
                                clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtEnd", new Object[]{m.getName(), m.pendingMode().getDisplayableName()}));//$NON-NLS-1$
                            }
                        }
                    }
                } else if (cs != null && cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                    int nMode = m_chMode.getSelectedIndex();
                    if (nMode >= 0) {
                        if (cs.getIndex() == Mech.SYSTEM_COCKPIT
                                && en.hasEiCockpit()
                                && en instanceof Mech) {
                            Mech mech = (Mech) en;
                            mech.setCockpitStatus(nMode);
                            clientgui.getClient().sendSystemModeChange(en.getId(), Mech.SYSTEM_COCKPIT, nMode);
                            if (mech.getCockpitStatus() == mech.getCockpitStatusNextRound()) {
                                clientgui.systemMessage(Messages.getString("MechDisplay.switched", new Object[]{"Cockpit", m_chMode.getSelectedItem()}));//$NON-NLS-1$
                            } else {
                                clientgui.systemMessage(Messages.getString("MechDisplay.willSwitchAtEnd", new Object[]{"Cockpit", m_chMode.getSelectedItem()}));//$NON-NLS-1$
                            }
                        }
                    }
                }
            }
        }

        // ActionListener
        public void actionPerformed(ActionEvent ae) {
            if ("dump".equals(ae.getActionCommand())) { //$NON-NLS-1$
                Mounted m = getSelectedEquipment();
                boolean bOwner = clientgui.getClient().getLocalPlayer().equals(en.getOwner());
                if (m == null || !bOwner || !(m.getType() instanceof AmmoType) ||
                        m.getShotsLeft() <= 0) {
                    return;
                }

                boolean bDumping;
                boolean bConfirmed;

                if (m.isPendingDump()) {
                    bDumping = false;
                    String title = Messages.getString("MechDisplay.CancelDumping.title"); //$NON-NLS-1$
                    String body = Messages.getString("MechDisplay.CancelDumping.message", new Object[]{m.getName()}); //$NON-NLS-1$
                    bConfirmed = clientgui.doYesNoDialog(title, body);
                } else {
                    bDumping = true;
                    String title = Messages.getString("MechDisplay.Dump.title"); //$NON-NLS-1$
                    String body = Messages.getString("MechDisplay.Dump.message", new Object[]{m.getName()}); //$NON-NLS-1$
                    bConfirmed = clientgui.doYesNoDialog(title, body);
                }

                if (bConfirmed) {
                    m.setPendingDump(bDumping);
                    clientgui.getClient().sendModeChange(en.getId(), en.getEquipmentNum(m), bDumping ? -1 : 0);
                }
            }
        }

        private void setBackGround() {
            Image tile = getToolkit().getImage(IMAGE_DIR + "/tile.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            int b = BackGroundDrawer.TILING_BOTH;
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                    BackGroundDrawer.VALIGN_TOP;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                    BackGroundDrawer.VALIGN_BOTTOM;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_TOP |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_BOTTOM |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_TOP |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_BOTTOM |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/br_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

        }

        public void valueChanged(ListSelectionEvent event) {
            if (event.getSource().equals(locList)) {
                m_chMode.removeAll();
                m_chMode.setEnabled(false);
                displaySlots();
            } else if (event.getSource().equals(slotList)) {
                m_bDumpAmmo.setEnabled(false);
                m_chMode.setEnabled(false);
                modeLabel.setEnabled(false);
                Mounted m = getSelectedEquipment();

                boolean bOwner = clientgui.getClient().getLocalPlayer().equals(en.getOwner());
                if (m != null && bOwner && m.getType() instanceof AmmoType
                        && !m.getType().hasInstantModeSwitch()
                        && clientgui.getClient().game.getPhase() != IGame.PHASE_DEPLOYMENT
                        && m.getShotsLeft() > 0 && !m.isDumping() && en.isActive()) {
                    m_bDumpAmmo.setEnabled(true);
                } else if (m != null && bOwner && m.getType().hasModes()) {
                    if (!m.isDestroyed() && en.isActive()) {
                        m_chMode.setEnabled(true);
                    }
                    if (!m.isDestroyed() && m.getType().hasFlag(MiscType.F_STEALTH)) {
                        m_chMode.setEnabled(true);
                    }//if the maxtech eccm option is not set then the ECM should not show anything.
                    if (m.getType().hasFlag(MiscType.F_ECM)
                            && !clientgui.getClient().game.getOptions().booleanOption("maxtech_eccm")) {
                        m_chMode.removeAll();
                        return;
                    }
                    modeLabel.setEnabled(true);
                    m_chMode.removeAll();
                    for (Enumeration e = m.getType().getModes(); e.hasMoreElements();) {
                        EquipmentMode em = (EquipmentMode) e.nextElement();
                        m_chMode.addItem(em.getDisplayableName());
                    }
                    m_chMode.setSelectedItem(m.curMode().getDisplayableName());
                } else {
                    CriticalSlot cs = getSelectedCritical();
                    if (cs != null && cs.getType() == CriticalSlot.TYPE_SYSTEM) {
                        if (cs.getIndex() == Mech.SYSTEM_COCKPIT
                                && en.hasEiCockpit()
                                && en instanceof Mech) {
                            m_chMode.removeAll();
                            m_chMode.setEnabled(true);
                            m_chMode.addItem("EI Off");
                            m_chMode.addItem("EI On");
                            m_chMode.addItem("Aimed shot");
                            m_chMode.setSelectedItem(new Integer(((Mech) en).getCockpitStatusNextRound()));
                        }
                    }
                }
            }
        }
    }

    /**
     * This class shows information about a unit that doesn't belong elsewhere.
     */
    private class ExtraPanel extends BufferedPanel
            implements ItemListener, ActionListener {

        private static final String IMAGE_DIR = "data/images/widgets";

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
        private JList narcList;
        private int myMechId;

        private Slider prompt;

        private int sinks;
        private boolean dontChange;

        ExtraPanel() {
            prompt = null;

            narcLabel = new JLabel
                    (Messages.getString("MechDisplay.AffectedBy"), JLabel.CENTER); //$NON-NLS-1$
            narcLabel.setOpaque(false);

            narcList = new JList(new DefaultListModel());

            // transport stuff
            //unusedL = new JLabel( "Unused Space:", JLabel.CENTER );

            unusedL = new JLabel
                    (Messages.getString("MechDisplay.UnusedSpace"), JLabel.CENTER); //$NON-NLS-1$
            unusedL.setOpaque(false);
            unusedR = new JTextArea("", 2, 25); //$NON-NLS-1$
            unusedR.setEditable(false);
            unusedR.setOpaque(false);

            carrysL = new JLabel
                    (Messages.getString("MechDisplay.Carryng"), JLabel.CENTER); //$NON-NLS-1$
            carrysL.setOpaque(false);
            carrysR = new JTextArea("", 4, 25); //$NON-NLS-1$
            carrysR.setEditable(false);
            carrysR.setOpaque(false);

            sinksL = new JLabel
                    (Messages.getString("MechDisplay.activeSinksLabel"), JLabel.CENTER);
            sinksL.setOpaque(false);
            sinksR = new JTextArea("", 2, 25);
            sinksR.setEditable(false);
            sinksR.setOpaque(false);

            sinks2B = new JButton(Messages.getString("MechDisplay.configureActiveSinksLabel"));
            sinks2B.setActionCommand("changeSinks");
            sinks2B.addActionListener(this);

            heatL = new JLabel
                    (Messages.getString("MechDisplay.HeatEffects"), JLabel.CENTER); //$NON-NLS-1$
            heatL.setOpaque(false);
            heatR = new JTextArea("", 4, 25); //$NON-NLS-1$
            heatR.setEditable(false);
            heatR.setOpaque(false);

            targSysL = new JLabel((Messages.getString("MechDisplay.TargSysLabel")).concat(" "), JLabel.CENTER);
            targSysL.setOpaque(false);

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
            c.weightx = 1.0;

            c.weighty = 0.0;
            gridbag.setConstraints(narcLabel, c);
            add(narcLabel);

            c.insets = new Insets(1, 9, 1, 9);
            c.weighty = 1.0;
            gridbag.setConstraints(narcList, c);
            add(new JScrollPane(narcList));

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

            b = BackGroundDrawer.TILING_HORIZONTAL |
                    BackGroundDrawer.VALIGN_TOP;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_HORIZONTAL |
                    BackGroundDrawer.VALIGN_BOTTOM;
            tile = getToolkit().getImage(IMAGE_DIR + "/h_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.TILING_VERTICAL |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/v_line.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_TOP |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_BOTTOM |
                    BackGroundDrawer.HALIGN_LEFT;
            tile = getToolkit().getImage(IMAGE_DIR + "/bl_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_TOP |
                    BackGroundDrawer.HALIGN_RIGHT;
            tile = getToolkit().getImage(IMAGE_DIR + "/tr_corner.gif"); //$NON-NLS-1$
            PMUtil.setImage(tile, this);
            addBgDrawer(new BackGroundDrawer(tile, b));

            b = BackGroundDrawer.NO_TILING |
                    BackGroundDrawer.VALIGN_BOTTOM |
                    BackGroundDrawer.HALIGN_RIGHT;
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
            if (clientgui.getClient().getLocalPlayer().getId() != en.getOwnerId()) {
                sinks2B.setEnabled(false);
                dontChange = true;
            } else {
                sinks2B.setEnabled(true);
                dontChange = false;
            }
            // Walk through the list of teams.  There
            // can't be more teams than players.
            StringBuffer buff;
            Enumeration loop = clientgui.getClient().game.getPlayers();
            while (loop.hasMoreElements()) {
                Player player = (Player) loop.nextElement();
                int team = player.getTeam();
                if (en.isNarcedBy(team) &&
                        !player.isObserver()) {
                    buff = new StringBuffer(Messages.getString("MechDisplay.NARCedBy")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(Player.teamNames[team])
                            .append(']'); //$NON-NLS-1$
                    ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
                }
                if (en.isINarcedBy(team) &&
                        !player.isObserver()) {
                    buff = new StringBuffer(Messages.getString("MechDisplay.INarcHoming")); //$NON-NLS-1$
                    buff.append(player.getName());
                    buff.append(" [")//$NON-NLS-1$
                            .append(Player.teamNames[team])
                            .append("] ")//$NON-NLS-1$
                            .append(Messages.getString("MechDisplay.attached"))//$NON-NLS-1$
                            .append('.'); //$NON-NLS-1$
                    ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
                }
            }
            if (en.isINarcedWith(INarcPod.ECM)) {
                buff = new StringBuffer(Messages.getString("MechDisplay.iNarcECMPodAttached")); //$NON-NLS-1$
                ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
            }
            if (en.isINarcedWith(INarcPod.HAYWIRE)) {
                buff = new StringBuffer(Messages.getString("MechDisplay.iNarcHaywirePodAttached")); //$NON-NLS-1$
                ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
            }
            if (en.isINarcedWith(INarcPod.NEMESIS)) {
                buff = new StringBuffer(Messages.getString("MechDisplay.iNarcNemesisPodAttached")); //$NON-NLS-1$
                ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
            }

            // Show inferno track.
            if (en.infernos.isStillBurning()) {
                buff = new StringBuffer(Messages.getString("MechDisplay.InfernoBurnRemaining")); //$NON-NLS-1$
                buff.append(en.infernos.getTurnsLeftToBurn());
                ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
            }
            if (en instanceof Tank && ((Tank) en).isOnFire()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages.getString("MechDisplay.OnFire"));
            }

            // Show electromagnic interference.
            if (en.isSufferingEMI()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages.getString("MechDisplay.IsEMId")); //$NON-NLS-1$
            }

            // Show ECM affect.
            Coords pos = en.getPosition();
            if (Compute.isAffectedByECM(en, pos, pos)) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages.getString("MechDisplay.InEnemyECMField")); //$NON-NLS-1$
            } else if (Compute.isAffectedByAngelECM(en, pos, pos)) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages.getString("MechDisplay.InEnemyAngelECMField")); //$NON-NLS-1$
            }

            // Show Turret Locked.
            if (en instanceof Tank &&
                    !((Tank) en).hasNoTurret() &&
                    !en.canChangeSecondaryFacing()) {
                ((DefaultListModel) narcList.getModel()).addElement(Messages.getString("MechDisplay.Turretlocked")); //$NON-NLS-1$
            }

            // Show jammed weapons.
            for (Mounted weapon : en.getWeaponList()) {
                if (weapon.isJammed()) {
                    buff = new StringBuffer(weapon.getName());
                    buff.append(Messages.getString("MechDisplay.isJammed")); //$NON-NLS-1$
                    ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
                }
            }

            // Show breached locations.
            for (int loc = 0; loc < en.locations(); loc++) {
                if (en.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    buff = new StringBuffer(en.getLocationName(loc));
                    buff.append(Messages.getString("MechDisplay.Breached")); //$NON-NLS-1$
                    ((DefaultListModel) narcList.getModel()).addElement(buff.toString());
                }
            }

            // transport values
            String unused = en.getUnusedString();
            if ("".equals(unused)) unused = Messages.getString("MechDisplay.None"); //$NON-NLS-1$ //$NON-NLS-2$
            unusedR.setText(unused);
            Enumeration iter = en.getLoadedUnits().elements();
            carrysR.setText(null);
            //boolean hasText = false;
            while (iter.hasMoreElements()) {
                carrysR.append(((Entity) iter.nextElement()).getShortName());
                carrysR.append("\n"); //$NON-NLS-1$
            }

            // Show club(s).
            for (Mounted club : en.getClubs()) {
                carrysR.append(club.getName());
                carrysR.append("\n"); //$NON-NLS-1$
            }

            // Show searchlight
            if (en.hasSpotlight()) {
                if (en.isUsingSpotlight())
                    carrysR.append(Messages.getString("MechDisplay.SearchlightOn")); //$NON-NLS-1$
                else
                    carrysR.append(Messages.getString("MechDisplay.SearchlightOff")); //$NON-NLS-1$
            }

            // Show Heat Effects, but only for Mechs.
            heatR.setText(""); //$NON-NLS-1$
            sinksR.setText("");

            if (en instanceof Mech) {
                Mech m = (Mech) en;

                sinks2B.setEnabled(!dontChange);
                sinks = m.getActiveSinksNextRound();
                if (m.hasDoubleHeatSinks()) {
                    sinksR.append(Messages.getString("MechDisplay.activeSinksTextDouble", new Object[]{new Integer(sinks), new Integer(sinks * 2)}));
                } else {
                    sinksR.append(Messages.getString("MechDisplay.activeSinksTextSingle", new Object[]{new Integer(sinks)}));
                }

                boolean hasTSM = false;
                boolean mtHeat = false;
                if (((Mech) en).hasTSM()) hasTSM = true;

                if (clientgui.getClient().game.getOptions().booleanOption("maxtech_heat")) {
                    mtHeat = true;
                }
                heatR.append(HeatEffects.getHeatEffects(en.heat, mtHeat, hasTSM));
            } else {
                // Non-Mechs cannot configure their heatsinks
                sinks2B.setEnabled(false);
            }

            targSysL.setText((Messages.getString("MechDisplay.TargSysLabel")).concat(" ").concat(MiscType.getTargetSysName(en.getTargSysType())));
        } // End public void displayMech( Entity )

        public void itemStateChanged(ItemEvent ev) {
        }

        public void actionPerformed(ActionEvent ae) {
            if ("changeSinks".equals(ae.getActionCommand()) && !dontChange) { //$NON-NLS-1$
                prompt = new Slider(clientgui.frame, Messages.getString("MechDisplay.changeSinks"), Messages.getString("MechDisplay.changeSinks"),
                        sinks, 0, ((Mech) clientgui.getClient().game.getEntity(myMechId)).getNumberOfSinks());
                if (!prompt.showDialog()) return;
                clientgui.menuBar.actionPerformed(ae);
                int helper = prompt.getValue();

                ((Mech) clientgui.getClient().game.getEntity(myMechId)).setActiveSinksNextRound(helper);
                clientgui.getClient().sendUpdateEntity(clientgui.getClient().game.getEntity(myMechId));
                displayMech(clientgui.getClient().game.getEntity(myMechId));
            }
        }
    }
}
