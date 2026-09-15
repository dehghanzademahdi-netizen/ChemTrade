const DB='https://chemlink-8909b-default-rtdb.firebaseio.com';
let listings=[];let allUsers=[];let activeType='همه';
const grid=document.getElementById('listingGrid'),input=document.getElementById('searchInput'),modal=document.getElementById('modal');
const esc=s=>String(s??'').replace(/[&<>\"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[m]));
const clean=s=>String(s??'').trim();
function normalize(o,key=''){
  const status=clean(o.status).toUpperCase();
  const type=clean(o.type||o.mode||o.offerType||'فروش');
  return {
    id:o.id||o.offerId||key||`${o.phone||o.owner||''}-${o.createdAt||o.name||Date.now()}`,
    name:o.name||o.title||o.productName||'ماده شیمیایی',
    official:o.publishedOfficial||o.official||'',
    market:o.publishedMarket||o.market||'',
    place:o.place||o.city||o.location||'',
    time:o.time||o.deliveryTime||'',
    description:o.description||'',
    type, status,
    raw:o
  };
}
function render(items=listings){
  grid.innerHTML=items.length?items.map(x=>`<article class="listing"><div class="listing-top"><span class="badge">${esc(x.type)}</span><span class="meta">${x.status==='APPROVED'?'تأیید شده':'ChemLink'}</span></div><h3>${esc(x.name)}</h3>${x.official?`<div class="meta">قیمت رسمی: ${esc(x.official)}</div>`:''}${x.market?`<div class="meta">قیمت بازار: ${esc(x.market)}</div>`:''}${x.place?`<div class="meta">مکان تحویل: ${esc(x.place)}</div>`:''}${x.time?`<div class="meta">زمان تحویل: ${esc(x.time)}</div>`:''}${x.description?`<div class="meta listing-desc">${esc(x.description)}</div>`:''}<div class="price"><b>${esc(x.market||x.official||'تماس برای قیمت')}</b><span>جزئیات ←</span></div></article>`).join(''):'<div class="listing"><h3>آگهی تأییدشده‌ای وجود ندارد</h3><div class="meta">آگهی‌های سایت مستقیماً از آگهی‌های تأییدشده اپ ChemLink خوانده می‌شوند.</div></div>';
}
function visible(){
  const q=input.value.trim().toLowerCase();
  return listings.filter(x=>(activeType==='همه'||x.type===activeType||((activeType==='فروش'||activeType==='خرید')&&x.type.includes(activeType)))&&(!q||(x.name+' '+x.official+' '+x.market+' '+x.place+' '+x.time+' '+x.description+' '+x.type).toLowerCase().includes(q)));
}
async function loadCloud(){
  try{
    const r=await fetch(`${DB}/chemlink/offers.json?ts=${Date.now()}`,{cache:'no-store'});if(!r.ok)throw Error();
    const data=await r.json();
    const entries=Array.isArray(data)?data.map((o,i)=>[String(i),o]):Object.entries(data||{});
    listings=entries.map(([key,o])=>normalize(o||{},key)).filter(x=>x.status==='APPROVED');
    render(visible());
    const active=document.querySelector('.stats-grid strong');if(active)active.textContent=`${listings.length.toLocaleString('fa-IR')}+`;
  }catch(e){
    listings=[];render([]);
  }
}
async function loadUsers(){
  try{
    const r=await fetch(`${DB}/chemlink/usersByPhone.json?ts=${Date.now()}`,{cache:'no-store'});if(!r.ok)throw Error();
    const d=await r.json();allUsers=d?Object.values(d):[];
    const suppliers=allUsers.filter(u=>/supplier|تامین|تأمین/i.test(u.type||'')).length;
    const consumers=allUsers.filter(u=>/consumer|مصرف/i.test(u.type||'')).length;
    const vals=document.querySelectorAll('.stats-grid strong');
    if(vals[1])vals[1].textContent=`${suppliers.toLocaleString('fa-IR')}+`;
    if(vals[2])vals[2].textContent=`${consumers.toLocaleString('fa-IR')}+`;
  }catch(e){}
}
function search(){render(visible());document.getElementById('listings').scrollIntoView({behavior:'smooth'});}
document.getElementById('searchBtn').onclick=search;input.addEventListener('keydown',e=>{if(e.key==='Enter')search()});
document.querySelectorAll('.quick button').forEach(b=>b.onclick=()=>{input.value=b.dataset.q;search()});
document.querySelectorAll('.category').forEach(b=>b.onclick=()=>{input.value=b.dataset.cat;search()});
document.querySelectorAll('.filter').forEach(b=>b.onclick=()=>{document.querySelectorAll('.filter').forEach(x=>x.classList.remove('active'));b.classList.add('active');activeType=b.textContent.trim();render(visible())});
function openModal(title,text){document.getElementById('modalTitle').textContent=title;document.getElementById('modalText').textContent=text;modal.classList.add('show');modal.setAttribute('aria-hidden','false')}function closeModal(){modal.classList.remove('show');modal.setAttribute('aria-hidden','true')}
document.getElementById('loginBtn').onclick=()=>openModal('ورود به ChemLink','ورود وب با حساب آنلاین ChemLink در حال اتصال به همان حساب اپ است.');document.getElementById('postBtn').onclick=()=>openModal('ثبت آگهی','ثبت آگهی وب در مرحله بعد به همان حساب و گردش تأیید اپ متصل می‌شود.');document.getElementById('ctaPost').onclick=()=>openModal('ثبت آگهی','ثبت آگهی وب در مرحله بعد به همان حساب و گردش تأیید اپ متصل می‌شود.');document.getElementById('footerLogin').onclick=e=>{e.preventDefault();openModal('ورود به ChemLink','ورود وب با حساب آنلاین ChemLink در حال اتصال به همان حساب اپ است.')};document.getElementById('footerPost').onclick=e=>{e.preventDefault();openModal('ثبت آگهی','ثبت آگهی وب در مرحله بعد به همان حساب و گردش تأیید اپ متصل می‌شود.')};document.getElementById('closeModal').onclick=closeModal;modal.onclick=e=>{if(e.target===modal)closeModal()};
loadCloud();loadUsers();setInterval(loadCloud,15000);setInterval(loadUsers,30000);
