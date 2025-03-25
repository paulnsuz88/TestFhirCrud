import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;


public class TestFhirCrud {
        public static void main(String[] args) throws IOException {

            OkHttpClient client = new OkHttpClient();
            final String baseFhirServerUrl = "http://hapi.fhir.org/baseR4";
            final int patientID = 596742;
            Request request = new Request.Builder()
                    .url(baseFhirServerUrl + "/Patient/" + patientID)
                    .build();

            System.out.print("Retrieving Patient resource with ID=" + patientID + " ...");
            try (Response response = client.newCall(request).execute()) {
                System.out.println("\nHTTP Status return code: " + response.code() + " (" + response.message() + ")");
                var responseBody = response.body();
                if (responseBody != null) {
                    String responseBodyStr = responseBody.string();
                    System.out.println("Response body returned:");
                    System.out.println(responseBodyStr);
                    // parse id value from response JSON, using Jackson
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(responseBodyStr);
                    String resourceType = rootNode.get("resourceType").toString();
                    int resourceId = rootNode.get("id").asInt();
                    System.out.println("Extracted values:");
                    System.out.println("resourceType = " + resourceType);
                    System.out.println("id = " + resourceId);
                }
            }
        }
    }
