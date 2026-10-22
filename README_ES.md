<div align="center">

# LunaBeat

**Melodía y letra se encuentran en cada reproducción.**

Reproductor de música local para Android · Edición de letras palabra por palabra · Gestión de biblioteca y etiquetas

[Descargar la última versión](https://github.com/2755337087/LunaBeat/releases/latest) · [Guía de uso](#quick-start) · [Informar de un problema](https://github.com/2755337087/LunaBeat/issues) · [Comunidad](#community-and-feedback)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

LunaBeat reúne la reproducción de música local, la creación de letras sincronizadas palabra por palabra y la organización de la biblioteca en una sola aplicación. Escucha con calma y sigue la letra al ritmo de la melodía, o divide la letra de una canción, ajusta sus tiempos, añade traducciones e información de duetos y exporta el resultado para tu propio flujo de trabajo.

## Vista previa de la interfaz

### Interfaz para teléfono

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · Interfaz para teléfono" width="960" />
</p>

### Interfaz para tableta

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · Interfaz para tableta" width="960" />
</p>

## Funciones principales

| Uso | Qué puedes hacer |
| --- | --- |
| Escuchar música local | Escanear audio local y explorar por artista, álbum o lista; disfrutar de reproducción en segundo plano, controles multimedia del sistema, cola de reproducción y temporizador de apagado. |
| Disfrutar de las letras | Previsualizar letras palabra por palabra con temas claros y oscuros y ajustes de barra de reproducción y fondo; mostrar letras en la pantalla de inicio, la barra de estado y mediante el Bluetooth del coche. |
| Sincronizar palabra por palabra | Usar los tres botones Inicio / Continuar / Fin y afinar los tiempos con velocidad de reproducción, saltos temporales, modo de seguimiento, deshacer y rehacer. |
| Editar letras | Segmentar palabras, dividir y unir letras, mover líneas, desplazar marcas de tiempo, convertir entre chino simplificado y tradicional, eliminar líneas vacías y configurar traducciones, duetos y coros. |
| Organizar música | Gestionar listas, favoritos y canciones con cinco estrellas; buscar coincidencias de portadas, etiquetas y letras por lotes, editar metadatos y renombrar archivos. |

### Importación y exportación

| Contenido | Importación | Exportación |
| --- | --- | --- |
| Letras en texto sin formato | Compatible | — |
| LRC (línea por línea / palabra por palabra) | Compatible | Compatible |
| LRC mejorado (ELRC) | Compatible | Compatible |
| TTML | Compatible | Compatible, conserva la información de duetos y coros |

También puedes buscar letras o importar audio para empezar a crear. Se recomienda TTML para conservar información estructural como duetos y coros; LRC sirve para la visualización habitual de letras.

<a id="quick-start"></a>

## Primeros pasos

### Escuchar y organizar la biblioteca

1. Abre la biblioteca musical y escanea el audio local del dispositivo.
2. Explora por artista, álbum, lista o favoritos y toca una canción para reproducirla.
3. Abre el reproductor completo para cambiar el modo de reproducción, gestionar la cola, ver letras o configurar el temporizador de apagado.
4. Para organizar archivos, busca primero coincidencias de etiquetas y letras por lotes; después edita las etiquetas o renombra los archivos.

### Crear la letra de una canción sincronizada palabra por palabra

1. **Importar**: selecciona el audio e importa letras en texto sin formato, LRC, ELRC o TTML. También puedes buscar letras.
2. **Preparar**: segmenta primero las palabras; después elimina líneas vacías, une letras o convierte entre chino simplificado y tradicional. Si ya hay tiempos, desplaza toda la línea de tiempo para alinearla primero.
3. **Sincronizar**: reproduce el audio y usa los tres botones siguientes al ritmo de cada línea de la letra.
4. **Afinar**: toca dos veces la letra para volver a escucharla. Comprueba los tiempos con el modo de seguimiento, deshacer y rehacer; mantén pulsada una unidad o línea para dividir, unir, mover o editar traducciones.
5. **Previsualizar y exportar**: comprueba el efecto palabra por palabra; elige LRC para la visualización habitual o TTML para la información de duetos y coros.

| Botón | Acción |
| --- | --- |
| Inicio | Fija el inicio de la unidad actual y permanece en ella. |
| Continuar | Fija el final de la unidad actual, pasa a la siguiente y fija también el inicio de esta. |
| Fin | Fija el final de la unidad actual y pasa a la siguiente, cuyo inicio queda pendiente de definir. |

Secuencia recomendada: `Inicio → Continuar → Continuar → … → Fin`, repetida en cada línea.

<details>
<summary>Consejos de sincronización</summary>

- Las columnas de tiempo y texto muestran inicio, letra, final y traducción.
- Toca una unidad una vez para seleccionarla y dos veces para saltar a su inicio y escucharla de nuevo.
- Se admiten retroceso / avance rápido, reproducción / pausa, cambios de velocidad y saltos a un tiempo concreto.
- Toca el tiempo de reproducción para entrar en el modo de seguimiento; toca el tiempo en la zona de sincronización para elegir un destino.
- Los duetos se pueden configurar a la izquierda (voz principal), a la derecha (segunda voz) o en el fondo (coros), y exportarse como TTML.

</details>

## Preguntas frecuentes

<details>
<summary>¿Debo elegir LRC o TTML?</summary>

Elige LRC para la visualización habitual de letras o TTML para conservar información estructural como duetos y coros. Comprueba el efecto palabra por palabra en la vista previa antes de exportar.

</details>

<details>
<summary>¿Qué debo tener en cuenta al exportar y reproducir letras con duetos?</summary>

Es preferible exportar la información de duetos y coros en TTML y verla en un reproductor compatible con esos efectos. La visualización real depende del soporte del reproductor para el formato de letra y su información asociada.

</details>

<details>
<summary>¿Cómo ajusto letras que aparecen todas antes o después de tiempo?</summary>

Desplaza primero las marcas de tiempo para alinear el conjunto y vuelve después a la pantalla de sincronización para afinar partes concretas.

</details>

<details>
<summary>¿Por dónde empiezo si es la primera vez que sincronizo letras?</summary>

Termina la segmentación de palabras y la limpieza del texto antes de sincronizar. Elige una canción que conozcas, usa «Inicio → Continuar → … → Fin» en cada línea y después vuelve a escucharla y ajusta los tiempos.

</details>

<a id="community-and-feedback"></a>

## Comunidad y comentarios

- [GitHub Issues · Informes de errores y sugerencias de funciones](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · Grupo de la comunidad LunaBeat](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · Grupo de la comunidad LunaBeat](https://qm.qq.com/q/N0fBvuWKOY) · Número del grupo: **964680520**

Al informar de un problema, incluye si es posible la versión de la aplicación, el dispositivo y la versión de Android, los pasos para reproducirlo y capturas de pantalla o formatos de archivo relacionados, para facilitar el diagnóstico.

## Agradecimientos

### Apoyo técnico

- [TagLib](https://github.com/taglib/taglib)
- [Lyricon](https://github.com/tomakino/lyricon)
- [OpenCC](https://github.com/BYVoid/OpenCC)
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)
- [163Music2Tag](https://gitee.com/Wangs-official/163Music2Tag)
- [TagLib(Kyant0)](https://github.com/Kyant0/taglib)
- [uCrop](https://github.com/Yalantis/uCrop)
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)
- [lottie-android](https://github.com/airbnb/lottie-android)
- [ICU Transliterator](https://unicode-org.github.io/icu/userguide/transforms/general/)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
- [AMLLTTML歌词站](https://amlldb.bikonoo.com/)
- [LunaBeat歌词站](https://github.com/2755337087/ttml-hub/blob/main/README.md)

### Inspiración

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
