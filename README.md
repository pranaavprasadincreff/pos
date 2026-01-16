# POS (Point of Sale) System

A modern Point of Sale system built with Spring Boot and MongoDB.

## Project Structure

The project is divided into two main modules:
- `pos-model`: Contains all Forms, and Data classes
- `pos-server`: Contains the application logic, controllers, and database operations

### Architecture Layers
1. **Controller Layer**: Handles HTTP requests and responses
2. **DTO Layer**: Handles data transformation and basic validation
3. **API Layer**: Implements business logic
4. **DAO Layer**: Handles database operations

## Technology Stack

- Java 21
- Spring Boot 3.2.3
- MongoDB
- Maven
- Lombok
- SpringDoc OpenAPI (Swagger)

## MongoDB Setup

### Local Development
1. Install MongoDB Community Edition:
   ```bash
   # For Ubuntu
   sudo apt-get install mongodb

   # For MacOS using Homebrew
   brew install mongodb-community
   ```

2. Start MongoDB service:
   ```bash
   # For Ubuntu
   sudo service mongodb start

   # For MacOS
   brew services start mongodb-community
   ```

3. Configure MongoDB connection in `application.properties`:
   ```properties
   spring.data.mongodb.host=localhost
   spring.data.mongodb.port=27017
   spring.data.mongodb.database=pos
   ```

### Remote MongoDB (Atlas)
1. Create a MongoDB Atlas account
2. Create a new cluster
3. Configure network access and database user
4. Update `application.properties` with your connection string:
   ```properties
   spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster-url>/<database-name>?retryWrites=true&w=majority
   ```

## Building and Running

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run the application:
   ```bash
   cd pos-server
   mvn spring-boot:run
   ```

## API Documentation

Once the application is running, access the Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

### Available Endpoints

#### User Management
- POST `/api/user/add` - Create a new user
- GET `/api/user/get-by-id/{id}` - Get user by ID
- POST `/api/user/get-all-paginated` - Get all users with pagination

## Testing

The project uses an embedded MongoDB for testing. Run tests with:
```bash
mvn test
```

### Test Coverage
- Input validation
- User creation and retrieval
- Pagination
- Error handling
- Duplicate email checks

## Project Features

1. **Data Validation**
   - Email format validation
   - Required field validation
   - Pagination parameters validation

2. **Error Handling**
   - Custom ApiException for business logic errors
   - Global exception handling
   - User-friendly error messages

3. **Auditing**
   - Creation timestamp
   - Update timestamp
   - Record IDs

4. **Pagination**
   - Page size limits
   - Sort by creation date
   - Total count information

## Owner
Anurag Singh 



# POS
# POS
