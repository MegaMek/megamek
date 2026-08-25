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
package megamek.client.bot.princess;

/**
 * What one hex offers a unit standing in it, expressed as rules effects rather than terrain names.
 *
 * <p>Every map is different, so no field here is a list of terrain types. Each is derived from the effect
 * the game's own rules code computes - what the hex does to fire against its occupant, to line of sight,
 * to heat - which is what makes a building read as concealment on a city map and rubble count the same as
 * brush, on maps the authors never saw. The one name-specific field is water's heat sinking, because the
 * rulebook itself is name-specific there.</p>
 *
 * <p>Built once per board per round by {@link HexPropertiesMap}; every use afterward is one map lookup.
 * This is the per-hex record IllianiBird proposed in the #8659 review, absorbing the walkable-bank labels
 * that started as their own structure.</p>
 *
 * @param bank            the walkable-bank region id, {@link BankRegions#WATER} for water hexes
 * @param partialCover    a standing Mek in this hex has partial cover against direct fire
 * @param concealment     the terrain to-hit modifier attacks against the occupant take, 0 for open ground
 * @param concealmentEdge concealing hex with at least one open neighbor: fire out, hard to hit, not blind
 * @param elevation       the hex level
 * @param overlooks       locally dominant height: two or more levels above the surrounding ground
 * @param heatSink        water deep enough to double an occupant's heat dissipation
 */
public record HexProperties(int bank, boolean partialCover, int concealment, boolean concealmentEdge,
      int elevation, boolean overlooks, boolean heatSink) {

    /** What off-board positions report: no bank, no cover, no anything. */
    static final HexProperties NOTHING = new HexProperties(BankRegions.WATER, false, 0, false, 0, false, false);
}
