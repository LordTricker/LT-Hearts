package pl.lordtricker.modid;

import net.fabricmc.api.ModInitializer;
import pl.lordtricker.modid.util.Messages;

public class ModTemplate implements ModInitializer {
    public static final String MOD_ID = "modid";

    @Override
    public void onInitialize() {
        Messages.init();
    }
}
