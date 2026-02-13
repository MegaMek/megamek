/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serial;
import java.net.InetAddress;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;

/**
 * A JPanel that holds the networking information about the network information of the current session.
 */
public class NetworkInformationDialog extends AbstractButtonDialog implements ActionListener {

    private JFrame frame;

    @Serial
    private static final long serialVersionUID = -4754010220963493049L;

    private final JLabel lblLocalIP = new JLabel(Messages.getString("NetworkInformation.localIP"));
    private JLabel localIP = new JLabel(Messages.getString("NetworkInformation.blankIP"));
    private final JLabel lblRemoteIP = new JLabel(Messages.getString("NetworkInformation.remoteIP"));
    private JLabel remoteIP = new JLabel(Messages.getString("NetworkInformation.blankIP"));
    ;
    private final JLabel lblConnectedIP = new JLabel(Messages.getString("NetworkInformation.connectedIP"));
    private JLabel connectedIP = new JLabel(Messages.getString("NetworkInformation.blankIP"));
    ;
    private final JButton butShowLocalIPs = new JButton(" " + Messages.getString("NetworkInformation.buttonShowIPs"));
    private final JButton butShowRemoteIPs = new JButton(" " + Messages.getString("NetworkInformation.buttonShowIPs"));
    private final JButton butShowHostIPs = new JButton(" " + Messages.getString("NetworkInformation.buttonShowIPs"));
    private final JLabel lblBlank = new JLabel("");
    private ClientGUI clientGui;

    /**
     * Constructs the network overview panel; the given ClientGUI is used to access the game data.
     */
    public NetworkInformationDialog(ClientGUI cg) {
        super(cg.getFrame(), "NetworkInformationDialog", "NetworkInformation.title");
        clientGui = cg;
        init(cg.getFrame());
    }

    public NetworkInformationDialog(JFrame frame) {
        super(frame, "NetworkInformationDialog", "NetworkInformation.title");
        init(frame);
    }

    private void init(JFrame frame) {
        this.frame = frame;
        refresh();
        initialize();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butShowLocalIPs) {
            if (localIP.isVisible()) {
                localIP.setVisible(false);
            } else {
                localIP.setVisible(true);
            }
        }
        if (e.getSource() == butShowRemoteIPs) {
            if (remoteIP.isVisible()) {
                remoteIP.setVisible(false);
            } else {
                remoteIP.setVisible(true);
            }
        }
        if (e.getSource() == butShowHostIPs) {
            if (connectedIP.isVisible()) {
                connectedIP.setVisible(false);
            } else {
                connectedIP.setVisible(true);
            }
        }
    }

    @Override
    protected Container createCenterPane() {
        var mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        refresh();
        localIP.setVisible(false);
        remoteIP.setVisible(false);
        connectedIP.setVisible(false);

        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
        row1.add(lblLocalIP);
        row1.add(Box.createHorizontalStrut(10));
        row1.add(localIP);
        row1.add(Box.createHorizontalGlue());
        row1.add(butShowLocalIPs);

        JPanel row2 = new JPanel();
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
        row2.add(lblRemoteIP);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(remoteIP);
        row2.add(Box.createHorizontalGlue());
        row2.add(butShowRemoteIPs);

        JPanel row3 = new JPanel();
        row3.setLayout(new BoxLayout(row3, BoxLayout.X_AXIS));
        row3.add(lblConnectedIP);
        row3.add(Box.createHorizontalStrut(10));
        row3.add(connectedIP);
        row3.add(Box.createHorizontalGlue());
        row3.add(butShowHostIPs);

        mainPanel.add(row1);
        mainPanel.add(row2);
        mainPanel.add(row3);

        butShowLocalIPs.addActionListener(this);
        butShowHostIPs.addActionListener(this);
        butShowRemoteIPs.addActionListener(this);
        mainPanel.setPreferredSize(new Dimension(400, 300));
        return mainPanel;
    }

    public void refresh() {
        localIP.setText(" " + getIPaddress(true));
        remoteIP.setText(" " + getIPaddress(false));
        if (clientGui != null) {
            connectedIP.setText(" " + clientGui.getClient().getHost());
        }
    }

    private String getIPaddress(boolean localIP) {
        String thisIpAddress = "";

        if (localIP) {
            try {
                InetAddress thisIp = InetAddress.getLocalHost();
                thisIpAddress = thisIp.getHostAddress();
            } catch (Exception e) {
            }
            if (thisIpAddress.length() == 0 || thisIpAddress == null) {
                thisIpAddress = "Could not obtain local IP address";
            }
        } else {
            try {
                URL whatismyip = new URL("http://checkip.amazonaws.com");
                BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
                thisIpAddress = in.readLine();
                in.close();
            } catch (Exception e) {
            }
            if (thisIpAddress.length() == 0 || thisIpAddress == null) {
                thisIpAddress = "Could not obtain public IP address";
            }
        }
        return thisIpAddress;
    }
}
