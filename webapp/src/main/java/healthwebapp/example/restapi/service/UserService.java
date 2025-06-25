package healthwebapp.example.restapi.service;

import healthwebapp.example.restapi.dto.UserDTO;
import healthwebapp.example.restapi.entity.User;
import healthwebapp.example.restapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final CustomS3Service s3Service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(CustomS3Service s3Service) {
        this.s3Service = s3Service;
    }

    // Method to find user by email (for authentication or user retrieval)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Method to create a new user (handling registration)
    public User createUser(User user) {
        user.setAccountCreated(LocalDateTime.now());
        user.setAccountUpdated(LocalDateTime.now());

        return userRepository.save(user);
    }

    // Method to update an existing user
    public User updateUser(User user) {
        user.setAccountUpdated(LocalDateTime.now());
        return userRepository.save(user);
    }

    // Method to update user password
    public User updatePassword(User user, String newPassword) {
        user.setPassword(encodePassword(newPassword));
        user.setAccountUpdated(LocalDateTime.now());
        return userRepository.save(user);
    }

    // Method to hash password
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    // Convert User entity to UserDTO for API responses
    public UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountCreated(),
                user.getAccountUpdated(),
                user.getProfilePicUrl(), // Assuming 'profilePicUrl' in User maps to 'file_name' in UserDTO
                user.getProfilePicUrl(), // Assuming 'profilePicUrl' also represents 'url'
                user.getProfilePicUploadDate(), // Mapping upload date
                user.getId().toString() // Converting Long ID to String for 'user_id'
        );
    }

    // Upload profile picture and update the User entity
    public void uploadProfilePicture(MultipartFile file, User user) throws IOException {
        UUID userIdAsUUID = UUID.nameUUIDFromBytes(user.getId().toString().getBytes());
        // Upload to S3 and get the file key
        String key = s3Service.uploadFile(file, userIdAsUUID);

        user.setPfpKey(key);

        // Set the S3 URL
        String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", s3Service.getBucketName(), key);
        user.setProfilePicUrl(fileUrl);
        user.setProfilePicUploadDate(LocalDateTime.now());

        userRepository.save(user);
    }

    // Delete profile picture
    public void deleteProfilePicture(User user) {
        if (user.getProfilePicUrl() != null) {
            // Delete the profile picture from S3
            s3Service.deleteFile(user.getPfpKey());

            user.setProfilePicUrl(null);
            user.setProfilePicUploadDate(null);
            userRepository.save(user);
        }
    }

    // Convert UserDTO to User entity for request processing
    public User convertToEntity(UserDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        return user;
    }

    public Optional<User> findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token);
    }

    // Method to delete a user
    public void deleteUser(User user) {
        // If the user has a profile picture, delete it from S3
        deleteProfilePicture(user);

        // Remove the user record from the database
        userRepository.delete(user);
    }
}
