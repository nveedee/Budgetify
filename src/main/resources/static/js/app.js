// Relativ, damit es auf localhost UND über die PC-IP vom Handy funktioniert
const API = '/api';

let state = {
  konten: [],
  transaktionen: [],
  budgets: [],
  dauerauftraege: [],
  produkte: [],
  transakTyp: 'eingang',
  scannerStream: null,
  kaufKontoId: null,              // gewähltes Konto für Quick-Buy
  aktuellerMonat: new Date().getMonth() + 1,
  aktuellesJahr: new Date().getFullYear()
};

// ===== INIT =====
async function init() {
  document.getElementById('aktuelles-datum').textContent =
    new Date().toLocaleDateString('de-CH', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  document.getElementById('t-datum').value = new Date().toISOString().split('T')[0];

  await ladeDaten();
  renderDashboard();
  await renderBudgets();
  renderTransaktionen();
  renderInventar();
  setupMonatFilter();

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js');
  }
}

// ===== DATEN LADEN =====
async function ladeDaten() {
  try {
    const [k, t, b, d, p] = await Promise.all([
      fetch(`${API}/konten`).then(r => r.json()),
      fetch(`${API}/transaktionen`).then(r => r.json()),
      fetch(`${API}/budgets`).then(r => r.json()),
      fetch(`${API}/dauerauftraege`).then(r => r.json()),
      fetch(`${API}/produkte`).then(r => r.json())
    ]);
    state.konten = k;
    state.transaktionen = t;
    state.budgets = b;
    state.dauerauftraege = d;
    state.produkte = p;
  } catch (e) {
    console.error('API nicht erreichbar:', e);
  }
}

// ===== NAVIGATION =====
function showPage(name, btn) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('page-' + name).classList.add('active');
  btn.classList.add('active');

  const fab = document.getElementById('fab-btn');
  fab.style.display = (name === 'scanner' || name === 'mehr' || name === 'stats') ? 'none' : 'flex';

  if (name === 'scanner') stopScanner();
  if (name === 'stats') renderStats();
}

// ===== DASHBOARD =====
function renderDashboard() {
  // Gesamtvermögen
  const total = state.konten.reduce((s, k) => s + (k.kontostand || 0), 0);
  document.getElementById('total-vermoegen').textContent = formatCHF(total);

  // Konten
  const grid = document.getElementById('konto-grid');
  grid.innerHTML = state.konten.map(k => `
    <div class="konto-card" style="border-left-color:${k.farbe || '#6366f1'}">
      <div class="konto-name">${k.name}</div>
      <div class="konto-betrag">${formatCHF(k.kontostand)}</div>
    </div>
  `).join('') || '<p style="color:var(--text2);font-size:14px">Noch keine Konten. Unter "Mehr" hinzufügen.</p>';

  // Daueraufträge
  const daListe = document.getElementById('dauerauftrag-liste');
  const daTotal = state.dauerauftraege.reduce((s, d) => s + (d.betrag || 0), 0);
  document.getElementById('dauerauftrag-summe').textContent = `- ${formatCHF(daTotal)}/Mt.`;
  daListe.innerHTML = state.dauerauftraege.map(d => `
    <div class="transaktion-item">
      <div class="transaktion-icon"></div>
      <div class="transaktion-info">
        <div class="transaktion-name">${d.beschreibung}</div>
        <div class="transaktion-datum">Am ${d.tagImMonat}. jeden Monats</div>
      </div>
      <div class="transaktion-betrag minus">-${formatCHF(d.betrag)}</div>
    </div>
  `).join('') || '<p style="color:var(--text2);font-size:14px">Keine Daueraufträge</p>';

  // Letzte Transaktionen (5 Stück)
  const liste = document.getElementById('letzte-transaktionen');
  const letzte = [...state.transaktionen].sort((a, b) =>
    new Date(b.datum) - new Date(a.datum)).slice(0, 5);
  liste.innerHTML = letzte.map(t => renderTransaktionItem(t)).join('') ||
    '<p style="color:var(--text2);font-size:14px">Noch keine Transaktionen</p>';
}

