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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.Serial;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;

/**
 * Tells the player who a passed Game Master vote appointed, and how the role is given up again. A plain message box
 * sized itself to the message's longest line, which made it uncomfortably wide; this dialog wraps the message to
 * its width instead, can be resized, and remembers its size and position like the vote dialog it follows.
 */
public class GameMasterAppointedDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(GameMasterAppointedDialog.class);

    /** The name the dialog stores its remembered size and position under. */
    private static final String DIALOG_NAME = "GameMasterAppointedDialog";

    /** The width the dialog opens at before the user has resized it; the height is measured from the wrapped text. */
    private static final int DEFAULT_WIDTH = 480;

    /** The empty border around the message, in unscaled pixels, on each side. */
    private static final int MESSAGE_BORDER = 10;

    public GameMasterAppointedDialog(JFrame parent, String message) {
        // an announcement, not a question: the lobby goes on while it is open
        super(parent, Messages.getString("GameMasterVoteDialog.passed.title"), false);

        JEditorPane messagePane = new JEditorPane("text/html", message);
        messagePane.setEditable(false);
        messagePane.setOpaque(false);
        // honor the component font and colors so the HTML follows the theme instead of rendering black on dark
        messagePane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        messagePane.setFont(UIManager.getFont("Label.font"));
        messagePane.setForeground(UIManager.getColor("Label.foreground"));

        JPanel panMain = new JPanel(new BorderLayout());
        int messageBorder = UIUtil.scaleForGUI(MESSAGE_BORDER);
        panMain.setBorder(BorderFactory.createEmptyBorder(messageBorder, messageBorder, messageBorder,
              messageBorder));
        panMain.add(messagePane, BorderLayout.CENTER);

        JButton butOkay = new JButton(Messages.getString("Okay"));
        butOkay.addActionListener(event -> dispose());
        JPanel panButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panButtons.add(butOkay);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
        getRootPane().setDefaultButton(butOkay);
        getRootPane().registerKeyboardAction(event -> dispose(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        // Packing an HTML message on its own lays it out as one long line, so the message is given the height its
        // text actually needs once wrapped to the dialog width. Packing then sizes the window around that, which
        // keeps the whole message visible however long it is and however the GUI is scaled.
        messagePane.setPreferredSize(wrappedSize(messagePane));
        pack();
        setMinimumSize(getSize());
        // Center on the parent as a first-time default; setPreferences then restores a remembered spot over it.
        setLocationRelativeTo(parent);
        setPreferences();
        growToFitMessage();
    }

    /**
     * Measures how tall the message is once wrapped to the dialog's width. The pane is laid out at that width first,
     * because an HTML pane only reports the height of wrapped text once it has a width to wrap to.
     *
     * @param messagePane The pane holding the message
     *
     * @return The size the message needs: the dialog's text width, and the height the wrapped text occupies
     */
    private static Dimension wrappedSize(JEditorPane messagePane) {
        int textWidth = UIUtil.scaleForGUI(DEFAULT_WIDTH - (2 * MESSAGE_BORDER));
        messagePane.setSize(new Dimension(textWidth, Short.MAX_VALUE));
        return new Dimension(textWidth, messagePane.getPreferredSize().height);
    }

    /**
     * Grows the dialog back to the size the message needs if a remembered size is smaller than that. The dialog used
     * to open at a fixed height that cut off the end of the message, and that too-small size was remembered; without
     * this, anyone who had already seen the dialog would keep the clipped size forever.
     */
    private void growToFitMessage() {
        Dimension needed = getMinimumSize();
        Dimension current = getSize();
        if ((current.width < needed.width) || (current.height < needed.height)) {
            setSize(Math.max(current.width, needed.width), Math.max(current.height, needed.height));
        }
    }

    /**
     * Restores the size and position the dialog was last left at, and keeps them up to date as it is moved and
     * resized. A remembered spot wins over the first-time centering above.
     */
    private void setPreferences() {
        try {
            setName(DIALOG_NAME);
            PreferencesNode preferences = MegaMek.getMMPreferences().forClass(GameMasterAppointedDialog.class);
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            // a dialog that cannot remember where it was is still perfectly usable
            LOGGER.error(ex, "Could not set the preferences of the game master appointed dialog");
        }
    }
}
