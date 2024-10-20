package megamek.client.ui.advancedsearch;

public enum WeaponClass {
    AUTOCANNON {
        public String toString() {
            return "Autocannon";
        }
    },
    RAC,
    ULTRA {
        public String toString() {
            return "Ultra A/C";
        }
    },
    LIGHT {
        public String toString() {
            return "Light A/C";
        }
    },
    MACHINE_GUN {
        public String toString() {
            return "Machine Gun";
        }
    },
    GAUSS {
        public String toString() {
            return "Gauss";
        }
    },
    BALLISTIC {
        public String toString() {
            return "Ballistic";
        }
    },
    PLASMA {
        public String toString() {
            return "Plasma";
        }
    },
    ENERGY {
        public String toString() {
            return "Energy";
        }
    },
    LASER {
        public String toString() {
            return "Laser";
        }
    },
    PULSE {
        public String toString() {
            return "Pulse Laser";
        }
    },
    RE_ENGINEERED {
        public String toString() {
            return "Re-Engineered Laser";
        }
    },
    PPC {
        public String toString() {
            return "PPC";
        }
    },
    TASER {
        public String toString() {
            return "Taser";
        }
    },
    FLAMER {
        public String toString() {
            return "Flamer";
        }
    },
    MISSILE {
        public String toString() {
            return "Missile";
        }
    },
    LRM,
    MRM,
    SRM,
    PHYSICAL {
        public String toString() {
            return "Physical (inc. industrial equipment)";
        }
    },
    AMS,
    PRACTICAL_PHYSICAL {
        public String toString() {
            return "Physical (weapons only)";
        }
    };

    public boolean matches(String name) {
        if (name.toLowerCase().contains("ammo")) {
            return false;
        }
        if (this == PHYSICAL) {
            String lName = name.toLowerCase();

            if (lName.contains("backhoe") ||
                lName.contains("saw") ||
                lName.contains("whip") ||
                lName.contains("claw") ||
                lName.contains("combine") ||
                lName.contains("flail") ||
                lName.contains("hatchet") ||
                lName.contains("driver") ||
                lName.contains("lance") ||
                lName.contains("mace") ||
                lName.contains("drill") ||
                lName.contains("ram") ||
                lName.contains("blade") ||
                lName.contains("cutter") ||
                lName.contains("shield") ||
                lName.contains("welder") ||
                lName.contains("sword") ||
                lName.contains("talons") ||
                lName.contains("wrecking")) {
                return true;
            }
        } else if (this == PRACTICAL_PHYSICAL) {
            String lName = name.toLowerCase();

            if (lName.contains("claw") ||
                lName.contains("flail") ||
                lName.contains("hatchet") ||
                lName.contains("lance") ||
                lName.contains("mace") ||
                lName.contains("blade") ||
                lName.contains("shield") ||
                lName.contains("sword") ||
                lName.contains("talons")) {
                return true;
            }
        } else if (this == MISSILE) {
            if ((name.toLowerCase().contains("lrm") ||
                name.toLowerCase().contains("mrm") ||
                name.toLowerCase().contains("srm")) &&
                !name.toLowerCase().contains("ammo")) {
                return true;
            }
        } else if (this == RE_ENGINEERED) {
            if (name.toLowerCase().contains("engineered")) {
                return true;
            }
        } else if (this == ENERGY) {
            if (WeaponClass.LASER.matches(name) || WeaponClass.PPC.matches(name) || WeaponClass.FLAMER.matches(name)) {
                return true;
            }
        } else if (this == MACHINE_GUN) {
            if ((name.toLowerCase().contains("mg") || name.toLowerCase().contains("machine")) && !name.toLowerCase().contains("ammo")) {
                return true;
            }
        } else if (this == BALLISTIC) {
            return WeaponClass.AUTOCANNON.matches(name) ||
                WeaponClass.GAUSS.matches(name) ||
                WeaponClass.MISSILE.matches(name) ||
                WeaponClass.MACHINE_GUN.matches(name);
        } else if (this == RAC) {
            if (name.toLowerCase().contains("rotary")) {
                return true;
            }
        } else if (this == ULTRA) {
            if (name.toLowerCase().contains("ultraa")) {
                return true;
            }
        } else if (name.toLowerCase().contains(this.name().toLowerCase()) && !name.toLowerCase().contains("ammo")) {
            return true;
        }
        return false;
    }
}
