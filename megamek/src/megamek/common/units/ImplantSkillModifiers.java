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

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

/**
 * The flat gunnery and piloting bonuses a warrior's implants give the unit they are in, summed for display.
 *
 * <p>The engine never changes the skill numbers themselves: a VDNI pilot with Gunnery 4 keeps Gunnery 4 and gets a
 * {@code -1} modifier on every attack. The lobby only ever showed the stored numbers, so a player who switched an
 * implant on saw nothing change and could not tell whether it was doing anything. This gathers the modifiers the
 * engine applies so a display can show the numbers the warrior actually rolls against.</p>
 *
 * <p>The rules mirrored here are the ones the engine applies: the gunnery modifiers in
 * {@code ComputeAttackerToHitMods} and the piloting modifiers in {@link Mek#addEntityBonuses} and
 * {@link Aero#addEntityBonuses}. Vehicles and infantry get the gunnery bonus but no driving or anti-Mek bonus,
 * because the engine grants none. Situational effects such as the Enhanced Imaging aimed-shot and darkness rules are
 * not flat modifiers and are left out. Every bonus is subject to the neural interface game option through
 * {@link Entity#hasActiveDNI()} and {@link Entity#hasActiveEiCockpit()}, so with the option Off nothing is
 * reported.</p>
 *
 * @param gunnery the total gunnery modifier, {@code 0} or negative
 * @param piloting the total piloting modifier, {@code 0} or negative
 * @param sources one entry per implant contributing a bonus, in the order the engine considers them
 */
public record ImplantSkillModifiers(int gunnery, int piloting, List<Source> sources) {

    /** No implant is changing anything. */
    public static final ImplantSkillModifiers NONE = new ImplantSkillModifiers(0, 0, List.of());

    private static final int PROTO_DNI_GUNNERY = -2;
    private static final int PROTO_DNI_PILOTING = -3;
    private static final int DNI_GUNNERY = -1;
    private static final int VDNI_PILOTING = -1;
    private static final int ENHANCED_IMAGING_PILOTING = -1;

    /**
     * One implant's share of the total.
     *
     * @param name the implant's display name
     * @param gunnery its gunnery modifier
     * @param piloting its piloting modifier
     */
    public record Source(String name, int gunnery, int piloting) {}

    /**
     * Gathers the modifiers for a unit.
     *
     * @param entity the unit, or {@code null} for no unit
     *
     * @return the modifiers in force, or {@link #NONE} for no unit, no crew, or no active implant
     */
    public static ImplantSkillModifiers of(@Nullable Entity entity) {
        if ((entity == null) || (entity.getCrew() == null)) {
            return NONE;
        }
        List<Source> sources = new ArrayList<>();
        addDirectNeuralInterface(entity, sources);
        addEnhancedImaging(entity, sources);
        if (sources.isEmpty()) {
            return NONE;
        }
        int gunnery = 0;
        int piloting = 0;
        for (Source source : sources) {
            gunnery += source.gunnery();
            piloting += source.piloting();
        }
        return new ImplantSkillModifiers(gunnery, piloting, List.copyOf(sources));
    }

    /**
     * The DNI family, checked from the strongest down because a warrior with a Buffered VDNI also holds a VDNI, and
     * the engine applies only the first that matches.
     */
    private static void addDirectNeuralInterface(Entity entity, List<Source> sources) {
        if (!entity.hasActiveDNI()) {
            return;
        }
        // Meks and fighters get the piloting side; the engine gives vehicles and infantry none (IO p.71, p.83)
        boolean pilotsMekOrFighter = (entity instanceof Mek) || (entity instanceof Aero);
        if (entity.hasAbility(OptionsConstants.MD_PROTO_DNI)) {
            // Only a Mek's piloting roll carries the Prototype DNI bonus
            int piloting = (entity instanceof Mek) ? PROTO_DNI_PILOTING : 0;
            sources.add(new Source(Messages.getString("WeaponAttackAction.ProtoDni"), PROTO_DNI_GUNNERY, piloting));
        } else if (entity.hasAbility(OptionsConstants.MD_BVDNI)) {
            // Buffered VDNI trades the piloting bonus for protection from feedback ("neuro-lag", IO p.71)
            sources.add(new Source(Messages.getString("WeaponAttackAction.Bvdni"), DNI_GUNNERY, 0));
        } else if (entity.hasAbility(OptionsConstants.MD_VDNI)) {
            int piloting = pilotsMekOrFighter ? VDNI_PILOTING : 0;
            sources.add(new Source(Messages.getString("WeaponAttackAction.Vdni"), DNI_GUNNERY, piloting));
        }
    }

    /** Enhanced Imaging steadies a Mek's piloting rolls; its gunnery effects are situational and not shown. */
    private static void addEnhancedImaging(Entity entity, List<Source> sources) {
        if ((entity instanceof Mek) && entity.hasActiveEiCockpit()) {
            sources.add(new Source(Messages.getString("Compute.EnhancedImaging"), 0, ENHANCED_IMAGING_PILOTING));
        }
    }

    /**
     * @return {@code true} if at least one implant is changing a skill
     */
    public boolean isAny() {
        return (gunnery != 0) || (piloting != 0);
    }
}
