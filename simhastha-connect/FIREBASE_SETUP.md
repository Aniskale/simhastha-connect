# Firebase Setup for Simhastha Connect

This app is wired for Cloud Firestore through the Firestore REST API.

## 1. Create a Web App in Firebase

1. Open Firebase Console.
2. Select the `SuperXKhumbh` project.
3. Click `Add app`.
4. Choose Web app.
5. Register it as `Simhastha Connect Desktop`.
6. Copy the `apiKey` from the Firebase config object.

## 2. Create Firestore Database

1. Open `Firestore Database`.
2. Click `Create database`.
3. Select a nearby location, such as `asia-south1` if available for your project.
4. Start in test mode for development only.

## 3. Add Local Config

Copy:

```text
src/main/resources/firebase.properties.example
```

to:

```text
src/main/resources/firebase.properties
```

Then fill:

```properties
firebase.enabled=true
firebase.projectId=superxkhumbh
firebase.apiKey=YOUR_WEB_API_KEY
```

## 4. Temporary Development Rules

Use these only while testing:

```js
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /appItems/{docId} {
      allow read, write: if true;
    }

    match /approvalRequests/{docId} {
      allow read, write: if true;
    }
  }
}
```

## 5. Production Direction

For production, do not keep public write rules and do not put a service account key inside this desktop app.

Use one of these:

- Firebase Auth plus role-based Firestore rules.
- A small backend API or Cloud Functions for admin-only writes.
- Service account only on a server, never inside the JavaFX app.

## 6. Firestore Collections Used

The app uses:

- `appItems`
- `approvalRequests`

`appItems` fields:

- `module`
- `title`
- `detail`
- `category`

`approvalRequests` fields:

- `type`
- `title`
- `detail`
- `targetModule`
