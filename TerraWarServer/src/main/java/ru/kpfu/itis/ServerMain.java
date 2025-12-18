package ru.kpfu.itis;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerMain {

    public static void main(String[] args) {
        ServerService server = new ServerService();
        try {
            server.start();
            log.info("Сервер запущен. Ожидаются подключения игроков...");

        } catch (IOException e) {
            log.error("Не удалось запустить сервер: {}", e.getMessage(), e);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                log.info("Сервер остановлен.");
            } catch (IOException e) {
                log.error("Ошибка остановки сервера: {}", e.getMessage(), e);
            }
        }));

    }
}


