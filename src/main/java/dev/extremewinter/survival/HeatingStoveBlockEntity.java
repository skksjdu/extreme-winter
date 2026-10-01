package dev.extremewinter.survival;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Native block entity ticking stops on pause/offline/unload; no wall-clock catch-up. */
public final class HeatingStoveBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int remaining;
    private java.util.UUID owner;
    private boolean ownerNotified;
    public final ContainerData data = new ContainerData() {
        public int get(int index) { return index == 0 ? remaining & 0xffff : remaining >>> 16; }
        public void set(int index, int value) {
            remaining = Math.clamp(index == 0 ? (remaining & 0xffff0000) | (value & 0xffff) : (remaining & 0xffff) | ((value & 0xffff) << 16), 0, 86400);
        }
        public int getCount() { return 2; }
    };
    public HeatingStoveBlockEntity(BlockPos pos, BlockState state) { super(HeatingContent.STOVE_ENTITY, pos, state); }
    public int remainingTicks() { return remaining; }
    public void setOwner(java.util.UUID owner) { this.owner = owner; ownerNotified = false; setChanged(); }
    @Override public int getContainerSize() { return 1; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && StoveFuel.ticks(stack) > 0; }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override protected Component getDefaultName() { return Component.translatable("block.extreme_winter.heating_stove"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new HeatingStoveMenu(id, inventory, this, data); }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(1, ItemStack.EMPTY); ContainerHelper.loadAllItems(input, items);
        remaining = Math.clamp(input.getIntOr("remainingTicks", 0), 0, 86400);
        owner = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        ownerNotified = false;
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); output.putInt("schemaVersion", 1);
        ContainerHelper.saveAllItems(output, items); output.putInt("remainingTicks", remaining);
        if (owner != null) output.store("owner", net.minecraft.core.UUIDUtil.CODEC, owner);
    }
    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components); remaining = components.getOrDefault(HeatingContent.REMAINING, 0); setChanged();
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder); if (remaining > 0) builder.set(HeatingContent.REMAINING, remaining);
    }
    @Override public void removeComponentsFromTag(ValueOutput output) { super.removeComponentsFromTag(output); output.discard("remainingTicks"); }
    public void copyToDrop(ItemStack stack) { if (stack.is(HeatingContent.STOVE_ITEM) && remaining > 0) stack.set(HeatingContent.REMAINING, remaining); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof net.minecraft.server.level.ServerLevel) { Containers.dropContents(level, pos, this); clearContent(); }
        super.preRemoveSideEffects(pos, state);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, HeatingStoveBlockEntity stove) {
        int before = stove.remaining;
        if (stove.remaining > 0) stove.remaining--;
        if (stove.remaining == 0) {
            var fuel = stove.items.getFirst(); int duration = StoveFuel.ticks(fuel);
            if (duration > 0 && !fuel.isEmpty()) { fuel.shrink(1); stove.remaining = duration; }
        }
        boolean lit = stove.remaining > 0;
        if (lit && stove.owner != null && !stove.ownerNotified && level instanceof net.minecraft.server.level.ServerLevel world) {
            var player = world.getServer().getPlayerList().getPlayer(stove.owner);
            if (player != null) { WinterTasks.stoveBurned(player); stove.ownerNotified = true; }
        }
        if (lit != state.getValue(HeatingStoveBlock.LIT)) level.setBlock(pos, state.setValue(HeatingStoveBlock.LIT, lit), Block.UPDATE_ALL);
        if (before != stove.remaining) stove.setChanged();
    }
}
