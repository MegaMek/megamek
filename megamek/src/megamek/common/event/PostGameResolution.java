package megamek.common.event;

import megamek.common.Entity;

import java.util.Enumeration;

public interface PostGameResolution {
    Enumeration<Entity> getEntities();

    Entity getEntity(int id);

    Enumeration<Entity> getGraveyardEntities();

    Enumeration<Entity> getWreckedEntities();

    Enumeration<Entity> getRetreatedEntities();

    Enumeration<Entity> getDevastatedEntities();
}
