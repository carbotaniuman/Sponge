/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.item;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.spongepowered.common.data.util.DataUtil.getData;
import static org.spongepowered.common.data.util.ItemsHelper.validateData;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackBuilder;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.interfaces.data.IMixinCustomDataHolder;
import org.spongepowered.common.inventory.SpongeItemStackSnapshot;
import org.spongepowered.common.service.persistence.NbtTranslator;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

public class SpongeItemStackBuilder implements ItemStackBuilder {
    private Set<DataManipulator<?, ?>> itemDataSet;
    private ItemType type;
    private int quantity;
    private int maxQuantity;
    private int damageValue = 0;
    @Nullable private NBTTagCompound compound;

    public SpongeItemStackBuilder() {
        reset();
    }

    @Override
    public ItemStackBuilder itemType(ItemType itemType) {
        checkNotNull(itemType, "Item type cannot be null");
        this.type = itemType;
        return this;
    }

    @Override
    public ItemStackBuilder quantity(int quantity) throws IllegalArgumentException {
        checkArgument(quantity > 0, "Quantity must be greater than 0");
        this.quantity = quantity;
        return this;
    }


    @Override
    public ItemStackBuilder itemData(ImmutableDataManipulator<?, ?> itemData) throws IllegalArgumentException {
        return itemData(itemData.asMutable());
    }

    @Override
    public ItemStackBuilder itemData(final DataManipulator<?, ?> itemData) throws IllegalArgumentException {
        checkNotNull(itemData, "Must have a non-null item data!");
        checkNotNull(this.type, "Cannot set item data without having set a type first!");
        // Validation is required, we can't let devs set block data on a non-block item!
        DataTransactionResult result = validateData(this.type, itemData);
        if (result.getType() != DataTransactionResult.Type.SUCCESS) {
            throw new IllegalArgumentException("The item data is not compatible with the current item type!");
        } else {
            if (this.itemDataSet == null) {
                this.itemDataSet = new HashSet<>();
            }
            this.itemDataSet.add(itemData);
            return this;
        }
    }

    @Override
    public ItemStackBuilder fromItemStack(ItemStack itemStack) {
        checkNotNull(itemStack, "Item stack cannot be null");
        this.itemDataSet = new HashSet<>();
        // Assumes the item stack's values don't need to be validated
        this.type = itemStack.getItem();
        this.quantity = itemStack.getQuantity();
        this.maxQuantity = itemStack.getMaxStackQuantity();
        if (itemStack instanceof net.minecraft.item.ItemStack) {
            final NBTTagCompound itemCompound = ((net.minecraft.item.ItemStack) itemStack).getTagCompound();
            if (itemCompound != null) {
                this.compound = (NBTTagCompound) itemCompound.copy();
            }
            this.itemDataSet.addAll(((IMixinCustomDataHolder) itemStack).getCustomManipulators());

        } else {
            this.itemDataSet.addAll(itemStack.getContainers());
        }
        return this;
    }

    @Override
    public ItemStackBuilder fromContainer(DataView container) {
        checkNotNull(container);
        if (!container.contains(DataQueries.ITEM_TYPE) || !container.contains(DataQueries.ITEM_COUNT)
            || !container.contains(DataQueries.ITEM_DAMAGE_VALUE)) {
            return this;
        }
        reset();

        final int count = getData(container, DataQueries.ITEM_COUNT, Integer.class);
        quantity(count);

        final String itemTypeId = getData(container, DataQueries.ITEM_TYPE, String.class);
        final ItemType itemType = Sponge.getSpongeRegistry().getType(ItemType.class, itemTypeId).get();
        itemType(itemType);

        this.damageValue = getData(container, DataQueries.ITEM_DAMAGE_VALUE, Integer.class);
        if (container.contains(DataQueries.UNSAFE_NBT)) {
            final NBTTagCompound compound = NbtTranslator.getInstance().translateData(container.getView(DataQueries.UNSAFE_NBT).get());
            if (compound.hasKey(NbtDataUtil.SPONGE_DATA, NbtDataUtil.TAG_COMPOUND)) {
                compound.removeTag(NbtDataUtil.SPONGE_DATA);
            }
            this.compound = compound;
        }
        if (container.contains(DataQueries.DATA_MANIPULATORS)) {
            final List<DataView> views = container.getViewList(DataQueries.DATA_MANIPULATORS).get();
            final List<DataManipulator<?, ?>> manipulators = DataUtil.deserializeManipulatorList(views);
            manipulators.forEach(this.itemDataSet::add);
        }
        return this;
    }

    @Override
    public ItemStackBuilder fromSnapshot(ItemStackSnapshot snapshot) {
        checkNotNull(snapshot, "The snapshot was null!");
        itemType(snapshot.getType());
        quantity(snapshot.getCount());
        quantity(snapshot.getCount());
        for (ImmutableDataManipulator<?, ?> manipulator : snapshot.getContainers()) {
            itemData(manipulator);
        }
        if (snapshot instanceof SpongeItemStackSnapshot) {
            this.damageValue = ((SpongeItemStackSnapshot) snapshot).getDamageValue();
            final Optional<NBTTagCompound> compoundOptional = ((SpongeItemStackSnapshot) snapshot).getCompound();
            if (compoundOptional.isPresent()) {
                this.compound = compoundOptional.get();
            } else {
                this.compound = null;
            }

        }
        return this;
    }

    @Override
    public ItemStackBuilder fromBlockSnapshot(BlockSnapshot blockSnapshot) {
        checkNotNull(blockSnapshot, "The snapshot was null!");
        reset();
        final Optional<ItemType> itemType = blockSnapshot.getState().getType().getItem();
        checkArgument(itemType.isPresent(), "ItemType not found for block type: " + blockSnapshot.getState().getType().getId());
        itemType(itemType.get());
        quantity(1);
        if (blockSnapshot instanceof SpongeBlockSnapshot) {
            final Block block = (Block) blockSnapshot.getState().getType();
            this.damageValue = block.damageDropped((IBlockState) blockSnapshot.getState());
            final Optional<NBTTagCompound> compound = ((SpongeBlockSnapshot) blockSnapshot).getCompound();
            if (compound.isPresent()) {
                this.compound = new NBTTagCompound();
                this.compound.setTag(NbtDataUtil.BLOCK_ENTITY_TAG, compound.get());
            }
            // todo probably needs more testing, but this'll do donkey...
        } else { // TODO handle through the API specifically handling the rest of the data stuff
            blockSnapshot.getContainers().forEach(this::itemData);
        }
        return this;
    }

    @Override
    public ItemStackBuilder reset() {
        this.type = null;
        this.quantity = 1;
        this.maxQuantity = 64;
        this.itemDataSet = new HashSet<>();
        this.compound = null;
        this.damageValue = 0;
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ItemStack build() throws IllegalStateException {
        checkState(this.type != null, "Item type has not been set");
        checkState(this.quantity <= this.maxQuantity, "Quantity cannot be greater than the max quantity (%s)", this.maxQuantity);
        final ItemStack stack = (ItemStack) new net.minecraft.item.ItemStack((Item) this.type, this.quantity, this.damageValue);
        if (this.compound != null) {
            ((net.minecraft.item.ItemStack) stack).setTagCompound((NBTTagCompound) this.compound.copy());
        }
        if (this.itemDataSet != null) {
            this.itemDataSet.forEach(stack::offer);
        }
        return stack;
    }
}
