#!/bin/bash
set -e

PDFLATEX="pdflatex -file-line-error -halt-on-error"
TARGET=${1:-paper}

$PDFLATEX $TARGET
bibtex $TARGET
$PDFLATEX $TARGET
$PDFLATEX $TARGET
rm -f $TARGET.{aux,bbl,blg,,cb,cb2,dvi,logout,pag,toc}
