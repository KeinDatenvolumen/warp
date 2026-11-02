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
