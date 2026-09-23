FROM node:22-bookworm

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg python3 python3-pip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Build the official bgutil PO-token HTTP provider.
# It runs locally beside ClipForge on port 4416.
RUN git clone --single-branch --branch 2.0.0 https://github.com/Brainicism/bgutil-ytdlp-pot-provider.git /opt/bgutil \
    && cd /opt/bgutil/server \
    && npm ci \
    && npx tsc

COPY requirements.txt ./requirements.txt
RUN python3 -m pip install --break-system-packages --no-cache-dir -U -r requirements.txt

COPY package.json ./package.json
RUN npm install --omit=dev

COPY . .
RUN mkdir -p work

ENV PORT=3000
ENV WHISPER_MODEL=tiny
ENV WHISPER_DEVICE=cpu
ENV WHISPER_COMPUTE=int8
ENV BGUTIL_PORT=4416

EXPOSE 3000

CMD ["sh","-c","node /opt/bgutil/server/build/main.js --host 127.0.0.1 --port 4416 & exec node server/server.js"]