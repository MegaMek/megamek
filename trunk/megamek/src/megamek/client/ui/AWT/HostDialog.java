package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import megamek.client.ui.Messages;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

/**
 * here's a quick class for the host new game diaglogue box
 */
public class HostDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -4493711407007393414L;
    public String name;
    public String serverPass;
    public int port;
    public boolean register;
    public String metaserver;
    public int goalPlayers;

    protected Label yourNameL, serverPassL, portL;
    protected TextField yourNameF, serverPassF, portF;
    protected Checkbox registerC;
    protected Label metaserverL;
    protected TextField metaserverF;
    protected Label goalL;
    protected TextField goalF;
    protected Button okayB, cancelB;

    public HostDialog(Frame frame) {
        super(frame, Messages.getString("MegaMek.HostDialog.title"), true); //$NON-NLS-1$

        yourNameL = new Label(
                Messages.getString("MegaMek.yourNameL"), Label.RIGHT); //$NON-NLS-1$
        serverPassL = new Label(
                Messages.getString("MegaMek.serverPassL"), Label.RIGHT); //$NON-NLS-1$
        portL = new Label(Messages.getString("MegaMek.portL"), Label.RIGHT); //$NON-NLS-1$

        yourNameF = new TextField(PreferenceManager.getClientPreferences()
                .getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverPassF = new TextField(PreferenceManager.getClientPreferences()
                .getLastServerPass(), 16);
        serverPassF.addActionListener(this);
        portF = new TextField(PreferenceManager.getClientPreferences()
                .getLastServerPort()
                + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);

        IClientPreferences cs = PreferenceManager.getClientPreferences();
        metaserver = cs.getMetaServerName();
        metaserverL = new Label(
                Messages.getString("MegaMek.metaserverL"), Label.RIGHT); //$NON-NLS-1$
        metaserverF = new TextField(metaserver);
        metaserverL.setEnabled(register);
        metaserverF.setEnabled(register);

        int goalNumber = cs.getGoalPlayers();
        goalL = new Label(Messages.getString("MegaMek.goalL"), Label.RIGHT); //$NON-NLS-1$
        goalF = new TextField(Integer.toString(goalNumber), 2);
        goalL.setEnabled(register);
        goalF.setEnabled(register);

        registerC = new Checkbox(Messages.getString("MegaMek.registerC")); //$NON-NLS-1$
        register = false;
        registerC.setState(register);
        registerC.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                boolean state = false;
                if (ItemEvent.SELECTED == event.getStateChange()) {
                    state = true;
                }
                metaserverL.setEnabled(state);
                metaserverF.setEnabled(state);
                goalL.setEnabled(state);
                goalF.setEnabled(state);
            }
        });

        okayB = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);

        cancelB = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        cancelB.setActionCommand("cancel"); //$NON-NLS-1$
        cancelB.addActionListener(this);
        cancelB.setSize(80, 24);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        add(yourNameL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        add(yourNameF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(serverPassL, c);
        add(serverPassL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverPassF, c);
        add(serverPassF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        add(portL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        add(portF);

        /***********************************************************************
         * WORK IN PROGRESS c.gridwidth = GridBagConstraints.REMAINDER; c.anchor =
         * GridBagConstraints.WEST; gridbag.setConstraints(registerC, c);
         * add(registerC); c.gridwidth = 1; c.anchor = GridBagConstraints.EAST;
         * gridbag.setConstraints(metaserverL, c); add(metaserverL); c.gridwidth =
         * GridBagConstraints.REMAINDER; c.anchor = GridBagConstraints.WEST;
         * gridbag.setConstraints(metaserverF, c); add(metaserverF); c.gridwidth =
         * 1; c.anchor = GridBagConstraints.EAST; gridbag.setConstraints(goalL,
         * c); add(goalL); c.gridwidth = GridBagConstraints.REMAINDER; c.anchor =
         * GridBagConstraints.WEST; gridbag.setConstraints(goalF, c);
         * add(goalF); /* WORK IN PROGRESS
         **********************************************************************/

        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(okayB, c);
        add(okayB);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancelB, c);
        add(cancelB);

        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            try {
                name = yourNameF.getText();
                serverPass = serverPassF.getText();
                register = registerC.getState();
                metaserver = metaserverF.getText();
                port = Integer.parseInt(portF.getText());
                goalPlayers = Integer.parseInt(goalF.getText());
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
                port = 2346;
                goalPlayers = 2;
            }

            // update settings
            PreferenceManager.getClientPreferences().setLastPlayerName(name);
            PreferenceManager.getClientPreferences().setLastServerPass(
                    serverPass);
            PreferenceManager.getClientPreferences().setLastServerPort(port);
            PreferenceManager.getClientPreferences().setValue(
                    "megamek.megamek.metaservername", //$NON-NLS-1$
                    metaserver);
            PreferenceManager.getClientPreferences().setValue(
                    "megamek.megamek.goalplayers", //$NON-NLS-1$
                    Integer.toString(goalPlayers));
        }
        setVisible(false);
    }
}
