$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'
$jar = Join-Path $backendDir 'target\chemexpress-crm-0.2.0.jar'

if (-not (Test-Path $jar)) {
    Write-Host '未找到后端 jar，正在编译打包（首次约需 1-2 分钟）...' -ForegroundColor Yellow
    Push-Location $backendDir
    mvn -q -DskipTests package
    Pop-Location
}

if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    Write-Host '正在安装前端依赖（首次约需 1-2 分钟）...' -ForegroundColor Yellow
    Push-Location $frontendDir
    npm install --no-audit --no-fund
    Pop-Location
}

Write-Host '启动后端 http://localhost:8080 ...' -ForegroundColor Cyan
$backend = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar) -WorkingDirectory $root -PassThru
Write-Host "后端进程 PID: $($backend.Id)"

Write-Host '启动前端 http://localhost:5173 ...' -ForegroundColor Cyan
$frontend = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', 'npm run dev') -WorkingDirectory $frontendDir -PassThru
Write-Host "前端进程 PID: $($frontend.Id)"

Write-Host '等待服务就绪...' -ForegroundColor Yellow
Start-Sleep -Seconds 10
Start-Process 'http://localhost:5173'

Write-Host ''
Write-Host '皓元医药营销 CRM 已启动' -ForegroundColor Green
Write-Host '  前端: http://localhost:5173'
Write-Host '  后端: http://localhost:8080'
Write-Host '  演示账号: admin / dsm.east / mr.chen / ops.zhou'
Write-Host '  统一密码: Chem@123'
