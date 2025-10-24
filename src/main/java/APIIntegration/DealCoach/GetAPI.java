package APIIntegration.DealCoach;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GetAPI {
    JsonNode dataArray;
    private int Pitch_Deck=0;
    private int Solution_Brief=0;
    private int Research_or_Analyst_Report=0;
    private int Others=0;

    public boolean getSolutionCategoryCount(String solutionsString) {
        try {
            if (solutionsString != null && !solutionsString.isEmpty()) {
                String[] solutions = solutionsString.split(",");

                // Create a JSON object
                Gson gson = new Gson();
                String jsonData = gson.toJson(new Solutions(solutions));

                // Base URL of the API
                String baseUrl = "https://api.structuredweb.com/library/?attributes=";

                // URL encode the JSON data
                String encodedData = URLEncoder.encode(jsonData, StandardCharsets.UTF_8.toString());

                // Construct the full URL with the encoded JSON data
                String fullUrl = baseUrl + encodedData;

                // Create an HttpClient instance
                HttpClient httpClient = HttpClientBuilder.create().build();

                // Create a GET request object with the full URL
                HttpGet request = new HttpGet(fullUrl);

                // Add the token header
                request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PostAPI.getToken());

                // Send the GET request
                HttpResponse response = httpClient.execute(request);

                // Get the response body as a string
                String responseBody = EntityUtils.toString(response.getEntity());

                // Use Jackson's ObjectMapper to pretty print the response JSON
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);

                // Get the "data" array from the response
                dataArray = rootNode.path("data");

                // Loop through the "data" array and count the number of items with "subtype" : "Solution Brief"
                for (JsonNode dataNode : dataArray) {
                    String subtype = dataNode.path("subtype").asText();
                    if (subtype.equals("Solution Brief")) {
                        Solution_Brief++;
                    } else if (subtype.equals("Pitch Deck")) {
                        Pitch_Deck++;
                    } else if (subtype.equals("Research or Analyst Report")) {
                        Research_or_Analyst_Report++;
                    }
                }
                int totalSum = Solution_Brief + Pitch_Deck + Research_or_Analyst_Report;
                Others = totalSubType() - totalSum;
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Class to represent the JSON structure
    static class Solutions {
        private String[] Solution;

        public Solutions(String[] Solution) {
            this.Solution = Solution;
        }

        public String[] getSolution() {
            return Solution;
        }

        public void setSolution(String[] Solution) {
            this.Solution = Solution;
        }
    }

    private int totalSubType(){
        int count = 0;
        // Iterate over each object in the "data" array
        for (JsonNode dataObject : dataArray) {
            // Get the value of "subtype" from each object
            String subtype = dataObject.get("subtype").asText();

            // Check if the value of "subtype" is not null
            if (subtype != null) {
                // Increment the count if "subtype" is found
                count++;
            }
        }
        return count;
    }

    public int getOthers() {
        return Others;
    }

    public int getPitch_Deck() {
        return Pitch_Deck;
    }

    public int getResearch_or_Analyst_Report() {
        return Research_or_Analyst_Report;
    }

    public int getSolution_Brief() {
        return Solution_Brief;
    }

}
