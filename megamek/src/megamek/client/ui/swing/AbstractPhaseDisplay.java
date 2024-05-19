/*
 * Copyright (c) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.*;
import megamek.common.event.*;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableDelegate;

import java.util.Objects;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

public abstract class AbstractPhaseDisplay extends SkinnedJPanel implements
        BoardViewListener, GameListener, Distractable {

    public static final int DONE_BUTTON_WIDTH = 160;

    protected final MegamekButton butDone;

    private final DistractableDelegate distractableDelegate = new DistractableDelegate();
    private final IClientGUI clientgui;

    protected AbstractPhaseDisplay(IClientGUI cg) {
        this(cg, SkinSpecification.UIComponents.PhaseDisplay.getComp(),
                SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
    }

    protected AbstractPhaseDisplay(IClientGUI cg, String borderSkinComp, String buttonSkinComp) {
        super(borderSkinComp, 0);
        clientgui = Objects.requireNonNull(cg);
        setBorder(new MegamekBorder(borderSkinComp));
        butDone = new MegamekButton("DONE", buttonSkinComp);
        setupDoneButton();
        MegaMekGUI.getKeyDispatcher().registerCommandAction(KeyCommandBind.DONE, this::shouldPerformKeyCommands, this::done);
    }

    private void setupDoneButton() {
        String f = guiScaledFontHTML(UIUtil.uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE)+ "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");
        butDone.addActionListener(e -> done());
    }

    private void done() {
        if (shouldPerformKeyCommands()) {
            // When the turn is ended, we could miss a key release event
            // This will ensure no repeating keys are stuck down
            MegaMekGUI.getKeyDispatcher().stopAllRepeating();
            ready();
        }
    }

    private boolean shouldPerformKeyCommands() {
        return ((clientgui.getClient().isMyTurn()
                || (clientgui.getClient().getGame().getTurn() == null)
                || (clientgui.getClient().getGame().getPhase().isReport())))
                && !clientgui.shouldIgnoreHotKeys()
                && !isIgnoringEvents()
                && isVisible()
                && butDone.isEnabled();
    }

    @Override
    public final boolean isIgnoringEvents() {
        return distractableDelegate.isIgnoringEvents();
    }

    @Override
    public final void setIgnoringEvents(boolean isDistracted) {
        distractableDelegate.setIgnoringEvents(isDistracted);
    }

    public abstract void ready();

    public IClientGUI getClientgui() {
        return clientgui;
    }

    //region Empty BoardListener

    @Override
    public void hexMoused(BoardViewEvent b) { }

    @Override
    public void hexSelected(BoardViewEvent b) { }

    @Override
    public void hexCursor(BoardViewEvent b) { }

    @Override
    public void boardHexHighlighted(BoardViewEvent b) { }

    @Override
    public void firstLOSHex(BoardViewEvent b) { }

    @Override
    public void secondLOSHex(BoardViewEvent b) { }

    @Override
    public void finishedMovingUnits(BoardViewEvent b) { }

    @Override
    public void unitSelected(BoardViewEvent b) { }

    //endregion

    //region Empty GameListener

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) { }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) { }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) { }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) { }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) { }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) { }

    @Override
    public void gameReport(GameReportEvent e) { }

    @Override
    public void gameEnd(GameEndEvent e) { }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) { }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) { }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) { }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) { }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) { }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) { }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) { }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) { }

    @Override
    public void gameNewAction(GameNewActionEvent e) { }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) { }

    @Override
    public void gameVictory(GameVictoryEvent e) { }

    //endregion
}
