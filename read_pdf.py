#!/usr/bin/env python3
"""
PDF 文档阅读工具
用于读取和解析 Detailed_Requirements.pdf
"""

from pypdf import PdfReader
import sys

def read_pdf(file_path):
    """读取 PDF 文件并返回文本内容"""
    try:
        reader = PdfReader(file_path)
        print(f"PDF 文件包含 {len(reader.pages)} 页\n")
        print("=" * 80)
        
        full_text = ""
        for page_num, page in enumerate(reader.pages, 1):
            text = page.extract_text()
            print(f"\n--- 第 {page_num} 页 ---")
            print(text)
            full_text += f"\n--- 第 {page_num} 页 ---\n{text}\n"
        
        print("\n" + "=" * 80)
        return full_text
    except Exception as e:
        print(f"读取 PDF 时出错: {e}")
        return None

if __name__ == "__main__":
    pdf_path = "/Users/wangchi/Documents/trae_projects/boiler-alpha/Detailed_Requirements.pdf"
    print(f"正在读取: {pdf_path}\n")
    content = read_pdf(pdf_path)
