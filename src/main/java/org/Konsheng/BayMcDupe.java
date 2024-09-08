package org.Konsheng;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class BayMcDupe extends JavaPlugin {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final HashMap<UUID, Long> lastDupeTime = new HashMap<>(); // 记录每个玩家上次执行 dupe 命令的时间

    @Override
    public void onEnable() {
        try (BukkitAudiences ignored = BukkitAudiences.create(this)) {
            getLogger().info("BayMcDupe 已启用");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("BayMcDupe 已禁用");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (label.equalsIgnoreCase("dupe") && sender instanceof Player player) {

            // 检查命令执行频率限制
            if (!canExecuteDupe(player)) {
                sendFormattedMessage(player, "<white>您的操作过于频繁, 请稍后再试");
                playErrorSound(player);
                return true;
            }

            // 记录当前时间
            lastDupeTime.put(player.getUniqueId(), System.currentTimeMillis());

            // 获取玩家手持物品
            ItemStack handItem = player.getInventory().getItemInMainHand();

            // 如果手上没有物品
            if (handItem.getType().isAir()) {
                sendFormattedMessage(player, "<white>您当前没有手持物品");
                playErrorSound(player);
                return true;
            }

            // 定义默认复制数量为1
            int count = 1;

            // 判断指令是否带有数量参数
            if (args.length == 1) {
                try {
                    count = Integer.parseInt(args[0]);

                    // 如果数量超过3，提示错误
                    if (count > 3) {
                        sendFormattedMessage(player, "<white>您单次最多可以复制 3 次物品");
                        playErrorSound(player);
                        return true;
                    } else if (count < 1) {
                        sendFormattedMessage(player, "<white>您最低只能复制 1 次物品");
                        playErrorSound(player);
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sendFormattedMessage(player, "<white>请您输入有效的数字");
                    playErrorSound(player);
                    return true;
                }
            }

            // 检查玩家快捷栏是否有足够空间
            if (!hasEnoughSpaceInHotbar(player.getInventory(), handItem, count)) {
                sendFormattedMessage(player, "<white>您的物品栏空间不足");
                playErrorSound(player);
                return true;
            }

            // 开始复制物品
            for (int i = 0; i < count; i++) {
                player.getInventory().addItem(new ItemStack(handItem));
            }

            sendFormattedMessage(player, "<white>您已成功复制 " + count + " 次手持物品");
            playSuccessSound(player);
        } else {
            sender.sendMessage("<gradient:#495aff:#0acffe><b>BayMc</b></gradient> <gray>» <white>此命令只能由玩家执行");
        }
        return true;
    }

    // 检查玩家的快捷栏是否有足够空间放置指定数量的物品
    private boolean hasEnoughSpaceInHotbar(PlayerInventory inventory, ItemStack item, int count) {
        int itemsToPlace = count; // 需要放置的物品总数

        // 遍历快捷栏（前9个格子）
        for (int i = 0; i < 9; i++) {
            ItemStack invItem = inventory.getItem(i);

            // 空格子，完全可以放一个物品
            if (invItem == null || invItem.getType() == Material.AIR) {
                itemsToPlace--; // 放入一个物品
            } else if (invItem.isSimilar(item)) {
                // 如果是相同物品，计算可以堆叠的空位
                int spaceInStack = invItem.getMaxStackSize() - invItem.getAmount();
                if (spaceInStack > 0) {
                    itemsToPlace -= Math.min(spaceInStack, itemsToPlace); // 尽可能多放入这个格子
                }
            }

            // 如果已经没有需要放置的物品了，返回 true
            if (itemsToPlace <= 0) {
                return true;
            }
        }

        // 如果 itemsToPlace 大于 0，说明快捷栏没有足够空间
        return false;
    }

    // 检查玩家是否可以执行 dupe 命令（限制为每秒2次）
    private boolean canExecuteDupe(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 获取上次执行时间
        if (lastDupeTime.containsKey(playerId)) {
            long lastTime = lastDupeTime.get(playerId);
            // 0.5秒内不能再次执行
            return currentTime - lastTime >= 500;
        }

        return true;
    }

    // 使用 Adventure API 发送消息
    private void sendFormattedMessage(Player player, String message) {
        try (BukkitAudiences adventure = BukkitAudiences.create(this)) {
            Audience audience = adventure.player(player);
            String prefix = "<gradient:#495aff:#0acffe><b>BayMc</b></gradient> <gray>» ";
            Component component = miniMessage.deserialize(prefix + message);
            audience.sendMessage(component);
        }
    }

    // 播放成功的音效
    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    // 播放错误的音效
    private void playErrorSound(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
}
