# Loan Shark — User Manual

**Loan Management System for Micro-Lenders**  
Version 1.0 | Web Portal & Borrower Self-Service

---

## Copy this block when asking ChatGPT for a PDF with images

```
I have a User Manual in Markdown (see full text below). I need a PDF-ready document with images.

- The manual contains FIGURE placeholders (FIGURE 1, FIGURE 2, … FIGURE 29). Each FIGURE has an "Image description:" line that describes exactly what the image should show (e.g. a login screen, a table, a dialog).
- For each FIGURE: either (a) generate a detailed image description or mockup instruction so I can add screenshots later, or (b) create a simple placeholder image (e.g. a box with the figure number and caption) that I can replace with real screenshots.
- Keep all section headings, steps, tables, and text exactly as in the manual. Only add or replace FIGUREs with image placeholders/descriptions.
- Output a single, well-formatted document (Markdown or HTML) that I can save as PDF. Number figures clearly (Figure 1, Figure 2, etc.) and keep the captions provided.
```

---

## Instructions for Adding Images (for ChatGPT or document designer)

This manual uses **FIGURE** placeholders. Each placeholder describes exactly what the image should show so you can:

- **Option A:** Paste this manual into ChatGPT and ask: *"Create a PDF-ready document with images. For each FIGURE, generate a clear description or placeholder image of: [paste the FIGURE description]."*
- **Option B:** Take real screenshots from the running app and replace the FIGURE blocks with those screenshots when producing the final PDF.

Every **FIGURE** below is written so that an AI or human can produce a matching image (screenshot, mockup, or illustration) without opening the application.

---

## 1. Introduction

### 1.1 What is Loan Shark?

Loan Shark is a **loan management system** for micro-lenders and loan providers. It supports:

- **Staff (Owner and Cashier):** Managing borrowers, loans, repayments, interest settings, user accounts, and manual verification reviews.
- **Borrowers:** Registering with identity verification (KYC), viewing their profile, applying for loans, seeing repayment schedules, and reading notifications.

The system runs as a **web application** (Vue 3 + Vuetify). Users sign in and see different menus and pages depending on their role.

### 1.2 User roles

| Role      | Who uses it        | Main capabilities |
|----------|---------------------|-------------------|
| **Owner** | Business owner/admin | Everything: dashboard, borrowers, loans, repayments, **users**, **verifications**, **blacklist**, **loan interest & term settings**. |
| **Cashier** | Front-desk staff   | Dashboard, borrowers, loans, repayments. Cannot manage users, verifications, blacklist, or interest settings. |
| **Borrower** | Loan customers     | Own profile, loans, repayment schedule, notifications. Must complete KYC and be approved before full access. |

---

**FIGURE 1 — System overview diagram**  
*Image description:* A simple diagram with three boxes: "Owner", "Cashier", "Borrower". Arrows from "Owner" to "Full system", from "Cashier" to "Operations (no settings/users/verifications)", and from "Borrower" to "My portal (profile, loans, schedule, notifications)". Caption: "Role-based access in Loan Shark."

---

## 2. Getting started

### 2.1 First-time setup: Create the owner account

If no owner exists yet, the login page shows **only** the form to create the first owner account.

**Steps:**

1. Open the application in your browser (e.g. `http://localhost:5174` or your deployed URL).
2. You should see a card titled something like **"Create owner account"** or **"Owner username"** and **"Password"**.
3. Enter a username and password for the owner.
4. Click **"Create Owner Account"**.
5. After success, the same page will then show **"Sign In"** and **"Borrower Registration"** tabs.

---

**FIGURE 2 — First-time owner registration screen**  
*Image description:* A centered card on a neutral background. Title: "Create owner account" or similar. Two fields: "Owner username" and "Password". One primary button: "Create Owner Account". No sidebar or navigation yet. Caption: "First-time setup: create the owner account."

---

### 2.2 Sign in (staff or borrower)

After the owner account exists:

