#!/usr/bin/env bash
set -euo pipefail

# Script to render PlantUML diagrams and assemble a PDF using pandoc.
# Usage: run from this docs/ folder: ./generate_pdf.sh

DIR=$(cd "$(dirname "$0")" && pwd)
cd "$DIR"

PLANTUML_JAR=plantuml.jar

# Ensure we have plantuml jar
if ! command -v plantuml >/dev/null 2>&1; then
  if [ ! -f "$PLANTUML_JAR" ]; then
    echo "plantuml not found. Downloading plantuml.jar..."
    curl -L -o "$PLANTUML_JAR" https://plantuml.com/plantuml.jar
  else
    echo "Using existing $PLANTUML_JAR"
  fi
  PLANTUML_CMD="java -jar $PLANTUML_JAR"
else
  echo "Found plantuml command on PATH"
  PLANTUML_CMD=plantuml
fi

# Render diagrams to PNG
echo "Rendering PlantUML diagrams to PNG..."
$PLANTUML_CMD -tpng login_service.puml
$PLANTUML_CMD -tpng transaction_service.puml
$PLANTUML_CMD -tpng sequence_diagram.puml

echo "Diagrams rendered: login_service.png, transaction_service.png, sequence_diagram.png"

# Create combined markdown that embeds the diagrams and includes both languages
COMBINED=combined_documentation.md
cat > "$COMBINED" <<'MD'
# Documentação Técnica

## Diagramas

### LoginService - Diagrama de Classes
![](login_service.png)

### TransactionService - Diagrama de Classes
![](transaction_service.png)

### Diagrama de Sequência
![](sequence_diagram.png)

---

## Documentação (PT-BR)

`explanation_pt.md`

---

## Documentation (EN)

`explanation_en.md`
MD

# Append the actual content files to the combined markdown so pandoc embeds text

cat explanation_pt.md >> "$COMBINED"
cat explanation_en.md >> "$COMBINED"

# Try to build PDF with pandoc
if command -v pandoc >/dev/null 2>&1; then
  echo "pandoc found — generating PDF (output: documentation.pdf)"
  pandoc "$COMBINED" -o documentation.pdf --pdf-engine=xelatex || {
    echo "pandoc failed to produce PDF; please ensure a LaTeX engine is installed (e.g., BasicTeX or MacTeX)"
    exit 1
  }
  echo "PDF gerado: $DIR/documentation.pdf"
else
  echo "pandoc not found. Install pandoc (and a LaTeX engine) to generate PDF."
  echo "On macOS: brew install pandoc && brew install --cask mactex (or BasicTeX)"
  echo "The combined markdown with images is at: $DIR/$COMBINED"
fi

echo "Done."