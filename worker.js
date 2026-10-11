import { onRequestPost as sendOtp } from "./functions/api/auth/send-otp.js";
import { onRequestPost as verifyOtp } from "./functions/api/auth/verify-otp.js";
import { onRequestGet as me } from "./functions/api/auth/me.js";
import { onRequestGet as getProfile, onRequestPost as saveProfile } from "./functions/api/auth/profile.js";
import { onRequestGet as getOffers, onRequestPost as createOffer } from "./functions/api/offers.js";
import { onRequestGet as adminBackup } from "./functions/api/admin/backup.js";
import { onRequestGet as getImage, onRequestPost as uploadImage } from "./functions/api/images.js";
import { onRequestGet as adminGetOffers, onRequestPost as adminUpdateOffer } from "./functions/api/admin/offers.js";

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
  });
}
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname === "/api/health" && request.method === "GET") {
      return json({ok:true,worker:"chemlink",build:"cloud-offers-v1",dbBinding:Boolean(env.DB),availableBindings:Object.keys(env),smsConfigured:Boolean(env.SMS_IR_API_KEY),host:url.host});
    }
    if (url.pathname === "/api/auth/send-otp" && request.method === "POST") return sendOtp({request,env,ctx});
    if (url.pathname === "/api/auth/verify-otp" && request.method === "POST") return verifyOtp({request,env,ctx});
    if (url.pathname === "/api/auth/me" && request.method === "GET") return me({request,env,ctx});
    if (url.pathname === "/api/auth/profile" && request.method === "GET") return getProfile({request,env,ctx});
    if (url.pathname === "/api/auth/profile" && request.method === "POST") return saveProfile({request,env,ctx});
    if (url.pathname === "/api/offers" && request.method === "GET") return getOffers({request,env,ctx});
    if (url.pathname === "/api/offers" && request.method === "POST") return createOffer({request,env,ctx});
    if (url.pathname === "/api/images" && request.method === "POST") return uploadImage({request,env,ctx});
    if (url.pathname.startsWith("/api/images/") && request.method === "GET") return getImage({request,env,ctx});
    if (url.pathname === "/api/admin/backup" && request.method === "GET") return adminBackup({request,env,ctx});
    if (url.pathname === "/api/admin/offers" && request.method === "GET") return adminGetOffers({request,env,ctx});
    if (url.pathname === "/api/admin/offers" && request.method === "POST") return adminUpdateOffer({request,env,ctx});
    return env.ASSETS.fetch(request);
  }
};