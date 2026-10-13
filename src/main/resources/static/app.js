// Site de devoluções: cliente solicita e acompanha, funcionário recebe na loja.
// Tudo roda numa página só; cada "tela" é uma função que devolve HTML.

const $ = (selector) => document.querySelector(selector);

const esc = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));


// ---------------------------------------------------------------------------
// Textos e ícones
// ---------------------------------------------------------------------------

const ICONS = {
  box: '<path d="m3 7 9-5 9 5v10l-9 5-9-5Z"/><path d="m3 7 9 5 9-5M12 12v10M7 4.8l9 5"/>',
  returns: '<path d="M7 7h8a6 6 0 0 1 0 12h-4M7 7l4-4M7 7l4 4M4 15v4M2 17h4"/>',
  bell: '<path d="M5 9a7 7 0 0 1 14 0v6l2 3H3l2-3ZM10 21h4"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  qr: '<path d="M3 3h6v6H3ZM15 3h6v6h-6ZM3 15h6v6H3ZM15 15h2v2h-2ZM21 14v4h-3v3M14 21h1"/>',
  list: '<path d="M9 5h12M9 12h12M9 19h12M3 5h1M3 12h1M3 19h1"/>',
  pin: '<path d="M19 9c0 5-7 12-7 12S5 14 5 9a7 7 0 0 1 14 0Z"/><circle cx="12" cy="9" r="2"/>',
  refresh: '<path d="M20 7a9 9 0 1 0 1 8M20 2v5h-5"/>',
  arrow: '<path d="M19 12H5M10 7l-5 5 5 5"/>',
  camera: '<path d="M8 5 10 2h4l2 3h5v16H3V5Z"/><circle cx="12" cy="12" r="4"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
};

const icon = (name) =>
  `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONS[name] || ICONS.box}</svg>`;

const STATUS_NAMES = {
  AGUARDANDO_ESCANEAMENTO: 'Aguardando entrega',
  QR_EXPIRADO: 'QR expirado',
  EM_ANALISE: 'Em análise',
  DESTINO_PENDENTE: 'Destino em processamento',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
};

const STATUS_COLORS = {
  CONCLUIDA: 'green',
  CANCELADA: 'gray',
  QR_EXPIRADO: 'red',
  DESTINO_PENDENTE: 'orange',
};

const DESTINATIONS = {
  ESTOQUE: 'Estoque',
  REPARO: 'Reparo',
  DOACAO_ONG: 'Doação para ONG',
};

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const isValidToken = (token) => UUID_PATTERN.test(token);


// ---------------------------------------------------------------------------
// Estado da página
// ---------------------------------------------------------------------------

const state = {
  user: null,
  view: 'itens',
  params: {},
  items: [],
  stores: [],
  selectedItem: null,
  unread: 0,
  // Muda a cada login/logout. Respostas que chegam depois disso são descartadas.
  epoch: 0,
};

let csrf = null;
let renderCount = 0;
let cameraStream = null;
let cameraTimer = null;
let decoderPromise = null;
let toastTimer = null;


// ---------------------------------------------------------------------------
// Formatação
// ---------------------------------------------------------------------------

function formatDate(value) {
  if (!value) return 'Não informado';
  return value.slice(0, 10).split('-').reverse().join('/');
}

function formatDateTime(value) {
  if (!value) return 'Não informado';
  return `${formatDate(value)} às ${value.slice(11, 16)}`;
}

