package dev.eliasnvx.femboymod.halloween;

import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import java.time.LocalDate;
import java.time.Month;

/** Whether it is Halloween for trick-or-treat. Pure date logic, covered by JUnit. */
public final class HalloweenSeason {

    /** Same window as vanilla's pumpkin-wearing mobs: October 20 to November 3. */
    private static final int OCTOBER_FROM = 20;
    private static final int NOVEMBER_TO = 3;

    private HalloweenSeason() {
    }

    public static boolean isHalloween(LocalDate date) {
        return date.getMonth() == Month.OCTOBER && date.getDayOfMonth() >= OCTOBER_FROM
                || date.getMonth() == Month.NOVEMBER && date.getDayOfMonth() <= NOVEMBER_TO;
    }

    public static boolean active(CommonConfig.Season season, LocalDate today) {
        return switch (season) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> isHalloween(today);
        };
    }

    /** With the server's config and today's date. */
    public static boolean active() {
        return active(FemboyConfig.common().halloween().season(), LocalDate.now());
    }
}
