#!/usr/bin/env python3
"""
PDF 文档阅读工具 - 完整版
将 PDF 内容保存到文本文件
"""

from pypdf import PdfReader
import sys

def read_and_save_pdf(file_path, output_path):
    """读取 PDF 文件并保存到文本文件"""
    try:
        reader = PdfReader(file_path)
        print(f"PDF 文件包含 {len(reader.pages)} 页")
        
        full_text = f"=== {file_path} ===\n"
        full_text += f"总页数: {len(reader.pages)}\n\n"
        
        for page_num, page in enumerate(reader.pages, 1):
            text = page.extract_text()
            full_text += f"\n{'='*80}\n"
            full_text += f"第 {page_num} 页\n"
            full_text += f"{'='*80}\n"
            full_text += text + "\n"
        
        # 保存到文件
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(full_text)
        
        print(f"\n✅ PDF 内容已保存到: {output_path}")
        return full_text
    except Exception as e:
        print(f"读取 PDF 时出错: {e}")
        return None

if __name__ == "__main__":
    pdf_path = "/Users/wangchi/Documents/trae_projects/boiler-alpha/Detailed_Requirements.pdf"
    output_path = "/Users/wangchi/Documents/trae_projects/boiler-alpha/Detailed_Requirements.txt"
    content = read_and_save_pdf(pdf_path, output_path)
