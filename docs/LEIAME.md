# FOCUS OS 0.1.0 — instalação e testes

## 1. Gerar o APK
**Opção A – Android Studio:** abra a pasta FocusOS (aceite instalar o SDK 34 / JDK 17) → Build > Build APK(s).
APK em app/build/outputs/apk/debug/app-debug.apk.
**Opção B – GitHub:** crie um repositório, envie esta pasta (branch main) → aba Actions > Build APK → baixe "FocusOS-apk".

## 2. Instalar na central
Copie o APK para um pendrive → gerenciador de arquivos da central → toque no APK (permita "fontes desconhecidas").
Via ADB: `adb install app-debug.apk`.

## 3. Launcher padrão
1. Aperte Home: se a central perguntar, escolha FOCUS OS > "Sempre".
2. Senão: FOCUS OS > ⚙ > "Launcher padrão", ou Configurações do Android > Apps > Apps padrão > App inicial.
3. Via ADB (sem root): `adb shell cmd package set-home-activity br.focusos/.MainActivity`.
4. Se o launcher de fábrica reassumir: descubra-o com
   `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME`
   e, só se necessário, `adb shell pm disable-user --user 0 <pacote>` (desfaz com `pm enable <pacote>`).

## 4. Música
FOCUS OS > cartão "Música" > Permitir. Se o Android 14 bloquear (app instalado fora da loja): Configurações > Apps > FOCUS OS > ⋮ > "Permitir configurações restritas" e tente de novo.

## 5. Teste de inicialização
Reinicie a central; o FOCUS OS deve abrir como tela inicial. Desligue o carro, espere a suspensão, ligue de novo e veja se volta ao FOCUS OS. Opcional: ative "Abrir ao ligar" + "Sobrepor apps" (o Android 10+ bloqueia abrir telas em segundo plano sem essa permissão).

## 6. Atalhos
Toque longo em Waze/Maps/Spotify/Rádio FM/USB para escolher outro app. Rádio e USB começam vazios (o pacote varia por firmware).

## Limitações desta versão
- Carro: ilustração vetorial lateral (um ângulo). Vários ângulos exigem imagens/modelo 3D.
- Sem CANBUS/OBD-II: velocidade, RPM etc. aparecem como "Não conectado".
- Sem temperatura externa, estado da ligação com o telefone nem app de navegação ativo (sem fonte de dados confiável).
- Favoritos/reordenação de atalhos: só os 5 atalhos editáveis; reorganização fica para a próxima versão.
- Código não compilado nem testado em aparelho: se der erro de build, envie a mensagem.
