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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.logging.MMLogger;

/**
 * Swaps one unit in a generated force for another, leaving the rest of its formation alone.
 *
 * <p>A player who does not want the Locust a lance rolled can ask for another unit in its place without rolling
 * the whole lance again. The new unit is drawn for the same slot: the same unit type, weight class, movement mode and
 * role, from the same faction table. It is never the design being replaced. If the formation has rules - a Recon
 * lance that needs fast units, say - the new unit must leave the formation still meeting them. When nothing does, the
 * slot keeps the unit it had.</p>
 *
 * <p>The pilot stays: the slot's commander is put in the new unit. The slot is also brought back into the force if
 * it had been struck out, since striking a unit out and rerolling it both say the player did not want that unit.</p>
 */
public final class UnitReroller {

    private static final MMLogger LOGGER = MMLogger.create(UnitReroller.class);

    /**
     * How many units are drawn for a slot before giving up on one its formation accepts. Each draw that breaks the
     * formation is ruled out before the next, so this bounds the work on a formation that nothing else fits.
     */
    static final int MAX_DRAWS = 10;

    /** Why a node cannot be rerolled as a single unit. */
    public enum Refusal {
        /** The node is a formation, or a slot that holds no unit or no pilot. */
        NOT_A_UNIT("ForceGeneratorDialog.rerollUnit.notAUnit"),
        /** The node is a ship, which has its own reroll that keeps its bays and docked craft. */
        SHIP("ForceGeneratorDialog.rerollUnit.ship"),
        /** The formation fields one design in every slot, such as an artillery battery. */
        ONE_DESIGN_FORMATION("ForceGeneratorDialog.rerollUnit.oneDesign");

        private final String messageKey;

        Refusal(String messageKey) {
            this.messageKey = messageKey;
        }

        /** @return the message key of the reason a player is shown */
        public String getMessageKey() {
            return messageKey;
        }
    }

    /**
     * What a reroll did.
     *
     * @param before the design the slot held
     * @param after  the design it holds now
     * @param draws  how many units were drawn before one fitted
     */
    public record Outcome(String before, String after, int draws) {
    }

    private UnitReroller() {
    }

    /**
     * Says whether a node can be rerolled as a single unit.
     *
     * @param slot the node the player picked
     *
     * @return why it cannot, or {@code null} when it can
     */
    public static @Nullable Refusal whyNotRerollable(ForceDescriptor slot) {
        if (ShipReroller.isShip(slot)) {
            LOGGER.debug("[ForceGen][RerollUnit] '{}': a ship; Reroll ship covers it", slot.parseName());
            return Refusal.SHIP;
        }
        if (!slot.isElement() || (slot.getEntity() == null)) {
            LOGGER.debug("[ForceGen][RerollUnit] '{}': not a unit (element={}, entity={})", slot.parseName(),
                  slot.isElement(), (slot.getEntity() == null) ? "none" : "set");
            return Refusal.NOT_A_UNIT;
        }
        if (slot.getCo() == null) {
            LOGGER.debug("[ForceGen][RerollUnit] '{}': has no pilot to put in a new unit", slot.parseName());
            return Refusal.NOT_A_UNIT;
        }
        ForceDescriptor parent = slot.getParent();
        if (parent == null) {
            return null;
        }
        boolean parentPinsModel = !parent.getModels().isEmpty();
        boolean parentPinsChassis = !parent.getChassis().isEmpty();
        if (parentPinsModel || parentPinsChassis) {
            LOGGER.debug("[ForceGen][RerollUnit] '{}': its formation '{}' fields one design (models={} chassis={})",
                  slot.parseName(), parent.parseName(), parent.getModels(), parent.getChassis());
            return Refusal.ONE_DESIGN_FORMATION;
        }
        return null;
    }

    /**
     * Replaces the unit in one slot with another drawn for the same slot.
     *
     * @param slot the node holding the unit to replace
     *
     * @return what was done, or {@code null} when the slot cannot be rerolled or nothing else fits it; the slot is
     *       left exactly as it was in that case
     */
    public static @Nullable Outcome reroll(ForceDescriptor slot) {
        if (whyNotRerollable(slot) != null) {
            return null;
        }
        String before = slot.getModelName();
        Set<String> pinnedModels = new HashSet<>(slot.getModels());
        Set<String> pinnedChassis = new HashSet<>(slot.getChassis());
        Set<String> pinnedVariants = new HashSet<>(slot.getVariants());
        FormationType formationType = (slot.getParent() == null) ? null : slot.getParent().getFormation();

        ModelRecord picked = null;
        int draws = 0;
        slot.getModels().clear();
        slot.getChassis().clear();
        slot.getVariants().clear();
        slot.getRejectedModels().add(before);
        try {
            while ((picked == null) && (draws < MAX_DRAWS)) {
                draws++;
                ModelRecord candidate = slot.generate();
                if (candidate == null) {
                    break;
                }
                if (keepsFormation(slot, candidate, formationType)) {
                    picked = candidate;
                } else {
                    slot.getRejectedModels().add(candidate.getKey());
                }
            }
        } finally {
            slot.getRejectedModels().clear();
        }

        if (picked == null) {
            slot.getModels().addAll(pinnedModels);
            slot.getChassis().addAll(pinnedChassis);
            slot.getVariants().addAll(pinnedVariants);
            LOGGER.info("[ForceGen][RerollUnit] '{}': nothing else fits this slot after {} draw(s){}; kept",
                  before, draws, (formationType == null) ? "" : " within " + formationType.getName());
            return null;
        }

        slot.setUnit(picked);
        slot.loadEntities(null, 0);
        slot.setIncludedRecursively(true);
        LOGGER.info("[ForceGen][RerollUnit] '{}' -> '{}' after {} draw(s)", before, picked.getKey(), draws);
        return new Outcome(before, picked.getKey(), draws);
    }

    /**
     * Whether the formation would still meet its rules with the candidate in this slot. A slot outside any formation,
     * or among siblings the unit cache does not know, always passes: there is nothing to check it against. Called once
     * per draw, so it logs at trace only.
     */
    private static boolean keepsFormation(ForceDescriptor slot, ModelRecord candidate,
          @Nullable FormationType formationType) {
        if ((formationType == null) || (slot.getParent() == null)) {
            return true;
        }
        MekSummaryCache cache = MekSummaryCache.getInstance();
        List<MekSummary> members = new ArrayList<>();
        for (ForceDescriptor sibling : slot.getParent().getSubForces()) {
            String modelName = (sibling == slot) ? candidate.getKey() : sibling.getModelName();
            MekSummary member = cache.getMek(modelName);
            if (member == null) {
                LOGGER.trace("[ForceGen][RerollUnit] '{}' not in the unit cache; {} not checked", modelName,
                      formationType.getName());
                return true;
            }
            members.add(member);
        }
        boolean qualifies = formationType.qualifies(members);
        if (!qualifies) {
            LOGGER.trace("[ForceGen][RerollUnit] '{}' would break {}; drawing again", candidate.getKey(),
                  formationType.getName());
        }
        return qualifies;
    }
}
