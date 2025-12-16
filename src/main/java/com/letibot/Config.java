package com.letibot;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    private static final Map<Long, String> userGroups = new HashMap<>();
    private static final String USER_DATA_FILE = "user_groups.properties";

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Файл config.properties не найден!");
                System.exit(1);
            }
            properties.load(input);

            // Заменяем токен из переменной окружения, если он есть
            String botToken = System.getenv("BOT_TOKEN");
            if (botToken != null && !botToken.trim().isEmpty()) {
                properties.setProperty("telegram.bot.token", botToken);
            }

            // Загружаем сохраненные группы пользователей
            loadUserGroups();

        } catch (FileNotFoundException e) {
            System.err.println("Файл конфигурации не найден: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Ошибка чтения конфигурации: " + e.getMessage());
            System.exit(1);
        } catch (SecurityException e) {
            System.err.println("Нет прав доступа к файлу конфигурации: " + e.getMessage());
            System.exit(1);
        }
    }

    public static String getBotUsername() {
        return properties.getProperty("telegram.bot.username");
    }

    public static String getBotToken() {
        return properties.getProperty("telegram.bot.token");
    }

    public static String getApiUrl() {
        return properties.getProperty("leti.api.url");
    }

    public static void setUserGroup(long userId, String groupNumber) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Некорректный ID пользователя");
        }
        if (groupNumber == null || !groupNumber.matches("\\d{4}")) {
            throw new IllegalArgumentException("Некорректный номер группы: " + groupNumber);
        }

        userGroups.put(userId, groupNumber);
        saveUserGroups();
    }

    public static String getUserGroup(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Некорректный ID пользователя");
        }
        return userGroups.get(userId);
    }

    public static boolean hasUserGroup(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Некорректный ID пользователя");
        }
        return userGroups.containsKey(userId) && userGroups.get(userId) != null;
    }

    private static void loadUserGroups() {
        File file = new File(USER_DATA_FILE);
        if (!file.exists()) {
            System.out.println("Файл пользовательских групп не найден, создаем новый");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties userProps = new Properties();
            userProps.load(fis);

            for (String key : userProps.stringPropertyNames()) {
                try {
                    long userId = Long.parseLong(key);
                    String group = userProps.getProperty(key);
                    userGroups.put(userId, group);
                } catch (NumberFormatException e) {
                    System.err.println("Некорректный ключ групп: " + key);
                }
            }
            System.out.println("Загружено " + userGroups.size() + " пользовательских групп");
        } catch (FileNotFoundException e) {
            System.err.println("Файл пользовательских групп не найден: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Ошибка чтения пользовательских групп: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Нет прав доступа к файлу групп: " + e.getMessage());
        }
    }

    private static void saveUserGroups() {
        try (FileOutputStream fos = new FileOutputStream(USER_DATA_FILE)) {
            Properties userProps = new Properties();

            for (Map.Entry<Long, String> entry : userGroups.entrySet()) {
                userProps.setProperty(String.valueOf(entry.getKey()), entry.getValue());
            }

            userProps.store(fos, "User Groups Data");
            System.out.println("Сохранено " + userGroups.size() + " пользовательских групп");
        } catch (FileNotFoundException e) {
            System.err.println("Не удалось создать файл пользовательских групп: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения пользовательских групп: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Нет прав для сохранения файла групп: " + e.getMessage());
        }
    }
}