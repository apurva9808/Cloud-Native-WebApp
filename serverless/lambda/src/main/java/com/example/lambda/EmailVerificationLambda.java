package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

public class EmailVerificationLambda {

    public String handleRequest(SNSEvent snsEvent, Context context) {
        context.getLogger().log("Lambda function invoked with SNS event: " + snsEvent);

        try {
            // Validate SNS event records
            if (snsEvent.getRecords() == null || snsEvent.getRecords().isEmpty()) {
                context.getLogger().log("Error: SNS event does not contain any records.");
                throw new RuntimeException("SNS event does not contain any records.");
            }

            // Extract and log the first SNS record
            context.getLogger().log("Extracting first SNS record...");
            SNSEvent.SNSRecord snsRecord = snsEvent.getRecords().get(0);
            String message = snsRecord.getSNS().getMessage();
            context.getLogger().log("Received SNS message: " + message);

            // Parse the SNS message
            context.getLogger().log("Parsing SNS message...");
            JSONObject messageJson = new JSONObject(message);
            String email = messageJson.getString("email");
            String verificationToken = messageJson.getString("verificationToken");
            String firstName = messageJson.optString("firstName", "User");

            context.getLogger().log("Parsed SNS message - Email: " + email +
                    ", Verification Token: " + verificationToken +
                    ", First Name: " + firstName);

            // Fetch the SendGrid API key and sender email from Secrets Manager
            context.getLogger().log("Fetching secrets from AWS Secrets Manager...");
            String secretName = System.getenv("SECRETS_MANAGER_NAME");
            JSONObject secretsJson = new JSONObject(getSecretValue(secretName, context));
            String apiKey = secretsJson.getString("sendgrid_api_key");
            String senderEmail = secretsJson.getString("sender_email");

            // Fetch the verification endpoint environment variable
            context.getLogger().log("Fetching VERIFY_ENDPOINT environment variable...");
            String verifyEndpoint = System.getenv("VERIFY_ENDPOINT");
            if (verifyEndpoint == null) {
                context.getLogger().log("Error: VERIFY_ENDPOINT environment variable is not set.");
                throw new RuntimeException("Environment variable VERIFY_ENDPOINT is not set");
            }
            context.getLogger().log("VERIFY_ENDPOINT: " + verifyEndpoint);

            // Generate and log the verification link
            context.getLogger().log("Generating verification link...");
            String verificationLink = String.format("%s?email=%s&token=%s", verifyEndpoint, email, verificationToken);
            context.getLogger().log("Generated verification link: " + verificationLink);

            // Log sending email step
            context.getLogger().log("Calling sendVerificationEmail() to send the verification email...");
            sendVerificationEmail(context, firstName, email, verificationLink, apiKey, senderEmail);

            // Log successful email sending
            context.getLogger().log("Email sent successfully to: " + email);
            return "Email sent successfully!";
        } catch (Exception e) {
            context.getLogger().log("Error processing SNS event: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendVerificationEmail(Context context, String firstName, String toEmail, String verificationLink,
                                       String apiKey, String senderEmail) throws Exception {
        context.getLogger().log("Preparing to send verification email...");

        // Log email content preparation
        context.getLogger().log("Preparing email content...");
        String subject = "Verify Your Email Address";
        String body = String.format(
                "Hi %s,\n\nPlease verify your email address by clicking the link below:\n%s\n\nThank you!",
                firstName, verificationLink
        );
        context.getLogger().log("Email Subject: " + subject);
        context.getLogger().log("Email Body: " + body);

        // Prepare and log the JSON payload for SendGrid API
        context.getLogger().log("Creating email payload for SendGrid API...");
        JSONObject emailPayload = new JSONObject();
        emailPayload.put("personalizations", new JSONArray()
                .put(new JSONObject()
                        .put("to", new JSONArray()
                                .put(new JSONObject().put("email", toEmail)))));
        emailPayload.put("from", new JSONObject().put("email", senderEmail));
        emailPayload.put("subject", subject);
        emailPayload.put("content", new JSONArray()
                .put(new JSONObject()
                        .put("type", "text/plain")
                        .put("value", body)));

        context.getLogger().log("Email Payload: " + emailPayload.toString());

        // Send email using SendGrid API
        context.getLogger().log("Connecting to SendGrid API...");
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.sendgrid.com/v3/mail/send").openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = emailPayload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
                context.getLogger().log("Email request sent to SendGrid.");
            }

            // Log response code
            int responseCode = connection.getResponseCode();
            context.getLogger().log("SendGrid API response code: " + responseCode);

            // Check for errors and log details
            if (responseCode >= 400) {
                context.getLogger().log("Error: Failed to send email. HTTP " + responseCode);
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                context.getLogger().log("SendGrid API Error Response: " + errorResponse.toString());
                throw new RuntimeException("Failed to send email: HTTP " + responseCode + " - " + errorResponse.toString());
            }

            context.getLogger().log("Verification email sent successfully!");
        } catch (Exception e) {
            context.getLogger().log("Error while sending email via SendGrid: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String getSecretValue(String secretName, Context context) {
        try (SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.US_EAST_1)
                .build()) {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            return getSecretValueResponse.secretString();
        } catch (SecretsManagerException e) {
            context.getLogger().log("Error retrieving secret: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to retrieve secret: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
