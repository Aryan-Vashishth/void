package core.misc;

import core.runtime.VOID;

public class VoidDemo {
    public static void main(String[] args) {
        VOID app = VOID.start();
        System.out.println("VOID instance created: " + app);
        app.shutdown();
    }
}
