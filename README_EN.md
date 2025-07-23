# Civic Insights Auth Service

A Spring Boot authentication microservice that provides OAuth2 integration, JWT token management, and user profile services for the Civic Insights platform.

## 🚀 Features

- **OAuth2 Authentication** - Google OAuth2 integration
- **JWT Token Management** - Access token generation, validation, and refresh
- **User Profile Management** - Complete user profile CRUD operations
- **Microservice Architecture** - Designed as a dedicated auth service
- **RESTful API** - Clean REST endpoints with proper HTTP status codes
- **Interactive API Documentation** - Complete OpenAPI 3.0 documentation with Swagger UI
- **Security** - Spring Security with JWT-based authentication
- **Database Integration** - MySQL with JPA/Hibernate

## 🛠 Tech Stack

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Security 6.5.1**
- **Spring Data JPA**
- **MySQL 8.4+**
- **JWT (JJWT 0.12.3)**
- **OpenAPI 3.0** - SpringDoc OpenAPI UI for interactive documentation
- **Lombok**
- **Gradle**

## 📋 Prerequisites

- Java 17 or higher
- MySQL 8.4 or higher
- Gradle 8.14.3 or higher (included via wrapper)

## ⚙️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd civic-insights-auth
```

### 2. Database Setup
Create a MySQL database:
```sql
CREATE DATABASE civic_insights;
```

### 3. Environment Variables
Set up the following environment variables or update `application.properties`:

```bash
# Google OAuth2
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export GOOGLE_REDIRECT_URI=http://localhost:8001/api/v1/auth/login/oauth2/code/google

# JWT Configuration
export JWT_SECRET_KEY=your-secret-key-here-minimum-256-bits
```

### 4. Run the Application
```bash
./gradlew bootRun
```

The service will start on `http://localhost:8001`

### 5. Access Swagger UI
Once the application is running, you can access the interactive API documentation at:
- **Swagger UI**: http://localhost:8001/swagger-ui.html

## 📚 API Documentation

### 🌐 Interactive API Documentation (Swagger UI)
**Swagger UI**: http://localhost:8001/swagger-ui.html
- Complete interactive documentation for all API endpoints
- "Try it out" functionality to test APIs directly
- JWT authentication integration (use Authorize button)
- Request/response schemas and examples provided
- **Real-time API Testing**: Test all endpoints directly in the browser

### 📋 OpenAPI Specification
- **JSON format**: http://localhost:8001/v3/api-docs
- **YAML format**: http://localhost:8001/v3/api-docs.yaml
- **SpringDoc OpenAPI UI**: Compliant with latest OpenAPI 3.0 standards
- **Security Schemas**: JWT Bearer token authentication support

### Base URL
```
http://localhost:8001/api/v1
```

### Authentication Endpoints

#### Google OAuth2 Login
**Endpoint:** `POST /auth/google`
```bash
curl -X POST http://localhost:8001/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"code": "google_auth_code"}'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "email": "user@example.com",
  "name": "John Doe",
  "role": "USER"
}
```

#### Google OAuth2 Callback (for web flows)
**Endpoint:** `GET /auth/login/oauth2/code/google?code={auth_code}`

#### Refresh Token
**Endpoint:** `POST /auth/refresh`
```bash
curl -X POST http://localhost:8001/api/v1/auth/refresh \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "refreshToken=your_refresh_token"
```

### Profile Management Endpoints

#### Get User Profile
**Endpoint:** `GET /profile`
```bash
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:8001/api/v1/profile
```

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "bio": "Software Engineer",
  "location": "Seoul, Korea",
  "website": "https://johndoe.com",
  "phoneNumber": "+82-10-1234-5678",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

#### Update User Profile
**Endpoint:** `PUT /profile`
```bash
curl -X PUT http://localhost:8001/api/v1/profile \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "bio": "Updated bio",
    "location": "Busan, Korea",
    "website": "https://newsite.com",
    "phoneNumber": "+82-10-9876-5432",
    "avatarUrl": "https://example.com/new-avatar.jpg"
  }'
```

## 🗄️ Database Schema

### Users Table
```sql
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NULL,
  `name` VARCHAR(255) NOT NULL,
  `provider` VARCHAR(50) NOT NULL,
  `provider_id` VARCHAR(255) NULL,
  `role` VARCHAR(50) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_email` (`email` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### User Profiles Table
```sql
CREATE TABLE `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `bio` TEXT NULL,
  `location` VARCHAR(255) NULL,
  `website` VARCHAR(255) NULL,
  `phone_number` VARCHAR(50) NULL,
  `avatar_url` VARCHAR(500) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_id` (`user_id` ASC),
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 🔧 Configuration

### Application Properties
```properties
# Server Configuration
server.port=8001

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3312/civic_insights
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# OAuth2 Configuration
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI}

