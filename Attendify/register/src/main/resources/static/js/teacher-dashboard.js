// DOM Elements
const API_BASE = "";
const originalFetch = window.fetch.bind(window);

window.fetch = (input, init = {}) => {
    const url = typeof input === "string" ? input : (input && input.url) ? input.url : "";
    const isApiCall = url.startsWith(`${API_BASE}/api/`) || url.startsWith("/api/");

    if (!isApiCall) {
        return originalFetch(input, init);
    }

    const token = (localStorage.getItem('teacherAuthToken') || localStorage.getItem('authToken'));
    const headers = new Headers(init.headers || {});
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    return originalFetch(input, { ...init, headers });
};

const navLinks = document.querySelectorAll('.nav-link');
const tabContents = document.querySelectorAll('.tab-content');
const pageTitle = document.getElementById('pageTitle');
let attendanceChart = null;
let teacherDeviceId = null;
let teacherSubjectChartInstance = null;
let teacherTrendChartInstance = null;
let teacherDoughnutInstance = null; // Fix for "Error loading report" crash
const TEACHER_STUDENT_PAGE_SIZE = 50;
let _teacherStudentSearchDebounce = null;
let _teacherStudentState = {
    page: 0,
    size: TEACHER_STUDENT_PAGE_SIZE,
    totalPages: 0,
    totalElements: 0,
    content: [],
    rowByRoll: new Map(),
    filtersInitialized: false
};

// =========================
// Chart Helpers & Integrity
// =========================
function getTeacherChartTooltipConfig() {
    return {
        callbacks: {
            label: function(context) {
                let label = context.dataset.label || '';
                if (label) label += ': ';
                const dataObj = context.chart.data.datasets[context.datasetIndex].allData?.[context.dataIndex];
                if (dataObj && dataObj.total !== undefined) {
                    const val = context.parsed.x !== undefined ? context.parsed.x : context.parsed.y;
                    return `${label}${val}% (${dataObj.present}/${dataObj.total} students)`;
                }
                return label + (context.parsed.x !== undefined ? context.parsed.x : context.parsed.y) + '%';
            }
        }
    };
}

function checkTeacherEmptyChart(data, canvasId, message = "No data available for these filters") {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return false;
    const ctx = canvas.getContext('2d');
    if (!data || data.length === 0) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = '#94a3b8';
        ctx.font = '14px Poppins';
        ctx.fillText(message, canvas.width / 2, canvas.height / 2);
        return true;
    }
    return false;
}


document.addEventListener("DOMContentLoaded", function () {
    setupUploadNotes();
    loadUploadedNotes();
    loadNotesFilters(); // Add this line
    // fingerprint for teacher device
    loadTeacherDeviceId();
    initTeacherAnalyticsFilters();
});

function loadTeacherDeviceId() {
    if (typeof FingerprintJS === 'undefined') {
        console.warn("FingerprintJS not loaded yet, retrying...");
        setTimeout(loadTeacherDeviceId, 500);
        return;
    }
    const fpPromise = FingerprintJS.load();
    fpPromise
        .then(fp => fp.get())
        .then(result => {
            teacherDeviceId = result.visitorId;
            console.log("Teacher Device ID captured:", teacherDeviceId);
        })
        .catch(err => console.error("FingerprintJS error:", err));
}
// Initialize the dashboard
document.addEventListener('DOMContentLoaded', function () {
    // Check authentication
    const role = (localStorage.getItem('teacherRole') || localStorage.getItem('role'));
    const userData = JSON.parse((localStorage.getItem('teacherLoggedUser') || localStorage.getItem('loggedUser')));

    // Security check
    if (!userData || role !== "teacher") {
        window.location.href = "login.html";
        return;
    }

    console.log("Teacher Logged In:", userData);

    const name = userData.name || userData.fullName || userData.teacherName || userData.username || "Teacher";

    // Update all name elements
    const nameElements = document.querySelectorAll('[id*="TeacherName"], [id*="teacherName"], #headerName');
    nameElements.forEach(element => {
        element.textContent = name;
    });

    const classTeacherInput = document.getElementById("classTeacher");
    if (classTeacherInput) {
        classTeacherInput.value = name;
    }
    // Update avatar
    const headerAvatar = document.getElementById('headerAvatar');
    if (headerAvatar) {
        const initials = getInitials(name);
        headerAvatar.textContent = initials;
        headerAvatar.style.backgroundColor = stringToColor(name);
    }

    // Initialize date and time
    updateDateTime();
    setInterval(updateDateTime, 1000);

    // Setup event listeners
    setupEventListeners();

    // Show initial tab (dashboard)
    showTab('dashboard');

    // Set active nav link
    setActiveNavLink('dashboard');

    setupTeacherMobileSidebar();

    loadTeacherSubjectsForNotes();
});

// Get initials for avatar
function getInitials(name) {
    return name.split(' ')
        .map(word => word[0])
        .join('')
        .toUpperCase()
        .slice(0, 2);
}

// Generate color from string
function stringToColor(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    let color = '#';
    for (let i = 0; i < 3; i++) {
        const value = (hash >> (i * 8)) & 0xFF;
        color += ('00' + value.toString(16)).substr(-2);
    }
    return color;
}

// Update date and time
function updateDateTime() {
    const now = new Date();

    // Format date
    const dateOptions = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const dateElement = document.getElementById('currentDate');
    if (dateElement) {
        dateElement.textContent = now.toLocaleDateString('en-US', dateOptions);
    }

    // Format time
    const timeElement = document.getElementById('currentTime');
    if (timeElement) {
        timeElement.textContent = now.toLocaleTimeString('en-US', {
            hour12: true,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }
}

function setupTeacherMobileSidebar() {
    const toggleBtn = document.getElementById('teacherMenuToggle');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('teacherSidebarOverlay');

    if (!toggleBtn || !sidebar || !overlay) return;

    toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('sidebar-open');
        overlay.classList.toggle('show');
        document.body.classList.toggle('menu-open');
    });

    overlay.addEventListener('click', () => {
        sidebar.classList.remove('sidebar-open');
        overlay.classList.remove('show');
        document.body.classList.remove('menu-open');
    });

    // Close on navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', () => {
            sidebar.classList.remove('sidebar-open');
            overlay.classList.remove('show');
            document.body.classList.remove('menu-open');
        });
    });

    window.addEventListener('resize', () => {
        if (window.innerWidth > 992) {
            sidebar.classList.remove('sidebar-open');
            overlay.classList.remove('show');
            document.body.classList.remove('menu-open');
        }
    });
}

// Setup event listeners
function setupEventListeners() {
    // Navigation links
    navLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const tab = this.getAttribute('data-tab');
            showTab(tab);
            setActiveNavLink(tab);
        });
    });

    // QR Generator
    setupQRGenerator();



    // Students tab
    setupStudentsTab();

    // Teacher Profile
    setupTeacherProfile();

    // Reports
    setupReportsTab();

    // Settings
    setupSettingsTab();
}

// Show specific tab
function showTab(tabName) {
    // Hide all tabs
    tabContents.forEach(tab => {
        tab.classList.remove('active');
    });

    // Show selected tab
    const selectedTab = document.getElementById(`${tabName}-tab`);
    if (selectedTab) {
        selectedTab.classList.add('active');

        // Update page title
        const tabTitles = {
            'dashboard': 'Teacher Dashboard',
            'qr-generator': 'Generate QR Code',
            'classes': 'My Classes',
            'students': 'Student Management',
            'teacher-profile': 'My Profile',
            'timetable': 'My Timetable',
            'reports': 'Attendance Reports',
            'notices': 'Global Notices',
            'leave-requests': 'Leave Requests',
            'upload-notes': 'Upload Notes',
            'settings': 'System Settings'
        };

        if (pageTitle) {
            pageTitle.textContent = tabTitles[tabName] || 'Dashboard';
        }

        // Load tab-specific content
        loadTabContent(tabName);
    }
}

// Set active navigation link
function setActiveNavLink(tabName) {
    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('data-tab') === tabName) {
            link.classList.add('active');
        }
    });
}

// Load tab-specific content
function loadTabContent(tabName) {
    switch (tabName) {
        case 'dashboard':
            loadDashboardContent();
            break;
        case 'qr-generator':
            loadQRGeneratorContent();
            loadTodayLectures(); // populate today's lecture dropdown from timetable
            break;

        case 'students':
            loadStudentsContent();
            break;
        case 'teacher-profile':
            loadTeacherProfileContent();
            break;
        case 'reports':
            loadReportsContent();
            break;
        case 'timetable':
            loadTimetableGrid();
            break;
        case 'settings':
            loadSettingsContent();
            break;
        case 'leave-requests':
            loadLeaveRequests();
            break;
        case 'notices':
            loadTeacherNotices();
            break;
    }
    populateSharedMasterData();
}

async function populateSharedMasterData() {
    try {
        const classSelects = ['newStudentClass'];
        const subSelects = ['manualSubjectSelect', 'notesSubject', 'manualSubjectSelect'];

        const classesRes = await fetch('/api/master/classes');
        const classes = await classesRes.json();

        classSelects.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.innerHTML = '<option value="">-- Select Class --</option>' +
                    classes.map(c => `<option value="${c.id}">${c.className}</option>`).join('');
            }
        });

        const subsRes = await fetch('/api/master/subjects');
        const subs = await subsRes.json();
        subSelects.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.innerHTML = '<option value="">-- Select Subject --</option>' +
                    subs.map(s => `<option value="${s.id}">${s.subjectName}</option>`).join('');
            }
        });
    } catch (e) {
        console.error("Master data pop error:", e);
    }
}


function getTeacherId() {
    const userData = JSON.parse((localStorage.getItem('teacherLoggedUser') || localStorage.getItem('loggedUser')));
    return userData?.id || null;
}


