@echo off
echo ============================================
echo  微招管理后台 - 前端构建脚本
echo ============================================
echo.
cd /d C:\Users\cg\Desktop\weib\admin-frontend

echo [1/2] 安装依赖...
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo 依赖安装失败，请检查 Node.js 和 npm 是否已安装
    pause
    exit /b 1
)

echo.
echo [2/2] 编译构建...
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo 构建失败，请查看上方错误信息
    pause
    exit /b 1
)

echo.
echo ============================================
echo  构建成功！
echo  文件已输出到: src/main/resources/static/admin/
echo  请在 IDEA 中 Build -> Rebuild Project
echo  然后重新启动 WeibApplication
echo ============================================
pause
