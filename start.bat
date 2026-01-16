@echo off
set /p version=Input version:
call build.bat
if not defined version (
java  -jar ./MWFPackager.jar ./build/libs/modularwarfare-shining-snapshot.jar mchhui/hebridge/
)else (
java  -jar ./MWFPackager.jar ./build/libs/modularwarfare-shining-snapshot.jar mchhui/hebridge/ %version%
)