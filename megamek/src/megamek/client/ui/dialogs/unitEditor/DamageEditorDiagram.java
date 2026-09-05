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
package megamek.client.ui.dialogs.unitEditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.io.Serial;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.ArmorPanel;
import megamek.client.ui.widget.picmap.LocationSelectListener;
import megamek.common.annotations.Nullable;
import megamek.common.units.Aero;
import megamek.common.units.Entity;

/**
 * The armor-diagram half of the unit damage editor: the unit's clickable armor diagram beside a panel of the chosen
 * location's controls. It keeps the diagram and the value coloring in step with the editor's spinners, so the editor
 * always shows the damage that pressing Okay would apply, and clicking a location on the diagram shows its panel.
 */
public class DamageEditorDiagram extends JSplitPane implements LocationSelectListener {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Names the divider for the stored divider location. */
    private static final String SPLIT_PANE_NAME = "unitEditorSplitPane";

    /** How much the armor diagram is enlarged even when there is no panel height to fill. */
    private static final double MIN_PAPERDOLL_SCALE = 1.6;

    /** How far the armor diagram may be enlarged, past which it turns blocky. */
    private static final double MAX_PAPERDOLL_SCALE = 2.5;

    /** How much of the screen height the enlarged armor diagram may take. */
    private static final double MAX_SCREEN_FRACTION = 0.7;

    private final Entity entity;
    private final UnitDamageControls controls;

    /** The unit's armor diagram, the same one the unit display uses. Clicking a location shows its panel. */
    private final ArmorPanel paperdoll;
    /** Holds the location panels, one shown at a time. */
    private final JPanel panCards = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    /** Chooses which location panel is shown; kept in step with the armor diagram. */
    private final JComboBox<LocationChoice> comboLocation = new JComboBox<>();

    public DamageEditorDiagram(Entity entity, UnitDamageControls controls) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.entity = entity;
        this.controls = controls;

        panCards.setLayout(cardLayout);
        for (int location = 0; location < controls.locationPanels.length; location++) {
            if (controls.locationPanels[location] != null) {
                panCards.add(controls.locationPanels[location], cardName(location));
                comboLocation.addItem(new LocationChoice(location, entity.getLocationName(location)));
            }
        }
        comboLocation.addActionListener(event -> {
            if (comboLocation.getSelectedItem() instanceof LocationChoice choice) {
                cardLayout.show(panCards, cardName(choice.location()));
            }
        });

        // The diagram sizes itself to its drawn content, so it needs no preferred size of its own.
        paperdoll = new ArmorPanel(entity.getGame(), this);
        paperdoll.setToolTipText(Messages.getString("UnitEditorDialog.paperdoll.tooltip"));

        JPanel panChooser = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panChooser.add(new JLabel(Messages.getString("UnitEditorDialog.location")));
        panChooser.add(comboLocation);

        // The general panel is shown under the chosen location rather than as another location to choose. There is
        // no general part of a unit to click on the diagram, so a panel that had to be chosen there was a panel
        // nobody would find, and what is in it - the unit's condition, its heat, its systems - is always relevant.
        JPanel panPanels = new JPanel();
        panPanels.setLayout(new BoxLayout(panPanels, BoxLayout.PAGE_AXIS));
        panCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        panPanels.add(panCards);
        if (controls.panGeneral != null) {
            controls.panGeneral.setAlignmentX(Component.LEFT_ALIGNMENT);
            panPanels.add(controls.panGeneral);
        }

        JPanel panRight = new JPanel(new BorderLayout());
        panRight.add(panChooser, BorderLayout.PAGE_START);
        panRight.add(new JScrollPane(panPanels), BorderLayout.CENTER);

        // The user can drag the divider to trade diagram size against panel size; the position is remembered.
        setLeftComponent(new JScrollPane(paperdoll));
        setRightComponent(panRight);
        setName(SPLIT_PANE_NAME);
        setResizeWeight(0.0);
        setOneTouchExpandable(true);

