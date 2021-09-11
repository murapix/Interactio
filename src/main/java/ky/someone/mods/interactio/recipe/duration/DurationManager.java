package ky.someone.mods.interactio.recipe.duration;

import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;

public class DurationManager<R extends DurationRecipe<T>, T> {
    protected static Map<Class<? extends DurationRecipe<?>>, Map<Level, DurationManager<?, ?>>> managers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <R extends DurationRecipe<T>, T, S extends StateHolder<?, ?>> DurationManager<R, T> get(Level world, InWorldRecipeType<R> storage, Class<R> cls) {
        return (DurationManager<R, T>) managers.computeIfAbsent(cls, k -> new WeakHashMap<>())
                .computeIfAbsent(world, k -> new DurationManager<>(world, storage, cls));
    }

    protected Map<BlockPos, SimpleEntry<R, Integer>> existingRecipes;
    protected RecipeDataTracker<T, R> tracker;
    protected InWorldRecipeType<R> storage;

    protected DurationManager(Level world, InWorldRecipeType<R> storage, Class<R> cls) {
        this.existingRecipes = new HashMap<>();
        this.storage = storage;
        this.tracker = RecipeDataTracker.get(world, cls);
    }

    public RecipeDataTracker<T, R> getTracker() {
        return this.tracker;
    }

    public static void tickAllRecipes(Level world) {
        DurationManager.managers.values().stream()
                .map(map -> map.get(world)).filter(Objects::nonNull)
                .forEach(manager -> manager.tickRecipes(world));
    }

    public void tickRecipes(Level world) {
        List<BlockPos> toRemove = new LinkedList<>();
        this.existingRecipes.forEach((pos, entry) -> {
            T input = tracker.getInput(pos);
            R recipe = entry.getKey();
            CraftingInfo info = new CraftingInfo(recipe, world, pos);
            int duration = entry.getValue() + 1;
            if (input == null)
                toRemove.add(pos);
            else if (recipe.canCraft(input, info)) {
                recipe.tick(input, info);
                entry.setValue(duration);
                if (recipe.isFinished(duration)) {
                    recipe.craft(input, info);
                    toRemove.add(pos);
                }
                tracker.clear(pos);
            } else toRemove.add(pos);
        });

        for (BlockPos pos : toRemove)
            this.existingRecipes.remove(pos);
        toRemove.clear();

        tracker.forEach((input, pos) -> {
            storage.apply(recipe -> recipe.canCraft(input, new CraftingInfo(recipe, world, pos)),
                    recipe -> trackOrCraft(world, pos, recipe, input));
        });
        tracker.clear();
    }

    private void trackOrCraft(Level world, BlockPos pos, R recipe, T input) {
        if (recipe.getDuration() == 0)
            recipe.craft(input, new CraftingInfo(recipe, world, pos));
        else this.existingRecipes.put(pos, new SimpleEntry<>(recipe, 0));
    }
}
