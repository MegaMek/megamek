/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * @author Ken Nguyen (kenn)
 */
public class RulerDialog extends JDialog implements BoardViewListener {
    private static final MMLogger logger = MMLogger.create(RulerDialog.class);

    @Serial
    private static final long serialVersionUID = -4820402626782115601L;
    // Dark theme: bright, saturated colors for contrast against dark backgrounds
    private static final Color DARK_COLOR_1 = new Color(0x4D, 0xD0, 0xE1);  // soft cyan
    private static final Color DARK_COLOR_2 = new Color(0xF4, 0x8F, 0xB1);  // soft pink

    // Light theme: vivid, medium-dark colors for readability on white/light gray backgrounds
    private static final Color LIGHT_COLOR_1 = new Color(0x00, 0x7B, 0xB8);  // strong blue
    private static final Color LIGHT_COLOR_2 = new Color(0xC6, 0x28, 0x28);  // strong red

    public static Color color1 = DARK_COLOR_1;
    public static Color color2 = DARK_COLOR_2;

    private Coords start;
    private Coords end;
    private Color startColor;
    private Color endColor;
    private int distance;
    private final BoardView bv;
    private final Game game;
    private boolean flip;

    private final JButton butFlip = new JButton();
    private final JTextField tf_start = new JTextField();
    private final JTextField tf_end = new JTextField();
    private final JLabel rangeLabel = new JLabel();
    private final JTextField tf_los1 = new JTextField();
    private final JTextField tf_los2 = new JTextField();
    private final JButton butClose = new JButton();
    private JLabel heightLabel1;
    private final JSpinner height1 = new JSpinner(new SpinnerNumberModel(1, -100, 200, 1));
    private final JLabel effectiveHeight1 = new JLabel();
    private final JLabel heightInfo1 = new JLabel();
    private JLabel heightLabel2;
    private final JSpinner height2 = new JSpinner(new SpinnerNumberModel(1, -100, 200, 1));
    private final JLabel effectiveHeight2 = new JLabel();
    private final JLabel heightInfo2 = new JLabel();

    private JPanel unitPanel1;
    private JPanel unitPanel2;
    private JLabel attackerPovLabel;
    private JLabel targetPovLabel;

    private static final String HEIGHT_TOOLTIP = "<html>TW unit height (p.43):<br>"
          + "<b>Height</b> (ground units): Mek 2, Superheavy 3, Vehicle/Inf 1<br>"
          + "<b>Elevation</b> (VTOL/WiGE): levels above hex terrain<br>"
          + "<b>Altitude</b> (aerospace): fixed value (1-10)</html>";
    private final JComboBox<EntityItem> cboEntity1 = new JComboBox<>();
    private final JComboBox<EntityItem> cboEntity2 = new JComboBox<>();
    private String entityName1 = "";
    private String entityName2 = "";
    private DiagramUnitType unitType1 = DiagramUnitType.OTHER;
    private DiagramUnitType unitType2 = DiagramUnitType.OTHER;
    /** True if point 1 entity is actually at altitude (airborne aero with altitude > 0). */
    private boolean atAltitude1 = false;
    /** True if point 2 entity is actually at altitude (airborne aero with altitude > 0). */
    private boolean atAltitude2 = false;
    /** Expected TW height for the entity at point 1, or -1 if no entity. Used to detect spinner overrides. */
    private int entityExpectedHeight1 = -1;
    /** Expected TW height for the entity at point 2, or -1 if no entity. Used to detect spinner overrides. */
    private int entityExpectedHeight2 = -1;
    /** Suppresses combo box listener events during programmatic updates. */
    private boolean updatingCombo = false;

    private final JButton butDiagram = new JButton();
    private final LOSElevationDiagramPanel diagramPanel = new LOSElevationDiagramPanel();
    private final JScrollPane diagramScrollPane = new JScrollPane(diagramPanel);
    private boolean diagramExpanded;

