function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","pragma":"no-cache"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
async function adminSession(request,env){
 const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
 if(!token||!env.DB)return null;
 const db=env.DB.withSession("first-primary");
 const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
 if(!row||Date.now()>Number(row.expires_at)||row.purpose!=="admin"||row.phone!=="09357236476")return null;
 return db;
}
async function ensureProfileSchema(db){
 await db.prepare("CREATE TABLE IF NOT EXISTS user_profiles(phone TEXT PRIMARY KEY,full_name TEXT NOT NULL,company_name TEXT NOT NULL,role TEXT NOT NULL,city TEXT NOT NULL,business_type TEXT NOT NULL,needed_materials TEXT NOT NULL DEFAULT '',address TEXT NOT NULL DEFAULT '',landline TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)").run();
 const info=await db.prepare("PRAGMA table_info(user_profiles)").all();
 const cols=new Set((info.results||[]).map(x=>x.name));
 if(!cols.has("address"))await db.prepare("ALTER TABLE user_profiles ADD COLUMN address TEXT NOT NULL DEFAULT ''").run();
 if(!cols.has("landline"))await db.prepare("ALTER TABLE user_profiles ADD COLUMN landline TEXT NOT NULL DEFAULT ''").run();
}
function mapProfile(p){return {phone:p.phone,fullName:p.full_name||"",companyName:p.company_name||"",role:p.role||"",city:p.city||"",businessType:p.business_type||"",neededMaterials:p.needed_materials||"",address:p.address||"",landline:p.landline||"",createdAt:p.created_at||0,updatedAt:p.updated_at||0,profileComplete:Boolean(p.full_name&&p.company_name&&p.role&&p.city&&p.business_type)};}
export async function onRequestGet({request,env}){
 try{
  const db=await adminSession(request,env);if(!db)return json({ok:false,error:"ورود مدیریت معتبر نیست؛ دوباره وارد شوید."},401);
  await ensureProfileSchema(db);
  const profiles=await db.prepare("SELECT phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at FROM user_profiles ORDER BY created_at DESC").all();
  const sessions=await db.prepare("SELECT phone,MAX(created_at) AS last_login,MAX(expires_at) AS session_expires FROM sessions WHERE purpose='user' GROUP BY phone").all();
  const byPhone=new Map((profiles.results||[]).map(p=>[p.phone,mapProfile(p)]));
  for(const s of sessions.results||[]){
   const p=byPhone.get(s.phone)||{phone:s.phone,fullName:"",companyName:"",role:"",city:"",businessType:"",neededMaterials:"",address:"",landline:"",createdAt:0,updatedAt:0,profileComplete:false};
   p.lastLogin=s.last_login||0;p.sessionExpires=s.session_expires||0;byPhone.set(s.phone,p);
  }
  return json({ok:true,users:[...byPhone.values()].sort((a,b)=>Number(b.updatedAt||b.createdAt||b.lastLogin||0)-Number(a.updatedAt||a.createdAt||a.lastLogin||0))});
 }catch(e){console.error("admin users GET",e);return json({ok:false,error:"دریافت فهرست کاربران انجام نشد."},500);}
}
export async function onRequestPost({request,env}){
 try{
  const db=await adminSession(request,env);if(!db)return json({ok:false,error:"ورود مدیریت معتبر نیست؛ دوباره وارد شوید."},401);
  const body=await request.json().catch(()=>({}));
  const phone=String(body.phone||"").replace(/[^0-9]/g,"");
  if(!/^09\d{9}$/.test(phone))return json({ok:false,error:"شماره موبایل کاربر معتبر نیست."},400);
  const fullName=String(body.fullName||"").trim().slice(0,120),companyName=String(body.companyName||"").trim().slice(0,180);
  const role=["buyer","supplier"].includes(body.role)?body.role:"buyer",city=String(body.city||"").trim().slice(0,100);
  const businessType=String(body.businessType||"").trim().slice(0,160),neededMaterials=String(body.neededMaterials||"").trim().slice(0,1000);
  const address=String(body.address||"").trim().slice(0,400),landline=String(body.landline||"").trim().slice(0,30);
  if(fullName.length<2||companyName.length<2||city.length<2||businessType.length<2)return json({ok:false,error:"نام، شرکت، شهر و زمینه فعالیت باید کامل باشند."},400);
  await ensureProfileSchema(db);const now=Date.now();
  await db.prepare("INSERT INTO user_profiles(phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(phone) DO UPDATE SET full_name=excluded.full_name,company_name=excluded.company_name,role=excluded.role,city=excluded.city,business_type=excluded.business_type,needed_materials=excluded.needed_materials,address=excluded.address,landline=excluded.landline,updated_at=excluded.updated_at")
   .bind(phone,fullName,companyName,role,city,businessType,neededMaterials,address,landline,now,now).run();
  return json({ok:true,message:"اطلاعات کاربر ذخیره شد."});
 }catch(e){console.error("admin users POST",e);return json({ok:false,error:"ذخیره اطلاعات کاربر انجام نشد."},500);}
}
