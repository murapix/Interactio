package ky.someone.mods.interactio.recipe.duration;

import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RecipeDataTracker<T, R extends DurationRecipe<T>> {
    protected static Map<Class<? extends DurationRecipe<?>>, Map<Level, RecipeDataTracker<?, ?>>> trackers = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected static <T, R extends DurationRecipe<T>> RecipeDataTracker<T, R> get(Level world, Class<R> cls) {
        return (RecipeDataTracker<T, R>) trackers.computeIfAbsent(cls, k -> new WeakHashMap<>())
                .computeIfAbsent(world, k -> new RecipeDataTracker<>());
    }

    protected Map<BlockPos, T> inputs;

    protected RecipeDataTracker() {
        this.inputs = new HashMap<>();
    }

    @Nullable
    public T getInput(BlockPos pos) {
        return inputs.get(pos);
    }

    public T getInput(BlockPos pos, Supplier<T> defaultGenerator) {
        return inputs.computeIfAbsent(pos, k -> defaultGenerator.get());
    }

    public void setInput(BlockPos pos, T input) {
        inputs.put(pos, input);
    }

    public void clear(BlockPos pos) {
        this.inputs.remove(pos);
    }

    public void clear() {
        this.inputs.clear();
    }

    public void forEach(BiConsumer<T, BlockPos> consumer) {
        for (BlockPos pos : inputs.keySet()) {
            if (inputs.get(pos) == null) continue;
            consumer.accept(inputs.get(pos), pos);
        }
    }
}
