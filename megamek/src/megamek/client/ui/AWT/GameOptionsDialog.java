/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.options.*;

/**
 * Responsible for displaying the current game options and allowing the user to
 * change them.
 *
 * @author  Ben
 * @version 
 */
public class GameOptionsDialog extends Dialog implements ActionListener, DialogOptionListener {
    
    private ClientGUI client;
    private GameOptions options;

    private boolean editable = true;

    private Vector optionComps = new Vector();

    private int maxOptionWidth = 0;
    
    private AlertDialog savedAlert = null;
    private Panel panOptions = new Panel();
    private ScrollPane scrOptions = new ScrollPane();
    
    private TextArea texDesc = new TextArea("Mouse over an option to see a description.", 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY);
    
    private Panel panPassword = new Panel();
    private Label labPass = new Label("Password:");
    private TextField texPass = new TextField(15);
    
    private Panel panButtons = new Panel();
    private Button butSave = new Button("Save");
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");

    /**
     * Initialize this dialog.
     *
     * @param   frame - the <code>Frame</code> parent of this dialog.
     * @param   options - the <code>GameOptions</code> to be displayed.
     */
    private void init( Frame frame, GameOptions options ) {
        this.options = options;
        
        scrOptions.add(panOptions);
        scrOptions.getVAdjustable().setUnitIncrement(10);
        
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
        setLocation(frame.getLocation().x + frame.getSize().width/2 - getSize().width/2,
                    frame.getLocation().y + frame.getSize().height/2 - getSize().height/2);

        savedAlert = new AlertDialog(frame, "Options saved", "Default games options saved. Next time you host a\ngame these options will be automatically loaded and set.");

    }

    /**
     * Creates new <code>GameOptionsDialog</code> for a <code>Client</code>
     *
     * @param   client - the <code>Client</code> parent of this dialog.
     */
    public GameOptionsDialog(ClientGUI client) {
        super(client.frame, "View/Edit Game Options...", true);
        this.client = client;
        this.init( client.frame, client.getClient().game.getOptions() );
    }

