package org.example.weatherbot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        try {
            String botToken = System.getenv("BOT_TOKEN");
            String weatherApiKey = System.getenv("WEATHER_API_KEY");

            if (botToken == null || botToken.isBlank()) {
                System.err.println("Ошибка: задайте переменную окружения BOT_TOKEN");
                System.exit(1);
            }
            if (weatherApiKey == null || weatherApiKey.isBlank()) {
                System.err.println("Ошибка: задайте переменную окружения WEATHER_API_KEY");
                System.exit(1);
            }


            TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();
            application.registerBot(botToken, new WeatherBot(botToken, weatherApiKey));

            System.out.println("Бот успешно запущен. Пиши ему в Telegram!");

            // Бесконечное ожидание
            Thread.currentThread().join();
        } catch (TelegramApiException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}