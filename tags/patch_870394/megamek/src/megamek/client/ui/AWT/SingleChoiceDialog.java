/*
 * MegaMek - Copyright (C) 2002,2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import megamek.common.Settings;

/**
 * A (somewhat primitive) dialog that asks a question and lets the user
 * select from the available choices.
 * The question string is tokenised on "\n".
 *
 * Based on Confirm
 * @author  suvarov454@sourceforge.net
 * @version $version: $
 */
public class SingleChoiceDialog
    extends Dialog implements ActionListener
{
    private boolean confirm = false;

    private Panel panButtons = new Panel();
    private Button butOK = new Button("OK");
    private Button butCancel = new Button("Cancel");

    /**
     * The checkboxes for available choices.
     */
    private Checkbox[] checkboxes = null;

    /**
     * Create a choice dialog.  If no choices are passed in, this will be
     * a very boring dialog, but it will not suffer an exception.
     *
     * @param   parent - the <code>Frame</code> that is locked by this dialog.
     * @param   title - the title <code>String</code> for this dialog.
     * @param   question - <code>String</code> displayed above the choices.
     *          The question string is tokenised on "\n".
     * @param   choices - an array of <code>String</code>s to be displayed.
     */
    public SingleChoiceDialog(Frame parent, String title,
                        String question, String[] choices)
    {
        super(parent, title, true);
        super.setResizable(false);

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        // Display the question in a scrollable, uneditable area.
        TextArea message = new TextArea(question, 5, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
        message.setEditable(false);

        c.gridy = 0;
        c.insets = new Insets(0, 5, 0, 5);
        add(message,c);

        // Do we have any choices?
        if ( choices != null && choices.length > 0 ) {

            // Display the choices as a single column of radio buttons.
            // The first checkbox is selected by default.
            Panel choiceArea = new Panel( new GridLayout(0, 1) );
            c.gridy++;
            c.insets = new Insets(0, 5, 0, 5);

            // If there are many choices, display them in a scroll pane.
            if ( choices.length > 5 ) {

                // Save the current value of c.fill; change it to HORIZONTAL.
                int saveFill = c.fill;
                c.fill = GridBagConstraints.HORIZONTAL;

                // Place the choice area in the center
                // of another panel that is scrolled.
                ScrollPane scroller = new ScrollPane();
                Panel scrollee = new Panel( new GridBagLayout() );
                GridBagConstraints center = new GridBagConstraints();
                center.anchor = GridBagConstraints.CENTER;
                scrollee.add( choiceArea, center );
                scroller.add( scrollee );
                add( scroller, c );

                // Restore the saved value of c.fill.
                c.fill = saveFill;

            } else {
                add( choiceArea, c );
            }

            CheckboxGroup radioGroup = new CheckboxGroup();
            this.checkboxes = new Checkbox[ choices.length ];
            for ( int loop = 0; loop < choices.length; loop++ ) {
                this.checkboxes[loop] = new Checkbox( choices[loop],
                                                      (loop == 0),
                                                      radioGroup );
                choiceArea.add( this.checkboxes[loop] );
            }

        } // End have-choices

        // Allow the user to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 5, 5, 5);
        add(panButtons,c);
        butOK.requestFocus();
        
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) { setVisible(false); }
            });
        
        pack();
        Dimension size = getSize();
        boolean updateSize = false;
        if ( size.width < Settings.minimumSizeWidth ) {
            size.width = Settings.minimumSizeWidth;
        }
        if ( size.height < Settings.minimumSizeHeight ) {
            size.height = Settings.minimumSizeHeight;
        }
        if ( updateSize ) {
            setSize( size );
            size = getSize();
        }
        setLocation(parent.getLocation().x + parent.getSize().width/2 - size.width/2,
                    parent.getLocation().y + parent.getSize().height/2 - size.height/2);
    };

    private void setupButtons() {
        butOK.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 5, 5, 5);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;

        c.gridwidth = 1;
        gridbag.setConstraints(butOK, c);
        panButtons.add(butOK);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    };

    public void actionPerformed(ActionEvent e) {
        // No choices, no selection.
        if ( this.checkboxes == null ) {
            confirm = false;
        } else if (e.getSource() == butOK) {
            confirm = true;
        } else {
            confirm = false;
        };
        this.setVisible(false);
    };

    /**
     * See if the user confirmed a choice.
     *
     * @return  <code>true</code> if the user has confirmed a choice.
     *          <code>false</code> if the user canceled or if no choices
     *          were available.
     */
    public boolean getAnswer() {
        return confirm;
    };

    /**
     * Which choice did the user select?
     *
     * @return  If no choices were available, or if the user canceled the
     *          choice, a value of -1 is returned, otherwise the index from
     *          the input array that matches the selected choice is returned.
     */
    public int getChoice() {
        int retval = -1;

        // Which checkbox was selected?
        // Handle choiceless dialogs.
        if ( this.checkboxes != null ) {
            for ( int loop = 0; loop < this.checkboxes.length; loop++ ) {
                if ( this.checkboxes[loop].getState() == true ) {
                    retval = loop;
                    break;
                }
            }
        }

        return retval;
    } // End public int getChoice()

} // End public class SingleChoiceDialog
