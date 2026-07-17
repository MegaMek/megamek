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
 * A gamemaster-set temporary change to a crew's effective skills: a delta to gunnery, a delta to piloting, and a
 * delta to the unit's individual initiative roll, each with a duration of its own, counting down in rounds or
 * permanent. The stored skills are never touched; the deltas are applied when the effective skill is read, so each
 * change reverses itself completely when it expires.
 * <p>
 * Skill deltas are added to the skill number, so a negative delta improves the crew (a -1 makes a 4 gunner a 3) and
 * a positive delta worsens it. The initiative delta is added to the unit's initiative roll, so there a positive
 * delta is the improvement. Held by {@link Crew} and applied inside its effective-skill getters; the per-slot
 * getters stay raw, so unit-list exports and lobby customization always see the crew's real skills.
 * </p>
 * <p>
 * Follows the countdown convention of the taser and magnetic pulse effects: {@link #newRound()} is called once per
 * round from {@link Entity#newRound(int)} and each modifier clears itself when its own counter runs out.
 * </p>
 */
public class TemporarySkillModifiers implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Rounds value meaning a modifier lasts until it is cleared by hand. */
    public static final int PERMANENT = -1;

    /** The effective skill is held to the normal skill range no matter the delta. */
    private static final int MIN_SKILL = 0;

    /* each modifier is a delta and its own duration; a cleared modifier is zero in both */
    private int gunneryDelta = 0;
    private int gunneryRounds = 0;
    private int pilotingDelta = 0;
    private int pilotingRounds = 0;
    private int initiativeDelta = 0;
    private int initiativeRounds = 0;

    /**
     * Sets all three modifiers with one shared duration, replacing any that were active; the /skillMod command's
     * form of a change. A zero delta clears its modifier, and zero rounds clears all three.
     *
     * @param gunneryDelta    added to the gunnery skill number; negative improves the crew
     * @param pilotingDelta   added to the piloting skill number; negative improves the crew
     * @param initiativeDelta added to the unit's individual initiative roll; positive improves the crew
     * @param rounds          how many rounds the modifiers last, or {@link #PERMANENT}
     */
    public void set(int gunneryDelta, int pilotingDelta, int initiativeDelta, int rounds) {
        setGunnery(gunneryDelta, rounds);
        setPiloting(pilotingDelta, rounds);
        setInitiative(initiativeDelta, rounds);
    }

    /**
     * Sets the gunnery modifier alone, replacing an active one. A zero delta or zero rounds clears it.
     *
     * @param delta  added to the gunnery skill number; negative improves the crew
     * @param rounds how many rounds the modifier lasts, or {@link #PERMANENT}
     */
    public void setGunnery(int delta, int rounds) {
        boolean cleared = (delta == 0) || (rounds == 0);
        gunneryDelta = cleared ? 0 : delta;
        gunneryRounds = cleared ? 0 : rounds;
    }

    /**
     * Sets the piloting modifier alone, replacing an active one. A zero delta or zero rounds clears it.
     *
     * @param delta  added to the piloting skill number; negative improves the crew
     * @param rounds how many rounds the modifier lasts, or {@link #PERMANENT}
     */
    public void setPiloting(int delta, int rounds) {
        boolean cleared = (delta == 0) || (rounds == 0);
        pilotingDelta = cleared ? 0 : delta;
        pilotingRounds = cleared ? 0 : rounds;
    }

    /**
     * Sets the initiative modifier alone, replacing an active one. A zero delta or zero rounds clears it.
     *
     * @param delta  added to the unit's individual initiative roll; positive improves the crew
     * @param rounds how many rounds the modifier lasts, or {@link #PERMANENT}
     */
    public void setInitiative(int delta, int rounds) {
        boolean cleared = (delta == 0) || (rounds == 0);
        initiativeDelta = cleared ? 0 : delta;
        initiativeRounds = cleared ? 0 : rounds;
    }

    /** Removes every modifier at once, as when a gamemaster takes back a change before it runs out. */
    public void clear() {
        setGunnery(0, 0);
        setPiloting(0, 0);
        setInitiative(0, 0);
    }

    /** @return {@code true} while any modifier is set, whether counting down or permanent */
    public boolean isActive() {
        return (gunneryRounds != 0) || (pilotingRounds != 0) || (initiativeRounds != 0);
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
        if (delta == 0) {
            return baseSkill;
        }
        return Math.max(MIN_SKILL, Math.min(Crew.MAX_SKILL, baseSkill + delta));
    }

    /** @return the delta to the unit's individual initiative roll; 0 while its modifier is not active */
    public int getInitiativeDelta() {
        return initiativeDelta;
    }

    /** @return the delta to the gunnery skill number; 0 while its modifier is not active */
    public int getGunneryDelta() {
        return gunneryDelta;
    }

    /** @return the delta to the piloting skill number; 0 while its modifier is not active */
    public int getPilotingDelta() {
        return pilotingDelta;
    }

    /** @return rounds the gunnery modifier still lasts: 0 when inactive, {@link #PERMANENT} when it does not expire */
    public int getGunneryRounds() {
        return gunneryRounds;
    }

    /** @return rounds the piloting modifier still lasts: 0 when inactive, {@link #PERMANENT} when it does not expire */
    public int getPilotingRounds() {
        return pilotingRounds;
    }

    /**
     * @return rounds the initiative modifier still lasts: 0 when inactive, {@link #PERMANENT} when it does not
     *       expire
     */
    public int getInitiativeRounds() {
        return initiativeRounds;
    }

    /**
     * Advances every modifier one round and clears each one whose time runs out, following the taser-effect
     * countdown idiom. Permanent modifiers are never counted down. Called from {@link Entity#newRound(int)}, which
     * runs before the round's initiative is rolled, so a modifier set during round N covers the rest of round N
     * plus the initiative rolls of the following {@code rounds - 1} rounds.
     */
    public void newRound() {
        if (gunneryRounds > 0) {
            setGunnery(gunneryDelta, gunneryRounds - 1);
        }
        if (pilotingRounds > 0) {
            setPiloting(pilotingDelta, pilotingRounds - 1);
        }
        if (initiativeRounds > 0) {
            setInitiative(initiativeDelta, initiativeRounds - 1);
        }
    }
}
