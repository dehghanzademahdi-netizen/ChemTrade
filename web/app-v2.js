let listings=[],currentProfile=null,adminUsers=[],selectedAdminUser=null;
const $=id=>document.getElementById(id);
const getSession=()=>localStorage.getItem('chemlinkSession')||'';
const loggedIn=()=>!!getSession();
function showLogin(){const m=$('modal');if(!m)return;m.classList.add('show','open');m.style.display='flex';m.setAttribute('aria-hidden','false')}
function hideLogin(){const m=$('modal');if(m){m.classList.remove('show','open');m.style.display='none';m.setAttribute('aria-hidden','true')}restoreLoginForm()}
function showIOSInstall(){const s=$('app-download');if(!s)return;let g=$('iosInstallGuide');if(!g){g=document.createElement('div');g.id='iosInstallGuide';g.style.cssText='margin-top:18px;padding:18px 20px;border:1px solid #dbe3ee;border-radius:16px;background:#f8fafc;color:#10233e;line-height:1.9';g.innerHTML='<strong style="display:block;font-size:15px;margin-bottom:7px">📱 نصب ChemLink روی آیفون</strong><span style="display:block;font-size:12px;color:#475569">این دکمه نمی‌تواند به‌تنهایی برنامه را به صفحه اصلی آیفون اضافه کند؛ iOS این کار را از داخل Safari انجام می‌دهد.</span><ol style="margin:9px 0 0;padding-right:22px;font-size:12px;color:#334155"><li>همین سایت را با <b>Safari</b> باز کنید.</li><li>دکمه <b>Share / اشتراک‌گذاری</b> را بزنید.</li><li><b>Add to Home Screen / افزودن به صفحه اصلی</b> را انتخاب کنید.</li><li>اگر گزینه <b>Open as Web App / باز کردن به‌عنوان وب‌اپ</b> نمایش داده شد، آن را روشن کنید و <b>Add</b> را بزنید.</li></ol><div style="margin-top:10px;font-size:10px;color:#64748b">بعد از این کار آیکون ChemLink روی Home Screen آیفون اضافه می‌شود.</div>';s.querySelector('.app-download-inner')?.appendChild(g)}g.scrollIntoView({behavior:'smooth',block:'center'});g.style.boxShadow='0 0 0 3px rgba(14,165,233,.15)';setTimeout(()=>g.style.boxShadow='',1800)}
function inferCategory(name){const n=String(name||'').toLowerCase();const groups=[
['حلال‌ها',['استون','متانول','اتانول','ایزوپروپانول','ipa','تولوئن','زایلین','اتیل استات','بوتیل استات','هگزان','mek','dmf','dmso','حلال']],
['اسیدها',['سولفوریک','کلریدریک','نیتریک','فسفریک','استیک','فرمیک','سیتریک','اگزالیک','بوریک','اسید']],
['بازها و قلیاها',['سود پرک','سود مایع','کاستیک','پتاس','آمونیاک','مونو اتانول','دی اتانول','تری اتانول','mea','dea','tea','قلیا']],
['افزودنی‌ها',['sles','labsa','capb','سورفکتانت','ضدکف','آنتی اسکالانت','ضدخوردگی','افزودنی']],
['محصولات پتروشیمی',['پلی اتیلن','پلی‌اتیلن','پلی پروپیلن','پلی‌پروپیلن','pvc','pet','meg','deg','teg','استایرن','اتیلن','پروپیلن','پتروشیمی']],
['کود و کشاورزی',['اوره','سولفات آمونیوم','نیترات آمونیوم','نیترات پتاسیم','map','dap','سولفات پتاسیم','کود']],
['نمک‌ها و مواد معدنی',['کلرید سدیم','کربنات سدیم','بی کربنات','کلرید کلسیم','سولفات سدیم','سولفات منیزیم','سیلیکا','کربنات کلسیم','تالک']],
['رزین، رنگ و پلیمر',['رزین','تینر','تیتان','مستربچ','کربن بلک','پلی استر','اپوکسی']]];
for(const [group,terms] of groups)if(terms.some(t=>n.includes(t)))return group;return 'سایر مواد';
}
function toEnglishDigits(v){return String(v||'').replace(/[۰-۹٠-٩]/g,ch=>{const n=ch.charCodeAt(0);return String(n>=0x06F0&&n<=0x06F9?n-0x06F0:n-0x0660)})}
function normalizePhone(v){v=toEnglishDigits(v).replace(/[\s\u200c()-]/g,'');return v.startsWith('+98')?'0'+v.slice(3):v}
function validPhone(v){return /^09\d{9}$/.test(v)}
async function api(path,options={}){const headers={'Content-Type':'application/json',...(options.headers||{})};if(getSession())headers.Authorization='Bearer '+getSession();let r;try{r=await fetch(path,{...options,headers,cache:'no-store'})}catch(e){throw new TypeError('ارتباط با سرور برقرار نشد؛ اتصال اینترنت یا مسیر API در دسترس نیست.')}const data=await r.json().catch(()=>({}));if(!r.ok||!data.ok){const error=new Error(data.error||('خطای سرور ('+r.status+').'));error.status=r.status;throw error}return data}
async function loadListings(options={}){const grid=$('listingGrid');if(!grid)return;try{const data=await api('/api/offers?_='+Date.now(),{method:'GET'});listings=(data.offers||[]).map(x=>({id:x.id,kind:x.kind||'sell',name:x.name||'',category:x.category||inferCategory(x.name),official:x.official||'',market:x.market||'',quantity:x.quantity||'',deliveryTime:x.deliveryTime||'',place:x.place||'',time:x.createdAt&&Number.isFinite(Number(x.createdAt))?new Date(Number(x.createdAt)).toLocaleDateString('fa-IR'):'',supplier:x.supplier||'',phone:x.phone||'',description:x.description||'',photo:x.photo||''}));renderListings(listings);return data}catch(e){grid.innerHTML='<div class="empty">دریافت آگهی‌های بازار آنلاین انجام نشد. صفحه را دوباره بارگذاری کنید.</div>';if(options.throwOnError)throw e;return null}}
function renderLocked(){const grid=$('listingGrid');if(!grid)return;grid.innerHTML='<div class="empty">برای نمایش آگهی‌های آنلاین، صفحه را تازه‌سازی کنید.</div>'}
function esc(s){return String(s||'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]||m))}
function renderListings(items){const grid=$('listingGrid');if(!grid)return;grid.innerHTML=items.length?items.map(x=>'<article class="listing-card">'+(x.photo?'<img class="listing-photo" src="'+esc(x.photo)+'" alt="تصویر آگهی" loading="lazy">':'')+'<div class="listing-body"><span class="badge">'+(x.kind==='buy'?'درخواست خرید':'عرضه برای فروش')+'</span><small style="display:block;color:#1769e0;margin-top:8px">'+esc(x.category||inferCategory(x.name))+'</small><h3>'+esc(x.name)+'</h3>'+(x.quantity?'<p><b>مقدار:</b> '+esc(x.quantity)+'</p>':'')+(x.official?'<p><b>قیمت رسمی نهایی:</b> '+esc(x.official)+'</p>':'')+(x.market?'<p><b>قیمت غیررسمی:</b> '+esc(x.market)+'</p>':'')+(x.deliveryTime?'<p><b>شرایط تحویل:</b> '+esc(x.deliveryTime)+'</p>':'')+'<small>'+esc(x.place)+' '+(x.time?'• '+esc(x.time):'')+'</small><button class="primary full details" data-id="'+esc(x.id||'')+'">جزئیات و ارتباط با مدیریت</button></div></article>').join(''):'<div class="empty">در حال حاضر آگهی تأییدشده‌ای وجود ندارد.</div>';grid.querySelectorAll('.details').forEach(b=>b.onclick=()=>{const x=listings.find(v=>String(v.id)===b.dataset.id);if(!x)return;const detail=document.createElement('div');detail.className='offer-detail';detail.innerHTML='<div><b>نوع آگهی:</b> '+(x.kind==='buy'?'خرید':'فروش')+'</div><div><b>ماده:</b> '+esc(x.name)+'</div><div><b>مقدار:</b> '+esc(x.quantity)+'</div><div><b>شهر:</b> '+esc(x.place)+'</div><div><b>قیمت رسمی نهایی:</b> '+esc(x.official||'برای استعلام با مدیریت تماس بگیرید')+'</div><div><b>قیمت غیررسمی:</b> '+esc(x.market||'برای استعلام با مدیریت تماس بگیرید')+'</div><div style="margin-top:12px;padding:12px;border-radius:12px;background:#f0f7ff"><b>ارتباط فقط از طریق مدیریت ChemLink</b><p>تلفن: <a href="tel:+989357236476">۰۹۳۵۷۲۳۶۴۷۶</a></p><p><a href="https://wa.me/989357236476" target="_blank" rel="noopener noreferrer">پیام در واتساپ مدیریت</a></p><p><a href="https://t.me/+989357236476" target="_blank" rel="noopener noreferrer">پیام در تلگرام مدیریت</a></p></div>';const card=document.querySelector('.modal-card');if(!card)return;$('modalTitle').textContent='جزئیات آگهی';$('modalText').textContent='اطلاعات تماس آگهی‌دهنده خصوصی است؛ برای خرید با مدیریت ChemLink در ارتباط باشید.';let old=$('profileForm');if(old)old.remove();$('phone').closest('label').style.display='none';$('otpWrap').style.display='none';$('recaptcha-container').style.display='none';let d=$('offerDetail');if(!d){d=document.createElement('div');d.id='offerDetail';$('modalAction').before(d)}d.innerHTML=detail.innerHTML;$('modalAction').style.display='none';showLogin();})}
function updateHeader(){const b=$('loginBtn'),role=localStorage.getItem('chemlinkRole')||'user',isAdmin=role==='admin';if(b)b.textContent=isAdmin?'پنل مدیریت':(currentProfile?('حساب کاربری: '+currentProfile.fullName):'ورود / ثبت‌نام');const out=$('logoutBtn');if(out)out.hidden=!loggedIn();const post=$('postBtn');if(post)post.hidden=isAdmin;const mine=$('myOffersBtn');if(mine)mine.hidden=!currentProfile||isAdmin;const search=$('searchInput');if(search)search.disabled=false;if(isAdmin)setAdminMode(true);else setAdminMode(false);const mySection=$('myOffers');if(mySection)mySection.hidden=!currentProfile||isAdmin}
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
 const phone=normalizePhone($('phone')?.value),code=toEnglishDigits($('otp')?.value||'').replace(/\D/g,'');
 if(!validPhone(phone)||!/^[0-9]{6}$/.test(code)){$('modalText').textContent='شماره موبایل یا کد ۶ رقمی معتبر نیست.';return}
 const purpose=phone===ADMIN_PHONE?'admin':'user';
 const b=$('modalAction');b.disabled=true;b.textContent='در حال تأیید...';
 try{
  const data=await api('/api/auth/verify-otp',{method:'POST',body:JSON.stringify({phone,code,purpose})});
  if(purpose==='admin'){
   localStorage.setItem('chemlinkSession',data.token);localStorage.setItem('chemlinkPhone',phone);localStorage.setItem('chemlinkRole','admin');
   currentProfile={fullName:'مدیریت ChemLink',role:'admin'};updateHeader();
   let backupMessage='';
   if(window.confirm('ورود مدیریت تأیید شد. همین حالا نسخه پشتیبان ChemLink روی این کامپیوتر ذخیره شود؟')){
    b.textContent='در حال ساخت پشتیبان...';
    try{await downloadAdminBackup(data.token);backupMessage='نسخه پشتیبان درخواست شد و در پوشه Downloads ذخیره می‌شود.'}
    catch(backupError){backupMessage='ورود مدیریت انجام شد؛ اما پشتیبان دانلود نشد: '+(backupError.message||'خطای نامشخص')}
   }else backupMessage='پشتیبان‌گیری را فعلاً رد کردی؛ هر زمان خواستی می‌توانی از صفحه پشتیبان‌گیری اقدام کنی.';
   hideLogin();await loadAdminOffers();if(backupMessage)renderAdminPanel(backupMessage);return;
  }
  localStorage.setItem('chemlinkSession',data.token);localStorage.setItem('chemlinkPhone',data.phone||phone);localStorage.setItem('chemlinkRole','user');
  const p=await api('/api/auth/profile',{method:'GET'});
  if(p.registered&&p.profile){currentProfile=p.profile;updateHeader();hideLogin();await loadListings()}else showProfileForm(p.profile||null)
 }catch(e){$('modalText').textContent=e.message||'تأیید کد انجام نشد.';b.textContent='تأیید کد'}
 finally{b.disabled=false}
}

let adminOffers=[],adminTab='PENDING',selectedAdminOffer=null;
function ensureAdminPanel(){let p=$('adminPanel');if(!p){p=document.createElement('section');p.id='adminPanel';p.className='section';p.style.cssText='min-height:70vh;padding:28px 0';document.querySelector('main')?.prepend(p)}return p}
function setAdminMode(on){const panel=ensureAdminPanel();panel.hidden=!on;document.querySelectorAll('main > section:not(#adminPanel)').forEach(x=>{if(x.id!=='myOffers')x.hidden=on});const footer=document.querySelector('footer');if(footer)footer.hidden=on;const nav=document.querySelector('.nav-links');if(nav)nav.hidden=false;const post=$('postBtn');if(post)post.hidden=on}
function adminStatusLabel(status){return status==='APPROVED'?'منتشرشده':status==='REJECTED'?'نیازمند اصلاح':'در انتظار بررسی'}
function adminDigits(v){return String(v??'').replace(/[۰-۹]/g,c=>'0123456789'['۰۱۲۳۴۵۶۷۸۹'.indexOf(c)]).replace(/[٠-٩]/g,c=>'0123456789'['٠١٢٣٤٥٦٧٨٩'.indexOf(c)])}
function adminMoney(v){const digits=adminDigits(v).replace(/[^0-9]/g,'');const n=Number(digits);return digits&&Number.isFinite(n)?n:null}
function adminFinal(base,type,margin){const n=adminMoney(base);if(n===null)return 'قابل محاسبه نیست';const m=Math.max(0,Number(margin)||0);return Math.round(type==='TOMAN'?n+m:n+n*m/100).toLocaleString('fa-IR')+' تومان'}
function renderAdminPanel(message=''){
 if(adminTab==='USERS'){renderAdminUsers(message);return}
 const panel=ensureAdminPanel(),items=adminOffers.filter(o=>String(o.status||'PENDING').toUpperCase()===adminTab);
 const tabs=[['PENDING','در انتظار بررسی'],['APPROVED','منتشرشده'],['REJECTED','نیازمند اصلاح'],['USERS','مدیریت کاربران']];
 let html='<div class="container"><div style="display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap"><div><span class="kicker">CHEMLINK ADMIN</span><h2 style="margin:6px 0">پنل مدیریت آگهی‌ها</h2><p style="margin:0;color:#64748b">بررسی آگهی، اعمال سود روی هر دو قیمت و انتشار در بازار</p></div><div style="display:flex;gap:8px;flex-wrap:wrap"><button id="adminUsersOpen" class="ghost">مدیریت کاربران</button><button id="adminRefresh" class="ghost">به‌روزرسانی آگهی‌ها</button></div></div>';
 if(message)html+='<p style="padding:12px;border-radius:10px;background:#fff7ed;color:#9a3412">'+esc(message)+'</p>';
 html+='<div style="display:flex;gap:8px;flex-wrap:wrap;margin:20px 0">'+tabs.map(t=>'<button class="'+(adminTab===t[0]?'primary':'ghost')+'" data-admin-tab="'+t[0]+'">'+t[1]+' ('+(t[0]==='USERS'?adminUsers.length:adminOffers.filter(o=>String(o.status||'PENDING').toUpperCase()===t[0]).length)+')</button>').join('')+'</div>';
 html+='<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:14px;align-items:start">';
 if(!items.length)html+='<div class="empty" style="grid-column:1/-1">در این بخش آگهی‌ای وجود ندارد.</div>';
 items.forEach(o=>{html+='<article style="background:white;border:1px solid #dbe3ee;border-radius:16px;padding:16px;box-shadow:0 5px 18px #0f172a0a"><div style="display:flex;justify-content:space-between;gap:8px"><span class="badge">'+(o.kind==='buy'?'درخواست خرید':'عرضه برای فروش')+'</span><small>'+adminStatusLabel(String(o.status||'PENDING').toUpperCase())+'</small></div><h3 style="margin:12px 0 6px">'+esc(o.name||'بدون نام')+'</h3><p style="margin:4px 0"><b>مقدار:</b> '+esc(o.quantity||'ثبت نشده')+'</p><p style="margin:4px 0"><b>قیمت رسمی تأمین‌کننده:</b> '+esc(o.official||'ثبت نشده')+'</p><p style="margin:4px 0"><b>قیمت غیررسمی تأمین‌کننده:</b> '+esc(o.market||'ثبت نشده')+'</p><p style="margin:4px 0"><b>تحویل:</b> '+esc(o.place||'')+' — '+esc(o.time||'')+'</p><p style="margin:4px 0"><b>فروشنده:</b> '+esc(o.supplier||'')+'</p><p style="margin:4px 0"><b>تلفن:</b> '+esc(o.phone||o.owner||'')+'</p><p style="white-space:pre-wrap;overflow-wrap:anywhere;margin:8px 0">'+esc(o.description||'')+'</p>';
 if(o.photo&&o.photo.startsWith('https://'))html+='<a href="'+esc(o.photo)+'" target="_blank" rel="noreferrer">مشاهده تصویر آگهی</a>';
 if(o.reason)html+='<p style="color:#b91c1c">پیام مدیریت: '+esc(o.reason)+'</p>';
 html+='<div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:12px"><button class="primary" data-review-id="'+esc(o.id)+'">'+(adminTab==='PENDING'?'بررسی و انتشار':'ویرایش / بازبینی')+'</button>';
 if(adminTab==='APPROVED')html+='<button class="ghost" data-unpublish-id="'+esc(o.id)+'">بازگردانی به انتظار</button>';
 html+='</div></article>'});
 html+='</div>';
 if(selectedAdminOffer){const o=selectedAdminOffer,mt=o.marginType==='TOMAN'?'TOMAN':'PERCENT',mg=Number(o.margin)||0;html+='<div id="adminReview" style="margin-top:22px;background:white;border:1px solid #cbd5e1;border-radius:18px;padding:18px;max-width:780px"><h3 style="margin-top:0">بررسی آگهی: '+esc(o.name)+'</h3><p>قیمت رسمی ثبت‌شده: <b>'+esc(o.official||'ثبت نشده')+'</b></p><p>قیمت غیررسمی ثبت‌شده: <b>'+esc(o.market||'ثبت نشده')+'</b></p><label style="display:block;margin:12px 0">نوع سود مدیریت<select id="adminMarginType" style="display:block;width:100%;padding:12px;border:1px solid #cbd5e1;border-radius:10px;margin-top:6px"><option value="PERCENT" '+(mt==='PERCENT'?'selected':'')+'>درصد</option><option value="TOMAN" '+(mt==='TOMAN'?'selected':'')+'>مبلغ ثابت به تومان</option></select></label><label style="display:block;margin:12px 0">میزان سود مدیریت<input id="adminMargin" type="number" min="0" step="any" value="'+mg+'" style="display:block;width:100%;box-sizing:border-box;padding:12px;border:1px solid #cbd5e1;border-radius:10px;margin-top:6px"></label><div style="padding:12px;border-radius:12px;background:#f8fafc;line-height:2"><div>قیمت رسمی پس از سود: <b id="adminFinalOfficial">'+adminFinal(o.official,mt,mg)+'</b></div><div>قیمت غیررسمی پس از سود: <b id="adminFinalMarket">'+adminFinal(o.market,mt,mg)+'</b></div></div><label style="display:block;margin:12px 0">دلیل رد / پیام اصلاحیه (در صورت نیاز)<textarea id="adminReason" rows="3" style="display:block;width:100%;box-sizing:border-box;padding:12px;border:1px solid #cbd5e1;border-radius:10px;margin-top:6px">'+esc(o.reason||'')+'</textarea></label><div style="display:flex;gap:8px;flex-wrap:wrap"><button class="primary" id="adminApprove">اعمال سود و انتشار</button><button class="ghost" id="adminReject">رد و درخواست اصلاح</button><button class="ghost" id="adminCloseReview">بستن</button></div></div>'}
 html+='</div>';panel.innerHTML=html;
 panel.querySelector('#adminRefresh')?.addEventListener('click',loadAdminOffers);panel.querySelector('#adminUsersOpen')?.addEventListener('click',()=>{adminTab='USERS';selectedAdminOffer=null;loadAdminUsers()});
 panel.querySelectorAll('[data-admin-tab]').forEach(b=>b.onclick=()=>{adminTab=b.dataset.adminTab;selectedAdminOffer=null;selectedAdminUser=null;if(adminTab==='USERS')loadAdminUsers();else renderAdminPanel()});
 panel.querySelectorAll('[data-review-id]').forEach(b=>b.onclick=()=>{selectedAdminOffer=adminOffers.find(o=>String(o.id)===b.dataset.reviewId)||null;renderAdminPanel();$('adminReview')?.scrollIntoView({behavior:'smooth',block:'start'})});
 panel.querySelectorAll('[data-unpublish-id]').forEach(b=>b.onclick=()=>adminDecision(b.dataset.unpublishId,'PENDING'));
 const updatePreview=()=>{if(!selectedAdminOffer)return;const type=$('adminMarginType')?.value||'PERCENT',m=$('adminMargin')?.value||'0';const a=$('adminFinalOfficial'),b=$('adminFinalMarket');if(a)a.textContent=adminFinal(selectedAdminOffer.official,type,m);if(b)b.textContent=adminFinal(selectedAdminOffer.market,type,m)};
 $('adminMarginType')?.addEventListener('change',updatePreview);$('adminMargin')?.addEventListener('input',updatePreview);
 $('adminCloseReview')?.addEventListener('click',()=>{selectedAdminOffer=null;renderAdminPanel()});
 $('adminApprove')?.addEventListener('click',()=>adminDecision(selectedAdminOffer?.id,'APPROVED'));
 $('adminReject')?.addEventListener('click',()=>adminDecision(selectedAdminOffer?.id,'REJECTED'));
}
function renderAdminUsers(message=''){
 const panel=ensureAdminPanel();panel.hidden=false;setAdminMode(true);
 let h='<div class="container"><div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap"><div><span class="kicker">CHEMLINK ADMIN</span><h2>مدیریت کاربران</h2><p style="color:#64748b">فهرست پایدار کاربران؛ اطلاعات در پایگاه داده نگهداری می‌شود.</p></div><div><button class="ghost" id="adminBackOffers">بازگشت به آگهی‌ها</button> <button class="primary" id="adminUsersRefresh">به‌روزرسانی</button></div></div>';
 if(message)h+='<p style="padding:12px;background:#fff7ed;color:#9a3412;border-radius:10px">'+esc(message)+'</p>';
 h+='<p><b>تعداد کاربران:</b> '+adminUsers.length+' — کاربران با اطلاعات ناقص هم در فهرست باقی می‌مانند.</p><input id="adminUserSearch" placeholder="جست‌وجوی نام، شرکت، موبایل، شهر یا مواد" style="width:100%;box-sizing:border-box;padding:12px;border:1px solid #cbd5e1;border-radius:10px;margin:8px 0 16px"><div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:12px">';
 if(!adminUsers.length)h+='<div class="empty">کاربری در فهرست نیست.</div>';
 adminUsers.forEach(u=>{h+='<article data-user-card="'+esc(u.phone)+'" style="background:white;border:1px solid #dbe3ee;border-radius:14px;padding:15px;overflow-wrap:anywhere"><b>'+esc(u.fullName||'نام تکمیل نشده')+'</b> <span class="badge">'+(u.profileComplete?'تکمیل‌شده':'ناقص')+'</span><p><b>موبایل:</b> '+esc(u.phone)+'</p><p><b>شرکت:</b> '+esc(u.companyName||'—')+'</p><p><b>نوع حساب:</b> '+esc(u.role==='supplier'?'تأمین‌کننده':u.role==='buyer'?'خریدار':'تعیین نشده')+'</p><p><b>شهر:</b> '+esc(u.city||'—')+'</p><p><b>زمینه فعالیت:</b> '+esc(u.businessType||'—')+'</p><p><b>مواد:</b> '+esc(u.neededMaterials||'—')+'</p><p><b>آدرس:</b> '+esc(u.address||'—')+'</p><p><b>تلفن ثابت:</b> '+esc(u.landline||'—')+'</p><small>ثبت: '+(u.createdAt?new Date(Number(u.createdAt)).toLocaleDateString('fa-IR'):'ثبت اولیه ناقص')+'</small><p><button class="primary" data-edit-user="'+esc(u.phone)+'">ویرایش اطلاعات</button></p></article>'});
 h+='</div>';
 if(selectedAdminUser){const u=selectedAdminUser;h+='<div style="margin-top:20px;padding:18px;border:1px solid #cbd5e1;border-radius:16px;background:white;max-width:720px"><h3>ویرایش کاربر '+esc(u.phone)+'</h3><form id="adminUserForm" style="display:grid;gap:10px"><label>نام و نام خانوادگی<input name="fullName" required value="'+esc(u.fullName||'')+'"></label><label>نام شرکت<input name="companyName" required value="'+esc(u.companyName||'')+'"></label><label>نوع حساب<select name="role"><option value="buyer" '+(u.role==='buyer'?'selected':'')+'>خریدار</option><option value="supplier" '+(u.role==='supplier'?'selected':'')+'>تأمین‌کننده</option></select></label><label>شهر<input name="city" required value="'+esc(u.city||'')+'"></label><label>زمینه فعالیت<input name="businessType" required value="'+esc(u.businessType||'')+'"></label><label>مواد موردنیاز یا قابل‌عرضه<textarea name="neededMaterials" rows="3">'+esc(u.neededMaterials||'')+'</textarea></label><label>آدرس<input name="address" value="'+esc(u.address||'')+'"></label><label>تلفن ثابت<input name="landline" value="'+esc(u.landline||'')+'"></label><div><button class="primary" type="submit">ذخیره اطلاعات کاربر</button> <button class="ghost" type="button" id="adminUserCancel">انصراف</button></div></form></div>'}
 h+='</div>';panel.innerHTML=h;
 $('adminBackOffers')?.addEventListener('click',()=>{adminTab='PENDING';selectedAdminUser=null;loadAdminOffers()});
 $('adminUsersRefresh')?.addEventListener('click',loadAdminUsers);
 $('adminUserSearch')?.addEventListener('input',()=>{const q=$('adminUserSearch').value.trim().toLowerCase();panel.querySelectorAll('[data-user-card]').forEach(c=>c.hidden=!c.innerText.toLowerCase().includes(q))});
 panel.querySelectorAll('[data-edit-user]').forEach(b=>b.onclick=()=>{selectedAdminUser=adminUsers.find(u=>u.phone===b.dataset.editUser)||null;renderAdminUsers()});
 $('adminUserCancel')?.addEventListener('click',()=>{selectedAdminUser=null;renderAdminUsers()});
 $('adminUserForm')?.addEventListener('submit',async e=>{e.preventDefault();const f=e.currentTarget;if(!f.reportValidity())return;const payload=Object.fromEntries(new FormData(f).entries());payload.phone=selectedAdminUser.phone;const b=f.querySelector('[type=submit]');b.disabled=true;b.textContent='در حال ذخیره...';try{await api('/api/admin/users',{method:'POST',body:JSON.stringify(payload)});selectedAdminUser=null;await loadAdminUsers('اطلاعات کاربر ذخیره شد.')}catch(err){renderAdminUsers('ذخیره اطلاعات انجام نشد: '+(err.message||'خطای نامشخص'))}});
}
async function loadAdminUsers(message=''){
 const p=ensureAdminPanel();p.hidden=false;setAdminMode(true);p.innerHTML='<div class="container"><h2>مدیریت کاربران</h2><p>در حال دریافت فهرست...</p></div>';
 try{const d=await api('/api/admin/users');adminUsers=d.users||[];renderAdminUsers(message)}catch(e){renderAdminUsers(e.message||'دریافت کاربران انجام نشد.')}
}

async function loadAdminOffers(message=''){
 setAdminMode(true);const p=ensureAdminPanel();p.hidden=false;p.innerHTML='<div class="container"><h2>پنل مدیریت آگهی‌ها</h2><p>در حال دریافت آگهی‌های فضای ابری...</p></div>';
 try{const d=await api('/api/admin/offers?_='+Date.now());adminOffers=d.offers||[];renderAdminPanel(message)}
 catch(e){renderAdminPanel(e.message||'دریافت آگهی‌ها انجام نشد.');if(String(e.message||'').includes('ورود مدیریت معتبر نیست')){localStorage.removeItem('chemlinkSession');localStorage.removeItem('chemlinkRole');localStorage.removeItem('chemlinkPhone');currentProfile=null;updateHeader();setAdminMode(false)}}
}
async function adminDecision(id,status){
 if(!id)return;const type=$('adminMarginType')?.value||selectedAdminOffer?.marginType||'PERCENT',margin=Number($('adminMargin')?.value??selectedAdminOffer?.margin??0)||0,reason=$('adminReason')?.value||'';
 try{
  const saved=await api('/api/admin/offers',{method:'POST',body:JSON.stringify({id,status,marginType:type,margin,reason:status==='REJECTED'?(reason||'لطفاً اطلاعات آگهی را اصلاح کنید'):reason})});
  selectedAdminOffer=null;
  if(status==='APPROVED'){
   let isPublic=false;
   for(let attempt=0;attempt<4;attempt++){
    const publicData=await api('/api/offers?_='+Date.now(),{method:'GET'});
    isPublic=(publicData.offers||[]).some(o=>String(o.id)===String(id)&&String(o.status||'APPROVED').toUpperCase()==='APPROVED');
    if(isPublic)break;
    if(attempt<3)await new Promise(resolve=>setTimeout(resolve,350));
   }
   if(!isPublic){
    const adminData=await api('/api/admin/offers?_='+Date.now());
    const latest=(adminData.offers||[]).find(o=>String(o.id)===String(id));
    adminOffers=adminData.offers||[];
    if(latest&&String(latest.status||'').toUpperCase()==='APPROVED'){
     adminTab='APPROVED';
     renderAdminPanel('وضعیت تأییدشده در پنل مدیریت ثبت است، اما API عمومی هنوز آگهی را نشان نمی‌دهد. برای جلوگیری از برگشت وضعیت، آگهی را دوباره تأیید نکنید؛ مشکل همگام‌سازی در حال بررسی است.');
    }else{
     adminTab='PENDING';
     renderAdminPanel('سرور تأیید آگهی را پایدار نگه نداشت. وضعیت برگشتی: '+String(latest?.status||'نامشخص')+'؛ لطفاً فعلاً دوباره تأیید نکنید.');
    }
    return;
   }
   await loadListings({throwOnError:true});
   adminTab='APPROVED';
   await loadAdminOffers('انتشار تأیید شد و آگهی از API عمومی دریافت شد.');
  }else{
   await loadAdminOffers('تغییر وضعیت آگهی ذخیره شد.');
   await loadListings();
  }
 }catch(e){renderAdminPanel('انتشار/ذخیره تأیید نشد: '+(e.message||'خطای نامشخص'))}
}
function logoutAccount(){
 localStorage.removeItem('chemlinkSession');localStorage.removeItem('chemlinkPhone');localStorage.removeItem('chemlinkRole');currentProfile=null;adminOffers=[];selectedAdminOffer=null;adminTab='PENDING';setAdminMode(false);if($('myOffers'))$('myOffers').hidden=true;updateHeader();hideLogin();loadListings();
}

async function saveProfile(e){if(e)e.preventDefault();const form=$('profileForm');if(!form||!form.reportValidity())return;const values=Object.fromEntries(new FormData(form).entries());const b=$('modalAction');b.disabled=true;b.textContent='در حال ذخیره...';try{const data=await api('/api/auth/profile',{method:'POST',body:JSON.stringify(values)});currentProfile=data.profile;updateHeader();hideLogin();await loadListings()}catch(err){$('modalText').textContent=err.message||'ذخیره اطلاعات انجام نشد.'}finally{b.disabled=false;b.textContent='ذخیره اطلاعات و ورود'}}
async function loadProfile(openForm=false){try{const p=await api('/api/auth/profile',{method:'GET'});if(p.registered&&p.profile){currentProfile=p.profile;updateHeader();if(openForm){showProfileForm(p.profile);showLogin()}else hideLogin();await loadListings()}else{currentProfile=null;updateHeader();showProfileForm(p.profile||null);showLogin();renderLocked()}}catch(e){if(e.status===401){localStorage.removeItem('chemlinkSession');localStorage.removeItem('chemlinkPhone');localStorage.removeItem('chemlinkRole');currentProfile=null;restoreLoginForm()}else{$('modalText').textContent='کد تأیید پذیرفته شد، اما بارگذاری حساب کامل نشد. نشست شما حفظ شده است؛ دوباره تلاش کنید. '+(e.message||'');$('modalAction').textContent='تلاش دوباره برای ورود';$('modalAction').onclick=()=>loadProfile(false)}updateHeader();renderLocked()}}
async function restoreSession(){if(!getSession()){updateHeader();renderLocked();return}if(localStorage.getItem('chemlinkRole')==='admin'){currentProfile={fullName:'مدیریت ChemLink',role:'admin'};updateHeader();await loadAdminOffers();return}try{await api('/api/auth/me',{method:'GET'});await loadProfile(false)}catch(e){localStorage.removeItem('chemlinkSession');localStorage.removeItem('chemlinkPhone');localStorage.removeItem('chemlinkRole');currentProfile=null;updateHeader();renderLocked()}}
async function uploadOfferImage(file){
 if(!file)return "";
 if(!["image/jpeg","image/png","image/webp"].includes(file.type))throw new Error("فرمت عکس باید JPG، PNG یا WebP باشد.");
 if(file.size>12*1024*1024)throw new Error("حجم فایل اصلی عکس نباید بیشتر از ۱۲ مگابایت باشد.");
 let bitmap;
 try{bitmap=await createImageBitmap(file)}catch(e){throw new Error("باز کردن تصویر ممکن نشد؛ یک فایل عکس معمولی انتخاب کنید.")}
 const maxSide=1200,scale=Math.min(1,maxSide/Math.max(bitmap.width,bitmap.height));
 const canvas=document.createElement("canvas");canvas.width=Math.max(1,Math.round(bitmap.width*scale));canvas.height=Math.max(1,Math.round(bitmap.height*scale));
 const ctx=canvas.getContext("2d");ctx.fillStyle="#ffffff";ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(bitmap,0,0,canvas.width,canvas.height);bitmap.close?.();
 let blob=await new Promise(resolve=>canvas.toBlob(resolve,"image/jpeg",0.78));
 if(!blob)throw new Error("فشرده‌سازی تصویر انجام نشد.");
 for(let q=0;blob.size>370*1024&&q<4;q++){blob=await new Promise(resolve=>canvas.toBlob(resolve,"image/jpeg",0.68-q*0.1));if(!blob)throw new Error("فشرده‌سازی تصویر انجام نشد.")}
 if(blob.size>380*1024)throw new Error("این عکس هنوز بزرگ است؛ لطفاً عکس کوچک‌تری انتخاب کنید.");
 const dataUrl=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result||""));reader.onerror=()=>reject(new Error("خواندن فایل عکس انجام نشد."));reader.readAsDataURL(blob)});
 const payload=await api("/api/images",{method:"POST",body:JSON.stringify({type:"image/jpeg",data:dataUrl.split(",")[1]||""})});
 return payload.url;
}
async function loadMyOffers(){const grid=$('myOffersGrid');if(!grid)return;if(!currentProfile||!getSession()){grid.innerHTML='<div class="empty">برای مشاهده آگهی‌های خود وارد حساب شوید.</div>';return}grid.innerHTML='<div class="empty">در حال دریافت آگهی‌های شما...</div>';try{const data=await api('/api/offers?mine=1&_='+Date.now(),{method:'GET'});const items=data.offers||[];window._myOffers=items;const labels={PENDING:'در انتظار بررسی مدیریت',APPROVED:'تأیید و منتشر شده',REJECTED:'نیازمند اصلاح'};grid.innerHTML=items.length?items.map(x=>'<article class="listing-card"><div class="listing-body"><span class="badge">'+esc(labels[String(x.status||'PENDING').toUpperCase()]||'در حال بررسی')+'</span><h3>'+esc(x.name)+'</h3><p>نوع: '+(x.kind==='buy'?'درخواست خرید':'عرضه برای فروش')+'</p><p>مقدار: '+esc(x.quantity)+'</p><p>محل تحویل: '+esc(x.place)+'</p>'+(x.reason?'<p><b>یادداشت مدیریت:</b> '+esc(x.reason)+'</p>':'')+'<button class="primary full edit-my-offer" data-id="'+esc(x.id)+'">درخواست اصلاح آگهی</button></div></article>').join(''):'<div class="empty">هنوز آگهی‌ای در حساب شما ثبت نشده است.</div>';grid.querySelectorAll('.edit-my-offer').forEach(b=>b.onclick=()=>editMyOffer(b.dataset.id))}catch(e){grid.innerHTML='<div class="empty">دریافت آگهی‌های شما انجام نشد: '+esc(e.message||'خطای نامشخص')+'</div>'}}
function editMyOffer(id){const x=(window._myOffers||[]).find(v=>String(v.id)===String(id));if(!x)return;const f=$('offerForm');if(!f)return;const set=(name,value)=>{const el=f.elements.namedItem(name);if(el)el.value=value??''};set('id',x.id);['kind','category','name','quantity','place','time','official','market','supplier','phone'].forEach(k=>set(k,x[k]));set('description',String(x.description||'').replace(/^نوع آگهی:.*\n?/,''));$('offerSubmit').textContent='ارسال درخواست اصلاح';$('offerMessage').textContent='پس از ارسال، آگهی دوباره برای تأیید مدیریت بررسی می‌شود.';$('offerMessage').className='offer-message';$('newOffer')?.scrollIntoView({behavior:'smooth',block:'start'});$('offerName')?.focus()}
async function submitOffer(e){
 e.preventDefault();
 if(!currentProfile||!getSession()){showLogin();$("modalText").textContent="برای ثبت آگهی ابتدا وارد حساب شوید.";return}
 const form=$("offerForm");if(!form||!form.reportValidity())return;
 const values=Object.fromEntries(new FormData(form).entries()),file=$("offerPhoto")?.files?.[0]||null;
 delete values.photoFile;
 const b=$("offerSubmit"),note=$("offerMessage");b.disabled=true;b.textContent="در حال ذخیره آنلاین...";note.textContent="";
 try{
  if(file){note.textContent="در حال فشرده‌سازی و بارگذاری عکس...";values.photo=await uploadOfferImage(file)}else if(values.id){const old=(window._myOffers||[]).find(x=>String(x.id)===String(values.id));if(old&&old.photo)values.photo=old.photo}
  const result=await api("/api/offers",{method:"POST",body:JSON.stringify(values)});
  const wasEdit=Boolean(values.id);form.reset();const idField=form.elements.namedItem("id");if(idField)idField.value="";$("offerSubmit").textContent="ثبت آگهی آنلاین";const preview=$("offerPhotoPreview");if(preview){preview.removeAttribute("src");preview.hidden=true;preview.style.display="none"}
  note.textContent=wasEdit?"درخواست اصلاح با موفقیت ثبت شد؛ آگهی پس از بررسی دوباره مدیریت منتشر می‌شود.":"آگهی با موفقیت ذخیره شد و پس از تأیید مدیریت در بازار نمایش داده می‌شود.";note.className="offer-message success";await loadMyOffers();
 }catch(err){
  let message=err?.message||"ثبت آگهی انجام نشد.";
  if(err instanceof TypeError||/failed to fetch|networkerror/i.test(message))message="ارتباط مرورگر با سرور برقرار نشد. صفحه را با Ctrl+F5 تازه‌سازی کنید؛ اگر ادامه داشت، اتصال اینترنت یا دسترسی به chemlink.shop/api را بررسی می‌کنیم.";
  note.textContent=message;note.className="offer-message error";
  if(String(message).includes("وارد حساب"))showLogin();
 }finally{b.disabled=false;b.textContent=$("offerForm")?.elements?.namedItem("id")?.value?"ارسال درخواست اصلاح":"ثبت آگهی آنلاین"}
}
function wire(){document.querySelectorAll('.nav-links a,.brand[href="#home"]').forEach(link=>link.addEventListener('click',e=>{const href=link.getAttribute('href');if(localStorage.getItem('chemlinkRole')==='admin'&&href&&href.startsWith('#')){e.preventDefault();setAdminMode(false);const target=document.querySelector(href);if(target){history.replaceState(null,'',href);target.scrollIntoView({behavior:'smooth',block:'start'})}}}));$('logoutBtn')?.addEventListener('click',logoutAccount);$('loginBtn')?.addEventListener('click',e=>{e.preventDefault();if(localStorage.getItem('chemlinkRole')==='admin')loadAdminOffers();else if(currentProfile){showLogin();loadProfile(true)}else showLogin()});['searchBtn','footerLogin'].forEach(id=>{const el=$(id);if(el)el.onclick=e=>{e.preventDefault();showLogin()}});['postBtn','ctaPost','footerPost'].forEach(id=>{const el=$(id);if(el)el.onclick=e=>{e.preventDefault();if(currentProfile){$('newOffer')?.scrollIntoView({behavior:'smooth',block:'start'});$('offerName')?.focus()}else{showLogin();$('modalText').textContent='برای ثبت آگهی ابتدا با پیامک وارد حساب خود شوید.'}}});$('closeModal')?.addEventListener('click',hideLogin);$('modal')?.addEventListener('click',e=>{if(e.target===$('modal'))hideLogin()});document.querySelectorAll('.category,.filter').forEach(el=>el.addEventListener('click',e=>{if(el.classList.contains('category')){const q=$('searchInput'),cat=el.dataset.cat||el.innerText;if(q){q.value=cat;renderListings(listings.filter(x=>String(x.category||inferCategory(x.name)).includes(cat)||((x.name+' '+x.official+' '+x.market+' '+x.place+' '+x.description).toLowerCase().includes(cat.toLowerCase()))));$('listings')?.scrollIntoView({behavior:'smooth'})}}}));document.querySelectorAll('.app-top-btn.ios').forEach(el=>el.addEventListener('click',e=>{e.preventDefault();showIOSInstall()}));$('searchInput')?.addEventListener('input',()=>{const q=$('searchInput').value.trim().toLowerCase();renderListings(listings.filter(x=>(x.name+' '+x.official+' '+x.market+' '+x.place+' '+x.description).toLowerCase().includes(q)))});$('myOffersBtn')?.addEventListener('click',()=>{if(!currentProfile){showLogin();return}loadMyOffers();$('myOffers')?.scrollIntoView({behavior:'smooth',block:'start'})});$('refreshMyOffers')?.addEventListener('click',loadMyOffers);$('offerForm')?.addEventListener('submit',submitOffer);$('offerName')?.addEventListener('input',()=>{const sel=$('offerForm')?.elements?.namedItem('category'),name=$('offerName').value;if(sel&&(!sel.value||sel.value==='سایر مواد')){const guess=inferCategory(name);if(guess!=='سایر مواد')sel.value=guess}});$('offerPhoto')?.addEventListener('change',()=>{const f=$('offerPhoto').files?.[0],p=$('offerPhotoPreview');if(!p)return;if(!f){p.removeAttribute('src');p.hidden=true;p.style.display='none';return}p.src=URL.createObjectURL(f);p.hidden=false;p.style.display='block'});restoreSession();loadListings()}
window.addEventListener('error',e=>{const t=$('modalText');if(t)t.textContent='خطای صفحه: '+(e.message||'خطای ناشناخته')});
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',wire);else wire();
setInterval(()=>{if(document.visibilityState==='visible')loadListings()},30000);
