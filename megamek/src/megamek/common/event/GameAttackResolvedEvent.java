/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common.event;

import java.io.Serial;

import megamek.common.ResolvedAttack;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/** Client observation at packet receipt. Retains the then-visible entities until listeners copy their render data. */
public final class GameAttackResolvedEvent extends GameEvent {
    @Serial
    private static final long serialVersionUID = 1L;
    private final ResolvedAttack result;
    private final Entity attacker;
    private final Targetable target;

    public GameAttackResolvedEvent(Object source, ResolvedAttack result, Entity attacker, Targetable target) {
        super(source);
        this.result = result;
        this.attacker = attacker;
        this.target = target;
    }

    public ResolvedAttack result() {
        return result;
    }

    public Entity attacker() {
        return attacker;
    }

    public Targetable target() {
        return target;
    }

    @Override
    public void fireEvent(GameListener listener) {
        listener.gameAttackResolved(this);
    }

    @Override
    public String getEventName() {
        return "Resolved attack";
    }
}