1. Open the login page.
2. Ensure **"Sign In"** is selected (not "Borrower Registration").
3. Enter **Username** and **Password**.
4. Click **"Sign In"**.
5. You are redirected to:
   - **Owner/Cashier:** Dashboard.
   - **Borrower (approved):** My Profile (borrower portal).
   - **Borrower (pending verification):** Verification Status page only.

---

**FIGURE 3 — Login page (Sign In)**  
*Image description:* A centered card, max width about 440px. Two tabs or buttons: "Sign In" (active) and "Borrower Registration". Form with "Username" and "Password" fields and a "Sign In" button. Short descriptive text above the form. Caption: "Login page — Sign In."

---

**FIGURE 4 — Login page (Borrower Registration)**  
*Image description:* Same card as Figure 3, but "Borrower Registration" is selected. No login form; instead a multi-step stepper with steps: "Details", "Location", "ID PDF", "Selfie", "Review". Step 1 (Details) visible with fields: Username, Password, First name, Last name, ID number, Phone, Email, Address, Employment, Income, Employer. Caption: "Login page — Borrower Registration (Step 1: Details)."

---

### 2.3 Layout after login

Once signed in, the screen has:

- **Left:** A **navigation drawer** (sidebar) with:
  - App title: **"Loan Shark"** and **"Operations Portal"** (or similar).
  - Menu items that depend on role (see sections below).
  - At the bottom: **"Signed in as [Role]"** and a **Logout** button.
- **Top:** An **app bar** with a menu icon and title **"Loan Management Portal"**.
- **Main area:** The current page (dashboard, borrowers, loans, etc.).

---

**FIGURE 5 — Staff layout (Owner) — Dashboard**  
*Image description:* Left sidebar with "Loan Shark", "Operations Portal", and menu items: Dashboard, Borrowers, Loans, Repayments, Users, Verifications, Blacklist, Loan interest & term. Bottom: "Signed in as Owner", Logout. Main area: "Operations Dashboard" with metric cards (e.g. Total loans, Active borrowers, Overdue) and a "Recent Actions" table with columns Category, Action, Reference, Amount, Performed By, Authorized By, Date. Search and pagination. Caption: "Owner view: Dashboard and navigation."

---

**FIGURE 6 — Borrower layout (approved) — My Profile**  
*Image description:* Left sidebar with "Loan Shark" and menu: My Profile, My Loans, Repayment Schedule, Notifications. Bottom: "Signed in as Borrower", Logout. Main area: "My Profile" or "Borrower account" page with borrower details. Caption: "Borrower view: Portal navigation and profile."

---

## 3. Borrower registration (KYC)

Borrowers register from the login page by choosing **"Borrower Registration"**. Registration is a **5-step process** (stepper).

### 3.1 Step 1 — Details

- **Username**, **Password**
- **First name**, **Last name**
- **ID number** (South African ID; format is validated)
- **Phone**, **Email**
- **Address**
- **Employment status**, **Monthly income**, **Employer name**

The system shows whether the ID number is valid (e.g. "South African ID number format is valid").  
Click **Next** to go to Step 2.

---

**FIGURE 7 — Borrower registration — Step 1 (Details)**  
*Image description:* Stepper with "Details" active. Form with Username, Password, First name, Last name, ID number (with validation message), Phone, Email, Address, Employment status, Monthly income, Employer name. "Next" and "Back" buttons. Caption: "Borrower registration — Personal details."

---

### 3.2 Step 2 — Location

- Location is captured **automatically** when the step loads (browser asks for location permission).
- The page shows status: e.g. "Location captured" with the address or coordinates, or "Location permission denied" / "Waiting for location…".
- If permission was denied, the user is warned that the profile cannot be saved without location when they reach Submit.
- **Next** continues to Step 3.

---

**FIGURE 8 — Borrower registration — Step 2 (Location)**  
*Image description:* Stepper with "Location" active. Text or card showing "Location captured" and a full address (e.g. "Alexandra, Johannesburg, Gauteng, South Africa") and/or coordinates. Optional "Retry" button. "Next" and "Back". Caption: "Borrower registration — Location capture."

---

### 3.3 Step 3 — ID PDF

