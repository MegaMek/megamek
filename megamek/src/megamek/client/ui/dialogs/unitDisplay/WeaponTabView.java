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
package megamek.client.ui.dialogs.unitDisplay;

import java.util.List;
import java.util.Optional;
import javax.swing.event.ListSelectionListener;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * The Weapon tab of the unit display, as the combat phase displays see it.
 * <p>
 * The firing, targeting, point-blank, pre-phase and physical displays drive the Weapon tab directly: they select
 * weapons, hand it the current target and to-hit, and listen for the player's weapon choice. This interface is exactly
 * that surface, so the phase displays do not depend on one concrete panel and a second Weapon tab can be dropped in
 * behind the same calls.
 */
public interface WeaponTabView {

    /**
     * Shows the given unit's weapons. Replaces the whole list; the selection is cleared.
     *
     * @param entity the unit to show
     */
    void displayMek(Entity entity);

    /**
     * Refreshes the tab for the given unit while keeping the selected weapon. Does nothing for {@code null}.
     *
     * @param entity the unit to refresh
     */
    void updateForEntity(@Nullable Entity entity);

    /**
     * @return the id of the unit whose weapons are shown
     */
    int getSelectedEntityId();

    /**
     * @return the weapon selected in the list, or {@code null} when nothing is selected
     */
    @Nullable
    WeaponMounted getSelectedWeapon();

    /**
     * @return the equipment number of the selected weapon on its unit, or -1 when nothing is selected
     */
    int getSelectedWeaponNum();

    /**
     * @return the ammo chosen for the selected weapon, if any. It may or may not be the ammo the weapon is linked to.
     */
    Optional<AmmoMounted> getSelectedAmmo();

    /**
     * Selects the weapon with the given equipment number; -1 clears the selection.
     *
     * @param weaponNumber the equipment number of the weapon on the displayed unit
     */
    void selectWeapon(int weaponNumber);

    /**
     * Selects the given weapon; {@code null} clears the selection.
     *
     * @param weapon the weapon to select
     */
    void selectWeapon(@Nullable WeaponMounted weapon);

    /** Selects the first weapon that can still fire this turn. */
    void selectFirstWeapon();

    /**
     * Selects the next weapon that can still fire this turn.
     *
     * @return the equipment number of the newly selected weapon, or -1
     */
    int selectNextWeapon();

    /**
     * Selects the previous weapon that can still fire this turn.
     *
     * @return the equipment number of the newly selected weapon, or -1
     */
    int selectPrevWeapon();

    /**
     * @return the equipment number of the next weapon that can still fire, without selecting it, or -1
     */
    int getNextWeaponNum();

    /**
     * @return the next weapon that can still fire, without selecting it, or {@code null}
     */
    @Nullable
    WeaponMounted getNextWeapon();

    /**
     * Shows the target the selected weapon is aimed at.
     *
     * @param target    the target, or {@code null} to show no target
     * @param extraInfo a line shown with the target, such as the aimed location, or {@code null}
     */
    void setTarget(@Nullable Targetable target, @Nullable String extraInfo);

    /**
     * Shows the to-hit for the selected weapon against the current target.
     *
     * @param toHit                  the to-hit, with its itemised modifiers
     * @param naturalAptitudeGunnery whether the attacker has the Natural Aptitude (Gunnery) ability, which changes the
     *                               odds shown
     */
    void setToHit(ToHitData toHit, boolean naturalAptitudeGunnery);

    /**
     * Shows the to-hit for the selected weapon against the current target, for an attacker without the Natural
     * Aptitude (Gunnery) ability.
     *
     * @param toHit the to-hit, with its itemised modifiers
     */
    void setToHit(ToHitData toHit);

    /**
     * Shows a message instead of a to-hit - the weapon has already fired, fires automatically, and so on.
     *
     * @param message the message
     */
    void setToHit(String message);

    /** Clears the to-hit. */
    void clearToHit();

    /**
     * Shows the range to the current target.
     *
     * @param effectiveDistance the range in hexes
     */
    void setRange(int effectiveDistance);

    /**
     * Shows the range as text - for an artillery shot that lands in a later turn, or a target on an unreachable board.
     *
     * @param rangeText the text to show in place of the range
     */
    void setRangeText(String rangeText);

    /** Clears the range to the target. */
    void clearRange();

    /**
     * Hands the tab the attacks declared so far this phase, so it can show them. A tab that does not show declared
     * attacks may ignore this.
     *
     * @param declaredAttacks the attacks declared so far, in declaration order
     */
    void setDeclaredAttacks(List<WeaponAttackAction> declaredAttacks);

    /**
     * Listens for the player's weapon selection. Events from this tab satisfy {@link #isWeaponSelectionSource}.
     *
     * @param listener the listener
     */
    void addWeaponSelectionListener(ListSelectionListener listener);

    /**
     * @param listener the listener to remove
     */
    void removeWeaponSelectionListener(ListSelectionListener listener);

    /**
     * @param eventSource the source of a {@link javax.swing.event.ListSelectionEvent}
     *
     * @return {@code true} if the event came from this tab's weapon selection
     */
    boolean isWeaponSelectionSource(@Nullable Object eventSource);

    /**
     * @return the target remembered before a weapon with a forced target (such as a VGL) was selected, or {@code null}
     */
    @Nullable
    Targetable getPrevTarget();

    /**
     * @param prevTarget the target to restore once a weapon with a forced target is deselected, or {@code null}
     */
    void setPrevTarget(@Nullable Targetable prevTarget);
}
