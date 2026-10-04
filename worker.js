import { onRequestPost as sendOtp } from "./functions/api/auth/send-otp.js";
import { onRequestPost as verifyOtp } from "./functions/api/auth/verify-otp.js";
import { onRequestGet as me } from "./functions/api/auth/me.js";

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store"
    }
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === "/api/auth/send-otp" && request.method === "POST") {
      return sendOtp({ request, env, ctx });
    }

    if (url.pathname === "/api/auth/verify-otp" && request.method === "POST") {
      return verifyOtp({ request, env, ctx });
    }

    if (url.pathname === "/api/auth/me" && request.method === "GET") {
      return me({ request, env, ctx });
    }

    return env.ASSETS.fetch(request);
  }
};
