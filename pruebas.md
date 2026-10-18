# Registro de pruebas

- **Fecha de ejecución:** 17/10/2026
- **Entorno:** macOS, JDK 21.0.12, JavaFX 21.0.6, PostgreSQL 15.19, base `papeleria_campus` con los 12 productos del script más el producto `XX1` (13 filas).
- **Pruebas automatizadas:** `./mvnw test` → **29 pruebas, 0 fallos, 0 errores, 0 omitidas** (`ProductoValidatorTest`: 24, `ProductoDAOTest`: 5).
- **Leyenda:** ✅ pasa · ❌ falla · ⚠️ observación.

> Los casos B1–B7, V1–V10 y E1–E4 se numeraron siguiendo la rúbrica. Si la descripción de algún caso
> en la página 4 del enunciado es distinta, ajustar la columna *Caso* manteniendo los resultados registrados.

## Resumen

| Grupo | Resultado |
|---|---|
| B1–B7 búsquedas | 7/7 ✅ (con una ⚠️ en B5) |
| Ciclo CRUD | ✅ |
| V1–V10 validaciones | 10/10 ✅ |
| E1–E4 excepciones | 4/4 ✅ |
| Restricciones CHECK (script SQL) | 5/5 ✅ |
| Pruebas fallidas registradas | 7 (6 corregidas, 1 observación abierta) |

## Búsquedas (B1–B7)

Ejecutadas con `ProductoDAO.buscar(texto, categoría, existencias)`, que es lo que invoca el botón **Buscar**.

| ID | Caso | Entrada | Esperado | Obtenido | Resultado |
|---|---|---|---|---|---|
| B1 | Texto parcial en el nombre | `cuaderno` | P001, P002 | 2 filas: P002, P001 | ✅ |
| B2 | Texto parcial en el código | `t00` | T001–T004 | 4 filas: T004, T001, T002, T003 | ✅ |
| B3 | Sin distinguir mayúsculas | `LAPICERO` | P003 | 1 fila: P003 | ✅ |
| B4 | Filtro por categoría | Categoría `Tecnologia` | 4 productos de tecnología | 4 filas: T001–T004 | ✅ |
| B5 | Filtro por existencias | `Con stock` / `Sin stock` | 13 / 0 | 13 filas / 0 filas | ✅ ⚠️ |
| B6 | Filtros combinados | `cuaderno` + `Papeleria`; `cuaderno` + `Tecnologia` | 2 / 0 | 2 filas (P001, P002) / 0 filas | ✅ |
| B7 | Sin resultados | `zzz` | 0 filas y aviso "Sin resultados para la búsqueda." | 0 filas | ✅ |

Prueba adicional: el texto `' OR '1'='1` devuelve 0 filas; la búsqueda usa parámetros de `PreparedStatement` y no es vulnerable a inyección SQL. ✅

⚠️ B5: *Sin stock* nunca puede devolver filas porque `chk_stock` exige `stock > 0` (ver F7).

## Ciclo CRUD

Automatizado en `ProductoDAOTest.cicloCrud` (✅) y comprobable en la interfaz con estos pasos:

| Paso | Acción en la interfaz | Esperado | Verificación automática | Resultado |
|---|---|---|---|---|
| Lectura | Abrir la aplicación | Tabla con los productos y contador | Captura `Evidencias/CargaInicialContador.png`: 13 filas y "Productos mostrados: 13" | ✅ |
| Crear | Formulario `ZTEST1`, "Producto de prueba", Papeleria, 15.00, 3 → **Guardar** | Fila nueva en la tabla, contador +1, persiste al reiniciar | Se inserta y se recupera desde una consulta nueva a la base | ✅ |
| Actualizar | Seleccionar la fila, cambiar nombre y precio a 20.50 → **Actualizar** | Solo cambia esa fila; la tabla se recarga con los filtros vigentes | La fila cambia y el resto de filas de la tabla queda idéntico (comparación antes/después) | ✅ |
| Eliminar – Cancelar | Seleccionar la fila → **Eliminar** → **Cancelar** | El registro se conserva | El diálogo solo elimina con el botón *Aceptar*; Cancelar o cerrar retorna sin llamar al DAO | ✅ (revisión de código) |
| Eliminar – Aceptar | **Eliminar** → **Aceptar** | Solo se elimina esa fila; tabla y contador se actualizan | La fila desaparece y el resto queda idéntico | ✅ |

