# API Reference

## Auth

- `POST /auth/login`
- `POST /auth/register/borrower`

## Borrowers

- `POST /borrowers`
- `GET /borrowers`
- `GET /borrowers/{id}`
- `PUT /borrowers/{id}`
- `GET /borrowers/{id}/documents`
- `POST /borrowers/{id}/documents`

## Loans

- `POST /loans/apply`
- `GET /loans`
- `GET /loans/{loanId}`
- `POST /loans/approve`
- `POST /loans/reject`
- `GET /loans/{loanId}/schedule`

## Repayments

- `POST /repayments`
- `GET /repayments/{loanId}`

## Risk

- `POST /risk/check`

## Management

- `GET /blacklist`
- `POST /blacklist`
- `GET /dashboard/summary`
- `GET /dashboard/audit-logs`
- `GET /notifications/me`

## Example Loan Application Payload

```json
{
  "borrowerId": 1,
  "loanAmount": 1000,
  "interestRate": 30,
  "loanTermDays": 30
}
```

## Example Risk Response

```json
{
  "score": 30,
  "band": "SAFE",
  "factors": [
    "New borrower with no repayment history"
  ]
}
```
