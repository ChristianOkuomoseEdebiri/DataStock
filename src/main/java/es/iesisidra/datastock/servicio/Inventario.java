package es.iesisidra.datastock.servicio;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.util.ColoresConsola;

/**
 * Estado común del inventario durante la ejecución de UT1.
 * <p>
 * Esta clase solo proporciona la colección compartida. No implementa
 * importación, validación, búsqueda, exportación ni persistencia: esos son
 * ejercicios que deberá desarrollar el alumnado.</p>
 */
public class Inventario {

    private final List<Producto> productos = new ArrayList<>();

    /**
     * Obtiene la colección que comparten las opciones del menú.
     * <p>
     * ATENCIÓN: la lista es modificable. El alumnado deberá aplicar las reglas
     * del enunciado antes de añadir o sustituir elementos.</p>
     */
    public List<Producto> getProductos() {
        return productos;
    }

    /**
     * metodo explorarRuta que nos permite ver toda la informacion subyacente de
     * una ruta dada por el usuario
     *
     * @param ruta
     */
    public void explorarRuta(String ruta) {

        try {
            if (ruta == null || ruta.trim().isEmpty()) { //la ruta que me dan no debe estar vacia
                System.out.println(ColoresConsola.ROJO + "ERROR: Se debe proporcionar una ruta válida (el texto está vacio)" + ColoresConsola.RESET);
                return; // no seguimos
            }
            // en caso de que la entrada no este vacia

            File archivo = new File(ruta.trim());

            // Primer caso: la ruta no existe
            if (!archivo.exists()) {
                System.out.println(ColoresConsola.ROJO + "ERROR: Se debe proporcionar una ruta válida (no existe)" + ColoresConsola.RESET);
                return; // no seguimos
            }

            // comprobamos si es un fichero y si es el caso mostramos su informacion
            if (archivo.isFile()) {
                System.out.println("---- EXPLORAR FICHERO ----");
                System.out.println("[NOMBRE] " + archivo.getName());
                System.out.println("[RUTA ABSOLUTA] " + archivo.getAbsolutePath());
                System.out.println("[TAMAÑO EN BYTES] " + archivo.length());
                return;
            }

            if (archivo.isDirectory()) {
                System.out.println("---- EXPLORAR DIRECTORIO ----");
                System.out.println("[RUTA] " + ruta);
                File[] contenido = archivo.listFiles(); // obtenemos el contenido del directorio y nos preparamos para recorrerlo en caso de que no este vacio

                if (contenido == null) { // es nulo o no contiene nada
                    System.out.println(ColoresConsola.AMARILLO + "AVISO: NO HAY CONTENIDO EN LA RUTA PROPORCIONADA" + ColoresConsola.RESET);
                    return; // no seguimos
                }

                // inicializo y sumare mientras recorro
                int totalFicheros = 0;
                int totalDirectorios = 0;

                for (File file : contenido) {
                    if (file.isFile()) {
                        System.out.println("[FICHERO] " + file.getName() + " " + file.length() + " bytes");
                        totalFicheros++; // sumo
                    } else if (file.isDirectory()) {
                        System.out.println("[DIR] " + file.getName());
                        totalDirectorios++; //sumo
                    }
                }

                System.out.println("Total ficheros: " + totalFicheros + " | Directorios: " + totalDirectorios);

            }

        } catch (SecurityException se) { // el sistema operativo bloquea por permisos
            System.out.println(ColoresConsola.ROJO + "Error de acceso: Permisos denegados por el sistema " + se.getMessage() + ColoresConsola.RESET);

        } catch (Exception e) { // cualquier otra excepcion inesperada
            System.out.println(ColoresConsola.ROJO + "Error inesperado al explorar la ruta " + e.getMessage() + ColoresConsola.RESET);
        }

    }

    /**
     * Valida un producto según las reglas dadas Devuelve una lista de errores,
     * si esta vacia: el producto es valido
     *
     * @param p
     * @return
     */
    private List<String> validarProducto(Producto p) {
        List<String> errores = new ArrayList<>();

        // 1. Comprobaciones del codigo
        if (p.getCodigo() == null || p.getCodigo().trim().isEmpty()) { // no puede estar vacio
            errores.add("El codigo es obligatorio");
        } else {
            String codigoLimpio = p.getCodigo().trim();
            if (codigoLimpio.length() > 12) { // no puede superar los 12 caracteres
                errores.add("No puede superar los 12 caracteres");
            }
            if (!codigoLimpio.matches("[A-Z0-9-]+")) {
                errores.add("No cumple con el patron valido");
            }

            // hay que comprobar si existe en memorio
            boolean duplicado = false;
            int i = 0; // para recorrer todos los productos
            while (i < productos.size() && !duplicado) { // mientras no tenga mas posiciones de procutos que recorrer y no haya encontradado un duplicado
                Producto prod = productos.get(i);
                if (prod.getCodigo() != null && prod.getCodigo().equalsIgnoreCase(codigoLimpio)) {
                    duplicado = true;

                }
                i++;

            }

            if (duplicado == true) {
                errores.add("Codigo duplicado");
            }
        }

        // 2. Comprobaciones del nombre
        if (p.getNombre() == null || p.getNombre().trim().length() < 3 || p.getNombre().length() > 100) {
            errores.add("El nombre debe tener entre 3 y 100 caracteres además de no poder estar vacio");
        }

        // 3. Comprobaciones del precio
        if (p.getPrecio() == null || p.getPrecio().doubleValue() <= 0) {
            errores.add("precio debe ser mayor que 0");
        }

        // 4. Stock
        if (p.getStock() < 0) {
            errores.add("El stock no puede ser negativo");
        }

        // 5. CategoriaId y ProveedorId
        if (p.getCategoriaId() <= 0) {
            errores.add("El ID de categoría debe ser mayor que 0");
        }
        if (p.getProveedorId() <= 0) {
            errores.add("El ID de proveedor debe ser mayor que 0");
        }

        return errores;
    }

