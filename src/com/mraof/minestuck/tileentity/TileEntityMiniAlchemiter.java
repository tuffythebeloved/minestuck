package com.mraof.minestuck.tileentity;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.alchemy.*;
import com.mraof.minestuck.block.MinestuckBlocks;
import com.mraof.minestuck.item.MinestuckItems;
import com.mraof.minestuck.tracker.MinestuckPlayerTracker;
import com.mraof.minestuck.util.IdentifierHandler;
import com.mraof.minestuck.util.MinestuckPlayerData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IInteractionObject;

public class TileEntityMiniAlchemiter extends TileEntityMachineProcess implements IInteractionObject
{
	private int ticks_since_update = 0;
	public IdentifierHandler.PlayerIdentifier owner;
	public GristType selectedGrist = GristType.Build;
	
	public TileEntityMiniAlchemiter()
	{
		super(MinestuckTiles.MINI_ALCHEMITER);
	}
	
	@Override
	public RunType getRunType()
	{
		return RunType.BUTTON_OVERRIDE;
	}
	
	@Override
	public int getSizeInventory()
	{
		return 2;
	}
	
	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack)
	{
		return index == 0 && stack.getItem() == MinestuckItems.cruxiteDowel;
	}
	
	@Override
	public boolean contentsValid()
	{
		if(!world.isBlockPowered(this.getPos()) && !this.inv.get(0).isEmpty() && this.owner != null)
		{
			//Check owner's cache: Do they have everything they need?
			ItemStack newItem = AlchemyRecipes.getDecodedItem(this.inv.get(0));
			if(newItem.isEmpty())
				if(!inv.get(0).hasTag() || !inv.get(0).getTag().hasKey("contentID"))
					newItem = new ItemStack(MinestuckBlocks.GENERIC_OBJECT);
				else return false;
			if(!inv.get(1).isEmpty() && (inv.get(1).getItem() != newItem.getItem() || inv.get(1).getMaxStackSize() <= inv.get(1).getCount()))
			{
				return false;
			}
			GristSet cost = GristRegistry.getGristConversion(newItem);
			if(newItem.getItem() == MinestuckItems.captchaCard)
				cost = new GristSet(selectedGrist, MinestuckConfig.cardCost);
			
			return GristHelper.canAfford(MinestuckPlayerData.getGristSet(this.owner), cost);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public void processContents()
	{
		ItemStack newItem = AlchemyRecipes.getDecodedItem(this.inv.get(0));
		
		if (newItem.isEmpty())
			newItem = new ItemStack(MinestuckBlocks.GENERIC_OBJECT);
		
		if (inv.get(1).isEmpty())
		{
			setInventorySlotContents(1, newItem);
		}
		else
		{
			this.inv.get(1).grow(1);
		}
		
		EntityPlayerMP player = owner.getPlayer();
		if (player != null)
			AlchemyRecipes.onAlchemizedItem(newItem, player);
		
		GristSet cost = GristRegistry.getGristConversion(newItem);
		if (newItem.getItem() == MinestuckItems.captchaCard)
			cost = new GristSet(selectedGrist, MinestuckConfig.cardCost);
		GristHelper.decrease(owner, cost);
		MinestuckPlayerTracker.updateGristCache(owner);
	}
	
	// We're going to want to trigger a block update every 20 ticks to have comparators pull data from the Alchemiter.
	@Override
	public void tick()
	{
		super.tick();
		if (this.ticks_since_update == 20)
		{
			world.updateComparatorOutputLevel(this.getPos(), this.getBlockState().getBlock());
			this.ticks_since_update = 0;
		}
		else
		{
			this.ticks_since_update++;
		}
	}
	
	@Override
	public void read(NBTTagCompound compound)
	{
		super.read(compound);
		
		this.selectedGrist = GristType.getTypeFromString(compound.getString("gristType"));
		if(this.selectedGrist == null)
		{
			this.selectedGrist = GristType.Build;
		}
		
		if(IdentifierHandler.hasIdentifier(compound, "owner"))
			owner = IdentifierHandler.load(compound, "owner");
	}
	
	@Override
	public NBTTagCompound write(NBTTagCompound compound)
	{
		compound.setString("gristType", selectedGrist.getRegistryName().toString());
		
		if(owner != null)
			owner.saveToNBT(compound, "owner");
		
		return super.write(compound);
	}
	
	@Override
	public ITextComponent getName()
	{
		return new TextComponentTranslation("container.mini_alchemiter");
	}
	
	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		if(side == EnumFacing.DOWN)
			return new int[] {1};
		else return new int[] {0};
	}
	
	public int comparatorValue()
	{
		if (getStackInSlot(0) != null && owner != null)
		{
			ItemStack newItem = AlchemyRecipes.getDecodedItem(getStackInSlot(0));
			if (newItem.isEmpty())
				if (!getStackInSlot(0).hasTag() || !getStackInSlot(0).getTag().hasKey("contentID"))
					newItem = new ItemStack(MinestuckBlocks.GENERIC_OBJECT);
				else return 0;
			if (!getStackInSlot(1).isEmpty() && (getStackInSlot(1).getItem() != newItem.getItem() || getStackInSlot(1).getItemDamage() != newItem.getItemDamage() || getStackInSlot(1).getMaxStackSize() <= getStackInSlot(1).getCount()))
			{
				return 0;
			}
			GristSet cost = GristRegistry.getGristConversion(newItem);
			if (newItem.getItem() == MinestuckItems.captchaCard)
				cost = new GristSet(selectedGrist, MinestuckConfig.cardCost);
			// We need to run the check 16 times. Don't want to hammer the game with too many of these, so the comparators are only told to update every 20 ticks.
			// Additionally, we need to check if the item in the slot is empty. Otherwise, it will attempt to check the cost for air, which cannot be alchemized anyway.
			if (cost != null && !getStackInSlot(0).isEmpty())
			{
				GristSet scale_cost;
				for (int lvl = 1; lvl <= 17; lvl++)
				{
					// We went through fifteen item cost checks and could still afford it. No sense in checking more than this.
					if (lvl == 17)
					{
						return 15;
					}
					// We need to make a copy to preserve the original grist amounts and avoid scaling values that have already been scaled. Keeps scaling linear as opposed to exponential.
					scale_cost = cost.copy().scaleGrist(lvl);
					if (!GristHelper.canAfford(MinestuckPlayerData.getGristSet(owner), scale_cost))
					{
						return lvl - 1;
					}
				}
			}
		}
		return 0;
	}
}