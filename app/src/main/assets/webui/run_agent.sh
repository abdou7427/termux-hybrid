#!/data/data/com.termux/files/usr/bin/sh
# تهيئة بيئة Termux الكاملة لضمان عمل ProcessBuilder في الجافا
export HOME=/data/data/com.termux/files/home
export PATH=/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets:/system/bin:/system/xbin
export LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib
export TERMUX_APP__DATA_DIR=/data/data/com.termux
export ANDROID_DATA=/data
export TMPDIR=$HOME/tmp

mkdir -p $TMPDIR

# تشغيل جسر البايثون مع تمرير المتغيرات
exec /data/data/com.termux/files/usr/bin/python3 /data/data/com.termux/files/home/agent_project/web_bridge.py "$@"