- Borrower must **upload a PDF** of their ID document.
- Only PDF is accepted; the file name is shown after selection.
- **Next** goes to Step 4.

---

**FIGURE 9 — Borrower registration — Step 3 (ID PDF)**  
*Image description:* Stepper with "ID PDF" active. File upload area or input for "ID copy (PDF only)" with a chosen file name (e.g. "ID.pdf"). "Next" and "Back". Caption: "Borrower registration — ID document upload."

---

### 3.4 Step 4 — Selfie

- Borrower must take a **live selfie** using the device camera (no file upload).
- The page shows a camera preview; user clicks to **capture** or **retake**.
- When capture is successful, a message like "Live selfie captured successfully" is shown.
- **Next** goes to Step 5.

---

**FIGURE 10 — Borrower registration — Step 4 (Selfie)**  
*Image description:* Stepper with "Selfie" active. Camera preview or placeholder for camera with "Capture" and "Retake" buttons. Status: "Live selfie captured successfully." "Next" and "Back". Caption: "Borrower registration — Selfie capture."

---

### 3.5 Step 5 — Review and submit

- A **summary** of entered details (and optionally location, ID file name, selfie status).
- **Submit** sends the registration.
- If validation fails (e.g. ID vs selfie mismatch, invalid ID, missing location), the system shows an **error message** and does not save; the user must correct and try again.
- On success: message like **"Profile created successfully. You must wait for owner review…"** and a button **"Go to verification status"** that sends the user to the Verification Status page after they sign in.

---

**FIGURE 11 — Borrower registration — Step 5 (Review & Submit)**  
*Image description:* Stepper with "Review" active. Summary list of personal details, location, ID file, selfie status. "Submit" and "Back" buttons. Caption: "Borrower registration — Review and submit."

---

**FIGURE 12 — Borrower registration — Success**  
*Image description:* Success alert: "Profile created successfully. You must wait for owner review before you can use the system. You can sign in and check your verification status." One button: "Go to verification status." Caption: "After successful borrower registration."

---

### 3.6 Borrower after registration

- Borrower can **sign in** immediately.
- Until the owner **approves** their verification, they see **only** the **Verification Status** page (and possibly a single menu item: "Verification Status").
- They cannot access My Profile, My Loans, Schedule, or Notifications until status is **ACTIVE**.

---

## 4. Verification Status (borrower)

- **Path:** After login as borrower with status not yet ACTIVE, or via menu **"Verification Status"**.
- **Content:** A card showing **KYC Review** with:
  - Status chip: e.g. PENDING_VERIFICATION, MANUAL_REVIEW, APPROVED, REJECTED.
  - Chips for **SA ID** (Valid/Invalid), **Face Match** (Passed/Needs Review).
  - Details: Borrower status, OCR confidence, face match score, review notes, location captured, last update.

---

**FIGURE 13 — Borrower — Verification Status page**  
*Image description:* Page title "Verification Status" and short description. One card "KYC Review" with status chips (e.g. MANUAL_REVIEW, SA ID Valid, Face Match Needs Review) and a list: Borrower status, OCR confidence, Face match score, Review notes, Location captured, Last update. Caption: "Borrower verification status (waiting for review)."

---

## 5. Owner / Staff — Verification reviews

Only the **Owner** can review borrower verifications.

- **Path:** Menu **Verifications**.
- **Page:** **"Verification Reviews"** — "Review borrower submissions that require manual approval or rejection."

### 5.1 Manual review queue

- A **table** lists cases: Borrower ID, Status, SA ID (Valid/Invalid), OCR, Face, **Notes** (truncated with tooltip for full text), and **Actions** (Approve / Reject).
- Search and pagination at the top.

### 5.2 Review dialog

- Click **Approve** or **Reject** on a row to open a **dialog**.
- Dialog shows:
  - **ID Copy PDF:** Embedded preview (or link to open) of the borrower’s ID document.
  - **Selfie Capture:** Image of the borrower’s selfie.
- Owner can add **Review notes** (e.g. reason for reject).
- Buttons: **Approve** or **Reject** to submit.

