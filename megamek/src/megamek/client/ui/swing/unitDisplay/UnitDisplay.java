/*
 * MegaMek - Copyright (C) 2000-2004, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.unitDisplay;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import megamek.client.event.MekDisplayEvent;
import megamek.client.event.MekDisplayListener;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.UnitDisplayDialog;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.UnitDisplayOrderPreferences;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.MekPanelTabStrip;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/**
 * Displays the info for a mek. This is also a sort of interface for special
 * movement and firing actions.
 */
public class UnitDisplay extends JPanel implements IPreferenceChangeListener {
    private static final MMLogger logger = MMLogger.create(UnitDisplay.class);

    // buttons & gizmos for top level
    private static final long serialVersionUID = -2060993542227677984L;
    private JButton butSwitchView;
    private JButton butSwitchLocation;
    private JPanel panA1;
    private JPanel panA2;
    private JPanel panB1;
    private JPanel panB2;
    private JPanel panC1;
    private JPanel panC2;
    private JSplitPane splitABC;
    private JSplitPane splitBC;
    private JSplitPane splitA1;
    private JSplitPane splitB1;
    private JSplitPane splitC1;
    private MekPanelTabStrip tabStrip;
    private JPanel displayP;
    private SummaryPanel mPan;
    private PilotPanel pPan;
    private ArmorPanel aPan;
    public WeaponPanel wPan;
    private SystemPanel sPan;
    private ExtraPanel ePan;
    private ClientGUI clientgui;
    private Entity currentlyDisplaying;
    private JLabel labTitle;
    private ArrayList<MekDisplayListener> eventListeners = new ArrayList<>();

    public static final String NON_TABBED_GENERAL = "General";
    public static final String NON_TABBED_PILOT = "Pilot";
    public static final String NON_TABBED_ARMOR = "Armor";
    public static final String NON_TABBED_WEAPON = "Weapon";
    public static final String NON_TABBED_SYSTEM = "System";
    public static final String NON_TABBED_EXTRA = "Extra";

    public static final String NON_TABBED_A1 = "NonTabbedA1";
    public static final String NON_TABBED_A2 = "NonTabbedA2";
    public static final String NON_TABBED_B1 = "NonTabbedB1";
    public static final String NON_TABBED_B2 = "NonTabbedB2";
    public static final String NON_TABBED_C1 = "NonTabbedC1";
    public static final String NON_TABBED_C2 = "NonTabbedC2";

    public static final int NON_TABBED_ZERO_INDEX = 0;
    public static final int NON_TABBED_ONE_INDEX = 1;
    public static final int NON_TABBED_TWO_INDEX = 2;
    public static final int NON_TABBED_THREE_INDEX = 3;
    public static final int NON_TABBED_FOUR_INDEX = 4;
    public static final int NON_TABBED_FIVE_INDEX = 5;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final UnitDisplayOrderPreferences UDOP = UnitDisplayOrderPreferences.getInstance();

    /**
     * Creates and lays out a new mek display.
     *
     * @param clientgui
     *                  The ClientGUI for the GUI that is creating this UnitDisplay.
     *                  This could be null, if there is no ClientGUI, such as with
     *                  MekWars.
     */
    public UnitDisplay(@Nullable ClientGUI clientgui) {
        this(clientgui, null);
    }

