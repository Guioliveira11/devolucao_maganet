// Maga Next — Troca e Devolução
// Cliente solicita e acompanha pelo celular; funcionário recebe, confere e define o destino na loja.

const $ = (selector) => document.querySelector(selector);

const esc = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));


// ---------------------------------------------------------------------------
// Textos, rótulos de negócio e ícones
// ---------------------------------------------------------------------------

const ICONS = {
  box: '<path d="m3 7 9-5 9 5v10l-9 5-9-5Z"/><path d="m3 7 9 5 9-5M12 12v10M7 4.8l9 5"/>',
  dress: '<path d="M9 3 7 6l2 3-3 11h12L15 9l2-3-2-3-3 2-3-2Z"/><path d="M9 9h6"/>',
  audio: '<path d="M4 13v-1a8 8 0 0 1 16 0v1"/><rect x="2" y="13" width="4" height="7" rx="2"/><rect x="18" y="13" width="4" height="7" rx="2"/>',
  tv: '<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8M12 17v4"/>',
  coffee: '<path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v7a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z"/><path d="M6 2v3M10 2v3M14 2v3"/>',
  returns: '<path d="M7 7h10l-3-3m3 3-3 3M17 17H7l3-3m-3 3 3 3"/>',
  refund: '<circle cx="12" cy="12" r="9"/><path d="M14.5 9.5c-.5-1-1.5-1.5-2.7-1.5-1.5 0-2.6.8-2.6 2s1 1.7 2.7 2c1.7.3 2.6.9 2.6 2.1 0 1.3-1.1 2.1-2.7 2.1-1.3 0-2.3-.5-2.8-1.6M12 6.5v11"/>',
  store: '<path d="M3 9 5 4h14l2 5"/><path d="M4 9v11h16V9"/><path d="M9 20v-6h6v6"/>',
  truck: '<rect x="1" y="5" width="14" height="12" rx="1"/><path d="M15 9h4l3 4v4h-7Z"/><circle cx="6" cy="18" r="2"/><circle cx="18" cy="18" r="2"/>',
  bell: '<path d="M5 9a7 7 0 0 1 14 0v6l2 3H3l2-3ZM10 21h4"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  qr: '<path d="M3 3h6v6H3ZM15 3h6v6h-6ZM3 15h6v6H3ZM15 15h2v2h-2ZM21 14v4h-3v3M14 21h1"/>',
  list: '<path d="M9 5h12M9 12h12M9 19h12M3 5h1M3 12h1M3 19h1"/>',
  pin: '<path d="M19 9c0 5-7 12-7 12S5 14 5 9a7 7 0 0 1 14 0Z"/><circle cx="12" cy="9" r="2"/>',
  refresh: '<path d="M20 7a9 9 0 1 0 1 8M20 2v5h-5"/>',
  arrow: '<path d="M19 12H5M10 7l-5 5 5 5"/>',
  chevronRight: '<path d="m9 6 6 6-6 6"/>',
  camera: '<path d="M8 5 10 2h4l2 3h5v16H3V5Z"/><circle cx="12" cy="12" r="4"/>',
  video: '<rect x="2" y="6" width="14" height="12" rx="2"/><path d="m16 10 6-3v10l-6-3Z"/>',
  comment: '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  checkCircle: '<circle cx="12" cy="12" r="9"/><path d="m8.5 12.2 2.4 2.4 4.8-5"/>',
  close: '<path d="M18 6 6 18M6 6l12 12"/>',
  closeCircle: '<circle cx="12" cy="12" r="9"/><path d="m15 9-6 6M9 9l6 6"/>',
  calendar: '<rect x="3" y="4" width="18" height="17" rx="2"/><path d="M16 2v4M8 2v4M3 9h18"/>',
  flag: '<path d="M5 21V4h13l-2.5 4.5L18 13H5"/>',
  leaf: '<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.4 19 2c1 2 2 4.2 2 8 0 5.5-4.8 10-10 10Z"/><path d="M2 21c0-3 1.8-5.4 5.1-6C9.5 14.5 12 13 13 12"/>',
  heart: '<path d="M19.5 12.6 12 20l-7.5-7.4A5 5 0 0 1 12 5.6a5 5 0 0 1 7.5 7Z"/>',
  wrench: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.7-3.7a6 6 0 0 1-7.9 7.9l-6.9 6.9a2.1 2.1 0 0 1-3-3l6.9-6.9a6 6 0 0 1 7.9-7.9Z"/>',
};

const icon = (name) =>
  `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONS[name] || ICONS.box}</svg>`;

function productGlyph(name = '') {
  const lower = name.toLowerCase();
  if (lower.includes('vestido') || lower.includes('camisa') || lower.includes('roupa')) return 'dress';
  if (lower.includes('fone') || lower.includes('caixa de som')) return 'audio';
  if (lower.includes('tv') || lower.includes('monitor')) return 'tv';
  if (lower.includes('cafeteira') || lower.includes('liquidificador')) return 'coffee';
  return 'box';
}

function productImageUrl(name = '') {
  const lower = name.toLowerCase();
  if (lower.includes('vestido') || lower.includes('camisa') || lower.includes('roupa')) return '/assets/vestido-camponesa.svg';
  if (lower.includes('fone') || lower.includes('caixa de som')) return '/assets/produto-audio.svg';
  if (lower.includes('tv') || lower.includes('monitor')) return '/assets/produto-tv.svg';
  if (lower.includes('cafeteira') || lower.includes('liquidificador')) return '/assets/produto-eletro.svg';
  return '/assets/produto-box.svg';
}

const STATUS_NAMES = {
  AGUARDANDO_ESCANEAMENTO: 'Aguardando entrega na loja',
  QR_EXPIRADO: 'QR Code expirado',
  EM_ANALISE: 'Em conferência',
  DESTINO_PENDENTE: 'Aprovada · Destino definido',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
  REPROVADA: 'Não aprovada',
};

const STATUS_COLORS = {
  CONCLUIDA: 'green',
  DESTINO_PENDENTE: 'green',
  CANCELADA: 'gray',
  QR_EXPIRADO: 'orange',
  REPROVADA: 'pink',
  EM_ANALISE: 'purple',
};

const MOTIVOS_DEVOLUCAO = {
  PRODUTO_COM_DEFEITO: 'Produto com defeito',
  PRODUTO_DANIFICADO: 'Produto danificado',
  DIFERENTE_DO_ANUNCIADO: 'Produto diferente do anunciado',
  PRODUTO_ERRADO: 'Produto errado',
  ARREPENDIMENTO: 'Arrependimento',
  OUTRO: 'Outro',
};

const TIPOS_RESOLUCAO = {
  TROCA: { title: 'Troca', desc: 'Receba outro produto', icon: 'returns' },
  REEMBOLSO: { title: 'Reembolso', desc: 'Receba o valor de volta', icon: 'refund' },
};

const DESTINATIONS = {
  ESTOQUE: 'Retornar ao estoque',
  REPARO: 'Encaminhar para reparo',
  DOACAO_ONG: 'Destinação socioambiental (ONG parceira)',
};

const ONGS_PARCEIRAS = {
  ECOSMAG: {
    id: 'ECOSMAG',
    title: 'ECOSMAG Sergipe',
    desc: 'Reciclagem e Inclusão Digital',
    icon: 'leaf',
  },
  EXERCITO_SALVACAO: {
    id: 'EXERCITO_SALVACAO',
    title: 'Exército da Salvação',
    desc: 'Assistência Social e Reutilização',
    icon: 'heart',
  },
};

const CHECKLIST_ITEMS = {
  CORRESPONDE_AO_PEDIDO: 'Produto corresponde ao pedido',
  SEM_SINAIS_DE_USO: 'Produto em condições adequadas',
  ACESSORIOS_COMPLETOS: 'Acessórios e itens presentes',
  EMBALAGEM_ORIGINAL: 'Embalagem conforme',
};

const MOTIVOS_REPROVACAO = {
  SINAIS_DE_USO: 'Produto com sinais de uso indevido',
  PRODUTO_DIVERGENTE: 'Produto divergente do pedido',
  ITENS_FALTANDO: 'Acessórios ou peças faltando',
  DANO_NAO_INFORMADO: 'Avaria incompatível com o relato',
  OUTRO: 'Outro motivo (especificar abaixo)',
};

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const isValidToken = (token) => UUID_PATTERN.test(token);


// ---------------------------------------------------------------------------
// Estado da aplicação
// ---------------------------------------------------------------------------

function blankDraft() {
  return {
    motivo: 'PRODUTO_COM_DEFEITO',
    comentario: '',
    evidencias: [],
    tipoResolucao: 'TROCA',
    lojaId: null,
  };
}

function blankInspection() {
  return {
    id: null,
    item: null,
    checked: Object.keys(CHECKLIST_ITEMS),
    decisao: 'APROVAR',
    destino: 'ESTOQUE',
    ongParceira: 'ECOSMAG',
    motivoReprovacao: 'SINAIS_DE_USO',
    observacao: '',
    resumoConclusao: null,
  };
}

const state = {
  user: null,
  view: 'itens',
  params: {},
  items: [],
  stores: [],
  selectedItem: null,
  draft: blankDraft(),
  inspection: blankInspection(),
  unread: 0,
  epoch: 0,
};

let csrf = null;
let renderCount = 0;
let cameraStream = null;
let cameraTimer = null;
let decoderPromise = null;
let toastTimer = null;


// ---------------------------------------------------------------------------
// Formatação e utilitários
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

function effectiveStatus(devolucao) {
  const expired = devolucao.qrExpiraEm && new Date(devolucao.qrExpiraEm) <= Date.now();
  if (devolucao.status === 'AGUARDANDO_ESCANEAMENTO' && expired) return 'QR_EXPIRADO';
  return devolucao.status;
}

// Reduz e comprime qualquer imagem escolhida no celular para caber com folga na requisição.
function compressImageFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Não foi possível ler a imagem selecionada.'));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error('Formato de imagem inválido.'));
      img.onload = () => {
        const maxSide = 720;
        const scale = Math.min(1, maxSide / Math.max(img.width || 1, img.height || 1));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(img.width * scale));
        canvas.height = Math.max(1, Math.round(img.height * scale));
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', 0.78));
      };
      img.src = String(reader.result);
    };
    reader.readAsDataURL(file);
  });
}


