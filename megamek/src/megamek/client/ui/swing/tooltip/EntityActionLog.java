/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.ui.swing.tooltip;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.actions.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * An ordered List-like collection of EntityActions with a cached description of each action,
 * created as the action is added.
 */
public class EntityActionLog implements Collection<EntityAction> {
    private final Game game;
    private final ArrayList<EntityAction> actions = new ArrayList<EntityAction>();
    // cache to prevent regeneration of info if action already processed
    private final HashMap<EntityAction, String> infoCache = new HashMap<EntityAction, String>();
    private final ArrayList<String> descriptions = new ArrayList<>();

    public EntityActionLog(Game game) {
        this.game = game;
    }

    /** @return a list of description strings. Note that there may be fewer than the number of actions
     * as similar actions are summarized in a single entry
     */
    public List<String> getDescriptions() {
        return descriptions;
    }

    /** repopulate the descriptions for each action, using the cache, or recomputing as appropriate */
    public void rebuildDescriptions() {
        descriptions.clear();
        for (EntityAction entityAction : actions) {
            addDescription(entityAction);
        }
    }

    /**
     * @return a clone of the internal List of EntityActions
     */
    public Vector<EntityAction> toVector() {
        return new Vector<>(actions);
    }

    /**
     * remove all items from all internal collections
     */
    @Override
    public void clear() {
        actions.clear();
        infoCache.clear();
        descriptions.clear();
    }

    @Override
    public boolean add(EntityAction entityAction) {
        if (!actions.add(entityAction)) {
            return false;
        }
        rebuildDescriptions();
        return true;
    }

    public void add(int index, EntityAction entityAction) {
        actions.add(index, entityAction);
        rebuildDescriptions();
    }

    /**
     * Remove an item and its caches
     */
    @Override
    public boolean remove(Object o) {
        if (!actions.remove(o)) {
            return false;
        }
        infoCache.remove(o);
        rebuildDescriptions();
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return actions.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends EntityAction> c) {
        if (!actions.addAll(c)) {
            return false;
        }
        rebuildDescriptions();
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (!actions.removeAll(c)) {
            return false;
        }
        for (var a : c) {
            infoCache.remove(a);
        }
        rebuildDescriptions();
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return actions.retainAll(c);
    }

    public EntityAction firstElement() {
        return actions.isEmpty() ? null : actions.get(0);
    }

    public EntityAction lastElement() {
        return actions.isEmpty() ? null : actions.get(actions.size()-1);
    }

    @Override
    public boolean isEmpty() {
        return actions.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return actions.contains(o);
    }

    @Override
    public int size() {
        return actions.size();
    }

    @Override
    public Iterator<EntityAction> iterator() {
        return actions.iterator();
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return actions.toArray(a);
    }

    @Override
    public Stream<EntityAction> stream() {
        return actions.stream();
    }

    private void addDescription(EntityAction entityAction) {
        if (entityAction instanceof WeaponAttackAction) {
            // ToHit may change as other actions are added,
            // so always re-evaluate all WeaponAttack Actions
            addWeaponAttackAction((WeaponAttackAction) entityAction);
        } else if (infoCache.containsKey(entityAction)) {
            // reuse previous description
            descriptions.add(infoCache.get(entityAction));
        } else {
            final String buffer = entityAction.toSummaryString(game);
            descriptions.add(buffer);
            infoCache.put(entityAction, buffer);
        }
    }

    /**
     * Adds description, using an existing line if the same weapon type was already listed
     */
    private void addWeaponAttackAction(WeaponAttackAction attack) {
        ToHitData toHit = attack.toHit(game, true);
        String table = toHit.getTableDesc();
        final String buffer = toHit.getValueAsString() + ((!table.isEmpty()) ? ' ' + table : "");
        final Entity entity = game.getEntity(attack.getEntityId());
        final String weaponName = (entity.getEquipment(attack.getWeaponId()).getType()).getName();
        final Mounted ammo = entity.getEquipment(attack.getAmmoId());
        final String ammoName = (ammo == null) ? "" : " [" + ((AmmoType) ammo.getType()).getShortName() + "] ";

        //add to an existing entry if possible
        boolean found = false;
        ListIterator<String> i = descriptions.listIterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.startsWith(weaponName)) {
                i.set(s + ", " + buffer);
                found = true;
                break;
            }
        }

        if (!found) {
            descriptions.add( weaponName + ammoName + Messages.getString("BoardView1.needs") + buffer);
        }
    }
}
