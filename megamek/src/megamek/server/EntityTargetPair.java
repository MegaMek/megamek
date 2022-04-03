package megamek.server;

import megamek.common.Entity;
import megamek.common.Targetable;

import java.util.Objects;

public  class EntityTargetPair {
    Entity ent;

    Targetable target;

    EntityTargetPair (Entity e, Targetable t) {
        ent = e;
        target = t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((null == o) || (getClass() != o.getClass())) {
            return false;
        }
        final EntityTargetPair other = (EntityTargetPair) o;
        return Objects.equals(ent, other.ent) && Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ent, target);
    }

}