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

| Seção | Conteúdo | Ação |
|-------|----------|------|
| Endereço | R. Celestino de Almeida, 308 - Castelo Branco, Cariacica - ES | **Mapa**: abre o app de mapas buscando o endereço |
| Cultos | lista abaixo | nenhuma |
| Instagram | @ipb.castelobranco | **Abrir**: abre `https://www.instagram.com/ipb.castelobranco/` no app do Instagram ou no navegador |
| Dízimos e ofertas | "PIX" (sem o nome do recebedor) | **QR**: abre o QR code, centralizado na horizontal, num diálogo. **Copiar**: copia o código copia e cola; o botão diz "Copiado" por 2 s |

Sem app que abra o mapa ou o link, nada acontece.

**PIX.** É o mesmo PIX estático do QR impresso da igreja (chave CPF do recebedor, sem valor fixo); publicá-lo com o
CPF foi decisão do responsável. O código fica em `ChurchInfo.PIX_CODE`, e o QR é um vetor gerado dele
(`res/drawable/church_pix_qr.xml`, fundo branco próprio para ler também no tema escuro): mudar a chave exige gerar os
dois de novo, juntos.

Cultos, na ordem da semana:

| Dia | Horário | Nome |
|-----|---------|------|
| Domingo | 09:00 | EBD |
| Domingo | 19:30 | Culto |
| Terça | 19:30 | Culto de oração |
| Quinta | 19:30 | Culto de oração |

## 3. Fora do escopo

- Editar os dados pelo Painel de Gestão.
- WhatsApp e YouTube: a igreja não tem.
