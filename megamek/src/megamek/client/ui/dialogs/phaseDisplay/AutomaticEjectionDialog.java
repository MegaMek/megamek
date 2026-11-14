/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.EjectionHazard;
import megamek.common.units.AutomaticEjectionRules;
import megamek.common.units.CrewArmorKitRules;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Warns that the conditions will kill any crew that ejects, and lets the player switch automatic ejection off unit by
 * unit before deploying.
 * <p>
 * The setting is otherwise reachable only from the lobby's unit configuration, which cannot be opened once play has
 * begun. Without this the warning would be telling a player about a problem they had no way left to fix.
 */
public class AutomaticEjectionDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger LOGGER = MMLogger.create(AutomaticEjectionDialog.class);
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);
    private static final int MAXIMUM_LIST_HEIGHT = UIUtil.scaleForGUI(335);
    private static final int SCROLL_INCREMENT = UIUtil.scaleForGUI(16);

    private final ClientGUI clientGUI;
    private final Game game;
    private final List<Entity> unitsWithEjectionSystems;
    private final String lethalCondition;
    private final Set<Integer> unitsWhoseCrewDies = new HashSet<>();
    private final Map<Integer, JCheckBox> ejectionBoxes = new HashMap<>();
    private final Map<Integer, Boolean> settingsOnOpening = new HashMap<>();
    private final JCheckBox dontAskAgain =
          new JCheckBox(Messages.getString("AutomaticEjectionDialog.dontAskAgain"));

    private JButton turnOffUnprotectedButton;
    private JButton deployButton;
    private JButton cancelButton;
    private boolean deploymentCancelled = true;

    /**
     * @param parent                   the frame to centre the dialog on
     * @param clientGUI                the client that any changes are sent through
     * @param unitsWithEjectionSystems the player's units that have an ejection system, whether it is on or off
     * @param lethalCondition          what it is out there that would kill an ejecting crew, phrased to open a
     *                                 sentence
     */
    public AutomaticEjectionDialog(JFrame parent, ClientGUI clientGUI, List<Entity> unitsWithEjectionSystems,
          String lethalCondition) {
        super(parent, Messages.getString("AutomaticEjectionDialog.title"), true);
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.unitsWithEjectionSystems = unprotectedFirst(unitsWithEjectionSystems);
        this.lethalCondition = lethalCondition;

        initializeUI();
        bindKeys();
        pack();
        // The packed size fits every line of text, so the player can make the window bigger but never cut it off
        setMinimumSize(getSize());
        setLocationRelativeTo(parent);
        LOGGER.debug("[EnvironmentalSealing] Automatic ejection dialog opened for {} unit(s)",
              unitsWithEjectionSystems.size());
    }

    /**
     * Puts the units whose crews would die on ejection at the top, so the ones that need a decision are seen first.
     * Each group keeps the order it was given in. Also records which units those are, for the rest of the dialog.
     *
     * @param units the player's units with an ejection system
     *
     * @return the same units, unprotected crews first
     */
    private List<Entity> unprotectedFirst(List<Entity> units) {
        List<Entity> unprotected = new ArrayList<>();
        List<Entity> protectedCrews = new ArrayList<>();
        for (Entity entity : units) {
            if (CrewArmorKitRules.survivesEjection(entity, game)) {
                protectedCrews.add(entity);
            } else {
                unprotected.add(entity);
                unitsWhoseCrewDies.add(entity.getId());
            }
        }
        unprotected.addAll(protectedCrews);
        return unprotected;
    }

    /**
     * Enter deploys and Escape cancels, as the two buttons would.
     */
    private void bindKeys() {
        getRootPane().setDefaultButton(deployButton);
        getRootPane().registerKeyboardAction(event -> cancelButton.doClick(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel instructions = new JLabel(
              Messages.getString("AutomaticEjectionDialog.instructions", lethalCondition));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        mainPanel.add(buildUnitList(), BorderLayout.CENTER);
        mainPanel.add(buildButtonPanel(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Builds the unit rows: one tick box each, showing whether that unit is currently set to eject. A crew that would
     * die out there has its crew entry in the warning colour, and its tick box says so when hovered.
     *
     * @return the scrollable list of units
     */
    private JScrollPane buildUnitList() {
        JPanel unitPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AutomaticEjectionDialog.unitHeader")
              + "</b></html>"), constraints);
        constraints.gridx = 1;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AutomaticEjectionDialog.crewHeader")
              + "</b></html>"), constraints);
        constraints.gridx = 2;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AutomaticEjectionDialog.ejectionHeader")
              + "</b></html>"), constraints);

        int row = 1;
        for (Entity entity : unitsWithEjectionSystems) {
            constraints.gridy = row++;

            constraints.gridx = 0;
            unitPanel.add(new JLabel(entity.getShortName()), constraints);

            boolean crewDies = unitsWhoseCrewDies.contains(entity.getId());
            constraints.gridx = 1;
            JLabel crewLabel = new JLabel(crewStatus(entity));
            if (crewDies) {
                crewLabel.setForeground(GUIP.getWarningColor());
            }
            unitPanel.add(crewLabel, constraints);

            // The column header names the setting, so the box itself carries no text
            constraints.gridx = 2;
            constraints.anchor = GridBagConstraints.CENTER;
            boolean willEject = AutomaticEjectionRules.willEjectAutomatically(entity, game);
            JCheckBox ejectionBox = new JCheckBox();
            ejectionBox.setSelected(willEject);
            ejectionBox.getAccessibleContext().setAccessibleName(Messages.getString(
                  "AutomaticEjectionDialog.ejectAutomatically.accessibleName", entity.getShortName()));
            ejectionBox.setToolTipText(Messages.getString(crewDies
                  ? "AutomaticEjectionDialog.ejectAutomatically.tooltip"
                  : "AutomaticEjectionDialog.ejectAutomatically.tooltip.protected"));
            unitPanel.add(ejectionBox, constraints);
            constraints.anchor = GridBagConstraints.WEST;
            ejectionBoxes.put(entity.getId(), ejectionBox);
            settingsOnOpening.put(entity.getId(), willEject);
        }

        JScrollPane scrollPane = new JScrollPane(unitPanel,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_INCREMENT);
        scrollPane.setPreferredSize(listSize(scrollPane, unitPanel));
        return scrollPane;
    }

    /**
     * Sizes the list from what is actually in it, so a long crew entry is never cut off. The height stops at a cap and
     * the list scrolls past it; room for the scroll bar is always kept, so a scrolling list does not hide the last
     * column.
     *
     * @param scrollPane the scroll pane the list sits in
     * @param unitPanel  the rows of units
     *
     * @return the size the list should open at
     */
    private static Dimension listSize(JScrollPane scrollPane, JPanel unitPanel) {
        Dimension contentSize = unitPanel.getPreferredSize();
        Insets borderInsets = scrollPane.getInsets();
        int scrollBarWidth = scrollPane.getVerticalScrollBar().getPreferredSize().width;
        int width = contentSize.width + scrollBarWidth + borderInsets.left + borderInsets.right;
        int height = Math.min(contentSize.height + borderInsets.top + borderInsets.bottom, MAXIMUM_LIST_HEIGHT);
        return new Dimension(width, height);
    }

    /**
     * What this unit's crew is wearing, and whether it is enough for what is out there.
     *
     * <p>Names the kit rather than just saying "protected", so a player can see which of their crews still need
     * one. When a crew has a kit that does not answer everything, the gap is named: a combat suit supplies air but
     * holds no pressure, so it is worth saying that vacuum is still going to kill them.</p>
     *
     * @param entity the unit whose crew is being described
     *
     * @return the text for the crew column
     */
    private String crewStatus(Entity entity) {
        EquipmentType armorKit = CrewArmorKitRules.crewArmorKit(entity, game);
        if (armorKit == null) {
            return Messages.getString("AutomaticEjectionDialog.crewHasNoKit");
        }
        Set<EjectionHazard> unanswered = CrewArmorKitRules.unansweredBy(entity, game);
        if (unanswered.isEmpty()) {
            return armorKit.getName();
        }
        List<String> hazardNames = new ArrayList<>();
        for (EjectionHazard hazard : unanswered) {
            hazardNames.add(hazard.getDisplayName());
        }
        return Messages.getString("AutomaticEjectionDialog.crewKitGap",
              armorKit.getName(), String.join(", ", hazardNames));
    }

    /**
     * Builds the button that unticks every unprotected crew at once, the tick box that turns the warning off for good,
     * and the two buttons under them.
     *
     * @return the panel that sits along the bottom of the dialog
     */
    private JPanel buildButtonPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel optionsPanel = new JPanel(new BorderLayout());
        JPanel bulkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
        turnOffUnprotectedButton = new JButton(Messages.getString("AutomaticEjectionDialog.turnOffUnprotected"));
        turnOffUnprotectedButton.setToolTipText(
              Messages.getString("AutomaticEjectionDialog.turnOffUnprotected.tooltip"));
        turnOffUnprotectedButton.setEnabled(!unitsWhoseCrewDies.isEmpty());
        turnOffUnprotectedButton.addActionListener(this);
        bulkPanel.add(turnOffUnprotectedButton);
        optionsPanel.add(bulkPanel, BorderLayout.NORTH);

        JPanel nagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, PADDING_SMALL));
        nagPanel.add(dontAskAgain);
        optionsPanel.add(nagPanel, BorderLayout.SOUTH);
        southPanel.add(optionsPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        deployButton = new JButton(Messages.getString("AutomaticEjectionDialog.deploy"));
        deployButton.addActionListener(this);
        cancelButton = new JButton(Messages.getString("AutomaticEjectionDialog.cancel"));
        cancelButton.addActionListener(this);
        buttonPanel.add(deployButton);
        buttonPanel.add(cancelButton);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        return southPanel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == turnOffUnprotectedButton) {
            turnOffUnprotectedCrews();
            return;
        }
        if (event.getSource() == deployButton) {
            applyEjectionChanges();
            deploymentCancelled = false;
        }
        if (dontAskAgain.isSelected()) {
            GUIP.setNagForAutoEject(false);
        }
        dispose();
    }

    /**
     * Unticks every unit whose crew would die on ejection. Nothing is sent yet: the player can still change a box, and
     * Cancel still undoes it all.
     */
    private void turnOffUnprotectedCrews() {
        for (Integer entityId : unitsWhoseCrewDies) {
            JCheckBox ejectionBox = ejectionBoxes.get(entityId);
            if (ejectionBox != null) {
                ejectionBox.setSelected(false);
            }
        }
        LOGGER.debug("[EnvironmentalSealing] Unticked automatic ejection for {} unprotected crew(s)",
              unitsWhoseCrewDies.size());
    }

    /**
     * Sends the server every setting the player changed, in either direction. Only what changed is sent, because the
     * server's copy of the unit is the one that decides whether a crew is thrown clear.
     */
    private void applyEjectionChanges() {
        for (Entity entity : unitsWithEjectionSystems) {
            JCheckBox ejectionBox = ejectionBoxes.get(entity.getId());
            if (ejectionBox == null) {
                continue;
            }
            boolean shouldEject = ejectionBox.isSelected();
            if (shouldEject != settingsOnOpening.get(entity.getId())) {
                LOGGER.debug("[EnvironmentalSealing] {}: automatic ejection set to {} at the player's request",
                      entity.getShortName(), shouldEject);
                clientGUI.getClient().sendEjectionSettingChange(entity.getId(), shouldEject);
            }
        }
    }

    /**
     * @return {@code true} if the player backed out rather than deploying
     */
    public boolean isDeploymentCancelled() {
        return deploymentCancelled;
    }
}
