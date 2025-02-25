/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.client.ui.swing.util;

import megamek.common.Entity;
import megamek.common.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EntitySelectionService {

    private final Set<Entity> selectedUnits = new HashSet<>();
    private int selectedUnitsTeam = -1;

    private final Player localPlayer;

    public EntitySelectionService(Player localPlayer) {
        this.localPlayer = localPlayer;
    }

    public void clearSelectedUnits() {
        selectedUnits.clear();
        selectedUnitsTeam = Player.TEAM_UNASSIGNED;
    }

    public void removeSelectedUnit(Entity entity) {
        selectedUnits.remove(entity);
        if (selectedUnits.isEmpty()) {
            selectedUnitsTeam = Player.TEAM_UNASSIGNED;;
        }
    }

    public void addSelectedUnits(Entity entity) {
        if (selectedUnits.isEmpty()) {
            selectedUnitsTeam = entity.getOwner().getTeam();
        }
        if (entity.getOwner().getTeam() == selectedUnitsTeam) {
            selectedUnits.add(entity);
        }
    }

    public void addSelectedUnits(Collection<Entity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        if (selectedUnits.isEmpty()) {
            selectedUnitsTeam = entities.stream().findAny().map(e -> e.getOwner().getTeam()).orElse(localPlayer.getTeam());
        }
        entities.stream().filter(e -> e.getOwner().getTeam() == selectedUnitsTeam).forEach(selectedUnits::add);
    }

    public void setSelectedUnits(Collection<Entity> entities) {
        this.selectedUnits.clear();
        if (entities.isEmpty()) {
            return;
        }
        selectedUnitsTeam = entities.stream().findAny().map(e -> e.getOwner().getTeam()).orElse(localPlayer.getTeam());
        entities.stream().filter(e -> e.getOwner().getTeam() == selectedUnitsTeam).forEach(selectedUnits::add);
    }

    public void setSelectedUnits(Entity entity) {
        this.selectedUnits.clear();
        selectedUnitsTeam = entity.getOwner().getTeam();
        this.selectedUnits.add(entity);
    }

    public Collection<Entity> getSelectedUnits() {
        return selectedUnits;
    }
}
