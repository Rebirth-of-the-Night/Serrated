package st.evening.mc.rotnsaws.util;

public enum SerrUtils {
    ;

    public static int ceilDiv(final int p, final int q) {
        return -Math.floorDiv(-p, q);
    }
}
