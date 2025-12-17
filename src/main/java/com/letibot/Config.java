package com.letibot;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    private static final Map<Long, String> userGroups = new HashMap<>();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл config.properties не найден");
            }
            properties.load(input);

            String botToken = System.getenv("BOT_TOKEN");
            if (botToken != null && !botToken.trim().isEmpty()) {
                properties.setProperty("telegram.bot.token", botToken);
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения конфигурации", e);
        }
    }

    public static String getBotUsername() {
        return properties.getProperty("telegram.bot.username");
    }

    public static String getBotToken() {
        return properties.getProperty("telegram.bot.token");
    }

    public static void setUserGroup(long userId, String groupNumber) {
        if (groupNumber.matches("\\d{4}")) {
            userGroups.put(userId, groupNumber);
        }
    }

    public static String getUserGroup(long userId) {
        return userGroups.get(userId);
    }
}