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
 */
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import megamek.common.board.BoardType;
import megamek.common.loaders.MapSettings;
import megamek.common.loaders.MapSettings.BoardLayer;

/**
 * Configures a Total Warfare multi-board stack above the primary (ground) board:
 * Low Altitude (Sky), High Altitude (Near Space), and Deep Space (Far Space). Any prefix of the stack may be
 * enabled, with each enabled layer carrying its own dimensions and an embed coordinate that says where the
 * layer below it sits in this layer's hex grid.
 *
 * <p>The dialog reads from and writes to the caller's {@link MapSettings} via
 * {@link MapSettings#getAdditionalBoards()} and {@link MapSettings#setAdditionalBoards(List)}. The caller is
 * responsible for sending the updated settings to the server.</p>
 */
public class MultiBoardSetupDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Strict TW stack order above the primary board, bottom-up. The primary board (managed elsewhere in the
     * lobby) sits below these; enabling layer i here requires every layer j < i to be enabled too.
     */
    private static final BoardType[] STACK_ORDER = {
          BoardType.SKY_WITH_TERRAIN,
          BoardType.NEAR_SPACE,
          BoardType.FAR_SPACE
    };

    private static final int DEFAULT_BOARD_WIDTH = 50;
    private static final int DEFAULT_BOARD_HEIGHT = 50;

    private final MapSettings mapSettings;
    private final Map<BoardType, LayerRow> rows = new EnumMap<>(BoardType.class);
    private boolean accepted;

    public MultiBoardSetupDialog(Frame parent, MapSettings mapSettings) {
        super(parent, "Multi-Board Setup (Total Warfare)", true);
        this.mapSettings = mapSettings;

        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        loadFromSettings();
        refreshEnabledState();

        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel description = new JLabel(
              "<html><body style='width:420px'>Enable additional board layers stacked above the primary ground map."
                    + " Each layer's embed coordinates are the hex on that layer that the layer below occupies."
                    + " Layers must be enabled bottom-up: Low Altitude before High Altitude before Deep Space."
                    + "</body></html>");
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(description);
        content.add(Box.createVerticalStrut(10));

        for (BoardType type : STACK_ORDER) {
            LayerRow row = new LayerRow(type);
            rows.put(type, row);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(row);
            content.add(Box.createVerticalStrut(6));
        }

        return content;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (commit()) {
                accepted = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            accepted = false;
            dispose();
        });
        buttons.add(okButton);
        buttons.add(cancelButton);
        getRootPane().setDefaultButton(okButton);
        return buttons;
    }

    private void loadFromSettings() {
        List<BoardLayer> existing = mapSettings.getAdditionalBoards();
        // existing[i].boardType is authoritative; use it to populate matching rows
        for (BoardLayer layer : existing) {
            LayerRow row = rows.get(layer.getBoardType());
            if (row != null) {
                row.loadFrom(layer);
            }
        }
    }

    private void refreshEnabledState() {
        // Each row is only enabled if every row below it in STACK_ORDER is enabled.
        // A row may become disabled — in that case we uncheck it to preserve the strict-stack invariant.
        boolean belowEnabled = true;
        for (BoardType type : STACK_ORDER) {
            LayerRow row = rows.get(type);
            row.setRowEnabled(belowEnabled);
            if (!belowEnabled) {
                row.setEnabledChecked(false);
            }
            belowEnabled = belowEnabled && row.isEnabledChecked();
        }
    }

    /**
     * Validates and writes the dialog state back to {@link #mapSettings}. On validation failure, returns false
     * and keeps the dialog open.
     */
    private boolean commit() {
        List<BoardLayer> layers = new ArrayList<>();
        for (BoardType type : STACK_ORDER) {
            LayerRow row = rows.get(type);
            if (!row.isEnabledChecked()) {
                break;
            }
            layers.add(row.buildLayer());
        }
        mapSettings.setAdditionalBoards(layers);
        return true;
    }

    public boolean isAccepted() {
        return accepted;
    }

    /** One row of the dialog: a single optional layer above the primary board. */
    private class LayerRow extends JPanel {

        @Serial
        private static final long serialVersionUID = 1L;

        private final BoardType boardType;
        private final JCheckBox enableBox;
        private final JSpinner widthSpinner;
        private final JSpinner heightSpinner;
        private final JSpinner embedXSpinner;
        private final JSpinner embedYSpinner;

        LayerRow(BoardType boardType) {
            this.boardType = boardType;
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(boardType.toString()));

            enableBox = new JCheckBox("Enable " + boardType.toString());
            enableBox.addActionListener(e -> refreshEnabledState());

            widthSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_BOARD_WIDTH, 16, 200, 1));
            heightSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_BOARD_HEIGHT, 16, 200, 1));
            embedXSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_BOARD_WIDTH / 2, 0, 199, 1));
            embedYSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_BOARD_HEIGHT / 2, 0, 199, 1));

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 4, 2, 4);
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 5;
            c.anchor = GridBagConstraints.LINE_START;
            add(enableBox, c);

            c.gridwidth = 1;
            c.gridy = 1;
            c.gridx = 0;
            add(new JLabel("Width:", SwingConstants.RIGHT), c);
            c.gridx = 1;
            add(widthSpinner, c);
            c.gridx = 2;
            add(new JLabel("Height:", SwingConstants.RIGHT), c);
            c.gridx = 3;
            add(heightSpinner, c);

            c.gridy = 2;
            c.gridx = 0;
            add(new JLabel("Embed X:", SwingConstants.RIGHT), c);
            c.gridx = 1;
            add(embedXSpinner, c);
            c.gridx = 2;
            add(new JLabel("Embed Y:", SwingConstants.RIGHT), c);
            c.gridx = 3;
            add(embedYSpinner, c);
        }

        boolean isEnabledChecked() {
            return enableBox.isSelected();
        }

        void setEnabledChecked(boolean on) {
            if (enableBox.isSelected() != on) {
                enableBox.setSelected(on);
            }
        }

        void setRowEnabled(boolean enabled) {
            enableBox.setEnabled(enabled);
            widthSpinner.setEnabled(enabled && enableBox.isSelected());
            heightSpinner.setEnabled(enabled && enableBox.isSelected());
            embedXSpinner.setEnabled(enabled && enableBox.isSelected());
            embedYSpinner.setEnabled(enabled && enableBox.isSelected());
        }

        void loadFrom(BoardLayer layer) {
            enableBox.setSelected(true);
            if (layer.getSettings() != null) {
                widthSpinner.setValue(layer.getSettings().getBoardWidth());
                heightSpinner.setValue(layer.getSettings().getBoardHeight());
            }
            int x = layer.getEmbedX() >= 0 ? layer.getEmbedX() : DEFAULT_BOARD_WIDTH / 2;
            int y = layer.getEmbedY() >= 0 ? layer.getEmbedY() : DEFAULT_BOARD_HEIGHT / 2;
            embedXSpinner.setValue(x);
            embedYSpinner.setValue(y);
        }

        BoardLayer buildLayer() {
            MapSettings layerSettings = MapSettings.getInstance();
            int w = (Integer) widthSpinner.getValue();
            int h = (Integer) heightSpinner.getValue();
            layerSettings.setBoardSize(w, h);
            layerSettings.setMapSize(1, 1);
            int ex = (Integer) embedXSpinner.getValue();
            int ey = (Integer) embedYSpinner.getValue();
            // Clamp embed coords to the layer's own dimensions so TWGameManager does not reject them.
            if (ex >= w) {
                ex = w - 1;
            }
            if (ey >= h) {
                ey = h - 1;
            }
            return new BoardLayer(boardType, layerSettings, ex, ey);
        }
    }
}
