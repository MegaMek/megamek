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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.logging.MMLogger;

/**
 * The priority target picker in a window of its own, for a bot that is already playing: opened from the in-game bot
 * Targets menu. The bot settings show the same {@link BotTargetPickerPanel} inline instead.
 */
public class BotTargetPickerDialog extends AbstractButtonDialog {

    private static final MMLogger LOGGER = MMLogger.create(BotTargetPickerDialog.class);

    /** The height of the unit list, in unscaled pixels. */
    private static final int LIST_HEIGHT = 340;

    private final BotTargetPickerPanel pickerPanel;

    /**
     * Opens the picker with the bot's current targets fixed as ticked: their priority can change but they cannot be
     * unticked, since in a running game the bot only takes orders to add a priority target.
     *
     * @param frame             the parent frame
     * @param game              the game as this client sees it
     * @param bot               the bot the orders go to
     * @param currentPriorities the bot priority targets so far, unit ID to target priority
     * @param fixedUnitIds      units that stay ticked
     */
    public BotTargetPickerDialog(JFrame frame, Game game, Player bot, Map<Integer, Integer> currentPriorities,
          Set<Integer> fixedUnitIds) {
        super(frame, "BotTargetPickerDialog", "BotConfigDialog.pickerTitle");
        setTitle(Messages.getString("BotConfigDialog.pickerTitle", bot.getName()));
        pickerPanel = new BotTargetPickerPanel(game, bot, currentPriorities, fixedUnitIds, LIST_HEIGHT);
        initialize();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                pickerPanel.focusSearch();
            }
        });
    }

    /**
     * @return the units picked, unit ID to target priority; enemy convoy units are left out, as the bot adds them
     *       itself
     */
    public Map<Integer, Integer> getPickedPriorities() {
        return pickerPanel.getPickedPriorities();
    }

    @Override
    protected Container createCenterPane() {
        pickerPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8), UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(8)));
        return pickerPanel;
    }

    @Override
    protected void okAction() {
        LOGGER.debug("[PriorityTargets] in-game picker confirmed: {}", pickerPanel.getPickedPriorities());
    }
}
