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
package megamek.client.ui.panels.phaseDisplay;

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
 * The properties editor of a control point: control radius, victory point value and the scoring scheme with its
 * preset-specific numbers. Shown as a modal pane with Okay / Remove Flag / Cancel by the Victory Setup phase
 * display when a player clicks a hex; the caller applies any removal, so the editor itself never touches the
 * game.
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
        schemeCombo.setRenderer(new MessageKeyRenderer("VictoryHex.scheme."));
        JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(scheme.getThreshold(), 1, 99, 1));
        JSpinner rateSpinner = new JSpinner(new SpinnerNumberModel(scheme.getRatePerTurn(), 1, 99, 1));
        JComboBox<HoldCounting> countingCombo = new JComboBox<>(HoldCounting.values());
        countingCombo.setSelectedItem(scheme.getHoldCounting());
        countingCombo.setRenderer(new MessageKeyRenderer("VictoryHex.counting.", true));
        radiusSpinner.setToolTipText(Messages.getString("VictoryHex.radius.tooltip"));
        victoryPointSpinner.setToolTipText(Messages.getString("VictoryHex.victoryPoints.tooltip"));
        Runnable refreshCountingTooltip = () -> {
            HoldCounting counting = (HoldCounting) countingCombo.getSelectedItem();
            countingCombo.setToolTipText(Messages.getString("VictoryHex.counting."
                  + counting.name().toLowerCase(Locale.ROOT) + ".tooltip"));
        };
        countingCombo.addActionListener(event -> refreshCountingTooltip.run());
        refreshCountingTooltip.run();

        JLabel thresholdLabel = new JLabel();
        JLabel rateLabel = new JLabel();
        JLabel countingLabel = new JLabel(Messages.getString("VictoryHex.counting"));
        JLabel schemeDescription = new JLabel();
        SchemeControls controls = new SchemeControls(schemeCombo, countingCombo, thresholdSpinner, rateSpinner,
              thresholdLabel, rateLabel, countingLabel, schemeDescription);
        schemeCombo.addActionListener(event -> refreshSchemeRows(controls));
        countingCombo.addActionListener(event -> refreshSchemeRows(controls));
        thresholdSpinner.addChangeListener(event -> refreshSchemeRows(controls));
        rateSpinner.addChangeListener(event -> refreshSchemeRows(controls));
        refreshSchemeRows(controls);

        JPanel propertiesPanel = new JPanel(new GridLayout(0, 2));
        propertiesPanel.add(new JLabel(Messages.getString("VictoryHex.radius")));
        propertiesPanel.add(radiusSpinner);
        propertiesPanel.add(new JLabel(Messages.getString("VictoryHex.victoryPoints")));
        propertiesPanel.add(victoryPointSpinner);
        propertiesPanel.add(new JLabel(Messages.getString("VictoryHex.scheme")));
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
        String removeLabel = Messages.getString("VictoryHex.remove");
        String cancelLabel = Messages.getString("Cancel");
        Object[] dialogOptions = { okLabel, removeLabel, cancelLabel };
        int result = JOptionPane.showOptionDialog(frame, editorPanel,
              Messages.getString("VictoryHex.title", marker.generalName()),
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
     * The scheme-dependent controls of the properties pane, bundled so the refresh logic can live in a method
     * instead of a long lambda.
     *
     * @param schemeCombo       the scoring scheme selector
     * @param countingCombo     the Hold turn-counting selector
     * @param thresholdSpinner  the threshold value (turns to secure, starting grip or points to capture)
     * @param rateSpinner       the per-turn rate (grip drain or capture progress)
     * @param thresholdLabel    the label naming the threshold for the selected preset
     * @param rateLabel         the label naming the rate for the selected preset
     * @param countingLabel     the label of the counting selector
     * @param schemeDescription the live plain-words description of the configured scheme
     */
    private record SchemeControls(JComboBox<SchemePreset> schemeCombo, JComboBox<HoldCounting> countingCombo,
          JSpinner thresholdSpinner, JSpinner rateSpinner, JLabel thresholdLabel, JLabel rateLabel,
          JLabel countingLabel, JLabel schemeDescription) {}

    /**
     * Rewrites the scheme-dependent rows for the currently selected preset: which rows are visible, what the
     * threshold and rate spinners mean, and the plain-words description of the point as actually configured.
     *
     * @param controls the pane's scheme-dependent controls
     */
    private static void refreshSchemeRows(SchemeControls controls) {
        SchemePreset preset = (SchemePreset) controls.schemeCombo().getSelectedItem();
        // the description reflects the CONFIGURED point: the chosen mode and the actual numbers,
        // not a generic text covering every possibility
        Object threshold = controls.thresholdSpinner().getValue();
        Object rate = controls.rateSpinner().getValue();
        HoldCounting counting = (HoldCounting) controls.countingCombo().getSelectedItem();
        String presetDescription = describeConfiguredPreset(preset, threshold, rate, counting);
        controls.schemeDescription().setText("<html><body style='width: 260px'>" + presetDescription
              + "</body></html>");
        controls.schemeCombo().setToolTipText(presetDescription);
        controls.thresholdSpinner().setToolTipText(Messages.getString(thresholdLabelKey(preset) + ".tooltip"));
        controls.rateSpinner().setToolTipText(Messages.getString(rateLabelKey(preset) + ".tooltip"));
        controls.thresholdLabel().setText(Messages.getString(thresholdLabelKey(preset)));
        controls.rateLabel().setText(Messages.getString(rateLabelKey(preset)));
        boolean usesThreshold = (preset == SchemePreset.HOLD) || (preset == SchemePreset.DEFEND)
              || (preset == SchemePreset.CAPTURE);
        boolean usesRate = (preset == SchemePreset.DEFEND) || (preset == SchemePreset.CAPTURE);
        boolean usesCounting = preset == SchemePreset.HOLD;
        controls.thresholdLabel().setVisible(usesThreshold);
        controls.thresholdSpinner().setVisible(usesThreshold);
        controls.rateLabel().setVisible(usesRate);
        controls.rateSpinner().setVisible(usesRate);
        controls.countingLabel().setVisible(usesCounting);
        controls.countingCombo().setVisible(usesCounting);
    }

    /**
     * @param preset    the selected scheme preset
     * @param threshold the configured threshold value
     * @param rate      the configured per-turn rate
     * @param counting  the configured Hold turn-counting mode
     *
     * @return the plain-words what-it-does / how-to-make-it-work description of the point as configured
     */
    private static String describeConfiguredPreset(SchemePreset preset, Object threshold, Object rate,
          HoldCounting counting) {
        return switch (preset) {
            case HOLD -> Messages.getString("VictoryHex.describe.hold."
                  + counting.name().toLowerCase(Locale.ROOT), threshold);
            case DEFEND -> Messages.getString("VictoryHex.describe.defend", threshold, rate);
            case CAPTURE -> Messages.getString("VictoryHex.describe.capture", threshold, rate);
            case STANDARD, RAID -> Messages.getString("VictoryHex.describe."
                  + preset.name().toLowerCase(Locale.ROOT));
        };
    }

    /**
     * @param preset the selected scheme preset
     *
     * @return the message key naming the threshold spinner for the preset; {@code + ".tooltip"} is its tooltip
     */
    private static String thresholdLabelKey(SchemePreset preset) {
        return switch (preset) {
            case DEFEND -> "VictoryHex.startingGrip";
            case CAPTURE -> "VictoryHex.pointsToCapture";
            case HOLD, STANDARD, RAID -> "VictoryHex.turnsToSecure";
        };
    }

    /**
     * @param preset the selected scheme preset
     *
     * @return the message key naming the rate spinner for the preset; {@code + ".tooltip"} is its tooltip
     */
    private static String rateLabelKey(SchemePreset preset) {
        return (preset == SchemePreset.DEFEND) ? "VictoryHex.gripDrainPerTurn" : "VictoryHex.progressPerTurn";
    }

    /**
     * Renders an enum combo entry through a message key built from a prefix and the lower-case enum name. With
     * {@code withItemTooltips}, each entry in the open list also shows its own tooltip from the same key plus
     * {@code .tooltip}.
     */
    private static class MessageKeyRenderer extends DefaultListCellRenderer {
        private final String keyPrefix;
        private final boolean withItemTooltips;

        MessageKeyRenderer(String keyPrefix) {
            this(keyPrefix, false);
        }

        MessageKeyRenderer(String keyPrefix, boolean withItemTooltips) {
            this.keyPrefix = keyPrefix;
            this.withItemTooltips = withItemTooltips;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Enum<?> enumValue) {
                String key = keyPrefix + enumValue.name().toLowerCase(Locale.ROOT);
                setText(Messages.getString(key));
                if (withItemTooltips) {
                    setToolTipText(Messages.getString(key + ".tooltip"));
                }
            }
            return this;
        }
    }

    /**
     * The same "what it does / how to make it work" wording the setup pane shows, for a scheme already
     * configured. The board tooltip uses it so a point explains itself the same way in both places.
     *
     * @param scheme The scheme to describe
     *
     * @return The description, ready to display
     */
    public static String describeScheme(ObjectiveScoringScheme scheme) {
        return describeConfiguredPreset(scheme.getPreset(), scheme.getThreshold(), scheme.getRatePerTurn(),
              scheme.getHoldCounting());
    }
}