// ---------------------------------------------------------------------------
// Componentes visuais reutilizáveis
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
        ${text ? `<p class="muted">${esc(text)}</p>` : ''}
      </div>
      ${refresh}
    </div>`;
}

const backButton = (view, label = 'Voltar', extraData = '') =>
  `<button type="button" class="back-button" data-action="navigate" data-view="${view}" ${extraData}>${icon('arrow')}${esc(label)}</button>`;

const navigateButton = (view, label, style = 'primary') =>
  `<button type="button" class="button ${style}" data-action="navigate" data-view="${view}">${label}</button>`;

function productSummaryBanner(item, eyebrowLabel = 'PRODUTO SELECIONADO') {
  const imgUrl = productImageUrl(item.produto);
  const priceOrQty = item.valorPago != null
    ? formatMoney(item.valorPago)
    : plural(item.quantidade || 1, 'unidade', 'unidades');
  return `
    <div class="product-summary-card">
      <div class="product-thumb">
        <img src="${imgUrl}" alt="${esc(item.produto)}" loading="lazy">
      </div>
      <div class="product-summary-text">
        <span class="eyebrow">${esc(eyebrowLabel)}</span>
        <h3>${esc(item.produto)}</h3>
        <p>Pedido #${esc(item.numeroPedido)} · <strong>${esc(priceOrQty)}</strong></p>
      </div>
    </div>`;
}

function pagination(page, action = 'page') {
  if (!page.total) return '';
  const totalPages = Math.max(1, Math.ceil(page.total / page.tamanho));
  const isFirst = page.pagina === 0;
  const isLast = (page.pagina + 1) * page.tamanho >= page.total;
  return `
    <div class="pagination">
      <button type="button" class="button outline" data-action="${action}" data-page="${page.pagina - 1}" ${isFirst ? 'disabled' : ''}>Anterior</button>
      <span>Página ${page.pagina + 1} de ${totalPages}<br>${plural(page.total, 'registro', 'registros')}</span>
      <button type="button" class="button outline" data-action="${action}" data-page="${page.pagina + 1}" ${isLast ? 'disabled' : ''}>Próxima</button>
    </div>`;
}

function progressStepper(status) {
  if (status === 'CANCELADA') return '';

  const labels = ['Solicitada', 'Entregue na loja', 'Conferência', status === 'REPROVADA' ? 'Não aprovada' : 'Concluída'];
  const stepByStatus = {
    AGUARDANDO_ESCANEAMENTO: 0,
    QR_EXPIRADO: 0,
    EM_ANALISE: 2,
    DESTINO_PENDENTE: 3,
    CONCLUIDA: 3,
    REPROVADA: 3,
  };
  const current = stepByStatus[status] ?? 0;
  const finished = ['CONCLUIDA', 'DESTINO_PENDENTE'].includes(status);

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
// Estrutura da página: login, tema por perfil, navegação
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
  state.draft = blankDraft();
  state.inspection = blankInspection();
  csrf = null;

  document.body.removeAttribute('data-role');
  $('#app').hidden = true;
  $('#login-screen').hidden = false;
  $('#initial-loading').hidden = true;
  $('#content').replaceChildren();
  $('#login-error').textContent = message;
  $('#login-error').hidden = !message;
  $('#password').value = '';
  closeRegister();

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
  ['itens', 'Meus pedidos', 'box'],
  ['devolucoes', 'Trocas e devoluções', 'returns'],
  ['avisos', 'Avisos', 'bell'],
  ['conta', 'Conta', 'user'],
];

const EMPLOYEE_MENU = [
  ['fila', 'Conferência', 'list'],
  ['receber', 'Ler QR Code', 'qr'],
  ['conta', 'Conta', 'user'],
];

const ONG_MENU = [
  ['doacoes', 'Doações', 'box'],
  ['conta', 'Conta', 'user'],
];

const MENUS = { CLIENTE: CLIENT_MENU, FUNCIONARIO: EMPLOYEE_MENU, ONG: ONG_MENU };

function renderNav() {
  if (!state.user) return;

  const menu = MENUS[state.user.perfil] || EMPLOYEE_MENU;
  const parentMap = {
    motivo: 'itens',
    evidencias: 'itens',
    lojas: 'itens',
    detalhe: 'devolucoes',
    conferir: 'fila',
    resultado: 'fila',
    concluido: 'fila',
  };
  const active = parentMap[state.view] || state.view;

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
    // Silencioso no contador auxiliar.
  }
}

async function enter(user) {
  state.user = user;
  markActivity();

  const isClient = user.perfil === 'CLIENTE';
  document.body.dataset.role = user.perfil;

  $('#login-screen').hidden = true;
  $('#app').hidden = false;
  $('#initial-loading').hidden = true;
  $('#header-name').textContent = user.nome;
  $('#header-search').hidden = !isClient;

  const badge = $('#header-role-badge');
  if (badge) {
    badge.textContent = isClient ? 'Cliente' : (user.perfil === 'ONG' ? 'ONG parceira' : (user.loja || 'Operação Loja'));
  }

  await go(isClient ? 'itens' : (user.perfil === 'ONG' ? 'doacoes' : 'fila'));
}

const LOADING_HTML = `
  <div class="loading" role="status"><span class="sr-only">Carregando…</span></div>
  <div class="cards skeleton-list" aria-hidden="true">
    <div class="skeleton"></div><div class="skeleton"></div>
  </div>`;

// Direcao da proxima transicao: 'back' (botao voltar), 'menu' (navegacao lateral) ou 'forward'
let navDirection = 'forward';
const SKELETON_DELAY_MS = 180;

function playEnterAnimation(content, direction) {
  content.classList.remove('view-leaving', 'view-anim', 'from-left', 'from-right');
  void content.offsetWidth; // reinicia a animacao mesmo se a classe ja estava aplicada
  content.classList.add('view-anim');
  if (direction === 'back') content.classList.add('from-left');
  else if (direction === 'forward') content.classList.add('from-right');
}

async function go(view, params = {}, { keepScroll = false } = {}) {
  if (!state.user) return;

  stopCamera();
  state.view = view;
  state.params = { ...params };
  const thisRender = ++renderCount;
  const scrollY = window.scrollY;
  const direction = navDirection;
  navDirection = 'forward';

  const content = $('#content');
  renderNav();
  content.setAttribute('aria-busy', 'true');

  // So mostra o skeleton se a tela demorar; em respostas rapidas evita o "pisca"
  let skeletonTimer = null;
  if (!keepScroll) {
    content.classList.add('view-leaving');
    skeletonTimer = setTimeout(() => {
      if (thisRender !== renderCount) return;
      content.classList.remove('view-leaving', 'view-anim');
      content.innerHTML = LOADING_HTML;
    }, SKELETON_DELAY_MS);
  }

  try {
    const render = VIEWS[view] || itemsView;
    const html = await render(params);
    if (thisRender !== renderCount) return;
    clearTimeout(skeletonTimer);

    if (keepScroll) content.classList.remove('view-anim');
    content.innerHTML = html;
    content.removeAttribute('aria-busy');
    content.focus({ preventScroll: true });
    window.scrollTo({ top: keepScroll ? scrollY : 0, behavior: 'instant' });
    if (!keepScroll) playEnterAnimation(content, direction);
    refreshUnreadCount();
  } catch (error) {
    if (thisRender !== renderCount || error.stale) return;
    clearTimeout(skeletonTimer);
    content.classList.remove('view-leaving');
    content.removeAttribute('aria-busy');

    if (error.status === 401) {
      handleError(error);
      return;
    }

    content.innerHTML = `
      ${pageHeading('VAMOS TENTAR DE NOVO', 'Não foi possível carregar', 'Seus dados não foram alterados.', false)}
      <div class="error-message" role="alert">${esc(error.message)}</div>
      <button type="button" class="button primary" data-action="refresh">Tentar novamente</button>`;
    playEnterAnimation(content, 'menu');
  }
}


// ---------------------------------------------------------------------------
// Telas do Cliente (Moodboard 1 — E-commerce Cliente)
// ---------------------------------------------------------------------------

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
  const imgUrl = productImageUrl(item.produto);
  const action = item.elegivelParaDevolucao
    ? `<button type="button" class="button primary full" data-action="select-item" data-id="${item.id}">
        Solicitar troca ou devolução
      </button>`
    : `<button type="button" class="button outline full" data-action="navigate" data-view="devolucoes">
        Consultar solicitações
      </button>`;

  return `
    <article class="card">
      <div class="product-visual">
        <img class="product-photo" src="${imgUrl}" alt="${esc(item.produto)}" loading="lazy">
      </div>
      <div class="order-card-row">
        <div>
          <div class="order-number">Pedido #${esc(item.numeroPedido)} · ${plural(item.quantidade, 'item', 'itens')}</div>
          <h3 class="order-title">${esc(item.produto)}</h3>
        </div>
        <div class="price">${esc(formatMoney(item.valorPago))}</div>
      </div>
      <div>
        <span class="tag ${item.elegivelParaDevolucao ? 'green' : 'gray'}">
          ${item.elegivelParaDevolucao ? 'Status: Entregue' : 'Prazo encerrado ou em devolução'}
        </span>
      </div>
      <div class="deadline">Elegível para troca ou devolução até <strong>${esc(formatDate(item.dataLimiteDevolucao))}</strong></div>
      ${action}
    </article>`;
}

function clientQuickActionsBar(eligibleItem) {
  const firstAction = eligibleItem
    ? `data-action="select-item" data-id="${eligibleItem.id}"`
    : `data-action="navigate" data-view="devolucoes"`;
  return `
    <section class="quick-actions-grid" aria-label="Ações rápidas do cliente">
      <button type="button" class="quick-action-card" ${firstAction}>
        <span class="quick-action-icon">${icon('returns')}</span>
        <span class="quick-action-body">
          <strong>Trocar produto</strong>
          <span>Solicite troca por outro tamanho ou item</span>
        </span>
        <span class="quick-action-chevron">${icon('chevronRight')}</span>
      </button>

      <button type="button" class="quick-action-card" ${firstAction}>
        <span class="quick-action-icon">${icon('refund')}</span>
        <span class="quick-action-body">
          <strong>Devolver produto</strong>
          <span>Peça devolução e receba seu reembolso</span>
        </span>
        <span class="quick-action-chevron">${icon('chevronRight')}</span>
      </button>

      <button type="button" class="quick-action-card" data-action="navigate" data-view="devolucoes">
        <span class="quick-action-icon">${icon('qr')}</span>
        <span class="quick-action-body">
          <strong>Gerar QR Code</strong>
          <span>Acesse seu código para entrega na loja</span>
        </span>
        <span class="quick-action-chevron">${icon('chevronRight')}</span>
      </button>
    </section>`;
}

async function itemsView({ pagina = 0, busca = '' } = {}) {
  const page = busca
    ? await searchItems(busca)
    : await api(`/itens?${toQueryString({ pagina, tamanho: 12 })}`);
  state.items = page.itens;

  const eligibleItem = page.itens.find((i) => i.elegivelParaDevolucao);
  const quickBar = !busca ? clientQuickActionsBar(eligibleItem) : '';

  const list = page.itens.length
    ? `<div class="cards">${page.itens.map(itemCard).join('')}</div>`
    : emptyState('Nenhum pedido encontrado', 'Seus pedidos entregues aparecerão aqui para troca ou devolução.');

  return pageHeading('E-COMMERCE CLIENTE', 'Meus pedidos', 'Selecione um item entregue para solicitar troca ou devolução.')
    + quickBar
    + list
    + pagination(page);
}

// Etapa 1 de 3 — Solicitar troca ou devolução (Motivo)
async function reasonView() {
  const item = state.selectedItem;
  if (!item) {
    return emptyState('Escolha primeiro um produto', 'Volte aos seus pedidos para iniciar uma solicitação.',
      navigateButton('itens', 'Ver meus pedidos'));
  }

  const motivoAtual = state.draft.motivo || 'PRODUTO_COM_DEFEITO';
  const radios = Object.entries(MOTIVOS_DEVOLUCAO).map(([key, label]) => `
    <label class="option-card ${motivoAtual === key ? 'selected' : ''}">
      <span class="option-body">
        <span class="option-title">${esc(label)}</span>
      </span>
      <input type="radio" name="motivo" value="${key}" ${motivoAtual === key ? 'checked' : ''} required>
    </label>
  `).join('');

  return `
    <div class="wizard-shell">
      <div class="wizard-topbar">
        ${backButton('itens', 'Meus pedidos')}
        <span class="step-pill">Tela 2 · Motivo</span>
      </div>
      ${pageHeading('ETAPA 1 DE 3', 'Solicitar troca ou devolução', 'Conte para a gente o que aconteceu com o seu pedido.', false)}

      <div class="wizard-grid">
        <aside>
          ${productSummaryBanner(item)}
          <div class="section-block">
            <h3>Como funciona</h3>
            <p class="section-subtitle">O processo leva menos de 1 minuto:</p>
            <div class="summary-kv-list">
              <div class="summary-kv-row"><span>1. Motivo</span><strong>O que aconteceu</strong></div>
              <div class="summary-kv-row"><span>2. Evidências</span><strong>Fotos e relato</strong></div>
              <div class="summary-kv-row"><span>3. Solução</span><strong>Troca ou reembolso + QR</strong></div>
            </div>
          </div>
        </aside>

        <form id="reason-form" class="section-block">
          <h3>Por que você deseja devolver?</h3>
          <p class="section-subtitle">Selecione a opção que melhor descreve o problema.</p>
          <div class="option-list cols-2">${radios}</div>
          <div style="margin-top: 18px;">
            <label for="reason-comment">Conte-nos mais (opcional)</label>
            <textarea id="reason-comment" name="comentario" maxlength="500"
                      placeholder="Ex.: O tamanho ficou diferente do esperado ou a peça apresentou defeito...">${esc(state.draft.comentario)}</textarea>
          </div>
          <div style="margin-top: 20px;">
            <button type="submit" class="button primary full">Continuar</button>
          </div>
        </form>
      </div>
    </div>`;
}

// Etapa 2 de 3 — Enviar evidências (Fotos e Comentário)
async function evidenceView() {
  const item = state.selectedItem;
  if (!item) {
    return emptyState('Escolha primeiro um produto', 'Volte aos seus pedidos para iniciar uma solicitação.',
      navigateButton('itens', 'Ver meus pedidos'));
  }

  const photos = state.draft.evidencias || [];
  const photoGrid = photos.length
    ? `<div class="photo-grid">
        ${photos.map((src, idx) => `
          <div class="photo-thumb">
            <img src="${esc(src)}" alt="Evidência ${idx + 1}">
            <button type="button" class="photo-remove" data-action="remove-photo" data-index="${idx}" aria-label="Remover foto ${idx + 1}">×</button>
          </div>
        `).join('')}
      </div>`
    : '';

  return `
    <div class="wizard-shell">
      <div class="wizard-topbar">
        ${backButton('motivo', 'Voltar ao motivo')}
        <span class="step-pill">Tela 3 · Evidências</span>
      </div>
      ${pageHeading('ETAPA 2 DE 3', 'Enviar evidências', 'Mostre o estado do produto para facilitar a conferência na loja.', false)}

      <div class="wizard-grid">
        <aside>
          ${productSummaryBanner(item)}
          <div class="section-block">
            <h3>Dica para agilizar</h3>
            <p class="section-subtitle" style="margin-bottom: 0;">
              Adicione fotos nítidas mostrando o produto, a etiqueta ou a embalagem. O atendente verá essas informações no balcão da loja.
            </p>
          </div>
        </aside>

        <form id="evidence-form" class="section-block">
          <input type="file" id="photo-input" accept="image/jpeg,image/png,image/webp" multiple hidden>

          <h3>Registros do produto</h3>
          <p class="section-subtitle">Você pode anexar até 3 fotos e complementar seu relato.</p>

          <div class="evidence-actions">
            <button type="button" class="evidence-row-btn" data-action="pick-photo">
              <span class="option-icon">${icon('camera')}</span>
              <span>Adicionar foto (${photos.length}/3)</span>
              <span class="plus">+</span>
            </button>

            <button type="button" class="evidence-row-btn" data-action="video-info">
              <span class="option-icon">${icon('video')}</span>
              <span>Adicionar vídeo</span>
              <span class="plus">+</span>
            </button>

            <button type="button" class="evidence-row-btn" data-action="focus-comment">
              <span class="option-icon">${icon('comment')}</span>
              <span>Adicionar comentário</span>
              <span class="plus">+</span>
            </button>
          </div>

          ${photoGrid}

          <div>
            <label for="evidence-comment">Comentário sobre o estado do produto</label>
            <textarea id="evidence-comment" name="comentario" maxlength="500"
                      placeholder="Descreva detalhes que ajudem o atendente na conferência...">${esc(state.draft.comentario)}</textarea>
          </div>

          <div style="margin-top: 20px;">
            <button type="submit" class="button primary full">Continuar</button>
          </div>
        </form>
      </div>
    </div>`;
}

function storeOptionCard(loja, selectedId) {
  const isSelected = Number(selectedId) === Number(loja.id);
  const distance = loja.distanciaKm != null
    ? ` · ${Number(loja.distanciaKm).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} km`
    : '';
  const hours = loja.horarioFuncionamento ? `<span class="option-desc">${esc(loja.horarioFuncionamento)}</span>` : '';

  return `
    <label class="option-card ${isSelected ? 'selected' : ''}">
      <span class="option-icon">${icon('pin')}</span>
      <span class="option-body">
        <span class="option-title">${esc(loja.nome)}${esc(distance)}</span>
        <span class="option-desc">${esc(loja.endereco || 'Endereço não informado')}</span>
        ${hours}
      </span>
      <input type="radio" name="lojaId" value="${loja.id}" ${isSelected ? 'checked' : ''} required>
    </label>`;
}

// Etapa 3 de 3 — Como deseja resolver? (Troca/Reembolso + Loja + Gerar QR Code)
async function storesView({ pagina = 0, busca = '', nearby } = {}) {
  if (!state.selectedItem) {
    return emptyState('Escolha primeiro um produto', 'Volte aos seus pedidos para iniciar uma solicitação.',
      navigateButton('itens', 'Ver meus pedidos'));
  }

  const page = nearby
    ? { itens: nearby, total: nearby.length, pagina: 0, tamanho: Math.max(1, nearby.length) }
    : await api(`/lojas?${toQueryString({ pagina, tamanho: 10, busca })}`);
  state.stores = page.itens;

  if (!state.draft.lojaId && page.itens.length) {
    state.draft.lojaId = page.itens[0].id;
  }

  const resolucaoAtual = state.draft.tipoResolucao || 'TROCA';
  const solutionCards = Object.entries(TIPOS_RESOLUCAO).map(([key, info]) => `
    <label class="option-card ${resolucaoAtual === key ? 'selected' : ''}">
      <span class="option-icon">${icon(info.icon)}</span>
      <span class="option-body">
        <span class="option-title">${esc(info.title)}</span>
        <span class="option-desc">${esc(info.desc)}</span>
      </span>
      <input type="radio" name="tipoResolucao" value="${key}" ${resolucaoAtual === key ? 'checked' : ''}>
    </label>
  `).join('');

  const storeCards = page.itens.length
    ? `<div class="option-list">${page.itens.map((l) => storeOptionCard(l, state.draft.lojaId)).join('')}</div>`
    : emptyState('Nenhuma loja encontrada', 'Tente outro nome ou endereço.');

  return `
    <div class="wizard-shell">
      <div class="wizard-topbar">
        ${backButton('evidencias', 'Voltar às evidências')}
        <span class="step-pill">Tela 4 · Solução</span>
      </div>
      ${pageHeading('ETAPA 3 DE 3', 'Como deseja resolver?', state.selectedItem.produto, false)}

      <form id="resolution-form" class="wizard-two-cols">
        <div>
          <section class="section-block">
            <h3>Escolha uma solução</h3>
            <p class="section-subtitle">Defina como prefere concluir sua solicitação após a conferência.</p>
            <div class="option-list">${solutionCards}</div>
          </section>

          <section class="section-block">
            <h3>Como deseja devolver?</h3>
            <div class="option-list">
              <label class="option-card selected">
                <span class="option-icon">${icon('store')}</span>
                <span class="option-body">
                  <span class="option-title">Levar a uma loja Maga Next</span>
                  <span class="option-desc">Grátis e sem precisar imprimir etiqueta · QR Code imediato</span>
                </span>
                <input type="radio" name="metodoEntrega" value="LOJA" checked>
              </label>

              <div class="option-card disabled" aria-disabled="true">
                <span class="option-icon">${icon('truck')}</span>
                <span class="option-body">
                  <span class="option-title">Coleta/entrega no endereço</span>
                  <span class="option-desc">Indisponível no momento para este pedido</span>
                </span>
                <input type="radio" disabled>
              </div>
            </div>
          </section>
        </div>

        <div>
          <section class="section-block">
            <h3>Selecione a loja de entrega</h3>
            <div class="store-actions">
              <span class="small-note">Escolha a unidade onde você entregará o produto:</span>
              <button type="button" class="button secondary" data-action="nearby">${icon('pin')} Lojas próximas</button>
            </div>
            ${storeCards}
          </section>

          <button type="submit" class="button primary full" data-busy-label="Gerando seu QR Code...">
            Gerar QR Code
          </button>
        </div>
      </form>
    </div>`;
}

function returnCard(devolucao) {
  const motivo = MOTIVOS_DEVOLUCAO[devolucao.motivo] || 'Solicitação registrada';
  const resolucao = TIPOS_RESOLUCAO[devolucao.tipoResolucao]?.title || 'Devolução';

  return `
    <article class="card">
      <div class="order-card-row">
        ${statusTag(devolucao.status)}
        <span class="order-number">#${devolucao.id}</span>
      </div>
      <div>
        <h3 class="order-title">${esc(devolucao.produto)}</h3>
        <p class="order-number">Pedido #${esc(devolucao.numeroPedido)} · ${esc(resolucao)}</p>
      </div>
      <div class="summary-kv-list">
        <div class="summary-kv-row"><span>Motivo</span><strong>${esc(motivo)}</strong></div>
        <div class="summary-kv-row"><span>Loja</span><strong>${esc(devolucao.loja)}</strong></div>
      </div>
      <div class="deadline">Solicitada em ${esc(formatDate(devolucao.criadaEm))}</div>
      <button type="button" class="button secondary full" data-action="detail" data-id="${devolucao.id}">
        Ver QR Code e detalhes
      </button>
    </article>`;
}

async function returnsView({ pagina = 0, status = '' } = {}) {
  const page = await api(`/devolucoes?${toQueryString({ pagina, tamanho: 12, status })}`);
  const devolucoes = page.itens.map((d) => ({ ...d, status: effectiveStatus(d) }));

  const list = devolucoes.length
    ? `<div class="cards">${devolucoes.map(returnCard).join('')}</div>`
    : emptyState('Nenhuma solicitação encontrada', 'Altere o filtro ou escolha um pedido para iniciar uma troca ou devolução.',
        navigateButton('itens', 'Ver meus pedidos'));

  return pageHeading('ACOMPANHAMENTO', 'Trocas e devoluções', 'Consulte seu QR Code e acompanhe cada etapa.')
    + `<div class="chips" role="group" aria-label="Filtrar por status">${chips(STATUS_NAMES, status, 'Todas')}</div>`
    + list
    + pagination(page);
}

// Tela 5 do Moodboard 1 — "Seu QR Code está pronto!" + Detalhes da solicitação
async function detailView({ id, histPagina = 0 } = {}) {
  const [detail, qr, history] = await Promise.all([
    api(`/devolucoes/${id}/acompanhamento`),
    api(`/devolucoes/${id}`),
    api(`/devolucoes/${id}/historico?pagina=${histPagina}&tamanho=10`),
  ]);
  detail.status = effectiveStatus(detail);
  const beforeDelivery = ['AGUARDANDO_ESCANEAMENTO', 'QR_EXPIRADO'].includes(detail.status);
  const hasActiveQr = Boolean(qr.qrCodeImagemBase64);

  const solucaoLabel = TIPOS_RESOLUCAO[detail.tipoResolucao]?.title || 'Troca ou Reembolso';
  const motivoLabel = MOTIVOS_DEVOLUCAO[detail.motivo] || 'Não especificado';
  const reprovacaoLabel = detail.motivoReprovacao
    ? (MOTIVOS_REPROVACAO[detail.motivoReprovacao] || detail.motivoReprovacao)
    : null;

  let qrBox = '';
  if (hasActiveQr) {
    qrBox = `
      <section class="card qr-showcase">
        <h2>Seu QR Code está pronto!</h2>
        <div class="qr-frame">
          <img src="data:image/png;base64,${esc(qr.qrCodeImagemBase64)}" alt="QR Code da devolução #${id}" width="210" height="210">
        </div>
        <div class="qr-order-meta">
          <strong>Pedido #${esc(detail.numeroPedido)}</strong>
          <span>${esc(detail.produto)}</span>
        </div>
        <p class="muted" style="font-size: 13px;">Apresente este código ao atendente da loja.</p>
        <div class="qr-validity-pill">⏱ Código válido por 24 horas</div>
        <p class="countdown" data-expires="${esc(qr.qrCodeExpiraEm)}"></p>
        <div style="margin-top: 14px; width: 100%;">
          <button type="button" class="button primary full" data-action="scroll-details">Ver detalhes</button>
        </div>
      </section>`;
  } else {
    let statusMessage = 'O produto já foi recebido na loja. Acompanhe o andamento ao lado.';
    if (beforeDelivery) statusMessage = 'O QR Code expirou. Você pode renová-lo gratuitamente dentro do prazo.';
    else if (detail.status === 'CANCELADA') statusMessage = 'Esta solicitação foi cancelada.';
    else if (detail.status === 'REPROVADA') statusMessage = 'A devolução não foi aprovada na conferência da loja.';

    qrBox = `
      <section class="card qr-showcase">
        <div class="item-icon" style="margin: 0 auto;">${icon(beforeDelivery ? 'qr' : 'check')}</div>
        <h2>${beforeDelivery ? 'QR Code expirado' : 'Status do atendimento'}</h2>
        <p class="muted" style="font-size: 13px;">${esc(statusMessage)}</p>
      </section>`;
  }

  let actions = '';
  if (beforeDelivery) {
    const renew = hasActiveQr
      ? ''
      : `<button type="button" class="button primary" data-action="renew" data-id="${id}">${icon('refresh')} Renovar QR Code</button>`;
    actions = `
      <div class="detail-actions">
        ${renew}
        <button type="button" class="button danger-outline" data-action="cancel" data-id="${id}">Cancelar solicitação</button>
      </div>`;
  }

  const rejectionNotice = detail.status === 'REPROVADA'
    ? `<div class="error-message" style="margin-top: 10px;">
        <strong>Devolução não aprovada na conferência:</strong> ${esc(reprovacaoLabel || 'Critérios não atendidos.')}
        ${detail.observacaoConferencia ? `<br><span>Observação da loja: ${esc(detail.observacaoConferencia)}</span>` : ''}
      </div>`
    : '';

  const timeline = history.itens.length
    ? `<ol class="timeline">${history.itens.map((e) => `
        <li><p>${esc(e.mensagem)}</p><time>${esc(formatDateTime(e.ocorridoEm))}</time></li>`).join('')}
      </ol>`
    : '<p class="muted">Ainda não há eventos registrados.</p>';

  return backButton('devolucoes', 'Todas as solicitações')
    + pageHeading(`SOLICITAÇÃO #${id}`, detail.produto, `Pedido #${detail.numeroPedido}`, false)
    + progressStepper(detail.status)
    + rejectionNotice
    + `
      <div class="detail-grid">
        <section id="request-details-card" class="card">
          <div class="order-card-row">
            <h3>Detalhes da solicitação</h3>
            ${statusTag(detail.status)}
          </div>
          <div class="summary-kv-list">
            <div class="summary-kv-row"><span>Solução</span><strong>${esc(solucaoLabel)}</strong></div>
            <div class="summary-kv-row"><span>Devolução</span><strong>Levar a uma loja Maga Next</strong></div>
            <div class="summary-kv-row"><span>Loja de entrega</span><strong>${esc(detail.loja)}<br><small class="muted">${esc(detail.enderecoLoja || '')}</small></strong></div>
            <div class="summary-kv-row"><span>Motivo informado</span><strong>${esc(motivoLabel)}</strong></div>
            <div class="summary-kv-row"><span>Quantidade</span><strong>${plural(detail.quantidade, 'unidade', 'unidades')}</strong></div>
            <div class="summary-kv-row"><span>Solicitada em</span><strong>${esc(formatDateTime(detail.criadaEm))}</strong></div>
            ${detail.recebidaEm ? `<div class="summary-kv-row"><span>Recebida na loja</span><strong>${esc(formatDateTime(detail.recebidaEm))}</strong></div>` : ''}
            ${detail.destino ? `<div class="summary-kv-row"><span>Destinação na loja</span><strong>${esc(DESTINATIONS[detail.destino] || detail.destino)}</strong></div>` : ''}
          </div>
          ${actions}
        </section>
        ${qrBox}
      </div>
      <section class="timeline-section">
        <h2 class="section-title">HISTÓRICO DA SOLICITAÇÃO</h2>
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
    : `<button type="button" class="text-button" data-action="read" data-id="${n.id}">Marcar como lida</button>`;

  return `
    <article class="card notification-card ${isRead ? '' : 'unread'}">
      <div class="notification-top">
        <span class="tag ${isRead ? 'gray' : ''}">${isRead ? 'Lida' : 'Novo aviso'}</span>
        <time>${esc(formatDateTime(n.criadaEm))}</time>
      </div>
      <p>${esc(n.mensagem)}</p>
      ${deadline}
      <div class="notification-actions">
        <button type="button" class="button secondary" data-action="detail" data-id="${n.solicitacaoId}">Ver solicitação</button>
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
    : emptyState('Tudo em dia', 'Quando houver uma atualização na sua troca ou devolução, ela aparecerá aqui.');

  return pageHeading('NOTIFICAÇÕES', 'Seus avisos', 'Atualizações de conferência e lembretes de prazo do QR Code.')
    + filter
    + list
    + pagination(page);
}

