package megamek.client.bot;

import java.util.Vector;

import megamek.common.Entity;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;

/**
 * TODO: add more options, pushing, kick both for quad mechs, etc.
 * 
 * also, what are the conditions for multiple physical attacks?
 */
public class PhysicalOption {
    public final static int NONE = 0;
    public final static int PUNCH_LEFT = 1;
    public final static int PUNCH_RIGHT = 2;
    public final static int PUNCH_BOTH = 3;
    public final static int KICK_LEFT = 4;
    public final static int KICK_RIGHT = 5;

    Entity attacker;
    Entity target;
    double expectedDmg;
    int type;

    public PhysicalOption(Entity attacker) {
        this.attacker = attacker;
        this.type = NONE;
    }

    public PhysicalOption(Entity attacker, Entity target, double dmg, int type) {
        this.attacker = attacker;
        this.target = target;
        this.expectedDmg = dmg;
        this.type = type;
    }

    public AbstractAttackAction toAction() {
        switch (type) {
            case PUNCH_LEFT :
                return new PunchAttackAction(attacker.getId(), target.getId(), PunchAttackAction.LEFT);
            case PUNCH_RIGHT :
                return new PunchAttackAction(attacker.getId(), target.getId(), PunchAttackAction.RIGHT);
            case PUNCH_BOTH :
                return new PunchAttackAction(attacker.getId(), target.getId(), PunchAttackAction.BOTH);
            case KICK_LEFT :
                return new KickAttackAction(attacker.getId(), target.getId(), KickAttackAction.LEFT);
            case KICK_RIGHT :
                return new KickAttackAction(attacker.getId(), target.getId(), KickAttackAction.RIGHT);
        }
        return null;
    }

    public Vector getVector() {
        AbstractAttackAction aaa = toAction();
        Vector v = new Vector();
        if (aaa != null) {
            v.addElement(aaa);
        }
        return v;
    }
}