// ===== TRANSAKTIONEN =====
function setupMonatFilter() {
  const chips = document.getElementById('monat-filter-chips');
  const monate = ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'];
  chips.innerHTML = monate.map((m, i) => `
    <div class="chip ${i + 1 === state.aktuellerMonat ? 'selected' : ''}"
         onclick="filterMonat(${i + 1}, this)">${m}</div>
  `).join('');
}

function filterMonat(monat, el) {
  state.aktuellerMonat = monat;
  document.querySelectorAll('#monat-filter-chips .chip').forEach(c => c.classList.remove('selected'));
  el.classList.add('selected');
  renderTransaktionen();
}

function renderTransaktionen() {
  const liste = document.getElementById('transaktionen-liste');
  const gefiltert = state.transaktionen.filter(t => {
    const d = new Date(t.datum);
    return d.getMonth() + 1 === state.aktuellerMonat && d.getFullYear() === state.aktuellesJahr;
  }).sort((a, b) => new Date(b.datum) - new Date(a.datum));

  const gruppen = gruppiereTransaktionen(gefiltert);
  liste.innerHTML = gruppen.map(g => renderTransaktionGruppe(g)).join('') ||
    '<p style="color:var(--text2);font-size:14px">Keine Transaktionen in diesem Monat</p>';
}

// Fasst identische Einträge (gleiche Beschreibung + gleicher Betrag) zusammen
function gruppiereTransaktionen(list) {
  const map = new Map();
  const reihenfolge = [];
  for (const t of list) {
    const key = `${t.beschreibung}|${t.betrag}|${t.kategorie || ''}`;
    if (!map.has(key)) {
      map.set(key, { ...t, count: 0, total: 0, letztes: t.datum });
      reihenfolge.push(key);
    }
    const g = map.get(key);
    g.count += 1;
    g.total += Number(t.betrag);
    if (new Date(t.datum) > new Date(g.letztes)) g.letztes = t.datum;
  }
  return reihenfolge.map(k => map.get(k))
    .sort((a, b) => new Date(b.letztes) - new Date(a.letztes));
}

function renderTransaktionGruppe(g) {
  const positiv = g.total >= 0;
  const klasse = positiv ? 'plus' : 'minus';
  const datum = new Date(g.letztes).toLocaleDateString('de-CH', { day: '2-digit', month: '2-digit' });
  const name = g.count > 1 ? `${g.beschreibung} <span class="tx-count">×${g.count}</span>` : g.beschreibung;
  const kat = g.kategorie || g.konto?.name || '';
  const sub = g.count > 1
    ? `${datum} · ${kat} · je ${formatCHF(g.betrag)}`
    : `${datum} · ${kat}`;
  return `
    <div class="transaktion-item">
      <div class="transaktion-icon"></div>
      <div class="transaktion-info">
        <div class="transaktion-name">${name}</div>
        <div class="transaktion-datum">${sub}</div>
      </div>
      <div class="transaktion-betrag ${klasse}">${positiv ? '+' : ''}${formatCHF(g.total)}</div>
    </div>
  `;
}

// Einzel-Item (für Dashboard "Letzte Transaktionen")
function renderTransaktionItem(t) {
  const positiv = t.betrag >= 0;
  const klasse = positiv ? 'plus' : 'minus';
  const datum = new Date(t.datum).toLocaleDateString('de-CH', { day: '2-digit', month: '2-digit' });
  return `
    <div class="transaktion-item">
      <div class="transaktion-icon"></div>
      <div class="transaktion-info">
        <div class="transaktion-name">${t.beschreibung}</div>
        <div class="transaktion-datum">${datum} · ${t.kategorie || t.konto?.name || ''}</div>
      </div>
      <div class="transaktion-betrag ${klasse}">${positiv ? '+' : ''}${formatCHF(t.betrag)}</div>
    </div>
  `;
}

