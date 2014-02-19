package megamek.common;

import java.io.Serializable;

public class TargetRollModifier implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7228584817530534507L;
    private int value;
    private String desc;
    private boolean cumulative = true;

    public TargetRollModifier(int value, String desc) {
        this.setValue(value);
        this.setDesc(desc);
    }

    public TargetRollModifier(int value, String desc, boolean cumulative) {
        this.setValue(value);
        this.setDesc(desc);
        this.setCumulative(cumulative);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isCumulative() {
        return cumulative;
    }

    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }
}