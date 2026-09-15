const DB='https://chemlink-8909b-default-rtdb.firebaseio.com';
const MANAGER_WA='989357236476';
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
    supplier:o.supplier||'',
    description:o.description||'',
    photo:o.photo||'',
    type,status,raw:o
  };
}
function extractOffers(data){
  if(Array.isArray(data))return data.map((o,i)=>[String(i),o]);
  if(data&&typeof data==='object')return Object.entries(data);
  return [];
}
function render(items=listings){
  grid.innerHTML=items.length?items.map(x=>`<article class="listing"><div class="listing-top"><span class="badge">${esc(x.type)}</span><span class="meta">آگهی اپ ChemLink</span></div>${x.photo?`<img class="listing-photo" src="${esc(x.photo)}" alt="${esc(x.name)}" loading="lazy">`:''}<h3>${esc(x.name)}</h3>${x.official?`<div class="meta">قیمت رسمی: ${esc(x.official)}</div>`:''}${x.market?`<div class="meta">قیمت بازار: ${esc(x.market)}</div>`:''}${x.place?`<div class="meta">مکان تحویل: ${esc(x.place)}</div>`:''}${x.time?`<div class="meta">زمان تحویل: ${esc(x.time)}</div>`:''}${x.description?`<div class="meta listing-desc">${esc(x.description)}</div>`:''}<div class="price"><b>${esc(x.market||x.official||'تماس برای قیمت')}</b><button class="details" data-id="${esc(x.id)}">مشاهده جزئیات و خرید ←</button></div></article>`).join(''):'<div class="listing"><h3>فعلاً آگهی تأییدشده‌ای وجود ندارد</h3><div class="meta">هر آگهی که در اپ ChemLink تأیید و منتشر شود، به‌صورت آنلاین در این بخش نمایش داده می‌شود.</div></div>';
  grid.querySelectorAll('.details').forEach(b=>b.onclick=()=>showOffer(b.dataset.id));
}
function visible(){
  const q=input.value.trim().toLowerCase();
  return listings.filter(x=>(activeType==='همه'||x.type===activeType||((activeType==='فروش'||activeType==='خرید')&&x.type.includes(activeType)))&&(!q||(x.name+' '+x.official+' '+x.market+' '+x.place+' '+x.time+' '+x.description+' '+x.type).toLowerCase().includes(q)));
}
function showOffer(id){
  const x=listings.find(v=>String(v.id)===String(id));if(!x)return;
  document.getElementById('modalTitle').textContent=x.name;
  document.getElementById('modalText').innerHTML=`<div class="offer-detail">${x.official?`<div><b>قیمت رسمی:</b> ${esc(x.official)}</div>`:''}${x.market?`<div><b>قیمت بازار:</b> ${esc(x.market)}</div>`:''}${x.place?`<div><b>مکان تحویل:</b> ${esc(x.place)}</div>`:''}${x.time?`<div><b>زمان تحویل:</b> ${esc(x.time)}</div>`:''}${x.description?`<div><b>توضیحات:</b> ${esc(x.description)}</div>`:''}<p>برای خرید و هماهنگی این آگهی با مدیریت ChemLink در ارتباط باشید.</p></div>`;
  document.getElementById('phone').style.display='none';
  document.getElementById('modalAction').textContent='ارتباط با مدیریت در واتساپ';
  document.getElementById('modalAction').onclick=()=>window.open(`https://wa.me/${MANAGER_WA}?text=${encodeURIComponent('سلام، درباره آگهی «'+x.name+'» در ChemLink اطلاعات می‌خواهم.')}`,'_blank');
  modal.classList.add('show');modal.setAttribute('aria-hidden','false');
}
async function loadCloud(){
  try{
    const r=await fetch(`${DB}/chemlink/offers.json?ts=${Date.now()}`,{cache:'no-store'});if(!r.ok)throw Error();
    const data=await r.json();
    listings=extractOffers(data).map(([key,o])=>normalize(o||{},key)).filter(x=>x.status==='APPROVED');
    render(visible());
    const active=document.querySelector('.stats-grid strong');if(active)active.textContent=listings.length.toLocaleString('fa-IR');
  }catch(e){listings=[];render([])}
}
async function loadUsers(){
  try{
    const r=await fetch(`${DB}/chemlink/usersByPhone.json?ts=${Date.now()}`,{cache:'no-store'});if(!r.ok)throw Error();
    const d=await r.json();allUsers=d?Object.values(d):[];
    const suppliers=allUsers.filter(u=>/supplier|تامین|تأمین/i.test(u.type||'')).length;
    const consumers=allUsers.filter(u=>/consumer|مصرف/i.test(u.type||'')).length;
    const vals=document.querySelectorAll('.stats-grid strong');
    if(vals[1])vals[1].textContent=suppliers.toLocaleString('fa-IR');
    if(vals[2])vals[2].textContent=consumers.toLocaleString('fa-IR');
  }catch(e){}
}
function search(){render(visible());document.getElementById('listings').scrollIntoView({behavior:'smooth'})}
document.getElementById('searchBtn').onclick=search;input.addEventListener('keydown',e=>{if(e.key==='Enter')search()});
document.querySelectorAll('.quick button').forEach(b=>b.onclick=()=>{input.value=b.dataset.q;search()});
document.querySelectorAll('.category').forEach(b=>b.onclick=()=>{input.value=b.dataset.cat;search()});
document.querySelectorAll('.filter').forEach(b=>b.onclick=()=>{document.querySelectorAll('.filter').forEach(x=>x.classList.remove('active'));b.classList.add('active');activeType=b.textContent.trim();render(visible())});
function openModal(title,text){document.getElementById('modalTitle').textContent=title;document.getElementById('modalText').textContent=text;document.getElementById('phone').style.display='';document.getElementById('modalAction').textContent='ادامه';document.getElementById('modalAction').onclick=()=>{};modal.classList.add('show');modal.setAttribute('aria-hidden','false')}
function closeModal(){modal.classList.remove('show');modal.setAttribute('aria-hidden','true')}
document.getElementById('loginBtn').onclick=()=>openModal('ورود به ChemLink','حساب وب و اپ روی زیرساخت آنلاین مشترک ChemLink هستند.');
document.getElementById('postBtn').onclick=()=>openModal('ثبت آگهی','آگهی‌های منتشرشده در سایت از همان آگهی‌های تأییدشده اپ ChemLink خوانده می‌شوند.');
document.getElementById('ctaPost').onclick=()=>openModal('ثبت آگهی','آگهی‌های منتشرشده در سایت از همان آگهی‌های تأییدشده اپ ChemLink خوانده می‌شوند.');
document.getElementById('footerLogin').onclick=e=>{e.preventDefault();openModal('ورود به ChemLink','حساب وب و اپ روی زیرساخت آنلاین مشترک ChemLink هستند.')};
document.getElementById('footerPost').onclick=e=>{e.preventDefault();openModal('ثبت آگهی','آگهی‌های منتشرشده در سایت از همان آگهی‌های تأییدشده اپ ChemLink خوانده می‌شوند.')};
document.getElementById('closeModal').onclick=closeModal;modal.onclick=e=>{if(e.target===modal)closeModal()};
loadCloud();loadUsers();setInterval(loadCloud,15000);setInterval(loadUsers,30000);
