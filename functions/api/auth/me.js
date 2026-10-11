function json(data,status=200){return new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});}
async function sha256(value){const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(String(value)));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");}
export async function onRequestGet({request,env}){
 try{
  if(!env.DB)return json({ok:false},503);
  const auth=request.headers.get("authorization")||"",token=auth.startsWith("Bearer ")?auth.slice(7):"";
  if(!token)return json({ok:false},401);
  const db=env.DB.withSession("first-primary");
  const row=await db.prepare("SELECT phone,purpose,expires_at FROM sessions WHERE token_hash=?").bind(await sha256(token)).first();
  if(!row||Date.now()>Number(row.expires_at)){if(row)await db.prepare("DELETE FROM sessions WHERE token_hash=?").bind(await sha256(token)).run();return json({ok:false},401);}
  return json({ok:true,phone:row.phone,purpose:row.purpose,expiresAt:row.expires_at});
 }catch(e){console.error(e);return json({ok:false},500);}
}
