package healthwebapp.example.restapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class HealthCheckController {

    @GetMapping("/cicd")
    public ResponseEntity<Void> healthCheck() {
        // Check the database connection or other health indicators
        return ResponseEntity.ok().build();
    }

    // Handle HEAD requests for /healthz by returning 200 OK with no body
//    @RequestMapping(value = "/healthz", method = RequestMethod.HEAD)
//    public ResponseEntity<Void> headRequest() {
//        return ResponseEntity.ok().build();
//    }
//
//    // Handle OPTIONS requests for /healthz by returning 200 OK with no body
//    @RequestMapping(value = "/healthz", method = RequestMethod.OPTIONS)
//    public ResponseEntity<Void> optionsRequest() {
//        return ResponseEntity.ok().build();
//    }

    // Catch-all method to return 405 for unsupported methods like POST, PUT, DELETE, etc.
    @RequestMapping(value = "/healthz", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.HEAD})
    public ResponseEntity<Void> unsupportedMethod() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
