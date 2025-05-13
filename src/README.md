# Medical & Event Management Desktop Application

## Overview
This is a modern desktop application developed in JavaFX as part of the academic program at **Esprit School of Engineering**.  
It complements the web application by providing a rich client interface for managing medical records, events, users, and media synchronization.

## Features
- **Authentication & Role Management:** Secure login, registration, password reset, role-based access (patients, doctors, admin), account verification, login history, JWT, captcha
- **User Management:** Add, edit, delete, list, and verify users (patients/doctors), profile management, rights management
- **Event Management:** Create, edit, delete, and view events; event registration; event categories; calendar integration; event statistics
- **Medical Records:** Create, view, edit medical records, patient/doctor history
- **Appointment Management:** Book, edit, cancel appointments, manage appointment states, view appointment history
- **Doctor & Patient Management:** Add, edit, delete, detailed profiles, list and statistics by role
- **Product Management:** Product catalog, add/edit/delete products, product categories, favorites, advanced search, product images
- **Order Management:** Shopping cart, place orders, order history, payment integration
- **Posts & Comments:** Create, edit, delete posts and comments, post categories, favorites, reactions, content moderation
- **Reclamations & Responses:** Create, track, edit, delete reclamations, manage types and responses
- **Notifications & Alerts:** Real-time notifications, display and manage alerts, WhatsApp/SMS/email notifications
- **Dashboard & Statistics:** Global statistics, charts, PDF export, advanced data exports
- **Calendar Integration:** Interactive calendar for events and appointments
- **Image & File Synchronization:** Upload and sync images/files with server, AI-based image validation
- **Medical Chatbot:** Automated user assistance, medical chatbot API integration
- **Advanced Security:** JWT, blacklist, session management, password hashing/generation, captcha, content moderation
- **Advanced Utilities:** Email, SMS, WhatsApp integration, statistics utilities, environment management, data export

## Tech Stack
- **Language:** Java 11+
- **UI Framework:** JavaFX
- **Architecture:** MVC (Model-View-Controller)
- **Build Tool:** Maven or Gradle
- **Other:** Custom utility modules, RESTful API integration

## Directory Structure
```
src/
├── main/
│   ├── java/
│   │   ├── com/
│   │   │   ├── controllers/
│   │   │   ├── models/
│   │   │   ├── services/
│   │   │   ├── dto/
│   │   │   ├── event/
│   │   │   ├── exceptions/
│   │   │   ├── utils/
│   │   │   └── MainApplication.java
│   └── resources/
│       ├── styles/
│       ├── view/
│       ├── images/
│       └── ...
└── test/
```

## Getting Started

### Prerequisites
- Java 11+ (JDK)
- Maven (or Gradle)
- Internet connection for API/data sync

### Installation
```bash
# Open the project in your preferred IDE (IntelliJ IDEA, Eclipse, NetBeans)
# or use Maven to build:
mvn clean install
# Run the application
mvn javafx:run
```

### Usage
- Log in as a patient, doctor, or admin
- Manage medical records, events, appointments, and products
- Synchronize images and files with the server
- Access statistics and dashboard views

## Acknowledgments
This project was completed as part of the academic curriculum at **Esprit School of Engineering**.

## Topics
`esprit-school-of-engineering`, `javafx`, `medical-management`, `event-management`, `dashboard`, `java`, `desktop-app`, `mvc`, `image-synchronization`
