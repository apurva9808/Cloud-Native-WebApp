package healthwebapp.example.restapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthwebapp.example.restapi.dto.UserDTO;
import healthwebapp.example.restapi.entity.User;
import healthwebapp.example.restapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;


import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Add this line
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        // Clean up any existing data in the repository
        userRepository.deleteAll();

        // Set up a user entity and save it in the repository
        user = new User();
        user.setEmail("golu.doe@example.com");
        user.setFirstName("golu");
        user.setLastName("Doe");

        // Encode the password using BCryptPasswordEncoder
        user.setPassword(new BCryptPasswordEncoder().encode("password123"));

        userRepository.save(user);

        // Set up the DTO object used in test cases
//
        userDTO = new UserDTO(
                null, // id
                "golu.doe@example.com", // email
                "golu", // firstName
                "Doe", // lastName
                null, // accountCreated
                null, // accountUpdated
                null, // fileName (profile picture filename)
                null, // url (profile picture URL)
                null, // uploadDate
                null  // userId
        );
    }

    // Test for updating user successfully
//   ((later))

//    // Test for user not found when trying to update
//    @Test
//    void testUpdateUserNotFound() throws Exception {
//        // Delete all users so the user is not found in the test
//        userRepository.deleteAll();
//
//        // Perform the PUT request and expect 404 Not Found
//        mockMvc.perform(put("/v1/user/self")
//                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("golu.doe@example.com:password123".getBytes()))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userDTO)))
//                .andExpect(status().isNotFound())
//                .andExpect(content().string("User not found"));
//    }

    // Test for retrieving the authenticated user's details
//    @Test
//    void testGetAuthenticatedUserSuccess() throws Exception {
//        // Perform the GET request with Basic Authentication credentials
//        mockMvc.perform(get("/v1/user/self")
//                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("golu.doe@example.com:password123".getBytes()))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value("golu.doe@example.com"));
//    }

    // Test for retrieving user when not found
//    @Test
//    void testGetAuthenticatedUserNotFound() throws Exception {
//        // Delete all users so the user is not found in the test
//        userRepository.deleteAll();
//
//        // Perform the GET request and expect 404 Not Found
//        mockMvc.perform(get("/v1/user/self")
//                        .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("golu.doe@example.com:password123".getBytes()))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound())
//                .andExpect(content().string("User not found"));
//    }

    // Test for creating a new user successfully
//    @Test
//    void testCreateUserSuccess() throws Exception {
//        UserDTO newUserDTO = new UserDTO("jane.doe@example.com", "Jane", "Doe", "password123");
//
//        // Perform the POST request to create a new user
//        mockMvc.perform(post("/v1/user")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newUserDTO)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));
//    }

    // Test for user creation failure when the email already exists
//    @Test
//    void testCreateUserEmailExists() throws Exception {
//        // Perform the POST request and expect 400 Bad Request due to existing email
//        mockMvc.perform(post("/v1/user")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(userDTO)))
//                .andExpect(status().isNotFound())
//                ;
//    }
}
