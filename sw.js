// Service Worker — caches app shell for offline/PWA installability
const CACHE_NAME = 'hubitat-dash-v1';
const SHELL_FILES = [
  'dashboard-test.html',
  'tiles.css',
  'hubitat-api.js',
  'tile-engine.js',
  'config-ui.js',
  'tiles/light.js',
  'tiles/switch.js',
  'tiles/fan.js',
  'tiles/media.js',
  'tiles/sensor.js',
  'tiles/climate.js',
  'tiles/temp-humidity.js',
  'tiles/ups.js',
  'tiles/cover.js',
  'tiles/door.js',
  'tiles/camera.js',
  'tiles/mode.js',
  'tiles/hsm.js',
  'manifest.json'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_FILES))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // Never cache API calls or WebSocket upgrades
  if (url.pathname.startsWith('/apps/') || url.pathname.startsWith('/hub/') ||
      url.pathname.startsWith('/device/') || url.pathname.startsWith('/eventsocket') ||
      url.pathname.startsWith('/modes') || url.pathname.startsWith('/hsm')) {
    return;
  }

  // Network-first for app shell, fall back to cache
  event.respondWith(
    fetch(event.request).then((response) => {
      if (response.ok) {
        const clone = response.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
      }
      return response;
    }).catch(() => caches.match(event.request))
  );
});
