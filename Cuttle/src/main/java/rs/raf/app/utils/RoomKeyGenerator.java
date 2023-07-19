package rs.raf.app.utils;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Component
public class RoomKeyGenerator {
    public static String generateKey() {
        try {
            // Generate a random string for the key
            String randomString = generateRandomString();

            // Create an instance of the MessageDigest class with the desired algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Generate the hash value for the random string
            byte[] hash = digest.digest(randomString.getBytes());

            // Convert the hash bytes to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // Return the hash key
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle the exception if the specified algorithm is not available
            e.printStackTrace();
            return null;
        }
    }

    private static String generateRandomString() {
        // Define the characters to be used in the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Set the desired length of the random string
        int length = 10;

        // Create a StringBuilder to store the random string
        StringBuilder sb = new StringBuilder(length);

        // Create an instance of the Random class
        Random random = new Random();

        // Generate the random string
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        // Return the random string
        return sb.toString();
    }

}