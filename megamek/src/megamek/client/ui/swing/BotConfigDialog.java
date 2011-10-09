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

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.Messages;

/**
 * BotConfigDialog is a dialog box that configures bot properties
 * TODO add appropriate configuration parameters for each bot
 */
public class BotConfigDialog extends JDialog implements ActionListener, KeyListener {

    /**
     *
     */
    private static final long serialVersionUID = -544663266637225925L;
    private JRadioButton testbot_radiobutton;
    private JRadioButton princess_radiobutton;
    private ButtonGroup selectbot_group=new ButtonGroup();
    
    JComboBox princess_verbosity;
    

    private JTextField namefield;
    private boolean custom_name=false; //did user not use default name?
    public boolean dialog_aborted=true; //did user not click Ok button?

    private JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$

    JPanel botspecificcards;
    
    public BotConfigDialog(JFrame parent) {
        super(parent, "Configure Bot", true);
        super.setResizable(false);

        setLocationRelativeTo(parent);

        butOK.addActionListener(this);

        testbot_radiobutton=new JRadioButton("TestBot");
        testbot_radiobutton.addActionListener(this);
        testbot_radiobutton.setSelected(true);
        selectbot_group.add(testbot_radiobutton);
        princess_radiobutton=new JRadioButton("Princess");
        princess_radiobutton.addActionListener(this);
        selectbot_group.add(princess_radiobutton);

        JPanel namepanel=new JPanel(new FlowLayout());
        namepanel.add(new JLabel("Name: "));
        namefield=new JTextField();
        namefield.setColumns(12);
        namefield.setText("TestBot");
        namefield.addKeyListener(this);
        namepanel.add(namefield);
        
        JPanel testbotconfigcard=new JPanel();
        
        JPanel princessconfigcard=new JPanel(new FlowLayout());
        princessconfigcard.add(new JLabel("Verbosity"));
        String[] verbosity_options={"0","1"};
        princess_verbosity=new JComboBox(verbosity_options);
        princess_verbosity.setSelectedIndex(1);
        princessconfigcard.add(princess_verbosity);
        
        botspecificcards=new JPanel(new CardLayout());
        botspecificcards.add(testbotconfigcard,"testbot_config");
        botspecificcards.add(princessconfigcard,"princess_config");

        JPanel toppanel=new JPanel(new GridLayout(1,0));
        
        JPanel selectbotpanel=new JPanel(new GridLayout(0,1));
        selectbotpanel.add(testbot_radiobutton);
        selectbotpanel.add(princess_radiobutton);
        selectbotpanel.add(namepanel);
        selectbotpanel.add(butOK);
        toppanel.add(selectbotpanel);
        toppanel.add(botspecificcards);
        //add(selectbotpanel);
        add(toppanel);
        pack();
    }

    public void actionPerformed(ActionEvent e) {
    	CardLayout cardlayout = (CardLayout)(botspecificcards.getLayout());
        if(e.getSource()==testbot_radiobutton) {
            if(!custom_name) {
                namefield.setText("TestBot");
            }
            cardlayout.show(botspecificcards, "testbot_config");

        } else if(e.getSource()==princess_radiobutton) {
            if(!custom_name) {
                namefield.setText("Princess");
            }
            cardlayout.show(botspecificcards, "princess_config");

        } else if(e.getSource()==butOK) {
            dialog_aborted=false;
            setVisible(false);
        }
    }

    public void keyTyped(KeyEvent e) {
        custom_name=true;
    }

    public void keyReleased(KeyEvent e) {};
    public void keyPressed(KeyEvent e) {};


    /**
     * gets the selected, configured bot from the dialog
     * @param host
     * @param port
     * @return
     */
    BotClient getSelectedBot(String host,int port) {
        if(testbot_radiobutton.isSelected()) {
            return new TestBot(getBotName(), host, port);
        } else if(princess_radiobutton.isSelected()) {
            Princess toreturn=new Princess(getBotName(),host,port);        	
            toreturn.verbosity=princess_verbosity.getSelectedIndex();
            return toreturn;
        }
        return null;  //shouldn't happen

    }

    String getBotName() {
        return namefield.getText();
    }
}
