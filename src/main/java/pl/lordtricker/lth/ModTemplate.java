package pl.lordtricker.lth;

import net.fabricmc.api.ModInitializer;
import pl.lordtricker.lth.util.Messages;

public class ModTemplate implements ModInitializer {
    public static final String MOD_ID = "lth";

    @Override
    public void onInitialize() {
        Messages.init();
    }
}
