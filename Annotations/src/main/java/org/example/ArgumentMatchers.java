package org.example;

public class ArgumentMatchers {

    static Object ANY = new Object();

    public static int anyInt() {
        OurMockito.addArg(ANY);
        return 0;
    }

    public static int eq(int val) {
        OurMockito.addArg(val);
        return 0;
    }

    public static float eq(float val) {
        OurMockito.addArg(val);
        return 0;
    }

    public static String eq(String val) {
        OurMockito.addArg(val);
        return "";
    }
}