function formatMoney(value) {
  if (value == null) return 'Valor não informado';
  return Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

const plural = (n, one, many) => `${n} ${n === 1 ? one : many}`;

function toQueryString(params) {
  const filled = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  return new URLSearchParams(filled).toString();
}

// O servidor só grava QR_EXPIRADO quando o job de expiração roda (a cada 5 min).
// Até lá, a data de validade já diz que o QR venceu, então mostramos isso na hora.
function effectiveStatus(devolucao) {
  const expired = devolucao.qrExpiraEm && new Date(devolucao.qrExpiraEm) <= Date.now();
  if (devolucao.status === 'AGUARDANDO_ESCANEAMENTO' && expired) return 'QR_EXPIRADO';
  return devolucao.status;
}


// ---------------------------------------------------------------------------
// Pedaços de HTML reaproveitados
// ---------------------------------------------------------------------------

const statusTag = (status) =>
  `<span class="tag ${STATUS_COLORS[status] || ''}">${esc(STATUS_NAMES[status] || status)}</span>`;

function selectOptions(values, selected, firstLabel = 'Todos') {
  const rest = Object.entries(values).map(([value, label]) =>
    `<option value="${value}" ${value === selected ? 'selected' : ''}>${esc(label)}</option>`);
  return `<option value="">${esc(firstLabel)}</option>` + rest.join('');
}

function chips(values, selected, firstLabel, action = 'chip') {
  return [['', firstLabel], ...Object.entries(values)]
    .map(([value, label]) => `
      <button type="button" class="chip" data-action="${action}" data-value="${esc(value)}"
              aria-pressed="${value === (selected || '')}">${esc(label)}</button>`)
    .join('');
}

const emptyState = (title, text, action = '') => `
  <div class="empty">
    ${icon('box')}
    <h2>${esc(title)}</h2>
    <p>${esc(text)}</p>
    ${action}
  </div>`;

function pageHeading(eyebrow, title, text, withRefresh = true) {
  const refresh = withRefresh
    ? `<button class="button outline" data-action="refresh" aria-label="Atualizar página">${icon('refresh')} Atualizar</button>`
    : '';
  return `
    <div class="page-heading">
      <div>
        <span class="eyebrow">${esc(eyebrow)}</span>
        <h1>${esc(title)}</h1>
        <p class="muted">${esc(text)}</p>
      </div>
      ${refresh}
    </div>`;
}

const backButton = (view, label = 'Voltar') =>
  `<button class="back-button" data-action="navigate" data-view="${view}">${icon('arrow')}${esc(label)}</button>`;

const navigateButton = (view, label, style = 'primary') =>
  `<button class="button ${style}" data-action="navigate" data-view="${view}">${label}</button>`;

function pagination(page, action = 'page') {
  if (!page.total) return '';
  const totalPages = Math.max(1, Math.ceil(page.total / page.tamanho));
  const isFirst = page.pagina === 0;
  const isLast = (page.pagina + 1) * page.tamanho >= page.total;
  return `
    <div class="pagination">
      <button class="button outline" data-action="${action}" data-page="${page.pagina - 1}" ${isFirst ? 'disabled' : ''}>Anterior</button>
      <span>Página ${page.pagina + 1} de ${totalPages}<br>${plural(page.total, 'registro', 'registros')}</span>
      <button class="button outline" data-action="${action}" data-page="${page.pagina + 1}" ${isLast ? 'disabled' : ''}>Próxima</button>
    </div>`;
}

function progressStepper(status) {
  if (status === 'CANCELADA') return '';

  const labels = ['Solicitada', 'Entregue na loja', 'Em análise', 'Concluída'];
  const stepByStatus = { AGUARDANDO_ESCANEAMENTO: 0, QR_EXPIRADO: 0, EM_ANALISE: 2, DESTINO_PENDENTE: 2, CONCLUIDA: 3 };
  const current = stepByStatus[status] ?? 0;
  const finished = status === 'CONCLUIDA';

  const steps = labels.map((label, i) => {
    const done = i < current || finished;
    const isCurrent = i === current && !finished;
    return `
      <li class="${done ? 'done' : isCurrent ? 'current' : ''}" ${isCurrent ? 'aria-current="step"' : ''}>
        <span class="step-dot" aria-hidden="true">${done ? icon('check') : i + 1}</span>
        <span class="step-label">${label}</span>
      </li>`;
  });

  return `<ol class="stepper" aria-label="Progresso da devolução">${steps.join('')}</ol>`;
}


// ---------------------------------------------------------------------------
// Comunicação com a API
// ---------------------------------------------------------------------------

async function fetchCsrfToken() {
  const response = await fetch('/api/v1/csrf', { credentials: 'same-origin', cache: 'no-store', redirect: 'error' });
  if (!response.ok) throw new Error('Não foi possível preparar a sessão. Tente novamente.');
  return response.json();
}

function friendlyErrorMessage(status) {
  if (status === 401) return 'Sua sessão terminou. Entre novamente.';
  if (status === 403) return 'Você não tem permissão ou sua sessão precisa ser atualizada.';
  if (status === 404) return 'Registro não encontrado.';
  if (status >= 500) return 'Não foi possível concluir agora. Tente novamente.';
  return 'Verifique os dados e tente novamente.';
}

async function api(path, { method = 'GET', body } = {}) {
  const epochAtStart = state.epoch;
  const headers = { Accept: 'application/json' };
  const isForm = body instanceof URLSearchParams;

  // O CSRF continua ligado no servidor: toda escrita precisa do token.
  if (method !== 'GET') {
    if (!csrf) csrf = await fetchCsrfToken();
    headers[csrf.headerName] = csrf.token;
  }

  let payload;
  if (body !== undefined) {
    headers['Content-Type'] = isForm ? 'application/x-www-form-urlencoded;charset=UTF-8' : 'application/json';
    payload = isForm ? body.toString() : JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method, headers, body: payload, credentials: 'same-origin', cache: 'no-store', redirect: 'error',
    });
  } catch {
    throw new Error('Não foi possível conectar. Confira sua conexão e tente novamente.');
  }

  if (epochAtStart !== state.epoch) {
    const error = new Error('Sessão alterada');
    error.stale = true;
    throw error;
  }

  const data = response.status === 204 ? null : await response.json().catch(() => null);

  if (!response.ok) {
    const serverMessage = data?.mensagem || (response.status < 500 ? data?.detail : null);
    const error = new Error(serverMessage || friendlyErrorMessage(response.status));
    error.status = response.status;
    error.fromServer = Boolean(serverMessage);
    if (response.status === 403) csrf = null;
    throw error;
  }

  return data;
}


// ---------------------------------------------------------------------------
// Estrutura da página: login, menu, navegação
// ---------------------------------------------------------------------------

function toast(message, isError = false) {
  clearTimeout(toastTimer);
  const el = $('#toast');
  el.textContent = message;
  el.classList.toggle('error', isError);
  el.hidden = false;
  toastTimer = setTimeout(() => { el.hidden = true; }, 5500);
}

function showLogin(message = '') {
  stopCamera();
  renderCount++;
  state.epoch++;
  state.user = null;
  state.unread = 0;
  state.items = [];
  state.stores = [];
  state.selectedItem = null;
  csrf = null;

  $('#app').hidden = true;
  $('#login-screen').hidden = false;
  $('#initial-loading').hidden = true;
  $('#content').replaceChildren();
  $('#login-error').textContent = message;
  $('#login-error').hidden = !message;
  $('#password').value = '';

  for (const dialog of [$('#confirm-dialog'), $('#idle-dialog')]) {
    if (dialog?.open) dialog.close('cancel');
  }
}

function handleError(error) {
  if (error.stale) return;
  if (error.status === 401) {
    showLogin('Sua sessão terminou. Entre novamente.');
    return;
  }
  toast(error.message, true);
}

const CLIENT_MENU = [
  ['itens', 'Compras', 'box'],
  ['devolucoes', 'Devoluções', 'returns'],
  ['avisos', 'Avisos', 'bell'],
  ['conta', 'Conta', 'user'],
];

const EMPLOYEE_MENU = [
  ['fila', 'Fila da loja', 'list'],
  ['receber', 'Receber', 'qr'],
  ['conta', 'Conta', 'user'],
];

