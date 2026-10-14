import { firebaseFetch } from "../../_lib/firebase.js";
const FIREBASE_ROOT="https://chemlink-8909b-default-rtdb.firebaseio.com";
function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","pragma":"no-cache"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
async function session(request,env){
 const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
 if(!token||!env.DB)return null;
 const db=env.DB.withSession("first-primary");
 const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
 if(!row||Date.now()>Number(row.expires_at)||row.purpose!=="admin"||row.phone!=="09357236476")return null;
 return db;
}
export async function onRequestGet({request,env}){
 try{
  const db=await session(request,env);
  if(!db)return json({ok:false,error:"برای پشتیبان‌گیری، ورود مدیریت لازم است."},401);
  const cloudResponse=await firebaseFetch(env,"/chemlink.json",{cache:"no-store"});
  if(!cloudResponse.ok)return json({ok:false,error:"خواندن اطلاعات فضای ابری ناموفق بود؛ فایل پشتیبان ناقص ساخته نشد."},503);
  const cloud=await cloudResponse.json();
  let profiles=[];
  try{const r=await db.prepare("SELECT phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at FROM user_profiles").all();profiles=r.results||[];}catch(e){console.error("backup D1 profiles",e);}
  let tableRows={};
  try{const r=await db.prepare("SELECT name FROM sqlite_master WHERE type='table'").all();tableRows={tables:(r.results||[]).map(x=>x.name)};}catch{}
  return json({ok:true,product:"ChemLink",backupVersion:1,createdAt:new Date().toISOString(),cloud:{chemlink:cloud||{}},database:{user_profiles:profiles,tables:tableRows.tables||[]},notice:"Keep this file private. It may contain personal and business contact information."});
 }catch(e){console.error("admin backup",e);return json({ok:false,error:"ساخت فایل پشتیبان انجام نشد."},500);}
}