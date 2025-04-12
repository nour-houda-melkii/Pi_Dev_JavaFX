package Services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AIDescriptionService {
    private static final Logger LOGGER = Logger.getLogger(AIDescriptionService.class.getName());

    // API configuration
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    // Flag to control whether we attempt online API calls
    private boolean useOnlineAPI = true;

    // Counter to track failed API calls
    private int failedApiCallCount = 0;
    private static final int MAX_FAILED_CALLS = 2;

    /**
     * Generates a product description based on the product name
     * @param productName The name of the product
     * @return AI-generated description
     */
    public String generateProductDescription(String productName) {
        if (!useOnlineAPI || failedApiCallCount >= MAX_FAILED_CALLS) {
            return generateOfflineProductDescription(productName);
        }

        String prompt = "Generate a short marketing description for a product named: " + productName;
        String result = generateDescription(prompt);

        // If the result indicates an error, fall back to offline generation
        if (result.startsWith("Error generating description:") ||
                result.startsWith("Failed to generate description")) {
            return generateOfflineProductDescription(productName);
        }

        return result;
    }

    /**
     * Generates a category description based on the category name
     * @param categoryName The name of the category
     * @return AI-generated description
     */
    public String generateCategoryDescription(String categoryName) {
        if (!useOnlineAPI || failedApiCallCount >= MAX_FAILED_CALLS) {
            return generateOfflineCategoryDescription(categoryName);
        }

        String prompt = "Generate a brief description for a product category named: " + categoryName;
        String result = generateDescription(prompt);

        // If the result indicates an error, fall back to offline generation
        if (result.startsWith("Error generating description:") ||
                result.startsWith("Failed to generate description")) {
            return generateOfflineCategoryDescription(categoryName);
        }

        return result;
    }

    /**
     * Makes the API call to generate a description
     * @param prompt The prompt to send to the AI
     * @return The generated description or error message
     */
    private String generateDescription(String prompt) {
        try {
            // Create connection
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000); // 10 second timeout

            // Create request body with proper JSON escaping
            String escapedPrompt = prompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            String requestBody = "{"
                    + "\"model\": \"gpt-3.5-turbo\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"You are a helpful assistant that writes concise product descriptions.\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + escapedPrompt + "\"}"
                    + "],"
                    + "\"temperature\": 0.7,"
                    + "\"max_tokens\": 100"
                    + "}";

            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode == 200 ? connection.getInputStream() : connection.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseString = response.toString();
            if (responseCode == 200) {
                // Check if the response contains the content field
                if (responseString.contains("\"content\"")) {
                    int contentStart = responseString.indexOf("\"content\":") + 11;
                    int contentEnd = responseString.indexOf("\"", contentStart);
                    if (contentEnd > contentStart) {
                        // Reset failed call counter on success
                        failedApiCallCount = 0;
                        return responseString.substring(contentStart, contentEnd);
                    }
                }

                // More robust parsing using substring search (basic but effective)
                if (responseString.contains("\"content\":")) {
                    String contentMarker = "\"content\":\"";
                    int startIndex = responseString.indexOf(contentMarker) + contentMarker.length();

                    // Look for the closing quote that's not escaped
                    boolean escaped = false;
                    StringBuilder content = new StringBuilder();

                    for (int i = startIndex; i < responseString.length(); i++) {
                        char c = responseString.charAt(i);
                        if (c == '\\') {
                            escaped = !escaped;
                            content.append(c);
                        } else if (c == '"' && !escaped) {
                            break;
                        } else {
                            escaped = false;
                            content.append(c);
                        }
                    }

                    if (content.length() > 0) {
                        // Reset failed call counter on success
                        failedApiCallCount = 0;
                        return content.toString();
                    }
                }

                LOGGER.warning("Failed to parse API response: " + responseString);
                return generateOfflineProductDescription(prompt);
            } else {
                // Increment failed call counter
                failedApiCallCount++;
                if (failedApiCallCount >= MAX_FAILED_CALLS) {
                    LOGGER.warning("API quota exceeded or other API error. Switching to offline mode.");
                    useOnlineAPI = false;
                }
                return "Error generating description: " + responseString;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to OpenAI API", e);
            failedApiCallCount++;
            return "Failed to generate description. Please try again or enter manually.";
        }
    }

    /**
     * A more sophisticated offline product description generator
     */
    private String generateOfflineProductDescription(String productName) {
        String[] templates = {
                "Introducing our premium %s, designed to elevate your experience with exceptional quality and performance.",
                "Discover the versatile %s, crafted with attention to detail and built to exceed your expectations.",
                "Experience the innovation of our %s, combining cutting-edge technology with elegant design.",
                "Our %s delivers unmatched performance and reliability, perfect for discerning customers.",
                "Meet the extraordinary %s, where functionality meets style in a seamless package.",
                "The %s offers superior features and durability, making it an essential addition to your collection.",
                "Elevate your lifestyle with our meticulously designed %s, setting new standards in its category."
        };

        return String.format(templates[new Random().nextInt(templates.length)], productName);
    }

    /**
     * A more sophisticated offline category description generator
     */
    private String generateOfflineCategoryDescription(String categoryName) {
        String[] templates = {
                "Explore our selection of %s, featuring top-quality products designed for performance and satisfaction.",
                "Our %s collection offers exceptional variety and value for every need and preference.",
                "Browse our premium %s range, carefully curated to provide superior options for all customers.",
                "Discover the perfect solutions in our %s category, where quality meets innovation.",
                "Find everything you need in our comprehensive %s section, designed with your satisfaction in mind."
        };

        return String.format(templates[new Random().nextInt(templates.length)], categoryName);
    }

    /**
     * Method to reset API usage flag (useful if you want to retry online API after some time)
     */
    public void resetApiUsage() {
        useOnlineAPI = true;
        failedApiCallCount = 0;
    }
}