// ===== BUDGETS =====
async function renderBudgets() {
  const label = document.getElementById('budget-monat-label');
  const liste = document.getElementById('budget-liste');
  const monatNamen = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
    'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'];
  label.textContent = `${monatNamen[state.aktuellerMonat - 1]} ${state.aktuellesJahr}`;

  if (state.budgets.length === 0) {
    liste.innerHTML = '<p style="color:var(--text2);font-size:14px">Keine Budgets definiert. Unter "Mehr" hinzufügen.</p>';
    return;
  }

  try {
    const uebersicht = await fetch(
      `${API}/budgets/uebersicht/${state.aktuellesJahr}/${state.aktuellerMonat}`
    ).then(r => r.json());

    liste.innerHTML = uebersicht.map(item => {
      const prozent = Number(item.prozent);          // echter Wert, kann > 100 sein
      const pct = Math.min(prozent, 100);            // Balkenbreite max. 100%
      const klasse = prozent >= 100 ? 'danger' : prozent >= 75 ? 'warning' : '';
      const ueber = prozent >= 100;
      const verbleibendNeg = Number(item.verbleibend) < 0;
      return `
        <div class="budget-item ${ueber ? 'over' : ''}">
          <div class="budget-header">
            <div class="budget-name">
              ${item.budget.icon ? item.budget.icon + ' ' : ''}${item.budget.kategorie}
              ${ueber ? '<span class="budget-badge-over">Überschritten</span>' : ''}
            </div>
            <div class="budget-amounts">${formatCHF(item.ausgegeben)} / ${formatCHF(item.budget.limitProMonat)}</div>
          </div>
          <div class="progress-bar">
            <div class="progress-fill ${klasse}" style="width:${pct}%"></div>
          </div>
          <div class="budget-verbleibend ${verbleibendNeg ? 'neg' : ''}">
            ${formatCHF(item.verbleibend)} verbleibend
          </div>
        </div>
      `;
    }).join('');
  } catch (e) {
    liste.innerHTML = '<p style="color:var(--text2)">Budget-Daten konnten nicht geladen werden.</p>';
  }
}

// ===== SCANNER (ZXing — läuft in allen Browsern, inkl. iPhone über HTTPS) =====
let zxingReader = null;

async function startScanner() {
  const hint = document.getElementById('scanner-hint');
  const video = document.getElementById('scanner-video');

  if (typeof ZXing === 'undefined') {
    hint.textContent = 'Scanner-Bibliothek nicht geladen';
    return;
  }
  // Kamera braucht einen sicheren Kontext (HTTPS oder localhost)
  if (!window.isSecureContext) {
    hint.textContent = 'Kamera nur über HTTPS — bitte "Manuell" nutzen';
    showToast('Kamera-Scan braucht HTTPS (am Handy: Railway-URL)', true);
    return;
  }

  try {
    hint.textContent = 'Kamera wird gestartet…';
    zxingReader = new ZXing.BrowserMultiFormatReader();
    video.style.display = 'block';

    // Rückkamera bevorzugen
    const constraints = { video: { facingMode: 'environment' } };
    await zxingReader.decodeFromConstraints(constraints, video, (result, err) => {
      if (result) {
        const code = result.getText();
        stopScanner();
        verarbeiteBarcode(code);
      }
      // err pro Frame ohne Treffer ist normal -> ignorieren
    });
    hint.textContent = 'Barcode in den Rahmen halten';
  } catch (e) {
    hint.textContent = 'Kamera-Zugriff nicht möglich — "Manuell" nutzen';
    showToast('Kamera-Zugriff verweigert oder nicht verfügbar', true);
  }
}

function stopScanner() {
  if (zxingReader) {
    try { zxingReader.reset(); } catch {}
    zxingReader = null;
  }
  if (state.scannerStream) {
    state.scannerStream.getTracks().forEach(t => t.stop());
    state.scannerStream = null;
  }
  const video = document.getElementById('scanner-video');
  if (video) video.style.display = 'none';
  const hint = document.getElementById('scanner-hint');
  if (hint) hint.textContent = 'Kamera starten';
}

async function verarbeiteBarcode(barcode) {
  document.getElementById('scanner-hint').textContent = `Suche "${barcode}"...`;
  const box = document.getElementById('gemini-result-box');
  if (box) box.style.display = 'none';
  try {
    const result = await fetch(`${API}/produkte/scan/${barcode}`).then(r => r.json());
    zeigeProduktModal(result, barcode);
  } catch (e) {
    showToast('Produktsuche fehlgeschlagen — bitte manuell eingeben', true);
    zeigeProduktModal({}, barcode);
  } finally {
    document.getElementById('scanner-hint').textContent = 'Kamera starten';
  }
}

