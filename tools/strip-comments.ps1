<#
.SYNOPSIS
  Удаляет комментарии из Java-файлов (строчные // и блочные /* */).
.DESCRIPTION
  Проходит по .java файлам, вырезает комментарии, сохраняя строки в строковых
  и char литералах, text blocks (""") и количество строк (пустые строки вместо
  удалённых блочных, чтобы номера строк не ехали... кроме схлопнутых).
  По умолчанию только показывает статистику (DryRun). Для записи: -Apply.
  Бэкап: -Backup (копирует файл в .bak).
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/strip-comments.ps1
  powershell -ExecutionPolicy Bypass -File tools/strip-comments.ps1 -Apply
  powershell -ExecutionPolicy Bypass -File tools/strip-comments.ps1 -Apply -Backup -Path src/client/java/exp/nefor/client/gui
#>
param(
    [string]$Path = "src",
    [switch]$Apply,
    [switch]$Backup
)

$ErrorActionPreference = "Stop"

function Remove-JavaComments {
    param([string]$text)

    $sb = New-Object System.Text.StringBuilder
    $i = 0
    $n = $text.Length
    $NORMAL = 0; $LINE = 1; $BLOCK = 2; $STR = 3; $CHAR = 4; $TBLOCK = 5
    $state = $NORMAL

    while ($i -lt $n) {
        $c = $text[$i]
        $next = if ($i + 1 -lt $n) { $text[$i + 1] } else { [char]0 }
        $next2 = if ($i + 2 -lt $n) { $text[$i + 2] } else { [char]0 }

        switch ($state) {
            $NORMAL {
                if ($c -eq '/' -and $next -eq '/') { $state = $LINE; $i += 2 }
                elseif ($c -eq '/' -and $next -eq '*') { $state = $BLOCK; $i += 2 }
                elseif ($c -eq '"' -and $next -eq '"' -and $next2 -eq '"') {
                    $sb.Append('"""') | Out-Null; $i += 3; $state = $TBLOCK
                }
                elseif ($c -eq '"') { $sb.Append($c) | Out-Null; $i++; $state = $STR }
                elseif ($c -eq "'") { $sb.Append($c) | Out-Null; $i++; $state = $CHAR }
                else { $sb.Append($c) | Out-Null; $i++ }
            }
            $LINE {
                if ($c -eq "`r" -or $c -eq "`n") { $state = $NORMAL }
                else { $i++ }
            }
            $BLOCK {
                if ($c -eq '*' -and $next -eq '/') { $state = $NORMAL; $i += 2 }
                else {
                    # сохраняем переносы строк, чтобы номера не ехали
                    if ($c -eq "`r" -or $c -eq "`n") { $sb.Append($c) | Out-Null }
                    $i++
                }
            }
            $STR {
                $sb.Append($c) | Out-Null
                if ($c -eq '\') { if ($i + 1 -lt $n) { $sb.Append($text[$i + 1]) | Out-Null; $i += 2 } else { $i++ } }
                elseif ($c -eq '"') { $i++; $state = $NORMAL }
                else { $i++ }
            }
            $CHAR {
                $sb.Append($c) | Out-Null
                if ($c -eq '\') { if ($i + 1 -lt $n) { $sb.Append($text[$i + 1]) | Out-Null; $i += 2 } else { $i++ } }
                elseif ($c -eq "'") { $i++; $state = $NORMAL }
                else { $i++ }
            }
            $TBLOCK {
                if ($c -eq '"' -and $next -eq '"' -and $next2 -eq '"') {
                    $sb.Append('"""') | Out-Null; $i += 3; $state = $NORMAL
                } else {
                    $sb.Append($c) | Out-Null
                    if ($c -eq '\') { if ($i + 1 -lt $n) { $sb.Append($text[$i + 1]) | Out-Null; $i += 2 } else { $i++ } }
                    else { $i++ }
                }
            }
        }
    }
    return $sb.ToString()
}

$files = Get-ChildItem -Recurse -Filter *.java -Path $Path -ErrorAction SilentlyContinue
$totalFiles = 0
$totalLines = 0
foreach ($f in $files) {
    $orig = Get-Content -Raw -LiteralPath $f.FullName
    if ($null -eq $orig) { continue }
    $clean = Remove-JavaComments $orig
    if ($clean -ceq $orig) { continue }
    $removed = ($orig.Split("`n").Count) - ($clean.Split("`n").Count)
    # считаем убранные //-хвосты построчно для статистики
    $totalFiles++
    $totalLines += $removed
    if ($Apply) {
        if ($Backup) { Copy-Item -LiteralPath $f.FullName -Destination ($f.FullName + ".bak") -Force }
        Set-Content -LiteralPath $f.FullName -Value $clean -NoNewline
        Write-Output ("cleaned: " + $f.FullName)
    }
}

if ($Apply) { Write-Output ("`nDone. Files: $totalFiles") }
else { Write-Output ("Dry run. Files with comments: $totalFiles (run with -Apply to strip)") }
