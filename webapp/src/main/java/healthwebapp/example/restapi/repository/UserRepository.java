package healthwebapp.example.restapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import healthwebapp.example.restapi.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Find a user by email


    Optional<User> findById(Long userId);

    // Find a user by verification token
    Optional<User> findByVerificationToken(String verificationToken);

    // Find a user by email
    Optional<User> findByEmail(String email);



    // Delete a user by user ID
    void deleteById(Long userId);// Return a
    void deleteByEmail(String email);
// n Optional<User>
}
