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
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.voting.Poll;
import megamek.common.voting.VoteChoice;
import megamek.common.voting.VoteThreshold;
import megamek.logging.MMLogger;

/**
 * The poll a Game Master vote runs in: every voter sees the same dialog with each voter's ballot as the server last
 * shared it, casts their own vote with yes or no and Send, and the requester may withdraw the request. The dialog
 * follows the vote as ballots come in and closes itself when the vote resolves; the outcome is announced in chat.
 * It works the same in the lobby and in play, and does not block either.
 */
public class GameMasterVoteDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(GameMasterVoteDialog.class);

    /** The name the dialog stores its remembered size and position under. */
    private static final String DIALOG_NAME = "GameMasterVoteDialog";

    /** The chat commands the dialog votes and cancels through, so the rules stay with the server commands. */
    private static final String ALLOW_COMMAND = "/allowGM";
    private static final String DENY_COMMAND = "/denyGM";
    private static final String CANCEL_COMMAND = "/cancelGM";

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final Client client;

    private final JLabel headerLabel = new JLabel();
    private final JLabel thresholdLabel = new JLabel();
    /** One row per voter, rebuilt on every update from the server. */
    private final JPanel voterPanel = new JPanel(new GridBagLayout());

    private final JRadioButton radioYes = new JRadioButton(Messages.getString("GameMasterVoteDialog.yes"));
    private final JRadioButton radioNo = new JRadioButton(Messages.getString("GameMasterVoteDialog.no"));
    private final JButton butSend = new JButton(Messages.getString("GameMasterVoteDialog.send"));
    private final JButton butCancel = new JButton(Messages.getString("GameMasterVoteDialog.cancel"));

    /** Whether the local player is the one who called the vote; only the requester may withdraw it. */
    private boolean localPlayerIsRequester = false;

    public GameMasterVoteDialog(JFrame parent, Client client) {
        // the game goes on while the vote runs, so the dialog must not block it
        super(parent, Messages.getString("GameMasterVoteDialog.title"), false);
        this.client = client;
        initComponents();
        // Closing the window is handled by hand so the requester's close withdraws the vote instead of leaving it
        // running on the server with no dialog to end it.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onWindowClosing();
            }
        });
        setMinimumSize(UIUtil.scaleForGUI(300, 150));
        // Center on the parent as a first-time default; setPreferences then restores a remembered spot over it.
        setLocationRelativeTo(parent);
        setPreferences();
    }

    /**
     * Handles the window's close button. The requester withdraws the vote, so it does not sit running on the server
     * with no dialog left to end it; any other voter just hides their copy, which returns on the next ballot.
     */
    private void onWindowClosing() {
        if (localPlayerIsRequester) {
            client.sendChat(CANCEL_COMMAND);
        } else {
            setVisible(false);
        }
    }

    /**
     * Restores the size and position the dialog was last left at, and keeps them up to date as it is moved and
     * resized. A remembered spot wins over the first-time centering above.
     */
    private void setPreferences() {
        try {
            setName(DIALOG_NAME);
            PreferencesNode preferences = MegaMek.getMMPreferences().forClass(GameMasterVoteDialog.class);
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            // a dialog that cannot remember where it was is still perfectly usable
            LOGGER.error(ex, "Could not set the preferences of the game master vote dialog");
        }
    }

    private void initComponents() {
        JPanel panMain = new JPanel();
        panMain.setLayout(new BoxLayout(panMain, BoxLayout.PAGE_AXIS));
        panMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        panMain.add(headerLabel);
        panMain.add(thresholdLabel);
        voterPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panMain.add(voterPanel);

        ButtonGroup voteGroup = new ButtonGroup();
        voteGroup.add(radioYes);
        voteGroup.add(radioNo);
        radioYes.setSelected(true);

        butSend.addActionListener(event -> client.sendChat(radioYes.isSelected() ? ALLOW_COMMAND : DENY_COMMAND));
        butCancel.setToolTipText(Messages.getString("GameMasterVoteDialog.cancel.tooltip"));
        butCancel.addActionListener(event -> client.sendChat(CANCEL_COMMAND));

        JPanel panButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panButtons.add(radioYes);
        panButtons.add(radioNo);
        panButtons.add(butSend);
        panButtons.add(butCancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
    }

    /**
     * Brings the dialog in line with the vote as the server last shared it: the voter rows, and which of the
     * controls the local player may use. The requester withdraws rather than votes, since their yes is what the
     * vote is about; players who are not voters watch without controls.
     *
     * @param poll the vote as the server last shared it
     */
    public void update(Poll poll) {
        Player requester = client.getGame().getPlayer(poll.getRequesterId());
        headerLabel.setText(Messages.getString("GameMasterVoteDialog.header",
              (requester != null) ? requester.getName() : Messages.getString("GameMasterVoteDialog.unknownPlayer")));
        thresholdLabel.setText(Messages.getString((poll.getThreshold() == VoteThreshold.MAJORITY)
              ? "GameMasterVoteDialog.threshold.majority"
              : "GameMasterVoteDialog.threshold.unanimous"));

        voterPanel.removeAll();
        int row = 0;
        for (Map.Entry<Integer, VoteChoice> vote : poll.getVotes().entrySet()) {
            Player voter = client.getGame().getPlayer(vote.getKey());
            String voterName = (voter != null)
                  ? voter.getName()
                  : Messages.getString("GameMasterVoteDialog.unknownPlayer");

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = row;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(2, 5, 2, 25);
            constraints.weightx = 1.0;
            voterPanel.add(new JLabel(voterName), constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.0;
            constraints.insets = new Insets(2, 5, 2, 5);
            voterPanel.add(choiceLabel(vote.getValue()), constraints);
            row++;
        }

        int localPlayerId = client.getLocalPlayer().getId();
        localPlayerIsRequester = localPlayerId == poll.getRequesterId();
        boolean localPlayerVotes = poll.hasVoter(localPlayerId) && !localPlayerIsRequester;
        radioYes.setVisible(localPlayerVotes);
        radioNo.setVisible(localPlayerVotes);
        butSend.setVisible(localPlayerVotes);
        butCancel.setVisible(localPlayerIsRequester);

        pack();
        repaint();
    }

    /** A voter's ballot as a colored label: waiting, yes, or no. */
    private JLabel choiceLabel(VoteChoice choice) {
        JLabel label = new JLabel(Messages.getString(switch (choice) {
            case YES -> "GameMasterVoteDialog.vote.yes";
            case NO -> "GameMasterVoteDialog.vote.no";
            default -> "GameMasterVoteDialog.vote.pending";
        }));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        switch (choice) {
            case YES -> label.setForeground(GUIP.getOkColor());
            case NO -> label.setForeground(GUIP.getWarningColor());
            default -> label.setForeground(Color.GRAY);
        }
        return label;
    }
}
