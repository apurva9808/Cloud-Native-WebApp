package healthwebapp.example.restapi.controller;

import healthwebapp.example.restapi.dto.UserDTO;
import healthwebapp.example.restapi.dto.UserVerificationPayload;
import healthwebapp.example.restapi.entity.User;
import healthwebapp.example.restapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Autowired
    private AmazonSNS snsClient;

    @Autowired
    private UserService userService;

    @Autowired
    private StatsDClient statsDClient;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // Create a new user
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user, BindingResult result) {
        long startTime = System.currentTimeMillis();
        logger.info("Received request to create a new user");

        // Check for validation errors
        if (result.hasErrors()) {
            logger.warn("Invalid request data for creating user: {}", result.getFieldErrors());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request data");
        }

        // Check if the email is already in use
        Optional<User> existingUser = userService.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            logger.warn("Email already in use: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already in use");
        }

        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpirationTime = LocalDateTime.now().plusMinutes(10);
        user.setVerificationToken(verificationToken);
        user.setTokenExpirationTime(tokenExpirationTime);
        user.setVerified(false);

        // Hash the password before saving
        user.setPassword(userService.encodePassword(user.getPassword()));

        // Create and save the new user
        User createdUser = userService.createUser(user);
        logger.info("New user created with ID: {}, email: {}", createdUser.getId(), createdUser.getEmail());

        try {
            // Create the payload for the verification email
            UserVerificationPayload messagePayload = new UserVerificationPayload(
                    createdUser.getId(),
                    createdUser.getEmail(),
                    verificationToken,
                    tokenExpirationTime
            );

            // Convert the payload to JSON format
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String messageJson = mapper.writeValueAsString(messagePayload);
            logger.info("Message payload for SNS: {}", messageJson);

            // Publish the message to the SNS topic
            String snsTopicArn = System.getenv("SNS_TOPIC_ARN");
            logger.info("Using SNS Topic ARN: {}", snsTopicArn);

            PublishRequest publishRequest = new PublishRequest()
                    .withTopicArn(snsTopicArn)
                    .withMessage(messageJson);

            PublishResult publishResult = snsClient.publish(publishRequest);
            logger.info("SNS message published successfully. Message ID: {}", publishResult.getMessageId());
        } catch (Exception e) {
            logger.error("Error publishing SNS message: {}", e.getMessage(), e);

            // Suggest enabling CloudWatch SNS logs for further debugging
            logger.info("If the issue persists, enable CloudWatch logging for the SNS topic.");
            logger.info("Steps: Go to the AWS SNS Console > Select your Topic > Enable CloudWatch Logs.");
            logger.info("Inspect the logs for delivery errors or message processing issues.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send verification email due to: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime("endpoint.user.create.time", endTime - startTime);
        statsDClient.incrementCounter("endpoint.user.create.count");

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/self")
    public ResponseEntity<?> deleteUser(@RequestParam("email") String email) {
        logger.info("Received request to delete user with email: {}", email);

        // Find the user by email
        Optional<User> userOptional = userService.findByEmail(email);

        if (!userOptional.isPresent()) {
            logger.warn("User not found for email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        try {
            // Delete the user
            userService.deleteUser(userOptional.get());
            logger.info("User with email {} deleted successfully", email);

            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting user with email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete user due to: " + e.getMessage());
        }
    }


    // Verify the user's email
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        Optional<User> userOptional = userService.findByVerificationToken(token);

        if (!userOptional.isPresent()) {
            logger.warn("Verification failed: Invalid token - {}", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired verification token.");
        }

        User user = userOptional.get();

        // Check if the token has expired
        if (LocalDateTime.now().isAfter(user.getTokenExpirationTime())) {
            logger.warn("Verification failed: Token expired for user ID {}", user.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Verification link has expired.");
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpirationTime(null);
        userService.updateUser(user);

        logger.info("User ID {} verified successfully", user.getId());
        return ResponseEntity.ok("Account verified successfully.");
    }

    // Get the authenticated user's details
    @GetMapping("/self")
    public ResponseEntity<UserDTO> getAuthenticatedUser() {
        long startTime = System.currentTimeMillis();
        logger.info("Received request to get authenticated user details");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt to /self endpoint");
            statsDClient.incrementCounter("endpoint.user.get.auth.error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserEmail = authentication.getName();
        Optional<User> userOptional = userService.findByEmail(currentUserEmail);

        if (userOptional.isPresent() && !userOptional.get().isVerified()) {
            logger.warn("Access blocked for unverified user: {}", currentUserEmail);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime("endpoint.user.get.time", endTime - startTime);
        statsDClient.incrementCounter("endpoint.user.get.count");

        return userOptional.map(user -> ResponseEntity.ok(userService.convertToDTO(user)))
                .orElseGet(() -> {
                    logger.warn("User not found for email: {}", currentUserEmail);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    // Update the authenticated user's details
    @PutMapping("/self")
    public ResponseEntity<?> updateAuthenticatedUser(@Valid @RequestBody User user, BindingResult result) {
        long startTime = System.currentTimeMillis();
        logger.info("Received request to update authenticated user");

        if (result.hasErrors()) {
            logger.warn("Invalid request data for updating user");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request data");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        Optional<User> existingUserOptional = userService.findByEmail(currentUserEmail);
        if (!existingUserOptional.isPresent()) {
            logger.warn("User not found for email: {}", currentUserEmail);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User existingUser = existingUserOptional.get();

        if (user.getFirstName() != null) {
            existingUser.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            existingUser.setLastName(user.getLastName());
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(userService.encodePassword(user.getPassword()));
        }

        User updatedUser = userService.updateUser(existingUser);

        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime("endpoint.user.update.time", endTime - startTime);
        statsDClient.incrementCounter("endpoint.user.update.count");

        UserDTO updatedUserDTO = userService.convertToDTO(updatedUser);
        return ResponseEntity.ok(updatedUserDTO);
    }

    // Upload or update profile picture
    @PostMapping(value = "/self/pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserDTO> uploadProfilePic(@RequestParam("profilePic") MultipartFile file) {
        long startTime = System.currentTimeMillis();
        logger.info("Received request to upload or update profile picture");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt to upload profile picture");
            statsDClient.incrementCounter("endpoint.user.pic.upload.auth.error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserEmail = authentication.getName();
        User user = userService.findByEmail(currentUserEmail).orElseThrow(() -> {
            logger.warn("User not found for email: {}", currentUserEmail);
            return new RuntimeException("User not found");
        });

        try {
            userService.uploadProfilePicture(file, user);
        } catch (IOException e) {
            logger.error("Error uploading profile picture: {}", e.getMessage());
            statsDClient.incrementCounter("endpoint.user.pic.upload.error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        UserDTO userDTO = userService.convertToDTO(user);

        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime("endpoint.user.pic.upload.time", endTime - startTime);
        statsDClient.incrementCounter("endpoint.user.pic.upload.count");

        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }

    // Delete profile picture
    @DeleteMapping("/self/pic")
    public ResponseEntity<UserDTO> deleteProfilePic() {
        long startTime = System.currentTimeMillis();
        logger.info("Received request to delete profile picture");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt to delete profile picture");
            statsDClient.incrementCounter("endpoint.user.pic.delete.auth.error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserEmail = authentication.getName();
        User user = userService.findByEmail(currentUserEmail).orElseThrow(() -> {
            logger.warn("User not found for email: {}", currentUserEmail);
            return new RuntimeException("User not found");
        });

        userService.deleteProfilePicture(user);

        UserDTO userDTO = userService.convertToDTO(user);
        userDTO.setFileName(null);
        userDTO.setUrl(null);
        userDTO.setUploadDate(null);

        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime("endpoint.user.pic.delete.time", endTime - startTime);
        statsDClient.incrementCounter("endpoint.user.pic.delete.count");

        return ResponseEntity.ok(userDTO);
    }

    // Unsupported HTTP methods for profile picture
    @RequestMapping(value = "/self/pic", method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.HEAD})
    public ResponseEntity<Void> handleUnsupportedMethods() {
        logger.info("Received unsupported HTTP method on /self/pic");
        statsDClient.incrementCounter("endpoint.user.pic.unsupported.method");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
