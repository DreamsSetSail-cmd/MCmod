# 通用推送重试：网络恢复后把 main 与所有本地标签推上去。
# 这个仓库所在网络会间歇性阻断 github.com（api.github.com 正常），
# 因此把「等网络 + 重试」做成一个可反复调用的脚本，而不是每次手写循环。
#
# 用法：pwsh -File tools/push-when-online.ps1 [-MaxAttempts 120]
param(
    [int]$MaxAttempts = 120,
    [int]$IntervalSeconds = 20
)

$ErrorActionPreference = 'Continue'
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("Path", "User")

function Test-GitHubUp {
    # github.com 与 api.github.com 是两个不同的可用性来源：
    # 这个网络会只挡前者，因此必须探测真正需要的那一个。
    try {
        $null = Invoke-WebRequest -Uri "https://github.com/favicon.ico" -Method Head `
            -TimeoutSec 12 -UseBasicParsing -ErrorAction Stop
        return $true
    } catch {
        # 404/403 也说明连上了
        return $_.Exception.Message -match '404|403'
    }
}

for ($i = 1; $i -le $MaxAttempts; $i++) {
    if (-not (Test-GitHubUp)) {
        Start-Sleep -Seconds $IntervalSeconds
        continue
    }

    Write-Host "[$i] github.com 可用，推送 main"
    git push origin main 2>&1 | Select-Object -Last 2
    if ($LASTEXITCODE -ne 0) {
        Start-Sleep -Seconds $IntervalSeconds
        continue
    }

    # main 推上去了；把所有还没推的标签也推上去
    $local = git tag
    $remote = (git ls-remote --tags origin 2>$null) -join "`n"
    foreach ($tag in $local) {
        if ($remote -notmatch [regex]::Escape("refs/tags/$tag")) {
            Write-Host "[$i] 推送标签 $tag"
            git push origin $tag 2>&1 | Select-Object -Last 2
        }
    }

    Write-Host "[$i] 完成"
    exit 0
}

Write-Host "重试 $MaxAttempts 次后仍不可用"
exit 1
