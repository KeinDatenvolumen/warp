# [KI-Log Nr. KI-228] (7235e0b) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(mpa): implement basic multi-page application with auth, profile, and GraphQL integration (WARP-034)
# [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
FROM node:22.20.0-alpine AS build

WORKDIR /app

COPY mpa/package.json mpa/package-lock.json ./

RUN npm ci

COPY mpa/ ./

RUN npm run build

FROM node:22.20.0-alpine

WORKDIR /app

ENV NODE_ENV=production

COPY mpa/package.json mpa/package-lock.json ./

RUN npm ci --omit=dev

COPY --from=build /app/dist ./dist
COPY mpa/views ./views
COPY mpa/public ./public

USER node

CMD ["node", "dist/server.js"]