# JWT Configuration
app.jwt.secret-key=${JWT_SECRET_KEY}
app.jwt.expiration-ms=86400000
app.jwt.refresh-expiration=604800000

# SpringDoc OpenAPI Configuration
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.try-it-out-enabled=true
```

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/makersworld/civic_insights_auth/
│   │   ├── config/
│   │   │   ├── JwtProperties.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebClientConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   └── UserProfileController.java
│   │   ├── dto/
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── GoogleTokenResponse.java
│   │   │   ├── GoogleUserInfoResponse.java
│   │   │   ├── UpdateProfileRequest.java
│   │   │   └── UserProfileDto.java
│   │   ├── enums/
│   │   │   ├── Provider.java
│   │   │   └── Role.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   └── UserProfile.java
│   │   ├── repository/
│   │   │   ├── UserProfileRepository.java
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── GoogleOAuth2Service.java
│   │   │   ├── JwtService.java
│   │   │   ├── UserProfileService.java
│   │   │   └── UserService.java
│   │   └── CivicInsightsAuthApplication.java
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/
    └── java/com/makersworld/civic_insights_auth/
        └── CivicInsightsAuthApplicationTests.java
```

## 👤 User Profile Behavior

### OAuth2 Authentication Flow
When a user authenticates via Google OAuth2:

1. **User Record**: Creates or updates the `users` table with:
   - Email, name, provider info
   - Updates existing user's name if account already exists

2. **User Profile**: **Automatically created for new users**
   - Profile records are created **immediately** during OAuth2 authentication
   - Pre-populated with available Google data (profile picture)
   - Other profile fields (bio, location, website, etc.) start as `null`
   - Existing users: Profile is NOT modified, preserving user customizations

### Profile Creation Timeline
```
Google OAuth2 → User created/updated → UserProfile created (new users only) → JWT issued
                                            ↓
                                Profile ready with avatar from Google
```

### Auto-Populated Google Data
For new users, the following Google data is automatically saved to the profile:
- ✅ **Profile picture URL** (avatarUrl field)
- ✅ **User name** (saved to User table)
- ✅ **Email** (saved to User table)

### Manual Profile Fields
These fields remain empty until the user updates them via `/api/v1/profile`:
- Bio
- Location  
- Website
- Phone number

**Note**: Existing users' profiles are never automatically modified to preserve their customizations. Only new registrations get auto-populated data.

## 🔐 Security

### JWT Token Structure
- **Access Token**: Valid for 24 hours (86400000ms)
- **Refresh Token**: Valid for 7 days (604800000ms)
- **Algorithm**: HMAC SHA-256

### Protected Endpoints
All `/api/v1/profile/**` endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <access_token>
```

### CORS Configuration
- **Allowed Origins**: `http://localhost:3000` (React frontend)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Credentials**: Enabled

## 🧪 Testing

### Run Tests
```bash
./gradlew test
```

### Manual API Testing
1. **Using Swagger UI (Recommended)**: http://localhost:8001/swagger-ui.html
   - Test APIs directly in the browser
   - Authenticate with JWT tokens to test protected endpoints
   
2. **Using cURL**: Use the provided curl examples

3. **API Clients**: Import OpenAPI specification into Postman/Insomnia

### 🔍 OAuth2 Endpoint Testing Guide

#### ✅ Testable Endpoints
```bash
# Google OAuth2 Login (POST method)
curl -X POST http://localhost:8001/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"code": "real_google_auth_code"}'

# Token Refresh
curl -X POST http://localhost:8001/api/v1/auth/refresh \
  -d "refreshToken=your_refresh_token"

# User Profile (JWT token required)
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:8001/api/v1/profile
```

#### ⚠️ OAuth2 Callback Endpoint Notes
```bash
# This endpoint returns 400 errors (normal behavior)
curl http://localhost:8001/api/v1/auth/login/oauth2/code/google

# Reason: Google OAuth2 dedicated endpoint, requires real auth codes
# Not for manual testing, only for Google OAuth2 flows
```

**Why OAuth2 callback endpoint returns 400 errors**:
- 🔒 **Security Design**: Rejects invalid requests
- 📋 **OAuth2 Specification**: Google OAuth2 codes are one-time use and cryptographically secure
- 🛡️ **Input Validation**: Random test strings should be rejected