---

**FIGURE 14 — Verifications — Manual review queue**  
*Image description:* Page "Verification Reviews". Table columns: Borrower, Status, SA ID, OCR, Face, Notes (truncated), Actions. Rows with Approve/Reject buttons. Search and pagination. Caption: "Owner — Manual verification queue."

---

**FIGURE 15 — Verifications — Review dialog (ID + Selfie)**  
*Image description:* Large dialog "Approve verification" or "Reject verification". Left: card "ID Copy PDF" with PDF preview or iframe. Right: card "Selfie Capture" with photo. Optional "Review notes" field. "Approve" and "Reject" (or "Cancel") buttons. Caption: "Owner — Review dialog with ID and selfie for verification."

---

## 6. Dashboard (staff)

- **Path:** **Dashboard** in the menu.
- **Content:**
  - **Metric cards:** e.g. Total loans, Active borrowers, Overdue schedules (or similar).
  - **Recent Actions table** (Owner only): Category, Action, Reference, Amount, Performed By, Authorized By, Date. Table shows 5 rows per page; search and pagination. Long text in "Details" truncated with tooltip.

---

**FIGURE 16 — Dashboard — Metric cards**  
*Image description:* Three (or more) cards in a row. Each card: small title (e.g. "Total loans"), large number, short caption. Optional icon. Caption: "Dashboard — Summary metrics."

---

## 7. Borrowers (staff)

- **Path:** **Borrowers** in the menu.
- **Page:** **"Borrower Management"** — onboard customers with identity, income, contact details.

### 7.1 Borrower table

- Columns: **Name**, **Phone**, **Status**, **Risk**, **Income**.
- **Owner:** Status is a **dropdown** so the owner can change borrower status (e.g. ACTIVE, PENDING_VERIFICATION, BLACKLISTED).
- **Cashier:** Status is read-only (chip).
- Search and pagination (e.g. 8 rows per page).

### 7.2 Create borrower (staff)

- Button **"Create Borrower"** opens a **dialog**.
- Form: Username, Password, First name, Last name, ID number, Phone, Email, Address, Employment status, Monthly income, Employer name.
- Info message: This flow **skips image verification** (no ID PDF, no selfie) so staff can create a borrower when images are problematic.
- Submit creates the borrower; they can sign in and may still need verification depending on business rules.

---

**FIGURE 17 — Borrowers — Table and Create**  
*Image description:* Page "Borrower Management" with "Create Borrower" button. Table: Name, Phone, Status (dropdown for owner), Risk, Income. Search and pagination. Caption: "Borrowers list and create button."

---

**FIGURE 18 — Borrowers — Create Borrower dialog**  
*Image description:* Dialog "Create Borrower Profile". Info alert: staff onboarding skips image verification. Form with all fields listed above. "Create" and "Cancel". Caption: "Create borrower (staff, no KYC images)."

---

## 8. Loans (staff and borrower)

### 8.1 Staff — Loans page

- **Path:** **Loans** in the menu.
- **Page:** Loan list and **apply for a loan** on behalf of a borrower.
- **Table:** Loan ID, Status, Borrower, Amount, Total, Due date, actions (e.g. View schedule). Search and pagination (8 per page).
- **Apply for loan:** Button opens dialog. **Borrower** = dropdown (select borrower by name, ID at back). **Loan amount** only (no interest rate or term — those come from admin settings). Info text: interest and terms set by business; borrower can pay any amount anytime. Submit creates the application.

---

**FIGURE 19 — Loans — Staff list and Apply**  
*Image description:* Page "Loans" with "Apply For Loan" button. Table: Loan, Status, Borrower, Amount, Total, Due. Search and pagination. Caption: "Staff — Loans list."

---

**FIGURE 20 — Loans — Apply for loan dialog (staff)**  
*Image description:* Dialog "Apply For A Loan". Dropdown "Borrower" (shows name, uses ID). Field "Loan amount". Info: interest and terms from business. "Submit" and "Cancel". Caption: "Staff — Apply for loan (amount only)."

---

