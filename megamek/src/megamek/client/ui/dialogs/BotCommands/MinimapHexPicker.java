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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.minimap.HexPickingMap;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.game.IGame;
import megamek.logging.MMLogger;

/**
 * Lets the player pick the hexes for a bot order by clicking them on a map with no full board view, such as the
 * Commander window's minimap. It works like {@link HexTargetPicker} does on the board: clicked hexes are marked and
 * numbered on the map, clicking a marked hex again unmarks it, and a small dialog lists them with Done and Cancel. A
 * one-hex order finishes on the first click. Picking is cancelled if the game phase changes.
 *
 * <p>The picked hexes go out as dash-joined hex numbers, e.g. {@code 1508-1504}, the same text the typed prompt
 * sends.</p>
 */
public class MinimapHexPicker {

    private static final MMLogger LOGGER = MMLogger.create(MinimapHexPicker.class);

    private final HexPickingMap map;
    private final IGame game;
    private final String orderDescription;
    private final boolean singleHex;
    private final int maxHexes;
    private final Consumer<String> onTargetsSelected;

    // in the order picked: waypoints are followed in that order
    private final List<Coords> pickedHexes = new ArrayList<>();
    private final Consumer<Coords> hexClickListener = this::toggleHex;
    private final GameListenerAdapter phaseChangeListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent event) {
            if (event.getNewPhase() != GamePhase.UNKNOWN) {
                finish(false);
            }
        }
    };
    private JDialog controlDialog;
    private JLabel statusLabel;
    private boolean finished = false;

    /**
     * @param map               the map to pick hexes on
     * @param game              the game, watched so a phase change cancels the pick
     * @param orderDescription  what the order is, shown in the dialog
     * @param singleHex         {@code true} to finish after the first hex
     * @param maxHexes          the most hexes that may be picked, or 0 for no limit
     * @param onTargetsSelected called with the picked hexes as dash-joined hex numbers, only if any were picked and
     *                          the player chose Done
     */
    public MinimapHexPicker(HexPickingMap map, IGame game, String orderDescription, boolean singleHex, int maxHexes,
          Consumer<String> onTargetsSelected) {
        this.map = map;
        this.game = game;
        this.orderDescription = orderDescription;
        this.singleHex = singleHex;
        this.maxHexes = maxHexes;
        this.onTargetsSelected = onTargetsSelected;
    }

    /**
     * Starts picking: listens for clicks on the map and shows the Done / Cancel dialog.
     */
    public void start() {
        listen();
        createControlDialog();
    }

    /**
     * Listens for clicks on the map and for phase changes, without the dialog. {@link #start()} calls this.
     */
    void listen() {
        LOGGER.info("[BotOrders] Minimap hex pick started: {} (single hex: {}, limit: {})", orderDescription,
              singleHex, maxHexes);
        map.addHexClickListener(hexClickListener);
        game.addGameListener(phaseChangeListener);
    }

    /**
     * Adds a clicked hex, or removes it if it was already picked. Refuses a hex past the limit.
     *
     * @param coords the clicked hex
     */
    void toggleHex(Coords coords) {
        if (finished) {
            return;
        }
        if (pickedHexes.remove(coords)) {
            refresh(null);
            return;
        }
        if ((maxHexes > 0) && (pickedHexes.size() >= maxHexes)) {
            refresh(Messages.getString("BotCommandPanel.HexPicker.tooMany", maxHexes));
            return;
        }
        pickedHexes.add(coords);
        if (singleHex) {
            finish(true);
        } else {
            refresh(null);
        }
    }

    /**
     * Ends the pick and removes the marks and listeners. Does nothing if the pick already ended.
     *
     * @param sendSelection {@code true} to send the picked hexes, if there are any
     */
    void finish(boolean sendSelection) {
        if (finished) {
            return;
        }
        finished = true;
        map.removeHexClickListener(hexClickListener);
        game.removeGameListener(phaseChangeListener);
        map.showPickedHexes(List.of(), false);
        if (controlDialog != null) {
            controlDialog.dispose();
        }
        if (sendSelection && !pickedHexes.isEmpty()) {
            String targets = targetText();
            LOGGER.info("[BotOrders] Minimap hex pick done: {}", targets);
            onTargetsSelected.accept(targets);
        } else {
            LOGGER.info("[BotOrders] Minimap hex pick cancelled with {} hex(es) picked", pickedHexes.size());
        }
    }

    /**
     * @return the picked hexes as dash-joined hex numbers, in the order picked
     */
    String targetText() {
        StringBuilder text = new StringBuilder();
        for (Coords coords : pickedHexes) {
            if (!text.isEmpty()) {
                text.append('-');
            }
            text.append(coords.hexCode(map.getPickingBoard()));
        }
        return text.toString();
    }

    /**
     * Redraws the marks and the dialog's list of picked hexes.
     *
     * @param warning a warning to show instead of the list, or {@code null}
     */
    private void refresh(String warning) {
        map.showPickedHexes(List.copyOf(pickedHexes), !singleHex);
        if (statusLabel == null) {
            return;
        }
        statusLabel.setText((warning != null) ? warning : Messages.getString("BotCommandPanel.HexPicker.status",
              pickedHexes.size(), hexList()));
        controlDialog.pack();
    }

    private String hexList() {
        StringBuilder text = new StringBuilder();
        for (Coords coords : pickedHexes) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(coords.hexCode(map.getPickingBoard()));
        }
        return text.toString();
    }

    private void createControlDialog() {
        controlDialog = new JDialog(SwingUtilities.getWindowAncestor(map.getPickingComponent()),
              Messages.getString("BotCommandPanel.HexPicker.title"));
        controlDialog.setAlwaysOnTop(true);
        controlDialog.setLayout(new BorderLayout(UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(5)));

        JLabel instructions = new JLabel(Messages.getString("BotCommandPanel.HexPicker.minimapInstructions",
              orderDescription));
        int border = UIUtil.scaleForGUI(10);
        instructions.setBorder(BorderFactory.createEmptyBorder(border, border, 0, border));
        controlDialog.add(instructions, BorderLayout.NORTH);

        statusLabel = new JLabel(Messages.getString("BotCommandPanel.HexPicker.status", 0, ""));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, border, 0, border));
        controlDialog.add(statusLabel, BorderLayout.CENTER);

        JButton doneButton = new JButton(Messages.getString("BotCommandPanel.HexPicker.done"));
        doneButton.addActionListener(event -> finish(true));
        JButton cancelButton = new JButton(Messages.getString("BotCommandPanel.HexPicker.cancel"));
        cancelButton.addActionListener(event -> finish(false));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(doneButton);
        buttonPanel.add(cancelButton);
        controlDialog.add(buttonPanel, BorderLayout.SOUTH);

        controlDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                finish(false);
            }
        });

        controlDialog.pack();
        controlDialog.setLocationRelativeTo(map.getPickingComponent());
        controlDialog.setVisible(true);
    }
}
