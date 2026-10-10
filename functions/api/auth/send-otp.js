const SMS_IR_TEMPLATE_ID = 167254;
const OTP_TTL_MS = 5 * 60 * 1000;
const RESEND_COOLDOWN_MS = 60 * 1000;

function json(data, status=200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {"content-type":"application/json; charset=utf-8","cache-control":"no-store"}
  });
}
function normalizePhone(value) {
  const raw = String(value || "").replace(/[\s-]/g, "");
  if (/^09\d{9}$/.test(raw)) return raw;
  if (/^\+989\d{9}$/.test(raw)) return "0" + raw.slice(3);
  if (/^989\d{9}$/.test(raw)) return "0" + raw.slice(2);
  return null;
}
function bytesToHex(bytes) {
  return [...new Uint8Array(bytes)].map(b=>b.toString(16).padStart(2,"0")).join("");
}
async function sha256(value) {
  const data = new TextEncoder().encode(String(value));
  return bytesToHex(await crypto.subtle.digest("SHA-256", data));
}
function code() {
  const bytes = new Uint32Array(1);
  crypto.getRandomValues(bytes);
  return String(100000 + (bytes[0] % 900000));
}
async function ensureSchema(db) {
  await db.prepare(`CREATE TABLE IF NOT EXISTS otp_codes (
    phone TEXT NOT NULL,
    purpose TEXT NOT NULL,
    code_hash TEXT NOT NULL,
    sent_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    message_id TEXT,
    PRIMARY KEY (phone, purpose)
  )`).run();
}
export async function onRequestPost({request, env}) {
  try {
    if (!env.DB) return json({ok:false,error:"اتصال پایگاه داده ورود پیامکی (DB) در Worker فعال نیست."},503);
    if (!env.SMS_IR_API_KEY) return json({ok:false,error:"کلید SMS_IR_API_KEY در محیط Production همین Worker در دسترس نیست."},503);
    const body = await request.json().catch(()=>({}));
    const phone = normalizePhone(body.phone);
    const purpose = body.purpose === "admin" ? "admin" : "user";
    if (!phone) return json({ok:false,error:"شماره موبایل معتبر نیست."},400);
    await ensureSchema(env.DB);
    const old = await env.DB.prepare("SELECT sent_at FROM otp_codes WHERE phone=? AND purpose=?").bind(phone,purpose).first();
    const now = Date.now();
    if (old?.sent_at && now - Number(old.sent_at) < RESEND_COOLDOWN_MS) {
      const wait = Math.ceil((RESEND_COOLDOWN_MS-(now-Number(old.sent_at)))/1000);
      return json({ok:false,error:`لطفاً ${wait} ثانیه دیگر دوباره درخواست کنید.`},429);
    }
    const otp = code();
    const response = await fetch("https://api.sms.ir/v1/send/verify", {
      method:"POST",
      headers:{"Content-Type":"application/json","x-api-key":env.SMS_IR_API_KEY},
      body:JSON.stringify({mobile:phone,templateId:SMS_IR_TEMPLATE_ID,parameters:[{name:"Code",value:otp}]})
    });
    const result = await response.json().catch(()=>null);
    if (!response.ok || !result || Number(result.status)!==1) {
      console.error("SMS.ir rejected OTP", {httpStatus:response.status,status:result?.status,message:result?.message});
      return json({ok:false,error:"ارسال پیامک انجام نشد."},502);
    }
    await env.DB.prepare(`INSERT INTO otp_codes(phone,purpose,code_hash,sent_at,expires_at,attempts,message_id)
      VALUES(?,?,?,?,?,?,?)
      ON CONFLICT(phone,purpose) DO UPDATE SET code_hash=excluded.code_hash,sent_at=excluded.sent_at,expires_at=excluded.expires_at,attempts=0,message_id=excluded.message_id`)
      .bind(phone,purpose,await sha256(otp),now,now+OTP_TTL_MS,0,String(result?.data?.messageId||"")).run();
    return json({ok:true,message:"کد تأیید ارسال شد."});
  } catch (e) {
    console.error(e);
    return json({ok:false,error:"خطای داخلی سرویس پیامکی."},500);
  }
}
