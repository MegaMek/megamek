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
import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * A modal dialog for choosing one or more Targetable objects. Can show stats
 * in brief or in detail.
 */
public class TargetChoiceDialog extends AbstractChoiceDialog<Targetable> {
    final ClientGUI clientGUI;
    Entity firingEntity;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

     /**
     * This creates a modal dialog to pick one or more Targetable objects.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false first,
     *                      item chosen will close the window
     * @param clientGUI     Needed to look up details of some targetables such as buildings
     */
    protected TargetChoiceDialog(JFrame frame, String title, String message,
                                 @Nullable List<Targetable> targets, boolean isMultiSelect,
                                 ClientGUI clientGUI) {
        super(frame, title, message, targets, isMultiSelect);
        this.clientGUI = clientGUI;
        // initialize must be called after all member variables set
        initialize();
    }

    /**
     * This creates a modal dialog to pick one or more Targetable objects.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false first,
     *                      item chosen will close the window
     * @param clientGUI     Needed to look up details of some targetables such as buildings
     * @param firingEntity Needed to determine ToHit modifiers for targets
     */
    protected TargetChoiceDialog(JFrame frame, String title, String message,
                                 @Nullable List<Targetable> targets, boolean isMultiSelect,
                                 ClientGUI clientGUI, @Nullable Entity firingEntity) {
        super(frame, title, message, targets, isMultiSelect);
        this.clientGUI = clientGUI;
        this.firingEntity = firingEntity;
        // initialize must be called after all member variables set
        initialize();
    }

    @Override
    protected void detailLabel(JToggleButton button, Targetable target) {
        String div = "<DIV WIDTH=" + UIUtil.scaleForGUI(500) + ">" +  infoText(target) + " " + UnitToolTip.getTargetTipDetail(target,
                clientGUI.getClient()) + "</DIV>";
        button.setText("<html>" + div + "</html>");
    }

    @Override
    protected void summaryLabel(JToggleButton button, Targetable target) {
        button.setText("<html>" + infoText(target) + "<BR>" + UnitToolTip.getTargetTipSummary(target,
                clientGUI.getClient()) + "</html>");
    }

    protected String infoText(Targetable target) {
        String result = "";

        if (firingEntity != null) {
            ToHitData thd = WeaponAttackAction.toHit(clientGUI.getClient().getGame(), firingEntity.getId(), target);
            thd.setLocation(target.getPosition());
            thd.setRange(firingEntity.getPosition().distance(target.getPosition()));
            if (thd.needsRoll()) {
                int mod = thd.getValue();
                result += "<br>Target To Hit Mod: <b>" + (mod < 0 ? "" : "+") + mod + "</b>";
            } else {
                result += "<br><b>" + thd.getValueAsString() + " To Hit</b>: " + thd.getDesc();

            }
        }

        return result;
    }

    /**
     * show modal dialog to chose one Targetable from a list
     * This creates a modal dialog to pick one or more entities.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       Targetables to chose from
     * @return chosen entity or null if not chosen
     */
    public static @Nullable Targetable showSingleChoiceDialog(JFrame frame, String title, String message,
                                                              @Nullable List<Targetable> targets,
                                                              ClientGUI clientGUI, @Nullable Entity firingEntity) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, title, message, targets, false, clientGUI, firingEntity);

        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getFirstChoice();
        }
        return null;
    }


    /**
     * show modal dialog to chose one Targetable from a list
     * This creates a modal dialog to pick one or more entities.
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       Targetables to chose from
     * @return chosen Targetables or empty list if none chosen
     */
    public static @Nullable List<Targetable> showMultiChoiceDialog(JFrame frame, String title, String message,
                                                                   @Nullable List<Targetable> targets,
                                                                   ClientGUI clientGUI, Entity firingEntity) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, title, message, targets, true, clientGUI, firingEntity);

        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getChosen();
        }
        return null;
    }
}
