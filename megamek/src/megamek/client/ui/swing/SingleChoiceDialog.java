/*
 * MegaMek - Copyright (C) 2002, 2003, 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.swing;

import javax.swing.JFrame;

/**
 * A (somewhat primitive) dialog that asks a question and lets the user select
 * from the available choices. The question string is tokenised on "\n". <p/>
 * Based on Confirm
 * 
 * @author suvarov454@sourceforge.net
 * @version $version: $
 */
public class SingleChoiceDialog extends ChoiceDialog {
    /**
     * 
     */
    private static final long serialVersionUID = 3189147678153823244L;

    /**
     * Create a choice dialog. If no choices are passed in, this will be a very
     * boring dialog, but it will not suffer an exception.
     * 
     * @param parent - the <code>Frame</code> that is locked by this dialog.
     * @param title - the title <code>String</code> for this dialog.
     * @param question - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param choices - an array of <code>String</code>s to be displayed.
     */
    public SingleChoiceDialog(JFrame parent, String title, String question,
            String[] choices) {
        super(parent, title, question, choices, true);
    }

    /**
     * Which choice did the user select?
     * 
     * @return If no choices were available, or if the user canceled the choice,
     *         a value of -1 is returned, otherwise the index from the input
     *         array that matches the selected choice is returned.
     */
    public int getChoice() {
        int retval = -1;

        // Did the player make any selection?
        int[] choices = super.getChoices();
        if (null != choices && choices.length > 0) {

            // Which checkbox was selected?
            retval = choices[0];
        }

        return retval;
    } // End public int getChoice()

} // End public class SingleChoiceDialog
