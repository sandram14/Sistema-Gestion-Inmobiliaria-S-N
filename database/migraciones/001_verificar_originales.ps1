# =====================================================================
# Migración 001 - Paso 1: verificar originales y copias (NO modifica nada)
# =====================================================================
# Para cada una de las 66 filas de database/seed/imagenes/MAPEO.csv comprueba que:
#   * el original exista en frontend/public/images/... y tenga la huella registrada;
#   * la copia exista en database/seed/imagenes/<codigo>/<uuid>.png con la misma huella.
# Termina con código 1 si encuentra cualquier diferencia.
#
# Uso (desde la raíz del repositorio):
#   powershell -ExecutionPolicy Bypass -File database\migraciones\001_verificar_originales.ps1
# =====================================================================
$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$mapeo = Import-Csv -Path (Join-Path $raiz 'database\seed\imagenes\MAPEO.csv') -Delimiter ';'

$problemas = New-Object System.Collections.Generic.List[string]
if ($mapeo.Count -ne 66) { $problemas.Add("MAPEO.csv debe tener 66 filas y tiene $($mapeo.Count)") }

foreach ($f in $mapeo) {
    $original = Join-Path $raiz ('frontend\public' + ($f.url_anterior -replace '/', '\'))
    $copia = Join-Path $raiz ('database\seed\imagenes' + (($f.url_nueva -replace '^/uploads', '') -replace '/', '\'))
    foreach ($archivo in @($original, $copia)) {
        if (-not (Test-Path -LiteralPath $archivo)) {
            $problemas.Add("Falta: $archivo")
        } else {
            $huella = (Get-FileHash -LiteralPath $archivo -Algorithm SHA256).Hash.ToLower()
            if ($huella -ne $f.sha256) { $problemas.Add("Huella distinta: $archivo") }
        }
    }
}

if ($problemas.Count -gt 0) {
    Write-Host "DETENER: se encontraron $($problemas.Count) diferencias. No continúe con la migración." -ForegroundColor Red
    $problemas | ForEach-Object { Write-Host "  $_" }
    exit 1
}
Write-Host "OK: 66 originales y 66 copias verificados por SHA-256 (frontend/public/images = database/seed/imagenes)." -ForegroundColor Green
