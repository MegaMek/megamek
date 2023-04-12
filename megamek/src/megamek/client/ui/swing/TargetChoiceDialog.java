package megamek.client.ui.swing;

import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.Targetable;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class TargetChoiceDialog extends ClientDialog {
    private static boolean showDetails = false;
    List<? extends Targetable> targets;
    private List<Targetable> choosen = new ArrayList<Targetable>();
    private boolean isMultiSelect;
    private boolean userResponse;
    private JPanel choicesPanel;
    private JToggleButton [] buttons;

    /** Creates new PlanetaryConditionsDialog and takes the conditions from the client's Game. */
    protected TargetChoiceDialog(JFrame frame, String message, String title,
                                 @Nullable List<? extends Targetable> targets, boolean isMultiSelect) {
        super(frame, title, true, true);
        this.targets = targets;
        this.isMultiSelect = isMultiSelect;
        this.setLayout(new BorderLayout());

        JPanel ops = new JPanel();
        ops.setLayout(new FlowLayout());
        this.add(ops, BorderLayout.PAGE_START);
        var msgLabel = new JLabel(message, JLabel.CENTER );
        ops.add(msgLabel);

        JToggleButton detailsCheckBox = new JToggleButton("Show details", showDetails);
        detailsCheckBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDetails();
            }
        });
        ops.add(detailsCheckBox);

        choicesPanel = new JPanel();
        JScrollPane listScroller = new JScrollPane(choicesPanel);
        this.add(listScroller, BorderLayout.CENTER);

        JPanel doneCancel = new JPanel();
        doneCancel.setLayout(new FlowLayout());
        this.add(doneCancel, BorderLayout.PAGE_END);

        if (this.isMultiSelect) {
            JButton doneButton = new JButton("OK");
            doneButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doOK();
                }
            });
            doneCancel.add(doneButton);
        }

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        doneCancel.add(cancelButton);
        initChoices();
        updateChoices();
    }

    public boolean showDialog() {
        userResponse = false;
        setVisible(true);
        return userResponse;
    }

    private void initChoices() {
        choicesPanel.setLayout(new GridLayout(0,2));
        buttons = new JToggleButton[targets.size()];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JToggleButton();
            Targetable target = targets.get(i);
            choicesPanel.add(buttons[i]);
            buttons[i].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    choose(target);
                }
            });

        }
    }

    protected void updateChoices() {
        if (targets == null) {
            return;
        }

        for (int i = 0; i < buttons.length; i++) {
            Targetable target = targets.get(i);
            buttons[i].setSelected(choosen.contains(target));
            if (showDetails) {
                buttons[i].setText("<html>" + UnitToolTip.getEntityTipBrief((Entity) target, null) + "</html>");
            } else {
                String mods = "";
                if (target instanceof Entity) {
                    //not sure if check is needed, safe to assume all targets are entity?
                    Game game = ((Entity)target).getGame();
                    int tmm = Compute.getTargetMovementModifier(game, target.getId()).getValue();
                    mods = ' '+(tmm < 0 ? "-" : "+")+tmm +' ';

                }
                buttons[i].setText("<html><b>" + target.getDisplayName() + "</b>"+mods+"</html>");
            }
        }
    }

    private void toggleDetails()
    {
        showDetails = !showDetails;
        updateChoices();
        // force a resize to fit change components
        this.setVisible(true);
    }

    private void choose(@Nullable Targetable choice) {
        if (!choosen.contains(choice)) {
            choosen.add(choice);
        }
        if (!isMultiSelect) {
            doOK();
        }
    }

    // use
    private void doOK()
    {
        this.userResponse = true;
        this.setVisible(false);
    }

    private void doCancel()
    {
        this.userResponse = false;
        this.setVisible(false);
    }

    /***
     *
     * @return first chosen item, or @null if nothing chosen
     */
    public @Nullable Targetable getFirstChoice() {
        return (choosen == null || choosen.size() == 0) ? null : choosen.get(0);
    }

    /***
     *
     * @return list of items picked by user
     */
    public @Nullable Targetable getChoosen() {
        return (choosen == null || choosen.size() == 0) ? null : choosen.get(0);
    }

    //TODO allow multi pick
    public static @Nullable Targetable showTargetChoiceDialog(JFrame frame, String message, String title, @Nullable List<? extends Targetable> targets, boolean isMultiSelect) {
        TargetChoiceDialog targetChoiceDialog = new TargetChoiceDialog(frame, message, title, targets, isMultiSelect);

        boolean userOkay = targetChoiceDialog.showDialog();
        if (userOkay) {
            return targetChoiceDialog.getFirstChoice();
        }
        return null;
    }
}
