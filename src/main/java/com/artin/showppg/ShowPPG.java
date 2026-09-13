package com.artin.showppg;

import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShowPPG extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "stop", "kick", "ban", "did", "op", "info", "help"
    );

    @Override
    public void onEnable() {
        getCommand("showppg").setExecutor(this);
        getCommand("showppg").setTabCompleter(this);
        getLogger().info("پلاگین ShowPPG با موفقیت فعال شد. ساخته شده توسط Artin.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("showppg")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "stop":
                handleStop(sender);
                break;

            case "kick":
                handleKick(sender, args);
                break;

            case "ban":
                handleBan(sender, args);
                break;

            case "did":
                handleDid(sender);
                break;

            case "op":
                handleOp(sender, args);
                break;

            case "info":
                handleInfo(sender);
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "دستور نامعتبر است. برای راهنما از /showppg help استفاده کن.");
                break;
        }

        return true;
    }

    // ---------------- STOP ----------------
    private void handleStop(CommandSender sender) {
        Bukkit.broadcastMessage(ChatColor.RED + "سرور توسط " + sender.getName() + " متوقف می‌شود...");
        getLogger().warning(sender.getName() + " دستور استاپ سرور را اجرا کرد.");
        Bukkit.getScheduler().runTask(this, () -> Bukkit.shutdown());
    }

    // ---------------- KICK ----------------
    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "استفاده صحیح: /showppg kick <نام بازیکن>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "بازیکنی با این نام آنلاین نیست.");
            return;
        }

        target.kickPlayer(ChatColor.RED + "شما توسط " + sender.getName() + " از سرور اخراج شدید.");
        sender.sendMessage(ChatColor.GREEN + "بازیکن " + target.getName() + " با موفقیت کیک شد.");
        getLogger().info(sender.getName() + " بازیکن " + target.getName() + " را کیک کرد.");
    }

    // ---------------- BAN ----------------
    @SuppressWarnings("deprecation")
    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "استفاده صحیح: /showppg ban <نام بازیکن>");
            return;
        }

        String targetName = args[1];

        Bukkit.getBanList(BanList.Type.NAME).addBan(
                targetName,
                "بن شده توسط " + sender.getName() + " با استفاده از ShowPPG",
                null,
                sender.getName()
        );

        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null && target.isOnline()) {
            target.kickPlayer(ChatColor.RED + "شما توسط " + sender.getName() + " بن شدید.");
        }

        sender.sendMessage(ChatColor.GREEN + "بازیکن " + targetName + " با موفقیت بن شد.");
        getLogger().info(sender.getName() + " بازیکن " + targetName + " را بن کرد.");
    }

    // ---------------- DID (پاک کردن سرور و همه‌ی مپ‌ها) ----------------
    private void handleDid(CommandSender sender) {
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD +
                "هشدار: تمام دنیاها/مپ‌های سرور توسط " + sender.getName() + " در حال پاک شدن است!");
        getLogger().warning(sender.getName() + " دستور پاک کردن کامل سرور (did) را اجرا کرد.");

        // جمع‌آوری مسیر پوشه‌ی تمام دنیاها قبل از خاموش شدن سرور
        List<File> worldFolders = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worldFolders.add(world.getWorldFolder());
        }

        // کیک کردن همه‌ی بازیکن‌ها
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(ChatColor.RED + "سرور در حال ریست کامل است. لطفاً بعداً دوباره وصل شوید.");
        }

        // ثبت یک shutdown hook که بعد از بسته شدن کامل جاوا،
        // وقتی دیگر هیچ فایلی توسط سرور قفل نیست، پوشه‌های دنیاها را پاک می‌کند
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (File folder : worldFolders) {
                deleteDirectoryRecursively(folder.toPath());
            }
        }));

        // خاموش کردن سرور تا حذف فایل‌ها به‌طور کامل و امن انجام شود
        Bukkit.getScheduler().runTask(this, () -> Bukkit.shutdown());
    }

    private void deleteDirectoryRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a)) // فایل‌های عمیق‌تر ابتدا حذف شوند
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    // ---------------- OP ----------------
    private void handleOp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "استفاده صحیح: /showppg op <نام بازیکن> <true|false>");
            return;
        }

        String targetName = args[1];
        String value = args[2].toLowerCase();

        if (!value.equals("true") && !value.equals("false")) {
            sender.sendMessage(ChatColor.RED + "مقدار باید true یا false باشد.");
            return;
        }

        boolean opValue = value.equals("true");
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        target.setOp(opValue);

        sender.sendMessage(ChatColor.GREEN + "وضعیت op برای " + targetName + " به " + value + " تغییر کرد.");
        getLogger().info(sender.getName() + " وضعیت op بازیکن " + targetName + " را به " + value + " تغییر داد.");
    }

    // ---------------- INFO ----------------
    private void handleInfo(CommandSender sender) {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSeconds = uptimeMillis / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        double tps = 0;
        try {
            tps = Bukkit.getServer().getTPS()[0];
        } catch (Throwable ignored) {
            // در برخی نسخه‌ها یا فورک‌ها ممکن است در دسترس نباشد
        }

        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "اطلاعات سرور" + ChatColor.GOLD + " =====");
        sender.sendMessage(ChatColor.AQUA + "نسخه سرور: " + ChatColor.WHITE + Bukkit.getVersion());
        sender.sendMessage(ChatColor.AQUA + "بازیکنان آنلاین: " + ChatColor.WHITE +
                Bukkit.getOnlinePlayers().size() + " / " + Bukkit.getMaxPlayers());
        sender.sendMessage(ChatColor.AQUA + "تعداد دنیاها: " + ChatColor.WHITE + Bukkit.getWorlds().size());
        sender.sendMessage(ChatColor.AQUA + "تعداد پلاگین‌ها: " + ChatColor.WHITE + Bukkit.getPluginManager().getPlugins().length);
        if (tps > 0) {
            sender.sendMessage(ChatColor.AQUA + "TPS: " + ChatColor.WHITE + String.format("%.2f", tps));
        }
        sender.sendMessage(ChatColor.AQUA + "حافظه مصرفی: " + ChatColor.WHITE + usedMemory + "MB / " + maxMemory + "MB");
        sender.sendMessage(ChatColor.AQUA + "زمان روشن بودن: " + ChatColor.WHITE +
                hours + "h " + minutes + "m " + seconds + "s");
    }

    // ---------------- HELP ----------------
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "راهنمای ShowPPG" + ChatColor.GOLD + " =====");
        sender.sendMessage(ChatColor.YELLOW + "این پلاگین برای مدیریت سریع و ساده‌ی سرور ساخته شده است.");
        sender.sendMessage(ChatColor.GRAY + "/showppg stop " + ChatColor.WHITE + "- خاموش کردن سرور");
        sender.sendMessage(ChatColor.GRAY + "/showppg kick <نام> " + ChatColor.WHITE + "- کیک کردن بازیکن");
        sender.sendMessage(ChatColor.GRAY + "/showppg ban <نام> " + ChatColor.WHITE + "- بن کردن بازیکن");
        sender.sendMessage(ChatColor.GRAY + "/showppg did " + ChatColor.WHITE + "- پاک کردن کامل سرور و تمام مپ‌ها");
        sender.sendMessage(ChatColor.GRAY + "/showppg op <نام> <true|false> " + ChatColor.WHITE + "- دادن یا گرفتن OP");
        sender.sendMessage(ChatColor.GRAY + "/showppg info " + ChatColor.WHITE + "- نمایش اطلاعات مهم سرور");
        sender.sendMessage(ChatColor.GRAY + "/showppg help " + ChatColor.WHITE + "- نمایش همین راهنما");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "این پلاگین توسط Artin ساخته شده است.");
    }

    // ---------------- TAB COMPLETE ----------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 &&
                (args[0].equalsIgnoreCase("kick") ||
                 args[0].equalsIgnoreCase("ban") ||
                 args[0].equalsIgnoreCase("op"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("op")) {
            return Arrays.asList("true", "false").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
