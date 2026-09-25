package dev.eliasnvx.femboymod.client.render.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pixel masks of the 10 hair clip shapes (same designs as the icons). R fill, O outline, Y gold, G stem, W white. */
final class HairClipShapes {

    static final Map<String, String[]> MASKS = new LinkedHashMap<>();

    static {
        MASKS.put("heart", new String[]{".OO..OO.", "ORRWORRO", "ORWRRRRO", "ORRRRRRO", ".ORRRRO.", "..ORRO..", "...OO..."});
        MASKS.put("star", new String[]{"...OO...", "...RR...", "OORRRROO", ".ORRRRO.", "..RRRR..", ".RRO.ORR.".substring(0, 8), ".O....O."});
        MASKS.put("bow", new String[]{"OO....OO", "ORRO.ORRO".substring(0, 8), "ORRRYRRO", "ORRYYRRO", "ORRRYRRO", "ORRO.ORO", "OO....OO"});
        MASKS.put("flower", new String[]{"..ORRO..", ".ORRRRO.", "ORRYYRRO", "ORYYYYRO", "ORRYYRRO", ".ORRRRO.", "..ORRO.."});
        MASKS.put("moon", new String[]{"..OOO...", ".ORRO...", "ORRO....", "ORR.....", "ORRO..O.", ".ORRRRO.", "..OOOO.."});
        MASKS.put("cherry", new String[]{".....GG.", "....G.G.", "...G..G.", "..G...G.", ".ORO.ORO", "ORWRORWR".substring(0, 8), ".ORO.ORO"});
        MASKS.put("bunny", new String[]{".OO..OO.", ".ORO.ORO".substring(0, 8), ".ORO.ORO", "ORRRRRRO", "ORGRRGRO", "ORRROORO".replace("OORO", "RRRO"), ".OOOOOO."});
        MASKS.put("fish", new String[]{"..OOOO..", ".ORRRRO.O", "ORGRRRROR", "ORRRRRRRO", ".ORRRRO.O", "..OOOO.."});
        MASKS.put("lightning", new String[]{"...OOOO.", "..ORRO..", ".ORRO...", "ORRRROO.", ".OORRO..", "..ORO...", ".OO....."});
        MASKS.put("butterfly", new String[]{"OO....OO", "ORRO.ORRO".substring(0, 8), "ORWRGRWRO".substring(0, 8), ".ORRGRRO", "ORRRGRRO", "ORRO.ORO", "OO....OO"});
    }

    private HairClipShapes() {
    }
}
