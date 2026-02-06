
# Скрипт PowerShell для переименования всех файлов с любым расширением в .py во всех подпапках
# Переименовывает file.txt -> file.py, script.bak.py -> script.py и т.д.

$root = $PSScriptRoot

Get-ChildItem -Path $root -Recurse -File | ForEach-Object {
    if ($_.Extension -ne ".py") {
        $newName = [System.IO.Path]::GetFileNameWithoutExtension($_.Name) + ".py"
        Rename-Item -Path $_.FullName -NewName $newName
    }
}