    public void importarJson(String ruta) {
        // primero valido la ruta
        if (ruta == null || ruta.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "No puedes introducir una ruta vacia" + ColoresConsola.RESET);
            return; // salgo
        }

        // si no esta vacia le quito espacios y verifico que exista
        File archivo = new File(ruta.trim());
        try {
            if (!archivo.exists() || !archivo.isFile()) { // debe de existir y no ser un directorio
                System.out.println(ColoresConsola.ROJO + "La ruta introducida no existe/es un directorio" + ColoresConsola.RESET);
                return;
            }

        } catch (SecurityException se) {
            System.out.println(ColoresConsola.ROJO + "Error de permisos: " + se.getMessage() + ColoresConsola.RESET);
            return;
        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "Error imprevisto: " + e.getMessage() + ColoresConsola.RESET);
            return;
        }

        // uso el try-with-resources para el cierre automatico del FileReader
        try (FileReader reader = new FileReader(archivo)) {
            // convierto el contenido del archivo en una estructura JSON
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            // verifico que la raiz tenga "productos" y que efectivamente sea un array
            if (!root.has("productos") || !root.get("productos").isJsonArray()) {
                System.out.println(ColoresConsola.ROJO + "La raiz debe debe contener el array de productos" + ColoresConsola.RESET);
                return;
            }

            // obtengo el array de productos y preparo el Gson para la conversion
            JsonArray arrayproductos = root.getAsJsonArray("productos");

            Gson gson = new Gson();

            // inicializo contadores y preparo la lista donde guardo los mensajes de error
            int encontrados = arrayproductos.size();
            int rechazados = 0;
            int importados = 0;

            List<String> incidencias = new ArrayList<>();

            for (int i = 0; i < encontrados; i++) {

                try {
                    // deserealizo el objeto en una instancia de la clase Producto
                    Producto p = gson.fromJson(arrayproductos.get(i), Producto.class);

                    // limpio espacios del codigo del producto
                    if (p.getCodigo() != null) { // si es nulo e intento hacer .trim tendria problemas
                        p.setCodigo(p.getCodigo().trim());
                    }

                    // validamos las reglas predefinidas antes
                    List<String> errores = validarProducto(p);

                    // si esta vacio no hay errores
                    if (errores.isEmpty()) {
                        productos.add(p);
                        importados++;
                    } else {
                        rechazados++; // tiene minimo un error

                        // construyo la cadena con los motivos de rechazo
                        StringBuilder mensajeError = new StringBuilder();

                        for (int j = 0; j < errores.size(); j++) {
                            mensajeError.append(errores.get(j));

                            if (j < errores.size() - 1) {
                                mensajeError.append(", "); // separador entre errores
                            }
                        }
                        // guardo la incidencia pero comprobando que el codigo no sea nulo
                        String cod;
                        if (p.getCodigo() != null && !p.getCodigo().isEmpty()) {
                            cod = p.getCodigo();
                        } else {
                            cod = "Producto " + (i + 1);
                        }

                        incidencias.add(cod + ": " + mensajeError.toString());
                    }

                } catch (Exception e) {
                    rechazados++;
                    incidencias.add("Producto " + (i + 1) + ": Error en el formato de datos");

                }

            }

            // resultado
            System.out.println("--- IMPORTAR JSON ---");
            System.out.println("Ruta: " + ruta);
            System.out.println("Productos encontrados: " + encontrados);
            System.out.println("Importados: " + importados + " | Rechazados: " + rechazados);

            // si se registraron incidencias durante el proceso, las muestro
            if (!incidencias.isEmpty()) {
                System.out.println("INCIDENCIAS:");
                for (String inc : incidencias) {
                    System.out.println(" - " + inc);
                }
                System.out.println("Importación finalizada con incidencias");
            } else {
                System.out.println("Importación finalizada con éxito");
            }
        } catch (JsonSyntaxException jse) { // ssuelen ser errores de sintaxis del JSON
            System.out.println(ColoresConsola.ROJO + "Error: El fichero JSON está malformado. Se cancela la importación." + jse.getMessage() + ColoresConsola.RESET);
        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "ERROR al leer el fichero: " + e.getMessage() + ColoresConsola.RESET);
        }

    }

}
