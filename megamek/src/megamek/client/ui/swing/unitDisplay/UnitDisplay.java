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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.MekPanelTabStrip;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/**
 * Displays the info for a mek. This is also a sort of interface for special
 * movement and firing actions.
 */
public class UnitDisplay extends JPanel {
    private static final MMLogger logger = MMLogger.create(UnitDisplay.class);

    // buttons & gizmos for top level
    @Serial
    private static final long serialVersionUID = -2060993542227677984L;

    private final JPanel panA1;
    private final JPanel panA2;
    private final JPanel panB1;
    private final JPanel panB2;
    private final JPanel panC1;
    private final JPanel panC2;
    private final JSplitPane splitABC;
    private final JSplitPane splitBC;
    private final JSplitPane splitA1;
    private final JSplitPane splitB1;
    private final JSplitPane splitC1;
    private final MekPanelTabStrip tabStrip;
    private final JPanel displayP;
    private final SummaryPanel mPan;
    private final PilotPanel pPan;
    private final ArmorPanel aPan;
    public WeaponPanel wPan;
    private final SystemPanel sPan;
    private final ExtraPanel ePan;
    private final ClientGUI clientgui;
    private Entity currentlyDisplaying;
    private final JLabel labTitle;
    private final List<MekDisplayListener> eventListeners = new ArrayList<>();

    JScrollPane mPanScroll;
    JScrollPane pPanScroll;
    JScrollPane aPanScroll;
    JScrollPane wPanScroll;
    JScrollPane sPanScroll;
    JScrollPane ePanScroll;

    private static final int SCROLL_STEPS = 24;
    private static final int SCROLLBAR_WIDTH = 6;

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

    private static final GUIPreferences GUI_PREFERENCES = GUIPreferences.getInstance();
    private static final UnitDisplayOrderPreferences UNIT_DISPLAY_ORDER_PREFERENCES = UnitDisplayOrderPreferences.getInstance();

    /**
     * Creates and lays out a new mek display.
     */
    public UnitDisplay() {
        this(null, null);
    }

    /**
     * Creates and lays out a new mek display.
     *
     * @param clientGui The ClientGUI for the GUI that is creating this UnitDisplay.
     */
    public UnitDisplay(@Nullable ClientGUI clientGui) {
        this(clientGui, null);
    }

