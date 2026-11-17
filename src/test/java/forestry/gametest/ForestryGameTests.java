package forestry.gametest;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.ModFileScanData;

// one test function and one test instance per @GameTest method, named <holder namespace>:<class>.<method>
@Mod(ForestryGameTests.MOD_ID)
public final class ForestryGameTests {
	public static final String MOD_ID = "forestrygametest";

	private record Test(Identifier id, Identifier structure, int maxTicks, Method method) {
	}

	private final List<Test> tests = new ArrayList<>();

	public ForestryGameTests(IEventBus modBus, ModContainer container) {
		ModFileScanData scan = container.getModInfo().getOwningFile().getFile().getScanResult();
		scan.getAnnotatedBy(GameTestHolder.class, ElementType.TYPE).forEach(data -> collect(data.clazz().getClassName()));
		if (this.tests.isEmpty()) {
			throw new IllegalStateException("no @GameTest method found in " + MOD_ID);
		}
		this.tests.sort(Comparator.comparing(Test::id));

		Map<String, DeferredRegister<Consumer<GameTestHelper>>> functions = new HashMap<>();
		for (Test test : this.tests) {
			functions.computeIfAbsent(test.id().getNamespace(), namespace -> DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, namespace))
					.register(test.id().getPath(), () -> helper -> invoke(test.method(), helper));
		}
		functions.values().forEach(register -> register.register(modBus));
		modBus.addListener(this::registerTests);
	}

	private void collect(String className) {
		Class<?> holder;
		try {
			// not initialized, a test class may hold content that does not exist this early
			holder = Class.forName(className, false, ForestryGameTests.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("missing test class " + className, e);
		}
		String namespace = holder.getAnnotation(GameTestHolder.class).value();
		String prefix = holder.getSimpleName().toLowerCase(Locale.ROOT) + ".";
		PrefixGameTestTemplate prefixTemplate = holder.getAnnotation(PrefixGameTestTemplate.class);
		boolean prefixed = prefixTemplate == null || prefixTemplate.value();

		for (Method method : holder.getDeclaredMethods()) {
			GameTest test = method.getAnnotation(GameTest.class);
			if (test == null) {
				continue;
			}
			Class<?>[] parameters = method.getParameterTypes();
			if (!Modifier.isStatic(method.getModifiers()) || parameters.length != 1 || parameters[0] != GameTestHelper.class) {
				throw new IllegalStateException("@GameTest method must be static and take a GameTestHelper: " + method);
			}
			method.setAccessible(true);
			String name = prefix + method.getName().toLowerCase(Locale.ROOT);
			String template = test.template().isEmpty() ? name : (prefixed ? prefix : "") + test.template();
			this.tests.add(new Test(Identifier.fromNamespaceAndPath(namespace, name), Identifier.fromNamespaceAndPath(namespace, template), test.timeoutTicks(), method));
		}
	}

	private void registerTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MOD_ID, "default"));
		for (Test test : this.tests) {
			ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, test.id());
			event.registerTest(test.id(), new FunctionGameTestInstance(function, new TestData<>(environment, test.structure(), test.maxTicks(), 0, true)));
		}
	}

	private static void invoke(Method method, GameTestHelper helper) {
		try {
			method.invoke(null, helper);
		} catch (InvocationTargetException e) {
			// the framework tells a failed assertion from a crash by the exception type
			if (e.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			throw new RuntimeException(e.getCause());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
