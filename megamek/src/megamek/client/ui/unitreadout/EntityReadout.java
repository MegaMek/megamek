/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.unitreadout;

import megamek.client.ui.util.ViewFormatting;
import megamek.common.*;
import megamek.common.annotations.Nullable;

/**
 * The Entity information shown in the unit selector and many other places in MM, MML and MHQ.
 *
 * <p>
 * Goals for the Entity Readout:
 * <UL>
 * <LI> It is not bound to official source formatting such as TROs
 * <LI> Should be adaptable to various output formats, currently HTML, plain text and discord
 * <LI> Should show information sufficient to recreate the unit in MML - while undamaged - or fill in a record sheet
 * (omitting elements that are invariable like conversion equipment or directly set by the rules like engine slots)
 * <LI> Should highlight damaged and destroyed items and critical hits as well as current values (movement)
 * <LI> Should show current ammo values
 * <LI> Need not show construction details without gameplay effects such as the maximum armor the unit type could carry
 * or the slot number on a mek
 * <LI> Need not show original values for damaged items unless those are relevant for gameplay
 * <LI> Should be organized into blocks that can be retrieved individually if necessary
 * </UL>
 *
 * <p>
 * The information is encoded in a series of classes that implement a common {@link ViewElement} interface, which can
 * format the element in any of the available output formats.
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
        return createReadout(entity, showDetail, false, ViewFormatting.HTML);
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
        return createReadout(entity, showDetail, useAlternateCost, ViewFormatting.HTML);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an equipment-only cost
     *                         for conventional infantry for MekHQ.
     * @param formatting       Which formatting style to use: HTML, Discord, or None (plaintext)
     */
    static EntityReadout createReadout(Entity entity, boolean showDetail, boolean useAlternateCost,
          ViewFormatting formatting) {
        return createReadout(entity, showDetail, useAlternateCost, (entity.getCrew() == null), formatting);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an equipment-only cost
     *                         for conventional infantry for MekHQ.
     * @param ignorePilotBV    If true then the BV calculation is done without including the pilot BV modifiers
     * @param formatting       Which formatting style to use: HTML, Discord, or None (plaintext)
     */
    static EntityReadout createReadout(Entity entity, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV, ViewFormatting formatting) {

        if (entity instanceof BattleArmor battleArmor) {
            return new BattleArmorReadout(battleArmor, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof Infantry infantry) {
            return new InfantryReadout(infantry, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof ProtoMek protoMek) {
            return new ProtoMekReadout(protoMek, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof GunEmplacement gunEmplacement) {
            return new GunEmplacementReadout(gunEmplacement, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof FighterSquadron squadron) {
            return new FighterSquadronReadout(squadron, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof Mek mek) {
            return new MekReadout(mek, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof Aero aero) {
            return new AeroReadout(aero, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else if (entity instanceof Tank tank) {
            return new TankReadout(tank, showDetail, useAlternateCost, ignorePilotBV, formatting);
        } else {
            return new GeneralEntityReadout2(entity, showDetail, useAlternateCost, ignorePilotBV, formatting);
        }
    }

    /**
     * The head section includes the title (unit name), tech level and availability, tonnage, bv, and cost.
     *
     * @return The data from the head section.
     */
    String getHeadSection();

    /**
     * The basic section includes general details such as movement, system equipment (cockpit, gyro, etc.) and armor.
     *
     * @return The data from the basic section
     */
    String getBasicSection();

    /**
     * The invalid section includes reasons why the unit is invalid
     *
     * @return The data from the invalid section
     */
    String getInvalidSection();

    /**
     * The loadout includes weapons, ammo, and other equipment broken down by location.
     *
     * @return The data from the loadout section.
     */
    String getLoadoutSection();

    /**
     * The fluff section includes fluff details like unit history and deployment patterns as well as quirks.
     *
     * @return The data from the fluff section.
     */
    String getFluffSection();

    /**
     * @return A summary including all four sections.
     */
    default String getReadout() {
        return getReadout(null);
    }

    /**
     * @return A summary including all sections, using the given font if applicable.
     */
    default String getReadout(@Nullable String fontName) {
        return getReadout(fontName, ViewFormatting.HTML);
    }

    /**
     * @return A summary including all four sections.
     */
    String getReadout(@Nullable String fontName, ViewFormatting formatting);
}
