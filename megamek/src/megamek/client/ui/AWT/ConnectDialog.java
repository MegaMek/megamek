package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import megamek.client.ui.Messages;
import megamek.common.preference.PreferenceManager;

/**
 * here's a quick class for the connect to game diaglogue box
 */
public class ConnectDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1638137243441415627L;
    public String name, serverAddr;
    public int port;

    protected Label yourNameL, serverAddrL, portL;
    protected TextField yourNameF, serverAddrF, portF;
    protected Button okayB, cancelB;

    public ConnectDialog(Frame frame) {
        super(frame, Messages.getString("MegaMek.ConnectDialog.title"), true); //$NON-NLS-1$

        yourNameL = new Label(
                Messages.getString("MegaMek.yourNameL"), Label.RIGHT); //$NON-NLS-1$
        serverAddrL = new Label(
                Messages.getString("MegaMek.serverAddrL"), Label.RIGHT); //$NON-NLS-1$
        portL = new Label(Messages.getString("MegaMek.portL"), Label.RIGHT); //$NON-NLS-1$

        yourNameF = new TextField(PreferenceManager.getClientPreferences()
                .getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverAddrF = new TextField(PreferenceManager.getClientPreferences()
                .getLastConnectAddr(), 16);
        serverAddrF.addActionListener(this);
        portF = new TextField(PreferenceManager.getClientPreferences()
                .getLastConnectPort()
                + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);

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
        gridbag.setConstraints(serverAddrL, c);
        add(serverAddrL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverAddrF, c);
        add(serverAddrF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        add(portL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        add(portF);

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
                serverAddr = serverAddrF.getText();
                port = Integer.decode(portF.getText().trim()).intValue();

            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }

            // update settings
            PreferenceManager.getClientPreferences().setLastPlayerName(name);
            PreferenceManager.getClientPreferences().setLastConnectAddr(
                    serverAddr);
            PreferenceManager.getClientPreferences().setLastConnectPort(port);
        }
        setVisible(false);
    }
}
