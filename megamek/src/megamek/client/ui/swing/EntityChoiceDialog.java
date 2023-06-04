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

import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * A modal dialog for choosing one or more Entities. Can show stats
 * in brief or in detail.
 */
public class EntityChoiceDialog extends AbstractChoiceDialog<Entity> {
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /**
     * This creates a modal dialog to pick one or more entities.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false first,
     *                      item chosen will close the window
     */
    protected EntityChoiceDialog(JFrame frame, String title, String message,
                                 @Nullable List<Entity> targets, boolean isMultiSelect) {
        super(frame, title, message, targets, isMultiSelect);
        // initialize must be called after all member variables set
        initialize();
    }

    @Override
    protected void detailLabel(JToggleButton button, Entity target) {
        String div = "<DIV WIDTH=" + UIUtil.scaleForGUI(500) + ">" + UnitToolTip.getEntityTipVitals(target, null) + "</DIV>";
        button.setText("<html>" + div + "</html>");
    }

    @Override
    protected void summaryLabel(JToggleButton button, Entity target) {
        button.setText("<html><b>" + target.getDisplayName() + "</b></html>");
    }

    /**
     * show modal dialog to chose one entity from a list
     * This creates a modal dialog to pick one or more entities.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       Entities to chose from
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
     * show modal dialog to chose one or more entities from a list
     * This creates a modal dialog to pick one or more entities.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       Entities to chose from
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
