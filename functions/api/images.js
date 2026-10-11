const FIREBASE_ROOT="https://chemlink-8909b-default-rtdb.firebaseio.com";
function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
async function getSession(request,env){
 const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
 if(!token||!env.DB)return null;
 const db=env.DB.withSession("first-primary");
 const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
 if(!row||Date.now()>Number(row.expires_at)||row.purpose!=="user")return null;
 return row;
}
export async function onRequestPost({request,env}){
 try{
  const session=await getSession(request,env);
  if(!session)return json({ok:false,error:"برای بارگذاری تصویر ابتدا وارد حساب کاربری شوید."},401);
  const body=await request.json().catch(()=>({}));
  const type=String(body.type||"");
  const data=String(body.data||"");
  if(!["image/jpeg","image/png","image/webp"].includes(type))return json({ok:false,error:"فرمت تصویر باید JPG، PNG یا WebP باشد."},415);
  if(!data||data.length>520000||!/^[A-Za-z0-9+/]+={0,2}$/.test(data))return json({ok:false,error:"حجم تصویر پس از فشرده‌سازی باید کمتر از ۳۸۰ کیلوبایت باشد."},413);
  const id=crypto.randomUUID().replace(/-/g,"");
  const record={type,data,owner:session.phone,createdAt:Date.now()};
  const saved=await fetch(FIREBASE_ROOT+"/chemlink/images/"+id+".json",{method:"PUT",headers:{"content-type":"application/json"},body:JSON.stringify(record)});
  if(!saved.ok)return json({ok:false,error:"ذخیره تصویر در فضای ابری انجام نشد."},503);
  return json({ok:true,id,url:"https://chemlink.shop/api/images/"+id},201);
 }catch(e){console.error("image upload failed",e);return json({ok:false,error:"بارگذاری تصویر انجام نشد؛ اتصال اینترنت را بررسی کنید و دوباره تلاش کنید."},503);}
}
export async function onRequestGet({request,env}){
 try{
  const id=new URL(request.url).pathname.split("/").filter(Boolean).pop()||"";
  if(!/^[a-f0-9]{32}$/i.test(id))return json({ok:false,error:"شناسه تصویر معتبر نیست."},400);
  const response=await fetch(FIREBASE_ROOT+"/chemlink/images/"+id+".json",{cache:"no-store"});
  if(!response.ok)return json({ok:false,error:"دریافت تصویر انجام نشد."},503);
  const record=await response.json();
  if(!record||!record.data||!record.type)return json({ok:false,error:"تصویر پیدا نشد."},404);
  const binary=atob(record.data),bytes=new Uint8Array(binary.length);
  for(let i=0;i<binary.length;i++)bytes[i]=binary.charCodeAt(i);
  return new Response(bytes,{status:200,headers:{"content-type":record.type,"cache-control":"public, max-age=31536000, immutable","x-content-type-options":"nosniff"}});
 }catch(e){console.error("image read failed",e);return json({ok:false,error:"خواندن تصویر انجام نشد."},503);}
}
