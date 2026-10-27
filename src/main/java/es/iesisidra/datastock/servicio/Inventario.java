package es.iesisidra.datastock.servicio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import es.iesisidra.datastock.config.XmlConfig;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.repository.ProductoXmlRepository;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;

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

    /**
     * Metodo para importar JSON con comprobaciones y validando el producto en
     * base a las reglas impuestas en nuestro metodo validarProducto
     *
     * @param ruta
     */
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

    // Instancia reutilizable del repositorio
    private ProductoXmlRepository xmlRepository = new ProductoXmlRepository();

    /**
     * Importa un catálogo de productos desde un fichero XML usando DOM. Lee el
     * fichero con el repositorio, valida los campos con if-else tradicionales y
     * añade solo los registros válidos al inventario.
     *
     * @param ruta
     */
    public void importarXml(String ruta) {
        // 1. Validar si la ruta recibida es nula o está vacía
        if (ruta == null || ruta.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "ERROR: La ruta está vacía." + ColoresConsola.RESET);
            return;
        }

        // 2. Comprobar existencia del fichero con la API Path de Java NIO
        Path path = Paths.get(ruta.trim());
        if (!Files.exists(path) || Files.isDirectory(path)) {
            System.out.println(ColoresConsola.ROJO + "ERROR: El fichero no existe o es un directorio." + ColoresConsola.RESET);
            return;
        }

        try {
            // 3. Cargar el árbol de nodos DOM a través del repositorio seguro
            Document doc = xmlRepository.leer(path);

            // Obtenemos todos los elementos con la etiqueta <producto>
            NodeList listaNodos = doc.getElementsByTagName("producto");
            int encontrados = listaNodos.getLength();

            int importados = 0;
            int rechazados = 0;
            List<String> incidencias = new ArrayList<>();

            // 4. Recorrido imperativo de la lista de nodos (sin lambdas ni iteradores complejos)
            for (int i = 0; i < encontrados; i++) {
                Node nodo = listaNodos.item(i);

                // Filtrar solo los nodos tipo elemento para evitar nodos de texto/saltos de línea
                if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                    Element elemento = (Element) nodo;
                    try {
                        // Extraer textos de las etiquetas
                        String codigo = obtenerTextoEtiqueta(elemento, "codigo");
                        String nombre = obtenerTextoEtiqueta(elemento, "nombre");

                        // Parseo del precio mediante estructura if-else sin operador ternario
                        String strPrecio = obtenerTextoEtiqueta(elemento, "precio");
                        BigDecimal precio = null;
                        if (strPrecio != null && !strPrecio.isEmpty()) {
                            try {
                                precio = new BigDecimal(strPrecio);
                            } catch (NumberFormatException nfe) {
                                precio = null;
                            }
                        }

                        // Parseo del stock
                        String strStock = obtenerTextoEtiqueta(elemento, "stock");
                        int stock = 0;
                        if (strStock != null && !strStock.isEmpty()) {
                            stock = Integer.parseInt(strStock);
                        }

                        // Parseo de los IDs
                        String strCatId = obtenerTextoEtiqueta(elemento, "categoriaId");
                        int categoriaId = 0;
                        if (strCatId != null && !strCatId.isEmpty()) {
                            categoriaId = Integer.parseInt(strCatId);
                        }

                        String strProvId = obtenerTextoEtiqueta(elemento, "proveedorId");
                        int proveedorId = 0;
                        if (strProvId != null && !strProvId.isEmpty()) {
                            proveedorId = Integer.parseInt(strProvId);
                        }

                        // Construcción del objeto Producto
                        Producto p = new Producto(codigo, nombre, precio, stock, categoriaId, proveedorId);

                        if (p.getCodigo() != null) {
                            p.setCodigo(p.getCodigo().trim());
                        }

                        // Reutilizar el método de validación común (sin lambdas/break)
                        List<String> errores = validarProducto(p);

                        if (errores.isEmpty()) {
                            productos.add(p);
                            importados++;
                        } else {
                            rechazados++;
                            StringBuilder msjError = new StringBuilder();
                            for (int j = 0; j < errores.size(); j++) {
                                msjError.append(errores.get(j));
                                if (j < errores.size() - 1) {
                                    msjError.append(", ");
                                }
                            }

                            // Determinar el identificador para la incidencia sin interrogación (sin ternario)
                            String cod;
                            if (p.getCodigo() != null && !p.getCodigo().isEmpty()) {
                                cod = p.getCodigo();
                            } else {
                                cod = "Producto " + (i + 1);
                            }

                            incidencias.add(cod + ": " + msjError.toString());
                        }

                    } catch (Exception e) {
                        rechazados++;
                        incidencias.add("Producto " + (i + 1) + ": Error al parsear los datos.");
                    }
                }
            }

            // 5. Presentación de resultados por consola
            System.out.println("---- IMPORTAR XML ----");
            System.out.println("Ruta: " + ruta);
            System.out.println("Productos encontrados: " + encontrados);
            System.out.println("Importados: " + importados + " | Rechazados: " + rechazados);

            if (!incidencias.isEmpty()) {
                System.out.println("INCIDENCIAS:");
                for (String inc : incidencias) {
                    System.out.println(" - " + inc);
                }
            }

        } catch (org.xml.sax.SAXException e) {
            System.out.println(ColoresConsola.ROJO + "ERROR: El fichero XML está malformado. Importación cancelada." + ColoresConsola.RESET);
        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "ERROR al leer el fichero: " + e.getMessage() + ColoresConsola.RESET);
        }
    }

    /**
     * Método auxiliar privado para obtener el valor texto de una etiqueta hija.
     */
    private String obtenerTextoEtiqueta(Element elementoPadre, String nombreEtiqueta) {

        NodeList lista = elementoPadre.getElementsByTagName(nombreEtiqueta);
        // si la lista existe y tiene al menos un elemento
        if (lista != null && lista.getLength() > 0) {
            Node nodo = lista.item(0);
            // si el nodo existe y tiene texto dentro
            if (nodo != null && nodo.getTextContent() != null) {
                return nodo.getTextContent().trim();
            }
        }
        // si la ertiqueta no existia o estaba vacioa
        return null;
    }

    public void listarTodos() {
        if (productos.isEmpty()) { // si esta vacio no debo mostrar nada mas que el aviso
            System.out.println(ColoresConsola.AMARILLO + "El inventario esta vacio" + ColoresConsola.RESET);
            return;
        }

        // ordeno por el metodo burbuja
        int n = productos.size();
        for (int i = 0; i < n - 1; i++) { // este for se encarga de repetir el proceso hasta que se culmine la ordenacion
            for (int j = 0; j < n - i - 1; j++) { // este for nos ayuda a comparar a "vecinos" es decir de dos en dos
                String c1 = productos.get(j).getCodigo();
                // siempre seleccionamos el que este un puesto a la derecha del
                String c2 = productos.get(j + 1).getCodigo();

                // tengo que tener cuidado ya que no puedo hacer un compare.ToIgnoraCase si uno de los dos es null
                if (c1 == null) {
                    c1 = "";
                }
                if (c2 == null) {
                    c2 = "";
                }

                // si el código actual es alfabeticamente mayor que el siguiente se intercambian
                if (c1.compareToIgnoreCase(c2) > 0) {
                    Producto temp = productos.get(j);
                    productos.set(j, productos.get(j + 1));
                    productos.set(j + 1, temp);
                }

            }

        }

        System.out.println("---- LISTADO DE PRODUCTOS ----");
        for (int k = 0; k < productos.size(); k++) { // enseño todos los productos
            System.out.println(productos.get(k).toString());

        }
    }

    public void buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "El codigo no puede estar vacio" + ColoresConsola.RESET);
            return;
        }

        // en caso de que el codigo tenga algo entramos al for a comparar
        for (Producto producto : productos) {
            if (producto.getCodigo() != null) { // no puedo comparar si es null
                if (producto.getCodigo().equals(codigo.trim())) { // comparamos codigos y en caso de que coincidan enseño su info
                    System.out.println(producto.toString());
                }

            }

        }
    }

    public void buscarPorNombre(String nombre) { // el codigo es practicamente igual solo que aqui si aplicamos el equalsIgnoreCase
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "El nombre no puede estar vacio" + ColoresConsola.RESET);
            return;
        }

        // en caso de que el codigo tenga algo entramos al for a comparar
        for (Producto producto : productos) {
            if (producto.getCodigo() != null) { // no puedo comparar si es null
                if (producto.getCodigo().equals(nombre.trim())) { // comparamos codigos y en caso de que coincidan enseño su info
                    System.out.println(producto.toString());
                }

            }

        }

    }

    /**
     * Metodo exportarJson que nos permite exportar el catalogo de productos a
     * un fichero en formato JSON usando la libreria GSON
     *
     * @param ruta Ruta del fichero donde se guaradara el JSON
     */
    public void exportarJson(String ruta) {
        // si no hay nada no exportamos
        if (productos.isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "El inventario está vacío. No hay nada que exportar." + ColoresConsola.RESET);
            return;
        }

        // la ruta debe ser valida
        if (ruta == null || ruta.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "ERROR: La ruta de destino no es válida." + ColoresConsola.RESET);
            return;
        }

        // creo una instancia de Gson configurada para aplicar formato legible (pretty printing)
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // try-with-resources para abrir y cerrar el writer de forma segura
        try (FileWriter writer = new FileWriter(ruta.trim())) {

            // convierto la lista de productos completa a texto JSON
            gson.toJson(productos, writer);

            System.out.println(ColoresConsola.VERDE + "Catalogo exportado correctamente a JSON en: " + ruta + ColoresConsola.RESET);

        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "ERROR al exportar a JSON: " + e.getMessage() + ColoresConsola.RESET);
        }

    }

    /**
     * Exporta el catálogo actual de productos a un fichero en formato XML
     * utilizando DOM
     *
     * @param ruta Ruta del fichero de destino donde se guarda el XML
     */
    public void exportarXml(String ruta) {
        if (productos.isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "El inventario está vacío. No hay nada que exportar." + ColoresConsola.RESET);
            return;
        }

        if (ruta == null || ruta.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "ERROR: La ruta de destino no es válida." + ColoresConsola.RESET);
            return;
        }

        try {
            // Inicializo un nuevo documento DOM en blanco
            DocumentBuilder builder = XmlConfig.domFactory().newDocumentBuilder();
            Document doc = builder.newDocument();

            // creo el nodo raiz <catalogo> y lo añadimos al documento
            Element root = doc.createElement("catalogo");
            doc.appendChild(root);

            // Recorro para incluir todos los productos
            for (int i = 0; i < productos.size(); i++) {
                Producto p = productos.get(i);

                // creo la etiqueta del nodo producto
                Element elemProducto = doc.createElement("producto");

                // añado las etiquetas hijas con sus valores correspondientes
                crearEtiquetaHija(doc, elemProducto, "codigo", p.getCodigo());
                crearEtiquetaHija(doc, elemProducto, "nombre", p.getNombre());

                String strPrecio = "";
                if (p.getPrecio() != null) {
                    strPrecio = p.getPrecio().toString();
                }

                // creo las etiquetas de los atributos de cada producto
                crearEtiquetaHija(doc, elemProducto, "precio", strPrecio);
                crearEtiquetaHija(doc, elemProducto, "stock", String.valueOf(p.getStock()));
                crearEtiquetaHija(doc, elemProducto, "categoriaId", String.valueOf(p.getCategoriaId()));
                crearEtiquetaHija(doc, elemProducto, "proveedorId", String.valueOf(p.getProveedorId()));

                // pongo cada producto en el catalogo
                root.appendChild(elemProducto);
            }

            // uso el emtodo del repositorio para guardar toda la estrictura del cocumento creado
            Path pathDestino = Paths.get(ruta.trim());
            xmlRepository.guardar(doc, pathDestino);

            System.out.println(ColoresConsola.VERDE + "Catálogo exportado correctamente a XML en: " + ruta + ColoresConsola.RESET);

        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "ERROR al exportar a XML: " + e.getMessage() + ColoresConsola.RESET);
        }
    }

    /**
     * Metodo auxiliar privado para simplificar la creación y adición de
     * etiquetas hijas al DOM
     */
    private void crearEtiquetaHija(Document doc, Element padre, String nombreEtiqueta, String valorTexto) {
        Element hija = doc.createElement(nombreEtiqueta);
        if (valorTexto != null) {
            hija.setTextContent(valorTexto);
        } else {
            hija.setTextContent("");
        }
        padre.appendChild(hija);
    }

    /**
     * Metodo encargado de la serializacion de productos
     *
     * @param ruta donde se crea el archivo .dat
     */
    public void serializarProductos(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "La ruta no puede estar vacia" + ColoresConsola.RESET);
            return; // no sigo
        }

        if (productos.isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "No hay productos" + ColoresConsola.RESET);
            return;
        }

        // uso try-with-resources para asegurar bien el cierre
        String confirmacion = LectorConsola.leerTexto("Todo preparado para la serializacion confirmacion: SI/NO");
        if (confirmacion == null || confirmacion.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "La confirmacion no puede estar vacia debe ser si o no"
                    + ColoresConsola.RESET);
            return;
        }

        // si es false no sigo
        if (!LectorConsola.confirmar(confirmacion)) {
            System.out.println(ColoresConsola.VERDE + "NEGACION RECIBIDA SALIENDO..." + ColoresConsola.RESET);
            return;
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ruta.trim()))) {
            oos.writeObject(productos);
            System.out.println(ColoresConsola.VERDE + productos.size() + " productos serializados correctamente" + ColoresConsola.RESET);
        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "ERROR a la hora de serializar los productos: " + e.getMessage()
                    + ColoresConsola.RESET);

        }

    }

    /**
     * Metodo encargado de obtener los productos ya serializados
     *
     * @param ruta donde esta el archivo .dat
     */
    public void deserializarProductos(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            System.out.println(ColoresConsola.ROJO + "La ruta no puede estar vacia" + ColoresConsola.RESET);
            return; // no sigo
        }

        if (!productos.isEmpty()) {
            String confirmacion = LectorConsola.leerTexto(
                    "Todo preparado para la deserializacion confirmacion para sustituir los productos en memoria: S/N");
            if (confirmacion == null || confirmacion.trim().isEmpty()) {
                System.out.println(ColoresConsola.ROJO + "La confirmacion no puede estar vacia debe ser S o N"
                        + ColoresConsola.RESET);
                return;
            }

            // si es false no sigo
            if (!LectorConsola.confirmar(confirmacion)) {
                System.out.println(ColoresConsola.VERDE + "NEGACION RECIBIDA SALIENDO..." + ColoresConsola.RESET);
                return;
            }
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ruta.trim()))) {
            // obtengo los productos
            List<Producto> productosObtenidos = (List<Producto>) ois.readObject();
            // creo una lista auxiliar
            List<Producto> listaAuxiliar = new ArrayList<>();

            // si el producto cumple todo entonces va a mi lista auxiliar
            for (Producto producto : productosObtenidos) {
                if (validarProducto(producto).isEmpty()) {
                    listaAuxiliar.add(producto);
                }
            }

            // quito los que estaban anteriormente y añado los deserializados
            productos.clear();
            productos.addAll(listaAuxiliar);
            System.out.println(ColoresConsola.VERDE + productos.size() + " productos obtenidos correctamente" + ColoresConsola.RESET);

        } catch (Exception e) {
            System.out.println(ColoresConsola.ROJO + "ERROR a la hora de obtener los productos: " + e.getMessage()
                    + ColoresConsola.RESET);
        }

    }

}
