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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Order {

    private final int id;
    private final int ownerId;
    private final int priority;
    private final OrderType orderType;
    private final int targetId;
    private final int formationId;
    private final int orderDelay;
    private final Condition condition;

    public Order(int id, int ownerId, int priority, OrderType orderType, int targetId, int formationId, int orderDelay, Condition condition) {
        this.id = id;
        this.ownerId = ownerId;
        this.priority = priority;
        this.orderType = orderType;
        this.targetId = targetId;
        this.formationId = formationId;
        this.orderDelay = orderDelay;
        this.condition = condition;
    }

    public Order(int id, int ownerId, int priority, OrderType orderType, Condition condition) {
        this.id = id;
        this.ownerId = ownerId;
        this.priority = priority;
        this.orderType = orderType;
        this.condition = condition;
        this.targetId = -1;
        this.formationId = -1;
        this.orderDelay = -1;
    }

    public Order(int id, int ownerId, int priority, OrderType orderType) {
        this.id = id;
        this.ownerId = ownerId;
        this.priority = priority;
        this.orderType = orderType;
        this.condition = null;
        this.targetId = -1;
        this.formationId = -1;
        this.orderDelay = -1;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getPriority() {
        return priority;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getTargetId() {
        return targetId;
    }

    public int getFormationId() {
        return formationId;
    }

    public int getOrderDelay() {
        return orderDelay;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Order order)) return false;

        return new EqualsBuilder()
            .append(getId(), order.getId())
            .append(getOwnerId(), order.getOwnerId())
            .append(getPriority(), order.getPriority())
            .append(getTargetId(), order.getTargetId())
            .append(getFormationId(), order.getFormationId())
            .append(getOrderDelay(), order.getOrderDelay())
            .append(getOrderType(), order.getOrderType())
            .append(getCondition(), order.getCondition())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getId()).append(getOwnerId()).append(getPriority()).append(getOrderType()).append(getTargetId())
            .append(getFormationId()).append(getOrderDelay()).append(getCondition()).toHashCode();
    }
}
