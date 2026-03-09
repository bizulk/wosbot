param(
    [string]$JarPattern = "wos-bot-*.jar",
    [string]$VmProcessName = "MuMuNxMain",
    [int]$TimeoutSec = 2700
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$logFile = Join-Path $PSScriptRoot "launch.log"

function Write-Log {
    param(
        [string]$Message
    )

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Add-Content -Path $logFile -Value "[$timestamp] $Message"
}

function Stop-ProcessTreeByPid {
    param(
        [int]$ProcessId
    )

    try {
        Write-Log "Stopping process tree for PID=$ProcessId"
        cmd.exe /c "taskkill /PID $ProcessId /T /F" | Out-Null
        Write-Log "Process tree stopped for PID=$ProcessId"
    }
    catch {
        Write-Log "Failed to stop process tree for PID=$ProcessId : $($_.Exception.Message)"
    }
}

function Resolve-BotJar {
    param(
        [string]$SearchRoot,
        [string]$Pattern
    )

	$matches = @(Get-ChildItem -Path $SearchRoot -Filter $Pattern -File -Recurse |
		Sort-Object LastWriteTime -Descending)

    if ($matches.Count -eq 0) {
        throw "No jar found matching pattern '$Pattern' under '$SearchRoot'"
    }

    if ($matches.Count -gt 1) {
        Write-Log "Multiple jar files matched pattern '$Pattern'. Selecting the most recently modified one: $($matches[0].FullName)"
    }
    else {
        Write-Log "Single jar matched pattern '$Pattern': $($matches[0].FullName)"
    }

    return $matches[0]
}

Write-Log "============================================================"
Write-Log "Script started"
Write-Log "Parameters: JarPattern='$JarPattern', VmProcessName='$VmProcessName', TimeoutSec=$TimeoutSec"

$proc = $null

try {
    # Resolve the bot jar dynamically using a wildcard pattern.
    # If multiple files match, the most recently modified one is selected.
    $jarFile = Resolve-BotJar -SearchRoot $PSScriptRoot -Pattern $JarPattern
    $jarPath = $jarFile.FullName
    $jarName = $jarFile.Name

    Write-Log "Resolved bot jar: $jarPath"

    # Prevent instance stacking by stopping previous Java processes
    # that were launched with the same jar file name.
    $oldJavaProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" |
        Where-Object { $_.CommandLine -like "*$jarName*" }

    if ($oldJavaProcesses) {
        foreach ($oldProcess in $oldJavaProcesses) {
            Write-Log "Found leftover bot Java process PID=$($oldProcess.ProcessId), CommandLine=$($oldProcess.CommandLine)"
            Stop-ProcessTreeByPid -ProcessId $oldProcess.ProcessId
        }
    }
    else {
        Write-Log "No leftover bot Java process found for jar '$jarName'"
    }

    # Start the Java bot and keep its PID for later cleanup.
    Write-Log "Starting bot: java -Dwosbot.autostart=true -jar `"$jarPath`""
    $proc = Start-Process -FilePath "java.exe" `
        -ArgumentList @("-Dwosbot.autostart=true", "-jar", $jarPath) `
        -PassThru

    Write-Log "Bot started with PID=$($proc.Id)"

    try {
        # Wait until the bot exits naturally, or stop waiting after the configured timeout.
        Write-Log "Waiting up to $TimeoutSec seconds for bot PID=$($proc.Id)"
        $null = Wait-Process -Id $proc.Id -Timeout $TimeoutSec
        Write-Log "Bot PID=$($proc.Id) exited before timeout"
    }
    catch {
        Write-Log "Timeout reached or wait interrupted for bot PID=$($proc.Id)"
    }
    finally {
        # Always terminate the full bot process tree to avoid leftovers.
        if ($null -ne $proc) {
            Stop-ProcessTreeByPid -ProcessId $proc.Id
        }
    }

    # Stop the emulator/VM host process if it is still running,
    # because it may keep the system awake and block sleep.
    $vmProcesses = Get-Process -Name $VmProcessName -ErrorAction SilentlyContinue

    if ($vmProcesses) {
        foreach ($vmProcess in $vmProcesses) {
            Write-Log "Found VM/emulator process '$VmProcessName' with PID=$($vmProcess.Id), attempting to stop it"
            Stop-ProcessTreeByPid -ProcessId $vmProcess.Id
        }
    }
    else {
        Write-Log "No VM/emulator process found with name '$VmProcessName'"
    }

    Write-Log "Script completed successfully"
}
catch {
    Write-Log "Unhandled error: $($_.Exception.Message)"
    throw
}
finally {
    Write-Log "Script ended"
}
