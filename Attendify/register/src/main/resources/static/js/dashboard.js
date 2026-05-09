const API_BASE = "";
const originalFetch = window.fetch.bind(window);

window.fetch = (input, init = {}) => {
    const url = typeof input === "string" ? input : (input && input.url) ? input.url : "";
    const isApiCall = url.startsWith(`${API_BASE}/api/`) || url.startsWith("/api/");

    if (!isApiCall) {
        return originalFetch(input, init);
    }

    const token = (localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken'));
    const headers = new Headers(init.headers || {});
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    return originalFetch(input, { ...init, headers });
};

// ----------------- GET LOGGED-IN USER -----------------
const user = JSON.parse((localStorage.getItem('studentLoggedUser') || localStorage.getItem('loggedUser')));
const role = (localStorage.getItem('studentRole') || localStorage.getItem('role'));
const token = (localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken'));

if (!user || role !== "student" || !token) {
    window.location.href = "login.html";
}

// ----------------- INITIALIZATION -----------------
document.addEventListener("DOMContentLoaded", function () {
    // Basic UI Setup
    document.getElementById("welcomeText").innerText = "Welcome, " + user.name + " 👋";
    document.getElementById("profileName").innerText = user.name;
    document.getElementById("profileEmail").innerText = user.email;
    document.getElementById("profileRoll").innerText = user.rollNo;
    
    const displayClassName = user.classMaster ? user.classMaster.className : (user.className || "-");
    const displayDivName = user.divisionMaster ? user.divisionMaster.divisionName : (user.divisionName || "");
    document.getElementById("profileClass").innerText = displayClassName + (displayDivName ? " " + displayDivName : "");

    // Load Data
    fetchDashboardData();
    fetchAttendanceRecords();
    fetchSubjectSummary();
    checkAttendanceStatus();
    loadStudentLeaves();
    loadClassTeachers();
    loadStudentNotes();
    setupStudentMobileSidebar();
    
    // Profile Setup Logic
    checkProfileSetup();
});

// ----------------- DASHBOARD DATA -----------------
function fetchDashboardData() {
    fetch(`/api/student/dashboard/${user.id}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch dashboard data");
            return res.json();
        })
        .then(data => {
            document.querySelector(".card.blue p").innerText = data.totalClasses;
            document.querySelector(".card.green p").innerText = data.present;
            document.querySelector(".card.red p").innerText = data.absent;
            document.querySelector(".card.purple p").innerText = data.percentage + "%";
            document.getElementById("attendancePercent").innerText = data.percentage + "%";

            const statCards = document.querySelectorAll(".attendance-stats .stat-card");
            if (statCards.length >= 2) {
                statCards[0].querySelector(".stat-value").innerText = data.present;
                statCards[0].querySelector(".stat-progress-bar.present").style.width = data.percentage + "%";
                statCards[1].querySelector(".stat-value").innerText = data.absent;
                statCards[1].querySelector(".stat-progress-bar.absent").style.width = (100 - data.percentage) + "%";
            }

            loadChart(data.present, data.absent);

            fetch("/api/settings")
                .then(res => res.json())
                .then(setting => {
                    const threshold = setting.attendanceThreshold;
                    const color = data.percentage < threshold ? "red" : "green";
                    document.querySelector(".card.purple p").style.color = color;
                    document.getElementById("attendancePercent").style.color = color;
                })
                .catch(err => console.error("Could not fetch settings:", err));
        })
        .catch(err => {
            console.error(err);
        });
}

function fetchAttendanceRecords() {
    fetch(`/api/student/attendance/${user.id}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch attendance records");
            return res.json();
        })
        .then(records => {
            const tbody = document.querySelector("#monthlyAttendanceTable tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(records) || records.length === 0) {
                tbody.innerHTML = `<tr><td colspan="3" style="text-align:center;color:#64748b;">No attendance records available.</td></tr>`;
                setupFilters();
                return;
            }
            records.forEach(r => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${r.date}</td>
                    <td>${r.subject}</td>
                    <td class="${r.status.toLowerCase()}">${r.status}</td>
                `;
                tbody.appendChild(tr);
            });
            setupFilters();
        })
        .catch(err => console.error(err));
}

function fetchSubjectSummary() {
    fetch(`/api/student/summary/subject/${user.id}`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch subject summary");
            return res.json();
        })
        .then(summaries => {
            const tbody = document.querySelector("#subjectSummaryTable tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(summaries) || summaries.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:#64748b;">No subject-wise summary available.</td></tr>`;
                return;
            }
            summaries.forEach(s => {
                const tr = document.createElement("tr");
                const perc = s.percentage.toFixed(1);
                tr.innerHTML = `
                    <td>${s.subject}</td>
                    <td>${s.presentCount}</td>
                    <td>${s.totalClasses}</td>
                    <td class="${s.percentage < 75 ? 'absent' : 'present'}">${perc}%</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(err => console.error(err));
}

function loadChart(present, absent) {
    const ctx = document.getElementById("attendanceChart");
    if (!ctx) return;
    new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Present", "Absent"],
            datasets: [{
                data: [present, absent],
                backgroundColor: ["#1cc88a", "#e74a3b"],
                borderWidth: 0
            }]
        },
        options: {
            plugins: { legend: { position: "bottom" } },
            cutout: "70%"
        }
    });
}

// ----------------- NOTES -----------------
function loadStudentNotes() {
    const notesList = document.getElementById("notesList");
    if (!notesList) return;

    fetch(`/api/notes/student/${user.id}`)
        .then(res => res.json())
        .then(data => {
            notesList.innerHTML = "";
            if (data.length === 0) {
                notesList.innerHTML = `<div style="text-align:center; padding:20px; color:#666;">No notes available for your class.</div>`;
                return;
            }
            data.forEach(note => {
                notesList.innerHTML += `
                    <div class="note-card">
                        <div>
                            <div class="note-title">${note.fileName}</div>
                            <div class="note-subject">Subject: ${note.subject}</div>
                        </div>
                        <a href="${note.fileUrl}" target="_blank" class="note-btn">📥 Download</a>
                    </div>
                `;
            });
        })
        .catch(err => console.error("Notes error:", err));
}

// ----------------- SECTION SWITCHING -----------------
function showSection(id) {
    document.querySelectorAll(".section").forEach(sec => sec.classList.add("hidden"));
    document.getElementById(id).classList.remove("hidden");
    closeStudentSidebar();

    if (id === 'notices') loadStudentNotices();
}

// ----------------- MOBILE SIDEBAR -----------------
function closeStudentSidebar() {
    const sidebar = document.getElementById("studentSidebar");
    const overlay = document.getElementById("studentSidebarOverlay");
    if (!sidebar || !overlay) return;
    sidebar.classList.remove("sidebar-open");
    overlay.classList.remove("show");
    document.body.classList.remove("menu-open");
}

function setupStudentMobileSidebar() {
    const toggleBtn = document.getElementById("studentMenuToggle");
    const sidebar = document.getElementById("studentSidebar");
    const overlay = document.getElementById("studentSidebarOverlay");
    if (!toggleBtn || !sidebar || !overlay) return;

    toggleBtn.addEventListener("click", () => {
        const isOpen = sidebar.classList.contains("sidebar-open");
        sidebar.classList.toggle("sidebar-open", !isOpen);
        overlay.classList.toggle("show", !isOpen);
        document.body.classList.toggle("menu-open", !isOpen);
    });

    overlay.addEventListener("click", closeStudentSidebar);
    window.addEventListener("resize", () => {
        if (window.innerWidth > 768) {
            closeStudentSidebar();
        }
    });
}

// ----------------- FILTERS -----------------
function setupFilters() {
    const buttons = document.querySelectorAll("#attendance .filter-btn");
    if (!buttons.length) return;

    const rows = () => document.querySelectorAll("#monthlyAttendanceTable tbody tr");
    buttons.forEach(btn => {
        if (btn.dataset.boundClick === "1") return;
        btn.dataset.boundClick = "1";
        btn.addEventListener("click", () => {
            const filter = btn.innerText.toLowerCase().trim();
            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            rows().forEach(tr => {
                if (tr.children.length < 3) {
                    tr.style.display = "";
                    return;
                }
                const status = (tr.children[2].innerText || "").toLowerCase().trim();
                const dateText = (tr.children[0].innerText || "").trim();
                const recordDate = new Date(dateText);
                const now = new Date();

                if (filter === "all") {
                    tr.style.display = "";
                } else if (filter === "this month") {
                    const validDate = !Number.isNaN(recordDate.getTime());
                    tr.style.display = (validDate && recordDate.getMonth() === now.getMonth() && recordDate.getFullYear() === now.getFullYear()) ? "" : "none";
                } else {
                    tr.style.display = (status === filter) ? "" : "none";
                }
            });
        });
    });
}

// ----------------- NOTIFICATIONS -----------------
async function checkAttendanceStatus() {
    try {
        const response = await fetch(`/api/attendance/check/${user.id}`);
        const message = await response.text();
        const alertDiv = document.getElementById("attendanceAlert");
        if (alertDiv) {
            alertDiv.innerText = message;
            alertDiv.style.backgroundColor = message.includes("Warning") ? "#ffcccc" : "#ccffcc";
            alertDiv.style.color = message.includes("Warning") ? "red" : "green";
        }
    } catch (error) {
        console.error("Attendance check error:", error);
    }
}

// ----------------- LEAVE SYSTEM -----------------
function submitLeave() {
    const teacherId = document.getElementById("leaveTeacher").value;
    const leaveData = {
        studentId: user.id,
        studentName: user.name,
        className: (user.classMaster ? user.classMaster.className : "") + (user.divisionMaster ? " " + user.divisionMaster.divisionName : ""),
        fromDate: document.getElementById("fromDate").value,
        toDate: document.getElementById("toDate").value,
        reason: document.getElementById("reason").value,
        teacher: teacherId ? { id: parseInt(teacherId) } : null
    };

    if (!leaveData.fromDate || !leaveData.toDate || !leaveData.reason || !teacherId) {
        alert("Please fill all fields.");
        return;
    }

    fetch("/api/leave/submit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(leaveData)
    })
    .then(res => res.text())
    .then(msg => {
        alert(msg);
        loadStudentLeaves();
        document.getElementById("leaveRequestForm")?.reset();
    })
    .catch(err => console.error("Submit error:", err));
}

function loadStudentLeaves() {
    fetch(`/api/leave/student/${user.id}`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.querySelector("#leaveTable tbody");
            if (!tbody) return;
            tbody.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;">No Leave Requests found</td></tr>`;
                return;
            }
            data.forEach(leave => {
                const statusClass = leave.status?.toLowerCase() === 'approved' ? 'status-approved' : 
                                    leave.status?.toLowerCase() === 'rejected' ? 'status-rejected' : 'status-pending';
                tbody.innerHTML += `
                    <tr>
                        <td>${leave.fromDate}</td>
                        <td>${leave.toDate}</td>
                        <td>${leave.reason}</td>
                        <td><span class="${statusClass}">${leave.status || 'Pending'}</span></td>
                    </tr>
                `;
            });
        })
        .catch(err => console.error("Load leaves error:", err));
}

async function loadClassTeachers() {
    const classId = user.classMaster ? user.classMaster.id : user.classId;
    const divId = user.divisionMaster ? user.divisionMaster.id : user.divisionId;
    if (!classId || !divId) return;

    try {
        const res = await fetch(`/api/master/classes/${classId}/divisions/${divId}/teachers`);
        const teachers = await res.json();
        const select = document.getElementById("leaveTeacher");
        if (!select) return;
        select.innerHTML = '<option value="">Select Teacher</option>';
        teachers.forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.id;
            opt.textContent = t.name;
            select.appendChild(opt);
        });
    } catch (err) { console.error("Load teachers error:", err); }
}

