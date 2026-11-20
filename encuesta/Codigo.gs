/**
 * EduPlanner: recibe las respuestas de la encuesta de la app y las escribe en esta hoja de cálculo.
 * Instrucciones de instalación en encuesta/LEEME.md.
 *
 * Hojas:
 *  - "Respuestas": una fila por respuesta. Los encabezados se crean solos con la primera respuesta; si la app
 *    agrega preguntas en una versión nueva, sus columnas se añaden al final sin mover las anteriores.
 *  - "Resumen": número de respuestas, SUS, NPS y media y desviación estándar de cada módulo. Son fórmulas que
 *    se recalculan solas; se reescriben con cada respuesta para apuntar a las columnas correctas.
 */

// Debe ser igual a encuesta.clave en local.properties de la app.
const CLAVE = 'CAMBIA_ESTA_CLAVE';

const HOJA_RESPUESTAS = 'Respuestas';
const HOJA_RESUMEN = 'Resumen';
const COL_ID = 'ID de respuesta';

function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000);
    const datos = JSON.parse(e.postData.contents);
    if (datos.clave !== CLAVE) return responder({ ok: false, error: 'clave' });
    if (!Array.isArray(datos.columnas) || !datos.id_respuesta) return responder({ ok: false, error: 'datos' });

    const libro = SpreadsheetApp.getActiveSpreadsheet();
    const hoja = libro.getSheetByName(HOJA_RESPUESTAS) || libro.insertSheet(HOJA_RESPUESTAS, 0);
    const encabezados = asegurarEncabezados(hoja, datos.columnas.map(function (c) { return String(c.encabezado); }));

    // Un reintento de la app (la fila llegó pero la respuesta no) no se duplica
    const colId = encabezados.indexOf(COL_ID) + 1;
    if (colId > 0 && hoja.getLastRow() > 1) {
      const ids = hoja.getRange(2, colId, hoja.getLastRow() - 1, 1).getValues().map(function (f) { return f[0]; });
      if (ids.indexOf(datos.id_respuesta) >= 0) return responder({ ok: true, duplicada: true });
    }

    const valores = {};
    datos.columnas.forEach(function (c) { valores[String(c.encabezado)] = limpiar(c.valor); });
    hoja.appendRow(encabezados.map(function (h) { return h in valores ? valores[h] : ''; }));

    asegurarResumen(libro, encabezados);
    return responder({ ok: true });
  } catch (err) {
    return responder({ ok: false, error: String(err) });
  } finally {
    lock.releaseLock();
  }
}

// doGet (el dashboard y la comprobación …/exec?estado) está en Dashboard.gs.

function asegurarEncabezados(hoja, nuevos) {
  const ultima = hoja.getLastColumn();
  const actuales = ultima > 0 ? hoja.getRange(1, 1, 1, ultima).getValues()[0].map(String) : [];
  const faltan = nuevos.filter(function (h) { return actuales.indexOf(h) < 0; });
  if (faltan.length > 0) {
    hoja.getRange(1, actuales.length + 1, 1, faltan.length).setValues([faltan]);
    hoja.getRange(1, 1, 1, actuales.length + faltan.length)
      .setFontWeight('bold').setBackground('#5e35b1').setFontColor('#ffffff').setWrap(true).setVerticalAlignment('top');
    hoja.setFrozenRows(1);
    hoja.setFrozenColumns(2);
  }
  return actuales.concat(faltan);
}

/** Números como números; un texto que empieza por = + - @ se guarda como texto, no como fórmula. */
function limpiar(valor) {
  if (valor === null || valor === undefined) return '';
  if (typeof valor === 'number' || typeof valor === 'boolean') return valor;
  const texto = String(valor);
  return /^[=+\-@]/.test(texto) ? "'" + texto : texto;
}

function responder(objeto) {
  return ContentService.createTextOutput(JSON.stringify(objeto)).setMimeType(ContentService.MimeType.JSON);
}

/** Letra de columna (1 → A, 27 → AA). */
function letra(numero) {
  let texto = '';
  while (numero > 0) {
    const resto = (numero - 1) % 26;
    texto = String.fromCharCode(65 + resto) + texto;
    numero = Math.floor((numero - 1) / 26);
  }
  return texto;
}

const COL_NPS = 'S9.3 Del 0 al 10, ¿qué tan probable es que recomiendes EduPlanner a un compañero?';
const MODULOS = ['Organización', 'Gamificación', 'IA', 'Notificaciones', 'QR', 'Diseño', 'Satisfacción', 'Impacto'];

function asegurarResumen(libro, encabezados) {
  const hoja = libro.getSheetByName(HOJA_RESUMEN) || libro.insertSheet(HOJA_RESUMEN);
  // Datos de una columna de "Respuestas" sin el encabezado, p. ej. Respuestas!$AB$2:$AB
  function col(encabezado) {
    const i = encabezados.indexOf(encabezado);
    if (i < 0) return null;
    const l = letra(i + 1);
    return HOJA_RESPUESTAS + '!$' + l + '$2:$' + l;
  }
  function formula(encabezado, cuerpo) {
    const c = col(encabezado);
    return c ? '=IFERROR(' + cuerpo(c) + ', "")' : '';
  }
  const filas = [
    ['Indicador', 'Valor', 'Cómo se lee'],
    ['Respuestas', formula(COL_ID, function (c) { return 'COUNTA(' + c + ')'; }), 'Número de encuestas recibidas'],
    ['SUS: media', formula('SUS (0-100)', function (c) { return 'AVERAGE(' + c + ')'; }), '0 a 100; por encima de 68 es aceptable'],
    ['SUS: desviación estándar', formula('SUS (0-100)', function (c) { return 'STDEV(' + c + ')'; }), ''],
    ['SUS: % por encima de 68', formula('SUS (0-100)', function (c) { return 'COUNTIF(' + c + ', ">68") / COUNT(' + c + ')'; }), ''],
    ['NPS', formula(COL_NPS, function (c) {
      return '(COUNTIF(' + c + ', ">=9") - COUNTIF(' + c + ', "<=6")) / COUNT(' + c + ') * 100';
    }), '% promotores (9-10) − % detractores (0-6); de −100 a 100']
  ];
  MODULOS.forEach(function (modulo) {
    const encabezado = 'Promedio ' + modulo;
    filas.push(['Media ' + modulo, formula(encabezado, function (c) { return 'AVERAGE(' + c + ')'; }),
      modulo === 'Impacto' ? 'Escala de 1 a 10' : 'Escala de 1 a 5']);
    filas.push(['Desv. estándar ' + modulo, formula(encabezado, function (c) { return 'STDEV(' + c + ')'; }), '']);
  });
  hoja.clear();
  hoja.getRange(1, 1, filas.length, 3).setValues(filas);
  hoja.getRange(1, 1, 1, 3).setFontWeight('bold').setBackground('#5e35b1').setFontColor('#ffffff');
  hoja.getRange(3, 2, filas.length - 2, 1).setNumberFormat('0.00');
  hoja.getRange(5, 2).setNumberFormat('0.0%');
  hoja.setColumnWidth(1, 220);
  hoja.setColumnWidth(3, 320);
}