function setupQRGenerator() {
    const generateQRBtn = document.getElementById('generateQRBtn');
    const qrDisplay = document.getElementById('qrDisplay');

    if (generateQRBtn) {
        generateQRBtn.addEventListener('click', function () {
            // Use the timetable-based lecture selection
            const lectureSelect = document.getElementById('lectureSelect');
            const durationSelect = document.getElementById('durationSelect');
            const duration = parseInt(durationSelect?.value || '10');

            const selectedOpt = lectureSelect?.options[lectureSelect.selectedIndex];
            if (!selectedOpt || !selectedOpt.value) {
                alert('Please select today\'s lecture from the dropdown first.');
                return;
            }

            const subjectId = selectedOpt.dataset.subjectId || '';
            const classId = selectedOpt.dataset.classId || '';
            const divisionId = selectedOpt.dataset.divisionId || '';

            const subjectName = selectedOpt.dataset.subject || '';
            const className = selectedOpt.dataset.className || '';
            const divisionName = selectedOpt.dataset.division || '';
            const timetableSlotId = selectedTimetableSlotId;

            if (!navigator.geolocation) {
                alert('Location not supported by this browser');
                return;
            }

            navigator.geolocation.getCurrentPosition(position => {
                const teacherLat = position.coords.latitude;
                const teacherLng = position.coords.longitude;

                const payload = {
                    subjectId,
                    classId,
                    divisionId,
                    teacherLat,
                    teacherLng,
                    radiusKm: 0.1,
                    duration,
                    timetableSlotId,
                    teacherDeviceId: teacherDeviceId,
                    teacherId: getTeacherId()
                };

                fetch(`${API_BASE}/api/session/create`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${(localStorage.getItem('teacherAuthToken') || localStorage.getItem('authToken')) || ''}`
                    },
                    body: JSON.stringify(payload)
                })
                    .then(res => {
                        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
                        return res.json();
                    })
                    .then(data => {
                        const sessionId = data.sessionId;
                        if (!sessionId) {
                            throw new Error('No sessionId returned from server');
                        }
                        if (qrDisplay) {
                            qrDisplay.style.display = 'flex';
                            const info = document.getElementById('qrClassInfo');
                            if (info) info.textContent = `${className} ${divisionName} — ${subjectName}`;
                            const nameEl = document.getElementById('qrTeacherName');
                            if (nameEl) nameEl.textContent = document.getElementById('headerName').textContent;
                            const ts = document.getElementById('qrTimestamp');
                            if (ts) ts.textContent = `Today, ${new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}`;
                            const timerTxt = document.getElementById('qrTimerText');
                            if (timerTxt) timerTxt.textContent = `${duration} minutes`;
                            generateQRCode(className, divisionName, duration, sessionId);
                        }
                    })
                    .catch(err => {
                        console.error('Failed to create session:', err);
                        alert('Unable to create QR session. Check console.');
                    });
            }, err => {
                console.error('Geolocation error:', err);
                alert('Unable to get location. Please allow location access.');
            });
        });
    }
}


// Generate QR code
function generateQRCode(className, division, duration, sessionId) {
    const qrImage = document.getElementById('qrImage');
    const qrTimer = document.getElementById('qrTimer');

    // Base URL for students (dynamic based on current host)
    const base = `${window.location.origin}/public/student-attendance.html`;

    const qrParams = new URLSearchParams();
    qrParams.append("class", className);
    qrParams.append("division", division);
    if (sessionId) {
        qrParams.append("sessionId", sessionId);
    }

    const qrData = `${base}?${qrParams.toString()}`;

    // Update QR image
    qrImage.src = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(qrData)}`;

    // Start countdown timer
    startCountdown(duration * 60, qrTimer);

    // Add share buttons if not already present
    addShareButtons();
}
// Start countdown timer
function startCountdown(seconds, timerElement) {
    let timeLeft = seconds;

    // Clear any existing timer
    if (window.qrTimerInterval) {
        clearInterval(window.qrTimerInterval);
    }

    window.qrTimerInterval = setInterval(() => {
        if (timeLeft <= 0) {
            clearInterval(window.qrTimerInterval);
            timerElement.textContent = '00:00';

            // Show expired message
            const qrDisplay = document.getElementById('qrDisplay');
            if (qrDisplay) {
                const expiredMsg = document.createElement('div');
                expiredMsg.className = 'expired-message';
                expiredMsg.innerHTML = '<i class="fas fa-exclamation-circle"></i> QR Code has expired!';
                qrDisplay.appendChild(expiredMsg);
            }

            return;
        }

        const minutes = Math.floor(timeLeft / 60);
        const secs = timeLeft % 60;
        timerElement.textContent = `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        timeLeft--;
    }, 1000);
}

// Add share buttons
function addShareButtons() {
    const qrDisplay = document.getElementById('qrDisplay');
    if (!qrDisplay) return;

    // Remove existing share buttons
    const existingShare = qrDisplay.querySelector('.share-buttons');
    if (existingShare) {
        existingShare.remove();
    }

    // Create share buttons container
    const shareButtons = document.createElement('div');
    shareButtons.className = 'share-buttons';
    shareButtons.innerHTML = `
        <h4>Share QR Code:</h4>
        <div class="share-options">
            <button class="btn btn-whatsapp" id="shareWhatsAppBtn">
                <i class="fab fa-whatsapp"></i> Share to WhatsApp
            </button>
            <button class="btn btn-secondary" id="downloadQRBtn">
                <i class="fas fa-download"></i> Download QR
            </button>
            <button class="btn btn-secondary" id="copyQRBtn">
                <i class="fas fa-copy"></i> Copy QR Link
            </button>
        </div>
    `;

    qrDisplay.appendChild(shareButtons);

    // Add event listeners to share buttons
    document.getElementById('shareWhatsAppBtn')?.addEventListener('click', shareToWhatsApp);
    document.getElementById('downloadQRBtn')?.addEventListener('click', downloadQRCode);
    document.getElementById('copyQRBtn')?.addEventListener('click', copyQRCode);
}

function shareToWhatsApp() {
    const classInfo = document.getElementById('qrClassInfo').textContent;
    const teacherName = document.getElementById('qrTeacherName').textContent;
    const qrImageUrl = document.getElementById('qrImage').src;

    const message =
        `📱 Attendance QR Code\n\n` +
        `📚 ${classInfo}\n` +
        `👨‍🏫 Teacher: ${teacherName}\n\n` +
        `📸 Scan QR using this link:\n${qrImageUrl}\n\n` +
        `⏰ ${document.getElementById('qrTimerText').textContent}`;

    const whatsappUrl =
        `https://wa.me/?text=${encodeURIComponent(message)}`;

    window.open(whatsappUrl, '_blank');
}

// Download QR code
function downloadQRCode() {
    const qrImage = document.getElementById('qrImage');
    const classInfo = document.getElementById('qrClassInfo').textContent
        .replace(/[^a-zA-Z0-9]/g, '_');

    // Create temporary link
    const link = document.createElement('a');
    link.href = qrImage.src;
    link.download = `QR_Attendance_${classInfo}_${Date.now()}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    alert('QR Code downloaded successfully!');
}

// Copy QR code URL
function copyQRCode() {
    const qrImage = document.getElementById('qrImage');

    // Create temporary input element
    const tempInput = document.createElement('input');
    tempInput.value = qrImage.src;
    document.body.appendChild(tempInput);
    tempInput.select();
    document.execCommand('copy');
    document.body.removeChild(tempInput);

    alert('QR Code URL copied to clipboard!');
}



// Show subtab
function showSubTab(parentTabId, subtabName) {
    const parentTab = document.getElementById(parentTabId);
    const subtabs = parentTab.querySelectorAll('.tab-content');

    subtabs.forEach(tab => {
        tab.classList.remove('active');
    });

    const selectedSubtab = document.getElementById(`${subtabName}-subtab`);
    if (selectedSubtab) {
        selectedSubtab.classList.add('active');
    }
}

// Setup Students Tab
function setupStudentsTab() {
    const studentTabs = document.querySelectorAll('#students-tab .tab');
    studentTabs.forEach(tab => {
        tab.addEventListener('click', function () {
            const subtab = this.getAttribute('data-subtab');
            showSubTab('students-tab', subtab);
            studentTabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });

    const addStudentBtn = document.getElementById('addStudentBtn');
    if (addStudentBtn) {
        addStudentBtn.addEventListener('click', function (e) {
            e.preventDefault();
            showSubTab('students-tab', 'add-student');
        });
    }

    const addStudentForm = document.getElementById('addStudentForm');
    if (addStudentForm) {
        addStudentForm.addEventListener('submit', function (e) {
            e.preventDefault();
            addNewStudent();
        });
    }

    const manualAttendanceForm = document.getElementById('manualAttendanceForm');
    if (manualAttendanceForm) {
        manualAttendanceForm.addEventListener('submit', function (e) {
            e.preventDefault();
            saveManualAttendance();
        });

        loadTeacherSubjectsInStudentTab();
    }


    // Load Students Button Click
    const loadBtn = document.querySelector("#students-tab .btn.btn-primary");

    if (loadBtn) {
        loadBtn.addEventListener("click", function () {
            loadStudentsForSelectedClass();
        });
    }

    // Initialize Add Student Dropdowns
    populateAddStudentDropdowns();
}

async function populateAddStudentDropdowns() {
    const classSelect = document.getElementById('newStudentClass');
    const divSelect = document.getElementById('newStudentDivision');
    if (!classSelect || !divSelect) return;

    try {
        const res = await fetch('/api/master/classes');
        const classes = await res.json();
        classSelect.innerHTML = '<option value="">-- Select Class --</option>';
        classes.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = c.className || c.name;
            classSelect.appendChild(opt);
        });

        classSelect.onchange = async () => {
            const classId = classSelect.value;
            divSelect.innerHTML = '<option value="">-- Select Division --</option>';
            divSelect.disabled = true;

            if (!classId) return;

            const resDiv = await fetch(`/api/master/classes/${classId}/divisions`);
            const divisions = await resDiv.json();
            divisions.forEach(d => {
                const opt = document.createElement('option');
                opt.value = d.id;
                opt.textContent = d.divisionName || d.name;
                divSelect.appendChild(opt);
            });
            divSelect.disabled = false;
        };
    } catch (err) {
        console.error("Error loading master data for Add Student:", err);
    }
}

// Setup Teacher Profile
// Profile UI Update Helper
function updateTeacherProfileUI(t) {
    if (!t) return;

    // 1. Header Updates
    const headerName = document.getElementById("headerName");
    const headerRole = document.getElementById("headerRole");
    const headerAvatar = document.getElementById("headerAvatar");

    if (headerName) headerName.textContent = t.name || "Teacher";
    if (headerRole) headerRole.textContent = "Teacher";
    if (headerAvatar) {
        const initials = getInitials(t.name || "T");
        headerAvatar.textContent = initials;
        headerAvatar.style.backgroundColor = stringToColor(t.name || "Teacher");
    }

    // 2. Profile Tab Details
    const nameEl = document.getElementById("teacherName");
    const emailEl = document.getElementById("teacherEmail");
    const phoneEl = document.getElementById("teacherPhone");
    const deptEl = document.getElementById("teacherDepartment");
    const profileAvatar = document.getElementById("teacherAvatar");

    if (nameEl) nameEl.textContent = t.name || "-";
    if (emailEl) emailEl.textContent = t.email || "-";
    if (phoneEl) phoneEl.textContent = t.mobilenumber || "-";

    if (deptEl) {
        if (t.department && t.department.departmentName) {
            deptEl.textContent = t.department.departmentName;
        } else if (t.department) {
            deptEl.textContent = t.department; // handle if just string
        } else {
            deptEl.textContent = "Not Assigned (Optional)";
        }
    }

    if (profileAvatar) {
        profileAvatar.textContent = getInitials(t.name || "T");
        profileAvatar.style.backgroundColor = stringToColor(t.name || "Teacher");
    }

    // 3. Form fields in Settings/Modals
    const fields = [
        ['accountTeacherName', t.name],
        ['accountTeacherEmail', t.email],
        ['accountTeacherPhone', t.mobilenumber || t.phone],
        ['accountDepartment', t.department?.departmentName || t.department],
        ['editTeacherName', t.name],
        ['editTeacherEmail', t.email],
        ['editTeacherPhone', t.mobilenumber || t.phone],
        ['editTeacherDepartment', t.department?.departmentName || t.department]
    ];

    fields.forEach(([id, val]) => {
        const el = document.getElementById(id);
        if (el) el.value = val || "";
    });

    // Stash ID/Obj globally if needed
    window.currentTeacherId = t.id;
    window.currentTeacherObj = t;
}

function setupTeacherProfile() {
    const userData = JSON.parse((localStorage.getItem('teacherLoggedUser') || localStorage.getItem('loggedUser')));
    if (!userData) return;

    // Fetch and populate EVERYTHING on startup
    fetchTeacherData(userData.email);

    // Edit Profile Button
    const editProfileBtn = document.getElementById('editTeacherProfileBtn');
    if (editProfileBtn) {
        editProfileBtn.onclick = () => openEditProfileModal();
    }

    // Change Password Button
    const changePasswordBtn = document.getElementById('changePasswordBtn');
    if (changePasswordBtn) {
        changePasswordBtn.onclick = () => {
            const modal = document.getElementById('changePasswordModal');
            if (modal) modal.style.display = 'flex';
        };
    }

    // Upload Photo Button
    const uploadPhotoBtn = document.getElementById('uploadPhotoBtn');
    if (uploadPhotoBtn) {
        uploadPhotoBtn.onclick = () => {
            alert("Photo upload feature is coming soon! You can use 'Account Settings' to update your info in the meantime.");
        };
    }

    // Modal Close Buttons
    const closeBtns = [
        'closeEditTeacherProfileBtn', 'cancelEditTeacherProfileBtn',
        'closeChangePasswordBtn', 'cancelChangePasswordBtn'
    ];
    closeBtns.forEach(id => {
        const btn = document.getElementById(id);
        if (btn) {
            btn.onclick = () => {
                const modal = btn.closest('.modal');
                if (modal) modal.style.display = 'none';
            };
        }
    });

    // Setup Form Listeners
    setupProfileFormListeners();
}

function openEditProfileModal() {
    const modal = document.getElementById('editTeacherProfileModal');
    if (!modal) return;

    const t = window.currentTeacherObj;
    if (t) {
        // Double check inputs are filled (though updateTeacherProfileUI should have done it)
        document.getElementById('editTeacherName').value = t.name || "";
        document.getElementById('editTeacherId').value = t.id || "";
        document.getElementById('editTeacherEmail').value = t.email || "";
        document.getElementById('editTeacherPhone').value = t.mobilenumber || "";
        document.getElementById('editTeacherDepartment').value = t.department?.departmentName || t.department || "";
        document.getElementById('editTeacherRole').value = t.role || "TEACHER";
        
        // New fields
        const bios = document.getElementById('editTeacherBio');
        if (bios) bios.value = t.bio || "";
        
        const addr = document.getElementById('editTeacherAddress');
        if (addr) addr.value = t.permanentAddress || "";
        
        const dob = document.getElementById('editTeacherDOB');
        if (dob) dob.value = t.dateOfBirth || "";
        
        const edu = document.getElementById('editTeacherEducation');
        if (edu) edu.value = t.education || "";
        
        const appt = document.getElementById('editTeacherAppointment');
        if (appt) appt.value = t.dateOfAppointment || "";

        const spouseName = document.getElementById('editTeacherSpouseName');
        if (spouseName) spouseName.value = t.spouseName || "";

        const spouseContact = document.getElementById('editTeacherSpouseContact');
        if (spouseContact) spouseContact.value = t.spouseContact || "";
    }

    modal.style.display = 'flex';
}

function setupProfileFormListeners() {
    // 1. Edit Profile Form
    const editForm = document.getElementById('editTeacherProfileForm');
    if (editForm) {
        editForm.onsubmit = async (e) => {
            e.preventDefault();
            const id = document.getElementById('editTeacherId').value;
            const updatedData = {
                name: document.getElementById('editTeacherName').value,
                email: document.getElementById('editTeacherEmail').value,
                mobilenumber: document.getElementById('editTeacherPhone').value,
                role: document.getElementById('editTeacherRole').value,
                bio: document.getElementById('editTeacherBio')?.value || "",
                permanentAddress: document.getElementById('editTeacherAddress')?.value || "",
                dateOfBirth: document.getElementById('editTeacherDOB')?.value || "",
                education: document.getElementById('editTeacherEducation')?.value || "",
                dateOfAppointment: document.getElementById('editTeacherAppointment')?.value || "",
                spouseName: document.getElementById('editTeacherSpouseName')?.value || "",
                spouseContact: document.getElementById('editTeacherSpouseContact')?.value || ""
            };

            try {
                const res = await fetch(`/api/teachers/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(updatedData)
                });
                if (!res.ok) throw new Error("Update failed");
                const updated = await res.json();
                alert("Profile updated successfully!");
                document.getElementById('editTeacherProfileModal').style.display = 'none';
                updateTeacherProfileUI(updated);
            } catch (err) {
                alert("Error updating profile: " + err.message);
            }
        };
    }

    // 2. Account Settings Form (in Settings tab)
    const accountForm = document.getElementById('accountSettingsForm');
    if (accountForm) {
        accountForm.onsubmit = async (e) => {
            e.preventDefault();
            const id = window.currentTeacherId;
            const updatedData = {
                name: document.getElementById('accountTeacherName').value,
                email: document.getElementById('accountTeacherEmail').value,
                mobilenumber: document.getElementById('accountTeacherPhone').value
            };

            try {
                const res = await fetch(`/api/teachers/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(updatedData)
                });
                if (!res.ok) throw new Error("Update failed");
                const updated = await res.json();
                alert("Account updated successfully!");
                updateTeacherProfileUI(updated);
            } catch (err) {
                alert("Error updating account: " + err.message);
            }
        };
    }

    // 3. Change Password Form (Modal)
    const passwordForm = document.getElementById('changePasswordForm');
    if (passwordForm) {
        passwordForm.onsubmit = async (e) => {
            e.preventDefault();
            const currentPass = document.getElementById('currentPasswordModal').value;
            const newPass = document.getElementById('newPasswordModal').value;
            const confirmPass = document.getElementById('confirmPasswordModal').value;

            if (newPass !== confirmPass) {
                alert("New passwords do not match!");
                return;
            }

            try {
                const res = await fetch(`/api/teachers/${window.currentTeacherId}/password`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ currentPassword: currentPass, newPassword: newPass })
                });
                const result = await res.json();
                if (!res.ok) throw new Error(result.message || "Failed to change password");

                alert("Password changed successfully!");
                passwordForm.reset();
                document.getElementById('changePasswordModal').style.display = 'none';
            } catch (err) {
                alert("Error: " + err.message);
            }
        };
    }
}

async function fetchTeacherData(identifier) {
    if (!identifier) return;
    try {
        const res = await fetch(`/api/teachers/${identifier}`);
        if (!res.ok) throw new Error("HTTP " + res.status);
        const t = await res.json();
        updateTeacherProfileUI(t);
    } catch (err) {
        console.error("Profile load error:", err);
    }
}

function loadTeacherProfileContent() {
    const userData = JSON.parse((localStorage.getItem('teacherLoggedUser') || localStorage.getItem('loggedUser')));
    if (userData?.email) fetchTeacherData(userData.email);
}

// Setup Reports Tab
function setupReportsTab() {
    const generateReportBtn = document.getElementById('generateReportBtn');
    if (generateReportBtn) {
        generateReportBtn.addEventListener('click', function () {
            generateReport();
        });
    }
}

function setupSettingsTab() {

    const menuItems = document.querySelectorAll('.settings-item');

    // Menu Click Switching
    menuItems.forEach(item => {
        item.addEventListener('click', function () {

            // Remove active from all menu items
            menuItems.forEach(i => i.classList.remove('active'));
            this.classList.add('active');

            const type = this.getAttribute('data-settings');
            showSettingsSection(type);
        });
    });

    // ===== Load Attendance Settings =====
    fetch("/api/settings")
        .then(res => {
            if (!res.ok) throw new Error("No settings found");
            return res.json();
        })
        .then(setting => {
            if (!setting) return;

            document.getElementById("attendanceThreshold").value = setting.attendanceThreshold || "";
            document.getElementById("lateThreshold").value = setting.lateArrivalMinutes || "";
            document.getElementById("autoAbsent").value = setting.autoMarkAbsentMinutes || "";
            document.getElementById("allowManualOverride").checked = setting.manualOverride || false;
            document.getElementById("sendAbsenceAlerts").checked = setting.sendAlerts || false;
        })
        .catch(err => console.log("Settings not loaded yet"));

    // ===== Save Attendance Settings =====
    const form = document.getElementById("attendanceSettingsForm");

    if (form) {
        form.addEventListener("submit", function (e) {
            e.preventDefault();

            const data = {
                id: 1,
                attendanceThreshold: parseInt(document.getElementById("attendanceThreshold").value),
                lateArrivalMinutes: parseInt(document.getElementById("lateThreshold").value),
                autoMarkAbsentMinutes: parseInt(document.getElementById("autoAbsent").value),
                manualOverride: document.getElementById("allowManualOverride").checked,
                sendAlerts: document.getElementById("sendAbsenceAlerts").checked
            };

            fetch("/api/settings/save", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(data)
            })
                .then(res => {
                    if (!res.ok) throw new Error("Save failed");
                    return res.text();
                })
                .then(msg => {
                    alert("Settings Saved Successfully ✅");
                })
                .catch(() => alert("Error saving settings ❌"));
        });
    }
}

// Show settings section
function showSettingsSection(settingsType) {
    const sections = document.querySelectorAll('.settings-section');
    sections.forEach(section => {
        section.classList.remove('active');
    });

    const selectedSection = document.getElementById(`${settingsType}-settings`);
    if (selectedSection) {
        selectedSection.classList.add('active');
    }
    if (settingsType === "leave") {
        loadAllLeaves();
    }
}



async function loadDashboardContent() {
    const teacherId = getTeacherId();
    if (!teacherId) return;

    try {
        const res = await fetch(`/api/teacher/timetable/${teacherId}/today`);
        const lectureSlots = await res.json();

        const container = document.querySelector('#dashboard-tab .classes-container');
        if (!container) return;

        if (!lectureSlots.length) {
            container.innerHTML = '<p style="color:#aaa;font-size:0.9rem;padding:1rem;">No lectures scheduled for today. <i class="fas fa-mug-hot"></i></p>';
            return;
        }

        container.innerHTML = lectureSlots.map(slot => `
            <div class="class-card">
                <div class="class-header">
                    <div class="class-title">${slot.subjectMaster ? slot.subjectMaster.subjectName : 'Unknown Subject'}</div>
                    <div class="stat-value" style="font-size:0.8rem; background:#f0f4ff; padding:2px 8px; border-radius:10px;">
                        ${slot.slot.startTime}
                    </div>
                </div>
                <div class="class-info">
                   <i class="fas fa-graduation-cap"></i> ${slot.classMaster ? slot.classMaster.className : '?'} 
                   ${slot.divisionMaster ? ' — Div ' + slot.divisionMaster.divisionName : ''}
                </div>
                <div class="class-stats">
                    <div class="stat-item">
                        <div class="stat-label"><i class="fas fa-door-open"></i> Room</div>
                        <div class="stat-value" style="font-size:0.9rem;">${slot.roomNo || '?'}</div>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error("Dashboard load error:", e);
    }
}

// Load QR Generator content
function loadQRGeneratorContent() {
    // classSelect was replaced by lectureSelect (timetable-based dropdown)
    // If the old classSelect still exists (legacy), populate it; otherwise skip
    const classSelect = document.getElementById('classSelect');
    if (!classSelect) return; // new UI uses lectureSelect dropdown instead

    const teacherName = document.getElementById('headerName').textContent.trim();
    if (!teacherName) {
        console.error("Teacher name not found in header");
        return;
    }

    fetch(`/api/classes/teacher/${teacherName}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch classes");
            return res.json();
        })
        .then(classes => {
            console.log("Classes from DB:", classes);
            classSelect.innerHTML = `<option value="">Select Class</option>`;
            classes.forEach(cls => {
                const option = document.createElement("option");
                option.value = cls.subject;
                option.textContent = `${cls.className} - ${cls.subject}`;
                classSelect.appendChild(option);
            });
        })
        .catch(err => console.error("Error loading classes:", err));
}


function loadClassesContent() {
    const teacherName = document.getElementById('headerName').textContent;

    fetch(`/api/classes/teacher/${teacherName}`)
        .then(res => res.json())
        .then(classes => {
            const container = document.getElementById('classesContainer');
            if (!container) return;

            if (classes.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <h3>No Classes Yet</h3>
                        <p>Create your first class.</p>
                    </div>`;
                return;
            }

            container.innerHTML = classes.map(cls => `
                <div class="class-card">
                    <div class="class-title">${cls.className} - ${cls.subject}</div>
                    <p><strong>Schedule:</strong> ${cls.schedule}</p>
                    <p><strong>Room:</strong> ${cls.room}</p>
                    <p><strong>Divisions:</strong> ${cls.divisions}</p>
                </div>
            `).join('');
        })
        .catch(err => console.error("Error loading classes:", err));
}

// Add event listener when teacher selects a class
const classSelect = document.getElementById('classSelect');
if (classSelect) {
    classSelect.addEventListener('change', loadStudentsForSelectedClass);
}


async function loadTeacherSubjectsInStudentTab() {
    try {
        const classDropdown = document.getElementById("filterClass");
        const divisionDropdown = document.getElementById("filterDivision");
        const subjectDropdown = document.getElementById("filterSubject");

        if (!classDropdown || !divisionDropdown || !subjectDropdown) return;

        if (_teacherStudentState.filtersInitialized) return;

        // Clear existing
        classDropdown.innerHTML = `<option value="">All Classes</option>`;
        divisionDropdown.innerHTML = `<option value="">All Divisions</option>`;
        subjectDropdown.innerHTML = `<option value="">All Subjects</option>`;

        // Load all classes first
        const classesRes = await fetch('/api/master/classes');
        const classes = await classesRes.json();
        classes.forEach(c => {
            classDropdown.innerHTML += `<option value="${c.id}">${c.className}</option>`;
        });

        // Add listeners for dependent dropdowns
        classDropdown.onchange = async () => {
            const classId = classDropdown.value;
            divisionDropdown.innerHTML = `<option value="">All Divisions</option>`;
            subjectDropdown.innerHTML = `<option value="">All Subjects</option>`;

            if (!classId) {
                loadStudentsForSelectedClass();
                return;
            }

            // Load divisions for class
            const divsRes = await fetch(`/api/master/classes/${classId}/divisions`);
            const divs = await divsRes.json();
            divs.forEach(d => {
                divisionDropdown.innerHTML += `<option value="${d.id}">${d.divisionName}</option>`;
            });

            // Load subjects for class
            const subsRes = await fetch(`/api/master/classes/${classId}/subjects`);
            const subs = await subsRes.json();
            subs.forEach(s => {
                subjectDropdown.innerHTML += `<option value="${s.id}">${s.subjectName}</option>`;
            });

            loadStudentsForSelectedClass();
        };

        divisionDropdown.onchange = () => loadStudentsForSelectedClass();
        subjectDropdown.onchange = () => loadStudentsForSelectedClass();
        _teacherStudentState.filtersInitialized = true;

    } catch (err) {
        console.error("Error populating filters:", err);
    }
}







function loadStudentsContent() {
    loadTeacherSubjectsInStudentTab();
    fetchTeacherStudentPage(0);
}


function loadStudentsForSelectedClass() {
    fetchTeacherStudentPage(0);
}

function getTeacherStudentQueryParams(pageNo = 0) {
    const params = new URLSearchParams();
    params.set("page", String(Math.max(pageNo, 0)));
    params.set("size", String(_teacherStudentState.size));

    const classId = document.getElementById("filterClass")?.value;
    const divisionId = document.getElementById("filterDivision")?.value;
    const subjectId = document.getElementById("filterSubject")?.value;
    const q = (document.getElementById("teacherStudentSearch")?.value || "").trim();

    if (classId) params.set("classId", classId);
    if (divisionId) params.set("divisionId", divisionId);
    if (subjectId) params.set("subjectId", subjectId);
    if (q) params.set("q", q);
    return params;
}

function renderTeacherStudentRows(rows) {
    const tbody = document.getElementById("studentTableBody");
    if (!tbody) return;

    if (!rows.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align:center;">
                    No students found
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = rows.map(s => `
        <tr>
            <td>${s.rollNo}</td>
            <td>${s.name}</td>
            <td>${s.className}</td>
            <td>${s.subject}</td>
            <td>${s.status || "-"}</td>
            <td class="action-icons">
                <i class="fas fa-eye view-icon" onclick="viewStudent('${s.rollNo}')" title="View"></i>
                <i class="fas fa-pen edit-icon" onclick="editStudent('${s.rollNo}')" title="Edit"></i>
                <i class="fas fa-trash delete-icon" onclick="deleteStudent(${s.id})" title="Delete"></i>
            </td>
        </tr>
    `).join("");
}

function updateTeacherStudentPager() {
    const countEl = document.getElementById("teacherStudentCount");
    const pageInfo = document.getElementById("teacherStudentPageInfo");
    const prevBtn = document.getElementById("teacherStudentPrevBtn");
    const nextBtn = document.getElementById("teacherStudentNextBtn");

    if (!countEl || !pageInfo || !prevBtn || !nextBtn) return;

    const { page, size, totalPages, totalElements, content } = _teacherStudentState;
    if (totalElements === 0) {
        countEl.textContent = "0 of 0";
        pageInfo.textContent = "Page 0 of 0";
        prevBtn.disabled = true;
        nextBtn.disabled = true;
        return;
    }

    const start = page * size + 1;
    const end = Math.min(page * size + content.length, totalElements);
    countEl.textContent = `${start}-${end} of ${totalElements}`;
    pageInfo.textContent = `Page ${page + 1} of ${Math.max(totalPages, 1)}`;
    prevBtn.disabled = page <= 0;
    nextBtn.disabled = page >= totalPages - 1;
}

async function fetchTeacherStudentPage(pageNo = 0) {
    const tbody = document.getElementById("studentTableBody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Loading students...</td></tr>`;

    try {
        const params = getTeacherStudentQueryParams(pageNo);
        const res = await fetch(`/api/attendance/teacher/student-list?${params.toString()}`);
        if (!res.ok) throw new Error(`Failed with status ${res.status}`);
        const payload = await res.json();

        let rows;
        if (Array.isArray(payload)) {
            rows = payload;
            _teacherStudentState.page = 0;
            _teacherStudentState.size = rows.length || TEACHER_STUDENT_PAGE_SIZE;
            _teacherStudentState.totalElements = rows.length;
            _teacherStudentState.totalPages = rows.length > 0 ? 1 : 0;
        } else {
            rows = Array.isArray(payload.content) ? payload.content : [];
            _teacherStudentState.page = payload.page ?? 0;
            _teacherStudentState.size = payload.size ?? TEACHER_STUDENT_PAGE_SIZE;
            _teacherStudentState.totalElements = payload.totalElements ?? 0;
            _teacherStudentState.totalPages = payload.totalPages ?? 0;
        }

        rows.sort((a, b) => String(a.rollNo || "").localeCompare(String(b.rollNo || ""), undefined, { numeric: true }));
        _teacherStudentState.content = rows;
        _teacherStudentState.rowByRoll = new Map(rows.map(r => [String(r.rollNo), r]));

        renderTeacherStudentRows(rows);
        updateTeacherStudentPager();
    } catch (err) {
        console.error("Error loading students:", err);
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#ef4444;">Error loading students.</td></tr>`;
    }
}

function applyTeacherStudentFilters() {
    clearTimeout(_teacherStudentSearchDebounce);
    _teacherStudentSearchDebounce = setTimeout(() => fetchTeacherStudentPage(0), 300);
}

function changeTeacherStudentPage(delta) {
    const nextPage = _teacherStudentState.page + delta;
    if (nextPage < 0 || nextPage >= _teacherStudentState.totalPages) {
        return;
    }
    fetchTeacherStudentPage(nextPage);
}

// ----------------- VIEW STUDENT -----------------
function viewStudent(rollNo) {
    const student = _teacherStudentState.rowByRoll.get(String(rollNo));
    if (!student) return alert("Student not found in current page");

    const mapping = {
        modalRollNo: student.rollNo,
        modalName: student.name,
        modalClass: student.className,
        modalSubject: student.subject,
        modalStatus: student.status
    };
    Object.entries(mapping).forEach(([id, value]) => {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = value || "-";
        }
    });

    const viewModal = document.getElementById("viewModal");
    if (viewModal) viewModal.style.display = "block";
}

function closeModal() {
    const viewModal = document.getElementById("viewModal");
    if (viewModal) viewModal.style.display = "none";
}

// ----------------- EDIT STUDENT -----------------
function editStudent(rollNo) {
    console.log("Edit clicked:", rollNo);

    const student = _teacherStudentState.rowByRoll.get(String(rollNo));
    if (!student) return alert("Student not found in current page");

    const modalFields = [
        ["editAttendanceId", student.id],
        ["editRollNo", student.rollNo],
        ["editName", student.name],
        ["editClass", student.className],
        ["editSubject", student.subject],
        ["editStatus", student.status || "Present"]
    ];

    modalFields.forEach(([id, value]) => {
        const el = document.getElementById(id);
        if (el) el.value = value || "";
    });

    const editModal = document.getElementById("editModal");
    if (editModal) editModal.style.display = "block";
}

// ----------------- UPDATE STUDENT -----------------
function updateStudent() {
    const rollNoEl = document.getElementById("editRollNo");
    if (!rollNoEl) return alert("Roll number input missing");

    const rollNo = rollNoEl.value;

    const data = {
        subject: document.getElementById("editSubject")?.value || "",
        status: document.getElementById("editStatus")?.value || ""
    };

    fetch(`/api/attendance/teacher/update/${rollNo}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
        .then(res => res.text())
        .then(msg => {
            alert(msg);
            const editModal = document.getElementById("editModal");
            if (editModal) editModal.style.display = "none";
            fetchTeacherStudentPage(_teacherStudentState.page);
        })
        .catch(err => console.error("Update error:", err));
}

function closeEditModal() {
    document.getElementById("editModal").style.display = "none";
}

// ============================================
// Teacher Profile & Department Selection
// ============================================

// [Redundant function removed - logic consolidated into lines 594-635]

function refreshTeacherProfileData(identifier) {
    if (identifier) fetchTeacherData(identifier);
}

function toggleDepartmentEdit() {
    const editSection = document.getElementById('departmentEditSection');
    const isEditing = editSection.style.display === 'block';

    if (!isEditing) {
        // Show and populate
        editSection.style.display = 'block';

        // Fetch departments
        fetch('/api/master/departments')
            .then(res => res.json())
            .then(departments => {
                const select = document.getElementById('teacherDepartmentSelect');
                select.innerHTML = '<option value="">-- No Department (School Mode) --</option>';

                departments.forEach(dept => {
                    const opt = document.createElement('option');
                    opt.value = dept.id;
                    opt.textContent = dept.departmentName;

                    // Pre-select if matches current
                    if (window.currentTeacherObj && window.currentTeacherObj.department && window.currentTeacherObj.department.id === dept.id) {
                        opt.selected = true;
                    }

                    select.appendChild(opt);
                });
            })
            .catch(err => console.error("Failed to load departments:", err));

    } else {
        editSection.style.display = 'none';
    }
}

function saveTeacherDepartment() {
    const select = document.getElementById('teacherDepartmentSelect');
    const deptId = select.value ? parseInt(select.value) : null;

    if (!window.currentTeacherId) return;

    fetch(`/api/teachers/${window.currentTeacherId}/department`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ departmentId: deptId })
    })
        .then(res => {
            if (!res.ok) throw new Error("Failed to update department");
            return res.json();
        })
        .then(updatedTeacher => {
            // Hide edit section
            document.getElementById('departmentEditSection').style.display = 'none';
            // Refresh UI
            updateTeacherProfileUI(updatedTeacher);
            // Update local storage so names are consistent
            const userData = JSON.parse((localStorage.getItem('teacherLoggedUser') || localStorage.getItem('loggedUser')));
            userData.department = updatedTeacher.department;
            localStorage.setItem('teacherLoggedUser', JSON.stringify(userData));

            alert("Department updated successfully!");
        })
        .catch(err => {
            console.error(err);
            alert("Error updating department.");
        });
}


function deleteStudent(id) {

    if (!confirm("Are you sure you want to delete this student?")) return;

    fetch(`/api/attendance/users/${id}`, {
        method: "DELETE"
    })
        .then(res => res.text())
        .then(() => {
            alert("Student deleted successfully ✅");
            fetchTeacherStudentPage(_teacherStudentState.page);
        })
        .catch(err => console.error("Delete error:", err));
}



// Load Settings content
function loadSettingsContent() {
    setupSettingsTab();
}

function saveSettings() {

    const settingsData = {
        attendanceThreshold: parseInt(document.getElementById("attendanceThreshold").value),
        lateArrivalMinutes: parseInt(document.getElementById("lateArrivalMinutes").value),
        autoMarkAbsentMinutes: parseInt(document.getElementById("autoMarkAbsentMinutes").value),
        manualOverride: document.getElementById("manualOverride").checked,
        sendAlerts: document.getElementById("sendAlerts").checked
    };

    fetch("/api/settings/save", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(settingsData)
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Server error: " + res.status);
            }
            return res.json();
        })
        .then(data => {
            alert("Settings saved successfully ✅");
        })
        .catch(error => {
            console.error("Save error:", error);
            alert("Error saving settings ❌");
        });
}

// Add new student
function addNewStudent() {
    const name = document.getElementById('newStudentName').value;
    const rollNo = document.getElementById('newStudentId').value;
    const classId = document.getElementById('newStudentClass').value;
    const divisionId = document.getElementById('newStudentDivision').value;
    const email = document.getElementById('newStudentEmail').value;
    const address = document.getElementById('newStudentAddress').value;
    const mobilenumber = document.getElementById('newStudentPhone').value;

    if (!name || !rollNo || !classId || !divisionId || !email) {
        alert('Please fill all required fields');
        return;
    }

    const data = {
        name: name,
        rollNo: rollNo,
        classMaster: { id: parseInt(classId) },
        divisionMaster: { id: parseInt(divisionId) },
        password: rollNo,
        email: email,
        address: address,
        mobilenumber: mobilenumber
    };

    fetch("/api/attendance/add-student", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    })
        .then(res => {
            if (!res.ok) throw new Error("Failed to add student");
            return res.text();
        })
        .then(msg => {
            alert(msg);
            document.getElementById('addStudentForm').reset();
            document.getElementById('newStudentDivision').disabled = true;
            loadStudentsContent();
            showSubTab('students-tab', 'student-list');
        })
        .catch(err => {
            console.error("Add student error:", err);
            alert("Error adding student");
        });
}

// Save manual attendance
function saveManualAttendance() {

    const rollNo = document.getElementById('manualStudentId').value;
    const subject = document.getElementById('manualSubjectSelect').value;
    const status = document.getElementById('attendanceStatusSelect').value;

    if (!rollNo || !subject || !status) {
        alert("Please fill all required fields");
        return;
    }

    const params = new URLSearchParams({
        rollNo: rollNo,
        subject: subject,
        status: status
    });

    fetch("/api/attendance/manual?" + params.toString(), {
        method: "POST"
    })
        .then(res => res.json())
        .then(data => {
            alert(data.message);
            document.getElementById('manualAttendanceForm').reset();
            loadStudentsContent();
        })
        .catch(err => {
            console.error("Manual attendance error:", err);
            alert("❌ Error saving attendance");
        });
}
// Open edit profile modal
function openEditProfileModal() {
    const modal = document.getElementById('editTeacherProfileModal');
    if (modal) {
        modal.style.display = 'flex';

        // Populate form with current data
        const userData = JSON.parse((localStorage.getItem('teacherLoggedUser') || localStorage.getItem('loggedUser')));
        document.getElementById('editTeacherName').value = userData?.name || '';
        document.getElementById('editTeacherEmail').value = userData?.email || '';
        document.getElementById('editTeacherPhone').value = userData?.mobilenumber || '';
        document.getElementById('editTeacherDepartment').value = userData?.department || '';
    }
}

// Generate QR for specific class
function generateQRForClass(className, division) {
    // Switch to QR generator tab
    showTab('qr-generator');

    // Pre-select class and division
    const classSelect = document.getElementById('classSelect');
    const divisionSelect = document.getElementById('divisionSelect');

    if (classSelect && divisionSelect) {
        // Find and select the class
        for (let i = 0; i < classSelect.options.length; i++) {
            if (classSelect.options[i].text.includes(className)) {
                classSelect.selectedIndex = i;
                break;
            }
        }

        // Select division
        divisionSelect.value = division;

        // Auto-generate QR after a short delay
        setTimeout(() => {
            document.getElementById('generateQRBtn').click();
        }, 100);
    }
}

let reportData = [];
function generateReport() {

    const classElement = document.getElementById('reportClass');

    if (!classElement) {
        alert("Report class dropdown not found!");
        return;
    }

    const classFilter = classElement.value;

    if (!classFilter) {
        alert("Please select class");
        return;
    }

    fetch(`/api/attendance/report?className=${encodeURIComponent(classFilter)}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch report");
            return res.json();
        })
        .then(data => {

            const table = document.getElementById("reportTable");
            const tbody = document.getElementById("reportTableBody");

            tbody.innerHTML = "";

            if (data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" class="text-center">No Data Found</td></tr>`;
                table.style.display = "table";
                return;
            }

            data.forEach(r => {
                const row = `
                    <tr>
                        <td>${r.date}</td>
                        <td>${r.subject}</td>
                        <td>${r.rollNo}</td>
                        <td>${r.name}</td>
                        <td>${r.status}</td>
                    </tr>
                `;
                tbody.innerHTML += row;
            });

            table.style.display = "table";

        })
        .catch(err => {
            console.error("Report error:", err);
            alert("Error generating report");
        });
}

function downloadReport() {

    const classElement = document.getElementById('reportClass');

    if (!classElement) {
        alert("Report class dropdown not found!");
        return;
    }

    const classFilter = classElement.value;

    if (!classFilter) {
        alert("Please select class");
        return;
    }

    // Fetch again separately
    fetch(`/api/attendance/report?className=${encodeURIComponent(classFilter)}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch report");
            return res.json();
        })
        .then(data => {

            if (data.length === 0) {
                alert("No data available to download");
                return;
            }

            let csvContent = "Date,Subject,Roll No,Name,Status\n";

            data.forEach(r => {
                csvContent += `${r.date},${r.subject},${r.rollNo},${r.name},${r.status}\n`;
            });

            const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });

            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = "attendance_report.csv";

            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

        })
        .catch(err => {
            console.error("Download error:", err);
            alert("Error downloading report");
        });
}

function generateAnalytics() {

    const type = document.getElementById("analysisType").value;

    if (!type) {
        alert("Please select analysis type");
        return;
    }

    let url = "";

    if (type === "subject") {
        url = "/api/attendance/analytics/subject";
    }
    else if (type === "department") {
        url = "/api/attendance/analytics/department";
    }
    else if (type === "date") {
        url = "/api/attendance/analytics/date";
    }

    fetch(url)
        .then(res => {
            if (!res.ok) {
                throw new Error("Server Error: " + res.status);
            }
            return res.json();
        })
        .then(data => {

            if (!data || data.length === 0) {
                alert("No Data Found");
                return;
            }

            const labels = [];
            const presentData = [];
            const absentData = [];

            data.forEach(item => {

                if (type === "date") {
                    labels.push(item.date);   // Date label
                } else {
                    labels.push(item.subject); // Subject or Department
                }

                presentData.push(item.present);
                absentData.push(item.absent);
            });

            const ctx = document.getElementById("attendanceChart").getContext("2d");

            if (attendanceChart !== null) {
                attendanceChart.destroy();
            }

            attendanceChart = new Chart(ctx, {
                type: "bar",
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: "Present",
                            data: presentData,
                            backgroundColor: "#4CAF50"
                        },
                        {
                            label: "Absent",
                            data: absentData,
                            backgroundColor: "#F44336"
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: {
                        title: {
                            display: true,
                            text: type === "subject"
                                ? "Subject-wise Attendance Analysis"
                                : type === "department"
                                    ? "Department-wise Attendance Analysis"
                                    : "Date-wise Attendance Analysis"
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });

        })
        .catch(err => {
            console.error("Analytics error:", err);
            alert("Error generating analytics.");
        });
}


// Edit class
function editClass(classId) {
    alert(`Edit class ${classId} - This would open edit modal in a real application`);
}

// Delete class
function deleteClass(classId) {
    if (confirm('Are you sure you want to delete this class?')) {
        let classes = JSON.parse(localStorage.getItem('teacherClasses') || '[]');
        classes = classes.filter(cls => cls.id !== classId);
        localStorage.setItem('teacherClasses', JSON.stringify(classes));

        // Reload classes
        loadClassesContent();
        loadDashboardContent();

        alert('Class deleted successfully!');
    }
}

// Logout function
function logout() {
    localStorage.removeItem('teacherAuthToken'); localStorage.removeItem('teacherLoggedUser'); localStorage.removeItem('teacherRole'); localStorage.removeItem('authToken'); localStorage.removeItem('loggedUser'); localStorage.removeItem('role');
    window.location.href = "login.html";
}

function finalizeAttendance() {
    const lectureSelect = document.getElementById("lectureSelect");

    if (!lectureSelect) {
        alert("Lecture selection not found");
        return;
    }

    const selectedOpt = lectureSelect.options[lectureSelect.selectedIndex];
    if (!selectedOpt || !selectedOpt.value) {
        alert("Please select today's lecture from the dropdown before finalizing.");
        return;
    }

    const subjectId = selectedOpt.dataset.subjectId || '';
    const classId = selectedOpt.dataset.classId || '';
    const divisionId = selectedOpt.dataset.divisionId || '';

    console.log("FINALIZE →", subjectId, classId, divisionId);

    fetch(
        `/api/attendance/finalize?subject=${encodeURIComponent(subjectId)}&className=${encodeURIComponent(classId)}&divisionId=${encodeURIComponent(divisionId)}`,
        { method: "POST" }
    )
        .then(res => {
            if (!res.ok) throw new Error("Failed");
            return res.text();
        })
        .then(msg => alert(msg))
        .catch(err => {
            console.error(err);
            alert("Error finalizing attendance");
        });
}


// Upload Notes
// ==============================
// Upload Notes Function (FINAL)
// ==============================



async function loadTeacherSubjectsForNotes() {
    const teacherId = getTeacherId();
    if (!teacherId) return;

    try {
        const res = await fetch(`/api/teacher/timetable/${teacherId}`);
        if (!res.ok) {
            console.warn("Could not load timetable for note subjects:", res.status);
            return;
        }
        const ttCells = await res.json();
        const cells = Array.isArray(ttCells) ? ttCells : [];

        const subjectDropdown = document.getElementById("notesSubject");
        if (!subjectDropdown) return;

        subjectDropdown.innerHTML = `<option value="">Select Subject</option>`;
        const uniqueSubjects = new Set();
        cells.forEach(cell => {
            if (cell && cell.subjectMaster && cell.subjectMaster.subjectName) {
                uniqueSubjects.add(cell.subjectMaster.subjectName);
            }
        });

        uniqueSubjects.forEach(sub => {
            subjectDropdown.innerHTML += `<option value="${sub}">${sub}</option>`;
        });
    } catch (err) {
        console.error("Error loading note subjects:", err);
    }
}

// --- Note Filter Dropdowns ---
async function loadNotesFilters() {
    try {
        const res = await fetch("/api/master/departments");
        if (!res.ok) throw new Error("Failed to fetch departments: " + res.status);
        const depts = await res.json();
        const select = document.getElementById("notesDepartment");
        if (!select) return;

        select.innerHTML = '<option value="">Select Department</option>';
        depts.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d.id;
            opt.textContent = d.departmentName;
            select.appendChild(opt);
        });

        select.addEventListener("change", function () {
            loadNotesClasses(this.value);
        });
    } catch (err) { console.error("Error loading departments for notes:", err); }
}

async function loadNotesClasses(deptId) {
    const select = document.getElementById("notesClass");
    const divSelect = document.getElementById("notesDivision");
    if (!select) return;

    select.innerHTML = '<option value="">Select Class</option>';
    divSelect.innerHTML = '<option value="">Select Division</option>';
    divSelect.disabled = true;

    if (!deptId) {
        select.disabled = true;
        return;
    }

    try {
        const res = await fetch("/api/master/classes");
        let classes = await res.json();

        // Filter classes by department
        classes = classes.filter(c => c.department && c.department.id == deptId);

        if (classes.length === 0) {
            select.innerHTML = '<option value="">No Classes Found</option>';
            select.disabled = true;
            return;
        }

        classes.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.className;
            select.appendChild(opt);
        });
        select.disabled = false;

        // Clear previous listeners if any (though best to add once)
        select.onchange = function () {
            loadNotesDivisions(this.value);
        };
    } catch (err) { console.error("Error loading classes for notes:", err); }
}

async function loadNotesDivisions(classId) {
    const select = document.getElementById("notesDivision");
    if (!select) return;

    select.innerHTML = '<option value="">Select Division</option>';
    if (!classId) {
        select.disabled = true;
        return;
    }

    try {
        const res = await fetch(`/api/master/classes/${classId}/divisions`);
        const divs = await res.json();

        if (divs.length === 0) {
            select.innerHTML = '<option value="">No Divisions Found</option>';
            select.disabled = true;
            return;
        }

        divs.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d.id;
            opt.textContent = d.divisionName;
            select.appendChild(opt);
        });
        select.disabled = false;
    } catch (err) { console.error("Error loading divisions for notes:", err); }
}

function setupUploadNotes() {


    const form = document.getElementById("uploadNotesForm");

    if (!form) {
        console.error("Upload Notes form not found!");
        return;
    }

    form.addEventListener("submit", async function (e) {
        e.preventDefault();

        const subjectElement = document.getElementById("notesSubject");
        const fileElement = document.getElementById("notesFile");
        const deptElement = document.getElementById("notesDepartment");
        const classElement = document.getElementById("notesClass");
        const divElement = document.getElementById("notesDivision");

        if (!subjectElement || !fileElement || !deptElement || !classElement || !divElement) {
            alert("Form elements not found");
            return;
        }

        const subject = subjectElement.value.trim();
        const file = fileElement.files[0];
        const deptId = deptElement.value;
        const classId = classElement.value;
        const divId = divElement.value;

        if (!subject || !deptId || !classId || !divId) {
            alert("Please select subject, department, class and division");
            return;
        }

        if (!file) {
            alert("Please choose a file to upload");
            return;
        }

        try {
            const formData = new FormData();
            formData.append("subject", subject);
            formData.append("file", file);
            formData.append("departmentId", deptId);
            formData.append("classId", classId);
            formData.append("divisionId", divId);

            const res = await fetch("/api/notes/upload", {
                method: "POST",
                body: formData
            });

            if (!res.ok) throw new Error("Upload failed with status " + res.status);

            const message = await res.text();
            alert("✅ " + message);

            form.reset();

            // 🔹 Refresh the uploaded notes table
            loadUploadedNotes();

        } catch (error) {
            console.error("Upload Error:", error);
            alert("❌ Upload failed. Please try again.");
        }
    });
}
async function loadUploadedNotes() {
    try {
        const res = await fetch("/api/notes/all");
        if (!res.ok) throw new Error("Failed to fetch notes");

        const notes = await res.json();
        const tbody = document.getElementById("uploadedNotesTableBody");
        tbody.innerHTML = "";

        if (notes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;">No notes uploaded yet.</td></tr>`;
            return;
        }
        notes.forEach(note => {
            const className = note.classMaster ? note.classMaster.className : "-";
            const divisionName = note.divisionMaster ? note.divisionMaster.divisionName : "-";
            const uploadTime = note.uploadTime ? new Date(note.uploadTime).toLocaleString() : "-";

            tbody.innerHTML += `
                <tr>
                    <td>${note.subject}</td>
                    <td>${className}</td>
                    <td>${divisionName}</td>
                    <td>${note.fileName}</td>
                    <td>${uploadTime}</td>
                    <td>
                        <a href="${note.fileUrl}" target="_blank" class="btn btn-sm btn-success">View</a>
                        <button onclick="deleteNote('${note.id}')" class="btn btn-sm btn-danger">Delete</button>
                    </td>
                </tr>
            `;
        });
    } catch (err) {
        console.error(err);
        alert("Error loading uploaded notes");
    }
}

async function deleteNote(id) {
    if (!confirm("Are you sure you want to delete this note?")) return;

    try {
        const res = await fetch(`/api/notes/delete/${id}`, {
            method: "DELETE"
        });

        if (!res.ok) throw new Error("Delete failed");

        const msg = await res.text();
        alert("✅ " + msg);
        loadUploadedNotes();
    } catch (err) {
        console.error("Delete Error:", err);
        alert("❌ Failed to delete note");
    }
}

// Export functions for onclick attributes
window.logout = logout;
window.deleteNote = deleteNote;
window.generateReport = generateReport;
window.editClass = editClass;
window.deleteClass = deleteClass;
window.generateQRForClass = generateQRForClass;
window.approveLeave = approveLeave;
window.rejectLeave = rejectLeave;
window.loadLeaveRequests = loadLeaveRequests;

// ================= SETTINGS NAVIGATION =================


function loadLeaveRequests() {
    const teacherId = getTeacherId();
    if (!teacherId) return;

    fetch(`/api/leave/teacher/${teacherId}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch leaves");
            return res.json();
        })
        .then(data => {
            const tbody = document.getElementById("leaveRequestsTableBody");
            if (!tbody) return;
            tbody.innerHTML = "";

            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;">No Leave Requests found for you.</td></tr>`;
                return;
            }

            data.forEach(leave => {
                const row = document.createElement("tr");
                row.innerHTML = `
                  <td>${leave.studentName}</td>
                  <td>${leave.className}</td>
                  <td>${leave.fromDate}</td>
                  <td>${leave.toDate}</td>
                  <td>${leave.reason}</td>
                  <td><span class="status-badge ${leave.status.toLowerCase()}">${leave.status}</span></td>
                  <td>
                    ${leave.status === "Pending"
                        ? `
                          <button class="btn btn-sm btn-success" onclick="approveLeave(${leave.id})">Approve</button>
                          <button class="btn btn-sm btn-danger" onclick="rejectLeave(${leave.id})">Reject</button>
                        `
                        : "-"
                    }
                  </td>
                `;
                tbody.appendChild(row);
            });
        })
        .catch(err => console.error("Teacher load error:", err));
}

function loadAllLeaves() {
    loadLeaveRequests();
}

function approveLeave(id) {
    fetch(`/api/leave/approve/${id}`, {
        method: "PUT"
    })
        .then(res => res.text())
        .then(message => {
            alert(message);
            loadLeaveRequests(); // Refresh table
        })
        .catch(error => console.error("Approve error:", error));
}

function rejectLeave(id) {
    fetch(`/api/leave/reject/${id}`, {
        method: "PUT"
    })
        .then(res => res.text())
        .then(message => {
            alert(message);
            loadLeaveRequests(); // Refresh table
        })
        .catch(error => console.error("Reject error:", error));
}

// =====================================================
// REPORTS TAB — Sub-tab switching
// =====================================================
function setupReportsTab() {
    const reportTabs = document.querySelectorAll('#report-subtabs .tab');
    reportTabs.forEach(tab => {
        tab.addEventListener('click', function () {
            const subtab = this.getAttribute('data-subtab');
            // switch tabs in the reports tab
            document.querySelectorAll('#reports-tab .tab-content').forEach(tc => tc.classList.remove('active'));
            const target = document.getElementById(subtab);
            if (target) target.classList.add('active');
            // update active tab highlight
            reportTabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

function loadReportsContent() {
    setupReportsTab();
    setupReportStudentFilters();
    setupConsolidatedReportFilters();
}

async function setupConsolidatedReportFilters() {
    try {
        const consolidatedSelect = document.getElementById('consolidatedReportClass');
        const legacySelect = document.getElementById('reportClass');

        const res = await fetch('/api/master/classes');
        if (!res.ok) return;
        const classes = await res.json();

        if (consolidatedSelect) {
            consolidatedSelect.innerHTML = '<option value="">Select Class</option>';
            classes.forEach(c => {
                consolidatedSelect.innerHTML += `<option value="${c.id}">${c.className}</option>`;
            });
        }

        if (legacySelect) {
            legacySelect.innerHTML = '<option value="">Select Class</option>';
            classes.forEach(c => {
                legacySelect.innerHTML += `<option value="${c.className}">${c.className}</option>`;
            });
        }
    } catch (e) {
        console.error('Consolidated filters error:', e);
    }
}

async function setupReportStudentFilters() {
    try {
        const classSelector = document.getElementById('reportStudentFilterClass');
        const divSelector = document.getElementById('reportStudentFilterDivision');
        if (!classSelector || !divSelector) return;

        classSelector.innerHTML = '<option value="">All Classes</option>';
        divSelector.innerHTML = '<option value="">All Divisions</option>';

        const res = await fetch('/api/master/classes');
        const classes = await res.json();
        classes.forEach(c => {
            classSelector.innerHTML += `<option value="${c.id}">${c.className}</option>`;
        });

        classSelector.onchange = async () => {
            const classId = classSelector.value;
            divSelector.innerHTML = '<option value="">All Divisions</option>';
            if (!classId) return;

            const divsRes = await fetch(`/api/master/classes/${classId}/divisions`);
            const divs = await divsRes.json();
            divs.forEach(d => {
                divSelector.innerHTML += `<option value="${d.id}">${d.divisionName}</option>`;
            });
        };
    } catch (e) {
        console.error("Report filters error:", e);
    }
}

// =====================================================
// TEACHER SUBJECT REPORT
// =====================================================
let subjectChartInstance = null;

async function loadTeacherSubjectReport() {
    const teacherId = getTeacherId();
    if (!teacherId) {
        document.getElementById('subjectReportStatus').textContent = '⚠️ Not logged in.';
        return;
    }
    document.getElementById('subjectReportStatus').textContent = 'Loading…';
    try {
        const res = await fetch(`/api/attendance/reports/subjects?teacherId=${teacherId}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderSubjectReportTable(data);
        renderSubjectReportChart(data);
        document.getElementById('subjectReportStatus').textContent = `✅ ${data.length} subject(s) found.`;
    } catch (err) {
        console.error('Subject report error:', err);
        document.getElementById('subjectReportStatus').textContent = '❌ Failed to load. Check console.';
    }
}

function renderSubjectReportTable(data) {
    const tbody = document.getElementById('subjectReportBody');
    const table = document.getElementById('subjectReportTable');
    tbody.innerHTML = '';
    if (!data.length) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#aaa;">No data found</td></tr>';
    } else {
        data.forEach(row => {
            const pct = row.percentage.toFixed(1);
            const badge = pct >= 75
                ? `<span style="color:#22c55e;font-weight:600;">✅ Good</span>`
                : `<span style="color:#ef4444;font-weight:600;">⚠️ Low</span>`;
            tbody.innerHTML += `<tr>
                <td>${row.subject}</td>
                <td>${row.presentCount}</td>
                <td>${row.totalCount}</td>
                <td><strong>${pct}%</strong></td>
                <td>${badge}</td>
            </tr>`;
        });
    }
    table.style.display = 'table';
}

function renderSubjectReportChart(data) {
    const canvas = document.getElementById('subjectReportChart');
    if (!canvas) return;
    if (subjectChartInstance) subjectChartInstance.destroy();
    subjectChartInstance = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: data.map(d => d.subject),
            datasets: [{
                label: 'Attendance %',
                data: data.map(d => d.percentage.toFixed(1)),
                backgroundColor: data.map(d => d.percentage >= 75 ? 'rgba(34,197,94,0.7)' : 'rgba(239,68,68,0.7)'),
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            scales: { y: { min: 0, max: 100, ticks: { callback: v => v + '%' } } },
            plugins: { legend: { display: false }, title: { display: true, text: 'Subject-wise Attendance %' } }
        }
    });
}

// =====================================================
// TEACHER CLASS REPORT
// =====================================================
let classChartInstance = null;

async function loadTeacherClassReport() {
    const teacherId = getTeacherId();
    if (!teacherId) {
        document.getElementById('classReportStatus').textContent = '⚠️ Not logged in.';
        return;
    }
    document.getElementById('classReportStatus').textContent = 'Loading…';
    try {
        const res = await fetch(`/api/attendance/reports/classes?teacherId=${teacherId}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderClassReportTable(data);
        renderClassReportChart(data);
        document.getElementById('classReportStatus').textContent = `✅ ${data.length} class(es) found.`;
    } catch (err) {
        console.error('Class report error:', err);
        document.getElementById('classReportStatus').textContent = '❌ Failed to load.';
    }
}

function renderClassReportTable(data) {
    const tbody = document.getElementById('classReportBody');
    const table = document.getElementById('classReportTable');
    tbody.innerHTML = '';
    if (!data.length) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#aaa;">No data found</td></tr>';
    } else {
        data.forEach(row => {
            const pct = row.percentage.toFixed(1);
            const badge = pct >= 75
                ? `<span style="color:#22c55e;font-weight:600;">✅ Good</span>`
                : `<span style="color:#ef4444;font-weight:600;">⚠️ Low</span>`;
            tbody.innerHTML += `<tr>
                <td>${row.className}</td>
                <td>${row.presentCount}</td>
                <td>${row.totalCount}</td>
                <td><strong>${pct}%</strong></td>
                <td>${badge}</td>
            </tr>`;
        });
    }
    table.style.display = 'table';
}

function renderClassReportChart(data) {
    const canvas = document.getElementById('classReportChart');
    if (!canvas) return;
    if (classChartInstance) classChartInstance.destroy();
    classChartInstance = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: data.map(d => d.className),
            datasets: [{
                label: 'Attendance %',
                data: data.map(d => d.percentage.toFixed(1)),
                backgroundColor: data.map(d => d.percentage >= 75 ? 'rgba(59,130,246,0.7)' : 'rgba(239,68,68,0.7)'),
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            scales: { y: { min: 0, max: 100, ticks: { callback: v => v + '%' } } },
            plugins: { legend: { display: false }, title: { display: true, text: 'Class-wise Attendance %' } }
        }
    });
}

// =====================================================
// TEACHER — STUDENT LIST
// =====================================================
async function loadTeacherStudentList() {
    const teacherId = getTeacherId();
    const classId = document.getElementById('reportStudentFilterClass')?.value;
    const divisionId = document.getElementById('reportStudentFilterDivision')?.value;

    if (!teacherId) {
        document.getElementById('studentListStatus').textContent = '⚠️ Not logged in.';
        return;
    }
    document.getElementById('studentListStatus').textContent = 'Loading…';
    try {
        let url = `/api/attendance/reports/students?teacherId=${teacherId}`;
        if (classId) url += `&classId=${classId}`;
        if (divisionId) url += `&divisionId=${divisionId}`;

        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const students = await res.json();

        const picker = document.getElementById('studentPickerSelect');
        picker.innerHTML = '<option value="">-- Select Student --</option>';
        if (!students.length) {
            document.getElementById('studentFilterRow').style.display = 'none';
            document.getElementById('studentListStatus').textContent = 'No students found for the selected filters.';
            return;
        }
        students.forEach(s => {
            picker.innerHTML += `<option value="${s.id}">${s.name} (${s.rollNo || s.id})</option>`;
        });

        document.getElementById('studentFilterRow').style.display = 'flex';
        document.getElementById('studentListStatus').textContent = `✅ ${students.length} student(s) loaded.`;
    } catch (err) {
        console.error('Student list error:', err);
        document.getElementById('studentListStatus').textContent = '❌ Failed to load.';
    }
}

// =====================================================
// TEACHER — STUDENT SUBJECT SUMMARY
// =====================================================
async function loadStudentSubjectSummary() {
    const studentId = document.getElementById('studentPickerSelect').value;
    if (!studentId) { alert('Please select a student.'); return; }

    try {
        const classId = document.getElementById('reportStudentFilterClass')?.value;
        let url = `/api/attendance/reports/student/${studentId}/summary`;
        if (classId) url += `?classId=${classId}`;

        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        const tbody = document.getElementById('studentSubjectSummaryBody');
        tbody.innerHTML = '';
        data.forEach(row => {
            const pct = row.percentage.toFixed(1);
            const color = pct >= 75 ? '#22c55e' : '#ef4444';
            tbody.innerHTML += `<tr>
                <td>${row.subject}</td>
                <td>${row.presentCount}</td>
                <td>${row.totalClasses}</td>
                <td style="color:${color};font-weight:600;">${pct}%</td>
            </tr>`;
        });

        document.getElementById('studentSubjectSummarySection').style.display = 'block';
        document.getElementById('studentDateRecordsSection').style.display = 'none';
    } catch (err) {
        console.error('Student summary error:', err);
        alert('Failed to load student summary.');
    }
}

// =====================================================
// TEACHER — STUDENT DATE-WISE RECORDS
// =====================================================
async function loadStudentDateRecords() {
    const studentId = document.getElementById('studentPickerSelect').value;
    if (!studentId) { alert('Please select a student.'); return; }

    const subject = document.getElementById('studentSubjectFilter').value.trim() || null;
    const from = document.getElementById('studentFromDate').value || null;
    const to = document.getElementById('studentToDate').value || null;

    const classId = document.getElementById('reportStudentFilterClass')?.value;

    let url = `/api/attendance/reports/student/${studentId}/records`;
    const params = new URLSearchParams();
    if (subject) params.append('subject', subject);
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    if (classId) params.append('classId', classId);
    if ([...params].length) url += '?' + params.toString();

    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        const tbody = document.getElementById('studentDateRecordsBody');
        tbody.innerHTML = '';
        if (!data.length) {
            tbody.innerHTML = '<tr><td colspan="3" style="text-align:center;color:#aaa;">No records found</td></tr>';
        } else {
            data.forEach(row => {
                const statusColor = row.status?.toLowerCase() === 'present' ? '#22c55e' : '#ef4444';
                tbody.innerHTML += `<tr>
                    <td>${row.date}</td>
                    <td>${row.subject}</td>
                    <td style="color:${statusColor};font-weight:600;">${row.status}</td>
                </tr>`;
            });
        }

        document.getElementById('studentDateRecordsSection').style.display = 'block';
        document.getElementById('studentSubjectSummarySection').style.display = 'none';
    } catch (err) {
        console.error('Date records error:', err);
        alert('Failed to load date records.');
    }
}

// =====================================================
// MY TIMETABLE — Grid Builder
// =====================================================

/** Teacher's existing timetable cells (keyed by slotId_day) */
let timetableData = {};

/**
 * Build and render the Mon-Sat timetable grid for the logged-in teacher.
 * Structure rows come from /api/admin/timetable/structure (ordered by slot_order).
 * Teacher's filled cells come from /api/teacher/timetable/{teacherId}.
 */
async function loadTimetableGrid() {
    const teacherId = getTeacherId();
    if (!teacherId) {
        document.getElementById('timetableStatus').textContent = '⚠️ Not logged in.';
        return;
    }

    const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

    try {
        timetableData = {};

        // 1. Load timetable structure (period/break rows defined by admin)
        const authToken = (localStorage.getItem('teacherAuthToken') || localStorage.getItem('authToken')) || '';
        const structRes = await fetch('/api/teacher/timetable/structure', {
            headers: { 'Authorization': 'Bearer ' + authToken }
        });
        if (!structRes.ok) throw new Error('Could not load structure: ' + structRes.status);

        const structurePayload = await structRes.json();
        const slots = Array.isArray(structurePayload) ? structurePayload : [];

        if (!slots.length) {
            document.getElementById('timetableStatus').textContent = '⚠️ No timetable structure found. Ask admin to set it up.';
            return;
        }

        // 2. Load teacher's filled cells
        const ttRes = await fetch(`/api/teacher/timetable/${teacherId}`, {
            headers: { 'Authorization': 'Bearer ' + authToken }
        });
        if (ttRes.ok) {
            const cellsPayload = await ttRes.json();
            const cells = Array.isArray(cellsPayload) ? cellsPayload : [];
            cells.forEach(cell => {
                if (!cell || !cell.slot || !cell.slot.id || !cell.dayOfWeek) return;
                timetableData[`${cell.slot.id}_${cell.dayOfWeek}`] = cell;
            });
        } else {
            console.warn('Could not load existing timetable cells:', ttRes.status);
        }

        // 3. Build header row
        const header = document.getElementById('timetableHeader');
        header.innerHTML = `<th style="padding:10px 12px;background:#1e1a3a;color:white;border-radius:8px;min-width:130px;">Period / Day</th>`;
        DAY_LABELS.forEach(d => {
            header.innerHTML += `<th style="padding:10px 16px;background:#322e72;color:white;border-radius:8px;min-width:150px;">${d}</th>`;
        });

        // 4. Build body rows (one per slot)
        const tbody = document.getElementById('timetableBody');
        tbody.innerHTML = '';

        // Pre-load all departments and classes for dropdowns
        const masterDeptsRes = await fetch('/api/master/departments');
        const masterDeptsPayload = masterDeptsRes.ok ? await masterDeptsRes.json() : [];
        const masterDepts = Array.isArray(masterDeptsPayload) ? masterDeptsPayload : [];

        const masterClassesRes = await fetch('/api/master/classes');
        const masterClassesPayload = masterClassesRes.ok ? await masterClassesRes.json() : [];
        const masterClasses = Array.isArray(masterClassesPayload) ? masterClassesPayload : [];

        slots.forEach(slot => {
            const isBreak = slot.slotType === 'BREAK';
            const bgRow = isBreak ? '#f5f5f8' : '#fff';
            const timeLabel = slot.startTime
                ? `<small style="color:#aaa;display:block;">${slot.startTime}${slot.endTime ? '–' + slot.endTime : ''}</small>`
                : '';

            let rowHtml = `<tr style="background:${bgRow};">
                <td style="padding:8px 12px;font-weight:600;border-radius:8px;background:${isBreak ? '#e8e5f5' : '#f3efff'};white-space:nowrap;">
                    ${slot.label}${timeLabel}
                </td>`;

            DAYS.forEach(day => {
                const cell = timetableData[`${slot.id}_${day}`];
                if (isBreak) {
                    rowHtml += `<td style="background:#f5f5f8;text-align:center;color:#bbb;border-radius:6px;padding:8px;">—</td>`;
                } else {
                    const clsId = cell?.classMaster?.id || '';
                    const deptId = cell?.classMaster?.department?.id || '';
                    const divId = cell?.divisionMaster?.id || '';
                    const subId = cell?.subjectMaster?.id || '';
                    const room = cell?.roomNo || '';

                    // Filter classes for this cell based on its department
                    const availableClasses = deptId
                        ? masterClasses.filter(c => c.department && c.department.id == deptId)
                        : masterClasses.filter(c => !c.department);

                    rowHtml += `<td style="padding:4px;border-radius:6px;">
                        <div style="display:flex;flex-direction:column;gap:3px;">
                            <select class="tt-select" data-slot="${slot.id}" data-day="${day}" data-field="departmentId"
                                onchange="onTtDepartmentChange(this)">
                                <option value="">Dept</option>
                                ${masterDepts.map(d => `<option value="${d.id}" ${deptId == d.id ? 'selected' : ''}>${d.departmentName}</option>`).join('')}
                            </select>
                            <select class="tt-select" data-slot="${slot.id}" data-day="${day}" data-field="classMasterId"
                                onchange="onTtClassChange(this)">
                                <option value="">Class</option>
                                ${availableClasses.map(c => `<option value="${c.id}" ${clsId == c.id ? 'selected' : ''}>${c.className}</option>`).join('')}
                            </select>
                            <select class="tt-select" data-slot="${slot.id}" data-day="${day}" data-field="divisionMasterId"
                                onchange="saveTimetableSlot(this)" ${!clsId ? 'disabled' : ''}>
                                <option value="">Div</option>
                                ${clsId ? '' : ''} 
                            </select>
                            <select class="tt-select" data-slot="${slot.id}" data-day="${day}" data-field="subjectMasterId"
                                onchange="saveTimetableSlot(this)" ${!clsId ? 'disabled' : ''}>
                                <option value="">Subject</option>
                            </select>
                            <input type="text" placeholder="Room"
                                value="${room}"
                                style="width:100%;padding:4px 6px;border:1px solid #ddd;border-radius:6px;font-size:0.82rem;"
                                data-slot="${slot.id}" data-day="${day}" data-field="roomNo"
                                onblur="saveTimetableSlot(this)">
                        </div>
                    </td>`;
                }
            });

            rowHtml += '</tr>';
            tbody.innerHTML += rowHtml;
        });

        // After building the table, trigger division/subject loads for existing data
        document.querySelectorAll('select[data-field="classMasterId"]').forEach(sel => {
            if (sel.value) onTtClassChange(sel, true);
        });

        document.getElementById('timetableGrid').style.display = 'table';
        document.getElementById('timetableStatus').textContent = `✅ Timetable loaded — ${slots.length} slot(s).`;

    } catch (err) {
        console.error('Timetable load error:', err);
        document.getElementById('timetableStatus').textContent = '❌ Error loading timetable. Check console.';
    }
}

/**
 * Auto-save a single timetable cell on blur.
 * Collects all 4 fields (className, division, subject, roomNo) for that slot+day.
 */
/**
 * When department is changed, load its classes and clear div/obj.
 */
async function onTtDepartmentChange(selectEl) {
    const deptId = selectEl.value;
    const slotId = selectEl.dataset.slot;
    const day = selectEl.dataset.day;

    const classSelect = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="classMasterId"]`);
    const divSelect = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="divisionMasterId"]`);
    const subSelect = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="subjectMasterId"]`);

    classSelect.innerHTML = '<option value="">Class</option>';
    divSelect.innerHTML = '<option value="">Div</option>';
    divSelect.disabled = true;
    subSelect.innerHTML = '<option value="">Subject</option>';
    subSelect.disabled = true;

    try {
        const masterClassesRes = await fetch('/api/master/classes');
        const masterClasses = await masterClassesRes.json();

        const availableClasses = deptId
            ? masterClasses.filter(c => c.department && c.department.id == deptId)
            : masterClasses.filter(c => !c.department);

        classSelect.innerHTML = '<option value="">Class</option>' + availableClasses.map(c => `<option value="${c.id}">${c.className}</option>`).join('');

        // Dept is just a filter, the cell state is actually saved when onblur/onchange of class/div/sub/room occurs,
        // but if class is cleared, we should save the blank state to the timetable.
        saveTimetableSlot(selectEl);
    } catch (e) { console.error(e); }
}

/**
 * When class is changed, load its divisions and subjects.
 * @param {HTMLSelectElement} selectEl 
 * @param {boolean} isInitialLoad If true, don't auto-save just populate.
 */
async function onTtClassChange(selectEl, isInitialLoad = false) {
    const classId = selectEl.value;
    const slotId = selectEl.dataset.slot;
    const day = selectEl.dataset.day;

    const divSelect = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="divisionMasterId"]`);
    const subSelect = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="subjectMasterId"]`);

    if (!classId) {
        divSelect.innerHTML = '<option value="">Div</option>';
        divSelect.disabled = true;
        subSelect.innerHTML = '<option value="">Subject</option>';
        subSelect.disabled = true;
        if (!isInitialLoad) saveTimetableSlot(selectEl);
        return;
    }

    try {
        divSelect.disabled = false;
        subSelect.disabled = false;

        // Fetch divisions
        const divsRes = await fetch(`/api/master/classes/${classId}/divisions`);
        const divs = await divsRes.json();

        // Fetch subjects
        const subsRes = await fetch(`/api/master/classes/${classId}/subjects`);
        const subs = await subsRes.json();

        // Get current values from timetableData
        const currentData = timetableData[`${slotId}_${day}`];
        const currentDivId = currentData?.divisionMaster?.id || '';
        const currentSubId = currentData?.subjectMaster?.id || '';

        divSelect.innerHTML = '<option value="">Div</option>' + divs.map(d => `<option value="${d.id}" ${currentDivId == d.id ? 'selected' : ''}>${d.divisionName}</option>`).join('');
        subSelect.innerHTML = '<option value="">Subject</option>' + subs.map(s => `<option value="${s.id}" ${currentSubId == s.id ? 'selected' : ''}>${s.subjectName}</option>`).join('');

        if (!isInitialLoad) saveTimetableSlot(selectEl);
    } catch (e) { console.error(e); }
}

/**
 * Auto-save a single timetable cell.
 */
async function saveTimetableSlot(changedInput) {
    const teacherId = getTeacherId();
    if (!teacherId) return;

    const slotId = changedInput.dataset.slot;
    const day = changedInput.dataset.day;

    const classVal = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="classMasterId"]`).value;
    const divVal = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="divisionMasterId"]`).value;
    const subVal = document.querySelector(`select[data-slot="${slotId}"][data-day="${day}"][data-field="subjectMasterId"]`).value;
    const roomVal = document.querySelector(`input[data-slot="${slotId}"][data-day="${day}"][data-field="roomNo"]`).value;

    const body = {
        classMaster: classVal ? { id: parseInt(classVal) } : null,
        divisionMaster: divVal ? { id: parseInt(divVal) } : null,
        subjectMaster: subVal ? { id: parseInt(subVal) } : null,
        roomNo: roomVal
    };

    try {
        const res = await fetch(`/api/teacher/timetable/${teacherId}/slot/${slotId}/${day}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (!res.ok) {
            const errText = await res.text().catch(() => '');
            throw new Error(`Save failed (${res.status}) ${errText}`);
        }
        const saved = await res.json();
        timetableData[`${slotId}_${day}`] = saved;

        changedInput.style.borderColor = '#22c55e';
        setTimeout(() => { changedInput.style.borderColor = '#ddd'; }, 1500);
    } catch (err) {
        console.error(err);
        changedInput.style.borderColor = '#ef4444';
    }
}

