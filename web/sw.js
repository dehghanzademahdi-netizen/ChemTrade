const CACHE='chemlink-web-v7';
const ASSETS=['./','./index.html','./styles.css','./manifest.webmanifest','./icons/chemlink-192.svg','./icons/chemlink-512.svg','./icons/chemlink-180.svg','./icons/chemlink-180.png'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
  if(event.request.method!=='GET')return;
  const url=new URL(event.request.url);
  if(url.hostname.includes('firebaseio.com')||url.hostname.includes('gstatic.com')||url.pathname.endsWith('/app.js')||url.pathname.endsWith('/app-v2.js')||url.pathname.endsWith('/index.html')||url.pathname.endsWith('/sw.js'))return;
  event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request).then(response=>{const copy=response.clone();caches.open(CACHE).then(c=>c.put(event.request,copy));return response;}).catch(()=>caches.match('./index.html'))));
});