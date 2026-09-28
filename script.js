

document.querySelectorAll('.rail-link').forEach(function (link) {
  link.addEventListener('click', function () {
    var targetId = link.getAttribute('data-target');

    document.querySelectorAll('.rail-link').forEach(function (l) {
      l.classList.toggle('is-active', l === link);
    });

    document.querySelectorAll('.view').forEach(function (view) {
      view.classList.toggle('is-active', view.id === targetId);
    });

    document.querySelector('.main').scrollTo({ top: 0, behavior: 'instant' });
  });
});


var assets = [
  { code: 'SRV-014', name: 'Servidor de base de datos', type: 'Servidor', owner: 'J. Martínez', criticality: 'Alta' },
  { code: 'SRV-021', name: 'Servidor web principal', type: 'Servidor', owner: 'J. Martínez', criticality: 'Alta' },
  { code: 'NET-002', name: 'Firewall perimetral', type: 'Red', owner: 'J. Martínez', criticality: 'Alta' },
  { code: 'SW-007', name: 'ERP contable', type: 'Software', owner: 'L. Gómez', criticality: 'Media' },
  { code: 'SW-012', name: 'Sistema de nómina', type: 'Software', owner: 'L. Gómez', criticality: 'Alta' },
  { code: 'WKS-034', name: 'Estación de trabajo — contabilidad', type: 'Equipo', owner: 'R. Pérez', criticality: 'Baja' },
  { code: 'WKS-041', name: 'Portátil dirección general', type: 'Equipo', owner: 'C. Rojas', criticality: 'Media' }
];


var OTHER_ASSETS_MARGIN = 42 - assets.length;

var TYPE_PREFIX = { Servidor: 'SRV', Software: 'SW', Equipo: 'WKS', Red: 'NET' };
var CRIT_CLASS = { Alta: 'level-high', Media: 'level-med', Baja: 'level-low' };

var ICON_EDIT = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>';
var ICON_TRASH = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/></svg>';

function nextCode(type) {
  var prefix = TYPE_PREFIX[type];
  var nums = assets
    .filter(function (a) { return a.code.indexOf(prefix + '-') === 0; })
    .map(function (a) { return parseInt(a.code.split('-')[1], 10); });
  var next = (nums.length ? Math.max.apply(null, nums) : 0) + 1;
  return prefix + '-' + String(next).padStart(3, '0');
}

function escapeHtml(str) {
  var div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function renderAssets() {
  var tbody = document.getElementById('activos-body');
  if (!tbody) return;
  tbody.innerHTML = '';

  assets.forEach(function (a) {
    var tr = document.createElement('tr');
    tr.innerHTML =
      '<td><code>' + a.code + '</code></td>' +
      '<td>' + escapeHtml(a.name) + '</td>' +
      '<td>' + a.type + '</td>' +
      '<td>' + escapeHtml(a.owner) + '</td>' +
      '<td><span class="level ' + CRIT_CLASS[a.criticality] + '">' + a.criticality + '</span></td>' +
      '<td>Activo</td>' +
      '<td><div class="row-actions">' +
        '<button class="icon-btn" data-edit="' + a.code + '" type="button" aria-label="Editar ' + a.code + '" title="Editar">' + ICON_EDIT + '</button>' +
        '<button class="icon-btn danger" data-delete="' + a.code + '" type="button" aria-label="Eliminar ' + a.code + '" title="Eliminar">' + ICON_TRASH + '</button>' +
      '</div></td>';
    tbody.appendChild(tr);
  });

  var counter = document.getElementById('activos-count');
  if (counter) counter.textContent = OTHER_ASSETS_MARGIN + assets.length;

  tbody.querySelectorAll('[data-edit]').forEach(function (btn) {
    btn.addEventListener('click', function () { openAssetModal(btn.getAttribute('data-edit')); });
  });
  tbody.querySelectorAll('[data-delete]').forEach(function (btn) {
    btn.addEventListener('click', function () { deleteAsset(btn.getAttribute('data-delete')); });
  });
}


var overlay = document.getElementById('asset-modal-overlay');
var form = document.getElementById('asset-form');
var modalTitle = document.getElementById('modal-title');
var codeHint = document.getElementById('asset-code-hint');
var idField = document.getElementById('asset-id');
var nameField = document.getElementById('asset-name');
var typeField = document.getElementById('asset-type');
var ownerField = document.getElementById('asset-owner');
var critField = document.getElementById('asset-criticality');

function updateCodeHint() {
  if (idField.value) {
    codeHint.textContent = 'Código asignado: ' + idField.value + ' (no cambia al editar)';
  } else {
    codeHint.textContent = 'Se le asignará el código ' + nextCode(typeField.value) + ' automáticamente';
  }
}

function openAssetModal(codeToEdit) {
  form.reset();
  if (codeToEdit) {
    var a = assets.find(function (x) { return x.code === codeToEdit; });
    if (!a) return;
    modalTitle.textContent = 'Editar activo';
    idField.value = a.code;
    nameField.value = a.name;
    typeField.value = a.type;
    ownerField.value = a.owner;
    critField.value = a.criticality;
  } else {
    modalTitle.textContent = 'Agregar activo';
    idField.value = '';
    critField.value = 'Media';
  }
  updateCodeHint();
  overlay.classList.add('is-open');
  nameField.focus();
}

function closeAssetModal() {
  overlay.classList.remove('is-open');
}

function deleteAsset(code) {
  var ok = window.confirm('¿Eliminar el activo ' + code + '? Esta acción no se puede deshacer.');
  if (!ok) return;
  assets = assets.filter(function (a) { return a.code !== code; });
  renderAssets();
}

var btnAdd = document.getElementById('btn-add-asset');
if (btnAdd) btnAdd.addEventListener('click', function () { openAssetModal(null); });

var btnClose = document.getElementById('modal-close-btn');
if (btnClose) btnClose.addEventListener('click', closeAssetModal);

var btnCancel = document.getElementById('modal-cancel-btn');
if (btnCancel) btnCancel.addEventListener('click', closeAssetModal);

if (overlay) {
  overlay.addEventListener('click', function (e) {
    if (e.target === overlay) closeAssetModal();
  });
}

document.addEventListener('keydown', function (e) {
  if (e.key === 'Escape' && overlay && overlay.classList.contains('is-open')) closeAssetModal();
});

if (typeField) typeField.addEventListener('change', updateCodeHint);

if (form) {
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var data = {
      name: nameField.value.trim(),
      type: typeField.value,
      owner: ownerField.value.trim(),
      criticality: critField.value
    };
    if (idField.value) {
      var existing = assets.find(function (x) { return x.code === idField.value; });
      if (existing) Object.assign(existing, data);
    } else {
      data.code = nextCode(data.type);
      assets.push(data);
    }
    renderAssets();
    closeAssetModal();
  });
}

renderAssets();
