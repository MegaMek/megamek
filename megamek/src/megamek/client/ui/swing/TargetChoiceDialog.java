package megamek.client.ui.swing;

import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Targetable;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * A modal dialog for choosing one or more Targetable objects. Can show info in brief or detail
 */
public class TargetChoiceDialog extends AbstractSelectionDialog<Targetable> {
    protected TargetChoiceDialog(JFrame frame, String message, String title,
                                 @Nullable List<Targetable> targets, boolean isMultiSelect) {
        super(frame, message, title, targets, isMultiSelect);
    }

    @Override
    protected void detailLabel(JToggleButton button, Targetable target)
    {
        button.setText("<html>" + UnitToolTip.getEntityTipBrief((Entity) target, null) + "</html>");
    }

    @Override
    protected void simpleLabel(JToggleButton button, Targetable target) {
        String mods = "";
        if (target instanceof Entity) {
            //not sure if check is needed, safe to assume all targets are entity?
            Game game = ((Entity)target).getGame();
            int tmm = Compute.getTargetMovementModifier(game, target.getId()).getValue();
            mods = ' '+(tmm < 0 ? "-" : "+")+tmm +' ';
        }
        button.setText("<html><b>" + target.getDisplayName() + "</b>"+mods+"</html>");
    }

    public static @Nullable Targetable showSingleChoiceDialog(JFrame frame, String message, String title, @Nullable List<Targetable> targets) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, message, title, targets, false);

        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getFirstChoice();
        }
        return null;
    }

    public static @Nullable List<Targetable> showMultiChoiceDialog(JFrame frame, String message, String title, @Nullable List<Targetable> targets) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, message, title, targets, true);

        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getChoosen();
        }
        return null;
    }
}
