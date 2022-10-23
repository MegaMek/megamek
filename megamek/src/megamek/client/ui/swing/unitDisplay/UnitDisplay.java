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

import megamek.client.event.MechDisplayEvent;
import megamek.client.event.MechDisplayListener;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MechPanelTabStrip;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

/**
 * Displays the info for a mech. This is also a sort of interface for special
 * movement and firing actions.
 */
public class UnitDisplay extends JPanel {
    // buttons & gizmos for top level
    private static final long serialVersionUID = -2060993542227677984L;
    private JButton butSwitchView;
    private JPanel mPanP;
    private JPanel pPanP;
    private JPanel aPanP;
    private JPanel wPanP;
    private JPanel sPanP;
    private JPanel ePanP;
    private JScrollPane pPanSP;
    private JSplitPane splitABC;
    private JSplitPane splitBC;
    private JSplitPane splitA1;
    private JSplitPane splitB1;
    private JSplitPane splitC1;
    private MechPanelTabStrip tabStrip;
    private JPanel displayP;
    private SummaryPanel mPan;
    private PilotPanel pPan;
    private ArmorPanel aPan;
    public WeaponPanel wPan;
    private SystemPanel sPan;
    private ExtraPanel ePan;
    private ClientGUI clientgui;
    private Entity currentlyDisplaying;
    private ArrayList<MechDisplayListener> eventListeners = new ArrayList<>();

    /**
     * Creates and lays out a new mech display.
     * 
     * @param clientgui
     *            The ClientGUI for the GUI that is creating this UnitDisplay.
     *            This could be null, if there is no ClientGUI, such as with
     *            MekWars.
     */
    public UnitDisplay(@Nullable ClientGUI clientgui) {
        this(clientgui, null);
    }
        
    public UnitDisplay(@Nullable ClientGUI clientgui,
            @Nullable MegaMekController controller) {
        super(new GridBagLayout());
        this.clientgui = clientgui;

        tabStrip = new MechPanelTabStrip(this);
        displayP = new JPanel(new CardLayout());
        mPan = new SummaryPanel(this);
        pPan = new PilotPanel(this);
        aPan = new ArmorPanel(clientgui != null ? clientgui.getClient().getGame() : null, this);
        wPan = new WeaponPanel(this);
        sPan = new SystemPanel(this);
        ePan = new ExtraPanel(this);

        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 1, 0, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        ((GridBagLayout) getLayout()).setConstraints(tabStrip, c);
        add(tabStrip);

        c.insets = new Insets(0, 1, 1, 1);
        c.weighty = 1.0;

        ((GridBagLayout) getLayout()).setConstraints(displayP, c);
        add(displayP);

        ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.SUMMARY);

        if (controller != null) {
            registerKeyboardCommands(this, controller);
        }

        mPanP = new JPanel(new BorderLayout());
        pPanP = new JPanel(new BorderLayout());
        aPanP = new JPanel(new BorderLayout());
        wPanP = new JPanel(new BorderLayout());
        sPanP = new JPanel(new BorderLayout());
        ePanP = new JPanel(new BorderLayout());
        pPanSP = new JScrollPane(pPanP, 20, 30);
        splitABC = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitBC = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitA1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitB1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitC1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        butSwitchView = new JButton("switch view");

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

        splitB1.setTopComponent(wPanP);
        splitB1.setBottomComponent(sPanP);
        splitA1.setTopComponent(mPanP);
        splitA1.setBottomComponent(pPanSP);
        splitC1.setTopComponent(ePanP);
        splitC1.setBottomComponent(aPanP);
        splitBC.setLeftComponent(splitB1);
        splitBC.setRightComponent(splitC1);
        splitABC.setLeftComponent(splitA1);
        splitABC.setRightComponent(splitBC);

        splitABC.setDividerLocation(GUIPreferences.getInstance().getDisplaSplitABCLoc());
        splitBC.setDividerLocation(GUIPreferences.getInstance().getDisplaSplitBCLoc());
        splitA1.setDividerLocation(GUIPreferences.getInstance().getDisplaSplitA1Loc());
        splitB1.setDividerLocation(GUIPreferences.getInstance().getDisplaSplitB1Loc());
        splitC1.setDividerLocation(GUIPreferences.getInstance().getDisplaSplitC1Loc());

