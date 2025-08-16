/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.List;
import javax.swing.JFrame;
import javax.swing.JToggleButton;

import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.util.UIUtil;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

/**
 * A modal dialog for choosing one or more Entities. Can show stats in brief or in detail.
 */
public class EntityChoiceDialog extends AbstractChoiceDialog<Entity> {

    /**
     * This creates a modal dialog to pick one or more entities.
     *
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false first, item chosen will close the
     *                      window
     */
    protected EntityChoiceDialog(JFrame frame, String title, String message,
          @Nullable List<Entity> targets, boolean isMultiSelect) {
        super(frame, title, message, targets, isMultiSelect);
        // initialize must be called after all member variables set
        initialize();
    }

    @Override
    protected void detailLabel(JToggleButton button, Entity target) {
        String div = "<DIV WIDTH="
              + UIUtil.scaleForGUI(500)
              + ">"
              + UnitToolTip.getEntityTipAsTarget(target, null)
              + "</DIV>";
        button.setText(UnitToolTip.wrapWithHTML(div));
    }

    @Override
    protected void summaryLabel(JToggleButton button, Entity target) {
        String txt = UnitToolTip.getTargetTipSummaryEntity(target, null);
        button.setText(UnitToolTip.wrapWithHTML(txt));
    }

    /**
     * show modal dialog to chose one entity from a list This creates a modal dialog to pick one or more entities.
     *
     * @param frame   parent @JFrame that owns this dialog
     * @param title   Resource key string only, plain text will result in NPE from Resources
     * @param message HTML or plain text message show at top of dialog
     * @param targets Entities to chose from
     *
     * @return chosen entity or null if not chosen
     */
    public static @Nullable Entity showSingleChoiceDialog(JFrame frame, String title, String message,
          @Nullable List<Entity> targets) {
        EntityChoiceDialog dialog = new EntityChoiceDialog(frame, title, message, targets, false);
        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getFirstChoice();
        }
        return null;
    }

    /**
     * show modal dialog to chose one or more entities from a list This creates a modal dialog to pick one or more
     * entities.
     *
     * @param frame   parent @JFrame that owns this dialog
     * @param title   Resource key string only, plain text will result in NPE from Resources
     * @param message HTML or plain text message show at top of dialog
     * @param targets Entities to chose from
     *
     * @return chosen entities or empty list if none chosen
     */
    public static @Nullable List<Entity> showMultiChoiceDialog(JFrame frame, String title, String message,
          @Nullable List<Entity> targets) {
        EntityChoiceDialog dialog = new EntityChoiceDialog(frame, title, message, targets, true);
        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getChosen();
        }
        return null;
    }
}
