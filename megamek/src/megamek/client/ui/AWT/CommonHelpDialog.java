/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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
import java.io.*;

import megamek.client.util.AdvancedLabel;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonHelpDialog extends Dialog
{
    /**
     * The help text that should be displayed to the user.
     */
    private String helpText = null;

    /**
     * Create a help dialog for the given parent <code>Frame</code> by
     * reading from the indicated <code>File</code>.
     *
     * @param   frame - the parent <code>Frame</code> for this dialog.
     *          This value should <b>not</b> be <code>null</code>.
     * @param   helpfile - the <code>File</code> containing the help text.
     *          This value should <b>not</b> be <code>null</code>.
     */
    public CommonHelpDialog( Frame frame, File helpfile ) {
        // Construct the superclass.
        super( frame );

        // Make sure we close at the appropriate times.
        this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    quit();
                }
            } );

        // Create a buffer to contain our help text.
        StringBuffer buff = new StringBuffer();

        // Were we passed a null helpfile?
        if ( helpfile == null ) {
            // Big error.
            this.setTitle( "No Help Available" );
            buff.append( "Help is currently unavailable." );
        } else {
            // Set our title.
            this.setTitle( "Help File: " + helpfile.getName() );

            // Try to read in the help file.
            boolean firstLine = true;
            try {
                BufferedReader input = new BufferedReader
                    ( new FileReader(helpfile) );
                String line = input.readLine();
                //                while ( line != null && line.length() > 0 ) {
                while ( line != null ) {
                    if ( firstLine ) {
                        firstLine = false;
                    } else {
                        buff.append( " \n" ); // the space is to force a line-feed on empty lines
                    }
                    buff.append( line );
                    line = input.readLine();
                }
            } catch ( IOException exp ) {
                if ( !firstLine ) {
                    buff.append( "\n \n" );
                }
                buff.append( "Error reading help file: " )
                    .append( exp.getMessage() );
                exp.printStackTrace();
            }

        } // End non-null-helpfile

        // Create the help dialog.
        this.setLayout( new BorderLayout() );
        AdvancedLabel lblHelp = new AdvancedLabel( buff.toString() );
        ScrollPane scroll = new ScrollPane();
        scroll.add( lblHelp );
        this.add( scroll, BorderLayout.CENTER );

        // Add a "Close" button.
        Button butClose = new Button( "Close" );
        butClose.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    quit();
                }
            } );
        this.add( butClose, BorderLayout.SOUTH );

        // Make the window half the screensize by default.
        Dimension screenSize = frame.getToolkit().getScreenSize();
        Dimension windowSize = new Dimension( screenSize.width / 2, 
                                              screenSize.height / 2 );
        this.pack();
        this.setSize( windowSize );

        // Place this dialog on middle of screen.
        this.setLocation(
            screenSize.width / 2 - windowSize.width / 2,
            screenSize.height / 2 - windowSize.height / 2 );
    }

    /**
     * Close this dialog.
     */
    /* package */ void quit() {
        this.hide();
    }

}
