package name.modid.client.config;

import name.modid.client.CrystalOptimizerClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CrystalOptimizerConfigScreen extends Screen {

    private final Screen parent;
    private boolean enabled;
    private final List<Button> toggleButtons = new ArrayList<>();
    private boolean changed;

    protected CrystalOptimizerConfigScreen(Screen parent) {
        super(Component.literal("CrystalOptimizer Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        enabled = CrystalOptimizerClient.getConfig().isEnabled();
        changed = false;

        int cx = width / 2;

        addRenderableWidget(Button.builder(
                toggleText("Enabled: ", enabled), button -> {
                    enabled = !enabled;
                    button.setMessage(toggleText("Enabled: ", enabled));
                    changed = true;
                }
        ).bounds(cx - 100, 30, 200, 20).build());

        addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE, button -> saveAndClose()
        ).bounds(cx - 100, height - 28, 200, 20).build());
    }

    public void close() {
        if (changed) save();
        if (minecraft != null) minecraft.setScreenAndShow(parent);
    }

    private void saveAndClose() {
        save();
        if (minecraft != null) minecraft.setScreenAndShow(parent);
    }

    private void save() {
        CrystalOptimizerConfig cfg = CrystalOptimizerClient.getConfig();
        cfg.setEnabled(enabled);
        cfg.save();
    }

    private static Component toggleText(String label, boolean value) {
        return Component.literal(label + (value ? "ON" : "OFF"));
    }

    private static Component entryText(String id, boolean on) {
        String name = id.replace("minecraft:", "");
        return Component.literal((on ? "§a✓ " : "§7  ") + name);
    }
}