package com.letibot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public class LetiScheduleBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return Config.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return Config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update);
            }
        } catch (TelegramApiException e) {
            System.err.println("Telegram API: " + e.getMessage());
        }
    }

    private void handleMessage(Update update) throws TelegramApiException {
        String messageText = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        try {
            if (messageText.equals("/start") || messageText.equals("/help") || messageText.equals("Назад в меню")) {
                sendWelcomeMessage(chatId, userId);
            } else if (messageText.equals("Сегодня")) {
                handleToday(chatId, userId);
            } else if (messageText.equals("Завтра")) {
                handleTomorrow(chatId, userId);
            } else if (messageText.equals("Неделя")) {
                handleWeek(chatId, userId);
            } else if (messageText.equals("Ближайшее")) {
                handleNear(chatId, userId);
            } else if (messageText.equals("Настройка группы")) {
                handleSettings(chatId, userId);
            } else if (messageText.equals("Понедельник")) {
                handleDay(chatId, userId, "monday");
            } else if (messageText.equals("Вторник")) {
                handleDay(chatId, userId, "tuesday");
            } else if (messageText.equals("Среда")) {
                handleDay(chatId, userId, "wednesday");
            } else if (messageText.equals("Четверг")) {
                handleDay(chatId, userId, "thursday");
            } else if (messageText.equals("Пятница")) {
                handleDay(chatId, userId, "friday");
            } else if (messageText.equals("Суббота")) {
                handleDay(chatId, userId, "saturday");
            } else if (messageText.equals("Ввести свою группу")) {
                requestGroupInput(chatId);
            } else if (messageText.matches("\\d{2,5}")) {
            //  любое число от 2 до 5 цифр
            if (messageText.matches("\\d{4}")) {
                handleSetGroup(chatId, userId, messageText);
            } else {
                sendMessageWithKeyboard(chatId,
                        "Некорректный номер группы.\nНомер группы должен состоять из 4 цифр.\n\n" +
                                "Пожалуйста, введите правильный номер группы:",
                        KeyboardManager.getBackKeyboard());
            }
        } else {
            sendMessageWithKeyboard(chatId, "Используйте кнопки меню", KeyboardManager.getMainKeyboard());
        }
//            } else if (messageText.matches("\\d{4}")) {
//                handleSetGroup(chatId, userId, messageText);
//            } else {
//                sendMessageWithKeyboard(chatId, "Используйте кнопки меню", KeyboardManager.getMainKeyboard());
//            }
        } catch (ScheduleException e) {
            sendMessageWithKeyboard(chatId, "Ошибка: " + e.getMessage(), KeyboardManager.getMainKeyboard());
        } catch (IOException e) {
            sendMessageWithKeyboard(chatId, "Ошибка подключения к серверу", KeyboardManager.getMainKeyboard());
        } catch (TelegramApiException e) {
            throw e;
        }
    }



//    private void sendWelcomeMessage(long chatId, long userId) throws TelegramApiException {
//        String userGroup = Config.getUserGroup(userId);
//        String groupInfo = userGroup != null ? "\n*Группа:* " + userGroup : "\n*Группа не установлена*";
//
//        String welcomeText = "*Бот расписания ЛЭТИ*\n\n" + groupInfo;
//        sendMessageWithKeyboard(chatId, welcomeText, KeyboardManager.getMainKeyboard());
//    }
private void sendWelcomeMessage(long chatId, long userId) throws TelegramApiException {
    String userGroup = Config.getUserGroup(userId);

    if (userGroup != null) {
        String welcomeText = "*Бот расписания ЛЭТИ*\n\n" +
                "*Ваша группа:* " + userGroup + "\n\n" +
                "Используйте кнопки ниже для получения расписания";
        sendMessageWithKeyboard(chatId, welcomeText, KeyboardManager.getMainKeyboard());
    } else {
        String welcomeText = "*Бот расписания ЛЭТИ*\n\n" +
                "Добро пожаловать! Этот бот поможет вам получить расписание занятий:)\n\n" +
                "*Введите номер своей группы* (4 цифры, например: 4354)\n\n" +
                "Или выберите группу из списка в меню 'Настройка группы'";
        sendMessageWithKeyboard(chatId, welcomeText, KeyboardManager.getMainKeyboard());
    }
}


    private void handleToday(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithKeyboard(chatId, "Сначала установите группу", KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String today = ScheduleFetcher.getTodayDayName();
        String schedule = ScheduleFetcher.parseScheduleForDay(json, today, userGroup);
        sendMessageWithKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleTomorrow(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithKeyboard(chatId, "Сначала установите группу", KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String tomorrow = ScheduleFetcher.getTomorrowDayName();
        String schedule = ScheduleFetcher.parseScheduleForDay(json, tomorrow, userGroup);
        sendMessageWithKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleWeek(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithKeyboard(chatId, "Сначала установите группу", KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String schedule = ScheduleFetcher.getWeekSchedule(json, userGroup);
        sendMessageWithKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleNear(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithKeyboard(chatId, "Сначала установите группу", KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String nearest = ScheduleFetcher.findNearestLesson(json, userGroup);
        sendMessageWithKeyboard(chatId, nearest, KeyboardManager.getMainKeyboard());
    }

    private void handleDay(long chatId, long userId, String day) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithKeyboard(chatId, "Сначала установите группу", KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String schedule = ScheduleFetcher.parseScheduleForDay(json, day, userGroup);
        sendMessageWithKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleSettings(long chatId, long userId) throws TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        String currentGroupInfo = userGroup != null ? "Текущая группа: *" + userGroup + "*\n\n" : "";

        String settingsText = "*Настройка группы*\n\n" + currentGroupInfo +
                "Выберите группу или введите свою";
        sendMessageWithKeyboard(chatId, settingsText, KeyboardManager.getGroupSetupKeyboard());
    }

    private void handleSetGroup(long chatId, long userId, String groupNumber) throws TelegramApiException {
        if (groupNumber.matches("\\d{4}")) {
            Config.setUserGroup(userId, groupNumber);
            sendMessageWithKeyboard(chatId, "Группа установлена: *" + groupNumber + "*",
                    KeyboardManager.getMainKeyboard());
        } else {
            sendMessageWithKeyboard(chatId,
                    "Некорректный номер группы.\nНомер группы должен состоять из 4 цифр.\n\n" +
                            "Пожалуйста, введите номер группы еще раз:",
                    KeyboardManager.getBackKeyboard());
        }
    }

    private void requestGroupInput(long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите номер группы (4 цифры):");
        execute(message);
    }

    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboard);
        execute(message);
    }
}