// ----------------- NOTICES -----------------
function loadStudentNotices() {
    const container = document.getElementById('studentNoticesContainer');
    if (!container) return;
    container.innerHTML = '<p style="text-align:center; padding: 20px;">Loading notices...</p>';

    fetch('/api/notices/all')
        .then(res => res.json())
        .then(notices => {
            container.innerHTML = '';
            if (notices.length === 0) {
                container.innerHTML = `
                    <div style="text-align:center; padding: 40px;">
                        <i class="fas fa-bullhorn" style="font-size: 3rem; color: #cbd5e1; margin-bottom: 20px;"></i>
                        <h3 style="color: #64748b;">No notices posted yet</h3>
                    </div>`;
                return;
            }

            notices.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
            notices.forEach(notice => {
                const date = new Date(notice.createdAt).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' });
                const fileLink = (notice.fileUrl) ? `
                    <div class="notice-footer">
                        <a href="${notice.fileUrl}" target="_blank" class="btn-download">
                            <i class="fas fa-paperclip"></i> Download Attachment
                        </a>
                    </div>` : '';

                container.innerHTML += `
                    <div class="notice-card">
                        <div class="notice-header">
                            <h4>${notice.title}</h4>
                            <span class="notice-date">${date}</span>
                        </div>
                        <div class="notice-body">${notice.content}</div>
                        ${fileLink}
                    </div>`;
            });
        })
        .catch(err => {
            console.error(err);
            container.innerHTML = '<p style="color: red; text-align: center;">Failed to load notices.</p>';
        });
}

