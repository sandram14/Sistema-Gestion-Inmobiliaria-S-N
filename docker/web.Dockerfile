# =====================================================================
# Web: sitio público y panel administrativo servidos por nginx
# =====================================================================
# Etapas 1 y 2 compilan cada aplicación con Node (npm ci + npm run build);
# etapa 3 copia los resultados a nginx, que además reenvía /api y /uploads al backend.
# Se usa Node sobre Debian (bookworm) porque Tailwind 4 depende de binarios nativos.
# Construir (desde la raíz del repositorio): docker compose -f docker/docker-compose.yml build web
# =====================================================================

# ---- Etapa 1: sitio público ----
FROM node:22-bookworm-slim AS sitio
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---- Etapa 2: panel administrativo ----
FROM node:22-bookworm-slim AS panel
WORKDIR /app
COPY admin/package.json admin/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY admin/ ./
RUN npm run build

# ---- Etapa 3: nginx ----
FROM nginx:1.27-alpine
RUN rm -f /etc/nginx/conf.d/default.conf
COPY docker/nginx/sn-inmobiliaria.conf /etc/nginx/conf.d/sn-inmobiliaria.conf
COPY docker/nginx/seguridad.conf /etc/nginx/snippets/seguridad.conf
COPY --from=sitio /app/dist /usr/share/nginx/sitio
COPY --from=panel /app/dist /usr/share/nginx/admin
EXPOSE 80
