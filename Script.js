// Vigentis — navegación entre módulos (100% en el cliente, sin backend).
// Sólo muestra/oculta secciones; no hay lectura ni escritura de datos reales.

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