-- Pruebas de las restricciones CHECK de catalogo.producto.
-- Cada INSERT debe FALLAR con la restricción indicada; ninguno modifica la tabla.
-- Todas usan una categoría válida para que el error corresponda solo a la regla probada.

-- chk_codigo_normalizado (código en minúsculas)
insert into catalogo.producto (codigo, nombre, categoria, precio, stock)
values ('x001', 'Prueba', 'Papeleria', 10.00, 1);

-- chk_nombre_longitud (nombre vacío)
insert into catalogo.producto (codigo, nombre, categoria, precio, stock)
values ('X002', '', 'Papeleria', 10.00, 1);

-- chk_categoria (categoría fuera de la lista)
insert into catalogo.producto (codigo, nombre, categoria, precio, stock)
values ('X003', 'Prueba', 'Muebles', 10.00, 1);

-- chk_precio (precio en cero)
insert into catalogo.producto (codigo, nombre, categoria, precio, stock)
values ('X004', 'Prueba', 'Papeleria', 0, 1);

-- chk_stock (stock negativo)
insert into catalogo.producto (codigo, nombre, categoria, precio, stock)
values ('X005', 'Prueba', 'Papeleria', 10.00, -1);
