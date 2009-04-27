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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.AdvancedLabel;
import megamek.common.Entity;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TriggerAPPodAction;

/**
 * A dialog displayed to the player when they have an opportunity to trigger an
 * Anti-Personell Pod on one of their units.
 */
public class TriggerAPPodDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 161017051720020010L;
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private AdvancedLabel labMessage;

    /** The <code>FirePodTracker</code>s for the entity's active AP Pods. */
    private Vector<TriggerPodTracker> trackers = new Vector<TriggerPodTracker>();

    /** The <code>int</code> ID of the entity that can fire AP Pods. */
    private int entityId = Entity.NONE;

    /**
     * A helper class to track when a AP Pod has been selected to be triggered.
     */
    private class TriggerPodTracker {

        /** The equipment number of the AP Pod that this is listening to. */
        private int podNum = Entity.NONE;

        /** The <code>Checkbox</code> being tracked. */
        private Checkbox checkbox = null;

        /** Create a tracker. */
        public TriggerPodTracker(Checkbox box, int pod) {
            podNum = pod;
            checkbox = box;
        }

        /**
         * See if this AP Pod should be triggered
         * 
         * @return <code>true</code> if the pod should be triggered.
         */
        public boolean isTriggered() {
            return checkbox.getState();
        }

        /**
         * Get the equipment number of this AP Pod.
         * 
         * @return the <code>int</code> of the pod.
         */
        public int getNum() {
            return podNum;
        }
    }

    /**
     * Display a dialog that shows the AP Pods on the entity, and allows the
     * player to fire any active pods.
     * 
     * @param parent the <code>Frame</code> parent of this dialog
     * @param entity the <code>Entity</code> that can fire AP Pods.
     */
    public TriggerAPPodDialog(Frame parent, Entity entity) {
        super(parent, Messages.getString("TriggerAPPodDialog.title"), true); //$NON-NLS-1$
        this.entityId = entity.getId();

        labMessage = new AdvancedLabel(
                Messages
                        .getString(
                                "TriggerAPPodDialog.selectPodsToTrigger", new Object[] { entity.getDisplayName() })); //$NON-NLS-1$ 

        // AP Pod checkbox panel.
        Panel panPods = new Panel();
        panPods.setLayout(new GridLayout(0, 1));

        // Walk through the entity's misc equipment, looking for AP Pods.
        for (Mounted mount : entity.getMisc()) {

            // Is this an AP Pod?
            if (mount.getType().hasFlag(MiscType.F_AP_POD)) {

                // Create a checkbox for the pod, and add it to the panel.
                StringBuffer message = new StringBuffer();
                message.append(entity.getLocationName(mount.getLocation()))
                        .append(" ") //$NON-NLS-1$
                        .append(mount.getName());
                Checkbox pod = new Checkbox(message.toString());
                panPods.add(pod);

                // Can the entity fire the pod?
                if (mount.canFire()) {
                    // Yup. Add a traker for this pod.
                    TriggerPodTracker tracker = new TriggerPodTracker(pod,
                            entity.getEquipmentNum(mount));
                    trackers.addElement(tracker);
                } else {
                    // Nope. Disable the checkbox.
                    pod.setEnabled(false);
                }

            } // End found-AP-Pod

        } // Look at the next piece of equipment.

        // OK button.
        butOkay.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);

        gridbag.setConstraints(panPods, c);
        add(panPods);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        gridbag.setConstraints(butOkay, c);
        add(butOkay);

        addWindowListener(new WindowAdapter() {
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
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - size.width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - size.height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }

    /**
     * Get the trigger actions that the user selected.
     * 
     * @return the <code>Enumeration</code> of <code>TriggerAPPodAction</code>
     *         objects that match the user's selections.
     */
    public Enumeration<EntityAction> getActions() {
        Vector<EntityAction> temp = new Vector<EntityAction>();

        // Walk through the list of AP Pod trackers.
        Enumeration<TriggerPodTracker> pods = trackers.elements();
        while (pods.hasMoreElements()) {
            TriggerPodTracker pod = pods.nextElement();

            // Should we create an action for this pod?
            if (pod.isTriggered()) {
                temp.addElement(new TriggerAPPodAction(entityId, pod.getNum()));
            }
        }

        return temp.elements();
    }

}
