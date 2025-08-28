/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import java.util.Objects;

import megamek.client.ui.dialogs.unitSelectorDialogs.ConfigurableMekViewPanel;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityNewOffboardEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.game.Game;

class LiveEntityReadoutPanel extends ConfigurableMekViewPanel {

    private final Game game;
    private final int entityId;

    public LiveEntityReadoutPanel(Game game, int entityId) {
        this.game = Objects.requireNonNull(game);
        this.entityId = entityId;
    }

    public void initialize() {
        game.addGameListener(gameListener);
        update();
    }

    private void update() {
        setEntity(game.getEntityFromAllSources(entityId));
    }

    void dispose() {
        game.removeGameListener(gameListener);
    }

    private final GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gameSettingsChange(GameSettingsChangeEvent e) {
            update();
        }

        @Override
        public void gameEntityNew(GameEntityNewEvent e) {
            update();
        }

        @Override
        public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
            update();
        }

        @Override
        public void gameEntityRemove(GameEntityRemoveEvent e) {
            update();
        }

        @Override
        public void gameEntityChange(GameEntityChangeEvent e) {
            update();
        }

        @Override
        public void gameNewAction(GameNewActionEvent e) {
            update();
        }
    };
}
