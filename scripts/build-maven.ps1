# Script de build Maven para comprae-client-sdk
try {
    Push-Location ".."
    Write-Host "Navegando para o diretório comprae-client-sdk..." -ForegroundColor Blue
} catch {
    Write-Host "❌ Não foi possível navegar para o diretório comprae-client-sdk" -ForegroundColor Red
    exit 1
}
mvn clean package

if ($LASTEXITCODE -eq 0) {
	Write-Host "Build Maven concluído com sucesso."
	$buildSuccess = $true
} else {
	Write-Host "Build Maven falhou."
	$buildSuccess = $false
}

return $buildSuccess

Write-Host "O resultado do build Maven é: $buildSuccess"
