# =====================================================================
# Migración 001 - Verificar que TODAS las imágenes de la base se sirven (NO modifica nada)
# =====================================================================
# Descarga cada URL de la tabla imagen desde el backend y comprueba:
#   * que ninguna URL siga empezando con /images/;
#   * que todas respondan 200;
#   * que las 66 fotos migradas lleguen con la huella SHA-256 registrada en MAPEO.csv.
# Termina con código 1 si alguna falla.
#
# Uso (desde la raíz del repositorio, con la base y Tomcat levantados):
#   powershell -ExecutionPolicy Bypass -File database\migraciones\001_verificar_http.ps1
# Parámetros opcionales: -Base http://localhost:8080   -ArchivoUrls <lista de URLs, una por línea>
# =====================================================================
param(
    [string]$Base = 'http://localhost:8080',
    [string]$ArchivoUrls = ''
)
$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$huellas = @{}
Import-Csv -Path (Join-Path $raiz 'database\seed\imagenes\MAPEO.csv') -Delimiter ';' | ForEach-Object { $huellas[$_.url_nueva] = $_.sha256 }

if ($ArchivoUrls) {
    $urls = Get-Content -LiteralPath $ArchivoUrls | Where-Object { $_.Trim() -ne '' }
} else {
    $compose = Join-Path $raiz 'docker\docker-compose.yml'
    $urls = docker compose -f $compose exec -T db psql -U sn_app -d sn_inmobiliaria -Atc "SELECT url FROM imagen ORDER BY url"
    if ($LASTEXITCODE -ne 0) { Write-Host 'No se pudo consultar la base (¿está levantada?).' -ForegroundColor Red; exit 1 }
}

$temporal = [System.IO.Path]::GetTempFileName()
$fallas = New-Object System.Collections.Generic.List[string]
$verificadasConHuella = 0
foreach ($url in $urls) {
    if ($url.StartsWith('/images/')) { $fallas.Add("Sigue apuntando a /images: $url"); continue }
    if (-not $url.StartsWith('/')) { continue }   # URLs externas (https://...) no se verifican
    try {
        $r = Invoke-WebRequest -Uri ($Base + $url) -OutFile $temporal -PassThru -UseBasicParsing
        if ($r.StatusCode -ne 200) { $fallas.Add("HTTP $($r.StatusCode): $url"); continue }
    } catch {
        $fallas.Add("No responde: $url ($($_.Exception.Message))"); continue
    }
    if ($huellas.ContainsKey($url)) {
        $h = (Get-FileHash -LiteralPath $temporal -Algorithm SHA256).Hash.ToLower()
        if ($h -ne $huellas[$url]) { $fallas.Add("Contenido distinto al original: $url") } else { $verificadasConHuella++ }
    }
}
Remove-Item -LiteralPath $temporal -ErrorAction SilentlyContinue

Write-Host "Imágenes en la base: $(@($urls).Count) · fotos migradas verificadas por huella: $verificadasConHuella de 66"
if ($fallas.Count -gt 0) {
    Write-Host "DETENER: $($fallas.Count) problemas:" -ForegroundColor Red
    $fallas | ForEach-Object { Write-Host "  $_" }
    exit 1
}
if ($verificadasConHuella -ne 66) {
    Write-Host 'DETENER: no se encontraron las 66 fotos migradas en la base.' -ForegroundColor Red
    exit 1
}
Write-Host 'OK: todas las imágenes responden 200 y las 66 migradas son idénticas a los originales.' -ForegroundColor Green