// =====================================================
// QR GENERATOR — Today's Lecture Dropdown
// =====================================================

/** Selected timetable slot id (used when creating QR session) */
let selectedTimetableSlotId = null;

/**
 * Load today's lecture slots into the #lectureSelect dropdown.
 * Called when the QR Generator tab becomes active.
 */
async function loadTodayLectures() {
    const teacherId = getTeacherId();
    const select = document.getElementById('lectureSelect');
    const manualSelect = document.getElementById('manualSessionSelect');
    const status = document.getElementById('lectureLoadStatus');
    if (!teacherId || !select) return;

    status.textContent = 'Loading today\'s lectures…';
    select.innerHTML = '<option value="">-- Select Today\'s Lecture --</option>';
    if (manualSelect) manualSelect.innerHTML = '<option value="">-- Select Today\'s Lecture --</option>';

    try {
        const res = await fetch(`/api/teacher/timetable/${teacherId}/today`);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const lectureSlots = await res.json();

        if (!lectureSlots.length) {
            status.textContent = '⚠️ No lectures found for today. Fill in your timetable first.';
            return;
        }

        lectureSlots.forEach(slot => {
            const label = `${slot.slot.label} — ${slot.subjectMaster ? slot.subjectMaster.subjectName : '?'} — ${slot.classMaster ? slot.classMaster.className : '?'} ${slot.divisionMaster ? slot.divisionMaster.divisionName : ''} — Room ${slot.roomNo || '?'}`;
            const opt = document.createElement('option');
            opt.value = slot.id;
            opt.textContent = label;
            // Stash the details as data attributes
            opt.dataset.subject = slot.subjectMaster ? slot.subjectMaster.subjectName : '';
            opt.dataset.className = slot.classMaster ? slot.classMaster.className : '';
            opt.dataset.division = slot.divisionMaster ? slot.divisionMaster.divisionName : '';
            opt.dataset.subjectId = slot.subjectMaster ? slot.subjectMaster.id : '';
            opt.dataset.classId = slot.classMaster ? slot.classMaster.id : '';
            opt.dataset.divisionId = slot.divisionMaster ? slot.divisionMaster.id : '';
            opt.dataset.room = slot.roomNo || '';
            select.appendChild(opt);
            
            if (manualSelect) {
                const mOpt = opt.cloneNode(true);
                manualSelect.appendChild(mOpt);
            }
        });

        status.textContent = `✅ ${lectureSlots.length} lecture(s) found for today.`;

        // Wire change handler for slot preview
        select.onchange = () => {
            const opt = select.options[select.selectedIndex];
            if (!opt.value) {
                selectedTimetableSlotId = null;
                document.getElementById('slotPreview').style.display = 'none';
                return;
            }
            selectedTimetableSlotId = parseInt(opt.value);
            document.getElementById('previewSubject').textContent = opt.dataset.subject || '—';
            document.getElementById('previewClass').textContent = opt.dataset.className || '—';
            document.getElementById('previewDivision').textContent = opt.dataset.division || '—';
            document.getElementById('previewRoom').textContent = opt.dataset.room || '—';
            document.getElementById('slotPreview').style.display = 'block';
        };

    } catch (err) {
        console.error('Today lectures error:', err);
        status.textContent = '❌ Failed to load. Check console.';
    }
}

