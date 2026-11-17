package forestry.core.platform.worldgen;

import com.mojang.serialization.MapCodec;
import forestry.api.core.genetics.IGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public abstract class AbstractForestryFeature implements Feature {
	// built in code for one species, never registered or written to a datapack
	@Override
	public MapCodec<? extends Feature> codec() {
		throw new UnsupportedOperationException("Forestry tree generators have no feature type");
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		return place(getDefaultGenome(), level, random, origin, false);
	}

	public abstract IGenome getDefaultGenome();

	public abstract boolean place(IGenome genome, LevelAccessor level, RandomSource rand, BlockPos pos, boolean forced);
}
