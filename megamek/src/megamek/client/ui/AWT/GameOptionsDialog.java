/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import gov.nist.gui.TabPanel;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

/**
 * Responsible for displaying the current game options and allowing the user to
 * change them.
 *
 * @author Ben
 * @version
 */
public class GameOptionsDialog extends Dialog implements ActionListener,
        DialogOptionListener {

    /**
     *
     */
    private static final long serialVersionUID = -4076751608068469452L;
    private ClientGUI client;
    private GameOptions options;

    private boolean editable = true;

    private Vector<DialogOptionComponent> optionComps = new Vector<DialogOptionComponent>();

    private int maxOptionWidth = 0;

    private TabPanel panOptions;
    private Panel groupPanel;
    private ScrollPane scrOptions = new ScrollPane();

    private TextArea texDesc = new TextArea(
            Messages.getString("GameOptionsDialog.optionDescriptionHint"), 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$

    private Panel panPassword = new Panel();
    private Label labPass = new Label(Messages
            .getString("GameOptionsDialog.Password")); //$NON-NLS-1$
    private TextField texPass = new TextField(15);

    private Panel panButtons = new Panel();
    private Button butDefaults = new Button(Messages
            .getString("GameOptionsDialog.Defaults")); //$NON-NLS-1$
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$

    private Frame currentFrame = new Frame();

    /**
     * Initialize this dialog.
     *
     * @param frame - the <code>Frame</code> parent of this dialog.
     * @param options - the <code>GameOptions</code> to be displayed.
     */
    private void init(Frame frame, GameOptions options) {
        this.options = options;
        currentFrame = frame;

        panOptions = new TabPanel();

        texDesc.setEditable(false);

        setupButtons();
        setupPassword();

        // layout
        setLayout(new GridBagLayout());

        // layout
        add(panOptions, GBC.eol().fill(GridBagConstraints.BOTH).insets(4, 4, 4, 4));
        add(texDesc, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(4, 0, 4, 0));
        add(panPassword, GBC.eol().anchor(GridBagConstraints.CENTER).insets(4, 4, 4, 4));
        add(panButtons, GBC.eol().anchor(GridBagConstraints.CENTER));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setSize(getSize().width, Math.max(getSize().height, 400));
        setResizable(true);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);

    }

    /**
     * Creates new <code>GameOptionsDialog</code> for a <code>Client</code>
     *
     * @param client - the <code>Client</code> parent of this dialog.
     */
    public GameOptionsDialog(ClientGUI client) {
        super(client.frame, Messages.getString("GameOptionsDialog.title"), true); //$NON-NLS-1$
        this.client = client;
        init(client.frame, client.getClient().game.getOptions());
    }

    /**
     * Creates new <code>GameOptionsDialog</code> for a given
     * <code>Frame</code>, with given set of options.
     *
     * @param frame - the <code>Frame</code> parent of this dialog.
     * @param options - the <code>GameOptions</code> to be displayed.
     */
    public GameOptionsDialog(Frame frame, GameOptions options) {
        super(frame, Messages.getString("GameOptionsDialog.title"), true); //$NON-NLS-1$
        init(frame, options);
        butOkay.setEnabled(false);
    }

    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }

    public void send() {
        Vector<IBasicOption> changed = new Vector<IBasicOption>();

        for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                .hasMoreElements();) {
            DialogOptionComponent comp = i.nextElement();

            if (comp.hasChanged()) {
                changed.addElement(comp.changedOption());
            }
        }

        if ((client != null) && (changed.size() > 0)) {
            client.getClient().sendGameOptions(texPass.getText(), changed);
        }
    }

    public void doSave() {
        GameOptions.saveOptions(getOptions());
    }

    public Vector<IBasicOption> getOptions() {
        Vector<IBasicOption> output = new Vector<IBasicOption>();

        for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                .hasMoreElements();) {
            DialogOptionComponent comp = i.nextElement();
            IBasicOption option = comp.changedOption();
            output.addElement(option);
        }
        return output;
    }

    public void resetToDefaults() {
        for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                .hasMoreElements();) {
            DialogOptionComponent comp = i.nextElement();
            comp.resetToDefault();
        }
    }

    private void refreshOptions() {
        panOptions.removeAll();
        optionComps.clear();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        //panOptions.setLayout(gridbag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            addGroup(group, gridbag, c);

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                addOption(option, gridbag, c);
            }
        }

        // Make the width accomadate the longest game option label
        // without needing to scroll horizontally.
        setSize(Math.min(currentFrame.getSize().width, maxOptionWidth + 30),
                Math.max(getSize().height, 400));

        validate();
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridbag,
            GridBagConstraints c) {
        groupPanel = new Panel();
        scrOptions = new ScrollPane();

        groupPanel.setLayout(gridbag);
        scrOptions.add(groupPanel);
        scrOptions.getVAdjustable().setUnitIncrement(10);

        gridbag.setConstraints(groupPanel, c);
        panOptions.add(group.getDisplayableName(),scrOptions);
    }

    private void addOption(IOption option, GridBagLayout gridbag,
            GridBagConstraints c) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this,
                option);

        gridbag.setConstraints(optionComp, c);
        groupPanel.add(optionComp);
        maxOptionWidth = Math.max(maxOptionWidth,
                optionComp.getPreferredSize().width);

        if (option.getName().equals("hidden_units")) {
            // FIXME
            // This is a convenient way to disable it until it's actually
            // usable.
            optionComp.setEditable(false);
        } else if (option.getName().equals("inf_deploy_even")) { //$NON-NLS-1$
            if (!(options.getOption("inf_move_even")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("inf_move_multi")) { //$NON-NLS-1$
            if ((options.getOption("inf_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("inf_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("inf_move_even")) { //$NON-NLS-1$
            if ((options.getOption("inf_move_multi")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("inf_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("inf_move_later")) { //$NON-NLS-1$
            if ((options.getOption("inf_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("inf_move_multi")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_deploy_even")) { //$NON-NLS-1$
            if (!(options.getOption("protos_move_even")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_move_multi")) { //$NON-NLS-1$
            if ((options.getOption("protos_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("protos_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_move_even")) { //$NON-NLS-1$
            if ((options.getOption("protos_move_multi")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("protos_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("protos_move_later")) { //$NON-NLS-1$
            if ((options.getOption("protos_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("protos_move_multi")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("visibility")) { //$NON-NLS-1$
            if (!(options.getOption("double_blind")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        }else if (option.getName().equals("tacops_falling_expanded")) { //$NON-NLS-1$
            if (!(options.getOption("tacops_hull_down")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        }else {
            optionComp.setEditable(editable);
        }
        optionComps.addElement(optionComp);
    }

    // Gets called when one of the options gets moused over.
    public void showDescFor(IOption option) {
        texDesc.setText(option.getDescription());
    }

    // Gets called when one of the option checkboxes is clicked.
    // Arguments are the GameOption object and the true/false
    // state of the checkbox.
    public void optionClicked(DialogOptionComponent comp, IOption option,
            boolean state) {
        if (option.getName().equals("inf_move_even")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("inf_deploy_even")) { //$NON-NLS-1$
                    comp_i.setEditable(state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("inf_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("inf_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("inf_move_multi")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("inf_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("inf_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("inf_move_later")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("inf_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("inf_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("protos_move_even")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("protos_deploy_even")) { //$NON-NLS-1$
                    comp_i.setEditable(state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("protos_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("protos_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("protos_move_multi")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("protos_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("protos_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("protos_move_later")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("protos_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if (comp_i.option.getName().equals("protos_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("individual_initiative")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i.hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("protos_deploy_even")) { //$NON-NLS-1$
                    comp_i.setEditable(false);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("protos_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("protos_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("protos_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("inf_deploy_even")) { //$NON-NLS-1$
                    comp_i.setEditable(false);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("inf_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("inf_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
                if (comp_i.option.getName().equals("inf_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
            }
        }
        if (option.getName().equals("vacuum")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("fire")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setState(false);
                }
            }
        }
        if (option.getName().equals("double_blind")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("visibility")) { //$NON-NLS-1$
                    comp_i.setEditable(state);
                }
            }
        }

        if (option.getName().equals("tacops_hull_down") ){
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i.hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ( comp_i.option.getName().equals("tacops_falling_expanded")){
                    comp_i.setEditable(state);
                    comp_i.setState(false);
                }
            }
        }
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butDefaults.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);

        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butDefaults, c);
        panButtons.add(butDefaults);
    }

    private void setupPassword() {
        panPassword.setLayout(new BorderLayout());

        panPassword.add(labPass, BorderLayout.WEST);
        panPassword.add(texPass, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOkay) {
            if (client != null) {
                send();
            }
            doSave();
        } else if (e.getSource() == butDefaults) {
            resetToDefaults();
            return;
        } else if (e.getSource() == butCancel) {
            refreshOptions();
        }

        setVisible(false);
    }

    /**
     * Update the dialog so that it is editable or view-only.
     *
     * @param editable - <code>true</code> if the contents of the dialog are
     *            editable, <code>false</code> if they are view-only.
     */
    public void setEditable(boolean editable) {

        // Set enabled state of all of the option components in the dialog.
        for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                .hasMoreElements();) {
            DialogOptionComponent comp = i.nextElement();
            comp.setEditable(editable);
        }

        // If the panel is editable, the player can commit or reset.
        texPass.setEnabled(editable);
        butOkay.setEnabled(editable);
        butDefaults.setEnabled(editable);

        // Update our data element.
        this.editable = editable;
    }

    /**
     * Determine whether the dialog is editable or view-only.
     *
     * @return <code>true</code> if the contents of the dialog are editable,
     *         <code>false</code> if they are view-only.
     */
    public boolean isEditable() {
        return editable;
    }

}
