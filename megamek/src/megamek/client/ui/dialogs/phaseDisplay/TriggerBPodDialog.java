/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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
import java.io.Serial;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.actions.TriggerBPodAction;
import megamek.common.board.Coords;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;

/**
 * A dialog displayed to the player when they have an opportunity to trigger an Anti-BA Pod on one of their units.
 */
public class TriggerBPodDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -5882060083607984056L;

    /**
     * The <code>FirePodTracker</code>s for the entity's active Anti-BA Pods.
     */
    private final ArrayList<TriggerPodTracker> trackers = new ArrayList<>();

    /**
     * The <code>int</code> ID of the entity that can fire Anti-BA Pods.
     */
    private final int entityId;

    private final ClientGUI clientGUI;

    /**
     * A helper class to track when a Anti-BA Pod has been selected to be triggered.
     *
     * @param podNum   The equipment number of the Anti-BA Pod that this is listening to.
     * @param checkbox The <code>JCheckBox</code> being tracked.
     */
    private record TriggerPodTracker(JCheckBox checkbox, int podNum) {

        /**
         * Create a tracker.
         */
        private TriggerPodTracker {
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
     * Display a dialog that shows the Anti-BA Pods on the entity, and allows the player to fire any active pods.
     *
     * @param clientGUI the <code>ClientGUI</code> parent of this dialog
     * @param entity    the <code>Entity</code> that can fire AP Pods.
     */
    public TriggerBPodDialog(ClientGUI clientGUI, Entity entity, String attackType) {
        super(clientGUI.getFrame(), Messages.getString("TriggerBPodDialog.title"), true);
        entityId = entity.getId();
        this.clientGUI = clientGUI;

        JTextArea labMessage = new JTextArea(Messages.getString("TriggerBPodDialog.selectPodsToTrigger",
              entity.getDisplayName()));
        labMessage.setEditable(false);
        labMessage.setOpaque(false);

        // AP Pod checkbox panel.
        JPanel panPods = new JPanel();
        panPods.setLayout(new GridLayout(0, 1));

        // Walk through the entity's weapons equipment, looking for Anti-BA Pods.
        for (Mounted<?> mount : entity.getWeaponList()) {

            // Is this an Anti-BA Pod?
            if (mount.getType().hasFlag(WeaponType.F_B_POD)) {
                // Create a checkbox for the pod, and add it to the panel.
                String message = entity.getLocationName(mount.getLocation())
                      + ' ' + mount.getName();
                JCheckBox pod = new JCheckBox(message);
                panPods.add(pod);

                // Can the entity fire the pod?
                if (mount.canFire()) {
                    // Only Leg's and CT BPods can be used against Leg attacks
                    if (attackType.equals(Infantry.LEG_ATTACK)
                          && (mount.getLocation() != Mek.LOC_CENTER_TORSO)
                          && (mount.getLocation() != Mek.LOC_LEFT_LEG)
                          && (mount.getLocation() != Mek.LOC_RIGHT_LEG)) {
                        if (entity instanceof QuadMek) {
                            if ((mount.getLocation() != Mek.LOC_LEFT_ARM)
                                  || (mount.getLocation() != Mek.LOC_RIGHT_ARM)) {
                                pod.setEnabled(false);
                            }
                        } else {
                            pod.setEnabled(false);
                        }
                    } // Only Forward Mounted Arm and Side Torso B-Pod's can be
                    // used against
                    // Swarm attacks
                    else if (attackType.equals(Infantry.SWARM_MEK)
                          && (mount.isRearMounted()
                          || (mount.getLocation() == Mek.LOC_CENTER_TORSO)
                          || (mount.getLocation() == Mek.LOC_LEFT_LEG) || (mount
                          .getLocation() == Mek.LOC_RIGHT_LEG))) {
                        pod.setEnabled(false);
                    } else {
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
        JButton butOkay = new JButton(Messages.getString("Okay"));
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
        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }
        setResizable(false);
        setLocation(clientGUI.getFrame().getLocation().x
                    + clientGUI.getFrame().getSize().width / 2 - size.width / 2,
              clientGUI.getFrame().getLocation().y
                    + clientGUI.getFrame().getSize().height / 2 - size.height
                    / 2);
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
    public Enumeration<TriggerBPodAction> getActions() {
        Vector<TriggerBPodAction> temp = new Vector<>();

        // Walk through the list of AP Pod trackers.
        for (TriggerPodTracker pod : trackers) {

            // Should we create an action for this pod?
            if (pod.isTriggered()) {
                Entity targetEntity = clientGUI.getClient().getGame().getEntity(entityId);

                if (targetEntity != null) {
                    temp.addElement(new TriggerBPodAction(entityId,
                          pod.getNum(),
                          chooseTarget(targetEntity.getPosition()).getId()));
                }
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
        final Game game = clientGUI.getClient().getGame();
        // Assume that we have *no* choice.
        Entity choice = null;

        // Get the available choices.

        // Convert the choices into a List of targets.
        List<Infantry> targets = new ArrayList<>();
        for (Entity ent : game.getEntitiesVector(pos)) {
            Entity targetEntity = game.getEntity(entityId);
            if (targetEntity != null && !targetEntity.equals(ent) && (ent instanceof Infantry infantry)) {
                targets.add(infantry);
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);

        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String input = (String) JOptionPane.showInputDialog(clientGUI.getFrame(),
                  Messages.getString("TriggerBPodDialog.ChooseTargetDialog.message", pos.getBoardNum()),
                  Messages.getString("TriggerBPodDialog.ChooseTargetDialog.title"),
                  JOptionPane.QUESTION_MESSAGE, null, SharedUtility.getDisplayArray(targets), null);
            choice = (Infantry) SharedUtility.getTargetPicked(targets, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Entity chooseTarget( Coords )

}
