package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;

/**
 * here's a quick class for the host new game diaglogue box
 */
public class HostDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -103094006944170081L;
    public String playerName;
    public String serverPass;
    public int port;
    private boolean register;
    private String metaserver;
    private int goalPlayers;
    private JLabel yourNameL;
    private JLabel serverPassL;
    private JLabel portL;
    JTextField yourNameF;
    private JTextField serverPassF;
    private JTextField portF;
    private JCheckBox registerC;
    JLabel metaserverL;
    JTextField metaserverF;
    JLabel goalL;
    JTextField goalF;
    private JButton okayB;
    private JButton cancelB;

    public HostDialog(JFrame frame) {
        super(frame, Messages.getString("MegaMek.HostDialog.title"), true); //$NON-NLS-1$
        yourNameL = new JLabel(
                Messages.getString("MegaMek.yourNameL"), SwingConstants.RIGHT); //$NON-NLS-1$
        serverPassL = new JLabel(
                Messages.getString("MegaMek.serverPassL"), SwingConstants.RIGHT); //$NON-NLS-1$
        portL = new JLabel(
                Messages.getString("MegaMek.portL"), SwingConstants.RIGHT); //$NON-NLS-1$
        yourNameF = new JTextField(PreferenceManager.getClientPreferences()
                .getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverPassF = new JTextField(PreferenceManager.getClientPreferences()
                .getLastServerPass(), 16);
        serverPassF.addActionListener(this);
        portF = new JTextField(PreferenceManager.getClientPreferences()
                .getLastServerPort()
                + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        metaserver = cs.getMetaServerName();
        metaserverL = new JLabel(
                Messages.getString("MegaMek.metaserverL"), SwingConstants.RIGHT); //$NON-NLS-1$
        metaserverF = new JTextField(metaserver);
        metaserverL.setEnabled(register);
        metaserverF.setEnabled(register);
        int goalNumber = cs.getGoalPlayers();
        goalL = new JLabel(
                Messages.getString("MegaMek.goalL"), SwingConstants.RIGHT); //$NON-NLS-1$
        goalF = new JTextField(Integer.toString(goalNumber), 2);
        goalL.setEnabled(register);
        goalF.setEnabled(register);
        registerC = new JCheckBox(Messages.getString("MegaMek.registerC")); //$NON-NLS-1$
        register = false;
        registerC.setSelected(register);
        registerC.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                boolean state = false;
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    state = true;
                }
                metaserverL.setEnabled(state);
                metaserverF.setEnabled(state);
                goalL.setEnabled(state);
                goalF.setEnabled(state);
            }
        });
        okayB = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);
        cancelB = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        cancelB.setActionCommand("cancel"); //$NON-NLS-1$
        cancelB.addActionListener(this);
        cancelB.setSize(80, 24);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        getContentPane().add(yourNameL);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        getContentPane().add(yourNameF);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(serverPassL, c);
        getContentPane().add(serverPassL);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverPassF, c);
        getContentPane().add(serverPassF);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        getContentPane().add(portL);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        getContentPane().add(portF);

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
        getContentPane().add(okayB);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancelB, c);
        getContentPane().add(cancelB);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (!"cancel".equals(e.getActionCommand())) { //$NON-NLS-1$
            try {
                playerName = yourNameF.getText();
                serverPass = serverPassF.getText();
                register = registerC.isSelected();
                metaserver = metaserverF.getText();
                port = Integer.parseInt(portF.getText());
                goalPlayers = Integer.parseInt(goalF.getText());
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
                port = 2346;
                goalPlayers = 2;
            }

            // update settings
            PreferenceManager.getClientPreferences().setLastPlayerName(
                    playerName);
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
