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

package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.clientDialogs.ClientDialog;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.annotations.Nullable;

/**
 * A modeless dialog showing a single board from the lobby's available-boards list at a readable size, opened by
 * double-clicking a board in the list. It doubles as a selector: the shown board can be assigned to any board
 * slot of the map, optionally rotated by 180 degrees, as an alternative to drag-and-drop or the right-click menu.
 */
class BoardQuickPreviewDialog extends ClientDialog {

    private final ChatLounge lobby;
    private final BoardImagePanel imagePanel = new BoardImagePanel();
    private final JCheckBox rotatedCheckBox = new JCheckBox(Messages.getString("ChatLounge.BoardPreview.rotated"));
    private final JComboBox<String> slotComboBox = new JComboBox<>();
    private final JButton useButton = new JButton(Messages.getString("ChatLounge.BoardPreview.useButton"));

    private String boardName = "";
    /** The shown board's minimap image; null when no preview can be rendered (server-only or generated boards). */
    private BufferedImage boardImage;
    /** The message shown instead of an image; null while an image is shown. */
    private String noPreviewMessage;

    BoardQuickPreviewDialog(JFrame owner, ChatLounge lobby) {
        super(owner, "", false);
        this.lobby = lobby;

        rotatedCheckBox.setToolTipText(Messages.getString("ChatLounge.BoardPreview.rotatedTooltip"));
        rotatedCheckBox.addActionListener(actionEvent -> imagePanel.repaint());
        useButton.setToolTipText(Messages.getString("ChatLounge.BoardPreview.useButtonTooltip"));
        useButton.addActionListener(actionEvent -> useBoard());

        JPanel controlsPanel = new FixedYPanel(new FlowLayout(FlowLayout.CENTER));
        controlsPanel.add(rotatedCheckBox);
        controlsPanel.add(slotComboBox);
        controlsPanel.add(useButton);

        setLayout(new BorderLayout());
        add(imagePanel, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(actionEvent -> setVisible(false),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Shows the dialog (or updates it, if already open) with the given board.
     *
     * @param newBoardName        the board's path name as listed in the available-boards list
     * @param newBoardImage       the board's minimap image, or {@code null} if it cannot be rendered on this
     *                            client (server-only or generated boards)
     * @param newNoPreviewMessage the message to show instead of an image, or {@code null} when an image is given
     * @param slotCount           the number of board slots of the current map (width x height)
     * @param isRotationAllowed   whether the board may be placed rotated (only boards with an even width)
     */
    void showBoard(String newBoardName, @Nullable BufferedImage newBoardImage,
          @Nullable String newNoPreviewMessage, int slotCount, boolean isRotationAllowed) {
        boardName = newBoardName;
        boardImage = newBoardImage;
        noPreviewMessage = newNoPreviewMessage;
        setTitle(Messages.getString("ChatLounge.BoardPreview.title", new File(newBoardName).getName()));

        rotatedCheckBox.setEnabled(isRotationAllowed);
        if (!isRotationAllowed) {
            rotatedCheckBox.setSelected(false);
        }
        rebuildSlotComboBox(slotCount);
        resizeToImage();

        if (!isVisible()) {
            center();
            setVisible(true);
        }
        imagePanel.repaint();
    }

    /** Rebuilds the target-slot dropdown for the given slot count, keeping the selection where possible. */
    private void rebuildSlotComboBox(int slotCount) {
        int previousSelection = Math.max(0, slotComboBox.getSelectedIndex());
        slotComboBox.removeAllItems();
        for (int slot = 0; slot < slotCount; slot++) {
            slotComboBox.addItem(Messages.getString("ChatLounge.BoardPreview.slot", slot + 1));
        }
        slotComboBox.setSelectedIndex(Math.min(previousSelection, slotCount - 1));
        slotComboBox.setVisible(slotCount > 1);
    }

    /** Sizes the dialog to the shown image (clamped to a sane range) the first time it opens. */
    private void resizeToImage() {
        if (isVisible()) {
            // Keep the size the player chose; the image scales to fit
            return;
        }
        int preferredWidth = 500;
        int preferredHeight = 400;
        if (boardImage != null) {
            preferredWidth = Math.clamp(boardImage.getWidth(), 300, getOwner().getWidth() * 2 / 3);
            preferredHeight = Math.clamp(boardImage.getHeight(), 300, getOwner().getHeight() * 2 / 3);
        }
        imagePanel.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        pack();
    }

    /** Assigns the previewed board to the chosen slot, honoring the rotation checkbox. */
    private void useBoard() {
        int slotIndex = Math.max(0, slotComboBox.getSelectedIndex());
        boolean isRotated = rotatedCheckBox.isEnabled() && rotatedCheckBox.isSelected();
        lobby.selectBoardFromPreview(boardName, slotIndex, isRotated);
    }

    /** Paints the board image scaled to fit (rotated when the checkbox is set), or the no-preview message. */
    private class BoardImagePanel extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (boardImage == null) {
                paintNoPreviewMessage(graphics);
                return;
            }
            double scale = Math.min((double) getWidth() / boardImage.getWidth(),
                  (double) getHeight() / boardImage.getHeight());
            int drawWidth = (int) (boardImage.getWidth() * scale);
            int drawHeight = (int) (boardImage.getHeight() * scale);
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (rotatedCheckBox.isSelected()) {
                // Board rotation is a 180 degree turn; show the image the way the board will be placed
                graphics2D.rotate(Math.PI, drawX + drawWidth / 2.0, drawY + drawHeight / 2.0);
            }
            graphics2D.drawImage(boardImage, drawX, drawY, drawWidth, drawHeight, null);
            graphics2D.dispose();
        }

        private void paintNoPreviewMessage(Graphics graphics) {
            if (noPreviewMessage == null) {
                return;
            }
            FontMetrics fontMetrics = graphics.getFontMetrics();
            int textX = (getWidth() - fontMetrics.stringWidth(noPreviewMessage)) / 2;
            int textY = getHeight() / 2;
            graphics.drawString(noPreviewMessage, Math.max(10, textX), textY);
        }
    }
}
