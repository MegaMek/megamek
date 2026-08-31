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

import java.awt.Color;
import java.io.Serial;
import javax.swing.JComponent;
import javax.swing.JSpinner;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.AbstractLocationDiagram;
import megamek.common.annotations.Nullable;
import megamek.common.units.Aero;
import megamek.common.units.Entity;

/**
 * The GM damage editor's view of a unit: the unit diagram on the left, and the armor, structure and critical-hit
 * controls of whichever location is chosen on the right.
 * <p>
 * The controls are built by {@link UnitDamagePanelBuilder} and held in {@link UnitDamageControls}; this class lays
 * them out as the location cards of an {@link AbstractLocationDiagram} and keeps the diagram showing the edits as
 * they are made, before they are applied.
 */
public class DamageEditorDiagram extends AbstractLocationDiagram {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String SPLIT_PANE_NAME = "unitEditorSplitPane";

    private final UnitDamageControls controls;

    public DamageEditorDiagram(Entity entity, UnitDamageControls controls) {
        super(entity, entity.getGame());
        this.controls = controls;
        getPaperdoll().setToolTipText(Messages.getString("UnitEditorDialog.paperdoll.tooltip"));
        setName(SPLIT_PANE_NAME);
        buildCards();
        wireDamageColoring();
    }

    @Override
    protected @Nullable JComponent createLocationCard(int location) {
        return (location < controls.locationPanels.length) ? controls.locationPanels[location] : null;
    }

    @Override
    protected @Nullable JComponent createGeneralCard() {
        return controls.panGeneral;
    }

    /**
     * Redraws the diagram and recolours the controls from the values currently in the editor.
     */
    public void refresh() {
        refreshDamageDisplay();
    }

    private void wireDamageColoring() {
        Entity entity = getEntity();
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
        refreshDiagram();
    }

    private void refreshDamageColoring() {
        Entity entity = getEntity();
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
    @Override
    public void refreshDiagram() {
        // The diagram builds its map sets when it is added to a displayable window, so there is nothing to draw
        // into before the editor is packed. The editor draws it once that has happened.
        if (!getPaperdoll().isDisplayable()) {
            return;
        }
        Entity entity = getEntity();
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
            getPaperdoll().setCriticalLocations(controls.locationsWithCrits());
            getPaperdoll().displayMek(entity);
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
        Entity entity = getEntity();
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
}
