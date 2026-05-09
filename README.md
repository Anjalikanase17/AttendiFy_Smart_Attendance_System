# Attendify - Smart Attendance System

Attendify is a comprehensive, role-based Smart Attendance Management System designed to streamline the process of taking attendance, managing shifts, and sharing resources. It leverages QR-code technology for secure check-ins and offers tailored dashboards for Administrators, Managers, Teachers, and Students/Employees.

---

## 🌟 Key Features

### 1. Role-Based Dashboards & Access Control
- **Admin**: Full system control. Can view system-wide analytics, manage user roles, and oversee all operations.
- **Manager**: Responsible for creating and assigning shifts, generating QR codes for sessions, and monitoring attendance for assigned groups.
- **Teacher**: Can view student attendance records, publish notices, and upload educational notes/resources.
- **Student/Employee**: Can scan QR codes to mark attendance (Check-in/Check-out), view personal attendance history, and access notes/notices.

### 2. QR-Code Based Attendance
- **Dynamic Generation**: Managers generate unique QR codes for specific shifts and dates.
- **Dual Scan System**: Supports both Check-in and Check-out scans to accurately track time spent in a session.
- **Secure Validation**: Validates the student's identity and ensures they are scanning the correct QR code for the current active shift.

### 3. Shift & Schedule Management
- **Shift Creation**: Define shift timings (e.g., Morning Shift: 9 AM - 1 PM).
- **Daily Scheduling**: Assign specific slots to managers/teachers for a given date.
- **Real-time Monitoring**: Track attendance in real-time as users scan in.

### 4. Communication & Resource Sharing
- **Notices**: Broadcast important announcements to all users.
- **Notes/Materials**: Teachers can upload PDFs or images which students can easily download from their dashboard.

---

## 🛠️ Technology Stack

- **Backend Architecture**: Java 17, Spring Boot 3.4.x
- **Security**: Spring Security with JWT (JSON Web Tokens) for stateless authentication.
- **Database**: MySQL (Hibernate / Spring Data JPA for ORM)
- **Frontend**: Vanilla HTML5, CSS3, and JavaScript (Fetch API for asynchronous requests). No heavy frontend frameworks, ensuring blazing fast load times.
- **Build Tool**: Maven
- **Deployment**: Docker containerization supported.

---

## 🚀 How to Setup (Local Environment)

Follow these steps to get the application running on your local machine.

### Prerequisites
1. **Java Development Kit (JDK) 17** installed.
2. **MySQL Server** (version 8.x recommended) installed and running.
3. **Maven** installed (or use the provided `mvnw` wrapper).

### Step 1: Database Setup
1. Open your MySQL client (e.g., MySQL Workbench or CLI).
2. Create a new database named `attendify`:
   ```sql
   CREATE DATABASE attendify;
   ```
3. The application uses `root` as the default username and `040678` as the password. If your local MySQL credentials differ, update them in `Attendify/register/src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   ```

### Step 2: Build the Project
1. Open a terminal and navigate to the project directory:
   ```bash
   cd Attendify/register
   ```
2. Clean and build the application using Maven:
   ```bash
   mvn clean install -DskipTests
   ```

### Step 3: Run the Application
1. Start the Spring Boot server:
   ```bash
   mvn spring-boot:run
   ```
2. The server will start on port `8081`. 

---

## 💻 How to Use the System

Once the server is running, access the application via your browser. Each role has a completely separate, dedicated workflow.

---

### 👤 Role 1 — Admin

The Admin is the **super-user** of the system. They set up the entire organizational structure that all other roles depend on.

**Step 1: Register & Login**
- Navigate to `http://localhost:8081/public/admin-register.html` to register the Admin account.
- Once registered, login at `http://localhost:8081/public/login.html`.
- You will be redirected to the **Admin Dashboard** (`admin-dashboard.html`).

**Step 2: Set Up the Organizational Structure**
Before any teachers or students can use the system, the Admin must configure:
- **Departments** — e.g., Computer Science, Mechanical Engineering.
- **Classes** (Class Master) — e.g., FY, SY, TY.
- **Divisions** (Division Master) — e.g., A, B, C.
- **Subjects** (Subject Master) — e.g., Mathematics, Physics, Algorithms.

All this is done from the **Master Data** section of the Admin Dashboard.

**Step 3: Register Teachers**
- Go to the **Teachers** tab.
- Click **Add Teacher** and fill in the teacher's name, email, department, and a temporary password.
- The teacher will receive credentials to log in.

**Step 4: Register Students**
- Go to the **Students** tab.
- Click **Add Student** and fill in student details including name, roll number, class, and division.
- Students can also self-register at `http://localhost:8081/public/register.html`.

**Step 5: Monitor the System**
- The Admin Dashboard homepage shows live stats: **Total Teachers**, **Total Students**, **Total Classes**, and **Today's Attendance Percentage**.
- Admins can search, filter, and manage all user records at any time.

---

### 📚 Role 2 — Teacher

The Teacher is responsible for conducting lectures, generating QR codes, finalizing attendance, and communicating with students.

**Step 1: Login**
- Go to `http://localhost:8081/public/login.html` and login with Teacher credentials.
- You will be redirected to the **Teacher Dashboard** (`teacher-dashboard.html`).

