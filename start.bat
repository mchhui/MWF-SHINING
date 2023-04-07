@echo off
set /p version=Input version:
call build.bat
if not defined version (
java  -jar ./MWFPackager.jar ./build/libs/modularwarfare-shining-snapshot.jar mchhui/modularmovements/
)else (
java  -jar ./MWFPackager.jar ./build/libs/modularwarfare-shining-snapshot.jar mchhui/modularmovements/ %version%
)