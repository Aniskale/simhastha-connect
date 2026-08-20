# Firebase Rules and Google Sign-In Setup

## Firestore Rules

Deploy `firestore.rules` to the `superxkhumbh` Firebase project.

Recommended CLI flow:

```powershell
firebase login
firebase use superxkhumbh
firebase deploy --only firestore:rules
```

## Create First Admin

Admin accounts must not be created from public registration screens.

1. Create an admin email/password user in Firebase Authentication.
2. Copy the UID.
3. Create Firestore document `users/{uid}`:

```json
{
  "uid": "<firebase-auth-uid>",
  "name": "Admin",
  "email": "admin@example.com",
  "mobile": "",
  "role": "admin",
  "status": "active",
  "createdAt": "manual",
  "updatedAt": "manual"
}
```

## Google Sign-In For JavaFX Desktop

Do not use Firebase web popup login in JavaFX. A secure desktop flow needs:

1. Google Cloud OAuth consent screen configured.
2. OAuth Client ID type: Desktop app.
3. Local redirect URI strategy, for example `http://127.0.0.1:<random-port>/callback`.
4. Exchange Google authorization code for Google ID token.
5. Exchange Google ID token with Firebase `accounts:signInWithIdp`.
6. Load/create `users/{uid}` profile.

Default role for a new Google account should be only `user`.
Business, transport operator and admin roles must use explicit role flows or admin assignment.
