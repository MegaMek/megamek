package megamek.client.ui.swing.tooltip;

import megamek.client.Client;
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
    Game game;
    Client client;

    protected ArrayList<EntityAction> actions = new ArrayList<EntityAction>();
    // need to cache to hit values since it goes to IMPOSSIBLE after action placed in attacks list
    HashMap<EntityAction, String> cache = new HashMap<EntityAction, String>();
    ArrayList<String> descriptions = new ArrayList<>();

    public EntityActionLog(Client client) {
        this.client = client;
        this.game = client.getGame();
    }

    /** @return a list of descrition strings. Note that there may be fewer than the number of actions
     * as similar actions are summarized in a single entry
     */
    public List<String> getDescriptions() {
        return descriptions;
    }

    void rebuild() {
        descriptions.clear();
        for (EntityAction entityAction : cache.keySet()) {
            addDescription(entityAction);
        }
    }

    /**
     * @return a clone of the internal Vector of EntityActions
     */
    public Vector<EntityAction> toVector() {
        return new Vector<>(actions);
    }

    /**
     * remove all items from collection
     */
    @Override
    public void clear() {
        cache.clear();
        descriptions.clear();
    }

    @Override
    public boolean add(EntityAction entityAction) {
        if (!actions.add(entityAction)) {
            return false;
        }
        addDescription(entityAction);
        return true;
    }

    public void add(int index, EntityAction entityAction) {
        actions.add(index, entityAction);
        addDescription(entityAction);
    }

    /**
     * Remove an item and its description cache
     * @param entityAction
     */
    @Override
    public boolean remove(Object o) {
        if (!actions.remove(o)) {
            return false;
        }
        cache.remove(o);
        rebuild();
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
        for (var a : c) {
            addDescription(a);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (!actions.removeAll(c)) {
            return false;
        }
        for (var a : c) {
            cache.remove(a);
        }
        rebuild();
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

    void addDescription(EntityAction entityAction) {
        if (entityAction instanceof WeaponAttackAction) {
            addWeapon((WeaponAttackAction) entityAction);
        } else if (entityAction instanceof KickAttackAction) {
            addWeapon((KickAttackAction) entityAction);
        } else if (entityAction instanceof PunchAttackAction) {
            addWeapon((PunchAttackAction) entityAction);
        } else if (entityAction instanceof PushAttackAction) {
            addWeapon((PushAttackAction) entityAction);
        } else if (entityAction instanceof ClubAttackAction) {
            addWeapon((ClubAttackAction) entityAction);
        } else if (entityAction instanceof ChargeAttackAction) {
            addWeapon((ChargeAttackAction) entityAction);
        } else if (entityAction instanceof DfaAttackAction) {
            addWeapon((DfaAttackAction) entityAction);
        } else if (entityAction instanceof ProtomechPhysicalAttackAction) {
            addWeapon((ProtomechPhysicalAttackAction) entityAction);
        } else if (entityAction instanceof SearchlightAttackAction) {
            addWeapon((SearchlightAttackAction) entityAction);
        } else if (entityAction instanceof SpotAction) {
            addWeapon((SpotAction) entityAction);
        } else {
            cache.put(entityAction, "");
            descriptions.add(entityAction.toDisplayableString(client));
        }
    }

    /**
     * Adds a weapon to this attack
     */
    void addWeapon(WeaponAttackAction attack) {
        final Entity entity = game.getEntity(attack.getEntityId());
        final WeaponType wtype = (WeaponType) entity.getEquipment(attack.getWeaponId()).getType();

        final String roll;
        if (cache.containsKey(attack)) {
            roll = cache.get(attack);
        } else {
            roll = attack.toHit(game).getValueAsString();
            cache.put(attack, roll);
        }

        String table = attack.toHit(game).getTableDesc();
        if (!table.isEmpty()) {
            table = " " + table;
        }
        boolean b = false;
        ListIterator<String> i = descriptions.listIterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.contains(wtype.getName())) {
                i.set(s + ", " + roll + table);
                b = true;
            }
        }

        if (!b) {
            descriptions.add(wtype.getName() + Messages.getString("BoardView1.needs") + roll + table);
        }
    }

    void addWeapon(KickAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            String rollLeft;
            String rollRight;
            final int leg = attack.getLeg();
            switch (leg) {
                case KickAttackAction.BOTH:
                    rollLeft = KickAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.LEFT).getValueAsString();
                    rollRight = KickAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.RIGHT).getValueAsString();
                    buffer = Messages.getString("BoardView1.kickBoth", rollLeft, rollRight);
                    break;
                case KickAttackAction.LEFT:
                    rollLeft = KickAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.LEFT).getValueAsString();
                    buffer = Messages.getString("BoardView1.kickLeft", rollLeft);
                    break;
                case KickAttackAction.RIGHT:
                    rollRight = KickAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), KickAttackAction.RIGHT).getValueAsString();
                    buffer = Messages.getString("BoardView1.kickRight", rollRight);
                    break;
                default:
                    buffer = "Error on kick action";
            }
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(PunchAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            String rollLeft;
            String rollRight;
            final int arm = attack.getArm();
            switch (arm) {
                case PunchAttackAction.BOTH:
                    rollLeft = PunchAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.LEFT, false).getValueAsString();
                    rollRight = PunchAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.RIGHT, false).getValueAsString();
                    buffer = Messages.getString("BoardView1.punchBoth", rollLeft, rollRight);
                    break;
                case PunchAttackAction.LEFT:
                    rollLeft = PunchAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.LEFT, false).getValueAsString();
                    buffer = Messages.getString("BoardView1.punchLeft", rollLeft);
                    break;
                case PunchAttackAction.RIGHT:
                    rollRight = PunchAttackAction.toHit(game, attack.getEntityId(), game.getTarget(attack.getTargetType(), attack.getTargetId()), PunchAttackAction.RIGHT, false).getValueAsString();
                    buffer = Messages.getString("BoardView1.punchRight", rollRight);
                    break;
                default:
                    buffer = "Error on punch action";
            }
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(PushAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            final String roll = attack.toHit(game).getValueAsString();
            buffer = Messages.getString("BoardView1.push", roll);
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(ClubAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            final String roll = attack.toHit(game).getValueAsString();
            final String club = attack.getClub().getName();
            buffer = Messages.getString("BoardView1.hit", club, roll);
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(ChargeAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            final String roll = attack.toHit(game).getValueAsString();
            buffer = Messages.getString("BoardView1.charge", roll);
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(DfaAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            final String roll = attack.toHit(game).getValueAsString();
            buffer = Messages.getString("BoardView1.DFA", roll);
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(ProtomechPhysicalAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            final String roll = attack.toHit(game).getValueAsString();
            buffer = Messages.getString("BoardView1.proto", roll);
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(SearchlightAttackAction attack) {
        String buffer;
        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            buffer = Messages.getString("BoardView1.Searchlight");
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
    }

    void addWeapon(SpotAction attack) {
        String buffer;
        Entity target = game.getEntity(attack.getTargetId());

        if (cache.containsKey(attack)) {
            buffer = cache.get(attack);
        } else {
            buffer = Messages.getString("BoardView1.Spot", (target != null) ? target.getShortName() : "" );
            cache.put(attack, buffer);
        }
        descriptions.add(buffer);
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
}
