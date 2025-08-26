/*
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


package megamek.common.autoResolve.acar.order;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;


/**
 * Orders is a collection of orders that are loaded into a game. This is used to give AI players access to strategic
 * objectives for the game being played and be able to change its behavior based on the orders given.
 *
 * @author Luana Coppio
 */
public class Orders implements Collection<Order> {

    private final MultiValuedMap<Integer, Order> ordersPerPlayer = new HashSetValuedHashMap<>();

    public Collection<Order> getOrders(int playerId) {
        return ordersPerPlayer.get(playerId);
    }

    public void resetOrders() {
        ordersPerPlayer.values().forEach(Order::reset);
    }

    @Override
    public int size() {
        return ordersPerPlayer.size();
    }

    public boolean isEmpty() {
        return ordersPerPlayer.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Order order) {
            var orders = ordersPerPlayer.get(order.getOwnerId());
            return orders != null && orders.contains(order);
        }
        return false;
    }

    @Override
    public Iterator<Order> iterator() {
        return ordersPerPlayer.values().iterator();
    }

    @Override
    public Object[] toArray() {
        return ordersPerPlayer.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return ordersPerPlayer.values().toArray(a);
    }

    @Override
    public boolean add(Order order) {
        return ordersPerPlayer.put(order.getOwnerId(), order);
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Order order) {
            return ordersPerPlayer.removeMapping(order.getOwnerId(), order);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return ordersPerPlayer.values().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Order> c) {
        var changed = false;
        for (Order order : c) {
            if (add(order)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        var changed = false;
        for (Object o : c) {
            if (remove(o)) {
                changed = true;
            }

        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        var changed = false;
        for (Order order : ordersPerPlayer.values()) {
            if (!collection.contains(order)) {
                if (remove(order)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        ordersPerPlayer.clear();
    }

}