function renderNav() {
  if (!state.user) return;

  const menu = state.user.perfil === 'CLIENTE' ? CLIENT_MENU : EMPLOYEE_MENU;
  // Telas internas acendem o item de menu de onde vieram.
  const active = { lojas: 'itens', detalhe: 'devolucoes' }[state.view] || state.view;

  $('#navigation').innerHTML = menu.map(([view, label, glyph]) => {
    const counter = view === 'avisos' && state.unread
      ? `<span class="nav-count" aria-label="${state.unread} não lidas">${state.unread > 99 ? '99+' : state.unread}</span>`
      : '';
    return `
      <button class="nav-button" type="button" data-view="${view}" ${view === active ? 'aria-current="page"' : ''}>
        ${icon(glyph)}<span>${label}</span>${counter}
      </button>`;
  }).join('');
}

async function refreshUnreadCount() {
  if (state.user?.perfil !== 'CLIENTE') return;
  try {
    const data = await api('/notificacoes?naoLidas=true&pagina=0&tamanho=1');
    if (state.user?.perfil === 'CLIENTE') {
      state.unread = data.total;
      renderNav();
    }
  } catch {
    // Se falhar, a própria tela principal já mostra o erro de sessão ou conexão.
  }
}

async function enter(user) {
  state.user = user;
  markActivity();

  $('#login-screen').hidden = true;
  $('#app').hidden = false;
  $('#initial-loading').hidden = true;
  $('#header-name').textContent = user.nome;
  $('#header-search').hidden = user.perfil !== 'CLIENTE';

  await go(user.perfil === 'CLIENTE' ? 'itens' : 'fila');
}

const LOADING_HTML = `
  <div class="loading" role="status"><span class="sr-only">Carregando…</span></div>
  <div class="cards skeleton-list" aria-hidden="true">
    <div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div>
  </div>`;

async function go(view, params = {}) {
  if (!state.user) return;

  stopCamera();
  state.view = view;
  state.params = { ...params };
  const thisRender = ++renderCount;

  const content = $('#content');
  renderNav();
  content.setAttribute('aria-busy', 'true');
  content.innerHTML = LOADING_HTML;

  try {
    const render = VIEWS[view] || itemsView;
    const html = await render(params);
    // O usuário pode ter trocado de tela enquanto esperava a resposta.
    if (thisRender !== renderCount) return;

    content.innerHTML = html;
    content.removeAttribute('aria-busy');
    content.focus({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: 'instant' });
    refreshUnreadCount();
  } catch (error) {
    if (thisRender !== renderCount || error.stale) return;
    content.removeAttribute('aria-busy');

    if (error.status === 401) {
      handleError(error);
      return;
    }

    content.innerHTML = `
      ${pageHeading('VAMOS TENTAR DE NOVO', 'Não foi possível carregar', 'Seus dados não foram alterados.', false)}
      <div class="error-message" role="alert">${esc(error.message)}</div>
      <button class="button primary" data-action="refresh">Tentar novamente</button>`;
  }
}


// ---------------------------------------------------------------------------
// Telas do cliente
// ---------------------------------------------------------------------------

// A API não tem busca de itens, então buscamos algumas páginas e filtramos aqui.
async function searchItems(term) {
  const all = [];
  let pagina = 0;
  let page;
  do {
    page = await api(`/itens?${toQueryString({ pagina, tamanho: 20 })}`);
    all.push(...page.itens);
    pagina++;
  } while (pagina * page.tamanho < page.total && pagina < 10);

  const needle = term.toLowerCase();
  const found = all.filter((i) => `${i.produto} ${i.numeroPedido}`.toLowerCase().includes(needle));
  return { itens: found, total: found.length, pagina: 0, tamanho: Math.max(1, found.length) };
}

function itemCard(item) {
  const action = item.elegivelParaDevolucao
    ? `<button class="button primary full" data-action="select-item" data-id="${item.id}">Solicitar devolução <span aria-hidden="true">→</span></button>`
    : '<p class="small-note">O prazo pode ter terminado ou já existir uma devolução. Consulte a aba Devoluções.</p>';

  return `
    <article class="card">
      <div class="card-header">
        <div class="item-icon">${icon('box')}</div>
        <span class="tag ${item.elegivelParaDevolucao ? 'green' : 'gray'}">
          ${item.elegivelParaDevolucao ? 'Disponível para devolução' : 'Indisponível'}
        </span>
      </div>
      <div>
        <h3>${esc(item.produto)}</h3>
        <p class="subtitle">Pedido ${esc(item.numeroPedido)} · ${plural(item.quantidade, 'unidade', 'unidades')}</p>
      </div>
      <div class="price">${esc(formatMoney(item.valorPago))}</div>
      <div class="deadline">Prazo para solicitar: <strong>${esc(formatDate(item.dataLimiteDevolucao))}</strong></div>
      ${action}
    </article>`;
}

async function itemsView({ pagina = 0, busca = '' } = {}) {
  const page = busca
    ? await searchItems(busca)
    : await api(`/itens?${toQueryString({ pagina, tamanho: 12 })}`);
  state.items = page.itens;

  const list = page.itens.length
    ? `<div class="cards">${page.itens.map(itemCard).join('')}</div>`
    : emptyState('Nenhuma compra por aqui', 'Seus itens aparecerão aqui quando estiverem vinculados à sua conta.');

  return pageHeading('SUAS COMPRAS', 'O que vamos devolver?', 'Escolha um produto e encontre uma loja para entregar.')
    + list
    + pagination(page);
}

function returnCard(devolucao) {
  return `
    <article class="card">
      <div class="card-header">${statusTag(devolucao.status)}<span class="subtitle">#${devolucao.id}</span></div>
      <div>
        <h3>${esc(devolucao.produto)}</h3>
        <p class="subtitle">Pedido ${esc(devolucao.numeroPedido)}</p>
      </div>
      <div class="item-meta"><span class="muted">Loja</span><strong>${esc(devolucao.loja)}</strong></div>
      <div class="deadline">Solicitada em ${esc(formatDate(devolucao.criadaEm))}</div>
      <button class="button secondary full" data-action="detail" data-id="${devolucao.id}">
        Acompanhar devolução ${icon('returns')}
      </button>
    </article>`;
}

