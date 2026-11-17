package forestry.arboriculture.entities;

import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.features.ArboricultureItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ForestryBoat extends Boat {
	private static final EntityDataAccessor<Integer> DATA_ID_WOOD_TYPE = SynchedEntityData.defineId(ForestryBoat.class, EntityDataSerializers.INT);

	public ForestryBoat(EntityType<? extends Boat> type, Level level) {
		this(type, level, new DropItem());
	}

	private ForestryBoat(EntityType<? extends Boat> type, Level level, DropItem dropItem) {
		super(type, level, dropItem);
		dropItem.boat = this;
	}

	public ForestryBoat(Level level, double x, double y, double z) {
		this(ArboricultureEntities.BOAT.entityType(), level);

		setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ID_WOOD_TYPE, ForestryWoodType.CAMELTHORN.ordinal());
	}

	protected Item getWoodDropItem() {
		return ArboricultureItems.BOAT.item(getWoodType());
	}

	// deviation: no checkFallDamage override, vanilla boats stopped breaking on a fall in 1.21.2

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("type", getWoodType().name());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.getString("type").ifPresent(name -> {
			try {
				setWoodType(ForestryWoodType.valueOf(name));
			} catch (IllegalArgumentException e) {
				setWoodType(ForestryWoodType.CAMELTHORN);
			}
		});
	}

	public void setWoodType(ForestryWoodType woodType) {
		this.entityData.set(DATA_ID_WOOD_TYPE, woodType.ordinal());
	}

	public ForestryWoodType getWoodType() {
		return ForestryWoodType.VALUES[this.entityData.get(DATA_ID_WOOD_TYPE)];
	}

	// a holder because the super constructor wants the drop before the wood type is known
	private static class DropItem implements Supplier<Item> {
		@Nullable
		private ForestryBoat boat;

		@Override
		public Item get() {
			return this.boat == null ? Items.AIR : this.boat.getWoodDropItem();
		}
	}
}
