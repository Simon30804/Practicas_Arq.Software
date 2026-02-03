package Practica1.Observador.Visualizadores;

import java.util.List;

import Practica1.Observador.Visualizador;

public class GraficaLinea extends Visualizador {
    private static final int ALTURA = 10;

    @Override
    public void mostrarVisualizacion(List<Integer> x, List<Integer> y) {
        if (y == null || y.isEmpty()) {
            System.out.println("No hay datos para graficar.");
            return;
        }
        int minY = y.stream().min(Integer::compareTo).orElse(0);
        int maxY = y.stream().max(Integer::compareTo).orElse(0);
        mostrarVisualizacion(x, y, minY, maxY);
    }

    @Override
    public void mostrarVisualizacion(List<Integer> x, List<Integer> y, int minY, int maxY) {
        if (y == null || y.isEmpty()) {
            System.out.println("No hay datos para graficar.");
            return;
        }
        if (maxY == minY) {
            maxY = minY + 1;
        }

        System.out.println("Gráfica de línea (rango Y: " + minY + " - " + maxY + ")");
        int n = y.size();
        int[] niveles = new int[n];

        for (int i = 0; i < n; i++) {
            int yi = y.get(i);
            int nivel = (int) Math.round((yi - minY) * 1.0 * (ALTURA - 1) / (maxY - minY));
            niveles[i] = Math.max(0, Math.min(ALTURA - 1, nivel));
        }

        for (int fila = ALTURA - 1; fila >= 0; fila--) {
            StringBuilder linea = new StringBuilder();
            for (int i = 0; i < n; i++) {
                linea.append(niveles[i] == fila ? "* " : "  ");
            }
            System.out.println(linea);
        }

        if (x != null && x.size() == n) {
            StringBuilder ejeX = new StringBuilder();
            for (Integer xi : x) {
                ejeX.append(xi).append(" ");
            }
            System.out.println(ejeX);
        }
    }
}