    /**
     * Creates new <code>GameOptionsDialog</code> for a given 
     * <code>Frame</code>, with given set of options.
     *
     * @param   frame - the <code>Frame</code> parent of this dialog.
     * @param   options - the <code>GameOptions</code> to be displayed.
     */
    public GameOptionsDialog( Frame frame, GameOptions options ) {
        super(frame, "View/Edit Game Options...", true);
        this.init( frame, options );
        butOkay.setEnabled( false );
    }

    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }
    
    public void send() {
        Vector changed = new Vector();
        
        for (Enumeration i = optionComps.elements(); i.hasMoreElements();) {
            DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();
            
            if (comp.hasChanged()) {
                changed.addElement(comp.changedOption());
            }
        }
        
        if (client != null && changed.size() > 0) {
            client.getClient().sendGameOptions(texPass.getText(), changed);
        }
    }
    
    public void doSave() {
      Vector output = new Vector();
      
      for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
        DialogOptionComponent comp = (DialogOptionComponent)i.nextElement();        
        IOption option = comp.changedOption();        
        output.addElement(option);
      }
      
      GameOptions.saveOptions(output);
      
      savedAlert.show();
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
        
        for (Enumeration i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = (IOptionGroup)i.nextElement();
            
            addGroup(group, gridbag, c);
            
            for (Enumeration j = group.getOptions(); j.hasMoreElements();) {
                IOption option = (IOption)j.nextElement();
                
                addOption(option, gridbag, c);
            }
        }

        // Make the width accomadate the longest game option label
        //  without needing to scroll horizontally.
        setSize(Math.min(client.getSize().width, maxOptionWidth + 30), Math.max(getSize().height, 400));

        validate();
    }
    
    private void addGroup(IOptionGroup group, GridBagLayout gridbag, GridBagConstraints c) {
        Label groupLabel = new Label(group.getName());
        
        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }
    
    private void addOption( IOption option, GridBagLayout gridbag,
                            GridBagConstraints c ) {
        DialogOptionComponent optionComp =
            new DialogOptionComponent(this, option);
        
        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);
        maxOptionWidth = Math.max( maxOptionWidth,
                                   optionComp.getPreferredSize().width );

        if (option.getName().equals("inf_deploy_even")) {
            if ( !(options.getOption("inf_move_even")).booleanValue()
                 || !editable ) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("inf_move_multi")) {
            if ( (options.getOption("inf_move_even")).booleanValue()
                 || (options.getOption("inf_move_later")).booleanValue()
                 || !editable ) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("inf_move_even")) {
            if ( (options.getOption("inf_move_multi")).booleanValue()
                 || (options.getOption("inf_move_later")).booleanValue()
                 || !editable ) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("inf_move_later")) {
            if ( (options.getOption("inf_move_even")).booleanValue()
                 || (options.getOption("inf_move_multi")).booleanValue()
                 || !editable ) {
                optionComp.setEditable(false);
            }
        }
        else if (option.getName().equals("protos_deploy_even")) {
            if ( !(options.getOption("protos_move_even")).booleanValue()
                 || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_move_multi")) {
            if ( (options.getOption("protos_move_even")).booleanValue()
                 || (options.getOption("protos_move_later")).booleanValue()
                 || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_move_even")) {
            if ( (options.getOption("protos_move_multi")).booleanValue()
                 || (options.getOption("protos_move_later")).booleanValue()
                 || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_move_later")) {
            if ( (options.getOption("protos_move_even")).booleanValue()
                 || (options.getOption("protos_move_multi")).booleanValue()
                 || !editable ) {
                optionComp.setEditable(false);
            }
        } else {
            optionComp.setEditable( editable );
        }
        optionComps.addElement(optionComp);
    }
    
    // Gets called when one of the options gets moused over.
    public void showDescFor(IOption option) {
        texDesc.setText(option.getDescription());
    }
    
    // Gets called when one of the option checkboxes is clicked.
    //  Arguments are the GameOption object and the true/false
    //  state of the checkbox.
    public void optionClicked(DialogOptionComponent comp, IOption option, boolean state) {
        if (option.getName().equals("inf_move_even")) {
            for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
                DialogOptionComponent comp_i = (DialogOptionComponent)i.nextElement();
                if (comp_i.option.getName().equals("inf_deploy_even")) {
                    comp_i.setEditable(state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("inf_move_multi")) {
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("inf_move_later")) {
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("inf_move_multi")) {
            for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
                DialogOptionComponent comp_i = (DialogOptionComponent)i.nextElement();
                if (comp_i.option.getName().equals("inf_move_even")) {
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("inf_move_later")) {
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("inf_move_later")) {
            for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
                DialogOptionComponent comp_i = (DialogOptionComponent)i.nextElement();
                if (comp_i.option.getName().equals("inf_move_even")) {
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("inf_move_multi")) {
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("protos_move_even")) {
            for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
                DialogOptionComponent comp_i = (DialogOptionComponent)i.nextElement();
                if (comp_i.option.getName().equals("protos_deploy_even")) {
                    comp_i.setEditable(state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("protos_move_multi")) {
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("protos_move_later")) {
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("protos_move_multi")) {
            for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
                DialogOptionComponent comp_i = (DialogOptionComponent)i.nextElement();
                if (comp_i.option.getName().equals("protos_move_even")) {
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("protos_move_later")) {
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("protos_move_later")) {
            for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
                DialogOptionComponent comp_i = (DialogOptionComponent)i.nextElement();
                if (comp_i.option.getName().equals("protos_move_even")) {
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("protos_move_multi")) {
                    comp_i.setEditable(!state);
                }
            }
        }
    }

    private void setupButtons() {
        butSave.addActionListener(this);
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
            
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(butSave, c);
        panButtons.add(butSave);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
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
        } else if (e.getSource() == butSave) {
          doSave();
          
          return;
        }
        
        this.setVisible(false);
    }

    /**
     * Update the dialog so that it is editable or view-only.
     *
     * @param   editable - <code>true</code> if the contents of the dialog
     *          are editable, <code>false</code> if they are view-only.
     */
    public void setEditable( boolean editable ) {

        // Set enabled state of all of the option components in the dialog.
        for ( Enumeration i = optionComps.elements(); i.hasMoreElements(); ) {
            DialogOptionComponent comp = 
                (DialogOptionComponent) i.nextElement();
            comp.setEditable( editable );
        }

        // If the panel is editable, the player can commit and save.
        texPass.setEnabled( editable );
        butOkay.setEnabled( editable && client != null );
        butSave.setEnabled( editable );

        // Update our data element.
        this.editable = editable;
    }

    /**
     * Determine whether the dialog is editable or view-only.
     *
     * @return  <code>true</code> if the contents of the dialog are
     *          editable, <code>false</code> if they are view-only.
     */
    public boolean isEditable() {
        return this.editable;
    }
        
}
