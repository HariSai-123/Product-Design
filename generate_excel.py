import zipfile
import xml.etree.ElementTree as ET
import os
import csv

def create_xlsx(filename, headers, rows):
    # We can write an XML-based Excel Spreadsheet or native XLSX zip structure
    # A standard XML Spreadsheet 2003 format (.xml or .xlsx-compatible) or standard openpyxl-like zip
    # Let's create both a CSV file and a fully structured XML Excel file + Zip XLSX file!
    
    # First, CSV:
    csv_file = filename.replace('.xlsx', '.csv')
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        writer.writerows(rows)
    print(f"Successfully generated {csv_file}")

    # Now XML Spreadsheet 2003 (.xls / .xlsx readable by Excel)
    xml_file = filename.replace('.xlsx', '_Spreadsheet.xml')
    xml_content = ['<?xml version="1.0"?>',
                   '<?mso-application progid="Excel.Sheet"?>',
                   '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"',
                   ' xmlns:o="urn:schemas-microsoft-com:office:office"',
                   ' xmlns:x="urn:schemas-microsoft-com:office:excel"',
                   ' xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"',
                   ' xmlns:html="http://www.w3.org/TR/REC-html40">',
                   ' <Styles>',
                   '  <Style ss:ID="Header">',
                   '   <Font ss:Bold="1" ss:Color="#FFFFFF"/>',
                   '   <Interior ss:Color="#0061A4" ss:Pattern="Solid"/>',
                   '   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>',
                   '  </Style>',
                   '  <Style ss:ID="Pass">',
                   '   <Interior ss:Color="#E8F8F0" ss:Pattern="Solid"/>',
                   '   <Font ss:Color="#007A44" ss:Bold="1"/>',
                   '  </Style>',
                   ' </Styles>',
                   ' <Worksheet ss:Name="Appium Test Cases">',
                   '  <Table>']
    
    # Add Header Row
    xml_content.append('   <Row>')
    for h in headers:
        xml_content.append(f'    <Cell ss:StyleID="Header"><Data ss:Type="String">{h}</Data></Cell>')
    xml_content.append('   </Row>')

    # Add Rows
    for row in rows:
        xml_content.append('   <Row>')
        for idx, val in enumerate(row):
            str_val = str(val).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
            style = ' ss:StyleID="Pass"' if str_val == 'PASS' else ''
            xml_content.append(f'    <Cell{style}><Data ss:Type="String">{str_val}</Data></Cell>')
        xml_content.append('   </Row>')

    xml_content.extend(['  </Table>', ' </Worksheet>', '</Workbook>'])
    
    with open(xml_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(xml_content))
    print(f"Successfully generated {xml_file}")

    # Create native Zip .xlsx structure
    # xlsx is a zip file containing [Content_Types].xml, _rels/.rels, xl/workbook.xml, xl/worksheets/sheet1.xml, xl/styles.xml, xl/sharedStrings.xml
    
    # 1. Collect unique strings for sharedStrings
    shared_strings = []
    string_map = {}
    
    def get_string_id(s):
        s = str(s)
        if s not in string_map:
            string_map[s] = len(shared_strings)
            shared_strings.append(s)
        return string_map[s]

    for h in headers:
        get_string_id(h)
    for row in rows:
        for val in row:
            get_string_id(val)

    # Build xl/sharedStrings.xml
    ss_xml = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
              f'<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{len(shared_strings)}" uniqueCount="{len(shared_strings)}">']
    for s in shared_strings:
        escaped = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')
        ss_xml.append(f'<si><t>{escaped}</t></si>')
    ss_xml.append('</sst>')

    # Build xl/worksheets/sheet1.xml
    sheet_xml = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
                 '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
                 '<sheetData>']
    
    # Header row
    sheet_xml.append('<row r="1">')
    for c_idx, h in enumerate(headers):
        s_id = get_string_id(h)
        col_letter = chr(65 + c_idx) if c_idx < 26 else f"A{chr(65 + c_idx - 26)}"
        sheet_xml.append(f'<c r="{col_letter}1" t="s"><v>{s_id}</v></c>')
    sheet_xml.append('</row>')

    # Data rows
    for r_idx, row in enumerate(rows, start=2):
        sheet_xml.append(f'<row r="{r_idx}">')
        for c_idx, val in enumerate(row):
            s_id = get_string_id(val)
            col_letter = chr(65 + c_idx) if c_idx < 26 else f"A{chr(65 + c_idx - 26)}"
            sheet_xml.append(f'<c r="{col_letter}{r_idx}" t="s"><v>{s_id}</v></c>')
        sheet_xml.append('</row>')

    sheet_xml.extend(['</sheetData>', '</worksheet>'])

    # Standard XMLs for XLSX container
    content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>'''

    rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>'''

    xl_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>'''

    workbook = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Appium Test Cases" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>'''

    with zipfile.ZipFile(filename, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('[Content_Types].xml', content_types)
        z.writestr('_rels/.rels', rels)
        z.writestr('xl/_rels/workbook.xml.rels', xl_rels)
        z.writestr('xl/workbook.xml', workbook)
        z.writestr('xl/sharedStrings.xml', '\n'.join(ss_xml))
        z.writestr('xl/worksheets/sheet1.xml', '\n'.join(sheet_xml))

    print(f"Successfully generated native XLSX Excel file: {filename}")

