package dev.eliasnvx.femboymod.fabric.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/**
 * Lets the client game test run end with exit code 0. Architectury Transformer's dev runtime leaves two non-daemon
 * thread pools behind ({@code pool-1}, {@code pool-2}, from its class reading), so after the tests stop the client
 * the JVM can't exit and vanilla's shutdown watchdog writes a crash report and exits with -8 fifteen seconds later.
 * Once the main thread has finished a clean shutdown this exits right away. A failed test still crashes the client
 * (and exits non-zero) before getting here. This mod only loads in the game test runs.
 */
public final class ClientTestExit implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            Thread main = Thread.currentThread();
            Thread exit = new Thread(() -> {
                try {
                    main.join();
                } catch (InterruptedException e) {
                    return;
                }
                System.exit(0);
            }, "femboymod-test-exit");
            exit.setDaemon(true);
            exit.start();
        });
    }
}
