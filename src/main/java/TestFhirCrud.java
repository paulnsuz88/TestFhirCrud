import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class TestFhirCrud {
    public static void main(String[] args) throws IOException {

        OkHttpClient client = new OkHttpClient();
        final String baseFhirServerUrl = "http://hapi.fhir.org/baseR4";
        final String formatOptions = "&_format=json&_pretty=true";
        final String limitOption = "&_count=50";
        String fullFhirServerUrl;

        // Ask which CRUD operation the user wants to execute
        Scanner scanner = new Scanner(System.in);
        char mainChoice;

        do {
            // Display the MAIN menu
            System.out.println("\nWhich CRUD action do you want to test?:");
            System.out.println("c) CREATE a Patient resource");
            System.out.println("r) READ a Patient resource");
            System.out.println("u) UPDATE a Patient resource");
            System.out.println("d) DELETE a Patient resource");
            System.out.println("q) QUIT");
            System.out.print("Enter your choice (c,r,u,d or q): ");

            mainChoice = Character.toLowerCase(scanner.next().charAt(0)); // Normalize input

            // Perform the chosen MAIN menu action
            switch (mainChoice) {
                case 'c':
                    System.out.println("Sorry! CREATE not implemented yet!");
                    break;
                case 'r':
                    // Display the READ sub-menu
                    System.out.println("\nHow would you like to specify the Patient?:");
                    System.out.println("i) Enter the numeric FHIR ID of the Patient resource");
                    System.out.println("g) Search for Patient(s) by exact GIVEN name");
                    System.out.println("f) Search for Patient(s) by exact FAMILY name");
                    System.out.println("b) Search for Patient(s) by exact BIRTHDATE");
                    System.out.print("Enter your choice (i,g,f or b): ");

                    char subChoice = Character.toLowerCase(scanner.next().charAt(0)); // Normalize input

                    // Perform the chosen READ sub-menu action
                    switch (subChoice) {
                        case 'i':
                            System.out.print("ID?: ");
                            //int patientID = 596742;
                            String patientID = scanner.next();
                            System.out.println("Retrieving Patient resource with ID=" + patientID + " ...");
                            fullFhirServerUrl = baseFhirServerUrl + "/Patient/" + patientID;
                            break;
                        case 'g':
                            System.out.print("Given Name?: ");
                            //String givenName = "Ana";
                            String givenName = scanner.next();
                            System.out.println("Retrieving Patient resource(s) with GIVEN name of " + givenName + " ...");
                            fullFhirServerUrl = baseFhirServerUrl + "/Patient?given:exact=" + givenName + limitOption + formatOptions;
                            break;
                        case 'f':
                            System.out.print("Family Name?: ");
                            //String familyName = "Methaila";
                            String familyName = scanner.next();
                            System.out.println("Retrieving Patient resource(s) with FAMILY name of " + familyName + " ...");
                            fullFhirServerUrl = baseFhirServerUrl + "/Patient?family:exact=" + familyName + limitOption + formatOptions;
                            break;
                        case 'b':
                            System.out.print("Birthdate (YYYY-MM-DD)?: ");
                            //String birthDate = "2001-01-01";
                            Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                            if (scanner.hasNext(pattern) == false) {
                                scanner.next();
                                System.out.println("Invalid date string - returning to Main Menu...");
                                continue;
                            }
                            String birthDate = scanner.next();
                            System.out.println("Retrieving Patient resource(s) with BIRTHDATE of " + birthDate + " ...");
                            fullFhirServerUrl = baseFhirServerUrl + "/Patient?birthdate:exact=" + birthDate + limitOption + formatOptions;
                            break;
                        default:
                            System.out.println("Invalid choice - returning to Main Menu...");
                            continue;
                    }
                    int callCounter = 1;
                    System.out.println("Making HTTPS REST (READ) call #" + callCounter + " with URL:");
                    System.out.println(fullFhirServerUrl);
                    Request request = new Request.Builder()
                            .url(fullFhirServerUrl)
                            .build();

                    String responseCode;
                    String responseMsg;
                    String responseBodyStr = null;
                    try (Response response = client.newCall(request).execute()) {
                        responseCode = String.valueOf(response.code());
                        responseMsg = response.message();
                        System.out.println("\nHTTP Status return code: " + responseCode + " (" + responseMsg + ")");
                        var responseBody = response.body();
                        if (responseBody != null) {
                            responseBodyStr = responseBody.string();
                        }
                    }
                    if (responseBodyStr != null) {
                        //System.out.println("Response body returned:");
                        //System.out.println(responseBodyStr);
                        // parse id value from response JSON, using Jackson
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(responseBodyStr);
                        String resourceType = rootNode.path("resourceType").asText();
                        String resourceId = rootNode.path("id").asText();
                        System.out.println("Extracted values:");
                        System.out.println("resourceType = " + resourceType);
                        System.out.println("id = " + resourceId);
                        if (resourceType.equals("Bundle")) {
                            JsonNode currentPagedResult = rootNode;
                            // Loop through multiple paged results, if necessary
                            // to find out how many total entries are in the bundle
                            while (currentPagedResult != null) {
                                // Loop through list of links, to find the one with a relation of "next"
                                String nextUrl = null;
                                for (JsonNode thisNode : currentPagedResult.get("link")) {
                                    if (thisNode.path("relation").asText().equals("next")) {
                                        nextUrl = thisNode.path("url").asText();
                                        break;
                                    }
                                }
                                if (nextUrl == null) {
                                    // if there is no relation -> next node, then this is the last paged result
                                    System.out.println("Traversed to last paged result of the bundle");
                                    int totalEntryCount = currentPagedResult.path("total").asInt();
                                    System.out.println("Found a total of " + totalEntryCount + " entries in the entire Bundle");
                                    // break out of inner loop
                                    break;
                                }
                                // else, if there is a next URL, get the next paged result
                                Request nextRequest = new Request.Builder()
                                        .url(nextUrl)
                                        .build();

                                System.out.println("Making HTTPS REST (READ) call #" + (++callCounter) + " with URL:");
                                System.out.println(nextUrl);
                                System.out.println("to get additional paged results...");
                                responseCode = null;
                                responseMsg = null;
                                responseBodyStr = null;
                                try (Response innerResponse = client.newCall(nextRequest).execute()) {
                                    responseCode = String.valueOf(innerResponse.code());
                                    responseMsg = innerResponse.message();
                                    System.out.println("\nHTTP Status return code: " + responseCode + " (" + responseMsg + ")");
                                    var responseBody = innerResponse.body();
                                    if (responseBody != null) {
                                        responseBodyStr = responseBody.string();
                                    }
                                }
                                ObjectMapper innerObjectMapper = new ObjectMapper();
                                currentPagedResult = innerObjectMapper.readTree(responseBodyStr);
                            }
                        }
                    }
                    break;
                case 'u':
                    System.out.println("Sorry! UPDATE not implemented yet!");
                    break;
                case 'd':
                    System.out.println("Sorry! DELETE not implemented yet!");
                    break;
                case 'q':
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        } while (mainChoice != 'q');
        scanner.close();
    }
}