# ClipForge AI + self-hosted Cobalt in ONE Render service
FROM node:24-bookworm AS cobalt-build

RUN apt-get update && apt-get install -y --no-install-recommends git python3 make g++ ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /cobalt
RUN git clone --depth 1 https://github.com/imputnet/cobalt.git .

RUN corepack enable
RUN pnpm install --prod --frozen-lockfile
RUN pnpm deploy --filter=@imput/cobalt-api --prod /prod/api

FROM node:24-bookworm

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
  ffmpeg python3 python3-pip ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# Cobalt API runtime
COPY --from=cobalt-build --chown=node:node /prod/api /cobalt
COPY --from=cobalt-build --chown=node:node /cobalt/.git /cobalt/.git

# ClipForge dependencies
COPY requirements.txt ./requirements.txt
RUN pip3 install --break-system-packages --no-cache-dir -r requirements.txt

COPY package.json ./package.json
RUN npm install --omit=dev

COPY . .
RUN mkdir -p work

ENV PORT=3000
ENV API_URL=http://127.0.0.1:9000/
ENV API_PORT=9000
ENV WHISPER_MODEL=tiny
ENV WHISPER_DEVICE=cpu
ENV WHISPER_COMPUTE=int8

EXPOSE 3000

CMD ["sh","-c","cd /cobalt && node src/cobalt & cd /app && node server/server.js"]