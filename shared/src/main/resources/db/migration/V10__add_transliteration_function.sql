-- Узбекская транслитерация: кириллица → латиница.
-- Используется для поиска "Ахрор Таиров" == "Ahror Tairov".
CREATE OR REPLACE FUNCTION uz_translit(s TEXT) RETURNS TEXT AS $$
DECLARE
  result TEXT := lower(s);
BEGIN
  -- Двухбуквенные диграфы — сначала
  result := replace(result, 'ш', 'sh');
  result := replace(result, 'ч', 'ch');
  result := replace(result, 'ж', 'j');  -- жж -> j (фонетически ж=j в узб.)
  result := replace(result, 'ъ', ''''); -- твёрдый знак
  result := replace(result, 'ь', '');   -- мягкий знак убираем

  -- Основные буквы (регистр уже нижний)
  result := replace(result, 'а', 'a');
  result := replace(result, 'б', 'b');
  result := replace(result, 'в', 'v');
  result := replace(result, 'г', 'g');
  result := replace(result, 'д', 'd');
  result := replace(result, 'е', 'e');
  result := replace(result, 'ё', 'yo');
  result := replace(result, 'з', 'z');
  result := replace(result, 'и', 'i');
  result := replace(result, 'й', 'y');
  result := replace(result, 'к', 'k');
  result := replace(result, 'л', 'l');
  result := replace(result, 'м', 'm');
  result := replace(result, 'н', 'n');
  result := replace(result, 'о', 'o');
  result := replace(result, 'п', 'p');
  result := replace(result, 'р', 'r');
  result := replace(result, 'с', 's');
  result := replace(result, 'т', 't');
  result := replace(result, 'у', 'u');
  result := replace(result, 'ф', 'f');
  result := replace(result, 'х', 'x');
  result := replace(result, 'ц', 'ts');
  result := replace(result, 'щ', 'sh');
  result := replace(result, 'ы', 'i');
  result := replace(result, 'э', 'e');
  result := replace(result, 'ю', 'yu');
  result := replace(result, 'я', 'ya');
  -- Специфичные узбекские буквы кириллицей
  result := replace(result, 'ғ', 'g''');
  result := replace(result, 'қ', 'q');
  result := replace(result, 'ҳ', 'h');
  result := replace(result, 'ӯ', 'o''');
  result := replace(result, 'ў', 'o''');

  RETURN result;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
