# Encuesta de EduPlanner → Google Sheets

La app envía cada encuesta terminada a una **aplicación web de Google Apps Script** vinculada a una hoja de cálculo. El script (`Codigo.gs`) escribe una fila por respuesta y mantiene una hoja de resumen.

## 1. Crear la hoja y el script (una sola vez)

1. Crea una hoja de cálculo nueva en Google Drive, por ejemplo "EduPlanner – Encuesta".
2. En la hoja, abre **Extensiones → Apps Script**.
3. Borra el contenido de `Código.gs` y pega el contenido de `encuesta/Codigo.gs`. Agrega también `Dashboard.gs` (**+ → Secuencia de comandos**) y `Dashboard.html` (**+ → HTML**, con el nombre exacto `Dashboard`) con el contenido de los archivos de esta carpeta.
4. Cambia `CAMBIA_ESTA_CLAVE` por una clave tuya, larga y difícil de adivinar (por ejemplo, 30 letras y números al azar).
5. Guarda (ícono del disquete).

## 2. Publicarlo como aplicación web

1. **Implementar → Nueva implementación**.
2. En "Seleccionar tipo" (engranaje), elige **Aplicación web**.
3. Configura:
   - **Ejecutar como:** Yo (tu cuenta).
   - **Quién tiene acceso:** Cualquier persona.
4. **Implementar**. Google pide autorizar el acceso a tus hojas de cálculo: acepta.
   - Si aparece "Google no ha verificado esta aplicación", entra en **Configuración avanzada → Ir a (nombre del proyecto)**. Es tu propio script.
5. Copia la **URL de la aplicación web**. Termina en `/exec`.
6. Para comprobarlo, abre la URL terminada en `/exec?estado`. Debe mostrar `{"ok":true,"estado":"activo"}`. La URL sin `?estado` muestra el dashboard (sección 5).

## 3. Configurar la app

En `local.properties`, en la raíz del proyecto, agrega:

```properties
encuesta.url=https://script.google.com/macros/s/XXXXXXXX/exec
encuesta.clave=LA_MISMA_CLAVE_DEL_SCRIPT
```

Vuelve a compilar la app. Si falta `encuesta.url`, la encuesta se puede llenar, pero al enviarla avisa que "aún no está disponible" y guarda las respuestas.

> `local.properties` no se sube a git. Cada persona que compile la app para los participantes necesita estas dos líneas.

## 4. Qué se guarda

**Hoja "Respuestas"**: una fila por encuesta. Los encabezados se crean con la primera respuesta.

- **Fecha de envío**, **ID de respuesta**, **Participante (anónimo)** y **Versión de la app**.
  - El participante es un identificador aleatorio por instalación. Sirve para detectar respuestas repetidas sin saber quién es la persona.
  - No se envían el correo ni el usuario.
- Una columna por pregunta. El encabezado es `S<sección>.<número> <texto de la pregunta>`.
  - Las preguntas de 1 a 5, de 1 a 10 y la de 0 a 10 se guardan como **números**.
  - Las demás se guardan como texto.
  - "Otra app" tiene su propia columna con el nombre escrito.
  - Si la persona no usó el QR, las preguntas S7.2 a S7.5 quedan vacías.
- **SUS (0-100)**: los ítems impares suman (respuesta − 1) y los pares (5 − respuesta); el total se multiplica por 2,5.
- **NPS categoría**: Promotor (9-10), Pasivo (7-8) o Detractor (0-6).
- **Promedio** de cada módulo, con las preguntas de 1 a 5: Organización, Gamificación, IA, Notificaciones, QR, Diseño y Satisfacción.
- **Promedio Impacto**: media de las cinco preguntas de 1 a 10 de la sección 10 (ayuda de la IA, mejoría en la vida estudiantil, uso juicioso, mejora de notas y menos procrastinación). "¿Seguirías usando EduPlanner?" se guarda como texto (No, Casi nunca, A veces, Muy seguido o Siempre).

**Hoja "Resumen"**: fórmulas que se recalculan solas.

- Número de respuestas.
- Media y desviación estándar del SUS, y el % por encima de 68.
- NPS: % de promotores menos % de detractores.
- Media y desviación estándar de cada módulo.

## Seguridad y datos

- Si la app reintenta el envío (por ejemplo, la conexión se cortó después de guardar), el script reconoce el **ID de respuesta** y no duplica la fila.
- Un texto que empieza por `=`, `+`, `-` o `@` se guarda como texto, para que nadie pueda meter fórmulas en la hoja.
- La clave evita envíos casuales de terceros. No es una protección fuerte, porque va dentro de la app. Si recibes basura, cambia la clave en el script y en `local.properties`, crea una nueva implementación y vuelve a compilar.
- Si cambias `Codigo.gs`, usa **Implementar → Gestionar implementaciones → Editar → Nueva versión**. Así la URL no cambia.

## 5. Dashboard de resultados

`Dashboard.gs` + `Dashboard.html` publican una página con los resultados, pensada para mostrar a otras personas. Está en la misma URL `/exec`. Solo muestra porcentajes agregados, nunca respuestas individuales ni textos escritos por los participantes. Se calcula en vivo cada vez que se abre.

- **Cifras principales:** % que seguirá usando EduPlanner ("muy seguido" o "siempre"), % que notó mejoría en su vida estudiantil y % al que le ayudaron las recomendaciones con IA (7 o más sobre 10).
- **Porcentaje de estudiantes que…**, en cuatro áreas:
  - **Rendimiento:** organiza mejor, entrega a tiempo, mejoró sus notas, procrastina menos.
  - **IA:** consejos útiles; de quienes aplicaron una recomendación, a cuántos les fue mejor; el método se ajustó; confió.
  - **Motivación:** estudia con más motivación, la racha, el enfoque, uso juicioso.
  - **Experiencia:** fácil de usar (SUS > 68), satisfecho, cumple expectativas, resumen de las 9:00.
- **¿La seguirían usando?:** la distribución de las respuestas (Siempre … No) y el NPS con el % de promotores.

Cuenta como positivo 4 o 5 en las preguntas de 1 a 5 y 7 o más en las de 1 a 10 (constantes `POSITIVO_1_A_5` y `POSITIVO_1_A_10`). Las preguntas se buscan por su número (`S10.2`…), así que cambiar el texto de una pregunta no rompe el dashboard.
