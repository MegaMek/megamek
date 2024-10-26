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
package megamek.server.trigger;

import megamek.common.IGame;

import java.util.Arrays;
import java.util.List;

/**
 * This Trigger implements a logic OR for all its subtriggers, i.e. it triggers at any moment when
 * at least one of its subtriggers is satisfied.
 */
public class OrTrigger implements Trigger {

    private final List<Trigger> triggers;

    public OrTrigger(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public OrTrigger(Trigger... triggers) {
        this(Arrays.asList(triggers));
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return triggers.stream().anyMatch(trigger -> trigger.isTriggered(game, event));
    }
}
