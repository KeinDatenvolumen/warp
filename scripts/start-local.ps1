$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$LogDirectory = Join-Path $ProjectRoot "logs/local"
$StartedProcesses = @()

function Write-Heading {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text
    )

    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Text
    Write-Host "============================================================"
}

function Assert-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CommandName,

        [Parameter(Mandatory = $true)]
        [string]$InstallationHint
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Der Befehl '$CommandName' wurde nicht gefunden. $InstallationHint"
    }
}

function Assert-PortAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,

        [Parameter(Mandatory = $true)]
        [string]$ServiceName
    )

    $Connection = Get-NetTCPConnection `
        -LocalPort $Port `
        -State Listen `
        -ErrorAction SilentlyContinue

    if ($Connection) {
        throw "Port $Port ist bereits belegt. Betroffener Dienst: $ServiceName"
    }
}

function Start-WarpProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Executable,

        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory
    )

    $StandardOutput = Join-Path $LogDirectory "$Name.log"
    $StandardError = Join-Path $LogDirectory "$Name-error.log"

    Write-Host "Starte $Name ..."

    $Process = Start-Process `
        -FilePath $Executable `
        -ArgumentList $Arguments `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $StandardOutput `
        -RedirectStandardError $StandardError `
        -PassThru `
        -NoNewWindow

    $script:StartedProcesses += $Process

    Write-Host "  PID: $($Process.Id)"
    Write-Host "  Log: $StandardOutput"

    return $Process
}

function Wait-ForHealth {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ServiceName,

        [Parameter(Mandatory = $true)]
        [string]$HealthUrl
    )

    for ($Attempt = 1; $Attempt -le 30; $Attempt++) {
        try {
            $Response = Invoke-RestMethod `
                -Uri $HealthUrl `
                -Method Get `
                -TimeoutSec 2

            if ($Response.status -eq "UP") {
                Write-Host ("{0,-20} UP" -f $ServiceName)
                return
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "$ServiceName wurde nicht rechtzeitig bereit. Prüfe die Logs unter $LogDirectory."
}

