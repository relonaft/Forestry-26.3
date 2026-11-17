package forestry.core.engine.genetics.root;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import forestry.api.ForestryConstants;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.engine.genetics.BreedingTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.Locale;

public class ServerBreedingHandler implements BreedingTrackerManager.SidedHandler {
	@Override
	@SuppressWarnings("unchecked")
	public <T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile) {
		// deviation: a saved data ID is a lowercase resource ID now, so the file moved from data/ApiaristTracker.<uuid>.dat
		// to data/forestry/apiaristtracker.<uuid>.dat and the tracker of a world from 1.21.1 is not read
		String filename = type.getBreedingTrackerFile(profile).toLowerCase(Locale.ROOT);
		Codec<BreedingTracker> codec = CompoundTag.CODEC.xmap(tag -> (BreedingTracker) type.createBreedingTracker(tag), tracker -> {
			CompoundTag tag = new CompoundTag();
			tracker.save(tag);
			return tag;
		});
		SavedDataType<BreedingTracker> savedType = new SavedDataType<>(ForestryConstants.forestry(filename), () -> (BreedingTracker) type.createBreedingTracker(), codec);
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		T tracker = (T) overworld.getDataStorage().computeIfAbsent(savedType);
		type.initializeBreedingTracker(tracker, overworld, profile);
		return tracker;
	}
}
