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

package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;

/**
 * A gamemaster-set temporary change to a crew's effective skills: a delta to gunnery and piloting, a delta to the
 * unit's individual initiative roll, and how many rounds the change lasts. The stored skills are never touched; the
 * deltas are applied when the effective skill is read, so the change reverses itself completely when it expires.
 * <p>
 * Skill deltas are added to the skill number, so a negative delta improves the crew (a -1 makes a 4 gunner a 3) and
 * a positive delta worsens it. The initiative delta is added to the unit's initiative roll, so there a positive
 * delta is the improvement. Held by {@link Crew} and applied inside its effective-skill getters; the per-slot
 * getters stay raw, so unit-list exports and lobby customization always see the crew's real skills.
 * </p>
 * <p>
 * Follows the countdown convention of the taser and magnetic pulse effects: {@link #newRound()} is called once per
 * round from {@link Entity#newRound(int)} and the modifiers clear themselves when the counter runs out.
 * </p>
 */
public class TemporarySkillModifiers implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Value for {@link #getRoundsRemaining()} meaning the modifiers last until they are cleared by hand. */
    public static final int PERMANENT = -1;

    /** The effective skill is held to the normal skill range no matter the delta. */
    private static final int MIN_SKILL = 0;

    private int gunneryDelta = 0;
    private int pilotingDelta = 0;
    private int initiativeDelta = 0;
    /** Rounds the modifiers still last: 0 means none are active, {@link #PERMANENT} means they do not expire. */
    private int roundsRemaining = 0;

    /**
     * Sets the modifiers, replacing any that were active. Zero rounds clears them no matter the deltas.
     *
     * @param gunneryDelta    added to the gunnery skill number; negative improves the crew
     * @param pilotingDelta   added to the piloting skill number; negative improves the crew
     * @param initiativeDelta added to the unit's individual initiative roll; positive improves the crew
     * @param rounds          how many rounds the modifiers last, or {@link #PERMANENT}
     */
    public void set(int gunneryDelta, int pilotingDelta, int initiativeDelta, int rounds) {
        if (rounds == 0) {
            clear();
            return;
        }
        this.gunneryDelta = gunneryDelta;
        this.pilotingDelta = pilotingDelta;
        this.initiativeDelta = initiativeDelta;
        roundsRemaining = rounds;
    }

    /** Removes the modifiers at once, as when a gamemaster takes back a change before it runs out. */
    public void clear() {
        gunneryDelta = 0;
        pilotingDelta = 0;
        initiativeDelta = 0;
        roundsRemaining = 0;
    }

    /** @return {@code true} while any modifiers are set, whether counting down or permanent */
    public boolean isActive() {
        return roundsRemaining != 0;
    }

    /**
     * @param baseGunnery the crew's real gunnery skill
     *
     * @return the effective gunnery skill: the base with the delta applied, held to the normal skill range
     */
    public int adjustGunnery(int baseGunnery) {
        return adjustSkill(baseGunnery, gunneryDelta);
    }

    /**
     * @param basePiloting the crew's real piloting skill
     *
     * @return the effective piloting skill: the base with the delta applied, held to the normal skill range
     */
    public int adjustPiloting(int basePiloting) {
        return adjustSkill(basePiloting, pilotingDelta);
    }

    private int adjustSkill(int baseSkill, int delta) {
        if (!isActive() || (delta == 0)) {
            return baseSkill;
        }
        return Math.max(MIN_SKILL, Math.min(Crew.MAX_SKILL, baseSkill + delta));
    }

    /** @return the delta to the unit's individual initiative roll, or 0 while no modifiers are active */
    public int getInitiativeDelta() {
        return isActive() ? initiativeDelta : 0;
    }

    public int getGunneryDelta() {
        return gunneryDelta;
    }

    public int getPilotingDelta() {
        return pilotingDelta;
    }

    /** @return rounds the modifiers still last: 0 when none are active, {@link #PERMANENT} when they do not expire */
    public int getRoundsRemaining() {
        return roundsRemaining;
    }

    /**
     * Advances the modifiers one round and clears them when their time runs out, following the taser-effect
     * countdown idiom. Permanent modifiers are never counted down. Called from {@link Entity#newRound(int)}, which
     * runs before the round's initiative is rolled, so a modifier set during round N covers the rest of round N
     * plus the initiative rolls of the following {@code rounds - 1} rounds.
     */
    public void newRound() {
        if (roundsRemaining > 0) {
            roundsRemaining--;
            if (roundsRemaining == 0) {
                clear();
            }
        }
    }
}
