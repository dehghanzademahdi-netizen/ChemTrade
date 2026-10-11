const FIREBASE_ROOT="https://chemlink-8909b-default-rtdb.firebaseio.com";
function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
function phoneDigits(value){return String(value||"").replace(/[^0-9]/g,"");}
async function getSession(request,env){
 const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
 if(!token||!env.DB)return null;
 const db=env.DB.withSession("first-primary");
 const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
 if(!row||Date.now()>Number(row.expires_at))return null;
 return {db,phone:row.phone,purpose:row.purpose};
}
async function readOffers(){
 const r=await fetch(FIREBASE_ROOT+"/chemlink/offers.json",{cache:"no-store"});
 if(!r.ok)throw new Error("cloud read failed");
 const data=await r.json();
 return Array.isArray(data)?data:(data&&typeof data==="object"?Object.values(data):[]);
}
function publicOffer(o){const k=String(o.kind||o.type||"").toLowerCase();return {id:o.id,category:o.category||"سایر مواد",kind:(["buy","request"].includes(k)||String(o.description||"").includes("نوع آگهی: درخواست خرید"))?"buy":"sell",official:o.publishedOfficial||o.official||"",market:o.publishedMarket||o.market||"",publishedOfficial:o.publishedOfficial||"",publishedMarket:o.publishedMarket||"",name:o.name||"",quantity:o.quantity||o.amount||"",deliveryTime:o.time||"",place:o.place||"",supplier:o.supplier||"",phone:o.phone||"",description:o.description||"",photo:o.photo||"",status:String(o.status||"PENDING").toUpperCase(),reason:o.reason||"",createdAt:o.createdAt||o.created_at||0,updatedAt:o.updatedAt||o.updated_at||0};}
function privateOffer(o){return {...publicOffer(o),official:o.official||"",market:o.market||"",publishedOfficial:o.publishedOfficial||"",publishedMarket:o.publishedMarket||"",owner:o.owner||o.phone||"",marginType:o.marginType||"PERCENT",margin:Number(o.margin)||0};}
export async function onRequestGet({request,env}){
 try{
  const url=new URL(request.url),mine=url.searchParams.get("mine")==="1";
  const offers=await readOffers();
  if(mine){
   const s=await getSession(request,env);if(!s||s.purpose!=="user")return json({ok:false,error:"برای دیدن آگهی‌های خود وارد حساب شوید."},401);
   return json({ok:true,offers:offers.filter(o=>o&&phoneDigits(o.owner||o.phone)===phoneDigits(s.phone)).map(privateOffer)});
  }
  return json({ok:true,offers:offers.filter(o=>o&&String(o.status||"").toUpperCase()==="APPROVED").map(publicOffer).sort((a,b)=>Number(b.createdAt||0)-Number(a.createdAt||0)).slice(0,100)});
 }
 catch(e){console.error(e);return json({ok:false,error:"دریافت آگهی‌های فضای ابری انجام نشد."},503);}
}
export async function onRequestPost({request,env}){
 try{
  const s=await getSession(request,env);
  if(!s||s.purpose!=="user")return json({ok:false,error:"برای ثبت آگهی ابتدا وارد حساب شوید."},401);
  const body=await request.json().catch(()=>({}));
  const kind=["sell","buy"].includes(body.kind)?body.kind:"sell";
  const name=String(body.name||"").trim().slice(0,180),category=String(body.category||"سایر مواد").trim().slice(0,100),official=String(body.official||"").trim().slice(0,100),market=String(body.market||"").trim().slice(0,100);
  const quantity=String(body.quantity||"").trim().slice(0,100),place=String(body.place||"").trim().slice(0,120),time=String(body.time||"").trim().slice(0,120);
  const supplier=String(body.supplier||"").trim().slice(0,160),phone=String(body.phone||s.phone).trim().slice(0,30);
  const description=String(body.description||"").trim().slice(0,2000),photo=String(body.photo||"").trim().slice(0,1500);
  const reason=String(body.reason||"").trim().slice(0,1000);
  if(name.length<2||place.length<2||quantity.length<1)return json({ok:false,error:"نام ماده، مقدار و محل تحویل را کامل کنید."},400);
  if(photo&&!/^https:\/\//i.test(photo))return json({ok:false,error:"برای همگام‌سازی عکس در همه دستگاه‌ها، لینک عکس باید با https شروع شود."},400);
  const offers=await readOffers(),now=Date.now();
  const requestedId=String(body.id||"").trim();
  let existingIndex=-1;
  if(requestedId){
   existingIndex=offers.findIndex(o=>o&&String(o.id)===requestedId&&phoneDigits(o.owner||o.phone)===phoneDigits(s.phone));
   if(existingIndex<0)return json({ok:false,error:"این آگهی برای حساب شما پیدا نشد؛ فهرست را تازه‌سازی کنید."},404);
   const old=offers[existingIndex];
   const updated={...old,kind,name,category,official,market,quantity,time:time||quantity,place,supplier,phone,owner:s.phone,description:(kind==="buy"?"نوع آگهی: درخواست خرید\n":"نوع آگهی: عرضه برای فروش\n")+description,photo,status:"PENDING",reason,marginType:old.marginType||"PERCENT",margin:Number(old.margin)||0,publishedOfficial:"",publishedMarket:"",updatedAt:now};
   offers[existingIndex]=updated;
   const saved=await fetch(FIREBASE_ROOT+"/chemlink/offers.json",{method:"PUT",headers:{"content-type":"application/json"},body:JSON.stringify(offers)});
   if(!saved.ok)return json({ok:false,error:"ذخیره تغییرات آگهی در فضای ابری انجام نشد."},503);
   return json({ok:true,offer:privateOffer(updated)});
  }
  const maxId=offers.reduce((max,o)=>Math.max(max,Number(o?.id)||0),0);
  const offer={id:maxId+1,kind,name,category,official,market,quantity,time:time||quantity,place,supplier,phone,owner:s.phone,description:(kind==="buy"?"نوع آگهی: درخواست خرید\n":"نوع آگهی: عرضه برای فروش\n")+description,photo,status:"PENDING",reason,marginType:"PERCENT",margin:0,publishedOfficial:"",publishedMarket:"",createdAt:now,updatedAt:now};
  offers.push(offer);
  const saved=await fetch(FIREBASE_ROOT+"/chemlink/offers.json",{method:"PUT",headers:{"content-type":"application/json"},body:JSON.stringify(offers)});
  if(!saved.ok)return json({ok:false,error:"ذخیره آگهی در فضای ابری انجام نشد."},503);
  return json({ok:true,offer:privateOffer(offer)},201);
 }catch(e){console.error(e);return json({ok:false,error:"ثبت آگهی در فضای ابری انجام نشد."},503);}
}
