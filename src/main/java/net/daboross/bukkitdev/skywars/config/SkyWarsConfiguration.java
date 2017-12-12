/*
 * Copyright (C) 2013-2016 Dabo Ross <http://www.daboross.net/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.daboross.bukkitdev.skywars.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.daboross.bukkitdev.bukkitstorageprotobuf.ProtobufStatic;
import net.daboross.bukkitdev.skywars.api.SkyStatic;
import net.daboross.bukkitdev.skywars.api.SkyWars;
import net.daboross.bukkitdev.skywars.api.arenaconfig.SkyArenaConfig;
import net.daboross.bukkitdev.skywars.api.config.ConfigColorCode;
import net.daboross.bukkitdev.skywars.api.config.SkyConfiguration;
import net.daboross.bukkitdev.skywars.api.config.SkyConfigurationException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class SkyWarsConfiguration implements SkyConfiguration {

    private final SkyArenaConfigLoader arenaLoader = new SkyArenaConfigLoader();
    private List<SkyArenaConfig> enabledArenas;
    private Map<String, String> arenaGamerules;
    private final SkyWars plugin;
    private Path arenaFolder;
    private boolean debug;
    private boolean skipUuidCheck;
    private boolean reportPluginStatistics;
    private ArenaOrder arenaOrder;
    private String messagePrefix;
    private boolean inventorySaveEnabled;
    private boolean experienceSaveEnabled;
    private boolean pghSaveEnabled;
    private boolean enableScore;
    private int deathScoreDiff;
    private int winScoreDiff;
    private int killScoreDiff;
    private boolean scoreUseSql;
    private String scoreSqlHost;
    private int scoreSqlPort;
    private String scoreSqlDatabase;
    private String scoreSqlUsername;
    private String scoreSqlPassword;
    private long scoreSaveInterval;
    private long scoreIndividualRankUpdateInterval;
    private int arenaDistanceApart;
    private boolean commandWhitelistEnabled;
    private boolean commandWhitelistABlacklist;
    private Pattern commandWhitelistCommandRegex;
    private boolean economyEnabled;
    private int economyWinReward;
    private int economyKillReward;
    private String locale;
    private boolean respawnPlayersImmediately;
    private boolean economyRewardMessages;
    private String[] joinSignLines;
    private boolean enableMultipleQueues;
    private Map<String, List<SkyArenaConfig>> arenasPerQueue;
    private boolean limitStartMessagesToArenaPlayers;
    private boolean limitDeathMessagesToArenaPlayers;
    private boolean limitEndMessagesToArenaPlayers;
    private boolean limitStartTimerMessagesToArenaPlayers;
    private boolean showUnavailableKitsInGui;
    private boolean replaceKitCommandWithGui;
    private boolean showKitGuiOnJoin;
    private long timeTillStartAfterMaxPlayers;
    private long timeTillStartAfterMinPlayers;
    private long timeBeforeGameStartsToCopyArena;
    private long inGamePlayerFreezeTime;
    private List<Long> startTimerMessageTimes;
    private boolean multiverseCoreHookEnabled;
    private boolean worldeditHookEnabled;
    private boolean multiinvWorkaroundPossible;
    private boolean multiinvWorkaroundForced;
    private boolean disableReport;
    private boolean recoverFromScoreErrors;
    private boolean developerOptions;
    private int arenaCopyingBlockSize;

    public SkyWarsConfiguration(SkyWars plugin) throws IOException, InvalidConfigurationException, SkyConfigurationException {
        this.plugin = plugin;
        load();
    }

    private void load() throws IOException, InvalidConfigurationException, SkyConfigurationException {
        // This is expected to be only done once, so it is OK to not store some values
        Path mainConfigFile = plugin.getDataFolder().toPath().resolve(Names.MAIN);
        SkyFileConfig mainConfig = new SkyFileConfig(mainConfigFile, plugin.getLogger());
        mainConfig.load();

        if (arenaFolder == null) {
            arenaFolder = plugin.getDataFolder().toPath().resolve(Names.ARENAS);
        }
        if (!Files.exists(arenaFolder)) {
            Files.createDirectories(arenaFolder);
        } else if (!Files.isDirectory(arenaFolder)) {
            throw new SkyConfigurationException("File " + arenaFolder.toAbsolutePath() + " exists but is not a directory");
        }

        int version = mainConfig.getSetInt(MainConfigKeys.VERSION, MainConfigDefaults.VERSION);
        if (version > 2) {
            throw new SkyConfigurationException("Version '" + version + "' as listed under " + MainConfigKeys.VERSION + " in file " + mainConfigFile.toAbsolutePath() + " is unknown.");
        }
        mainConfig.getConfig().set(MainConfigKeys.VERSION, MainConfigDefaults.VERSION);

        debug = mainConfig.getSetBoolean(MainConfigKeys.DEBUG, MainConfigDefaults.DEBUG);
        SkyStatic.setDebug(debug);
        ProtobufStatic.setDebug(debug);

        reportPluginStatistics = mainConfig.getSetBoolean(MainConfigKeys.REPORT_STATISTICS, MainConfigDefaults.REPORT_STATISTICS);

        skipUuidCheck = mainConfig.getConfig().getBoolean(MainConfigKeys.SKIP_UUID_CHECK, MainConfigDefaults.SKIP_UUID_CHECK);
        String arenaOrderString = mainConfig.getSetString(MainConfigKeys.ARENA_ORDER, MainConfigDefaults.ARENA_ORDER.toString());
        arenaOrder = ArenaOrder.getOrder(arenaOrderString);
        if (arenaOrder == null) {
            throw new SkyConfigurationException("Invalid ArenaOrder '" + arenaOrderString + "' found under " + MainConfigKeys.ARENA_ORDER + " in file " + mainConfigFile.toAbsolutePath() + ". Valid values: " + Arrays.toString(ArenaOrder.values()));
        }

        messagePrefix = ConfigColorCode.translateCodes(mainConfig.getSetString(MainConfigKeys.MESSAGE_PREFIX, MainConfigDefaults.MESSAGE_PREFIX));
        mainConfig.setStringIfNot(MainConfigKeys.MESSAGE_PREFIX, messagePrefix);
        inventorySaveEnabled = mainConfig.getSetBoolean(MainConfigKeys.SAVE_INVENTORY, MainConfigDefaults.SAVE_INVENTORY);
        experienceSaveEnabled = mainConfig.getSetBoolean(MainConfigKeys.SAVE_EXPERIENCE, inventorySaveEnabled);
        pghSaveEnabled = mainConfig.getSetBoolean(MainConfigKeys.SAVE_POSITION_GAMEMODE_HEALTH, inventorySaveEnabled);
        if ((pghSaveEnabled || experienceSaveEnabled) && !inventorySaveEnabled) {
            throw new SkyConfigurationException("Inventory saving must be enabled to enable experience saving or position-health-gamemode saving!");
        }

        enableMultipleQueues = mainConfig.getSetBoolean(MainConfigKeys.ENABLE_MULTIPLE_QUEUES, MainConfigDefaults.ENABLE_MULTIPLE_QUEUES);

        Map<String, List<String>> queueDescriptions;
        if (enableMultipleQueues) {
            queueDescriptions = mainConfig.getSetStringListMap(MainConfigKeys.QUEUE_DESCRIPTIONS, MainConfigDefaults.QUEUE_DESCRIPTIONS);
            if (queueDescriptions.size() <= 0) {
                throw new SkyConfigurationException("Multiple queues enabled, yet queue-descriptions is empty.");
            }
        } else {
            List<String> enabledArenaNames = mainConfig.getSetStringList(MainConfigKeys.ENABLED_ARENAS, MainConfigDefaults.ENABLED_ARENAS);
            queueDescriptions = Collections.singletonMap(null, enabledArenaNames);
            if (enabledArenaNames.isEmpty()) {
                throw new SkyConfigurationException("No arenas enabled");
            }
            mainConfig.getSetStringListMap(MainConfigKeys.QUEUE_DESCRIPTIONS, MainConfigDefaults.QUEUE_DESCRIPTIONS);
        }
        enabledArenas = new ArrayList<>();

        locale = mainConfig.getSetString(MainConfigKeys.LOCALE, MainConfigDefaults.LOCALE);

        arenaGamerules = Collections.unmodifiableMap(mainConfig.getSetStringMap(MainConfigKeys.ARENA_GAMERULES, MainConfigDefaults.ARENA_GAMERULES));

        respawnPlayersImmediately = mainConfig.getSetBoolean(MainConfigKeys.RESPAWN_PLAYERS_IMMEDIATELY, MainConfigDefaults.RESPAWN_PLAYERS_IMMEDIATELY);

        // Score
        enableScore = mainConfig.getSetBoolean(MainConfigKeys.Score.ENABLE, MainConfigDefaults.Score.ENABLE);
        winScoreDiff = mainConfig.getSetInt(MainConfigKeys.Score.WIN_DIFF, MainConfigDefaults.Score.WIN_DIFF);
        deathScoreDiff = mainConfig.getSetInt(MainConfigKeys.Score.DEATH_DIFF, MainConfigDefaults.Score.DEATH_DIFF);
        killScoreDiff = mainConfig.getSetInt(MainConfigKeys.Score.KILL_DIFF, MainConfigDefaults.Score.KILL_DIFF);
        scoreSaveInterval = mainConfig.getSetLong(MainConfigKeys.Score.SAVE_INTERVAL, MainConfigDefaults.Score.SAVE_INTERVAL);
        // Score.SQL
        scoreUseSql = mainConfig.getSetBoolean(MainConfigKeys.Score.USE_SQL, MainConfigDefaults.Score.USE_SQL);
        scoreSqlHost = mainConfig.getSetString(MainConfigKeys.Score.SQL_HOST, MainConfigDefaults.Score.SQL_HOST);
        scoreSqlPort = mainConfig.getSetInt(MainConfigKeys.Score.SQL_PORT, MainConfigDefaults.Score.SQL_PORT);
        scoreSqlDatabase = mainConfig.getSetString(MainConfigKeys.Score.SQL_DATABASE, MainConfigDefaults.Score.SQL_DATABASE);
        scoreSqlUsername = mainConfig.getSetString(MainConfigKeys.Score.SQL_USERNAME, MainConfigDefaults.Score.SQL_USERNAME);
        scoreSqlPassword = mainConfig.getSetString(MainConfigKeys.Score.SQL_PASSWORD, MainConfigDefaults.Score.SQL_PASSWORD);
        scoreIndividualRankUpdateInterval = mainConfig.getSetLong(MainConfigKeys.Score.SQL_UPDATE_INDIVIDUALS_RANK_INTERVAL, MainConfigDefaults.Score.SQL_UPDATE_INDIVIDUALS_RANK_INTERVAL);

        // Ensure the user has adjusted save interval to a sensible value when adjusting sql use.
        // If this was done on purpose, it just needs to be set to a non-default value (301/31 works)
        if (scoreSaveInterval == MainConfigDefaults.Score.SAVE_INTERVAL && scoreUseSql) {
            mainConfig.overwriteValue(MainConfigKeys.Score.SAVE_INTERVAL, MainConfigDefaults.Score.SAVE_INTERVAL_WITH_SQL);
        } else if (scoreSaveInterval == MainConfigDefaults.Score.SAVE_INTERVAL_WITH_SQL && !scoreUseSql) {
            mainConfig.overwriteValue(MainConfigKeys.Score.SAVE_INTERVAL, MainConfigDefaults.Score.SAVE_INTERVAL);
        }
        // Economy
        economyEnabled = mainConfig.getSetBoolean(MainConfigKeys.Economy.ENABLE, MainConfigDefaults.Economy.ENABLE);
        economyKillReward = mainConfig.getSetInt(MainConfigKeys.Economy.KILL_REWARD, MainConfigDefaults.Economy.KILL_REWARD);
        economyWinReward = mainConfig.getSetInt(MainConfigKeys.Economy.WIN_REWARD, MainConfigDefaults.Economy.WIN_REWARD);
        economyRewardMessages = mainConfig.getSetBoolean(MainConfigKeys.Economy.MESSAGE, MainConfigDefaults.Economy.MESSAGE);

        arenaDistanceApart = mainConfig.getSetInt(MainConfigKeys.ARENA_DISTANCE_APART, MainConfigDefaults.ARENA_DISTANCE_APART);
        arenaCopyingBlockSize = mainConfig.getSetInt(MainConfigKeys.ARENA_COPYING_BLOCK_SIZE, MainConfigDefaults.ARENA_COPYING_BLOCK_SIZE);

        commandWhitelistEnabled = mainConfig.getSetBoolean(MainConfigKeys.CommandWhitelist.WHITELIST_ENABLED, MainConfigDefaults.CommandWhitelist.WHITELIST_ENABLED);
        commandWhitelistABlacklist = mainConfig.getSetBoolean(MainConfigKeys.CommandWhitelist.IS_BLACKLIST, MainConfigDefaults.CommandWhitelist.IS_BLACKLIST);
        commandWhitelistCommandRegex = createCommandRegex(mainConfig.getSetStringList(MainConfigKeys.CommandWhitelist.COMMAND_WHITELIST, MainConfigDefaults.CommandWhitelist.COMMAND_WHITELIST));

        joinSignLines = mainConfig.getSetFixedArray(MainConfigKeys.JOIN_SIGN_LINES, MainConfigDefaults.JOIN_SIGN_LINES);

        // per-arena messages
        limitStartMessagesToArenaPlayers = mainConfig.getSetBoolean(MainConfigKeys.LIMIT_START_MESSAGES_TO_ARENA, MainConfigDefaults.LIMIT_START_MESSAGES_TO_ARENA);
        limitDeathMessagesToArenaPlayers = mainConfig.getSetBoolean(MainConfigKeys.LIMIT_DEATH_MESSAGES_TO_ARENA, MainConfigDefaults.LIMIT_DEATH_MESSAGES_TO_ARENA);
        limitEndMessagesToArenaPlayers = mainConfig.getSetBoolean(MainConfigKeys.LIMIT_END_MESSAGES_TO_ARENA, MainConfigDefaults.LIMIT_END_MESSAGES_TO_ARENA);
        limitStartTimerMessagesToArenaPlayers = mainConfig.getSetBoolean(MainConfigKeys.LIMIT_START_TIMER_MESSAGES_TO_ARENA, MainConfigDefaults.LIMIT_START_TIMER_MESSAGES_TO_ARENA);

        // Kit GUI
        showUnavailableKitsInGui = mainConfig.getSetBoolean(MainConfigKeys.KIT_GUI_SHOW_UNAVAILABLE_KITS, MainConfigDefaults.KIT_GUI_SHOW_UNAVAILABLE_KITS);
        replaceKitCommandWithGui = mainConfig.getSetBoolean(MainConfigKeys.KIT_GUI_REPLACE_KIT_COMMAND, MainConfigDefaults.KIT_GUI_REPLACE_KIT_COMMAND);
        showKitGuiOnJoin = mainConfig.getSetBoolean(MainConfigKeys.KIT_GUI_AUTO_SHOW_ON_JOIN, MainConfigDefaults.KIT_GUI_AUTO_SHOW_ON_JOIN);

        // Start timer
        timeTillStartAfterMaxPlayers = mainConfig.getSetLong(MainConfigKeys.TIME_TILL_START_AFTER_MAX_PLAYERS, MainConfigDefaults.TIME_TILL_START_AFTER_MAX_PLAYERS);
        timeTillStartAfterMinPlayers = mainConfig.getSetLong(MainConfigKeys.TIME_TILL_START_AFTER_MIN_PLAYERS, MainConfigDefaults.TIME_TILL_START_AFTER_MIN_PLAYERS);
        timeBeforeGameStartsToCopyArena = mainConfig.getSetLong(MainConfigKeys.TIME_BEFORE_GAME_STARTS_TO_COPY_ARENA, MainConfigDefaults.TIME_BEFORE_GAME_STARTS_TO_COPY_ARENA);
        inGamePlayerFreezeTime = mainConfig.getSetLong(MainConfigKeys.IN_GAME_PLAYER_FREEZE_TIME, MainConfigDefaults.IN_GAME_PLAYER_FREEZE_TIME);
        startTimerMessageTimes = mainConfig.getSetLongList(MainConfigKeys.START_TIMER_MESSAGE_TIMES, MainConfigDefaults.START_TIMER_MESSAGE_TIMES);

        // Report disable
        disableReport = mainConfig.getConfig().getBoolean(MainConfigKeys.DISABLE_REPORT, MainConfigDefaults.DISABLE_REPORT);
        recoverFromScoreErrors = !mainConfig.getConfig().getBoolean(MainConfigKeys.DISABLE_SCORE_RECOVERY, MainConfigDefaults.DISABLE_SCORE_RECOVERY);

        // Hooks
        multiverseCoreHookEnabled = mainConfig.getSetBoolean(MainConfigKeys.Hooks.MULTIVERSE_CORE, MainConfigDefaults.Hooks.MULTIVERSE_CORE);
        worldeditHookEnabled = mainConfig.getSetBoolean(MainConfigKeys.Hooks.WORLDEDIT, MainConfigDefaults.Hooks.WORLDEDIT);

        multiinvWorkaroundPossible = mainConfig.getSetBoolean(MainConfigKeys.Hooks.MULTIINV_WORKAROUND, MainConfigDefaults.Hooks.MULTIINV_WORKAROUND_WHEN_FOUND);
        multiinvWorkaroundForced = mainConfig.getSetBoolean(MainConfigKeys.Hooks.FORCE_MULTIINV_WORKAROUND, MainConfigDefaults.Hooks.FORCE_MULTIINV_WORKAROUND);

        // Developer options
        developerOptions = mainConfig.getConfig().getBoolean(MainConfigKeys.DEVELOPER_OPTIONS, MainConfigDefaults.DEVELOPER_OPTIONS);
        if (developerOptions) {
            plugin.getLogger().info("Enabling developer options.");
        }

        // Remove deprecated values
        mainConfig.removeValues(MainConfigKeys.Deprecated.CHAT_PREFIX, MainConfigKeys.Deprecated.PREFIX_CHAT);

        // Save
        mainConfig.save(String.format(Headers.CONFIG));

        // Arenas
        arenasPerQueue = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : queueDescriptions.entrySet()) {
            List<SkyArenaConfig> arenas = new ArrayList<>(entry.getValue().size());
            for (String arenaName : entry.getValue()) {
                arenas.add(loadArena(arenaName));
            }
            arenasPerQueue.put(entry.getKey(), arenas);
        }
    }

    private Pattern createCommandRegex(List<String> commands) {
        if (commands.isEmpty()) {
            return null;
        } else {
            StringBuilder b = new StringBuilder("(?i)^(" + Matcher.quoteReplacement(commands.get(0)));
            for (int i = 1; i < commands.size(); i++) {
                b.append("|").append(Matcher.quoteReplacement(commands.get(i)));
            }
            b.append(")( .*|$)");
            return Pattern.compile(b.toString());
        }
    }

    @Override
    public void reload() throws IOException, InvalidConfigurationException, SkyConfigurationException {
        load();
    }

    private SkyArenaConfig loadArena(String name) throws SkyConfigurationException {
        if (enabledArenas == null) {
            throw new IllegalStateException("Enabled arenas null");
        }
        Path file = arenaFolder.resolve(name + ".yml");
        if (!Files.exists(file)) {
            String fileName = Paths.get(Names.ARENAS, name + ".yml").toString();
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException ex) {
                throw new SkyConfigurationException(name + " is in " + MainConfigKeys.ENABLED_ARENAS + " but file " + file.toAbsolutePath() + " could not be found.");
            }
        }
        SkyArenaConfig arenaConfig = arenaLoader.loadArena(file, name);
        enabledArenas.add(arenaConfig);

        saveArena(file, arenaConfig, String.format(Headers.ARENA, name));
        return arenaConfig;
    }

    public void saveArena(Path path, SkyArenaConfig arenaConfig, String header) {
        YamlConfiguration newConfig = new YamlConfiguration();
        newConfig.options().header(header).indent(2);
        arenaConfig.serialize(newConfig);
        try {
            newConfig.save(path.toFile());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arena config to file " + path.toAbsolutePath(), ex);
        }
    }

    @Override
    public void saveArena(SkyArenaConfig arena) {
        saveArena(arena.getFile(), arena, String.format(Headers.ARENA, arena.getArenaName()));
    }

    @Override
    public boolean shouldLimitStartMessagesToArenaPlayers() {
        return limitStartMessagesToArenaPlayers;
    }

    @Override
    public boolean shouldLimitDeathMessagesToArenaPlayers() {
        return limitDeathMessagesToArenaPlayers;
    }

    @Override
    public boolean shouldLimitEndMessagesToArenaPlayers() {
        return limitEndMessagesToArenaPlayers;
    }

    @Override
    public boolean shouldLimitStartTimerMessagesToArenaPlayers() {
        return limitStartTimerMessagesToArenaPlayers;
    }

    @Override
    public List<SkyArenaConfig> getEnabledArenas() {
        return Collections.unmodifiableList(enabledArenas);
    }

    @Override
    public boolean areMultipleQueuesEnabled() {
        return enableMultipleQueues;
    }

    @Override
    public Set<String> getQueueNames() {
        return Collections.unmodifiableSet(arenasPerQueue.keySet());
    }

    @Override
    public List<SkyArenaConfig> getArenasForQueue(final String queueName) {
        List<SkyArenaConfig> list = arenasPerQueue.get(queueName);
        if (list != null) {
            return Collections.unmodifiableList(list);
        } else {
            return null;
        }
    }

    @Override
    public boolean areEconomyRewardMessagesEnabled() {
        return economyRewardMessages;
    }

    @Override
    public Path getArenaFolder() {
        return arenaFolder;
    }

    @Override
    public ArenaOrder getArenaOrder() {
        return arenaOrder;
    }

    @Override
    public String getMessagePrefix() {
        return messagePrefix;
    }

    @Override
    public boolean isInventorySaveEnabled() {
        return inventorySaveEnabled;
    }

    @Override
    public boolean isExperienceSaveEnabled() {
        return experienceSaveEnabled;
    }

    @Override
    public boolean isPghSaveEnabled() {
        return pghSaveEnabled;
    }

    @Override
    public boolean isEnableScore() {
        return enableScore;
    }

    @Override
    public Map<String, String> getArenaGamerules() {
        return arenaGamerules;
    }

    @Override
    public int getDeathScoreDiff() {
        return deathScoreDiff;
    }

    @Override
    public int getWinScoreDiff() {
        return winScoreDiff;
    }

    @Override
    public int getKillScoreDiff() {
        return killScoreDiff;
    }

    @Override
    public long getScoreSaveInterval() {
        return scoreSaveInterval;
    }

    @Override
    public int getArenaDistanceApart() {
        return arenaDistanceApart;
    }

    @Override
    public boolean isCommandWhitelistEnabled() {
        return commandWhitelistEnabled;
    }

    @Override
    public boolean isCommandWhitelistABlacklist() {
        return commandWhitelistABlacklist;
    }

    @Override
    public Pattern getCommandWhitelistCommandRegex() {
        return commandWhitelistCommandRegex;
    }

    @Override
    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    @Override
    public int getEconomyWinReward() {
        return economyWinReward;
    }

    @Override
    public int getEconomyKillReward() {
        return economyKillReward;
    }

    @Override
    public String getLocale() {
        return locale;
    }

    @Override
    public boolean isDisableReport() {
        return disableReport;
    }

    @Override
    public boolean isScoreUseSql() {
        return scoreUseSql;
    }

    @Override
    public String getScoreSqlHost() {
        return scoreSqlHost;
    }

    @Override
    public int getScoreSqlPort() {
        return scoreSqlPort;
    }

    @Override
    public String getScoreSqlUsername() {
        return scoreSqlUsername;
    }

    @Override
    public String getScoreSqlPassword() {
        return scoreSqlPassword;
    }

    @Override
    public String getScoreSqlDatabase() {
        return scoreSqlDatabase;
    }

    @Override
    public long getScoreIndividualRankUpdateInterval() {
        return scoreIndividualRankUpdateInterval;
    }

    @Override
    public boolean isMultiverseCoreHookEnabled() {
        return multiverseCoreHookEnabled;
    }

    @Override
    public boolean isMultiinvWorkaroundPossible() {
        return multiinvWorkaroundPossible;
    }

    @Override
    public boolean isMultiinvWorkaroundForced() {
        return multiinvWorkaroundForced;
    }

    @Override
    public boolean isWorldeditHookEnabled() {
        return worldeditHookEnabled;
    }

    @Override
    public boolean isSkipUuidCheck() {
        return skipUuidCheck;
    }

    @Override
    public boolean areDeveloperOptionsEnabled() {
        return developerOptions;
    }

    @Override
    public boolean isRecoverFromScoreErrors() {
        return recoverFromScoreErrors;
    }

    @Override
    public boolean isRespawnPlayersImmediately() {
        return respawnPlayersImmediately;
    }

    @Override
    public String[] getJoinSignLines() {
        return joinSignLines;
    }

    @Override
    public boolean isShowUnavailableKitsInGui() {
        return showUnavailableKitsInGui;
    }

    @Override
    public boolean isReplaceKitCommandWithGui() {
        return replaceKitCommandWithGui;
    }

    @Override
    public boolean isShowKitGuiOnJoin() {
        return showKitGuiOnJoin;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isReportPluginStatistics() {
        return reportPluginStatistics;
    }

    @Override
    public long getTimeTillStartAfterMaxPlayers() {
        return timeTillStartAfterMaxPlayers;
    }

    @Override
    public long getTimeTillStartAfterMinPlayers() {
        return timeTillStartAfterMinPlayers;
    }

    @Override
    public long getTimeBeforeGameStartToCopyArena() {
        return timeBeforeGameStartsToCopyArena;
    }

    @Override
    public long getInGamePlayerFreezeTime() {
        return inGamePlayerFreezeTime;
    }

    @Override
    public List<Long> getStartTimerMessageTimes() {
        return startTimerMessageTimes;
    }

    @Override
    public int getArenaCopyingBlockSize() {
        return arenaCopyingBlockSize;
    }

    private static class Names {

        private static final String MAIN = "main-config.yml";
        private static final String ARENAS = "arenas";

        private Names() {
        }
    }

    private static class Headers {

        private static final String CONFIG = "####### config.yml #######%n"
                + "%n"
                + "All comment changes will be removed.%n"
                + "%n"
                + "For documentation, please visit %n"
                + "https://dabo.guru/projects/skywars/configuring-skywars%n"
                + "#########";
        private static final String ARENA = "####### %s.yml ###%n"
                + "This is the Skyblock Warriors arena config.%n"
                + "%n"
                + "All values that are not in this configuration will be inherited from%n"
                + " arena-parent.yml%n"
                + "%n"
                + "All comment changes will be removed.%n"
                + "%n"
                + "For documentation, please visit %n"
                + "https://dabo.guru/projects/skywars/configuring-arenas%n"
                + "#######";

        private Headers() {
        }
    }
}
