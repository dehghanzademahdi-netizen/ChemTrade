let listings=[],currentProfile=null;
const $=id=>document.getElementById(id);
const getSession=()=>localStorage.getItem('chemlinkSession')||'';
const loggedIn=()=>!!getSession();
function showLogin(){const m=$('modal');if(!m)return;m.classList.add('show','open');m.style.display='flex';m.setAttribute('aria-hidden','false')}
function hideLogin(){const m=$('modal');if(m){m.classList.remove('show','open');m.style.display='none';m.setAttribute('aria-hidden','true')}restoreLoginForm()}
function showIOSInstall(){const s=$('app-download');if(!s)return;let g=$('iosInstallGuide');if(!g){g=document.createElement('div');g.id='iosInstallGuide';g.style.cssText='margin-top:18px;padding:18px 20px;border:1px solid #dbe3ee;border-radius:16px;background:#f8fafc;color:#10233e;line-height:1.9';g.innerHTML='<strong style="display:block;font-size:15px;margin-bottom:7px">📱 نصب ChemLink روی آیفون</strong><span style="display:block;font-size:12px;color:#475569">این دکمه نمی‌تواند به‌تنهایی برنامه را به صفحه اصلی آیفون اضافه کند؛ iOS این کار را از داخل Safari انجام می‌دهد.</span><ol style="margin:9px 0 0;padding-right:22px;font-size:12px;color:#334155"><li>همین سایت را با <b>Safari</b> باز کنید.</li><li>دکمه <b>Share / اشتراک‌گذاری</b> را بزنید.</li><li><b>Add to Home Screen / افزودن به صفحه اصلی</b> را انتخاب کنید.</li><li>اگر گزینه <b>Open as Web App / باز کردن به‌عنوان وب‌اپ</b> نمایش داده شد، آن را روشن کنید و <b>Add</b> را بزنید.</li></ol><div style="margin-top:10px;font-size:10px;color:#64748b">بعد از این کار آیکون ChemLink روی Home Screen آیفون اضافه می‌شود.</div>';s.querySelector('.app-download-inner')?.appendChild(g)}g.scrollIntoView({behavior:'smooth',block:'center'});g.style.boxShadow='0 0 0 3px rgba(14,165,233,.15)';setTimeout(()=>g.style.boxShadow='',1800)}
function normalizePhone(v){v=(v||'').replace(/[\s-]/g,'');return v.startsWith('+98')?'0'+v.slice(3):v}
function validPhone(v){return /^09\d{9}$/.test(v)}
async function api(path,options={}){const headers={'Content-Type':'application/json',...(options.headers||{})};if(getSession())headers.Authorization='Bearer '+getSession();const r=await fetch(path,{...options,headers,cache:'no-store'});const data=await r.json().catch(()=>({}));if(!r.ok||!data.ok)throw new Error(data.error||'ارتباط با سرور انجام نشد.');return data}
async function loadListings(){const grid=$('listingGrid');if(!grid)return;try{const data=await api('/api/offers',{method:'GET'});listings=(data.offers||[]).map(x=>({id:x.id,kind:x.kind||'sell',name:x.name||'',official:x.official||'',market:x.market||'',quantity:x.quantity||'',place:x.place||'',time:x.createdAt&&Number.isFinite(Number(x.createdAt))?new Date(Number(x.createdAt)).toLocaleDateString('fa-IR'):'',supplier:x.supplier||'',phone:x.phone||'',description:x.description||'',photo:x.photo||''}));renderListings(listings)}catch(e){grid.innerHTML='<div class="empty">دریافت آگهی‌های بازار آنلاین انجام نشد. صفحه را دوباره بارگذاری کنید.</div>'}}
function renderLocked(){const grid=$('listingGrid');if(!grid)return;grid.innerHTML='<div class="empty">برای نمایش آگهی‌های آنلاین، صفحه را تازه‌سازی کنید.</div>'}
function esc(s){return String(s||'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]||m))}
function renderListings(items){const grid=$('listingGrid');if(!grid)return;grid.innerHTML=items.length?items.map(x=>'<article class="listing-card">'+(x.photo?'<img class="listing-photo" src="'+esc(x.photo)+'" alt="تصویر آگهی" loading="lazy">':'')+'<div class="listing-body"><span class="badge">'+(x.kind==='buy'?'درخواست خرید':'عرضه برای فروش')+'</span><h3>'+esc(x.name)+'</h3>'+(x.quantity?'<p><b>مقدار:</b> '+esc(x.quantity)+'</p>':'')+(x.official?'<p><b>قیمت اعلامی:</b> '+esc(x.official)+'</p>':'')+(x.market?'<p><b>قیمت بازار:</b> '+esc(x.market)+'</p>':'')+'<small>'+esc(x.place)+' '+(x.time?'• '+esc(x.time):'')+'</small><button class="primary full details" data-id="'+esc(x.id||'')+'">مشاهده جزئیات و تماس</button></div></article>').join(''):'<div class="empty">در حال حاضر آگهی تأییدشده‌ای وجود ندارد.</div>';grid.querySelectorAll('.details').forEach(b=>b.onclick=()=>{const x=listings.find(v=>String(v.id)===b.dataset.id);if(!x)return;const detail=document.createElement('div');detail.className='offer-detail';detail.innerHTML='<div><b>نوع آگهی:</b> '+(x.kind==='buy'?'خرید':'فروش')+'</div><div><b>ماده:</b> '+esc(x.name)+'</div><div><b>مقدار:</b> '+esc(x.quantity)+'</div><div><b>شهر:</b> '+esc(x.place)+'</div><div><b>تأمین‌کننده:</b> '+esc(x.supplier)+'</div><div><b>توضیحات:</b> '+esc(x.description)+'</div><div><b>تلفن تماس:</b> '+esc(x.phone)+'</div>';const card=document.querySelector('.modal-card');if(!card)return;$('modalTitle').textContent='جزئیات آگهی';$('modalText').textContent='';let old=$('profileForm');if(old)old.remove();$('phone').closest('label').style.display='none';$('otpWrap').style.display='none';$('recaptcha-container').style.display='none';let d=$('offerDetail');if(!d){d=document.createElement('div');d.id='offerDetail';$('modalAction').before(d)}d.innerHTML=detail.innerHTML;$('modalAction').style.display='none';showLogin();})}
function updateHeader(){const b=$('loginBtn');if(b)b.textContent=currentProfile?('حساب کاربری: '+currentProfile.fullName):'ورود / ثبت‌نام';const search=$('searchInput');if(search)search.disabled=false}
function showProfileForm(profile=null){
 const card=document.querySelector('.modal-card');if(!card)return;
 $('modalTitle').textContent=profile?'ویرایش اطلاعات حساب':'تکمیل ثبت‌نام در ChemLink';
 $('phone').closest('label').style.display='none';$('otpWrap').style.display='none';$('recaptcha-container').style.display='none';
 let form=$('profileForm');if(!form){form=document.createElement('form');form.id='profileForm';form.style.cssText='max-height:55vh;overflow:auto;padding:2px 2px 8px';$('modalAction').before(form)}
 form.innerHTML='<label>نام و نام خانوادگی<input name="fullName" required minlength="2" autocomplete="name" value="'+esc(profile?.fullName||'')+'"></label><label>نام شرکت / مجموعه<input name="companyName" required minlength="2" autocomplete="organization" value="'+esc(profile?.companyName||'')+'"></label><label>نوع حساب<select name="role" required style="width:100%;padding:12px;margin-top:7px;border:1px solid var(--line);border-radius:11px;background:white"><option value="">انتخاب کنید</option><option value="buyer">خریدار / کارخانه / مصرف‌کننده</option><option value="supplier">تأمین‌کننده / فروشنده</option></select></label><label>شهر<input name="city" required minlength="2" autocomplete="address-level2" value="'+esc(profile?.city||'')+'"></label><label>آدرس (اختیاری)<input name="address" autocomplete="street-address" value="'+esc(profile?.address||'')+'"></label><label>تلفن ثابت (اختیاری)<input name="landline" inputmode="tel" value="'+esc(profile?.landline||'')+'"></label><label>زمینه فعالیت<input name="businessType" required minlength="2" placeholder="مثلاً تولید مواد شوینده، کشاورزی، پتروشیمی" value="'+esc(profile?.businessType||'')+'"></label><label>مواد اولیه موردنیاز یا قابل‌عرضه<textarea name="neededMaterials" rows="3" placeholder="نام مواد شیمیایی، گرید یا توضیحات (اختیاری)" style="width:100%;padding:12px;margin-top:7px;border:1px solid var(--line);border-radius:11px;font:inherit">'+esc(profile?.neededMaterials||'')+'</textarea></label>';
 form.querySelector('[name="role"]').value=profile?.role||'';
 $('modalText').textContent='شماره موبایل شما تأیید شده است. اطلاعات زیر را تکمیل کنید تا حساب شما ساخته و ذخیره شود.';
 const button=$('modalAction');button.textContent='ذخیره اطلاعات و ورود';button.disabled=false;button.onclick=saveProfile;
}
function restoreLoginForm(){const form=$('profileForm');if(form)form.remove();$('offerDetail')?.remove();$('modalAction').style.display='block';$('phone').closest('label').style.display='block';$('otpWrap').style.display='none';$('recaptcha-container').style.display='block';$('phone').disabled=false;$('modalTitle').textContent='ورود / ثبت‌نام با پیامک';$('modalText').textContent='شماره موبایل خود را وارد کنید تا کد تأیید پیامکی دریافت کنید.';$('modalAction').textContent='دریافت کد پیامکی';$('modalAction').disabled=false;$('modalAction').onclick=startSmsLogin}
const ADMIN_PHONE='09357236476';
async function downloadAdminBackup(token){
 const response=await fetch('/api/admin/backup',{headers:{Authorization:'Bearer '+token,Accept:'application/json'},cache:'no-store'});
 const data=await response.json().catch(()=>null);
 if(!response.ok||!data||!data.ok)throw new Error(data?.error||'ساخت فایل پشتیبان انجام نشد.');
 const content=JSON.stringify(data,null,2);
 const check=JSON.parse(content);
 if(!check.product||!check.createdAt)throw new Error('پشتیبان برگشتی کامل نیست؛ فایل ذخیره نشد.');
 const blob=new Blob(['\uFEFF',content],{type:'application/json;charset=utf-8'});
 const url=URL.createObjectURL(blob),a=document.createElement('a');
 a.href=url;a.download='ChemLink-backup-'+new Date().toISOString().replace(/[:.]/g,'-')+'.json';a.style.display='none';
 document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);
}
async function startSmsLogin(){
 const phone=normalizePhone($('phone')?.value);
 if(!validPhone(phone)){$('modalText').textContent='لطفاً شماره موبایل معتبر ۱۱ رقمی وارد کنید.';return}
 const purpose=phone===ADMIN_PHONE?'admin':'user';
 const b=$('modalAction');b.disabled=true;b.textContent='در حال ارسال...';
 try{await api('/api/auth/send-otp',{method:'POST',body:JSON.stringify({phone,purpose})});$('otpWrap').style.display='block';$('phone').disabled=true;$('modalText').textContent='کد ۶ رقمی ارسال شد. آن را وارد کنید.';b.textContent='تأیید کد';b.onclick=verifySmsLogin;$('otp')?.focus()}
 catch(e){$('modalText').textContent=e.message||'ارسال کد انجام نشد.';b.textContent='دریافت کد پیامکی'}
 finally{b.disabled=false}
}
async function verifySmsLogin(){
 const phone=normalizePhone($('phone')?.value),code=String($('otp')?.value||'').replace(/\D/g,'');
 if(!validPhone(phone)||!/^[0-9]{6}$/.test(code)){$('modalText').textContent='شماره موبایل یا کد ۶ رقمی معتبر نیست.';return}
 const purpose=phone===ADMIN_PHONE?'admin':'user';
 const b=$('modalAction');b.disabled=true;b.textContent='در حال تأیید...';
 try{
  const data=await api('/api/auth/verify-otp',{method:'POST',body:JSON.stringify({phone,code,purpose})});
  if(purpose==='admin'){
   $('modalTitle').textContent='ورود مدیریت تأیید شد';
   $('phone').closest('label').style.display='none';$('otpWrap').style.display='none';$('recaptcha-container').style.display='none';
   if(window.confirm('ورود مدیریت تأیید شد. همین حالا نسخه پشتیبان ChemLink روی این کامپیوتر ذخیره شود؟')){
    b.textContent='در حال ساخت پشتیبان...';
    try{
     await downloadAdminBackup(data.token);
     $('modalText').textContent='فایل پشتیبان ساخته شد. در پوشه Downloads فایل ChemLink-backup با پسوند .json را بررسی کن. برای دیدن محتوا می‌توانی آن را با Notepad باز کنی.';
    }catch(backupError){
     $('modalText').textContent='ورود مدیریت انجام شد، اما دانلود پشتیبان ناموفق بود: '+(backupError.message||'خطای نامشخص')+'\\nبرای تلاش مجدد، صفحه پشتیبان‌گیری مدیریت را باز کن.';
    }
   }else $('modalText').textContent='پشتیبان ذخیره نشد. هر وقت خواستی از صفحه پشتیبان‌گیری مدیریت دانلود کن.';
   b.textContent='بستن';b.disabled=false;b.onclick=hideLogin;
   return;
  }
  localStorage.setItem('chemlinkSession',data.token);localStorage.setItem('chemlinkPhone',data.phone||phone);
  const p=await api('/api/auth/profile',{method:'GET'});
  if(p.registered&&p.profile){currentProfile=p.profile;updateHeader();hideLogin();await loadListings()}else showProfileForm(p.profile||null)
 }catch(e){$('modalText').textContent=e.message||'تأیید کد انجام نشد.';b.textContent='تأیید کد'}
 finally{b.disabled=false}
}
async function saveProfile(e){if(e)e.preventDefault();const form=$('profileForm');if(!form||!form.reportValidity())return;const values=Object.fromEntries(new FormData(form).entries());const b=$('modalAction');b.disabled=true;b.textContent='در حال ذخیره...';try{const data=await api('/api/auth/profile',{method:'POST',body:JSON.stringify(values)});currentProfile=data.profile;updateHeader();hideLogin();await loadListings()}catch(err){$('modalText').textContent=err.message||'ذخیره اطلاعات انجام نشد.'}finally{b.disabled=false;b.textContent='ذخیره اطلاعات و ورود'}}
async function loadProfile(openForm=false){try{const p=await api('/api/auth/profile',{method:'GET'});if(p.registered&&p.profile){currentProfile=p.profile;updateHeader();if(openForm){showProfileForm(p.profile);showLogin()}else hideLogin();await loadListings()}else{currentProfile=null;updateHeader();showProfileForm(p.profile||null);showLogin();renderLocked()}}catch(e){currentProfile=null;updateHeader();localStorage.removeItem('chemlinkSession');localStorage.removeItem('chemlinkPhone');restoreLoginForm();renderLocked()}}
async function restoreSession(){if(!getSession()){updateHeader();renderLocked();return}try{await api('/api/auth/me',{method:'GET'});await loadProfile(false)}catch(e){localStorage.removeItem('chemlinkSession');localStorage.removeItem('chemlinkPhone');currentProfile=null;updateHeader();renderLocked()}}
async function submitOffer(e){e.preventDefault();if(!currentProfile||!getSession()){showLogin();$('modalText').textContent='برای ثبت آگهی ابتدا وارد حساب شوید.';return}const form=$('offerForm');if(!form||!form.reportValidity())return;const values=Object.fromEntries(new FormData(form).entries());const b=$('offerSubmit'),note=$('offerMessage');b.disabled=true;b.textContent='در حال ذخیره آنلاین...';note.textContent='';try{await api('/api/offers',{method:'POST',body:JSON.stringify(values)});form.reset();note.textContent='آگهی با موفقیت در پایگاه داده ابری ذخیره شد و پس از تأیید مدیریت در بازار نمایش داده می‌شود.';note.className='offer-message success';}catch(err){note.textContent=err.message||'ثبت آگهی انجام نشد.';note.className='offer-message error';if(String(err.message||'').includes('وارد حساب'))showLogin()}finally{b.disabled=false;b.textContent='ثبت آگهی آنلاین'}}
function wire(){['loginBtn','searchBtn','footerLogin'].forEach(id=>{const el=$(id);if(el)el.onclick=e=>{e.preventDefault();showLogin()}});['postBtn','ctaPost','footerPost'].forEach(id=>{const el=$(id);if(el)el.onclick=e=>{e.preventDefault();if(currentProfile){$('newOffer')?.scrollIntoView({behavior:'smooth',block:'start'});$('offerName')?.focus()}else{showLogin();$('modalText').textContent='برای ثبت آگهی ابتدا با پیامک وارد حساب خود شوید.'}}});$('closeModal')?.addEventListener('click',hideLogin);$('modal')?.addEventListener('click',e=>{if(e.target===$('modal'))hideLogin()});document.querySelectorAll('.category,.filter').forEach(el=>el.addEventListener('click',e=>{if(el.classList.contains('category')){const q=$('searchInput');if(q){q.value=el.dataset.cat||el.innerText;renderListings(listings.filter(x=>(x.name+' '+x.official+' '+x.market+' '+x.place+' '+x.description).toLowerCase().includes(q.value.toLowerCase())));$('listings')?.scrollIntoView({behavior:'smooth'})}}}));document.querySelectorAll('.app-top-btn.ios').forEach(el=>el.addEventListener('click',e=>{e.preventDefault();showIOSInstall()}));$('searchInput')?.addEventListener('input',()=>{const q=$('searchInput').value.trim().toLowerCase();renderListings(listings.filter(x=>(x.name+' '+x.official+' '+x.market+' '+x.place+' '+x.description).toLowerCase().includes(q)))});$('offerForm')?.addEventListener('submit',submitOffer);restoreSession();loadListings()}
window.addEventListener('error',e=>{const t=$('modalText');if(t)t.textContent='خطای صفحه: '+(e.message||'خطای ناشناخته')});
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',wire);else wire();
