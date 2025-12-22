@echo off
echo Stopping all  services 

for /f "tokens=5" %%p in ('netstat -ano ^| findstr LISTENING') do (
    for /f "tokens=2" %%j in ('tasklist /FI "PID eq %%p" ^| findstr java.exe') do (
        echo Killing PID %%p
        taskkill /PID %%p /F
    )
)

echo Done. All are stopped
pause
