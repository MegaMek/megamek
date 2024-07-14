/*
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
package megamek.server.sbf;

import megamek.common.actions.EntityAction;

public abstract class AbstractSBFActionHandler implements SBFActionHandler {

    private final EntityAction action;
    private final SBFGameManager gameManager;
    private boolean isFinished = false;

    public AbstractSBFActionHandler(EntityAction action, SBFGameManager gameManager) {
        this.action = action;
        this.gameManager = gameManager;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void setFinished() {
        isFinished = true;
    }

    @Override
    public EntityAction getAction() {
        return action;
    }

    @Override
    public SBFGameManager gameManager() {
        return gameManager;
    }
}