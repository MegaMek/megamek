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

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.Messages;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveScoringScheme.HoldCounting;
import megamek.common.equipment.ObjectiveScoringScheme.SchemePreset;

/**
 * The properties editor of a designated victory hex: control radius, victory point value and the scoring scheme
 * with its preset-specific numbers. Shown as a modal pane with Okay / Remove Flag / Cancel; the caller applies the
 * transport (sending the player info) and any removal, so the same editor serves both the board preview's flag
 * click and the victory dialog's control point list.
 */
public final class VictoryHexPropertiesPane {

    /** What the user chose in the properties pane. */
    public enum Result {
        /** The user confirmed; the marker's properties have been updated. */
        SAVED,
        /** The user chose to remove the flag; the caller removes the marker from the designation list. */
        REMOVED,
        /** The user cancelled; the marker is unchanged. */
        CANCELLED
    }

    private VictoryHexPropertiesPane() {
    }

    /**
     * Shows the properties editor for the given marker and applies the edits to it on confirmation.
     *
     * @param frame  the parent frame
     * @param marker the designated marker to edit
     *
     * @return what the user chose; on {@link Result#REMOVED} the caller removes the marker itself
     */
    public static Result edit(JFrame frame, ObjectiveMarker marker) {
        ObjectiveScoringScheme scheme = marker.getScoringScheme();
        JSpinner radiusSpinner = new JSpinner(
              new SpinnerNumberModel(marker.getControlRadius(), 0, ObjectiveMarker.MAX_CONTROL_RADIUS, 1));
        JSpinner victoryPointSpinner = new JSpinner(
              new SpinnerNumberModel(marker.getVictoryPointValue(), 1, 99, 1));
        JComboBox<SchemePreset> schemeCombo = new JComboBox<>(SchemePreset.values());
        schemeCombo.setSelectedItem(scheme.getPreset());
        schemeCombo.setRenderer(new MessageKeyRenderer("ChatLounge.victoryHex.scheme."));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(scheme.getThreshold(), 1, 99, 1));
        JSpinner rateSpinner = new JSpinner(new SpinnerNumberModel(scheme.getRatePerTurn(), 1, 99, 1));
        JComboBox<HoldCounting> countingCombo = new JComboBox<>(HoldCounting.values());
        countingCombo.setSelectedItem(scheme.getHoldCounting());
        countingCombo.setRenderer(new MessageKeyRenderer("ChatLounge.victoryHex.counting."));
        radiusSpinner.setToolTipText(Messages.getString("ChatLounge.victoryHex.radius.tooltip"));
        victoryPointSpinner.setToolTipText(Messages.getString("ChatLounge.victoryHex.victoryPoints.tooltip"));
        countingCombo.setToolTipText(Messages.getString("ChatLounge.victoryHex.counting.tooltip"));

        JLabel thresholdLabel = new JLabel();
        JLabel rateLabel = new JLabel();
        JLabel countingLabel = new JLabel(Messages.getString("ChatLounge.victoryHex.counting"));
        JLabel schemeDescription = new JLabel();
        Runnable refreshSchemeRows = () -> {
            SchemePreset preset = (SchemePreset) schemeCombo.getSelectedItem();
            String presetDescription = Messages.getString("ChatLounge.victoryHex.describe."
                  + preset.name().toLowerCase(Locale.ROOT));
            schemeDescription.setText("<html><body style='width: 260px'>" + presetDescription
                  + "</body></html>");
            schemeCombo.setToolTipText(presetDescription);
            thresholdSpinner.setToolTipText(Messages.getString(switch (preset) {
                case HOLD -> "ChatLounge.victoryHex.turnsToSecure.tooltip";
                case DEFEND -> "ChatLounge.victoryHex.startingGrip.tooltip";
                case CAPTURE -> "ChatLounge.victoryHex.pointsToCapture.tooltip";
                case STANDARD, RAID -> "ChatLounge.victoryHex.turnsToSecure.tooltip";
            }));
            rateSpinner.setToolTipText(Messages.getString((preset == SchemePreset.DEFEND)
                  ? "ChatLounge.victoryHex.gripDrainPerTurn.tooltip"
                  : "ChatLounge.victoryHex.progressPerTurn.tooltip"));
            boolean usesThreshold = (preset == SchemePreset.HOLD) || (preset == SchemePreset.DEFEND)
                  || (preset == SchemePreset.CAPTURE);
            boolean usesRate = (preset == SchemePreset.DEFEND) || (preset == SchemePreset.CAPTURE);
            boolean usesCounting = preset == SchemePreset.HOLD;
            thresholdLabel.setText(Messages.getString(switch (preset) {
                case HOLD -> "ChatLounge.victoryHex.turnsToSecure";
                case DEFEND -> "ChatLounge.victoryHex.startingGrip";
                case CAPTURE -> "ChatLounge.victoryHex.pointsToCapture";
                case STANDARD, RAID -> "ChatLounge.victoryHex.turnsToSecure";
            }));
            rateLabel.setText(Messages.getString((preset == SchemePreset.DEFEND)
                  ? "ChatLounge.victoryHex.gripDrainPerTurn"
                  : "ChatLounge.victoryHex.progressPerTurn"));
            thresholdLabel.setVisible(usesThreshold);
            thresholdSpinner.setVisible(usesThreshold);
            rateLabel.setVisible(usesRate);
            rateSpinner.setVisible(usesRate);
            countingLabel.setVisible(usesCounting);
            countingCombo.setVisible(usesCounting);
        };
        schemeCombo.addActionListener(event -> refreshSchemeRows.run());
        refreshSchemeRows.run();