async function accountView() {
  const user = state.user;
  const isClient = user.perfil === 'CLIENTE';
  const initial = esc((user.nome || '?').slice(0, 1).toUpperCase());

  const isOng = user.perfil === 'ONG';
  let body = isOng
    ? '<p class="muted">Perfil de ONG parceira: acompanhe as doações encaminhadas pelas lojas e confirme o recebimento dos produtos.</p>'
    : '<p class="muted">Perfil habilitado para conferência física de produtos, leitura de QR Code e destinação socioambiental ou estoque.</p>';
  if (isClient) {
    const prefs = await api('/notificacoes/preferencias');
    body = `
      <form id="preferences-form">
        <label class="preference">
          <div>Atualizações da solicitação<p>Recebimento na loja, resultado da conferência e destinação.</p></div>
          <input name="atualizacoes" type="checkbox" ${prefs.atualizacoes ? 'checked' : ''}>
        </label>
        <label class="preference">
          <div>Lembretes de validade do QR Code<p>Aviso quando o QR Code estiver próximo de expirar.</p></div>
          <input name="lembretesQr" type="checkbox" ${prefs.lembretesQr ? 'checked' : ''}>
        </label>
        <button class="button primary full" type="submit">Salvar preferências</button>
      </form>`;
  }

  return pageHeading('PERFIL DE ACESSO', 'Sua conta', 'Confira seus dados e preferências no Maga Next.', false) + `
    <section class="card account-card">
      <div class="account-intro">
        <div class="account-avatar" aria-hidden="true">${initial}</div>
        <div>
          <h3>${esc(user.nome)}</h3>
          <p class="muted">${isClient ? 'Cliente E-commerce' : isOng ? 'ONG parceira' : `Atendente de Loja · ${esc(user.loja)}`}</p>
        </div>
      </div>
      ${body}
    </section>`;
}


