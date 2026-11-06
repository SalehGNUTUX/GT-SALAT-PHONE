#!/usr/bin/env bash
# يوضّب صور «خلفيات/» إلى لوحاتٍ موحّدة 720×1600 (WebP) مع مصغّراتٍ في assets/backgrounds/.
# تبقى الصورة كاملةً حادّةً بعرض اللوحة، ويُملأ الفراغ بامتدادٍ ضبابيٍّ منها بحافّةٍ متدرّجة.
# الاستعمال: tools/prepare_backgrounds.sh   (من جذر المشروع؛ يتطلّب ImageMagick)
set -euo pipefail
SRC="خلفيات"; OUT="app/src/main/assets/backgrounds"; W=720; H=1600; FEATHER=170
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT/thumb" "$OUT/blur"

# المعرّف | الملفّ | المسافة تحت الصورة بالبكسل (الباقي فوقها) | بكسلاتٌ تُقَصّ من أسفل المصدر (اختياريّ)
while IFS='|' read -r id file below chop; do
  convert "$SRC/$file" -gravity south -chop "0x${chop:-0}" +repage -gravity northwest "PNG24:$TMP/src.png"
  in="$TMP/src.png"
  convert "$in" -resize "${W}x" -unsharp 0x0.8+0.5+0.02 "$TMP/fg.png"
  h=$(identify -format '%h' "$TMP/fg.png")
  if (( h >= H )); then
    # أطول من اللوحة: قصٌّ من الأسفل (أرضيّةٌ تغطّيها بطاقات الرئيسيّة)
    convert "$TMP/fg.png" -gravity north -crop "${W}x${H}+0+0" +repage "$TMP/out.png"
  else
    y=$(( H - h - below ))
    # كلّ صورةٍ تُملأ من جهتها الناقصة فقط.
    # الامتداد من شريط الحافّة المجاورة فقط (لا من الصورة كلّها) فلا تظهر أشباح قمرٍ أو شمس.
    strip=$(( h / 14 ))
    convert -size "${W}x${H}" xc:black -colorspace sRGB -type TrueColor "PNG24:$TMP/bg.png"
    if (( y > 0 )); then
      convert "$TMP/fg.png" -crop "${W}x${strip}+0+0" +repage -flip -resize 24x24! -blur 0x3 -resize "${W}x$(( y + FEATHER ))!" "PNG24:$TMP/top.png"
      convert "$TMP/bg.png" "$TMP/top.png" -geometry +0+0 -compose over -composite "PNG24:$TMP/bg.png"
    fi
    if (( below > 0 )); then
      convert "$TMP/fg.png" -gravity south -crop "${W}x${strip}+0+0" +repage -flip -resize 24x24! -blur 0x3 -resize "${W}x$(( below + FEATHER ))!" "PNG24:$TMP/bot.png"
      convert "$TMP/bg.png" "$TMP/bot.png" -gravity south -geometry +0+0 -compose over -composite -gravity northwest "PNG24:$TMP/bg.png"
    fi
    # قناعٌ معتمٌ في الوسط يتلاشى عند الحافّتين العليا والسفلى
    convert -size "${W}x${h}" xc:white "$TMP/mask.png"
    (( y > 0 )) && convert "$TMP/mask.png" \( -size "${W}x${FEATHER}" gradient:black-white \) -gravity north -compose multiply -composite "$TMP/mask.png"
    (( below > 0 )) && convert "$TMP/mask.png" \( -size "${W}x${FEATHER}" gradient:white-black \) -gravity south -compose multiply -composite "$TMP/mask.png"
    convert "$TMP/fg.png" "$TMP/mask.png" -alpha off -compose CopyOpacity -composite "$TMP/fgm.png"
    convert "$TMP/bg.png" "$TMP/fgm.png" -geometry "+0+$y" -compose over -composite "PNG24:$TMP/out.png"
  fi
  convert "$TMP/out.png" -strip -define webp:method=6 -quality 80 "$OUT/$id.webp"
  convert "$TMP/out.png" -strip -resize 216x480 -define webp:method=6 -quality 72 "$OUT/thumb/$id.webp"
  # نسخةٌ مموَّهةٌ محضَّرة: تُرسم خلف البطاقات الزجاجيّة بلا كلفة تمويهٍ وقت التشغيل.
  convert "$TMP/out.png" -strip -resize 240x533 -blur 0x11 -define webp:method=6 -quality 74 "$OUT/blur/$id.webp"
done <<'LIST'
haram|المسجد الحرام.jpeg|0
nabawi|المسجد النبوي.jpeg|0
aqsa|المسجد الأقصى.jpeg|0
hassan2_sea|مسجد الحسن الثاني 2.jpeg|0
hassan2_sunset|مسجد الحسن الثاني.jpeg|120
hassan_day|مسجد حسان.jpeg|0
hassan_night|مسجد حسان 2.jpeg|120
qarawiyyin|مسجد القرويين.jpeg|150|72
LIST
