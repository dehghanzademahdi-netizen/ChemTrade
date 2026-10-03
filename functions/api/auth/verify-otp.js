const SESSION_TTL_MS=2592000000;
function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});}
function normalizePhone(value){const raw=String(value||"").replace(/[\s-]/g,"");if(/^09\d{9}$/.test(raw))return raw;if(/^\+989\d{9}$/.test(raw))return "0"+raw.slice(3);if(/^989\d{9}$/.test(raw))return "0"+raw.slice(2);return null;}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
function makeToken(){const b=new Uint8Array(32);crypto.getRandomValues(b);return [...b].map(x=>x.toString(16).padStart(2,"0")).join("");}
async function schema(db){await db.prepare("CREATE TABLE IF NOT EXISTS otp_codes(phone TEXT NOT NULL,purpose TEXT NOT NULL,code_hash TEXT NOT NULL,sent_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,message_id TEXT,PRIMARY KEY(phone,purpose))").run();await db.prepare("CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,phone TEXT NOT NULL,purpose TEXT NOT NULL,created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL)").run();}
export async function onRequestPost({request,env}){
 try{
  if(!env.DB)return json({ok:false,error:"پایگاه داده سرویس فعال نیست."},503);
  const body=await request.json().catch(()=>({}));const phone=normalizePhone(body.phone);const code=String(body.code||"").trim();const purpose=body.purpose==="admin"?"admin":"user";
  if(!phone||!/^[0-9]{6}$/.test(code))return json({ok:false,error:"شماره موبایل یا کد تأیید معتبر نیست."},400);
  await schema(env.DB);
  const row=await env.DB.prepare("SELECT * FROM otp_codes WHERE phone=? AND purpose=?").bind(phone,purpose).first();
  if(!row)return json({ok:false,error:"کد تأیید پیدا نشد. دوباره درخواست کد کنید."},404);
  if(Date.now()>Number(row.expires_at)){await env.DB.prepare("DELETE FROM otp_codes WHERE phone=? AND purpose=?").bind(phone,purpose).run();return json({ok:false,error:"کد تأیید منقضی شده است."},410);}
  const attempts=Number(row.attempts||0);
  if(attempts>=5){await env.DB.prepare("DELETE FROM otp_codes WHERE phone=? AND purpose=?").bind(phone,purpose).run();return json({ok:false,error:"تعداد تلاش‌ها بیش از حد مجاز است. دوباره کد بگیرید."},429);}
  if(await sha256(code)!==String(row.code_hash)){await env.DB.prepare("UPDATE otp_codes SET attempts=attempts+1 WHERE phone=? AND purpose=?").bind(phone,purpose).run();return json({ok:false,error:"کد تأیید صحیح نیست."},400);}
  await env.DB.prepare("DELETE FROM otp_codes WHERE phone=? AND purpose=?").bind(phone,purpose).run();
  const rawToken=makeToken();const now=Date.now();
  await env.DB.prepare("INSERT INTO sessions(token_hash,phone,purpose,created_at,expires_at) VALUES(?,?,?,?,?)").bind(await sha256(rawToken),phone,purpose,now,now+SESSION_TTL_MS).run();
  return json({ok:true,token:rawToken,phone,expiresAt:now+SESSION_TTL_MS});
 }catch(e){console.error(e);return json({ok:false,error:"خطای داخلی هنگام تأیید کد."},500);}
}