// Öffnet das Produkt-Modal, vorausgefüllt mit den Scan-Daten (Name/Preis/Kategorie/Bild)
function zeigeProduktModal(data = {}, barcode = '') {
  document.getElementById('p-name').value = data.name || '';
  document.getElementById('p-preis').value =
    (data.preis != null && data.preis !== '') ? Number(data.preis).toFixed(2) : '';
  document.getElementById('p-kategorie').value = data.kategorie || '';
  document.getElementById('p-barcode').value = data.barcode || barcode || '';

  const sel = document.getElementById('p-gekauftBei');
  if (sel) sel.value = data.gekauftBei || 'Migros';

  const bild = document.getElementById('p-bild');
  if (bild) {
    if (data.bildUrl) {
      bild.innerHTML = `<img src="${data.bildUrl}" alt="${data.name || ''}">`;
      bild.style.display = 'block';
    } else {
      bild.innerHTML = '';
      bild.style.display = 'none';
    }
  }
  openModal('modal-produkt');
}

function manuellEingeben(barcode = '') {
  zeigeProduktModal({}, barcode);
}

function renderInventar() {
  const liste = document.getElementById('inventar-liste');
  if (state.produkte.length === 0) {
    liste.innerHTML = '<p style="color:var(--text2);font-size:14px;margin-top:16px">Noch keine Produkte im Inventar</p>';
    return;
  }
  // Nach kaufCount absteigend sortieren (meistgekauft zuerst)
  const sorted = [...state.produkte].sort((a, b) => (b.kaufCount || 0) - (a.kaufCount || 0));
  // Konto-Vorauswahl initialisieren (erstes Konto), falls noch nicht gesetzt
  if (state.kaufKontoId == null && state.konten[0]) state.kaufKontoId = state.konten[0].id;

  // "Zahlen von"-Selektor: Konto für Quick-Buy wählen statt blind erstes
  const kontoSelector = state.konten.length ? `
    <div class="kauf-konto-row">
      <span class="kauf-konto-label">Zahlen von</span>
      <select class="kauf-konto-select" onchange="state.kaufKontoId = this.value">
        ${state.konten.map(k => `<option value="${k.id}" ${k.id == state.kaufKontoId ? 'selected' : ''}>${k.name}</option>`).join('')}
      </select>
    </div>` : '';

  liste.innerHTML = `
    <div class="section-header" style="margin-top:20px">Inventar (${state.produkte.length})</div>
    ${kontoSelector}
    ${sorted.map(p => `
      <div class="produkt-card">
        <div class="produkt-info">
          <div class="produkt-name">${p.name}</div>
          <div class="produkt-meta">${formatCHF(p.preis)} · ${p.kategorie || 'Keine Kategorie'}${p.gekauftBei ? ' · ' + p.gekauftBei : ''}</div>
          <span class="produkt-badge" data-kauf-badge="${p.id}">× ${p.kaufCount || 0} mal gekauft</span>
        </div>
        <button class="btn-kaufen" onclick="kaufeProdukt(${p.id}, this)">Kaufen</button>
      </div>
    `).join('')}`;
}

// Setzt einen "Sicher?"-Button zurück in den Normalzustand
function resetKaufBtn(btn) {
  if (!btn) return;
  clearTimeout(btn._confirmTimer);
  btn.dataset.confirm = '';
  btn.classList.remove('confirming');
  btn.textContent = 'Kaufen';
}

