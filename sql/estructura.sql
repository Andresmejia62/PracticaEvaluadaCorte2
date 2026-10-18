-- Estructura de la base papeleria_campus: esquema catalogo y tabla producto con sus restricciones.
-- Ejecutar conectado a la base papeleria_campus (ver README, sección "Creación de la base de datos").
-- ATENCIÓN: si la tabla ya existe se elimina junto con sus datos y se vuelve a crear vacía.

CREATE SCHEMA IF NOT EXISTS catalogo;

DROP TABLE IF EXISTS catalogo.producto;

CREATE TABLE catalogo.producto (
    id_producto serial primary key,
    codigo      varchar(10)   not null unique,
    nombre      varchar(80)   not null,
    categoria   varchar(20)   not null,
    precio      numeric(10,2) not null,
    stock       integer       not null,

    constraint chk_codigo_normalizado check (codigo = upper(trim(codigo)) and char_length(codigo) > 0),
    constraint chk_nombre_longitud check (nombre = trim(nombre) and char_length(nombre) between 1 and 80),
    constraint chk_precio check (precio > 0),
    constraint chk_stock check (stock > 0),
    constraint chk_categoria check (categoria in ('Papeleria', 'Tecnologia', 'Accesorios'))
);
