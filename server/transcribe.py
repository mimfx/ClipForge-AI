import sys,json,os
try:
 from faster_whisper import WhisperModel
except Exception as e:
 raise SystemExit(2)
video=sys.argv[1]; outdir=sys.argv[2]
model=WhisperModel(os.environ.get("WHISPER_MODEL","small"),device=os.environ.get("WHISPER_DEVICE","cpu"),compute_type=os.environ.get("WHISPER_COMPUTE","int8"))
segments,_=model.transcribe(video,beam_size=3,vad_filter=True)
segs=[{"start":float(s.start),"end":float(s.end),"text":s.text.strip()} for s in segments if s.text.strip()]
terms=("but","because","secret","mistake","never","always","why","how","best","worst","crazy","important","actually","money","learn")
scores=[]
for s in segs:
 t=s["text"].lower();scores.append(sum(1 for x in terms if x in t)+min(len(t)/120,1))
top=sorted(range(len(segs)),key=lambda i:scores[i],reverse=True)[:8]
top.sort(key=lambda i:segs[i]["start"])
selected=top[:5]
titles=[segs[i]["text"][:70] for i in selected]
vals=[round(scores[i],1) for i in selected]
selected_data=[{"start":segs[i]["start"],"end":segs[i]["end"],"text":segs[i]["text"]} for i in selected]\nprint(json.dumps({"segments":segs,"selected":selected_data,"titles":titles,"scores":vals}))
