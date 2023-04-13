package megamek.client.ui.swing;

import megamek.client.ui.swing.tooltip.HexTooltip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

/**
 * A modal dialog for choosing one or more Targetable objects. Can show info in brief or detail
 */
public class TargetChoiceDialog extends AbstractChoiceDialog<Targetable> {
    ClientGUI clientGUI;
    Entity firingEntity;


    protected TargetChoiceDialog(JFrame frame, String message, String title,
                                 @Nullable List<Targetable> targets, boolean isMultiSelect,
                                 ClientGUI clientGUI) {
        super(frame, message, title, targets, isMultiSelect);
        this.clientGUI = clientGUI;
    }

    protected TargetChoiceDialog(JFrame frame, String message, String title,
                                 @Nullable List<Targetable> targets, boolean isMultiSelect,
                                 ClientGUI clientGUI, @Nullable Entity firingEntity) {
        this(frame, message, title, targets, isMultiSelect, clientGUI);
        this.firingEntity = firingEntity;
    }

    @Override
    protected void detailLabel(JToggleButton button, Targetable target) {
        if (target instanceof Entity) {
            button.setText("<html>" + infoText(target)
                    + UnitToolTip.getEntityTipVitals((Entity) target, null)
                    + "</html>");
        } else if (target instanceof BuildingTarget) {
            button.setText("<html>" + infoText(target)
                    + HexTooltip.getBuildingTargetTip((BuildingTarget) target, clientGUI.getClient().getBoard())
                    + "</html>");
        } else if (target instanceof Hex) {
            button.setText("<html>" + infoText(target)
                    + HexTooltip.getHexTip((Hex) target, clientGUI.getClient(), clientGUI)
                    + "</html>");
        } else {
            summaryLabel(button, target);
        }
    }

    @Override
    protected void summaryLabel(JToggleButton button, Targetable target) {
        String result = infoText(target);
        if (target instanceof Entity) {
            result += "<br>" + UnitToolTip.getOneLineSummary((Entity) target)  ;
        } else if (target instanceof BuildingTarget) {
            result += "<br>" + HexTooltip.getOneLineSummary((BuildingTarget) target, clientGUI.getClient().getBoard())  ;
        }
        button.setText("<html>"+ result +"</html>");
    }

    protected String infoText(Targetable target)
    {
        String result =  "<b>" + target.getDisplayName() + "</b>";

        if (firingEntity != null) {
            ToHitData thd = WeaponAttackAction.toHit(clientGUI.getClient().getGame(), firingEntity.getId(), target);
            thd.setLocation(target.getPosition());
            thd.setRange(firingEntity.getPosition().distance(target.getPosition()));
            if (thd.needsRoll())
            {
                int mod = thd.getValue();
                result += "<br>Target To Hit Mod: <b>"+(mod < 0 ? "" : "+") + mod + "</b>";
            } else {
                result += "<br><b>"+thd.getValueAsString()+" To Hit</b>: "+thd.getDesc();

            }
        }

        return result;
    }

    /**
     * show modal dialog to return one or null @Targetable from chosen from the target list
     * @param targets list of Targetable that can be selected from
     * @return list of chosen Targetable, will be null if none chosen
     */
    public static @Nullable Targetable showSingleChoiceDialog(JFrame frame, String message, String title,
                                                              @Nullable List<Targetable> targets,
                                                              ClientGUI clientGUI, @Nullable Entity firingEntity) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, message, title, targets, false, clientGUI, firingEntity);

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
    public static @Nullable List<Targetable> showMultiChoiceDialog(JFrame frame, String message, String title,
                                                                   @Nullable List<Targetable> targets,
                                                                   ClientGUI clientGUI, Entity firingEntity) {
        TargetChoiceDialog dialog = new TargetChoiceDialog(frame, message, title, targets, true, clientGUI, firingEntity);

        boolean userOkay = dialog.showDialog();
        if (userOkay) {
            return dialog.getChosen();
        }
        return null;
    }
}
