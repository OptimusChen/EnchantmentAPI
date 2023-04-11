package enchantmentapi.enchantmentapi.test;

import enchantmentapi.enchantmentapi.CustomEnchantment;
import org.bukkit.inventory.ItemStack;

public class TestEnchantment extends CustomEnchantment {
    @Override
    protected String getName() {
        return "Test";
    }

    @Override
    protected String getNamespace() {
        return "EnchantmentAPI";
    }

    @Override
    protected int getMaxLevel() {
        return 10;
    }

    @Override
    protected boolean canEnchant(ItemStack item) {
        return true;
    }
}
