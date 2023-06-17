/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ui.swing.boardview;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.common.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An overlay for the Boardview that displays a selection of keybinds
 * for the current game situation
 *
 * @author SJuliez
 */
public class KeyBindingsOverlay extends AbstractBoardViewOverlay {

    /** The keybinds to be shown during the firing phases (incl. physical etc.) */
    private static final List<KeyCommandBind> BINDS_FIRE = Arrays.asList(
            KeyCommandBind.NEXT_WEAPON,
            KeyCommandBind.PREV_WEAPON,
            KeyCommandBind.UNDO_LAST_STEP,
            KeyCommandBind.NEXT_TARGET,
            KeyCommandBind.NEXT_TARGET_VALID,
            KeyCommandBind.NEXT_TARGET_NOALLIES,
            KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES,
            KeyCommandBind.DONE_NO_ACTION
    );

    /** The keybinds to be shown during the movement phase */
    private static final List<KeyCommandBind> BINDS_MOVE = Arrays.asList(
            KeyCommandBind.TOGGLE_MOVEMODE,
            KeyCommandBind.UNDO_LAST_STEP,
            KeyCommandBind.TOGGLE_CONVERSIONMODE,
            KeyCommandBind.DONE_NO_ACTION
    );

    /** The keybinds to be shown in all phases during the local player's turn */
    private static final List<KeyCommandBind> BINDS_MY_TURN = Arrays.asList(
            KeyCommandBind.CANCEL,
            KeyCommandBind.DONE,
            KeyCommandBind.NEXT_UNIT,
            KeyCommandBind.PREV_UNIT,
            KeyCommandBind.CENTER_ON_SELECTED
    );

    /** The keybinds to be shown in all phases during any player's turn */
    private static final List<KeyCommandBind> BINDS_ANY_TURN = Arrays.asList(
            KeyCommandBind.TOGGLE_CHAT,
            KeyCommandBind.DRAW_LABELS,
            KeyCommandBind.HEX_COORDS
    );

    /** The keybinds to be shown in the Board Editor */
    private static final List<KeyCommandBind> BINDS_BOARD_EDITOR = Arrays.asList(
            KeyCommandBind.HEX_COORDS
    );

    private static final List<String> ADDTL_BINDS = Arrays.asList(
            Messages.getString("KeyBindingsDisplay.fixedBinds").split("\n"));

    private static final List<String> ADDTL_BINDS_BOARD_EDITOR = Arrays.asList(
            Messages.getString("KeyBindingsDisplay.fixedBindsBoardEd").split("\n"));

    /**
     * An overlay for the Boardview that displays a selection of keybinds
     * for the current game situation.
     */
    public KeyBindingsOverlay(Game game, ClientGUI cg) {
        super(game, cg, new Font("SansSerif", Font.PLAIN, 13),
                Messages.getString("KeyBindingsDisplay.heading", KeyCommandBind.getDesc(KeyCommandBind.KEY_BINDS)) );
    }

    /** @return an ArrayList of all text lines to be shown. */
    @Override
    protected List<String> assembleTextLines() {
        List<String> result = new ArrayList<>();
        addHeader(result);

        if (clientGui != null) {
            // In a game, not the Board Editor
            // Most of the keybinds are only active during the local player's turn
            if ((clientGui.getClient() != null) && (clientGui.getClient().isMyTurn())) {
                List<KeyCommandBind> listForPhase = new ArrayList<>();
                switch (currentPhase) {
                    case MOVEMENT:
                        listForPhase = BINDS_MOVE;
                        break;
                    case FIRING:
                    case OFFBOARD:
                    case PHYSICAL:
                        listForPhase = BINDS_FIRE;
                        break;
                    default:
                        break;
                }

                result.addAll(convertToStrings(listForPhase));
                result.addAll(convertToStrings(BINDS_MY_TURN));
            }
            result.addAll(convertToStrings(BINDS_ANY_TURN));
            result.addAll(ADDTL_BINDS);
        } else {
            // Board Editor
            result.addAll(convertToStrings(BINDS_BOARD_EDITOR));
            result.addAll(ADDTL_BINDS_BOARD_EDITOR);
        }

        return result;
    }

    /** Converts a list of KeyCommandBinds to a list of formatted strings. */
    private List<String> convertToStrings(List<KeyCommandBind> kcbs) {
        List<String> result = new ArrayList<>();
        for (KeyCommandBind kcb: kcbs) {
            String label = Messages.getString("KeyBinds.cmdNames." + kcb.cmd);
            String d = KeyCommandBind.getDesc(kcb);
            result.add(label + ": " + d);
        }
        return result;
    }

    @Override
    protected boolean getVisibilityGUIPreference() {
        return GUIP.getShowKeybindsOverlay();
    }

    @Override
    protected int getDistTop(Rectangle clipBounds, int overlayHeight) {
        return 30;
    }

    @Override
    protected int getDistSide(Rectangle clipBounds, int overlayWidth) {
        return 30;
    }
}