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
 * GameOptionsDialog.java
 *
 * Created on April 26, 2002, 2:14 PM
 */

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.common.*;
import megamek.common.options.*;

/**
 * Responsible for displaying the current game options and allowing the user to
 * change them.
 *
 * @author  Ben
 * @version 
 */
public class GameOptionsDialog extends Dialog implements ActionListener {
    
    private Client client;
    private GameOptions options;
    
    private Vector optionComps = new Vector();
    
    private Panel panOptions = new Panel();
    private ScrollPane scrOptions = new ScrollPane();
    
    private TextArea texDesc = new TextArea("Mouse over an option to see a description.", 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);
    
    private Panel panPassword = new Panel();
    private Label labPass = new Label("Password:");
    private TextField texPass = new TextField(15);
    
    private Panel panButtons = new Panel();
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");
    
    /** Creates new GameOptionsDialog */
    public GameOptionsDialog(Client client) {
        super(client.frame, "View/Edit Game Options...", true);
        this.client = client;
        this.options = client.game.getOptions();
        
        scrOptions.add(panOptions);
        
        texDesc.setEditable(false);
        
        setupButtons();
        setupPassword();
        
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
            
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(scrOptions, c);
        add(scrOptions);
            
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(texDesc, c);
        add(texDesc);
        
        gridbag.setConstraints(panPassword, c);
        add(panPassword);
        
        gridbag.setConstraints(panButtons, c);
        add(panButtons);
        
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { setVisible(false); }
	});
        
        pack();
        setSize(getSize().width, Math.max(getSize().height, 400));
        setResizable(false);
        setLocation(client.frame.getLocation().x + client.frame.getSize().width/2 - getSize().width/2,
                    client.frame.getLocation().y + client.frame.getSize().height/2 - getSize().height/2);
    }
    
    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }
    
    public void send() {
        Vector changed = new Vector();
        
        for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
            OptionComponent comp = (OptionComponent)i.nextElement();
            
            if (comp.hasChanged()) {
                changed.addElement(comp.changedOption());
            }
        }
        
        if (changed.size() > 0) {
            client.sendGameOptions(texPass.getText(), changed);
        }
    }
    
    private void refreshOptions() {
        panOptions.removeAll();
        optionComps = new Vector();
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 0, 0);
        c.ipadx = 0;    c.ipady = 0;
        
        for (Enumeration i = options.groups(); i.hasMoreElements();) {
            OptionGroup group = (OptionGroup)i.nextElement();
            
            addGroup(group, gridbag, c);
            
            for (Enumeration j = group.options(); j.hasMoreElements();) {
                GameOption option = (GameOption)j.nextElement();
                
                addOption(option, gridbag, c);
            }
        }
        
        validate();
    }
    
    private void addGroup(OptionGroup group, GridBagLayout gridbag, GridBagConstraints c) {
        Label groupLabel = new Label(group.getName());
        
        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }
    
    private void addOption(GameOption option, GridBagLayout gridbag, GridBagConstraints c) {
        OptionComponent optionComp = new OptionComponent(option);
        
        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);
        
        optionComps.addElement(optionComp);
    }
    
    
    private void showDescFor(GameOption option) {
        texDesc.setText(option.getDesc());
    }
    
    
    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);
            
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }
    
    private void setupPassword() {
        panPassword.setLayout(new BorderLayout());
            
        panPassword.add(labPass, BorderLayout.WEST);
        panPassword.add(texPass, BorderLayout.CENTER);
    }
    
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOkay) {
            send();
        }
        this.setVisible(false);
    }

    
    private class OptionComponent extends Panel implements MouseListener
    {
        GameOption option;
        
        private Checkbox checkbox;
        private TextField textField;
        private Label label;
        
        public OptionComponent(GameOption option) {
            this.option = option;
            
            addMouseListener(this);
            
            setLayout(new BorderLayout());
            switch(option.getType()) {
                case GameOption.BOOLEAN :
                    checkbox = new Checkbox(option.getFullName(), option.booleanValue());
                    checkbox.addMouseListener(this);
                    add(checkbox, BorderLayout.CENTER);
                    break;
                default :
                    textField = new TextField(option.stringValue(), 2);
                    textField.addMouseListener(this);
                    add(textField, BorderLayout.WEST);
                    label = new Label(option.getFullName());
                    label.addMouseListener(this);
                    add(label, BorderLayout.CENTER);
                    break;
            }
            
        }
        
        public boolean hasChanged() {
            return !option.getValue().equals(getValue());
        }
        
        private Object getValue() {
            switch(option.getType()) {
                case GameOption.BOOLEAN :
                    return new Boolean(checkbox.getState());
                case GameOption.INTEGER :
                    return Integer.valueOf(textField.getText());
                case GameOption.FLOAT :
                    return Float.valueOf(textField.getText());
                default :
                    return null;
            }
        }
        
        /**
         * Returns a new option, representing the option in it's changed state.
         */
        public GameOption changedOption() {
            return new GameOption(option.getShortName(), "", "", option.getType(), getValue());
        }
        
        public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
        }
        public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
            showDescFor(option);
        }
        public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
        }
        public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
        }
        public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
        }
        
    }
}
