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
package org.spongepowered.common.mixin.api.mcp.entity.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.item.inventory.adapter.impl.comp.EquipmentInventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.comp.MainPlayerInventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.minecraft.PlayerInventoryLens;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
@Mixin(InventoryPlayer.class)
public abstract class InventoryPlayerMixin_API implements PlayerInventory {

    @Shadow @Final public List<NonNullList<ItemStack>> allInventories;
    @Shadow public int currentItem;
    @Shadow public EntityPlayer player;
    @Shadow private int timesChanged;

    @Shadow public abstract int getSizeInventory();
    @Shadow public abstract ItemStack getStackInSlot(int index);

    protected SlotCollection slots;
    protected Fabric inventory;
    protected Lens lens;

    private Player api$carrier;
    private MainPlayerInventoryAdapter api$main;
    @Nullable private EquipmentInventoryAdapter api$equipment;
    private SlotAdapter api$offhand;

    @Override
    public Optional<Player> getCarrier() {
        return Optional.ofNullable(this.api$carrier);
    }

    @Override
    public MainPlayerInventory getMain() {
        if (this.api$main == null && this.lens instanceof PlayerInventoryLens) {
            this.api$main = (MainPlayerInventoryAdapter) ((PlayerInventoryLens) this.lens).getMainLens().getAdapter(this.inventory, this);
        }
        return this.api$main;
    }

    @Override
    public EquipmentInventory getEquipment() {
        if (this.api$equipment == null) {
            this.api$equipment = (EquipmentInventoryAdapter) ((PlayerInventoryLens) this.lens).getEquipmentLens().getAdapter(this.inventory, this);
        }
        return this.api$equipment;
    }

    @Override
    public Slot getOffhand() {
        if (this.api$offhand == null && this.lens instanceof PlayerInventoryLens) {
            this.api$offhand = (SlotAdapter) ((PlayerInventoryLens) this.lens).getOffhandLens().getAdapter(this.inventory, this);
        }
        return this.api$offhand;
    }

}