package exp.nefor.client.util.player.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.Identifier;

public class InventoryUtil {

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static ItemStack checkSlot(int slot) {
        var client = mc();
        if (client.player == null) return ItemStack.EMPTY;
        return client.player.getInventory().getStack(slot);
    }

    public static ItemStack findItem(Item item) {
        var client = mc();
        if (client.player == null) return ItemStack.EMPTY;
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static int totalCount(Item item) {
        var client = mc();
        if (client.player == null) return 0;
        PlayerInventory inv = client.player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) count += stack.getCount();
        }
        return count;
    }

    public static int totalCountPotionInviz() {
        var client = mc();
        if (client.player == null) return 0;
        PlayerInventory inv = client.player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (isInvisibilityPotion(stack)) count += stack.getCount();
        }
        return count;
    }

    public static boolean isInvisibilityPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.isOf(Items.POTION)) return false;

        var potionContent = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContent == null) return false;

        var potionEntryOpt = potionContent.potion();
        if (potionEntryOpt.isEmpty()) return false;

        Identifier stackPotionId = Registries.POTION.getId(potionEntryOpt.get().value());
        if (stackPotionId == null) return false;

        return stackPotionId.equals(Identifier.of("minecraft", "invisibility"))
                || stackPotionId.equals(Identifier.of("minecraft", "long_invisibility"))
                || stackPotionId.equals(Identifier.of("minecraft", "strong_invisibility"));
    }

    public static boolean isBlocked(ItemStack s) {
        return s.isOf(Items.LIME_DYE) || s.isOf(Items.BLACK_STAINED_GLASS_PANE) || s.isOf(Items.ARROW)
                || s.isOf(Items.SPECTRAL_ARROW) || s.isOf(Items.TIPPED_ARROW);
    }

    public static ItemStack checkGuiSlot(int slot) {
        var client = mc();
        if (client.player == null || !(client.currentScreen instanceof GenericContainerScreen))
            return ItemStack.EMPTY;

        var handler = (GenericContainerScreenHandler) client.player.currentScreenHandler;
        if (slot < 0 || slot >= handler.slots.size()) return ItemStack.EMPTY;

        return handler.slots.get(slot).getStack();
    }
}
