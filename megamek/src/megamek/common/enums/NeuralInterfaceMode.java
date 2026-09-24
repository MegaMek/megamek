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
package megamek.common.enums;

import megamek.common.annotations.Nullable;
import megamek.common.options.IGameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

/**
 * How much of the neural interface rules a game is playing with, and what that means for an implant.
 *
 * <p>Whether a warrior's implant does anything is a game option, not a property of the warrior: the
 * same implanted pilot in the same machine is inert in one game and not in another. The option carries
 * three settings, and the difference between the two that are on is whether the machine has to carry
 * the interface hardware as well - so a pilot who is fine under one is inert under the other. The same
 * option is also the only switch for Manei Domini implants of every kind: Off means a warrior may carry
 * none, and either setting that is on allows the whole implant group.</p>
 *
 * <p>The setting is stored as a display string, and reading it was written out separately everywhere it
 * was needed. Both applications and the engine ask this instead, so the bridge from implant to benefit
 * is defined once and the answers cannot drift apart.</p>
 */
public enum NeuralInterfaceMode {
    /** The rules are not in play, and no implant does anything. */
    OFF(OptionsConstants.NEURAL_INTERFACE_MODE_OFF),
    /** The implant alone is enough; the machine needs no interface hardware. */
    PILOT_ABILITIES_ONLY(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY),
    /** The implant works only through interface hardware fitted to the machine. */
    FULL_TRACKING(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING);

    private static final MMLogger LOGGER = MMLogger.create(NeuralInterfaceMode.class);

    private final String optionValue;

    NeuralInterfaceMode(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * @return the value this setting is stored as in the game options
     */
    public String optionValue() {
        return optionValue;
    }

    /**
     * Reads the setting from a game's options.
     *
     * @param gameOptions the options to read, or {@code null} where there is no game to read - a unit
     *                    sitting in a campaign rather than on a board
     *
     * @return the setting in force, or {@link #OFF} when there is nothing to read
     */
    public static NeuralInterfaceMode from(@Nullable IGameOptions gameOptions) {
        if (gameOptions == null) {
            return OFF;
        }
        IOption option = gameOptions.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE);
        return fromOptionValue((option == null) ? null : option.stringValue());
    }

    /**
     * Reads the setting from its stored value.
     *
     * @param value the stored value, or {@code null}
     *
     * @return the matching setting, or {@link #OFF} for anything unrecognised, which keeps an
     *       unreadable setting from switching rules on rather than off
     */
    public static NeuralInterfaceMode fromOptionValue(@Nullable String value) {
        if ((value == null) || value.isBlank()) {
            return OFF;
        }
        String trimmed = value.trim();
        for (NeuralInterfaceMode mode : values()) {
            if (mode.optionValue.equals(trimmed)) {
                return mode;
            }
        }
        LOGGER.warn("Unknown neural interface mode '{}'; defaulting to Off.", trimmed);
        return OFF;
    }

    /**
     * @return {@code true} if the neural interface rules are in play at all
     */
    public boolean isOn() {
        return this != OFF;
    }

    /**
     * Whether a warrior may carry Manei Domini implants at all. The old separate Manei Domini switch was folded
     * into this setting, so the answer is the same as {@link #isOn()}: Off means no implants of any kind, and
     * either setting that is on allows the whole implant group, neural or not.
     *
     * @return {@code true} if Manei Domini implants are allowed in this game
     */
    public boolean allowsImplants() {
        return isOn();
    }

    /**
     * @return {@code true} if an implant only works through interface hardware fitted to the machine,
     *       which is what separates the two settings that are on
     */
    public boolean requiresInterfaceHardware() {
        return this == FULL_TRACKING;
    }

    /**
     * Whether an implant is doing anything, which is the whole of the bridge from carrying one to
     * gaining by it.
     *
     * @param hasImplant          whether the warrior carries the implant
     * @param hasWorkingInterface whether the machine carries interface hardware that is not shut down
     *
     * @return {@code true} if the implant is providing its benefit
     */
    public boolean grantsBenefit(boolean hasImplant, boolean hasWorkingInterface) {
        if (!isOn() || !hasImplant) {
            return false;
        }
        return !requiresInterfaceHardware() || hasWorkingInterface;
    }
}
