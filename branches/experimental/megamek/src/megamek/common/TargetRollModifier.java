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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetRollModifier)) return false;

        TargetRollModifier that = (TargetRollModifier) o;

        if (cumulative != that.cumulative) return false;
        if (value != that.value) return false;
        if (!desc.equals(that.desc)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value;
        result = 31 * result + desc.hashCode();
        result = 31 * result + (cumulative ? 1 : 0);
        return result;
    }
}