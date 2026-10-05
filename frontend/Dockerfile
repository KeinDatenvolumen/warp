FROM node:22.20.0-alpine AS build

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./

RUN npm ci

# Kopiert alle Quelldateien aus frontend/ direkt nach /app
COPY frontend/ ./

ARG VITE_BFF_BASE_URL
ENV VITE_BFF_BASE_URL=${VITE_BFF_BASE_URL}

RUN npm run build

FROM node:22.20.0-alpine

WORKDIR /app

ENV NODE_ENV=production

COPY frontend/server.mjs server.mjs
COPY --from=build /app/dist dist

USER node

CMD ["node", "server.mjs"]