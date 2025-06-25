package healthwebapp.example.restapi.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDTO {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime accountCreated;

    private LocalDateTime accountUpdated;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("url")
    private String url;

    @JsonProperty("upload_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime uploadDate;

    @JsonProperty("user_id")
    private String userId;

    // Default constructor
    public UserDTO() {}

    // Full constructor matching the convertToDTO method
    public UserDTO(Long id, String email, String firstName, String lastName, LocalDateTime accountCreated, LocalDateTime accountUpdated, String fileName, String url, LocalDateTime uploadDate, String userId) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountCreated = accountCreated;
        this.accountUpdated = accountUpdated;
        this.fileName = fileName;
        this.url = url;
        this.uploadDate = uploadDate;
        this.userId = userId;
    }

    // Getters and Setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDateTime getAccountCreated() { return accountCreated; }
    public void setAccountCreated(LocalDateTime accountCreated) { this.accountCreated = accountCreated; }

    public LocalDateTime getAccountUpdated() { return accountUpdated; }
    public void setAccountUpdated(LocalDateTime accountUpdated) { this.accountUpdated = accountUpdated; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
