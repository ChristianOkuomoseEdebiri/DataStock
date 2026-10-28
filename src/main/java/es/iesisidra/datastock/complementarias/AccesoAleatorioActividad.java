package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.servicio.Inventario;

/**
 * Punto de entrada reservado para RF-FIC-08: RandomAccessFile.
 * <p>Actividad obligatoria e independiente del menú principal.</p>
 */
public final class AccesoAleatorioActividad {
    private AccesoAleatorioActividad() { }
    /** TODO: generar, consultar y modificar registros de 36 bytes. */
    public static void main(String[] args) {
        Inventario inventario = new Inventario();
        inventario.importarXml("datos/ejemplos/catalogo_valido.xml");
        inventario.generarStockBinario("datos/salidas/stock.bin");
        inventario.gestionarAccesoAleatorioStock("datos/salidas/stock.bin");
    }
}
