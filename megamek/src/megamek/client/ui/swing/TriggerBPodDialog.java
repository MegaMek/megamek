/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.WeaponType;
import megamek.common.actions.TriggerBPodAction;

/**
 * A dialog displayed to the player when they have an opportunity to trigger an
 * Anti-BA Pod on one of their units.
 */
public class TriggerBPodDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -5882060083607984056L;
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JTextArea labMessage;

    /**
     * The <code>FirePodTracker</code>s for the entity's active Anti-BA Pods.
     */
    private ArrayList<TriggerPodTracker> trackers = new ArrayList<TriggerPodTracker>();

    /**
     * The <code>int</code> ID of the entity that can fire Anti-BA Pods.
     */
    private int entityId = Entity.NONE;

    private ClientGUI clientgui;

    /**
     * A helper class to track when a Anti-BA Pod has been selected to be triggered.
     */
    private class TriggerPodTracker {

        /**
         * The equipment number of the Anti-BA Pod that this is listening to.
         */
        private int podNum = Entity.NONE;

        /**
         * The <code>JCheckBox</code> being tracked.
         */
        private JCheckBox checkbox;

        /**
         * Create a tracker.
         */
        public TriggerPodTracker(JCheckBox box, int pod) {
            podNum = pod;
            checkbox = box;
        }

        /**
         * See if this Anti-BA Pod should be triggered
         *
         * @return <code>true</code> if the pod should be triggered.
         */
        public boolean isTriggered() {
            return checkbox.isSelected();
        }

        /**
         * Get the equipment number of this Anti-BA Pod.
         *
         * @return the <code>int</code> of the pod.
         */
        public int getNum() {
            return podNum;
        }
    }

    /**
     * Display a dialog that shows the Anti-BA Pods on the entity, and allows the
     * player to fire any active pods.
     *
     * @param parent the <code>Frame</code> parent of this dialog
     * @param entity the <code>Entity</code> that can fire AP Pods.
     */
    public TriggerBPodDialog(ClientGUI clientgui, Entity entity, String attackType) {
        super(clientgui.frame, Messages.getString("TriggerBPodDialog.title"), true); //$NON-NLS-1$
        entityId = entity.getId();
        this.clientgui = clientgui;


        labMessage = new JTextArea(
                Messages.getString(
                        "TriggerBPodDialog.selectPodsToTrigger",
                        new Object[] { entity.getDisplayName() })); //$NON-NLS-1$
        labMessage.setEditable(false);
        labMessage.setOpaque(false);

        // AP Pod checkbox panel.
        JPanel panPods = new JPanel();
        panPods.setLayout(new GridLayout(0, 1));

        // Walk through the entity's weapons equipment, looking for Anti-BA Pods.
        for (Mounted mount : entity.getWeaponList()) {


            // Is this an Anti-BA Pod?
            if (mount.getType().hasFlag(WeaponType.F_B_POD)) {

                // Create a checkbox for the pod, and add it to the panel.
                StringBuffer message = new StringBuffer();
                message.append(entity.getLocationName(mount.getLocation()))
                        .append(' ')//$NON-NLS-1$
                        .append(mount.getName());
                JCheckBox pod = new JCheckBox(message.toString());
                panPods.add(pod);

                // Can the entity fire the pod?
                if (mount.canFire()) {
                    //Only Leg's and CT BPods can be used against Leg attacks
                    if ( attackType.equals(Infantry.LEG_ATTACK)
                            && (mount.getLocation() != Mech.LOC_CT)
                            && (mount.getLocation() != Mech.LOC_LLEG)
                            && (mount.getLocation() != Mech.LOC_RLEG) ){
                        if (entity instanceof QuadMech) {
                            if ((mount.getLocation() != Mech.LOC_LARM)
                                    || (mount.getLocation() != Mech.LOC_RARM)) {
                                        pod.setEnabled(false);
                        }
                        } else {
                            pod.setEnabled(false);
                        }
                    } //Only Forward Mounted Arm and Side Torso B-Pod's can be used against
                      //Swarm attacks
                    else if ( attackType.equals(Infantry.SWARM_MEK)
                            && (mount.isRearMounted()
                            || (mount.getLocation() == Mech.LOC_CT)
                            || (mount.getLocation() == Mech.LOC_LLEG)
                            || (mount.getLocation() == Mech.LOC_RLEG))  ){
                        pod.setEnabled(false);
                    }else {
                        // Yup. Add a traker for this pod.
                        TriggerPodTracker tracker = new TriggerPodTracker(pod,
                                entity.getEquipmentNum(mount));
                        trackers.add(tracker);

                    }
                } else {
                    // Nope. Disable the checkbox.
                    pod.setEnabled(false);
                }

            } // End found-Anti-BA-Pod

        } // Look at the next piece of equipment.

        // OK button.
        butOkay.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMessage, c);
        getContentPane().add(labMessage);

        gridbag.setConstraints(panPods, c);
        getContentPane().add(panPods);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        gridbag.setConstraints(butOkay, c);
        getContentPane().add(butOkay);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        Dimension size = getSize();
        boolean updateSize = false;
        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }
        if (updateSize) {
            setSize(size);
            size = getSize();
        }
        setResizable(false);
        setLocation(clientgui.frame.getLocation().x + clientgui.frame.getSize().width / 2
                - size.width / 2, clientgui.frame.getLocation().y
                + clientgui.frame.getSize().height / 2 - size.height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false);
    }

    /**
     * Get the trigger actions that the user selected.
     *
     * @return the <code>Enumeration</code> of <code>TriggerAPPodAction</code>
     *         objects that match the user's selections.
     */
    public Enumeration<TriggerBPodAction> getActions() {
        Vector<TriggerBPodAction> temp = new Vector<TriggerBPodAction>();

        // Walk through the list of AP Pod trackers.
        for (TriggerPodTracker pod : trackers) {

            // Should we create an action for this pod?
            if (pod.isTriggered()) {

                temp.addElement(new TriggerBPodAction(entityId, pod.getNum(), chooseTarget(clientgui.client.game.getEntity(entityId).getPosition()).getId()));
            }
        }

        return temp.elements();
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Entity chooseTarget(Coords pos) {

        // Assume that we have *no* choice.
        Entity choice = null;

        // Get the available choices.
        Enumeration<Entity> choices = clientgui.client.game.getEntities(pos);

        // Convert the choices into a List of targets.
        Vector<Entity> targets = new Vector<Entity>();
        while (choices.hasMoreElements()) {
            choice = choices.nextElement();
            if (!clientgui.client.game.getEntity(entityId).equals(choice) && (choice instanceof Infantry)) {
                targets.addElement(choice);
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {

            // Return that choice.
            choice = targets.elementAt(0);

        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String[] names = new String[targets.size()];
            for (int loop = 0; loop < names.length; loop++) {
                names[loop] = targets.elementAt(loop).getDisplayName();
            }
            SingleChoiceDialog choiceDialog = new SingleChoiceDialog(
                    clientgui.frame,
                    Messages.getString("TriggerBPodDialog.ChooseTargetDialog.title"), //$NON-NLS-1$
                    Messages.getString(
                                    "TriggerBPodDialog.ChooseTargetDialog.message", new Object[] { pos.getBoardNum() }), //$NON-NLS-1$
                    names);
            choiceDialog.setVisible(true);
            if (choiceDialog.getAnswer() == true) {
                choice = targets.elementAt(choiceDialog.getChoice());
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Entity chooseTarget( Coords )

}