async function returnsView({ pagina = 0, status = '' } = {}) {
  const page = await api(`/devolucoes?${toQueryString({ pagina, tamanho: 12, status })}`);
  const devolucoes = page.itens.map((d) => ({ ...d, status: effectiveStatus(d) }));

  const list = devolucoes.length
    ? `<div class="cards">${devolucoes.map(returnCard).join('')}</div>`
    : emptyState('Nenhuma devolução encontrada', 'Altere o filtro ou solicite a devolução de uma compra.',
        navigateButton('itens', 'Ver compras'));

  return pageHeading('PASSO A PASSO', 'Suas devoluções', 'Acompanhe o caminho de cada produto.')
    + `<div class="chips" role="group" aria-label="Filtrar por status">${chips(STATUS_NAMES, status, 'Todas')}</div>`
    + list
    + pagination(page);
}

function storeCard(loja) {
  const distance = loja.distanciaKm != null
    ? `<p>${Number(loja.distanciaKm).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} km de distância</p>`
    : '';
  const hours = loja.horarioFuncionamento ? `<p>${esc(loja.horarioFuncionamento)}</p>` : '';

  return `
    <article class="card store-card">
      <div class="item-icon">${icon('pin')}</div>
      <div class="store-text">
        <h3>${esc(loja.nome)}</h3>
        <p>${esc(loja.endereco || 'Endereço não informado')}</p>
        ${distance}
        ${hours}
      </div>
      <button class="button outline" data-action="create-return" data-id="${loja.id}">Escolher loja</button>
    </article>`;
}

async function storesView({ pagina = 0, busca = '', nearby } = {}) {
  if (!state.selectedItem) {
    return emptyState('Escolha primeiro um produto', 'Volte às compras para iniciar uma devolução.',
      navigateButton('itens', 'Ver compras'));
  }

  const page = nearby
    ? { itens: nearby, total: nearby.length, pagina: 0, tamanho: Math.max(1, nearby.length) }
    : await api(`/lojas?${toQueryString({ pagina, tamanho: 10, busca })}`);
  state.stores = page.itens;

  const list = page.itens.length
    ? `<div class="store-list">${page.itens.map(storeCard).join('')}</div>`
    : emptyState('Nenhuma loja encontrada', 'Tente outro nome ou endereço.');

  return backButton('itens', 'Voltar às compras')
    + pageHeading('ESCOLHA ONDE ENTREGAR', 'Uma loja perto de você', state.selectedItem.produto, false)
    + `
      <form class="search-form" id="store-search">
        <input name="busca" aria-label="Nome ou endereço da loja" value="${esc(busca)}" maxlength="120"
               placeholder="Nome da loja, bairro ou endereço">
        <button class="button primary">Buscar</button>
      </form>
      <div class="store-actions">
        <p class="muted">Ou encontre lojas num raio de 20 km.</p>
        <button class="button secondary" data-action="nearby">${icon('pin')} Usar minha localização</button>
      </div>
      <div class="notice">A devolução inclui todas as unidades deste item. Leve o produto à loja escolhida e apresente o QR.</div>`
    + list
    + (nearby ? '' : pagination(page));
}

function infoRow(label, value) {
  return `<div><dt>${label}</dt><dd>${value}</dd></div>`;
}

function qrSection(detail, qr, beforeDelivery) {
  if (qr.qrCodeImagemBase64) {
    return `
      <img src="data:image/png;base64,${esc(qr.qrCodeImagemBase64)}" alt="QR para entregar esta devolução na loja" width="230" height="230">
      <p>Mostre este QR ao funcionário da loja.<br>Válido até ${esc(formatDateTime(qr.qrCodeExpiraEm))}.</p>
      <p class="countdown" data-expires="${esc(qr.qrCodeExpiraEm)}"></p>`;
  }

  let message = 'O QR não é mais necessário. Acompanhe as próximas etapas abaixo.';
  if (beforeDelivery) message = 'O QR expirou. Você pode tentar renová-lo dentro do prazo da compra.';
  else if (detail.status === 'CANCELADA') message = 'Esta solicitação foi cancelada.';

  return `<div class="item-icon">${icon(beforeDelivery ? 'qr' : 'check')}</div><p>${message}</p>`;
}

async function detailView({ id, histPagina = 0 } = {}) {
  const [detail, qr, history] = await Promise.all([
    api(`/devolucoes/${id}/acompanhamento`),
    api(`/devolucoes/${id}`),
    api(`/devolucoes/${id}/historico?pagina=${histPagina}&tamanho=10`),
  ]);
  detail.status = effectiveStatus(detail);
  const beforeDelivery = ['AGUARDANDO_ESCANEAMENTO', 'QR_EXPIRADO'].includes(detail.status);

  const info = [
    infoRow('Loja de entrega', `<strong>${esc(detail.loja)}</strong><br>${esc(detail.enderecoLoja || 'Endereço não informado')}`),
    infoRow('Quantidade', plural(detail.quantidade, 'unidade', 'unidades')),
    infoRow('Solicitada em', esc(formatDateTime(detail.criadaEm))),
    detail.recebidaEm ? infoRow('Recebida na loja', esc(formatDateTime(detail.recebidaEm))) : '',
    detail.destino ? infoRow('Destino do produto', esc(DESTINATIONS[detail.destino] || detail.destino)) : '',
    detail.concluidaEm ? infoRow('Concluída em', esc(formatDateTime(detail.concluidaEm))) : '',
  ].join('');

  let actions = '';
  if (beforeDelivery) {
    const renew = qr.qrCodeImagemBase64
      ? ''
      : `<button class="button primary" data-action="renew" data-id="${id}">${icon('refresh')} Renovar QR</button>`;
    actions = `
      <div class="detail-actions">
        ${renew}
        <button class="button danger" data-action="cancel" data-id="${id}">Cancelar solicitação</button>
      </div>`;
  }

  const timeline = history.itens.length
    ? `<ol class="timeline">${history.itens.map((e) => `
        <li><p>${esc(e.mensagem)}</p><time>${esc(formatDateTime(e.ocorridoEm))}</time></li>`).join('')}
      </ol>`
    : '<p class="muted">Ainda não há eventos registrados.</p>';

  return backButton('devolucoes', 'Todas as devoluções')
    + pageHeading(`DEVOLUÇÃO #${id}`, detail.produto, `Pedido ${detail.numeroPedido}`, false)
    + progressStepper(detail.status)
    + `
      <div class="detail-grid">
        <section class="card">
          ${statusTag(detail.status)}
          <dl class="detail-info">${info}</dl>
        </section>
        <section class="card qr-card">
          <h3>${beforeDelivery ? 'Seu QR de entrega' : 'Recebimento'}</h3>
          ${qrSection(detail, qr, beforeDelivery)}
        </section>
      </div>`
    + actions
    + `
      <section class="timeline-section">
        <h2 class="section-title">HISTÓRICO DA DEVOLUÇÃO</h2>
        ${timeline}
        ${pagination(history, 'history-page')}
      </section>`;
}

