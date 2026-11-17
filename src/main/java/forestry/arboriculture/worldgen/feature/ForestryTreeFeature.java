package forestry.arboriculture.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.arboriculture.commands.TreeGenHelper;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public record ForestryTreeFeature(IGenome genome) implements Feature {
	// lazy because the genome codec needs the karyotype, which does not exist at class init
	public static final MapCodec<ForestryTreeFeature> CODEC = Codec.lazyInitialized(() -> SpeciesUtil.TREE_TYPE.get().getKaryotype().getGenomeCodec())
		.fieldOf("genome")
		.xmap(ForestryTreeFeature::new, ForestryTreeFeature::genome);

	@Override
	public MapCodec<ForestryTreeFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		ITreeSpecies species = this.genome.getActiveSpecies().cast();

		return TreeGenHelper.generateTree(species, this.genome, level, random, origin);
	}
}
