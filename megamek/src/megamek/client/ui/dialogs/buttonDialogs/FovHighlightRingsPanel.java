/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.Messages;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;

/** Edits the distance and colour pairs used by the field-of-view highlight overlay. */
final class FovHighlightRingsPanel extends JPanel {
    static final int MIN_DISTANCE = 1;
    static final int MAX_DISTANCE = 60;

    private static final int DEFAULT_DISTANCE_STEP = 5;
    private static final String DEFAULT_COLOUR_HSB = "0.3 1.0 1.0";

    private final JPanel rowsPanel = new JPanel(new GridBagLayout());
    private final JButton addButton = new JButton(Messages.getString(
        "TacticalOverlaySettingsDialog.FovHighlightRanges.Add"));
    private final List<RingRange> ranges = new ArrayList<>();
    private final Runnable changeHandler;

    private boolean adjusting;
    private boolean editorEnabled = true;

    FovHighlightRingsPanel(String radiiValue, String coloursValue, Runnable changeHandler) {
        super(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        this.changeHandler = changeHandler;
        setOpaque(false);
        rowsPanel.setOpaque(false);
        add(rowsPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actions.setOpaque(false);
        addButton.setToolTipText(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Add.tooltip"));
        addButton.addActionListener(event -> addRange());
        actions.add(addButton);
        add(actions, BorderLayout.SOUTH);

        setValues(radiiValue, coloursValue);
    }

    void setValues(String radiiValue, String coloursValue) {
        adjusting = true;
        ranges.clear();
        ranges.addAll(parseRanges(radiiValue, coloursValue));
        if (ranges.isEmpty()) {
            ranges.add(defaultRange());
        }
        rebuildRows();
        adjusting = false;
    }

    String getRadiiValue() {
        return String.join(" ", ranges.stream()
                .map(range -> Integer.toString(range.distance))
                .toList());
    }

    String getColoursValue() {
        return String.join(" ; ", ranges.stream()
            .map(range -> range.serializedHsb)
              .toList());
    }

    int getRangeCount() {
        return ranges.size();
    }

    int getDistance(int index) {
        return ranges.get(index).distance;
    }

    Color getColour(int index) {
        return ranges.get(index).colour;
    }

    void setDistance(int index, int distance) {
        updateDistance(ranges.get(index), distance, null);
    }

    void setColour(int index, Color colour) {
        RingRange range = ranges.get(index);
        range.colour = colour;
        range.serializedHsb = serializeColour(colour);
        rebuildRows();
        notifyChanged();
    }

    void addRange() {
        int distance = nextAvailableDistance();
        if (distance < MIN_DISTANCE) {
            return;
        }
        RingRange previous = ranges.getLast();
        ranges.add(new RingRange(distance, previous.colour, previous.serializedHsb));
        sortRanges();
        rebuildRows();
        notifyChanged();
    }

    void removeRange(int index) {
        if (ranges.size() <= 1) {
            return;
        }
        ranges.remove(index);
        rebuildRows();
        notifyChanged();
    }

    void setEditorEnabled(boolean enabled) {
        editorEnabled = enabled;
        rebuildRows();
    }

    private void rebuildRows() {
        rowsPanel.removeAll();
        addHeader();
        for (int index = 0; index < ranges.size(); index++) {
            addRangeRow(index, ranges.get(index));
        }
        addButton.setEnabled(editorEnabled && (nextAvailableDistance() >= MIN_DISTANCE));
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private void addHeader() {
        JLabel distanceHeader = new JLabel(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Distance"));
        distanceHeader.setEnabled(editorEnabled);
        rowsPanel.add(distanceHeader, constraints(0, 0, 2, 0.0, GridBagConstraints.NONE));

        JLabel colourHeader = new JLabel(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Color"));
        colourHeader.setEnabled(editorEnabled);
        rowsPanel.add(colourHeader, constraints(2, 0, 1, 1.0, GridBagConstraints.HORIZONTAL));
    }

    private void addRangeRow(int index, RingRange range) {
        JSpinner distanceSpinner = new JSpinner(new SpinnerNumberModel(
              range.distance, MIN_DISTANCE, MAX_DISTANCE, 1));
        distanceSpinner.setEnabled(editorEnabled);
        distanceSpinner.setToolTipText(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Distance.tooltip"));
        distanceSpinner.getAccessibleContext().setAccessibleName(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRanges.Distance.accessible", index + 1));
        distanceSpinner.addChangeListener(event -> updateDistance(
              range, (int) distanceSpinner.getValue(), distanceSpinner));
        rowsPanel.add(distanceSpinner, constraints(0, index + 1, 1, 0.0, GridBagConstraints.HORIZONTAL));

        JLabel unitLabel = new JLabel(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Unit"));
        unitLabel.setEnabled(editorEnabled);
        rowsPanel.add(unitLabel, constraints(1, index + 1, 1, 0.0, GridBagConstraints.NONE));

        JButton colourButton = createColourButton(index, range);
        rowsPanel.add(colourButton, constraints(2, index + 1, 1, 1.0, GridBagConstraints.HORIZONTAL));

        JButton removeButton = new JButton();
        removeButton.setIcon(FontHandler.symbolIcon(0xE872,
              removeButton.getFont().getSize() + UIUtil.scaleForGUI(2), removeButton.getForeground()));
        sizeIconButtonToControlHeight(removeButton, distanceSpinner);
        removeButton.setEnabled(editorEnabled && (ranges.size() > 1));
        removeButton.setToolTipText(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Remove.tooltip"));
        removeButton.getAccessibleContext().setAccessibleName(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRanges.Remove.accessible", index + 1));
        removeButton.addActionListener(event -> removeRange(index));
        rowsPanel.add(removeButton, constraints(3, index + 1, 1, 0.0, GridBagConstraints.NONE));
    }

    static void sizeIconButtonToControlHeight(JButton button, JComponent control) {
        int side = control.getPreferredSize().height;
        Dimension size = new Dimension(side, side);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    private JButton createColourButton(int index, RingRange range) {
        JButton button = new JButton(colourText(range.colour), colourIcon(range.colour));
        button.setEnabled(editorEnabled);
        button.setToolTipText(Messages.getString(
            "TacticalOverlaySettingsDialog.FovHighlightRanges.Color.tooltip"));
        button.getAccessibleContext().setAccessibleName(Messages.getString(
              "TacticalOverlaySettingsDialog.FovHighlightRanges.Color.accessible", index + 1));
        button.addActionListener(event -> {
            Color selected = JColorChooser.showDialog(this,
                Messages.getString("TacticalOverlaySettingsDialog.FovHighlightRanges.Color.choose"),
                range.colour);
            if (selected != null) {
                range.colour = selected;
                range.serializedHsb = serializeColour(selected);
                button.setText(colourText(selected));
                button.setIcon(colourIcon(selected));
                notifyChanged();
            }
        });
        return button;
    }

    private void updateDistance(RingRange range, int distance, JSpinner source) {
        int clampedDistance = Math.clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
        if (adjusting || (range.distance == clampedDistance)) {
            return;
        }
        boolean duplicate = ranges.stream()
            .anyMatch(candidate -> (candidate != range) && (candidate.distance == clampedDistance));
        if (duplicate) {
            Toolkit.getDefaultToolkit().beep();
            if (source != null) {
                adjusting = true;
                source.setValue(range.distance);
                adjusting = false;
            }
            return;
        }
        range.distance = clampedDistance;
        sortRanges();
        rebuildRows();
        notifyChanged();
    }

    private int nextAvailableDistance() {
        int preferred = Math.min(MAX_DISTANCE, ranges.getLast().distance + DEFAULT_DISTANCE_STEP);
        if (ranges.stream().noneMatch(range -> range.distance == preferred)) {
            return preferred;
        }
        for (int distance = MIN_DISTANCE; distance <= MAX_DISTANCE; distance++) {
            int candidate = distance;
            if (ranges.stream().noneMatch(range -> range.distance == candidate)) {
                return candidate;
            }
        }
        return -1;
    }

    private void sortRanges() {
        ranges.sort(Comparator.comparingInt(range -> range.distance));
    }

    private void notifyChanged() {
        if (!adjusting) {
            changeHandler.run();
        }
    }

    private static List<RingRange> parseRanges(String radiiValue, String coloursValue) {
        String radiiText = radiiValue == null ? "" : radiiValue.trim();
        String coloursText = coloursValue == null ? "" : coloursValue.trim();
        if (radiiText.isEmpty() || coloursText.isEmpty()) {
            return List.of();
        }

        String[] radii = radiiText.split("\\s+");
        String[] colours = coloursText.split(";");
        List<RingRange> parsed = new ArrayList<>();
        int pairCount = Math.min(radii.length, colours.length);
        for (int index = 0; index < pairCount; index++) {
            try {
                int distance = Math.clamp(Integer.parseInt(radii[index]), MIN_DISTANCE, MAX_DISTANCE);
                if (parsed.stream().anyMatch(range -> range.distance == distance)) {
                    continue;
                }
                String[] hsb = colours[index].trim().split("\\s+");
                if (hsb.length < 3) {
                    continue;
                }
                float hue = Float.parseFloat(hsb[0]);
                float saturation = Float.parseFloat(hsb[1]);
                float brightness = Float.parseFloat(hsb[2]);
                if (!Float.isFinite(hue) || !Float.isFinite(saturation) || !Float.isFinite(brightness)) {
                    continue;
                }
                Color colour = new Color(Color.HSBtoRGB(hue, saturation, brightness));
                parsed.add(new RingRange(distance, colour,
                    String.join(" ", hsb[0], hsb[1], hsb[2])));
            } catch (NumberFormatException ignored) {
                // Keep other valid pairs from a hand-edited preferences file.
            }
        }
        parsed.sort(Comparator.comparingInt(range -> range.distance));
        return parsed;
    }

    private static RingRange defaultRange() {
        Color colour = new Color(Color.HSBtoRGB(0.3f, 1.0f, 1.0f));
        return new RingRange(DEFAULT_DISTANCE_STEP, colour, DEFAULT_COLOUR_HSB);
    }

    private static String serializeColour(Color colour) {
        float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
        return Float.toString(hsb[0]) + " " + Float.toString(hsb[1]) + " " + Float.toString(hsb[2]);
    }

    private static String colourText(Color colour) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", colour.getRed(), colour.getGreen(), colour.getBlue());
    }

    private static ImageIcon colourIcon(Color colour) {
        int width = UIUtil.scaleForGUI(24);
        int height = UIUtil.scaleForGUI(16);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, colour.getRGB());
            }
        }
        return new ImageIcon(image);
    }

    private static GridBagConstraints constraints(int x, int y, int width, double weight,
        int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.weightx = weight;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = fill;
        constraints.insets = new Insets(UIUtil.scaleForGUI(3), 0,
              UIUtil.scaleForGUI(3), x == 3 ? 0 : UIUtil.scaleForGUI(8));
        return constraints;
    }

    private static final class RingRange {
        private int distance;
        private Color colour;
        private String serializedHsb;

        private RingRange(int distance, Color colour, String serializedHsb) {
            this.distance = distance;
            this.colour = colour;
            this.serializedHsb = serializedHsb;
        }
    }
}
