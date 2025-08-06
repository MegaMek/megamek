/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.dialogs.phaseDisplay;

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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.Entity;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.actions.TriggerAPPodAction;

/**
 * A dialog displayed to the player when they have an opportunity to trigger an Anti-Personell Pod on one of their
 * units.
 */
public class TriggerAPPodDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -9009039614015364943L;
    private JButton butOkay = new JButton(Messages.getString("Okay"));
    private JTextArea labMessage;

    /**
     * The <code>FirePodTracker</code>s for the entity's active AP Pods.
     */
    private ArrayList<TriggerPodTracker> trackers = new ArrayList<>();

    /**
     * The <code>int</code> ID of the entity that can fire AP Pods.
     */
    private int entityId = Entity.NONE;

    /**
     * A helper class to track when a AP Pod has been selected to be triggered.
     */
    private class TriggerPodTracker {

        /**
         * The equipment number of the AP Pod that this is listening to.
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
         * See if this AP Pod should be triggered
         *
         * @return <code>true</code> if the pod should be triggered.
         */
        public boolean isTriggered() {
            return checkbox.isSelected();
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
     * Display a dialog that shows the AP Pods on the entity, and allows the player to fire any active pods.
     *
     * @param parent the <code>Frame</code> parent of this dialog
     * @param entity the <code>Entity</code> that can fire AP Pods.
     */
    public TriggerAPPodDialog(JFrame parent, Entity entity) {
        super(parent, Messages.getString("TriggerAPPodDialog.title"), true);
        entityId = entity.getId();

        labMessage = new JTextArea(Messages.getString("TriggerAPPodDialog.selectPodsToTrigger",
              entity.getDisplayName()));
        labMessage.setEditable(false);
        labMessage.setOpaque(false);

        // AP Pod checkbox panel.
        JPanel panPods = new JPanel();
        panPods.setLayout(new GridLayout(0, 1));

        // Walk through the entity's misc equipment, looking for AP Pods.
        for (Mounted<?> mount : entity.getMisc()) {
            // Is this an AP Pod?
            if (mount.getType().hasFlag(MiscType.F_AP_POD)) {

                // Create a checkbox for the pod, and add it to the panel.
                StringBuffer message = new StringBuffer();
                message.append(entity.getLocationName(mount.getLocation()))
                      .append(' ')
                      .append(mount.getName());
                JCheckBox pod = new JCheckBox(message.toString());
                panPods.add(pod);

                // Can the entity fire the pod?
                if (mount.canFire()) {
                    // Yup. Add a traker for this pod.
                    TriggerPodTracker tracker = new TriggerPodTracker(pod,
                          entity.getEquipmentNum(mount));
                    trackers.add(tracker);
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
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                    - size.width / 2,
              parent.getLocation().y
                    + parent.getSize().height / 2 - size.height / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setVisible(false);
    }

    /**
     * Get the trigger actions that the user selected.
     *
     * @return the <code>Enumeration</code> of <code>TriggerAPPodAction</code> objects that match the user's selections.
     */
    public Enumeration<TriggerAPPodAction> getActions() {
        Vector<TriggerAPPodAction> temp = new Vector<>();

        // Walk through the list of AP Pod trackers.
        for (TriggerPodTracker pod : trackers) {

            // Should we create an action for this pod?
            if (pod.isTriggered()) {
                temp.addElement(new TriggerAPPodAction(entityId, pod.getNum()));
            }
        }

        return temp.elements();
    }

}
