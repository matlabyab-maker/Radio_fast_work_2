import os, asyncio, uuid, tempfile, pathlib, signal, shutil
from fastapi import FastAPI, Header, HTTPException, Query
from fastapi.responses import StreamingResponse, FileResponse
app=FastAPI(title="Fast Radio Transcoder")
KEY=os.getenv("API_KEY",""); SEM=asyncio.Semaphore(int(os.getenv("MAX_CONCURRENT_TRANSCODES","2")))
RECORDS={}
def auth(k):
    if KEY and k!=KEY: raise HTTPException(401,"invalid api key")
def valid(u): return u.startswith("http://") or u.startswith("https://")
@app.get("/health")
def health(): return {"ok":True,"service":"fast-radio-transcoder","recording_jobs":len(RECORDS)}
@app.get("/formats")
def formats(): return {"codecs":{"amr-nb":[4,5,6,7,8,10,12],"opus":[16,24,32,48,64,96,128],"mp3":[16,24,32,48,64,96,128]}}
@app.get("/stream")
async def stream(url:str=Query(...),codec:str="amr-nb",bitrate:int=12,x_api_key:str|None=Header(default=None)):
    auth(x_api_key)
    if not valid(url): raise HTTPException(400,"source must be http/https")
    bitrate=max(4,min(128,bitrate));
    if codec=="amr-nb": args=["-c:a","libopencore_amrnb","-ar","8000","-ac","1","-b:a",f"{min(12,max(4,bitrate))}k","-f","amr","pipe:1"]; mime="audio/amr"
    elif codec=="opus": args=["-c:a","libopus","-ar","24000","-ac","1","-b:a",f"{bitrate}k","-f","ogg","pipe:1"]; mime="audio/ogg"
    else: args=["-c:a","libmp3lame","-ar","22050","-ac","1","-b:a",f"{bitrate}k","-f","mp3","pipe:1"]; mime="audio/mpeg"
    await SEM.acquire(); p=await asyncio.create_subprocess_exec("ffmpeg","-hide_banner","-loglevel","error","-reconnect","1","-reconnect_streamed","1","-reconnect_delay_max","5","-i",url,*args,stdout=asyncio.subprocess.PIPE,stderr=asyncio.subprocess.PIPE)
    async def gen():
      try:
       while True:
        b=await p.stdout.read(16384)
        if not b: break
        yield b
      finally:
       if p.returncode is None: p.kill()
       await p.wait(); SEM.release()
    return StreamingResponse(gen(),media_type=mime,headers={"X-Fast-Radio-Transcoded":"1"})
@app.post("/record/start")
async def record_start(url:str=Query(...),codec:str="amr-nb",bitrate:int=12,x_api_key:str|None=Header(default=None)):
    auth(x_api_key)
    if not valid(url): raise HTTPException(400,"source must be http/https")
    if codec!="amr-nb": raise HTTPException(400,"recording is AMR-NB in this version")
    rid=uuid.uuid4().hex; path=str(pathlib.Path(tempfile.gettempdir())/(rid+".amr"))
    args=["ffmpeg","-y","-hide_banner","-loglevel","error","-reconnect","1","-reconnect_streamed","1","-reconnect_delay_max","5","-i",url,"-c:a","libopencore_amrnb","-ar","8000","-ac","1","-b:a","12k","-f","amr",path]
    p=await asyncio.create_subprocess_exec(*args,stdout=asyncio.subprocess.DEVNULL,stderr=asyncio.subprocess.DEVNULL); RECORDS[rid]=(p,path); return {"id":rid,"codec":"amr-nb"}
@app.get("/record/stop")
async def record_stop(id:str=Query(...),x_api_key:str|None=Header(default=None)):
    auth(x_api_key)
    job=RECORDS.pop(id,None)
    if not job: raise HTTPException(404,"recording not found")
    p,path=job
    if p.returncode is None:
      try: p.terminate()
      except Exception: pass
    try: await asyncio.wait_for(p.wait(),timeout=5)
    except Exception:
      try: p.kill()
      except Exception: pass
      await p.wait()
    if not os.path.exists(path): raise HTTPException(500,"recording file missing")
    return FileResponse(path,media_type="audio/amr",filename="FastRadio_"+id+".amr",background=None)
