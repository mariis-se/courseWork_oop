package com.letibot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonIOException;

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
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (TelegramApiException e) {
            System.err.println("Ошибка API: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Некорректный аргумент: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка в обработке: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMessage(Update update) throws TelegramApiException {
        String messageText = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        System.out.println("Получено (" + userId + "): " + messageText);

        try {
            if (messageText.equals("/start") || messageText.equals("/help") || messageText.equals("меню") || messageText.equals("Назад в меню")) {
                sendWelcomeMessage(chatId, userId);
            } else if (messageText.equals("Сегодня")) {
                handleToday(chatId, userId);
            } else if (messageText.equals("Завтра")) {
                handleTomorrow(chatId, userId);
            } else if (messageText.equals("Неделя")) {
                handleWeek(chatId, userId);
            } else if (messageText.equals("Ближайшее")) {
                handleNear(chatId, userId);
            } else if (messageText.equals("Моя группа")) {
                handleMyGroup(chatId, userId);
            } else if (messageText.equals("Настройки")) {
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
            } else if (messageText.equals("Другая...")) {
                requestGroupInput(chatId);
            } else if (messageText.equals("Назад")) {
                sendWelcomeMessage(chatId, userId);
            } else if (messageText.matches("\\d{4}")) {
                //  является ли сообщение номером группы
                handleSetGroup(chatId, userId, messageText);
            } else {
                sendMessageWithReplyKeyboard(chatId,
                        "Я не понимаю эту команду:(( Используйте кнопки меню или /help для справки.",
                        KeyboardManager.getMainKeyboard());
            }
        } catch (ScheduleException e) {
            sendMessageWithReplyKeyboard(chatId, "Ошибка расписания: " + e.getMessage(),
                    KeyboardManager.getMainKeyboard());
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                sendMessageWithReplyKeyboard(chatId, "Превышено время ожидания ответа от сервера ЛЭТИ.",
                        KeyboardManager.getMainKeyboard());
            } else if (e instanceof UnknownHostException) {
                sendMessageWithReplyKeyboard(chatId, "Не удалось найти сервер ЛЭТИ. Проверьте интернет-соединение.",
                        KeyboardManager.getMainKeyboard());
            } else {
                sendMessageWithReplyKeyboard(chatId, "Ошибка подключения к серверу ЛЭТИ. Попробуйте позже.",
                        KeyboardManager.getMainKeyboard());
            }
        } catch (JsonSyntaxException | JsonIOException e) {
            sendMessageWithReplyKeyboard(chatId, "Ошибка формата данных от сервера ЛЭТИ. Попробуйте позже.",
                    KeyboardManager.getMainKeyboard());
        } catch (DateTimeParseException e) {
            sendMessageWithReplyKeyboard(chatId, "Ошибка обработки времени. Данные повреждены.",
                    KeyboardManager.getMainKeyboard());
        } catch (NoSuchElementException | NullPointerException e) {
            sendMessageWithReplyKeyboard(chatId, "Ошибка обработки данных. Некоторые данные отсутствуют.",
                    KeyboardManager.getMainKeyboard());
        } catch (IllegalArgumentException e) {
            sendMessageWithReplyKeyboard(chatId, "Некорректный запрос: " + e.getMessage(),
                    KeyboardManager.getMainKeyboard());
        } catch (TelegramApiException e) {
            throw e; // Перебрасываем выше для обработки в onUpdateReceived
        } catch (Exception e) {
            sendMessageWithReplyKeyboard(chatId,
                    "Непредвиденная ошибка: " + e.getClass().getSimpleName(),
                    KeyboardManager.getMainKeyboard());
        }
    }

    private void handleCallbackQuery(Update update) throws TelegramApiException {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        long userId = update.getCallbackQuery().getFrom().getId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        System.out.println("Callback от user" + userId + ": " + callbackData);

        try {
            if (callbackData.startsWith("day_")) {
                String day = callbackData.substring(4);
                if (day.equals("today")) {
                    handleToday(chatId, userId);
                } else if (day.equals("tomorrow")) {
                    handleTomorrow(chatId, userId);
                } else {
                    handleDay(chatId, userId, day);
                }

                // Удаляем inline-клавиатуру
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(String.valueOf(chatId));
                editMessage.setMessageId(messageId);
                editMessage.setText("Выбран день: " + getRussianDayName(day));
                execute(editMessage);
            }
        } catch (ScheduleException e) {
            sendMessageWithInlineKeyboard(chatId, "Ошибка расписания: " + e.getMessage(),
                    KeyboardManager.getDaysInlineKeyboard());
        } catch (IOException e) {
            sendMessageWithInlineKeyboard(chatId, "Ошибка подключения к серверу ЛЭТИ. Попробуйте позже.",
                    KeyboardManager.getDaysInlineKeyboard());
        } catch (Exception e) {
            sendMessageWithInlineKeyboard(chatId,
                    "⚠Ошибка при обработке запроса: " + e.getMessage(),
                    KeyboardManager.getDaysInlineKeyboard());
        }
    }

    private void sendWelcomeMessage(long chatId, long userId) throws TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        String groupInfo = "";

        if (userGroup != null) {
            groupInfo = "\n *Ваша группа:* " + userGroup;
        } else {
            groupInfo = "\n *Группа не установлена*\n" +
                    "Нажмите '️ Настройки' для установки группы";
        }

        String welcomeText =
                " *Бот расписания ЛЭТИ*\n\n" +
                        " *Используйте кнопки ниже:*\n" +
                        groupInfo + "\n\n" +
                        " *Расписание:*\n" +
                        "•  Сегодня\n" +
                        "•  Завтра\n" +
                        "•  Неделя\n" +
                        "•  Ближайшее\n\n" +
                        " *Дни недели:*\n" +
                        "Понедельник - Суббота\n\n" +
                        " *Настройки:*\n" +
                        "•  Моя группа\n" +
                        "•  Настройки";

        sendMessageWithReplyKeyboard(chatId, welcomeText, KeyboardManager.getMainKeyboard());
    }

    private void handleToday(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithReplyKeyboard(chatId, " Сначала установите группу в настройках!",
                    KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String today = ScheduleFetcher.getTodayDayName();
        String schedule = ScheduleFetcher.parseScheduleForDay(json, today, userGroup);

        sendMessageWithReplyKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleTomorrow(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithReplyKeyboard(chatId, " Сначала установите группу в настройках!",
                    KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String tomorrow = ScheduleFetcher.getTomorrowDayName();
        String schedule = ScheduleFetcher.parseScheduleForDay(json, tomorrow, userGroup);

        sendMessageWithReplyKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleWeek(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithReplyKeyboard(chatId, " Сначала установите группу в настройках!",
                    KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String schedule = ScheduleFetcher.getWeekSchedule(json, userGroup);

        sendMessageWithReplyKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleNear(long chatId, long userId) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithReplyKeyboard(chatId, " Сначала установите группу в настройках!",
                    KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String nearest = ScheduleFetcher.findNearestLesson(json, userGroup);

        sendMessageWithReplyKeyboard(chatId, nearest, KeyboardManager.getMainKeyboard());
    }

    private void handleDay(long chatId, long userId, String day) throws ScheduleException, IOException, TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup == null) {
            sendMessageWithReplyKeyboard(chatId, " Сначала установите группу в настройках!",
                    KeyboardManager.getMainKeyboard());
            return;
        }

        String json = ScheduleFetcher.getScheduleForGroup(userGroup);
        String schedule = ScheduleFetcher.parseScheduleForDay(json, day, userGroup);

        sendMessageWithReplyKeyboard(chatId, schedule, KeyboardManager.getMainKeyboard());
    }

    private void handleMyGroup(long chatId, long userId) throws TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        if (userGroup != null) {
            sendMessageWithReplyKeyboard(chatId,
                    " *Ваша текущая группа:* " + userGroup +
                            "\n\nИзменить группу можно в настройках.",
                    KeyboardManager.getMainKeyboard());
        } else {
            sendMessageWithReplyKeyboard(chatId,
                    " *Группа не установлена*\n\n" +
                            "Нажмите '⚙️ Настройки' для установки группы.",
                    KeyboardManager.getMainKeyboard());
        }
    }

    private void handleSettings(long chatId, long userId) throws TelegramApiException {
        String userGroup = Config.getUserGroup(userId);
        String currentGroupInfo = userGroup != null ?
                "Текущая группа: *" + userGroup + "*\n\n" :
                "Группа не установлена\n\n";

        String settingsText = " *Настройки*\n\n" +
                currentGroupInfo +
                "Выберите действие:\n" +
                "• Выберите группу из списка ниже\n" +
                "• Или введите номер группы вручную\n" +
                "• ' Назад' - вернуться в меню";

        sendMessageWithReplyKeyboard(chatId, settingsText, KeyboardManager.getGroupSetupKeyboard());
    }

    private void handleSetGroup(long chatId, long userId, String groupNumber) throws TelegramApiException {
        if (groupNumber.matches("\\d{4}")) {
            Config.setUserGroup(userId, groupNumber);
            sendMessageWithReplyKeyboard(chatId,
                    " Группа установлена: *" + groupNumber + "*\n\n" +
                            "Теперь вы можете использовать все функции бота!",
                    KeyboardManager.getMainKeyboard());
        } else {
            sendMessageWithReplyKeyboard(chatId,
                    " Некорректный номер группы. Используйте формат: 4354",
                    KeyboardManager.getGroupSetupKeyboard());
        }
    }

    private void requestGroupInput(long chatId) throws TelegramApiException {
        sendMessageWithReplyKeyboard(chatId,
                "Введите номер группы вручную (например: 4354):",
                KeyboardManager.getBackKeyboard());
    }

    private String getRussianDayName(String englishDay) {
        switch (englishDay.toLowerCase()) {
            case "monday": return "Понедельник";
            case "tuesday": return "Вторник";
            case "wednesday": return "Среда";
            case "thursday": return "Четверг";
            case "friday": return "Пятница";
            case "saturday": return "Суббота";
            case "today": return "Сегодня";
            case "tomorrow": return "Завтра";
            default: return englishDay;
        }
    }

    // Разделенные методы для разных типов клавиатур
    private void sendMessageWithReplyKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        execute(message);
    }

    private void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        execute(message);
    }

    private void sendSimpleMessage(long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        execute(message);
    }
}