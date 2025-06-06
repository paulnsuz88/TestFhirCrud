import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class TestFhirCrud {
    static final String limitOption = "&_count=50";
    static final String miscOptions = limitOption;
    static final String acceptHeaderValue = "application/fhir+json";
    static Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    static OkHttpClient httpClient = new OkHttpClient();
    static Scanner sysInScanner = new Scanner(System.in);
    static String baseFhirServerUrl;
    static String fullFhirServerUrl;

    public static void main(String[] args) throws IOException {
        final String fhirSvrMenuStr = """
Which FHIR server do you want to test with?:
    h) HAPI FHIR base R4
    w) WildFHIR (aegis.net) R4
Enter your choice (h or w):
""";
        final String mainMenuStr = """
Which CRUD action do you want to test?:
    c) CREATE a Patient resource
    r) READ a Patient resource
    u) UPDATE a Patient resource
    d) DELETE a Patient resource
    q) QUIT
Enter your choice (c,r,u,d or q):
""";
        char mainChoice;
        // Ask which FHIR server to use
        svrMenuLoop:
        while (true) {
            // Display the FHIR Server menu
            System.out.print(fhirSvrMenuStr);
            // Only look at the first character of the response, converted to lower case
            mainChoice = Character.toLowerCase(sysInScanner.next().charAt(0));

            // Perform the chosen MAIN menu action
            switch (mainChoice) {
                case 'h':
                    baseFhirServerUrl = "http://hapi.fhir.org/baseR4";
                    break svrMenuLoop;
                case 'w':
                    baseFhirServerUrl = "https://wildfhir.aegis.net/r4";
                    break svrMenuLoop;
                default:
                    System.out.println("Invalid FHIR Server choice, please try again.");
                    break;
            }
        }
        System.out.println("baseFhirServerUrl = " + baseFhirServerUrl);
        // Ask which CRUD operation the user wants to execute
        do {
            // Display the MAIN menu
            System.out.print(mainMenuStr);
            // Only look at the first character of the response, converted to lower case
            mainChoice = Character.toLowerCase(sysInScanner.next().charAt(0));
            // Perform the chosen MAIN menu action
            switch (mainChoice) {
                case 'c':
                    if (!createFhir()) continue;
                    break;
                case 'r':
                    if (!readFhir()) continue;
                    break;
                case 'u':
                    if (!updateFhir()) continue;
                    break;
                case 'd':
                    if (!deleteFhir()) continue;
                    break;
                case 'q':
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid CRUD Action choice, please try again.");
            }
        } while (mainChoice != 'q');
        sysInScanner.close();
    }

    static boolean createFhir() throws IOException {
        final String createGenderMenuStr = """
Gender?:
    m) male
    f) female
    o) other
    u) unknown
""";
        // Collect GIVEN NAME
        System.out.print("Given Name?: ");
        String givenName = sysInScanner.next();
        // Collect FAMILY NAME
        System.out.print("Family Name?: ");
        String familyName = sysInScanner.next();
        // Collect GENDER
        System.out.print(createGenderMenuStr);
        // Only look at the first character of the response, converted to lower case
        char genderChoice = Character.toLowerCase(sysInScanner.next().charAt(0));
        String genderStr;
        // Perform the chosen MAIN menu action
        genderSelectionLoop:
        while (true) {
            switch (genderChoice) {
                case 'm':
                    genderStr = "male";
                    break genderSelectionLoop;
                case 'f':
                    genderStr = "female";
                    break genderSelectionLoop;
                case 'o':
                    genderStr = "other";
                    break genderSelectionLoop;
                case 'u':
                    genderStr = "unknown";
                    break genderSelectionLoop;
                default:
                    System.out.println("Invalid FHIR gender choice, please try again.");
                    break;
            }
        }
        System.out.println("gender = " + genderStr);
        // Collect DATE OF BIRTH
        String birthDate;
        while (true) {
            System.out.print("Birthdate (YYYY-MM-DD)?: ");
            //String birthDate = "2001-01-01";
            if (!sysInScanner.hasNext(datePattern)) {
                // read in badly-formatted date and throw it away
                sysInScanner.next();
                System.out.println("Invalid date string...");
                continue;
            }
            birthDate = sysInScanner.next();
            break;
        }
        fullFhirServerUrl = baseFhirServerUrl + "/Patient";
        final String postBodyFormatStr = """
            {
                "resourceType": "Patient",
                "name": [
                    {
                        "family": "%s",
                        "given": [
                            "%s"
                        ]
                    }
                ],
                "gender": "%s",
                "birthDate": "%s"
            }""";
        System.out.println("Making HTTPS REST (POST) call with this URL:");
        System.out.println(fullFhirServerUrl);
        String postBodyStr = String.format(postBodyFormatStr, familyName, givenName, genderStr, birthDate);
        System.out.println("and this BODY:");
        System.out.println(postBodyStr);
        RequestBody postBody = RequestBody.create(postBodyStr, MediaType.parse("application/fhir+json"));
        Request request = new Request.Builder()
                .url(fullFhirServerUrl)
                .post(postBody)
                .addHeader("Accept", acceptHeaderValue)
                .build();

        String responseCode;
        String responseMsg;
        String responseBodyStr = null;
        try (Response response = httpClient.newCall(request).execute()) {
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
            // Print resourceType and id from response
            String resourceType = rootNode.path("resourceType").asText();
            String resourceId = rootNode.path("id").asText();
            System.out.println("resourceType = " + resourceType);
            if (!resourceId.isEmpty()) System.out.println("id = " + resourceId);
            // Locate the human-readable XHTML "text" field if it exists in the response
            String xhtmlTextDiv = rootNode.path("text").path("div").toString();
            Document xhtmlDoc = Jsoup.parse(xhtmlTextDiv, "", Parser.xmlParser());
            String textDivStr = xhtmlDoc.text().replace("\\n", "\n");
            // Print the human-readable message as plain text
            if (!textDivStr.isEmpty()) {
                System.out.println("text.div value from Response (intended to be human-readable):");
                System.out.println(textDivStr);
            }
        }
        return true;
    }
    static boolean readFhir() throws IOException {
        final String readFhirMenuStr = """
How would you like to specify the Patient?:");
    i) Enter the numeric FHIR ID of the Patient resource");
    g) Search for Patient(s) by exact GIVEN name");
    f) Search for Patient(s) by exact FAMILY name");
    b) Search for Patient(s) by exact BIRTHDATE");
Enter your choice (i,g,f or b): ");
""";
        // Display the READ sub-menu
        System.out.print(readFhirMenuStr);
        // Only look at the first character of the response, converted to lower case
        char subChoice = Character.toLowerCase(sysInScanner.next().charAt(0));

        // Perform the chosen READ sub-menu action
        switch (subChoice) {
            case 'i':
                System.out.print("ID?: ");
                //String patientID = "596742";
                //String patientID = "3a3260fd008144899ce7503cd9794341";
                String patientID = sysInScanner.next();
                System.out.println("Retrieving Patient resource with ID=" + patientID + " ...");
                fullFhirServerUrl = baseFhirServerUrl + "/Patient/" + patientID;
                break;
            case 'g':
                System.out.print("Given Name?: ");
                //String givenName = "Ana";
                String givenName = sysInScanner.next();
                System.out.println("Retrieving Patient resource(s) with GIVEN name of " + givenName + " ...");
                fullFhirServerUrl = baseFhirServerUrl + "/Patient?given:exact=" + givenName + miscOptions;
                break;
            case 'f':
                System.out.print("Family Name?: ");
                //String familyName = "Methaila";
                String familyName = sysInScanner.next();
                System.out.println("Retrieving Patient resource(s) with FAMILY name of " + familyName + " ...");
                fullFhirServerUrl = baseFhirServerUrl + "/Patient?family:exact=" + familyName + miscOptions;
                break;
            case 'b':
                String birthDate;
                while (true) {
                    System.out.print("Birthdate (YYYY-MM-DD)?: ");
                    //String birthDate = "2001-01-01";
                    if (!sysInScanner.hasNext(datePattern)) {
                        sysInScanner.next();
                        System.out.println("Invalid date string...");
                        continue;
                    }
                    birthDate = sysInScanner.next();
                    break;
                }
                System.out.println("Retrieving Patient resource(s) with BIRTHDATE of " + birthDate + " ...");
                //fullFhirServerUrl = baseFhirServerUrl + "/Patient?birthdate:exact=" + birthDate + miscOptions;
                fullFhirServerUrl = baseFhirServerUrl + "/Patient?birthdate=" + birthDate + miscOptions;
                break;
            default:
                System.out.println("Invalid choice - returning to Main Menu...");
                return false;
        }
        int callCounter = 1;
        System.out.println("Making HTTPS REST (GET) call #" + callCounter + " with URL:");
        System.out.println(fullFhirServerUrl);
        Request request = new Request.Builder()
                .url(fullFhirServerUrl)
                .addHeader("Accept", acceptHeaderValue)
                .build();

        String responseCode;
        String responseMsg;
        String responseBodyStr = null;
        try (Response response = httpClient.newCall(request).execute()) {
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
            // Print resourceType and id from response
            String resourceType = rootNode.path("resourceType").asText();
            String resourceId = rootNode.path("id").asText();
            System.out.println("resourceType = " + resourceType);
            if (!resourceId.isEmpty()) System.out.println("id = " + resourceId);
            // Locate the human-readable XHTML "text" field if it exists in the response
            String xhtmlTextDiv = rootNode.path("text").path("div").toString();
            Document xhtmlDoc = Jsoup.parse(xhtmlTextDiv, "", Parser.xmlParser());
            String textDivStr = xhtmlDoc.text().replace("\\n", "\n");
            // Print the human-readable message as plain text
            if (!textDivStr.isEmpty()) {
                System.out.println("text.div value from Response (intended to be human-readable):");
                System.out.println(textDivStr);
            }
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

                    System.out.println("Making HTTPS REST (GET) call #" + (++callCounter) + " with URL:");
                    System.out.println(nextUrl);
                    System.out.println("to get additional paged results...");
                    responseBodyStr = null;
                    try (Response innerResponse = httpClient.newCall(nextRequest).execute()) {
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
        return true;
    }

    static boolean updateFhir() throws IOException {
        System.out.println("Sorry! UPDATE not implemented yet!");
        return false;
    }
    static boolean deleteFhir() throws IOException {
        System.out.println("Sorry! DELETE not implemented yet!");
        return false;
    }
}