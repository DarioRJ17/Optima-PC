package com.optimapc.backend.montarPC;

// Representa un componente que se aleja significativamente del equilibrio esperado.
// 'desviacion' es el valor absoluto de la diferencia (0.0–1.0).
public record ComponenteDesbalanceado(String nombre, boolean sobredimensionado, double desviacion) {}
