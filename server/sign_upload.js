// Lightweight Node.js signing endpoint for Cloudinary upload signatures
// Usage: set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET in environment

const express = require('express');
const cloudinary = require('cloudinary').v2;
const cors = require('cors');

const app = express();
app.use(cors());

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET
});

app.get('/sign', (req, res) => {
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = cloudinary.utils.api_sign_request({ timestamp }, process.env.CLOUDINARY_API_SECRET);
  res.json({ signature, timestamp, api_key: process.env.CLOUDINARY_API_KEY, cloud_name: process.env.CLOUDINARY_CLOUD_NAME });
});

const port = process.env.PORT || 4000;
app.listen(port, () => console.log(`Cloudinary signing server listening on ${port}`));
