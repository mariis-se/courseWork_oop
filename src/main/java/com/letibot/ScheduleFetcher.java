package com.letibot;

import com.google.gson.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import java.time.DateTimeException;
import java.time.format.TextStyle;

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
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ScheduleException("API вернуло ошибку: " + response.getStatusLine().getStatusCode());
            }

            return EntityUtils.toString(response.getEntity(), "UTF-8");
        }
    }

    public static String getCurrentWeekParity() {
        LocalDate startOfSemester = LocalDate.of(2025, 9, 2);
        long weeksSinceStart = ChronoUnit.WEEKS.between(startOfSemester, LocalDate.now());
        boolean isOddWeek = weeksSinceStart % 2 == 0;
        return isOddWeek ? "нечётная" : "чётная";
    }

    public static String getCurrentWeekInfo() {
        return "*Текущая неделя:* " + getCurrentWeekParity();
    }

    public static String parseScheduleForDay(String json, String day, String groupNumber) throws ScheduleException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has(groupNumber)) {
                throw new ScheduleException("Группа не найдена");
            }

            JsonObject groupData = root.getAsJsonObject(groupNumber);
            JsonObject days = groupData.getAsJsonObject("days");
            int dayIndex = getDayIndex(day);

            if (dayIndex == -1) {
                throw new ScheduleException("Неверный день недели");
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

            List<JsonObject> lessonList = new ArrayList<>();
            for (JsonElement lesson : lessons) {
                lessonList.add(lesson.getAsJsonObject());
            }

            lessonList.sort((a, b) -> {
                String timeA = getSafeString(a, "start_time", "00:00");
                String timeB = getSafeString(b, "start_time", "00:00");
                return timeA.compareTo(timeB);
            });

            StringBuilder result = new StringBuilder();
            String dayName = dayObject.has("name") ?
                    dayObject.get("name").getAsString() : getRussianDayName(dayIndex);

            result.append(getCurrentWeekInfo()).append("\n");
            result.append("*").append(dayName).append("*\n\n");

            for (JsonObject lesson : lessonList) {
                String startTime = getSafeString(lesson, "start_time", "??:??");
                String endTime = getSafeString(lesson, "end_time", "??:??");
                String subject = getSafeString(lesson, "name", "Предмет не указан");
                String type = getSafeString(lesson, "subjectType", "");
                String teacher = getSafeString(lesson, "teacher", "");
                String room = getSafeString(lesson, "room", "");
                String form = getSafeString(lesson, "form", "");
                String weekType = getSafeString(lesson, "week", "");

                String weekInfo = "";
                if (!weekType.isEmpty() && !weekType.equals("null")) {
                    weekInfo = getWeekTypeInfo(weekType);
                }

                result.append("*").append(startTime).append(" - ").append(endTime);
                if (!weekInfo.isEmpty()) {
                    result.append(" ").append(weekInfo);
                }
                result.append("*\n");

                result.append(" ").append(subject);
                if (!type.isEmpty()) {
                    result.append(" (").append(type).append(")");
                }
                result.append("\n");

                if (!teacher.isEmpty() && !teacher.equals("null")) {
                    result.append(" ").append(teacher).append("\n");
                }

                if ("online".equalsIgnoreCase(form) || "distant".equalsIgnoreCase(form)) {
                    result.append(" Онлайн");
                } else if (!room.isEmpty() && !room.equals("null")) {
                    result.append(" Ауд. ").append(room);
                }

                result.append("\n\n");
            }

            return result.toString();

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата данных");
        }
    }

    public static String getWeekSchedule(String json, String groupNumber) throws ScheduleException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has(groupNumber)) {
                throw new ScheduleException("Группа не найдена");
            }

            JsonObject groupData = root.getAsJsonObject(groupNumber);
            JsonObject days = groupData.getAsJsonObject("days");

            StringBuilder result = new StringBuilder();
            result.append(getCurrentWeekInfo()).append("\n");
            result.append("*Расписание для группы ").append(groupNumber).append("*\n\n");

            String[] russianDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

            boolean hasLessons = false;

            for (int i = 0; i < 6; i++) {
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

                            for (JsonElement lesson : lessons) {
                                JsonObject lessonObj = lesson.getAsJsonObject();
                                String startTime = getSafeString(lessonObj, "start_time", "??:??");
                                String endTime = getSafeString(lessonObj, "end_time", "??:??");
                                String subject = getSafeString(lessonObj, "name", "Предмет");
                                String type = getSafeString(lessonObj, "subjectType", "");
                                String room = getSafeString(lessonObj, "room", "");

                                result.append("  • ").append(startTime).append("-").append(endTime);
                                result.append(" - ").append(subject);

                                if (!type.isEmpty()) {
                                    result.append(" (").append(type).append(")");
                                }

                                if (!room.isEmpty() && !room.equals("null")) {
                                    result.append(" (").append(room).append(")");
                                }

                                result.append("\n");
                            }
                            result.append("\n");
                        }
                    }
                }
            }

            if (!hasLessons) {
                return "На этой неделе занятий нет.";
            }

            return result.toString();

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата данных");
        }
    }

    public static String findNearestLesson(String json, String groupNumber) throws ScheduleException {
        try {
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();
            DayOfWeek currentDayOfWeek = today.getDayOfWeek();

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has(groupNumber)) {
                throw new ScheduleException("Группа не найдена");
            }

            JsonObject groupData = root.getAsJsonObject(groupNumber);
            JsonObject days = groupData.getAsJsonObject("days");

            // Ищем на сегодня
            int todayIndex = currentDayOfWeek.getValue() - 1;
            String todayKey = String.valueOf(todayIndex);

            if (days.has(todayKey)) {
                JsonObject todayObj = days.getAsJsonObject(todayKey);
                if (todayObj.has("lessons") && !todayObj.get("lessons").isJsonNull()) {
                    JsonArray lessons = todayObj.getAsJsonArray("lessons");

                    JsonObject nearestLesson = null;
                    LocalTime nearestTime = null;

                    for (JsonElement lesson : lessons) {
                        JsonObject lessonObj = lesson.getAsJsonObject();
                        String startTimeStr = getSafeString(lessonObj, "start_time", "");
                        if (!startTimeStr.isEmpty()) {
                            try {
                                LocalTime lessonTime = LocalTime.parse(startTimeStr);

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
                            JsonObject firstLesson = lessons.get(0).getAsJsonObject();

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

            return "Ближайших занятий не найдено";

        } catch (JsonSyntaxException e) {
            throw new ScheduleException("Ошибка формата данных");
        }
    }

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
        result.append("*Ближайшее занятие*\n\n");
        result.append("*").append(day).append("*\n");
        result.append("*").append(startTime).append(" - ").append(endTime).append("*\n");
        result.append(" ").append(subject);

        if (!type.isEmpty()) {
            result.append(" (").append(type).append(")");
        }
        result.append("\n");

        if (!teacher.isEmpty() && !teacher.equals("null")) {
            result.append(" ").append(teacher).append("\n");
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
            return true;
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

    private static int getDayIndex(String day) {
        switch (day.toLowerCase()) {
            case "monday": return 0;
            case "tuesday": return 1;
            case "wednesday": return 2;
            case "thursday": return 3;
            case "friday": return 4;
            case "saturday": return 5;
            default: return -1;
        }
    }

    private static String getRussianDayName(int index) {
        String[] days = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
        return (index >= 0 && index < days.length) ? days[index] : "День недели";
    }

    private static String getSafeString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            String value = obj.get(key).getAsString();
            return (value == null || value.equals("null") || value.trim().isEmpty()) ?
                    defaultValue : value.trim();
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
            default: return day;
        }
    }


}

class ScheduleException extends Exception {
    public ScheduleException(String message) {
        super(message);
    }
}