# Script PowerShell para executar diferentes tipos de teste

param(
    [Parameter(Position=0)]
    [string]$TipoTeste = "all"
)

Write-Host "🧪 Comprae Client SDK - Executador de Testes" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

function Executar-TestesUnitarios {
    Write-Host "🔧 Executando testes unitários..." -ForegroundColor Green
    mvn clean test
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Testes unitários concluídos com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Falha nos testes unitários!" -ForegroundColor Red
        exit 1
    }
}

function Executar-TestesIntegracao {
    Write-Host "🔗 Executando testes de integração..." -ForegroundColor Yellow
    mvn clean verify -Dtest=none -DfailIfNoTests=false -Dit.test=**/integracao/*Test
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Testes de integração concluídos com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Falha nos testes de integração!" -ForegroundColor Red
        exit 1
    }
}

function Executar-TestesPerformance {
    Write-Host "⚡ Executando testes de performance..." -ForegroundColor Magenta
    mvn clean verify -Dtest=none -DfailIfNoTests=false -Dit.test=**/performance/*Test
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Testes de performance concluídos com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Falha nos testes de performance!" -ForegroundColor Red
        exit 1
    }
}

function Executar-TodosOsTestes {
    Write-Host "🚀 Executando todos os testes..." -ForegroundColor Cyan
    mvn clean verify
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Todos os testes concluídos com sucesso!" -ForegroundColor Green
        Mostrar-RelatorioCobertura
    } else {
        Write-Host "❌ Falha na execução dos testes!" -ForegroundColor Red
        exit 1
    }
}

function Mostrar-RelatorioCobertura {
    Write-Host ""
    Write-Host "📊 Relatório de Cobertura:" -ForegroundColor Cyan
    Write-Host "   Arquivo: target/site/jacoco/index.html" -ForegroundColor Gray
    
    if (Test-Path "target/site/jacoco/index.html") {
        Write-Host "   Abrindo relatório no navegador..." -ForegroundColor Gray
        Start-Process "target/site/jacoco/index.html"
    }
}

function Mostrar-Ajuda {
    Write-Host "Uso: .\test-runner.ps1 [TIPO]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Tipos de teste disponíveis:" -ForegroundColor Green
    Write-Host "  unit        - Apenas testes unitários" -ForegroundColor White
    Write-Host "  integration - Apenas testes de integração" -ForegroundColor White
    Write-Host "  performance - Apenas testes de performance" -ForegroundColor White
    Write-Host "  all         - Todos os testes (padrão)" -ForegroundColor White
    Write-Host "  help        - Mostrar esta ajuda" -ForegroundColor White
    Write-Host ""
    Write-Host "Exemplos:" -ForegroundColor Green
    Write-Host "  .\test-runner.ps1 unit        # Executa apenas testes unitários" -ForegroundColor Gray
    Write-Host "  .\test-runner.ps1 performance # Executa apenas benchmarks" -ForegroundColor Gray
    Write-Host "  .\test-runner.ps1 all         # Executa todos os testes" -ForegroundColor Gray
}

# Processar comando
switch ($TipoTeste.ToLower()) {
    "unit" {
        Executar-TestesUnitarios
    }
    "integration" {
        Executar-TestesIntegracao
    }
    "performance" {
        Executar-TestesPerformance
    }
    "all" {
        Executar-TodosOsTestes
    }
    "help" {
        Mostrar-Ajuda
    }
    default {
        Write-Host "Tipo de teste desconhecido: $TipoTeste" -ForegroundColor Red
        Write-Host ""
        Mostrar-Ajuda
        exit 1
    }
}

Write-Host ""
Write-Host "🎉 Execução concluída!" -ForegroundColor Green