// ---------------------------------------------------------------------------
// Telas do Funcionário (Moodboard 2 — Operação Loja em Azul-marinho)
// ---------------------------------------------------------------------------

function waitingTime(minutes) {
  if (minutes == null) return 'Recém-recebido';
  return minutes < 60 ? `${minutes} min aguardando` : `${Math.floor(minutes / 60)} h aguardando`;
}

function queueCard(d) {
  const motivo = MOTIVOS_DEVOLUCAO[d.motivo] || 'Devolução em loja';
  const resolucao = TIPOS_RESOLUCAO[d.tipoResolucao]?.title || 'Troca/Devolução';
  const imgUrl = productImageUrl(d.produto);

  const action = d.status === 'EM_ANALISE'
    ? `<button type="button" class="button primary full" data-action="inspect" data-id="${d.id}">
        Conferir produto
      </button>`
    : `<p class="small-note">Destinação registrada: <strong>${esc(DESTINATIONS[d.destino] || d.destino)}</strong></p>`;

  return `
    <article class="card">
      <div class="order-card-row">
        <span class="tag ${d.status === 'EM_ANALISE' ? 'green' : 'purple'}">
          ${d.status === 'EM_ANALISE' ? 'Aguardando conferência' : 'Destino definido'}
        </span>
        <span class="order-number">Pedido #${esc(d.numeroPedido)}</span>
      </div>
      <div style="display: flex; gap: 12px; align-items: center;">
        <div class="product-thumb">
          <img src="${imgUrl}" alt="${esc(d.produto)}" loading="lazy">
        </div>
        <div style="flex: 1; min-width: 0;">
          <h3 class="order-title">${esc(d.produto)}</h3>
          <p class="order-number">${esc(motivo)} · ${esc(resolucao)}</p>
          <p class="order-number">${esc(waitingTime(d.minutosDesdeRecebimento))}</p>
        </div>
        <div class="category-badge" aria-hidden="true">${icon(productGlyph(d.produto))}</div>
      </div>
      ${action}
    </article>`;
}

