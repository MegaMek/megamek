/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.BasicPathRanker;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.Messages;
import megamek.common.Coords;

/**
 * BotConfigDialog is a dialog box that configures bot properties TODO add
 * appropriate configuration parameters for each bot
 */
public class BotConfigDialog extends JDialog implements ActionListener,
        KeyListener {

    private static final String PRINCESS_PANEL = "princess_config";
    private static final String TESTBOT_PANEL = "testbot_config";

    /**
     *
     */
    private static final long serialVersionUID = -544663266637225925L;
    private JRadioButton testbot_radiobutton;
    private JRadioButton princess_radiobutton;
    private ButtonGroup selectbot_group = new ButtonGroup();

    // Items for princess config here
    JComboBox princessVerbosity;
    JTextField princess_target_hex_num_x;
    JButton princess_addtarget_button;
    JList princess_targets_list;
    DefaultListModel princess_targets_list_model = new DefaultListModel();
    JCheckBox princess_forcedwithdrawal;
    JCheckBox princess_shouldflee;
    JCheckBox princess_mustflee;
    JComboBox princess_homeedge; //The board edge to be used in a forced withdrawal.
    JSlider aggression_slidebar;

    private JTextField namefield;
    private boolean custom_name = false; // did user not use default name?
    public boolean dialog_aborted = true; // did user not click Ok button?

    private JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$

    JPanel botspecificcards;

    public BotConfigDialog(JFrame parent) {
        super(parent, "Configure Bot", true);
        super.setResizable(false);

//        setLocationRelativeTo(parent);

        setLayout(new BorderLayout());
        add(switchBotPanel(), BorderLayout.NORTH);
        botspecificcards = new JPanel(new CardLayout());
        botspecificcards.add(new JPanel(), TESTBOT_PANEL);
        botspecificcards.add(princessPanel(), PRINCESS_PANEL);
        add(botspecificcards, BorderLayout.CENTER);
        butOK.addActionListener(this);

        add(okayPanel(), BorderLayout.SOUTH);

        validate();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel switchBotPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        testbot_radiobutton = new JRadioButton("Test Bot");
        testbot_radiobutton = new JRadioButton("TestBot");
        testbot_radiobutton.addActionListener(this);
        princess_radiobutton = new JRadioButton("Princess Bot");
        princess_radiobutton.addActionListener(this);
        testbot_radiobutton.setSelected(true);
        selectbot_group.add(testbot_radiobutton);
        princess_radiobutton = new JRadioButton("Princess");
        princess_radiobutton.addActionListener(this);
        selectbot_group.add(princess_radiobutton);
        testbot_radiobutton.setSelected(true);
        panel.add(testbot_radiobutton);
        panel.add(princess_radiobutton);
        return panel;
    }

    private JPanel okayPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        JPanel namepanel = new JPanel(new FlowLayout());
        namepanel.add(new JLabel("Name: "));
        namefield = new JTextField();
        namefield.setText("TestBot");
        namefield.setColumns(12);
        namefield.setToolTipText("The name of the bot player.");
        namefield.addKeyListener(this);
        namepanel.add(namefield);
        panel.add(namepanel);

        butOK.addActionListener(this);
        panel.add(butOK);

        panel.validate();
        return panel;
    }

    private JPanel princessPanel() {

        //Setup layout.
        JPanel panel = new JPanel();
        GridBagConstraints constraints = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        //Initialize constraints.
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(2,2,2,2);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        //Row 1 Column 1
        constraints.gridy = 0;
        constraints.gridx = 0;
        JLabel verbosityLabel = new JLabel("Verbosity:");
        layout.setConstraints(verbosityLabel, constraints);
        panel.add(verbosityLabel);

        //Row 1 Column 2;
        constraints.gridx++;
        princessVerbosity = new JComboBox(Princess.LogLevel.getLogLevelNames());
        princessVerbosity.setSelectedIndex(0);
        layout.setConstraints(princessVerbosity, constraints);
        panel.add(princessVerbosity);

        //Row 2 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        
        //Span 2 columns.
        constraints.gridwidth = 2;        
        princess_forcedwithdrawal = new JCheckBox("Forced Withdrawal");
        princess_forcedwithdrawal.setToolTipText("Makes Princess follow the Forced Withdrawal rules.");
        layout.setConstraints(princess_forcedwithdrawal, constraints);
        panel.add(princess_forcedwithdrawal);
        
        //Row 3 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0,2,0,2);

        princess_shouldflee = new JCheckBox("Immediate Withdrawal");
        princess_shouldflee.setToolTipText("Princess will Withdraw to Home Edge, but will not Flee unless Crippled.");
        princess_shouldflee.addActionListener(this);
        layout.setConstraints(princess_shouldflee, constraints);
        panel.add(princess_shouldflee);
        
        //Row 4 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0,10,2,2);
        princess_mustflee = new JCheckBox("Must Flee");
        princess_mustflee.setToolTipText("Princess will Flee even if not crippled.");
        princess_mustflee.setEnabled(false);
        layout.setConstraints(princess_mustflee, constraints);
        panel.add(princess_mustflee);
        
        //Row 5 Column 1.
        constraints.gridy++;
        constraints.gridx=0;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(2,2,2,2);
        JLabel homeEdgeLabel = new JLabel("Home Edge:");
        layout.setConstraints(homeEdgeLabel, constraints);
        panel.add(homeEdgeLabel);

        //Row 5 Column 2.
        constraints.gridx++;
        princess_homeedge = new JComboBox(new String[]{"North", "South", "West", "East"});
        princess_homeedge.setToolTipText("Sets the board edge Princess will retreat units to when following Forced Withdrawal.");
        princess_homeedge.setSelectedIndex(0);
        layout.setConstraints(princess_homeedge, constraints);
        panel.add(princess_homeedge);

        //Row 5.5
        constraints.gridy++;
        constraints.gridx=0;
        JLabel aggressionlabel=new JLabel("Aggression");
        panel.add(aggressionlabel,constraints);
        constraints.gridy++;
        aggression_slidebar=new JSlider(SwingConstants.HORIZONTAL,0,100,50);
        Hashtable<Integer, JLabel> aggression_slidebar_labels = new Hashtable<Integer, JLabel>();
        aggression_slidebar_labels.put(new Integer(0),new JLabel("Meek"));
        aggression_slidebar_labels.put(new Integer(100),new JLabel("Beserker"));
        aggression_slidebar.setLabelTable(aggression_slidebar_labels);
        aggression_slidebar.setPaintLabels(true);
        panel.add(aggression_slidebar,constraints);




        //Row 6 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        JLabel targetsLabel = new JLabel("Strategic Targets:");
        layout.setConstraints(targetsLabel, constraints);
        panel.add(targetsLabel);

        //Row 6 Column 2.
        constraints.gridx++;
        princess_target_hex_num_x = new JTextField();
        princess_target_hex_num_x.setToolTipText("Enter the hex number of the target.");
        princess_target_hex_num_x.setColumns(4);
        layout.setConstraints(princess_target_hex_num_x, constraints);
        panel.add(princess_target_hex_num_x);

        //Row 7 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;

        //Span 2 columns.
        constraints.gridwidth = 2;
        princess_addtarget_button = new JButton("Add Strategic Target");
        princess_addtarget_button.setToolTipText("Adds the target hex to the list of strategic targets.");
        princess_addtarget_button.addActionListener(this);
        layout.setConstraints(princess_addtarget_button, constraints);
        panel.add(princess_addtarget_button);

        //Row 8 Column 1.
        constraints.gridy++;
        princess_targets_list = new JList(princess_targets_list_model);
        princess_targets_list.setToolTipText("List of target hexes Princess will attempt to attack if a building is present.");
        princess_targets_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        princess_targets_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        princess_targets_list.setLayoutOrientation(JList.VERTICAL);
        JScrollPane targetScroller = new JScrollPane(princess_targets_list);
        targetScroller.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(targetScroller);
        layout.setConstraints(targetScroller, constraints);

        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        CardLayout cardlayout = (CardLayout) (botspecificcards.getLayout());
        if (e.getSource() == testbot_radiobutton) {
            if (!custom_name) {
                namefield.setText("TestBot");
            }
            cardlayout.show(botspecificcards, TESTBOT_PANEL);

        } else if (e.getSource() == princess_radiobutton) {
            if (!custom_name) {
                namefield.setText("Princess");
            }
            cardlayout.show(botspecificcards, PRINCESS_PANEL);

        } else if (e.getSource() == butOK) {
            dialog_aborted = false;
            setVisible(false);
        } else if (e.getSource() == princess_addtarget_button) {
            princess_targets_list_model.addElement(princess_target_hex_num_x
                    .getText());
        } else if (e.getSource() == princess_shouldflee) {
        	princess_mustflee.setEnabled(princess_shouldflee.isSelected());
        }
        
    }

    public void keyTyped(KeyEvent e) {
        custom_name = true;
    }

    public void keyReleased(KeyEvent e) {
    };

    public void keyPressed(KeyEvent e) {
    };

    /**
     * gets the selected, configured bot from the dialog
     *
     * @param host
     * @param port
     * @return
     */
    BotClient getSelectedBot(String host, int port) {
        if (testbot_radiobutton.isSelected()) {
            return new TestBot(getBotName(), host, port);
        } else if (princess_radiobutton.isSelected()) {
            Princess toreturn = new Princess(getBotName(), host, port, Princess.LogLevel.getLogLevel(princessVerbosity.getSelectedIndex()));
            toreturn.aggression = aggression_slidebar.getValue();
            // Add targets, adjusting hexes appropriately
            for (int i = 0; i < princess_targets_list_model.getSize(); i++) {
                int xpos = Integer.parseInt(((String)princess_targets_list_model.get(i))
                        .substring(0, 2)) - 1;
                int ypos = Integer.parseInt(((String)princess_targets_list_model.get(i))
                        .substring(2, 4)) - 1;
                System.err
                        .println("adding " + Integer.toString(xpos) + " , "
                                + Integer.toString(ypos)
                                + " to strategic targets list");
                toreturn.strategic_targets.add(new Coords(xpos, ypos));
            }
            // do forced withdrawal?
            toreturn.forced_withdrawal = princess_forcedwithdrawal.isSelected();
            toreturn.should_flee = princess_shouldflee.isSelected();
            if (princess_shouldflee.isSelected()) {
            	toreturn.must_flee = princess_mustflee.isSelected();
            }
            toreturn.setHomeEdge(BasicPathRanker.HomeEdge.getHomeEdge(princess_homeedge.getSelectedIndex()));
            return toreturn;
        }
        return null; // shouldn't happen

    }

    String getBotName() {
        return namefield.getText();
    }
}
