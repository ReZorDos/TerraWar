package ru.kpfu.itis;

import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) {
        ServerService server = new ServerService();
        try {
            server.start();
            System.out.println("Сервер запущен. Ожидаются подключения игроков...");

        } catch (IOException e) {
            System.err.println("Не удалось запустить сервер: " + e.getMessage());
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                System.out.println("Сервер остановлен.");
            } catch (IOException e) {
                System.err.println("Ошибка при остановке сервера: " + e.getMessage());
            }
        }));

    }
}