// Tela 1 do Moodboard 2 — Pedidos para conferência
async function queueView({ pagina = 0, status = '', destino = '', minimoHoras = '' } = {}) {
  const page = await api(`/funcionario/devolucoes?${toQueryString({ pagina, tamanho: 12, status, destino, minimoHoras })}`);

  const nextToInspect = page.itens.find((d) => d.status === 'EM_ANALISE');
  const featured = nextToInspect ? `
    <section class="featured-queue-card">
      <div class="product-visual">
        <span class="tag green featured-status-pill">Aguardando conferência</span>
        <img class="product-photo" src="${productImageUrl(nextToInspect.produto)}" alt="${esc(nextToInspect.produto)}">
      </div>
      <div class="featured-queue-body">
        <div class="order-card-row">
          <div>
            <span class="order-number">Pedido #${esc(nextToInspect.numeroPedido)}</span>
            <h2 style="font-size: 22px; margin-top: 2px;">${esc(nextToInspect.produto)}</h2>
            <p class="muted" style="font-size: 13px; margin-top: 2px;">Aguardando de devolução · ${esc(MOTIVOS_DEVOLUCAO[nextToInspect.motivo] || 'Verificação física')}</p>
          </div>
          <div class="category-badge">${icon(productGlyph(nextToInspect.produto))}</div>
        </div>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-top: 6px;">
          <button type="button" class="button primary full" data-action="inspect" data-id="${nextToInspect.id}">
            ${icon('qr')} Escanear QR Code / Conferir
          </button>
          <button type="button" class="button secondary full" data-action="navigate" data-view="receber">
            Ler outro QR Code
          </button>
        </div>
      </div>
    </section>` : '';

  const stageOptions = { EM_ANALISE: 'Aguardando conferência', DESTINO_PENDENTE: 'Destino definido' };
  const ageOptions = { 2: 'Mais de 2 horas', 24: 'Mais de 24 horas', 48: 'Mais de 48 horas' };

  const filters = `
    <form class="filters" id="queue-filter">
      <div class="field">
        <label for="queue-status">Etapa</label>
        <select id="queue-status" name="status">${selectOptions(stageOptions, status)}</select>
      </div>
      <div class="field">
        <label for="queue-dest">Destinação</label>
        <select id="queue-dest" name="destino">${selectOptions(DESTINATIONS, destino)}</select>
      </div>
      <div class="field">
        <label for="queue-age">Espera</label>
        <select id="queue-age" name="minimoHoras">${selectOptions(ageOptions, String(minimoHoras), 'Qualquer tempo')}</select>
      </div>
      <button type="submit" class="button secondary">Filtrar</button>
    </form>`;

  const list = page.itens.length
    ? `<div class="cards">${page.itens.map(queueCard).join('')}</div>`
    : emptyState('Nenhum pedido aguardando', 'Escaneie um QR Code no balcão para iniciar uma nova conferência.',
        navigateButton('receber', 'Escanear QR Code'));

  return pageHeading(state.user.loja || 'OPERAÇÃO DE LOJA', 'Pedidos para conferência', 'Acesse os pedidos que aguardam conferência na loja.')
    + featured
    + filters
    + list
    + pagination(page);
}

// Tela 2 do Moodboard 2 — Conferir produto
async function inspectView({ id } = {}) {
  const targetId = Number(id || state.inspection.id);
  const item = await api(`/funcionario/devolucoes/${targetId}`);

  if (state.inspection.id !== item.id) {
    state.inspection = {
      ...blankInspection(),
      id: item.id,
      item,
      checked: item.itensConferidos?.length ? [...item.itensConferidos] : Object.keys(CHECKLIST_ITEMS),
    };
  } else {
    state.inspection.item = item;
  }

  const motivo = MOTIVOS_DEVOLUCAO[item.motivo] || item.motivo || 'Tamanho diferente do esperado';
  const resolucao = TIPOS_RESOLUCAO[item.tipoResolucao]?.title || 'Troca ou Reembolso';
  const checkedSet = new Set(state.inspection.checked);

  const checklistHtml = Object.entries(CHECKLIST_ITEMS).map(([key, label]) => {
    const isChecked = checkedSet.has(key);
    return `
      <label class="check-item">
        <input type="checkbox" name="itemConferencia" value="${key}" ${isChecked ? 'checked' : ''}>
        <span class="check-item-title">${esc(label)}</span>
        <span class="check-hint">${isChecked ? 'Validado' : 'Toque p/ validar'}</span>
      </label>`;
  }).join('');

  const evidenceGallery = item.evidencias?.length
    ? `<div style="margin-top: 14px;">
        <span class="order-number">Fotos enviadas pelo cliente:</span>
        <div class="photo-grid">
          ${item.evidencias.map((src, i) => `<div class="photo-thumb"><img src="${esc(src)}" alt="Evidência ${i + 1}"></div>`).join('')}
        </div>
      </div>`
    : '';

  return `
    <div class="wizard-shell">
      <div class="wizard-topbar">
        ${backButton('fila', 'Pedidos para conferência')}
        <span class="step-pill">Etapa 1 de 2 · Conferência</span>
      </div>
      ${pageHeading('CONFERIR PRODUTO', 'Conferir produto', 'Verifique os itens recebidos antes de registrar a decisão.', false)}

      <div class="wizard-two-cols">
        <div>
          <div class="product-summary-card received-card">
            <div class="product-thumb">
              <img src="${productImageUrl(item.produto)}" alt="${esc(item.produto)}">
            </div>
            <div class="product-summary-text">
              <span class="eyebrow">PRODUTO RECEBIDO</span>
              <h3>${esc(item.produto)}</h3>
              <p class="order-link">Pedido #${esc(item.numeroPedido)}</p>
            </div>
            <div class="category-badge">${icon(productGlyph(item.produto))}</div>
          </div>

          <section class="section-block">
            <h3>Detalhes da solicitação</h3>
            <div class="request-detail-list">
              <div class="request-detail-item">
                <span class="detail-mini-icon">${icon('refresh')}</span>
                <div>
                  <span class="detail-mini-label">MOTIVO DA DEVOLUÇÃO</span>
                  <strong>${esc(item.comentario || motivo)}</strong>
                </div>
              </div>
              <div class="request-detail-item">
                <span class="detail-mini-icon">${icon('calendar')}</span>
                <div>
                  <span class="detail-mini-label">DATA DA SOLICITAÇÃO</span>
                  <strong>${esc(formatDate(item.criadaEm || item.recebidaEm))}</strong>
                </div>
              </div>
              <div class="request-detail-item">
                <span class="detail-mini-icon">${icon('returns')}</span>
                <div>
                  <span class="detail-mini-label">SOLUÇÃO ESCOLHIDA PELO CLIENTE</span>
                  <strong>${esc(resolucao)}</strong>
                </div>
              </div>
            </div>
            ${evidenceGallery}
          </section>
        </div>

        <form id="inspect-form" class="section-block">
          <h3>Conferência</h3>
          <p class="section-subtitle">Indique o status de cada etapa da conferência física:</p>
          <div class="checklist-group">${checklistHtml}</div>
          <div style="margin-top: 20px;">
            <button type="submit" class="button primary full">
              ${icon('checkCircle')} Concluir conferência
            </button>
          </div>
        </form>
      </div>
    </div>`;
}

