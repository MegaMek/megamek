package megamek.client.ui.swing;

import megamek.client.ui.swing.tooltip.HexTooltip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.*;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * A modal dialog for choosing one or more Targetable objects. Can show info in brief or detail
 */
public class TargetChoiceDialog extends AbstractChoiceDialog<Targetable> {
    ClientGUI clientGUI;

    protected TargetChoiceDialog(JFrame frame, String message, String title,
                                 @Nullable List<Targetable> targets, boolean isMultiSelect,
                                 ClientGUI clientGUI) {
        super(frame, message, title, targets, isMultiSelect);
        this.clientGUI = clientGUI;
    }

    @Override
    protected void detailLabel(JToggleButton button, Targetable target) {
        if (target instanceof Entity) {
            button.setText("<html>" + UnitToolTip.getEntityTipBrief((Entity) target, null) + "</html>");
        } else if (target instanceof BuildingTarget) {
            button.setText("<html>" + HexTooltip.getBuildingTargetTip((BuildingTarget) target, clientGUI.getClient().getBoard()) + "</html>");
        } else if (target instanceof Hex) {
            button.setText("<html>" + HexTooltip.getHexTip((Hex) target, clientGUI.getClient(), clientGUI) + "</html>");
        } else {
            summaryLabel(button, target);
        }
    }

    @Override
    protected void summaryLabel(JToggleButton button, Targetable target) {
        String mods = "";
        if (target instanceof Entity) {
            //not sure if check is needed, safe to assume all targets are entity?
            Game game = ((Entity)target).getGame();
            int tmm = Compute.getTargetMovementModifier(game, target.getId()).getValue();
            mods = ' '+(tmm < 0 ? "-" : "+")+tmm +' ';
        }
        button.setText("<html><b>" + target.getDisplayName() + "</b>"+mods+"</html>");
    }

    /**
     * show modal dialog to return one or null @Targetable from chosen from the target list
     * @param targets list of Targetable that can be selected from
     * @return list of chosen Targetable, will be null if none chosen
     */
    public static @Nullable Targetable showSingleChoiceDialog(JFrame frame, String message, String title,
                                                              @Nullable List<Targetable> targets, ClientGUI clientGUI) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, message, title, targets, false, clientGUI);

        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getFirstChoice();
        }
        return null;
    }

    /**
     * show modal dialog to return zero or more @Targetable from chosen from the target list
     * @param targets list of Targetable that can be selected from
     * @return list of chosen Targetable, can be empty
     */
    public static @Nullable List<Targetable> showMultiChoiceDialog(JFrame frame, String message, String title, @Nullable List<Targetable> targets, ClientGUI clientGUI) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, message, title, targets, true, clientGUI);

        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getChosen();
        }
        return null;
    }
}
