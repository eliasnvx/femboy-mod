package dev.eliasnvx.femboymod.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * {@code femboymod:crafting_transmute}: a backport of 26.3's {@code minecraft:crafting_transmute} (1.20.1 has none).
 * Shapeless: exactly one {@code input} plus one {@code material}. The result keeps the input's NBT, which is where
 * 1.20.1 keeps what 26.3 has as components (backpack contents, charms, colorway, dye, custom name); the result's
 * own NBT is merged on top.
 * The input and result counts behave like vanilla's default (one material, no count added to the result).
 */
public final class TransmuteRecipe implements CraftingRecipe {

    /** One input and one material, like 26.3's default {@code material_count}. */
    private static final int INGREDIENT_COUNT = 2;

    private final ResourceLocation id;
    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient input;
    private final Ingredient material;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;

    public TransmuteRecipe(ResourceLocation id, String group, CraftingBookCategory category, Ingredient input, Ingredient material,
                           ItemStack result) {
        this.id = id;
        this.group = group;
        this.category = category;
        this.input = input;
        this.material = material;
        this.result = result;
        this.ingredients = NonNullList.of(Ingredient.EMPTY, input, material);
    }

    @Override
    public boolean matches(CraftingContainer craftingInput, Level level) {
        int ingredientCount = 0;
        for (int slot = 0; slot < craftingInput.getContainerSize(); slot++) {
            if (!craftingInput.getItem(slot).isEmpty()) {
                ingredientCount++;
            }
        }
        if (ingredientCount != INGREDIENT_COUNT) {
            return false;
        }
        ItemStack found = findInput(craftingInput);
        if (found.isEmpty()) {
            return false;
        }
        boolean hasMaterial = false;
        for (int slot = 0; slot < craftingInput.getContainerSize(); slot++) {
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
        return hasMaterial && !ItemStack.isSameItemSameTags(found, transmute(found));
    }

    @Override
    public ItemStack assemble(CraftingContainer craftingInput, RegistryAccess registries) {
        ItemStack found = findInput(craftingInput);
        return found.isEmpty() ? ItemStack.EMPTY : transmute(found);
    }

    private ItemStack findInput(CraftingContainer craftingInput) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < craftingInput.getContainerSize(); slot++) {
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
        ItemStack out = new ItemStack(result.getItem(), result.getCount());
        CompoundTag inputTag = from.getTag();
        if (inputTag != null) {
            out.setTag(inputTag.copy());
        }
        CompoundTag resultTag = result.getTag();
        if (resultTag != null) {
            out.getOrCreateTag().merge(resultTag.copy());
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= INGREDIENT_COUNT;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
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
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FemboyRecipes.CRAFTING_TRANSMUTE.get();
    }

    /**
     * Same JSON shape as 26.3's {@code crafting_transmute}: category, group, input, material, result. The result is
     * read like a shaped recipe's ({@code item}, {@code count}, and {@code nbt}, see RecipeResultNbtMixin).
     */
    public static final class Serializer implements RecipeSerializer<TransmuteRecipe> {

        @Override
        public TransmuteRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory category = CraftingBookCategory.CODEC
                    .byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            Ingredient input = Ingredient.fromJson(GsonHelper.getNonNull(json, "input"), false);
            Ingredient material = Ingredient.fromJson(GsonHelper.getNonNull(json, "material"), false);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new TransmuteRecipe(id, group, category, input, material, result);
        }

        @Override
        public TransmuteRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            Ingredient input = Ingredient.fromNetwork(buf);
            Ingredient material = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            return new TransmuteRecipe(id, group, category, input, material, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TransmuteRecipe recipe) {
            buf.writeUtf(recipe.group);
            buf.writeEnum(recipe.category);
            recipe.input.toNetwork(buf);
            recipe.material.toNetwork(buf);
            buf.writeItem(recipe.result);
        }
    }
}
