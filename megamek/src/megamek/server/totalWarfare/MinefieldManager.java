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
package megamek.server.totalWarfare;

import java.util.Enumeration;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.MiscMounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Handles laying minefields: dispenser-laid mines (see {@link #layMine}) and the various weapon-delivered Thunder and
 * FASCAM minefields fired by artillery, bombs, and scatterable LRMs. Extracted from {@link TWGameManager} to keep that
 * class from growing further; {@code TWGameManager} delegates its public delivery methods here.
 */
class MinefieldManager extends AbstractTWRuleHandler {

    MinefieldManager(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Delivers a thunder-aug shot to the targeted hex area. Thunder-Augs are 7 hexes, though, so...
     *
     * @param damage The per-hex density of the incoming minefield; that is, the final value with any modifiers (such as
     *               halving and rounding just for <em>being</em> T-Aug) already applied.
     */
    public void deliverThunderAugMinefield(Coords coords, int playerId, int damage, int entityId) {
        Game game = getGame();
        Coords mfCoord;
        for (int dir = 0; dir < 7; dir++) {
            // May need to reset here for each new hex.
            int hexDamage = damage;
            if (dir == 6) {// The targeted hex.
                mfCoord = coords;
            } else {// The hex in the dir direction from the targeted hex.
                mfCoord = coords.translated(dir);
            }

            // Only if this is on the board...
            if (game.getBoard().contains(mfCoord)) {
                Minefield minefield = null;
                Enumeration<Minefield> minefields = game.getMinefields(mfCoord).elements();
                // Check if there already are Thunder minefields in the hex.
                while (minefields.hasMoreElements()) {
                    Minefield mf = minefields.nextElement();
                    if (mf.getType() == Minefield.TYPE_CONVENTIONAL) {
                        minefield = mf;
                        break;
                    }
                }

                // Did we find a Thunder minefield in the hex?
                // N.B. damage Thunder minefields equals the number of
                // missiles, divided by two, rounded up.
                if (minefield == null) {
                    // Nope. Create a new Thunder minefield
                    minefield = Minefield.createMinefield(mfCoord, playerId, Minefield.TYPE_CONVENTIONAL, hexDamage);
                    game.addMinefield(minefield);
                    gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
                } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
                    // Yup. Replace the old one.
                    gameManager.removeMinefield(minefield);
                    hexDamage += minefield.getDensity();

                    // Damage from Thunder minefields are capped.
                    if (hexDamage > Minefield.MAX_DAMAGE) {
                        hexDamage = Minefield.MAX_DAMAGE;
                    }
                    minefield.setDensity(hexDamage);
                    game.addMinefield(minefield);
                    gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
                }
            }
        }
    }

    /**
     * Adds a Thunder minefield to the hex.
     *
     * @param coords   the minefield's coordinates
     * @param playerId the deploying player's id
     * @param damage   the amount of damage the minefield does
     * @param entityId an entity that might spot the minefield
     */
    public void deliverThunderMinefield(Coords coords, int playerId, int damage, int entityId) {
        Game game = getGame();
        Minefield minefield = null;
        Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_CONVENTIONAL) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder minefield
        if (minefield == null) {
            minefield = Minefield.createMinefield(coords, playerId, Minefield.TYPE_CONVENTIONAL, damage);
            game.addMinefield(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
            // Add to the old one
            gameManager.removeMinefield(minefield);
            int oldDamage = minefield.getDensity();
            damage += oldDamage;
            damage = Math.min(damage, Minefield.MAX_DAMAGE);
            minefield.setDensity(damage);
            game.addMinefield(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        }
    }

    /**
     * Adds a Thunder Inferno minefield to the hex.
     *
     * @param coords   the minefield's coordinates
     * @param playerId the deploying player's id
     * @param damage   the amount of damage the minefield does
     * @param entityId an entity that might spot the minefield
     */
    public void deliverThunderInfernoMinefield(Coords coords, int playerId, int damage, int entityId) {
        Game game = getGame();
        Minefield minefield = null;
        Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_INFERNO) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder Inferno minefield
        if (minefield == null) {
            minefield = Minefield.createMinefield(coords, playerId, Minefield.TYPE_INFERNO, damage);
            game.addMinefield(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
            // Add to the old one
            gameManager.removeMinefield(minefield);
            int oldDamage = minefield.getDensity();
            damage += oldDamage;
            damage = Math.min(damage, Minefield.MAX_DAMAGE);
            minefield.setDensity(damage);
            game.addMinefield(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        }
    }

    /**
     * Delivers an artillery FASCAM shot to the targeted hex area.
     */
    public void deliverFASCAMMinefield(Coords coords, int playerId, int damage, int entityId) {
        Game game = getGame();
        // Only if this is on the board...
        if (game.getBoard().contains(coords)) {
            Minefield minefield = null;
            Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
            // Check if there already are Thunder minefields in the hex.
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (mf.getType() == Minefield.TYPE_CONVENTIONAL) {
                    minefield = mf;
                    break;
                }
            }
            // Did we find a Thunder minefield in the hex?
            if (minefield == null) {
                minefield = Minefield.createMinefield(coords, playerId, Minefield.TYPE_CONVENTIONAL, damage);
                game.addMinefield(minefield);
                gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
            } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
                // Add to the old one.
                gameManager.removeMinefield(minefield);
                int oldDamage = minefield.getDensity();
                damage += oldDamage;
                damage = Math.min(damage, Minefield.MAX_DAMAGE);
                minefield.setDensity(damage);
                game.addMinefield(minefield);
                gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
            }
        }
    }

    /**
     * Adds a Thunder-Active minefield to the hex.
     *
     * @param coords   the minefield's coordinates
     * @param playerId the deploying player's id
     * @param damage   the amount of damage the minefield does
     * @param entityId an entity that might spot the minefield
     */
    public void deliverThunderActiveMinefield(Coords coords, int playerId, int damage, int entityId) {
        Game game = getGame();
        Minefield minefield = null;
        Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_ACTIVE) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder-Active minefield
        if (minefield == null) {
            minefield = Minefield.createMinefield(coords, playerId, Minefield.TYPE_ACTIVE, damage);
            game.addMinefield(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
            // Add to the old one
            gameManager.removeMinefield(minefield);
            int oldDamage = minefield.getDensity();
            damage += oldDamage;
            damage = Math.min(damage, Minefield.MAX_DAMAGE);
            minefield.setDensity(damage);
            game.addMinefield(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        }
    }

    /**
     * Adds a Thunder-Vibrabomb minefield to the hex.
     */
    public void deliverThunderVibraMinefield(Coords coords, int playerId, int damage, int sensitivity, int entityId) {
        Game game = getGame();
        Minefield minefield = null;
        Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_VIBRABOMB) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder-Vibra minefield
        if (minefield == null) {
            minefield = Minefield.createMinefield(coords, playerId, Minefield.TYPE_VIBRABOMB, damage, sensitivity);
            game.addMinefield(minefield);
            game.addVibrabomb(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
            // Add to the old one
            gameManager.removeMinefield(minefield);
            int oldDamage = minefield.getDensity();
            damage += oldDamage;
            damage = Math.min(damage, Minefield.MAX_DAMAGE);
            minefield.setDensity(damage);
            game.addMinefield(minefield);
            game.addVibrabomb(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        }
    }

    /**
     * Adds an EMP minefield to the hex, as deployed by a vehicular or battle armor mine dispenser (TO:AUE p.177).
     *
     * @param coords   the minefield's coordinates
     * @param playerId the deploying player's id
     * @param damage   the amount of damage the minefield does
     * @param setting  the weight threshold (in tons) that triggers the mine
     * @param entityId an entity that might spot the minefield
     */
    public void deliverThunderEMPMinefield(Coords coords, int playerId, int damage, int setting, int entityId) {
        Game game = getGame();
        Minefield minefield = null;
        Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
        // Check if there already is an EMP minefield in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_EMP) {
                minefield = mf;
                break;
            }
        }

        // Create a new EMP minefield
        if (minefield == null) {
            minefield = Minefield.createMinefield(coords, playerId, Minefield.TYPE_EMP, damage, setting);
            game.addMinefield(minefield);
            game.addEMPMine(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        } else if (minefield.getDensity() < Minefield.MAX_DAMAGE) {
            // Add to the old one
            gameManager.removeMinefield(minefield);
            int oldDamage = minefield.getDensity();
            damage += oldDamage;
            damage = Math.min(damage, Minefield.MAX_DAMAGE);
            minefield.setDensity(damage);
            game.addMinefield(minefield);
            game.addEMPMine(minefield);
            gameManager.checkForRevealMinefield(minefield, game.getEntity(entityId));
        }
    }

    /**
     * let an entity lay a mine
     *
     * @param entity the <code>Entity</code> that should lay a mine
     * @param mineId an <code>int</code> pointing to the mine
     */
    void layMine(Entity entity, int mineId, Coords coords) {
        MiscMounted mine = (MiscMounted) entity.getEquipment(mineId);
        Report r;
        if (!mine.isMissing()) {
            int reportId = switch (mine.getMineType()) {
                case MiscMounted.MINE_CONVENTIONAL -> {
                    deliverThunderMinefield(coords, entity.getOwnerId(), 10, entity.getId());
                    yield 3500;
                }
                case MiscMounted.MINE_VIBRABOMB -> {
                    deliverThunderVibraMinefield(coords,
                          entity.getOwnerId(),
                          10,
                          mine.getVibraSetting(),
                          entity.getId());
                    yield 3505;
                }
                case MiscMounted.MINE_ACTIVE -> {
                    deliverThunderActiveMinefield(coords, entity.getOwnerId(), 10, entity.getId());
                    yield 3510;
                }
                case MiscMounted.MINE_INFERNO -> {
                    deliverThunderInfernoMinefield(coords, entity.getOwnerId(), 10, entity.getId());
                    yield 3515;
                }
                case MiscMounted.MINE_EMP -> {
                    deliverThunderEMPMinefield(coords, entity.getOwnerId(), 10, mine.getEmpSetting(), entity.getId());
                    yield 3516;
                }
                case MiscMounted.MINE_COMMAND_DETONATED ->
                    // Placeholder: command-detonated mine deployment by dispenser is not yet implemented, so no
                    // minefield is laid. The shot is still consumed below to match the other cases.
                      3517;
                default -> 0;
            };
            mine.setShotsLeft(mine.getUsableShotsLeft() - 1);
            if (mine.getUsableShotsLeft() <= 0) {
                mine.setMissing(true);
            }
            r = new Report(reportId);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(coords.getBoardNum());
            addReport(r);
            entity.setLayingMines(true);
        }
    }
}
