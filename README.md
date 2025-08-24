# Saarland Events API

## About The Project

This is the backend for the **"Saarland Events"** web application, developed using **Spring Boot**. The API provides a comprehensive set of endpoints for managing events, users, categories, and cities, and implements secure authentication and integration with third-party services.

## Key Features

* **RESTful Architecture:** A clear and structured organization of API endpoints.
* **Security:** Implemented authentication using **JWT** and **OAuth2** for Google Sign-In. User roles (USER, ADMIN) are used to control access to the API.
* **Content Management:** Full CRUD functionality for events, cities, and categories, available to administrators.
* **Event Moderation:** Users can submit their own events, which are placed in a moderation queue for an administrator to approve or reject.
* **Pagination and Filtering:** All major lists (events, users) support pagination and flexible filtering for high performance.
* **Third-Party Service Integrations:**
    * **DeepL API** for automatic translation of event content.
    * **SendGrid API** for sending email notifications (reminders, password resets).
    * **Supabase** for cloud-based image storage.

## Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot 3.3.2
* **Database:** PostgreSQL with Spring Data JPA & Hibernate
* **Security:** Spring Security (JWT, OAuth2)
* **Build Tool:** Maven

## Getting Started

### Prerequisites

* Java 21+
* Maven
* PostgreSQL

### Installation

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd events-api
    ```

2.  **Set up the database:**
    Create a new database in PostgreSQL and provide the connection details in your environment variables.

3.  **Environment Variables:**
    An example of the required variables can be found in `src/main/resources/application.properties`.

    **Key variables:**
    * `SPRING_DATASOURCE_URL`: `jdbc:postgresql://localhost:5432/your_db_name`
    * `SPRING_DATASOURCE_USERNAME`: `your_db_user`
    * `SPRING_DATASOURCE_PASSWORD`: `your_db_password`
    * `APP_JWT_SECRET`: Your secret key for JWT.
    * `DEEPL_AUTH_KEY`: Your DeepL API key.
    * `SENDGRID_API_KEY`: Your SendGrid API key.
    * `SUPABASE_URL`: The URL for your Supabase project.
    * `SUPABASE_SERVICE_KEY`: The service key for Supabase.
    * `GOOGLE_CLIENT_ID`: Your Client ID from Google Cloud.
    * `GOOGLE_CLIENT_SECRET`: Your Client Secret from Google Cloud.
    * `APP_OAUTH2_REDIRECT_URI`: `http://localhost:5173/auth/callback` (for local development).

### Running the Application

Execute the following command in the project's root directory:

```bash
./mvnw spring-boot:run