#!/usr/bin/env pwsh
# T22 简易压测脚本（wrk/ab 不可用时的降级方案）
# 对 4 个核心读接口各发 100 请求，记录 P95/P99/QPS
param(
    [string]$Base = "http://localhost:8080",
    [int]$Requests = 100,
    [int]$Concurrency = 10
)

$ErrorActionPreference = "SilentlyContinue"
$results = @{}

$endpoints = @(
    @{ Name = "/api/v1/shares";       Url = "$Base/api/v1/shares?current=1&size=9" },
    @{ Name = "/api/v1/posts";        Url = "$Base/api/v1/posts?current=1&size=10" },
    @{ Name = "/api/v1/hot";          Url = "$Base/api/v1/hot?size=10" },
    @{ Name = "/api/v1/shares/1";     Url = "$Base/api/v1/shares/1" }
)

foreach ($ep in $endpoints) {
    $times = [System.Collections.ArrayList]::new()
    $jobs = @()
    $startTime = [DateTime]::Now

    for ($i = 1; $i -le $Requests; $i++) {
        $jobs += [System.Threading.Tasks.Task]::Run({
            param($url)
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            try { $null = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10 } catch {}
            $sw.Stop()
            return $sw.ElapsedMilliseconds
        }, $ep.Url)
    }
    [System.Threading.Tasks.Task]::WaitAll($jobs.ToArray())
    $elapsed = (([DateTime]::Now - $startTime).TotalSeconds)

    foreach ($job in $jobs) { $times.Add($job.Result) | Out-Null }
    $sorted = $times | Sort-Object
    $p50 = $sorted[[Math]::Floor($sorted.Count * 0.5)]
    $p95 = $sorted[[Math]::Floor($sorted.Count * 0.95)]
    $p99 = $sorted[[Math]::Floor($sorted.Count * 0.99)]
    $qps = [Math]::Round($Requests / $elapsed, 1)

    $results[$ep.Name] = @{ QPS = $qps; P50 = $p50; P95 = $p95; P99 = $p99; Elapsed = [Math]::Round($elapsed, 2) }

    Write-Host "$($ep.Name): QPS=$qps | P50=${p50}ms | P95=${p95}ms | P99=${p99}ms | Total=${([Math]::Round($elapsed,2))}s"
}

Write-Host "`n--- Slow SQL Check ---"
$slowLog = ssh root@172.25.41.137 "docker exec community-mysql cat /var/lib/mysql/*-slow.log 2>/dev/null | grep -c 'Query_time' || echo 0"
Write-Host "Slow queries (>=0.2s): $slowLog"
