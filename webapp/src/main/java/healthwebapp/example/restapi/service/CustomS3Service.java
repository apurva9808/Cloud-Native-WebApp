package healthwebapp.example.restapi.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service
public class CustomS3Service {

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public CustomS3Service(@Value("${aws.s3.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        // Create S3 client with default credentials and region
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1) // Specify your desired region
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    public String uploadFile(MultipartFile file, UUID userId) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Invalid file type. Only PNG, JPG, and JPEG are allowed");
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = String.format("users/%s/profile-picture-%s%s",
                userId,
                UUID.randomUUID().toString(),
                fileExtension);

        // Set metadata for the file
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(file.getSize());

        // Upload the file
        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, key, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.Private); // Set file permissions
            s3Client.putObject(request);
        } catch (AmazonServiceException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getErrorMessage(), e);
        }

        return key;
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, key);
            s3Client.deleteObject(deleteObjectRequest);
        } catch (AmazonServiceException e) {
            throw new RuntimeException("Failed to delete file from S3: " + e.getErrorMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return ".jpg";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        return (lastDotIndex == -1) ? ".jpg" : filename.substring(lastDotIndex);
    }

    public String getBucketName() {
        return this.bucketName;
    }
}
