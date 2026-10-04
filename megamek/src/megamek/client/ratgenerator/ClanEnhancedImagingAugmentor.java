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
package megamek.client.ratgenerator;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AugmentedUnitType;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.options.IGameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Fits Clan warriors with the enhanced imaging neural implant.
 *
 * <p>EI is Clan-only (<i>Interstellar Operations: Alternate Eras</i>, p. 71), and works only with
 * walking motive systems - the construction rules name battle armour, ProtoMeks and Meks, and nothing
 * else.</p>
 *
 * <p>About one Clan warrior in twenty takes the implant, the rest declining it for its dangerous and
 * painful side effects. Those who do take it serve together, which is why the Inner Sphere over-counted
 * them: a scout meeting one EI unit concluded the Clans were riddled with them. That clustering is
 * modelled directly - the roll is made once per formation rather than once per warrior, so a formation
 * is either an EI unit or it is not. Rolling per warrior would give the same overall proportion spread
 * thinly across every star, which is the impression the Inner Sphere got and not what was true.</p>
 */
public final class ClanEnhancedImagingAugmentor {

    private static final MMLogger LOGGER = MMLogger.create(ClanEnhancedImagingAugmentor.class);

    /**
     * The Clans among whom the implant is more popular. Only the two named in the source are listed;
     * a Clan absent from here is not thereby a Warden, merely unrecorded.
     */
    private static final List<String> CRUSADER_MINDED_CLANS = List.of("CJF", "CSJ");

    /**
     * Roughly one Clan warrior in twenty takes the implant, the rest declining it for its dangerous
     * and painful side effects. Because the roll is per formation, one formation in twenty is an EI
     * unit and the proportion of warriors comes out the same.
     */
    private static final double BASE_FORMATION_CHANCE = 0.05;

    /**
     * The chance among the Clans the source calls more receptive. Doubled rather than derived: the
     * source gives the overall figure and says these Clans exceed it, but not by how much. The
     * baseline is left at the stated one in twenty so the average across the Clans stays there rather
     * than falling below it - which is what happens if the unnamed Clans are discounted to make room.
     */
    private static final double CRUSADER_FORMATION_CHANCE = 0.10;

    private ClanEnhancedImagingAugmentor() {
    }

    /**
     * Fits the EI implant to the warriors of whichever formations take it.
     *
     * @param root        the generated force; {@code null} is ignored
     * @param isClan      whether the force is a Clan force, the implant being theirs alone
     * @param gameOptions the options the force is generated for, read for the neural interface rules
     */
    public static void augment(@Nullable ForceDescriptor root, boolean isClan,
          @Nullable IGameOptions gameOptions) {
        if (root == null) {
            return;
        }
        String faction = root.getFaction();
        boolean rulesAllowNeuralInterfaces = neuralInterfaceRulesAreOn(gameOptions);
        LOGGER.info("[EnhancedImaging] ENTER: faction='{}', isClan={}, neural interface rules on={}",
              faction, isClan, rulesAllowNeuralInterfaces);

        if (!isClan) {
            LOGGER.info("[EnhancedImaging] SKIPPED - '{}' is not a Clan, and the implant is theirs"
                        + " alone.", faction);
            return;
        }
        if (!rulesAllowNeuralInterfaces) {
            LOGGER.info("[EnhancedImaging] SKIPPED - the neural interface rules are off in this game's"
                        + " options, so an implant fitted here would do nothing.");
            return;
        }

        double chance = chanceFor(faction);
        int augmented = augmentTree(root, chance);
        LOGGER.info("[EnhancedImaging] DONE: {} warrior(s) implanted, at a {}% chance per formation",
              augmented, Math.round(chance * 100));
    }

    /**
     * How likely one of a Clan's formations is to be an EI unit.
     *
     * <p>Public so MekHQ's own generator rolls the same odds against the same Clans; a campaign and a
     * skirmish should not disagree about how common EI warriors are.</p>
     *
     * @param factionCode the Clan the force is generated for
     *
     * @return how likely one of its formations is to be an EI unit
     */
    public static double formationChanceFor(@Nullable String factionCode) {
        return chanceFor(factionCode);
    }

