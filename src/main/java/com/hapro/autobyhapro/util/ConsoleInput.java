package com.hapro.autobyhapro.util;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ConsoleInput {

    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public String readText(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    public int readInt(String label, int defaultValue) {
        System.out.print(label);

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            System.out.println("Giá trị không hợp lệ, dùng mặc định: " + defaultValue);
            return defaultValue;
        }
    }

    public long readLong(String label, long defaultValue) {
        System.out.print(label);

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            System.out.println("Giá trị không hợp lệ, dùng mặc định: " + defaultValue);
            return defaultValue;
        }
    }

    public void waitForEnter() {
        System.out.println();
        System.out.print("Nhấn Enter để tiếp tục...");
        scanner.nextLine();
    }
}
