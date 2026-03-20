package com.github.osodevops.akka.openapi.core.fixtures;

import com.github.osodevops.akka.openapi.annotations.*;

import java.util.List;

/**
 * Test fixture: endpoint annotated with all OpenAPI annotations.
 * Uses mock Akka annotations (MockHttpEndpoint/MockGet/MockPost etc.)
 * plus real OpenAPI annotations.
 */
@MockHttpEndpoint("/api/v1/customers")
@OpenAPITag(name = "Customers", description = "Customer management operations",
    externalDocsUrl = "https://docs.example.com/customers",
    externalDocsDescription = "Customer API Guide")
@OpenAPIInfo(
    title = "Customer API",
    version = "2.0.0",
    description = "API for managing customer records",
    termsOfService = "https://example.com/terms",
    contactName = "API Support",
    contactEmail = "support@example.com",
    contactUrl = "https://example.com/support",
    licenseName = "Apache 2.0",
    licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
)
@OpenAPIServer(url = "https://api.example.com", description = "Production")
@OpenAPIServer(url = "https://api-staging.example.com", description = "Staging")
public class AnnotatedEndpoint {

    @MockGet
    @OpenAPIResponse(status = "200", description = "Successfully retrieved customer list")
    public List<CustomerDto> listCustomers() {
        return List.of();
    }

    @MockGet("/{id}")
    @OpenAPIResponse(status = "200", description = "Customer found")
    @OpenAPIResponse(status = "404", description = "Customer not found")
    public CustomerDto getCustomer(String id) {
        return new CustomerDto();
    }

    @MockPost
    @OpenAPIResponse(status = "201", description = "Customer created successfully")
    @OpenAPIResponse(status = "400", description = "Invalid request data")
    @OpenAPIResponse(status = "409", description = "Customer with this email already exists")
    @OpenAPIExample(name = "newCustomer", summary = "Create a new customer",
        value = "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}")
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        return new CustomerDto();
    }

    @MockDelete("/{id}")
    @OpenAPIResponse(status = "204", description = "Customer deleted successfully")
    @OpenAPIResponse(status = "404", description = "Customer not found")
    public void deleteCustomer(String id) {
        // no-op
    }
}
