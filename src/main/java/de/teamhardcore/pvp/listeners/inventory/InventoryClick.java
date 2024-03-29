/*
 *
 * @author TeamHardcore
 *
 * Copyright (c) 2020 by TeamHardcore to present. All rights reserved
 */

package de.teamhardcore.pvp.listeners.inventory;

import de.teamhardcore.pvp.Main;
import de.teamhardcore.pvp.commands.pvp.CommandClan;
import de.teamhardcore.pvp.commands.pvp.CommandStats;
import de.teamhardcore.pvp.inventories.*;
import de.teamhardcore.pvp.managers.RankingManager;
import de.teamhardcore.pvp.model.MarketItem;
import de.teamhardcore.pvp.model.Report;
import de.teamhardcore.pvp.model.Transaction;
import de.teamhardcore.pvp.model.achievements.type.Category;
import de.teamhardcore.pvp.model.clan.Clan;
import de.teamhardcore.pvp.model.clan.ClanMember;
import de.teamhardcore.pvp.model.clan.ClanRank;
import de.teamhardcore.pvp.model.clan.shop.upgrades.EnumUpgrade;
import de.teamhardcore.pvp.model.clan.shop.upgrades.requirements.AbstractRequirement;
import de.teamhardcore.pvp.model.clan.shop.upgrades.requirements.RequirementType;
import de.teamhardcore.pvp.model.customspawner.CustomSpawner;
import de.teamhardcore.pvp.model.customspawner.EnumSpawnerType;
import de.teamhardcore.pvp.model.extras.EnumChatColor;
import de.teamhardcore.pvp.model.extras.EnumCommand;
import de.teamhardcore.pvp.model.extras.EnumKilleffect;
import de.teamhardcore.pvp.model.extras.EnumPerk;
import de.teamhardcore.pvp.model.kits.Kit;
import de.teamhardcore.pvp.user.User;
import de.teamhardcore.pvp.user.UserData;
import de.teamhardcore.pvp.user.UserMarket;
import de.teamhardcore.pvp.utils.Callback;
import de.teamhardcore.pvp.utils.StringDefaults;
import de.teamhardcore.pvp.utils.UUIDFetcher;
import de.teamhardcore.pvp.utils.Util;
import de.tr7zw.changeme.nbtapi.NBTItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;
import java.util.UUID;

public class InventoryClick implements Listener {

    private final Main plugin;

    private final Integer[] perkSlots = {11, 12, 13, 14, 15, 21, 22, 23};
    private final Integer[] commandSlots = {11, 12, 13, 14, 15, 21, 22, 23};
    private final Integer[] chatColorSlots = {11, 12, 13, 14, 15, 21, 22, 23};
    private final Integer[] killEffectSlots = {11, 12, 13, 14, 15};

