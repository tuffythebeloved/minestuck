package com.mraof.minestuck.world.gen;

import com.mraof.minestuck.world.gen.feature.structure.GateStructure;
import com.mraof.minestuck.world.gen.feature.structure.blocks.StructureBlockRegistry;
import com.mraof.minestuck.world.lands.LandTypePair;
import net.minecraft.core.Holder;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.levelgen.*;

public class LandGenSettings
{
	private final LandTypePair landTypes;
	private final StructureBlockRegistry blockRegistry;
	private GateStructure.PieceFactory gatePiece;
	public float oceanChance = 1/3F, roughChance = 1/5F;
	
	LandGenSettings(LandTypePair landTypes)
	{
		this.landTypes = landTypes;
		
		blockRegistry = new StructureBlockRegistry();
		landTypes.getTerrain().registerBlocks(blockRegistry);
		landTypes.getTitle().registerBlocks(blockRegistry);
		
		landTypes.getTerrain().setGenSettings(this);
		landTypes.getTitle().setGenSettings(this);
		
	}
	
	public LandTypePair getLandTypes()
	{
		return landTypes;
	}
	
	public StructureBlockRegistry getBlockRegistry()
	{
		return blockRegistry;
	}
	
	public void setGatePiece(GateStructure.PieceFactory factory)
	{
		gatePiece = factory;
	}
	
	public GateStructure.PieceFactory getGatePiece()
	{
		return gatePiece;
	}
	
	Holder<NoiseGeneratorSettings> createDimensionSettings()
	{
		/*TODO structure settings go elsewhere now
		Map<StructureFeature<?>, StructureFeatureConfiguration> structures = new HashMap<>();
		structures.put(MSFeatures.LAND_GATE, new StructureFeatureConfiguration(1, 0, 0));
		structures.put(MSFeatures.SMALL_RUIN, new StructureFeatureConfiguration(16, 4, 59273643));
		structures.put(MSFeatures.IMP_DUNGEON, new StructureFeatureConfiguration(16, 4, 34527185));
		structures.put(MSFeatures.CONSORT_VILLAGE, new StructureFeatureConfiguration(24, 5, 10387312));
		
		StructureSettings structureSettings = new StructureSettings(Optional.empty(), structures);
		*/
		NoiseSettings noiseSettings = NoiseSettings.create(0, 256, new NoiseSamplingSettings(1, 1, 80, 160),
				new NoiseSlider(-10, 3, 0), new NoiseSlider(-30, 0, 0), 1, 2,
				new TerrainShaper(CubicSpline.constant(0.035F), CubicSpline.constant(1), CubicSpline.constant(0)));
		
		NoiseGeneratorSettings settings = new NoiseGeneratorSettings(noiseSettings, blockRegistry.getBlockState("ground"),
				blockRegistry.getBlockState("ocean"), new NoiseRouterWithOnlyNoises(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero()), SurfaceRules.bandlands(), 64, false, false, false, false);
		
		return Holder.direct(settings);
	}
}