// Quick-Buy mit Confirm (2 Stufen) + Doppelklick-Schutz
async function kaufeProdukt(id, btn) {
  // 1. Stufe: erster Klick fragt nach ("Sicher?"), 2. Klick bestätigt
  if (btn && btn.dataset.confirm !== 'yes') {
    // andere offene Confirms zurücksetzen
    document.querySelectorAll('.btn-kaufen.confirming').forEach(resetKaufBtn);
    btn.dataset.confirm = 'yes';
    btn.textContent = 'Sicher?';
    btn.classList.add('confirming');
    btn._confirmTimer = setTimeout(() => resetKaufBtn(btn), 3000);
    return;
  }
  // bestätigt: Confirm aufheben, Button sperren (Doppelklick-Schutz)
  if (btn) {
    clearTimeout(btn._confirmTimer);
    btn.classList.remove('confirming');
    btn.dataset.confirm = '';
    btn.disabled = true;
    btn.textContent = '…';
  }

  // Badge sofort optimistisch hochzählen
  const badge = document.querySelector(`[data-kauf-badge="${id}"]`);
  if (badge) {
    const m = badge.textContent.match(/\d+/);
    badge.textContent = `× ${(m ? parseInt(m[0], 10) : 0) + 1} mal gekauft`;
  }

  const kontoId = state.kaufKontoId || (state.konten[0] ? state.konten[0].id : '');
  try {
    const q = kontoId ? `?kontoId=${kontoId}` : '';
    const res = await fetch(`${API}/produkte/${id}/kaufen${q}`, { method: 'POST' });
    if (!res.ok) throw new Error('kaufen fehlgeschlagen');
    await ladeDaten();        // gesamten State neu laden
    renderDashboard();        // Kontostand aktualisieren
    renderTransaktionen();    // neue Transaktion anzeigen
    renderInventar();         // Inventar neu rendern (ersetzt den gesperrten Button)
    await renderBudgets();    // Budgets aktualisieren
    showToast('Abgezogen ✓');
  } catch (e) {
    showToast('Fehler beim Kaufen', true);
    renderInventar();         // Badge/Button zurücksetzen
  }
}

// ===== STATS =====
let monthlyChart = null;
let categoryChart = null;
const STATS_PALETTE = ['#007AFF', '#34C759', '#FF9500', '#AF52DE', '#FF3B30', '#5AC8FA', '#FFCC00', '#8E8E93'];

