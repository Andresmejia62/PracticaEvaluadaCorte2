-- Carga de los 12 productos iniciales de papeleria_campus.
-- También sirve para RESTAURAR la base de prueba: borra todos los productos actuales y
-- reinicia la secuencia de id_producto, de modo que los productos vuelven a tener los IDs 1 a 12.
-- Requiere haber ejecutado antes sql/estructura.sql.

BEGIN;

TRUNCATE TABLE catalogo.producto RESTART IDENTITY;

INSERT INTO catalogo.producto (codigo, nombre, categoria, precio, stock) VALUES
('P001', 'Cuaderno universitario', 'Papeleria',   65.00, 25),
('P002', 'Cuaderno cuadriculado',  'Papeleria',   72.50, 12),
('P003', 'Lapicero azul',          'Papeleria',   12.00, 80),
('P004', 'Resma de papel carta',   'Papeleria',  185.00,  1),
('T001', 'Memoria USB 32 GB',      'Tecnologia', 210.00,  8),
('T002', 'Mouse inalámbrico',      'Tecnologia', 320.00,  6),
('T003', 'Teclado USB',            'Tecnologia', 450.00,  4),
('T004', 'Cable HDMI',             'Tecnologia', 180.00,  1),
('A001', 'Mochila escolar',        'Accesorios', 850.00,  5),
('A002', 'Estuche de lápices',     'Accesorios',  95.00, 18),
('A003', 'Botella reutilizable',   'Accesorios', 240.00,  1),
('A004', 'Regla metálica',         'Accesorios',  45.00, 20);

COMMIT;
