# ---- Build stage: build React SPA ----
FROM node:24-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

# ---- Runtime stage: unprivileged nginx serving static + proxying /api ----
FROM nginxinc/nginx-unprivileged:1.28-alpine AS runtime

LABEL org.opencontainers.image.title="it-helpdesk-frontend" \
      org.opencontainers.image.description="IT Helpdesk single-page application"

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