// =====================================================
// CONSOLIDATED CLASS REPORT
// =====================================================
async function setupConsolidatedReportFilters() {
    try {
        const classSelector = document.getElementById('consolidatedReportClass');
        if (!classSelector) return;

        classSelector.innerHTML = '<option value="">Select Class</option>';

        const res = await fetch('/api/master/classes');
        const classes = await res.json();
        classes.forEach(c => {
            classSelector.innerHTML += `<option value="${c.id}">${c.className}</option>`;
        });
    } catch (e) {
        console.error("Consolidated report filters error:", e);
    }
}

async function loadConsolidatedReport() {
    const classId = document.getElementById('consolidatedReportClass').value;
    if (!classId) {
        alert("Please select a class");
        return;
    }

    const statusEl = document.getElementById('consolidatedReportStatus');
    statusEl.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Fetching consolidated data...';

    try {
        const res = await fetch(`/api/attendance/reports/consolidated?classId=${classId}`);
        if (!res.ok) throw new Error("Failed to fetch consolidated report");

        const data = await res.json();
        renderConsolidatedReport(data);
        statusEl.innerHTML = `<span style="color:#22c55e;">✅ Consolidated report loaded for ${data.students.length} students.</span>`;
    } catch (err) {
        console.error(err);
        statusEl.innerHTML = '<span style="color:#ef4444;">❌ Failed to load report. Check console.</span>';
    }
}