### 8.2 Borrower — My Loans

- **Path:** **My Loans** in the borrower menu.
- **Page:** Same idea: list of the borrower’s loans (Loan, Status, Amount, Total, Due, View Schedule). **Apply For Loan** opens a dialog where the borrower enters **only Loan amount** (no rate/term). Info text explains business-defined interest and flexible repayment.

---

**FIGURE 21 — Borrower — My Loans and Apply**  
*Image description:* Borrower portal page "My Loans". Table of own loans. "Apply For Loan" button. Dialog with only "Loan amount" and Submit. Caption: "Borrower — My loans and apply."

---

## 9. Repayments (staff)

- **Path:** **Repayments** in the menu.
- **Page:** **"Repayments"** — capture payments and review history.

### 9.1 Repayment history table

- Columns: **ID**, **Loan**, **Amount**, **Method**, **Reference**, **Payer** (staff username who captured the payment).
- Search and pagination (8 per page). **Load Loan History** or similar to refresh.

### 9.2 Record payment

- **"Record Payment"** opens a dialog.
- **Loan** = dropdown (select loan).
- **Amount paid**, **Payment method** (dropdown), **Reference number**.
- Submit records the payment.

---

**FIGURE 22 — Repayments — History and Record Payment**  
*Image description:* Page "Repayments" with "Record Payment" button. Table: ID, Loan, Amount, Method, Reference, Payer. Search and pagination. Caption: "Repayments — History and capture."

---

**FIGURE 23 — Repayments — Record payment dialog**  
*Image description:* Dialog "Capture Payment". Dropdown Loan, field Amount paid, dropdown Payment method, field Reference number. "Record Payment" and "Cancel". Caption: "Record payment dialog."

---

## 10. Repayment schedule (borrower)

- **Path:** **Repayment Schedule** in the borrower menu.
- **Page:** List or view of the borrower’s repayment schedule(s) for their loans (e.g. per loan, due dates, amounts). May be reached from "View Schedule" on a loan in My Loans.

---

**FIGURE 24 — Borrower — Repayment schedule**  
*Image description:* Borrower page "Repayment Schedule" with a table or list of schedule entries (loan, due date, amount, status, etc.). Caption: "Borrower — Repayment schedule."

---

## 11. Notifications (borrower)

- **Path:** **Notifications** in the borrower menu.
- **Page:** List of in-app notifications (e.g. profile created, status change, application status change). Messages may be truncated with tooltip for full text. After reading, notifications can disappear from the list but remain in the database. Pagination (8 per page).

---

**FIGURE 25 — Borrower — Notifications**  
*Image description:* Page "Notifications" with a list of notification items (title/message, date). Optional read/unread state. Pagination. Caption: "Borrower — Notifications list."

---

## 12. Users (owner only)

- **Path:** **Users** in the menu (Owner only).
- **Page:** **"User Management"** — owner can create, edit, disable, delete any user.

### 12.1 Users table