    private final JButton butCompare = new JButton();
    private final DefaultTableModel compareTableModel = new DefaultTableModel(
          new String[] {
                Messages.getString("Ruler.compareMode"),
                Messages.getString("Ruler.compareAttacker"),
                Messages.getString("Ruler.compareTarget")
          }, 0) {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable compareTable = new JTable(compareTableModel);
    private final JScrollPane compareScrollPane = new JScrollPane(compareTable);
    private boolean compareExpanded;

    public RulerDialog(JFrame frame, BoardView boardView, Game game) {
        super(frame, getRulerTitle(game), false);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        start = null;
        end = null;
        flip = true;
        updateThemeColors();
        startColor = color1;
        endColor = color2;

        bv = boardView;
        this.game = game;
        boardView.addBoardViewListener(this);

        try {
            jbInit();
            restoreSavedBounds();
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    saveBounds();
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    saveBounds();
                }
            });
        } catch (Exception ex) {
            logger.error(ex, "");
        }
    }

    private void jbInit() {
        butFlip.setText(Messages.getString("Ruler.flip"));
        butFlip.addActionListener(e -> butFlip_actionPerformed());
        butClose.setText(Messages.getString("Ruler.Close"));
        butClose.addActionListener(e -> butClose_actionPerformed());

        heightLabel1 = new JLabel(Messages.getString("Ruler.Height"), SwingConstants.RIGHT);
        heightLabel1.setForeground(startColor);
        height1.setToolTipText(HEIGHT_TOOLTIP);
        height1.addChangeListener(e -> heightSpinnerChanged());

        heightLabel2 = new JLabel(Messages.getString("Ruler.Height"), SwingConstants.RIGHT);
        heightLabel2.setForeground(endColor);
        height2.setToolTipText(HEIGHT_TOOLTIP);
        height2.addChangeListener(e -> heightSpinnerChanged());

        cboEntity1.setVisible(false);
        cboEntity1.addActionListener(e -> entityComboChanged(cboEntity1, true));
        cboEntity2.setVisible(false);
        cboEntity2.addActionListener(e -> entityComboChanged(cboEntity2, false));

        // --- Side-by-side unit panels ---
        unitPanel1 = buildUnitPanel(heightLabel1, height1, effectiveHeight1,
              heightInfo1, cboEntity1, color1);
        unitPanel2 = buildUnitPanel(heightLabel2, height2, effectiveHeight2,
              heightInfo2, cboEntity2, color2);

        // Range display between unit panels: "Range" header + "<- NUMBER ->" value
        rangeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rangeLabel.setFont(rangeLabel.getFont().deriveFont(Font.BOLD, UIUtil.scaleForGUI(18.0f)));

        JLabel rangeHeader = new JLabel(Messages.getString("Ruler.Distance"), SwingConstants.CENTER);
        rangeHeader.setFont(rangeHeader.getFont().deriveFont(Font.PLAIN, UIUtil.scaleForGUI(10.0f)));

        JPanel rangePanel = new JPanel(new GridBagLayout());
        GridBagConstraints rc = new GridBagConstraints();
        rc.gridx = 0;
        rc.gridy = 0;
        rc.anchor = GridBagConstraints.CENTER;
        rangePanel.add(rangeHeader, rc);
        rc.gridy = 1;
        rangePanel.add(rangeLabel, rc);

        JPanel unitsRow = new JPanel(new GridBagLayout());
        GridBagConstraints uc = new GridBagConstraints();
        uc.gridx = 0;
        uc.gridy = 0;
        uc.weightx = 1.0;
        uc.fill = GridBagConstraints.BOTH;
        uc.insets = new Insets(0, 0, 0, 0);
        unitsRow.add(unitPanel1, uc);
        uc.gridx = 1;
        uc.weightx = 0;
        uc.fill = GridBagConstraints.VERTICAL;
        uc.insets = new Insets(0, UIUtil.scaleForGUI(4), 0, UIUtil.scaleForGUI(4));
        unitsRow.add(rangePanel, uc);
        uc.gridx = 2;
        uc.weightx = 1.0;
        uc.fill = GridBagConstraints.BOTH;
        uc.insets = new Insets(0, 0, 0, 0);
        unitsRow.add(unitPanel2, uc);

        // --- Details panel (Start/End/Distance/LOS) ---
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints dc = new GridBagConstraints();
        int detailRow = 0;

        tf_start.setEditable(false);
        tf_end.setEditable(false);
        tf_los1.setEditable(false);
        tf_los2.setEditable(false);

        // Start / End on one row
        dc.gridy = detailRow;
        dc.anchor = GridBagConstraints.EAST;
        dc.fill = GridBagConstraints.NONE;
        dc.insets = new Insets(2, 4, 0, 2);
        dc.gridx = 0;
        detailsPanel.add(new JLabel(Messages.getString("Ruler.Start"), SwingConstants.RIGHT), dc);
        dc.gridx = 1;
        dc.anchor = GridBagConstraints.WEST;
        dc.fill = GridBagConstraints.HORIZONTAL;
        dc.weightx = 0.5;
        detailsPanel.add(tf_start, dc);
        dc.gridx = 2;
        dc.anchor = GridBagConstraints.EAST;
        dc.fill = GridBagConstraints.NONE;
        dc.weightx = 0;
        detailsPanel.add(new JLabel(Messages.getString("Ruler.End"), SwingConstants.RIGHT), dc);
        dc.gridx = 3;
        dc.anchor = GridBagConstraints.WEST;
        dc.fill = GridBagConstraints.HORIZONTAL;
        dc.weightx = 0.5;
        detailsPanel.add(tf_end, dc);
        detailRow++;

        // Attacker POV
        attackerPovLabel = new JLabel(Messages.getString("Ruler.attackerPOV") + ":", SwingConstants.RIGHT);
        attackerPovLabel.setForeground(startColor);
        dc.gridy = detailRow;
        dc.gridx = 0;
        dc.anchor = GridBagConstraints.EAST;
        dc.fill = GridBagConstraints.NONE;
        dc.weightx = 0;
        detailsPanel.add(attackerPovLabel, dc);
        dc.gridx = 1;
        dc.gridwidth = 3;
        dc.anchor = GridBagConstraints.WEST;
        dc.fill = GridBagConstraints.HORIZONTAL;
        dc.weightx = 1.0;
        detailsPanel.add(tf_los1, dc);
        dc.gridwidth = 1;
        dc.weightx = 0;
        detailRow++;

        // Target POV
        targetPovLabel = new JLabel(Messages.getString("Ruler.targetPOV") + ":", SwingConstants.RIGHT);
        targetPovLabel.setForeground(endColor);
        dc.gridy = detailRow;
        dc.gridx = 0;
        dc.anchor = GridBagConstraints.EAST;
        dc.fill = GridBagConstraints.NONE;
        detailsPanel.add(targetPovLabel, dc);
        dc.gridx = 1;
        dc.gridwidth = 3;
        dc.anchor = GridBagConstraints.WEST;
        dc.fill = GridBagConstraints.HORIZONTAL;
        dc.weightx = 1.0;
        detailsPanel.add(tf_los2, dc);
        dc.gridwidth = 1;
        dc.weightx = 0;
        detailRow++;

        // Buttons: Diagram toggle, Compare toggle, Flip, Close - all on one row
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        diagramExpanded = guiPreferences.getRulerDiagramVisible();
        updateDiagramButtonText();
        butDiagram.addActionListener(e -> toggleDiagram());

        compareExpanded = guiPreferences.getRulerCompareVisible();
        updateCompareButtonText();
        butCompare.addActionListener(e -> toggleCompare());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(butDiagram);
        buttonPanel.add(butCompare);
        buttonPanel.add(butFlip);
        buttonPanel.add(butClose);
        dc.gridy = detailRow;
        dc.gridx = 0;
        dc.gridwidth = 4;
        dc.anchor = GridBagConstraints.CENTER;
        dc.fill = GridBagConstraints.NONE;
        dc.insets = new Insets(4, 0, 0, 0);
        detailsPanel.add(buttonPanel, dc);

        // --- Main layout assembly ---
        JPanel panelMain = new JPanel(new GridBagLayout());
        GridBagConstraints mc = new GridBagConstraints();
        int mainRow = 0;

        mc.gridx = 0;
        mc.gridy = mainRow++;
        mc.fill = GridBagConstraints.HORIZONTAL;
        mc.weightx = 1.0;
        mc.insets = new Insets(0, 0, 0, 0);
        panelMain.add(unitsRow, mc);

        mc.gridy = mainRow++;
        panelMain.add(detailsPanel, mc);

        diagramScrollPane.setVisible(diagramExpanded);
        diagramScrollPane.setMinimumSize(UIUtil.scaleForGUI(200, 150));
        diagramScrollPane.setPreferredSize(UIUtil.scaleForGUI(500, 200));
        mc.gridy = mainRow++;
        mc.fill = GridBagConstraints.BOTH;
        mc.weighty = 1.0;
        mc.insets = new Insets(0, 0, 0, 0);
        panelMain.add(diagramScrollPane, mc);

        // Compare table setup
        compareTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        compareTable.setRowSelectionAllowed(false);
        compareTable.setFillsViewportHeight(true);
        compareTable.getTableHeader().setReorderingAllowed(false);
        compareTable.setDefaultRenderer(Object.class, new CompareTableRenderer());

        compareScrollPane.setVisible(compareExpanded);
        compareScrollPane.setMinimumSize(UIUtil.scaleForGUI(200, 80));
        compareScrollPane.setPreferredSize(UIUtil.scaleForGUI(500, 90));
        mc.gridy = mainRow;
        mc.weighty = 0.3;
        panelMain.add(compareScrollPane, mc);

        JScrollPane sp = new JScrollPane(panelMain);
        setLayout(new BorderLayout());
        add(sp);

        setResizable(true);
        setMinimumSize(UIUtil.scaleForGUI(450, 250));
        validate();
        setVisible(false);
    }

    /**
     * Builds one of the two side-by-side unit panels containing height label, spinner, effective height, info line, and
     * entity combo.
     */
    private JPanel buildUnitPanel(JLabel heightLabel, JSpinner heightSpinner,
          JLabel effectiveLabel, JLabel infoLabel, JComboBox<EntityItem> entityCombo, Color color) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createLineBorder(color.darker(), 1));

        effectiveLabel.setForeground(color);
        effectiveLabel.setFont(effectiveLabel.getFont().deriveFont(Font.ITALIC));
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        infoLabel.setForeground(color);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 4, 0, 4);
        int row = 0;

        // Row 0: Height label + spinner + effective height
        gc.gridy = row;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.EAST;
        gc.fill = GridBagConstraints.NONE;
        panel.add(heightLabel, gc);
        gc.gridx = 1;
        gc.anchor = GridBagConstraints.WEST;
        panel.add(heightSpinner, gc);
        gc.gridx = 2;
        gc.weightx = 1.0;
        panel.add(effectiveLabel, gc);
        gc.weightx = 0;
        row++;

        // Row 1: Info line (Hex + Height = LOS)
        gc.gridy = row;
        gc.gridx = 0;
        gc.gridwidth = 3;
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 4, 2, 4);
        panel.add(infoLabel, gc);
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        row++;

        // Row 2: Entity combo (hidden until hex has multiple entities)
        gc.gridy = row;
        gc.gridx = 0;
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 4, 2, 4);
        panel.add(entityCombo, gc);

        return panel;
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    private void cancel() {
        dispose();
        butClose_actionPerformed();
    }

    /**
     * Shows the dialog without stealing focus from the board view. This allows the user to continue ALT/CTRL+clicking
     * hexes without the dialog intercepting input. Focus is re-enabled when the user explicitly clicks on the dialog to
     * interact with it (e.g., height fields, combo boxes).
     */
    private void showWithoutFocus() {
        updateThemeColors();
        applyColorsToUI();
        if (!isVisible()) {
            setFocusableWindowState(false);
            setVisible(true);
            setFocusableWindowState(true);
        } else {
            repaint();
        }
    }

    private void clear() {
        start = null;
        end = null;
        entityName1 = "";
        entityName2 = "";
        unitType1 = DiagramUnitType.OTHER;
        unitType2 = DiagramUnitType.OTHER;
        atAltitude1 = false;
        atAltitude2 = false;
        entityExpectedHeight1 = -1;
        entityExpectedHeight2 = -1;
        heightLabel1.setText(Messages.getString("Ruler.Height"));
        heightLabel2.setText(Messages.getString("Ruler.Height"));
        effectiveHeight1.setText("");
        effectiveHeight2.setText("");
        heightInfo1.setText("");
        heightInfo2.setText("");
        updatingCombo = true;
        try {
            cboEntity1.setModel(new DefaultComboBoxModel<>());
            cboEntity2.setModel(new DefaultComboBoxModel<>());
            cboEntity1.setVisible(false);
            cboEntity2.setVisible(false);
        } finally {
            updatingCombo = false;
        }
    }

    /**
     * Detects whether the current L&amp;F is dark or light and sets color1/color2 accordingly. Dark themes get brighter
     * colors for contrast; light themes get deeper colors.
     */
    private static void updateThemeColors() {
        Color panelBg = UIManager.getColor("Panel.background");
        boolean isDark = (panelBg != null)
              && ((panelBg.getRed() + panelBg.getGreen() + panelBg.getBlue()) / 3 < 128);
        color1 = isDark ? DARK_COLOR_1 : LIGHT_COLOR_1;
        color2 = isDark ? DARK_COLOR_2 : LIGHT_COLOR_2;
    }

    /**
     * Applies the current color1/color2 to all color-dependent UI elements: panel borders, labels, info text, effective
     * height, and POV labels.
     */
    private void applyColorsToUI() {
        startColor = flip ? color1 : color2;
        endColor = flip ? color2 : color1;

        // Unit panel borders
        if (unitPanel1 != null) {
            unitPanel1.setBorder(BorderFactory.createLineBorder(color1.darker(), 1));
        }
        if (unitPanel2 != null) {
            unitPanel2.setBorder(BorderFactory.createLineBorder(color2.darker(), 1));
        }

        // Height labels, info lines, effective height labels
        heightLabel1.setForeground(color1);
        heightLabel2.setForeground(color2);
        heightInfo1.setForeground(color1);
        heightInfo2.setForeground(color2);
        effectiveHeight1.setForeground(color1);
        effectiveHeight2.setForeground(color2);

        // POV labels
        if (attackerPovLabel != null) {
            attackerPovLabel.setForeground(startColor);
        }
        if (targetPovLabel != null) {
            targetPovLabel.setForeground(endColor);
        }
    }

    private void addPoint(Coords c) {
        if (end != null) {
            // Both points already set - start a new measurement
            clear();
            setVisible(false);
        }

        if (start == null) {
            start = c;
            Entity tallest = populateEntityCombo(c, cboEntity1);
            if (tallest != null) {
                applyEntitySelection(tallest, true);
            }
        } else if (start.equals(c)) {
            clear();
            setVisible(false);
        } else {
            end = c;
            distance = start.distance(end);
            Entity tallest = populateEntityCombo(c, cboEntity2);
            if (tallest != null) {
                applyEntitySelection(tallest, false);
            }
            setText();
            showWithoutFocus();
        }
    }

    /**
     * Applies an entity selection to the appropriate point's UI fields: sets the height spinner, entity name, unit
     * type, and height label.
     *
     * @param entity       the selected entity
     * @param isFirstPoint true for attacker (point 1), false for target (point 2)
     */
    private void applyEntitySelection(Entity entity, boolean isFirstPoint) {
        boolean sensorReturn = isSensorReturn(entity);
        // For sensor returns: hide identity and use generic height/type so state isn't revealed.
        // The player knows *something* is there and can still compute LOS/range using its position.
        int twHeight = sensorReturn ? 1 : LOSHeightCalculation.twHeightFromEntity(entity);
        DiagramUnitType unitType = sensorReturn ? DiagramUnitType.OTHER : DiagramUnitType.fromEntity(entity);
        boolean isAtAltitude = !sensorReturn && (entity.getAltitude() > 0) && unitType.isAltitudeUnit();
        String heightTerm = sensorReturn
              ? Messages.getString("Ruler.Height")
              : getHeightLabelSuffix(isAtAltitude, entity, unitType);
        String label = sensorReturn
              ? Messages.getString("BoardView1.sensorReturn") + " " + heightTerm
              : entity.getShortName() + " " + heightTerm;
        JSpinner heightSpinner = isFirstPoint ? height1 : height2;
        heightSpinner.setValue(twHeight);
        if (isFirstPoint) {
            entityName1 = sensorReturn ? Messages.getString("BoardView1.sensorReturn") : entity.getDisplayName();
            unitType1 = unitType;
            atAltitude1 = isAtAltitude;
            entityExpectedHeight1 = twHeight;
            heightLabel1.setText(label);
        } else {
            entityName2 = sensorReturn ? Messages.getString("BoardView1.sensorReturn") : entity.getDisplayName();
            unitType2 = unitType;
            atAltitude2 = isAtAltitude;
            entityExpectedHeight2 = twHeight;
            heightLabel2.setText(label);
        }
    }

    /**
     * Returns the currently selected entity for the given point, or null if "None" is selected.
     */
    private Entity getSelectedEntity(boolean isFirstPoint) {
        JComboBox<EntityItem> combo = isFirstPoint ? cboEntity1 : cboEntity2;
        EntityItem item = (EntityItem) combo.getSelectedItem();
        return (item != null) ? item.entity() : null;
    }

    /**
     * Returns true if the spinner value matches the entity's expected height, meaning the user hasn't manually
     * overridden it and we can use the entity-based LOS path.
     */
    private boolean isSpinnerAtEntityHeight(boolean isFirstPoint) {
        int expectedHeight = isFirstPoint ? entityExpectedHeight1 : entityExpectedHeight2;
        if (expectedHeight < 0) {
            return false;
        }
        int spinnerValue = (int) (isFirstPoint ? height1 : height2).getValue();
        return spinnerValue == expectedHeight;
    }

    /**
     * Returns the appropriate height label suffix based on TW terminology (TW p.43):
     * <ul>
     *   <li>"Altitude:" for airborne aerospace units (fixed values, independent of hex level)</li>
     *   <li>"Elevation:" for airborne non-aerospace units (VTOLs, WiGE - relative to hex level)</li>
     *   <li>"Height:" for ground units (levels above hex terrain)</li>
     * </ul>
     */
    private static String getHeightLabelSuffix(boolean isAtAltitude, Entity entity, DiagramUnitType unitType) {
        if (isAtAltitude) {
            return Messages.getString("Ruler.Altitude");
        }
        if (unitType.isElevationUnit() && entity.getElevation() > 0) {
            return Messages.getString("Ruler.Elevation");
        }
        return Messages.getString("Ruler.Height");
    }

    private void setText() {
        int h1 = (int) height1.getValue();
        int h2 = (int) height2.getValue();

        if (!game.getBoard().contains(start) || !game.getBoard().contains(end)) {
            return;
        }

        // Determine if we can use entity-based LOS (same as fire phase)
        boolean attackerIsFirst = flip;
        Entity attackerEntity = getSelectedEntity(attackerIsFirst);
        Entity targetEntity = getSelectedEntity(!attackerIsFirst);
        boolean spinnerMatch1 = isSpinnerAtEntityHeight(true);
        boolean spinnerMatch2 = isSpinnerAtEntityHeight(false);
        boolean useEntityPath = (attackerEntity != null) && (targetEntity != null)
              && spinnerMatch1 && spinnerMatch2;

        String toHit1;
        String toHit2;
        if (useEntityPath) {
            // Entity-based path: identical to fire phase LOS calculation
            toHit1 = LOSModifierCalculator.computeEntityBasedModifiers(game, attackerEntity, targetEntity);
            toHit2 = LOSModifierCalculator.computeEntityBasedModifiers(game, targetEntity, attackerEntity);
        } else {
            // Manual path: scenario testing with spinner overrides or no entities
            boolean isMek1 = unitType1.isMek();
            boolean isMek2 = unitType2.isMek();
            Coords attackerPos = flip ? start : end;
            Coords targetPos = flip ? end : start;
            int attackerHeight = flip ? h1 : h2;
            int targetHeight = flip ? h2 : h1;
            boolean attackerIsMek = flip ? isMek1 : isMek2;
            boolean targetIsMek = flip ? isMek2 : isMek1;
            boolean attackerIsAlt = flip ? atAltitude1 : atAltitude2;
            boolean targetIsAlt = flip ? atAltitude2 : atAltitude1;

            Player localPlayer = bv.getLocalPlayer();
            toHit1 = LOSModifierCalculator.computeFullModifiers(game, attackerPos, targetPos,
                  attackerHeight, targetHeight, attackerIsMek, targetIsMek,
                  attackerIsAlt, targetIsAlt, localPlayer);
            toHit2 = LOSModifierCalculator.computeFullModifiers(game, targetPos, attackerPos,
                  targetHeight, attackerHeight, targetIsMek, attackerIsMek,
                  targetIsAlt, attackerIsAlt, localPlayer);
        }

        tf_start.setText(start.toString());
        tf_end.setText(end.toString());
        rangeLabel.setText("<- " + distance + " ->");
        tf_los1.setText(toHit1);
        tf_los2.setText(toHit2);

        // When using entity-based path, compute the authoritative LOS result for the diagram
        Boolean entityLosBlocked = null;
        if (useEntityPath) {
            LosEffects entityLos = LosEffects.calculateLOS(game, attackerEntity, targetEntity);
            entityLosBlocked = !entityLos.canSee();
        }

        updateHeightInfo();
        updateDiagram(entityLosBlocked);
        if (compareExpanded) {
            updateCompareTable();
        }
    }

    /**
     * Updates the info labels below each height spinner showing ground level, absolute top elevation, and unit status.
     */
    private void updateHeightInfo() {
        if (start != null && game.getBoard().contains(start)) {
            int twHeight = (int) height1.getValue();
            heightInfo1.setText(buildHeightInfoText(start, twHeight, unitType1, atAltitude1));
            effectiveHeight1.setText(buildEffectiveHeightText(start, twHeight, atAltitude1));
        }
        if (end != null && game.getBoard().contains(end)) {
            int twHeight = (int) height2.getValue();
            heightInfo2.setText(buildHeightInfoText(end, twHeight, unitType2, atAltitude2));
            effectiveHeight2.setText(buildEffectiveHeightText(end, twHeight, atAltitude2));
        }
    }

    /**
     * Builds the "(Effective Height: X)" text shown next to the spinner. This is the absolute LOS height that matters
     * for line-of-sight calculations.
     */
    private String buildEffectiveHeightText(Coords coords, int twHeight, boolean isAtAltitude) {
        Hex hex = game.getBoard().getHex(coords);
        if (hex == null) {
            return "";
        }
        if (isAtAltitude) {
            return "(Eff. Alt. for LOS: " + twHeight + ")";
        }
        int effectiveHeight = hex.getLevel() + twHeight;
        return "(Eff. Height for LOS: " + effectiveHeight + ")";
    }

    private String buildHeightInfoText(Coords coords, int twHeight, DiagramUnitType unitType,
          boolean isAtAltitude) {
        Hex hex = game.getBoard().getHex(coords);
        if (hex == null) {
            return "";
        }

        int groundLevel = hex.getLevel();
        boolean isElevation = !isAtAltitude && unitType.isElevationUnit() && twHeight > 1;

        StringBuilder info = new StringBuilder();
        if (isAtAltitude) {
            // Altitude: fixed value independent of hex level (TW p.43)
            info.append("Effective Altitude for LOS: ").append(twHeight);
        } else if (isElevation) {
            // Elevation: airborne non-aero (VTOL/WiGE), relative to hex level (TW p.43)
            int effectiveHeight = twHeight + groundLevel;
            info.append("Hex: ").append(groundLevel);
            info.append(" + Elev: ").append(twHeight);
            info.append(" = Effective Height for LOS: ").append(effectiveHeight);
        } else {
            // Level/Height: ground units (TW p.43)
            int effectiveHeight = twHeight + groundLevel;
            info.append("Hex: ").append(groundLevel);
            info.append(" + Height: ").append(twHeight);
            info.append(" = Effective Height for LOS: ").append(effectiveHeight);
        }

        // Status flags (respect double-blind visibility - don't reveal hidden enemy Mek hull-down state)
        if (unitType.isMek() && LOSModifierCalculator.isMekHullDownAt(game, coords, bv.getLocalPlayer())) {
            info.append(" | Hull Down");
        }

        int absTop = LOSHeightCalculation.toAbsoluteHeight(twHeight, groundLevel, isAtAltitude);
        if (hex.containsTerrain(Terrains.WATER) && hex.depth() > 0) {
            if (absTop < groundLevel) {
                info.append(" | Underwater");
            } else if (absTop == groundLevel) {
                info.append(" | In Water");
            }
        }

        return info.toString();
    }

    /**
     * Updates the elevation diagram panel with current LOS data.
     *
     * @param entityLosBlocked if non-null, overrides the diagram's own LOS calculation with the entity-based result
     *                         (from the fire phase code path). Null means use the diagram's manual AttackInfo-based
     *                         calculation.
     */
    private void updateDiagram(Boolean entityLosBlocked) {
        if (!diagramExpanded || start == null || end == null) {
            return;
        }

        int h1 = (int) height1.getValue();
        int h2 = (int) height2.getValue();

        if (!game.getBoard().contains(start) || !game.getBoard().contains(end)) {
            return;
        }

        LosEffects.AttackInfo attackInfo;
        if (flip) {
            attackInfo = LOSModifierCalculator.buildAttackInfo(game, start, end, h1, h2,
                  unitType1.isMek(), unitType2.isMek(),
                  atAltitude1, atAltitude2);
        } else {
            attackInfo = LOSModifierCalculator.buildAttackInfo(game, end, start, h2, h1,
                  unitType2.isMek(), unitType1.isMek(),
                  atAltitude2, atAltitude1);
        }

        Coords attackerPos = flip ? start : end;
        Coords targetPos = flip ? end : start;
        boolean attackerHullDown = LOSModifierCalculator.isMekHullDownAt(game, attackerPos, bv.getLocalPlayer());
        boolean targetHullDown = LOSModifierCalculator.isMekHullDownAt(game, targetPos, bv.getLocalPlayer());

        String attackerName = flip ? entityName1 : entityName2;
        String targetName = flip ? entityName2 : entityName1;
        DiagramUnitType attackerType = flip ? unitType1 : unitType2;
        DiagramUnitType targetType = flip ? unitType2 : unitType1;
        boolean attackerIsAlt = flip ? atAltitude1 : atAltitude2;
        boolean targetIsAlt = flip ? atAltitude2 : atAltitude1;

        LOSDiagramData diagramData;
        if (entityLosBlocked != null) {
            // Use pre-computed entity-based LOS result (matches fire phase)
            diagramData = LOSDiagramDataBuilder.buildWithLosResult(game, attackInfo,
                  entityLosBlocked, attackerHullDown, targetHullDown,
                  attackerType, targetType, attackerIsAlt, targetIsAlt,
                  attackerName, targetName);
        } else {
            // Use manual AttackInfo-based LOS (scenario testing)
            diagramData = LOSDiagramDataBuilder.build(game, attackInfo,
                  attackerHullDown, targetHullDown, attackerType, targetType,
                  attackerIsAlt, targetIsAlt,
                  attackerName, targetName);
        }

        diagramPanel.setData(diagramData);
    }

    private void toggleDiagram() {
        diagramExpanded = !diagramExpanded;
        updateDiagramButtonText();
        diagramScrollPane.setVisible(diagramExpanded);
        GUIPreferences.getInstance().setRulerDiagramVisible(diagramExpanded);

        if (diagramExpanded && start != null && end != null) {
            setText();
        }

        revalidate();
        repaint();
    }

    private void updateDiagramButtonText() {
        butDiagram.setText(diagramExpanded
              ? Messages.getString("Ruler.hideDiagram")
              : Messages.getString("Ruler.showDiagram"));
    }

    private void toggleCompare() {
        compareExpanded = !compareExpanded;
        updateCompareButtonText();
        compareScrollPane.setVisible(compareExpanded);
        GUIPreferences.getInstance().setRulerCompareVisible(compareExpanded);

        if (compareExpanded && start != null && end != null) {
            updateCompareTable();
        }

        revalidate();
        repaint();
    }

    private void updateCompareButtonText() {
        butCompare.setText(compareExpanded
              ? Messages.getString("Ruler.hideCompare")
              : Messages.getString("Ruler.showCompare"));
    }

    /**
     * Computes LOS under all three rule modes and populates the comparison table. Highlights the row matching the
     * currently active mode.
     */
    private void updateCompareTable() {
        if (start == null || end == null) {
            return;
        }
        if (!game.getBoard().contains(start) || !game.getBoard().contains(end)) {
            return;
        }

        int h1 = (int) height1.getValue();
        int h2 = (int) height2.getValue();
        boolean isMek1 = unitType1.isMek();
        boolean isMek2 = unitType2.isMek();
        Coords attackerPos = flip ? start : end;
        Coords targetPos = flip ? end : start;
        int attackerHeight = flip ? h1 : h2;
        int targetHeight = flip ? h2 : h1;
        boolean attackerIsMek = flip ? isMek1 : isMek2;
        boolean targetIsMek = flip ? isMek2 : isMek1;
        boolean attackerIsAlt = flip ? atAltitude1 : atAltitude2;
        boolean targetIsAlt = flip ? atAltitude2 : atAltitude1;

        LOSModifierCalculator.LOSComparison comparison = LOSModifierCalculator.computeAllModes(
              game, attackerPos, targetPos,
              attackerHeight, targetHeight, attackerIsMek, targetIsMek,
              attackerIsAlt, targetIsAlt, bv.getLocalPlayer());

        compareTableModel.setRowCount(0);
        compareTableModel.addRow(new Object[] {
              Messages.getString("Ruler.modeStandard"),
              comparison.standardAttacker(),
              comparison.standardTarget()
        });
        compareTableModel.addRow(new Object[] {
              Messages.getString("Ruler.modeDiagrammed"),
              comparison.diagrammedAttacker(),
              comparison.diagrammedTarget()
        });
        compareTableModel.addRow(new Object[] {
              Messages.getString("Ruler.modeDeadZone"),
              comparison.deadZoneAttacker(),
              comparison.deadZoneTarget()
        });

        compareTable.repaint();
    }

    /**
     * Returns the row index (0-2) of the currently active LOS mode: 0 = Standard, 1 = Diagrammed, 2 = Dead Zone.
     */
    private int getActiveModeRow() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)) {
            return 1;
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)) {
            return 2;
        }
        return 0;
    }

    /**
     * Custom cell renderer for the comparison table. Highlights the active mode row with bold text, and rows that
     * differ from the active mode with a subtle background.
     */
    private class CompareTableRenderer extends DefaultTableCellRenderer {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component component = super.getTableCellRendererComponent(
                  table, value, isSelected, hasFocus, row, column);

            int activeModeRow = getActiveModeRow();

            if (row == activeModeRow) {
                // Active mode: bold text
                component.setFont(component.getFont().deriveFont(Font.BOLD));
                component.setBackground(table.getBackground());
            } else {
                component.setFont(component.getFont().deriveFont(Font.PLAIN));
                // Check if this row's results differ from active mode row
                boolean differs = false;
                if (table.getModel().getRowCount() > Math.max(row, activeModeRow)) {
                    for (int col = 1; col <= 2; col++) {
                        Object activeValue = table.getModel().getValueAt(activeModeRow, col);
                        Object rowValue = table.getModel().getValueAt(row, col);
                        if ((activeValue != null) && !activeValue.equals(rowValue)) {
                            differs = true;
                            break;
                        }
                    }
                }
                if (differs) {
                    // Subtle highlight for differing rows
                    Color background = table.getBackground();
                    boolean isDark = (background.getRed() + background.getGreen()
                          + background.getBlue()) / 3 < 128;
                    component.setBackground(isDark
                          ? background.brighter()
                          : new Color(255, 255, 220));
                } else {
                    component.setBackground(table.getBackground());
                }
            }
            component.setForeground(table.getForeground());
            return component;
        }
    }

    private void restoreSavedBounds() {
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        int savedWidth = guiPreferences.getRulerSizeWidth();
        int savedHeight = guiPreferences.getRulerSizeHeight();
        int savedX = guiPreferences.getRulerPosX();
        int savedY = guiPreferences.getRulerPosY();

        if ((savedWidth > 0) && (savedHeight > 0)) {
            setSize(savedWidth, savedHeight);
        } else {
            setSize(UIUtil.scaleForGUI(600, 350));
        }
        if ((savedX >= 0) && (savedY >= 0)) {
            setLocation(savedX, savedY);
        } else {
            setLocationRelativeTo(getOwner());
        }
        UIUtil.updateWindowBounds(this);
    }

    private void saveBounds() {
        if (!isVisible()) {
            return;
        }
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        guiPreferences.setRulerSizeWidth(getWidth());
        guiPreferences.setRulerSizeHeight(getHeight());
        guiPreferences.setRulerPosX(getX());
        guiPreferences.setRulerPosY(getY());
    }

    /**
     * Returns the ruler dialog title based on which optional LOS rules are active.
     */
    private static String getRulerTitle(Game game) {
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)) {
            return Messages.getString("Ruler.titleDiagrammedLOS");
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)) {
            return Messages.getString("Ruler.titleDeadZone");
        }
        return Messages.getString("Ruler.title");
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        // ALT+click triggers the ruler/LOS tool via BOARD_HEX_CLICKED.
        // CTRL+click is intercepted by BoardView.checkLOS() and arrives
        // via firstLOSHex()/secondLOSHex() events instead.
        if ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                addPoint(b.getCoords());
            }
        }

        bv.drawRuler(start, end, startColor, endColor);
    }

    @Override
    public void hexCursor(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void boardHexHighlighted(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void hexSelected(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void firstLOSHex(BoardViewEvent b) {
        addPoint(b.getCoords());
        bv.drawRuler(start, end, startColor, endColor);
    }

    @Override
    public void secondLOSHex(BoardViewEvent b) {
        addPoint(b.getCoords());
        bv.drawRuler(start, end, startColor, endColor);
    }

    void butFlip_actionPerformed() {
        flip = !flip;
        applyColorsToUI();

        setText();
        setVisible(true);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void butClose_actionPerformed() {
        clear();
        setVisible(false);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void heightSpinnerChanged() {
        if (start != null && end != null) {
            setText();
        }
    }


    /**
     * Handles entity combo box selection changes. Updates the height, entity name, and unit type from the selected
     * entity, then recalculates LOS.
     */
    private void entityComboChanged(JComboBox<EntityItem> combo, boolean isFirstPoint) {
        if (updatingCombo) {
            return;
        }
        EntityItem selected = (EntityItem) combo.getSelectedItem();
        if (selected == null) {
            return;
        }

        Entity entity = selected.entity();
        if (entity != null) {
            applyEntitySelection(entity, isFirstPoint);
        } else {
            // "None" selected - reset to manual defaults
            JSpinner heightSpinner = isFirstPoint ? height1 : height2;
            heightSpinner.setValue(1);
            if (isFirstPoint) {
                entityName1 = "";
                unitType1 = DiagramUnitType.OTHER;
                atAltitude1 = false;
                entityExpectedHeight1 = -1;
                heightLabel1.setText(Messages.getString("Ruler.Height"));
            } else {
                entityName2 = "";
                unitType2 = DiagramUnitType.OTHER;
                atAltitude2 = false;
                entityExpectedHeight2 = -1;
                heightLabel2.setText(Messages.getString("Ruler.Height"));
            }
        }

        if (start != null && end != null) {
            setText();
        }
    }

    /**
     * Populates an entity combo box with all entities at the given coordinates. Selects the tallest entity by default.
     * Returns the selected entity's data.
     *
     * @param coords the hex coordinates
     * @param combo  the combo box to populate
     *
     * @return the tallest entity found, or null if no entities present
     */
    private Entity populateEntityCombo(Coords coords, JComboBox<EntityItem> combo) {
        updatingCombo = true;
        try {
            DefaultComboBoxModel<EntityItem> model = new DefaultComboBoxModel<>();
            model.addElement(new EntityItem(null));

            List<Entity> entities = getVisibleEntitiesAt(coords);
            Entity tallestEntity = null;
            int tallestHeight = Integer.MIN_VALUE;
            int tallestIndex = 0;

            for (Entity entity : entities) {
                boolean sensorReturn = isSensorReturn(entity);
                model.addElement(new EntityItem(entity, sensorReturn));
                // For sensor returns, don't use real height for tallest comparison - we don't know it
                int twHeight = sensorReturn ? 1 : LOSHeightCalculation.twHeightFromEntity(entity);
                if (twHeight > tallestHeight) {
                    tallestHeight = twHeight;
                    tallestEntity = entity;
                    tallestIndex = model.getSize() - 1;
                }
            }

            combo.setModel(model);
            if (tallestEntity != null) {
                combo.setSelectedIndex(tallestIndex);
            }
            combo.setVisible(model.getSize() > 2);
            return tallestEntity;
        } finally {
            updatingCombo = false;
        }
    }

    /**
     * Returns the entities at the given hex that are visible OR detected by the local player. Excludes enemy units the
     * player has neither seen nor detected. Sensor-return-only entities ARE included so the player can check LOS/range
     * to them, but their identity and state should be hidden in the UI (see {@link #isSensorReturn(Entity)}).
     */
    private List<Entity> getVisibleEntitiesAt(Coords coords) {
        List<Entity> all = game.getEntitiesVector(coords);
        if (bv.getLocalPlayer() == null) {
            return all;
        }
        List<Entity> visible = new ArrayList<>();
        for (Entity entity : all) {
            if (!EntityVisibilityUtils.detectedOrHasVisual(bv.getLocalPlayer(), game, entity)) {
                // Player can't see or detect this entity at all
                continue;
            }
            visible.add(entity);
        }
        return visible;
    }

    /**
     * Returns true if the given entity is a sensor return only (detected but not visually identified). Sensor returns
     * should display with a generic "Sensor Return" label and have their state hidden.
     */
    private boolean isSensorReturn(Entity entity) {
        return (bv.getLocalPlayer() != null)
              && EntityVisibilityUtils.onlyDetectedBySensors(bv.getLocalPlayer(), entity);
    }

    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // ignored
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // ignored
    }

    /**
     * Combo box entry for an entity at a hex. When {@code sensorReturn} is true, the display hides
     * the entity's identity and state to avoid leaking double-blind information.
     */
    record EntityItem(Entity entity, boolean sensorReturn) {
        EntityItem(Entity entity) {
            this(entity, false);
        }

        @Override
        public String toString() {
            if (entity == null) {
                return Messages.getString("Ruler.noEntity");
            }
            if (sensorReturn) {
                return Messages.getString("BoardView1.sensorReturn");
            }
            int elevation = entity.getElevation();
            int effectiveElevation = entity.relHeight() + 1;
            return entity.getDisplayName() + " [Elev " + elevation + ", Effective " + effectiveElevation + "]";
        }
    }
}
