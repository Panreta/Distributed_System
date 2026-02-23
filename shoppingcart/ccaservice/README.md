# Credit Card Authorizer

Credit Card Authorizer (CCA) is a Spring Boot service that simulates a payment gateway. It exposes a synchronous HTTP API to validate credit card formats and mock payment authorization decisions for the Shopping Cart Service (SCS).

## Architecture

Plaintext

	ALB ──HTTP──▶  SCS  ── HTTP ──▶  CCA (Authorize)
	                                  │
	                                  └── (Mock Decision)

## What It Does

- Exposes a simple HTTP health endpoint for AWS Application Load Balancers (ALB).
- Accepts authorization requests containing a credit card number.
- Validates the credit card format using a strict regex pattern (`^\d{4}-\d{4}-\d{4}-\d{4}$`).
- Simulates payment processing by returning a randomized decision (90% authorization success rate, 10% decline rate).

## API Endpoints

Base path: `/credit-card` (CreditCardController)

### 1) Health Check (for ALB)

- `GET /credit-card/health`
- Response: `200 OK`, `text/plain` body: `OK`

### 2) Authorize

- `POST /credit-card/authorize`
- Consumes: `application/json`
- Produces: `text/plain`

#### Request (example)

JSON

	{
	  "cardNumber": "1234-5678-1234-5678"
	}

#### Responses

- `200 OK` : `Authorized` (Format is valid and payment is approved)
- `402 Payment Required` : `Declined` (Format is valid but payment is randomly declined)
- `400 Bad Request` : `Invalid card format` (Card number is null, empty, or fails regex validation)

## Tech Stack

- Java 17
- Spring Boot
- Maven

## Project Structure

Plaintext

	ccaservice/
	├── src/main/java/com/cs6650/ccaservice/
	│   ├── controller/
	│   │   └── CreditCardController.java    # Entry point for health check and authorization APIs
	│   ├── dto/
	│   │   └── CardRequest.java             # Data Transfer Object for incoming authorization requests
	│   └── CcaServiceApplication.java       # Application bootstrap class
	├── src/main/resources/
	│   └── application.properties           # Environment configuration
	└── pom.xml                              # Dependency management

## Configuration (`application.properties`)

This service requires minimal configuration. Environment variables can override default properties.

### Server

| **Property**  | **Env Override** | **Default** | **Notes**           |
| ------------- | ---------------- | ----------- | ------------------- |
| `server.port` | `CCA_PORT`       | `8080`      | Port used by Tomcat |

## Local Development

### Prerequisites

- Java 17
- Maven

### Run CCA

Navigate to the project root directory and execute:

	mvn clean package -DskipTests
	mvn spring-boot:run

Default base URL: `http://localhost:8080`

### Test

**Health Check:**

	curl -i http://localhost:8080/credit-card/health

**Valid Authorization (Will return 200 or 402):**

	curl -i -X POST "http://localhost:8080/credit-card/authorize" \
	  -H "Content-Type: application/json" \
	  -d '{"cardNumber": "1234-5678-1234-5678"}'

**Invalid Format (Will return 400):** 

	curl -i -X POST "http://localhost:8080/credit-card/authorize" \
	  -H "Content-Type: application/json" \
	  -d '{"cardNumber": "1234567812345678"}'

### Overriding config with environment variables

	export CCA_PORT=8080
	mvn spring-boot:run

## Team Checklist

- **Deployment URL**: Ensure the deployed CCA instance's Private IP or DNS is correctly configured in the SCS environment variables (`CCA_BASE_URL`).
- **Format Contract**: Ensure SCS trims and sanitizes the card number to match the exact `XXXX-XXXX-XXXX-XXXX` format before calling this service.