function renderStats() {
  const now = new Date();
  const curM = now.getMonth();
  const curY = now.getFullYear();
  const tx = state.transaktionen;
  const monateLang = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
    'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'];
  const monateKurz = ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez'];

  const label = document.getElementById('stats-monat-label');
  if (label) label.textContent = `${monateLang[curM]} ${curY}`;

  // ---- A) Letzte 6 Monate: Einnahmen vs. Ausgaben ----
  const months = [];
  for (let i = 5; i >= 0; i--) {
    const d = new Date(curY, curM - i, 1);
    months.push({ y: d.getFullYear(), m: d.getMonth(), label: monateKurz[d.getMonth()] });
  }
  const inMonat = (t, mo) => {
    const d = new Date(t.datum);
    return d.getFullYear() === mo.y && d.getMonth() === mo.m;
  };
  const einnahmen = months.map(mo =>
    tx.filter(t => inMonat(t, mo) && Number(t.betrag) > 0)
      .reduce((s, t) => s + Number(t.betrag), 0));
  const ausgaben = months.map(mo =>
    tx.filter(t => inMonat(t, mo) && Number(t.betrag) < 0)
      .reduce((s, t) => s + Math.abs(Number(t.betrag)), 0));

  if (typeof Chart !== 'undefined') {
    const ctxM = document.getElementById('chart-monthly');
    if (monthlyChart) monthlyChart.destroy();
    monthlyChart = new Chart(ctxM, {
      type: 'bar',
      data: {
        labels: months.map(m => m.label),
        datasets: [
          { label: 'Einnahmen', data: einnahmen, backgroundColor: '#34C759', borderRadius: 6, borderSkipped: false },
          { label: 'Ausgaben', data: ausgaben, backgroundColor: '#FF3B30', borderRadius: 6, borderSkipped: false }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { usePointStyle: true, boxWidth: 8, font: { size: 12 } } } },
        scales: {
          x: { grid: { display: false }, border: { display: false }, ticks: { font: { size: 11 } } },
          y: { grid: { display: false }, border: { display: false }, ticks: { display: false }, beginAtZero: true }
        }
      }
    });
  }

  // ---- B) Ausgaben nach Kategorie (aktueller Monat) ----
  const catMap = {};
  tx.filter(t => {
    const d = new Date(t.datum);
    return d.getFullYear() === curY && d.getMonth() === curM && Number(t.betrag) < 0;
  }).forEach(t => {
    const key = (t.budget && t.budget.kategorie) || t.kategorie || 'Sonstiges';
    catMap[key] = (catMap[key] || 0) + Math.abs(Number(t.betrag));
  });
  const catLabels = Object.keys(catMap);
  const catData = catLabels.map(k => catMap[k]);
  const catTotal = catData.reduce((a, b) => a + b, 0);

  if (typeof Chart !== 'undefined') {
    const ctxC = document.getElementById('chart-category');
    if (categoryChart) { categoryChart.destroy(); categoryChart = null; }
    if (catLabels.length) {
      categoryChart = new Chart(ctxC, {
        type: 'doughnut',
        data: { labels: catLabels, datasets: [{ data: catData, backgroundColor: STATS_PALETTE, borderWidth: 0 }] },
        options: { responsive: true, maintainAspectRatio: false, cutout: '62%', plugins: { legend: { display: false } } }
      });
    }
  }

  const legend = document.getElementById('chart-category-legend');
  legend.innerHTML = catLabels.length ? catLabels.map((k, i) => {
    const pct = catTotal ? Math.round(catData[i] / catTotal * 100) : 0;
    return `<div class="legend-row">
      <span class="legend-dot" style="background:${STATS_PALETTE[i % STATS_PALETTE.length]}"></span>
      <span class="legend-name">${k}</span>
      <span class="legend-val">${pct}% · ${formatCHF(catData[i])}</span>
    </div>`;
  }).join('') : '<p style="color:var(--text2);font-size:14px;padding:4px 0 0">Keine Ausgaben diesen Monat</p>';

  // ---- C) Top Produkte ----
  const top = [...state.produkte].sort((a, b) => (b.kaufCount || 0) - (a.kaufCount || 0)).slice(0, 5);
  const topEl = document.getElementById('stats-top-produkte');
  topEl.innerHTML = top.length ? top.map((p, i) => `
    <div class="rank-item">
      <span class="rank-num">${i + 1}</span>
      <div class="rank-info">
        <div class="rank-name">${p.name}</div>
        <div class="rank-sub">${p.kaufCount || 0}× · ${p.kategorie || '—'}</div>
      </div>
      <div class="rank-total">${formatCHF((p.kaufCount || 0) * Number(p.preis || 0))}</div>
    </div>
  `).join('') : '<p style="color:var(--text2);font-size:14px;padding:16px">Keine Produkte</p>';

  // ---- D) Quick Stats (aktueller Monat) ----
  const monthTx = tx.filter(t => {
    const d = new Date(t.datum);
    return d.getFullYear() === curY && d.getMonth() === curM;
  });
  const monthSpend = monthTx.filter(t => Number(t.betrag) < 0).reduce((s, t) => s + Math.abs(Number(t.betrag)), 0);
  const monthIncome = monthTx.filter(t => Number(t.betrag) > 0).reduce((s, t) => s + Number(t.betrag), 0);
  const tageVergangen = now.getDate();
  const avgDay = monthSpend / (tageVergangen || 1);
  const groesste = monthTx.filter(t => Number(t.betrag) < 0)
    .reduce((m, t) => Math.max(m, Math.abs(Number(t.betrag))), 0);
  const gespart = monthIncome - monthSpend;
  const daTotal = state.dauerauftraege.reduce((s, d) => s + Number(d.betrag || 0), 0);

  document.getElementById('stats-quick').innerHTML =
    statTile('Ø Ausgaben/Tag', formatCHF(avgDay)) +
    statTile('Grösste Ausgabe', formatCHF(groesste)) +
    statTile('Gespart', formatCHF(gespart), gespart >= 0 ? 'plus' : 'minus') +
    statTile('Daueraufträge/Mt.', formatCHF(daTotal));
}

function statTile(label, value, cls = '') {
  return `<div class="stat-tile">
    <div class="stat-label">${label}</div>
    <div class="stat-value ${cls}">${value}</div>
  </div>`;
}