function notificationCard(n) {
  const isRead = Boolean(n.lidaEm);
  const deadline = n.qrExpiraEm
    ? `<p class="muted">Prazo deste lembrete: ${esc(formatDateTime(n.qrExpiraEm))}.</p>`
    : '';
  const markRead = isRead
    ? ''
    : `<button class="text-button" data-action="read" data-id="${n.id}">Marcar como lida</button>`;

  return `
    <article class="card notification-card ${isRead ? '' : 'unread'}">
      <div class="notification-top">
        <span class="tag ${isRead ? 'gray' : ''}">${isRead ? 'Lida' : 'Nova'}</span>
        <time>${esc(formatDateTime(n.criadaEm))}</time>
      </div>
      <p>${esc(n.mensagem)}</p>
      ${deadline}
      <div class="notification-actions">
        <button class="button secondary" data-action="detail" data-id="${n.solicitacaoId}">Ver devolução</button>
        ${markRead}
      </div>
    </article>`;
}

async function notificationsView({ pagina = 0, naoLidas = false } = {}) {
  const page = await api(`/notificacoes?${toQueryString({ pagina, tamanho: 12, naoLidas })}`);

  const filter = `
    <div class="chips" role="group" aria-label="Filtrar avisos">
      <button type="button" class="chip" data-action="chip-unread" data-value="" aria-pressed="${!naoLidas}">Todos</button>
      <button type="button" class="chip" data-action="chip-unread" data-value="1" aria-pressed="${!!naoLidas}">Não lidos</button>
    </div>`;

  const list = page.itens.length
    ? `<div class="notification-list">${page.itens.map(notificationCard).join('')}</div>`
    : emptyState('Tudo em dia', 'Quando houver uma atualização, você poderá acompanhá-la aqui.');

  return pageHeading('FIQUE POR DENTRO', 'Seus avisos', 'Atualizações da devolução e lembretes do QR.')
    + filter
    + list
    + pagination(page);
}

async function accountView() {
  const user = state.user;
  const isClient = user.perfil === 'CLIENTE';
  const initial = esc((user.nome || '?').slice(0, 1).toUpperCase());

  let body = '<p class="muted">Seu acesso permite acompanhar e receber devoluções vinculadas à sua loja.</p>';
  if (isClient) {
    const prefs = await api('/notificacoes/preferencias');
    body = `
      <form id="preferences-form">
        <label class="preference">
          <div>Atualizações da devolução<p>Recebimento, mudança de destino e outras etapas.</p></div>
          <input name="atualizacoes" type="checkbox" ${prefs.atualizacoes ? 'checked' : ''}>
        </label>
        <label class="preference">
          <div>Lembretes de QR<p>Um aviso aqui no site quando o QR estiver perto de vencer.</p></div>
          <input name="lembretesQr" type="checkbox" ${prefs.lembretesQr ? 'checked' : ''}>
        </label>
        <button class="button primary full" type="submit">Salvar preferências</button>
      </form>
      <p class="small-note">Os avisos ficam na aba Avisos. Não são enviados por e-mail ou por notificação do celular.</p>`;
  }

  return pageHeading('DO SEU JEITO', 'Sua conta', 'Confira seu acesso e personalize os avisos.', false) + `
    <section class="card account-card">
      <div class="account-intro">
        <div class="account-avatar" aria-hidden="true">${initial}</div>
        <div>
          <h3>${esc(user.nome)}</h3>
          <p>${isClient ? 'Cliente' : `Funcionário · ${esc(user.loja)}`}</p>
        </div>
      </div>
      ${body}
    </section>`;
}


// ---------------------------------------------------------------------------
// Telas do funcionário
// ---------------------------------------------------------------------------

function waitingTime(minutes) {
  return minutes < 60 ? `${minutes} min de espera` : `${Math.floor(minutes / 60)} h de espera`;
}

function queueCard(d) {
  const received = d.recebidaEm
    ? `Recebida em ${esc(formatDateTime(d.recebidaEm))}<br>${waitingTime(d.minutosDesdeRecebimento)}`
    : 'Recebimento sem data registrada';

  const footer = d.status === 'EM_ANALISE'
    ? `
      <form class="queue-actions" data-destination="${d.id}">
        <label class="sr-only" for="dest-${d.id}">Destino da devolução #${d.id}</label>
        <select id="dest-${d.id}" name="destino" required>${selectOptions(DESTINATIONS, '', 'Escolha o destino')}</select>
        <button class="button primary">Definir</button>
      </form>`
    : `<p class="small-note">Destino: <strong>${esc(DESTINATIONS[d.destino] || d.destino)}</strong><br>Aguardando processamento.</p>`;

  return `
    <article class="card">
      <div class="card-header">${statusTag(d.status)}<span class="subtitle">#${d.id}</span></div>
      <div>
        <h3>${esc(d.produto)}</h3>
        <p class="subtitle">Pedido ${esc(d.numeroPedido)} · ${d.quantidade} un.</p>
      </div>
      <p class="queue-time">${received}</p>
      ${footer}
    </article>`;
}

