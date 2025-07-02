package StatsTracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HTTPClient {
    private static final Logger logger = LogManager.getLogger(HTTPClient.class.getName());

    private final String baseUrl;
    private final String userId;
    private final String token;

    public HTTPClient(String baseUrl, String userId, String token) {
        this.baseUrl = baseUrl;
        this.userId = userId;
        this.token = token;
    }

    public String get(String endpoint) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = setupConnection(endpoint, "GET");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (IOException e) {
            logger.error("Error during POST request to endpoint " + endpoint, e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String post(String endpoint, String jsonBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = setupConnection(endpoint, "POST");
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection setupConnection(String endpoint, String method) throws IOException {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("User-ID", userId);

        if (method.equals("POST")) {
            connection.setDoOutput(true);
        }

        return connection;
    }
}