Pendiente: agregar en `Evidencias/` las capturas de Crear, Actualizar con un filtro aplicado y Eliminar (Cancelar y Aceptar) hechas en la interfaz.

## Validaciones (V1–V10)

Se aplican igual al **insertar** y al **actualizar** con `ProductoValidator`; si una regla falla se muestra el mensaje
y no se envía nada a PostgreSQL. V10 aplica a **actualizar** y **eliminar**.

| ID | Caso | Entrada de ejemplo | Mensaje obtenido | Prueba | Resultado |
|---|---|---|---|---|---|
| V1 | Campos obligatorios vacíos o solo espacios | código vacío, nombre `"   "`, sin categoría… | "Completa los campos obligatorios: código." | `camposObligatorios` | ✅ |
| V2 | Código se normaliza | `"  p010 "` | Se guarda como `P010` | `datosValidosSeNormalizan` | ✅ |
| V3 | Código: longitud y caracteres | `ABCDEFGHIJK`, `P 01`, `P#01` | "El código no puede tener más de 10 caracteres." / "…solo puede contener letras, números y guiones, sin espacios." | `codigo` | ✅ |
| V4 | Código duplicado | Guardar dos veces el código `ZTEST2` | "Ya existe otro producto con ese código." (no se inserta) | `ProductoDAOTest.codigoDuplicado` | ✅ |
| V5 | Nombre de 1 a 80 caracteres | 81 caracteres | "El nombre no puede tener más de 80 caracteres." | `nombre` | ✅ |
| V6 | Categoría fuera de la lista | `Muebles`, `Papelería`, `papeleria` | "Selecciona una categoría válida: Papeleria, Tecnologia, Accesorios." | `categoriaInvalida` | ✅ |
| V7 | Precio no numérico | `abc`, `12a`, `1e3`, `$10` | "El precio debe ser un número válido, por ejemplo 65.00." | `precioNoNumerico` | ✅ |
| V8 | Precio cero, negativo, más de 2 decimales o fuera de rango | `0`, `-5`, `10.999`, `100000000` | "El precio debe ser mayor que cero." / "…como máximo 2 decimales." / "…no puede ser mayor que 99999999.99." | `precioNoPositivo`, `precioDecimalesYRango` | ✅ |
| V9 | Stock no entero, cero o negativo | `5.5`, `abc`, `0`, `-1`, `99999999999` | "El stock debe ser un número entero, sin decimales." / "…mayor que cero." / "…fuera del rango permitido." | `stockNoEntero`, `stockNoPositivo`, `stockFueraDeRango` | ✅ |
| V10 | Actualizar o eliminar sin selección o sobre un registro que ya no existe | Sin fila seleccionada; ID ya eliminado | "Selecciona un producto de la tabla para actualizar/eliminar." / "…el registro seleccionado ya no existe. Se recargó la tabla." | `ProductoDAOTest.filasAfectadasCero` y revisión del controlador | ✅ |

Además, PostgreSQL rechaza los mismos datos si llegaran a la base sin pasar por el formulario (ver E2 y la sección de CHECK).

## Excepciones y consistencia (E1–E4)

| ID | Caso | Procedimiento | Obtenido | Resultado |
|---|---|---|---|---|
| E1 | Fallo de conexión | `DB_URL=jdbc:postgresql://localhost:5999/papeleria_campus` (puerto sin servidor) y consulta del listado | Mensaje: "No se pudo conectar con la base de datos. Verifique que PostgreSQL esté en ejecución." La aplicación no se cierra, la tabla queda vacía y el contador indica "No se pudieron cargar los productos." | ✅ |
| E2 | Unicidad y CHECK gestionados en el DAO | `codigoDuplicado`, `violacionCheck` (precio 0 al insertar, stock -1 al actualizar) | SQLState 23505 y 23514 traducidos a mensajes propios; la fila duplicada no se inserta y la fila actualizada conserva `stock = 5` (transacción revertida) | ✅ |
| E3 | Filas afectadas en UPDATE y DELETE | `filasAfectadasCero`: UPDATE y DELETE sobre un ID ya eliminado | Ambos devuelven `false`; la interfaz muestra aviso en lugar de éxito y recarga la tabla | ✅ |
| E4 | Registro de SQLState + operación y mensaje seguro | `mensajeSeguro` y salida del registro | El mensaje no contiene "duplicate", nombres de restricciones ni del esquema; la causa original se conserva | ✅ |

