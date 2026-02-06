# Скрипт PowerShell для переименования всех файлов .py во всех подпапках
# Заменяет расширение .py на .bak.py (пример)
# Можно изменить Rename-Item по необходимости

$root = $PSScriptRoot

Get-ChildItem -Path $root -Recurse -Filter *.py | ForEach-Object {
    $newName = $_.Name -replace '\.py$', '.bak.py'
    Rename-Item -Path $_.FullName -NewName $newName
}