async function queueView({ pagina = 0, status = '', destino = '', minimoHoras = '' } = {}) {
  const page = await api(`/funcionario/devolucoes?${toQueryString({ pagina, tamanho: 12, status, destino, minimoHoras })}`);

  const stageOptions = { EM_ANALISE: 'Em análise', DESTINO_PENDENTE: 'Destino pendente' };
  const ageOptions = { 2: 'Mais de 2 horas', 24: 'Mais de 24 horas', 48: 'Mais de 48 horas' };

  const filters = `
    <form class="filters" id="queue-filter">
      <div class="field">
        <label for="queue-status">Etapa</label>
        <select id="queue-status" name="status">${selectOptions(stageOptions, status)}</select>
      </div>
      <div class="field">
        <label for="queue-dest">Destino</label>
        <select id="queue-dest" name="destino">${selectOptions(DESTINATIONS, destino)}</select>
      </div>
      <div class="field">
        <label for="queue-age">Tempo de espera</label>
        <select id="queue-age" name="minimoHoras">${selectOptions(ageOptions, String(minimoHoras), 'Qualquer tempo')}</select>
      </div>
      <button class="button secondary">Filtrar</button>
    </form>`;

  const list = page.itens.length
    ? `<div class="cards">${page.itens.map(queueCard).join('')}</div>`
    : emptyState('Fila em dia', 'Não há devoluções para os filtros selecionados.',
        navigateButton('receber', 'Receber um produto'));

  return pageHeading(state.user.loja || 'SUA LOJA', 'Fila de devoluções', 'As entregas mais antigas aparecem primeiro.')
    + filters
    + list
    + pagination(page);
}

async function receiveView() {
  return pageHeading('ATENDIMENTO NA LOJA', 'Receber um produto', 'Leia o QR apresentado pelo cliente ou cole o código.', false) + `
    <div class="scan-options">
      <button class="button primary" data-action="camera">${icon('camera')} Abrir câmera</button>
      <button class="button outline" data-action="stop-camera" hidden>Fechar câmera</button>
    </div>
    <div id="camera-box" class="camera-box" hidden>
      <video id="camera-video" playsinline muted></video>
      <div class="camera-hint" aria-hidden="true"></div>
    </div>
    <p id="camera-status" class="notice" role="status" hidden></p>
    <form id="receive-form" class="scan-form">
      <label for="return-token">Código da devolução</label>
      <input id="return-token" name="token" required maxlength="36" autocapitalize="none" spellcheck="false"
             autocomplete="off" placeholder="Cole o código do QR">
      <p class="muted">Confira o produto antes de confirmar. O recebimento inicia a análise e desativa o QR.</p>
      <button class="button primary full">Confirmar recebimento</button>
    </form>
    <div id="receipt-result" class="receipt-result" role="status"></div>`;
}

const VIEWS = {
  itens: itemsView,
  devolucoes: returnsView,
  lojas: storesView,
  detalhe: detailView,
  avisos: notificationsView,
  conta: accountView,
  fila: queueView,
  receber: receiveView,
};


// ---------------------------------------------------------------------------
// Ações (cliques e formulários)
// ---------------------------------------------------------------------------

function askConfirmation(title, description, confirmLabel = 'Confirmar') {
  const dialog = $('#confirm-dialog');
  $('#confirm-title').textContent = title;
  $('#confirm-description').textContent = description;
  $('#confirm-action').textContent = confirmLabel;

  if (typeof dialog.showModal !== 'function') {
    return Promise.resolve(window.confirm(`${title}\n\n${description}`));
  }
  return new Promise((resolve) => {
    dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
    dialog.showModal();
  });
}

// Trava o botão enquanto a ação roda, evitando cliques duplos.
async function whileBusy(button, work) {
  if (button.disabled) return;
  button.disabled = true;
  button.setAttribute('aria-busy', 'true');
  try {
    await work();
  } catch (error) {
    handleError(error);
  } finally {
    button.disabled = false;
    button.removeAttribute('aria-busy');
  }
}

async function getCurrentPosition() {
  if (!navigator.geolocation) throw new Error('Localização indisponível. Busque pelo nome ou endereço.');
  try {
    return await new Promise((resolve, reject) =>
      navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 12000, maximumAge: 60000 }));
  } catch {
    throw new Error('Não foi possível acessar a localização. Você pode buscar pelo nome ou endereço.');
  }
}

async function showNearbyStores() {
  const { coords } = await getCurrentPosition();
  const query = toQueryString({ latitude: coords.latitude, longitude: coords.longitude, raioKm: 20 });
  const nearby = await api(`/lojas/proximas?${query}`);
  if (state.view === 'lojas') await go('lojas', { nearby });
}

async function createReturn(storeId) {
  const item = state.selectedItem;
  const loja = state.stores.find((s) => String(s.id) === storeId);
  if (!item || !loja) return;

  const ok = await askConfirmation('Confirmar devolução?',
    `${item.produto} · ${item.quantidade} unidade(s). Entrega em ${loja.nome}.`, 'Confirmar solicitação');
  if (!ok) return;

  const result = await api('/devolucoes', { method: 'POST', body: { itemCompradoId: item.id, lojaId: loja.id } });
  toast('Solicitação criada. Seu QR está pronto.');
  await go('detalhe', { id: result.id });
}

async function renewQr(id) {
  await api(`/devolucoes/${id}/qrcode/renovar`, { method: 'POST' });
  toast('QR atualizado.');
  await go('detalhe', { id });
}

async function cancelReturn(id) {
  const ok = await askConfirmation('Cancelar solicitação?', 'O QR desta devolução deixará de funcionar.', 'Cancelar solicitação');
  if (!ok) return;
  await api(`/devolucoes/${id}/cancelar`, { method: 'POST' });
  toast('Solicitação cancelada.');
  await go('detalhe', { id });
}

async function markNotificationRead(id) {
  await api(`/notificacoes/${id}/ler`, { method: 'POST' });
  toast('Aviso marcado como lido.');
  await go('avisos', state.params);
}

