#!/usr/bin/env python3
import sys
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

def run_web_mode(prompt, translate_flag='n', file_path=None):
    try:
        from systems.router import Router
        from core.translator import detect_language, translate_text

        router = Router()

        if file_path and file_path.strip():
            full_prompt = f"{prompt}\n[Attached File: {file_path}]"
        else:
            full_prompt = prompt

        # 1. إشارة بدء العملية (تظهر أيقونة التفكير)
        print("__STATUS_THINKING__", flush=True)

        # السماح لـ trace_step بالظهور كخطوات أولية
        response = router.route(full_prompt, None)

        # 2. إشارة بدء التوليد (تتحول أيقونة التفكير إلى آلة طابعة)
        print("__STATUS_GENERATING__", flush=True)

        # 3. إرسال الرد النهائي
        print(f"__FINAL_RESPONSE__|||{response}", flush=True)

        # 4. الترجمة إذا لزم الأمر
        if translate_flag == 'y' and response and detect_language(response) == 'en':
            translated = translate_text(response, 'ar')
            print(f"__TRANSLATION__|||{translated}", flush=True)

    except Exception as e:
        print(f"__STREAM_ERROR__|||{e}", flush=True)


def run_translate_mode(text, target_lang='ar'):
    try:
        from core.translator import translate_text
        translated = translate_text(text, target_lang)
        print(f"__TRANSLATION__|||{translated}", flush=True)
    except Exception as e:
        print(f"__STREAM_ERROR__|||{e}", flush=True)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 web_bridge.py \"prompt\" [y/n] [file_path]")
        sys.exit(1)

    if sys.argv[1] == "--start-server":
        try:
            from systems.extractor import extractor
            extractor.start_server()
            print("Server starting...", flush=True)
        except Exception as e:
            print(f"Error: {e}", flush=True)
        sys.exit(0)
    elif sys.argv[1] == "--stop-server":
        os.system("pkill -f llama-server")
        print("Server stopped.", flush=True)
        sys.exit(0)
    elif sys.argv[1] == "--translate":
        if len(sys.argv) < 3:
            print("Usage: python3 web_bridge.py --translate \"text\" [target_lang]")
            sys.exit(1)
        text_to_translate = sys.argv[2]
        target_lang = sys.argv[3] if len(sys.argv) > 3 else 'ar'
        run_translate_mode(text_to_translate, target_lang)
        sys.exit(0)

    user_prompt = sys.argv[1]
    do_translate = sys.argv[2] if len(sys.argv) > 2 else 'n'
    attached_file = sys.argv[3] if len(sys.argv) > 3 else None

    run_web_mode(user_prompt, do_translate, attached_file)
