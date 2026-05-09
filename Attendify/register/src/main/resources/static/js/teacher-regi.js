document.getElementById("registerForm").addEventListener("submit", function (e) {
    e.preventDefault(); // prevent form from submitting normally

    // Get form values
    const user = {
        name: document.getElementById("name").value.trim(),
        mobilenumber: document.getElementById("mobilenumber").value.trim(),
        email: document.getElementById("email").value.trim(),
        schoolCode: document.getElementById("schoolCode").value.trim(),
        password: document.getElementById("password").value.trim()
    };

    // Send data to backend API
    fetch("/api/teachers/register", {
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
            msgEl.className = 'message show success'; // assuming CSS exists
            msgEl.style.color = "green";
            setTimeout(() => {
                window.location.href = "login.html";
            }, 1500);
            document.getElementById("registerForm").reset();
        })
        .catch(err => {
            const msgEl = document.getElementById("message");
            msgEl.innerText = err.message || "Error occurred!";
            msgEl.className = 'message show error';
            msgEl.style.color = "red";
        });
});
