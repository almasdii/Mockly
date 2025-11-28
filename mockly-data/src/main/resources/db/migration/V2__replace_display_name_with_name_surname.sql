-- V2__replace_display_name_with_name_surname.sql
-- Замена display_name на name и surname

-- Добавляем новые колонки
ALTER TABLE profiles 
    ADD COLUMN name VARCHAR(50),
    ADD COLUMN surname VARCHAR(50);

-- Миграция данных: если display_name был заполнен, пытаемся разделить на name и surname
-- Простая логика: берем первое слово как name, остальное как surname
UPDATE profiles 
SET 
    name = CASE 
        WHEN display_name IS NOT NULL AND display_name != '' THEN
            SPLIT_PART(TRIM(display_name), ' ', 1)
        ELSE NULL
    END,
    surname = CASE 
        WHEN display_name IS NOT NULL AND display_name != '' AND POSITION(' ' IN TRIM(display_name)) > 0 THEN
            SUBSTRING(TRIM(display_name) FROM POSITION(' ' IN TRIM(display_name)) + 1)
        ELSE NULL
    END
WHERE display_name IS NOT NULL AND display_name != '';

-- Удаляем старую колонку
ALTER TABLE profiles DROP COLUMN display_name;

