package com.springboot.training.httpclient;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public class EmployeeApiClientDemo {

    // Client-side copies of the API's DTOs. The client only needs the JSON shape,
    // not the server's classes.
    record EmployeeResponse(Long id, String name, String email) { }
    record CreateEmployeeRequest(String name, String email) { }

    public static void main(String[] args) {

        // One reusable client. baseUrl is prepended to every uri(...) call.
        // The context path (/employee-api) is the WAR file name; adjust it to yours.
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:8080/spring-boot-training")
                .build();

        // GET one employee: GET /employees/1
        EmployeeResponse one = client.get()
                .uri("/employees/{id}", 1)              // URI template; the value is substituted
                .retrieve()                             // sends the request; throws on 4xx/5xx
                .body(EmployeeResponse.class);          // JSON converted to the record by Jackson
        System.out.println("GET one:  " + one);

        // GET the list: GET /employees
        // A generic type has no runtime class, so ParameterizedTypeReference carries it
        List<EmployeeResponse> all = client.get()
                .uri("/employees")
                .retrieve()
                .body(new ParameterizedTypeReference<List<EmployeeResponse>>() { });
        System.out.println("GET all:  " + all);

        // POST a new employee: POST /employees
        // toEntity(...) keeps the status code and headers along with the body
        ResponseEntity<EmployeeResponse> created = client.post()
                .uri("/employees")
                .contentType(MediaType.APPLICATION_JSON)                 // body is sent as JSON
                .body(new CreateEmployeeRequest("Ravi", "ravi@example.com"))
                .retrieve()
                .toEntity(EmployeeResponse.class);
        System.out.println("POST status:   " + created.getStatusCode());
        System.out.println("POST Location: " + created.getHeaders().getLocation());
        System.out.println("POST body:     " + created.getBody());

        // Error case: a 4xx response makes retrieve() throw. The exception carries
        // the status code and the response body (our ErrorResponse JSON).
        try {
            client.get()
                    .uri("/employees/{id}", 99)
                    .retrieve()
                    .body(EmployeeResponse.class);
        } catch (HttpClientErrorException ex) {
            System.out.println("Error status: " + ex.getStatusCode());
            System.out.println("Error body:   " + ex.getResponseBodyAsString());
        }
    }
}