// ===== SPEICHERN =====
async function speichereTransaktion() {
  const betrag = parseFloat(document.getElementById('t-betrag').value);
  const payload = {
    beschreibung: document.getElementById('t-beschreibung').value,
    betrag: state.transakTyp === 'eingang' ? Math.abs(betrag) : -Math.abs(betrag),
    datum: document.getElementById('t-datum').value,
    konto: { id: parseInt(document.getElementById('t-konto').value) },
    budget: document.getElementById('t-budget').value ? { id: parseInt(document.getElementById('t-budget').value) } : null,
    kategorie: state.transakTyp
  };

  if (!payload.beschreibung || isNaN(betrag)) return showToast('Bitte alle Felder ausfüllen', true);

  await fetch(`${API}/transaktionen`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  closeModal('modal-transaktion');
  await ladeDaten();
  renderDashboard();
  renderTransaktionen();
  await renderBudgets();
  showToast('Transaktion gespeichert');
}

async function speichereKonto() {
  const payload = {
    name: document.getElementById('k-name').value,
    kontostand: parseFloat(document.getElementById('k-stand').value) || 0,
    farbe: document.getElementById('k-farbe').value
  };
  if (!payload.name) return showToast('Name erforderlich', true);
  await fetch(`${API}/konten`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  closeModal('modal-konto');
  await ladeDaten();
  renderDashboard();
  await renderBudgets();
  updateSelects();
  showToast('Konto hinzugefügt');
}

async function speichereDauerauftrag() {
  const payload = {
    beschreibung: document.getElementById('da-beschreibung').value,
    betrag: parseFloat(document.getElementById('da-betrag').value) || 0,
    tagImMonat: parseInt(document.getElementById('da-tag').value) || 1,
    vonKonto: { id: parseInt(document.getElementById('da-konto').value) }
  };
  if (!payload.beschreibung) return showToast('Beschreibung erforderlich', true);
  await fetch(`${API}/dauerauftraege`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  closeModal('modal-dauerauftrag');
  await ladeDaten();
  renderDashboard();
  await renderBudgets();
  showToast('Dauerauftrag gespeichert');
}

async function speichereBudget() {
  const payload = {
    kategorie: document.getElementById('b-kategorie').value,
    limitProMonat: parseFloat(document.getElementById('b-limit').value) || 0,
    icon: document.getElementById('b-icon').value,
    farbe: document.getElementById('b-farbe').value
  };
  if (!payload.kategorie) return showToast('Kategorie erforderlich', true);
  await fetch(`${API}/budgets`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  closeModal('modal-budget');
  await ladeDaten();
  await renderBudgets();
  updateSelects();
  showToast('Budget gespeichert');
}

async function speichereProdukt() {
  const payload = {
    name: document.getElementById('p-name').value,
    preis: parseFloat(document.getElementById('p-preis').value) || 0,
    kategorie: document.getElementById('p-kategorie').value,
    barcode: document.getElementById('p-barcode').value,
    gekauftBei: document.getElementById('p-gekauftBei') ? document.getElementById('p-gekauftBei').value : null
  };
  if (!payload.name) return showToast('Name erforderlich', true);
  await fetch(`${API}/produkte`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
  });
  closeModal('modal-produkt');
  document.getElementById('gemini-result-box').style.display = 'none';
  await ladeDaten();
  renderInventar();
  await renderBudgets();
  showToast('Produkt gespeichert');
}

// ===== UTILS =====
function updateSelects() {
  const kontoSelects = ['t-konto', 'da-konto'];
  kontoSelects.forEach(id => {
    const sel = document.getElementById(id);
    if (sel) sel.innerHTML = state.konten.map(k =>
      `<option value="${k.id}">${k.name}</option>`).join('');
  });

  const budgetSel = document.getElementById('t-budget');
  budgetSel.innerHTML = '<option value="">Kein Budget</option>' +
    state.budgets.map(b => `<option value="${b.id}">${b.icon ? b.icon + ' ' : ''}${b.kategorie}</option>`).join('');
}

function selectTyp(el) {
  document.querySelectorAll('[data-typ]').forEach(c => c.classList.remove('selected'));
  el.classList.add('selected');
  state.transakTyp = el.dataset.typ;
}

function openModal(id) {
  updateSelects();
  document.getElementById(id).classList.add('open');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}

// Click outside modal to close
document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', e => {
    if (e.target === overlay) overlay.classList.remove('open');
  });
});

function formatCHF(val) {
  return 'CHF ' + Number(val || 0).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, "'");
}

function showToast(msg, error = false) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.style.background = error ? 'var(--red)' : 'var(--green)';
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2500);
}

init();
