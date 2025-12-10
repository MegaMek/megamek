/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.overlay;

import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.preference.PreferenceChangeEvent;

/**
 * An overlay for the BoardView that displays a selection of keybinds for the current game situation
 *
 * @author SJuliez
 */
public class KeyBindingsOverlay extends AbstractBoardViewOverlay {

    /** The keybinds to be shown during the firing phases (incl. physical etc.) */
    private static final List<KeyCommandBind> BINDS_FIRE = Arrays.asList(KeyCommandBind.NEXT_WEAPON,
          KeyCommandBind.PREV_WEAPON,
          KeyCommandBind.UNDO_LAST_STEP,
          KeyCommandBind.NEXT_TARGET,
          KeyCommandBind.NEXT_TARGET_VALID,
          KeyCommandBind.NEXT_TARGET_NO_ALLIES,
          KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES,
          KeyCommandBind.PHYS_PUNCH,
          KeyCommandBind.PHYS_KICK,
          KeyCommandBind.PHYS_PUSH,
          KeyCommandBind.DONE_NO_ACTION);

    /** The keybinds to be shown during the movement phase */
    private static final List<KeyCommandBind> BINDS_MOVE = Arrays.asList(KeyCommandBind.MOVE_STEP_FORWARD,
          KeyCommandBind.MOVE_STEP_BACKWARD,
          KeyCommandBind.TURN_LEFT,
          KeyCommandBind.TURN_RIGHT,
          KeyCommandBind.TOGGLE_MOVE_MODE,
          KeyCommandBind.MOVE_BACKUP,
          KeyCommandBind.MOVE_GO_PRONE,
          KeyCommandBind.MOVE_GETUP,
          KeyCommandBind.UNDO_LAST_STEP,
          KeyCommandBind.TOGGLE_CONVERSION_MODE,
          KeyCommandBind.DONE_NO_ACTION);

    /** The keybinds to be shown in all phases during the local player's turn */
    private static final List<KeyCommandBind> BINDS_MY_TURN = Arrays.asList(KeyCommandBind.CANCEL,
          KeyCommandBind.DONE,
          KeyCommandBind.NEXT_UNIT,
          KeyCommandBind.PREV_UNIT,
          KeyCommandBind.CENTER_ON_SELECTED);

    /** The keybinds to be shown in all phases during any player's turn */
    private static final List<KeyCommandBind> BINDS_ANY_TURN = Arrays.asList(KeyCommandBind.TOGGLE_CHAT,
          KeyCommandBind.DRAW_LABELS,
          KeyCommandBind.HEX_COORDS);

    /** The keybinds to be shown in the Board Editor */
    private static final List<KeyCommandBind> BINDS_BOARD_EDITOR = List.of(KeyCommandBind.HEX_COORDS);

    private static final List<String> ADDITIONAL_BINDS = Arrays.asList(Messages.getString(
                "KeyBindingsDisplay.fixedBinds")
          .split("\n"));

    private static final List<String> ADDITIONAL_BINDS_BOARD_EDITOR = Arrays.asList(Messages.getString(
          "KeyBindingsDisplay.fixedBindsBoardEd").split("\n"));

    /**
     * An overlay for the BoardView that displays a selection of keybinds for the current game situation.
     */
    public KeyBindingsOverlay(BoardView boardView) {
        super(boardView, new Font("SansSerif", Font.PLAIN, 13));
    }

    @Override
    protected String getHeaderText() {
        return Messages.getString("KeyBindingsDisplay.heading", KeyCommandBind.getDesc(KeyCommandBind.KEY_BINDS));
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
            result.addAll(ADDITIONAL_BINDS);
        } else {
            // Board Editor
            result.addAll(convertToStrings(BINDS_BOARD_EDITOR));
            result.addAll(ADDITIONAL_BINDS_BOARD_EDITOR);
        }

        return result;
    }

    /** Converts a list of KeyCommandBinds to a list of formatted strings. */
    private List<String> convertToStrings(List<KeyCommandBind> keyCommandBinds) {
        List<String> result = new ArrayList<>();
        for (KeyCommandBind kcb : keyCommandBinds) {
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

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.SHOW_KEYBINDS_OVERLAY)) {
            setVisible((boolean) e.getNewValue());
            scheduleBoardViewRepaint();
        }
        super.preferenceChange(e);
    }
}
