/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.client.ui.swing;

import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * A modal dialog for choosing one or more Entities. Can show stats
 *  in brief or in detail.
 */
public class EntityChoiceDialog extends AbstractChoiceDialog<Entity> {

    protected EntityChoiceDialog(JFrame frame, String message, String title,
                                 @Nullable List<Entity> targets, boolean isMultiSelect) {
        super(frame, message, title, targets, isMultiSelect);
    }

    @Override
    protected void detailLabel(JToggleButton button, Entity target) {
        button.setText("<html>" + UnitToolTip.getEntityTipVitals(target, null) + "</html>");
    }

    @Override
    protected void summaryLabel(JToggleButton button, Entity target) {
        button.setText("<html><b>" + target.getDisplayName() + "</b></html>");
    }

    /**
     * show modal dialog to return one or null @Entity from chosen from the target list
     * @param targets list of Entity that can be selected from
     * @return list of chosen Entity, will be null if none chosen
     */
    public static @Nullable Entity showSingleChoiceDialog(JFrame frame, String message, String title, @Nullable List<Entity> targets) {
        EntityChoiceDialog dialog = new EntityChoiceDialog(frame, message, title, targets, false);
        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getFirstChoice();
        }
        return null;
    }

    /**
     * show modal dialog to return zero or more @Entity from chosen from the target list
     * @param targets list of Entity that can be selected from
     * @return list of chosen Entity, will be null if none chosen
     */
    public static @Nullable List<Entity> showMultiChoiceDialog(JFrame frame, String message, String title, @Nullable List<Entity> targets) {
        EntityChoiceDialog dialog = new EntityChoiceDialog(frame, message, title, targets, true);

        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getChosen();
        }
        return null;
    }
}
