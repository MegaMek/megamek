/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * Represents a single turn within a phase of the game, where a specific player has to declare
 * his/her action. The default game turn allows a player to move any entity.
 *
 * @author Ben
 * @since September 6, 2002, 11:52 AM
 */
public class GameTurn implements Serializable {
    private static final long serialVersionUID = -8340385894504735190L;

    /** The constant to represent Infantry (and Battle Armor) entities. */
    public static final int CLASS_INFANTRY = 1;

    /** The constant to represent Protomech entities. */
    public static final int CLASS_PROTOMECH = 2;

    /** The constant to represent Tank entities. */
    public static final int CLASS_TANK = 4;

    /** The constant to represent Mech entities. */
    public static final int CLASS_MECH = 8;

    /** The constant to represent Gun Emplacement entities. */
    public static final int CLASS_GUN_EMPLACEMENT = 16;

    /** The constant to represent Aero entities. */
    public static final int CLASS_AERO   = 32;

    /** The constant to represent space station entities. */
    public static final int CLASS_SPACE_STATION   = 64;

    /** The constant to represent jumpship entities. */
    public static final int CLASS_JUMPSHIP   = 128;

    /** The constant to represent warship entities. */
    public static final int CLASS_WARSHIP   = 256;

    /** The constant to represent dropship entities. */
    public static final int CLASS_DROPSHIP   = 512;

    /** The constant to represent warship entities. */
    public static final int CLASS_SMALL_CRAFT   = 1024;

    private int playerId;
    
    /**
     * Various optionals rules force certain unit types to move multiple units for one turn, such as
     * mek and vehicle lance rules; this flag keeps track of whether this turn was generated as one
     * of these multi-turns.
     */
    private boolean isMultiTurn = false;

