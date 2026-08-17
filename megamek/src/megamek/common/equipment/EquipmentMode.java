/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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


package megamek.common.equipment;

import java.util.Hashtable;
import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.weapons.Weapon;

/**
 * <p>
 * Class EquipmentMode describes a Equipment's particular mode.
 * <p>
 * The <code>getDisplayableName</code> method allows you to obtain the localized string from a predefined
 * <code>ResourceBundle</code>.
 * <p>
 * The <code>equals</code> function allows to check if the mode is equivalent to the mode identified by the given name.
 * <p>
 * There is no way to create the instance of the <code>EquipmentMode</code> directly, use
 * <code>EquipmentMode#getMode</code> instead.
 *
 * @see EquipmentType
 * @see Mounted
 */
public class EquipmentMode {

    /**
     * Hash of all modes
     */
    protected static Hashtable<String, EquipmentMode> modesHash = new Hashtable<>();

    /**
     * Bundle key suffix for the label used when a mode describes what the equipment currently is.
     */
    private static final String STATE_NAME_SUFFIX = ".state";

    /**
     * Bundle key suffix for the label used when a mode is offered as a change the player can select.
     */
    private static final String ACTION_NAME_SUFFIX = ".action";

    /**
     * Unique internal mode identifier. Used as the part of the key to look for the displayable name presented to user.
     */
    protected String name;

    /**
     * <p>
     * Protected constructor since we don't allow direct creation of the mode. Modes available via <code>getMode</code>
     * <p>
     * Constructs the new mode denoted by the given name.
     *
     * @param name unique mode identifier
     */
    protected EquipmentMode(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * @return mode name/identifier
     */
    public String getName() {
        return name;
    }

    /**
     * @return the localized displayable name presented by the GUI to the user.
     */
    public String getDisplayableName() {
        return getDisplayableName(false);
    }

    /**
     * @return the localized displayable name presented by the GUI to the user.
     */
    public String getDisplayableName(boolean wantNormal) {
        String result = EquipmentMessages.getString("EquipmentMode." + name);
        if ((result != null) && (!wantNormal || !result.isBlank())) {
            return result;
        }

        if (wantNormal) {
            return EquipmentMessages.getString("EquipmentMode.Normal");
        } else {
            return name;
        }
    }

    /**
     * Returns the label to show when this mode is what the equipment currently is, or is about to become. Some systems
     * read naturally as a command when offered as a choice but not as a description of a state - Enhanced Imaging is
     * selected with "Engage Enhanced Imaging" but, once running, is described as "Enhanced Imaging Engaged".
     *
     * @param equipmentType the equipment the mode belongs to, or {@code null} when the caller has no equipment context
     *
     * @return the state label, falling back to {@link #getDisplayableName()} when no state label is defined
     *
     * @see #getActionName(EquipmentType)
     */
    public String getStateName(@Nullable EquipmentType equipmentType) {
        return getContextualName(equipmentType, STATE_NAME_SUFFIX);
    }

    /**
     * Returns the label to show when this mode is offered to the player as a change they can make, such as an entry in
     * the unit display's mode dropdown.
     *
     * @param equipmentType the equipment the mode belongs to, or {@code null} when the caller has no equipment context
     *
     * @return the action label, falling back to {@link #getDisplayableName()} when no action label is defined
     *
     * @see #getStateName(EquipmentType)
     */
    public String getActionName(@Nullable EquipmentType equipmentType) {
        return getContextualName(equipmentType, ACTION_NAME_SUFFIX);
    }

    /**
     * Looks up an equipment-specific label for this mode. The keys are scoped by equipment because mode names such as
     * {@link Mounted#MODE_OFF} are shared by many unrelated systems and must keep their plain label everywhere they
     * have not been given one of their own.
     *
     * @param equipmentType the equipment the mode belongs to, or {@code null}
     * @param keySuffix     {@link #STATE_NAME_SUFFIX} or {@link #ACTION_NAME_SUFFIX}
     *
     * @return the scoped label, or {@link #getDisplayableName()} when none is defined
     */
    private String getContextualName(@Nullable EquipmentType equipmentType, String keySuffix) {
        if (equipmentType != null) {
            String contextualName = EquipmentMessages.getString(
                  "EquipmentMode." + equipmentType.getInternalName() + "." + name + keySuffix);
            if ((contextualName != null) && !contextualName.isBlank()) {
                return contextualName;
            }
        }
        return getDisplayableName();
    }

    /**
     * @param name mode name
     *
     * @return unique mode that corresponds to the given name
     */
    public static EquipmentMode getMode(String name) {
        return modesHash.computeIfAbsent(name, EquipmentMode::new);
    }

    /**
     * @param modeName The name of the mode to compare with. Overloaded just for {@code String} classes.
     *
     * @return <code>true</code> if this mode equals to the mode denoted by the given name
     */
    @Override
    public boolean equals(Object modeName) {
        if (modeName instanceof EquipmentMode) {
            return this.hashCode() == modeName.hashCode();
        }

        return false;
    }

    /**
     * @param modeName The name of the mode to compare with. Overloaded just for {@code String} classes.
     *
     * @return <code>true</code> if this mode equals to the mode denoted by the given name
     */
    public boolean equals(String modeName) {
        return name.equals(modeName);
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isHeat() {
        return name.equals(Weapon.MODE_FLAMER_HEAT) || name.equals(Weapon.MODE_INDIRECT_HEAT);
    }

    public boolean isIndirect() {
        return name.equals(Weapon.MODE_MISSILE_INDIRECT) || name.equals(Weapon.MODE_INDIRECT_HEAT);
    }

    public boolean isArmed() {
        return name.equalsIgnoreCase("armed");
    }

    public boolean isOff() {
        return name.equalsIgnoreCase("off");
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
