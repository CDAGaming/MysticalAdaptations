package com.focamacho.mysticaladaptations.tileentity;

import com.blakebr0.cucumber.helper.StackHelper;
import com.blakebr0.cucumber.util.VanillaPacketDispatcher;
import com.blakebr0.mysticalagriculture.crafting.ReprocessorManager;
import com.blakebr0.mysticalagriculture.tileentity.reprocessor.TileEssenceReprocessor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.SlotFurnaceFuel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class TileInsaniumReprocessor extends TileEssenceReprocessor {

	private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
	private int progress;
	private int fuel;
	private int fuelLeft;
	private int fuelItemValue;

	private int packetCount;
	private boolean packet;

	@Override
	public NBTTagCompound writeCustomNBT(NBTTagCompound tag) {
		tag.setInteger("Progress", this.progress);
		tag.setInteger("Fuel", this.fuel);
		tag.setInteger("FuelLeft", this.fuelLeft);
		tag.setInteger("FuelItemValue", this.fuelItemValue);

		ItemStackHelper.saveAllItems(tag, this.inventory);

		return tag;
	}

	@Override
	public void readCustomNBT(NBTTagCompound tag) {
		this.progress = tag.getInteger("Progress");
		this.fuel = tag.getInteger("Fuel");
		this.fuelLeft = tag.getInteger("FuelLeft");
		this.fuelItemValue = tag.getInteger("FuelItemValue");

		ItemStackHelper.loadAllItems(tag, this.inventory);
	}

	@Override
	public void update() {
		if (this.getWorld().isRemote)
			return;
		
		boolean mark = false;

		int fuelPerTick = Math.min(Math.min(this.fuelLeft, this.getFuelUsage() * 2), this.getFuelCapacity() - this.fuel);
		if (this.fuel < this.getFuelCapacity()) {
			ItemStack fuel = this.getStackInSlot(1);
			if (this.fuelLeft <= 0 && !fuel.isEmpty()) {
				this.fuelItemValue = TileEntityFurnace.getItemBurnTime(fuel);
				this.fuelLeft = this.fuelItemValue;
				this.decrStackSize(1, 1);
			}

			if (this.fuelLeft > 0) {
				this.fuel += fuelPerTick;
				this.fuelLeft -= fuelPerTick;

				if (this.fuelLeft <= 0) {
					this.fuelItemValue = 0;
				}

				mark = true;
			}
		}

		if (this.fuel >= this.getFuelUsage()) {
			ItemStack input = this.getStackInSlot(0);
			ItemStack output = this.getStackInSlot(2);

			if (!input.isEmpty()) {
				ItemStack recipeOutput = ReprocessorManager.getOutput(input);

				if (!recipeOutput.isEmpty() && (output.isEmpty() || StackHelper.canCombineStacks(output, recipeOutput))) {
					this.progress++;
					this.fuel -= this.getFuelUsage();

					if (this.progress >= this.getOperationTime()) {
						this.decrStackSize(0, 1);

						if (output.isEmpty()) {
							this.setInventorySlotContents(2, recipeOutput.copy());
						} else {
							output.grow(recipeOutput.getCount());
						}

						this.progress = 0;
					}

					mark = true;
				}
			} else {
				if (this.progress > 0) {
					this.progress = 0;
					mark = true;
				}
			}
		}

		if (mark) {
			this.markDirty();
		}
	}

	@Override
	public int getSizeInventory() {
		return 3;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return this.inventory.get(slot);
	}

	@Override
	public ItemStack decrStackSize(int slot, int decrement) {
		return ItemStackHelper.getAndSplit(this.inventory, slot, decrement);
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.world.getTileEntity(this.getPos()) == this && player.getDistanceSq(this.getPos().add(0.5D, 0.5D, 0.5D)) <= 64.0D;
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		if (index == 2) {
			return false;
		} else if (index != 1) {
			return true;
		} else {
			ItemStack itemstack = this.inventory.get(1);
			return TileEntityFurnace.isItemFuel(stack) || SlotFurnaceFuel.isBucket(stack) && itemstack.getItem() != Items.BUCKET;
		}
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		ItemStack itemstack = this.inventory.get(index);
		boolean flag = !stack.isEmpty() && stack.isItemEqual(itemstack) && ItemStack.areItemStackTagsEqual(stack, itemstack);
		this.inventory.set(index, stack);

		if (stack.getCount() > this.getInventoryStackLimit()) {
			stack.setCount(this.getInventoryStackLimit());
		}
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStackHelper.getAndRemove(this.inventory, index);
	}

	@Override
	public void openInventory(EntityPlayer player) {

	}

	@Override
	public void closeInventory(EntityPlayer player) {

	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {

	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {

	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		return side == EnumFacing.UP ? new int[] { 0 } : side == EnumFacing.DOWN ? new int[] { 2 } : new int[] { 1 };
	}

	@Override
	public boolean canInsertItem(int index, ItemStack stack, EnumFacing facing) {
		return this.isItemValidForSlot(index, stack);
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack, EnumFacing facing) {
		return index == 2;
	}

	@Override
	public boolean isEmpty() {
		return !this.inventory.stream().anyMatch(s -> !s.isEmpty());
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing side) {
		return this.getCapability(capability, side) != null;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return (T) new SidedInvWrapper(this, facing);
		}

		return super.getCapability(capability, facing);
	}

	@Override
	public int getProgress() {
		return this.progress;
	}

	@Override
	public boolean isWorking() {
		return this.progress > 0 && this.fuelLeft >= this.getFuelUsage();
	}

	@Override
	public int getFuel() {
		return this.fuel;
	}

	@Override
	public int getFuelLeft() {
		return this.fuelLeft;
	}

	@Override
	public int getFuelItemValue() {
		return this.fuelItemValue;
	}
	
	@Override
	public int getOperationTime() {
		return 3;
	}

	@Override
	public int getFuelUsage() {
		return 28;
	}

	@Override
	public int getFuelCapacity() {
		return 32400;
	}

	@Override
	public void markDirty() {
		super.markDirty();
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(this.getPos(), -1, this.getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) {
		this.readFromNBT(packet.getNbtCompound());
	}

	@Override
	public final NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}
}