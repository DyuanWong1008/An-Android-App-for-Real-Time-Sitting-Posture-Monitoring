README for Developers
1.	Overview
Posture Guard is an Android application designed to help users monitor and improve their posture using live camera functionality. It features posture detection, statistics tracking, and user profile management. This document provides guidance for developers to understand and modify the system.

2.	Features
•	Real-time posture monitoring with visual and audio feedback.
•	User authentication (Login, Signup, Forgot Password).
•	Posture statistics tracking and visualization.
•	Profile management, including language selection and password updates.

3.	Tech Stack
•	Programming Language: Kotlin and Java
•	IDE: Android Studio
•	Database: 
	Firebase (cloud storage for user statistics) and 
	SQLite (local storage for user statistics)
•	AI Model: MoveNet for posture detection

4.	Project Structure
a)	MainActivity.kt: Manages global application settings and navigation.
b)	HomeFragment.kt: Handles the main posture monitoring interface.
c)	LoginFragment.kt & SignupFragment.kt: Manages user authentication.
d)	ProfileFragment.kt: Allows users to view and edit their profile.
e)	StatisticFragment.kt: Displays user statistics.
f)	CameraUtils.kt: Handles camera initialization and management.
g)	DatabaseHelper.kt: Manages SQLite database interactions.

5.	Key Components
a)	Camera Activation
The camera is initialized when the user starts posture monitoring. Use the openCamera() and closeCamera() methods in CameraUtils.kt.

b)	Posture Monitoring
i.	MoveNet AI Model Details:
•	Capabilities: Detects 17 keypoints, including head, shoulders, elbows, hips, knees, and ankles.
•	Real-Time Performance: Processes posture data at 30 frames per second to ensure timely feedback.
•	Customization: The pre-trained model was fine-tuned using augmented datasets to recognize 'Standard Sitting,' 'Forward Head,' and 'Cross-Legged' postures accurately.
•	TensorFlow Lite: Allows lightweight and on-device inference without internet connectivity, ensuring data privacy and efficient performance.

c)	The AI model, MoveNet, detects three postures:
•	Standard: Neutral posture, providing positive feedback.
•	Crossleg: Poor posture, triggering sound alerts and visual feedback.
•	Forwardhead: Poor posture, triggering similar alerts. MoveNet is a pre-trained model capable of detecting 17 keypoints on the human body. It uses TensorFlow Lite and requires dependencies such as TensorFlow libraries. Alerts and reminders are managed in HomeFragment.kt.

6.	Posture Score
The "Posture Score" is calculated based on the ratio of good posture instances to total monitored time. Higher scores indicate better posture consistency.

7.	Data Persistence
User statistics, such as posture type, duration, and timestamps, are stored in an SQLite database. Modify DatabaseHelper.kt for changes to database structure or queries.

8.	Known Issues
1.	Lag during posture monitoring.
2.	Camera switching (front/rear) needs improvement.
3.	Device switching (CPU/GPU/NNAPI) is under development.

9.	How to Contribute
1.	Clone the repository.
2.	Make changes in a separate branch.
3.	Test thoroughly before submitting a pull request.
 



User Manual for Posture Guard
Welcome to Posture Guard
Posture Guard is your companion for maintaining a healthy posture. This manual will guide you through using the app effectively.
1. Launching the App
Upon opening the app, you will see the Welcome Page featuring the Posture Guard logo. Tap "Login" to access your account or "Sign Up" to create a new one.

2. Login
Enter your email and password, then tap Login. If you forgot your password, tap Forgot Password? to recover it.

3. Signup
Fill in your details, including name, age, email, and password, then tap Sign Up. Select your gender using the radio buttons.

4. Home Page
•	Sound Alerts: The app uses a short beep to indicate minor bad posture and a continuous tone for prolonged bad posture (e.g., 10 seconds or more).
•	Visual Feedback: Red text warnings and flashing icons appear on the home screen when a bad posture is detected. Green visual cues, like a thumbs-up icon, appear when the user maintains good posture.
•	Vibration Alerts: For users in quiet environments or those with hearing impairments, a vibration is triggered as an alternative to sound notifications. This can be enabled or disabled in the app settings.
•	Start Monitoring: Tap the button to begin posture monitoring. Your camera will activate, and posture detection will start.
•	Real-time Feedback: Receive sound alerts and visual cues for bad posture (e.g., "Crossleg" or "Forwardhead") to prompt corrective action.
•	Switch Camera: Use the dropdown to switch between front and rear cameras.

5. Statistics Page
View your posture statistics:
•	Monitor Time: Total time spent in monitoring sessions.
•	Posture Score: A numerical indicator of posture quality, calculated based on good and bad posture instances. The posture score is calculated dynamically using the following formula:
o	Good Posture: Each detected instance of good posture adds one point.
o	Bad Posture: Each instance of bad posture subtracts one point. Prolonged bad posture (lasting over 10 seconds) results in additional deductions. The score is expressed as a percentage: “(Good Postures / Total Postures) * 100”. A score of 80% or higher indicates consistent good posture.
•	Breakdown: Counts of good and bad posture events, with specific data for "Forwardhead" and "Crossleg" detections. Use the date picker to view statistics for a specific period.

6. Profile Page
Manage your account:
•	View and Edit Personal Details: Name, age, and gender.
•	Language Selection: Choose from English, Chinese, or Malay.
•	Change Password: Update your account credentials securely.
•	Logout: End your session securely.

7. Forgot Password
Enter your registered email to receive a password reset link. Follow the instructions in the email to update your password.

8. Settings
•	Edit Profile: Update your name, age, or gender.
•	Change Password: Securely update your account credentials.

9. Best Practices
a)	Privacy and Data Security:
•	Encryption: All user data, including posture statistics and login credentials, is encrypted using AES-256 for secure storage.
•	Local Storage: Posture data is stored locally in SQLite for quick access and remains within the device unless explicitly shared by the user.
•	Firebase Integration: Firebase Authentication ensures secure login and password recovery, and its Firestore database securely stores user-specific data, such as posture statistics and session history.
•	No Third-Party Access: The app does not share or sell user data to third parties, ensuring complete privacy.
b)	Real-World Use Cases:
•	Office Work: Use Posture Guard to ensure proper posture during long working hours at a desk, reducing back and neck strain.
•	At Home: Monitor posture while watching TV or relaxing to develop healthier sitting habits.
•	Physical Therapy: Share statistics with healthcare providers to tailor therapy sessions and track progress.
•	For Students: Improve study posture to enhance focus and reduce fatigue during extended study sessions.
•	Use the app in a well-lit environment for accurate posture detection.
•	Review statistics regularly to track your posture habits.
•	Ensure your device’s camera is clean for optimal performance.

10.	Troubleshooting
•	Lagging? Restart the app or ensure no other heavy apps are running.
•	Camera issues? Check permissions or switch cameras.
•	Statistics not updating? Verify database access or reinstall the app.



Thank you for choosing Posture Guard. Stay healthy and upright!

