/*
 * Copyright (c) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay;

import java.util.Objects;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.clientGUI.IClientGUI;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekBorder;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.SkinSpecification;
import megamek.client.ui.widget.SkinnedJPanel;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityNewOffboardEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.event.player.GamePlayerConnectedEvent;
import megamek.common.event.player.GamePlayerDisconnectedEvent;
import megamek.common.game.IGame;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableDelegate;
import megamek.logging.MMLogger;

/**
 * This is the base class for all the "displays" which take control during the local player's turn. Only one display is
 * shown at each time; the ChatLounge is also a display. The ChatLounge doesn't show the
 * {@link megamek.client.ui.clientGUI.boardview.BoardView} but most other displays do. Typically, the display itself is
 * the button bar at the bottom of the GUI.
 * <p>
 * Note that a display being active does not mean that it is also the player's turn. The display should allow inspecting
 * units and other actions even when it's another player's turn. Also, even when it's the local player's turn, a unit is
 * not necessarily selected. The unit that is selected to act (if there is one) is not necessarily the same as the one
 * open in the unit display.
 */
public abstract class AbstractPhaseDisplay extends SkinnedJPanel implements
                                                                 BoardViewListener, GameListener, Distractable {

    public static final int DONE_BUTTON_WIDTH = 160;

    protected final MegaMekButton butDone;

    private static final MMLogger LOGGER = MMLogger.create(AbstractPhaseDisplay.class);

    private final DistractableDelegate distractableDelegate = new DistractableDelegate();

    // where the game stood when this display was last muted, to tell on unmuting whether a turn change was dropped
    private GamePhase mutedPhase;
    private int mutedRound = -1;
    private int mutedTurnIndex = -1;

    /** The IClientGUI that this display is a part of. Cannot be null. */
    private final IClientGUI clientGUI;

    /**
     * Creates a phase display using the standard skin settings for phase displays.
     *
     * @param cg The IClientGUI parent of this display
     */
    protected AbstractPhaseDisplay(IClientGUI cg) {
        this(cg,
              SkinSpecification.UIComponents.PhaseDisplay.getComp(),
              SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
    }

    /**
     * Creates a phase display using the given skin settings for the button panel and the buttons.
     *
     * @param cg The IClientGUI parent of this display
     *
     * @see SkinSpecification.UIComponents#getComp()
     */
    protected AbstractPhaseDisplay(IClientGUI cg, String panelSkin, String buttonSkin) {
        super(panelSkin, 0);
        clientGUI = Objects.requireNonNull(cg);
        setBorder(new MegaMekBorder(panelSkin));

        butDone = new MegaMekButton("DONE", buttonSkin);
        String f = UIUtil.fontHTML(UIUtil.uiLightViolet()) + KeyCommandBind.getDesc(KeyCommandBind.DONE) + "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");
        butDone.addActionListener(e -> done());

        MegaMekGUI.getKeyDispatcher().registerCommandAction(KeyCommandBind.DONE, this::shouldPerformDone, this::done);
    }

    private void done() {
        if (shouldPerformDone()) {
            // When the turn is ended, we could miss a key release event
            // This will ensure no repeating keys are stuck down
            MegaMekGUI.getKeyDispatcher().stopAllRepeating();
            ready();
        }
    }

    private boolean shouldPerformDone() {
        return ((clientGUI.getClient().isMyTurn() ||
              (clientGUI.getClient().getGame().getTurn() == null) ||
              (clientGUI.getClient().getGame().getPhase().isReport()))) &&
              !clientGUI.shouldIgnoreHotKeys() &&
              !isIgnoringEvents() &&
              isVisible() &&
              butDone.isEnabled();
    }

    @Override
    public final boolean isIgnoringEvents() {
        return distractableDelegate.isIgnoringEvents();
    }

    /**
     * Mutes or unmutes this display's game events. A muted display drops every turn change; a hex picker for a bot
     * order mutes it while the player clicks the board. When the display is unmuted in the same phase and round and the
     * turn has moved on meanwhile, the current turn is handed to {@link #gameTurnChange} again, so a turn that became
     * the player's while they were picking is not left showing as another player's (issue #9503).
     *
     * @param isDistracted {@code true} to mute game events, {@code false} to hear them again
     */
    @Override
    public final void setIgnoringEvents(boolean isDistracted) {
        boolean wasMuted = distractableDelegate.isIgnoringEvents();
        if (isDistracted == wasMuted) {
            distractableDelegate.setIgnoringEvents(isDistracted);
            return;
        }
        LOGGER.debug("[TurnSync] {} events {} by {}", getClass().getSimpleName(),
              isDistracted ? "muted" : "unmuted", callerName());
        IGame game = clientGUI.getClient().getGame();
        distractableDelegate.setIgnoringEvents(isDistracted);
        if (isDistracted) {
            mutedPhase = game.getPhase();
            mutedRound = game.getCurrentRound();
            mutedTurnIndex = game.getTurnIndex();
        } else if (turnMovedOnWhileMuted(game)) {
            replayCurrentTurn(game);
        }
    }

    /**
     * @return {@code true} if the turn changed since this display was muted, within the same phase and round; a new
     *       phase or round is not a missed turn change, as the display is switched in afresh for it
     */
    private boolean turnMovedOnWhileMuted(IGame game) {
        return (game.getPhase() == mutedPhase)
              && (game.getCurrentRound() == mutedRound)
              && (game.getTurnIndex() != mutedTurnIndex)
              && (game.getTurn() != null);
    }

    /** Hands the current turn to {@link #gameTurnChange}, as the turn change dropped while muted would have. */
    private void replayCurrentTurn(IGame game) {
        int turnPlayerId = game.getTurn().playerId();
        Player turnPlayer = game.getPlayer(turnPlayerId);
        if ((turnPlayer == null) && (turnPlayerId != Player.PLAYER_NONE)) {
            return;
        }
        int turnIndex = game.getTurnIndex();
        int previousPlayerId = (turnIndex > 0) ? game.getTurnsList().get(turnIndex - 1).playerId() : Player.PLAYER_NONE;
        LOGGER.info("[TurnSync] {}: turn {} ({}) arrived while muted; replaying it", getClass().getSimpleName(),
              turnIndex, (turnPlayer == null) ? "no player" : turnPlayer.getName());
        gameTurnChange(new GameTurnChangeEvent(game, turnPlayer, previousPlayerId));
    }

    /** @return the class and method that called {@link #setIgnoringEvents}, for the log */
    private static String callerName() {
        return StackWalker.getInstance()
              .walk(frames -> frames.skip(2)
                    .findFirst()
                    .map(frame -> frame.getClassName() + '.' + frame.getMethodName())
                    .orElse("unknown"));
    }

    /**
     * Tells the display to finish the current player turn and send all planned actions to the server. Planned actions
     * are e.g. movement, attacks or deployment. Usually, the planned actions are all actions that, together, make up a
     * single unit's turn, e.g. all weapon attacks of one unit.
     */
    public abstract void ready();

    public IClientGUI getClientGUI() {
        return clientGUI;
    }

    public MegaMekButton getButDone() {
        return butDone;
    }

    /**
     * Sends a Done packet to the server. Shortcut for clientGUI.getClient().sendDone(true).
     *
     * @see megamek.client.IClient#sendDone(boolean)
     */
    protected void sendDone() {
        clientGUI.getClient().sendDone(true);
    }

    /**
     * @return True when the client determines that it is the local player's turn to act. Shortcut to
     *       clientGUI.getClient().isMyTurn().
     */
    protected boolean isMyTurn() {
        return clientGUI.getClient().isMyTurn();
    }

    //region Empty BoardViewListener

    @Override
    public void hexMoused(BoardViewEvent b) {}

    @Override
    public void hexSelected(BoardViewEvent b) {}

    @Override
    public void hexCursor(BoardViewEvent b) {}

    @Override
    public void boardHexHighlighted(BoardViewEvent b) {}

    @Override
    public void firstLOSHex(BoardViewEvent b) {}

    @Override
    public void secondLOSHex(BoardViewEvent b) {}

    @Override
    public void finishedMovingUnits(BoardViewEvent b) {}

    @Override
    public void unitSelected(BoardViewEvent b) {}

    //endregion

    //region Empty GameListener

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {}

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {}

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {}

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {}

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {}

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {}

    @Override
    public void gameReport(GameReportEvent e) {}

    @Override
    public void gameEnd(GameEndEvent e) {}

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {}

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {}

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {}

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {}

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {}

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {}

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {}

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {}

    @Override
    public void gameNewAction(GameNewActionEvent e) {}

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {}

    @Override
    public void gameVictory(PostGameResolution e) {}

    //endregion
}
