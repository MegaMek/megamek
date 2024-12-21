/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.bot;

import megamek.common.Game;
import megamek.common.enums.GamePhase;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PhaseHandler {

    private final GamePhase phase;
    private final Game game;
    private final Runnable executePhase;

    public PhaseHandler(GamePhase phase, Game game, Runnable executePhase) {
        this.phase = phase;
        this.game = game;
        this.executePhase = executePhase;
    }

    private boolean isPhase(GamePhase phase) {
        return this.phase == phase;
    }

    protected Game getGame() {
        return game;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void execute() {
        if (isPhase(getGame().getPhase())) {
            try {
                this.executePhase.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof PhaseHandler that)) return false;

        return new EqualsBuilder().append(phase, that.phase).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(phase).toHashCode();
    }
}
