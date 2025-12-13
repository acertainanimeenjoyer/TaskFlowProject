# TaskFlow Deployment Guide

This repository contains both the frontend and backend for the TaskFlow application, organized separately for easy deployment to different services.

## Project Structure

```
TaskFlow/
├── frontend/          # React + TypeScript + Vite
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── src/              # Spring Boot backend
├── pom.xml           # Maven configuration
└── README.md
```

## Frontend Deployment

### Deploy to Vercel (Recommended)

1. **Connect Repository**
   - Go to [Vercel](https://vercel.com)
   - Import your GitHub repository
   - Select the `frontend` folder as the root directory

2. **Configure Build Settings**
   - Framework Preset: `Vite`
   - Root Directory: `frontend`
   - Build Command: `npm run build`
   - Output Directory: `dist`
   - Install Command: `npm install`

3. **Environment Variables**
   ```
   VITE_API_URL=<your-backend-url>/api
   ```

### Alternative: Deploy to Netlify

1. **Build Settings**
   - Base directory: `frontend`
   - Build command: `npm run build`
   - Publish directory: `frontend/dist`

2. **Environment Variables**
   ```
   VITE_API_URL=<your-backend-url>/api
   ```

### Local Development

```bash
cd frontend
npm install
npm run dev
```

## Backend Deployment

### Deploy to Railway

1. **Create New Project**
   - Go to [Railway](https://railway.app)
   - Create new project from GitHub repo
   - Select root directory (backend is at root)

2. **Configure Settings**
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/webapp-1.0.0.jar`
   - Port: `8080`

3. **Environment Variables**
   ```
   SPRING_DATA_MONGODB_URI=<your-mongodb-connection-string>
   JWT_SECRET=<your-jwt-secret-key>
   ALLOWED_ORIGINS=<your-frontend-url>
   ```

### Alternative: Deploy to Render

1. **Build Settings**
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/webapp-1.0.0.jar`

2. **Environment Variables** (same as above)

### Alternative: Deploy to Heroku

1. **Create Procfile** (if not exists)
   ```
   web: java -jar target/webapp-1.0.0.jar
   ```

2. **Deploy**
   ```bash
   heroku create your-app-name
   git push heroku main
   ```

### Local Development

```bash
mvn spring-boot:run
```

## MongoDB Setup

The application requires a MongoDB database. You can:

1. **Use MongoDB Atlas** (Recommended for production)
   - Create free cluster at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
   - Get connection string
   - Add to backend environment variables

2. **Local MongoDB**
   - Install MongoDB locally
   - Connection string: `mongodb://localhost:27017/taskflow`

## Environment Variables

### Frontend (.env)
```env
VITE_API_URL=http://localhost:8080/api
```

### Backend (application.properties or environment)
```properties
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/taskflow
jwt.secret=your-secret-key-here-at-least-256-bits
allowed.origins=http://localhost:5173,https://your-frontend-domain.com
```

## Deployment Checklist

- [ ] MongoDB database created and accessible
- [ ] Backend deployed with correct environment variables
- [ ] Frontend deployed with VITE_API_URL pointing to backend
- [ ] CORS configured (allowed.origins includes frontend URL)
- [ ] JWT secret is secure and random
- [ ] WebSocket connections working (test chat feature)

## Testing Deployments

1. **Backend Health Check**
   ```bash
   curl https://your-backend-url/api/auth/health
   ```

2. **Frontend**
   - Open frontend URL
   - Try to register/login
   - Test creating team, project, task
   - Test chat functionality

## Troubleshooting

### CORS Errors
- Ensure `allowed.origins` in backend includes your frontend URL
- Check that frontend is using correct `VITE_API_URL`

### WebSocket Connection Failed
- Ensure backend URL uses `https://` (not `http://`)
- Check that WebSocket endpoint is accessible: `wss://your-backend-url/ws`

### Database Connection Issues
- Verify MongoDB connection string
- Check MongoDB Atlas IP whitelist (add `0.0.0.0/0` for any IP)
- Ensure database user has correct permissions

## Support

For issues or questions, please check:
- [README.md](./README.md) - Project overview
- [API_REFERENCE.md](./API_REFERENCE.md) - API documentation
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
