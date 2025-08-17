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

package megamek.common.autoresolve.acar;

import java.util.Map;

import megamek.client.AbstractClient;
import megamek.client.IClient;
import megamek.common.game.IGame;
import megamek.common.Player;

public class SimulatedClient implements IClient {

    private final IGame game;
    private final Player localPlayer;

    public SimulatedClient(IGame game) {
        this.game = game;
        this.localPlayer = game.getPlayer(0);
    }

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public int getLocalPlayerNumber() {
        return localPlayer.getId();
    }

    @Override
    public Player getLocalPlayer() {
        return localPlayer;
    }


    // The following methods are not used in the context of the Abstract Combat Auto Resolve
    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public boolean isMyTurn() {
        return false;
    }

    @Override
    public void setLocalPlayerNumber(int localPlayerNumber) {}

    @Override
    public Map<String, AbstractClient> getBots() {
        return Map.of();
    }

    @Override
    public void sendDone(boolean done) {}

    @Override
    public void sendChat(String message) {}

    @Override
    public void sendPause() {
        //
    }

    @Override
    public void die() {}
}
