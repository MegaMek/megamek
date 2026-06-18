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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * End-Phase dialog for declaring Bridge-Layer (AVLB) deployments (TO:AuE p.241). Lists each of the local player's units
 * that may deploy this End Phase, with a checkbox showing the hex the folding bridge would be laid in (directly in
 * front of the unit, along its facing). Checked units have their declaration sent to the server; the bridge is placed
 * at the end of the following turn provided the unit stays stationary.
 *
 * @author Claude Code (Opus 4.8)
 */
public class BridgeDeployDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger LOGGER = MMLogger.create(BridgeDeployDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);

    private final ClientGUI clientGUI;
    private final Game game;

    private JButton applyButton;
    private JButton cancelButton;

    /** Eligible unit id -> its "deploy this unit" checkbox. */
    private final Map<Integer, JCheckBox> deployCheckBoxes = new LinkedHashMap<>();

    public BridgeDeployDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("BridgeDeployDialog.title"), true);
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();

        initializeUI(eligibleUnits());

        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * @return the local player's units that may declare a deployment this End Phase.
     */
    private List<Entity> eligibleUnits() {
        Player localPlayer = clientGUI.getClient().getLocalPlayer();
        int localPlayerId = (localPlayer != null) ? localPlayer.getId() : Player.PLAYER_NONE;
        List<Entity> units = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if ((entity.getOwnerId() == localPlayerId) && entity.canDeclareBridgeDeploy(game)) {
                units.add(entity);
            }
        }
        return units;
    }

    private void initializeUI(List<Entity> units) {
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        JLabel instructions = new JLabel(Messages.getString("BridgeDeployDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));
        add(instructions, BorderLayout.NORTH);

        JPanel unitPanel = new JPanel(new GridLayout(0, 1, 0, UIUtil.scaleForGUI(4)));
        unitPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        for (Entity entity : units) {
            Coords target = entity.getBridgeLayerTargetCoords();
            String targetHex = (target != null) ? target.getBoardNum() : "?";
            JCheckBox checkBox = new JCheckBox(
                  Messages.getString("BridgeDeployDialog.unit", entity.getShortName(), targetHex), true);
            deployCheckBoxes.put(entity.getId(), checkBox);
            unitPanel.add(checkBox);
        }
        add(new JScrollPane(unitPanel), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        applyButton = new JButton(Messages.getString("BridgeDeployDialog.btnApply"));
        applyButton.addActionListener(this);
        cancelButton = new JButton(Messages.getString("BridgeDeployDialog.btnCancel"));
        cancelButton.addActionListener(this);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == applyButton) {
            applyDeclarations();
        }
        dispose();
    }

    /**
     * Sends a deployment declaration to the server for every checked unit.
     */
    private void applyDeclarations() {
        Client client = clientGUI.getClient();
        for (Map.Entry<Integer, JCheckBox> entry : deployCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                LOGGER.debug("[AVLB] declaring bridge deployment for unit #{}", entry.getKey());
                client.sendDeployBridge(entry.getKey());
            }
        }
    }
}
