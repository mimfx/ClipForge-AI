import express from "express";
import {execFile} from "node:child_process";
import {promisify} from "node:util";
import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";

const exec = promisify(execFile);
const app = express();
const jobs = new Map();
const PORT = Number(process.env.PORT || 3000);
const ROOT = process.cwd();
const WORK = path.join(ROOT, "work");

app.use(express.json({limit:"30kb"}));
app.use((req,res,next)=>{
  res.setHeader("Access-Control-Allow-Origin", process.env.ALLOW_ORIGIN || "*");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  if(req.method === "OPTIONS") return res.sendStatus(204);
  next();
});
app.use(express.static(path.join(ROOT,"public")));

const okurl = u => {
  try { const x = new URL(u); return ["http:","https:"].includes(x.protocol); }
  catch { return false; }
};
const run = (cmd,args,opts={}) => exec(cmd,args,{maxBuffer:30*1024*1024,...opts});
const ytArgs = args => ["--js-runtimes","deno","--remote-components","ejs:github",...args];
const ytExtractArgs = [];
const COBALT_URL = String(process.env.COBALT_URL || "http://127.0.0.1:9000").replace(/\/$/,"");


async function job(j){
  const dir=path.join(WORK,j.id);
  await fs.mkdir(dir,{recursive:true});
  try{
    j.stage="Inspecting"; j.message="Preparing the source…"; j.progress=8;
    const target=path.join(dir,"source.mp4");
    let meta=null;

    const info=await run("yt-dlp",ytArgs(["--no-playlist","--dump-single-json",j.source]),{timeout:180000});
    try{meta=JSON.parse(info.stdout)}catch{}

    j.stage="Downloading"; j.message="Downloading the source video…"; j.progress=18;
    await run("yt-dlp",ytArgs(["--no-playlist","-f","bv*[height<=1080]+ba/b[height<=1080]/b","--merge-output-format","mp4","--retries","5","--fragment-retries","5","-o",target,j.source]),{timeout:40*60*1000});

    const files=await fs.readdir(dir);
    const src=files.find(x=>x.startsWith("source.") || x==="source.mp4");
    if(!src) throw Error("The video could not be downloaded. Check that the URL is public and accessible.");

    let duration=Number(meta?.duration)||0;
    if(!duration){
      try{
        const probe=await run("ffprobe",["-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",path.join(dir,src)],{timeout:60000});
        duration=Number(probe.stdout)||60;
      }catch{ duration=60; }
    }
    j.stage="Transcribing"; j.message="Transcribing the video for highlight detection…"; j.progress=38;
    let transcript=null;
    try{
      const tr=await run("python3",["server/transcribe.py",path.join(dir,src),dir],{timeout:30*60*1000});
      transcript=JSON.parse(tr.stdout);
    }catch(err){ j.note="Whisper transcription was unavailable, so baseline clip selection was used."; }

    j.stage="Selecting moments"; j.message=transcript?.segments?.length?"Scoring the strongest moments…":"Selecting usable moments…"; j.progress=52;
    const count=Math.min(5,Math.max(1,Number(j.count)||3));
    let starts=[];
    if(transcript?.selected?.length){
      starts=transcript.selected.slice(0,count).map(x=>Math.max(0,Number(x.start)-4));
    } else if(transcript?.segments?.length){
      const segs=transcript.segments.filter(s=>s.text?.trim());
      const stride=Math.max(1,Math.floor(segs.length/count));
      for(let i=0;i<count && i*stride<segs.length;i++) starts.push(Math.max(0,Number(segs[i*stride].start)-4));
    }
    if(!starts.length){
      for(let i=0;i<count;i++) starts.push(Math.max(0,Math.min(Math.max(0,duration-20),duration*(i+1)/(count+1)-10)));
    }
    const len=Math.min(50,Math.max(15,duration/6));
    const baseVf=j.format==="1:1"?"scale=1080:1080:force_original_aspect_ratio=increase,crop=1080:1080":j.format==="16:9"?"scale=1280:720:force_original_aspect_ratio=increase,crop=1280:720":"scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920";

    j.stage="Rendering"; j.message="Rendering clips and captions…"; j.progress=62; j.clips=[];
    const segments=transcript?.segments||[];
    for(let i=0;i<starts.length;i++){
      const start=starts[i];
      const actualLen=Math.min(len,Math.max(1,duration-start));
      const end=start+actualLen;
      const out=path.join(dir,`clip-${i+1}.mp4`);
      const args=["-y","-ss",String(start),"-i",path.join(dir,src),"-t",String(actualLen),"-vf",baseVf];
      if(j.captions && segments.length){
        const srt=path.join(dir,`clip-${i+1}.srt`);
        if(await writeSrtForClip(segments,start,end,srt)) args[args.indexOf("-vf")+1]=`${baseVf},subtitles='${safeFile(srt)}'`;
      }
      args.push("-c:v","libx264","-preset","veryfast","-crf","28","-c:a","aac","-b:a","128k","-movflags","+faststart",out);
      await run("ffmpeg",args,{timeout:15*60*1000});
      j.clips.push({title:transcript?.titles?.[i]||`Highlight ${i+1}`,start:`${Math.floor(start)}s`,end:`${Math.floor(end)}s`,format:j.format,score:transcript?.scores?.[i]||"AI PICK",video:`/media/${j.id}/clip-${i+1}.mp4`});
      j.progress=62+Math.round((i+1)/starts.length*33);
    }
    j.status="done"; j.stage="Complete"; j.message=`${j.clips.length} clips are ready.`; j.progress=100;
  }catch(e){
    j.status="error"; j.stage="Failed"; j.message=e?.stderr?.slice(-1200)||e?.message||"Processing failed.";
  }
}

app.get("/health",(req,res)=>res.json({ok:true,service:"ClipForge AI",time:new Date().toISOString()}));
app.get("/api/health",(req,res)=>res.json({ok:true}));
app.post("/api/clip",(req,res)=>{
  const source=String(req.body?.url||"").trim();
  if(!okurl(source)) return res.status(400).json({error:"Enter a valid http/https video URL."});
  const j={id:id(),source,count:req.body.count,format:req.body.format||"9:16",captions:Boolean(req.body.captions),status:"queued",stage:"Queued",message:"Waiting for the processing worker…",progress:3,clips:[]};
  jobs.set(j.id,j);
  job(j).catch(()=>{});
  res.status(202).json({id:j.id});
});
app.get("/api/jobs/:id",(req,res)=>{const j=jobs.get(req.params.id);if(!j)return res.status(404).json({error:"Job not found"});res.json(j)});
app.use("/media/:id",(req,res,next)=>jobs.has(req.params.id)?next():res.sendStatus(404));
app.use("/media",express.static(WORK));
app.get("/{*splat}",(req,res)=>res.sendFile(path.join(ROOT,"public","index.html")));

await fs.mkdir(WORK,{recursive:true});
app.listen(PORT,()=>console.log(`ClipForge AI listening on ${PORT}`));