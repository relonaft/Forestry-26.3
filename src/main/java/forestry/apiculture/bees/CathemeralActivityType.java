package forestry.apiculture.bees;

import forestry.api.apiculture.IActivityType;
import forestry.api.apiculture.LightPreference;
import forestry.api.core.ForestryError;
import forestry.api.core.IError;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.joml.Vector2i;

import java.util.List;

public class CathemeralActivityType implements IActivityType {
	// deviation: 26.2 replaced the octave noise class. This stack repeats what PerlinNoise.create(random, List.of(5, 3, 6))
	// built: octaves 3, 5 and 6 seeded by name, each with half the weight of the one before. Values are floats now
	private static final Noise NOISE = createNoise();

	private static Noise createNoise() {
		PositionalRandomFactory random = RandomSource.create(13L).forkPositional();
		int firstOctave = 3;
		double[] amplitudes = {1.0, 0.0, 1.0, 1.0};
		double factor = Math.pow(2.0, firstOctave);
		double valueFactor = Math.pow(2.0, amplitudes.length - 1) / (Math.pow(2.0, amplitudes.length) - 1.0);
		NoiseStack.Builder stack = NoiseStack.builder();
		for (int i = 0; i < amplitudes.length; i++) {
			if (amplitudes[i] != 0.0) {
				stack.add(new PerlinNoise(random.fromHashOf("octave_" + (firstOctave + i))), factor, (float) (valueFactor * amplitudes[i]));
			}
			factor *= 2.0;
			valueFactor /= 2.0;
		}
		return stack.build();
	}

	// Active during [x,y)
	public static Vector2i getSleepPeriod(BlockPos pos) {
		int offset = (int) getOffset(pos);
		if (offset > 0) {
			return new Vector2i(24000 - offset, -12000 + (24000 - offset));
		} else {
			return new Vector2i(-offset, 12000 - offset);
		}
	}

	// Varies according to position, but y coordinate has lower impact on variation
	private static long getOffset(BlockPos pos) {
		return (long) (NOISE.get(pos.getX() / 40.0, pos.getY() / 1000.0, pos.getZ() / 40.0) * 24000L);
	}

	@Override
	public boolean isActive(long gameTime, long dayTime, BlockPos pos) {
		long adjustedTime = gameTime + getOffset(pos);

		return adjustedTime % 24000L < 12000L;
	}

	@Override
	public IError getInactiveError(long gameTime, long dayTime, BlockPos pos) {
		return ForestryError.SLEEPY;
	}

	@Override
	public LightPreference getLightPreference() {
		return LightPreference.ANY;
	}

	@Override
	public boolean isDominant() {
		return false;
	}
}