function Stop-WarpProcesses {
    Write-Host ""
    Write-Host "Beende lokale WaRP-Prozesse ..."

    foreach ($Process in $script:StartedProcesses) {
        if ($null -ne $Process -and -not $Process.HasExited) {
            Stop-Process `
                -Id $Process.Id `
                -Force `
                -ErrorAction SilentlyContinue
        }
    }

    Write-Host "Alle durch dieses Skript gestarteten Prozesse wurden beendet."
}

try {
    Set-Location $ProjectRoot

    Write-Heading "WaRP lokale Entwicklungsumgebung"

    Assert-Command `
        -CommandName "java" `
        -InstallationHint "Installiere ein JDK 21 und konfiguriere JAVA_HOME."

    Assert-Command `
        -CommandName "node" `
        -InstallationHint "Installiere Node.js 22.12.0 oder neuer."

    Assert-Command `
        -CommandName "npm" `
        -InstallationHint "npm wird gemeinsam mit Node.js installiert."

    $WrapperJar = Join-Path `
        $ProjectRoot `
        "gradle/wrapper/gradle-wrapper.jar"

    if (-not (Test-Path $WrapperJar)) {
        throw "gradle/wrapper/gradle-wrapper.jar fehlt."
    }

    Write-Heading "Installierte Versionen"

    & java -version
    & node --version
    & npm --version
    & "$ProjectRoot/gradlew.bat" --version

    Write-Heading "Ports prüfen"

    Assert-PortAvailable -Port 8080 -ServiceName "BFF-Service"
    Assert-PortAvailable -Port 8081 -ServiceName "Identity-Service"
    Assert-PortAvailable -Port 8082 -ServiceName "Routing-Service"
    Assert-PortAvailable -Port 8083 -ServiceName "Weather-Service"
    Assert-PortAvailable -Port 8084 -ServiceName "Storage-Service"
    Assert-PortAvailable -Port 5173 -ServiceName "React-Frontend"

    Write-Host "Alle benötigten Ports sind frei."

    New-Item `
        -ItemType Directory `
        -Force `
        -Path $LogDirectory `
        | Out-Null

    $NodeModules = Join-Path $ProjectRoot "frontend/node_modules"

    if (-not (Test-Path $NodeModules)) {
        Write-Heading "Frontend-Abhängigkeiten installieren"

        Push-Location (Join-Path $ProjectRoot "frontend")

        try {
            & npm ci

            if ($LASTEXITCODE -ne 0) {
                throw "npm ci ist fehlgeschlagen."
            }
        }
        finally {
            Pop-Location
        }
    }

    Write-Heading "Backend-Services starten"

    Start-WarpProcess `
        -Name "bff-service" `
        -Executable "$ProjectRoot/gradlew.bat" `
        -Arguments @(
            ":services:bff-service:bootRun",
            "--console=plain"
        ) `
        -WorkingDirectory $ProjectRoot `
        | Out-Null

    Start-WarpProcess `
        -Name "identity-service" `
        -Executable "$ProjectRoot/gradlew.bat" `
        -Arguments @(
            ":services:identity-service:bootRun",
            "--console=plain"
        ) `
        -WorkingDirectory $ProjectRoot `
        | Out-Null

    Start-WarpProcess `
        -Name "routing-service" `
        -Executable "$ProjectRoot/gradlew.bat" `
        -Arguments @(
            ":services:routing-service:bootRun",
            "--console=plain"
        ) `
        -WorkingDirectory $ProjectRoot `
        | Out-Null

    Start-WarpProcess `
        -Name "weather-service" `
        -Executable "$ProjectRoot/gradlew.bat" `
        -Arguments @(
            ":services:weather-service:bootRun",
            "--console=plain"
        ) `
        -WorkingDirectory $ProjectRoot `
        | Out-Null

    Start-WarpProcess `
        -Name "storage-service" `
        -Executable "$ProjectRoot/gradlew.bat" `
        -Arguments @(
            ":services:storage-service:bootRun",
            "--console=plain"
        ) `
        -WorkingDirectory $ProjectRoot `
        | Out-Null

    Write-Heading "Frontend starten"

    Start-WarpProcess `
        -Name "frontend" `
        -Executable "npm.cmd" `
        -Arguments @("run", "dev") `
        -WorkingDirectory (Join-Path $ProjectRoot "frontend") `
        | Out-Null

    Write-Heading "Auf Services warten"

    Wait-ForHealth `
        -ServiceName "BFF-Service" `
        -HealthUrl "http://localhost:8080/actuator/health/readiness"

    Wait-ForHealth `
        -ServiceName "Identity-Service" `
        -HealthUrl "http://localhost:8081/actuator/health/readiness"

    Wait-ForHealth `
        -ServiceName "Routing-Service" `
        -HealthUrl "http://localhost:8082/actuator/health/readiness"

    Wait-ForHealth `
        -ServiceName "Weather-Service" `
        -HealthUrl "http://localhost:8083/actuator/health/readiness"

    Wait-ForHealth `
        -ServiceName "Storage-Service" `
        -HealthUrl "http://localhost:8084/actuator/health/readiness"

    Write-Heading "WaRP wurde erfolgreich gestartet"

    Write-Host "Frontend:"
    Write-Host "  http://localhost:5173"
    Write-Host ""
    Write-Host "BFF-Gesamtstatus:"
    Write-Host "  http://localhost:8080/api/v1/system/health"
    Write-Host ""
    Write-Host "Logs:"
    Write-Host "  $LogDirectory"
    Write-Host ""
    Write-Host "Drücke Strg+C, um alle gestarteten Prozesse zu beenden."

    while ($true) {
        Start-Sleep -Seconds 5

        foreach ($Process in $StartedProcesses) {
            if ($Process.HasExited) {
                throw "Der Prozess mit PID $($Process.Id) wurde unerwartet beendet."
            }
        }
    }
}
finally {
    Stop-WarpProcesses
}