// Telas 3, 4 e 5 do Moodboard 2 — Resultado da conferência (Aprovar vs Reprovar + Parceiros Socioambientais)
async function resultView({ id } = {}) {
  const targetId = Number(id || state.inspection.id);
  if (!state.inspection.item || state.inspection.id !== targetId) {
    state.inspection.item = await api(`/funcionario/devolucoes/${targetId}`);
    state.inspection.id = targetId;
  }

  const item = state.inspection.item;
  const isApproved = state.inspection.decisao !== 'REPROVAR';
  const destinoAtual = state.inspection.destino || 'ESTOQUE';
  const ongAtual = state.inspection.ongParceira || 'ECOSMAG';
  const motivoReprovAtual = state.inspection.motivoReprovacao || 'SINAIS_DE_USO';

  const ongSubSelector = destinoAtual === 'DOACAO_ONG' ? `
    <div class="ong-subpanel">
      <span class="ong-subtitle">Parceiros socioambientais</span>
      <div class="option-list">
        ${Object.values(ONGS_PARCEIRAS).map((ong) => `
          <label class="option-card ong-card ${ongAtual === ong.id ? 'selected' : ''}">
            <span class="option-icon ong-icon">${icon(ong.icon)}</span>
            <span class="option-body">
              <span class="option-title">${esc(ong.title)}</span>
              <span class="option-desc">${esc(ong.desc)}</span>
            </span>
            <input type="radio" name="ongParceira" value="${ong.id}" ${ongAtual === ong.id ? 'checked' : ''}>
          </label>
        `).join('')}
      </div>
    </div>` : '';

  const decisionPanel = isApproved ? `
    <section class="result-panel approved">
      <div class="result-banner">
        <div class="result-banner-icon">${icon('checkCircle')}</div>
        <div>
          <h3>Devolução aprovada</h3>
          <p class="muted" style="font-size: 12px;">Produto verificado e liberado para destinação</p>
        </div>
      </div>

      <h3 style="margin-bottom: 10px;">Destinação do produto</h3>
      <div class="option-list">
        <label class="option-card ${destinoAtual === 'ESTOQUE' ? 'selected' : ''}">
          <span class="option-icon">${icon('box')}</span>
          <span class="option-body">
            <span class="option-title">Retornar ao estoque</span>
            <span class="option-desc">Produto íntegro e pronto para nova venda na loja</span>
          </span>
          <input type="radio" name="destino" value="ESTOQUE" ${destinoAtual === 'ESTOQUE' ? 'checked' : ''}>
        </label>

        <label class="option-card ${destinoAtual === 'REPARO' ? 'selected' : ''}">
          <span class="option-icon">${icon('wrench')}</span>
          <span class="option-body">
            <span class="option-title">Encaminhar para reparo</span>
            <span class="option-desc">Assistência técnica e recondicionamento</span>
          </span>
          <input type="radio" name="destino" value="REPARO" ${destinoAtual === 'REPARO' ? 'checked' : ''}>
        </label>

        <label class="option-card ${destinoAtual === 'DOACAO_ONG' ? 'selected' : ''}">
          <span class="option-icon">${icon('leaf')}</span>
          <span class="option-body">
            <span class="option-title">Destinação socioambiental</span>
            <span class="option-desc">Doação e reutilização com parceiros socioambientais</span>
          </span>
          <input type="radio" name="destino" value="DOACAO_ONG" ${destinoAtual === 'DOACAO_ONG' ? 'checked' : ''}>
        </label>
      </div>

      ${ongSubSelector}

      <div style="margin-top: 20px;">
        <button type="submit" class="button primary full">
          ${icon('check')} Confirmar destinação
        </button>
      </div>
    </section>` : `
    <section class="result-panel rejected">
      <div class="result-banner">
        <div class="result-banner-icon">${icon('closeCircle')}</div>
        <div>
          <h3 style="color: var(--pink);">Devolução não aprovada</h3>
          <p class="muted" style="font-size: 12px;">O cliente receberá a orientação pelo aplicativo</p>
        </div>
      </div>

      <h3 style="margin-bottom: 10px;">Informe o motivo da reprovação</h3>
      <div class="option-list cols-2">
        ${Object.entries(MOTIVOS_REPROVACAO).map(([key, label]) => `
          <label class="option-card ${motivoReprovAtual === key ? 'selected' : ''}">
            <span class="option-body"><span class="option-title">${esc(label)}</span></span>
            <input type="radio" name="motivoReprovacao" value="${key}" ${motivoReprovAtual === key ? 'checked' : ''}>
          </label>
        `).join('')}
      </div>

      <div style="margin-top: 16px;">
        <label for="reject-notes">Observação para o registro (obrigatória se escolher "Outro")</label>
        <textarea id="reject-notes" name="observacao" maxlength="500"
                  placeholder="Descreva o que foi constatado na conferência...">${esc(state.inspection.observacao)}</textarea>
      </div>

      <div style="margin-top: 20px;">
        <button type="submit" class="button pink full">
          ${icon('flag')} Finalizar atendimento
        </button>
      </div>
    </section>`;

  return `
    <div class="wizard-shell">
      <div class="wizard-topbar">
        <button type="button" class="back-button" data-action="inspect" data-id="${item.id}">
          ${icon('arrow')} Voltar à conferência
        </button>
        <span class="step-pill">Etapa 2 de 2 · Decisão</span>
      </div>
      ${pageHeading('RESULTADO DA CONFERÊNCIA', 'Resultado da conferência', 'Registre a decisão final para este atendimento.', false)}

      <div class="wizard-grid">
        <aside>
          <div class="product-summary-card received-card">
            <div class="product-thumb">
              <img src="${productImageUrl(item.produto)}" alt="${esc(item.produto)}">
            </div>
            <div class="product-summary-text">
              <span class="eyebrow">PRODUTO RECEBIDO</span>
              <h3>${esc(item.produto)}</h3>
              <p class="order-link">Pedido #${esc(item.numeroPedido)}</p>
            </div>
            <div class="category-badge">${icon(productGlyph(item.produto))}</div>
          </div>

          <section class="section-block">
            <h3>O produto atende aos critérios de troca/devolução?</h3>
            <p class="section-subtitle">Indique o status final da conferência física:</p>
            <div class="status-pills-preview">
              <span class="tag green">Aprovado</span>
              <span class="tag pink">Reprovado</span>
            </div>
            <div class="option-list">
              <button type="button" class="decision-card approve ${isApproved ? 'active' : ''}" data-action="set-decision" data-value="APROVAR">
                <span class="decision-dot">${icon('check')}</span>
                <span class="decision-title">SIM – Aprovar devolução</span>
              </button>

              <button type="button" class="decision-card reject ${!isApproved ? 'active' : ''}" data-action="set-decision" data-value="REPROVAR">
                <span class="decision-dot">${icon('close')}</span>
                <span class="decision-title">NÃO – Reprovar devolução</span>
              </button>
            </div>
          </section>
        </aside>

        <form id="result-form">
          ${decisionPanel}
        </form>
      </div>
    </div>`;
}

// Tela 6 do Moodboard 2 — Atendimento concluído
async function completedView() {
  const info = state.inspection.resumoConclusao || {
    numeroPedido: '—',
    produto: 'Produto conferido',
    aprovada: true,
    detalhe: 'A decisão foi registrada com sucesso.',
  };

  return `
    <div class="completion-screen">
      <div class="completion-badge">${icon('check')}</div>
      <h1>Atendimento concluído</h1>
      <p>
        A decisão foi registrada com sucesso para o pedido <strong>#${esc(info.numeroPedido)}</strong>
        (${esc(info.produto)}).<br>${esc(info.detalhe)}
      </p>
      <button type="button" class="button full" data-action="navigate" data-view="fila">
        ${icon('refresh')} Processar outro pedido
      </button>
    </div>`;
}

async function receiveView() {
  return pageHeading('BALCÃO DA LOJA', 'Escanear QR Code', 'Leia o QR Code apresentado pelo cliente ou cole o código abaixo.', false) + `
    <div class="wizard-two-cols">
      <section class="section-block">
        <h3>Leitura pela câmera</h3>
        <p class="section-subtitle">Aponte a câmera do dispositivo para o QR Code na tela do cliente.</p>
        <div class="scan-options">
          <button type="button" class="button primary" data-action="camera">${icon('camera')} Abrir câmera</button>
          <button type="button" class="button outline" data-action="stop-camera" hidden>Fechar câmera</button>
        </div>
        <div id="camera-box" class="camera-box" hidden>
          <video id="camera-video" playsinline muted></video>
          <div class="camera-hint" aria-hidden="true"></div>
        </div>
        <p id="camera-status" class="notice" role="status" hidden></p>
      </section>

      <form id="receive-form" class="scan-form">
        <h3>Digitar ou colar código</h3>
        <p class="section-subtitle">Caso prefira no computador do balcão, informe o código UUID abaixo.</p>
        <label for="return-token">Código do QR Code</label>
        <input id="return-token" name="token" required maxlength="36" autocapitalize="none" spellcheck="false"
               autocomplete="off" placeholder="Ex.: 550e8400-e29b-41d4-a716-446655440000">
        <p class="muted" style="font-size: 13px; margin: 10px 0 18px;">
          O recebimento valida o QR Code do cliente e abre imediatamente a tela de conferência física.
        </p>
        <button type="submit" class="button primary full">Confirmar recebimento e conferir</button>
      </form>
    </div>`;
}

// ---------------------------------------------------------------------------
// Telas da ONG parceira
// ---------------------------------------------------------------------------

const DONATION_FILTERS = { DESTINO_PENDENTE: 'A receber', CONCLUIDA: 'Recebidas' };

function donationCard(d) {
  const done = d.status === 'CONCLUIDA';
  const action = done
    ? `<p class="small-note">Recebimento confirmado em <strong>${esc(formatDateTime(d.concluidaEm))}</strong>.</p>`
    : `<button type="button" class="button primary full" data-action="confirm-donation" data-id="${d.id}">Confirmar recebimento</button>`;

  return `
    <article class="card">
      <div class="order-card-row">
        <span class="tag ${done ? 'green' : 'orange'}">${done ? 'Recebida' : 'A receber'}</span>
        <span class="order-number">Pedido #${esc(d.numeroPedido)}</span>
      </div>
      <div class="donation-body">
        <div class="product-thumb">
          <img src="${productImageUrl(d.produto)}" alt="${esc(d.produto)}" loading="lazy">
        </div>
        <div class="donation-info">
          <h3 class="order-title">${esc(d.produto)}</h3>
          <p class="order-number">${esc(d.categoria || 'Sem categoria')} · ${plural(d.quantidade, 'unidade', 'unidades')}</p>
          <p class="order-number">Loja de origem: ${esc(d.loja)}</p>
        </div>
        <div class="category-badge" aria-hidden="true">${icon(productGlyph(d.produto))}</div>
      </div>
      ${action}
    </article>`;
}

async function donationsView({ pagina = 0, status = '' } = {}) {
  const page = await api(`/ong/doacoes?${toQueryString({ pagina, tamanho: 12, status })}`);

  const list = page.itens.length
    ? `<div class="cards">${page.itens.map(donationCard).join('')}</div>`
    : emptyState('Nenhuma doação por aqui', 'Quando uma loja encaminhar produtos de uma categoria aceita pela sua ONG, eles aparecerão aqui.');

  return pageHeading('DOAÇÕES', 'Doações encaminhadas à sua ONG', 'Confirme o recebimento quando os produtos chegarem.')
    + `<div class="chips" role="group" aria-label="Filtrar doações">${chips(DONATION_FILTERS, status, 'Todas', 'chip-donation')}</div>`
    + list
    + pagination(page);
}