function renderConsolidatedReport(data) {
    const table = document.getElementById('consolidatedReportTable');
    const head = document.getElementById('consolidatedReportHead');
    const body = document.getElementById('consolidatedReportBody');

    head.innerHTML = '';
    body.innerHTML = '';

    // Build Header
    let headerRow = '<tr><th>Name</th><th>Roll No</th>';
    data.subjects.forEach(sub => {
        headerRow += `<th>${sub} %</th>`;
    });
    headerRow += '<th>Overall %</th></tr>';
    head.innerHTML = headerRow;

    // Build Body
    if (data.students.length === 0) {
        body.innerHTML = `<tr><td colspan="${data.subjects.length + 3}" style="text-align:center;">No students found in this class</td></tr>`;
    } else {
        data.students.forEach(s => {
            let row = `<tr><td>${s.name}</td><td>${s.rollNo}</td>`;
            data.subjects.forEach(sub => {
                const pct = s.subjectPercentages[sub] || 0;
                const color = pct >= 75 ? '#22c55e' : (pct > 0 ? '#f59e0b' : '#ef4444');
                row += `<td style="color:${color}; font-weight:600; padding:12px; border:1px solid #eee;">${pct.toFixed(1)}%</td>`;
            });
            const overallPct = s.overallPercentage || 0;
            const overallColor = overallPct >= 75 ? '#16a34a' : '#dc2626';
            row += `<td style="background:#f9fafb; font-weight:bold; color:${overallColor}; padding:12px; border:1px solid #eee;">${overallPct.toFixed(1)}%</td></tr>`;
            body.innerHTML += row;
        });
    }

    table.style.display = 'table';
}