    /** Creates a new instance of GameTurn */
    public GameTurn(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerNum() {
        return playerId;
    }

    public void setPlayerNum(int playerId) {
        this.playerId = playerId;
    }

    /**
     * Determine if the specified entity is a valid one to use for this turn.
     *
     * @param entity the <code>Entity</code> that may take this turn.
     * @param game The {@link Game} the turn belongs to
     * @return <code>true</code> if the specified entity can take this turn.
     *         <code>false</code> if the entity is not valid for this turn.
     */
    public boolean isValidEntity(final @Nullable Entity entity, final Game game) {
        return isValidEntity(entity, game, true);
    }

    /**
     * Determine if the specified entity is a valid one to use for this turn.
     * <p>
     * In addition to the "standard" validity checks, there is also a check for the optional rules
     * "infantry move later" and "protos move later." This checks to see if those options are
     * enabled and if there is a valid non-infantry (or proto) unit to move and if so, the entity
     * is invalid.
     * <p>
     * There are certain instances where this check should not be used when the optional rules are
     * enabled (such as loading infantry into a unit). Hence, the use of these additional checks is
     * specified by a boolean input parameter.
     *
     * @param entity the <code>Entity</code> that may take this turn.
     * @param game The {@link Game} the turn belongs to
     * @param useValidNonInfantryCheck Boolean that determines if we should check to see if infantry
     *                                can be moved yet
     * @return <code>true</code> if the specified entity can take this turn.
     *         <code>false</code> if the entity is not valid for this turn.
     */
    public boolean isValidEntity(final @Nullable Entity entity, final Game game,
                                 final boolean useValidNonInfantryCheck) {
        return (entity != null) && (entity.getOwnerId() == playerId) && entity.isSelectableThisTurn()
                // This next bit enforces the "A players Infantry/ProtoMechs move after that player's other units" options.
                && !(useValidNonInfantryCheck && game.getPhase().isMovement()
                && (((entity instanceof Infantry) && game.getOptions().booleanOption(OptionsConstants.INIT_INF_MOVE_LATER))
                || ((entity instanceof Protomech) && game.getOptions().booleanOption(OptionsConstants.INIT_PROTOS_MOVE_LATER)))
                && game.checkForValidNonInfantryAndOrProtomechs(playerId));
    }

    /**
     * @return true if the player and entity are both valid.
     */
    public boolean isValid(final int playerId, final @Nullable Entity entity, final Game game) {
        return (playerId == this.playerId) && isValidEntity(entity, game);
    }

    /**
     * @return true if the player is valid.
     */
    public boolean isValid(int playerId, IGame game) {
        return playerId == this.playerId;
    }

    public boolean isMultiTurn() {
        return isMultiTurn;
    }

    public void setMultiTurn(boolean isMultiTurn) {
        this.isMultiTurn = isMultiTurn;
    }

    /**
     * Get the class code for the given entity.
     *
     * @param entity the <code>Entity</code> whose class code is needed.
     * @return the <code>int</code> code for the entity's class.
     */
    public static int getClassCode(final Entity entity) {
        // Start with subclasses of Aero
        if (entity instanceof SpaceStation) {
            return CLASS_SPACE_STATION;
        } else if (entity instanceof Warship) {
            return CLASS_WARSHIP;
        }  else if (entity instanceof Jumpship) {
            return CLASS_JUMPSHIP;
        } else if (entity instanceof Dropship) {
            return entity.isAirborne() ? CLASS_DROPSHIP : CLASS_TANK;
        } else if ((entity instanceof SmallCraft) && entity.isAirborne()) {
            return CLASS_SMALL_CRAFT;
            // Anything else that's still airborne is treated as an Aero
            // (VTOLs aren't considered airborne, since it's based on altitude and not elevation)
        } else if (entity.isAirborne()) {
            return CLASS_AERO;
        } else if (entity instanceof Infantry) {
            return CLASS_INFANTRY;
        } else if (entity instanceof Protomech) {
            return CLASS_PROTOMECH;
        } else if (entity instanceof GunEmplacement) {
            return CLASS_GUN_EMPLACEMENT;
        } else if ((entity instanceof Tank) || entity.isAero()) {
            return CLASS_TANK;
        } else if (entity instanceof Mech) {
            return CLASS_MECH;
        } else {
            return 0;
        }
    }

    /**
     * Prints out a shortened class name (w/o package information) plus the id of the player whose
     * turn this is for.
     */
    @Override
    public String toString() {
        String className = getClass().getName();
        return className.substring(className.lastIndexOf('.') + 1) + " pid: " + playerId;
    }

    /**
     * A type of game turn that allows only one specific entity to move.
     */
    public static class SpecificEntityTurn extends GameTurn {
        private static final long serialVersionUID = -4209080275946913689L;
        private int entityId;

        public SpecificEntityTurn(int playerId, int entityId) {
            super(playerId);
            this.entityId = entityId;
        }

        public int getEntityNum() {
            return entityId;
        }

        public void setEntityNum(int entityId) {
            this.entityId = entityId;
        }

        /**
         * @return true if the entity is normally valid and it is the specific entity that can move
         * this turn.
         */
        @Override
        public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
            return super.isValidEntity(entity, game, useValidNonInfantryCheck)
                    && (entity.getId() == entityId);
        }

        @Override
        public String toString() {
            return super.toString() + " eid: " + entityId;
        }
    }

    /**
     * A type of game turn that allows only one specific entity to trigger their Anti-Personnel pods
     * against attacking infantry.
     */
    public static class TriggerAPPodTurn extends SpecificEntityTurn {
        private static final long serialVersionUID = -5104845305165987340L;

        public TriggerAPPodTurn(int playerId, int entityId) {
            super(playerId, entityId);
        }

