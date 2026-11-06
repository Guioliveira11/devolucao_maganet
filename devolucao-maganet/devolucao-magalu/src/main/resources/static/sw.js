// Service worker mínimo, necessário para instalar o site como app.
// Propositalmente NÃO armazena respostas em cache: a API é autenticada,
// com CSRF e dados pessoais, e nada disso deve ficar guardado no aparelho.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));
self.addEventListener('fetch', event => {
  if (event.request.method === 'GET' && event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(() => new Response(
      '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Sem conexão</title><body style="font-family:sans-serif;padding:32px;text-align:center"><h1>Sem conexão</h1><p>Conecte-se à internet e tente novamente.</p>',
      { status: 503, headers: { 'Content-Type': 'text/html; charset=utf-8' } })));
  }
});
