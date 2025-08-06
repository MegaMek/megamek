/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import megamek.common.Aero;
import megamek.common.BombLoadout;
import megamek.common.BombType;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.Game;
import megamek.common.HitData;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.logging.MMLogger;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 * @since Sep 23, 2004
 */
public class SpaceBombAttackHandler extends WeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(SpaceBombAttackHandler.class);

    @Serial
    private static final long serialVersionUID = -2439937071168853215L;

    /**
     * @param toHit              {@link ToHitData} Object
     * @param weaponAttackAction {@link WeaponAttackAction} Object
     * @param game               {@link Game} Object
     * @param twGameManager      {@link TWGameManager} Object
     */
    public SpaceBombAttackHandler(ToHitData toHit, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) {
        super(toHit, weaponAttackAction, game, twGameManager);
        generalDamageType = HitData.DAMAGE_NONE;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        BombLoadout payload = waa.getBombPayload();
        if (null == payload) {
            return 0;
        }
        int numberOfBombs = payload.getTotalBombs();

        if (bDirect) {
            numberOfBombs = Math.min(numberOfBombs + (toHit.getMoS() / 3), numberOfBombs * 2);
        }

        numberOfBombs = applyGlancingBlowModifier(numberOfBombs, false);

        return numberOfBombs;
    }

    /**
     * Does this attack use the cluster hit table? necessary to determine how Aero damage should be applied
     */
    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected void useAmmo() {
        BombLoadout payload = waa.getBombPayload();
        if (!(ae.isAero()) || null == payload || payload.isEmpty()) {
            return;
        }

        // Need to remove ammo from fighters within a squadron
        if (ae instanceof FighterSquadron) {
            handleSquadronAmmoExpenditure(payload);
        } else {
            // Ammo expenditure for a single fighter
            handleSingleFighterAmmoExpenditure(payload);
        }

        super.useAmmo();
    }

    /**
     * Handles ammunition expenditure for fighter squadrons. In a squadron, salvos consist of one bomb from each fighter
     * equipped with the proper type.
     */
    private void handleSquadronAmmoExpenditure(BombLoadout payload) {
        List<Entity> activeFighters = ae.getActiveSubEntities();
        if (activeFighters.isEmpty()) {
            return;
        }

        for (Map.Entry<BombTypeEnum, Integer> entry : payload.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int bombCount = entry.getValue();

            if (bombCount <= 0) {continue;}

            // Remove bombs from individual fighters in the squadron
            removeBombsFromSquadronFighters(activeFighters, bombType, bombCount);

            // Remove bombs from the squadron entity itself
            removeSquadronBombs(bombType, bombCount, activeFighters.size());
        }
    }

    /**
     * Removes bombs from individual fighters within a squadron.
     */
    private void removeBombsFromSquadronFighters(List<Entity> activeFighters, BombTypeEnum bombType, int bombCount) {
        int fighterIndex = 0;

        for (int i = 0; i < bombCount; i++) {
            boolean bombRemoved = false;
            int iterations = 0;

            // Round-robin through fighters to find and remove bombs
            while (!bombRemoved && iterations <= activeFighters.size()) {
                Aero fighter = (Aero) activeFighters.get(fighterIndex);

                if (removeBombFromEntity(fighter, bombType)) {
                    bombRemoved = true;
                }

                iterations++;
                fighterIndex = (fighterIndex + 1) % activeFighters.size();
            }

            if (iterations > activeFighters.size()) {
                LOGGER.error("Couldn't find ammo for a dropped bomb of type: {}", bombType.getDisplayName());
            }
        }
    }

    /**
     * Removes bombs from the squadron entity itself based on salvo calculations.
     */
    private void removeSquadronBombs(BombTypeEnum bombType, int bombCount, int activeFighterCount) {
        int numSalvos = (int) Math.ceil((double) bombCount / activeFighterCount);

        for (int salvo = 0; salvo < numSalvos; salvo++) {
            if (!removeBombFromEntity(ae, bombType)) {
                LOGGER.warn("Could not remove squadron bomb for salvo {} of type: {}",
                      salvo, bombType.getDisplayName());
                break;
            }
        }
    }

    /**
     * Handles ammunition expenditure for single fighters.
     */
    private void handleSingleFighterAmmoExpenditure(BombLoadout payload) {
        for (Map.Entry<BombTypeEnum, Integer> entry : payload.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int bombCount = entry.getValue();

            for (int i = 0; i < bombCount; i++) {
                if (!removeBombFromEntity(ae, bombType)) {
                    LOGGER.warn("Could not remove bomb {} of {} for type: {}",
                          i + 1, bombCount, bombType.getDisplayName());
                    break;
                }
            }
        }
    }

    /**
     * Removes a single bomb of the specified type from the given entity.
     *
     * @param entity   The entity to remove the bomb from
     * @param bombType The type of bomb to remove
     *
     * @return true if a bomb was successfully removed, false otherwise
     */
    private boolean removeBombFromEntity(Entity entity, BombTypeEnum bombType) {
        for (Mounted<?> bomb : entity.getBombs()) {
            if (isBombRemovable(bomb, bombType)) {
                bomb.setShotsLeft(0);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a mounted bomb can be removed (correct type, not destroyed, has shots).
     *
     * @param bomb     The mounted bomb to check
     * @param bombType The bomb type we're looking for
     *
     * @return true if the bomb can be removed
     */
    private boolean isBombRemovable(Mounted<?> bomb, BombTypeEnum bombType) {
        return ((BombType) bomb.getType()).getBombType() == bombType
              && !bomb.isDestroyed()
              && bomb.getUsableShotsLeft() > 0;
    }
}