async function emailConsolidatedReport(event) {
    const classId = document.getElementById('consolidatedReportClass').value;
    const email = document.getElementById('hodEmail').value.trim();

    if (!classId) {
        alert("Please select a class first");
        return;
    }
    if (!email || !email.includes('@')) {
        alert("Please enter a valid HOD email address");
        return;
    }

    const btn = document.querySelector('button[onclick="emailConsolidatedReport()"]');
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';

    try {
        const res = await fetch(`/api/attendance/reports/email?classId=${classId}&email=${encodeURIComponent(email)}`, {
            method: 'POST'
        });
        if (!res.ok) throw new Error("Failed to send email");

        alert("✅ Report sent successfully to " + email);
    } catch (err) {
        console.error(err);
        alert("❌ Failed to send email. Ensure your SMTP settings are configured in application.properties.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
}

// Global Tab Switcher for Reports
window.switchReportTab = function (tabEl) {
    const subtabsContainer = tabEl.closest('#report-subtabs');
    const contentContainer = document.getElementById('reports-tab');

    // Deactivate all tabs in this container
    subtabsContainer.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    // Activate clicked tab
    tabEl.classList.add('active');

    // Hide all subtab contents
    contentContainer.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

    // Show target subtab
    const targetId = tabEl.getAttribute('data-subtab');
    const targetEl = document.getElementById(targetId);
    if (targetEl) {
        targetEl.classList.add('active');
        // Trigger specific loads if needed
        if (targetId === 'consolidated-report-subtab') {
            setupConsolidatedReportFilters();
        } else if (targetId === 'monthly-report-subtab') {
            setupMonthlyReportFilters();
        }
    }
};

// =====================================================
// MONTHLY REPORT LOGIC
// =====================================================
async function setupMonthlyReportFilters() {
    try {
        const classSelector = document.getElementById('monthlyReportClass');
        const divSelector = document.getElementById('monthlyReportDivision');
        const yearInput = document.getElementById('monthlyReportYear');
        const monthInput = document.getElementById('monthlyReportMonth');

        if (!classSelector || !divSelector) return;

        // Set default month/year
        const now = new Date();
        if (yearInput && !yearInput.value) yearInput.value = now.getFullYear();
        if (monthInput && !monthInput.value) monthInput.value = now.getMonth() + 1;

        // Initial state: Division selector disabled until class is selected
        divSelector.innerHTML = '<option value="">-- Select Class First --</option>';
        divSelector.disabled = true;

        // Fetch classes
        const classRes = await fetch('/api/master/classes');
        const classes = await classRes.json();
        classSelector.innerHTML = '<option value="">-- Select Class --</option>' +
            classes.map(c => `<option value="${c.id}">${c.className}</option>`).join('');

        // Handle class change to load divisions
        classSelector.onchange = async () => {
            const classId = classSelector.value;
            if (!classId) {
                divSelector.innerHTML = '<option value="">-- Select Class First --</option>';
                divSelector.disabled = true;
                return;
            }

            try {
                divSelector.innerHTML = '<option value="">Loading divisions...</option>';
                const divsRes = await fetch(`/api/master/classes/${classId}/divisions`);
                if (!divsRes.ok) throw new Error("Failed to fetch divisions");
                const divisions = await divsRes.json();

                if (divisions.length === 0) {
                    divSelector.innerHTML = '<option value="">No divisions found</option>';
                    divSelector.disabled = true;
                } else {
                    divSelector.innerHTML = '<option value="">-- Select Division --</option>' +
                        divisions.map(d => `<option value="${d.id}">${d.divisionName}</option>`).join('');
                    divSelector.disabled = false;
                }
            } catch (err) {
                console.error("Error loading divisions:", err);
                divSelector.innerHTML = '<option value="">Error loading divisions</option>';
            }
        };

    } catch (e) {
        console.error("Monthly report filters error:", e);
    }
}

async function downloadMonthlyExcel() {
    const classId = document.getElementById('monthlyReportClass').value;
    const divId = document.getElementById('monthlyReportDivision').value;
    const month = document.getElementById('monthlyReportMonth').value;
    const year = document.getElementById('monthlyReportYear').value;
    const statusEl = document.getElementById('monthlyReportStatus');

    if (!classId || !divId || !month || !year) {
        alert("Please select Class, Division, Month, and Year.");
        return;
    }

    statusEl.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Generating Excel...';

    try {
        const url = `/api/attendance/monthly-report?classId=${classId}&divisionId=${divId}&month=${month}&year=${year}`;
        const authToken = localStorage.getItem('teacherAuthToken') || '';
        const res = await fetch(url, {
            headers: { 'Authorization': 'Bearer ' + authToken }
        });

        if (res.status === 401 || res.status === 403) {
            statusEl.innerHTML = '<span style="color:#ef4444;">Session expired. Please login again.</span>';
            return;
        }

        if (!res.ok) throw new Error("Failed to fetch monthly report data: " + res.status);
        const data = await res.json();

        if (!data.students || data.students.length === 0) {
            statusEl.innerHTML = '<span style="color:#ef4444;">No data found for the selected criteria.</span>';
            return;
        }

        // Prepare data for SheetJS
        const workbook = XLSX.utils.book_new();
        const sheetData = [];

        // Header Row
        const header = ["Roll No", "Name"];
        data.subjects.forEach(sub => header.push(sub));
        header.push("Overall %");
        sheetData.push(header);

        // Student Rows
        data.students.forEach(s => {
            const row = [s.rollNo, s.name];
            data.subjects.forEach(sub => {
                const subData = s.subjects[sub];
                row.push(subData ? `${subData.pct}%` : "0%");
            });
            row.push(`${s.overall.pct}%`);
            sheetData.push(row);
        });

        const worksheet = XLSX.utils.aoa_to_sheet(sheetData);

        // Basic styling/formatting (Column widths)
        const colWidths = [{ wch: 10 }, { wch: 25 }];
        data.subjects.forEach(() => colWidths.push({ wch: 15 }));
        colWidths.push({ wch: 12 });
        worksheet['!cols'] = colWidths;

        XLSX.utils.book_append_sheet(workbook, worksheet, "Monthly Attendance");

        // Download file
        const fileName = `Monthly_Attendance_${month}_${year}.xlsx`;
        XLSX.writeFile(workbook, fileName);

        statusEl.innerHTML = `<span style="color:#22c55e;">✅ Excel downloaded successfully!</span>`;
    } catch (err) {
        console.error(err);
        statusEl.innerHTML = '<span style="color:#ef4444;">❌ Error generating Excel.</span>';
    }
}

// =====================================================
// MANUAL ATTENDANCE BULK ENTRY
// =====================================================
async function loadStudentsForSession() {
    const select = document.getElementById('manualSessionSelect');
    if (!select || !select.value) {
        alert('Please select a lecture session first.');
        return;
    }
    const opt = select.options[select.selectedIndex];
    const classId = opt.dataset.classId;
    const divisionId = opt.dataset.divisionId;
    const timetableSlotId = select.value;

    const tbody = document.getElementById('manualAttendanceTableBody');
    const statusText = document.getElementById('manualAttendanceStatus');
    
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Loading students...</td></tr>';
    document.getElementById('manualAttendanceGridContainer').style.display = 'block';
    statusText.textContent = '';

    try {
        const teacherId = getTeacherId();
        // Fetch all students for this class/div
        const studentsRes = await fetch(`/api/attendance/teacher/students?teacherId=${teacherId}&classId=${classId}&divisionId=${divisionId}`);
        
        let students = [];
        if (studentsRes.ok) {
           students = await studentsRes.json();
        } else {
           // Fallback to student-list and filter manually if the specific API is missing
           const listRes = await fetch(`/api/attendance/teacher/student-list`);
           if(listRes.ok) {
                const allStudents = await listRes.json();
                students = allStudents.filter(s => s.className === opt.dataset.className);
           } else {
               throw new Error('Fetch students failed');
           }
        }

        tbody.innerHTML = '';
        if (students.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#aaa;">No students found in this class.</td></tr>';
            return;
        }

        // Check who is already marked present for this session
        let presentRolls = [];
        try {
            const presentRes = await fetch(`/api/attendance/session/${timetableSlotId}/present-rolls`);
            if (presentRes.ok) {
                presentRolls = await presentRes.json();
            }
        } catch(e) { console.warn("Could not fetch existing session present rolls", e); }

        students.forEach(s => {
            const isPresent = presentRolls.includes(s.rollNo);
            const badgeText = isPresent ? 'Present' : 'Absent';
            const badgeBg = isPresent ? '#dcfce7' : '#fee2e2';
            const badgeColor = isPresent ? '#22c55e' : '#ef4444';
            const isChecked = isPresent ? 'checked' : '';

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="padding:12px; border-bottom:1px solid #eee;">${s.rollNo}</td>
                <td style="padding:12px; border-bottom:1px solid #eee;">${s.name}</td>
                <td style="padding:12px; border-bottom:1px solid #eee; text-align:center;">
                    <span class="status-badge" id="badge_${s.rollNo}" style="padding:4px 8px; border-radius:12px; font-size:0.8rem; background:${badgeBg}; color:${badgeColor};">${badgeText}</span>
                </td>
                <td style="padding:12px; border-bottom:1px solid #eee; text-align:center;">
                    <label class="switch" style="position:relative; display:inline-block; width:44px; height:24px;">
                        <input type="checkbox" onchange="toggleManualRow(this, '${s.rollNo}')" data-roll="${s.rollNo}" style="opacity:0; width:0; height:0;" ${isChecked}>
                        <span class="slider round" style="position:absolute; cursor:pointer; top:0; left:0; right:0; bottom:0; background-color:#ccc; transition:.4s; border-radius:24px;"></span>
                        <style>
                            input:checked + .slider { background-color: #22c55e; }
                            input:checked + .slider:before { transform: translateX(20px); }
                            .slider:Before { position:absolute; content:""; height:16px; width:16px; left:4px; bottom:4px; background-color:white; transition:.4s; border-radius:50%; }
                        </style>
                    </label>
                </td>
            `;
            tbody.appendChild(tr);
        });

    } catch (err) {
        console.error('Error loading session students:', err);
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:red;">Failed to load students.</td></tr>';
    }
}

function toggleManualRow(checkbox, rollNo) {
    const badge = document.getElementById(`badge_${rollNo}`);
    if (checkbox.checked) {
        badge.textContent = 'Present';
        badge.style.background = '#dcfce7';
        badge.style.color = '#22c55e';
    } else {
        badge.textContent = 'Absent';
        badge.style.background = '#fee2e2';
        badge.style.color = '#ef4444';
    }
}

async function saveBulkAttendance() {
    const select = document.getElementById('manualSessionSelect');
    if (!select || !select.value) return;

    const opt = select.options[select.selectedIndex];
    const teacherId = getTeacherId();
    
    // Payload definition based on AttendanceController.BulkAttendanceRequest
    const payload = {
        timetableSlotId: parseInt(select.value),
        classId: parseInt(opt.dataset.classId),
        divisionId: opt.dataset.divisionId ? parseInt(opt.dataset.divisionId) : null,
        subjectId: opt.dataset.subjectId ? parseInt(opt.dataset.subjectId) : null,
        teacherId: parseInt(teacherId),
        attendance: []
    };

    const checkboxes = document.querySelectorAll('#manualAttendanceTableBody input[type="checkbox"]');
    checkboxes.forEach(cb => {
        payload.attendance.push({
            rollNo: cb.dataset.roll,
            status: cb.checked ? 'Present' : 'Absent'
        });
    });

    if (payload.attendance.length === 0) {
        alert('No students to save.');
        return;
    }

    const statusText = document.getElementById('manualAttendanceStatus');
    statusText.textContent = 'Saving...';

    try {
        const res = await fetch('/api/attendance/manual/bulk', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (!res.ok) throw new Error('Save failed');
        const data = await res.json();
        
        statusText.style.color = '#22c55e';
        statusText.textContent = data.message || 'Saved successfully ✅';
        setTimeout(() => { statusText.textContent = ''; }, 3000);
        
    } catch (err) {
        console.error('Failed to save manual bulk:', err);
        statusText.style.color = '#ef4444';
        statusText.textContent = '❌ Error saving attendance.';
    }
}

// =====================================================
// QR SHARING FUNCTIONS
// =====================================================
async function shareQrCode() {
    const qrImg = document.getElementById('qrImage');
    if (!qrImg || !qrImg.src || qrImg.src.includes('SampleQRData')) {
        alert("Please generate a QR code first!");
        return;
    }

    const shareData = {
        title: 'Attendance QR Code',
        text: 'Please scan this QR code to mark your attendance.',
        url: qrImg.src
    };

    if (navigator.share) {
        try {
            await navigator.share(shareData);
            console.log('QR Code shared successfully');
        } catch (err) {
            console.error('Error sharing QR code:', err);
        }
    } else {
        alert("Sharing is not supported on this browser. Use the Copy Link button instead.");
    }
}

function copyQrLink() {
    const qrImg = document.getElementById('qrImage');
    if (!qrImg || !qrImg.src || qrImg.src.includes('SampleQRData')) {
        alert("Please generate a QR code first!");
        return;
    }

    navigator.clipboard.writeText(qrImg.src).then(() => {
        alert("QR Code URL copied to clipboard! You can paste and share it.");
    }).catch(err => {
        console.error('Failed to copy: ', err);
        alert("Failed to copy QR code link.");
    });
}

// =====================================================
// TEACHER ANALYTICS & VISUAL REPORTS
// =====================================================

async function initTeacherAnalyticsFilters() {
    const classSel = document.getElementById('teacherAnalyticsClass');
    if (!classSel) return;
    
    try {
        const res = await fetch('/api/master/classes');
        const classes = await res.json();
        classes.forEach(c => classSel.innerHTML += `<option value="${c.id}">${c.className}</option>`);
    } catch (e) { console.error('Failed to load class filters', e); }

    try {
        const res = await fetch('/api/master/subjects');
        const subjects = await res.json();
        const subjSel = document.getElementById('teacherAnalyticsSub');
        if (subjSel) {
            subjects.forEach(s => subjSel.innerHTML += `<option value="${s.id}">${s.subjectName}</option>`);
        }
    } catch (e) { console.error('Failed to load subject filters', e); }
}

async function onTeacherAnalyticsClassChange() {
    const classId = document.getElementById('teacherAnalyticsClass').value;
    const divSel = document.getElementById('teacherAnalyticsDiv');
    divSel.innerHTML = '<option value="">All Divisions</option>';
    if (classId) {
        try {
            const res = await fetch(`/api/master/classes/${classId}/divisions`);
            const divs = await res.json();
            divs.forEach(d => divSel.innerHTML += `<option value="${d.id}">${d.divisionName}</option>`);
        } catch (e) { console.error(e); }
    }
    refreshTeacherCharts();
}

function refreshTeacherCharts() {
    loadTeacherSubjectChart();
    loadTeacherTrendChart();
}

function getTeacherAnalyticsQueryParams() {
    const classId = document.getElementById('teacherAnalyticsClass')?.value || '';
    const divId = document.getElementById('teacherAnalyticsDiv')?.value || '';
    const subId = document.getElementById('teacherAnalyticsSub')?.value || '';
    let params = [];
    if (classId) params.push(`classId=${classId}`);
    if (divId) params.push(`divisionId=${divId}`);
    if (subId) params.push(`subjectId=${subId}`);
    return params.length > 0 ? '?' + params.join('&') : '';
}

async function loadTeacherSubjectChart() {
    try {
        const res = await fetch(`/api/attendance/analytics/subject${getTeacherAnalyticsQueryParams()}`);
        if (!res.ok) return;
        let data = await res.json();

        if (checkTeacherEmptyChart(data, 'teacherSubjectChart')) {
            if (teacherSubjectChartInstance) teacherSubjectChartInstance.destroy();
            return;
        }

        // INTEGRITY: Sort by percentage Descending
        data.sort((a, b) => {
            const pctA = a.total > 0 ? (a.present / a.total) : 0;
            const pctB = b.total > 0 ? (b.present / b.total) : 0;
            return pctB - pctA;
        });

        const ctx = document.getElementById('teacherSubjectChart');
        if (!ctx) return;
        if (teacherSubjectChartInstance) teacherSubjectChartInstance.destroy();

        const labels = data.map(d => d.subject || 'N/A');
        const presents = data.map(d => d.total > 0 ? Math.round((d.present / d.total) * 100) : 0);
        const colors = presents.map(p => p >= 75 ? '#10b981' : p >= 50 ? '#f59e0b' : '#ef4444');

        teacherSubjectChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{ 
                    label: 'Presence %', 
                    data: presents, 
                    backgroundColor: colors, 
                    borderRadius: 6,
                    allData: data
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: { 
                    legend: { display: false },
                    tooltip: getTeacherChartTooltipConfig()
                },
                scales: {
                    x: { beginAtZero: true, max: 100, ticks: { callback: v => v + '%' } },
                    y: { grid: { display: false } }
                }
            }
        });
    } catch (err) { console.error('Teacher subject chart error', err); }
}

async function loadTeacherTrendChart() {
    try {
        const res = await fetch(`/api/attendance/analytics/date${getTeacherAnalyticsQueryParams()}`);
        if (!res.ok) return;
        const raw = await res.json();
        const ctx = document.getElementById('teacherTrendChart');
        if (!ctx) return;
        if (teacherTrendChartInstance) teacherTrendChartInstance.destroy();

        const data = raw.slice(-14);
        const labels = data.map(d => {
            const dt = new Date(d.date);
            return dt.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
        });
        const percents = data.map(d => d.total > 0 ? Math.round((d.present / d.total) * 100) : 0);

        teacherTrendChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: 'Day-wise %',
                    data: percents,
                    fill: true,
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16,185,129,0.12)',
                    pointBackgroundColor: '#10b981',
                    tension: 0.4,
                    borderWidth: 2.5,
                    allData: data
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { 
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const d = context.dataset.allData[context.dataIndex];
                                const pct = d.total > 0 ? Math.round((d.present / d.total) * 100) : 0;
                                return `Presence: ${pct}% (${d.present}/${d.total} students)`;
                            }
                        }
                    }
                },
                scales: {
                    y: { beginAtZero: true, max: 100, ticks: { callback: v => v + '%' }, grid: { color: '#f1f5f9' } },
                    x: { grid: { display: false } }
                }
            }
        });
    } catch (err) { console.error('Teacher trend chart error', err); }
}

// Fixed missing reporting table functions
async function loadTeacherSubjectReport() {
    const status = document.getElementById('subjectReportStatus');
    const tbody = document.getElementById('subjectReportBody');
    const table = document.getElementById('subjectReportTable');
    
    if (status) status.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
    try {
        const res = await fetch('/api/attendance/analytics/subject');
        if (!res.ok) throw new Error("Load failed");
        const data = await res.json();
        
        if (tbody) {
            tbody.innerHTML = '';
            data.forEach(d => {
                const pct = d.total > 0 ? Math.round((d.present / d.total) * 100) : 0;
                const statusClass = pct >= 75 ? 'success' : pct >= 50 ? 'warning' : 'danger';
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${d.subject}</td>
                    <td>${d.present}</td>
                    <td>${d.total}</td>
                    <td>${pct}%</td>
                    <td><span class="status-badge ${statusClass}">${pct >= 75 ? 'Good' : pct >= 50 ? 'Average' : 'Low'}</span></td>
                `;
                tbody.appendChild(tr);
            });
        }
        
        if (table) table.style.display = 'table';
        if (status) status.innerHTML = '✅ Updated';
        
        // Update the report chart too
        loadTeacherSubjectReportChart(data);
    } catch (e) {
        if (status) status.innerHTML = '❌ Error loading report';
        console.error(e);
    }
}

async function loadTeacherClassReport() {
    const status = document.getElementById('classReportStatus');
    const tbody = document.getElementById('classReportBody');
    const table = document.getElementById('classReportTable');
    
    if (status) status.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
    try {
        const res = await fetch('/api/attendance/analytics/department');
        if (!res.ok) throw new Error("Load failed");
        const data = await res.json();
        
        if (tbody) {
            tbody.innerHTML = '';
            data.forEach(d => {
               const pct = d.total > 0 ? Math.round((d.present / d.total) * 100) : 0;
               const tr = document.createElement('tr');
               tr.innerHTML = `
                    <td>${d.subject}</td> <!-- Repository uses 'subject' field for class name in department analytics -->
                    <td>${d.present}</td>
                    <td>${d.total}</td>
                    <td>${pct}%</td>
                    <td><span class="status-badge ${pct >= 75 ? 'success' : 'warning'}">${pct}%</span></td>
               `;
               tbody.appendChild(tr);
            });
        }
        
        if (table) table.style.display = 'table';
        if (status) status.innerHTML = '✅ Updated';
    } catch (e) {
        if (status) status.innerHTML = '❌ Error';
        console.error(e);
    }
}

// Helper to keep old chart container working if needed
function loadTeacherSubjectReportChart(data) {
    const ctx = document.getElementById('subjectReportChart');
    if (!ctx) return;
    
    // FIX: Destroy existing instance to prevent "Error loading report" crash
    if (teacherDoughnutInstance) teacherDoughnutInstance.destroy();

    teacherDoughnutInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: data.map(d => d.subject),
            datasets: [{
                data: data.map(d => d.present),
                backgroundColor: data.map((_, i) => `hsl(${i * 137.508}, 70%, 50%)`)
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { 
                title: { display: true, text: 'Presence Distribution (Student Counts)' },
                legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } }
            }
        }
    });
}

