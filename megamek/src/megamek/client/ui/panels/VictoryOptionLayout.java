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
package megamek.client.ui.panels;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

/**
 * The presentation rules for the victory options, in one place so every dialog that shows them behaves the
 * same way: which number fields are bounded and to what, and which options are only meaningful while their
 * master switch is on.
 * <p>
 * Before this the bounds lived only in the game options pane and the greying nowhere, so the lobby's
 * Victory dialog offered the same settings as unbounded free text - a player could type any number into
 * "Victory points needed to win immediately" - and both dialogs let a victory point threshold be set
 * while Use Objectives was off, where it does nothing. Both dialogs now read these rules.
 */
public final class VictoryOptionLayout {

    /** Largest sensible value for the count of conditions a side must meet. */
    public static final int MAX_CONDITIONS = 100;

    /** Battle Value percentages are percentages. */
    public static final int MAX_PERCENT = 100;

    /** A force ratio is a percentage, but a lopsided one is legitimate. */
    public static final int MAX_RATIO_PERCENT = 10000;

    /** Rounds, kills and victory points all share a generous ceiling. */
    public static final int MAX_COUNT = 10000;

    /** The bounded number options, and the range each accepts. */
    private static final Map<String, int[]> BOUNDS = new LinkedHashMap<>();

    /** Options that do nothing unless their master switch is on, mapped to that switch. */
    private static final Map<String, String> DEPENDS_ON = new LinkedHashMap<>();

    static {
        BOUNDS.put(OptionsConstants.VICTORY_ACHIEVE_CONDITIONS, new int[] { 1, MAX_CONDITIONS });
        BOUNDS.put(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT, new int[] { 1, MAX_PERCENT });
        BOUNDS.put(OptionsConstants.VICTORY_BV_RATIO_PERCENT, new int[] { 1, MAX_RATIO_PERCENT });
        BOUNDS.put(OptionsConstants.VICTORY_GAME_TURN_LIMIT, new int[] { 1, MAX_COUNT });
        BOUNDS.put(OptionsConstants.VICTORY_GAME_KILL_COUNT, new int[] { 1, MAX_COUNT });
        // zero is meaningful for both thresholds: it is how they are switched off
        BOUNDS.put(OptionsConstants.VICTORY_VP_WIN_THRESHOLD, new int[] { 0, MAX_COUNT });
        BOUNDS.put(OptionsConstants.VICTORY_VP_LOSS_THRESHOLD, new int[] { 0, MAX_COUNT });

        DEPENDS_ON.put(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT, OptionsConstants.VICTORY_USE_BV_DESTROYED);
        DEPENDS_ON.put(OptionsConstants.VICTORY_BV_RATIO_PERCENT, OptionsConstants.VICTORY_USE_BV_RATIO);
        DEPENDS_ON.put(OptionsConstants.VICTORY_GAME_TURN_LIMIT, OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT);
        DEPENDS_ON.put(OptionsConstants.VICTORY_GAME_KILL_COUNT, OptionsConstants.VICTORY_USE_KILL_COUNT);
        DEPENDS_ON.put(OptionsConstants.VICTORY_VP_WIN_THRESHOLD, OptionsConstants.VICTORY_USE_OBJECTIVES);
        DEPENDS_ON.put(OptionsConstants.VICTORY_VP_LOSS_THRESHOLD, OptionsConstants.VICTORY_USE_OBJECTIVES);
        DEPENDS_ON.put(OptionsConstants.VICTORY_VP_SUDDEN_DEATH, OptionsConstants.VICTORY_USE_OBJECTIVES);
    }

    private VictoryOptionLayout() {
    }

    /**
     * @param optionName the name of a victory option
     *
     * @return the accepted range as {@code {minimum, maximum}}, or {@code null} when the option is not a
     *       bounded number
     */
    public static @Nullable int[] boundsFor(String optionName) {
        int[] range = BOUNDS.get(optionName);
        return (range == null) ? null : new int[] { range[0], range[1] };
    }

    /**
     * @param optionName the name of a victory option
     *
     * @return {@code true} when this option only takes effect while another option is switched on
     */
    public static boolean isDependent(String optionName) {
        return DEPENDS_ON.containsKey(optionName);
    }

    /**
     * @return every dependent option mapped to the master switch it needs, in display order
     */
    public static Map<String, String> dependencies() {
        return Collections.unmodifiableMap(DEPENDS_ON);
    }

    /**
     * Applies both rules to a set of victory option components: bounded spinners where a number field has a
     * sensible range, and greying out for every option that needs a master switch. Components for options
     * outside the victory group are left alone.
     *
     * @param components every option component on display, in any order
     */
    public static void apply(List<DialogOptionComponentYPanel> components) {
        Map<String, DialogOptionComponentYPanel> byName = new LinkedHashMap<>();
        for (DialogOptionComponentYPanel component : components) {
            byName.put(component.getOption().getName(), component);
        }
        for (DialogOptionComponentYPanel component : components) {
            String optionName = component.getOption().getName();
            int[] range = BOUNDS.get(optionName);
            if (range != null) {
                component.useIntegerSpinner(range[0], range[1]);
            }
            DialogOptionComponentYPanel master = byName.get(DEPENDS_ON.get(optionName));
            if (master != null) {
                component.setEditableWhenSelected(master);
            }
        }
    }
}
