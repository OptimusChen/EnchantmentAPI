package enchantmentapi.enchantmentapi;

import de.tr7zw.nbtapi.NBTItem;
import enchantmentapi.enchantmentapi.test.TestEnchantment;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.nbt.MojangsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.A;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class EnchantmentAPI extends JavaPlugin implements Listener {

    private static final List<CustomEnchantment> enchants = new ArrayList<>();
    private TestEnchantment test;
    private Glow glow;

    @Override
    public void onEnable() {
        glow = new Glow();
        test = new TestEnchantment();

        registerEnchantment(glow);
        registerEnchantment(test);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    private void registerEnchantment(Enchantment enchantment) {
        try {
            Field field = Enchantment.class.getDeclaredField("acceptingNew");
            field.setAccessible(true);
            field.set(null, true);
            Enchantment.registerEnchantment(enchantment);
        } catch (Exception ignored) { }
    }

    public static EnchantmentAPI getInstance() { return getPlugin(EnchantmentAPI.class); }

    public static List<CustomEnchantment> getEnchants() { return enchants; }

    public static void registerEnchantment(CustomEnchantment enchant) {
        enchants.add(enchant);
    }

    public static ItemStack addEnchantment(ItemStack item, CustomEnchantment enchant, int level) {
        if (!enchant.canEnchant(item)) return item;

        item.addEnchantment(getInstance().glow, 1);

        level = Math.min(level, enchant.getMaxLevel());

        String key = enchant.getKey();

        NBTItem nbt = new NBTItem(item);
        boolean already = nbt.hasKey(key);

        String loreId = enchant.getName();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        if (already && meta.getLore() != null) {
            for (String s : meta.getLore()) {
                if (s.startsWith(loreId)) {
                    lore.remove(s);
                    break;
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        LinkedList<String> linked = new LinkedList<>();

        for (String s : lore) {
            linked.addLast(s);
        }

        linked.addFirst(ChatColor.GRAY + loreId + " " + toRoman(level));

        if (item.getType().equals(Material.ENCHANTED_BOOK)) {
            meta.setDisplayName(ChatColor.YELLOW + "Enchanted Book");
        }

        meta.setLore(linked);
        item.setItemMeta(meta);

        nbt = new NBTItem(item);

        nbt.setInteger(key, level);

        return nbt.getItem();
    }

    public static ItemStack removeEnchantment(ItemStack item, CustomEnchantment enchant) {
        String key = enchant.getKey();

        NBTItem nbt = new NBTItem(item);

        if (!nbt.hasKey(key)) return item;

        String loreId = enchant.getName();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        if (meta.getLore() != null) {
            for (String s : meta.getLore()) {
                if (s.startsWith(loreId)) {
                    lore.remove(s);
                    break;
                }
            }
        }

        item.removeEnchantment(getInstance().glow);

        meta.setLore(lore);
        item.setItemMeta(meta);

        nbt = new NBTItem(item);

        nbt.removeKey(key);

        return nbt.getItem();
    }

    public static boolean hasEnchantment(ItemStack item, CustomEnchantment enchant) {
        return new NBTItem(item).hasKey(enchant.getKey());
    }

    public static HashMap<CustomEnchantment, Integer> getCustomEnchantments(ItemStack item) {
        NBTItem nbt = new NBTItem(item);
        HashMap<CustomEnchantment, Integer> enchants = new HashMap<>();

        for (CustomEnchantment enchant : getEnchants()) {
            if (nbt.hasKey(enchant.getKey())) {
                enchants.put(enchant, nbt.getInteger(enchant.getKey()));
            }
        }

        return enchants;
    }

    @EventHandler
    public void onPrep(PrepareAnvilEvent e) {
        AnvilInventory inv = e.getInventory();

        ItemStack first = inv.getItem(0), second = inv.getItem(1), result = inv.getItem(2);

        if (first == null || second == null) return;

        HashMap<CustomEnchantment, Integer> firstEnchants = getCustomEnchantments(first);
        HashMap<CustomEnchantment, Integer> secEnchants = getCustomEnchantments(second);

        HashMap<CustomEnchantment, Integer> resultEnchants = new HashMap<>();

        for (Map.Entry<CustomEnchantment, Integer> entry : firstEnchants.entrySet()) {
            CustomEnchantment key = entry.getKey();
            int level = entry.getValue();

            if (!secEnchants.containsKey(key)) {
                resultEnchants.put(key, level);
                continue;
            }

            int secLevel = secEnchants.get(key);
            int newLevel = Math.max(secLevel, level);

            if (level == secLevel) newLevel = secLevel + 1;

            resultEnchants.put(key, newLevel);
        }

        for (Map.Entry<CustomEnchantment, Integer> entry : secEnchants.entrySet()) {
            CustomEnchantment key = entry.getKey();
            int level = entry.getValue();

            if (!firstEnchants.containsKey(key)) {
                if (!resultEnchants.containsKey(key)) resultEnchants.put(key, level);
            }
        }

        AtomicInteger cost = new AtomicInteger(inv.getRepairCost());

        if (result == null && (!firstEnchants.isEmpty() || !secEnchants.isEmpty())) {
            result = first.clone();

            cost.set(2);
        }

        ItemMeta meta = result.getItemMeta();
        meta.setLore(new ArrayList<>());
        result.setItemMeta(meta);

        for (Map.Entry<CustomEnchantment, Integer> entry : resultEnchants.entrySet()) {
            result = addEnchantment(result, entry.getKey(), entry.getValue());
        }

        e.setResult(result);

        getServer().getScheduler().runTask(this, () -> e.getInventory().setRepairCost(cost.get()));
    }

    @EventHandler
    public void onGrind(PrepareGrindstoneEvent e) {
        GrindstoneInventory inv = e.getInventory();

        ItemStack result = inv.getItem(2);
        HashMap<CustomEnchantment, Integer> enchants = getCustomEnchantments(result);

        for (CustomEnchantment enchant : enchants.keySet()) {
            result = removeEnchantment(result, enchant);
        }

        e.setResult(result);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        item = addEnchantment(item, test, 1);
        e.getPlayer().getInventory().addItem(item);

        item = new ItemStack(Material.ENCHANTED_BOOK);
        item = addEnchantment(item, test, 1);
        e.getPlayer().getInventory().addItem(item);
    }

    private final static TreeMap<Integer, String> romanMap = new TreeMap<Integer, String>() {{
        put(1000, "M");
        put(900, "CM");
        put(500, "D");
        put(400, "CD");
        put(100, "C");
        put(90, "XC");
        put(50, "L");
        put(40, "XL");
        put(10, "X");
        put(9, "IX");
        put(5, "V");
        put(4, "IV");
        put(1, "I");
    }};

    private static String toRoman(int number) {
        if (number <= 0) return "";

        int l = romanMap.floorKey(number);
        if (number == l) {
            return romanMap.get(number);
        }
        return romanMap.get(l) + toRoman(number - l);
    }
}