async function confirmDonation(id) {
  const ok = await askConfirmation('Confirmar recebimento?',
    'Confirme que os produtos desta doação já chegaram à sua ONG. A doação será marcada como concluída.', 'Confirmar recebimento');
  if (!ok) return;
  await api(`/ong/doacoes/${id}/confirmar`, { method: 'POST' });
  toast('Recebimento confirmado. Obrigado!');
  await go('doacoes', state.params);
}


const VIEWS = {
  itens: itemsView,
  motivo: reasonView,
  evidencias: evidenceView,
  lojas: storesView,
  devolucoes: returnsView,
  detalhe: detailView,
  avisos: notificationsView,
  conta: accountView,
  doacoes: donationsView,
  fila: queueView,
  conferir: inspectView,
  resultado: resultView,
  concluido: completedView,
  receber: receiveView,
};


// ---------------------------------------------------------------------------
// Interações, cliques e submissões de formulário
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

async function whileBusy(button, work) {
  if (button.disabled) return;
  const originalHtml = button.innerHTML;
  const busyLabel = button.dataset.busyLabel;
  button.disabled = true;
  button.setAttribute('aria-busy', 'true');
  if (busyLabel) button.textContent = busyLabel;
  try {
    await work();
  } catch (error) {
    handleError(error);
  } finally {
    button.disabled = false;
    button.removeAttribute('aria-busy');
    if (busyLabel) button.innerHTML = originalHtml;
  }
}

async function getCurrentPosition() {
  if (!navigator.geolocation) throw new Error('Localização indisponível. Selecione uma loja da lista.');
  try {
    return await new Promise((resolve, reject) =>
      navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 12000, maximumAge: 60000 }));
  } catch {
    throw new Error('Não foi possível acessar sua localização. Escolha uma loja na lista abaixo.');
  }
}

async function showNearbyStores() {
  const { coords } = await getCurrentPosition();
  const query = toQueryString({ latitude: coords.latitude, longitude: coords.longitude, raioKm: 20 });
  const nearby = await api(`/lojas/proximas?${query}`);
  if (state.view === 'lojas') await go('lojas', { nearby });
}

async function submitReturnRequest() {
  const item = state.selectedItem;
  if (!item) return;

  const form = $('#resolution-form');
  if (form) {
    const data = new FormData(form);
    if (data.get('tipoResolucao')) state.draft.tipoResolucao = String(data.get('tipoResolucao'));
    if (data.get('lojaId')) state.draft.lojaId = Number(data.get('lojaId'));
  }

  if (!state.draft.lojaId) {
    throw new Error('Selecione uma loja para entregar o produto.');
  }

  const body = {
    itemCompradoId: item.id,
    lojaId: Number(state.draft.lojaId),
    motivo: state.draft.motivo || 'PRODUTO_COM_DEFEITO',
    comentario: state.draft.comentario?.trim() || null,
    tipoResolucao: state.draft.tipoResolucao || 'TROCA',
    evidencias: state.draft.evidencias || [],
  };

  const result = await api('/devolucoes', { method: 'POST', body });
  toast('Solicitação registrada! Seu QR Code está pronto.');
  await go('detalhe', { id: result.id });
}

async function renewQr(id) {
  await api(`/devolucoes/${id}/qrcode/renovar`, { method: 'POST' });
  toast('QR Code renovado com sucesso.');
  await go('detalhe', { id });
}

async function cancelReturn(id) {
  const ok = await askConfirmation('Cancelar solicitação?', 'O QR Code desta devolução deixará de funcionar.', 'Cancelar solicitação');
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

async function submitInspectionResult() {
  const id = state.inspection.id;
  const item = state.inspection.item;
  if (!id || !item) return;

  const form = $('#result-form');
  const data = form ? new FormData(form) : new FormData();

  if (state.inspection.decisao !== 'REPROVAR') {
    const destino = String(data.get('destino') || state.inspection.destino || 'ESTOQUE');
    const ongParceira = String(data.get('ongParceira') || state.inspection.ongParceira || 'ECOSMAG');
    const ongLabel = ONGS_PARCEIRAS[ongParceira]?.title || ongParceira;

    await api(`/funcionario/devolucoes/${id}/destino`, {
      method: 'POST',
      body: { destino, itensConferidos: state.inspection.checked },
    });
    const destDesc = destino === 'DOACAO_ONG'
      ? `Destinação socioambiental · Parceiro: ${ongLabel}`
      : (DESTINATIONS[destino] || destino);

    state.inspection.resumoConclusao = {
      numeroPedido: item.numeroPedido,
      produto: item.produto,
      aprovada: true,
      detalhe: `Devolução aprovada · ${destDesc}.`,
    };
    toast('Devolução aprovada e destinação confirmada.');
    await go('concluido');
  } else {
    const motivo = String(data.get('motivoReprovacao') || state.inspection.motivoReprovacao || 'SINAIS_DE_USO');
    const observacao = String(data.get('observacao') || '').trim();
    if (motivo === 'OUTRO' && !observacao) {
      throw new Error('Descreva na observação o motivo da reprovação.');
    }
    await api(`/funcionario/devolucoes/${id}/reprovar`, {
      method: 'POST',
      body: { motivo, observacao: observacao || null, itensConferidos: state.inspection.checked },
    });
    state.inspection.resumoConclusao = {
      numeroPedido: item.numeroPedido,
      produto: item.produto,
      aprovada: false,
      detalhe: `Devolução não aprovada (${MOTIVOS_REPROVACAO[motivo] || motivo}).`,
    };
    toast('Atendimento finalizado.');
    await go('concluido');
  }
}

async function receiveProduct(token) {
  if (!isValidToken(token)) {
    throw new Error('Confira o código. Ele deve ser o UUID completo do QR Code da devolução.');
  }
  const result = await api('/funcionario/devolucoes/escanear', { method: 'POST', body: { token } });
  stopCamera();
  toast('Produto recebido! Iniciando conferência física.');
  state.inspection = blankInspection();
  await go('conferir', { id: result.id });
}

$('#navigation').addEventListener('click', (event) => {
  const button = event.target.closest('[data-view]');
  if (!button) return;
  $('#header-search-input').value = '';
  navDirection = 'menu';
  go(button.dataset.view);
});

$('#content').addEventListener('click', (event) => {
  const button = event.target.closest('button[data-action]');
  if (!button || button.disabled) return;
  const { action, id, view, page, value, index } = button.dataset;
  if (button.classList.contains('back-button')) navDirection = 'back';

  switch (action) {
    case 'navigate': return go(view);
    case 'refresh': return go(state.view, state.params);
    case 'chip': return go('devolucoes', { status: value });
    case 'chip-unread': return go('avisos', { naoLidas: value === '1' });
    case 'chip-donation': return go('doacoes', { status: value });
    case 'page': return go(state.view, { ...state.params, pagina: Number(page) });
    case 'history-page': return go('detalhe', { ...state.params, histPagina: Number(page) });
    case 'detail': return go('detalhe', { id });
    case 'stop-camera': return stopCamera();

    case 'scroll-details': {
      const card = $('#request-details-card');
      if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }

    case 'select-item':
      state.selectedItem = state.items.find((i) => String(i.id) === id);
      state.draft = blankDraft();
      return go('motivo');

    case 'pick-photo': {
      const commentBox = $('#evidence-comment');
      if (commentBox) state.draft.comentario = commentBox.value;
      if ((state.draft.evidencias || []).length >= 3) {
        toast('Você já adicionou o limite máximo de 3 fotos.', true);
        return;
      }
      $('#photo-input')?.click();
      return;
    }

    case 'remove-photo': {
      const commentBox = $('#evidence-comment');
      if (commentBox) state.draft.comentario = commentBox.value;
      state.draft.evidencias.splice(Number(index), 1);
      return go('evidencias');
    }

    case 'video-info':
      toast('Dica: adicione até 3 fotos nítidas do produto. O suporte a vídeo estará disponível em breve.');
      return;

    case 'focus-comment':
      $('#evidence-comment')?.focus();
      return;

    case 'inspect':
      return go('conferir', { id: Number(id) });

    case 'set-decision': {
      const notes = $('#reject-notes');
      if (notes) state.inspection.observacao = notes.value;
      state.inspection.decisao = value;
      return go('resultado', { id: state.inspection.id }, { keepScroll: true });
    }
  }

  const handlers = {
    camera: () => startCamera(),
    nearby: () => showNearbyStores(),
    renew: () => renewQr(id),
    cancel: () => cancelReturn(id),
    read: () => markNotificationRead(id),
    'confirm-donation': () => confirmDonation(id),
  };
  if (handlers[action]) whileBusy(button, handlers[action]);
});

$('#content').addEventListener('change', async (event) => {
  const target = event.target;

  // Compressão e adição de fotos na Etapa 2
  if (target.id === 'photo-input' && target.files?.length) {
    const commentBox = $('#evidence-comment');
    if (commentBox) state.draft.comentario = commentBox.value;

    const remaining = 3 - state.draft.evidencias.length;
    const files = Array.from(target.files).slice(0, remaining);
    try {
      for (const file of files) {
        const dataUrl = await compressImageFile(file);
        state.draft.evidencias.push(dataUrl);
      }
      toast('Foto adicionada às evidências.');
      await go('evidencias');
    } catch (error) {
      toast(error.message, true);
    }
    return;
  }

  // Atualiza visualmente o texto "Validado" / "Toque p/ validar" na checklist do funcionário
  if (target.name === 'itemConferencia') {
    const hint = target.closest('.check-item')?.querySelector('.check-hint');
    if (hint) hint.textContent = target.checked ? 'Validado' : 'Toque p/ validar';
    return;
  }

  // O destaque inicial vem do HTML (.selected); ao trocar de opcao ele precisa acompanhar o radio marcado
  if (target.type === 'radio' && target.name) {
    document.querySelectorAll(`input[type="radio"][name="${target.name}"]`).forEach((radio) => {
      radio.closest('.option-card')?.classList.toggle('selected', radio.checked);
    });
  }

  if (target.name === 'destino') {
    state.inspection.destino = target.value;
    if (state.view === 'resultado') await go('resultado', { id: state.inspection.id }, { keepScroll: true });
    return;
  } else if (target.name === 'ongParceira') {
    state.inspection.ongParceira = target.value;
  } else if (target.name === 'motivoReprovacao') {
    state.inspection.motivoReprovacao = target.value;
  } else if (target.name === 'tipoResolucao') {
    state.draft.tipoResolucao = target.value;
  } else if (target.name === 'lojaId') {
    state.draft.lojaId = Number(target.value);
  }

  const form = target.closest('#queue-filter');
  if (form && target.matches('select')) form.requestSubmit();
});

async function savePreferences(data) {
  const body = { atualizacoes: data.has('atualizacoes'), lembretesQr: data.has('lembretesQr') };
  await api('/notificacoes/preferencias', { method: 'PUT', body });
  toast('Preferências salvas.');
}

$('#content').addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.target;
  const data = new FormData(form);
  const button = form.querySelector('button[type=submit], button:not([type])');

  if (form.id === 'reason-form') {
    state.draft.motivo = String(data.get('motivo') || 'PRODUTO_COM_DEFEITO');
    state.draft.comentario = String(data.get('comentario') || '').trim();
    go('evidencias');
    return;
  }

  if (form.id === 'evidence-form') {
    state.draft.comentario = String(data.get('comentario') || '').trim();
    go('lojas');
    return;
  }

  if (form.id === 'inspect-form') {
    state.inspection.checked = data.getAll('itemConferencia').map(String);
    state.inspection.decisao = state.inspection.checked.length === Object.keys(CHECKLIST_ITEMS).length
      ? 'APROVAR'
      : 'REPROVAR';
    go('resultado', { id: state.inspection.id });
    return;
  }

  if (form.id === 'queue-filter') {
    go('fila', Object.fromEntries(data));
    return;
  }

  if (!button) return;

  whileBusy(button, async () => {
    if (form.id === 'resolution-form') await submitReturnRequest();
    else if (form.id === 'result-form') await submitInspectionResult();
    else if (form.id === 'preferences-form') await savePreferences(data);
    else if (form.id === 'receive-form') await receiveProduct(String(data.get('token')).trim());
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
      const password = $('#password').value;
      $('#password').value = '';
      await performLogin($('#username').value.trim(), password);
    } catch (error) {
      const wrongCredentials = error.status === 401 && !error.fromServer;
      $('#login-error').textContent = wrongCredentials ? 'Login ou senha inválidos.' : error.message;
      $('#login-error').hidden = false;
      $('#password').value = '';
    }
  });
});

