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

package megamek.client.ui.dialogs.phaseDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JToggleButton;

import megamek.client.ui.Messages;
import megamek.client.ui.enums.DialogResult;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentActivation;
import megamek.common.equipment.MiscMounted;
import megamek.common.units.Entity;

/**
 * Asks the player which of a unit's ECM suites to leave on. A unit may use only one ECM suite at a time, of any type
 * (TM p.213, CO p.200), so this opens as soon as a player puts a second suite into use, and every suite the player
 * does not pick is switched off.
 *
 * <p>The caller supplies the mode to show against each suite rather than the dialog reading it from the equipment,
 * because at the point the player is asked, the mode they just picked has not been applied yet - in the unit display
 * it is still pending, and in the lobby it lives in a dropdown until the customization is confirmed.</p>
 */
public class EcmSuiteChoiceDialog extends AbstractChoiceDialog<MiscMounted> {

    private final transient Entity entity;
    private final transient Map<MiscMounted, String> suiteModeNames;

    /**
     * Creates the modal dialog. Call {@link #showSingleChoiceDialog(JFrame, Entity, Map)} rather than this
     * constructor.
     *
     * @param frame          parent frame that owns this dialog
     * @param entity         the unit whose ECM suites are in conflict
     * @param suiteModeNames the suites to choose between, each mapped to the mode label to show against it
     * @param ecmSuites      the same suites as a list, in the order they are offered
     */
    protected EcmSuiteChoiceDialog(JFrame frame, Entity entity, Map<MiscMounted, String> suiteModeNames,
          List<MiscMounted> ecmSuites) {
        super(frame, "EcmSuiteChoiceDialog.title",
              Messages.getString("EcmSuiteChoiceDialog.message", entity.getShortName()),
              ecmSuites, false);
        this.entity = entity;
        this.suiteModeNames = suiteModeNames;
        // The suites can read alike down to the location, so one per row keeps the labels from being squeezed
        // side by side
        setColumns(1);
        // initialize must be called after all member variables set
        initialize();
        // There is nothing to show beyond the summary, so the details toggle would only ever be a dead control
        setUseDetailed(false);
    }

    @Override
    protected void detailLabel(JToggleButton button, MiscMounted target) {
        // The details toggle is hidden, so this is only ever reached through the summary label
        summaryLabel(button, target);
    }

    @Override
    protected void summaryLabel(JToggleButton button, MiscMounted target) {
        button.setText(Messages.getString("EcmSuiteChoiceDialog.suite",
              EquipmentActivation.ecmSuiteLabel(entity, target), suiteModeNames.get(target)));
    }

    /**
     * Shows the modal dialog and returns the single ECM suite the player wants to leave on.
     *
     * @param frame          parent frame that owns this dialog
     * @param entity         the unit whose ECM suites are in conflict
     * @param suiteModeNames the suites to choose between, each mapped to the mode label to show against it; use an
     *                       ordered map so that the suites are offered in mount order
     *
     * @return the suite to leave on, or {@code null} if the player cancelled without choosing
     */
    public static @Nullable MiscMounted showSingleChoiceDialog(JFrame frame, Entity entity,
          Map<MiscMounted, String> suiteModeNames) {
        EcmSuiteChoiceDialog dialog = new EcmSuiteChoiceDialog(frame, entity, suiteModeNames,
              new ArrayList<>(suiteModeNames.keySet()));
        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getFirstChoice();
        }
        return null;
    }
}