        wireDamageColoring();
    }

    /**
     * Enlarges the armor diagram. It is drawn at a fixed size that is small on a modern screen, and smaller still
     * beside a location panel full of equipment, which leaves the editor with a band of empty space. So it is grown
     * to the height of the tallest location panel, but never less than {@link #MIN_PAPERDOLL_SCALE}, never so far
     * that it turns blocky, and never past the screen.
     */
    public void enlargeToFillDialog() {
        int drawnHeight = paperdoll.getPreferredSize().height;
        if (drawnHeight <= 0) {
            return;
        }
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        double panelScale = (double) panCards.getPreferredSize().height / drawnHeight;
        double screenScale = (screenHeight * MAX_SCREEN_FRACTION) / drawnHeight;
        double scale = Math.max(panelScale, MIN_PAPERDOLL_SCALE);
        scale = Math.min(scale, Math.min(MAX_PAPERDOLL_SCALE, screenScale));
        if (scale > 1.0) {
            paperdoll.setDisplayScale(scale);
        }
    }

    /** Redraws the diagram and recolors the values to match the spinners; call it once the editor has been packed. */
    public void refresh() {
        refreshDamageDisplay();
    }

    /** The diagram is here to pick locations with, so a single click is enough to select one. */
    @Override
    public boolean selectsOnSingleClick() {
        return true;
    }

    /** Shows the panel of the location the user picked in the armor diagram. */
    @Override
    public void locationSelected(int location) {
        if ((location >= 0) && (location < controls.locationPanels.length) && (controls.locationPanels[location] != null)) {
            comboLocation.setSelectedItem(new LocationChoice(location, entity.getLocationName(location)));
        }
    }

    /**
     * Keeps the armor diagram and the value coloring in step with the spinners, so the editor always shows the
     * damage that pressing Okay would apply.
     */
    private void wireDamageColoring() {
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                controls.spnArmor[location].addChangeListener(event -> refreshDamageDisplay());
            }
            if (null != controls.spnRear[location]) {
                controls.spnRear[location].addChangeListener(event -> refreshDamageDisplay());
            }
            if (null != controls.spnInternal[location]) {
                controls.spnInternal[location].addChangeListener(event -> refreshDamageDisplay());
            }
        }
        // the diagram carries a heat scale, so it follows the heat control too
        if (null != controls.spnHeat) {
            controls.spnHeat.addChangeListener(event -> refreshDamageDisplay());
        }
        // a location with a critical hit is striped on the diagram, so it follows the crit controls as well
        controls.critsByLocation.values()
              .forEach(crits -> crits.forEach(crit -> crit.addHitsChangedListener(this::refreshDamageDisplay)));
        refreshDamageDisplay();
    }

    private void refreshDamageDisplay() {
        refreshDamageColoring();
        refreshPaperdoll();
    }

    private void refreshDamageColoring() {
        for (int location = 0; location < entity.locations(); location++) {
            Color worstColor = colorSpinner(controls.spnArmor[location], entity.getOArmor(location, false), null);
            worstColor = colorSpinner(controls.spnRear[location], entity.getOArmor(location, true), worstColor);
            if (!(entity instanceof Aero)) {
                worstColor = colorSpinner(controls.spnInternal[location], entity.getOInternal(location), worstColor);
            }
            if ((null != controls.locationLabels) && (null != controls.locationLabels[location]) && (null != worstColor)) {
                controls.locationLabels[location].setForeground(worstColor);
            }
        }
        if ((entity instanceof Aero aero) && (null != controls.structuralIntegrityLabel) && (null != controls.spnInternal[0])) {
            Color siColor = colorSpinner(controls.spnInternal[0], aero.getOSI(), null);
            if (null != siColor) {
                controls.structuralIntegrityLabel.setForeground(siColor);
            }
        }
    }

    /**
     * Colors one spinner's text by how damaged its value is and returns the more severe of that color and the given
     * one, so callers can color the location label by its worst value.
     */
    private @Nullable Color colorSpinner(@Nullable JSpinner spinner, int originalValue, @Nullable Color worstSoFar) {
        if ((null == spinner) || (originalValue <= 0)) {
            return worstSoFar;
        }
        int currentValue = (Integer) spinner.getValue();
        Color color = damageColor(currentValue, originalValue);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setForeground(color);
        }
        return moreSevere(color, worstSoFar);
    }

    private Color damageColor(int currentValue, int originalValue) {
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        if (currentValue <= 0) {
            return guiPreferences.getUnitTooltipArmorMiniColorDamaged();
        } else if (currentValue < originalValue) {
            return guiPreferences.getUnitTooltipArmorMiniColorPartialDamage();
        }
        return guiPreferences.getUnitTooltipArmorMiniColorIntact();
    }

    private @Nullable Color moreSevere(@Nullable Color first, @Nullable Color second) {
        if (null == second) {
            return first;
        }
        if (null == first) {
            return second;
        }
        GUIPreferences guiPreferences = GUIPreferences.getInstance();
        Color damaged = guiPreferences.getUnitTooltipArmorMiniColorDamaged();
        Color partial = guiPreferences.getUnitTooltipArmorMiniColorPartialDamage();
        if (damaged.equals(first) || damaged.equals(second)) {
            return damaged;
        }
        if (partial.equals(first) || partial.equals(second)) {
            return partial;
        }
        return first;
    }

    /**
     * Redraws the armor diagram to show the values currently in the spinners, rather than the unit's actual damage.
     * <p>
     * The diagram renders straight off the unit, but the editor's edits are pending until Okay is pressed, so the
     * pending values are written into the unit, the diagram is redrawn from them, and the unit is put back as it
     * was. Nothing observes the unit in between: armor and structure are plain fields with no listeners, and this
     * runs on the event dispatch thread. Critical hits are not drawn on the diagram, so they need no preview.
     * </p>
     */
    private void refreshPaperdoll() {
        // The diagram builds its map sets when it is added to a displayable window, so there is nothing to draw
        // into before the editor is packed. The editor draws it once that has happened.
        if (!paperdoll.isDisplayable()) {
            return;
        }
        int[] actualArmor = new int[entity.locations()];
        int[] actualRear = new int[entity.locations()];
        int[] actualInternal = new int[entity.locations()];
        for (int location = 0; location < entity.locations(); location++) {
            actualArmor[location] = entity.getArmor(location, false);
            actualRear[location] = entity.getArmor(location, true);
            actualInternal[location] = entity.getInternal(location);
        }
        int actualStructuralIntegrity = (entity instanceof Aero aero) ? aero.getSI() : 0;
        int actualHeat = entity.heat;

        try {
            applyPendingValuesForDisplay();
            // the crits are not on the unit yet, so the diagram is told which locations to stripe
            paperdoll.setCriticalLocations(controls.locationsWithCrits());
            paperdoll.displayMek(entity);
        } finally {
            for (int location = 0; location < entity.locations(); location++) {
                entity.setArmor(actualArmor[location], location, false);
                entity.setArmor(actualRear[location], location, true);
                entity.setInternal(actualInternal[location], location);
            }
            if (entity instanceof Aero aero) {
                aero.setSI(actualStructuralIntegrity);
            }
            entity.heat = actualHeat;
        }
    }

    /** Writes the spinner values into the unit so the armor diagram can be drawn from them. */
    private void applyPendingValuesForDisplay() {
        for (int location = 0; location < entity.locations(); location++) {
            if (null != controls.spnArmor[location]) {
                entity.setArmor((Integer) controls.spnArmor[location].getValue(), location, false);
            }
            if (null != controls.spnRear[location]) {
                entity.setArmor((Integer) controls.spnRear[location].getValue(), location, true);
            }
            if ((null != controls.spnInternal[location]) && !(entity instanceof Aero)) {
                entity.setInternal((Integer) controls.spnInternal[location].getValue(), location);
            }
        }
        if ((entity instanceof Aero aero) && (null != controls.spnInternal[0])) {
            aero.setSI((Integer) controls.spnInternal[0].getValue());
        }
        if (null != controls.spnHeat) {
            entity.heat = (Integer) controls.spnHeat.getValue();
        }
    }

    private static String cardName(int location) {
        return "location-" + location;
    }

    /** An entry of the location chooser. Equality is by location so the chooser can be set by location alone. */
    private record LocationChoice(int location, String name) {
        @Override
        public boolean equals(Object other) {
            return (other instanceof LocationChoice choice) && (choice.location == location);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(location);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
