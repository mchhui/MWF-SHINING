package com.modularwarfare.common.container;

import com.modularwarfare.ModConfig;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.container.chest.ChestPaddingInventory;
import com.modularwarfare.common.container.chest.FixedSixRowChestInventory;
import com.modularwarfare.common.container.chest.IPaddingSlot;
import com.modularwarfare.common.container.chest.SlotBackpackDisplayPadding;
import com.modularwarfare.common.container.chest.SlotChestPadding;
import com.modularwarfare.common.guns.ItemAttachment;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.network.PacketGunAddAttachment;
import com.modularwarfare.utility.ChestGuiLayout;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerChestModified extends Container {

    public static final int MAX_CHEST_ROWS = ChestGuiLayout.CHEST_ROWS;
    public static final int MAX_CHEST_SLOTS = ChestGuiLayout.CHEST_SLOTS;
    public static final int PLAYER_SLOT_COUNT = 36;

    private static final EntityEquipmentSlot[] EQUIPMENT_SLOTS = new EntityEquipmentSlot[]{
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
    };


    public final IInventory chestInventory;
    public final FixedSixRowChestInventory chestView;
    public final int chestSlotCount;
    public final EntityPlayer player;
    public IExtraItemHandler extra;

    public ContainerChestModified(final IInventory chestInventory, final EntityPlayer player) {
        this.chestInventory = chestInventory;
        this.chestSlotCount = chestInventory.getSizeInventory();
        this.chestView = new FixedSixRowChestInventory(chestInventory);
        this.player = player;
        this.addSlots(player.inventory, player);
    }

    public static ContainerChestModified fromVanillaChest(final ContainerChest vanilla, final EntityPlayer player) {
        final ContainerChestModified modified = new ContainerChestModified(vanilla.getLowerChestInventory(), player);
        modified.windowId = vanilla.windowId;
        copyStacksFromVanilla(vanilla, modified);
        return modified;
    }

    public static void copyStacksFromVanilla(final Container source, final ContainerChestModified target) {
        for (final Slot targetSlot : target.inventorySlots) {
            final ItemStack stack = findStackInSource(source, target, targetSlot);
            if (!stack.isEmpty()) {
                targetSlot.putStack(stack.copy());
            }
        }
        target.inventoryItemStacks.clear();
        for (final Slot slot : target.inventorySlots) {
            target.inventoryItemStacks.add(slot.getStack());
        }
    }

    @Nullable
    private static ItemStack findStackInSource(final Container source, final ContainerChestModified target, final Slot targetSlot) {
        final IInventory targetInv = targetSlot.inventory;
        final int targetIdx = targetSlot.getSlotIndex();
        for (final Slot sourceSlot : source.inventorySlots) {
            final IInventory sourceInv = sourceSlot.inventory;
            final int sourceIdx = sourceSlot.getSlotIndex();
            if (targetInv == target.chestView && sourceInv == target.chestInventory && sourceIdx == targetIdx) {
                return sourceSlot.getStack();
            }
            if (targetInv == sourceInv && sourceIdx == targetIdx) {
                return sourceSlot.getStack();
            }
        }
        return ItemStack.EMPTY;
    }

    public static ContainerChestModified fromChestInventory(final IInventory chestInventory, final EntityPlayer player, final int windowId) {
        final ContainerChestModified modified = new ContainerChestModified(chestInventory, player);
        modified.windowId = windowId;
        return modified;
    }

    public static boolean isVanillaChestContainer(final Container container) {
        return container instanceof ContainerChest && !(container instanceof ContainerChestModified);
    }

    public int getChestAreaSlotCount() {
        return ChestGuiLayout.CHEST_SLOTS;
    }

    public boolean isChestPaddingSlot(final int containerSlotIndex) {
        return containerSlotIndex >= this.chestSlotCount && containerSlotIndex < getChestAreaSlotCount();
    }

    public boolean isPaddingSlot(final int containerSlotIndex) {
        if (containerSlotIndex < 0 || containerSlotIndex >= this.inventorySlots.size()) {
            return false;
        }
        final Slot slot = this.inventorySlots.get(containerSlotIndex);
        return slot instanceof IPaddingSlot && ((IPaddingSlot) slot).isPaddingSlot();
    }

    public int getBackpackDisplayEnd() {
        return getBackpackContentsStart() + ChestGuiLayout.BACKPACK_DISPLAY_SLOTS;
    }

    public int getPlayerMainStart() {
        return getChestAreaSlotCount();
    }

    public int getHotbarStart() {
        return getChestAreaSlotCount() + 27;
    }

    public int getArmorStart() {
        return getChestAreaSlotCount() + PLAYER_SLOT_COUNT;
    }

    public void applySlotLayout() {
        for (final Slot slot : this.inventorySlots) {
            if (slot.inventory == this.chestView) {
                final int index = slot.getSlotIndex();
                slot.xPos = ChestGuiLayout.chestSlotX(index % ChestGuiLayout.CHEST_COLS);
                slot.yPos = ChestGuiLayout.chestSlotY(index / ChestGuiLayout.CHEST_COLS);
            } else if (slot.inventory instanceof InventoryPlayer) {
                this.applyPlayerInventorySlotLayout(slot);
            } else if (slot instanceof SlotBackpack) {
                slot.xPos = ChestGuiLayout.equipSlotX();
                slot.yPos = ChestGuiLayout.equipSlotY(5);
            } else if (slot instanceof SlotVest) {
                slot.xPos = ChestGuiLayout.equipSlotX();
                slot.yPos = ChestGuiLayout.equipSlotY(6);
            } else if (slot instanceof SlotItemHandler || slot instanceof SlotBackpackDisplayPadding) {
                final int index = slot.getSlotIndex();
                if (index >= 0 && index < ChestGuiLayout.BACKPACK_DISPLAY_SLOTS) {
                    slot.xPos = ChestGuiLayout.backpackSlotX(index % ChestGuiLayout.BACKPACK_COLS);
                    slot.yPos = ChestGuiLayout.backpackSlotY(index / ChestGuiLayout.BACKPACK_COLS);
                }
            }
        }
    }

    private void applyPlayerInventorySlotLayout(final Slot slot) {
        final int index = slot.getSlotIndex();
        if (index >= 9 && index < 36) {
            final int grid = index - 9;
            slot.xPos = ChestGuiLayout.playerSlotX(grid % 9);
            slot.yPos = ChestGuiLayout.playerSlotY(grid / 9);
        } else if (index >= 0 && index < 9) {
            slot.xPos = ChestGuiLayout.playerSlotX(index);
            slot.yPos = ChestGuiLayout.hotbarSlotY();
        } else if (index >= 36 && index <= 39) {
            slot.xPos = ChestGuiLayout.equipSlotX();
            slot.yPos = ChestGuiLayout.equipSlotY(39 - index);
        } else if (index == 40) {
            slot.xPos = ChestGuiLayout.equipSlotX();
            slot.yPos = ChestGuiLayout.equipSlotY(4);
        }
    }

    public int getOffhandIndex() {
        return getArmorStart() + 4;
    }

    public int getBackpackEquipIndex() {
        return getOffhandIndex() + 1;
    }

    public int getVestIndex() {
        return getBackpackEquipIndex() + 1;
    }

    public int getBackpackContentsStart() {
        return getVestIndex() + 1;
    }

    public int getBackpackContentsEnd() {
        return getBackpackContentsStart() + getBackpackContentSlotCount();
    }

    public int getBackpackContentSlotCount() {
        if (this.extra == null || !this.extra.getStackInSlot(0).hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return 0;
        }
        return this.extra.getStackInSlot(0).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getSlots();
    }

    public void addSlots(final InventoryPlayer playerInv, final EntityPlayer entityPlayer) {
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();
        this.extra = entityPlayer.getCapability(CapabilityExtra.CAPABILITY, null);

        for (int index = 0; index < ChestGuiLayout.CHEST_SLOTS; ++index) {
            final int row = index / ChestGuiLayout.CHEST_COLS;
            final int col = index % ChestGuiLayout.CHEST_COLS;
            this.addSlotToContainer(new SlotChestPadding(this.chestView, index,
                    ChestGuiLayout.chestSlotX(col), ChestGuiLayout.chestSlotY(row)));
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                        ChestGuiLayout.playerSlotX(col), ChestGuiLayout.playerSlotY(row)));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInv, col,
                    ChestGuiLayout.playerSlotX(col), ChestGuiLayout.hotbarSlotY()));
        }

        for (int k = 0; k < 4; k++) {
            final EntityEquipmentSlot equipmentSlot = EQUIPMENT_SLOTS[k];
            this.addSlotToContainer(new Slot(playerInv, 36 + (3 - k), ChestGuiLayout.equipSlotX(), ChestGuiLayout.equipSlotY(k)) {
                @Override
                public int getSlotStackLimit() {
                    return 1;
                }

                @Override
                public boolean isItemValid(final ItemStack stack) {
                    return stack.getItem().isValidArmor(stack, equipmentSlot, entityPlayer);
                }

                @Override
                public boolean canTakeStack(final EntityPlayer playerIn) {
                    final ItemStack itemstack = this.getStack();
                    return !itemstack.isEmpty() && !playerIn.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack)
                            ? false : super.canTakeStack(playerIn);
                }

                @Override
                public String getSlotTexture() {
                    return ItemArmor.EMPTY_SLOT_NAMES[equipmentSlot.getIndex()];
                }
            });
        }

        this.addSlotToContainer(new Slot(playerInv, 40, ChestGuiLayout.equipSlotX(), ChestGuiLayout.equipSlotY(4)) {
            @Override
            @Nullable
            public String getSlotTexture() {
                return "minecraft:items/empty_armor_slot_shield";
            }
        });

        if (this.extra != null) {
            this.addSlotToContainer(new SlotBackpack(this.extra, 0, ChestGuiLayout.equipSlotX(), ChestGuiLayout.equipSlotY(5)) {
                @Override
                public void onSlotChanged() {
                    ContainerChestModified.this.addSlots(playerInv, entityPlayer);
                    super.onSlotChanged();
                }
            });

            this.addSlotToContainer(new SlotVest(this.extra, 1, ChestGuiLayout.equipSlotX(), ChestGuiLayout.equipSlotY(6)) {
                @Override
                public void onSlotChanged() {
                    ContainerChestModified.this.addSlots(playerInv, entityPlayer);
                    super.onSlotChanged();
                }
            });
        }

        this.addBackpackDisplaySlots();
        this.applySlotLayout();
    }

    private void addBackpackDisplaySlots() {
        final int usedSlots = this.getBackpackContentSlotCount();
        final IItemHandler backpackInvent = usedSlots > 0
                ? this.extra.getStackInSlot(0).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                : null;

        for (int i = 0; i < ChestGuiLayout.BACKPACK_DISPLAY_SLOTS; i++) {
            final int col = i % ChestGuiLayout.BACKPACK_COLS;
            final int row = i / ChestGuiLayout.BACKPACK_COLS;
            final int x = ChestGuiLayout.backpackSlotX(col);
            final int y = ChestGuiLayout.backpackSlotY(row);

            if (backpackInvent != null && i < usedSlots) {
                this.addBackpackContentSlot(backpackInvent, i, x, y);
            } else {
                this.addSlotToContainer(new SlotBackpackDisplayPadding(ChestPaddingInventory.INSTANCE, i, x, y));
            }
        }
    }

    private void addBackpackContentSlot(final IItemHandler backpackInvent, final int i, final int x, final int y) {
            this.addSlotToContainer(new SlotItemHandler(backpackInvent, i, x, y) {
                @Override
                public boolean isItemValid(@Nonnull final ItemStack stack) {
                    if (stack.getItem() instanceof ItemBackpack) {
                        final ItemBackpack itemBackpack = (ItemBackpack) ContainerChestModified.this.extra.getStackInSlot(0).getItem();
                        if (itemBackpack.type.allowSmallerBackpackStorage) {
                            final int otherBackpackSize = ((ItemBackpack) stack.getItem()).type.size;
                            return otherBackpackSize <= backpackInvent.getSlots();
                        }
                        return false;
                    }
                    if (stack.getItem() instanceof ItemGun) {
                        final ItemBackpack itemBackpack = (ItemBackpack) ContainerChestModified.this.extra.getStackInSlot(0).getItem();
                        if (itemBackpack.type.maxWeaponStorage != null) {
                            return getNumberOfGuns(backpackInvent) < itemBackpack.type.maxWeaponStorage;
                        }
                    }
                    return super.isItemValid(stack);
                }

                private int getNumberOfGuns(final IItemHandler inv) {
                    int numGuns = 0;
                    for (int j = 0; j < inv.getSlots(); j++) {
                        if (inv.getStackInSlot(j).getItem() instanceof ItemGun) {
                            numGuns++;
                        }
                    }
                    return numGuns;
                }
            });
    }

    @Override
    public ItemStack slotClick(final int slotId, final int dragType, final ClickType clickTypeIn, final EntityPlayer entityPlayer) {
        if (slotId >= 0 && slotId < this.inventorySlots.size() && this.isPaddingSlot(slotId)) {
            return ItemStack.EMPTY;
        }
        if (slotId < 0 && slotId != -999) {
            return ItemStack.EMPTY;
        }
        if (slotId >= this.inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        if (clickTypeIn == ClickType.QUICK_MOVE) {
            return super.slotClick(slotId, dragType, clickTypeIn, entityPlayer);
        }

        if (clickTypeIn != ClickType.PICKUP && clickTypeIn != ClickType.QUICK_CRAFT && clickTypeIn != ClickType.THROW) {
            return ItemStack.EMPTY;
        }

        final ItemStack held = entityPlayer.inventory.getItemStack();
        if (ModConfig.INSTANCE.guns.acceptAttachmentDrag && entityPlayer instanceof EntityPlayerMP
                && slotId >= 0 && slotId < this.inventorySlots.size() && slotId != -999
                && dragType == 0 && clickTypeIn == ClickType.PICKUP && !held.isEmpty()) {
            final ItemStack clickItem = this.inventorySlots.get(slotId).getStack();
            if (held.getItem() instanceof ItemAttachment && clickItem.getItem() instanceof ItemGun) {
                final ItemGun gun = (ItemGun) clickItem.getItem();
                if (gun.type.canAcceptAttachment(held)) {
                    new PacketGunAddAttachment(-999).handleServerSide((EntityPlayerMP) entityPlayer);
                    return ItemStack.EMPTY;
                }
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, entityPlayer);
    }

    @Override
    public void onContainerClosed(final EntityPlayer entityPlayer) {
        super.onContainerClosed(entityPlayer);
        this.chestInventory.closeInventory(entityPlayer);
        this.persistBackpackNbt();
    }

    private void persistBackpackNbt() {
        if (this.player.world.isRemote || this.extra == null
                || !this.extra.getStackInSlot(0).hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return;
        }
        final ItemStack backpackStack = this.extra.getStackInSlot(0);
        final IItemHandler backpackInventory = backpackStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (backpackStack.hasTagCompound()) {
            final NBTTagCompound nbt = backpackStack.getTagCompound();
            nbt.setTag("_items", CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.writeNBT(backpackInventory, null));
            backpackStack.setTagCompound(nbt);
        }
    }

    @Override
    public boolean canInteractWith(final EntityPlayer playerIn) {
        return this.chestInventory.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(final EntityPlayer playerIn, final int index) {
        final Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack itemstack = ItemStack.EMPTY;
        final ItemStack sourceStack = slot.getStack();
        itemstack = sourceStack.copy();
        boolean transferred = false;

        final int chestEnd = this.chestSlotCount;
        final int playerMainStart = getPlayerMainStart();
        final int hotbarStart = getHotbarStart();
        final int armorStart = getArmorStart();
        final int backpackContentsStart = getBackpackContentsStart();
        final int backpackContentsEnd = getBackpackContentsEnd();

        if (this.isPaddingSlot(index)) {
            return ItemStack.EMPTY;
        }

        if (index >= 0 && index < chestEnd) {
            transferred = this.mergeItemStack(sourceStack, playerMainStart, hotbarStart + 9, false);
        } else if (index >= armorStart && index < getOffhandIndex()) {
            transferred = this.mergeItemStack(sourceStack, playerMainStart, hotbarStart + 9, false);
        } else if (index == getOffhandIndex()) {
            transferred = this.mergeItemStack(sourceStack, playerMainStart, hotbarStart + 9, false);
        } else if (this.extra != null && (index == getBackpackEquipIndex() || index == getVestIndex())) {
            transferred = this.mergeItemStack(sourceStack, playerMainStart, hotbarStart + 9, false);
            if (!transferred) {
                transferred = this.mergeItemStack(sourceStack, 0, chestEnd, false);
            }
        } else if (index >= playerMainStart && index < hotbarStart) {
            transferred = this.mergeItemStack(sourceStack, 0, chestEnd, false);
            if (!transferred && backpackContentsEnd > backpackContentsStart) {
                transferred = this.mergeItemStack(sourceStack, backpackContentsStart, backpackContentsEnd, false);
            }
            if (!transferred) {
                transferred = this.mergeItemStack(sourceStack, hotbarStart, hotbarStart + 9, false);
            }
        } else if (index >= hotbarStart && index < armorStart) {
            transferred = this.mergeItemStack(sourceStack, 0, chestEnd, false);
            if (!transferred && backpackContentsEnd > backpackContentsStart) {
                transferred = this.mergeItemStack(sourceStack, backpackContentsStart, backpackContentsEnd, false);
            }
            if (!transferred) {
                transferred = this.mergeItemStack(sourceStack, playerMainStart, hotbarStart, false);
            }
        } else if (index >= backpackContentsStart && index < backpackContentsEnd) {
            transferred = this.mergeItemStack(sourceStack, playerMainStart, hotbarStart, false);
            if (!transferred) {
                transferred = this.mergeItemStack(sourceStack, hotbarStart, hotbarStart + 9, false);
            }
            if (!transferred) {
                transferred = this.mergeItemStack(sourceStack, 0, chestEnd, false);
            }
        }

        if (!transferred) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        if (sourceStack.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(playerIn, sourceStack);
        this.persistBackpackNbt();
        return itemstack;
    }
}
