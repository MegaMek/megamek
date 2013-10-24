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
 * BoardComponent.java
 *
 * Created on March 25, 2002, 11:35 AM
 */

package megamek.client.ui.AWT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Scrollbar;

/**
 * @author Ben
 * @version
 */
public class BoardComponent extends Panel {

    /**
     * 
     */
    private static final long serialVersionUID = 5110070086699741736L;
    private Scrollbar scrVertical = new Scrollbar(Scrollbar.VERTICAL);
    private Scrollbar scrHorizontal = new Scrollbar(Scrollbar.HORIZONTAL);
    private Panel panBlank = new Panel();

    /** Creates new BoardComponent */
    public BoardComponent(BoardView1 bv) {

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(bv, c);
        add(bv);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(scrVertical, c);
        add(scrVertical);

        c.gridwidth = 1;
        gridbag.setConstraints(scrHorizontal, c);
        add(scrHorizontal);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(panBlank, c);
        add(panBlank);

    }

}
