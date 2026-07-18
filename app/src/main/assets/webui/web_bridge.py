#!/usr/bin/env python3
"""
POCKET AGENT - Web Bridge Mode
تشغيل محرك الوكيل وجلب الرد النظيف لواجهة الويب دون تسرب السجلات
"""
import sys
import os
import io

# ضمان أن مجلد المشروع في المسار
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

def run_web_mode(prompt, translate_flag='n', file_path=None):
    try:
        from systems.router import Router
        from core.translator import detect_language, translate_text
        
        router = Router()
        
        # دمج مسار الملف مع السؤال إذا وجد
        if file_path and file_path.strip():
            full_prompt = f"{prompt}\n[Attached File: {file_path}]"
        else:
            full_prompt = prompt
            
        # كتم المخرجات المؤقتة (stdout) لمنع trace_step من الظهور في الشات
        old_stdout = sys.stdout
        sys.stdout = io.StringIO() 
        
        response = router.route(full_prompt, None)
        
        # استعادة المخرجات الطبيعية
        sys.stdout = old_stdout
        
        if not response:
            print("Agent returned an empty response.")
            return

        # التعامل مع الترجمة
        if translate_flag == 'y' and detect_language(response) == 'en':
            translated = translate_text(response, 'ar')
            print(f"{response}\n\n🌐 الترجمة:\n{translated}")
        else:
            # طباعة الرد النظيف فقط
            print(response)

    except Exception as e:
        # في حال حدوث خطأ، تأكد من استعادة stdout قبل طباعة الخطأ
        sys.stdout = old_stdout
        print(f"Error in Web Bridge: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 web_bridge.py \"prompt\" [y/n] [file_path]")
        sys.exit(1)
    
    user_prompt = sys.argv[1]
    do_translate = sys.argv[2] if len(sys.argv) > 2 else 'n'
    attached_file = sys.argv[3] if len(sys.argv) > 3 else None
    
    run_web_mode(user_prompt, do_translate, attached_file)