// ----------------- PROFILE SETUP -----------------
function getUserClassId() {
    return (user && (
        (user.classMaster && user.classMaster.id) ||
        user.classId ||
        user.classMasterId ||
        user.class_id
    ));
}

function getUserDivisionId() {
    return (user && (
        (user.divisionMaster && user.divisionMaster.id) ||
        user.divisionId ||
        user.divisionMasterId ||
        user.division_id
    ));
}

function checkProfileSetup() {
    const currentClass = (document.getElementById("profileClass")?.textContent || "").trim();
    const hasClass = getUserClassId();
    const hasDiv = getUserDivisionId();
    
    if (!hasClass || !hasDiv || currentClass === "-") {
        const modal = document.getElementById("setupModal");
        if (modal) {
            modal.style.display = "flex";
            modal.classList.remove("hidden");
            loadSetupClasses();

            const classSelect = document.getElementById("setupClassMaster");
            const saveBtn = document.getElementById("saveSetupBtn");
            if (classSelect && !classSelect.dataset.boundChange) {
                classSelect.addEventListener("change", function() {
                    loadSetupDivisions(this.value);
                });
                classSelect.dataset.boundChange = "1";
            }
            if (saveBtn && !saveBtn.dataset.boundClick) {
                saveBtn.addEventListener("click", saveSetup);
                saveBtn.dataset.boundClick = "1";
            }
        }
    }
}