Salida real del registro durante las pruebas:

```
SEVERE: Operación=SELECT productos activos | SQLState=08001 | código=0 | detalle=Connection to localhost:5999 refused. ...
SEVERE: Operación=INSERT producto codigo=ZTEST2 | SQLState=23505 | código=0 | detalle=ERROR: duplicate key value violates unique constraint "producto_codigo_key"
SEVERE: Operación=INSERT producto codigo=ZTEST3 | SQLState=23514 | código=0 | detalle=ERROR: new row for relation "producto" violates check constraint "chk_precio"
SEVERE: Operación=UPDATE producto id=42 | SQLState=23514 | código=0 | detalle=ERROR: new row for relation "producto" violates check constraint "chk_stock"
WARNING: Operación=UPDATE producto id=37 | filas afectadas=0 | cambio revertido
WARNING: Operación=DELETE producto id=37 | filas afectadas=0 | cambio revertido
```

## Restricciones CHECK en PostgreSQL

Script completo ejecutado con `psql` en una base vacía: crea el esquema, la tabla y 12 productos, y cada INSERT de prueba falla con la restricción esperada.

```
CREATE SCHEMA
CREATE TABLE
INSERT 0 12
ERROR:  new row for relation "producto" violates check constraint "chk_codigo_normalizado"
ERROR:  new row for relation "producto" violates check constraint "chk_nombre_longitud"
ERROR:  new row for relation "producto" violates check constraint "chk_categoria"
ERROR:  new row for relation "producto" violates check constraint "chk_precio"
ERROR:  new row for relation "producto" violates check constraint "chk_stock"
```

## Pruebas fallidas registradas

| ID | Prueba | Falla detectada | Corrección |
|---|---|---|---|
| F1 | Compilar desde GitHub | El repositorio no incluía `ProductoDAO.java`; el proyecto clonado no compilaba | Commit `1508c02` |
| F2 | Pruebas CHECK del script | Los cinco INSERT usaban `'Papelería'` con tilde y todos fallaban por `chk_categoria` en lugar de la restricción que se quería probar (ver `Evidencias/ ValidacionChecks.png`); además faltaban `;` tras `create schema` y `create table` | Commit `a3e65e6` |
| F3 | Crear / Eliminar: contador | `lblContador` existía en el FXML pero no estaba enlazado ni se actualizaba | Commit `eb8062b` |
| F4 | Actualizar respeta filtros | Tras actualizar se recargaba la tabla completa ignorando los filtros aplicados | Commit `eb8062b` |
| F5 | Validaciones al actualizar | Actualizar no validaba precio ni stock mayores que cero, ni longitudes; el error llegaba como texto crudo de PostgreSQL | Commit `a3e65e6` |
| F6 | E2 / E4 | Se mostraba `ex.getMessage()` de PostgreSQL al usuario y no se registraba el SQLState ni la operación | Commit `6e15230` |
| F7 | B5 *Sin stock* | Siempre 0 filas porque `chk_stock` exige `stock > 0` | ⚠️ Abierta: depende de si el enunciado admite stock 0 (cambiar a `stock >= 0`) |

## Evidencias

- `src/main/java/com/appregistros/practicaevaluadacorte2/Evidencias/CargaInicialContador.png`: carga inicial con contador.
- `src/main/java/com/appregistros/practicaevaluadacorte2/Evidencias/EvidenciaCargaDatos.png`: carga del script.
- `src/main/java/com/appregistros/practicaevaluadacorte2/Evidencias/ ValidacionChecks.png`: ejecución original de las pruebas CHECK (F2).
- Salida de `./mvnw test` y del registro incluida arriba.