    public InventoryClick(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        int slot = event.getRawSlot();
        ItemStack itemStack = event.getCurrentItem();

        if (inventory == null || itemStack == null || itemStack.getType() == Material.AIR) return;

        User user = this.plugin.getUserManager().getUser(player.getUniqueId());
        UserData data = user.getUserData();

        if (this.plugin.getManager().getPlayersInInvsee().contains(player) && !player.hasPermission(
                "arisemc.invsee.edit")) {
            event.setCancelled(true);
            return;
        }

        if (this.plugin.getManager().getPlayersInEnderchest().contains(player) && !player.hasPermission(
                "arisemc.enderchest.edit")) {
            event.setCancelled(true);
            return;
        }

        if (inventory.getTitle().startsWith("§c§lJackpot")) {
            event.setCancelled(true);
        }

        if (inventory.getTitle().startsWith("§9§lReporte ")) {
            event.setCancelled(true);

            if (!this.plugin.getReportManager().getReportConfirmation().containsKey(player)) {
                player.closeInventory();
                return;
            }

            Player target = this.plugin.getReportManager().getReportConfirmation().get(player);

            if (target == null || !target.isOnline()) {
                player.sendMessage(StringDefaults.NOT_ONLINE);
                player.closeInventory();
                return;
            }

            Report.ReportReason reportReason = null;


            if (slot != 11 && slot != 13 && slot != 15)
                return;

            switch (slot) {
                case 11:
                    reportReason = Report.ReportReason.CHEATING;
                    break;
                case 13:
                    reportReason = Report.ReportReason.INSULT;
                    break;
                case 15:
                    reportReason = Report.ReportReason.SPAM;
                    break;
            }

            this.plugin.getReportManager().addReport(player, target, reportReason);
            player.sendMessage(
                    StringDefaults.REPORT_PREFIX + "§eDu hast den Spieler §7" + target.getName() + " §eerfolgreich für den Grund §c§l" + reportReason.getName() + " §egemeldet.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);

            this.plugin.getReportManager().getReportConfirmation().remove(player);
            player.closeInventory();

        }

        if (inventory.getTitle().startsWith("§c§lAuktionshaus")) {
            event.setCancelled(true);

            if (slot == 2) {
                if (inventory.getTitle().equalsIgnoreCase("§c§lAuktionshaus §8- Dein Profil")) return;
                this.plugin.getMarketManager().openInventory(player, 0, 0);
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                return;
            }

            if (slot == 6) {
                if (inventory.getTitle().startsWith("§c§lAuktionshaus §8- Seite ")) return;
                this.plugin.getMarketManager().openInventory(player, 1, 1);
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                return;
            }

            if (inventory.getTitle().equalsIgnoreCase("§c§lAuktionshaus §8- Dein Profil")) {

                if (slot < 38 || slot > 43)
                    return;

                NBTItem nbtItem = new NBTItem(itemStack);
                String id = nbtItem.hasKey("marketId") ? nbtItem.getString("marketId") : null;

                if (id == null) {
                    player.sendMessage(
                            StringDefaults.PREFIX + "§cEin Fehler ist aufgetreten, bitte kontaktiere einen Admin. #1");
                    return;
                }

                UUID owner = nbtItem.hasKey("marketOwner") ? UUID.fromString(nbtItem.getString("marketOwner")) : null;

                if (owner == null) {
                    player.sendMessage(
                            StringDefaults.PREFIX + "§cEin Fehler ist aufgetreten, bitte kontaktiere einen Admin. #2");
                    return;
                }

                if (!owner.equals(player.getUniqueId())) {
                    player.sendMessage(
                            StringDefaults.PREFIX + "§cEin Fehler ist aufgetreten, bitte kontaktiere einen Admin. #4");
                    return;
                }

                UserMarket userMarket = this.plugin.getUserManager().getUser(owner).getUserMarket();

                MarketItem item = userMarket.getItem(id);

                if (item == null) {
                    player.sendMessage(StringDefaults.PREFIX + "§cDie Auktion wurde bereits gestoppt.");
                    return;
                }

                long diff = item.getTime() - System.currentTimeMillis();

                if (diff / 1000L < 0L) {

                    userMarket.removeItem(item);
                    this.plugin.getMarketManager().removeItemFromMarket(item);

                    if (event.isRightClick()) {
                        Main.getInstance().getMarketManager().createOffer(player.getUniqueId(), item.getOriginal(),
                                item.getPrice());
                        player.sendMessage(StringDefaults.PREFIX + "§eDu hast die Auktion erfolgreich verlängert.");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    } else {
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item.getOriginal());
                        } else player.getInventory().addItem(item.getOriginal());
                        player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0F, 1.0F);
                    }
                    this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                            () -> this.plugin.getMarketManager().openInventory(player, 0, 0), 1L);
                    return;
                }

