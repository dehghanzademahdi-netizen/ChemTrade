const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();

const SMS_IR_API_KEY = defineSecret("SMS_IR_API_KEY");
const SMS_IR_TEMPLATE_ID = 167254;
const REGION = "us-central1";
const OTP_TTL_MS = 5 * 60 * 1000;
const RESEND_COOLDOWN_MS = 60 * 1000;
const MAX_ATTEMPTS = 5;

function normalizePhone(value) {
  const raw = String(value || "").replace(/[\s-]/g, "");
  if (/^09\d{9}$/.test(raw)) return raw;
  if (/^\+989\d{9}$/.test(raw)) return "0" + raw.slice(3);
  if (/^989\d{9}$/.test(raw)) return "0" + raw.slice(2);
  return null;
}
function hashCode(code) {
  return crypto.createHash("sha256").update(String(code)).digest("hex");
}
function generateCode() {
  return String(crypto.randomInt(100000, 1000000));
}
function otpRef(phone, purpose) {
  return admin.database().ref("otp").child(purpose).child(phone);
}

exports.sendSmsOtp = onCall(
  { region: REGION, secrets: [SMS_IR_API_KEY] },
  async (request) => {
    const phone = normalizePhone(request.data?.phone);
    const purpose = request.data?.purpose === "admin" ? "admin" : "user";
    if (!phone) throw new HttpsError("invalid-argument", "شماره موبایل معتبر نیست.");

    const ref = otpRef(phone, purpose);
    const old = (await ref.get()).val() || {};
    const now = Date.now();

    if (old.sentAt && now - Number(old.sentAt) < RESEND_COOLDOWN_MS) {
      const wait = Math.ceil((RESEND_COOLDOWN_MS - (now - Number(old.sentAt))) / 1000);
      throw new HttpsError("resource-exhausted", `لطفاً ${wait} ثانیه دیگر دوباره درخواست کنید.`);
    }

    const code = generateCode();
    const response = await fetch("https://api.sms.ir/v1/send/verify", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": SMS_IR_API_KEY.value()
      },
      body: JSON.stringify({
        mobile: phone,
        templateId: SMS_IR_TEMPLATE_ID,
        parameters: [{ name: "Code", value: code }]
      })
    });

    let result = null;
    try { result = await response.json(); } catch (_) {}

    if (!response.ok || !result || Number(result.status) !== 1) {
      console.error("SMS.ir rejected OTP", {
        httpStatus: response.status,
        status: result?.status,
        message: result?.message
      });
      throw new HttpsError("internal", "ارسال پیامک انجام نشد.");
    }

    await ref.set({
      hash: hashCode(code),
      sentAt: now,
      expiresAt: now + OTP_TTL_MS,
      attempts: 0,
      messageId: result?.data?.messageId || null
    });

    return { ok: true, message: "کد تأیید ارسال شد." };
  }
);

exports.verifySmsOtp = onCall(
  { region: REGION, secrets: [SMS_IR_API_KEY] },
  async (request) => {
    const phone = normalizePhone(request.data?.phone);
    const code = String(request.data?.code || "").trim();
    const purpose = request.data?.purpose === "admin" ? "admin" : "user";

    if (!phone || !/^\d{6}$/.test(code)) {
      throw new HttpsError("invalid-argument", "شماره موبایل یا کد تأیید معتبر نیست.");
    }

    const ref = otpRef(phone, purpose);
    const snap = await ref.get();
    const record = snap.val();

    if (!record) throw new HttpsError("not-found", "کد تأیید پیدا نشد. دوباره درخواست کد کنید.");
    if (Date.now() > Number(record.expiresAt || 0)) {
      await ref.remove();
      throw new HttpsError("deadline-exceeded", "کد تأیید منقضی شده است.");
    }

    const attempts = Number(record.attempts || 0);
    if (attempts >= MAX_ATTEMPTS) {
      await ref.remove();
      throw new HttpsError("resource-exhausted", "تعداد تلاش‌ها بیش از حد مجاز است. دوباره کد بگیرید.");
    }

    if (hashCode(code) !== String(record.hash)) {
      await ref.update({ attempts: attempts + 1 });
      throw new HttpsError("invalid-argument", "کد تأیید صحیح نیست.");
    }

    await ref.remove();

    const authPhone = "+98" + phone.slice(1);
    let user;
    try {
      user = await admin.auth().getUserByPhoneNumber(authPhone);
    } catch (err) {
      if (err.code !== "auth/user-not-found") throw err;
      user = await admin.auth().createUser({ phoneNumber: authPhone });
    }

    const customToken = await admin.auth().createCustomToken(user.uid, {
      phoneVerified: true,
      otpPurpose: purpose
    });

    return { ok: true, customToken, uid: user.uid, phone };
  }
);