$('#navigation').addEventListener('click', (event) => {
  const button = event.target.closest('[data-view]');
  if (!button) return;
  $('#header-search-input').value = '';
  go(button.dataset.view);
});

$('#content').addEventListener('click', (event) => {
  const button = event.target.closest('button[data-action]');
  if (!button || button.disabled) return;
  const { action, id, view, page, value } = button.dataset;

  // Ações que só trocam de tela.
  switch (action) {
    case 'navigate': return go(view);
    case 'refresh': return go(state.view, state.params);
    case 'chip': return go('devolucoes', { status: value });
    case 'chip-unread': return go('avisos', { naoLidas: value === '1' });
    case 'page': return go(state.view, { ...state.params, pagina: Number(page) });
    case 'history-page': return go('detalhe', { ...state.params, histPagina: Number(page) });
    case 'detail': return go('detalhe', { id });
    case 'stop-camera': return stopCamera();
    case 'select-item':
      state.selectedItem = state.items.find((i) => String(i.id) === id);
      return go('lojas');
  }

  // Ações que falam com o servidor ou com o aparelho.
  const handlers = {
    camera: () => startCamera(),
    nearby: () => showNearbyStores(),
    'create-return': () => createReturn(id),
    renew: () => renewQr(id),
    cancel: () => cancelReturn(id),
    read: () => markNotificationRead(id),
  };
  if (handlers[action]) whileBusy(button, handlers[action]);
});

// Os filtros da fila aplicam assim que o funcionário troca uma opção.
$('#content').addEventListener('change', (event) => {
  const form = event.target.closest('#queue-filter');
  if (form && event.target.matches('select')) form.requestSubmit();
});

async function savePreferences(data) {
  const body = { atualizacoes: data.has('atualizacoes'), lembretesQr: data.has('lembretesQr') };
  await api('/notificacoes/preferencias', { method: 'PUT', body });
  toast('Preferências salvas.');
}

async function setDestination(returnId, destino) {
  if (!destino) return;
  const ok = await askConfirmation('Definir destino?',
    `A devolução #${returnId} será encaminhada para ${DESTINATIONS[destino].toLowerCase()}. Ela continuará pendente até o processamento.`);
  if (!ok) return;

  await api(`/funcionario/devolucoes/${returnId}/destino`, { method: 'POST', body: { destino } });
  toast('Destino definido. Processamento pendente.');
  await go('fila', state.params);
}

async function receiveProduct(token) {
  if (!isValidToken(token)) {
    throw new Error('Confira o código. Ele deve ser o código completo apresentado no QR da devolução.');
  }
  const ok = await askConfirmation('Confirmar recebimento?',
    'Confirme que o produto está em suas mãos. O QR será desativado e a devolução seguirá para análise.', 'Receber produto');
  if (!ok) return;

  const result = await api('/funcionario/devolucoes/escanear', { method: 'POST', body: { token } });
  stopCamera();

  if (state.view === 'receber') {
    $('#return-token').value = '';
    $('#receipt-result').innerHTML = `
      <section class="card">
        <h3>Produto recebido ${icon('check')}</h3>
        <p>A devolução #${result.id} está em análise.</p>
        ${navigateButton('fila', 'Abrir fila da loja', 'secondary')}
      </section>`;
  }
  toast('Recebimento confirmado.');
}

$('#content').addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.target;
  const data = new FormData(form);
  const button = form.querySelector('button[type=submit], button:not([type])');
  if (!button) return;

  if (form.id === 'store-search') return go('lojas', { busca: data.get('busca') });
  if (form.id === 'queue-filter') return go('fila', Object.fromEntries(data));

  whileBusy(button, async () => {
    if (form.id === 'preferences-form') await savePreferences(data);
    else if (form.id === 'receive-form') await receiveProduct(String(data.get('token')).trim());
    else if (form.dataset.destination) await setDestination(form.dataset.destination, data.get('destino'));
  });
});

$('#header-search').addEventListener('submit', (event) => {
  event.preventDefault();
  const input = $('#header-search-input');
  go('itens', { busca: input.value.trim() });
  input.blur();
});


// ---------------------------------------------------------------------------
// Login e logout
// ---------------------------------------------------------------------------

$('#login-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const button = event.target.querySelector('[type=submit]');

  whileBusy(button, async () => {
    $('#login-error').hidden = true;
    try {
      csrf = await fetchCsrfToken();
      const credentials = new URLSearchParams({ username: $('#username').value.trim(), password: $('#password').value });
      await api('/sessao', { method: 'POST', body: credentials });
      $('#password').value = '';
      // O Spring troca o token CSRF depois do login.
      csrf = await fetchCsrfToken();
      await enter(await api('/sessao'));
    } catch (error) {
      // O servidor manda uma mensagem própria quando a conta está bloqueada.
      const wrongCredentials = error.status === 401 && !error.fromServer;
      $('#login-error').textContent = wrongCredentials ? 'Login ou senha inválidos.' : error.message;
      $('#login-error').hidden = false;
      $('#password').value = '';
    }
  });
});

$('#show-password').addEventListener('click', () => {
  const password = $('#password');
  const show = password.type === 'password';
  password.type = show ? 'text' : 'password';
  $('#show-password').textContent = show ? 'Ocultar' : 'Mostrar';
  $('#show-password').setAttribute('aria-pressed', String(show));
});

$('#logout').addEventListener('click', (event) => whileBusy(event.currentTarget, async () => {
  await api('/sessao/sair', { method: 'POST' });
  showLogin();
}));


// ---------------------------------------------------------------------------
// Leitura de QR pela câmera
// ---------------------------------------------------------------------------

function stopCamera() {
  clearTimeout(cameraTimer);
  cameraTimer = null;

  if (cameraStream) {
    cameraStream.getTracks().forEach((track) => track.stop());
    cameraStream = null;
  }

  const video = $('#camera-video');
  if (video) {
    video.pause();
    video.srcObject = null;
  }
  if ($('#camera-box')) $('#camera-box').hidden = true;
  const stopButton = $('[data-action="stop-camera"]');
  if (stopButton) stopButton.hidden = true;
}

