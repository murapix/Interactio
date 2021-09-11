package ky.someone.mods.interactio.recipe.util;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class CraftingInfo {
    protected final InWorldRecipe<?, ?> recipe;
    protected final Level world;
    protected final BlockPos pos;
    protected final BlockState blockState;
    protected final FluidState fluidState;

    public CraftingInfo(InWorldRecipe<?, ?> recipe, Level world, BlockPos pos) {
        this.recipe = recipe;
        this.world = world;
        this.pos = pos;
        this.blockState = world.getBlockState(pos);
        this.fluidState = world.getFluidState(pos);
    }

    public InWorldRecipe<?, ?> getRecipe() {
        return recipe;
    }

    public Level getWorld() {
        return world;
    }

    public JsonObject getJson() {
        return recipe.getJson();
    }

    public Vec3 getPos() {
        return Vec3.atCenterOf(pos);
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public FluidState getFluidState() {
        return fluidState;
    }
}
