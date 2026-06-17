const fs = require('fs');
const path = require('path');

async function upload() {
  console.log("=== Gofile Upload Script Started ===");
  // 1. Find the APK file
  const apkPath = './app/build/outputs/apk/debug/app-debug.apk';
  if (!fs.existsSync(apkPath)) {
    console.error("Error: App APK not found at " + apkPath);
    process.exit(1);
  }
  const fileStats = fs.statSync(apkPath);
  console.log(`Found APK of size: ${fileStats.size} bytes`);
  console.log(`Last modified: ${fileStats.mtime}`);

  // 2. Fetch servers
  let server = 'store1';
  try {
    const sResp = await fetch('https://api.gofile.io/servers');
    const sData = await sResp.json();
    if (sData && sData.status === 'ok' && sData.data) {
      server = sData.data.server || (sData.data.servers && sData.data.servers[0] && sData.data.servers[0].name) || 'store1';
    }
  } catch (err) {
    console.error("Failed to get servers, using store1 fallback:", err.message);
  }
  console.log("Using Gofile server:", server);

  // 3. Create anonymous account/token
  let token = null;
  try {
    const aResp = await fetch('https://api.gofile.io/accounts', { method: 'POST' });
    const aData = await aResp.json();
    if (aData && aData.status === 'ok' && aData.data && aData.data.token) {
      token = aData.data.token;
      console.log("Authenticated with token:", token);
    }
  } catch (err) {
    console.error("Failed to authenticate account:", err.message);
  }

  // 4. Create FormData and attach file
  const fileBuffer = fs.readFileSync(apkPath);
  const blob = new Blob([fileBuffer], { type: 'application/vnd.android.package-archive' });
  
  const form = new FormData();
  form.append('file', blob, 'DevNet-debug.apk');
  if (token) {
    form.append('token', token);
  }

  // 5. Upload file
  const uploadUrl = `https://${server}.gofile.io/contents/uploadfile`;
  console.log("Uploading to:", uploadUrl);
  
  try {
    const response = await fetch(uploadUrl, {
      method: 'POST',
      body: form
    });
    const result = await response.json();
    if (result && result.status === 'ok' && result.data && result.data.downloadPage) {
      console.log("\n==================================================");
      console.log("🎉 SUCCESSFUL GOFILE UPLOAD 🎉");
      console.log("URL: " + result.data.downloadPage);
      console.log("==================================================\n");
    } else {
      console.error("Upload failed or returned unexpected response:", JSON.stringify(result));
    }
  } catch (err) {
    console.error("Upload failed with error:", err.message);
  }
}

upload();
