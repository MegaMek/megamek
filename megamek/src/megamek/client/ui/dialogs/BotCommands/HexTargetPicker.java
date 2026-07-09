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
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.common.RangeType;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.util.Distractable;

/**
 * Lets the player designate target hexes for a bot order by clicking them on the board, instead of typing hex numbers.
 * Clicked hexes are highlighted on the board and listed by hex number in a small floating control dialog with
 * Done/Cancel; clicking a selected hex again deselects it. While picking, the current phase display is told to ignore
 * events so the same clicks do not also drive unit movement or firing. Picking ends automatically if the game phase
 * changes.
 *
 * @author HammerGS
 */
public class HexTargetPicker {

    // bitmask for drawing all six hex edges of the highlight sprite
    private static final int ALL_HEX_BORDERS = 63;

    private final ClientGUI clientGUI;
    private final BoardView boardView;
    private final String orderDescription;
    private final boolean singleHex;
    private final int maxHexes;
    private final Consumer<String> onTargetsSelected;

    // insertion order matters: waypoints are followed in the order they were picked
    private final Map<Coords, FieldOfFireSprite> pickedHexes = new LinkedHashMap<>();
    private JDialog controlDialog;
    private JLabel statusLabel;
    private BoardViewListenerAdapter hexClickListener;
    private GameListenerAdapter phaseChangeListener;
    private Distractable suppressedDisplay;
    private boolean finished = false;

    /**
     * Creates a hex target picker.
     *
     * @param clientGUI         The client GUI, used for the board view, toasts, and the parent frame
     * @param boardView         The board view to pick hexes on
     * @param orderDescription  Human-readable description of the order shown in the control dialog
     * @param singleHex         {@code true} to finish automatically after the first hex is picked
     * @param maxHexes          The maximum number of hexes that may be picked, or 0 for no limit
     * @param onTargetsSelected Called with the picked hexes as dash-separated hex numbers (e.g. "0810-0811")
     */
    public HexTargetPicker(ClientGUI clientGUI, BoardView boardView, String orderDescription, boolean singleHex,
          int maxHexes, Consumer<String> onTargetsSelected) {
        this.clientGUI = clientGUI;
        this.boardView = boardView;
        this.orderDescription = orderDescription;
        this.singleHex = singleHex;
        this.maxHexes = maxHexes;
        this.onTargetsSelected = onTargetsSelected;
    }

    /**
     * Starts picking: suppresses the current phase display, hooks the board view, and shows the control dialog.
     */
    public void start() {
        if (clientGUI.getCurrentPanel() instanceof Distractable distractable) {
            suppressedDisplay = distractable;
            suppressedDisplay.setIgnoringEvents(true);
        }

        hexClickListener = new BoardViewListenerAdapter() {
            @Override
            public void hexMoused(BoardViewEvent event) {
                if ((event.getType() != BoardViewEvent.BOARD_HEX_CLICKED)
                      || (event.getButton() != MouseEvent.BUTTON1)
                      || (event.getCoords() == null)) {
                    return;
                }
                addHex(event.getCoords());
            }
        };
        boardView.addBoardViewListener(hexClickListener);

        phaseChangeListener = new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent event) {
                if (event.getNewPhase() != GamePhase.UNKNOWN) {
                    finish(false);
                }
            }
        };
        clientGUI.getClient().getGame().addGameListener(phaseChangeListener);

        createControlDialog();
    }

    private void addHex(Coords coords) {
        if (pickedHexes.containsKey(coords)) {
            // clicking a selected hex again deselects it
            FieldOfFireSprite highlight = pickedHexes.remove(coords);
            boardView.removeSprites(List.of(highlight));
            clientGUI.addToast(ToastLevel.INFO, Messages.getString("BotCommandPanel.HexPicker.hexRemoved",
                  coords.getBoardNum()));
            updateStatus();
            return;
        }
        if ((maxHexes > 0) && (pickedHexes.size() >= maxHexes)) {
            // at the tube limit for a volley - reject extra hexes instead of silently dropping them later
            clientGUI.addToast(ToastLevel.WARNING, Messages.getString("BotCommandPanel.HexPicker.tooMany", maxHexes));
            return;
        }
        FieldOfFireSprite highlight = new FieldOfFireSprite(boardView, RangeType.RANGE_SHORT, coords,
              ALL_HEX_BORDERS);
        pickedHexes.put(coords, highlight);
        boardView.addSprites(List.of(highlight));
        clientGUI.addToast(ToastLevel.INFO, Messages.getString("BotCommandPanel.HexPicker.hexAdded",
              coords.getBoardNum(), pickedHexes.size()));
        if (singleHex) {
            finish(true);
        } else {
            updateStatus();
        }
    }

    private void createControlDialog() {
        controlDialog = new JDialog(clientGUI.getFrame(),
              Messages.getString("BotCommandPanel.HexPicker.title"), false);
        controlDialog.setAlwaysOnTop(true);
        controlDialog.setLayout(new BorderLayout(5, 5));

        JLabel instructions = new JLabel(Messages.getString("BotCommandPanel.HexPicker.instructions",
              orderDescription));
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        controlDialog.add(instructions, BorderLayout.NORTH);

        statusLabel = new JLabel(Messages.getString("BotCommandPanel.HexPicker.status", 0, ""));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        controlDialog.add(statusLabel, BorderLayout.CENTER);

        JButton doneButton = new JButton(Messages.getString("BotCommandPanel.HexPicker.done"));
        doneButton.addActionListener(evt -> finish(true));
        JButton cancelButton = new JButton(Messages.getString("BotCommandPanel.HexPicker.cancel"));
        cancelButton.addActionListener(evt -> finish(false));
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
        controlDialog.setLocationRelativeTo(clientGUI.getFrame());
        controlDialog.setVisible(true);
    }

    private void updateStatus() {
        String hexNumbers = pickedHexes.keySet().stream()
              .map(Coords::getBoardNum)
              .collect(Collectors.joining(", "));
        statusLabel.setText(Messages.getString("BotCommandPanel.HexPicker.status",
              pickedHexes.size(), hexNumbers));
        controlDialog.pack();
    }

    /**
     * Ends the picking session, restoring the phase display and removing all hooks.
     *
     * @param sendSelection {@code true} to deliver the picked hexes to the consumer (if any were picked)
     */
    private void finish(boolean sendSelection) {
        if (finished) {
            return;
        }
        finished = true;
        boardView.removeBoardViewListener(hexClickListener);
        clientGUI.getClient().getGame().removeGameListener(phaseChangeListener);
        if (suppressedDisplay != null) {
            suppressedDisplay.setIgnoringEvents(false);
        }
        if (controlDialog != null) {
            controlDialog.dispose();
        }
        boardView.removeSprites(pickedHexes.values());
        if (sendSelection && !pickedHexes.isEmpty()) {
            String targets = pickedHexes.keySet().stream()
                  .map(coords -> coords.hexCode(boardView.getBoard()))
                  .collect(Collectors.joining("-"));
            onTargetsSelected.accept(targets);
        }
    }
}
