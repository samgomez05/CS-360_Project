# Inventory Management App

## Overview

This is an Inventory Management application developed as part of the CS-360 course at Southern New
Hampshire University. The app allows users to manage inventory items, including adding, updating,
viewing, and deleting items. It also includes user authentication.

## Features

- Inventory management (add, update, view, delete items)
- First-time admin registration with customizable credentials
- Data persistence using SQLite
- Responsive design for tablets and phones

## Dependencies

- Google Material Components
- Android SDK
    - AndroidX libraries
- Firebase Cloud Messaging (FCM)
- Gradle

## Installation

### Prerequisites

- Android Studio Meerkat | 2024.3.1 Patch 1 or higher
- Android Emulator or physical Android device
- Java Development Kit (JDK) 11 or higher

### Steps

1. Clone the repository:
      ```
      git clone https://github.com/techiesam05/inventory-management-app.git
      cd inventory-management-app
      ```

2. Open the project in Android Studio:  
   - Launch Android Studio.
   - Select Open an existing project.
   - Navigate to the cloned repository and select the project folder.

3. Configure Firebase:
    - Add your Firebase project’s `google-services.json` file to the `app` directory to enable push
      notifications.
    - Follow the Firebase setup instructions for Android if needed.

4. Build the project:
    - Click on Build in the top menu.
    - Select Make Project to build the application and download all dependencies.

5. Run the application:  
   - Connect an Android device or start an Android emulator.
   - Click on the Run button or select Run > Run 'app' from the top menu.

## Usage

- Login and Registration:
    - On the first launch, the app allows first-time admin registration.
    - Users are prompted to create an account with customizable credentials.
    - These credentials are securely stored and required for subsequent logins using FirebaseAuth.

- Manage Inventory:
    - Tap on the floating action button (FAB) to add new inventory items.
    - Edit items by selecting them from the list.
    - View item details or delete them directly from the inventory list.


## License

- Author:
    - Samuel Gomez

## Acknowledgments

- Southern New Hampshire University
- CS-360 Course Instructors

For any issues or contributions, please open an issue or submit a pull request on the GitHub
repository.
