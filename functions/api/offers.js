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
 const paths=["/chemlink/offers.json","/chemlinkSnapshot.json","/offers.json"];
 const settled=await Promise.allSettled(paths.map(async path=>{
  const r=await fetch(FIREBASE_ROOT+path,{cache:"no-store",headers:{"cache-control":"no-cache","pragma":"no-cache"}});
  if(!r.ok)throw new Error("cloud read failed: "+path);
  return {path,data:await r.json()};
 }));
 const results=settled.filter(x=>x.status==="fulfilled").map(x=>x.value);
 if(!results.length)throw new Error("cloud read failed for all known offer stores");
 const map=new Map();
 const key=o=>[String(o.owner||o.phone||""),String(o.id||""),String(o.createdAt||o.created_at||""),String(o.name||"")].join("|");
 const add=value=>{
  const arr=Array.isArray(value)?value:(value&&typeof value==="object"?Object.values(value):[]);
  for(const o of arr){if(!o||typeof o!=="object")continue;const k=key(o);if(k==="|||")continue;
   if(!map.has(k))map.set(k,o);
   else {const old=map.get(k);map.set(k,{...old,...o});}
  }
 };
 for(const result of results){
  if(result.path==="/chemlinkSnapshot.json"&&result.data&&typeof result.data==="object")add(result.data.offers);
  else add(result.data);
 }
 return [...map.values()];
}
function normalizedOfferStatus(o){
 const status=String(o.status||o.state||o.approvalStatus||"").trim().toUpperCase();
 if(["APPROVED","PUBLISHED","ACTIVE","LIVE","PUBLIC"].includes(status)||o.approved===true||o.isApproved===true||o.published===true||o.isPublished===true||!!(o.approvedAt&&(o.publishedOfficial||o.publishedMarket)))return "APPROVED";
 if(["REJECTED","NEEDS_CORRECTION","CORRECTION","CHANGES_REQUESTED"].includes(status))return "REJECTED";
 return "PENDING";
}
function publicOffer(o){const k=String(o.kind||o.type||"").toLowerCase();return {id:o.id,category:o.category||"",kind:(["buy","request"].includes(k)||String(o.description||"").includes("نوع آگهی: درخواست خرید"))?"buy":"sell",official:o.publishedOfficial||o.official||"",market:o.publishedMarket||o.market||"",publishedOfficial:o.publishedOfficial||"",publishedMarket:o.publishedMarket||"",name:o.name||"",quantity:o.quantity||o.amount||"",deliveryTime:o.time||"",place:o.place||"",photo:o.photo||"",status:normalizedOfferStatus(o),createdAt:o.createdAt||o.created_at||0,updatedAt:o.updatedAt||o.updated_at||0};}
function isPubliclyApproved(o){return normalizedOfferStatus(o)==="APPROVED";}
function privateOffer(o){return {...publicOffer(o),official:o.official||"",market:o.market||"",publishedOfficial:o.publishedOfficial||"",publishedMarket:o.publishedMarket||"",owner:o.owner||o.phone||"",phone:o.phone||"",supplier:o.supplier||"",description:o.description||"",reason:o.reason||"",marginType:o.marginType||"PERCENT",margin:Number(o.margin)||0};}
export async function onRequestGet({request,env}){
 try{
  const url=new URL(request.url),mine=url.searchParams.get("mine")==="1";
  const s=await getSession(request,env);
  if(!s||!["user","admin"].includes(s.purpose))return json({ok:false,error:"برای مشاهده آگهی‌ها و قیمت‌های بازار ابتدا وارد حساب ChemLink شوید."},401);
  const offers=await readOffers();
  if(mine){
   if(s.purpose!=="user")return json({ok:false,error:"برای دیدن آگهی‌های خود وارد حساب کاربری شوید."},403);
   return json({ok:true,offers:offers.filter(o=>o&&phoneDigits(o.owner||o.phone)===phoneDigits(s.phone)).map(privateOffer)});
  }
  return json({ok:true,offers:offers.filter(o=>o&&isPubliclyApproved(o)).map(o=>({...publicOffer(o),status:"APPROVED"})).sort((a,b)=>{const at=Number(a.createdAt)||0,bt=Number(b.createdAt)||0;return bt-at;})});
 }
 catch(e){console.error(e);return json({ok:false,error:"دریافت آگهی‌های فضای ابری انجام نشد."},503);}
}
export async function onRequestPost({request,env}){
 try{
  const s=await getSession(request,env);
  if(!s||s.purpose!=="user")return json({ok:false,error:"برای ثبت آگهی ابتدا وارد حساب شوید."},401);
  const body=await request.json().catch(()=>({}));
  if(body.action==="DELETE"){
   const id=String(body.id||"");if(!id)return json({ok:false,error:"شناسه آگهی معتبر نیست."},400);
   const offers=await readOffers();
   const index=offers.findIndex(o=>o&&String(o.id)===id&&phoneDigits(o.owner||o.phone)===phoneDigits(s.phone));
   if(index<0)return json({ok:false,error:"این آگهی برای حساب شما پیدا نشد."},404);
   offers.splice(index,1);
   const saved=await fetch(FIREBASE_ROOT+"/chemlink/offers.json",{method:"PUT",headers:{"content-type":"application/json"},body:JSON.stringify(offers)});
   if(!saved.ok)return json({ok:false,error:"حذف آگهی از فضای ابری انجام نشد."},503);
   return json({ok:true,deletedId:id});
  }
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