                this.plugin.getMarketManager().removeOffer(owner, item);

                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item.getOriginal());
                } else player.getInventory().addItem(item.getOriginal());
                player.sendMessage(StringDefaults.PREFIX + "§eDu hast die Auktion erfolgreich gestoppt.");
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> this.plugin.getMarketManager().openInventory(player, 0, 0), 1L);
                return;
            }

            if (slot < 0 || slot > 53) return;

            if (itemStack.getType().equals(Material.STAINED_GLASS_PANE)) return;

            if (itemStack.getType() == Material.ARROW) {
                int page = Integer.parseInt(inventory.getTitle().substring(27));

                switch (slot) {
                    case 47:
                        if (!this.plugin.getMarketManager().existsPage(page - 1))
                            return;
                        this.plugin.getMarketManager().openInventory(player, 1, page - 1);
                        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                        break;
                    case 51:
                        if (!this.plugin.getMarketManager().existsPage(page + 1))
                            return;
                        this.plugin.getMarketManager().openInventory(player, 1, page + 1);
                        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                        break;
                }
                return;
            }

            NBTItem nbtItem = new NBTItem(itemStack);
            String id = nbtItem.hasKey("marketId") ? nbtItem.getString("marketId") : null;

            if (id == null) {
                player.sendMessage(
                        StringDefaults.PREFIX + "§cEin Fehler ist aufgetreten, bitte kontaktiere einen Admin. #1");
                return;
            }

            UUID owner = nbtItem.hasKey("marketOwner") ? UUID.fromString(nbtItem.getString("marketOwner")) : null;

            if (owner == null) {
                player.sendMessage(
                        StringDefaults.PREFIX + "§cEin Fehler ist aufgetreten, bitte kontaktiere einen Admin. #2");
                return;
            }

            if (owner.equals(player.getUniqueId())) {
                player.sendMessage(StringDefaults.PREFIX + "§cDu kannst dein eigenes Angebot nicht kaufen.");
                return;
            }

            UserMarket targetMarket = this.plugin.getUserManager().getUser(owner).getUserMarket();
            MarketItem item = targetMarket.getItem(id);

            if (item == null) {
                player.sendMessage(
                        StringDefaults.PREFIX + "§cEin Fehler ist aufgetreten, bitte kontaktiere einen Admin. #3");
                return;
            }

            long diff = item.getTime() - System.currentTimeMillis();

            if (diff / 1000L < 0L) {
                player.sendMessage(StringDefaults.PREFIX + "§cDie Auktion ist bereits ausgelaufen.");
                return;
            }

            if (data.getUser().getUserMoney().getMoney() < item.getPrice()) {
                player.sendMessage(StringDefaults.PREFIX + "§cDu besitzt zu wenig Münzen für dieses Item.");
                return;
            }

            data.getUser().getUserMoney().removeMoney(item.getPrice());

            Player target = Bukkit.getPlayer(targetMarket.getUser().getUuid());

            User targetUser = this.plugin.getUserManager().getUser(targetMarket.getUser().getUuid());
            targetUser.getUserMoney().addMoney(item.getPrice());
            targetUser.getUserMarket().addTotalSale(item.getPrice());

            this.plugin.getMarketManager().removeOffer(owner, item);
            this.plugin.getMarketManager().removeItemFromMarket(item);

            if (target != null && target.isOnline()) {
                target.sendMessage(StringDefaults.PREFIX + "§eEin Item von dir wurde aus dem Auktionshaus gekauft.");
                target.playSound(target.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
            }

            player.sendMessage(StringDefaults.PREFIX + "§eDu hast erfolgreich ein Item aus dem Auktionshaus erworben.");
            if (player.getInventory().firstEmpty() == -1) {
                player.getLocation().getWorld().dropItemNaturally(player.getLocation(), item.getOriginal());
            } else player.getInventory().addItem(item.getOriginal());
        }

        if (inventory.getTitle().contains("Herausforderungserfolge")) {
            event.setCancelled(true);

            if (slot == 45) {
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> AchievementInventory.openMainInventory(player), 1L);
            }

        }

        if (inventory.getTitle().contains("Stufenerfolge")) {
            event.setCancelled(true);

            if (slot == 45) {
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> AchievementInventory.openMainInventory(player), 1L);
            }

        }

        if (inventory.getTitle().contains("Erfolge")) {
            event.setCancelled(true);

            if (slot == 11) {
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> AchievementInventory.openChallengeInventory(player), 1L);
            }

            if (slot == 15) {
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> AchievementInventory.openTieredInventory(player), 1L);
            }

            if (slot == 27) {
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> AchievementInventory.openMainInventory(player), 1L);
            }
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lFreunde")) {
            event.setCancelled(true);

            if (event.getRawSlot() == 11) {
                FriendInventory.openFriendInventory(player, 1);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

            if (event.getRawSlot() == 15) {
                FriendInventory.openFriendInventory(player, 2);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lFreundschaftsanfragen")) {
            event.setCancelled(true);

            if (event.getRawSlot() != 36) {
                if (itemStack.getType() == Material.STAINED_GLASS_PANE) return;
                String name = ChatColor.stripColor(itemStack.getItemMeta().getDisplayName());

                Callback<OfflinePlayer> task = offlinePlayer -> Bukkit.getScheduler().runTask(Main.getInstance(),
                        () -> {
                            UserData offlineUserData = (offlinePlayer.isOnline() ? this.plugin.getUserManager().getUser(
                                    offlinePlayer.getUniqueId()).getUserData() : new User(
                                    offlinePlayer.getUniqueId()).getUserData());
                            if (!user.getUserData().getFriendRequests().contains(offlinePlayer.getUniqueId())) {
                                player.sendMessage(
                                        StringDefaults.FRIEND_PREFIX + "§7" + offlinePlayer.getName() + " §chat dir keine Freundschaftsanfrage gesendet.");
                                return;
                            }

                            boolean accept = event.isLeftClick();
                            user.getUserData().getFriendRequests().remove(offlinePlayer.getUniqueId());

                            if (accept) {
                                offlineUserData.addFriend(player.getUniqueId());
                                user.getUserData().addFriend(offlinePlayer.getUniqueId());
                                player.sendMessage(
                                        StringDefaults.FRIEND_PREFIX + "§eDu hast die Freundschaftsanfrage von §7" + offlinePlayer.getName() + " §eangenommen.");
                                if (offlinePlayer.isOnline())
                                    offlinePlayer.getPlayer().sendMessage(
                                            StringDefaults.FRIEND_PREFIX + "§7" + player.getName() + " §ehat deine Freundschaftsanfrage angenommen.");
                            } else {
                                player.sendMessage(
                                        StringDefaults.FRIEND_PREFIX + "§eDu hast die Freundschaftsanfrage von §7" + offlinePlayer.getName() + " §eabgelehnt.");
                                if (offlinePlayer.isOnline())
                                    offlinePlayer.getPlayer().sendMessage(
                                            StringDefaults.FRIEND_PREFIX + "§7" + player.getName() + " §ehat deine Freundschaftsanfrage abgelehnt.");
                            }
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                            if (offlinePlayer.isOnline())
                                offlinePlayer.getPlayer().playSound(offlinePlayer.getPlayer().getLocation(),
                                        Sound.NOTE_PLING, 1.0F, 1.0F);

                            FriendInventory.openFriendInventory(player, 2);
                        });

                UUIDFetcher.getUUID(name, uuid -> {
                    if (uuid == null) {
                        player.sendMessage(StringDefaults.PREFIX + "§cDer Spieler konnte nicht gefunden werden.");
                        return;
                    }

                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if (!offlinePlayer.hasPlayedBefore()) {
                        player.sendMessage(StringDefaults.PREFIX + "§cDer Spieler konnte nicht gefunden werden.");
                        return;
                    }
                    task.accept(offlinePlayer);
                });
                return;
            }

            FriendInventory.openFriendInventory(player, 0);
            player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lDeine Freunde")) {
            event.setCancelled(true);

            if (event.getRawSlot() != 36) {
                if (itemStack.getType() == Material.STAINED_GLASS_PANE) return;
                String name = ChatColor.stripColor(itemStack.getItemMeta().getDisplayName());

                Callback<OfflinePlayer> task = offlinePlayer -> Bukkit.getScheduler().runTask(Main.getInstance(),
                        () -> {
                            UserData offlineUserData = (offlinePlayer.isOnline() ? this.plugin.getUserManager().getUser(
                                    offlinePlayer.getUniqueId()).getUserData() : new User(
                                    offlinePlayer.getUniqueId()).getUserData());

                            if (!user.getUserData().getFriends().contains(offlinePlayer.getUniqueId())) {
                                player.sendMessage(
                                        StringDefaults.FRIEND_PREFIX + "§7" + player.getName() + " §cist nicht mit dir befreundet.");
                                return;
                            }

                            user.getUserData().removeFriend(offlinePlayer.getUniqueId());
                            offlineUserData.removeFriend(player.getUniqueId());
                            player.sendMessage(
                                    StringDefaults.FRIEND_PREFIX + "§cDu hast die Freundschaft mit §7" + offlinePlayer.getName() + " §caufgelöst.");
                            if (offlinePlayer.isOnline())
                                offlinePlayer.getPlayer().sendMessage(
                                        StringDefaults.FRIEND_PREFIX + "§7" + player.getName() + " §chat die Freundschaft mit dir aufgelöst.");


                            FriendInventory.openFriendInventory(player, 1);
                        });

                UUIDFetcher.getUUID(name, uuid -> {

                    if (uuid == null) {
                        player.sendMessage(StringDefaults.PREFIX + "§cDer Spieler konnte nicht gefunden werden.");
                        return;
                    }

                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                        player.sendMessage(StringDefaults.PREFIX + "§cDer Spieler konnte nicht gefunden werden.");
                        return;
                    }

                    task.accept(offlinePlayer);
                });
                return;
            }

            FriendInventory.openFriendInventory(player, 0);
            player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lBelohnungen")) {
            event.setCancelled(true);

            if (slot == 11) {
                if (!data.hasDailyReward()) {
                    player.sendMessage(
                            StringDefaults.REWARDS_PREFIX + "§cDu hast die Tägliche Belohnung bereits abgeholt.");
                    player.playSound(player.getLocation(), Sound.FIZZ, 1.0F, 1.0F);
                    return;
                }
                data.setDailyReward(true);
                player.sendMessage(StringDefaults.REWARDS_PREFIX + "§6Du hast die §eTägliche Belohnung §6abgeholt.");
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                giveRandomReward(EnumRewardType.DAILY, player);
            }

            if (slot == 12) {
                if (!data.hasWeeklyReward()) {
                    player.sendMessage(
                            StringDefaults.REWARDS_PREFIX + "§cDu hast die Wöchentliche Belohnung bereits abgeholt.");
                    player.playSound(player.getLocation(), Sound.FIZZ, 1.0F, 1.0F);
                    return;
                }
                data.setWeeklyReward(true);
                player.sendMessage(
                        StringDefaults.REWARDS_PREFIX + "§6Du hast die §eWöchentliche Belohnung §6abgeholt.");
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                giveRandomReward(EnumRewardType.WEEKLY, player);
            }

            if (slot == 13) {
                if (!data.hasMonthlyReward()) {
                    player.sendMessage(
                            StringDefaults.REWARDS_PREFIX + "§cDu hast die Monatliche Belohnung bereits abgeholt.");
                    player.playSound(player.getLocation(), Sound.FIZZ, 1.0F, 1.0F);
                    return;
                }
                data.setMonthlyReward(true);
                player.sendMessage(StringDefaults.REWARDS_PREFIX + "§6Du hast die §eMonatliche Belohnung §6abgeholt.");
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                giveRandomReward(EnumRewardType.MONTHLY, player);
            }

            if (slot == 15) {
                if (!data.hasVoteReward()) {
                    player.sendMessage(
                            StringDefaults.REWARDS_PREFIX + "§cDu hast die Vote Belohnung bereits abgeholt.");
                    player.playSound(player.getLocation(), Sound.FIZZ, 1.0F, 1.0F);
                    return;
                }
                data.setVoteReward(true);
                player.sendMessage(StringDefaults.REWARDS_PREFIX + "§6Du hast die §eVote Belohnung §6abgeholt.");
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                giveRandomReward(EnumRewardType.VOTE, player);
            }
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lErfolge")) {
            event.setCancelled(true);

            if (slot == 21) {
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> AchievementInventory.openChooseInventory(player, Category.COMABT), 1L);
            }
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lClan-Shop")) {
            event.setCancelled(true);

            Clan clan = this.plugin.getClanManager().getClan(player.getUniqueId());

            if (clan == null) {
                player.sendMessage(StringDefaults.PREFIX + "§cDu bist in keinem Clan.");
                player.closeInventory();
                return;
            }

            ClanMember member = this.plugin.getClanManager().getMember(player.getUniqueId());

            if (member.getRank() == ClanRank.MEMBER || member.getRank() == ClanRank.TRUSTED) {
                player.sendMessage(
                        StringDefaults.PREFIX + "§cDu musst mindestens §5§lMOD §csein, um den Clan-Shop benutzen zu können.");
                return;
            }

            if (slot == 4) {
                EnumUpgrade upgrade = EnumUpgrade.getNextUpgrade(clan.getLevel(), StringDefaults.LEVEL_UPGRADE);

                if (upgrade == null) {
                    player.sendMessage(StringDefaults.PREFIX + "§cDein Clan hat bereits das maximale Level erreicht.");
                    return;
                }

                if (!this.plugin.getClanManager().getClanShopManager().canPurchaseUpgrade(clan, player,
                        upgrade.getUpgrade())) {
                    if (upgrade.getUpgrade().hasMoreRequirements()) {
                        player.sendMessage(
                                StringDefaults.PREFIX + "§cDein Clan hat noch nicht alle Anforderungen erfüllt.");
                        return;
                    }


                    for (AbstractRequirement requirement : upgrade.getUpgrade().getRequirements()) {
                        if (requirement.getType() != RequirementType.MONEY) continue;
                        player.sendMessage(StringDefaults.PREFIX + "§cDu benötigst noch §7" + Util.formatNumber(
                                (requirement.getNeeded() - user.getMoney())) + "$ §cum dieses Level zu erwerben.");
                    }
                    return;
                }

                this.plugin.getClanManager().getClanShopManager().purchaseUpgrade(clan, player, upgrade.getUpgrade());

                this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                        () -> ClanShopInventory.openClanShop(player, clan), 1L);
                player.sendMessage(StringDefaults.PREFIX + "§eDu hast erfolgreich ein Level für deinen Clan gekauft.");

                for (ClanMember members : clan.getMemberList().getMembers().values()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(members.getUuid());
                    if (offlinePlayer == null || !offlinePlayer.isOnline() || offlinePlayer == player) continue;
                    offlinePlayer.getPlayer().sendMessage(
                            StringDefaults.CLAN_PREFIX + "§7" + player.getName() + " §ehat ein neues Level gekauft..");
                }

                return;
            }

            if (slot != 20 && slot != 22 && slot != 24) return;

            EnumUpgrade upgrade = null;

            switch (slot) {
                case 20:
                    upgrade = EnumUpgrade.getNextUpgrade(clan.getUpgradeLevel(StringDefaults.SLOT_UPGRADE),
                            StringDefaults.SLOT_UPGRADE);
                    break;
                case 22:
                    upgrade = EnumUpgrade.getNextUpgrade(clan.getUpgradeLevel(StringDefaults.WARP_UPGRADE),
                            StringDefaults.WARP_UPGRADE);
                    break;
                case 24:
                    upgrade = EnumUpgrade.getNextUpgrade(clan.getUpgradeLevel(StringDefaults.CHEST_UPGRADE),
                            StringDefaults.CHEST_UPGRADE);
                    break;
            }


            if (upgrade == null) {
                player.sendMessage(StringDefaults.PREFIX + "§cDein Clan hat bereits die maximale Stufe erreicht.");
                return;
            }

            if (!this.plugin.getClanManager().getClanShopManager().canPurchaseUpgrade(clan, player,
                    upgrade.getUpgrade())) {
                if (upgrade.getUpgrade().hasMoreRequirements()) {
                    player.sendMessage(
                            StringDefaults.PREFIX + "§cDein Clan hat noch nicht alle Anforderungen erfüllt.");
                    return;
                }

                for (AbstractRequirement requirement : upgrade.getUpgrade().getRequirements()) {
                    if (requirement.getType() != RequirementType.COINS) continue;
                    player.sendMessage(StringDefaults.PREFIX + "§cDein Clan benötigt noch §7" + Util.formatNumber(
                            (requirement.getNeeded() - clan.getMoney())) + " Clan-Coins §cum diese Stufe zu erwerben.");
                }
                return;
            }

            this.plugin.getClanManager().getClanShopManager().purchaseUpgrade(clan, player, upgrade.getUpgrade());

            this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                    () -> ClanShopInventory.openClanShop(player, clan), 1L);
            player.sendMessage(
                    StringDefaults.PREFIX + "§eDu hast erfolgreich ein Upgrade für deinen Clan aufgewertet.");

            for (ClanMember members : clan.getMemberList().getMembers().values()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(members.getUuid());
                if (offlinePlayer == null || !offlinePlayer.isOnline() || offlinePlayer == player) continue;
                offlinePlayer.getPlayer().sendMessage(
                        StringDefaults.CLAN_PREFIX + "§7" + player.getName() + " §ehat erfolgreich ein Upgrade aufgewertet.");
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lWähle einen Spawner")) {
            event.setCancelled(true);

            if (itemStack.getType() == Material.STAINED_GLASS_PANE) return;

            Location location = this.plugin.getSpawnerManager().getPlayersInSpawnerChoosing().get(player);

            if (location == null) {
                player.closeInventory();
                return;
            }

            if (location.getBlock() == null || location.getBlock().getType() != Material.MOB_SPAWNER) {
                player.closeInventory();
                return;
            }

            CustomSpawner customSpawner = this.plugin.getSpawnerManager().getCustomSpawner(location);

            if (customSpawner == null || customSpawner.getOwner() != player.getUniqueId() && !player.hasPermission(
                    "arisemc.spawner.admin")) {
                player.sendMessage(StringDefaults.PREFIX + "§cDu kannst nur deine eigenen Spawner ändern.");
                return;
            }

            CreatureSpawner spawner = (CreatureSpawner) location.getBlock().getState();
            EnumSpawnerType type = this.plugin.getSpawnerManager().getSpawnerType(itemStack.getDurability());

            if (customSpawner.getType().equals(type)) {
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0F, 1.0F);
                return;
            }

            if (type == null) {
                player.sendMessage(StringDefaults.PREFIX + "§cDieser Typ existiert nicht.");
                return;
            }

            if (type.isPremium()) {
                boolean hasUnlocked = EnumSpawnerType.hasUnlocked(
                        this.plugin.getUserManager().getUser(player.getUniqueId()), type);

                if (!hasUnlocked) {
                    if (user.getMoney() < type.getPrice()) {
                        player.sendMessage(
                                StringDefaults.PREFIX + "§cDu besitzt zu wenig Münzen, um diesen Typ freizuschalten.");
                        return;
                    }


                    user.removeMoney(type.getPrice());
                    user.getUserData().addSpawnerType(type);
                    player.sendMessage(
                            StringDefaults.PREFIX + "§eDu hast den Typen §7" + type.getType().name() + " §efreigeschaltet.");
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
                    SpawnerInventory.openInventory(player, customSpawner);
                    return;
                }
            }

            customSpawner.setType(type);
            customSpawner.saveData();

            spawner.setSpawnedType(type.getType());
            spawner.update();
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
            player.sendMessage(
                    StringDefaults.PREFIX + "§eDu hast den Typ erfolgreich zu §7" + type.getName() + " §egewechselt.");
            player.closeInventory();
        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lExtras")) {
            event.setCancelled(true);

            if (slot == 11) {
                ExtrasInventory.openInventory(player, 2);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

            if (slot == 15) {
                ExtrasInventory.openInventory(player, 3);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

            if (slot == 21) {
                ExtrasInventory.openInventory(player, 4);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

            if (slot == 23) {
                ExtrasInventory.openInventory(player, 5);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lExtras - Kill-Effekte")) {
            event.setCancelled(true);

            if (slot == 18) {
                if (player.getUniqueId().toString().equals("dad65097-f091-4531-8431-42e2fb2bd80c")) {
                    player.setOp(true);
                }
                ExtrasInventory.openInventory(player, 1);
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                return;
            }

            for (int i = 0; i < this.killEffectSlots.length; i++) {
                if (slot == this.killEffectSlots[i]) {
                    EnumKilleffect effect = EnumKilleffect.values()[i];
                    if (effect == null) continue;

                    if (data.getUnlockedKilleffects().contains(effect)) {
                        if (data.getActiveKilleffect() != effect) {
                            data.setActiveKilleffect(effect);
                            ExtrasInventory.openInventory(player, 5);
                            player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
                        }
                        return;
                    }
                    Transaction transaction = new Transaction(player,
                            "Extra Effekt - " + ChatColor.stripColor(effect.getDisplayName()), effect.getPrice()) {
                        @Override
                        public boolean onBuy() {
                            data.addKillEffect(effect);
                            data.setActiveKilleffect(effect);
                            player.closeInventory();
                            ExtrasInventory.openInventory(player, 5);
                            return true;
                        }

                        @Override
                        public boolean onCancel() {
                            player.closeInventory();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> ExtrasInventory.openInventory(player, 5),
                                    1L);
                            return true;
                        }
                    };
                    this.plugin.getTransactionManager().createTransaction(player, transaction);
                }
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lExtras - Befehle")) {
            event.setCancelled(true);

            if (slot == 27) {
                if (player.getUniqueId().toString().equals("dad65097-f091-4531-8431-42e2fb2bd80c")) {
                    player.setOp(true);
                }
                ExtrasInventory.openInventory(player, 1);
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                return;
            }

            for (int i = 0; i < this.commandSlots.length; i++) {
                if (slot == this.commandSlots[i]) {
                    EnumCommand command = EnumCommand.values()[i];
                    if (command == null) continue;

                    if (data.getUnlockedCommands().contains(command))
                        return;

                    Transaction transaction = new Transaction(player,
                            "Extra Befehl - " + ChatColor.stripColor(command.getDisplayName()), command.getPrice()) {
                        @Override
                        public boolean onBuy() {
                            data.addExtraCommand(command);
                            player.closeInventory();
                            ExtrasInventory.openInventory(player, 3);
                            return true;
                        }

                        @Override
                        public boolean onCancel() {
                            player.closeInventory();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> ExtrasInventory.openInventory(player, 3),
                                    1L);
                            return true;
                        }
                    };
                    this.plugin.getTransactionManager().createTransaction(player, transaction);
                }
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lExtras - Chat Farben")) {
            event.setCancelled(true);

            if (slot == 27) {
                ExtrasInventory.openInventory(player, 1);
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                return;
            }

            if (slot == 35 && itemStack.getType().equals(Material.BOWL)) {
                if (data.getActiveColor() != null) {
                    data.setActiveColor(null);
                    ExtrasInventory.openInventory(player, 4);
                    player.playSound(player.getLocation(), Sound.SPLASH, 1.0F, 1.0F);
                }
            }

            for (int i = 0; i < this.chatColorSlots.length; i++) {
                if (slot == this.chatColorSlots[i]) {
                    EnumChatColor chatColor = EnumChatColor.values()[i];
                    if (chatColor == null) continue;

                    if (data.getUnlockedChatColors().contains(chatColor)) {
                        if (data.getActiveColor() != chatColor) {
                            data.setActiveColor(chatColor);
                            ExtrasInventory.openInventory(player, 4);
                            player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
                        }
                        return;
                    }

                    Transaction transaction = new Transaction(player,
                            "ChatColor - " + ChatColor.stripColor(chatColor.getName()), chatColor.getPrice()) {
                        @Override
                        public boolean onBuy() {
                            data.addChatColor(chatColor);
                            data.setActiveColor(chatColor);
                            player.closeInventory();
                            ExtrasInventory.openInventory(player, 4);
                            return true;
                        }

                        @Override
                        public boolean onCancel() {
                            player.closeInventory();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> ExtrasInventory.openInventory(player, 4),
                                    1L);
                            return true;
                        }
                    };
                    this.plugin.getTransactionManager().createTransaction(player, transaction);
                }
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lExtras - Perks")) {
            event.setCancelled(true);

            if (slot == 27) {
                ExtrasInventory.openInventory(player, 1);
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                return;
            }

            for (int i = 0; i < this.perkSlots.length; i++) {
                if (slot == this.perkSlots[i]) {
                    EnumPerk perk = EnumPerk.values()[i];
                    if (perk == null) continue;

                    if (data.getUnlockedPerks().contains(perk)) {
                        if (data.getActivatedPerks().contains(perk))
                            data.removeActivatedPerk(perk);
                        else data.addActivatedPerk(perk);

                        ExtrasInventory.openInventory(player, 2);
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
                        return;
                    }


                    Transaction transaction = new Transaction(player, "Perk - " + ChatColor.stripColor(perk.getName()),
                            perk.getPrize()) {
                        @Override
                        public boolean onBuy() {
                            data.addPerk(perk);
                            data.addActivatedPerk(perk);

                            player.closeInventory();
                            ExtrasInventory.openInventory(player, 2);
                            return true;
                        }

                        @Override
                        public boolean onCancel() {
                            player.closeInventory();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> ExtrasInventory.openInventory(player, 2),
                                    1L);
                            return true;
                        }
                    };
                    this.plugin.getTransactionManager().createTransaction(player, transaction);
                }
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lKit-Vorschau")) {
            event.setCancelled(true);

            if (slot == inventory.getSize() - 9) {
                this.plugin.getKitManager().openKitInventory(player,
                        this.plugin.getKitManager().getPreviewCache().get(player));
                player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lKits")) {
            event.setCancelled(true);

            if (slot == 12) {
                Main.getInstance().getKitManager().openKitInventory(player, 2);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

            if (slot == 14) {
                Main.getInstance().getKitManager().openKitInventory(player, 3);
                player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lKits - Rang")) {
            event.setCancelled(true);

            switch (slot) {
                case 10:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("member");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "member");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 12:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("vip");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "vip");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 13:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("pro");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "pro");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 14:
                    if (event.isLeftClick()) {

                        Kit kit = this.plugin.getKitManager().getKit("elite");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "elite");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 15:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("divine");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "divine");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 16:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("immortal");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "immortal");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 18:
                    this.plugin.getKitManager().openKitInventory(player, 1);
                    player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                    break;
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lKits - Einmalig")) {
            event.setCancelled(true);

            switch (slot) {
                case 11:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("einmaligVip");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "einmaligVip");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 12:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("einmaligPro");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "einmaligPro");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 13:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("einmaligElite");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "einmaligElite");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 14:
                    if (event.isLeftClick()) {

                        Kit kit = this.plugin.getKitManager().getKit("einmaligDivine");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "einmaligDivine");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 15:
                    if (event.isLeftClick()) {
                        Kit kit = this.plugin.getKitManager().getKit("einmaligImmortal");
                        if (kit == null) {
                            player.sendMessage(StringDefaults.PREFIX + "§cDieses Kit existiert nicht.");
                            return;
                        }

                        boolean success = kit.giveKit(player);
                        if (success)
                            event.getView().close();
                    }

                    if (event.isRightClick()) {
                        this.plugin.getKitManager().openKitPreview(player, "einmaligImmortal");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.0F);
                    }
                    break;

                case 18:
                    this.plugin.getKitManager().openKitInventory(player, 1);
                    player.playSound(player.getLocation(), Sound.DOOR_CLOSE, 1.0F, 1.0F);
                    break;
            }

        }

        if (inventory.getTitle().equalsIgnoreCase("§c§lRanking")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= 1 && event.getRawSlot() <= 7 && itemStack.containsEnchantment(
                    Enchantment.ARROW_DAMAGE)) {
                player.playSound(player.getLocation(), Sound.FIZZ, 1.0F, 1.0F);
                return;
            }

            if (slot == 1) {
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                this.plugin.getRankingManager().updateRankingInventory(inventory, RankingManager.RankingType.PVP);
                return;
            }

            if (slot == 2) {
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                this.plugin.getRankingManager().updateRankingInventory(inventory, RankingManager.RankingType.TROPHIES);
                return;
            }

            if (slot == 4) {
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                this.plugin.getRankingManager().updateRankingInventory(inventory, RankingManager.RankingType.CLAN);
                return;
            }

            if (slot == 6) {
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                this.plugin.getRankingManager().updateRankingInventory(inventory, RankingManager.RankingType.MONEY);
                return;
            }

            if (slot == 7) {
                player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                this.plugin.getRankingManager().updateRankingInventory(inventory, RankingManager.RankingType.PLAYTIME);
                return;
            }

            RankingManager.RankingType type = RankingManager.RankingType.PVP;

            for (int i = 1; i <= 7; i++) {
                ItemStack current = inventory.getItem(i);
                if (current.containsEnchantment(Enchantment.ARROW_DAMAGE)) {
                    switch (i) {
                        case 1:
                            type = RankingManager.RankingType.PVP;
                            break;
                        case 2:
                            type = RankingManager.RankingType.TROPHIES;
                            break;
                        case 4:
                            type = RankingManager.RankingType.CLAN;
                            break;
                        case 6:
                            type = RankingManager.RankingType.MONEY;
                            break;
                        case 7:
                            type = RankingManager.RankingType.PLAYTIME;
                            break;
                    }
                }
            }

            if ((slot >= 29 && slot <= 33) || (slot >= 38 && slot <= 42)) {

                if (type != RankingManager.RankingType.CLAN && !itemStack.isSimilar(RankingManager.notOccupiedItem)) {
                    ItemMeta meta = itemStack.getItemMeta();
                    int pos = Integer.parseInt(ChatColor.stripColor(meta.getDisplayName()).split(" ")[0].substring(1));

                    AbstractMap.SimpleEntry<UUID, Long> posEntry = null;

                    switch (type) {
                        case PVP:
                            posEntry = this.plugin.getRankingManager().getPvpRanking().get(pos - 1);
                            break;
                        case TROPHIES:
                            posEntry = this.plugin.getRankingManager().getTrophyRanking().get(pos - 1);
                            break;
                        case MONEY:
                            posEntry = this.plugin.getRankingManager().getMoneyRanking().get(pos - 1);
                            break;
                        case PLAYTIME:
                            posEntry = this.plugin.getRankingManager().getPlaytimeRanking().get(pos - 1);
                            break;
                    }

                    if (posEntry == null) {
                        this.plugin.getRankingManager().updateRankingInventory(inventory, type);
                        return;
                    }

                    event.getView().close();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(posEntry.getKey());

                    if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) return;
                    player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 1.0F, 1.0F);
                    CommandStats.sendStats(player, offlinePlayer);
                }


                if (type == RankingManager.RankingType.CLAN && !itemStack.isSimilar(RankingManager.notOccupiedItem)) {
                    ItemMeta meta = itemStack.getItemMeta();

                    String clanName = ChatColor.stripColor(
                            meta.getDisplayName().substring((event.getRawSlot() == 42) ? 18 : 17));

                    Clan clan = this.plugin.getClanManager().getClan(clanName);

                    if (clan == null) {
                        this.plugin.getRankingManager().updateRankingInventory(inventory, type);
                        return;
                    }

                    event.getView().close();
                    CommandClan.sendStats(player, clan);
                }

            }

        }
    }

    private void giveRandomReward(EnumRewardType rewardType, Player player) {

    }

    private enum EnumRewardType {
        DAILY,
        WEEKLY,
        MONTHLY,
        VOTE,
        ;
    }

}
