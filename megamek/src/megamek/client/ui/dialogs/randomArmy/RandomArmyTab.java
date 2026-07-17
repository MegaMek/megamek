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

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.common.loaders.MekSummary;
import megamek.common.options.GameOptions;

import java.util.List;

/**
 * This interface is implemented by Tabs that are shown in the Random Army Dialogs which are subclassed from
 * AbstractRandomArmyDialog. While the random army creators have wildly different inputs where a common interface
 * doesn't seem feasible, at least the Tabs that use them have some common behaviour. They can, if they want, use
 * provided GameOptions and a SkillGenerator and when generateMekSummaries() is used, they should provide a unit list as
 * a result.
 */
interface RandomArmyTab {

    // TODO: general improvements?
    // TODO: return ForceDescriptor instead of List<Entity> as the superior class?
    // TODO: allow force gen with skill generation so that skills can influence force balancing (total BV)
    // TODO: Give ForceDescriptor a semblance of an API?
    // TODO: allow other returns (how?) AlphaStrikeElement / SBF Unit are options
    // TODO: tabs might not want to extend JPanel, use composition instead

    /**
     * Makes this Tab use its present parameters to roll up a force and returns this force.
     *
     * @return A list of units generated
     */
    List<MekSummary> generateMekSummaries();

    /**
     * Uses the given game options to adapt this tabs settings.
     * <p>
     * By default, this method does nothing.
     */
    default void setGameOptions(GameOptions gameOptions) {}

    /**
     * Uses the given skill generator for generated units.
     * <p>
     * By default, this method does nothing.
     */
    default void setSkillGenerator(AbstractSkillGenerator skillGenerator) {}
}
