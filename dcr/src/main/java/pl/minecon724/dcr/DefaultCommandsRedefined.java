package pl.minecon724.dcr;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class DefaultCommandsRedefined extends JavaPlugin implements CommandExecutor {
	private File configYml = new File(getDataFolder(), "config.yml");
	FileConfiguration configCfg = YamlConfiguration.loadConfiguration(configYml);
	String noPermission;
	String missingArgs;
	String invalidArg;
	ChatColor plColorEnabled;
	ChatColor plColorDisabled;
	String plHoverStr;
	String plPrefix;
	String plSeparator;
	String plEnabledStr;
	String plDisabledStr;
	Boolean isBlacklist;
    List<String> displayList;
    String veFormat;
    String liPrefix;
    String liSeparator;
    ChatColor liPlayerColor;
	
	@Override
	public void onEnable() {
		Metrics metrics = new Metrics(this, 13260);
		if (!(configYml.exists())) {
			saveResource("config.yml", false);
			configCfg = YamlConfiguration.loadConfiguration(configYml);
		}
		ConfigurationSection formatSection = configCfg.getConfigurationSection("format");
		ConfigurationSection plFormatSection = configCfg.getConfigurationSection("format").getConfigurationSection("plugins");
		ConfigurationSection veFormatSection = configCfg.getConfigurationSection("format").getConfigurationSection("version");
		ConfigurationSection liFormatSection = configCfg.getConfigurationSection("format").getConfigurationSection("list");
		ConfigurationSection displaySection = configCfg.getConfigurationSection("display");
		noPermission = ChatColor.translateAlternateColorCodes('&', formatSection.getString("noPermission"));
		missingArgs = ChatColor.translateAlternateColorCodes('&', formatSection.getString("missingArgs"));
		invalidArg =  ChatColor.translateAlternateColorCodes('&', formatSection.getString("invalidArg"));
		plColorEnabled = ChatColor.valueOf(plFormatSection.getString("enabled"));
	    plColorDisabled = ChatColor.valueOf(plFormatSection.getString("disabled")); 
		plHoverStr = ChatColor.translateAlternateColorCodes('&', plFormatSection.getString("hover"));
		plPrefix = ChatColor.translateAlternateColorCodes('&', plFormatSection.getString("prefix"));
		plSeparator = ChatColor.translateAlternateColorCodes('&', plFormatSection.getString("separator"));
		plEnabledStr = ChatColor.translateAlternateColorCodes('&', plFormatSection.getString("enabledStr"));
		plDisabledStr = ChatColor.translateAlternateColorCodes('&', plFormatSection.getString("disabledStr"));
		isBlacklist = displaySection.getBoolean("blacklist");
		displayList = displaySection.getStringList("list");
		veFormat = ChatColor.translateAlternateColorCodes('&', veFormatSection.getString("format"));
		liPrefix = ChatColor.translateAlternateColorCodes('&', liFormatSection.getString("prefix"));
		liSeparator = ChatColor.translateAlternateColorCodes('&', liFormatSection.getString("separator"));
		liPlayerColor = ChatColor.valueOf(liFormatSection.getString("playerColor"));
		getLogger().info("Display mode: " + (isBlacklist ? "blacklist" : "whitelist"));
		getLogger().info("Display list: " + StringUtils.join(displayList, ", "));
		getCommand("dcr").setExecutor(this);
		metrics.addCustomChart(new Metrics.SimplePie("display_mode", () -> (displayList.size() >= 0 ? (isBlacklist ? "Blacklist" : "Whitelist") : "None")));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]) {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("plugins")) {
				if (!sender.hasPermission("dcr.plugins")) {
					sender.sendMessage(noPermission.replace("%perm%", "dcr.plugins"));
					return true;
				}
				List<Plugin> plugins = new LinkedList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
				if (isBlacklist) {
					plugins.clear();
					for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
						if (!displayList.contains(p.getName())) plugins.add(p);
					}
				} else if (!isBlacklist) {
					plugins.clear();
					for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
						if (displayList.contains(p.getName())) plugins.add(p);
					}
				}
				Collections.sort(plugins, new Comparator<Plugin>() {
				    public int compare(Plugin obj1, Plugin obj2) {
				        return obj1.getName().compareTo(obj2.getName());
				    }
			    });
				int c = 0;
				Integer loaded = 0;
				for (Plugin p : plugins) loaded += (p.isEnabled() ? 1 : 0);
				BaseComponent mesg = new TextComponent(plPrefix.replace("%count%", Integer.toString(plugins.size())).replace("%enabled%", loaded.toString()));
				for (Plugin p : plugins) {
				    TextComponent info = new TextComponent(p.getName());
				    info.setColor(p.isEnabled() ? plColorEnabled : plColorDisabled);
				    PluginDescriptionFile desc = p.getDescription();
				    String hover = plHoverStr.replace("%name%", desc.getName()).replace("%version%", desc.getVersion()).replace("%description%", (desc.getDescription() != null ? desc.getDescription() : "No description")).replace("%authors%", (desc.getAuthors().size() > 0 ? StringUtils.join(desc.getAuthors(), ", ") : "None")).replace("%website%", (desc.getWebsite() != null ? desc.getWebsite() : "Unknown")).replace("%status%", (p.isEnabled() ? plEnabledStr : plDisabledStr));
				    info.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
				    if (c < plugins.size() - 1) {
				    	info.addExtra(plSeparator);
				    }
				    mesg.addExtra(info);
				    c++;
				}
				sender.spigot().sendMessage(mesg);
			} else if (args[0].equalsIgnoreCase("version")) {
				if (!sender.hasPermission("dcr.version")) {
					sender.sendMessage(noPermission.replace("%perm%", "dcr.version"));
					return true;
				}
		        sender.sendMessage(veFormat.replace("%software%", Bukkit.getName()).replace("%version%", Bukkit.getVersion()).replace("%api%", Bukkit.getBukkitVersion()));
			} else if (args[0].equalsIgnoreCase("list")) {
				if (!sender.hasPermission("dcr.list")) {
					sender.sendMessage(noPermission.replace("%perm%", "dcr.list"));
					return true;
				}
				BaseComponent mesg = new TextComponent(liPrefix.replace("%online%", Integer.toString(Bukkit.getOnlinePlayers().size())).replace("%max%", Integer.toString(Bukkit.getMaxPlayers()))+"\n");
				int c = 0;
				for (Player p : Bukkit.getOnlinePlayers()) {
					TextComponent info = new TextComponent(p.getDisplayName());
					info.setColor(liPlayerColor);
					info.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new ComponentBuilder("{name:\"" + p.getName() + "\", type:\"Player\", id:\"" + p.getUniqueId() + "\"}").create()));
					info.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + p.getName() + " "));
					if (c < Bukkit.getOnlinePlayers().size() - 1) {
				    	info.addExtra(liSeparator);
				    }
					mesg.addExtra(info);
					c++;
				}
				sender.spigot().sendMessage(mesg);
			} else {
				sender.sendMessage(invalidArg);
			}
		} else {
			sender.sendMessage(missingArgs);
		}
		return true;
    }
}
