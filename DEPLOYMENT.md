# Deployment Guide

This project consists of two parts that need to be deployed separately:

## Architecture Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Frontend     │────▶│    Backend      │────▶│   MongoDB       │
│  (Vercel/etc)   │     │ (Railway/etc)   │     │    Atlas        │
│  React + Vite   │     │  Spring Boot    │     │   (Cloud)       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Frontend Deployment (Vercel)

### 1. Prerequisites
- GitHub account with this repository
- Vercel account (free tier works)

### 2. Deploy to Vercel

1. Go to [vercel.com](https://vercel.com) and sign in with GitHub
2. Click "New Project"
3. Import your repository
4. Set the **Root Directory** to `frontend`
5. Framework Preset: Vite
6. Build Command: `npm run build`
7. Output Directory: `dist`

### 3. Environment Variables (Vercel Dashboard)

Set these in Project Settings → Environment Variables:

| Variable | Value | Example |
|----------|-------|---------|
| `VITE_API_URL` | Your backend URL + /api | `https://your-backend.railway.app/api` |
| `VITE_WS_URL` | Your backend URL + /ws/chat | `https://your-backend.railway.app/ws/chat` |

---

## Backend Deployment (Railway / Render)

### Option A: Railway (Recommended)

1. Go to [railway.app](https://railway.app) and sign in with GitHub
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your repository
4. Railway will auto-detect Spring Boot

#### Environment Variables (Railway Dashboard)

| Variable | Value |
|----------|-------|
| `PORT` | `8080` (usually auto-set) |
| `MONGODB_URI` | Your MongoDB Atlas connection string |
| `MONGODB_DATABASE` | `webapp` |
| `JWT_SECRET` | A long random string (64+ chars) |
| `JWT_EXPIRATION_MS` | `86400000` |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend.vercel.app` |
| `LOG_LEVEL` | `INFO` |

### Option B: Render

1. Go to [render.com](https://render.com) and sign in
2. New → Web Service
3. Connect your repository
4. Settings:
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/webapp-1.0.0.jar`
   - **Environment**: Java 21

---

## MongoDB Atlas Setup

Your project already uses MongoDB Atlas. Make sure:

1. **Network Access**: Add `0.0.0.0/0` to allow connections from any IP (for Railway/Render)
2. **Database User**: Create a user with read/write permissions
3. **Connection String**: Use the SRV format: `mongodb+srv://user:pass@cluster.mongodb.net/`

---

## Post-Deployment Checklist

- [ ] Frontend loads without errors
- [ ] Login/Register works
- [ ] Teams/Projects/Tasks load correctly
- [ ] WebSocket chat connects and works
- [ ] No CORS errors in browser console

---

## Troubleshooting

### CORS Errors
Make sure `CORS_ALLOWED_ORIGINS` on the backend includes your frontend URL (no trailing slash).

### 502 Bad Gateway
Backend might be starting up (takes 30-60 seconds) or crashed. Check logs.

### WebSocket Connection Failed
1. Check `VITE_WS_URL` is correct
2. Some platforms need WebSocket support enabled
3. Use `wss://` for HTTPS sites, not `ws://`

### MongoDB Connection Timeout
1. Check Atlas Network Access whitelist
2. Verify connection string format
3. Check username/password are URL-encoded if they have special characters

---

## Local Development

```bash
# Frontend
cd frontend
cp .env.example .env.local
npm install
npm run dev

# Backend (separate terminal)
cd ..
./mvnw spring-boot:run
```
