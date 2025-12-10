/*
 * Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.board.Board;
import megamek.common.ManeuverType;
import megamek.common.moves.MovePath;

/**
 * A (somewhat primitive) dialog that asks a question and lets the player select from the available choices. The
 * question string is tokenised on "\n". <p> Refactored from SingleChoiceDialog (which was based on Confirm)
 *
 * @author suvarov454@sourceforge.net
 */
public class ManeuverChoiceDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 3093043054221558221L;

    private boolean confirm;

    private final JPanel panButtons = new JPanel();
    private final JButton butOK = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));

    /**
     * The checkboxes for available choices.
     */
    private AbstractButton[] checkboxes;

    /**
     * Current VSTOL status for tooltip generation
     */
    private boolean isVSTOL_CF = false;

    /**
     * Create and initialize the dialog.
     *
     * @param parent  - the <code>Frame</code> that is locked by this dialog.
     * @param choices - an array of <code>String</code>s to be displayed.
     */
    private void initialize(JFrame parent, String[] choices) {
        super.setResizable(false);

        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        // Do we have any choices?
        if (choices != null && choices.length > 0) {

            // Display the choices as a single column of radio buttons.
            // The first checkbox is selected by default.
            JPanel choiceArea = new JPanel(new GridLayout(0, 1));
            c.gridy++;
            c.insets = new Insets(0, 10, 0, 10);

            // If there are many choices, display them in a scroll pane.
            GridBagConstraints center = new GridBagConstraints();
            center.anchor = GridBagConstraints.CENTER;
            if (choices.length > 10) {

                // Save the current value of c.fill; change it to HORIZONTAL.
                int saveFill = c.fill;
                c.fill = GridBagConstraints.HORIZONTAL;

                // Place the choice area in the center of another panel that is scrolled.
                JPanel scrolled = new JPanel(new GridBagLayout());
                scrolled.add(choiceArea, center);
                JScrollPane scroller = new JScrollPane(scrolled);
                getContentPane().add(scroller, c);

                // Restore the saved value of c.fill.
                c.fill = saveFill;

            } else {
                getContentPane().add(choiceArea, c);
            }

            // Single choice dialogs use radio buttons.
            checkboxes = new JRadioButton[choices.length];
            ButtonGroup radioGroup = new ButtonGroup();
            for (int loop = 0; loop < choices.length; loop++) {
                checkboxes[loop] = new JRadioButton(choices[loop],
                      loop == 0);
                radioGroup.add(checkboxes[loop]);
                choiceArea.add(checkboxes[loop]);
            }
        } // End have-choices

        // Allow the player to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 10, 5, 10);
        getContentPane().add(panButtons, c);
        butOK.requestFocus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        Dimension size = getSize();

        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }
        setLocation(parent.getLocation().x + parent.getSize().width / 2
              - size.width / 2, parent.getLocation().y
              + parent.getSize().height / 2 - size.height / 2);
    }

    private void setupButtons() {
        butOK.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridBagLayout);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 5, 5, 5);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;

        c.gridwidth = 1;
        gridBagLayout.setConstraints(butOK, c);
        panButtons.add(butOK);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBagLayout.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }

    /**
     * Create a choice dialog. The player can choose any or all of the choices. If no choices are passed in, this will
     * be a very boring dialog, but it will not suffer an exception.
     *
     * @param parent - the <code>Frame</code> that is locked by this dialog.
     * @param title  - the title <code>String</code> for this dialog.
     */
    public ManeuverChoiceDialog(JFrame parent, String title) {
        super(parent, title, true);
        String[] choices = new String[ManeuverType.MAN_SIZE];
        for (int type = 0; type < ManeuverType.MAN_SIZE; type++) {
            choices[type] = ManeuverType.getTypeName(type);
        }
        initialize(parent, choices);
    }

    /**
     * Create a choice dialog. The player can choose any or all of the choices. If no choices are passed in, this will
     * be a very boring dialog, but it will not suffer an exception.
     *
     * @param parent   - the <code>Frame</code> that is locked by this dialog.
     * @param title    - the title <code>String</code> for this dialog.
     * @param question - <code>String</code> displayed above the choices. The question string is tokenised on "\n".
     *
     * @deprecated question is never used, use the other constructor instead.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public ManeuverChoiceDialog(JFrame parent, String title, String question) {
        super(parent, title, true);
        String[] choices = new String[ManeuverType.MAN_SIZE];
        for (int type = 0; type < ManeuverType.MAN_SIZE; type++) {
            choices[type] = ManeuverType.getTypeName(type);
        }
        initialize(parent, choices);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // No choices, no selection.
        if (checkboxes == null) {
            confirm = false;
            setVisible(false);
        } else if (e.getSource().equals(butOK)) {
            confirm = true;
            setVisible(false);
        } else {
            confirm = false;
            setVisible(false);
        }
    }

    /**
     * See if the player confirmed a choice.
     *
     * @return <code>true</code> if the player has confirmed a choice.
     *       <code>false</code> if the player canceled, if the player did
     *       not select a choice, or if no choices were available.
     */
    public boolean getAnswer() {
        return (-1 != getChoice());
    }

    /**
     * Which choices did the player select?
     *
     * @return If no choices were available, if the player canceled, if the player did not select a choice, or if the
     *       player canceled the choice, a <code>null</code> value is returned, otherwise an array of the
     *       <code>int</code> indexes from the input array that match the selected choices is returned.
     */
    public int getChoice() {
        int[] retVal = { -1 };

        // Did the player make a choice?
        if (checkboxes != null && confirm) {

            // Make a temporary array that can hold all answers.
            int[] temp = new int[checkboxes.length];

            // Fill the temporary array.
            int index = 0;
            for (int loop = 0; loop < checkboxes.length; loop++) {
                if (checkboxes[loop].isSelected()) {
                    temp[index] = loop;
                    index++;
                }
            }

            // Do we need to shrink the array?
            if (checkboxes.length == index) {
                // No, the player selected all choices.
                retVal = temp;
            } else if (index > 0) {
                // Yup. Create an array and copy the values from temp.
                retVal = new int[index];
                System.arraycopy(temp, 0, retVal, 0, index);
            }
            // If 0 == index, then we want to return a null array.
        }

        return retVal[0];
    }

    /**
     * Updates the maneuver buttons based on current unit state and generates tooltips.
     *
     * @param velocity Current unit velocity
     * @param altitude Current unit altitude
     * @param ceiling  Current ceiling for the unit
     * @param isVTOL   Whether the unit is a VSTOL
     * @param distance Distance traveled so far this turn
     * @param board    The game board
     * @param mp       The current movement path
     */
    public void checkPerformability(int velocity, int altitude, int ceiling,
          boolean isVTOL, int distance, Board board, MovePath mp) {
        // Store current VSTOL status for tooltip generation
        this.isVSTOL_CF = isVTOL;

        for (int type = 0; type < ManeuverType.MAN_SIZE; type++) {
            boolean canPerform = ManeuverType.canPerform(type, velocity, altitude, ceiling, isVTOL, distance, board, mp);
            checkboxes[type].setEnabled(canPerform);
            // Update tooltip to show current state using ManeuverType
            checkboxes[type].setToolTipText(
                ManeuverType.getManeuverTooltip(type, canPerform, velocity, altitude, isVSTOL_CF));
        }
    }
}
