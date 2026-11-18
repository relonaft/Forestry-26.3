package forestry.gametest.client;

import net.minecraft.resources.Identifier;

import forestry.gametest.ForestryGameTests;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;

// hands the JEI runtime to JeiSnapshots, does nothing else
@JeiPlugin
public class JeiSnapshotPlugin implements IModPlugin {
	static volatile IJeiRuntime runtime;

	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(ForestryGameTests.MOD_ID, "snapshots");
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
	}
}