// ---------------------------------------------------------------------------
// Cadastro (cliente, funcionário e ONG)
// ---------------------------------------------------------------------------

async function performLogin(username, password) {
  csrf = await fetchCsrfToken();
  await api('/sessao', { method: 'POST', body: new URLSearchParams({ username, password }) });
  // O Spring troca o token CSRF depois do login.
  csrf = await fetchCsrfToken();
  await enter(await api('/sessao'));
}

const REGISTER_KINDS = { cliente: 'Cliente', funcionario: 'Funcionário', ong: 'ONG' };
const registerState = { kind: 'cliente', options: null };

function registerInput(name, label, { type = 'text', autocomplete = 'off', maxlength = 255, hint = '', inputmode = '', placeholder = '' } = {}) {
  return `
    <label for="reg-${name}">${esc(label)}</label>
    <input id="reg-${name}" name="${name}" type="${type}" autocomplete="${autocomplete}" maxlength="${maxlength}"
           ${inputmode ? `inputmode="${inputmode}"` : ''} placeholder="${esc(placeholder)}" autocapitalize="none" spellcheck="false">
    ${hint ? `<p class="field-hint">${esc(hint)}</p>` : ''}`;
}

function registerFields(kind, options) {
  const access =
    registerInput('login', 'Login', { autocomplete: 'username', maxlength: 50, hint: 'Letras minúsculas, números, ponto, hífen ou sublinhado.' })
    + registerInput('senha', 'Senha', { type: 'password', autocomplete: 'new-password', maxlength: 72, hint: 'No mínimo 8 caracteres.' })
    + registerInput('senhaConfirmacao', 'Repita a senha', { type: 'password', autocomplete: 'new-password', maxlength: 72 });

  if (kind === 'funcionario') {
    const stores = (options?.lojas || []).map((l) => `<option value="${l.id}">${esc(l.nome)}</option>`).join('');
    return registerInput('nome', 'Nome completo', { autocomplete: 'name', maxlength: 150 })
      + registerInput('matricula', 'Matrícula', { maxlength: 50 })
      + `<label for="reg-lojaId">Loja onde trabalha</label>
         <select id="reg-lojaId" name="lojaId"><option value="">Escolha a loja</option>${stores}</select>`
      + registerInput('codigoConvite', 'Código de convite', { type: 'password', maxlength: 100, hint: 'Fornecido pela administração da loja.' })
      + access;
  }

  if (kind === 'ong') {
    const categories = (options?.categorias || []).map((c) => `
      <label class="check-option"><input type="checkbox" name="categorias" value="${esc(c)}">${esc(c)}</label>`).join('');
    return registerInput('nome', 'Nome da ONG', { maxlength: 255 })
      + registerInput('cnpj', 'CNPJ', { inputmode: 'numeric', maxlength: 18, placeholder: '00.000.000/0000-00' })
      + `<span class="field-label">Categorias de produto que a ONG aceita</span>
         <div class="check-grid">${categories}</div>`
      + registerInput('codigoConvite', 'Código de convite', { type: 'password', maxlength: 100, hint: 'Fornecido pela administração.' })
      + access;
  }

  return registerInput('nome', 'Nome completo', { autocomplete: 'name', maxlength: 150 })
    + registerInput('cpf', 'CPF', { inputmode: 'numeric', maxlength: 14, placeholder: '000.000.000-00' })
    + registerInput('email', 'E-mail', { type: 'email', autocomplete: 'email', maxlength: 255 })
    + registerInput('telefone', 'Telefone (opcional)', { type: 'tel', autocomplete: 'tel', maxlength: 20 })
    + access;
}

function renderRegister() {
  const { kind, options } = registerState;
  const allowed = Object.keys(REGISTER_KINDS).filter((k) =>
    k === 'cliente' || (k === 'funcionario' && options?.cadastroFuncionario) || (k === 'ong' && options?.cadastroOng));

  const tabs = $('#register-tabs');
  tabs.hidden = allowed.length < 2;
  tabs.innerHTML = allowed.map((k) => `
    <button type="button" class="role-tab" role="tab" data-kind="${k}" aria-selected="${k === kind}">${REGISTER_KINDS[k]}</button>`).join('');

  $('#register-form').innerHTML = registerFields(kind, options) + `
    <p id="register-error" class="error-message" role="alert" hidden></p>
    <button class="button primary full" type="submit">Criar conta</button>`;
}

async function openRegister() {
  $('#login-card').hidden = true;
  $('#register-card').hidden = false;
  registerState.kind = 'cliente';
  registerState.options = null;
  renderRegister();
  try {
    registerState.options = await api('/cadastro/opcoes');
    // Só redesenha se houver outros tipos de conta, para não apagar o que a pessoa já digitou.
    if (registerState.options.cadastroFuncionario || registerState.options.cadastroOng) renderRegister();
  } catch {
    // Sem as opções, o cadastro de cliente continua disponível.
  }
}

function closeRegister() {
  const card = $('#register-card');
  if (!card) return;
  card.hidden = true;
  $('#login-card').hidden = false;
  $('#register-form').replaceChildren();
}

function registerBody(kind, data) {
  const text = (name) => String(data.get(name) ?? '').trim();
  const access = { login: text('login'), senha: String(data.get('senha') ?? '') };
  if (kind === 'funcionario') {
    return { nome: text('nome'), matricula: text('matricula'), lojaId: text('lojaId') ? Number(text('lojaId')) : null,
      codigoConvite: text('codigoConvite'), ...access };
  }
  if (kind === 'ong') {
    return { nome: text('nome'), cnpj: text('cnpj'), categorias: data.getAll('categorias'),
      codigoConvite: text('codigoConvite'), ...access };
  }
  return { nome: text('nome'), cpf: text('cpf'), email: text('email'), telefone: text('telefone'), ...access };
}

$('#open-register').addEventListener('click', openRegister);
$('#back-to-login').addEventListener('click', closeRegister);

$('#register-tabs').addEventListener('click', (event) => {
  const tab = event.target.closest('[data-kind]');
  if (!tab || tab.dataset.kind === registerState.kind) return;
  registerState.kind = tab.dataset.kind;
  renderRegister();
});

$('#register-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = new FormData(form);
  const button = form.querySelector('[type=submit]');
  const showError = (message) => {
    const box = $('#register-error');
    box.textContent = message;
    box.hidden = false;
  };

  whileBusy(button, async () => {
    $('#register-error').hidden = true;
    if (String(data.get('senha') ?? '') !== String(data.get('senhaConfirmacao') ?? '')) {
      showError('As senhas não conferem.');
      return;
    }

    const body = registerBody(registerState.kind, data);
    try {
      csrf = await fetchCsrfToken();
      await api(`/cadastro/${registerState.kind}`, { method: 'POST', body });
    } catch (error) {
      showError(error.message);
      return;
    }

    try {
      await performLogin(body.login, body.senha);
      toast('Cadastro concluído! Bem-vindo(a) ao Maga Next.');
    } catch {
      closeRegister();
      $('#username').value = body.login;
      toast('Cadastro concluído! Entre com seu login e senha.');
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
    throw new Error('A câmera precisa de uma conexão HTTPS ou localhost. Você também pode colar o código.');
  }

  const decode = await loadQrDecoder();
  if (userLeft()) return;

  let stream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: { facingMode: { ideal: 'environment' } } });
  } catch {
    throw new Error('Não foi possível acessar a câmera. Verifique a permissão ou cole o código.');
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
  status.textContent = 'Aponte a câmera para o QR Code apresentado pelo cliente.';
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
        status.textContent = 'Código lido com sucesso! Confirme abaixo para iniciar a conferência.';
        return;
      }
      if (token) status.textContent = 'Este QR não parece ser uma devolução Maga Next.';
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
// Contagem regressiva do QR Code
// ---------------------------------------------------------------------------

let lastExpiredQr = '';

function updateCountdowns() {
  document.querySelectorAll('[data-expires]').forEach((el) => {
    const remaining = new Date(el.dataset.expires) - Date.now();
    if (Number.isNaN(remaining)) return;

    if (remaining <= 0) {
      el.textContent = 'QR Code expirado';
      el.classList.add('expired');
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
// Sessão parada: avisa aos 28 min e encerra aos 30 min
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
// Inicialização
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

const canUseServiceWorker = location.protocol === 'https:' || ['localhost', '127.0.0.1'].includes(location.hostname);
if ('serviceWorker' in navigator && canUseServiceWorker) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}));
}
