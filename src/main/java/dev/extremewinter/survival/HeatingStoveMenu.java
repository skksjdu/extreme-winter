package dev.extremewinter.survival;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class HeatingStoveMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public HeatingStoveMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1), new SimpleContainerData(2)); }
    public HeatingStoveMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(HeatingContent.STOVE_MENU, id); checkContainerSize(container, 1); checkContainerDataCount(data, 2);
        this.container = container; this.data = data;
        addSlot(new Slot(container, 0, 80, 35) { @Override public boolean mayPlace(ItemStack stack) { return StoveFuel.ticks(stack) > 0; } });
        addStandardInventorySlots(inventory, 8, 84); addDataSlots(data);
    }
    public int remainingTicks() { return (data.get(0) & 0xffff) | ((data.get(1) & 0xffff) << 16); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        if (index == 0) { if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY; }
        else if (StoveFuel.ticks(stack) > 0) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
        else if (index < 28) { if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
