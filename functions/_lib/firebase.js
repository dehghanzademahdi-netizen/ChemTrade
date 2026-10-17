const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.database";
let cachedToken = "";
let cachedUntil = 0;

function base64url(value) {
  const bytes = typeof value === "string" ? new TextEncoder().encode(value) : new Uint8Array(value);
  let binary = "";
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
  return btoa(binary).replaceAll("=", "").replaceAll("+", "-").replaceAll("/", "_");
}

function pemBytes(pem) {
  const body = String(pem || "")
    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
    .replaceAll("-----END PRIVATE KEY-----", "")
    .replaceAll("\\n", "").replaceAll("\\r", "").replaceAll(" ", "").replaceAll("\\t", "");
  const binary = atob(body);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

async function accessToken(env) {
  if (cachedToken && Date.now() < cachedUntil - 60_000) return cachedToken;
  const raw = env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error("Cloudflare Secret FIREBASE_SERVICE_ACCOUNT_JSON is missing.");
  let account;
  try { account = JSON.parse(raw); } catch { throw new Error("Cloudflare Secret FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON."); }
  if (!account.client_email || !account.private_key) throw new Error("Firebase service account JSON is missing required fields.");
  const now = Math.floor(Date.now() / 1000);
  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64url(JSON.stringify({ iss: account.client_email, scope: SCOPE, aud: TOKEN_URL, iat: now, exp: now + 3600 }));
  const unsigned = header + "." + claim;
  const key = await crypto.subtle.importKey("pkcs8", pemBytes(account.private_key), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const assertion = unsigned + "." + base64url(signature);
  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion })
  });
  if (!response.ok) {
    const detail = await response.text().catch(() => "");
    console.error("Firebase OAuth token exchange failed", response.status, detail.slice(0, 300));
    throw new Error("Firebase service account authentication failed (HTTP " + response.status + ").");
  }
  const result = await response.json();
  if (!result.access_token) throw new Error("Firebase OAuth response did not include an access token.");
  cachedToken = result.access_token;
  cachedUntil = Date.now() + Math.max(60, Number(result.expires_in) || 3600) * 1000;
  return cachedToken;
}

export async function firebaseFetch(env, path, init = {}) {
  const token = await accessToken(env);
  const url = new URL("https://chemlink-8909b-default-rtdb.firebaseio.com" + path);
  // Realtime Database REST documents OAuth access tokens via the access_token query parameter.
  // Use this documented mechanism so the service-account credentials are honored for admin reads/writes.
  url.searchParams.set("access_token", token);
  const headers = new Headers(init.headers || {});
  headers.delete("authorization");
  if (!headers.has("accept")) headers.set("accept", "application/json");
  return fetch(url.toString(), { ...init, headers, cache: init.cache || "no-store" });
}