- Columns: **Username**, **Role**, **Status**, **Borrower Link** (e.g. Borrower #id or "-"), **Created**, **Actions** (Edit, Delete).
- Search and pagination.

### 12.2 Create / Edit user

- **"Create User"** opens a dialog. Fields: **Username**, **Password**, **Role** (e.g. OWNER, CASHIER, BORROWER), **Status** (e.g. ACTIVE, INACTIVE).
- **Edit:** Same dialog, password optional (leave blank to keep). **Update User** / **Create User** and **Cancel**.

---

**FIGURE 26 — Users — Table and Create**  
*Image description:* Page "User Management" with "Create User" and "Owner only" chip. Table: Username, Role, Status, Borrower Link, Created, Actions (Edit, Delete). Caption: "Owner — User management."

---

**FIGURE 27 — Users — Create/Edit user dialog**  
*Image description:* Dialog "Create User" or "Edit User". Username, Password (optional when editing), Role dropdown, Status dropdown. "Create User" / "Update User" and "Cancel". Caption: "Create or edit user (owner only)."

---

## 13. Blacklist (owner only)

- **Path:** **Blacklist** in the menu.
- **Page:** List of blacklisted entries. Table: borrower/user reference, **Reason** (truncated with tooltip if long), date, actions. Search and pagination (8 per page). Owner can add/remove or manage blacklist entries.

---

**FIGURE 28 — Blacklist — Table**  
*Image description:* Page "Blacklist" with table of blacklist entries. Columns e.g. Borrower/User, Reason (truncated), Date, Actions. Search and pagination. Caption: "Owner — Blacklist management."

---

## 14. Loan interest & term settings (owner only)

- **Path:** **Loan interest & term** in the menu.
- **Page:** **"Loan interest & term settings"** — configure how interest and terms apply when borrowers apply with only an amount.

### 14.1 Settings form

- **Default interest rate (%)**
- **Interest type:** Simple or Compound
- **Interest period (days)** (e.g. 30)
- **Grace period (days)** (e.g. 0 or 2 — days after due date before extra interest)
- **Default loan term (days)** (e.g. 365 — used when borrower does not specify term)
- **Save settings** button.

Success/error messages shown after save.

---

**FIGURE 29 — Loan interest & term settings**  
*Image description:* Page "Loan interest & term settings" with "Owner only" chip. Card "Current settings" with form: Default interest rate (%), Interest type (Simple/Compound), Interest period (days), Grace period (days), Default loan term (days). "Save settings" button. Caption: "Owner — Loan interest and term configuration."

---

## 15. Currency and table behavior

- **Currency:** All monetary amounts are shown **formatted** (e.g. with currency symbol and thousands separators) across the app.
- **Tables:** Dashboard "Recent Actions" shows **5 rows** per page; other tables (borrowers, loans, repayments, users, blacklist, notifications, verifications) show **8 rows** per page. **Search** at the top filters from the server; **pagination** moves between pages.
- **Long text:** In tables, long text (e.g. verification notes, action details, blacklist reason, notification message) is **truncated** (e.g. 90 characters) with "…"; **tooltip** shows the full text on hover.

---

## 16. Logout

- In the **left sidebar**, at the bottom, click **Logout**.
- Session is cleared and the user is redirected to the **Login** page.

---

## 17. Quick reference — Routes and access

| Path | Who | Description |
|------|-----|-------------|
| `/login` | All | Login, first-time owner creation, borrower registration |
| `/dashboard` | Owner, Cashier | Operations dashboard |
| `/borrowers` | Owner, Cashier | Borrower list and create |
| `/loans` | Owner, Cashier | Loan list and apply |
| `/repayments` | Owner, Cashier | Repayment history and record payment |
| `/users` | Owner only | User CRUD |
| `/verifications` | Owner only | Manual KYC review queue and approve/reject |
| `/blacklist` | Owner only | Blacklist management |
| `/settings/loan-interest` | Owner only | Interest rate, type, period, grace, default term |
| `/my-portal/profile` | Borrower (ACTIVE) | My profile |
| `/my-portal/loans` | Borrower (ACTIVE) | My loans and apply |
| `/my-portal/schedule` | Borrower (ACTIVE) | Repayment schedule |
| `/my-portal/notifications` | Borrower (ACTIVE) | Notifications |
| `/my-portal/verification` | Borrower (any) | Verification status (only page when not ACTIVE) |

---

## 18. Converting this manual to PDF

1. **With Microsoft Word:** Open this `.md` file in Word (or paste the content), insert screenshots or generated images where each FIGURE is described, then **File → Save As → PDF**.
2. **With Pandoc:**  
   `pandoc docs/USER_MANUAL.md -o docs/USER_MANUAL.pdf`  
   Add `--resource-path` if images are in a folder. You can replace FIGURE blocks with image files and reference them as `![Caption](image.png)`.
3. **For ChatGPT:** Paste this manual and say: *"Use this user manual. For each FIGURE, add a clear image placeholder or description so I can replace it with a screenshot. Output a single document (or section) ready to be saved as PDF, with [FIGURE N] replaced by a short image description or placeholder box labeled FIGURE N."*

---

*End of User Manual*
