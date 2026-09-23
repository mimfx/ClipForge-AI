FROM node:20-bookworm
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
  ffmpeg python3 python3-pip git ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# Install yt-dlp's current YouTube Proof-of-Origin token provider.
# This lets yt-dlp obtain the PO tokens YouTube currently requires.
RUN git clone --depth 1 --branch 2.0.0 https://github.com/Brainicism/bgutil-ytdlp-pot-provider.git /root/bgutil-ytdlp-pot-provider \
  && cd /root/bgutil-ytdlp-pot-provider/server \
  && npm ci \
  && npx tsc

COPY requirements.txt ./requirements.txt
RUN pip3 install --break-system-packages --no-cache-dir -r requirements.txt

COPY package.json ./package.json
RUN npm install --omit=dev

COPY . .
RUN mkdir -p work

ENV PORT=3000
ENV WHISPER_MODEL=tiny
ENV WHISPER_DEVICE=cpu
ENV WHISPER_COMPUTE=int8

EXPOSE 3000
CMD ["npm","start"]