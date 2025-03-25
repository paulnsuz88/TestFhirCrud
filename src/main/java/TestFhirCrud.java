import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;


public class TestFhirCrud {
        public static void main(String[] args) throws IOException {

            OkHttpClient client = new OkHttpClient();
            final String baseFhirServerUrl = "http://hapi.fhir.org/baseR4";
            final int patientID = 596742;
            Request request = new Request.Builder()
                    .url(baseFhirServerUrl + "/Patient/" + patientID)
                    .build();

            System.out.print("Retrieving Patient resource with ID=" + patientID + "...");
            try (Response response = client.newCall(request).execute()) {
                System.out.println("\nHTTP Status return code: " + response.code() + " (" + response.message() + ")");
                if (response.body() != null) {
                    System.out.println("Resource body returned:");
                    System.out.println(response.body().string());
                }
            }
        }
    }
