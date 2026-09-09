package exp.nefor.client.module.impl.player;

import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.api.Category;
import exp.nefor.client.module.ModuleManager;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.util.player.chat.ChatUtil;
import exp.nefor.client.util.client.ClickUtil;
import exp.nefor.client.util.Color;
import exp.nefor.client.util.player.inventory.InventoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AutoSell extends Module {

    private static final String SELL_GUI = "Прода";
    private static final String AUCTION_GUI = "Аук";
    private static final String STORAGE_GUI_1 = "Хранилище";
    private static final String STORAGE_GUI_2 = "Ender Chest";

    private int state = 0;
    private int waitTimer = 0;
    private boolean loop = false;

    private final SliderSetting clickDelay = new SliderSetting("Задержка кликов", 40, 300, 5, 70);

    public AutoSell() {
        super("AutoSell", "Автопродажа инвизок на аукционе", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        addSettings(clickDelay);
    }

    public boolean isLoopEnabled() {
        return loop;
    }

    @Override
    protected void onEnable() {
        if (state != 0) return;
        loop = true;
        startCycle();
    }

    @Override
    protected void onDisable() {
        loop = false;
        state = 0;
        ClickUtil.clear();
        RenderSystem.notification("Автопродажа остановлена", Color.YELLOW);
    }

    @Override
    public void onTick() {
        int delay = (int) clickDelay.getValue();
        ClickUtil.setDefaultPause(delay, delay + 30);
        tickSelling(MinecraftClient.getInstance());
    }

    private void startCycle() {
        state = 1;
        waitTimer = 0;
        ChatUtil.sendMessage("/ah sellgui 50000");
    }

    private void tickSelling(MinecraftClient client) {
        if (client.player == null) return;

        switch (state) {
            case 1 -> {
                if (client.currentScreen instanceof GenericContainerScreen screen
                        && screen.getTitle().getString().contains(SELL_GUI)) {
                    moveSlotSelectedAutoSell();
                    state = 2;
                    ClickUtil.add(client.player::closeHandledScreen);
                    waitTimer = ClickUtil.rand(100, 110);
                } else if (++waitTimer > 200) {
                    RenderSystem.notification("Гуи не открыто", Color.RED);
                    setEnabled(false);
                }
            }

            case 2 -> {
                if (--waitTimer <= 0) {
                    ChatUtil.sendMessage("/ah");
                    waitTimer = 10;
                } else {
                    int syncId = client.player.currentScreenHandler.syncId;
                    if (client.currentScreen instanceof GenericContainerScreen screen
                            && screen.getTitle().getString().contains(AUCTION_GUI)) {
                        ClickUtil.add(() -> {
                            if (client.interactionManager != null) {
                                client.interactionManager.clickSlot(syncId, 46, 0, SlotActionType.PICKUP, client.player);
                            }
                        });
                        state = 3;
                    }
                }
            }

            case 3 -> {
                if (client.interactionManager == null) return;
                if (!(client.currentScreen instanceof GenericContainerScreen screen)) return;

                String title = screen.getTitle().getString();
                if (!title.contains(STORAGE_GUI_1) && !title.contains(STORAGE_GUI_2)) return;

                state = 4;
                scheduleStorageClear();
            }

            case 4 -> {
                if (ClickUtil.isEmpty()) {
                    if (loop) {
                        RenderSystem.notification("Цикл завершён, начинаю заново...", Color.GREEN);
                        startCycle();
                    } else {
                        RenderSystem.notification("Закончил епта", Color.GREEN);
                        setEnabled(false);
                    }
                }
            }
        }
    }

    private void moveSlotSelectedAutoSell() {
        var client = MinecraftClient.getInstance();
        var inter = client.interactionManager;
        if (client.player == null || inter == null) return;

        if (!(client.currentScreen instanceof GenericContainerScreen screen)) return;
        String title = screen.getTitle().getString();
        if (!title.contains(SELL_GUI)) {
            RenderSystem.notification("Не тот GUI", Color.RED);
            return;
        }

        var handler = (GenericContainerScreenHandler) client.player.currentScreenHandler;
        int syncId = handler.syncId;
        int containerStart = handler.getRows() * 9;
        int totalSlots = handler.slots.size();

        List<Integer> freeSlots = new ArrayList<>();
        for (int i = 0; i < containerStart; i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (InventoryUtil.isBlocked(stack)) continue;
            if (stack.isEmpty()) freeSlots.add(i);
        }
        if (freeSlots.isEmpty()) {
            RenderSystem.notification("Нет свободных лотов", Color.RED);
            return;
        }

        int sourceSlot = -1;
        for (int i = containerStart; i < totalSlots; i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (InventoryUtil.isInvisibilityPotion(stack) && !stack.isEmpty()) {
                sourceSlot = i;
                break;
            }
        }
        if (sourceSlot == -1) {
            RenderSystem.notification("Нету инвиза в инвентаре", Color.RED);
            return;
        }

        ItemStack sellStack = handler.slots.get(sourceSlot).getStack();
        int toPlace = Math.min(freeSlots.size(), sellStack.getCount());
        final int fSync = syncId;
        final int fSrc = sourceSlot;

        ClickUtil.add(() -> inter.clickSlot(fSync, fSrc, 0, SlotActionType.PICKUP, client.player));

        for (int k = 0; k < toPlace; k++) {
            final int slot = freeSlots.get(k);
            ClickUtil.add(() -> inter.clickSlot(fSync, slot, 1, SlotActionType.PICKUP, client.player));
        }

        ClickUtil.add(() -> inter.clickSlot(fSync, fSrc, 0, SlotActionType.PICKUP, client.player));

        int confirm = -1;
        for (int i = 0; i < totalSlots; i++) {
            if (handler.slots.get(i).getStack().isOf(Items.LIME_DYE)) {
                confirm = i;
                break;
            }
        }
        if (confirm != -1) {
            final int fConfirm = confirm;
            ClickUtil.add(() -> inter.clickSlot(fSync, fConfirm, 0, SlotActionType.PICKUP, client.player));
        }

        ClickUtil.add(client.player::closeHandledScreen);

        RenderSystem.notification("Разложил", Color.GREEN);
    }

    private void scheduleStorageClear() {
        ClickUtil.add(() -> ClickUtil.pauseMs(150, 160));
        ClickUtil.add(() -> {
            var client = MinecraftClient.getInstance();
            if (client.player == null || client.interactionManager == null) return;
            if (!(client.currentScreen instanceof GenericContainerScreen screen)) return;

            String currentTitle = screen.getTitle().getString();
            if (!currentTitle.contains(STORAGE_GUI_1) && !currentTitle.contains(STORAGE_GUI_2)) return;

            var handler = (GenericContainerScreenHandler) client.player.currentScreenHandler;

            int foundSlot = -1;
            for (int i = 0; i < 27; i++) {
                ItemStack item = handler.slots.get(i).getStack();
                if (InventoryUtil.isInvisibilityPotion(item) && !item.isEmpty()) {
                    foundSlot = i;
                    break;
                }
            }

            if (foundSlot == -1) {
                client.player.closeHandledScreen();
                return;
            }

            client.interactionManager.clickSlot(handler.syncId, foundSlot, 1, SlotActionType.PICKUP, client.player);
            scheduleStorageClear();
        });
    }
}
