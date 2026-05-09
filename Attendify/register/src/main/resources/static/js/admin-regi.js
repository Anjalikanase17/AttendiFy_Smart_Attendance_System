
(function () {
    const form = document.getElementById("adminRegisterForm");
    const messageBox = document.getElementById("message");

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        const schoolName = document.getElementById("schoolName").value.trim();
        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value.trim();

        const adminData = {
            schoolName,
            email,
            password
        };

        const btn = e.target.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.innerText = "Registering...";

        fetch("/api/admin/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(adminData)
        })
            .then(async res => {
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Registration failed");
                }
                return data;
            })
            .then(data => {
                // Success
                messageBox.innerHTML = `
                    <div style="color: #166534; background: #dcfce7; padding: 15px; border-radius: 8px; border: 1px solid #bbf7d0;">
                        ✅ Registration Successful!<br>
                        <strong>Your School Code is: ${data.schoolCode}</strong><br>
                        Please keep this code safe. Your teachers and students will need it.
                    </div>
                `;
                form.reset();
                // Optional: redirect to login after a delay
                // setTimeout(() => window.location.href = "login.html", 5000);
            })
            .catch(err => {
                messageBox.innerHTML = `
                    <div style="color: #991b1b; background: #fee2e2; padding: 15px; border-radius: 8px; border: 1px solid #fecaca;">
                        ❌ Error: ${err.message}
                    </div>
                `;
            })
            .finally(() => {
                btn.disabled = false;
                btn.innerText = "Register Organization";
            });
    });
})();
