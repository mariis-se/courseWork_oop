package com.letibot;

import com.google.gson.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ScheduleFetcher {

    public static String getScheduleForGroup(String groupId) throws IOException, ScheduleException {
        String url = String.format(
                "https://digital.etu.ru/api/mobile/schedule?groupNumber=%s&season=autumn&year=2025&joinWeeks=true&withURL=true",
                groupId
        );

        return fetchJsonFromUrl(url);
    }

    private static String fetchJsonFromUrl(String url) throws IOException, ScheduleException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Telegram Bot (Java)");
            request.setHeader("Accept", "application/json");

            HttpResponse response = client.execute(request);
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() != 200) {
                throw new ScheduleException("API вернуло ошибку: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
            }

            return EntityUtils.toString(response.getEntity(), "UTF-8");

        } catch (HttpHostConnectException e) {
            throw new ScheduleException("Не удалось подключиться к серверу ЛЭТИ. Проверьте интернет-соединение.");
        } catch (SocketTimeoutException e) {
            throw new ScheduleException("Превышено время ожидания ответа от сервера.");
        } catch (UnknownHostException e) {
            throw new ScheduleException("Сервер ЛЭТИ не найден. Возможно, проблемы с DNS.");
        } catch (ClientProtocolException e) {
            throw new ScheduleException("Ошибка протокола при запросе к API.");
        } catch (IOException e) {
            throw new ScheduleException("Ошибка ввода-вывода: " + e.getMessage());
        }
    }

    public static String getCurrentWeekParity() {
        //  первая учебная неделя сентября - нечетная
        LocalDate startOfSemester = LocalDate.of(2025, 9, 2); // 2 сент 2025
        long weeksSinceStart = ChronoUnit.WEEKS.between(startOfSemester, LocalDate.now());

        // Если weeksSinceStart четное - нечетная неделя, нечетное - четная
        boolean isOddWeek = weeksSinceStart % 2 == 0;

        return isOddWeek ? "нечётная" : "чётная";
    }

    public static String getCurrentWeekInfo() {
        return "*Текущая неделя:* " + getCurrentWeekParity();
    }

    public static String parseScheduleForDay(String json, String day, String groupNumber) throws ScheduleException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Получаем данные группы
            if (!root.has(groupNumber)) {
                throw new ScheduleException("Группа " + groupNumber + " не найдена в расписании.");
            }

            JsonObject groupData = root.getAsJsonObject(groupNumber);
            if (!groupData.has("days")) {
                throw new ScheduleException("Дни недели не найдены для группы " + groupNumber + ".");
            }

            JsonObject days = groupData.getAsJsonObject("days");

            // Преобразуем день недели в числовой индекс
            int dayIndex = getDayIndex(day);
            if (dayIndex == -1) {
                throw new ScheduleException("Неверный день недели. Используйте: monday, tuesday, wednesday, thursday, friday, saturday");
            }

            String dayKey = String.valueOf(dayIndex);
            if (!days.has(dayKey)) {
                return "В этот день занятий нет.";
            }

            JsonObject dayObject = days.getAsJsonObject(dayKey);
            if (!dayObject.has("lessons") || dayObject.get("lessons").isJsonNull()) {
                return "В этот день занятий нет.";
            }

            JsonArray lessons = dayObject.getAsJsonArray("lessons");
            if (lessons.size() == 0) {
                return "В этот день занятий нет.";
            }

            // Сортируем занятия по времени начала
            List<JsonObject> lessonList = new ArrayList<>();
            for (JsonElement lesson : lessons) {
                lessonList.add(lesson.getAsJsonObject());
            }

            lessonList.sort((a, b) -> {
                String timeA = getSafeString(a, "start_time", "00:00");
                String timeB = getSafeString(b, "start_time", "00:00");
                try {
                    return LocalTime.parse(timeA).compareTo(LocalTime.parse(timeB));
                } catch (Exception e) {
                    return 0;
                }
            });

            // Формируем красивый ответ
            StringBuilder result = new StringBuilder();
            String dayName = dayObject.has("name") ?
                    dayObject.get("name").getAsString() : getRussianDayName(dayIndex);

            result.append(getCurrentWeekInfo()).append("\n");
            result.append(" *").append(dayName).append("*\n\n");

            for (JsonObject lesson : lessonList) {
                String startTime = getSafeString(lesson, "start_time", "??:??");
                String endTime = getSafeString(lesson, "end_time", "??:??");
                String subject = getSafeString(lesson, "name", "Предмет не указан");
                String type = getSafeString(lesson, "subjectType", "");
                String teacher = getSafeString(lesson, "teacher", "");
                String secondTeacher = getSafeString(lesson, "second_teacher", "");
                String room = getSafeString(lesson, "room", "");
                String form = getSafeString(lesson, "form", "");
                String weekType = getSafeString(lesson, "week", "");

                // Определяем тип недели
                String weekInfo = "";
                if (!weekType.isEmpty() && !weekType.equals("null")) {
                    weekInfo = getWeekTypeInfo(weekType);
                }

                // Форматирование времени
                result.append(" *").append(startTime).append(" - ").append(endTime);
                if (!weekInfo.isEmpty()) {
                    result.append(" ").append(weekInfo);
                }
                result.append("*\n");

                // Предмет и тип
                result.append(" ").append(subject);
                if (!type.isEmpty()) {
                    result.append(" (").append(type).append(")");
                }
                result.append("\n");

                // Преподаватель
                if (!teacher.isEmpty() && !teacher.equals("null")) {
                    if (!secondTeacher.isEmpty() && !secondTeacher.equals("null")) {
                        result.append(" ").append(teacher).append(", ").append(secondTeacher).append("\n");
                    } else {
                        result.append(" ").append(teacher).append("\n");
                    }
                }

                // Аудитория и форма обучения
                if ("online".equalsIgnoreCase(form) || "distant".equalsIgnoreCase(form)) {
                    result.append(" Онлайн");
                    String url = getSafeString(lesson, "url", "");
                    if (!url.isEmpty() && !url.equals("null")) {
                        result.append("\n ").append(url);
                    }
                } else if (!room.isEmpty() && !room.equals("null")) {
                    result.append(" Ауд. ").append(room);
                } else {
                    result.append(" Аудитория не указана");
                }

                result.append("\n\n");
            }

            return result.toString();

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата JSON от сервера: " + e.getMessage());
        } catch (JsonIOException e) {
            throw new ScheduleException("Ошибка чтения JSON: " + e.getMessage());
        } catch (Exception e) {
            throw new ScheduleException("Ошибка при обработке расписания: " + e.getMessage());
        }
    }

    public static String getWeekSchedule(String json, String groupNumber) throws ScheduleException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has(groupNumber)) {
                throw new ScheduleException("Группа " + groupNumber + " не найдена в расписании.");
            }

            JsonObject groupData = root.getAsJsonObject(groupNumber);
            if (!groupData.has("days")) {
                throw new ScheduleException("Расписание для группы " + groupNumber + " не найдено.");
            }

            JsonObject days = groupData.getAsJsonObject("days");

            StringBuilder result = new StringBuilder();
            result.append(getCurrentWeekInfo()).append("\n");
            result.append(" *Расписание для группы ").append(groupNumber).append("*\n\n");

            String[] russianDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

            boolean hasLessons = false;

            for (int i = 0; i < russianDays.length; i++) {
                String dayKey = String.valueOf(i);
                if (days.has(dayKey)) {
                    JsonObject day = days.getAsJsonObject(dayKey);
                    if (day.has("lessons") && !day.get("lessons").isJsonNull()) {
                        JsonArray lessons = day.getAsJsonArray("lessons");
                        if (lessons.size() > 0) {
                            hasLessons = true;

                            String dayName = day.has("name") ?
                                    day.get("name").getAsString() : russianDays[i];
                            result.append("*").append(dayName).append("*:\n");

                            // Сортируем занятия по времени
                            List<JsonObject> lessonList = new ArrayList<>();
                            for (JsonElement lesson : lessons) {
                                lessonList.add(lesson.getAsJsonObject());
                            }

                            lessonList.sort((a, b) -> {
                                String timeA = getSafeString(a, "start_time", "00:00");
                                String timeB = getSafeString(b, "start_time", "00:00");
                                try {
                                    return LocalTime.parse(timeA).compareTo(LocalTime.parse(timeB));
                                } catch (Exception e) {
                                    return 0;
                                }
                            });

                            for (JsonObject lesson : lessonList) {
                                String startTime = getSafeString(lesson, "start_time", "??:??");
                                String endTime = getSafeString(lesson, "end_time", "??:??");
                                String subject = getSafeString(lesson, "name", "Предмет");
                                String type = getSafeString(lesson, "subjectType", "");
                                String room = getSafeString(lesson, "room", "");
                                String form = getSafeString(lesson, "form", "");
                                String weekType = getSafeString(lesson, "week", "");

                                // Форматируем информацию о неделе
                                String weekInfo = "";
                                if (!weekType.isEmpty() && !weekType.equals("null")) {
                                    weekInfo = getWeekTypeShort(weekType);
                                }

                                // Форматируем аудиторию
                                String roomInfo = "";
                                if ("online".equalsIgnoreCase(form) || "distant".equalsIgnoreCase(form)) {
                                    roomInfo = "Онлайн";
                                } else if (!room.isEmpty() && !room.equals("null")) {
                                    roomInfo = room;
                                } else {
                                    roomInfo = "—";
                                }

                                result.append("  • ").append(startTime).append("-").append(endTime);
                                if (!weekInfo.isEmpty()) {
                                    result.append(weekInfo);
                                }
                                result.append(" - ").append(subject);

                                if (!type.isEmpty()) {
                                    result.append(" (").append(type).append(")");
                                }

                                result.append(" (").append(roomInfo).append(")\n");
                            }
                            result.append("\n");
                        }
                    }
                }
            }

            if (!hasLessons) {
                return " На этой неделе занятий нет.";
            }

            return result.toString();

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата JSON от сервера: " + e.getMessage());
        } catch (JsonIOException e) {
            throw new ScheduleException("Ошибка чтения JSON: " + e.getMessage());
        } catch (Exception e) {
            throw new ScheduleException("Ошибка при обработке расписания: " + e.getMessage());
        }
    }

    // Функция поиска ближайшего занятия
    public static String findNearestLesson(String json, String groupNumber) throws ScheduleException {
        try {
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();
            DayOfWeek currentDayOfWeek = today.getDayOfWeek();

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has(groupNumber)) {
                throw new ScheduleException("Группа " + groupNumber + " не найдена в расписании.");
            }

            JsonObject groupData = root.getAsJsonObject(groupNumber);
            if (!groupData.has("days")) {
                throw new ScheduleException("Расписание для группы " + groupNumber + " не найдено.");
            }

            JsonObject days = groupData.getAsJsonObject("days");

            // Ищем на сегодня
            int todayIndex = currentDayOfWeek.getValue() - 1; // Monday = 0
            String todayKey = String.valueOf(todayIndex);

            if (days.has(todayKey)) {
                JsonObject todayObj = days.getAsJsonObject(todayKey);
                if (todayObj.has("lessons") && !todayObj.get("lessons").isJsonNull()) {
                    JsonArray lessons = todayObj.getAsJsonArray("lessons");

                    // Ищем ближайшее занятие на сегодня
                    JsonObject nearestLesson = null;
                    LocalTime nearestTime = null;

                    for (JsonElement lesson : lessons) {
                        JsonObject lessonObj = lesson.getAsJsonObject();
                        String startTimeStr = getSafeString(lessonObj, "start_time", "");
                        if (!startTimeStr.isEmpty()) {
                            try {
                                LocalTime lessonTime = LocalTime.parse(startTimeStr);

                                // Проверяем четность недели
                                String weekType = getSafeString(lessonObj, "week", "");
                                if (isLessonForCurrentWeek(weekType)) {
                                    if (lessonTime.isAfter(now) || lessonTime.equals(now)) {
                                        if (nearestTime == null || lessonTime.isBefore(nearestTime)) {
                                            nearestTime = lessonTime;
                                            nearestLesson = lessonObj;
                                        }
                                    }
                                }
                            } catch (DateTimeException e) {
                                // Пропускаем некорректное время
                                continue;
                            }
                        }
                    }

                    if (nearestLesson != null) {
                        return formatNearestLesson(nearestLesson, "сегодня");
                    }
                }
            }

            // Если на сегодня не нашли, ищем на ближайшие дни
            for (int i = 1; i <= 7; i++) {
                int nextDayIndex = (todayIndex + i) % 7;
                String nextDayKey = String.valueOf(nextDayIndex);

                if (days.has(nextDayKey)) {
                    JsonObject dayObj = days.getAsJsonObject(nextDayKey);
                    if (dayObj.has("lessons") && !dayObj.get("lessons").isJsonNull()) {
                        JsonArray lessons = dayObj.getAsJsonArray("lessons");

                        if (lessons.size() > 0) {
                            // Берем первое занятие дня
                            JsonObject firstLesson = lessons.get(0).getAsJsonObject();

                            // Определяем день недели
                            String dayName = "";
                            switch (i) {
                                case 1: dayName = "завтра"; break;
                                case 2: dayName = "послезавтра"; break;
                                default:
                                    LocalDate targetDate = today.plusDays(i);
                                    dayName = targetDate.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
                                    dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
                            }

                            return formatNearestLesson(firstLesson, dayName);
                        }
                    }
                }
            }

            return " Ближайших занятий не найдено.";

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата JSON от сервера: " + e.getMessage());
        } catch (JsonIOException e) {
            throw new ScheduleException("Ошибка чтения JSON: " + e.getMessage());
        } catch (DateTimeException e) {
            throw new ScheduleException("Ошибка обработки даты/времени: " + e.getMessage());
        } catch (Exception e) {
            throw new ScheduleException("Ошибка при поиске ближайшего занятия: " + e.getMessage());
        }
    }

    // Вспомогательные методы (остаются без изменений)
    private static String formatNearestLesson(JsonObject lesson, String day) {
        String startTime = getSafeString(lesson, "start_time", "??:??");
        String endTime = getSafeString(lesson, "end_time", "??:??");
        String subject = getSafeString(lesson, "name", "Предмет не указан");
        String type = getSafeString(lesson, "subjectType", "");
        String teacher = getSafeString(lesson, "teacher", "");
        String room = getSafeString(lesson, "room", "");
        String form = getSafeString(lesson, "form", "");
        String weekType = getSafeString(lesson, "week", "");

        StringBuilder result = new StringBuilder();
        result.append(" *Ближайшее занятие*\n\n");
        result.append(" *").append(day).append("*\n");
        result.append(" *").append(startTime).append(" - ").append(endTime).append("*\n");
        result.append(" ").append(subject);

        if (!type.isEmpty()) {
            result.append(" (").append(type).append(")");
        }
        result.append("\n");

        if (!teacher.isEmpty() && !teacher.equals("null")) {
            result.append("👨‍🏫 ").append(teacher).append("\n");
        }

        if ("online".equalsIgnoreCase(form) || "distant".equalsIgnoreCase(form)) {
            result.append(" Онлайн");
        } else if (!room.isEmpty() && !room.equals("null")) {
            result.append(" Ауд. ").append(room);
        }

        if (!weekType.isEmpty() && !weekType.equals("null")) {
            result.append("\n ").append(getWeekTypeInfo(weekType));
        }

        return result.toString();
    }

    private static boolean isLessonForCurrentWeek(String weekType) {
        if (weekType.isEmpty() || weekType.equals("null") || weekType.equals("3")) {
            return true; // Для всех недель
        }

        String currentParity = getCurrentWeekParity();
        boolean isCurrentOdd = currentParity.equals("нечётная");

        return (isCurrentOdd && weekType.equals("1")) || (!isCurrentOdd && weekType.equals("2"));
    }

    private static String getWeekTypeInfo(String weekType) {
        switch (weekType) {
            case "1": return "(Нечётная неделя)";
            case "2": return "(Чётная неделя)";
            case "3": return "(Все недели)";
            default: return "";
        }
    }

    private static String getWeekTypeShort(String weekType) {
        switch (weekType) {
            case "1": return " [НЧ]";
            case "2": return " [ЧТ]";
            case "3": return " [ВС]";
            default: return "";
        }
    }

    private static int getDayIndex(String day) {
        switch (day.toLowerCase()) {
            case "monday": case "понедельник": case "mon": return 0;
            case "tuesday": case "вторник": case "tue": return 1;
            case "wednesday": case "среда": case "wed": return 2;
            case "thursday": case "четверг": case "thu": return 3;
            case "friday": case "пятница": case "fri": return 4;
            case "saturday": case "суббота": case "sat": return 5;
            case "sunday": case "воскресенье": case "sun": return 6;
            default: return -1;
        }
    }

    private static String getRussianDayName(int index) {
        String[] days = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        return (index >= 0 && index < days.length) ? days[index] : "День недели";
    }

    private static String getSafeString(JsonObject obj, String key, String defaultValue) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                String value = obj.get(key).getAsString();
                return (value == null || value.equals("null") || value.trim().isEmpty()) ?
                        defaultValue : value.trim();
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return defaultValue;
    }

    public static String getTomorrowDayName() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DayOfWeek day = tomorrow.getDayOfWeek();
        return translateDay(day.getDisplayName(TextStyle.FULL, new Locale("ru")).toLowerCase());
    }

    public static String getTodayDayName() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();
        return translateDay(day.getDisplayName(TextStyle.FULL, new Locale("ru")).toLowerCase());
    }

    private static String translateDay(String day) {
        switch (day) {
            case "понедельник": return "monday";
            case "вторник": return "tuesday";
            case "среда": return "wednesday";
            case "четверг": return "thursday";
            case "пятница": return "friday";
            case "суббота": return "saturday";
            case "воскресенье": return "sunday";
            default: return day;
        }
    }
}

class ScheduleException extends Exception {
    public ScheduleException(String message) {
        super(message);
    }

    public ScheduleException(String message, Throwable cause) {
        super(message, cause);
    }
}