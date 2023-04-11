package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Targetable;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class TargetChoiceDialog extends ClientDialog {
    List<? extends Targetable> targets;
    private Targetable choice;
    private boolean userResponse;

    /** Creates new PlanetaryConditionsDialog and takes the conditions from the client's Game. */
    protected TargetChoiceDialog(JFrame frame, @Nullable List<? extends Targetable> targets) {
        super(frame, Messages.getString("PlanetaryConditionsDialog.title"), true, true);
        this.targets = targets;
        this.setLayout(new GridLayout());
        updateChoices();
    }
    public boolean showDialog() {
        userResponse = false;
        setVisible(true);
        return userResponse;
    }
    private void updateChoices() {
        if (targets == null) {
            return;
        }

        for (Targetable target : targets) {
            //            JButton button = new JButton("<html><b>"+target.getDisplayName()+"</b></html>");
            JButton button = new JButton("<html>"+ UnitToolTip.getEntityTipUnitDisplay((Entity)target, null)+"</html>");
            button.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    choose(target);
                }
            });
            this.add(button);
        }
    }

    private void choose(@Nullable Targetable choice) {
        this.choice = choice;
        this.userResponse = true;
        this.setVisible(false);
    }
    public Targetable getChoice() {
        return  choice;
    }


    //TODO allow multi pick
    public static @Nullable Targetable showTargetChoiceDialog(JFrame frame, @Nullable List<? extends Targetable> targets) {
        TargetChoiceDialog targetChoiceDialog = new TargetChoiceDialog(frame, targets);

        boolean userOkay = targetChoiceDialog.showDialog();
        if (userOkay) {
            return targetChoiceDialog.getChoice();
        }
        return null;
    }
}
