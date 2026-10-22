<div align="center">

# LunaBeat

**Melodia e letra se encontram a cada reprodução.**

Reprodutor de música local para Android · Edição de letras palavra por palavra · Gerenciamento de biblioteca e tags

[Baixar a versão mais recente](https://github.com/2755337087/LunaBeat/releases/latest) · [Guia de uso](#quick-start) · [Relatar um problema](https://github.com/2755337087/LunaBeat/issues) · [Comunidade](#community-and-feedback)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

O LunaBeat reúne a reprodução de música local, a criação de letras sincronizadas palavra por palavra e a organização da biblioteca em um único aplicativo. Ouça com tranquilidade e acompanhe as letras ao ritmo da melodia, ou segmente a letra de uma música, ajuste os tempos, adicione traduções e informações de dueto e exporte o resultado para seu próprio fluxo de trabalho.

## Prévia da interface

### Interface para celular

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · Interface para celular" width="960" />
</p>

### Interface para tablet

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · Interface para tablet" width="960" />
</p>

## Principais recursos

| Uso | O que você pode fazer |
| --- | --- |
| Ouvir música local | Buscar arquivos de áudio locais e navegar por artista, álbum ou playlist; usar reprodução em segundo plano, controles de mídia do sistema, fila de reprodução e temporizador de desligamento. |
| Acompanhar as letras | Pré-visualizar letras palavra por palavra com temas claros e escuros e ajustes da barra de reprodução e do plano de fundo; exibir letras na tela inicial, na barra de status e pelo Bluetooth do carro. |
| Sincronizar palavra por palavra | Usar os três botões Início / Continuar / Fim e refinar os tempos com ajustes de velocidade, saltos no tempo, modo de acompanhamento, desfazer e refazer. |
| Editar letras | Segmentar palavras, dividir e unir letras, mover linhas, deslocar marcações de tempo, converter entre chinês simplificado e tradicional, remover linhas vazias e configurar traduções, duetos e vocais de apoio. |
| Organizar músicas | Gerenciar playlists, favoritos e músicas com cinco estrelas; encontrar capas, tags e letras em lote, editar metadados e renomear arquivos. |

### Importação e exportação

| Conteúdo | Importação | Exportação |
| --- | --- | --- |
| Letras em texto simples | Compatível | — |
| LRC (linha por linha / palavra por palavra) | Compatível | Compatível |
| LRC aprimorado (ELRC) | Compatível | Compatível |
| TTML | Compatível | Compatível, preservando informações de dueto e vocais de apoio |

Você também pode pesquisar letras ou importar áudio para começar a criar. Recomenda-se TTML para preservar informações estruturais, como duetos e vocais de apoio; LRC serve para a exibição comum de letras.

<a id="quick-start"></a>

## Primeiros passos

### Ouvir e organizar a biblioteca

1. Abra a biblioteca musical e busque os arquivos de áudio locais do dispositivo.
2. Navegue por artista, álbum, playlist ou favoritos e toque em uma música para reproduzi-la.
3. Abra o reprodutor completo para mudar o modo de reprodução, gerenciar a fila, ver letras ou configurar o temporizador de desligamento.
4. Para organizar arquivos, encontre primeiro tags e letras em lote; depois edite as tags ou renomeie os arquivos.

### Criar a letra de uma música sincronizada palavra por palavra

1. **Importar**: selecione o áudio e importe letras em texto simples, LRC, ELRC ou TTML. Você também pode pesquisar letras.
2. **Preparar**: segmente primeiro as palavras; depois remova linhas vazias, una letras ou converta entre chinês simplificado e tradicional. Se já houver tempos definidos, desloque toda a linha do tempo para alinhá-la primeiro.
3. **Sincronizar**: reproduza o áudio e use os três botões abaixo no ritmo de cada linha da letra.
4. **Refinar**: toque duas vezes na letra para voltar e ouvir novamente. Confira os tempos com o modo de acompanhamento, desfazer e refazer; pressione uma unidade ou linha por alguns instantes para dividir, unir, mover ou editar traduções.
5. **Pré-visualizar e exportar**: confira o efeito palavra por palavra; escolha LRC para a exibição comum ou TTML para informações de dueto e vocais de apoio.

| Botão | Ação |
| --- | --- |
| Início | Define o início da unidade atual e permanece nela. |
| Continuar | Define o fim da unidade atual, avança para a próxima e também define o início dela. |
| Fim | Define o fim da unidade atual e avança para a próxima, cujo início será definido depois. |

Sequência recomendada: `Início → Continuar → Continuar → … → Fim`, repetida em cada linha.

<details>
<summary>Dicas de sincronização</summary>

- As colunas de tempo e texto mostram início, letra, fim e tradução.
- Toque uma vez em uma unidade para selecioná-la e duas vezes para ir ao início dela e ouvir novamente.
- Há suporte para retroceder / avançar, reproduzir / pausar, alterar a velocidade e ir para um tempo específico.
- Toque no tempo de reprodução para entrar no modo de acompanhamento; toque no tempo na área de sincronização para escolher um destino.
- Os duetos podem ser configurados à esquerda (voz principal), à direita (segunda voz) ou no fundo (vocais de apoio) e exportados em TTML.

</details>

## Perguntas frequentes

<details>
<summary>Devo escolher LRC ou TTML?</summary>

Escolha LRC para a exibição comum de letras ou TTML para preservar informações estruturais, como duetos e vocais de apoio. Confira o efeito palavra por palavra na prévia antes de exportar.

</details>

<details>
<summary>O que devo considerar ao exportar e reproduzir letras com duetos?</summary>

Prefira TTML para exportar informações de dueto e vocais de apoio e veja o resultado em um reprodutor compatível com esses efeitos. A exibição depende do suporte do reprodutor ao formato da letra e às informações associadas.

</details>

<details>
<summary>Como ajusto letras que estão todas adiantadas ou atrasadas?</summary>

Desloque primeiro as marcações de tempo para alinhar o conjunto e depois volte à tela de sincronização para refinar trechos específicos.

</details>

<details>
<summary>Por onde começar se nunca sincronizei letras?</summary>

Conclua a segmentação das palavras e a limpeza do texto antes de sincronizar. Escolha uma música conhecida, use “Início → Continuar → … → Fim” em cada linha e depois ouça novamente e faça pequenos ajustes.

</details>

<a id="community-and-feedback"></a>

## Comunidade e feedback

- [GitHub Issues · Relatos de erros e sugestões de recursos](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · Grupo da comunidade LunaBeat](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · Grupo da comunidade LunaBeat](https://qm.qq.com/q/N0fBvuWKOY) · Número do grupo: **964680520**

Ao relatar um problema, inclua, se possível, a versão do aplicativo, o dispositivo e a versão do Android, os passos para reproduzir o problema e capturas de tela ou formatos de arquivo relacionados, para facilitar o diagnóstico.

## Agradecimentos

### Apoio técnico

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

### Inspiração

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