    public UnitDisplay(@Nullable ClientGUI clientgui,
            @Nullable MegaMekController controller) {
        super(new GridBagLayout());
        this.clientgui = clientgui;

        labTitle = new JLabel("Title");

        tabStrip = new MekPanelTabStrip(this);
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();
        Image tile = getToolkit()
                .getImage(new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        BackGroundDrawer bgd = new BackGroundDrawer(tile, b);
        tabStrip.addBgDrawer(bgd);

        displayP = new JPanel(new CardLayout());
        mPan = new SummaryPanel(this);
        pPan = new PilotPanel(this);
        aPan = new ArmorPanel(clientgui != null ? clientgui.getClient().getGame() : null, this);
        wPan = new WeaponPanel(this, clientgui != null ? clientgui.getClient() : null);
        sPan = new SystemPanel(this);
        ePan = new ExtraPanel(this);

        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 1, 0, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        ((GridBagLayout) getLayout()).setConstraints(labTitle, c);
        add(labTitle);

        ((GridBagLayout) getLayout()).setConstraints(tabStrip, c);
        add(tabStrip);

        c.insets = new Insets(0, 1, 1, 1);
        c.weighty = 1.0;

        ((GridBagLayout) getLayout()).setConstraints(displayP, c);
        add(displayP);

        if (controller != null) {
            registerKeyboardCommands(this, controller);
        }

        panA1 = new JPanel(new BorderLayout());
        panA2 = new JPanel(new BorderLayout());
        panB1 = new JPanel(new BorderLayout());
        panB2 = new JPanel(new BorderLayout());
        panC1 = new JPanel(new BorderLayout());
        panC2 = new JPanel(new BorderLayout());
        splitABC = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitBC = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitA1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitB1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitC1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        butSwitchView = new JButton(Messages.getString("UnitDisplay.SwitchView"));
        butSwitchLocation = new JButton(Messages.getString("UnitDisplay.SwitchLocation"));

        splitABC.setOneTouchExpandable(true);
        splitBC.setOneTouchExpandable(true);
        splitA1.setOneTouchExpandable(true);
        splitB1.setOneTouchExpandable(true);
        splitC1.setOneTouchExpandable(true);
        splitABC.setDividerSize(10);
        splitBC.setDividerSize(10);
        splitA1.setDividerSize(10);
        splitB1.setDividerSize(10);
        splitC1.setDividerSize(10);
        splitABC.setResizeWeight(0.3);
        splitBC.setResizeWeight(0.7);
        splitA1.setResizeWeight(0.9);
        splitB1.setResizeWeight(0.6);
        splitC1.setResizeWeight(0.6);

        splitB1.setTopComponent(panB1);
        splitB1.setBottomComponent(panB2);
        splitA1.setTopComponent(panA1);
        splitA1.setBottomComponent(panA2);
        splitC1.setTopComponent(panC1);
        splitC1.setBottomComponent(panC2);
        splitBC.setLeftComponent(splitB1);
        splitBC.setRightComponent(splitC1);
        splitABC.setLeftComponent(splitA1);
        splitABC.setRightComponent(splitBC);

        splitABC.setDividerLocation(GUIP.getUnitDisplaySplitABCLoc());
        splitBC.setDividerLocation(GUIP.getUnitDisplaySplitBCLoc());
        splitA1.setDividerLocation(GUIP.getUnitDisplaySplitA1Loc());
        splitB1.setDividerLocation(GUIP.getUnitDisplaySplitB1Loc());
        splitC1.setDividerLocation(GUIP.getUnitDisplaySplitC1Loc());

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;

        ((GridBagLayout) getLayout()).setConstraints(butSwitchView, c);
        add(butSwitchView);

        c.weightx = 1.0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(butSwitchLocation, c);
        add(butSwitchLocation);

        butSwitchView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clientgui != null) {
                    UnitDisplayDialog unitDisplayDialog = clientgui.getUnitDisplayDialog();
                    if (!(GUIP.getUnitDisplayStartTabbed())) {
                        saveSplitterLoc();
                        GUIP.setUnitDisplayNontabbedPosX(unitDisplayDialog.getLocation().x);
                        GUIP.setUnitDisplayNontabbedPosY(unitDisplayDialog.getLocation().y);
                        GUIP.setUnitDisplayNonTabbedSizeWidth(unitDisplayDialog.getSize().width);
                        GUIP.setUnitDisplayNonTabbedSizeHeight(unitDisplayDialog.getSize().height);
                        unitDisplayDialog.setLocation(GUIP.getUnitDisplayPosX(), GUIP.getUnitDisplayPosY());
                        unitDisplayDialog.setSize(GUIP.getUnitDisplaySizeWidth(), GUIP.getUnitDisplaySizeHeight());
                        setDisplayTabbed();
                    } else {
                        GUIP.setUnitDisplayPosX(unitDisplayDialog.getLocation().x);
                        GUIP.setUnitDisplayPosY(unitDisplayDialog.getLocation().y);
                        GUIP.setUnitDisplaySizeWidth(unitDisplayDialog.getSize().width);
                        GUIP.setUnitDisplaySizeHeight(unitDisplayDialog.getSize().height);
                        unitDisplayDialog.setLocation(GUIP.getUnitDisplayNontabbedPosX(),
                                GUIP.getUnitDisplayNontabbedPosY());
                        unitDisplayDialog.setSize(GUIP.getUnitDisplayNonTabbedSizeWidth(),
                                GUIP.getUnitDisplayNonTabbedSizeHeight());
                        setDisplayNonTabbed();
                    }
                }
            }
        });

        butSwitchLocation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUIP.toggleUnitDisplayLocation();
            }
        });

        if (GUIP.getUnitDisplayStartTabbed()) {
            setDisplayTabbed();
        } else {
            setDisplayNonTabbed();
        }

        adaptToGUIScale();
        GUIP.addPreferenceChangeListener(this);
    }

    /**
     * switch display to the tabbed version
     *
     */
    private void setDisplayTabbed() {
        tabStrip.setVisible(true);

        displayP.removeAll();
        panA1.removeAll();
        panA2.removeAll();
        panB1.removeAll();
        panB2.removeAll();
        panC1.removeAll();
        panC2.removeAll();

        displayP.add(MekPanelTabStrip.SUMMARY, mPan);
        displayP.add(MekPanelTabStrip.PILOT, pPan);
        displayP.add(MekPanelTabStrip.ARMOR, aPan);
        displayP.add(MekPanelTabStrip.WEAPONS, wPan);
        displayP.add(MekPanelTabStrip.SYSTEMS, sPan);
        displayP.add(MekPanelTabStrip.EXTRAS, ePan);

        tabStrip.setTab(MekPanelTabStrip.SUMMARY_INDEX);

        displayP.revalidate();
        displayP.repaint();

        GUIP.setUnitDisplayStartTabbed(true);
    }

    /**
     * switch display to the non tabbed version
     *
     */
    public void setDisplayNonTabbed() {
        tabStrip.setVisible(false);

        displayP.removeAll();
        panA1.removeAll();
        panA2.removeAll();
        panB1.removeAll();
        panB2.removeAll();
        panC1.removeAll();
        panC2.removeAll();

        mPan.setVisible(true);
        pPan.setVisible(true);
        aPan.setVisible(true);
        wPan.setVisible(true);
        sPan.setVisible(true);
        ePan.setVisible(true);

        linkParentChild(UnitDisplay.NON_TABBED_A1, UDOP.getString(UnitDisplay.NON_TABBED_A1));
        linkParentChild(UnitDisplay.NON_TABBED_B1, UDOP.getString(UnitDisplay.NON_TABBED_B1));
        linkParentChild(UnitDisplay.NON_TABBED_C1, UDOP.getString(UnitDisplay.NON_TABBED_C1));
        linkParentChild(UnitDisplay.NON_TABBED_A2, UDOP.getString(UnitDisplay.NON_TABBED_A2));
        linkParentChild(UnitDisplay.NON_TABBED_B2, UDOP.getString(UnitDisplay.NON_TABBED_B2));
        linkParentChild(UnitDisplay.NON_TABBED_C2, UDOP.getString(UnitDisplay.NON_TABBED_C2));

        displayP.add(splitABC);

        displayP.revalidate();
        displayP.repaint();

        GUIP.setUnitDisplayStartTabbed(false);
    }

    /**
     * Save splitter locations to preferences
     *
     */
    public void saveSplitterLoc() {
        GUIP.setUnitDisplaySplitABCLoc(splitABC.getDividerLocation());
        GUIP.setUnitDisplaySplitBCLoc(splitBC.getDividerLocation());
        GUIP.setUnitDisplaySplitA1Loc(splitA1.getDividerLocation());
        GUIP.setUnitDisplaySplitB1Loc(splitB1.getDividerLocation());
        GUIP.setUnitDisplaySplitC2Loc(splitC1.getDividerLocation());
    }

    /**
     * connect parent to child panel
     *
     */
    private void linkParentChild(String t, String v) {
        switch (t) {
            case UnitDisplay.NON_TABBED_A1:
                addChildPanel(panA1, v);
                break;
            case UnitDisplay.NON_TABBED_A2:
                addChildPanel(panA2, v);
                break;
            case UnitDisplay.NON_TABBED_B1:
                addChildPanel(panB1, v);
                break;
            case UnitDisplay.NON_TABBED_B2:
                addChildPanel(panB2, v);
                break;
            case UnitDisplay.NON_TABBED_C1:
                addChildPanel(panC1, v);
                break;
            case UnitDisplay.NON_TABBED_C2:
                addChildPanel(panC2, v);
                break;
        }
    }

    /**
     * add child panel to parent
     *
     */
    private void addChildPanel(JPanel p, String v) {
        switch (v) {
            case UnitDisplay.NON_TABBED_GENERAL:
                p.add(mPan, BorderLayout.CENTER);
                break;
            case UnitDisplay.NON_TABBED_PILOT:
                p.add(pPan, BorderLayout.CENTER);
                break;
            case UnitDisplay.NON_TABBED_WEAPON:
                p.add(wPan, BorderLayout.CENTER);
                break;
            case UnitDisplay.NON_TABBED_SYSTEM:
                p.add(sPan, BorderLayout.CENTER);
                break;
            case UnitDisplay.NON_TABBED_EXTRA:
                p.add(ePan, BorderLayout.CENTER);
                break;
            case UnitDisplay.NON_TABBED_ARMOR:
                p.add(aPan, BorderLayout.CENTER);
                break;
        }
    }

    public void setTitleVisible(boolean b) {
        labTitle.setVisible(b);
    }

    /**
     * Register the keyboard commands that the UnitDisplay should process
     *
     * @param ud
     * @param controller
     */
    private void registerKeyboardCommands(final UnitDisplay ud,
            final MegaMekController controller) {
        controller.registerCommandAction(KeyCommandBind.UD_GENERAL, ud::isVisible,
                () -> showPanel(MekPanelTabStrip.SUMMARY));
        controller.registerCommandAction(KeyCommandBind.UD_PILOT, ud::isVisible,
                () -> showPanel(MekPanelTabStrip.PILOT));
        controller.registerCommandAction(KeyCommandBind.UD_ARMOR, ud::isVisible,
                () -> showPanel(MekPanelTabStrip.ARMOR));
        controller.registerCommandAction(KeyCommandBind.UD_SYSTEMS, ud::isVisible,
                () -> showPanel(MekPanelTabStrip.SYSTEMS));
        controller.registerCommandAction(KeyCommandBind.UD_WEAPONS, ud::isVisible,
                () -> showPanel(MekPanelTabStrip.WEAPONS));
        controller.registerCommandAction(KeyCommandBind.UD_EXTRAS, ud::isVisible,
                () -> showPanel(MekPanelTabStrip.EXTRAS));
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (!e.isConsumed()) {
            return super.processKeyBinding(ks, e, condition, pressed);
        } else {
            return true;
        }
    }

    /**
     * Displays the specified entity in the panel.
     */
    public void displayEntity(Entity en) {
        if ((en == null) || (currentlyDisplaying == en)) {
            // Issue #5650 - this method should not be executed if the currently displayed
            // entity hasn't changed.
            return;
        }
        currentlyDisplaying = en;
        updateDisplay();
        if (clientgui != null) {
            clientgui.clearFieldOfFire();
            clientgui.hideFleeZone();
        }
    }

    protected void updateDisplay() {
        if (clientgui != null) {
            String enName = currentlyDisplaying.getShortName();
            enName += " [" + UnitToolTip.getDamageLevelDesc(currentlyDisplaying, false) + "]";
            clientgui.getUnitDisplayDialog().setTitle(enName);
            labTitle.setText(enName);
        }

        mPan.displayMek(currentlyDisplaying);
        pPan.displayMek(currentlyDisplaying);
        aPan.displayMek(currentlyDisplaying);
        wPan.displayMek(currentlyDisplaying);
        sPan.displayMek(currentlyDisplaying);
        ePan.displayMek(currentlyDisplaying);
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
        if (GUIP.getUnitDisplayStartTabbed()) {
            ((CardLayout) displayP.getLayout()).show(displayP, s);
        }

        if (MekPanelTabStrip.SUMMARY.equals(s)) {
            tabStrip.setTab(MekPanelTabStrip.SUMMARY_INDEX);
        } else if (MekPanelTabStrip.PILOT.equals(s)) {
            tabStrip.setTab(MekPanelTabStrip.PILOT_INDEX);
        } else if (MekPanelTabStrip.ARMOR.equals(s)) {
            tabStrip.setTab(MekPanelTabStrip.ARMOR_INDEX);
        } else if (MekPanelTabStrip.WEAPONS.equals(s)) {
            tabStrip.setTab(MekPanelTabStrip.WEAPONS_INDEX);
        } else if (MekPanelTabStrip.SYSTEMS.equals(s)) {
            tabStrip.setTab(MekPanelTabStrip.SYSTEMS_INDEX);
        } else if (MekPanelTabStrip.EXTRAS.equals(s)) {
            tabStrip.setTab(MekPanelTabStrip.EXTRAS_INDEX);
        }
    }

    /**
     * Used to force the display to the Systems tab, on a specific location
     *
     * @param loc
     */
    public void showSpecificSystem(int loc) {
        if (GUIP.getUnitDisplayStartTabbed()) {
            ((CardLayout) displayP.getLayout()).show(displayP, MekPanelTabStrip.SYSTEMS);
        }

        tabStrip.setTab(MekPanelTabStrip.SYSTEMS_INDEX);
        sPan.selectLocation(loc);
    }

    /**
     * Adds the specified mek display listener to receive events from this
     * view.
     *
     * @param listener the listener.
     */
    public void addMekDisplayListener(MekDisplayListener listener) {
        eventListeners.add(listener);
    }

    /**
     * Notifies attached listeners of the event.
     *
     * @param event the mek display event.
     */
    void processMekDisplayEvent(MekDisplayEvent event) {
        for (int i = 0; i < eventListeners.size(); i++) {
            MekDisplayListener lis = eventListeners.get(i);
            switch (event.getType()) {
                case MekDisplayEvent.WEAPON_SELECTED:
                    lis.weaponSelected(event);
                    break;
                default:
                    logger
                            .error("Received unknown event " + event.getType() + " in processMekDisplayEvent");
                    break;
            }
        }
    }

    /**
     * Returns the UnitDisplay's ClientGUI reference, which can be null.
     *
     * @return
     */
    @Nullable
    public ClientGUI getClientGUI() {
        return clientgui;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustContainer(this, UIUtil.FONT_SCALE1);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }
}
