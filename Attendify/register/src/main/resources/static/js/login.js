
(function () {
    const messageBox = document.getElementById("message");

    document.getElementById("loginForm").addEventListener("submit", function (e) {
        e.preventDefault();

        const role = document.getElementById("role").value;
        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value.trim();

        if (!role) {
            showMessage("Please select a role");
            return;
        }

        const user = { email, password };

        let url = "";
        if (role === "student") {
            url = "/api/users/login";
        } else if (role === "teacher") {
            url = "/api/teachers/login";
        } else if (role === "admin") {
            url = "/api/admin/login";
        }

        // tiny loading state (optional)
        const btn = e.target.querySelector('button[type="submit"]');
        if (btn.disabled) return; // prevent double click submissions freezing the UI
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span>⏳</span> signing in...';
        btn.disabled = true;

        fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        })
            .then(async res => {
                const data = await res.json().catch(() => ({}));
                if (!res.ok) {
                    showMessage(data.message || "Invalid email or password");
                    return null;
                }
                return data;
            })
            .then(data => {
                if (!data) return;

                console.log("Login Success:", data);
                const userData = data.user || data;
                if (data.token) {
                    localStorage.setItem(role + "AuthToken", data.token);
                }
                localStorage.setItem(role + "LoggedUser", JSON.stringify(userData));
                localStorage.setItem(role + "Role", role);

                // show success message
                showMessage(`✅ welcome! redirecting to ${role} dashboard...`, 'success');

                // real redirect
                if (role === "teacher") {
                    window.location.href = "teacher-dashboard.html";
                } else if (role === "student") {
                    window.location.href = "dashboard.html";
                } else if (role === "admin") {
                    window.location.href = "admin-dashboard.html";
                }
            })
            .catch(err => {
                console.error(err);
                showMessage("⚠️ server error. try again later.");
            })
            .finally(() => {
                btn.innerHTML = '<span><i class="fa-solid fa-arrow-right"></i></span> Log in to dashboard';
                btn.disabled = false;
            });
    });

    document.getElementById("registerBtn").addEventListener("click", function () {
        const role = document.getElementById("role").value;

        if (!role) {
            alert("Please select role first (student/teacher)");
            return;
        }

        if (role === "admin") {
            window.location.href = "admin-register.html";
            return;
        }

        window.location.href = role === "student"
            ? "register.html"
            : "teacher-register.html";
    });


    function showMessage(msg, type = 'error') {
        messageBox.innerText = msg;
        messageBox.className = 'error-msg show';
        messageBox.style.display = ''; // Clear any inline hide overrides
        if (type === 'success') {
            messageBox.style.background = 'rgba(74, 222, 128, 0.2)';
            messageBox.style.color = '#166534';
            messageBox.style.borderColor = '#86efac';
        } else {
            messageBox.style.background = 'rgba(252, 165, 165, 0.25)';
            messageBox.style.color = '#aa1f2e';
            messageBox.style.borderColor = 'rgba(248, 113, 113, 0.3)';
        }
    }

    // extra: hide message when typing
    document.querySelectorAll('#loginForm input, #loginForm select').forEach(field => {
        field.addEventListener('focus', () => {
            messageBox.classList.remove('show');
        });
    });

    // Forgot Password Logic
    const forgotModal = document.getElementById("forgotPasswordModal");
    const forgotLink = document.querySelector(".forgot-link");
    const closeForgotBtn = document.getElementById("closeForgotModal");
    const forgotForm = document.getElementById("forgotPasswordForm");

    if (forgotLink) {
        forgotLink.addEventListener("click", function (e) {
            e.preventDefault();
            forgotModal.classList.add("show");
        });
    }

    if (closeForgotBtn) {
        closeForgotBtn.addEventListener("click", function () {
            forgotModal.classList.remove("show");
        });
    }

    // Close on outside click
    window.addEventListener("click", function (e) {
        if (e.target === forgotModal) {
            forgotModal.classList.remove("show");
        }
    });

    if (forgotForm) {
        forgotForm.addEventListener("submit", function (e) {
            e.preventDefault();
            const role = document.getElementById("forgotRole").value;
            const email = document.getElementById("forgotEmail").value.trim();
            const mobile = document.getElementById("forgotMobile").value.trim();
            const password = document.getElementById("forgotNewPassword").value.trim();

            const btn = e.target.querySelector('button[type="submit"]');
            const originalText = btn.innerText;
            btn.innerText = "Processing...";
            btn.disabled = true;

            let url = role === "student" ? "/api/users/forgot-password" : "/api/teachers/forgot-password";

            fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, mobile, password })
            })
                .then(async res => {
                    const data = await res.json().catch(() => ({}));
                    if (!res.ok) {
                        throw new Error(data.message || "Something went wrong");
                    }
                    return data;
                })
                .then(data => {
                    alert("✅ Password reset successful! You can now login.");
                    forgotModal.classList.remove("show");
                    forgotForm.reset();
                })
                .catch(err => {
                    alert("❌ Error: " + err.message);
                })
                .finally(() => {
                    btn.innerText = originalText;
                    btn.disabled = false;
                });
        });
    }

    // initial style (hidden)
    messageBox.style.display = 'none';
})();