        JPanel propertiesPanel = new JPanel(new GridLayout(0, 2));
        propertiesPanel.add(new JLabel(Messages.getString("ChatLounge.victoryHex.radius")));
        propertiesPanel.add(radiusSpinner);
        propertiesPanel.add(new JLabel(Messages.getString("ChatLounge.victoryHex.victoryPoints")));
        propertiesPanel.add(victoryPointSpinner);
        propertiesPanel.add(new JLabel(Messages.getString("ChatLounge.victoryHex.scheme")));
        propertiesPanel.add(schemeCombo);
        propertiesPanel.add(thresholdLabel);
        propertiesPanel.add(thresholdSpinner);
        propertiesPanel.add(countingLabel);
        propertiesPanel.add(countingCombo);
        propertiesPanel.add(rateLabel);
        propertiesPanel.add(rateSpinner);

        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.PAGE_AXIS));
        editorPanel.add(propertiesPanel);
        editorPanel.add(schemeDescription);

        String okLabel = Messages.getString("Okay");
        String removeLabel = Messages.getString("ChatLounge.victoryHex.remove");
        String cancelLabel = Messages.getString("Cancel");
        Object[] dialogOptions = { okLabel, removeLabel, cancelLabel };
        int result = JOptionPane.showOptionDialog(frame, editorPanel,
              Messages.getString("ChatLounge.victoryHex.title", marker.generalName()),
              JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, dialogOptions, okLabel);
        if (result == 1) {
            return Result.REMOVED;
        }
        if (result != 0) {
            return Result.CANCELLED;
        }
        marker.setControlRadius((Integer) radiusSpinner.getValue());
        marker.setVictoryPointValue((Integer) victoryPointSpinner.getValue());
        scheme.setPreset((SchemePreset) schemeCombo.getSelectedItem());
        scheme.setThreshold((Integer) thresholdSpinner.getValue());
        scheme.setRatePerTurn((Integer) rateSpinner.getValue());
        scheme.setHoldCounting((HoldCounting) countingCombo.getSelectedItem());
        return Result.SAVED;
    }

    /**
     * @param marker a designated marker
     *
     * @return a one-line description of the marker for list rows: name, hex, scheme and its key numbers
     */
    public static String describe(ObjectiveMarker marker) {
        ObjectiveScoringScheme scheme = marker.getScoringScheme();
        String schemeName = Messages.getString("ChatLounge.victoryHex.scheme."
              + scheme.getPreset().name().toLowerCase(Locale.ROOT));
        StringBuilder description = new StringBuilder(marker.generalName());
        if (marker.getLobbyPosition() != null) {
            description.append(" [").append(marker.getLobbyPosition().getBoardNum()).append(']');
        }
        description.append(" - ").append(schemeName);
        switch (scheme.getPreset()) {
            case HOLD -> description.append(", ").append(scheme.getThreshold()).append(' ')
                  .append(Messages.getString("ChatLounge.victoryHex.describe.turns"));
            case DEFEND, CAPTURE -> description.append(", ").append(scheme.getThreshold());
            case STANDARD, RAID -> { }
        }
        description.append(", ").append(Messages.getString("ChatLounge.victoryHex.describe.radius"))
              .append(' ').append(marker.getControlRadius())
              .append(", ").append(marker.getVictoryPointValue()).append(" VP");
        return description.toString();
    }

    /** Renders an enum combo entry through a message key built from a prefix and the lower-case enum name. */
    private static class MessageKeyRenderer extends DefaultListCellRenderer {
        private final String keyPrefix;

        MessageKeyRenderer(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Enum<?> enumValue) {
                setText(Messages.getString(keyPrefix + enumValue.name().toLowerCase(Locale.ROOT)));
            }
            return this;
        }
    }
}
