/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import java.util.Collection;

import megamek.client.ui.util.ViewFormatting;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;

/**
 * The Entity information shown in the unit selector and many other places in MM, MML and MHQ.
 * <p>The information is encoded in a series of classes that implement a common {@link ViewElement} interface, which
 * can format the element in any of the available output formats.
 *
 * <p>
 * Goals for the Entity Readout:
 * <UL>
 * <LI> Should be adaptable to various output formats, currently HTML, plain text and discord, on-the-fly
 * rather than fixed at construction</LI>
 * <LI> Should show information sufficient to recreate the unit in MML (while undamaged)</LI>
 * <LI> Should highlight damaged and destroyed items and critical hits as well as current values (movement)</LI>
 * <LI> Should show current ammo values</LI>
 * <LI> Need not show construction details without gameplay effects such as the maximum armor the unit type could carry
 * or the slot number of an item on a mek</LI>
 * <LI> Should show derived stats when they are difficult or tedious to get (total armor, cost, BV etc.)</LI>
 * <LI> Need not show original values for damaged items unless those are relevant for gameplay</LI>
 * <LI> Should be organized into blocks that can be retrieved individually</LI>
 * </UL>
 * To-Dos:
 * <UL>
 * <LI> Heat sink locations are not displayed</LI>
 * <LI> Output formatting is given at construction time, although most elements can output for any formatting on
 * the fly. This should be changed to always take a formatting parameter when obtaining the readout result.</LI>
 * </UL>
 */
public interface EntityReadout {

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities. Produced output
     * formatted in html.
     *
     * @param entity     The entity to summarize
     * @param showDetail If true, shows individual weapons that make up weapon bays.
     */
    static EntityReadout createReadout(Entity entity, boolean showDetail) {
        return createReadout(entity, showDetail, false);
    }


    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities. Produced output
     * formatted in html.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an equipment-only cost
     *                         for conventional infantry for MekHQ.
     */
    static EntityReadout createReadout(Entity entity, boolean showDetail, boolean useAlternateCost) {
        return createReadout(entity,
              showDetail,
              useAlternateCost,
              entity.getCrew() != null);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an equipment-only cost
     *                         for conventional infantry for MekHQ.
     * @param ignorePilotBV    If true then the BV calculation is done without including the pilot BV modifiers
     */
    static EntityReadout createReadout(Entity entity, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV) {

        if (entity instanceof BattleArmor battleArmor) {
            return new BattleArmorReadout(battleArmor, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof Infantry infantry) {
            return new InfantryReadout(infantry, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof ProtoMek protoMek) {
            return new ProtoMekReadout(protoMek, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof GunEmplacement gunEmplacement) {
            return new GunEmplacementReadout(gunEmplacement, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof FighterSquadron squadron) {
            return new FighterSquadronReadout(squadron, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof Mek mek) {
            return new MekReadout(mek, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof Aero aero) {
            return new AeroReadout(aero, showDetail, useAlternateCost, ignorePilotBV);
        } else if (entity instanceof Tank tank) {
            return new TankReadout(tank, showDetail, useAlternateCost, ignorePilotBV);
        } else {
            // the selection above should be exhaustive, but to be safe:
            return new GeneralEntityReadout(entity, showDetail, useAlternateCost, ignorePilotBV);
        }
    }

    /**
     * @return The formatted basic values section, including movement, system equipment (cockpit, gyro, etc.) and armor.
     */
    String getBasicSection(ViewFormatting formatting);

    /**
     * @return The formatted loadout section, including weapons, ammo, and other equipment.
     */
    String getLoadoutSection(ViewFormatting formatting);

    /**
     * @return The formatted readout with all sections (including fluff texts, if present), using HTML output
     *       formatting.
     */
    default String getFullReadout() {
        return getFullReadout(null, ViewFormatting.HTML);
    }

    /**
     * @return The formatted readout with all sections (including fluff texts, if present), using the given output
     *       formatting
     */
    default String getFullReadout(ViewFormatting formatting) {
        return getFullReadout(null, formatting);
    }

    /**
     * @return The formatted readout with all sections (including fluff texts, if present), using the given font if
     *       applicable and using the given output formatting.
     */
    String getFullReadout(@Nullable String fontName, ViewFormatting formatting);

    /**
     * @return The formatted readout including only the given sections, using the given font if applicable and using the
     *       given output formatting.
     */
    String getReadout(String fontName, ViewFormatting formatting, Collection<ReadoutSections> sectionsToShow);
}
