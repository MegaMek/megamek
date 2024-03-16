/*
 * Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a Docking Collar (Docking Hardpoint, TO: AUE p.116) with which a JumpShip,
 * WarShip, Space Station or Mobile Structure can carry one DropShip.
 */
public class DockingCollar implements Transporter {
    private static final long serialVersionUID = -4699786673513410716L;

    private final Set<Integer> dockedUnits = new HashSet<>();
    private boolean damaged = false;
    private final int collarId;
    transient Game game;

    /**
     * Creates a JumpShip Docking Collar that can carry one dropship.
     *
     * @param collarId the Id of this collar, used for tracking in MHQ
     */
    public DockingCollar(int collarId) {
        this.collarId = collarId;
    }

    public String getType() {
        return "Docking Collar";
    }

    @Override
    public boolean canLoad(Entity unit) {
        return (unit instanceof Dropship) && !((Dropship) unit).isDockCollarDamaged()
                && (getUnused() == 1) && !isDamaged();
    }

    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Cannot load "
                    + unit.getShortName() + " into this Docking Collar.");
        }
        dockedUnits.add(unit.getId());
    }

    // Recovery is different from loading in that it uses up a recovery slot
    // load is only used in deployment phase
    public void recover(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Cannot recover "
                    + unit.getShortName() + " into this Docking Collar.");
        }
        dockedUnits.add(unit.getId());
    }

    @Override
    public List<Entity> getLoadedUnits() {
        correctDockedUnitList();
        return dockedUnits.stream()
                .map(id -> game.getEntity(id))
                .collect(Collectors.toList());
    }

    /**
     * Cleans out docked unit IDs that no longer match an active entity. This is a precaution
     * that ideally shouldn't be necessary.
     */
    private void correctDockedUnitList() {
        if (dockedUnits.removeIf(id -> game.getEntity(id) == null)) {
            LogManager.getLogger().warn("Unit IDs mapping to a null Entity found in this docking collar.");
        }
    }

    /**
     * DropShips launchable from this Docking Collar. This is different from loaded in that
     * units in recovery cannot launch.
     *
     * @return A list of DropShips that can launch from this Docking Collar. The list may be empty
     * but not null.
     */
    public List<Entity> getLaunchableUnits() {
        if (damaged) {
            return new ArrayList<>();
        } else {
            correctDockedUnitList();
            return dockedUnits.stream()
                    .map(id -> game.getEntity(id))
                    .filter(entity -> entity.getRecoveryTurn() == 0)
                    .filter(entity -> entity instanceof Dropship)
                    .filter(entity -> !((Dropship) entity).isDockCollarDamaged())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean unload(Entity unit) {
        return !isDamaged() && dockedUnits.remove(unit.getId());
    }

    @Override
    public String getUnusedString() {
        return "Dropship - " + ((getUnused() == 1) ? "1 unit" : "Docking Collar occupied");
    }

    @Override
    public double getUnused() {
        return dockedUnits.isEmpty() ? 1 : 0;
    }

    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return false;
    }

    @Override
    public @Nullable Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public final List<Entity> getExternalUnits() {
        return new ArrayList<>();
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }

    public boolean isDamaged() {
        return damaged;
    }

    public void setDamaged(boolean newStatus) {
        damaged = newStatus;
    }

    @Override
    public int hardpointCost() {
        return 1;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void resetTransporter() {
        dockedUnits.clear();
    }

    @Override
    public String toString() {
        return "dockingcollar";
    }
    
    public int getCollarNumber() {
        return collarId;
    }
}
