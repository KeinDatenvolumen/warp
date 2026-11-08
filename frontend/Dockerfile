# [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
# [KI-Log Nr. KI-053] (5c398f2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: update map style, api key name and deplyoment (WARP-009)
# [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
FROM node:22.20.0-alpine AS build

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./

RUN npm ci

# Kopiert alle Quelldateien aus frontend/ direkt nach /app
COPY frontend/ ./

ARG VITE_BFF_BASE_URL
ARG VITE_MAPTILER_PUBLIC_API_KEY

ENV VITE_BFF_BASE_URL=${VITE_BFF_BASE_URL}
ENV VITE_MAPTILER_PUBLIC_API_KEY=${VITE_MAPTILER_PUBLIC_API_KEY}

RUN npm run build

FROM node:22.20.0-alpine

WORKDIR /app

ENV NODE_ENV=production

COPY frontend/server.mjs server.mjs
COPY --from=build /app/dist dist

USER node

CMD ["node", "server.mjs"]