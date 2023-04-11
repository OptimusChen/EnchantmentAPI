package enchantmentapi.enchantmentapi;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public abstract class CustomEnchantment implements Listener {

    public CustomEnchantment() {
        Bukkit.getPluginManager().registerEvents(this, EnchantmentAPI.getInstance());
    }

    protected abstract String getName();
    protected abstract String getNamespace();
    protected abstract int getMaxLevel();
    protected abstract boolean canEnchant(ItemStack item);

    public String getKey() { return "customenchantments." + getNamespace() + ":" + getName(); }

}
