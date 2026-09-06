# Business inventory hold integration

The presentation booking flow validates the inventory value currently displayed to the user. It deliberately does not decrement inventory on the JavaFX client.

Before a production business booking creates a Razorpay order, the trusted payment backend atomically reads the `businessItems` record, verifies `availableUnits`, decrements it, and creates the canonical pending booking. On Razorpay verified payment it converts that hold to confirmed; on cancellation, failure, or expiry it restores the units. This requires backend-only Firebase Admin SDK credentials through `GOOGLE_APPLICATION_CREDENTIALS` (or `PAYMENTS_FIREBASE_SERVICE_ACCOUNT_PATH`).

No client-side or in-memory substitute is permitted.

The backend exposes this lifecycle boundary through
`BusinessInventoryHoldService`: `createHold` runs before an order is created,
`confirmHold` runs only after Razorpay signature verification, and `releaseHold`
runs for cancellation, verification failure, webhook failure, or an order-creation
failure. The shipped fallback is intentionally fail-closed for `BUSINESS` payments
until the service account is configured. It does not open Razorpay or claim a
booking is confirmed without Admin SDK transaction access.
