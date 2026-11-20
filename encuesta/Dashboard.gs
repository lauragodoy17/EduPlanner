/**
 * EduPlanner: dashboard público de resultados de la encuesta.
 *
 * Abrir la URL de la aplicación web (…/exec) muestra el dashboard. Solo muestra porcentajes agregados:
 * nunca respuestas individuales ni textos escritos por los participantes.
 * La app sigue enviando las respuestas por POST (doPost, en Código.gs); …/exec?estado devuelve el JSON de
 * comprobación {"ok":true,"estado":"activo"}.
 *
 * Usa HOJA_RESPUESTAS, COL_ID y responder() de Código.gs.
 */

// Qué cuenta como respuesta positiva
const POSITIVO_1_A_5 = 4;   // 4 o 5: "de acuerdo" o "totalmente de acuerdo"
const POSITIVO_1_A_10 = 7;  // 7 o más

function doGet(e) {
  if (e && e.parameter && e.parameter.estado !== undefined) {
    return responder({ ok: true, estado: 'activo' });
  }
  const plantilla = HtmlService.createTemplateFromFile('Dashboard');
  plantilla.datos = JSON.stringify(calcularDashboard());
  return plantilla.evaluate()
    .setTitle('EduPlanner · Resultados')
    .addMetaTag('viewport', 'width=device-width, initial-scale=1')
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

/** Todos los números del dashboard, calculados en vivo desde la hoja "Respuestas". */
function calcularDashboard() {
  const hoja = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(HOJA_RESPUESTAS);
  if (!hoja || hoja.getLastRow() < 2) return { total: 0, grupos: [] };

  const valores = hoja.getDataRange().getValues();
  const encabezados = valores[0].map(String);
  const iId = encabezados.indexOf(COL_ID);
  const filas = valores.slice(1).filter(function (f) { return iId < 0 || f[iId] !== ''; });

  // Respuestas de una pregunta por su número ("S10.2"), sin las vacías
  function respuestas(id) {
    const i = encabezados.findIndex(function (h) { return h.indexOf(id + ' ') === 0 || h === id; });
    if (i < 0) return [];
    return filas.map(function (f) { return f[i]; }).filter(function (v) { return v !== '' && v !== null; });
  }
  function porcentaje(lista, cumple) {
    if (lista.length === 0) return null;
    return Math.round(lista.filter(cumple).length * 100 / lista.length);
  }
  function deAcuerdo(id) {
    const lista = respuestas(id).map(Number);
    return { pct: porcentaje(lista, function (v) { return v >= POSITIVO_1_A_5; }), n: lista.length };
  }
  function alto(id) {
    const lista = respuestas(id).map(Number);
    return { pct: porcentaje(lista, function (v) { return v >= POSITIVO_1_A_10; }), n: lista.length };
  }
  function indicador(dato, titulo, detalle) {
    return { pct: dato.pct, n: dato.n, titulo: titulo, detalle: detalle };
  }

  // Seguir usándola: "Muy seguido" o "Siempre"
  const seguir = respuestas('S10.4');
  const OPCIONES_SEGUIR = ['Siempre', 'Muy seguido', 'A veces', 'Casi nunca', 'No'];
  const distribucionSeguir = OPCIONES_SEGUIR.map(function (o) {
    return { opcion: o, pct: porcentaje(seguir, function (v) { return v === o; }) || 0 };
  });
  const seguiran = { pct: porcentaje(seguir, function (v) { return v === 'Siempre' || v === 'Muy seguido'; }), n: seguir.length };

  // Recomendación (0 a 10): promotores y NPS
  const nps = respuestas('S9.3').map(Number);
  const promotores = porcentaje(nps, function (v) { return v >= 9; });
  const detractores = porcentaje(nps, function (v) { return v <= 6; });

  // De quienes aplicaron una recomendación de la IA, a cuántos les fue mejor
  const aplicaron = respuestas('S5.9').filter(function (v) { return v !== 'No la apliqué'; });
  const mejorConIa = { pct: porcentaje(aplicaron, function (v) { return v === 'Mejor que antes'; }), n: aplicaron.length };

  // Facilidad de uso: SUS por encima de 68
  const sus = respuestas('SUS (0-100)').map(Number);
  const facil = { pct: porcentaje(sus, function (v) { return v > 68; }), n: sus.length };

  return {
    total: filas.length,
    actualizado: Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "d 'de' MMMM 'de' yyyy, HH:mm"),
    destacados: [
      indicador(seguiran, 'seguirá usando EduPlanner', 'Respondió que la usaría "muy seguido" o "siempre" después de la prueba'),
      indicador(alto('S10.2'), 'notó una mejoría en su vida estudiantil', 'Calificó la mejoría con 7 o más sobre 10'),
      indicador(alto('S10.1'), 'sintió que las recomendaciones con IA le ayudaron', 'Calificó la ayuda con 7 o más sobre 10')
    ],
    grupos: [
      {
        titulo: 'Rendimiento académico',
        indicadores: [
          indicador(deAcuerdo('S3.11'), 'organiza mejor sus actividades académicas', 'De acuerdo o totalmente de acuerdo'),
          indicador(deAcuerdo('S3.12'), 'entrega sus tareas a tiempo con más frecuencia', 'De acuerdo o totalmente de acuerdo'),
          indicador(alto('S10.5'), 'percibió una mejora en sus notas', '7 o más sobre 10'),
          indicador(alto('S10.6'), 'siente que procrastina menos', '7 o más sobre 10')
        ]
      },
      {
        titulo: 'Recomendaciones con inteligencia artificial',
        indicadores: [
          indicador(deAcuerdo('S5.5'), 'encontró claros y útiles los consejos de la recomendación', 'De acuerdo o totalmente de acuerdo'),
          indicador(mejorConIa, 'de quienes aplicaron una recomendación, le fue mejor que antes', 'Entre quienes aplicaron al menos una'),
          indicador(deAcuerdo('S5.4'), 'dice que el método recomendado se ajustó a su forma de estudiar', 'De acuerdo o totalmente de acuerdo'),
          indicador(deAcuerdo('S5.7'), 'confió en las recomendaciones', 'De acuerdo o totalmente de acuerdo')
        ]
      },
      {
        titulo: 'Motivación y hábitos',
        indicadores: [
          indicador(deAcuerdo('S4.11'), 'aumentó su motivación para estudiar, no solo para usar la app', 'De acuerdo o totalmente de acuerdo'),
          indicador(deAcuerdo('S4.3'), 'usó la app de forma constante gracias a la racha', 'De acuerdo o totalmente de acuerdo'),
          indicador(deAcuerdo('S4.7'), 'se concentró mejor con las sesiones de enfoque', 'De acuerdo o totalmente de acuerdo'),
          indicador(alto('S10.3'), 'usó EduPlanner de forma juiciosa', '7 o más sobre 10')
        ]
      },
      {
        titulo: 'Experiencia',
        indicadores: [
          indicador(facil, 'la considera fácil de usar', 'Puntaje de usabilidad (SUS) por encima de 68'),
          indicador(deAcuerdo('S9.1'), 'está satisfecho/a con EduPlanner', 'De acuerdo o totalmente de acuerdo'),
          indicador(deAcuerdo('S9.2'), 'dice que cumple lo que espera de una app académica', 'De acuerdo o totalmente de acuerdo'),
          indicador(deAcuerdo('S6.2'), 'planea mejor su día con el resumen de las 9:00', 'De acuerdo o totalmente de acuerdo')
        ]
      }
    ],
    seguir: distribucionSeguir,
    promotores: promotores,
    nps: (promotores === null || detractores === null) ? null : promotores - detractores
  };
}
