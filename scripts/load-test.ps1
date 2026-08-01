<#
.SYNOPSIS
    Stress test HPA hr-backend (staging) - genere de la charge concurrente
    via un port-forward local, pour contourner le NetworkPolicy
    default-deny-all du namespace (qui bloque le DNS/egress des pods
    ad-hoc sans label app=hr-backend/hr-frontend).

.PARAMETER DurationSeconds
    Duree de la charge en secondes (defaut 180).

.PARAMETER Concurrency
    Nombre de boucles curl paralleles (defaut 40).

.PARAMETER Namespace
    Namespace cible (defaut staging).

.EXAMPLE
    .\load-test.ps1
    .\load-test.ps1 -DurationSeconds 120 -Concurrency 20
#>

param(
    [int]$DurationSeconds = 180,
    [int]$Concurrency = 40,
    [string]$Namespace = "staging"
)

Write-Host "Ouverture du port-forward vers hr-backend (namespace: $Namespace)..."
$portForwardJob = Start-Job -ScriptBlock {
    param($ns)
    kubectl port-forward svc/hr-backend -n $ns 8081:8081
} -ArgumentList $Namespace

Start-Sleep -Seconds 3

try {
    Invoke-WebRequest -Uri "http://localhost:8081/api/health-check" -UseBasicParsing -TimeoutSec 5 | Out-Null
    Write-Host "Port-forward OK, health-check a repondu."
} catch {
    Write-Host "ATTENTION : health-check n'a pas encore repondu, on continue quand meme."
}

Write-Host "Lancement de $Concurrency jobs de charge concurrents pendant $DurationSeconds secondes..."
$loadJobs = 1..$Concurrency | ForEach-Object {
    Start-Job -ScriptBlock {
        param($duration)
        $end = (Get-Date).AddSeconds($duration)
        while ((Get-Date) -lt $end) {
            try {
                Invoke-WebRequest -Uri "http://localhost:8081/api/employees" -UseBasicParsing -TimeoutSec 5 | Out-Null
            } catch {}
        }
    } -ArgumentList $DurationSeconds
}

Write-Host "Charge en cours... (observer dans un autre terminal : kubectl get hpa hr-backend -n $Namespace -w)"
$loadJobs | Wait-Job | Out-Null
Write-Host "Charge terminee."

$loadJobs | Remove-Job

Write-Host "Arret du port-forward..."
Stop-Job -Job $portForwardJob
Remove-Job -Job $portForwardJob -Force

Write-Host "Termine. Redescente HPA automatique sous ~5 min : kubectl get hpa hr-backend -n $Namespace -w"
