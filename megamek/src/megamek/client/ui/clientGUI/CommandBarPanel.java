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

package megamek.client.ui.clientGUI;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import megamek.client.Client;
import megamek.client.ui.BugReportMessages;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.BotCommands.BotCommandsPanel;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.HazardButton;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.SkinSpecification;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * The command strip that sits directly under the menu bar, above the board. It always ends in the Commands button,
 * which opens the game commands menu ({@link GameCommandsMenu}), followed by the Report a Bug button, and it is the
 * strip the bot commands panel docks into when the player has it set to Dock.
 *
 * <p>Keeping the Commands button on a strip of its own, rather than on the bot commands panel, means it stays where
 * the player left it no matter how the bot commands panel is set: a player who commands no bots at all, and has the bot
 * panel switched off, still has the game commands one click away.</p>
 *
 * <p>The Report a Bug button is styled as a hazard-striped stop button so that a player who has just watched
 * something go wrong can find it without reading the strip. It lives here rather than in the phase display's Done
 * column because that column is already two buttons deep during the action phases, and a third row there cost board
 * space in every phase.</p>
 */
public class CommandBarPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(CommandBarPanel.class);

    private static final BugReportMessages BUG_REPORT_I18N = new BugReportMessages();

    /** The height of the strip, matching the docked layout of the bot commands panel it sits beside. */
    private static final int BAR_HEIGHT = 40;
    /** Narrower than it was before the Report a Bug button joined it, which is what pays for that button's width. */
    private static final int COMMANDS_BUTTON_WIDTH = 110;
    private static final int REPORT_BUG_BUTTON_WIDTH = 130;
    /** The gap between the two buttons at the end of the strip. */
    private static final int BUTTON_GAP = 2;

    private final ClientGUI clientGUI;
    private final MegaMekButton commandsButton;
    /** The bot commands panel while it is docked in this bar, or {@code null} while it is off or floating. */
    private BotCommandsPanel dockedBotCommandsPanel;

    /**
     * Creates the command strip.
     *
     * @param clientGUI The client GUI the commands are issued from
     */
    public CommandBarPanel(ClientGUI clientGUI) {
        super(new BorderLayout(2, 2));
        this.clientGUI = clientGUI;
        UIUtil.applyTopBarBackground(this);

        commandsButton = new MegaMekButton(Messages.getString("GameCommands.title"),
              SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
        commandsButton.setToolTipText(Messages.getString("GameCommands.tooltip"));
        commandsButton.setPreferredSize(UIUtil.scaleForGUI(COMMANDS_BUTTON_WIDTH, BAR_HEIGHT));
        commandsButton.addActionListener(event -> showCommandsPopup());

        HazardButton reportBugButton = new HazardButton(BUG_REPORT_I18N.get("package.reportBug"));
        reportBugButton.setToolTipText(BUG_REPORT_I18N.get("package.reportBug.tooltip"));
        reportBugButton.setPreferredSize(UIUtil.scaleForGUI(REPORT_BUG_BUTTON_WIDTH, BAR_HEIGHT));
        reportBugButton.addActionListener(event -> BugReportDialog.showWithGameTools(clientGUI.getFrame(),
              this::totalWarfareClient));

        // Commands first, so it keeps the spot it had when it was alone, then Report a Bug on the far right.
        JPanel endOfStrip = new JPanel(new BorderLayout(UIUtil.scaleForGUI(BUTTON_GAP), 0));
        endOfStrip.setOpaque(false);
        endOfStrip.add(commandsButton, BorderLayout.CENTER);
        endOfStrip.add(reportBugButton, BorderLayout.EAST);
        add(endOfStrip, BorderLayout.EAST);
    }

    /**
     * @return the client of the running game when it is one whose save the bug report packager understands, otherwise
     *       {@code null}; a Strategic BattleForce game, for instance, packages its logs alone
     */
    private @Nullable Client totalWarfareClient() {
        return (clientGUI.getClient() instanceof Client totalWarfareClient) ? totalWarfareClient : null;
    }

    /**
     * Builds the game commands menu and shows it under the Commands button. The menu is built on click, so a failure
     * there would otherwise be lost on the event thread and leave the button looking dead; it is logged instead.
     */
    private void showCommandsPopup() {
        try {
            JPopupMenu popup = new GameCommandsMenu(clientGUI).createPopup();
            popup.show(commandsButton, 0, commandsButton.getHeight());
        } catch (Exception exception) {
            LOGGER.error(exception, "[GameCommands] Failed to build the game commands popup");
        }
    }

    /**
     * Docks the bot commands panel into the strip, to the left of the Commands button. Docking the panel that is
     * already docked does nothing, so this can be called whenever the panel's location is refreshed.
     *
     * @param botCommandsPanel The bot commands panel to dock
     */
    public void dockBotCommandsPanel(BotCommandsPanel botCommandsPanel) {
        if (botCommandsPanel.equals(dockedBotCommandsPanel)) {
            return;
        }
        dockedBotCommandsPanel = botCommandsPanel;
        add(botCommandsPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        LOGGER.debug("[GameCommands] bot commands panel docked into the command bar");
    }

    /**
     * Takes the bot commands panel back out of the strip, leaving the Commands button behind. Called when the panel
     * moves to its floating dialog.
     */
    public void undockBotCommandsPanel() {
        if (dockedBotCommandsPanel == null) {
            return;
        }
        remove(dockedBotCommandsPanel);
        dockedBotCommandsPanel = null;
        revalidate();
        repaint();
        LOGGER.debug("[GameCommands] bot commands panel undocked from the command bar");
    }
}