// O leitor (jsQR) é grande, então só carregamos quando a câmera é aberta.
function loadQrDecoder() {
  if (window.jsQR) return Promise.resolve(window.jsQR);
  if (decoderPromise) return decoderPromise;

  decoderPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = '/vendor/jsQR.js';
    script.onload = () => resolve(window.jsQR);
    script.onerror = () => {
      script.remove();
      decoderPromise = null;
      reject(new Error('Não foi possível abrir o leitor. Você pode colar o código.'));
    };
    document.head.append(script);
  });
  return decoderPromise;
}

async function startCamera() {
  stopCamera();
  const renderAtStart = renderCount;
  const userLeft = () => renderAtStart !== renderCount;

  if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
    throw new Error('A câmera precisa de uma conexão HTTPS. Você também pode colar o código.');
  }

  const decode = await loadQrDecoder();
  if (userLeft()) return;

  let stream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: { facingMode: { ideal: 'environment' } } });
  } catch {
    throw new Error('Não foi possível acessar a câmera. Verifique a permissão do navegador ou cole o código.');
  }
  if (userLeft()) {
    stream.getTracks().forEach((track) => track.stop());
    return;
  }

  cameraStream = stream;
  const video = $('#camera-video');
  video.srcObject = stream;
  $('#camera-box').hidden = false;
  $('[data-action="stop-camera"]').hidden = false;

  const status = $('#camera-status');
  status.textContent = 'Aponte a câmera para o QR apresentado pelo cliente.';
  status.hidden = false;

  try {
    await video.play();
  } catch {
    stopCamera();
    throw new Error('Não foi possível iniciar a câmera. Tente novamente ou cole o código.');
  }

  const canvas = document.createElement('canvas');
  const context = canvas.getContext('2d', { willReadFrequently: true });

  const scanFrame = () => {
    if (!cameraStream || userLeft()) return;

    if (video.readyState >= 2 && video.videoWidth) {
      // Reduz a imagem para no máximo 640px: decodifica mais rápido e não perde leitura.
      canvas.width = Math.min(video.videoWidth, 640);
      canvas.height = Math.round(video.videoHeight * canvas.width / video.videoWidth);
      context.drawImage(video, 0, 0, canvas.width, canvas.height);

      const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
      const result = decode(pixels.data, pixels.width, pixels.height);
      const token = result?.data?.trim();

      if (token && isValidToken(token)) {
        stopCamera();
        $('#return-token').value = token;
        $('#return-token').focus();
        status.textContent = 'Código lido. Confira o produto e confirme o recebimento.';
        return;
      }
      if (token) status.textContent = 'Este QR não parece ser uma devolução. Tente o QR apresentado no site.';
    }

    cameraTimer = setTimeout(scanFrame, 180);
  };
  scanFrame();
}

document.addEventListener('visibilitychange', () => {
  if (document.hidden) stopCamera();
});
window.addEventListener('pagehide', stopCamera);


// ---------------------------------------------------------------------------
// Contagem regressiva do QR
// ---------------------------------------------------------------------------

// Só visual: quem decide se o QR vale é o servidor.
let lastExpiredQr = '';

function updateCountdowns() {
  document.querySelectorAll('[data-expires]').forEach((el) => {
    const remaining = new Date(el.dataset.expires) - Date.now();
    if (Number.isNaN(remaining)) return;

    if (remaining <= 0) {
      el.textContent = 'QR expirado';
      el.classList.add('expired');
      // Recarrega a tela uma única vez para mostrar o botão de renovar.
      const key = state.params.id + el.dataset.expires;
      if (state.view === 'detalhe' && lastExpiredQr !== key) {
        lastExpiredQr = key;
        go('detalhe', state.params);
      }
      return;
    }

    const hours = Math.floor(remaining / 3_600_000);
    const minutes = Math.floor((remaining % 3_600_000) / 60_000);
    const seconds = Math.floor((remaining % 60_000) / 1000);
    el.textContent = `Expira em ${hours ? `${hours}h ` : ''}${minutes}min ${String(seconds).padStart(2, '0')}s`;
    el.classList.toggle('urgent', remaining < 3_600_000);
  });
}

setInterval(updateCountdowns, 1000);


// ---------------------------------------------------------------------------
// Sessão parada: avisa aos 28 min e sai aos 30 min (mesmo tempo do servidor)
// ---------------------------------------------------------------------------

const IDLE_WARNING_MS = 28 * 60 * 1000;
const IDLE_LOGOUT_MS = 30 * 60 * 1000;
let lastActivity = Date.now();

function markActivity() {
  lastActivity = Date.now();
}

['pointerdown', 'keydown', 'scroll', 'touchstart'].forEach((type) =>
  document.addEventListener(type, markActivity, { passive: true }));

async function checkIdle() {
  if (!state.user) return;
  const idleFor = Date.now() - lastActivity;
  const dialog = $('#idle-dialog');

  if (idleFor >= IDLE_LOGOUT_MS) {
    await api('/sessao/sair', { method: 'POST' }).catch(() => {});
    showLogin('Sua sessão foi encerrada depois de 30 minutos sem uso.');
    return;
  }

  if (idleFor >= IDLE_WARNING_MS) {
    if (!dialog.open) dialog.showModal();
  } else if (dialog.open) {
    dialog.close();
  }
}

setInterval(checkIdle, 10_000);
$('#idle-dialog').addEventListener('close', markActivity);


// ---------------------------------------------------------------------------
// Início
// ---------------------------------------------------------------------------

async function start() {
  try {
    const user = await api('/sessao');
    csrf = await fetchCsrfToken();
    await enter(user);
  } catch (error) {
    showLogin(error.status === 401 ? '' : error.message);
  }
}

start();

// Permite instalar o site como app. O service worker não guarda nada em cache.
const canUseServiceWorker = location.protocol === 'https:' || ['localhost', '127.0.0.1'].includes(location.hostname);
if ('serviceWorker' in navigator && canUseServiceWorker) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}));
}
