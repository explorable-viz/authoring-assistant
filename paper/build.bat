@echo off
setlocal enabledelayedexpansion

set "PDFLATEX=pdflatex -file-line-error -halt-on-error"
set "TARGET=%1"
if "%TARGET%"=="" set "TARGET=paper"

%PDFLATEX% %TARGET%
bibtex %TARGET%
%PDFLATEX% %TARGET%
%PDFLATEX% %TARGET%

del /F /Q %TARGET%.aux %TARGET%.dvi %TARGET%.log %TARGET%.bbl %TARGET%.blg %TARGET%.out %TARGET%.pag %TARGET%.cb %TARGET%.cb2 %TARGET%.toc

endlocal
