function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
const FIREBASE_ROOT="https://chemlink-8909b-default-rtdb.firebaseio.com";
async function firebase(path,method="GET",body=null){
  const response=await fetch(FIREBASE_ROOT+"/"+path+".json",{method,headers:{"content-type":"application/json"},body:body===null?undefined:JSON.stringify(body)});
  if(!response.ok)throw new Error("Firebase mirror failed");
  return response.json();
}
async function mirrorProfile(p){
  if(!p)return;
  const item={phone:p.phone,type:p.role==="supplier"?"Supplier":"Consumer",role:p.role,name:p.full_name,fullName:p.full_name,company:p.company_name,companyName:p.company_name,city:p.city,businessType:p.business_type,neededMaterials:p.needed_materials||"",address:p.address||"",landline:p.landline||"",updatedAt:p.updated_at,createdAt:p.created_at};
  const key=encodeURIComponent(String(p.phone).replace(/\D/g,""));
  const old=await firebase("chemlink/usersByPhone/"+key).catch(()=>null);
  const merged={...(old&&typeof old==="object"?old:{}),...item};
  await firebase("chemlink/usersByPhone/"+key,"PUT",merged);
  const users=await firebase("chemlink/users").catch(()=>[]);
  const arr=Array.isArray(users)?users:[];
  const index=arr.findIndex(x=>x&&String(x.phone||"").replace(/\D/g,"")===key);
  if(index>=0)arr[index]={...arr[index],...merged};else arr.push(merged);
  await firebase("chemlink/users","PUT",arr);
}
async function importFirebaseProfile(db,phone){
  const key=encodeURIComponent(String(phone).replace(/\D/g,""));
  let old=await firebase("chemlink/usersByPhone/"+key).catch(()=>null);
  if(!old||typeof old!=="object"){
    const users=await firebase("chemlink/users").catch(()=>[]);
    old=(Array.isArray(users)?users:[]).find(x=>x&&String(x.phone||"").replace(/\D/g,"")===key)||null;
  }
  if(!old)return null;
  const fullName=String(old.fullName||old.name||"").trim();
  const companyName=String(old.companyName||old.company||"").trim();
  const role=(old.role==="supplier"||String(old.type||"").toLowerCase().includes("supplier"))?"supplier":"buyer";
  const city=String(old.city||"").trim();
  const businessType=String(old.businessType||old.business_type||"فعالیت بازرگانی").trim();
  if(fullName.length<2||companyName.length<2||city.length<2)return null;
  const now=Date.now();
  await db.prepare("INSERT INTO user_profiles(phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(phone) DO UPDATE SET full_name=excluded.full_name,company_name=excluded.company_name,role=excluded.role,city=excluded.city,business_type=excluded.business_type,needed_materials=excluded.needed_materials,address=excluded.address,landline=excluded.landline,updated_at=excluded.updated_at")
   .bind(phone,fullName,companyName,role,city,businessType,String(old.neededMaterials||old.needed_materials||""),String(old.address||""),String(old.landline||""),Number(old.createdAt||now),now).run();
  return db.prepare("SELECT phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at FROM user_profiles WHERE phone=?").bind(phone).first();
}
function complete(p){return Boolean(p&&p.full_name&&p.company_name&&["buyer","supplier"].includes(p.role)&&p.city&&p.business_type);}
async function ensureProfileSchema(db){
  await db.prepare("CREATE TABLE IF NOT EXISTS user_profiles(phone TEXT PRIMARY KEY,full_name TEXT NOT NULL,company_name TEXT NOT NULL,role TEXT NOT NULL,city TEXT NOT NULL,business_type TEXT NOT NULL,needed_materials TEXT NOT NULL DEFAULT '',address TEXT NOT NULL DEFAULT '',landline TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)").run();
  const info=await db.prepare("PRAGMA table_info(user_profiles)").all();
  const cols=new Set((info.results||[]).map(x=>x.name));
  if(!cols.has("address"))await db.prepare("ALTER TABLE user_profiles ADD COLUMN address TEXT NOT NULL DEFAULT ''").run();
  if(!cols.has("landline"))await db.prepare("ALTER TABLE user_profiles ADD COLUMN landline TEXT NOT NULL DEFAULT ''").run();
}
async function getSession(request,env){
  const auth=request.headers.get("authorization")||"";
  const token=auth.startsWith("Bearer ")?auth.slice(7):"";
  if(!token||!env.DB)return null;
  const db=env.DB.withSession("first-primary");
  const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
  if(!row||Date.now()>Number(row.expires_at))return null;
  return {db,phone:row.phone,purpose:row.purpose};
}
function publicProfile(p){return p?{phone:p.phone,fullName:p.full_name,companyName:p.company_name,role:p.role,city:p.city,businessType:p.business_type,neededMaterials:p.needed_materials||"",address:p.address||"",landline:p.landline||"",createdAt:p.created_at,updatedAt:p.updated_at}:null;}
export async function onRequestGet({request,env}){
  try{
    const session=await getSession(request,env);
    if(!session)return json({ok:false,error:"نشست ورود معتبر نیست. دوباره وارد شوید."},401);
    if(session.purpose!=="user")return json({ok:false,error:"این نشست برای حساب کاربری نیست."},403);
    await ensureProfileSchema(session.db);
    let p=await session.db.prepare("SELECT phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at FROM user_profiles WHERE phone=?").bind(session.phone).first();
    if(!complete(p))p=await importFirebaseProfile(session.db,session.phone)||p;
    if(complete(p))await mirrorProfile(p).catch(e=>console.error("Firebase profile mirror",e));
    return json({ok:true,registered:complete(p),profile:publicProfile(p)});
  }catch(e){console.error(e);return json({ok:false,error:"دریافت اطلاعات حساب انجام نشد."},500);}
}
export async function onRequestPost({request,env}){
  try{
    const session=await getSession(request,env);
    if(!session)return json({ok:false,error:"نشست ورود معتبر نیست. دوباره وارد شوید."},401);
    if(session.purpose!=="user")return json({ok:false,error:"این نشست برای حساب کاربری نیست."},403);
    const body=await request.json().catch(()=>({}));
    const fullName=String(body.fullName||"").trim().slice(0,120);
    const companyName=String(body.companyName||"").trim().slice(0,180);
    const role=String(body.role||"");
    const city=String(body.city||"").trim().slice(0,100);
    const businessType=String(body.businessType||"").trim().slice(0,160);
    const neededMaterials=String(body.neededMaterials||"").trim().slice(0,1000);
    const address=String(body.address||"").trim().slice(0,400);
    const landline=String(body.landline||"").trim().slice(0,30);
    if(fullName.length<2||companyName.length<2||!["buyer","supplier"].includes(role)||city.length<2||businessType.length<2)
      return json({ok:false,error:"نام و نام خانوادگی، نام شرکت، نوع حساب، شهر و زمینه فعالیت را کامل کنید."},400);
    const db=session.db,now=Date.now();
    await ensureProfileSchema(db);
    await db.prepare("INSERT INTO user_profiles(phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(phone) DO UPDATE SET full_name=excluded.full_name,company_name=excluded.company_name,role=excluded.role,city=excluded.city,business_type=excluded.business_type,needed_materials=excluded.needed_materials,address=excluded.address,landline=excluded.landline,updated_at=excluded.updated_at")
      .bind(session.phone,fullName,companyName,role,city,businessType,neededMaterials,address,landline,now,now).run();
    const p=await db.prepare("SELECT phone,full_name,company_name,role,city,business_type,needed_materials,address,landline,created_at,updated_at FROM user_profiles WHERE phone=?").bind(session.phone).first();
    await mirrorProfile(p).catch(e=>console.error("Firebase profile mirror",e));
    return json({ok:true,registered:true,profile:publicProfile(p)});
  }catch(e){console.error(e);return json({ok:false,error:"ذخیره اطلاعات ثبت‌نام انجام نشد."},500);}
}
