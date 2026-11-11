# A Igreja — Spec

Tela com as informações básicas da igreja, o que um visitante procura primeiro: onde fica, quando tem culto e como
seguir. É aberta para todos, logado ou não, e funciona offline.

Os dados ficam **fixos no app**, sem backend: mudam pouco, e o app já força a atualização ao abrir. Mudar qualquer dado
exige nova versão.

---

## 1. Navegação

- Rota `AppRoutes.CHURCH` (`church`), `composable` inline no `AppNavHost`.
- Abre pelo item "A Igreja" do menu lateral, sempre visível (`specs/core/spec.md` §6.3).

## 2. Conteúdo

A tela tem duas partes, numa coluna só com rolagem.

**Cabeçalho (cartão de visita).** Faixa verde (`ipbGreen`, a mesma cor da barra do topo), com os cantos de baixo
arredondados. De cima para baixo: o logo branco da IPB (`ipb_white_logo`), "IPB Castelo Branco",
"Igreja Presbiteriana do Brasil" e o endereço com o ícone de lugar. Embaixo, três botões do mesmo tamanho, lado a lado:

| Botão | Ícone | Ação |
|-------|-------|------|
| Como chegar | direções | abre o app de mapas buscando o endereço |
| Instagram | logo do Instagram | abre `https://www.instagram.com/ipb.castelobranco/` no app do Instagram ou no navegador |
| Ofertas | símbolo do PIX | abre a folha "Dízimos e ofertas" |

Endereço: R. Celestino de Almeida, 308 - Castelo Branco, Cariacica - ES. Sem app que abra o mapa ou o link, nada
acontece.

**Folha "Dízimos e ofertas".** Folha que sobe de baixo, com o título, a frase "Aponte a câmera do app do banco ou copie
o código.", o QR code centralizado e o botão cheio "Copiar código PIX". O botão copia o código copia e cola e diz
"Copiado" por 2 s.

**Horários.** Título "Horários" e um cartão com os cultos agrupados por dia. A semana começa na segunda, então o
Domingo vem por último. Cada dia mostra o nome em cima (maiúsculas pequenas, na cor primária) e, abaixo, uma linha por
culto com o horário em negrito e o nome. Todas as linhas de culto têm a mesma altura, e o espaço em volta da divisória
entre dois dias é parecido com o espaço entre dois cultos do mesmo dia.

| Dia | Horário | Nome |
|-----|---------|------|
| Terça | 19:30 | Culto de oração |
| Quinta | 19:30 | Culto de oração |
| Domingo | 09:00 | Escola bíblica dominical |
| Domingo | 19:30 | Culto de adoração |

**Selo do culto.** Um culto da lista leva um selo, calculado pela hora do aparelho e atualizado a cada minuto:

- **"Agora"** (fundo verde): o culto começou há menos de 90 minutos.
- **"Próximo"**: senão, o culto que começa primeiro a partir de agora, mesmo que seja na semana seguinte.

**PIX.** É o mesmo PIX estático do QR impresso da igreja (chave CPF do recebedor, sem valor fixo); publicá-lo com o
CPF foi decisão do responsável. O código fica em `ChurchInfo.PIX_CODE`, e o QR é um vetor gerado dele
(`res/drawable/church_pix_qr.xml`, fundo branco próprio para ler também no tema escuro): mudar a chave exige gerar os
dois de novo, juntos.

## 3. Fora do escopo

- Editar os dados pelo Painel de Gestão.
- WhatsApp e YouTube: a igreja não tem.
