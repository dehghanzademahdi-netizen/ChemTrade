const FIREBASE_ROOT="https://chemlink-8909b-default-rtdb.firebaseio.com";
function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","pragma":"no-cache"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
async function adminSession(request,env){
 const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
 if(!token||!env.DB)return null;
 const db=env.DB.withSession("first-primary");
 const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
 if(!row||Date.now()>Number(row.expires_at)||row.purpose!=="admin"||row.phone!=="09357236476")return null;
 return row;
}
async function readOffers(){
 const r=await fetch(FIREBASE_ROOT+"/chemlink/offers.json",{cache:"no-store"});
 if(!r.ok)throw new Error("cloud read failed");
 const data=await r.json();
 return Array.isArray(data)?data:(data&&typeof data==="object"?Object.values(data):[]);
}
async function writeOffers(offers){
 const r=await fetch(FIREBASE_ROOT+"/chemlink/offers.json",{method:"PUT",headers:{"content-type":"application/json"},body:JSON.stringify(offers)});
 if(!r.ok)throw new Error("cloud write failed");
}
function money(value){
 const n=Number(value);
 return Number.isFinite(n)?new Intl.NumberFormat("fa-IR",{maximumFractionDigits:0}).format(Math.round(n))+" تومان":"";
}
function amount(value){
 const digits=String(value??"").replace(/[۰-۹]/g,c=>String("۰۱۲۳۴۵۶۷۸۹".indexOf(c))).replace(/[٠-٩]/g,c=>String("٠١٢٣٤٥٦٧٨٩".indexOf(c))).replace(/[^0-9]/g,"");
 const n=Number(digits);return digits&&Number.isFinite(n)?n:null;
}
function finalPrice(base,type,margin){
 const n=amount(base);if(n===null)return "";
 const m=Number(margin)||0;
 return money(type==="TOMAN"?n+m:n+n*m/100);
}
export async function onRequestGet({request,env}){
 try{
  const s=await adminSession(request,env);if(!s)return json({ok:false,error:"ورود مدیریت معتبر نیست؛ دوباره وارد شوید."},401);
  const offers=await readOffers();
  return json({ok:true,offers:offers.filter(Boolean).map(o=>({...o,status:String(o.status||"PENDING").toUpperCase()})).sort((a,b)=>Number(b.createdAt||0)-Number(a.createdAt||0))});
 }catch(e){console.error("admin offers GET",e);return json({ok:false,error:"خواندن آگهی‌های مدیریت انجام نشد."},503);}
}
export async function onRequestPost({request,env}){
 try{
  const s=await adminSession(request,env);if(!s)return json({ok:false,error:"ورود مدیریت معتبر نیست؛ دوباره وارد شوید."},401);
  const body=await request.json().catch(()=>({}));
  const id=String(body.id??"");const status=String(body.status||"").toUpperCase();
  if(!id||!["APPROVED","REJECTED","PENDING"].includes(status))return json({ok:false,error:"شناسه یا وضعیت آگهی معتبر نیست."},400);
  const offers=await readOffers();const index=offers.findIndex(o=>o&&String(o.id)===id);
  if(index<0)return json({ok:false,error:"آگهی در فضای ابری پیدا نشد."},404);
  const old=offers[index];const marginType=body.marginType==="TOMAN"?"TOMAN":"PERCENT";
  const margin=Math.max(0,Math.min(100000000000,Number(body.margin)||0));
  const reason=String(body.reason||"").trim().slice(0,1000);
  const updated={...old,status,reason,marginType,margin,updatedAt:Date.now()};
  if(status==="APPROVED"){
   updated.publishedOfficial=finalPrice(old.official,marginType,margin)||String(old.official||"");
   updated.publishedMarket=finalPrice(old.market,marginType,margin)||String(old.market||"");
   updated.approvedAt=Date.now();updated.approvedBy=s.phone;
  }else if(status==="REJECTED"){
   updated.publishedOfficial="";updated.publishedMarket="";
  }
  offers[index]=updated;await writeOffers(offers);
  return json({ok:true,offer:updated});
 }catch(e){console.error("admin offers POST",e);return json({ok:false,error:"ذخیره تغییرات آگهی انجام نشد."},503);}
}
