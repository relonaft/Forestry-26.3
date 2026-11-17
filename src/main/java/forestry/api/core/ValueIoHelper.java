package forestry.api.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Used to keep block entities reading and writing {@link CompoundTag} now that Minecraft hands them
 * a {@link ValueInput} and a {@code ValueOutput}. The saved layout stays the same.
 */
public final class ValueIoHelper {
	private ValueIoHelper() {
	}

	/**
	 * Rebuilds the tag that the input reads from.
	 *
	 * @param input The input handed to the block entity
	 * @return a new tag with every entry of the input
	 */
	public static CompoundTag toTag(ValueInput input) {
		CompoundTag tag = new CompoundTag();
		for (String key : input.keySet()) {
			input.read(key, ExtraCodecs.NBT).ifPresent(value -> tag.put(key, value));
		}
		return tag;
	}

	/**
	 * Writes every entry of a tag to an output, so code that builds a tag can save through the new API.
	 *
	 * @param output The output to write to
	 * @param tag    The tag whose entries are written
	 */
	public static void storeAll(ValueOutput output, CompoundTag tag) {
		for (String key : tag.keySet()) {
			output.store(key, ExtraCodecs.NBT, tag.get(key));
		}
	}

	/**
	 * Determines the registries to save a block entity with.
	 *
	 * @param blockEntity The block entity being saved
	 * @return the registries of its level, or empty registries when it has no level
	 */
	public static HolderLookup.Provider registries(BlockEntity blockEntity) {
		Level level = blockEntity.getLevel();
		return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
	}
}
