package com.optimapc.backend.montarPC;

import java.util.List;

// Resultado del cálculo de equilibrio de una configuración de PC.
// 'score' va de 0 a 100 (100 = equilibrio perfecto).
// 'componentes' lista todos los que superan el umbral de desviación, ordenados de mayor a menor desviación.
public record EquilibrioResult(double score, List<ComponenteDesbalanceado> componentes) {}
