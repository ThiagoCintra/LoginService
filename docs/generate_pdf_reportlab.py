#!/usr/bin/env python3
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.lib import utils
import os

DOC = "documentation_reportlab.pdf"

styles = getSampleStyleSheet()
if 'Heading1Large' not in styles:
    styles.add(ParagraphStyle(name='Heading1Large', parent=styles['Heading1'], fontSize=18, spaceAfter=6))
if 'Code' not in styles:
    styles.add(ParagraphStyle(name='Code', fontName='Courier', fontSize=8))

def fit_image(path, max_width_mm=170):
    img = utils.ImageReader(path)
    iw, ih = img.getSize()
    maxw = max_width_mm * mm
    if iw > maxw:
        ratio = maxw / iw
        return Image(path, width=iw*ratio, height=ih*ratio)
    return Image(path)

def md_to_paragraphs(md_text):
    # Very simple markdown to paragraphs: handle headings (#) and code fences ```
    lines = md_text.splitlines()
    paras = []
    buf = []
    in_code = False
    code_buf = []
    for line in lines:
        if line.strip().startswith('```'):
            if in_code:
                # flush code block
                code_text = '\n'.join(code_buf)
                paras.append(('code', code_text))
                code_buf = []
                in_code = False
            else:
                in_code = True
            continue
        if in_code:
            code_buf.append(line)
            continue
        if line.strip() == '':
            if buf:
                paras.append(('p','\n'.join(buf)))
                buf = []
            continue
        if line.startswith('#'):
            # heading
            if buf:
                paras.append(('p','\n'.join(buf)))
                buf = []
            level = len(line) - len(line.lstrip('#'))
            text = line.lstrip('#').strip()
            paras.append((f'h{level}', text))
            continue
        # normal line
        buf.append(line)
    if in_code and code_buf:
        paras.append(('code','\n'.join(code_buf)))
    if buf:
        paras.append(('p','\n'.join(buf)))
    return paras


def build_pdf():
    doc = SimpleDocTemplate(DOC, pagesize=A4, rightMargin=20*mm, leftMargin=20*mm, topMargin=20*mm, bottomMargin=20*mm)
    story = []

    # Title
    story.append(Paragraph('Documentação Técnica — LoginService & TransactionService', styles['Heading1Large']))
    story.append(Spacer(1, 6))

    # Insert diagrams if present
    for img_name, title in [('login_service.png', 'LoginService - Diagrama de Classes'), ('transaction_service.png', 'TransactionService - Diagrama de Classes'), ('sequence_diagram.png', 'Diagrama de Sequência')]:
        img_path = os.path.join(os.path.dirname(__file__), img_name)
        if os.path.exists(img_path):
            story.append(Paragraph(title, styles['Heading2']))
            story.append(Spacer(1,4))
            story.append(fit_image(img_path))
            story.append(Spacer(1,12))

    story.append(PageBreak())

    # Add PT-BR
    pt_path = os.path.join(os.path.dirname(__file__), 'explanation_pt.md')
    en_path = os.path.join(os.path.dirname(__file__), 'explanation_en.md')

    if os.path.exists(pt_path):
        story.append(Paragraph('Documentação (PT-BR)', styles['Heading1']))
        with open(pt_path, 'r', encoding='utf-8') as f:
            md = f.read()
        paras = md_to_paragraphs(md)
        for kind, text in paras:
            if kind.startswith('h'):
                story.append(Paragraph(text, styles['Heading2']))
            elif kind == 'p':
                # replace backticks with monospace tags
                story.append(Paragraph(text.replace('`', ''), styles['Normal']))
            elif kind == 'code':
                story.append(Paragraph('<pre>%s</pre>' % text.replace('&','&amp;').replace('<','&lt;').replace('>','&gt;'), styles['Code']))
            story.append(Spacer(1,6))
    story.append(PageBreak())

    if os.path.exists(en_path):
        story.append(Paragraph('Documentation (EN)', styles['Heading1']))
        with open(en_path, 'r', encoding='utf-8') as f:
            md = f.read()
        paras = md_to_paragraphs(md)
        for kind, text in paras:
            if kind.startswith('h'):
                story.append(Paragraph(text, styles['Heading2']))
            elif kind == 'p':
                story.append(Paragraph(text.replace('`', ''), styles['Normal']))
            elif kind == 'code':
                story.append(Paragraph('<pre>%s</pre>' % text.replace('&','&amp;').replace('<','&lt;').replace('>','&gt;'), styles['Code']))
            story.append(Spacer(1,6))

    doc.build(story)
    print('PDF gerado:', DOC)

if __name__ == '__main__':
    build_pdf()
