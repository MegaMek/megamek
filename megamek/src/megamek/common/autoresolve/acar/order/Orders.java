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

package megamek.common.autoresolve.acar.order;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.Collection;
import java.util.Iterator;


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
    public boolean retainAll(Collection<?> c) {
        var changed = false;
        for (Order order : ordersPerPlayer.values()) {
            if (!c.contains(order)) {
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
