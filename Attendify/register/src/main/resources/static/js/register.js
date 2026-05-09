
document.addEventListener("DOMContentLoaded", function () {
    // Class and division fields moved to dashboard
});

document.getElementById("registerForm").addEventListener("submit", function (e) {
    e.preventDefault();

    const user = {
        name: document.getElementById("name").value,
        rollNo: document.getElementById("rollNo").value,
        email: document.getElementById("email").value,
        address: document.getElementById("address").value,
        mobilenumber: document.getElementById("mobilenumber").value,
        schoolCode: document.getElementById("schoolCode").value,
        password: document.getElementById("password").value
    };

    fetch("/api/users/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(user)
    })
        .then(res => {
            if (!res.ok) return res.json().then(err => { throw new Error(err.message || "Registration failed") });
            return res.json();
        })
        .then(data => {
            document.getElementById("message").innerText = "Registration Successful!";
            const msgEl = document.getElementById("message");
            msgEl.classList.add('show', 'success');
            setTimeout(() => {
                window.location.href = "login.html";
            }, 1500);
            document.getElementById("registerForm").reset();
        })
        .catch(err => {
            document.getElementById("message").innerText = err.message || "Error occurred!";
            document.getElementById("message").classList.add('show');
        });
});
