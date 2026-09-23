FROM node:20-bookworm

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg python3 python3-pip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Build the official bgutil PO-token generation script.
RUN git clone --depth 1 --branch 1.3.2 https://github.com/Brainicism/bgutil-ytdlp-pot-provider.git /opt/bgutil \
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
ENV BGUTIL_SCRIPT=/opt/bgutil/server/build/generate_once.js

EXPOSE 3000

CMD ["npm","start"]