**Step 2: View Your Timetable**
- The Teacher Dashboard shows your **assigned timetable** — which class, division, and subject you teach each day.
- This timetable is configured by the Admin under the **Timetable Structure** settings.

**Step 3: Start an Attendance Session (Generate QR Code)**
- On your active timetable slot, click the **Start Session / Generate QR** button.
- The system will:
  1. Capture your **GPS location** (used as the classroom anchor point for geofencing).
  2. Create a unique **Session ID** with an expiry time (default: 10 minutes, configurable).
  3. Pre-generate **"Absent"** records for all students in that class/division for this subject. This ensures even non-scanners are automatically marked absent.
  4. Display a visual **QR Code** on screen encoding the session ID.
- Display this QR code on a projector so students can scan it.

**Step 4: Monitor Live Attendance**
- As students scan the QR code, the dashboard updates in real-time showing who has been marked **Present**.

**Step 5: Finalize Attendance**
- Once the session window closes (QR expires), click **Finalize**.
- Any student who did not scan will remain marked **Absent**.
- The Teacher can also perform **Manual Attendance** — searching for a student by roll number and manually setting their status to Present or Absent.
- The Teacher can do **Bulk Manual Entry** by loading the full class list and toggling each student's status on a grid, then submitting all at once.

**Step 6: View Attendance Reports & Analytics**
- Navigate to the **Reports** section to view:
  - **Subject-wise Analytics** — Attendance percentage per subject across all classes.
  - **Date-wise Analytics** — Attendance trend across specific dates.
  - **Student-level Report** — Filter by class, division, or subject to see individual student records.
  - **Monthly Report (Excel Download)** — Generate and download a full monthly attendance sheet with subject-wise present/total/percentage breakdown per student.

**Step 7: Post Notices**
- Go to the **Notices** tab.
- Click **Add Notice**, write a title and message body, and submit.
- All students can see the notice on their dashboard immediately.

**Step 8: Upload Notes / Study Materials**
- Go to the **Notes** tab.
- Click **Upload Note**, provide a title, description, and attach a file (PDF or Image, up to 20 MB).
- Students can browse and download these files from their dashboard.

**Step 9: Manage Leave Requests**
- Go to the **Leave Requests** tab.
- Students submit leave requests which appear here with a **Pending** status.
- The Teacher can **Approve** or **Reject** each request. The status is instantly updated for the student.

---

### 🎓 Role 3 — Student / Employee

Students interact with the system primarily to mark their own attendance and access resources.

**Step 1: Register & Login**
- Self-register at `http://localhost:8081/public/register.html` by filling in your name, roll number, email, and password.
- Login at `http://localhost:8081/public/login.html`.
- You will be redirected to the **Student Dashboard** (`dashboard.html`).

**Step 2: Mark Attendance via QR Scan**
- When a Teacher starts a session in class, open your Student Dashboard.
- Click the **Scan QR Code** button (camera permission required).
- Point the camera at the teacher's QR code displayed on the projector.
- The system performs **three validations** before marking you Present:
  1. **Session Validity** — Is this QR code still active (not expired)?
  2. **Geofence Check** — Are you within 100 meters of the teacher's GPS location?
  3. **Anti-Proxy Check** — Has this device already been used to mark attendance for this session? (One device = one student per session).
- If all checks pass, you receive a **"Attendance marked successfully ✅"** confirmation.

**Step 3: View Your Attendance History**
- The dashboard displays your personal attendance records organized by subject.
- You can see your **present count**, **total classes**, and **attendance percentage** for each subject.
- If your attendance drops below a threshold, the system sends an **email alert** to you automatically.

**Step 4: Read Notices**
- All notices posted by Teachers are visible in the **Notices** section of your dashboard.

**Step 5: Download Notes**
- Go to the **Notes** section to browse and download materials uploaded by your teachers (PDFs, images, etc.).

**Step 6: Submit a Leave Request**
- Go to the **Leave** section.
- Fill in the reason, dates, and select the relevant teacher.
- Submit the form. The request appears in the teacher's dashboard with a **Pending** status.
- You can track the status (Pending → Approved / Rejected) from your own dashboard.

---

## 🔐 Security Overview

- All API endpoints are protected via **JWT (JSON Web Tokens)**.
- The JWT token is issued on successful login and must be sent in the `Authorization: Bearer <token>` header for all subsequent requests.
- The system uses **Admin Context** to ensure data isolation — every record (students, teachers, attendance, notices) is scoped to the Admin who created it. No data leaks between different organizations using the same system.

---

## 🐳 Docker Deployment

If you prefer to run the application using Docker, a `Dockerfile` is included in the root directory.

1. Build the Docker image:
   ```bash
   docker build -t attendify-app .
   ```
2. Run the container, passing your database connection as environment variables:
   ```bash
   docker run -p 8081:8081 \
     -e DATABASE_URL=jdbc:mysql://host.docker.internal:3306/attendify \
     -e DB_USERNAME=root \
     -e DB_PASSWORD=040678 \
     attendify-app
   ```
   > **Note**: `host.docker.internal` lets the Docker container connect to MySQL running on your local machine. On Linux, you may need to use `--network=host` instead.

---

*© 2026 SPCL Infotech Pvt Ltd. All Rights Reserved.*