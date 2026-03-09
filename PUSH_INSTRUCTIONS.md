# Push LendHand to GitHub

Your project is committed locally. To create the repo and push:

## 1. Create the repository on GitHub

1. Go to **https://github.com/new**
2. **Repository name:** `lendhand` or `loan-shark` (or any name you prefer)
3. Leave it **empty** (no README, .gitignore, or license)
4. Click **Create repository**

## 2. Add remote and push (PowerShell)

Replace `YOUR_USERNAME` with your GitHub username if different from `pbhaninaa`:

```powershell
cd "C:\Users\philasandeB\Desktop\My Apps\Loan Shark"

git remote add origin https://github.com/YOUR_USERNAME/loan-shark.git
git branch -M main
git push -u origin main
```

If you already added `origin` with a different URL:

```powershell
git remote set-url origin https://github.com/YOUR_USERNAME/loan-shark.git
git push -u origin main
```

## 3. If you prefer SSH

```powershell
git remote add origin git@github.com:YOUR_USERNAME/loan-shark.git
git push -u origin main
```

After the first push, your code will be on GitHub.
