/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * BoardSelectionDialog.java
 *
 * Created on March 25, 2002, 6:28 PM
 */

package megamek.client;

import java.awt.*;
import java.awt.event.*;

import megamek.common.*;

/**
 *
 * @author  Ben
 * @version 
 */
public class BoardSelectionDialog 
    extends Dialog implements ActionListener
{
    
    private Label labBoardWidth = new Label("Board Width (hexes):", Label.RIGHT);
    private Label labBoardHeight = new Label("Board Height (hexes):", Label.RIGHT);
    private TextField texBoardWidth = new TextField(4);
    private TextField texBoardHeight = new TextField(4);
    
    private Label labMapWidth = new Label("Map Width (board):", Label.RIGHT);
    private Label labMapHeight = new Label("Map Height (board):", Label.RIGHT);
    private TextField texMapWidth = new TextField(4);
    private TextField texMapHeight = new TextField(4);
    
    private Panel panMapsSelected = new Panel();
    private Label labMapsSelected = new Label("Maps Selected:", Label.CENTER);
    private List lisMapsSelected = new List(12);

    private Panel panMapsAvailable = new Panel();
    private Label labMapsAvailable = new Label("Maps Available :", Label.CENTER);
    private List lisMapsAvailable = new List(12);
    
    private Panel panButton = new Panel();
    private Button butOkay = new Button("Update");
    private Button butOkay = new Button("Okay");
    private Button butOkay = new Button("Cancel");
    

    /** Creates new BoardSelectionDialog */
    public BoardSelectionDialog(Frame parent) {
        super(parent, "Edit Board Layout...", true);
    }
    
    
    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }

}
