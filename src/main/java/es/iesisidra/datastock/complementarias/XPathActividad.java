package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.servicio.Inventario;

/**
 * Punto de entrada reservado para RF-FIC-07: consultas XPath.
 * <p>Actividad obligatoria e independiente del menú principal.</p>
 */
public final class XPathActividad {
    private XPathActividad() { }
    /** TODO: implementar las cuatro consultas XPath descritas en el enunciado. */
    public static void main(String[] args) {
        Inventario inventario = new Inventario();
        inventario.consultaXPath("datos/ejemplos/catalogo_valido.xml");
    }
}
