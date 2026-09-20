# Albion Solo Market — Beta 0.1
Android companion app (Americas), Kotlin + Jetpack Compose.

## Beta
Inputs: capital/time. Modes: City→City, Local Flip, Caerleon/Black Market. Engine models net profit, ROI, silver/hour, quantity, freshness/confidence and Next Best Action. Missing data is never converted to zero.

## AODP
`AodpClient` isolates the Albion Online Data Project source. UI currently starts in `SEM DADOS` rather than shipping fake prices. Next iteration parses live quotes into scanner opportunities.

## Build
GitHub Actions workflow builds and uploads `app-debug.apk` as `AlbionSoloMarket-beta-apk`.