async function loadSetupClasses() {
    try {
        const res = await fetch("/api/master/classes");
        const classes = await res.json();
        const select = document.getElementById("setupClassMaster");
        if (!select) return;
        select.innerHTML = '<option value="">Select Class</option>';
        classes.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.className;
            select.appendChild(opt);
        });
    } catch (err) { console.error(err); }
}

async function loadSetupDivisions(classId) {
    const select = document.getElementById("setupDivisionMaster");
    if (!select) return;
    select.innerHTML = '<option value="">Select Division</option>';
    if (!classId) { select.disabled = true; return; }
    try {
        const res = await fetch(`/api/master/classes/${classId}/divisions`);
        const divs = await res.json();
        divs.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d.id;
            opt.textContent = d.divisionName;
            select.appendChild(opt);
        });
        select.disabled = false;
    } catch (err) { console.error(err); }
}

async function saveSetup() {
    const classId = document.getElementById("setupClassMaster").value;
    const divId = document.getElementById("setupDivisionMaster").value;
    const errorEl = document.getElementById("setupError");

    if (!classId || !divId) {
        if (errorEl) { errorEl.textContent = "Please select both."; errorEl.style.display = "block"; }
        return;
    }

    try {
        const res = await fetch(`/api/users/${user.id}/class-division`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ classMaster: { id: parseInt(classId) }, divisionMaster: { id: parseInt(divId) } })
        });
        if (!res.ok) throw new Error("Failed to update profile");
        const updatedUser = await res.json();
        localStorage.setItem('studentLoggedUser', JSON.stringify(updatedUser));
        window.location.reload();
    } catch (err) {
        if (errorEl) { errorEl.textContent = err.message; errorEl.style.display = "block"; }
    }
}

// ----------------- LOGOUT -----------------
function logout() {
    localStorage.removeItem('studentAuthToken'); localStorage.removeItem('studentLoggedUser'); localStorage.removeItem('studentRole'); 
    localStorage.removeItem('authToken'); localStorage.removeItem('loggedUser'); localStorage.removeItem('role');
    window.location.href = "login.html";
}