        /**
         * @return true if the entity matches this game turn, even if the entity has declared an
         * action.
         */
        @Override
        public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
            final boolean oldDone = entity.done;
            entity.done = false;
            final boolean result = super.isValidEntity(entity, game, useValidNonInfantryCheck);
            entity.done = oldDone;
            return result;
        }
    }

    /**
     * A type of game turn that allows only one specific entity to trigger their Anti-Battle Armor
     * pods against attacking infantry/BA.
     */
    public static class TriggerBPodTurn extends SpecificEntityTurn {
        private static final long serialVersionUID = -9082006433957145275L;
        private String attackType;

        public TriggerBPodTurn(int playerId, int entityId, String attackType) {
            super(playerId, entityId);
            this.attackType = attackType;
        }

        public String getAttackType() {
            return attackType;
        }

        /**
         * @return true if the entity matches this game turn, even if the entity has declared an
         * action.
         */
        @Override
        public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
            final boolean oldDone = entity.done;
            entity.done = false;
            final boolean result = super.isValidEntity(entity, game, useValidNonInfantryCheck);
            entity.done = oldDone;
            return result;
        }
    }

    /**
     * A type of game turn that allows only one specific entity to counterattack a break grapple by
     * the original attacker
     */
    public static class CounterGrappleTurn extends SpecificEntityTurn {
        private static final long serialVersionUID = 5248356977626018582L;

        public CounterGrappleTurn(int playerId, int entityId) {
            super(playerId, entityId);
        }

        /**
         * @return true if the entity matches this game turn, even if the entity has declared an
         * action.
         */
        @Override
        public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
            final boolean oldDone = entity.done;
            entity.done = false;
            final boolean result = super.isValidEntity(entity, game, useValidNonInfantryCheck);
            entity.done = oldDone;
            return result;
        }
    }

    /**
     * A type of game turn that allows only certain types of units to move.
     */
    public static class EntityClassTurn extends GameTurn {
        private static final long serialVersionUID = 1305684619846966124L;
        private final int mask;

        /**
         * Only allow entities for the given player which have types in the
         * class mask to move.
         *
         * @param playerId the <code>int</code> ID of the player
         * @param classMask the <code>int</code> bitmask containing all the
         *            valid class types for this move.
         */
        public EntityClassTurn(int playerId, int classMask) {
            super(playerId);
            mask = classMask;
        }

        /**
         * Determine if the given entity is a valid one to use for this turn.
         *
         * @param entity the <code>Entity</code> being tested for the move.
         * @param game The {@link Game} the entity belongs to
         * @return <code>true</code> if the entity can be moved.
         */
        @Override
        public boolean isValidEntity(final @Nullable Entity entity, final Game game,
                                     final boolean useValidNonInfantryCheck) {
            // The entity must pass the requirements of the parent class and be in the mask.
            return super.isValidEntity(entity, game, useValidNonInfantryCheck)
                    && isValidClass(getClassCode(entity));
        }

        /**
         * Determine if entities of the given class get to move.
         *
         * @param classCode the <code>int</code> class code being tested
         * @return <code>true</code> if entities of that class can move.
         */
        public boolean isValidClass(int classCode) {
            return (classCode & mask) != 0;
        }

        /**
         * Get the class code of this turn
         * @return the classcode of this turn
         */
        public int getTurnCode() {
            return mask;
        }

        @Override
        public String toString() {
            return super.toString() + " mask: " + mask;
        }
    }

    /**
     * A type of game turn that indicates that one or more players should be given the opportunity
     * to unload entities that are stranded on immobile transports. Each player declares which
     * stranded units they will unload at the beginning of the movement phase, without being told
     * what stranded units their opponent(s) are unloading.
     * <p>
     * According to
     * <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">Randall Bills</a>,
     * the "minimum move" rule allow stranded units to dismount at the start of the turn.
     */
    public static class UnloadStrandedTurn extends GameTurn {
        private static final long serialVersionUID = 2403095752478007872L;
        private int[] entityIds;

        /**
         * Any player that owns an entity whose ID is in the passed array should be given a chance
         * to unload it.
         *
         * @param ids the array of <code>int</code> IDs of stranded entities. This value must not be
         *           <code>null</code> or empty.
         * @throws IllegalArgumentException if a <code>null</code> or empty value is passed for ids.
         */
        public UnloadStrandedTurn(int... ids) {
            super(Player.PLAYER_NONE);

            // Validate input.
            if (ids == null) {
                throw new IllegalArgumentException("the passed array of ids is null");
            } else if (ids.length == 0) {
                throw new IllegalArgumentException("the passed array of ids is empty");
            }

            // Create a copy of the array to prevent any post-call shenanigans.
            entityIds = new int[ids.length];
            System.arraycopy(ids, 0, entityIds, 0, ids.length);
        }

        /**
         * Any player that owns an entity in the passed enumeration should be given a chance to
         * unload it.
         *
         * @param entities the <code>Enumeration</code> of stranded entities. This value must not be
         *                <code>null</code> or empty.
         * @throws IllegalArgumentException if a <code>null</code> or empty value is passed for
         * entities.
         */
        public UnloadStrandedTurn(Iterator<Entity> entities) {
            super(Player.PLAYER_NONE);

            // Validate input.
            if (entities == null) {
                throw new IllegalArgumentException("the passed enumeration of entities is null");
            } else if (!entities.hasNext()) {
                throw new IllegalArgumentException("the passed enumeration of entities is empty");
            }

            // Get the first entity.
            Entity entity = entities.next();

            // Do we need to get more entities?
            if (entities.hasNext()) {
                // It's a bit of a hack, but get the Game from the first/ entity, and create a
                // temporary array that can hold the IDs of every entity in the game.
                int[] ids = new int[entity.game.getNoOfEntities()];
                int length = 0;

                // Store the first entity's ID.
                ids[length++] = entity.getId();

                // Walk the list of remaining stranded entities.
                while (entities.hasNext()) {
                    ids[length++] = entities.next().getId();
                }

                // Create an array that just holds the stranded entity ids.
                entityIds = new int[length];
                System.arraycopy(ids, 0, entityIds, 0, length);
            } else {
                // There was only one stranded entity.
                entityIds = new int[1];
                entityIds[0] = entity.getId();
            }
        }

        public int[] getEntityIds() {
            return entityIds;
        }

        /**
         * Determine if the given entity is a valid one to use for this turn.
         *
         * @param entity the <code>Entity</code> being tested for the move.
         * @param game The {@link Game} the entity belongs to
         * @return <code>true</code> if the entity can be moved.
         */
        @Override
        public boolean isValidEntity(final @Nullable Entity entity, final Game game) {
            // Null entities don't need to be checked.
            if (entity == null) {
                return false;
            }

            // Check if any entity in the array is valid.
            final int entityId = entity.getId();
            return IntStream.range(0, entityIds.length).anyMatch(index -> entityId == entityIds[index]);
        }

        /**
         * @return true if the player and entity are both valid.
         */
        @Override
        public boolean isValid(final int playerId, final @Nullable Entity entity, final Game game) {
            return isValidEntity(entity, game) && (entity.getOwnerId() == playerId);
        }

        /**
         * @return true if the player is valid.
         */
        public boolean isValid(int playerId, Game game) {
            return IntStream.range(0, entityIds.length)
                    .anyMatch(index -> (game.getEntity(entityIds[index]) != null)
                            && (playerId == game.getEntity(entityIds[index]).getOwnerId()));
        }

        @Override
        public String toString() {
            return super.toString() + ", entity IDs: [" + Arrays.toString(entityIds) + ']';
        }
    }

    /**
     * A type of game turn that allows only entities belonging to certain units to move.
     */
    public static class UnitNumberTurn extends GameTurn {
        private static final long serialVersionUID = -681892308327846884L;
        private final short unitNumber;

        /**
         * Only allow entities for the given player which have types in the class mask to move.
         *
         * @param playerId the <code>int</code> ID of the player
         * @param unit the <code>int</code> unit number of the entities allowed to move.
         */
        public UnitNumberTurn(int playerId, short unit) {
            super(playerId);
            unitNumber = unit;
        }

        /**
         * @return true if the specified entity is a valid one to use for this turn.
         */
        @Override
        public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
            return super.isValidEntity(entity, game, useValidNonInfantryCheck)
                    && (unitNumber == entity.getUnitNumber());
        }
    }
}
