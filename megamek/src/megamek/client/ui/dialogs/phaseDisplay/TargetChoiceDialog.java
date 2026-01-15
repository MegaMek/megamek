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
import java.util.Optional;
import javax.swing.JFrame;
import javax.swing.JToggleButton;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.util.UIUtil;
import megamek.common.units.Entity;
import megamek.common.equipment.INarcPod;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;

/**
 * A modal dialog for choosing one or more Targetable objects. Can show stats in brief or in detail.
 */
public class TargetChoiceDialog extends AbstractChoiceDialog<Targetable> {
    final ClientGUI clientGUI;
    Entity firingEntity;

    /**
     * This creates a modal dialog to pick one or more Targetable objects.
     *
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false first, item chosen will close the
     *                      window
     * @param clientGUI     Needed to look up details of some targetables such as buildings
     * @param firingEntity  Needed to determine ToHit modifiers for targets
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
        String div = infoText(target) + " " + UnitToolTip.getTargetTipDetail(target, clientGUI.getClient());
        div = "<DIV WIDTH=" + UIUtil.scaleForGUI(500) + ">" + div + "</DIV>";
        button.setText(UnitToolTip.wrapWithHTML(div));
    }

    @Override
    protected void summaryLabel(JToggleButton button, Targetable target) {
        String txt = infoText(target) + "<BR>" + UnitToolTip.getTargetTipSummary(target, clientGUI.getClient());
        button.setText(UnitToolTip.wrapWithHTML(txt));
    }

    protected String infoText(Targetable target) {
        String result = "";

        if (firingEntity != null) {
            ToHitData thd;
            if (target instanceof INarcPod) {
                // must not call getPosition() on INarcPods! Check both arms if necessary
                thd = BrushOffAttackAction.toHit(clientGUI.getClient().getGame(),
                      firingEntity.getId(), target, BrushOffAttackAction.RIGHT);
                if (thd.getValue() == TargetRoll.IMPOSSIBLE) {
                    thd = BrushOffAttackAction.toHit(clientGUI.getClient().getGame(),
                          firingEntity.getId(), target, BrushOffAttackAction.LEFT);
                }
            } else {
                thd = WeaponAttackAction.toHit(clientGUI.getClient().getGame(), firingEntity.getId(),
                      Optional.empty(), Optional.empty(), Optional.empty(), target);
                thd.setLocation(target.getPosition());
                thd.setRange(firingEntity.getPosition().distance(target.getPosition()));
            }
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
     * show modal dialog to chose one Targetable from a list This creates a modal dialog to pick one or more entities.
     *
     * @param frame   parent @JFrame that owns this dialog
     * @param title   Resource key string only, plain text will result in NPE from Resources
     * @param message HTML or plain text message show at top of dialog
     * @param targets Targetables to chose from
     *
     * @return chosen entity or null if not chosen
     */
    public static @Nullable Targetable showSingleChoiceDialog(JFrame frame, String title, String message,
          @Nullable List<Targetable> targets,
          ClientGUI clientGUI, @Nullable Entity firingEntity) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame,
              title,
              message,
              targets,
              false,
              clientGUI,
              firingEntity);

        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getFirstChoice();
        }
        return null;
    }


    /**
     * show modal dialog to chose one Targetable from a list This creates a modal dialog to pick one or more entities.
     *
     * @param frame   parent @JFrame that owns this dialog
     * @param title   Resource key string only, plain text will result in NPE from Resources
     * @param message HTML or plain text message show at top of dialog
     * @param targets Targetables to chose from
     *
     * @return chosen Targetables or empty list if none chosen
     */
    public static @Nullable List<Targetable> showMultiChoiceDialog(JFrame frame, String title, String message,
          @Nullable List<Targetable> targets,
          ClientGUI clientGUI, Entity firingEntity) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame,
              title,
              message,
              targets,
              true,
              clientGUI,
              firingEntity);

        DialogResult result = dialog.showDialog();
        if (result == DialogResult.CONFIRMED) {
            return dialog.getChosen();
        }
        return null;
    }
}
