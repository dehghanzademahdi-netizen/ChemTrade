function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
async function getSession(request,env){
  const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
  if(!token||!env.DB)return null;
  const db=env.DB.withSession("first-primary");
  const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
  if(!row||Date.now()>Number(row.expires_at))return null;
  return {db,phone:row.phone,purpose:row.purpose};
}
async function ensureOffersSchema(db){
  await db.prepare("CREATE TABLE IF NOT EXISTS offers(id TEXT PRIMARY KEY,owner_phone TEXT NOT NULL,kind TEXT NOT NULL,name TEXT NOT NULL,official_price TEXT NOT NULL DEFAULT '',market_price TEXT NOT NULL DEFAULT '',quantity TEXT NOT NULL DEFAULT '',place TEXT NOT NULL DEFAULT '',supplier TEXT NOT NULL DEFAULT '',contact_phone TEXT NOT NULL DEFAULT '',description TEXT NOT NULL DEFAULT '',photo TEXT NOT NULL DEFAULT '',status TEXT NOT NULL DEFAULT 'PENDING',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)").run();
  await db.prepare("CREATE INDEX IF NOT EXISTS idx_offers_status_created ON offers(status,created_at DESC)").run();
  await db.prepare("CREATE INDEX IF NOT EXISTS idx_offers_owner_created ON offers(owner_phone,created_at DESC)").run();
}
function publicOffer(o){return {id:o.id,kind:o.kind,name:o.name,official:o.official_price,market:o.market_price,quantity:o.quantity,place:o.place,supplier:o.supplier,phone:o.contact_phone,description:o.description,photo:o.photo,status:o.status,createdAt:o.created_at,updatedAt:o.updated_at};}
export async function onRequestGet({request,env}){
 try{
  if(!env.DB)return json({ok:false,error:"پایگاه داده آنلاین در دسترس نیست."},503);
  const db=env.DB.withSession("first-primary");await ensureOffersSchema(db);
  const url=new URL(request.url),mine=url.searchParams.get("mine")==="1";
  if(mine){
   const s=await getSession(request,env);if(!s||s.purpose!=="user")return json({ok:false,error:"برای دیدن آگهی‌های خود وارد حساب شوید."},401);
   const rows=await s.db.prepare("SELECT * FROM offers WHERE owner_phone=? ORDER BY created_at DESC LIMIT 100").bind(s.phone).all();
   return json({ok:true,offers:(rows.results||[]).map(publicOffer)});
  }
  const rows=await db.prepare("SELECT * FROM offers WHERE status='APPROVED' ORDER BY created_at DESC LIMIT 100").all();
  return json({ok:true,offers:(rows.results||[]).map(publicOffer)});
 }catch(e){console.error(e);return json({ok:false,error:"دریافت آگهی‌ها انجام نشد."},500);}
}
export async function onRequestPost({request,env}){
 try{
  const s=await getSession(request,env);
  if(!s||s.purpose!=="user")return json({ok:false,error:"برای ثبت آگهی ابتدا وارد حساب شوید."},401);
  const body=await request.json().catch(()=>({}));
  const kind=["sell","buy"].includes(body.kind)?body.kind:"sell";
  const name=String(body.name||"").trim().slice(0,180);
  const official=String(body.official||"").trim().slice(0,100);
  const market=String(body.market||"").trim().slice(0,100);
  const quantity=String(body.quantity||"").trim().slice(0,100);
  const place=String(body.place||"").trim().slice(0,120);
  const supplier=String(body.supplier||"").trim().slice(0,160);
  const phone=String(body.phone||s.phone).trim().slice(0,30);
  const description=String(body.description||"").trim().slice(0,2000);
  const photo=String(body.photo||"").trim().slice(0,1500);
  if(name.length<2||place.length<2||quantity.length<1)return json({ok:false,error:"نام ماده، مقدار و شهر را کامل کنید."},400);
  if(photo&&!/^https:\/\//i.test(photo))return json({ok:false,error:"لینک تصویر باید با https شروع شود."},400);
  await ensureOffersSchema(s.db);
  const id=crypto.randomUUID(),now=Date.now();
  await s.db.prepare("INSERT INTO offers(id,owner_phone,kind,name,official_price,market_price,quantity,place,supplier,contact_phone,description,photo,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
   .bind(id,s.phone,kind,name,official,market,quantity,place,supplier,phone,description,photo,"PENDING",now,now).run();
  return json({ok:true,offer:{id,kind,name,official,market,quantity,place,supplier,phone,description,photo,status:"PENDING",createdAt:now,updatedAt:now}},201);
 }catch(e){console.error(e);return json({ok:false,error:"ثبت آگهی در پایگاه داده آنلاین انجام نشد."},500);}
}
