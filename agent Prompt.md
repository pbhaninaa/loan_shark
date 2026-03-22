# LendHand System Upgrade Specification

## Migration to Multi-Merchant (SaaS) Architecture

---

## 📌 Objective

Transform the existing **single-business loan management system** into a **multi-tenant SaaS platform** where multiple businesses (merchants) can independently use the system while being fully isolated from each other.

Additionally, introduce a **subscription and billing system** managed by Support roles.

---

## 🧠 Core Concept

The system must support:

* Multiple **merchants (businesses)**
* Each merchant has:

  * Its own users
  * Its own borrowers
  * Its own loans and transactions
* **Invite-based onboarding** for users
* A **Support layer** for system oversight and billing
* A **subscription + transaction fee model**

---

## 🏗️ Required Architectural Changes

---

## 1. 🏢 Merchant (Business Profile)

### Table: `merchants`

Fields:

* `id`
* `name`
* `email`
* `phone`
* `status` (ACTIVE / INACTIVE / SUSPENDED)
* `created_at`

---

## 2. 👥 Users Update

### Modify `users` table:

Add:

* `merchant_id` (nullable for support users)

Fields:

* `id`
* `username`
* `password`
* `role`
* `merchant_id`

---

## 3. 🔗 Data Isolation

Add `merchant_id` to:

* borrowers
* loans
* repayments
* transactions
* blacklist

---

## 4. 🔐 Role System Redesign

### Merchant Roles:

* ADMIN
* CASHIER
* BORROWER

---

### Support Roles (NEW):

#### 1. SUPPORT_ADMIN

* Full system access
* Manages subscriptions, pricing, rules

#### 2. SUPPORT_USER

* Can view all merchants
* Can assist/support merchants
* Cannot manage pricing or system rules

---

## 5. 🔑 Authentication (JWT)

JWT must include:

```json
{
  "userId": 1,
  "role": "ADMIN",
  "merchantId": 10
}
```

---

## 6. ⚙️ Backend Logic

* Merchant users → filter by `merchant_id`
* Support users → access all data

---

## 7. 🧩 API Changes

### Support Admin APIs

* Manage subscriptions
* Set pricing rules
* View revenue analytics
* Send notifications

---

### Support User APIs

* View merchants
* View loans/transactions
* Assist merchants

---

## 8. 🔗 Merchant Invite System (NEW - REQUIRED)

### 📌 Purpose

Ensure users (Borrowers / Cashiers) are securely linked to a merchant **without manually selecting a merchant**.

---

### 🗄️ New Table: `merchant_invites`

Fields:

* `id`
* `merchant_id` (Foreign Key → merchants.id)
* `email` (optional)
* `token` (UNIQUE, secure identifier)
* `role` (BORROWER / CASHIER)
* `status` (PENDING / USED / EXPIRED)
* `expiry_date`
* `created_at`

---

### 🔑 Invite Link Format

```
https://yourapp.com/register?token=abc123xyz
```

---

### ⚙️ Backend Flow

#### Step 1: Create Invite

* Only ADMIN can create invites
* Generate secure token (UUID)
* Assign:

  * `merchant_id`
  * `role`
  * `expiry_date`

---

#### Step 2: Validate Invite

* When user opens link:

  * Validate token exists
  * Check status = PENDING
  * Check not expired

---

#### Step 3: Register via Invite

* User submits registration form
* System:

  * Extracts `merchant_id` from invite
  * Assigns role from invite
  * Creates user
  * Marks invite as USED

---

### 🔐 Security Rules

* Token must be:

  * Unique
  * Random (UUID recommended)
* Invite must:

  * Expire (24–72 hours)
  * Be usable only once
* Do NOT allow manual merchant selection

---

### 🚫 Important Constraint

❌ No dropdown or manual merchant selection during registration
✅ Merchant must ONLY be assigned via invite

  

---

## 9. 💰 Subscription & Billing System (NEW)

---

## 🗄️ Table: `subscriptions`

Fields:

* `id`
* `merchant_id`
* `monthly_fee`
* `status` (ACTIVE / OVERDUE / CANCELLED)
* `next_billing_date`
* `last_payment_date`

---

## 🗄️ Table: `subscription_payments`

Fields:

* `id`
* `merchant_id`
* `amount`
* `payment_date`
* `status` (PAID / FAILED)

---

## 🗄️ Table: `transaction_fees`

Tracks revenue per action:

Fields:

* `id`
* `merchant_id`
* `reference_id` (loan/payment id)
* `type` (LOAN_APPLICATION / REPAYMENT)
* `amount`
* `created_at`

---

## 💡 Billing Model

Each merchant pays:

### 1. Monthly Subscription Fee

* Fixed amount set by SUPPORT_ADMIN

---

### 2. Transaction-Based Fees

Charge for:

* Loan application
* Loan repayment
* Any money-related client action

---

## 10. ⚙️ Fee Calculation Logic

Example:

```text
Loan Application Fee = R5  
Repayment Fee = R2 per transaction
```

---

## 11. 📊 Revenue Tracking (SUPPORT ADMIN)

Support Admin must see:

### Per Merchant:

* Total subscription paid
* Total transaction fees
* Total revenue generated

---

### System Wide:

* Total monthly subscriptions
* Total transaction fees
* Grand total revenue

---

## 12. 📊 Required Dashboards

### Support Admin Dashboard

* Total merchants
* Active vs overdue subscriptions
* Revenue per merchant
* Total system revenue
* Transaction volume

---

### Merchant Dashboard

* Current subscription status
* Monthly charges
* Transaction fee summary

---

## 13. 🔔 Notifications System

---

## Types:

### 1. Subscription Reminders

* Before due date
* On due date
* After overdue

---

### 2. Payment Confirmation

* When subscription is paid

---

### 3. System Notifications

* Admin messages to merchants

---

## 🗄️ Table: `notifications`

Fields:

* `id`
* `merchant_id` (nullable for global)
* `message`
* `type`
* `status` (READ / UNREAD)
* `created_at`

---

## 14. 📩 Notification Capabilities

### Support Admin Can:

* Send reminders
* Send bulk notifications
* Notify overdue merchants

---

## 15. 🚨 Subscription Enforcement

If merchant is:

### OVERDUE:

* Restrict:

  * Loan creation
  * Transactions

---

### SUSPENDED:

* Full access blocked

---

## 16. 🔄 Updated System Flow

1. Create Merchant
2. Create Admin
3. Admin invites users
4. Merchant operates system
5. System tracks transactions
6. Fees accumulate automatically
7. Monthly billing generated
8. Support Admin monitors payments
9. Notifications sent if overdue

---

## 17. 🛡️ Security Rules

* Enforce `merchant_id` filtering
* Prevent cross-merchant access
* Restrict pricing to SUPPORT_ADMIN
* Log all financial operations

---

## 18. 🚨 Migration Considerations

* Assign default merchant to existing data
* Create default subscription for existing merchant
* Backfill transaction fee records if needed

---

## 19. 📈 Future Enhancements

* Keep the payment process as it is for now i will enhance it later 
* Invoice generation
* Tiered pricing plans

---

## ✅ Deliverables Expected

* Subscription system implementation
* Transaction fee tracking
* Support Admin & Support User roles
* Revenue dashboards
* Notification system
* Billing enforcement logic

---

## 📌 Summary

This system now becomes a **fully monetized SaaS platform**, enabling:

* Multi-merchant operations
* Secure onboarding via invites
* Subscription-based revenue
* Transaction-based earnings
* Full admin oversight and control

---

**End of Specification**