    /**
     * Whether the implant works with this kind of unit.
     *
     * <p>Public for the same reason as {@link #formationChanceFor}: the construction rules naming
     * which units EI works with are the rules, not this class's business.</p>
     *
     * @param unitType the kind of unit the warrior crews
     *
     * @return {@code true} if EI works with it, which the rules limit to walking motive systems
     */
    public static boolean canUseEnhancedImaging(@Nullable AugmentedUnitType unitType) {
        return (unitType == AugmentedUnitType.BATTLE_MEK)
              || (unitType == AugmentedUnitType.INDUSTRIAL_MEK)
              || (unitType == AugmentedUnitType.BATTLE_ARMOR)
              || (unitType == AugmentedUnitType.PROTOMEK);
    }

    /**
     * @return {@code true} if the game's neural interface rules are switched on, EI being hidden and
     *       inert without them
     */
    public static boolean neuralInterfaceRulesAllowImplants(@Nullable IGameOptions gameOptions) {
        return NeuralInterfaceMode.from(gameOptions).isOn();
    }

    /**
     * @param factionCode the Clan the force is generated for
     *
     * @return how likely one of its formations is to be an EI unit
     */
    private static double chanceFor(@Nullable String factionCode) {
        if (factionCode == null) {
            return BASE_FORMATION_CHANCE;
        }
        String primary = factionCode.split("\\.")[0];
        return CRUSADER_MINDED_CLANS.stream().anyMatch(primary::equalsIgnoreCase)
              ? CRUSADER_FORMATION_CHANCE
              : BASE_FORMATION_CHANCE;
    }

    /**
     * Walks the force, deciding formation by formation whether it is an EI unit.
     *
     * @return how many warriors were implanted
     */
    private static int augmentTree(ForceDescriptor formation, double chance) {
        // The roll is made here, for the whole formation, which is what makes EI warriors serve
        // together rather than appearing one to a star.
        if (!formation.getSubForces().isEmpty() && (Math.random() < chance)) {
            int implanted = implantEveryWarrior(formation);
            if (implanted > 0) {
                LOGGER.debug("[EnhancedImaging] '{}' is an EI unit: {} warrior(s) implanted",
                      formation.parseName(), implanted);
                return implanted;
            }
        }
        int augmented = 0;
        for (ForceDescriptor subFormation : formation.getSubForces()) {
            augmented += augmentTree(subFormation, chance);
        }
        return augmented;
    }

    /**
     * Implants every warrior in the formation whose unit can use EI.
     *
     * @return how many were implanted
     */
    private static int implantEveryWarrior(ForceDescriptor formation) {
        int implanted = 0;
        for (ForceDescriptor subFormation : formation.getSubForces()) {
            Entity entity = subFormation.getEntity();
            if ((entity != null) && (entity.getCrew() != null) && canUseEnhancedImaging(entity)) {
                IOption option = entity.getCrew().getOptions()
                                       .getOption(OptionsConstants.MD_EI_IMPLANT);
                if (option != null) {
                    option.setValue(true);
                    implanted++;
                }
            }
            implanted += implantEveryWarrior(subFormation);
        }
        return implanted;
    }

    /**
     * @return {@code true} if EI works with this unit, which the rules limit to those with a walking
     *       motive system
     */
    private static boolean canUseEnhancedImaging(Entity entity) {
        return canUseEnhancedImaging(AugmentedUnitType.forEntity(entity));
    }

    /**
     * @return {@code true} if the game's neural interface rules are switched on, EI being hidden and
     *       inert without them
     */
    private static boolean neuralInterfaceRulesAreOn(@Nullable IGameOptions gameOptions) {
        return neuralInterfaceRulesAllowImplants(gameOptions);
    }
}
