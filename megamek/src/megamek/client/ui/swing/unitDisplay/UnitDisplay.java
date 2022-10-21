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
        displayP.add(MechPanelTabStrip.SUMMARY, mPan);
        pPan = new PilotPanel(this);
        displayP.add(MechPanelTabStrip.PILOT, pPan);
        aPan = new ArmorPanel(clientgui != null ? clientgui.getClient().getGame() : null, this);
        displayP.add(MechPanelTabStrip.ARMOR, aPan);
        wPan = new WeaponPanel(this);
        displayP.add(MechPanelTabStrip.WEAPONS, wPan);
        sPan = new SystemPanel(this);
        displayP.add(MechPanelTabStrip.SYSTEMS, sPan);
        ePan = new ExtraPanel(this);
        displayP.add(MechPanelTabStrip.EXTRAS, ePan);

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

        butSwitchView = new JButton("switch view");
        butSwitchView.setPreferredSize(new Dimension(500,20));

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        ((GridBagLayout) getLayout()).setConstraints(butSwitchView, c);
        add(butSwitchView);

        butSwitchView.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (displayP.getLayout().getClass().getTypeName() == "java.awt.GridLayout") {
                       tabStrip.setVisible(true);
                       tabStrip.setTab(MechPanelTabStrip.SUMMARY_INDEX);
                       displayP.setLayout(new CardLayout());

                       // tab name is lost when layout changed to GridLayout, remove and add to correct
                       displayP.remove(0);
                       displayP.remove(0);
                       displayP.remove(0);
                       displayP.remove(0);
                       displayP.remove(0);
                       displayP.remove(0);

                       displayP.add(MechPanelTabStrip.SUMMARY, mPan);
                       displayP.add(MechPanelTabStrip.PILOT, pPan);
                       displayP.add(MechPanelTabStrip.ARMOR, aPan);
                       displayP.add(MechPanelTabStrip.WEAPONS, wPan);
                       displayP.add(MechPanelTabStrip.SYSTEMS, sPan);
                       displayP.add(MechPanelTabStrip.EXTRAS, ePan);
                }
                else {
                    tabStrip.setVisible(false);
                    displayP.setLayout(new GridLayout(2, 3, 5, 5));

                    mPan.setVisible(true);
                    pPan.setVisible(true);
                    aPan.setVisible(true);
                    wPan.setVisible(true);
                    sPan.setVisible(true);
                    ePan.setVisible(true);
                }

                displayP.revalidate();
            }
        });
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
                        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
                        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
                        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
                        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
                        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
                        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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
        if (displayP.getLayout().getClass().getTypeName() == "java.awt.CardLayout") {
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