package healthwebapp.example.restapi.controller;

import healthwebapp.example.restapi.dto.UserDTO;
import healthwebapp.example.restapi.entity.User;
import healthwebapp.example.restapi.service.UserService;
import com.timgroup.statsd.StatsDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private StatsDClient statsDClient;  // Mock StatsDClient for metrics tracking

    @Mock
    private BindingResult bindingResult;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private UserController userController;

    private UserDTO userDTO;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userDTO = new UserDTO(
                null,
                "golu.doe@example.com",
                "golu",
                "Doe",
                null,
                null,
                null,
                null,
                null,
                null
        );

        user = new User();
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPassword("encodedPassword123");
    }

    @Test
    void testUploadProfilePicUnauthorized() throws IOException {
        // Simulate no user found (unauthorized access)
        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());

        // Call the method under test
        ResponseEntity<UserDTO> responseEntity = userController.uploadProfilePic(file);

        // Verify the response and mock interactions
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        verify(userService, never()).uploadProfilePicture(any(MultipartFile.class), any(User.class));
        verify(statsDClient, times(1)).incrementCounter("endpoint.user.pic.upload.auth.error");
    }

    // Example of uncommenting additional tests and ensuring they have proper mock setup.

//    @Test
//    void testGetAuthenticatedUserSuccess() {
//        when(userService.findByEmail(anyString())).thenReturn(Optional.of(user));
//        when(userService.convertToDTO(user)).thenReturn(userDTO);
//
//        ResponseEntity<UserDTO> responseEntity = userController.getAuthenticatedUser();
//
//        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
//        assertEquals(userDTO, responseEntity.getBody());
//        verify(userService, times(1)).findByEmail(anyString());
//    }

//    @Test
//    void testGetAuthenticatedUserUnauthorized() {
//        when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
//
//        ResponseEntity<UserDTO> responseEntity = userController.getAuthenticatedUser();
//
//        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
//        verify(userService, times(1)).findByEmail(anyString());
//    }

//    @Test
//    void testUploadProfilePicSuccess() throws IOException {
//        when(userService.findByEmail(anyString())).thenReturn(Optional.of(user));
//        when(userService.convertToDTO(user)).thenReturn(userDTO);
//
//        ResponseEntity<UserDTO> responseEntity = userController.uploadProfilePic(file);
//
//        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
//        verify(userService, times(1)).uploadProfilePicture(file, user);
//    }
//
//    @Test
//    void testDeleteProfilePicSuccess() {
//        when(userService.findByEmail(anyString())).thenReturn(Optional.of(user));
//        when(userService.convertToDTO(user)).thenReturn(userDTO);
//
//        ResponseEntity<UserDTO> responseEntity = userController.deleteProfilePic();
//
//        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
//        assertEquals(userDTO, responseEntity.getBody());
//        verify(userService, times(1)).deleteProfilePicture(user);
//    }
}
