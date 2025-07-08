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
package megamek.common.universe;

public enum FactionTag {

    /** Inner sphere */
    IS, PERIPHERY, DEEP_PERIPHERY, CLAN,
    /** A bunch of dirty pirates */
    PIRATE,
    /** Major mercenary bands */
    MERC,
    /** Major trading company */
    TRADER,
    /**
     * Super Power: the Terran Hegemony, the First Star League, and the Federated
     * Commonwealth. (CamOps p12)
     */
    SUPER,
    /**
     * Major Power: e.g. Inner Sphere Great Houses, Republic of the Sphere, Terran
     * Alliance,
     * Second Star League, Inner Sphere Clans. (CamOps p12)
     */
    MAJOR,
    /**
     * Faction is limited to a single star system, or potentially just a part of a
     * planet (CamOps p12)
     */
    MINOR,
    /** Independent world or Small State (CamOps p12) */
    SMALL,
    /** Faction is rebelling against the superior ("parent") faction */
    REBEL,
    /**
     * Faction isn't overtly acting on the political/military scale; think ComStar
     * before clan invasion
     */
    INACTIVE,
    /** Faction represents empty space */
    ABANDONED,
    /** Faction represents a lack of unified government */
    CHAOS,
    /** Faction is campaign-specific, generated on the fly */
    GENERATED,
    /** Faction is hidden from view */
    HIDDEN,
    /** Faction code is not intended to be for players */
    SPECIAL,
    /** Faction is meant to be played */
    PLAYABLE,
    /** Faction is an independent noble (Camops p. 39) */
    NOBLE,
    /** Faction is an independent planetary government (Camops p. 39) */
    PLANETARY_GOVERNMENT,
    /** Faction is an independent corporation (Camops p. 39) */
    CORPORATION,
    /** Faction is stingy and tends to pay less for contracts (Camops p. 42) */
    STINGY,
    /** Faction is generous and tends to pay more for contracts (Camops p. 42) */
    GENEROUS,
    /** Faction is controlling with mercenary command rights (Camops p. 42) */
    CONTROLLING,
    /** Faction is lenient with mercenary command rights (Camops p. 42) */
    LENIENT,
    /** Faction performs batchall */
    BATCHALL,
    /**
     * Represents an aggregate of independent 'factions', rather than a singular organization. For example, "PIR"
     * (pirates) is used to abstractly represent all pirates, not individual pirate groups.
     */
    AGGREGATE,
}
