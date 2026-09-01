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
package megamek.common.equipment;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * How a control point is fought over and what it is worth: the scoring scheme of an {@link ObjectiveMarker}.
 *
 * <P>A scheme is one of five presets. {@code STANDARD} and {@code RAID} are the two printed-rules schemes:
 * standard control scoring (the per-turn friendly-plus-enemy pairing) and Objective Raid end-scoring. The other
 * three are labeled mission options beyond the printed rules, each built on a per-point counter:</P>
 *
 * <UL>
 * <LI>{@code HOLD} - the point is secured by a side that holds it for a number of turns, counted consecutively
 * (losing the point resets the count) or cumulatively (held turns add up across interruptions), chosen at game
 * setup.</LI>
 * <LI>{@code DEFEND} - the point starts secured by its owner with a grip value; each End Phase an enemy unit is
 * present in the zone drains the grip, and at zero or below the point falls to the draining side.</LI>
 * <LI>{@code CAPTURE} - a progress meter climbs while an enemy of the owner holds the zone and is pushed back
 * while the owner holds it; at the capture threshold the point is captured.</LI>
 * </UL>
 *
 * <P>Securing, falling or capturing a point awards the marker's victory point value once to the side that
 * achieved it. The scheme object also carries the point's counter state during a game, so it travels with the
 * marker; the state is reset when the game resets to the lobby.</P>
 *
 * <P>This is the bot-facing summary of a point as well: {@link #isDecided()}, {@link #getSecuredTeam()},
 * {@link #getSecuredPlayerId()} and {@link #remainingToDecide(int, int)} tell an AI what the point wants without
 * re-deriving the mission.</P>
 */
public class ObjectiveScoringScheme implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The value of {@link #getSecuredTeam()} / {@link #getSecuredPlayerId()} while no side has decided the point. */
    public static final int NO_SIDE = -1;

    /** The five scoring presets a control point can use. */
    public enum SchemePreset {
        /** Printed rules: standard control scoring, the per-turn friendly-plus-enemy pairing (the default). */
        STANDARD,
        /** Printed rules: Objective Raid - the point awards its value once, to its controller at game end. */
        RAID,
        /** Mission option: hold the point for a number of turns to secure it. */
        HOLD,
        /** Mission option: the owner's grip on the point is drained by enemy presence; at zero it falls. */
        DEFEND,
        /** Mission option: a progress meter contested by both sides; at the threshold the point is captured. */
        CAPTURE
    }

    /** How the {@code HOLD} preset counts held turns; the player chooses this at game setup. */
    public enum HoldCounting {
        /** Losing the point resets the count - an unbroken run is required (the default). */
        CONSECUTIVE,
        /** Held turns add up across interruptions - losing the point only pauses the count. */
        CUMULATIVE
    }

    private SchemePreset preset = SchemePreset.STANDARD;
    private int threshold = 10;
    private int ratePerTurn = 1;
    private HoldCounting holdCounting = HoldCounting.CONSECUTIVE;

    // --- counter state during a game (travels with the marker, reset on a game reset) ---
    private int securedTeam = NO_SIDE;
    private int securedPlayerId = NO_SIDE;
    private boolean victoryPointsAwarded = false;
    private int defendGrip = 0;
    private boolean defendGripInitialized = false;
    private boolean retainsControlWhenEmpty = false;
    private Map<Integer, Integer> heldTurnsByTeam = new HashMap<>();
    private Map<Integer, Integer> heldTurnsByPlayer = new HashMap<>();
    private Map<Integer, Integer> captureProgressByTeam = new HashMap<>();
    private Map<Integer, Integer> captureProgressByPlayer = new HashMap<>();

    /** @return a standard control scoring scheme, the printed-rules default */
    public static ObjectiveScoringScheme standard() {
        return new ObjectiveScoringScheme();
    }

    /** @return an Objective Raid scheme (printed rules): the point end-scores its value to its final controller */
    public static ObjectiveScoringScheme raid() {
        ObjectiveScoringScheme scheme = new ObjectiveScoringScheme();
        scheme.preset = SchemePreset.RAID;
        return scheme;
    }

    /**
     * @param turnsToSecure the number of held turns that secures the point
     * @param counting      whether the turns must be consecutive or merely add up
     *
     * @return a Hold scheme: hold the point for the given number of turns to secure it
     */
    public static ObjectiveScoringScheme hold(int turnsToSecure, HoldCounting counting) {
        ObjectiveScoringScheme scheme = new ObjectiveScoringScheme();
        scheme.preset = SchemePreset.HOLD;
        scheme.threshold = turnsToSecure;
        scheme.holdCounting = counting;
        return scheme;
    }

    /**
     * @param startingGrip the grip value the owner starts with
     * @param drainPerTurn how much grip each End Phase with an enemy present drains
     *
     * @return a Defend scheme: the point falls when enemy presence has drained the owner's grip to zero or below
     */
    public static ObjectiveScoringScheme defend(int startingGrip, int drainPerTurn) {
        ObjectiveScoringScheme scheme = new ObjectiveScoringScheme();
        scheme.preset = SchemePreset.DEFEND;
        scheme.threshold = startingGrip;
        scheme.ratePerTurn = drainPerTurn;
        return scheme;
    }

    /**
     * @param pointsToCapture the progress needed to capture the point
     * @param progressPerTurn how much progress a controlling turn adds (or, for the owner, pushes back)
     *
     * @return a Capture scheme: a contested progress meter; at the threshold the point is captured
     */
    public static ObjectiveScoringScheme capture(int pointsToCapture, int progressPerTurn) {
        ObjectiveScoringScheme scheme = new ObjectiveScoringScheme();
        scheme.preset = SchemePreset.CAPTURE;
        scheme.threshold = pointsToCapture;
        scheme.ratePerTurn = progressPerTurn;
        return scheme;
    }

    public SchemePreset getPreset() {
        return preset;
    }

    public void setPreset(SchemePreset preset) {
        this.preset = preset;
    }

    /**
     * @return the scheme's deciding number: held turns to secure ({@code HOLD}), the starting grip
     *       ({@code DEFEND}) or the capture progress needed ({@code CAPTURE}); unused by the printed-rules presets
     */
    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /** @return the per-turn rate: grip drained ({@code DEFEND}) or progress added ({@code CAPTURE}) each End Phase */
    public int getRatePerTurn() {
        return ratePerTurn;
    }

    public void setRatePerTurn(int ratePerTurn) {
        this.ratePerTurn = ratePerTurn;
    }

    /** @return how the {@code HOLD} preset counts held turns */
    public HoldCounting getHoldCounting() {
        return holdCounting;
    }

    public void setHoldCounting(HoldCounting holdCounting) {
        this.holdCounting = holdCounting;
    }

    /** @return {@code true} once the point is decided - secured, fallen or captured - by some side */
    public boolean isDecided() {
        return (securedTeam != NO_SIDE) || (securedPlayerId != NO_SIDE);
    }

    /** @return the team that decided the point, or {@link #NO_SIDE} (see {@link #getSecuredPlayerId()}) */
    public int getSecuredTeam() {
        return securedTeam;
    }

    /** @return the unteamed player that decided the point, or {@link #NO_SIDE} */
    public int getSecuredPlayerId() {
        return securedPlayerId;
    }

    /**
     * Marks the point decided by the given side. Exactly one of the two values should be set; the other is
     * {@link #NO_SIDE}.
     *
     * @param team     the deciding team, or {@link #NO_SIDE}
     * @param playerId the deciding unteamed player, or {@link #NO_SIDE}
     */
    public void setSecuredBy(int team, int playerId) {
        this.securedTeam = team;
        this.securedPlayerId = playerId;
    }

    /** @return {@code true} when the one-time victory point award for deciding this point has been made */
    public boolean isVictoryPointsAwarded() {
        return victoryPointsAwarded;
    }

    public void setVictoryPointsAwarded(boolean victoryPointsAwarded) {
        this.victoryPointsAwarded = victoryPointsAwarded;
    }

    /**
     * @return the owner's remaining grip on a {@code DEFEND} point; initialized to the starting grip on first
     *       use. The grip can be drained below zero - a plain sentinel would mistake that for uninitialized.
     */
    public int getDefendGrip() {
        if (!defendGripInitialized) {
            defendGrip = threshold;
            defendGripInitialized = true;
        }
        return defendGrip;
    }

    public void setDefendGrip(int defendGrip) {
        this.defendGrip = defendGrip;
        this.defendGripInitialized = true;
    }

    /**
     * @param team     the side's team, or {@link #NO_SIDE} for an unteamed player
     * @param playerId the side's player when unteamed, otherwise ignored
     *
     * @return the held-turn count ({@code HOLD}) of the given side
     */
    public int getHeldTurns(int team, int playerId) {
        return sideValue(heldTurnsByTeam(), heldTurnsByPlayer(), team, playerId);
    }

    /** Sets the held-turn count of the given side (see {@link #getHeldTurns(int, int)}). */
    public void setHeldTurns(int team, int playerId, int turns) {
        setSideValue(heldTurnsByTeam(), heldTurnsByPlayer(), team, playerId, turns);
    }

    /** @return the highest held-turn count any side has on this point ({@code HOLD}) */
    public int bestHeldTurns() {
        int best = 0;
        for (int turns : heldTurnsByTeam().values()) {
            best = Math.max(best, turns);
        }
        for (int turns : heldTurnsByPlayer().values()) {
            best = Math.max(best, turns);
        }
        return best;
    }

    /** Resets every side's held-turn count to zero - the consecutive-counting reaction to a lost point. */
    public void resetAllHeldTurns() {
        heldTurnsByTeam().clear();
        heldTurnsByPlayer().clear();
    }

    /**
     * @param team     the side's team, or {@link #NO_SIDE} for an unteamed player
     * @param playerId the side's player when unteamed, otherwise ignored
     *
     * @return the capture progress ({@code CAPTURE}) of the given side
     */
    public int getCaptureProgress(int team, int playerId) {
        return sideValue(captureProgressByTeam(), captureProgressByPlayer(), team, playerId);
    }

    /** Sets the capture progress of the given side (see {@link #getCaptureProgress(int, int)}). */
    public void setCaptureProgress(int team, int playerId, int progress) {
        setSideValue(captureProgressByTeam(), captureProgressByPlayer(), team, playerId, progress);
    }

    /** @return the highest capture progress any side has on this point ({@code CAPTURE}) */
    public int bestCaptureProgress() {
        int best = 0;
        for (int progress : captureProgressByTeam().values()) {
            best = Math.max(best, progress);
        }
        for (int progress : captureProgressByPlayer().values()) {
            best = Math.max(best, progress);
        }
        return best;
    }

    /**
     * Pushes every side's capture progress back by the given amount, to a minimum of zero - the owner holding a
     * {@code CAPTURE} point undoes the attackers' progress.
     *
     * @param amount the progress removed from every side
     */
    public void pushBackAllCaptureProgress(int amount) {
        captureProgressByTeam().replaceAll((team, progress) -> Math.max(0, progress - amount));
        captureProgressByPlayer().replaceAll((player, progress) -> Math.max(0, progress - amount));
    }

    /**
     * The bot-facing distance to the point's decision for the given side: turns still to hold ({@code HOLD}),
     * grip still to drain ({@code DEFEND}, for the attacker) or progress still needed ({@code CAPTURE}).
     *
     * @param team     the side's team, or {@link #NO_SIDE} for an unteamed player
     * @param playerId the side's player when unteamed, otherwise ignored
     *
     * @return the remaining amount, 0 when the point is already decided, or -1 when the preset has no counter
     *       (the printed-rules presets re-evaluate control every turn)
     */
    public int remainingToDecide(int team, int playerId) {
        if (isDecided()) {
            return 0;
        }
        return switch (preset) {
            case HOLD -> Math.max(0, threshold - getHeldTurns(team, playerId));
            case DEFEND -> Math.max(0, getDefendGrip());
            case CAPTURE -> Math.max(0, threshold - getCaptureProgress(team, playerId));
            case STANDARD, RAID -> -1;
        };
    }

    /** Clears the in-game counter state, for a game that resets back to the lobby. The setup values remain. */
    public void resetState() {
        securedTeam = NO_SIDE;
        securedPlayerId = NO_SIDE;
        victoryPointsAwarded = false;
        defendGrip = 0;
        defendGripInitialized = false;
        heldTurnsByTeam().clear();
        heldTurnsByPlayer().clear();
        captureProgressByTeam().clear();
        captureProgressByPlayer().clear();
    }

    // Lazy accessors: a marker deserialized from a stream written before a map existed restores it as null,
    // because Java deserialization skips field initializers.
    private Map<Integer, Integer> heldTurnsByTeam() {
        if (heldTurnsByTeam == null) {
            heldTurnsByTeam = new HashMap<>();
        }
        return heldTurnsByTeam;
    }

    private Map<Integer, Integer> heldTurnsByPlayer() {
        if (heldTurnsByPlayer == null) {
            heldTurnsByPlayer = new HashMap<>();
        }
        return heldTurnsByPlayer;
    }

    private Map<Integer, Integer> captureProgressByTeam() {
        if (captureProgressByTeam == null) {
            captureProgressByTeam = new HashMap<>();
        }
        return captureProgressByTeam;
    }

    private Map<Integer, Integer> captureProgressByPlayer() {
        if (captureProgressByPlayer == null) {
            captureProgressByPlayer = new HashMap<>();
        }
        return captureProgressByPlayer;
    }

    private int sideValue(Map<Integer, Integer> byTeam, Map<Integer, Integer> byPlayer, int team, int playerId) {
        if (team != NO_SIDE) {
            return byTeam.getOrDefault(team, 0);
        }
        return byPlayer.getOrDefault(playerId, 0);
    }

    private void setSideValue(Map<Integer, Integer> byTeam, Map<Integer, Integer> byPlayer, int team, int playerId,
          int value) {
        if (team != NO_SIDE) {
            byTeam.put(team, value);
        } else {
            byPlayer.put(playerId, value);
        }
    }

    /**
     * Whether this point keeps its last controller after the zone empties. Off by default, which is how
     * control has always worked: a point nobody stands in goes neutral at the next End Phase. On, the point
     * stays with whoever last held it until another side takes it, so a mission can use more control points
     * than either side has units to garrison - and so a point can begin the game already held.
     *
     * @return {@code true} when control survives an empty zone
     */
    public boolean retainsControlWhenEmpty() {
        return retainsControlWhenEmpty;
    }

    public void setRetainsControlWhenEmpty(boolean retainsControlWhenEmpty) {
        this.retainsControlWhenEmpty = retainsControlWhenEmpty;
    }
}
