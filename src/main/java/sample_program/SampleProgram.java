package sample_program;

import common.Records;
import licence_client.ClientApi;

import java.util.Scanner;

public class SampleProgram {
    public static void main(String[] args) {
        ClientApi api = new ClientApi();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("""
                1. Start discovery
                2. Set licence
                3. Get licence token
                4. Stop
                0. Exit
                """);

            String option = scanner.nextLine();

            switch (option) {
                case "1" -> api.start();
                case "2" -> {
                    System.out.print("User: ");
                    String user = scanner.nextLine();
                    System.out.print("Key: ");
                    String key = scanner.nextLine();
                    api.setLicence(user, key);
                }
                case "3" -> {
                    Records.ServerResponse response = api.getLicenceToken();
                    System.out.println(response);
                }
                case "4" -> api.stop();
                case "0" -> {
                    api.stop();
                    return;
                }
                default -> {
                    System.out.println("Invalid option. Try again.");
                }
            }
        }
    }
}
