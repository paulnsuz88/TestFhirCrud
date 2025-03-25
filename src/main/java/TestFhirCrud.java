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

            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    System.out.println("HTTP Status Code: " + response.code() + " (" + response.message() + ")");
                    System.out.println(response.body().string());
                }
                else {
                    System.err.println("Empty Response!");
                }
            }
        }
    }
