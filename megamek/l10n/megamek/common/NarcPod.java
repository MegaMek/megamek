package megamek.common;

import java.io.Serializable;

public class NarcPod implements Serializable {
    private int team;
    private int location;

    public NarcPod(int team, int location) {
        this.team = team;
        this.location = location;
    }

    public int getTeam() {
        return team;
    }
    
    public int getLocation() {
        return location;
    }

    public boolean equals (NarcPod other) {
        if (this.location == other.location
            && this.team == other.team) {
            return true;
        }
        return false;
    }
}
