package dev.eliasnvx.femboymod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * {@code femboymod:crafting_transmute}: a backport of 26.3's {@code minecraft:crafting_transmute} (1.21.1 has none).
 * Shapeless: exactly one {@code input} plus one {@code material}. The result keeps every component of the input
 * (backpack contents, charms, colorway, dye, custom name), then the result's own component patch is applied on top.
 * The input and result counts behave like vanilla's default (one material, no count added to the result).
 */
public final class TransmuteRecipe implements CraftingRecipe {

    /** One input and one material, like 26.3's default {@code material_count}. */
    private static final int INGREDIENT_COUNT = 2;

    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient input;
    private final Ingredient material;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;

    public TransmuteRecipe(String group, CraftingBookCategory category, Ingredient input, Ingredient material, ItemStack result) {
        this.group = group;
        this.category = category;
        this.input = input;
        this.material = material;
        this.result = result;
        this.ingredients = NonNullList.of(Ingredient.EMPTY, input, material);
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level) {
        if (craftingInput.ingredientCount() != INGREDIENT_COUNT) {
            return false;
        }
        ItemStack found = findInput(craftingInput);
        if (found.isEmpty()) {
            return false;
        }
        boolean hasMaterial = false;
        for (int slot = 0; slot < craftingInput.size(); slot++) {
            ItemStack stack = craftingInput.getItem(slot);
            if (stack.isEmpty() || stack == found) {
                continue;
            }
            if (!material.test(stack)) {
                return false;
            }
            hasMaterial = true;
        }
        // 26.3 refuses a transmute that would give back the very same stack
        return hasMaterial && !ItemStack.isSameItemSameComponents(found, transmute(found));
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider registries) {
        ItemStack found = findInput(craftingInput);
        return found.isEmpty() ? ItemStack.EMPTY : transmute(found);
    }

    private ItemStack findInput(CraftingInput craftingInput) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < craftingInput.size(); slot++) {
            ItemStack stack = craftingInput.getItem(slot);
            if (!stack.isEmpty() && input.test(stack)) {
                if (!found.isEmpty()) {
                    return ItemStack.EMPTY; // two inputs: no match
                }
                found = stack;
            }
        }
        return found;
    }

    private ItemStack transmute(ItemStack from) {
        ItemStack out = from.transmuteCopy(result.getItem(), result.getCount());
        out.applyComponents(result.getComponentsPatch());
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= INGREDIENT_COUNT;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FemboyRecipes.CRAFTING_TRANSMUTE.get();
    }

    /** Same JSON shape as 26.3's {@code crafting_transmute}: category, group, input, material, result. */
    public static final class Serializer implements RecipeSerializer<TransmuteRecipe> {

        private static final MapCodec<TransmuteRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.category),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(r -> r.input),
                Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(r -> r.material),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, TransmuteRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                CraftingBookCategory.STREAM_CODEC, r -> r.category,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.input,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.material,
                ItemStack.STREAM_CODEC, r -> r.result,
                TransmuteRecipe::new);

        @Override
        public MapCodec<TransmuteRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
