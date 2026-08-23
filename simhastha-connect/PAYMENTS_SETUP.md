# Simhastha Connect Payment Foundation

This payment foundation now includes centralized order creation, backend signature verification, webhook reconciliation, payment status persistence, recovery queries, a generic booking-confirmation hook, and initial module connections for the existing JavaFX app.
Part 3 connects the currently available Simhastha Connect modules to that centralized payment flow without adding separate Razorpay implementations.

## JavaFX client configuration

The JavaFX app reads optional payment settings from system properties, environment variables, or `src/main/resources/payment.properties`.

Required client-side values:

- `PAYMENTS_ENABLED=true`
- `PAYMENTS_BACKEND_BASE_URL=http://localhost:8080`

Only the backend returns the public Razorpay Key ID needed by Checkout. Never store `RAZORPAY_KEY_SECRET` in JavaFX resources or committed files.

## Reference backend

The reference backend entry point is:

`com.simhastha.payment.backend.ReferencePaymentBackendServer`

It exposes:

`POST /api/payments/order`
`POST /api/payments/verify`
`POST /api/payments/cancel`
`GET /api/payments/status?internalPaymentId=...`
`GET /api/payments/status?bookingId=...&userId=...`
`POST /api/payments/webhook`

Required backend environment variables:

- `PAYMENTS_BACKEND_ENABLED=true`
- `RAZORPAY_KEY_ID=rzp_test_...`
- `RAZORPAY_KEY_SECRET=...`
- `RAZORPAY_WEBHOOK_SECRET=...`
- `PAYMENTS_BACKEND_PORT=8080` optional
- `PAYMENTS_FIRESTORE_PROJECT_ID=...` optional, defaults to `FIREBASE_PROJECT_ID`
- `PAYMENTS_FIRESTORE_BEARER_TOKEN=...` optional trusted server token for Firestore REST writes

The backend creates Razorpay orders with amount converted to paise and returns only the public checkout data:

- `internalPaymentId`
- `razorpayOrderId`
- `keyId`
- `amount`
- `currency`
- `receipt`
- `status`

## Verification flow

After Razorpay Checkout returns `razorpay_payment_id`, `razorpay_order_id`, and `razorpay_signature`, the JavaFX client sends them to `/api/payments/verify`. The backend verifies the signature with `RAZORPAY_KEY_SECRET`, checks payment ownership against the stored internal payment record, and only then moves the centralized payment record to a successful state.

If JavaFX loses connectivity after checkout, the app returns a safe pending result:

`Payment is being verified. Please do not pay again.`

The webhook endpoint is used for authoritative reconciliation if the app closes or retries happen.

## Firestore payments collection

Payment records are stored in the centralized `payments` collection when trusted backend Firestore configuration is provided. Documents include:

- internal payment and booking IDs
- user/module/item/business references
- Razorpay order/payment IDs
- amount in paise and currency
- payment and verification status
- idempotency key
- timestamps
- refund foundation fields
- source/provider

Do not store Razorpay key secrets, webhook secrets, or private credentials in Firestore.

Firestore rules allow users to read their own payment records and admins to read/write. Normal clients cannot mark payments as paid. In production, use a trusted backend/Admin SDK or IAM-authenticated server path for authoritative payment writes.

## Webhook setup

In the Razorpay Dashboard, configure a webhook URL pointing to:

`https://your-backend.example.com/api/payments/webhook`

Configure the same webhook secret in the backend as `RAZORPAY_WEBHOOK_SECRET`.

Structured event support exists for:

- `payment.authorized`
- `payment.captured`
- `payment.failed`
- `order.paid`
- `refund.processed`

Duplicate webhook deliveries are ignored idempotently.

## Recovery/status refresh

Use:

- `GET /api/payments/status?internalPaymentId=...`
- `GET /api/payments/status?bookingId=...&userId=...`

These are the foundations for future user payment history and booking recovery screens.

## Part 2 status

Part 2 now includes verification, webhook foundation, payment persistence, and a generic booking confirmation hook.

## Part 3 module integration

The following user-facing flows now create centralized `PaymentRequest` objects and call the shared `PaymentService`:

- Kumbh Packages: `PACKAGE`
- Stay reservations: `STAY`
- Verified paid Puja services: `PUJA`
- Payment-enabled Business services: `BUSINESS`
- Optional private paid transport booking: `TRANSPORT`

The following remain free/public and do not require Razorpay:

- Government transport information
- Emergency and women/child safety information
- Lost & Found
- Ghats & Snan information
- Government schedule
- Announcements

Server-side price validation uses approved catalog item IDs and rejects mismatched client amounts. In Part 3 this is catalog-based because the current project does not yet have separate production package/stay/puja/business booking repositories.

## Tickets and dashboards

Verified successful payments create a reusable booking/ticket record in the app state and display a printable Simhastha Connect E-Ticket. The ticket contains safe verification metadata and a QR-style verification reference. It does not contain Razorpay secrets or private credentials.

Dashboard surfaces added:

- User Dashboard: `My Bookings & Payments`
- Admin Dashboard: `Payments & Transactions`
- Business Dashboard: `Payments & Revenue`

## Refund foundation

Refund status fields and webhook handling for `refund.processed` exist in the centralized payment model/backend. A full admin-triggered Razorpay refund API action is not enabled yet, so the UI does not fake successful refunds.

## Deferred after Part 3

- Module-specific pricing validation against real booking records
- Deeper user payment history filters
- Advanced admin payment dashboard filters/details
- Business-owner scoping against production business ownership records
- Refund request/action UI
- Full production end-to-end Razorpay testing
