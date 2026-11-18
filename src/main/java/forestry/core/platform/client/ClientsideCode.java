package forestry.core.platform.client;

import forestry.core.engine.genetics.GeneticsReloadHandler;
import forestry.core.engine.genetics.root.ClientBreedingHandler;
import forestry.core.engine.genetics.root.ServerBreedingHandler;
import forestry.core.platform.client.book.BookClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

// Rule of thumb for safely calling client code: client classes must be in the body of a static method
// in another class (like this one), guarded by an if statement checking FMLEnvironment.getDist() == Dist.CLIENT
// Calls to this class must be guarded by an if statement.
// DistExecutor will be deprecated for removal in 1.20, and it doesn't work anyway.
public class ClientsideCode {
	private static RecipeMap recipes = RecipeMap.EMPTY;

	public static ServerBreedingHandler newBreedingHandler() {
		return new ClientBreedingHandler();
	}

	public static void openForestersManual() {
		BookClient.open();
	}

	public static boolean hasShiftDown() {
		return Minecraft.getInstance().hasShiftDown();
	}

	public static RecipeMap getRecipeMap() {
		return recipes;
	}

	// a 26.3 client has no recipe manager, the server sends the types ModuleCore asks for
	public static void onRecipesReceived(RecipesReceivedEvent event) {
		recipes = event.getRecipeMap();
		GeneticsReloadHandler.rebuildMutations(recipes);
	}

	public static Registry<Fluid> getFluidRegistry() {
		return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.FLUID);
	}

	public static void markForUpdate(BlockPos pos) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			BlockState state = level.getBlockState(pos);
			level.setBlocksDirty(pos, state, state);
		}
	}

	public static RegistryAccess getRegistryAccess() {
		return Minecraft.getInstance().level.registryAccess();
	}

	public static Optional<Resource> getResource(Identifier path) {
		return Minecraft.getInstance().getResourceManager().getResource(path);
	}
}
