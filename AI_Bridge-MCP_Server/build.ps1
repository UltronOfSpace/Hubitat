param(
    [string]$OutputDir = "$PSScriptRoot\dist"
)

$ErrorActionPreference = "Stop"
$srcDir = $PSScriptRoot
$zipName = "AI_Bridge-MCP_Server.zip"
$outputPath = Join-Path $OutputDir $zipName

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }
if (Test-Path $outputPath) { Remove-Item $outputPath -Force }

$files = @(
    "UltronOfSpace.mcpProtocol.groovy",
    "UltronOfSpace.hubitatMcpServer.groovy",
    "install.txt",
    "update.txt"
) | ForEach-Object { Join-Path $srcDir $_ }

foreach ($f in $files) {
    if (-not (Test-Path $f)) { throw "Missing file: $f" }
}

Compress-Archive -Path $files -DestinationPath $outputPath -Force
Write-Host "Built bundle: $outputPath"
Write-Host "Size: $([math]::Round((Get-Item $outputPath).Length/1KB, 1)) KB"
