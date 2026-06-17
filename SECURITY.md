Security and Cloudinary/Firebase Best Practices

1. Do NOT embed Cloudinary `api_secret` in the mobile application. Use `upload_preset` for unsigned uploads or sign upload requests server-side.

2. Use the Secrets Gradle Plugin (`app/.env`) for local development and CI secrets, and ensure `.env` is in `.gitignore`.

3. Recommended upload flow for production:
   - For public media (avatars, post images) create an unsigned `upload_preset` with appropriate `allowed_formats` and `folder` restrictions.
   - For sensitive uploads, implement a server endpoint that signs the upload parameters using `api_secret` and returns the `signature` to the client.

4. Firebase Security Rules:
   - Require authentication for writes to `users`, `posts`, `messages`, `projects`, `jobs`, and `groups`.
   - Validate username uniqueness and disallow clients from setting `isAdmin` or other privileged fields.
   - Example (Firestore) rules snippet:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if true;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
      allow delete: if false;
    }

    match /posts/{postId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

5. FCM & Notifications: Keep tokens server-side mapped to user ids. Use Cloud Functions or your server to send targeted push notifications.

6. Crashlytics & Analytics: Initialize in `Application` or early in startup; keep data collection under user consent where required.

7. Rotate Cloudinary API keys if they were previously committed. Treat the keys in `app/.env` as compromised if they were pushed to a public repo.
