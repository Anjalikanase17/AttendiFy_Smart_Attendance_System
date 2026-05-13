const params = new URLSearchParams(window.location.search);
const className = params.get("class");
const sessionId = params.get("sessionId");
const msgEl = document.getElementById("msg");

let deviceId = null;

// ✅ Validate QR params AFTER page loads
window.onload = () => {
  if (!className || !sessionId) {
    msgEl.textContent = "Invalid QR link (Missing parameters) ❌";
    msgEl.style.color = "red";
    document.getElementById("attendanceForm").style.display = "none";
  }
};

// 🔹 Generate device ID
function loadDeviceId() {
  if (typeof FingerprintJS === 'undefined') {
    setTimeout(loadDeviceId, 500);
    return;
  }

  FingerprintJS.load()
    .then(fp => fp.get())
    .then(result => {
      deviceId = result.visitorId;
      console.log("Device ID:", deviceId);
    })
    .catch(() => {
      // ✅ fallback immediately (no delay)
      deviceId = 'fallback-' + Date.now();
      console.log("Fallback Device ID:", deviceId);
    });
}

// Load device ID
loadDeviceId();

document.getElementById("attendanceForm").addEventListener("submit", e => {
  e.preventDefault();

  const rollNo = document.getElementById("rollNo").value.trim();

  if (!rollNo) {
    msgEl.textContent = "Enter Roll Number ❌";
    msgEl.style.color = "red";
    return;
  }

  // ✅ Don't block user if deviceId not ready
  if (!deviceId) {
    deviceId = 'fallback-' + Date.now();
  }

  if (!navigator.geolocation) {
    msgEl.textContent = "Location not supported ❌";
    msgEl.style.color = "red";
    return;
  }

  msgEl.textContent = "Getting location... 📍";
  msgEl.style.color = "blue";

  navigator.geolocation.getCurrentPosition(
    position => {
      const latitude = position.coords.latitude;
      const longitude = position.coords.longitude;

      msgEl.textContent = "Marking attendance... ⏳";

      const formData = new URLSearchParams();
      formData.append("rollNo", rollNo);
      formData.append("deviceId", deviceId);
      formData.append("latitude", latitude);
      formData.append("longitude", longitude);
      formData.append("sessionId", sessionId);

      // ✅ Add timeout to fetch (IMPORTANT)
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 10000);

      fetch("https://kia-unogled-lionheartedly.ngrok-free.dev/api/attendance/mark", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formData.toString(),
        signal: controller.signal
      })
        .then(res => {
          clearTimeout(timeout);

          if (!res.ok) {
            return res.text().then(text => {
              throw new Error(text || "Server Error");
            });
          }
          return res.json();
        })
        .then(data => {
          msgEl.textContent = data.message || "Response received";
          msgEl.style.color = data.message?.includes("success") ? "green" : "red";
        })
        .catch(error => {
          console.error(error);

          if (error.name === "AbortError") {
            msgEl.textContent = "Server taking too long. Try again ❌";
          } else {
            msgEl.textContent = "Error: " + error.message;
          }

          msgEl.style.color = "red";
        });
    },
    error => {
      console.error(error);

      let errorMsg = "Location error ❌";
      if (error.code === 1) errorMsg = "Location permission denied ❌";
      if (error.code === 2) errorMsg = "Location unavailable ❌";
      if (error.code === 3) errorMsg = "Location timeout ❌";

      msgEl.textContent = errorMsg;
      msgEl.style.color = "red";
    },
    {
      enableHighAccuracy: false,
      timeout: 15000,
      maximumAge: 0
    }
  );
});