    /**
     * Creates and lays out a new mek display.
     * @param clientGui The ClientGUI for the GUI that is creating this UnitDisplay.
     * @param controller The MegaMekController for the GUI that is creating this UnitDisplay.
     */
    public UnitDisplay(@Nullable ClientGUI clientGui,
            @Nullable MegaMekController controller) {
        super(new GridBagLayout());
        this.clientgui = clientGui;

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
        JScrollPane scrollPane = new JScrollPane(displayP);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Wrap in JScrollPane
        mPanScroll = createConfiguredScrollPane(mPan);
        pPanScroll = createConfiguredScrollPane(pPan);
        aPanScroll = createConfiguredScrollPane(aPan);
        wPanScroll = createConfiguredScrollPane(wPan);
        sPanScroll = createConfiguredScrollPane(sPan);
        ePanScroll = createConfiguredScrollPane(ePan);

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

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;

        ((GridBagLayout)getLayout()).setConstraints(displayP, c);
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
        JButton butSwitchView = new JButton(Messages.getString("UnitDisplay.SwitchView"));
        JButton butSwitchLocation = new JButton(Messages.getString("UnitDisplay.SwitchLocation"));

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

        splitABC.setDividerLocation(GUI_PREFERENCES.getUnitDisplaySplitABCLoc());
        splitBC.setDividerLocation(GUI_PREFERENCES.getUnitDisplaySplitBCLoc());
        splitA1.setDividerLocation(GUI_PREFERENCES.getUnitDisplaySplitA1Loc());
        splitB1.setDividerLocation(GUI_PREFERENCES.getUnitDisplaySplitB1Loc());
        splitC1.setDividerLocation(GUI_PREFERENCES.getUnitDisplaySplitC1Loc());

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

        butSwitchView.addActionListener(e -> {
            if (clientgui != null) {
                UnitDisplayDialog unitDisplayDialog = clientgui.getUnitDisplayDialog();
                if (!(GUI_PREFERENCES.getUnitDisplayStartTabbed())) {
                    saveSplitterLoc();
                    GUI_PREFERENCES.setUnitDisplayNontabbedPosX(unitDisplayDialog.getLocation().x);
                    GUI_PREFERENCES.setUnitDisplayNontabbedPosY(unitDisplayDialog.getLocation().y);
                    GUI_PREFERENCES.setUnitDisplayNonTabbedSizeWidth(unitDisplayDialog.getSize().width);
                    GUI_PREFERENCES.setUnitDisplayNonTabbedSizeHeight(unitDisplayDialog.getSize().height);
                    unitDisplayDialog.setLocation(GUI_PREFERENCES.getUnitDisplayPosX(), GUI_PREFERENCES.getUnitDisplayPosY());
                    unitDisplayDialog.setSize(GUI_PREFERENCES.getUnitDisplaySizeWidth(), GUI_PREFERENCES.getUnitDisplaySizeHeight());
                    setDisplayTabbed();
                } else {
                    GUI_PREFERENCES.setUnitDisplayPosX(unitDisplayDialog.getLocation().x);
                    GUI_PREFERENCES.setUnitDisplayPosY(unitDisplayDialog.getLocation().y);
                    GUI_PREFERENCES.setUnitDisplaySizeWidth(unitDisplayDialog.getSize().width);
                    GUI_PREFERENCES.setUnitDisplaySizeHeight(unitDisplayDialog.getSize().height);
                    unitDisplayDialog.setLocation(GUI_PREFERENCES.getUnitDisplayNontabbedPosX(),
                            GUI_PREFERENCES.getUnitDisplayNontabbedPosY());
                    unitDisplayDialog.setSize(GUI_PREFERENCES.getUnitDisplayNonTabbedSizeWidth(),
                            GUI_PREFERENCES.getUnitDisplayNonTabbedSizeHeight());
                    setDisplayNonTabbed();
                }
            }
        });

        butSwitchLocation.addActionListener(e -> GUI_PREFERENCES.toggleUnitDisplayLocation());

        if (GUI_PREFERENCES.getUnitDisplayStartTabbed()) {
            setDisplayTabbed();
        } else {
            setDisplayNonTabbed();
        }
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

        displayP.add(MekPanelTabStrip.SUMMARY, mPanScroll);
        displayP.add(MekPanelTabStrip.PILOT, pPanScroll);
        displayP.add(MekPanelTabStrip.ARMOR, aPanScroll);
        displayP.add(MekPanelTabStrip.WEAPONS, wPanScroll);
        displayP.add(MekPanelTabStrip.SYSTEMS, sPanScroll);
        displayP.add(MekPanelTabStrip.EXTRAS, ePanScroll);

        tabStrip.setTab(MekPanelTabStrip.SUMMARY_INDEX);

        displayP.revalidate();
        displayP.repaint();

        GUI_PREFERENCES.setUnitDisplayStartTabbed(true);
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

        mPanScroll.setVisible(true);
        pPanScroll.setVisible(true);
        aPanScroll.setVisible(true);
        wPanScroll.setVisible(true);
        sPanScroll.setVisible(true);
        ePanScroll.setVisible(true);

        linkParentChild(UnitDisplay.NON_TABBED_A1, UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplay.NON_TABBED_A1));
        linkParentChild(UnitDisplay.NON_TABBED_B1, UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplay.NON_TABBED_B1));
        linkParentChild(UnitDisplay.NON_TABBED_C1, UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplay.NON_TABBED_C1));
        linkParentChild(UnitDisplay.NON_TABBED_A2, UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplay.NON_TABBED_A2));
        linkParentChild(UnitDisplay.NON_TABBED_B2, UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplay.NON_TABBED_B2));
        linkParentChild(UnitDisplay.NON_TABBED_C2, UNIT_DISPLAY_ORDER_PREFERENCES.getString(UnitDisplay.NON_TABBED_C2));

        displayP.add(splitABC);

        displayP.revalidate();
        displayP.repaint();

        GUI_PREFERENCES.setUnitDisplayStartTabbed(false);
    }

    /**
     * Save splitter locations to preferences
     *
     */
    public void saveSplitterLoc() {
        GUI_PREFERENCES.setUnitDisplaySplitABCLoc(splitABC.getDividerLocation());
        GUI_PREFERENCES.setUnitDisplaySplitBCLoc(splitBC.getDividerLocation());
        GUI_PREFERENCES.setUnitDisplaySplitA1Loc(splitA1.getDividerLocation());
        GUI_PREFERENCES.setUnitDisplaySplitB1Loc(splitB1.getDividerLocation());
        GUI_PREFERENCES.setUnitDisplaySplitC2Loc(splitC1.getDividerLocation());
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
            case UnitDisplay.NON_TABBED_GENERAL -> p.add(mPanScroll, BorderLayout.CENTER);
            case UnitDisplay.NON_TABBED_PILOT -> p.add(pPanScroll, BorderLayout.CENTER);
            case UnitDisplay.NON_TABBED_WEAPON -> p.add(wPanScroll, BorderLayout.CENTER);
            case UnitDisplay.NON_TABBED_SYSTEM -> p.add(sPanScroll, BorderLayout.CENTER);
            case UnitDisplay.NON_TABBED_EXTRA -> p.add(ePanScroll, BorderLayout.CENTER);
            case UnitDisplay.NON_TABBED_ARMOR -> p.add(aPanScroll, BorderLayout.CENTER);
        }
    }

    public void setTitleVisible(boolean b) {
        labTitle.setVisible(b);
    }

    /**
     * Register the keyboard commands that the UnitDisplay should process
     *
     * @param ud UnitDisplay
     * @param controller MegaMekController
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

        displayP.revalidate();
        displayP.repaint();
    }

    private JScrollPane createConfiguredScrollPane(PicMap picMap) {
        JScrollPane scrollPane = new JScrollPane(picMap);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_STEPS);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(SCROLLBAR_WIDTH, Integer.MAX_VALUE));
        return scrollPane;
    }

    /**
     * Returns the entity we're currently displaying
     */
    public Entity getCurrentEntity() {
        return currentlyDisplaying;
    }

    /**
     * Changes to the specified panel.
     */
    public void showPanel(String s) {
        if (GUI_PREFERENCES.getUnitDisplayStartTabbed()) {
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
        displayP.revalidate();
        displayP.repaint();
    }

    /**
     * Used to force the display to the Systems tab, on a specific location
     *
     * @param loc the location to show
     */
    public void showSpecificSystem(int loc) {
        if (GUI_PREFERENCES.getUnitDisplayStartTabbed()) {
            ((CardLayout) displayP.getLayout()).show(displayP, MekPanelTabStrip.SYSTEMS);
        }

        tabStrip.setTab(MekPanelTabStrip.SYSTEMS_INDEX);
        sPan.selectLocation(loc);
        displayP.revalidate();
        displayP.repaint();
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
        for (MekDisplayListener lis : eventListeners) {
            if (event.getType() == MekDisplayEvent.WEAPON_SELECTED) {
                lis.weaponSelected(event);
            } else {
                logger.error("Received unknown event {} in processMekDisplayEvent", event.getType());
            }
        }
    }

    /**
     * Returns the UnitDisplay's ClientGUI reference, which can be null.
     *
     * @return the ClientGUI reference, or null if not applicable.
     */
    @Nullable
    public ClientGUI getClientGUI() {
        return clientgui;
    }
}
