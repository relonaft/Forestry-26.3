package forestry.core.platform.util;

import com.mojang.authlib.GameProfile;
import forestry.Forestry;
import forestry.core.platform.network.IStreamable;
import io.netty.buffer.Unpooled;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.connection.ConnectionType;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public abstract class NBTUtilForestry {
	private static final String GAME_PROFILE_ID = "Id";
	private static final String GAME_PROFILE_NAME = "Name";

	public static CompoundTag writeStreamableToNbt(IStreamable streamable, CompoundTag nbt, HolderLookup.Provider registries) {
		return writeStreamableToNbt(streamable, nbt, (RegistryAccess) registries);
	}

	public static CompoundTag writeStreamableToNbt(IStreamable streamable, CompoundTag nbt, RegistryAccess registries) {
		RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
		streamable.writeData(data);

		byte[] bytes = new byte[data.readableBytes()];
		data.getBytes(0, bytes);
		nbt.putByteArray("dataBytes", bytes);
		return nbt;
	}

	public static void readStreamableFromNbt(IStreamable streamable, CompoundTag nbt, HolderLookup.Provider registries) {
		readStreamableFromNbt(streamable, nbt, (RegistryAccess) registries);
	}

	public static void readStreamableFromNbt(IStreamable streamable, CompoundTag nbt, RegistryAccess registries) {
		if (nbt.contains("dataBytes")) {
			byte[] bytes = nbt.getByteArray("dataBytes").orElse(new byte[0]);
			RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), registries, ConnectionType.NEOFORGE);
			streamable.readData(data);
		}
	}

	@Nullable
	public static GameProfile readGameProfile(CompoundTag nbt) {
		if (nbt.isEmpty()) {
			return null;
		}

		UUID id = nbt.read(GAME_PROFILE_ID, UUIDUtil.CODEC).orElse(null);
		String name = nbt.getString(GAME_PROFILE_NAME).orElse(null);
		if (id == null && (name == null || name.isEmpty())) {
			return null;
		}
		// authlib rejects a null id or name. A half-written profile is a name-only trade station address
		// or an id-only owner, so fill the missing half with the value the rest of the mod reads as absent
		return new GameProfile(id == null ? Util.NIL_UUID : id, name == null ? "" : name);
	}

	public static CompoundTag writeGameProfile(CompoundTag nbt, GameProfile profile) {
		if (profile.id() != null) {
			nbt.store(GAME_PROFILE_ID, UUIDUtil.CODEC, profile.id());
		}
		if (profile.name() != null) {
			nbt.putString(GAME_PROFILE_NAME, profile.name());
		}
		return nbt;
	}

	@Nullable
	public static CompoundTag getItemStackTag(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData == null || customData.isEmpty() ? null : customData.copyTag();
	}

	public static void setItemStackTag(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
	}

	// NbtUtils had these until 1.21.5. The codec writes the same int array
	public static Optional<BlockPos> readBlockPos(CompoundTag tag, String key) {
		return tag.read(key, BlockPos.CODEC);
	}

	public static Tag writeBlockPos(BlockPos pos) {
		return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
	}

	// until 1.21.5 the stacks had these as their own parse, save and saveOptional methods

	public static Optional<ItemStack> parseItem(HolderLookup.Provider registries, Tag tag) {
		return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
				.resultOrPartial(error -> Forestry.LOGGER.error("Tried to load invalid item: '{}'", error));
	}

	public static Tag saveItem(ItemStack stack, HolderLookup.Provider registries) {
		if (stack.isEmpty()) {
			throw new IllegalStateException("Cannot encode empty ItemStack");
		}
		return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
	}

	public static Tag saveItemOptional(ItemStack stack, HolderLookup.Provider registries) {
		return stack.isEmpty() ? new CompoundTag() : saveItem(stack, registries);
	}

	public static Optional<FluidStack> parseFluid(HolderLookup.Provider registries, Tag tag) {
		return FluidStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
				.resultOrPartial(error -> Forestry.LOGGER.error("Tried to load invalid fluid: '{}'", error));
	}

	public static FluidStack parseFluidOptional(HolderLookup.Provider registries, CompoundTag tag) {
		return tag.isEmpty() ? FluidStack.EMPTY : parseFluid(registries, tag).orElse(FluidStack.EMPTY);
	}

	public static Tag saveFluid(FluidStack stack, HolderLookup.Provider registries) {
		if (stack.isEmpty()) {
			throw new IllegalStateException("Cannot encode empty FluidStack");
		}
		return FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
	}
}
