package com.letibot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class KeyboardManager {

    public static ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Сегодня");
        row1.add("Завтра");
        row1.add("Неделя");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Ближайшее");
        row2.add("Моя группа");
        row2.add("Настройки");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Понедельник");
        row3.add("Вторник");
        row3.add("Среда");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("Четверг");
        row4.add("Пятница");
        row4.add("Суббота");
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getGroupSetupKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("4351");
        row1.add("4352");
        row1.add("4353");
        row1.add("4354");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("4355");
        row2.add("4356");
        row2.add("Другая...");
        row2.add("Назад");
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // выбор дня недели
    public static InlineKeyboardMarkup getDaysInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Первый ряд
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("Понедельник", "day_monday"));
        row1.add(createInlineButton("Вторник", "day_tuesday"));
        row1.add(createInlineButton("Среда", "day_wednesday"));
        rows.add(row1);

        // Второй ряд
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("Четверг", "day_thursday"));
        row2.add(createInlineButton("Пятница", "day_friday"));
        row2.add(createInlineButton("Суббота", "day_saturday"));
        rows.add(row2);

        // Третий ряд
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Сегодня", "day_today"));
        row3.add(createInlineButton("Завтра", "day_tomorrow"));
        rows.add(row3);

        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    public static InlineKeyboardMarkup getActionsInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("Сегодня", "action_today"));
        row1.add(createInlineButton("Завтра", "action_tomorrow"));
        row1.add(createInlineButton("Неделя", "action_week"));
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("Ближайшее", "action_near"));
        row2.add(createInlineButton("Моя группа", "action_mygroup"));
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Сменить группу", "action_changegroup"));
        row3.add(createInlineButton("Помощь", "action_help"));
        rows.add(row3);

        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    // клавиатура для подтверждения
    public static InlineKeyboardMarkup getConfirmationKeyboard(String action) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createInlineButton("Да", action + "_yes"));
        row.add(createInlineButton("Нет", action + "_no"));
        rows.add(row);

        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }
    //назад
    public static ReplyKeyboardMarkup getBackKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Назад в меню");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // вспомогательный метод для создания кнопки
    private static InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
}