        butSwitchView.setPreferredSize(new Dimension(500,20));

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        ((GridBagLayout) getLayout()).setConstraints(butSwitchView, c);
        add(butSwitchView);

        butSwitchView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(GUIPreferences.getInstance().getDisplayStartTabbed())) {
                    setDisplayTabbed();
                }
                else {
                    setDisplayNonTabbed();
                }

                displayP.revalidate();
            }
        });

        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
            setDisplayTabbed();
        }
        else {
            setDisplayNonTabbed();
        }
    }

    /**
     * switch display to the tabbed version
     *
     */
    private void setDisplayTabbed() {
        tabStrip.setVisible(true);

        if (displayP.getComponentCount() > 0){
            displayP.removeAll();
        }
        if (mPanP.getComponentCount() > 0){
            mPanP.removeAll();
        }
        if (pPanP.getComponentCount() > 0){
            pPanP.removeAll();
        }
        if (aPanP.getComponentCount() > 0){
            aPanP.removeAll();
        }
        if (wPanP.getComponentCount() > 0){
            wPanP.removeAll();
        }
        if (sPanP.getComponentCount() > 0){
            sPanP.removeAll();
        }
        if (ePanP.getComponentCount() > 0){
            ePanP.removeAll();
        }

        displayP.add(MechPanelTabStrip.SUMMARY, mPan);
        displayP.add(MechPanelTabStrip.PILOT, pPan);
        displayP.add(MechPanelTabStrip.ARMOR, aPan);
        displayP.add(MechPanelTabStrip.WEAPONS, wPan);
        displayP.add(MechPanelTabStrip.SYSTEMS, sPan);
        displayP.add(MechPanelTabStrip.EXTRAS, ePan);

        tabStrip.setTab(MechPanelTabStrip.SUMMARY_INDEX);

        GUIPreferences.getInstance().setDisplayStartTabbed(true);
    }

    /**
     * switch display to the non tabbed version
     *
     */
    private void setDisplayNonTabbed() {
        tabStrip.setVisible(false);

        if (displayP.getComponentCount() > 0){
            displayP.removeAll();
        }

        mPan.setVisible(true);
        pPan.setVisible(true);
        aPan.setVisible(true);
        wPan.setVisible(true);
        sPan.setVisible(true);
        ePan.setVisible(true);

        mPanP.add(mPan, BorderLayout.CENTER);
        pPanP.add(pPan, BorderLayout.PAGE_START);
        aPanP.add(aPan, BorderLayout.CENTER);
        wPanP.add(wPan, BorderLayout.CENTER);
        sPanP.add(sPan, BorderLayout.CENTER);
        ePanP.add(ePan, BorderLayout.CENTER);
        displayP.add(splitABC);

        GUIPreferences.getInstance().setDisplayStartTabbed(false);
    }

    /*
    * Save splitter locations to preferences
    *
    */
    public void saveSpliterLoc() {
        GUIPreferences.getInstance().setDisplaySplitABCLoc(splitABC.getDividerLocation());
        GUIPreferences.getInstance().setDisplaySplitBCLoc(splitBC.getDividerLocation());
        GUIPreferences.getInstance().setDisplaySplitA1Loc(splitA1.getDividerLocation());
        GUIPreferences.getInstance().setDisplaySplitB1Loc(splitB1.getDividerLocation());
        GUIPreferences.getInstance().setDisplaySplitC2Loc(splitC1.getDividerLocation());
    }

    /**
     * Register the keyboard commands that the UnitDisplay should process
     *
     * @param ud
     * @param controller
     */
    private void registerKeyboardCommands(final UnitDisplay ud,
            final MegaMekController controller) {
        // Display General Tab
        controller.registerCommandAction(KeyCommandBind.UD_GENERAL.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (ud.isVisible()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
                            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.SUMMARY);
                        }

                        tabStrip.setTab(MechPanelTabStrip.SUMMARY_INDEX);
                    }

                });

        // Display Pilot Tab
        controller.registerCommandAction(KeyCommandBind.UD_PILOT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (ud.isVisible()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
                            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.PILOT);
                        }

                        tabStrip.setTab(MechPanelTabStrip.PILOT_INDEX);
                    }

                });

        // Display Armor Tab
        controller.registerCommandAction(KeyCommandBind.UD_ARMOR.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (ud.isVisible()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
                            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.ARMOR);
                        }

                        tabStrip.setTab(MechPanelTabStrip.ARMOR_INDEX);
                    }

                });

        // Display Systems Tab
        controller.registerCommandAction(KeyCommandBind.UD_SYSTEMS.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (ud.isVisible()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
                            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.SYSTEMS);
                        }

                        tabStrip.setTab(MechPanelTabStrip.SYSTEMS_INDEX);
                    }

                });

        // Display Weapons Tab
        controller.registerCommandAction(KeyCommandBind.UD_WEAPONS.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (ud.isVisible()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
                            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.WEAPONS);
                        }

                        tabStrip.setTab(MechPanelTabStrip.WEAPONS_INDEX);
                    }

                });

        // Display Extras Tab
        controller.registerCommandAction(KeyCommandBind.UD_EXTRAS.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (ud.isVisible()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
                            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.EXTRAS);
                        }

                        tabStrip.setTab(MechPanelTabStrip.EXTRAS_INDEX);
                    }

                });
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
        String enName = en.getShortName();
        switch (en.getDamageLevel()) {
            case Entity.DMG_CRIPPLED:
                enName += " [CRIPPLED]";
                break;
            case Entity.DMG_HEAVY:
                enName += " [HEAVY DMG]";
                break;
            case Entity.DMG_MODERATE:
                enName += " [MODERATE DMG]";
                break;
            case Entity.DMG_LIGHT:
                enName += " [LIGHT DMG]";
                break;
            default:
                enName += " [UNDAMAGED]";
        }

        if (clientgui != null) {
            clientgui.getUnitDisplayDialog().setTitle(enName);
        }

        currentlyDisplaying = en;

        mPan.displayMech(en);
        pPan.displayMech(en);
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
        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
            ((CardLayout) displayP.getLayout()).show(displayP, s);
        }

        if (MechPanelTabStrip.SUMMARY.equals(s)) {
            tabStrip.setTab(MechPanelTabStrip.SUMMARY_INDEX);
        } else if (MechPanelTabStrip.PILOT.equals(s)) {
            tabStrip.setTab(MechPanelTabStrip.PILOT_INDEX);
        } else if (MechPanelTabStrip.ARMOR.equals(s)) {
            tabStrip.setTab(MechPanelTabStrip.ARMOR_INDEX);
        } else if (MechPanelTabStrip.WEAPONS.equals(s)) {
            tabStrip.setTab(MechPanelTabStrip.WEAPONS_INDEX);
        } else if (MechPanelTabStrip.SYSTEMS.equals(s)) {
            tabStrip.setTab(MechPanelTabStrip.SYSTEMS_INDEX);
        } else if (MechPanelTabStrip.EXTRAS.equals(s)) {
            tabStrip.setTab(MechPanelTabStrip.EXTRAS_INDEX);
        }
    }

    /**
     * Used to force the display to the Systems tab, on a specific location
     * @param loc
     */
    public void showSpecificSystem(int loc) {
        if (GUIPreferences.getInstance().getDisplayStartTabbed()) {
            ((CardLayout) displayP.getLayout()).show(displayP, MechPanelTabStrip.SYSTEMS);
        }

        tabStrip.setTab(MechPanelTabStrip.SYSTEMS_INDEX);
        sPan.selectLocation(loc);
    }

    /**
     * Adds the specified mech display listener to receive events from this
     * view.
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
    void processMechDisplayEvent(MechDisplayEvent event) {
        for (int i = 0; i < eventListeners.size(); i++) {
            MechDisplayListener lis = eventListeners.get(i);
            switch (event.getType()) {
                case MechDisplayEvent.WEAPON_SELECTED:
                    lis.weaponSelected(event);
                    break;
                default:
                    LogManager.getLogger().error("Received unknown event " + event.getType() + " in processMechDisplayEvent");
                    break;
            }
        }
    }

    /**
     * Returns the UnitDisplay's ClientGUI reference, which can be null.
     * @return
     */
    @Nullable
    public ClientGUI getClientGUI() {
        return clientgui;
    }
}