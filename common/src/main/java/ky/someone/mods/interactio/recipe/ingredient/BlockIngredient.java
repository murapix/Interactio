package ky.someone.mods.interactio.recipe.ingredient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.util.EntrySerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An {@code Ingredient}-equivalent for blocks, based heavily on the vanilla implementation.
 */
public class BlockIngredient implements Predicate<BlockState> {

    public static final BlockIngredient EMPTY = new BlockIngredient(Stream.empty());

    private final IBlockList[] acceptedBlocks;
    private Collection<Block> matchingBlocks;

    protected BlockIngredient(Stream<? extends IBlockList> blockLists) {
        this.acceptedBlocks = blockLists.toArray(IBlockList[]::new);
    }

    /**
     * Get a list of all {@link Block}s which match this ingredient. Used for JEI support.
     *
     * @return A list of matching blocks
     */
    public Collection<Block> getMatchingBlocks() {
        this.determineMatchingBlocks();
        return matchingBlocks;
    }

    private void determineMatchingBlocks() {
        if (this.matchingBlocks == null) {
            this.matchingBlocks = Arrays.stream(this.acceptedBlocks)
                    .flatMap(list -> list.getBlocks().stream())
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Test for a match.
     *
     * @param block Block to check the ingredient against.
     * @return True if the block matches the ingredient
     */
    public boolean test(@Nullable Block block) {
        if (block == null) {
            return false;
        } else {
            this.determineMatchingBlocks();
            return matchingBlocks.contains(block);
        }
    }

    /**
     * Test for a match using a block. Does not consider block amount.
     *
     * @param state Block state to check the ingredient against
     * @return True if the block matches the ingredient
     */
    public boolean test(@Nullable BlockState state) {
        return test(state == null ? Blocks.AIR : state.getBlock());
    }

    /**
     * Deserialize a {@link BlockIngredient} from JSON.
     *
     * @param json The JSON object
     * @return A new BlockIngredient
     * @throws JsonSyntaxException If the JSON cannot be parsed
     */
    public static BlockIngredient deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                return new BlockIngredient(Stream.of(deserializeBlockList(json.getAsJsonObject())));
            } else if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.size() == 0) {
                    throw new JsonSyntaxException("Array cannot be empty, at least one block or tag must be defined");
                }
                return new BlockIngredient(StreamSupport.stream(arr.spliterator(), false)
                        .map(element -> deserializeBlockList(element.getAsJsonObject())));
            } else {
                throw new JsonSyntaxException("Expected either an object or an array of objects for block ingredient");
            }
        }

        throw new JsonSyntaxException("Block cannot be null");
    }

    public static IBlockList deserializeBlockList(JsonObject json) {
        if (json.has("block") && json.has("tag")) {
            throw new JsonSyntaxException("Block ingredient should have either 'tag' or 'block', not both!");
        } else if (json.has("block")) {
            Block block = EntrySerializer.BLOCK.fromJson(json);
            return new SingleBlockList(block);
        } else if (json.has("tag")) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            Tag<Block> tag = BlockTags.getAllTags().getTag(id);
            if (tag == null) {
                throw new JsonSyntaxException("Unknown block tag '" + id + "'");
            }
            return new TagList(tag);
        }

        throw new JsonSyntaxException("Block ingredient should have either 'tag' or 'block'");
    }

    /**
     * Reads a {@link BlockIngredient} from a packet buffer. Use with {@link #toNetwork(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     * @return A new BlockIngredient
     */
    public static BlockIngredient fromNetwork(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        return new BlockIngredient(Stream.generate(() -> new SingleBlockList(EntrySerializer.BLOCK.fromNetwork(buffer))).limit(size));
    }

    /**
     * Writes the ingredient to a packet buffer. Use with {@link #fromNetwork(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     */
    public void toNetwork(FriendlyByteBuf buffer) {
        this.determineMatchingBlocks();
        buffer.writeVarInt(matchingBlocks.size());
        matchingBlocks.forEach(block -> EntrySerializer.BLOCK.toNetwork(buffer, block));
    }

    public interface IBlockList {
        Collection<Block> getBlocks();

        JsonObject serialize();
    }

    public static class SingleBlockList implements IBlockList {
        private final Block block;

        public SingleBlockList(Block block) {
            this.block = block;
        }

        public Collection<Block> getBlocks() {
            return Collections.singleton(this.block);
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("block", Utils.blockId(this.block).toString());
            return jsonobject;
        }
    }

    public static class TagList implements IBlockList {
        private final Tag<Block> tag;

        public TagList(Tag<Block> tagIn) {
            this.tag = tagIn;
        }

        public Collection<Block> getBlocks() {
            return this.tag.getValues();
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("tag", BlockTags.getAllTags().getIdOrThrow(tag).toString());
            return jsonobject;
        }
    }

}