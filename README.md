# Inventory Management App

## Overview

This is an Inventory Management application developed as part of the CS-360 course at Southern New Hampshire University. The app allows users to manage inventory items, including adding, viewing, and deleting items. It also includes user authentication and notification features.  


## Features
  - User authentication
  - Inventory management (add, view, delete items)
  - Notifications for inventory alerts
  - Admin user setup on first-time use


## Dependencies
  - Google Material Components
  - Android SDK
    - AndroidX libraries
  - Java
  - Gradle


## Installation
### Prerequisites
  - Android Studio Ladybug | 2024.2.1 Patch 3
  - Android Emulator or physical Android device
  - Java Development Kit (JDK)


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

  3. Build the project:  
    - Click on Build in the top menu.
    - Select Make Project to build the project and download dependencies.

  4. Run the application:  
    - Connect an Android device or start an Android emulator.
    - Click on the Run button or select Run > Run 'app' from the top menu.

  5. Grant necessary permissions:  
    - On first launch, the app will request permissions for sending SMS and posting notifications. Ensure these permissions are granted.


## Usage
  - Login:  
    - Use the default admin credentials (username: admin, password: admin123) on first-time setup.
    - Change the password after the first login for security.

  - Manage Inventory:  
    - Use the floating action button (FAB) to add new inventory items.
    - View and delete items from the inventory list.

  - Notifications:  
    - The app will send notifications for inventory alerts, such as when an item is out of stock.

## License
  - Author:
    - Samuel Gomez

## Acknowledgments
  - Southern New Hampshire University
  - CS-360 Course Instructors

For any issues or contributions, please open an issue or submit a pull request on the GitHub repository.