**Real Google OAuth2 Flow**:
1. User clicks "Login with Google"
2. Redirect to Google consent screen
3. User grants permissions
4. Google calls callback endpoint with real auth code
5. Service returns JWT tokens

### 🛠️ Common Troubleshooting

#### Q: OAuth2 callback endpoint returns 400 errors
**A**: This is normal behavior!
```bash
# ❌ Testing like this returns 400 errors (expected)
curl http://localhost:8001/api/v1/auth/login/oauth2/code/google

# ✅ Use POST endpoint instead
curl -X POST http://localhost:8001/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"code": "google_auth_code"}'
```

#### Q: "Required parameter 'code' is not present" error
**A**: OAuth2 callback endpoint should only be called by Google.
- For manual testing: Use `POST /api/v1/auth/google`
- For web flows: Use `GET /api/v1/auth/login/oauth2/code/google` (Google only)

#### Q: "Environment variables not working"
**A**: Check environment variable setup:
```bash
# Check current environment variables
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET
echo $GOOGLE_REDIRECT_URI

# Correct setup example
export GOOGLE_CLIENT_ID="your-actual-google-client-id"
export GOOGLE_CLIENT_SECRET="your-actual-google-client-secret"
export GOOGLE_REDIRECT_URI="http://localhost:8001/api/v1/auth/login/oauth2/code/google"

# Run application with environment variables
GOOGLE_CLIENT_ID=your-id GOOGLE_CLIENT_SECRET=your-secret ./gradlew bootRun
```

#### Q: OAuth2 redirect not working
**A**: Check Google Cloud Console setup:
1. **Google Cloud Console** → APIs & Services → Credentials
2. Select **OAuth 2.0 Client IDs**
3. Add exactly to **Authorized redirect URIs**:
   ```
   http://localhost:8001/api/v1/auth/login/oauth2/code/google
   ```
4. **Save** and wait a few minutes before testing

#### Q: How to test JWT tokens?
**A**: Use Swagger UI:
1. Go to http://localhost:8001/swagger-ui.html
2. Click "Authorize" button
3. Enter token in format: `Bearer <your-jwt-token>`
4. Test protected endpoints

## 🚀 Deployment

### Production Considerations
1. **Environment Variables**: Set all required environment variables
2. **Database**: Configure production MySQL instance
3. **HTTPS**: Enable SSL/TLS for production
4. **CORS**: Update allowed origins for production frontend URL
5. **JWT Secret**: Use a strong, randomly generated secret key (minimum 256 bits)

### Docker (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/civic-insights-auth-*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 Integration with Other Services

This auth service is designed to work in a microservices architecture:

1. **Frontend Applications**: Use the OAuth2 and JWT endpoints for user authentication
2. **Other Microservices**: Validate JWT tokens using the same secret key and validation logic
3. **API Gateway**: Can route authentication requests to this service

### Example Integration
```javascript
// Frontend: Login with Google
const response = await fetch('/api/v1/auth/google', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ code: googleAuthCode })
});

const { accessToken } = await response.json();

// Use token for subsequent API calls
const profileResponse = await fetch('/api/v1/profile', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
```

## 📝 License

This project is part of the Civic Insights platform.

## 📞 Support

For questions or issues, please contact the development team or create an issue in the repository.

---

## 🆕 Recent Updates

### v1.1.0 - OpenAPI/Swagger Integration Complete
- ✅ **SpringDoc OpenAPI UI** integration completed
- ✅ **Interactive API Documentation** http://localhost:8001/swagger-ui.html
- ✅ **JWT Authentication Integration** Direct token testing in Swagger UI
- ✅ **OAuth2 Endpoint Optimization** and improved error handling
- ✅ **Security Configuration Updates** SpringDoc endpoint support
- ✅ **Detailed Troubleshooting Guide** added
- ✅ **Environment Variable Setup Guide** completed

### Key Improvements
- **API Documentation**: Complete interactive documentation compliant with OpenAPI 3.0 standards
- **Developer Experience**: Direct API testing and JWT authentication in Swagger UI
- **Error Handling**: Clear explanation of OAuth2 callback endpoint 400 errors
- **Setup Guides**: Detailed guides for Google Cloud Console and environment variable configuration
- **Security Enhancement**: Optimized security settings to expose only correct endpoints

---

**Built with ❤️ for the Civic Insights Platform** 