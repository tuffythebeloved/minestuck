package com.mraof.minestuck.data;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.util.MSTags.Fluids;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

import static com.mraof.minestuck.fluid.MSFluids.*;

public class MinestuckFluidTagsProvider extends FluidTagsProvider
{
	public MinestuckFluidTagsProvider(DataGenerator generatorIn, @Nullable ExistingFileHelper existingFileHelper)
	{
		super(generatorIn, Minestuck.MOD_ID, existingFileHelper);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void registerTags()
	{
		getOrCreateBuilder(FluidTags.WATER).addTags(Fluids.OIL, Fluids.BLOOD, Fluids.BRAIN_JUICE, Fluids.WATER_COLORS, Fluids.ENDER, Fluids.LIGHT_WATER);
		getOrCreateBuilder(Fluids.OIL).add(OIL.get(), FLOWING_OIL.get());
		getOrCreateBuilder(Fluids.BLOOD).add(BLOOD.get(), FLOWING_BLOOD.get());
		getOrCreateBuilder(Fluids.BRAIN_JUICE).add(BRAIN_JUICE.get(), FLOWING_BRAIN_JUICE.get());
		getOrCreateBuilder(Fluids.WATER_COLORS).add(WATER_COLORS.get(), FLOWING_WATER_COLORS.get());
		getOrCreateBuilder(Fluids.ENDER).add(ENDER.get(), FLOWING_ENDER.get());
		getOrCreateBuilder(Fluids.LIGHT_WATER).add(LIGHT_WATER.get(), FLOWING_LIGHT_WATER.get());
	}
	
	@Override
	public String getName()
	{
		return "Minestuck Fluid Tags";
	}
}