// Helper to trigger initial loads when Visual Analytics sub-tab becomes active
const originalSwitchReportTab = window.switchReportTab;
window.switchReportTab = function(tabEl) {
    if (typeof originalSwitchReportTab === 'function') {
        originalSwitchReportTab(tabEl);
    }
    const targetId = tabEl.getAttribute('data-subtab');
    if (targetId === 'visual-analytics-subtab') {
        refreshTeacherCharts();
    }
};

async function loadTeacherNotices() {
    const container = document.getElementById('teacherNoticesContainer');
    if (!container) return;

    try {
        const response = await fetch('/api/notices/all');
        if (!response.ok) throw new Error('Failed to fetch notices');

        const notices = await response.json();

        if (!notices || notices.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-bullhorn"></i>
                    <h3>No Notices Yet</h3>
                    <p>Important announcements from the admin will appear here.</p>
                </div>`;
            return;
        }

        container.innerHTML = notices.map(notice => `
            <div class="notice-card" style="border-left: 4px solid var(--secondary-color);">
                <div class="notice-header">
                    <h4>${notice.title}</h4>
                    <span class="notice-date">${new Date(notice.createdAt).toLocaleDateString()}</span>
                </div>
                <p class="notice-content">${notice.content}</p>
                ${notice.fileUrl ? `
                    <div class="notice-footer">
                        <a href="${notice.fileUrl}" target="_blank" class="btn btn-secondary btn-small">
                            <i class="fas fa-paperclip"></i> View Attachment
                        </a>
                    </div>` : ''}
            </div>
        `).join('');

    } catch (error) {
        console.error('Error loading notices:', error);
        container.innerHTML = '<p class="error-message">Unable to load notices. Please check your connection.</p>';
    }
}
async function loadLeaveRequests() {
    const tbody = document.getElementById('leaveRequestsTableBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;"><i class="fas fa-spinner fa-spin"></i> Loading...</td></tr>';

    try {
        const teacherId = getTeacherId();
        const response = await fetch(`/api/leave/all`); // Alternatively use /api/leave/teacher/${teacherId}
        if (!response.ok) throw new Error('Failed to load leave requests');

        const leaves = await response.json();
        if (!leaves || leaves.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">No leave requests found.</td></tr>';
            return;
        }

        tbody.innerHTML = leaves.map(leave => `
            <tr>
                <td>${leave.studentName || 'N/A'}</td>
                <td>${leave.className || 'N/A'}</td>
                <td>${leave.fromDate || '-'}</td>
                <td>${leave.toDate || '-'}</td>
                <td><small>${leave.reason || '-'}</small></td>
                <td>
                    <span class="attendance-status ${leave.status?.toLowerCase() === 'approved' ? 'present' : leave.status?.toLowerCase() === 'rejected' ? 'absent' : 'late'}">
                        ${leave.status || 'Pending'}
                    </span>
                </td>
                <td>
                    <div style="display:flex; gap:5px;">
                        ${leave.status === 'Pending' ? `
                            <button class="btn btn-success btn-small" onclick="approveLeave(${leave.id})">
                                <i class="fas fa-check"></i>
                            </button>
                            <button class="btn btn-danger btn-small" onclick="rejectLeave(${leave.id})">
                                <i class="fas fa-times"></i>
                            </button>
                        ` : '<span style="font-size:0.8rem;color:#888;">Processed</span>'}
                    </div>
                </td>
            </tr>
        `).join('');

    } catch (error) {
        console.error('Leave requests error:', error);
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:red;">Error loading requests.</td></tr>';
    }
}

async function approveLeave(id) {
    if (!confirm('Are you sure you want to approve this leave?')) return;
    try {
        const res = await fetch(`/api/leave/approve/${id}`, { method: 'PUT' });
        if (res.ok) {
            alert("Leave approved successfully!");
            loadLeaveRequests();
        } else {
            alert("Failed to approve leave.");
        }
    } catch (e) {
        console.error(e);
        alert("Error approving leave.");
    }
}

async function rejectLeave(id) {
    if (!confirm('Are you sure you want to reject this leave?')) return;
    try {
        const res = await fetch(`/api/leave/reject/${id}`, { method: 'PUT' });
        if (res.ok) {
            alert("Leave rejected successfully!");
            loadLeaveRequests();
        } else {
            alert("Failed to reject leave.");
        }
    } catch (e) {
        console.error(e);
        alert("Error rejecting leave.");
    }
}
