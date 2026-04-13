FROM node:lts-alpine AS builder

RUN corepack enable && corepack prepare pnpm@latest --activate

WORKDIR /app
RUN apk add --no-cache git && \
    git clone --depth 1 https://github.com/SAP/e-mobility-charging-stations-simulator.git . && \
    pnpm install --ignore-scripts --frozen-lockfile && \
    pnpm build

FROM node:lts-alpine

WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist

CMD ["node", "dist/start.js"]
