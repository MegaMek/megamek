package megamek.common.rules;

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

public interface RulesManager {

    /**
     * Provides access to the rules governing targeting, line of sight,
     * spotting, and related targeting mechanics.
     *
     * @return the RulesTarget implementation in use
     */
    public RulesTarget getRulesTarget();

    /**
     * Provides access to the rules governing ammunition types,
     * special munitions, and ammo‑related modifiers.
     *
     * @return the RulesAmmo implementation in use
     */
    public RulesAmmo getRulesAmmo();

    /**
     * Provides access to the rules governing C3 networks,
     * C3i, and related command‑and‑control systems.
     *
     * @return the RulesC3 implementation in use
     */
    public RulesC3 getRulesC3();

    /**
     * Provides access to the rules governing artillery fire,
     * indirect fire procedures, scatter, and related mechanics.
     *
     * @return the RulesArtillery implementation in use
     */
    public RulesArtillery getRulesArtillery();

    /**
     * Provides access to the rules governing armor types,
     * armor interactions, and armor‑related modifiers.
     *
     * @return the RulesArmor implementation in use
     */
    public RulesArmor getRulesArmor();

    /**
     * Provides access to general charts, lookup tables,
     * and reference data used by the rules system.
     *
     * @return the RulesCharts implementation in use
     */
    public RulesCharts getRulesCharts();

    /**
     * Provides access to environmental rules such as weather,
     * light conditions, hazards, and battlefield effects.
     *
     * @return the RulesEnvironment implementation in use
     */
    public RulesEnvironment getRulesEnvironment();

    /**
     * Provides access to the rules governing equipment behavior,
     * including special gear, systems, and non‑weapon items.
     *
     * @return the RulesEquipment implementation in use
     */
    public RulesEquipment getRulesEquipment();

    /**
     * Provides access to the rules governing explosions,
     * blast effects, area damage, and related interactions.
     *
     * @return the RulesExplosions implementation in use
     */
    public RulesExplosions getRulesExplosions();

    /**
     * Provides access to core game‑flow rules such as turn structure,
     * phase handling, and global game mechanics.
     *
     * @return the RulesGame implementation in use
     */
    public RulesGame getRulesGame();

    /**
     * Provides access to the rules governing heat generation,
     * dissipation, thresholds, and heat‑related effects.
     *
     * @return the RulesHeat implementation in use
     */
    public RulesHeat getRulesHeat();

    /**
     * Provides access to the rules governing movement,
     * including walking, running, jumping, and movement modifiers.
     *
     * @return the RulesMovement implementation in use
     */
    public RulesMovement getRulesMovement();

    /**
     * Provides access to the rules governing physical attacks,
     * such as punches, kicks, charges, and other melee actions.
     *
     * @return the RulesPhysical implementation in use
     */
    public RulesPhysical getRulesPhysical();

    /**
     * Provides access to the rules governing pilot skill checks,
     * consciousness rolls, and other pilot‑related mechanics.
     *
     * @return the RulesPilot implementation in use
     */
    public RulesPilot getRulesPilot();

    /**
     * Provides access to the rules governing Piloting Skill Rolls (PSRs),
     * including fall checks, stability checks, and related modifiers.
     *
     * @return the RulesPSR implementation in use
     */
    public RulesPSR getRulesPSR();

    /**
     * Provides access to terrain rules, including terrain types,
     * movement effects, cover, and terrain‑based modifiers.
     *
     * @return the RulesTerrain implementation in use
     */
    public RulesTerrain getRulesTerrain();

    /**
     * Provides access to underwater rules, including submerged movement,
     * visibility, pressure effects, and underwater combat.
     *
     * @return the RulesUnderwater implementation in use
     */
    public RulesUnderwater getRulesUnderwater();

    /**
     * Provides access to the rules governing unit types,
     * unit capabilities, and unit‑specific interactions.
     *
     * @return the RulesUnits implementation in use
     */
    public RulesUnits getRulesUnits();

    /**
     * Provides access to the rules governing weapons,
     * weapon attacks, firing modifiers, and damage resolution.
     *
     * @return the RulesWeapons implementation in use
     */
    public RulesWeapons getRulesWeapons();
}
