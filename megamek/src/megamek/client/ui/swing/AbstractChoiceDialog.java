package megamek.client.ui.swing;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChoiceDialog<T> extends ClientDialog {
    private static boolean showDetails = false;
    List<T> targets;
    private List<T> choosen = new ArrayList<T>();
    private boolean isMultiSelect;
    private boolean userResponse;
    private JPanel choicesPanel;
    private JToggleButton [] buttons;

    /** Creates new PlanetaryConditionsDialog and takes the conditions from the client's Game. */
    protected AbstractChoiceDialog(JFrame frame, String message, String title,
                                   @Nullable List<T> targets, boolean isMultiSelect) {
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
    }

    public boolean showDialog() {
        userResponse = false;
        updateChoices();
        setVisible(true);
        return userResponse;
    }

    private void initChoices() {
        choicesPanel.setLayout(new GridLayout(0,2));
        buttons = new JToggleButton[targets.size()];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JToggleButton();
            T target = targets.get(i);
            choicesPanel.add(buttons[i]);
            buttons[i].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    choose(target);
                }
            });

        }
    }

    private void updateChoices() {
        if (targets == null) {
            return;
        }

        for (int i = 0; i < buttons.length; i++) {
            T target = targets.get(i);
            buttons[i].setSelected(choosen.contains(target));
            if (showDetails) {
                detailLabel(buttons[i], target);
            } else {
                simpleLabel(buttons[i], target);
            }
        }
    }

    abstract protected void detailLabel(JToggleButton button, T target);


    abstract  protected void simpleLabel(JToggleButton button, T target);

    private void toggleDetails()
    {
        showDetails = !showDetails;
        updateChoices();
        // force a resize to fit change components
        this.setVisible(true);
    }

    private void choose(@Nullable T choice) {
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
    public @Nullable T getFirstChoice() {
        return (choosen == null || choosen.size() == 0) ? null : choosen.get(0);
    }

    /***
     *
     * @return list of items picked by user
     */
    public @Nullable List<T> getChoosen() {
        return (choosen == null || choosen.size() == 0) ? null : choosen;
    }
}
