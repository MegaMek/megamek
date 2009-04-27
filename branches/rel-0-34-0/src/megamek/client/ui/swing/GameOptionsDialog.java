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

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

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
 */
public class GameOptionsDialog extends JDialog implements ActionListener,
        DialogOptionListener {

    /**
     *
     */
    private static final long serialVersionUID = -6072295678938594119L;
    private ClientGUI client;
    private GameOptions options;

    private boolean editable = true;

    private Vector<DialogOptionComponent> optionComps = new Vector<DialogOptionComponent>();

    private int maxOptionWidth;

    private JTabbedPane panOptions = new JTabbedPane();
    private JScrollPane scrOptions;
    private JPanel groupPanel;

    private JTextArea texDesc = new JTextArea(Messages
            .getString("GameOptionsDialog.optionDescriptionHint"), 3, 35); //$NON-NLS-1$

    private JPanel panPassword = new JPanel();
    private JLabel labPass = new JLabel(Messages
            .getString("GameOptionsDialog.Password")); //$NON-NLS-1$
    private JTextField texPass = new JTextField(15);

    private JPanel panButtons = new JPanel();
    private JButton butDefaults = new JButton(Messages
            .getString("GameOptionsDialog.Defaults")); //$NON-NLS-1$
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JFrame currentFrame;

    /**
     * Initialize this dialog.
     *
     * @param frame - the <code>Frame</code> parent of this dialog.
     * @param options - the <code>GameOptions</code> to be displayed.
     */
    private void init(JFrame frame, GameOptions options) {
        this.options = options;
        currentFrame = frame;

        texDesc.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        texDesc.setLineWrap(true);
        texDesc.setWrapStyleWord(true);
        texDesc.setEditable(false);
        texDesc.setOpaque(false);

        setupButtons();
        setupPassword();
        JPanel mainPanel = new JPanel(new GridBagLayout());

        // layout
        mainPanel.add(panOptions, GBC.eol().fill(GridBagConstraints.BOTH).insets(5, 5, 5, 5));
        mainPanel.add(new JScrollPane(texDesc), GBC.eol().fill(GridBagConstraints.BOTH).insets(5, 0, 5, 0));
        mainPanel.add(panPassword, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 5, 5));
        mainPanel.add(panButtons, GBC.eol().anchor(GridBagConstraints.CENTER));

        getContentPane().add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setSize(mainPanel.getSize().width, Math.max(mainPanel.getSize().height, 400));
        setResizable(true);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
        Dimension size = new Dimension(getSize().width*40/100,getSize().height*59/100);
        panOptions.setPreferredSize(size);
        panOptions.setMinimumSize(size);
        panOptions.setMaximumSize(size);

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
    public GameOptionsDialog(JFrame frame, GameOptions options) {
        super(frame, Messages.getString("GameOptionsDialog.title"), true); //$NON-NLS-1$
        init(frame, options);
        butOkay.setEnabled(false);
    }

    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }

    private void send() {
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

    private void doSave() {
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

    private void resetToDefaults() {
        for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                .hasMoreElements();) {
            DialogOptionComponent comp = i.nextElement();
            comp.resetToDefault();
        }
    }

    private void refreshOptions() {
        panOptions.removeAll();
        optionComps = new Vector<DialogOptionComponent>();

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            addGroup(group);

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();

                addOption(option);
            }
        }

        // Make the width accomadate the longest game option label
        // without needing to scroll horizontally.
        setSize(Math.min(currentFrame.getSize().width, maxOptionWidth + 30),
                Math.max(getSize().height, 400));

        validate();
    }

    private void addGroup(IOptionGroup group) {
        groupPanel = new JPanel();
        scrOptions = new JScrollPane(groupPanel);
        groupPanel.setLayout(new BoxLayout(groupPanel,BoxLayout.Y_AXIS));
        scrOptions.setAutoscrolls(true);
        scrOptions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrOptions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        panOptions.addTab(group.getDisplayableName(),scrOptions);
    }

    private void addOption(IOption option) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this,
                option);

        groupPanel.add(optionComp);
        maxOptionWidth = Math.max(maxOptionWidth,
                optionComp.getPreferredSize().width);

        if (option.getName().equals("hidden_units")) {
            // FIXME
            // This is a convenient way to disable it until it's actually
            // usable.
            optionComp.setEditable(false);
        } else if ("inf_deploy_even".equals(option.getName())) { //$NON-NLS-1$
            if (!(options.getOption("inf_move_even")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("inf_move_multi".equals(option.getName())) { //$NON-NLS-1$
            if ((options.getOption("inf_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("inf_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("inf_move_even".equals(option.getName())) { //$NON-NLS-1$
            if ((options.getOption("inf_move_multi")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("inf_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("inf_move_later".equals(option.getName())) { //$NON-NLS-1$
            if ((options.getOption("inf_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("inf_move_multi")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("protos_deploy_even".equals(option.getName())) { //$NON-NLS-1$
            if (!(options.getOption("protos_move_even")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("protos_move_multi".equals(option.getName())) { //$NON-NLS-1$
            if ((options.getOption("protos_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("protos_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("protos_move_even".equals(option.getName())) { //$NON-NLS-1$
            if ((options.getOption("protos_move_multi")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("protos_move_later")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("protos_move_later".equals(option.getName())) { //$NON-NLS-1$
            if ((options.getOption("protos_move_even")).booleanValue() //$NON-NLS-1$
                    || (options.getOption("protos_move_multi")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if ("visibility".equals(option.getName())) { //$NON-NLS-1$
            if (!(options.getOption("double_blind")).booleanValue() //$NON-NLS-1$
                    || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("tacops_falling_expanded")) { //$NON-NLS-1$
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
        texDesc.setCaretPosition(0);
    }

    // Gets called when one of the option checkboxes is clicked.
    // Arguments are the GameOption object and the true/false
    // state of the checkbox.
    public void optionClicked(DialogOptionComponent comp, IOption option,
            boolean state) {
        if ("inf_move_even".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("inf_deploy_even".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(state);
                    comp_i.setSelected(false);
                }
                if ("inf_move_multi".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if ("inf_move_later".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if ("inf_move_multi".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("inf_move_even".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if ("inf_move_later".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if ("inf_move_later".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("inf_move_even".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if ("inf_move_multi".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if ("protos_move_even".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("protos_deploy_even".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(state);
                    comp_i.setSelected(false);
                }
                if ("protos_move_multi".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if ("protos_move_later".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if ("protos_move_multi".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("protos_move_even".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if ("protos_move_later".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if ("protos_move_later".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("protos_move_even".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
                if ("protos_move_multi".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                }
            }
        }
        if (option.getName().equals("individual_initiative")) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if (comp_i.option.getName().equals("protos_deploy_even")) { //$NON-NLS-1$
                    comp_i.setEditable(false);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("protos_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("protos_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("protos_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("inf_deploy_even")) { //$NON-NLS-1$
                    comp_i.setEditable(false);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("inf_move_even")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("inf_move_multi")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
                if (comp_i.option.getName().equals("inf_move_later")) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
            }
        }
        if ("vacuum".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("fire".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
            }
        }
        if ("tacops_hull_down".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("tacops_falling_expanded".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(!state);
                    comp_i.setSelected(false);
                }
            }
        }
        if ("double_blind".equals(option.getName())) { //$NON-NLS-1$
            for (Enumeration<DialogOptionComponent> i = optionComps.elements(); i
                    .hasMoreElements();) {
                DialogOptionComponent comp_i = i.nextElement();
                if ("visibility".equals(comp_i.option.getName())) { //$NON-NLS-1$
                    comp_i.setEditable(state);
                }
            }
        }
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butDefaults.addActionListener(this);

        panButtons.add(butOkay);

        panButtons.add(butCancel);

        panButtons.add(butDefaults);
    }

    private void setupPassword() {
        panPassword.setLayout(new BorderLayout());

        panPassword.add(labPass, BorderLayout.WEST);
        panPassword.add(texPass, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {
            if (client != null) {
                send();
            }
            doSave();
        } else if (e.getSource().equals(butDefaults)) {
            resetToDefaults();
            return;
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
