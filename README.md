# AlbionSM — Albion Solo Market

Android companion app para Albion Online, servidor Americas. Kotlin + Jetpack Compose.

## Versão em desenvolvimento
**V0.4 beta** — applicationId `com.albionsolomarket.app`.

O modo **Cidade → Cidade** usa preços reais do Albion Online Data Project (AODP) e calcula lucro líquido, ROI, prata/hora, quantidade e confiança/frescor. Dados ausentes nunca são inventados: a interface mostra **SEM DADOS**.

A V0.4 reorganiza os modos: Black Market passa a ser destino de Cidade → Cidade; Flip Local recebe seleção de cidade e, em Caerleon, diferencia Market Flip de Black Market Flip. Esses modos só serão marcados como funcionais quando a lógica real estiver conectada.

## Build
GitHub Actions executa testes unitários e `assembleDebug`, gerando o artefato `AlbionSM-V0.4-debug-apk`.

## Assinatura
O projeto deve usar uma chave de assinatura persistente para builds distribuídos. **Keystores e senhas nunca entram no repositório.** Eles devem ser fornecidos ao GitHub Actions por Secrets. Até essa configuração externa existir, builds debug de runners diferentes podem não atualizar um APK já instalado.

Veja `CHANGELOG.md` e `docs/DEVELOPMENT.md`.
