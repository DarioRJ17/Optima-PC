package com.optimapc.backend.montarPC;

import java.util.List;

import org.springframework.stereotype.Service;

import com.optimapc.backend.modelo.Componente;

/**
 * Default CompatibilityService that explicitly reminds developer to implement rules.
 * It throws UnsupportedOperationException to make the missing implementation visible at runtime.
 */
@Service
public class DefaultCompatibilityService {

    public boolean isCompatible(Componente candidate, List<Componente> selected) {
        throw new UnsupportedOperationException("Compatibility check not implemented. Please implement CompatibilityService with your rules.");
    }
}
