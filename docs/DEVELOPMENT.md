# Desenvolvimento do AlbionSM

## Fluxo de versão
1. Usuário testa o APK no dispositivo real.
2. Feedback aprovado vira requisito da próxima versão.
3. Implementação é feita na branch main.
4. CI executa `testDebugUnitTest` e `assembleDebug`.
5. APK recebe nome com a versão.
6. Versão é testada no dispositivo.
7. Changelog e estado funcional são mantidos no repositório.

## Regras de dados
- Fonte de mercado: Albion Online Data Project, servidor Americas.
- Não fabricar preço, oportunidade ou disponibilidade.
- Sem dados suficientes = SEM DADOS.
- Cotação desatualizada pode ser exibida, mas não deve ser escolhida como melhor ação.
- Cidade → Cidade é o modo funcional atual.
- Flip Local / Black Market só deixam de ser SEM DADOS após integração real.

## Assinatura Android
Objetivo: permitir atualização V0.4 → V0.5 → V0.6 sem desinstalar o app.

A chave privada de assinatura e suas senhas são segredos e não podem ser commitidas. O workflow deve receber um keystore persistente e credenciais por GitHub Actions Secrets e materializá-lo apenas durante o job.

Sugestão de secrets:
- `ALBIONSM_KEYSTORE_BASE64`
- `ALBIONSM_KEYSTORE_PASSWORD`
- `ALBIONSM_KEY_ALIAS`
- `ALBIONSM_KEY_PASSWORD`

Quando esses secrets estiverem configurados, o Gradle/Actions deve assinar todas as versões distribuídas com a mesma chave. O APK já instalado só aceita atualização se applicationId e assinatura forem compatíveis e versionCode for superior.

## Identidade
- applicationId: `com.albionsolomarket.app`
- V0.4: versionCode 4 / versionName `0.4.0-beta`
