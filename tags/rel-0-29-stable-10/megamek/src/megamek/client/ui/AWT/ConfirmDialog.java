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

package megamek.client;

import java.awt.*;
import java.awt.event.*;

import megamek.common.*;
import megamek.client.util.*;

/**
 * A simple yes/no confirmation dialog.
 */
public class ConfirmDialog
    extends Dialog implements ActionListener {
    
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();

    private boolean useCheckbox;
    private Checkbox botherCheckbox;

    private Panel panButtons = new Panel();
    private Button butYes = new Button("Yes");
    private Button butNo = new Button("No");

    private boolean confirmation = false;

    public ConfirmDialog(Frame p, String title, String question) {
        this(p, title, question, false);
    }

    public ConfirmDialog(Frame p, String title, String question, boolean includeCheckbox) {
        super(p, title, true);
        super.setResizable(false);
        useCheckbox = includeCheckbox;

        setLayout(gridbag);
        addQuestion(question);
        addInputs();
        finishSetup(p);
    }

    private void addQuestion(String question) {
        AdvancedLabel questionLabel = new AdvancedLabel(question);
        c.gridheight = 2;
        c.insets = new Insets(5, 5, 5, 5);
        gridbag.setConstraints(questionLabel, c);
        add(questionLabel);
    }

    private void addInputs() {
        int y = 2;

        c.gridheight = 1;

        if (useCheckbox) {
            botherCheckbox = new Checkbox("Do not bother me again");
        
            c.gridy = y++;
            gridbag.setConstraints(botherCheckbox, c);
            add(botherCheckbox);
        }

        butYes.addActionListener(this);
        butNo.addActionListener(this);

        GridBagLayout buttonGridbag = new GridBagLayout();
        GridBagConstraints bc = new GridBagConstraints();
        panButtons.setLayout(buttonGridbag);
        bc.insets = new Insets(5, 5, 5, 5);
        bc.ipadx = 20;    bc.ipady = 5;
        buttonGridbag.setConstraints(butYes, bc);
        panButtons.add(butYes);
        buttonGridbag.setConstraints(butNo, bc);
        panButtons.add(butNo);

        c.gridy = y;

        gridbag.setConstraints(panButtons, c);
        add(panButtons);
    }

    private void finishSetup(Frame p) {
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    setVisible(false);
                }
            });

        pack();
        setLocation(p.getLocation().x + p.getSize().width/2 - getSize().width/2,
                    p.getLocation().y + p.getSize().height/2 - getSize().height/2);
    }
    
    public boolean getAnswer() {
        return confirmation;
    }

    public boolean getShowAgain() {
        if (botherCheckbox == null) {
            return true;
        }
        return !botherCheckbox.getState();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butYes) {
            confirmation = true;
        } else if (actionEvent.getSource() == butNo) {
            confirmation = false;
        }
        this.setVisible(false);
    }
}
