package org.example.weatherbot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final String botToken;
    private final String weatherApiKey;
    private final String defaultCity = "Samara";

    public WeatherBot(String botToken, String weatherApiKey) {
        this.botToken = botToken;
        this.weatherApiKey = weatherApiKey;

        if (this.botToken == null || this.botToken.isBlank()) {
            throw new IllegalStateException("BOT_TOKEN не задан!");
        }
        // аналогично для weatherApiKey, если нужно

        telegramClient = new OkHttpTelegramClient(this.botToken);
    }
    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String text = update.getMessage().getText().trim().toLowerCase();
        long chatId = update.getMessage().getChatId();

        if (text.equals("/start") || text.equals("/help")) {
            sendText(chatId,
                    "Привет! Я показываю погоду.\n\n" +
                            "Команды:\n" +
                            "/weather — погода в " + defaultCity + "\n" +
                            "/weather [город] — погода в указанном городе\n" +
                            "Пример: /weather Приволжье"
            );
        }
        else if (text.startsWith("/weather")) {
            if (text.contains("очко")){
                sendText(chatId, "🌡️ В очке Андрюхи сейчас 9999999°C (ощущается как будто его в жопу ебали 100 человек)");
                return;
            }
            String city = defaultCity;


            if (text.contains(" ")) {
                String[] parts = text.split("\\s+", 2);
                if (parts.length > 1) {
                    city = parts[1].trim();
                }
            }
            String answer = getWeather(city);
            sendText(chatId, answer);
        }
        else {
            sendText(chatId, "Не понял команду. Попробуй /help");
        }
    }

    private void sendText(long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
        try {
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки: " + e.getMessage());
        }
    }

    private String getWeather(String city) {
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?q=" + city +
                    "&appid=" + weatherApiKey +
                    "&units=metric" +
                    "&lang=ru";

            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Не удалось получить погоду (код " + response.statusCode() + ")";
            }

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(response.body(), JsonObject.class);

            if (root.has("cod") && root.get("cod").getAsInt() != 200) {
                String msg = root.has("message") ? root.get("message").getAsString() : "Город не найден";
                return "Ошибка: " + msg;
            }

            JsonObject main = root.getAsJsonObject("main");
            double temp = main.get("temp").getAsDouble();
            double feelsLike = main.get("feels_like").getAsDouble();
            String desc = root.getAsJsonArray("weather")
                    .get(0).getAsJsonObject()
                    .get("description").getAsString();

            return String.format("🌡️ В %s сейчас %.1f°C (ощущается как %.1f°C)\n%s",
                    city, temp, feelsLike, desc);
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка соединения с OpenWeatherMap 😔";
        }
    }

    public TelegramClient getTelegramClient() {
        return telegramClient;
    }
}