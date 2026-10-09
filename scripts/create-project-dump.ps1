$ErrorActionPreference = 'Stop'

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$OutputName  = 'dump.txt'
$Output      = Join-Path $ProjectRoot $OutputName

$ExcludedDirs = @(
    '.git', '.gradle', '.idea', '.vscode', 'node_modules', 'build',
    'target', 'out', 'dist', 'coverage', 'logs', 'tmp', 'temp'
)

$SensitivePatterns = @(
    '.env', '.env.*', '*.pem', '*.key', '*.p12', '*.pfx', '*.jks', '*.keystore',
    'credentials*', 'secrets*', 'secret*',
    'application-local.yml', 'application-local.yaml', 'application-local.properties'
)
$SensitiveAllowlist = @('.env.example')

$IgnoredPatterns = @(
    'dump.txt', '*.log', '*.class', '*.jar', '*.war', '*.zip', '*.tar', '*.gz', '*.7z',
    '*.png', '*.jpg', '*.jpeg', '*.gif', '*.webp', '*.ico',
    '*.pdf', '*.doc', '*.docx', '*.xls', '*.xlsx',
    '*.db', '*.sqlite', '*.sqlite3',
    'Thumbs.db', 'desktop.ini'
)

$Branch = [string][char]0x251C + [char]0x2500 + [char]0x2500 + ' '
$Corner = [string][char]0x2514 + [char]0x2500 + [char]0x2500 + ' '
$Pipe   = [string][char]0x2502 + '   '

function Test-NameMatch {
    param([string]$Name, [string[]]$Patterns)
    foreach ($pattern in $Patterns) {
        if ($Name -like $pattern) { return $true }   # -like ist case-insensitive
    }
    return $false
}

function Test-Sensitive {
    param([string]$Name)
    if ($SensitiveAllowlist -contains $Name) { return $false }
    return Test-NameMatch -Name $Name -Patterns $SensitivePatterns
}

function Test-TextFile {
    param([string]$Path, [long]$Length)
    if ($Length -eq 0) { return $false }
    $stream = [System.IO.File]::Open($Path, 'Open', 'Read', 'ReadWrite')
    try {
        $buffer = New-Object byte[] 8192
        $count  = $stream.Read($buffer, 0, $buffer.Length)
    } finally {
        $stream.Dispose()
    }
    for ($i = 0; $i -lt $count; $i++) {
        if ($buffer[$i] -eq 0) { return $false }
    }
    return $true
}

function Get-VisibleChildren {
    param([string]$Dir)
    @(Get-ChildItem -LiteralPath $Dir -Force -ErrorAction SilentlyContinue |
        Where-Object {
            -not ($_.PSIsContainer -and ($ExcludedDirs -contains $_.Name)) -and
            -not ($_.Name -eq $OutputName -and $Dir -eq $ProjectRoot)
        } |
        Sort-Object Name)
}

function Get-Tree {
    param([string]$Dir, [string]$Prefix = '')
    $entries = Get-VisibleChildren -Dir $Dir
    for ($i = 0; $i -lt $entries.Count; $i++) {
        $entry  = $entries[$i]
        $isLast = ($i -eq $entries.Count - 1)
        if ($isLast) { $connector = $Corner } else { $connector = $Branch }
        "$Prefix$connector$($entry.Name)"
        $isLink = $entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint
        if ($entry.PSIsContainer -and -not $isLink) {
            if ($isLast) { $childPrefix = $Prefix + '    ' } else { $childPrefix = $Prefix + $Pipe }
            Get-Tree -Dir $entry.FullName -Prefix $childPrefix
        }
    }
}

function Get-ProjectFiles {
    param([string]$Dir)
    foreach ($entry in (Get-VisibleChildren -Dir $Dir)) {
        if ($entry.PSIsContainer) {
            $isLink = $entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint
            if (-not $isLink) { Get-ProjectFiles -Dir $entry.FullName }
        } else {
            $entry
        }
    }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$writer = New-Object System.IO.StreamWriter($Output, $false, $utf8NoBom)
$writer.NewLine = "`n"
$sep = '=' * 42

try {
    $writer.WriteLine($sep)
    $writer.WriteLine('WARP-PROJEKTDUMP')
    $writer.WriteLine("Erstellt am: $((Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ'))")
    $writer.WriteLine($sep)
    $writer.WriteLine()
    $writer.WriteLine('PROJEKTSTRUKTUR')
    $writer.WriteLine($sep)
    $writer.WriteLine('.')
    foreach ($line in (Get-Tree -Dir $ProjectRoot)) { $writer.WriteLine($line) }

    $writer.WriteLine()
    $writer.WriteLine()
    $writer.WriteLine($sep)
    $writer.WriteLine('DATEI-INHALTE')
    $writer.WriteLine($sep)

    foreach ($file in (Get-ProjectFiles -Dir $ProjectRoot)) {
        $rel = $file.FullName.Substring($ProjectRoot.Length).TrimStart('\', '/') -replace '\\', '/'

        if (Test-Sensitive -Name $file.Name) {
            Write-Host "Uebersprungen, potenziell sensibel: ./$rel" -ForegroundColor Yellow
            continue
        }
        if (Test-NameMatch -Name $file.Name -Patterns $IgnoredPatterns) { continue }

        try {
            if (-not (Test-TextFile -Path $file.FullName -Length $file.Length)) { continue }
            $content = [System.IO.File]::ReadAllText($file.FullName) -replace "`r`n", "`n"
        } catch {
            Write-Host "Uebersprungen, nicht lesbar: ./$rel" -ForegroundColor Yellow
            continue
        }

        $writer.WriteLine()
        $writer.WriteLine()
        $writer.WriteLine("--- FILE: $rel ---")
        $writer.Write($content)
        if (-not $content.EndsWith("`n")) { $writer.WriteLine() }
    }

    $writer.WriteLine()
    $writer.WriteLine()
    $writer.WriteLine($sep)
    $writer.WriteLine('DUMP ABGESCHLOSSEN')
    $writer.WriteLine($sep)
} finally {
    $writer.Dispose()
}

Write-Host 'Fertig. Der Projektdump wurde erstellt:'
Write-Host $Output
Write-Host ''
Write-Host 'WICHTIG:'
Write-Host 'Pruefe dump.txt vor dem Weitergeben auf Zugangsdaten, Tokens und persoenliche Daten.'