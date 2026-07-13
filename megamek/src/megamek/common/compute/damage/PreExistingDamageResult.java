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
package megamek.common.compute.damage;

import java.util.List;

/**
 * The outcome of one pre-existing damage simulation (First Succession War, p.144). All values are absolute remaining
 * values (not deltas), indexed by unit location, ready to be written into the unit editor dialog's controls.
 *
 * <p>Locations the unit does not use keep the value they had when the simulation started. For fighters, per-location
 * internal structure is unused and {@code structuralIntegrity} carries the remaining SI instead.</p>
 *
 * <p>This record is transient dialog data and is never serialized or stored in game state, so it needs no
 * {@code SerializationHelper} converter.</p>
 *
 * @param armor               remaining front armor by location
 * @param rearArmor           remaining rear armor by location (only meaningful where the unit has rear armor)
 * @param internal            remaining internal structure by location (unused for fighters)
 * @param structuralIntegrity remaining structural integrity (fighters only; 0 for other units)
 * @param critAssignments     the critical hits rolled, one entry per hit
 */
public record PreExistingDamageResult(int[] armor, int[] rearArmor, int[] internal, int structuralIntegrity,
      List<CritAssignment> critAssignments) {
}
